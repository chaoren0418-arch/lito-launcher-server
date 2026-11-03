package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Real service scheduler, explicit parsed shared policy, typed request and
 * durable-publication seam; not a native HWND or Main/TLS integration test. */
class ModernTimeAttackServiceTest {
    @TempDir Path temporary;
    static class Journal implements ModernMatchSettlement.JournaledStore {
        final AtomicInteger reserved=new AtomicInteger(),released=new AtomicInteger(),records=new AtomicInteger();
        volatile ModernMatchSettlement.Event event;
        volatile ModernMatchSettlement.ReceiptState state=ModernMatchSettlement.ReceiptState.PENDING;
        volatile boolean closed;ModernMatchSettlement.Store recording=(e,d)->{};
        @Override public ModernMatchSettlement.Reservation reserve(long match,long generation) {
            reserved.incrementAndGet();return new ModernMatchSettlement.Reservation() {
                boolean freed;
                @Override public void record(ModernMatchSettlement.Event e,long deadline)throws Exception {
                    assertEquals(match,e.matchId());assertEquals(generation,e.generation());assertTrue(deadline>System.nanoTime());
                    records.incrementAndGet();recording.persist(e,deadline);event=e;
                }
                @Override public ModernMatchSettlement.ReceiptState state(){return state;}
                @Override public void close(){if(!freed){freed=true;released.incrementAndGet();}}
            };
        }
        @Override public boolean accepting(){return !closed;}
        @Override public void persist(ModernMatchSettlement.Event e,long deadline){fail("legacy writer cannot receive cooperative result");}
        @Override public void close(){closed=true;}
        @Override public boolean awaitTermination(long timeout){return closed;}
        @Override public boolean isQuiescent(){return closed;}
    }
    static final class Fixture implements AutoCloseable {
        final TournamentPresence presence=new TournamentPresence();final ModernRooms rooms;
        final List<ModernBattleEngineBotTest.Player> players=new ArrayList<>();
        final ModernWorldService.Mapping mapping;final ModernWorldService service;final Journal journal;
        final AtomicInteger physicalReads=new AtomicInteger();
        Fixture(Path root,int humans,Journal journal)throws Exception {this(root,humans,journal,20000);}
        Fixture(Path root,int humans,Journal journal,int duration)throws Exception {
            this.journal=journal;var properties=ModernTimeAttackConfigurationTest.world(root);
            properties.setProperty(ModernTimeAttackConfigurationTest.PREFIX+"timeAttackDurationMillis",Integer.toString(duration));
            mapping=ModernTimeAttackConfigurationTest.mapping(root,properties);
            rooms=new ModernRooms(Set.of(mapping.binding().key()),Set.of(),Map.of(mapping.binding().key(),60));
            service=new ModernWorldService(rooms,Map.of(mapping.binding().key(),mapping),new ModernWorldService.Limits(2,1,1,2000,1000,
                    new ModernBattleEngine.Limits(2000,0,60000)),bound->Math.min(19,bound-1),journal);
            for(int i=0;i<humans;i++) {
                int source=i==0?7:42;var owner=presence.connect();var row=new ModernApplicationProtocol.CharacterRow(3333,"player-"+i,1,0,80);
                var selection=new TournamentPresence.Selection(new TournamentPresence.CharacterKey(source,3333),"server-"+source,row.name(),80,1,0);
                assertEquals(TournamentPresence.SelectStatus.ACCEPTED,presence.select(owner,selection));rooms.enterLobby(owner,selection,1,0,true);
                var p=new ModernBattleEngineBotTest.Player(owner,new ModernRooms.Scope(source,3333,1,0),row);players.add(p);
                if(i==0)command(p,ModernRooms.Action.CREATE,0,0,0,ModernRoomsTest.draft(60,4,540,0));
                else {var s=room(players.get(0)).summary();command(p,ModernRooms.Action.JOIN,s.id(),s.revision(),0,null);}
                register(p,25,null);
            }
            for(var p:players){var s=room(p).summary();command(p,ModernRooms.Action.READY,s.id(),s.revision(),1,null);}
        }
        void register(ModernBattleEngineBotTest.Player p,int version,ModernWorldService.PhysicalReader reader) {
            service.register(p.owner(),p.scope(),deadline->{fail("combat cannot use roster stats fallback");return null;},reader==null?deadline->{physicalReads.incrementAndGet();
                return ModernPhysicalCombatTest.fighter(ModernPhysicalCombatTest.fixtureSource(p.scope().sourceId(),p.row(),ModernNpcServiceTest.HUGE,10,1));}:reader,
                    true,null,false,null,false,false,false,false,false,false,new ModernSessionBoundary.Negotiated(1,version,1,(ModernSessionProtocol.KNOWN_CAPABILITIES&~(ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_NPC_FIRE_STORM))));
        }
        ModernRooms.RoomSnapshot room(ModernBattleEngineBotTest.Player p){return rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),ModernRooms.Action.READ,0,0,0,null,"")).view().joined();}
        void command(ModernBattleEngineBotTest.Player p,ModernRooms.Action action,long room,long rev,int value,ModernRooms.Draft draft) {
            ModernRoomsTest.accepted(rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),action,room,rev,value,draft,"")));
        }
        ModernWorldProtocol.Reply prepare() {var p=players.get(0);var s=room(p).summary();return service.handle(p.owner(),new ModernWorldProtocol.Request(p.scope(),ModernWorldProtocol.Action.PREPARE,0,s.id(),s.revision(),0,0,0,0,0,ModernWorldProtocol.ZERO_DIGEST));}
        ModernWorldProtocol.Frame world(ModernBattleEngineBotTest.Player p){return service.handle(p.owner(),new ModernWorldProtocol.Request(p.scope(),ModernWorldProtocol.Action.READ,0,0,0,0,0,0,0,0,ModernWorldProtocol.ZERO_DIGEST)).frame();}
        ModernNpcProtocol.Request request(ModernBattleEngineBotTest.Player p,ModernWorldProtocol.Frame w,ModernNpcProtocol.Action action,long seq,long serial,long life) {
            return new ModernNpcProtocol.Request(p.scope(),action,new ModernNpcRoster.Scope(w.room().summary().id(),w.matchId(),w.generation()),seq,serial,life);
        }
        ModernNpcProtocol.Reply send(ModernBattleEngineBotTest.Player p,ModernNpcProtocol.Request request)throws Exception {
            var reply=service.npc(p.owner(),request);var message=new ModernApplicationProtocol.NpcResult(71,request,reply);
            ModernNpcServiceTest.wireVersions(message,25);return reply;
        }
        ModernNpcProtocol.Frame read(ModernBattleEngineBotTest.Player p,ModernWorldProtocol.Frame w)throws Exception{return send(p,request(p,w,ModernNpcProtocol.Action.READ,0,0,0)).frame();}
        ModernWorldProtocol.Frame loading()throws Exception {
            var reply=prepare();assertEquals(ModernWorldProtocol.Status.OK,reply.status());assertEquals(ModernWorldProtocol.Phase.PREPARING,reply.frame().phase());
            assertEquals(0,reply.frame().timeAttack().revision());assertEquals(1,journal.reserved.get());
            var p=players.get(0);ModernNpcServiceTest.await(()->world(p).phase()!=ModernWorldProtocol.Phase.PREPARING);
            var w=world(p);assertEquals(ModernWorldProtocol.Phase.LOADING,w.phase());return w;
        }
        ModernWorldProtocol.Frame active()throws Exception {
            var w=loading();assertTrue(read(players.get(0),w).actors().isEmpty());
            for(var p:players)assertEquals(ModernWorldProtocol.Status.OK,service.handle(p.owner(),new ModernWorldProtocol.Request(p.scope(),ModernWorldProtocol.Action.LOADED,0,0,0,w.matchId(),w.generation(),0,0,0,w.manifest().digest())).status());
            w=world(players.get(0));assertEquals(ModernWorldProtocol.Phase.ACTIVE,w.phase());assertEquals(-1,w.timeAttack().startedMillis());return w;
        }
        ModernNpcProtocol.Reply tower(ModernBattleEngineBotTest.Player p,ModernWorldProtocol.Frame w)throws Exception{return send(p,request(p,w,ModernNpcProtocol.Action.ATTACK_TOWER,1,0,0));}
        ModernNpcProtocol.Reply hit(ModernBattleEngineBotTest.Player p,ModernWorldProtocol.Frame w,long sequence)throws Exception {
            var n=read(p,w);var actor=n.actors().stream().filter(a->a.hp()>0).findFirst().orElseThrow();Thread.sleep(3);
            var reply=send(p,request(p,w,ModernNpcProtocol.Action.ATTACK,sequence,actor.serial(),actor.life()));assertEquals(ModernNpcProtocol.Status.OK,reply.status());return reply;
        }
        ModernWorldProtocol.Reply leave(ModernBattleEngineBotTest.Player p,ModernWorldProtocol.Frame w){return service.handle(p.owner(),new ModernWorldProtocol.Request(p.scope(),ModernWorldProtocol.Action.LEAVE,0,0,0,w.matchId(),w.generation(),0,0,0,ModernWorldProtocol.ZERO_DIGEST));}
        @Override public void close()throws Exception {service.close();assertTrue(service.awaitTermination(5000));for(var p:players){rooms.disconnect(p.owner());presence.disconnect(p.owner());}}
    }
    @Test void soloRunsPreparingLoadedTowerAllWavesReceiptAndNormalReturn()throws Exception {
        var journal=new Journal();try(var f=new Fixture(temporary,1,journal)) {
            var p=f.players.get(0);var w=f.active();assertEquals(ModernNpcServiceTest.HUGE,f.read(p,w).combat().vitals().get(0).hp());
            assertEquals(ModernNpcProtocol.Status.OK,f.tower(p,w).status());assertEquals(0,f.read(p,w).combat().world().timeAttack().towerHp());
            ModernNpcProtocol.Reply last=null;for(int i=0;i<3;i++)last=f.hit(p,w,i+2);
            var result=last.frame().combat().world();assertEquals(ModernWorldProtocol.Phase.FINISHED,result.phase());assertNull(result.completion());
            assertEquals(ModernTimeAttack.End.CLEARED,result.timeAttack().end());assertEquals(journal.event,result.timeAttack().event(result));assertEquals(1,journal.records.get());
            assertEquals(ModernMatchSettlement.Verdict.WIN,journal.event.participants().get(0).verdict());
            journal.state=ModernMatchSettlement.ReceiptState.SAVED;var saved=f.world(p);assertEquals(result.revision()+1,saved.revision());
            assertEquals(journal.event,saved.timeAttack().event(saved));assertEquals(ModernWorldProtocol.SettlementState.SAVED,saved.timeAttack().receipt().state());
            assertEquals(ModernWorldProtocol.Status.OK,f.leave(p,w).status());assertNull(f.world(p));assertFalse(f.rooms.isPreparing(p.owner()));
        }assertEquals(1,journal.released.get());
    }
    @Test void ownerLeavesWithoutAbortingSurvivorOrAllowingRoomLeaveBeforeEngineRetires()throws Exception {
        var journal=new Journal();try(var f=new Fixture(temporary,2,journal)) {
            var a=f.players.get(0);var b=f.players.get(1);var w=f.active();assertEquals(ModernNpcProtocol.Status.OK,f.tower(a,w).status());
            var room=f.room(a).summary();assertEquals(ModernRooms.Status.DENIED,f.rooms.execute(a.owner(),new ModernRooms.Command(a.scope(),ModernRooms.Action.LEAVE,room.id(),room.revision(),0,null,"")).status());
            assertEquals(ModernWorldProtocol.Status.OK,f.leave(a,w).status());room=f.room(a).summary();f.command(a,ModernRooms.Action.LEAVE,room.id(),room.revision(),0,null);
            var after=f.world(b);assertEquals(ModernWorldProtocol.Phase.ACTIVE,after.phase());assertEquals(2,after.timeAttack().leader());assertEquals(1,after.timeAttack().departedMask());
            assertEquals(2,after.actors().size());assertEquals(0,journal.records.get());assertThrows(SecurityException.class,()->f.read(a,w));
            for(int i=0;i<3;i++)f.hit(b,w,i+1);
            assertEquals(List.of(ModernMatchSettlement.Verdict.LOSS,ModernMatchSettlement.Verdict.WIN),journal.event.participants().stream().map(ModernMatchSettlement.Participant::verdict).toList());
        }
    }
    @Test void lastDisconnectRecordsFullFrozenPartyWithoutNeedingAnyRemainingOwner()throws Exception {
        var journal=new Journal();try(var f=new Fixture(temporary,2,journal)) {
            var a=f.players.get(0);var b=f.players.get(1);var w=f.active();f.tower(a,w);
            f.service.detach(a.owner());f.rooms.disconnect(a.owner());assertEquals(ModernWorldProtocol.Phase.ACTIVE,f.world(b).phase());
            f.service.detach(b.owner());f.rooms.disconnect(b.owner());assertEquals(1,journal.records.get());assertEquals(ModernTimeAttack.End.PARTY_DEFEATED,journal.event.timeAttack().end());
            assertEquals(2,journal.event.participants().size());assertTrue(journal.event.participants().stream().allMatch(p->p.verdict()==ModernMatchSettlement.Verdict.LOSS));
            ModernNpcServiceTest.await(()->f.service.matchCount()==0);assertEquals(0,f.service.registrationCount());
        }assertEquals(1,journal.released.get());
    }
    @Test void realTimerProducesLossAndOldProtocolCannotReadFreshCharacterData()throws Exception {
        try(var f=new Fixture(temporary.resolve("timer"),1,new Journal(),100)) {
            var p=f.players.get(0);var w=f.active();f.tower(p,w);Thread.sleep(120);var finalWorld=f.world(p);
            assertEquals(ModernTimeAttack.End.TIMEOUT,finalWorld.timeAttack().end());assertEquals(ModernMatchSettlement.Verdict.LOSS,f.journal.event.participants().get(0).verdict());
        }
        try(var f=new Fixture(temporary.resolve("old"),1,new Journal())) {
            f.register(f.players.get(0),24,null);assertEquals(ModernWorldProtocol.Status.UNAVAILABLE,f.prepare().status());assertEquals(0,f.physicalReads.get());assertEquals(0,f.journal.reserved.get());
        }
    }
    @Test void finalHitAndConcurrentReadWaitForDurableRecordThenPreserveOneReceipt()throws Exception {
        var journal=new Journal();var entered=new CountDownLatch(1);var proceed=new CountDownLatch(1);
        journal.recording=(e,d)->{entered.countDown();assertTrue(proceed.await(3,TimeUnit.SECONDS));};
        try(var f=new Fixture(temporary,1,journal)) {
            var p=f.players.get(0);var w=f.active();f.tower(p,w);f.hit(p,w,2);f.hit(p,w,3);
            var hit=new AtomicReference<ModernNpcProtocol.Reply>();var read=new AtomicReference<ModernWorldProtocol.Frame>();var failure=new AtomicReference<Throwable>();
            var attacker=new Thread(()->{try{hit.set(f.hit(p,w,4));}catch(Throwable t){failure.set(t);}});
            var reader=new Thread(()->{try{read.set(f.world(p));}catch(Throwable t){failure.set(t);}});
            try {attacker.start();assertTrue(entered.await(2,TimeUnit.SECONDS));reader.start();Thread.sleep(30);assertNull(hit.get());assertNull(read.get());
                proceed.countDown();attacker.join(3000);reader.join(3000);assertFalse(attacker.isAlive());assertFalse(reader.isAlive());assertNull(failure.get());
                assertEquals(journal.event,hit.get().frame().combat().world().timeAttack().event(hit.get().frame().combat().world()));
                assertEquals(journal.event,read.get().timeAttack().event(read.get()));assertEquals(1,journal.records.get());
            }finally{proceed.countDown();attacker.join(4000);reader.join(4000);}
        }
    }
    @Test void failedDurableRecordCannotLeakFinishedButRetiresAllOwnedWorkers()throws Exception {
        var journal=new Journal();journal.recording=(e,d)->{throw new java.io.IOException("fixture local force failure");};
        try(var f=new Fixture(temporary,1,journal)) {
            var p=f.players.get(0);var w=f.active();f.tower(p,w);f.hit(p,w,2);f.hit(p,w,3);
            assertThrows(IllegalStateException.class,()->f.hit(p,w,4));assertThrows(IllegalStateException.class,()->f.world(p));
            assertNull(journal.event);assertFalse(f.rooms.isPreparing(p.owner()));
        }assertEquals(1,journal.released.get());
    }
    @Test void preparingFailureAbortsWithValidExtensionAndNoInventedResult()throws Exception {
        var journal=new Journal();try(var f=new Fixture(temporary,1,journal)) {
            var p=f.players.get(0);f.register(p,25,deadline->{throw new java.io.IOException("fixture unavailable");});
            assertEquals(ModernWorldProtocol.Status.OK,f.prepare().status());ModernNpcServiceTest.await(()->f.world(p).phase()==ModernWorldProtocol.Phase.ABORTED);
            var w=f.world(p);assertEquals(ModernTimeAttack.Phase.ABORTED,w.timeAttack().phase());assertNull(w.timeAttack().receipt());assertEquals(0,journal.records.get());
            assertEquals(ModernWorldProtocol.Status.OK,f.leave(p,w).status());
        }assertEquals(1,journal.released.get());
    }
    @Test void cooperativeAdmissionRequiresJournalAndRejectsUnreservableCapacityBeforeReadingCharacters()throws Exception {
        var p=ModernTimeAttackConfigurationTest.world(temporary);var mapping=ModernTimeAttackConfigurationTest.mapping(temporary,p);
        var rooms=new ModernRooms(Set.of(mapping.binding().key()));var limits=new ModernWorldService.Limits(2,1,1,2000,1000,new ModernBattleEngine.Limits(2000,0,60000));
        assertThrows(IllegalArgumentException.class,()->new ModernWorldService(rooms,Map.of(mapping.binding().key(),mapping),limits));
        assertThrows(IllegalArgumentException.class,()->new ModernWorldService(rooms,Map.of(mapping.binding().key(),mapping),limits,bound->0,(e,d)->fail("not durable")));
        var badClocks=mapping.withClockOverrides(new ModernWorldService.ClockOverrides(null,null,60001));
        assertThrows(IllegalArgumentException.class,()->new ModernWorldService(rooms,Map.of(mapping.binding().key(),badClocks),limits,bound->0,new Journal()));
        var full=new Journal(){@Override public ModernMatchSettlement.Reservation reserve(long m,long g){throw new IllegalStateException("fixture full");}};
        try(var f=new Fixture(temporary.resolve("capacity"),1,full)) {
            assertEquals(ModernWorldProtocol.Status.UNAVAILABLE,f.prepare().status());assertEquals(0,f.physicalReads.get());
            assertFalse(f.rooms.isPreparing(f.players.get(0).owner()));assertEquals(0,f.service.matchCount());
        }
    }
    @Test void lastLeaveCannotAcknowledgeBeforeDurablePartyDefeatAndKeepsNoOrphan()throws Exception {
        var journal=new Journal();var entered=new CountDownLatch(1);var proceed=new CountDownLatch(1);
        journal.recording=(e,d)->{entered.countDown();assertTrue(proceed.await(3,TimeUnit.SECONDS));};
        try(var f=new Fixture(temporary,1,journal)) {
            var p=f.players.get(0);var w=f.active();var reply=new AtomicReference<ModernWorldProtocol.Reply>();var failure=new AtomicReference<Throwable>();
            var leave=new Thread(()->{try{reply.set(f.leave(p,w));}catch(Throwable t){failure.set(t);}});
            try {leave.start();assertTrue(entered.await(2,TimeUnit.SECONDS));Thread.sleep(30);assertNull(reply.get());assertNull(journal.event);
                proceed.countDown();leave.join(3000);assertFalse(leave.isAlive());assertNull(failure.get());assertEquals(ModernWorldProtocol.Status.OK,reply.get().status());
                assertEquals(ModernTimeAttack.End.PARTY_DEFEATED,journal.event.timeAttack().end());assertEquals(-1,journal.event.timeAttack().startedMillis());
                assertNull(f.world(p));ModernNpcServiceTest.await(()->f.service.matchCount()==0);
            }finally{proceed.countDown();leave.join(4000);}
        }assertEquals(1,journal.records.get());assertEquals(1,journal.released.get());
    }
}
