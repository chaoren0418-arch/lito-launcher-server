package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/** Actual TLS -> selected principal -> bounded JDBC -> isolated central MySQL.
 * Synthetic map provider, no claim of graphical or complete battle acceptance. */
class ModernMatchmakingRealMysqlTest {
    @TempDir Path temp;
    @BeforeAll static void certificates()throws Exception{ModernMatchmakingNavigationTest.certificates();}
    @Test @Timeout(value=90,unit=TimeUnit.SECONDS)
    void sourceQualifiedDurableTotalsDriveAdmissionsAndSqlFailureNeverBecomesZeroHistory()throws Exception{
        String configured=System.getenv(ModernSettlementMysqlFixture.ENV);
        Assumptions.assumeTrue(configured!=null&&!configured.isBlank(),"explicit isolated MySQL integration only");
        try(var sql=ModernSettlementMysqlFixture.open(Path.of(configured))){
            assertNotEquals(3306,sql.port());sql.verifyInnoDb();
            sql.store().persist(ModernMatchSettlementTest.event(),ModernSettlementMysqlFixture.deadline(10));
            assertArrayEquals(new long[]{1,0,1},sql.total(7,3333));assertArrayEquals(new long[]{0,1,1},sql.total(42,3333));
            var jdbc=ModernCharacterRecordRepository.jdbc(sql::openConnection,ModernSettlementMysqlFixture.DATABASE);var reads=new AtomicInteger();
            ModernCharacterRecordRepository checked=(p,s,o)->{
                ModernMatchmakingNavigationTest.principal(p,s,o);var result=jdbc.character(p,s,o);reads.incrementAndGet();
                assertEquals(new ModernCharacterRecord.Snapshot(new TournamentPresence.CharacterKey(s,o),true,s==7?1:0,s==7?0:1,1),result);
                assertEquals(0,sql.activeConnections());return result;
            };
            try(var services=new ModernMatchmakingNavigationTest.Services(temp.resolve("qualified"));
                    var first=new ModernMatchmakingNavigationTest.Peer(services,checked,3000);
                    var second=new ModernMatchmakingNavigationTest.Peer(services,checked,3000);
                    var a=new ModernMatchmakingNavigationTest.Client(first,34,ModernMatchmakingNavigationTest.CAPS);
                    var b=new ModernMatchmakingNavigationTest.Client(second,34,ModernMatchmakingNavigationTest.CAPS)){
                a.login();b.login();a.enter(7,1);b.enter(42,1);
                var at=a.start().result();var bt=b.start().result();assertEquals(2,reads.get());
                assertEquals(ModernMatchmaking.State.WAITING,at.state());assertEquals(ModernMatchmaking.State.WAITING,bt.state());
                var matched=a.read(at.ticketId()).result();assertEquals(ModernMatchmaking.State.MATCHED,matched.state());
                assertEquals(matched.rooms().view().joined(),b.read(bt.ticketId()).result().rooms().view().joined());
                assertEquals(ModernMatchmaking.State.MATCHED,a.cancel(at.ticketId()).result().state());assertEquals(2,reads.get());
                a.close();first.success();b.close();second.success();
            }
            // Read-only query against a nonexistent schema on this OWNED server;
            // a real Connector/J failure must not admit an invented 0/0 player.
            var missing=ModernCharacterRecordRepository.jdbc(sql::openConnection,"missing_matchmaking_fixture");
            try(var services=new ModernMatchmakingNavigationTest.Services(temp.resolve("unavailable"));
                    var peer=new ModernMatchmakingNavigationTest.Peer(services,missing,3000);
                    var client=new ModernMatchmakingNavigationTest.Client(peer,34,ModernMatchmakingNavigationTest.CAPS)){
                client.login();client.enter(7,1);var result=client.start();assertEquals(ModernMatchmakingProtocol.Status.UNAVAILABLE,result.status());
                assertNull(result.result());assertEquals(0,services.rooms.queuedMatchmakingCount());
                assertEquals(ModernMatchmaking.State.IDLE,client.read(0).result().state());client.close();peer.success();
            }
            assertEquals(0,sql.activeConnections());assertArrayEquals(new long[]{1,2,2},sql.counts());
            assertArrayEquals(new long[]{1,0,1},sql.total(7,3333));assertArrayEquals(new long[]{0,1,1},sql.total(42,3333));
            sql.successful();
        }
    }
}
