package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Real scheduler/admission/publication tests. The journal below probes the
 * service contract; disk + isolated SQL idempotency is tested separately. */
class ModernQuestServiceTest {
    static final class Journal implements ModernMatchSettlement.JournaledStore {
        final ModernMatchSettlement.Store writer;
        final AtomicInteger reservations=new AtomicInteger(),records=new AtomicInteger(),released=new AtomicInteger();
        volatile ModernMatchSettlement.Event event;
        volatile ModernMatchSettlement.ReceiptState state=ModernMatchSettlement.ReceiptState.PENDING;
        boolean closed;
        Journal(){this((e,d)->{});}
        Journal(ModernMatchSettlement.Store writer){this.writer=writer;}
        @Override public ModernMatchSettlement.Reservation reserve(long match,long generation) {
            reservations.incrementAndGet();
            return new ModernMatchSettlement.Reservation() {
                boolean freed;
                @Override public void record(ModernMatchSettlement.Event e,long deadline)throws Exception {
                    assertEquals(match,e.matchId());assertEquals(generation,e.generation());assertEquals(2,e.mode());
                    writer.persist(e,deadline);event=e;records.incrementAndGet();state=ModernMatchSettlement.ReceiptState.SAVED;
                }
                @Override public ModernMatchSettlement.ReceiptState state(){return state;}
                @Override public void close(){if(!freed){freed=true;released.incrementAndGet();}}
            };
        }
        @Override public boolean accepting(){return !closed;}
        @Override public void persist(ModernMatchSettlement.Event e,long d){fail("journal must precede ACK");}
        @Override public void close(){closed=true;}
        @Override public boolean awaitTermination(long millis){return closed;}
        @Override public boolean isQuiescent(){return closed;}
    }
    static ModernNpcProtocol.Request clearToLast(ModernNpcServiceTest.Fixture f)throws Exception {
        var w=f.loading();var before=f.read(f.a,w).frame().combat().world().quest();
        assertTrue(before.enabled());assertTrue(before.actors().isEmpty());assertEquals(1,before.lanes().size());
        assertTrue(before.targets().stream().allMatch(t->t.lane()==0));
        boolean symbols=BattleQuestEncounter.usesPerRoomSymbols(f.key.logicalMapId());
        assertEquals(symbols?4:1,before.targets().size());
        f.ack(f.a,w);f.ack(f.b,w);long sequence=0;
        for(int stage=1;stage<=4;stage++) {
            final int expected=stage;
            ModernNpcServiceTest.await(()->f.read(f.a,w).frame().actors().stream().anyMatch(a->a.hp()>0&&a.stage()==expected));
            var npc=f.read(f.a,w).frame().actors().stream().filter(a->a.hp()>0).findFirst().orElseThrow();
            var r=f.request(f.a,w,++sequence,npc.serial(),npc.life());
            var killed=f.service.npc(f.a.owner(),r);assertEquals(ModernNpcProtocol.Status.OK,killed.status());ModernNpcServiceTest.wire(r,killed);
            assertNull(killed.frame().combat().world().completion());Thread.sleep(4);
            if(stage==4||symbols) {
                var q=f.read(f.a,w).frame().combat().world().quest();var target=q.actors().stream().filter(a->a.hp()>0).findFirst().orElseThrow();
                var strike=new ModernNpcProtocol.Request(f.a.scope(),ModernNpcProtocol.Action.ATTACK_OBJECTIVE,r.world(),++sequence,target.slot(),0);
                if(stage==4)return strike;
                var destroyed=f.service.npc(f.a.owner(),strike);assertEquals(ModernNpcProtocol.Status.OK,destroyed.status());ModernNpcServiceTest.wire(strike,destroyed);
                assertNull(destroyed.frame().combat().world().completion());Thread.sleep(4);
            }
        }
        throw new AssertionError();
    }
    @Test void allThreeFlowsPublishSavedResultToBothIsolatedAudiencesAndCanLeaveNormally()throws Exception {
        for(int map:List.of(554,513,527)) {
            var journal=new Journal();
            try(var f=new ModernNpcServiceTest.Fixture(100,true,false,journal,map)) {
                var strike=clearToLast(f);assertNull(journal.event);
                var won=f.service.npc(f.a.owner(),strike);ModernNpcServiceTest.wire(strike,won);
                assertEquals(ModernNpcProtocol.Status.OK,won.status());assertEquals(1,journal.records.get());
                var w=won.frame().combat().world();var loss=f.read(f.b,w);ModernNpcServiceTest.wire(f.request(f.b,w,0,0,0),loss);
                for(var reply:List.of(won,loss)) {
                    var frame=reply.frame().combat().world();assertEquals(ModernWorldProtocol.Phase.FINISHED,frame.phase());
                    assertEquals(ModernWorldProtocol.SettlementState.SAVED,frame.completion().state());assertEquals(journal.event,frame.completion().event());
                    assertEquals(2,frame.completion().event().participants().size());assertEquals(1,frame.actors().size());
                    assertTrue(reply.frame().combat().vitals().stream().allMatch(v->v.hp()>0));
                }
                assertTrue(loss.frame().combat().world().quest().actors().isEmpty());assertEquals(0,loss.frame().combat().world().quest().winningLane());
                assertEquals(List.of(7,42),journal.event.participants().stream().map(p->p.key().sourceId()).toList());
                assertTrue(journal.event.participants().stream().allMatch(p->p.key().objectId()==3333));
                assertEquals(ModernNpcProtocol.Status.DENIED,f.service.npc(f.a.owner(),strike).status());assertEquals(1,journal.records.get());
                for(var p:List.of(f.a,f.b))assertNull(f.service.handle(p.owner(),new ModernWorldProtocol.Request(p.scope(),ModernWorldProtocol.Action.LEAVE,0,0,0,w.matchId(),w.generation(),0,0,0,ModernNpcServiceTest.ZERO)).frame());
                assertTrue(f.view().members().stream().noneMatch(ModernRooms.Member::ready));
            }
            assertTrue(journal.closed);assertEquals(1,journal.released.get());
        }
    }
    @Test void legacyPeerCannotReserveDurableSlotOrReadProfiles()throws Exception {
        var journal=new Journal();try(var f=new ModernNpcServiceTest.Fixture(100,true,false,journal,554)) {
            f.register(f.b,20,(ModernSessionProtocol.KNOWN_CAPABILITIES&~(ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_NPC_FIRE_STORM)),null);
            assertEquals(ModernWorldProtocol.Status.UNAVAILABLE,f.prepare().status());
            assertEquals(0,f.physicalReads.get());assertEquals(0,journal.reservations.get());
        }
    }
    @Test void lastObjectiveHitAndLosingReadWaitForJournalBeforeAnyTerminalAck()throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        var journal=new Journal((e,d)->{entered.countDown();assertTrue(release.await(3,TimeUnit.SECONDS));});
        try(var f=new ModernNpcServiceTest.Fixture(100,true,false,journal,554)) {
            var strike=clearToLast(f);var w=f.world();var hit=new AtomicReference<ModernNpcProtocol.Reply>();var read=new AtomicReference<ModernNpcProtocol.Reply>();var error=new AtomicReference<Throwable>();
            var attacker=new Thread(()->{try{hit.set(f.service.npc(f.a.owner(),strike));}catch(Throwable t){error.set(t);}});
            var reader=new Thread(()->{try{read.set(f.read(f.b,w));}catch(Throwable t){error.set(t);}});
            try {
                attacker.start();assertTrue(entered.await(1,TimeUnit.SECONDS));reader.start();Thread.sleep(30);
                assertNull(hit.get());assertNull(read.get());assertNull(journal.event);release.countDown();attacker.join(2000);reader.join(2000);
                assertFalse(attacker.isAlive());assertFalse(reader.isAlive());assertNull(error.get());
                assertEquals(journal.event,hit.get().frame().combat().world().completion().event());
                assertEquals(journal.event,read.get().frame().combat().world().completion().event());
            }finally{release.countDown();attacker.join(3000);reader.join(3000);}
        }
    }
    @Test void failedJournalCannotLeakWinAndAbortCannotFabricateResult()throws Exception {
        var journal=new Journal((e,d)->{throw new java.io.IOException("fixture force failure");});
        try(var f=new ModernNpcServiceTest.Fixture(100,true,false,journal,554)) {
            var strike=clearToLast(f);var w=f.world();assertThrows(IllegalStateException.class,()->f.service.npc(f.a.owner(),strike));
            assertThrows(IllegalStateException.class,()->f.read(f.b,w));f.service.tickNow();assertEquals(0,journal.records.get());
        }
        var unused=new Journal((e,d)->fail("abort is not a result"));
        try(var f=new ModernNpcServiceTest.Fixture(100,true,false,unused,554)) {
            var w=f.active().combat().world();f.service.handle(f.a.owner(),new ModernWorldProtocol.Request(f.a.scope(),ModernWorldProtocol.Action.LEAVE,0,0,0,w.matchId(),w.generation(),0,0,0,ModernNpcServiceTest.ZERO));
            var after=f.read(f.b,w).frame().combat().world();assertEquals(ModernWorldProtocol.Phase.ABORTED,after.phase());assertNull(after.completion());assertEquals(0,unused.records.get());
        }
    }
}
