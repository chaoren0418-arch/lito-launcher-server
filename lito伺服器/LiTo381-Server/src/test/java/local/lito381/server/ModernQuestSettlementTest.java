package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernMatchSettlement.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/** Real engine victories, not fabricated client FINISHED or empty-roster wins.
 * Persistence qualification only: the v20 service must still refuse objectives. */
class ModernQuestSettlementTest {
    @TempDir Path directory;
    static List<Event> victories()throws Exception {
        var events=new ArrayList<Event>();int winner=0;
        for(int map:List.of(554,513,527)) {
            var world=ModernQuestObjectivesTest.finish(map,winner);
            assertTrue(world.vitals().stream().allMatch(v->v.hp()>0));
            assertEquals(2,world.result().mode());assertEquals(map,world.result().mapId());
            assertEquals(world.revision(),world.result().engineRevision());
            assertEquals(winner,world.quest().progress().winningLane());
            events.add(world.result());winner=1-winner;
        }
        return List.copyOf(events);
    }
    static long deadline(){return System.nanoTime()+TimeUnit.SECONDS.toNanos(10);}
    @Test void allThreeActualObjectiveFlowsRoundTripTheDurableOutboxAndRejectResultIdMutation()throws Exception {
        var events=victories();byte[] binding=new byte[32];
        try(var box=new ModernMatchOutbox(directory,binding)) {
            for(var e:events){box.record(e,deadline());box.record(e,deadline());}
            assertEquals(new HashSet<>(events),new HashSet<>(box.pending(deadline())));assertEquals(3,box.size());
            var r=events.get(0);var changed=new Event(r.resultId(),r.matchId(),r.generation(),r.engineRevision(),r.mode(),r.mapId(),r.finishedAtMillis()+1,r.participants());
            assertEquals(ModernMatchOutbox.Failure.CONFLICT,assertThrows(ModernMatchOutbox.OutboxException.class,()->box.record(changed,deadline())).failure());
        }
        try(var reopened=new ModernMatchOutbox(directory,binding)) {
            assertEquals(new HashSet<>(events),new HashSet<>(reopened.pending(deadline())));
            for(var event:events)assertArrayEquals(ModernMatchSettlementJdbc.canonical(event),
                    ModernMatchSettlementJdbc.canonical(reopened.pending(deadline()).stream().filter(e->e.resultId().equals(event.resultId())).findFirst().orElseThrow()));
        }
    }
    @Test @Timeout(value=150,unit=TimeUnit.SECONDS)
    void actualQuestVictoriesPersistSourceQualifiedTotalsAndRestartReplayDoesNotDoubleCount()throws Exception {
        var configured=System.getenv(ModernSettlementMysqlFixture.ENV);
        Assumptions.assumeTrue(configured!=null&&!configured.isBlank(),"explicit isolated MySQL home required; native service is never used");
        var events=victories();byte[] binding=new byte[32];
        try(var mysql=ModernSettlementMysqlFixture.open(Path.of(configured))) {
            assertNotEquals(3306,mysql.port());mysql.verifyInnoDb();
            try(var box=new ModernMatchOutbox(directory,binding)) {
                for(var event:events) {
                    box.record(event,deadline());mysql.store().persist(event,deadline());mysql.store().persist(event,deadline());
                    // Deliberately do not acknowledge: model an interrupted sender
                    // after a real COMMIT, leaving the original durable event.
                }
            }
            assertArrayEquals(new long[]{3,6,2},mysql.counts());
            assertArrayEquals(new long[]{2,1,3},mysql.total(7,1));
            assertArrayEquals(new long[]{1,2,3},mysql.total(42,1)); // same objid, distinct source
            mysql.restart();assertEquals(2,mysql.starts());assertFalse(mysql.forcedTermination());
            var box=new ModernMatchOutbox(directory,binding);
            assertEquals(new HashSet<>(events),new HashSet<>(box.pending(deadline())));
            var coordinator=new ModernSettlementCoordinator(box,mysql.store(),2_000,null);
            try {
                long until=deadline();
                while(box.size()!=0){assertTrue(System.nanoTime()<until,"bounded exact-result replay");Thread.sleep(5);}
                assertTrue(coordinator.accepting());
            } finally {
                coordinator.close();assertTrue(coordinator.awaitTermination(10_000));assertTrue(coordinator.isQuiescent());
            }
            assertArrayEquals(new long[]{3,6,2},mysql.counts());
            assertArrayEquals(new long[]{2,1,3},mysql.total(7,1));assertArrayEquals(new long[]{1,2,3},mysql.total(42,1));
            assertEquals(0,mysql.activeConnections());
            try(var empty=new ModernMatchOutbox(directory,binding)){assertEquals(List.of(),empty.pending(deadline()));}
            mysql.successful();
        }
    }
}
