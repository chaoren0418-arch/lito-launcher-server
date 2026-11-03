package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class ModernHasteSqlFixtureTest {
    @Test @Timeout(value=120,unit=TimeUnit.SECONDS)
    void slowQuestKeepsNpcBotsAndIndependentOldHasteRules()throws Exception {
        var mysql=System.getenv(ModernSettlementMysqlFixture.ENV);var shared=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(mysql!=null&&shared!=null);
        var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
        try(var f=ModernQuestSqlFixture.openSlowQuest(Path.of(mysql),Path.of(shared),tls)) {
            var original=ModernSlowMainSqlTest.sourceRows(f);
            assertTrue(f.config.worlds().slowEnabled());assertFalse(f.config.worlds().hasteEnabled());
            assertTrue(f.config.worlds().npcEnabled());assertTrue(f.config.worlds().questEnabled());
            assertTrue(f.config.worlds().npcMagicEnabled());assertFalse(f.config.worlds().greaterHasteEnabled());
            assertFalse(f.config.worlds().timeAttackEnabled());
            var server=ModernMain.start(f.config);try{assertEquals(0,server.snapshot().accepted());}
            finally{server.close();assertTrue(server.awaitTermination(5000));}
            assertEquals(original,ModernSlowMainSqlTest.sourceRows(f));
            assertArrayEquals(new long[]{0,0,0},f.mysql.counts());assertEquals(0,f.mysql.botCount());
            assertEquals(0,f.mysql.activeConnections());assertFalse(f.mysql.forcedTermination());f.successful();
        }
    }

    @Test @Timeout(value=120,unit=TimeUnit.SECONDS)
    void targetedTimeAttackMainRequiresOneCooperativeCampAndKeepsImportedSourcesReadOnly()throws Exception {
        var mysql=System.getenv(ModernSettlementMysqlFixture.ENV);var shared=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(mysql!=null&&shared!=null);
        var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
        try(var f=ModernQuestSqlFixture.openTargetHasteTimeAttack(Path.of(mysql),Path.of(shared),tls)) {
            assertTrue(f.config.worlds().hasteEnabled());assertTrue(f.config.worlds().npcEnabled());
            assertTrue(f.config.worlds().timeAttackEnabled());assertTrue(f.config.worlds().npcMagicEnabled());
            assertFalse(f.config.worlds().questEnabled());assertFalse(f.config.worlds().greaterHasteEnabled());
            var server=ModernMain.start(f.config);try{assertEquals(0,server.snapshot().accepted());}
            finally{server.close();assertTrue(server.awaitTermination(5000));}
            ModernHasteSqlTestPeer.assertSourceUnchanged(f);assertArrayEquals(new long[]{0,0,0},f.mysql.counts());
            assertEquals(0,f.mysql.botCount());assertEquals(0,f.mysql.activeConnections());f.successful();
        }
    }
    @Test @Timeout(value=120,unit=TimeUnit.SECONDS)
    void targetedQuestMainKeepsNpcBotsTimedRulesAndSourceOwnership()throws Exception {
        var mysql=System.getenv(ModernSettlementMysqlFixture.ENV);var shared=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(mysql!=null&&shared!=null);
        var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
        try(var f=ModernQuestSqlFixture.openTargetHasteQuest(Path.of(mysql),Path.of(shared),tls)) {
            assertTrue(f.config.worlds().hasteEnabled());assertTrue(f.config.worlds().npcEnabled());
            assertTrue(f.config.worlds().questEnabled());assertTrue(f.config.worlds().npcMagicEnabled());
            assertFalse(f.config.worlds().greaterHasteEnabled());assertFalse(f.config.worlds().timeAttackEnabled());
            var server=ModernMain.start(f.config);try{assertEquals(0,server.snapshot().accepted());}
            finally{server.close();assertTrue(server.awaitTermination(5000));}
            ModernHasteSqlTestPeer.assertSourceUnchanged(f);
            assertArrayEquals(new long[]{0,0,0},f.mysql.counts());assertEquals(0,f.mysql.botCount());
            assertEquals(0,f.mysql.activeConnections());f.successful();
        }
    }

    @Test @Timeout(value=120,unit=TimeUnit.SECONDS)
    void realMainRulesAndSourceQualificationKeepWideStatsAndReadOnlyOwnership()throws Exception {
        var mysql=System.getenv(ModernSettlementMysqlFixture.ENV);var shared=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(mysql!=null&&shared!=null);
        var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
        try(var f=ModernQuestSqlFixture.openTargetHaste(Path.of(mysql),Path.of(shared),tls);
                var executor=new ModernBackendExecutor(1,1);var gate=executor.openSession()) {
            assertTrue(f.config.worlds().hasteEnabled());assertFalse(f.config.worlds().greaterHasteEnabled());assertFalse(f.config.worlds().npcEnabled());
            for(var source:f.config.sources()) {
                var dao=new ModernJdbcBackend(source.database(),1000,5000);
                gate.call(10000,scope->{
                    var auth=dao.authenticate(ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD);assertTrue(auth.accepted());
                    String owner=source.id()==7?"fixture-user":"FIXTURE-USER";
                    var physical=dao.physicalCharacter(source.id(),owner,3333,source.catalogDatabase(),true,false,false,false).orElseThrow();
                    var fighter=ModernPhysicalCombat.qualify(physical,ModernCombatProfileTest.binding(physical.input().equipment()),java.util.Optional.empty(),System.currentTimeMillis());
                    var learned=dao.hasteCharacter(source.id(),owner,3333,source.catalogDatabase()).orElseThrow();
                    assertEquals(source.id()==7?5000000000L:12,fighter.profile().effective().intelligence());
                    assertEquals(source.id()==7?15:40,ModernHaste.qualify(fighter).mpCost());
                    assertEquals(source.id()==7?ModernHasteCatalog.Status.QUALIFIED:ModernHasteCatalog.Status.UNLEARNED,learned.status());
                    return null;
                });
                var connections=new ModernSettlementConnections(source.database(),1000,3000,1);
                try {
                    try(var c=connections.open(ModernSettlementMysqlFixture.deadline(5));
                            var s=c.prepareStatement("UPDATE `"+ModernQuestSqlFixture.source(source.id())+"`.characters SET CurMp=0 WHERE objid=3333")) {
                        c.setReadOnly(false);s.setQueryTimeout(2);assertEquals(1142,assertThrows(SQLException.class,s::executeUpdate).getErrorCode());
                    }
                }finally{connections.close();assertTrue(connections.awaitTermination(5000));}
            }
            var server=ModernMain.start(f.config);try{assertEquals(0,server.snapshot().accepted());}
            finally{server.close();assertTrue(server.awaitTermination(5000));}
            ModernHasteSqlTestPeer.assertSourceUnchanged(f);
            executor.close();assertTrue(executor.awaitTermination(5000));assertEquals(0,f.mysql.activeConnections());f.successful();
        }
    }
}
