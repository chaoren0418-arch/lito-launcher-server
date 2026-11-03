package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class ModernGreaterHasteSqlFixtureTest {
    @Test @Timeout(value=120,unit=TimeUnit.SECONDS)
    void realMainRulesAndSourceQualificationKeepWideStatsAndReadOnlyOwnership()throws Exception {
        var mysql=System.getenv(ModernSettlementMysqlFixture.ENV);var shared=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(mysql!=null&&shared!=null);
        var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
        try(var f=ModernQuestSqlFixture.openGreaterHaste(Path.of(mysql),Path.of(shared),tls);
                var executor=new ModernBackendExecutor(1,1);var gate=executor.openSession()) {
            assertTrue(f.config.worlds().greaterHasteEnabled());assertFalse(f.config.worlds().npcEnabled());
            for(var source:f.config.sources()) {
                var dao=new ModernJdbcBackend(source.database(),1000,5000);
                gate.call(10000,scope->{
                    var auth=dao.authenticate(ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD);assertTrue(auth.accepted());
                    String owner=source.id()==7?"fixture-user":"FIXTURE-USER";
                    var physical=dao.physicalCharacter(source.id(),owner,3333,source.catalogDatabase(),true,false,false,false).orElseThrow();
                    var fighter=ModernPhysicalCombat.qualify(physical,ModernCombatProfileTest.binding(physical.input().equipment()),java.util.Optional.empty(),System.currentTimeMillis());
                    var learned=dao.greaterHasteCharacter(source.id(),owner,3333,source.catalogDatabase()).orElseThrow();
                    assertEquals(source.id()==7?5000000000L:12,fighter.profile().effective().intelligence());
                    assertEquals(source.id()==7?25:60,ModernGreaterHaste.qualify(fighter).mpCost());
                    assertEquals(source.id()==7?ModernGreaterHasteCatalog.Status.QUALIFIED:ModernGreaterHasteCatalog.Status.UNLEARNED,learned.status());
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
            ModernGreaterHasteSqlTestPeer.assertSourceUnchanged(f);
            executor.close();assertTrue(executor.awaitTermination(5000));assertEquals(0,f.mysql.activeConnections());f.successful();
        }
    }
}
