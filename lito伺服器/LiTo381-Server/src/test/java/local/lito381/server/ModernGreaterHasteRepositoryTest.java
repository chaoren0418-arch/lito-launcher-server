package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernGreaterHasteRepositoryTest {
    @TempDir Path temporary;
    private ModernServerConfig config()throws Exception {
        String text="schema=1\nbind.port=0\ntls.pkcs12="+temporary.resolve("unused.p12").toString().replace('\\','/')
                +"\ntls.password.env=TLS\nsources=7\nsource.7.name=Sun\nsource.7.mysql.host=127.0.0.1\nsource.7.mysql.port=1"
                +"\nsource.7.mysql.database=fixture\nsource.7.mysql.username.env=USER\nsource.7.mysql.password.env=PASSWORD"
                +"\nsource.7.mysql.passwordScheme=plain\nsource.7.mysql.tls=disabled\n";
        return ModernServerConfig.parse(text.getBytes(StandardCharsets.UTF_8),key->"synthetic-only");
    }
    private static ModernAccountAuthentication.Principal principal() {
        return new ModernAccountAuthentication.Principal("display-only",Set.of(
                new ModernAccountAuthentication.Source(7,"Sun"),new ModernAccountAuthentication.Source(42,"Moon")),
                Map.of(7,"sun-owner",42,"moon-owner"));
    }
    @Test void copiedSourcesAndPerSourceCanonicalAccountsOwnTheBoundedRead()throws Exception {
        try(var config=config()) {
            var sun=new ModernDisintegrateCatalogTest.Jdbc(List.of(ModernGreaterHasteCatalogTest.row()));
            var moon=new ModernDisintegrateCatalogTest.Jdbc(List.of(ModernGreaterHasteCatalogTest.row()));
            var a=new ModernJdbcBackend(config.sources().get(0).database(),1000,3000,(url,p)->sun.connection());
            var b=new ModernJdbcBackend(config.sources().get(0).database(),1000,3000,(url,p)->moon.connection());
            var bindings=new HashMap<Integer,ModernGreaterHasteRepository.Source>();
            bindings.put(7,new ModernGreaterHasteRepository.Source(a,"sun_catalog"));
            bindings.put(42,new ModernGreaterHasteRepository.Source(b,"moon_catalog"));
            var repo=ModernGreaterHasteRepository.jdbc(bindings);bindings.clear();
            var first=ModernEquipmentSourceJdbcTest.scoped(()->repo.character(principal(),7,3333)).orElseThrow();
            var second=ModernEquipmentSourceJdbcTest.scoped(()->repo.character(principal(),42,3333)).orElseThrow();
            assertEquals(new TournamentPresence.CharacterKey(7,3333),first.key());
            assertEquals(new TournamentPresence.CharacterKey(42,3333),second.key());
            assertEquals("sun-owner",sun.parameters.get(2));assertEquals("moon-owner",moon.parameters.get(2));
            assertTrue(sun.queries.get(1).contains("`sun_catalog`.`skills`"));
            assertTrue(moon.queries.get(1).contains("`moon_catalog`.`skills`"));
            for(var jdbc:List.of(sun,moon)) {
                assertTrue(jdbc.readOnly);assertEquals(1,jdbc.closedConnections);
                assertEquals(3,jdbc.closedStatements);assertEquals(3,jdbc.closedResults);
                assertEquals(List.of(1,2,4),jdbc.limits);
                assertTrue(jdbc.timeouts.stream().allMatch(n->n>0&&n<=30),"bounded by this test's 30-second scope");
            }
        }
    }
    @Test void invalidAuthorityInputsAndRetiredScopeOpenNoConnections()throws Exception {
        try(var config=config()) {
            int[] opened={0};
            var dao=new ModernJdbcBackend(config.sources().get(0).database(),1000,3000,(url,p)->{opened[0]++;throw new AssertionError("no I/O allowed");});
            var source=new ModernGreaterHasteRepository.Source(dao,"catalog");
            var repo=ModernGreaterHasteRepository.jdbc(Map.of(7,source));
            assertThrows(SecurityException.class,()->repo.character(principal(),99,3333));
            assertThrows(IllegalStateException.class,()->repo.character(principal(),42,3333));
            for(String catalog:new String[]{null,"","a;drop","a".repeat(65)}) {
                assertThrows(IllegalArgumentException.class,()->new ModernGreaterHasteRepository.Source(dao,catalog));
                assertThrows(IllegalArgumentException.class,()->ModernEquipmentSourceJdbcTest.scoped(()->dao.greaterHasteCharacter(7,"owner",3333,catalog)));
            }
            for(long id:new long[]{Long.MIN_VALUE,0})
                assertThrows(SQLException.class,()->ModernEquipmentSourceJdbcTest.scoped(()->repo.character(principal(),7,id)));
            for(String account:new String[]{null,"","bad\nname","界".repeat(22)})
                assertThrows(SQLException.class,()->ModernEquipmentSourceJdbcTest.scoped(()->dao.greaterHasteCharacter(7,account,3333,"catalog")));
            var scope=ModernDisintegrateCatalogTest.scope();ModernBackendScope.bind(scope);
            try {
                scope.cancel(ModernBackendExecutor.Failure.CANCELLED);
                assertEquals(ModernBackendExecutor.Failure.CANCELLED,
                        assertThrows(ModernBackendExecutor.BackendException.class,()->repo.character(principal(),7,3333)).failure());
            } finally {scope.finish();ModernBackendScope.unbind();}
            assertEquals(0,opened[0]);
            assertThrows(IllegalArgumentException.class,()->ModernGreaterHasteRepository.jdbc(null));
            assertThrows(IllegalArgumentException.class,()->ModernGreaterHasteRepository.jdbc(Map.of()));
            assertThrows(IllegalArgumentException.class,()->ModernGreaterHasteRepository.jdbc(Map.of(0,source)));
            var tooMany=new HashMap<Integer,ModernGreaterHasteRepository.Source>();
            for(int i=1;i<=65;i++)tooMany.put(i,source);
            assertThrows(IllegalArgumentException.class,()->ModernGreaterHasteRepository.jdbc(tooMany));
            assertFalse(source.toString().contains("catalog"));
        }
    }
    @Test void cancelledCatalogNeverEscapesAsAQualifiedSnapshotAndClosesOwnedConnection()throws Exception {
        try(var config=config()) {
            for(int query=1;query<=3;query++)for(int next=1;next<=2;next++) {
                if(query==1&&next==2)continue;
                var jdbc=new ModernDisintegrateCatalogTest.Jdbc(List.of(ModernGreaterHasteCatalogTest.row()));
                jdbc.cancelQuery=query;jdbc.cancelNext=next;
                var dao=new ModernJdbcBackend(config.sources().get(0).database(),1000,3000,(url,p)->jdbc.connection());
                var repo=ModernGreaterHasteRepository.jdbc(Map.of(7,new ModernGreaterHasteRepository.Source(dao,"catalog")));
                assertEquals(ModernBackendExecutor.Failure.CANCELLED,
                        assertThrows(ModernBackendExecutor.BackendException.class,()->
                            ModernEquipmentSourceJdbcTest.scoped(()->repo.character(principal(),7,3333))).failure());
                assertEquals(1,jdbc.closedConnections);assertEquals(jdbc.queries.size(),jdbc.closedStatements);
                assertEquals(jdbc.queries.size(),jdbc.closedResults);
            }
        }
    }
}
