package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernWorldProtocol.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ModernMatchDescriptorServiceTest {
    static final String ZERO="0".repeat(64);
    static final class Journal implements ModernMatchSettlement.JournaledStore {
        int reservations,records;ModernMatchSettlement.Event event;
        public ModernMatchSettlement.Reservation reserve(long match,long generation){reservations++;return new ModernMatchSettlement.Reservation(){
            boolean saved;public void record(ModernMatchSettlement.Event e,long deadline){assertEquals(match,e.matchId());assertEquals(generation,e.generation());assertFalse(saved);saved=true;records++;event=e;}
            public ModernMatchSettlement.ReceiptState state(){return saved?ModernMatchSettlement.ReceiptState.SAVED:ModernMatchSettlement.ReceiptState.PENDING;}
            public void close(){}
        };}
        public void persist(ModernMatchSettlement.Event e,long deadline){throw new AssertionError("journal path only");}
        public boolean accepting(){return true;}public void close(){}public boolean awaitTermination(long millis){return true;}public boolean isQuiescent(){return true;}
    }
    static final class Fixture implements AutoCloseable {
        final ModernRoomsTest.Fixture rooms=new ModernRoomsTest.Fixture();
        final ModernRoomsTest.Player a=rooms.player(7,3333,80,0),b=rooms.player(42,3333,80,0);
        final Journal journal=new Journal();final AtomicInteger reads=new AtomicInteger(),captures=new AtomicInteger(),terminals=new AtomicInteger();
        final Map<Integer,ModernShopAuthority> shops=new java.util.HashMap<>();
        final Map<Integer,ModernShopCatalog.Snapshot> catalogs=new java.util.HashMap<>();
        final ModernWorldService service;
        Fixture(int secondVersion)throws Exception {
            var map=ModernWorldMap.fromSectorBytes(514,Map.of(new ModernWorldMap.SectorKey(0x8000,0x8000),ModernWorldMapTest.openSectorBytes()),()->false);
            var scene=new ModernWorldBinding(1,514,map);var fight=new ModernWorldBinding(1,509,map);var cell=new ModernWorldMap.Cell(32778,32778);
            service=new ModernWorldService(rooms.rooms,Map.of(scene.key(),new ModernWorldService.Mapping(scene,cell,1_000_000),
                fight.key(),new ModernWorldService.Mapping(fight,cell,1_000_000,1_000_000,0,false,false,true)),
                new ModernWorldService.Limits(4,1,1,2000,5,new ModernBattleEngine.Limits(2000,1,10000)),bound->bound-1,journal);
            for(var p:List.of(a,b)){
                var selection=rooms.presence.selected(p.owner());var row=new ModernApplicationProtocol.CharacterRow(3333,selection.characterName(),selection.characterClass(),selection.sex(),80);
                var fighter=ModernPhysicalCombatTest.fighter(ModernBattleEngineHonorTest.source(p.scope().sourceId(),row,p==a?100:1,0));
                var catalog=new ModernShopCatalog.Snapshot(p.scope().sourceId(),"fixture",row,List.of(ModernShopAuthorityTest.offer(40,40)));
                var shop=new ModernShopAuthority(20000,p.scope().sourceId(),row,"fixture");shops.put(p.scope().sourceId(),shop);catalogs.put(p.scope().sourceId(),catalog);
                var view=ModernShopAuthorityTest.read(shop,catalog);assertEquals(ModernShopAuthority.Status.OK,ModernShopAuthorityTest.buy(shop,view,2,catalog).status());
                int version=p==a?15:secondVersion;int caps=version>=15?65536|767:767;
                service.register(p.owner(),p.scope(),deadline->{reads.incrementAndGet();return row;},deadline->{reads.incrementAndGet();return fighter;},true,null,false,new ModernWorldService.MatchItems(){
                    public List<ModernShopAuthority.Stack> capture(long match,long generation,long deadline){captures.incrementAndGet();return shop.capture(match,generation,deadline,catalog);}
                    public void terminal(long match,long generation){terminals.incrementAndGet();shop.terminal(match,generation);}
                },false,false,false,false,false,false,new ModernSessionBoundary.Negotiated(1,version,1,caps));
            }
            ModernRoomsTest.accepted(rooms.create(a,ModernRoomsTest.draft(2,1,514,0)));ModernRoomsTest.accepted(rooms.join(b,rooms.view(a).joined().summary(),""));
        }
        void configure(int map){ModernRoomsTest.accepted(rooms.act(a,ModernRooms.Action.CONFIGURE,0,ModernRoomsTest.draft(2,1,map,0)));}
        Reply prepare(){for(var p:List.of(a,b))ModernRoomsTest.accepted(rooms.act(p,ModernRooms.Action.READY,1));var s=rooms.view(a).joined().summary();return service.handle(a.owner(),new Request(a.scope(),Action.PREPARE,0,s.id(),s.revision(),0,0,0,0,0,ZERO));}
        Frame read(){return service.handle(a.owner(),new Request(a.scope(),Action.READ,0,0,0,0,0,0,0,0,ZERO)).frame();}
        Frame active()throws Exception{
            var reply=prepare();assertEquals(Status.OK,reply.status());var descriptor=reply.frame().descriptor();assertEquals(Phase.PREPARING,reply.frame().phase());
            long deadline=System.nanoTime()+3_000_000_000L;Frame frame;
            do{frame=read();assertEquals(descriptor,frame.descriptor());assertTrue(System.nanoTime()-deadline<0);if(frame.phase()==Phase.PREPARING)Thread.sleep(2);}while(frame.phase()==Phase.PREPARING);
            assertEquals(Phase.LOADING,frame.phase(),"abort="+frame.abortReason());
            for(var p:List.of(a,b))assertEquals(Status.OK,service.handle(p.owner(),new Request(p.scope(),Action.LOADED,0,0,0,frame.matchId(),frame.generation(),0,0,0,frame.manifest().digest())).status());
            do{frame=read();assertEquals(descriptor,frame.descriptor());assertTrue(System.nanoTime()-deadline<0);if(frame.phase()!=Phase.ACTIVE)Thread.sleep(2);}while(frame.phase()!=Phase.ACTIVE);return frame;
        }
        void leave(Frame frame){for(var p:List.of(a,b))assertEquals(Status.OK,service.handle(p.owner(),new Request(p.scope(),Action.LEAVE,0,0,0,frame.matchId(),frame.generation(),0,0,0,ZERO)).status());}
        ModernShopAuthority.Snapshot shop(int source){return ModernShopAuthorityTest.read(shops.get(source),catalogs.get(source));}
        public void close()throws Exception{service.close();assertTrue(service.awaitTermination(4000));for(var p:List.of(a,b)){rooms.rooms.disconnect(p.owner());rooms.presence.disconnect(p.owner());}assertEquals(0,rooms.rooms.memberCount());assertTrue(service.isQuiescent());}
    }
    @Test void oneLegacyHumanRejectsSceneBeforeReadsCaptureJournalOrLoadingButCompatibleCombatStillWorks()throws Exception{
        try(var f=new Fixture(14)){
            var reply=f.prepare();assertEquals(Status.UNAVAILABLE,reply.status());assertEquals(Reason.INVALID_STATE,reply.reason());assertNull(reply.frame());
            assertEquals(0,f.reads.get());assertEquals(0,f.captures.get());assertEquals(0,f.journal.reservations);assertFalse(f.rooms.rooms.isPreparing(f.a.owner()));
            assertEquals(19920,f.shop(7).balance());f.configure(509);var active=f.active();
            assertEquals(ModernMatchDescriptor.Ruleset.DEATHMATCH_ELIMINATION,active.descriptor().ruleset());assertEquals(9,active.descriptor().features());
            f.leave(active);assertEquals(1,f.journal.reservations);assertEquals(0,f.journal.records);assertEquals(2,f.terminals.get());
        }
    }
    @Test void sceneFightSceneKeepExactDescriptorAndReserveResetSettleOnlyTheItemMatch()throws Exception{
        try(var f=new Fixture(15)){
            var before=f.shop(7);var scene=f.active();assertEquals(0,scene.descriptor().features());assertEquals(1,scene.descriptor().countdownNanos());
            var noCombat=f.service.combat(f.a.owner(),new ModernCombatProtocol.Request(f.a.scope(),ModernCombatProtocol.Action.READ,scene.matchId(),scene.generation(),0,0));
            assertEquals(ModernCombatProtocol.Status.UNAVAILABLE,noCombat.status());assertNull(noCombat.frame());
            f.leave(scene);assertEquals(before,f.shop(7));assertEquals(0,f.captures.get());assertEquals(0,f.terminals.get());assertEquals(0,f.journal.reservations);
            f.configure(509);var fight=f.active();assertTrue(fight.matchId()>scene.matchId());assertNotEquals(scene.generation(),fight.generation());
            assertEquals(2,f.captures.get());assertEquals(1,f.journal.reservations);
            var used=f.service.item(f.a.owner(),new ModernItemProtocol.Request(f.a.scope(),ModernItemProtocol.Action.USE,fight.matchId(),fight.generation(),1,40010));
            assertEquals(ModernItemProtocol.Status.OK,used.status());assertEquals(1,used.frame().items().get(0).stocks().get(0).remaining());
            ModernCombatProtocol.Reply hit=null;long deadline=System.nanoTime()+2_000_000_000L;
            for(long sequence=1;sequence<=100;sequence++){hit=f.service.combat(f.a.owner(),new ModernCombatProtocol.Request(f.a.scope(),ModernCombatProtocol.Action.ATTACK,fight.matchId(),fight.generation(),sequence,2));
                if(hit.frame().world().phase()==Phase.FINISHED)break;assertTrue(System.nanoTime()-deadline<0);Thread.sleep(2);}
            assertNotNull(hit);assertEquals(Phase.FINISHED,hit.frame().world().phase());assertEquals(fight.descriptor(),hit.frame().world().descriptor());
            assertEquals(1,f.journal.records);assertEquals(2,f.journal.event.participants().size());assertEquals(List.of(7,42),f.journal.event.participants().stream().map(p->p.identity().sourceId()).toList());
            var terminal=f.read();assertEquals(SettlementState.SAVED,terminal.completion().state());f.leave(terminal);
            var reset=f.shop(7);assertEquals(before.roundId()+1,reset.roundId());assertEquals(20000,reset.balance());assertTrue(reset.stacks().isEmpty());assertEquals(2,f.terminals.get());
            f.configure(514);var again=f.active();assertEquals(scene.descriptor(),again.descriptor());f.leave(again);
            assertEquals(reset,f.shop(7));assertEquals(1,f.journal.reservations);assertEquals(1,f.journal.records);assertEquals(2,f.captures.get());assertEquals(2,f.terminals.get());
        }
    }
}
