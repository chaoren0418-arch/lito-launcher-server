package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class ModernSlowSqlTestPeerTest {
    @Test void interactiveWorldActuallyCoversTheOperatorDeadlineWithoutChangingHeadlessRules() {
        assertEquals(120000L,ModernSlowSqlTestPeer.worldTimeoutMillis("slow-sql"));
        assertEquals(600000L,ModernSlowSqlTestPeer.worldTimeoutMillis("slow-window"));
        assertEquals(600000L,ModernSlowSqlTestPeer.worldTimeoutMillis("slow-healing-window"));
        assertEquals(1_800_000L,ModernSlowSqlTestPeer.worldTimeoutMillis("slow-healing-dev"));
        assertTrue(ModernSlowSqlTestPeer.worldTimeoutMillis("slow-window")>540000L);
        for(var mode:new String[]{"","Slow-window","window","slow-sql-window"})
            assertThrows(IllegalArgumentException.class,()->ModernSlowSqlTestPeer.worldTimeoutMillis(mode));
    }
    @Test void headlessLifetimeKeepsItsOriginalBounds() {
        assertEquals(5000,ModernSlowSqlTestPeer.lifetimeMillis("slow-sql","5000"));
        assertEquals(180000,ModernSlowSqlTestPeer.lifetimeMillis("slow-sql","180000"));
        for(var value:new String[]{"4999","180001","600000"})
            assertThrows(IllegalArgumentException.class,()->ModernSlowSqlTestPeer.lifetimeMillis("slow-sql",value));
    }
    @Test void explicitInteractiveLifetimeIsBounded() {
        assertEquals(30000,ModernSlowSqlTestPeer.lifetimeMillis("slow-window","30000"));
        assertEquals(600000,ModernSlowSqlTestPeer.lifetimeMillis("slow-window","600000"));
        assertEquals(600000,ModernSlowSqlTestPeer.lifetimeMillis("slow-healing-window","600000"));
        assertEquals(43_200_000,ModernSlowSqlTestPeer.lifetimeMillis("slow-healing-dev","43200000"));
        for(var value:new String[]{"29999","600001","2147483647"})
            assertThrows(IllegalArgumentException.class,()->ModernSlowSqlTestPeer.lifetimeMillis("slow-window",value));
        for(var value:new String[]{"59999","43200001"})
            assertThrows(IllegalArgumentException.class,()->ModernSlowSqlTestPeer.lifetimeMillis("slow-healing-dev",value));
    }
    @Test void unknownModesCannotRequestAnOperatorLifetime() {
        for(var mode:new String[]{"","Slow-window","window","slow-sql-window"})
            assertThrows(IllegalArgumentException.class,()->ModernSlowSqlTestPeer.lifetimeMillis(mode,"600000"));
    }
    @Test void malformedOrOverflowingDurationsNeverStartAPeer() {
        for(var mode:new String[]{"slow-sql","slow-window","slow-healing-window","slow-healing-dev"})
            for(var value:new String[]{"","-1","abc","2147483648","600000ms"})
                assertThrows(IllegalArgumentException.class,()->ModernSlowSqlTestPeer.lifetimeMillis(mode,value));
    }
    @Test @Timeout(value=120,unit=TimeUnit.SECONDS)
    void r383CombinedOperatorFixtureBootsOwnedMainWithoutChangingSources()throws Exception {
        var mysql=System.getenv(ModernSettlementMysqlFixture.ENV);
        var shared=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(mysql!=null&&shared!=null);
        var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
        try(var f=ModernQuestSqlFixture.openSlowPlayerHealingWindow(Path.of(mysql),Path.of(shared),tls)) {
            var original=ModernSlowMainSqlTest.sourceRows(f);
            assertTrue(f.config.worlds().slowEnabled());
            assertTrue(f.config.worlds().playerHealingEnabled());
            var server=ModernMain.start(f.config);
            try{assertEquals(0,server.snapshot().accepted());}
            finally{server.close();assertTrue(server.awaitTermination(5000));}
            assertEquals(original,ModernSlowMainSqlTest.sourceRows(f));
            assertArrayEquals(new long[]{0,0,0},f.mysql.counts());
            assertEquals(0,f.mysql.botCount());
            assertEquals(0,f.mysql.activeConnections());
            assertFalse(f.mysql.forcedTermination());f.successful();
        }
    }
}
