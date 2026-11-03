package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ModernDisintegrateServiceTest {
    static final String ZERO="0".repeat(64);
    static class Fixture implements AutoCloseable {
        final ModernRoomsTest.Fixture rooms=new ModernRoomsTest.Fixture();
        final ModernRoomsTest.Player a=rooms.player(7,3333,80,0),b=rooms.player(42,3333,80,0);
        final List<ModernRoomsTest.Player> players=List.of(a,b);
        final ModernWorldService service;
        final AtomicInteger physicalReads=new AtomicInteger(),skill77Reads=new AtomicInteger();
        Fixture(boolean bothCap,boolean wrongSource,boolean unavailable)throws Exception {
            var map=ModernWorldMap.fromSectorBytes(514,Map.of(new ModernWorldMap.SectorKey(0x8000,0x8000),ModernWorldMapTest.openSectorBytes()),()->false);
            var binding=new ModernWorldBinding(1,514,map);
            var mapping=new ModernWorldService.Mapping(binding,new ModernWorldMap.Cell(32778,32778),200_000_000,1_000_000_000,700_000_000,true,false,true,true,false,false,true,null,false).withDisintegrate(true);
            service=new ModernWorldService(rooms.rooms,Map.of(binding.key(),mapping),
                    new ModernWorldService.Limits(2,1,1,2000,5,new ModernBattleEngine.Limits(2000,0,10000)),bound->0,null,Map.of(7,100.0,42,100.0));
            ModernRoomsTest.accepted(rooms.create(a,ModernRoomsTest.draft(2,1,514,0)));ModernRoomsTest.accepted(rooms.join(b,rooms.view(a).joined().summary(),""));
            for(int index=0;index<2;index++) {
                var p=players.get(index);var selected=rooms.presence.selected(p.owner());
                var row=new ModernApplicationProtocol.CharacterRow(3333,selected.characterName(),selected.characterClass(),selected.sex(),selected.level());
                var fighter=ModernPhysicalCombatTest.fighter(ModernDisintegrateTestPeer.lawfulSource(ModernBattleEngineHonorTest.source(p.scope().sourceId(),row,10000,1000)));
                var skills=new ModernSkills.Snapshot(p.scope().sourceId(),row,ModernBattleEngineBotTest.learned());boolean enabled=index==0||bothCap;
                var peer=new ModernSessionBoundary.Negotiated(1,enabled?27:26,1,(ModernSessionProtocol.KNOWN_CAPABILITIES&~(ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_NPC_FIRE_STORM))&~(ModernSessionProtocol.CAP_NPC|(enabled?0:ModernSessionProtocol.CAP_DISINTEGRATE)));
                service.register(p.owner(),p.scope(),deadline->{throw new AssertionError("physical bundle required");},deadline->{physicalReads.incrementAndGet();return fighter;},true,
                    deadline->skills,true,new ModernWorldService.MatchItems(){
                        public List<ModernShopAuthority.Stack> capture(long match,long generation,long deadline){throw new AssertionError();}
                        public List<ModernShopAuthority.Stack> capture(long match,long generation,long deadline,ModernInventoryWeight.Basis basis){assertNotNull(basis);return List.of();}
                        public void terminal(long match,long generation){}
                    },true,false,false,true,false,false,peer,enabled?deadline->{
                        skill77Reads.incrementAndGet();assertTrue(System.nanoTime()<deadline);
                        if(unavailable)throw new java.io.IOException("test source unavailable");
                        return new ModernDisintegrateCatalog.Snapshot(wrongSource?new TournamentPresence.CharacterKey(99,3333):fighter.key(),ModernDisintegrateCatalog.Status.QUALIFIED);
                    }:null);
                ModernRoomsTest.accepted(rooms.act(p,ModernRooms.Action.READY,1));
            }
        }
        ModernWorldProtocol.Reply prepare(){var room=rooms.view(a).joined().summary();return service.handle(a.owner(),new ModernWorldProtocol.Request(a.scope(),ModernWorldProtocol.Action.PREPARE,0,room.id(),room.revision(),0,0,0,0,0,ZERO));}
        ModernWorldProtocol.Frame read(){return service.handle(a.owner(),new ModernWorldProtocol.Request(a.scope(),ModernWorldProtocol.Action.READ,0,0,0,0,0,0,0,0,ZERO)).frame();}
        ModernWorldProtocol.Frame waitPrepared()throws Exception {
            assertEquals(ModernWorldProtocol.Status.OK,prepare().status());long end=System.nanoTime()+3_000_000_000L;
            while(true){var w=read();if(w.phase()!=ModernWorldProtocol.Phase.PREPARING)return w;assertTrue(System.nanoTime()<end);Thread.sleep(2);}
        }
        ModernWorldProtocol.Frame active()throws Exception {
            var w=waitPrepared();assertEquals(ModernWorldProtocol.Phase.LOADING,w.phase(),"abort="+w.abortReason());
            for(var p:players)service.handle(p.owner(),new ModernWorldProtocol.Request(p.scope(),ModernWorldProtocol.Action.LOADED,0,0,0,w.matchId(),w.generation(),0,0,0,w.manifest().digest()));
            var active=read();assertEquals(ModernWorldProtocol.Phase.ACTIVE,active.phase());return active;
        }
        ModernDisintegrateState.Request request(ModernWorldProtocol.Frame w,long sequence){return new ModernDisintegrateState.Request(a.scope(),w.matchId(),w.generation(),sequence,1,2,1);}
        public void close()throws Exception {
            service.close();assertTrue(service.awaitTermination(4000));assertTrue(service.isQuiescent());
            for(var p:players){rooms.rooms.disconnect(p.owner());rooms.presence.disconnect(p.owner());}
            assertEquals(0,rooms.rooms.memberCount());assertEquals(0,rooms.presence.ownerCount());
        }
    }
    @Test void allHumansMustNegotiateBeforeAnySourceRead()throws Exception {
        try(var f=new Fixture(false,false,false)){
            assertEquals(ModernWorldProtocol.Status.UNAVAILABLE,f.prepare().status());assertEquals(0,f.physicalReads.get());assertEquals(0,f.skill77Reads.get());assertFalse(f.rooms.rooms.isPreparing(f.a.owner()));
        }
    }
    @Test void actualPreparationReadsEachSourceOnceThenTypedDispatchConsumesNoAdditionalSql()throws Exception {
        try(var f=new Fixture(true,false,false)){
            var w=f.active();assertTrue(w.descriptor().has(ModernMatchDescriptor.DISINTEGRATE));assertEquals(2,f.skill77Reads.get());
            var r=f.request(w,1);var reply=f.service.disintegrate(f.a.owner(),r);
            assertEquals(ModernSpellProtocol.Status.OK,reply.status());assertEquals(ModernDisintegrateState.Reason.NONE,reply.reason());
            assertEquals(1,reply.frame().disintegrations().get(0).castCount());assertEquals(501,reply.frame().disintegrations().get(0).lawful());
            var message=new ModernApplicationProtocol.DisintegrateResult(1,r,reply);assertEquals(message,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(message,27),27));
            var denied=f.service.disintegrate(f.a.owner(),f.request(w,2));assertEquals(ModernDisintegrateState.Reason.TOO_FAST,denied.reason());assertEquals(reply.frame().disintegrations(),denied.frame().disintegrations());
            assertEquals(2,f.skill77Reads.get());assertThrows(SecurityException.class,()->f.service.disintegrate(f.a.owner(),r));
        }
    }
    @Test void wrongSourceOrFailedQualificationAbortsRatherThanStartingWithDefaultPermission()throws Exception {
        for(boolean wrong:new boolean[]{true,false})try(var f=new Fixture(true,wrong,!wrong)){
            var w=f.waitPrepared();assertEquals(ModernWorldProtocol.Phase.ABORTED,w.phase());assertTrue(f.skill77Reads.get()>0);assertFalse(f.rooms.rooms.isPreparing(f.a.owner()));
        }
    }
    @Test void dispatchRejectsWrongOwnerScopeGenerationAndDetachedRoom()throws Exception {
        try(var f=new Fixture(true,false,false)){
            var w=f.active();var good=f.request(w,1);
            assertThrows(SecurityException.class,()->f.service.disintegrate(f.b.owner(),good));
            for(var scope:List.of(new ModernRooms.Scope(42,3333,1,0),new ModernRooms.Scope(7,3334,1,0),new ModernRooms.Scope(7,3333,2,0),new ModernRooms.Scope(7,3333,1,1))) {
                var bad=new ModernDisintegrateState.Request(scope,w.matchId(),w.generation(),1,1,2,1);
                assertThrows(SecurityException.class,()->f.service.disintegrate(f.a.owner(),bad));
            }
            var bad=new ModernDisintegrateState.Request(f.a.scope(),w.matchId(),w.generation()+1,1,1,2,1);assertThrows(SecurityException.class,()->f.service.disintegrate(f.a.owner(),bad));
            f.service.detach(f.a.owner());assertThrows(SecurityException.class,()->f.service.disintegrate(f.a.owner(),good));assertEquals(2,f.skill77Reads.get());
        }
    }
}
