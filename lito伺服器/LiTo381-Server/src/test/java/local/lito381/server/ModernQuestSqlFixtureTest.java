package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class ModernQuestSqlFixtureTest {
    @Test @Timeout(value=120,unit=TimeUnit.SECONDS)
    void actualSourceQueriesReadBothDistinctOwnersInventoryAndPhysicalPoliciesBeforeMainStarts()throws Exception {
        var mysql=System.getenv(ModernSettlementMysqlFixture.ENV);var shared=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(mysql!=null&&shared!=null);
        var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
        try(var f=ModernQuestSqlFixture.open(Path.of(mysql),Path.of(shared),tls);var executor=new ModernBackendExecutor(1,1);var gate=executor.openSession()) {
            for(var source:f.config.sources()) {
                var dao=new ModernJdbcBackend(source.database(),1000,5000);
                Throwable failure=gate.call(5000,scope->{try{
                    assertTrue(dao.authenticate(ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD).accepted());
                    var rows=dao.characters(ModernQuestSqlFixture.ACCOUNT);assertEquals(1,rows.size());assertEquals(3333,rows.get(0).objectId());
                    var data=dao.persistedCharacter(source.id(),ModernQuestSqlFixture.ACCOUNT,3333,source.catalogDatabase()).orElseThrow();
                    assertEquals(2,data.inventory().size());assertTrue(data.inventory().get(0).equipped());
                    assertEquals(source.id()==7?2:5,data.inventory().get(1).quantity());
                    assertEquals(source.id()==7?9007199254740993L:9007199254740995L,data.persisted().experience());
                    var physical=dao.physicalCharacter(source.id(),ModernQuestSqlFixture.ACCOUNT,3333,source.catalogDatabase(),false,false,true,false).orElseThrow();
                    assertEquals(data,physical.input().equipment().source());assertEquals(1,physical.input().weapons().size());
                    assertTrue(dao.skillsCharacter(source.id(),ModernQuestSqlFixture.ACCOUNT,3333,source.catalogDatabase()).orElseThrow().skills().isEmpty());
                    assertEquals(1,dao.shopCharacter(source.id(),ModernQuestSqlFixture.ACCOUNT,3333,source.catalogDatabase()).orElseThrow().offers().size());
                    assertNotNull(dao.gmCommandCharacter(source.id(),ModernQuestSqlFixture.ACCOUNT,3333,source.catalogDatabase()).orElseThrow());
                    return null;
                }catch(Throwable diagnostic){return diagnostic;}});
                if(failure!=null)throw new AssertionError("isolated SQL source "+source.id(),failure);
            }
            var server=ModernMain.start(f.config);try{assertEquals(0,server.snapshot().accepted());}finally{server.close();assertTrue(server.awaitTermination(5000));}
            executor.close();assertTrue(executor.awaitTermination(5000));assertEquals(0,f.mysql.activeConnections());f.successful();
        }
    }
}
