package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernEquipmentStartupTest {
    private static final Path TLS=Path.of("target/modern-tls-test-fixtures").toAbsolutePath();
    @TempDir Path temporary;
    @BeforeAll static void tls() throws Exception { ModernTlsTestMaterial.prepare(TLS); }

    @Test void productionReadProbeUsesSeparateConsumableCredentialAttemptsForEverySource() throws Exception {
        char[] password="probe-fixture-only".toCharArray();
        var borrowed=new java.util.ArrayList<char[]>();
        var principal=new ModernAccountAuthentication.Principal("login",java.util.Set.of(new ModernAccountAuthentication.Source(7,"Sun")),java.util.Map.of(7,"owner"));
        try (var executor=new ModernBackendExecutor(1,1);var gate=executor.openSession()) {
            for (int i=0;i<2;i++) assertSame(principal,ModernEquipmentRepositoryReadProbe.authenticate(gate,(account,owned)->{
                assertArrayEquals("probe-fixture-only".toCharArray(),owned);borrowed.add(owned);return principal;
            },"login",password));
            assertArrayEquals("probe-fixture-only".toCharArray(),password);
            assertEquals(2,borrowed.size());assertNotSame(borrowed.get(0),borrowed.get(1));
            for (char[] consumed:borrowed) assertArrayEquals(new char[consumed.length],consumed);
            executor.close();assertTrue(executor.awaitTermination(3000));
        } finally { java.util.Arrays.fill(password,'\0'); }
    }

    private ModernServerConfig config(Path directory,int port) throws Exception {
        return ModernServerConfig.parse(("schema=1\nbind.port="+port+"\ntls.pkcs12="+TLS.resolve("server.p12").toString().replace('\\','/')
                +"\ntls.password.env=T\nsources=7\nsource.7.name=Sun\nsource.7.mysql.host=127.0.0.1\nsource.7.mysql.port=3306\n"
                +"source.7.mysql.database=not_a_live_database\nsource.7.catalog.database=not_a_live_catalog\n"
                +"source.7.mysql.username.env=U\nsource.7.mysql.password.env=P\nsource.7.mysql.passwordScheme=plain\nsource.7.mysql.tls=disabled\n"
                +"equipment.storeDirectory="+directory.toString().replace('\\','/')+"\n").getBytes(StandardCharsets.UTF_8),
                name->name.equals("T")?"modern-lito-fixture-only":"unused-test-credential");
    }
    @Test void realMainWiringOwnsStoreUntilListenerSessionsAndBackendActuallyRetire() throws Exception {
        Path directory=Files.createDirectory(temporary.resolve("equipment"));
        try (var config=config(directory,0)) {
            var server=ModernMain.start(config);
            try {
                assertTrue(Files.isRegularFile(directory.resolve(".modern-equipment.lock")));
                assertEquals(0,server.snapshot().accepted());
                assertEquals(ModernEquipmentOverrideStore.Failure.BUSY,
                        assertThrows(ModernEquipmentOverrideStore.StoreException.class,()->new ModernEquipmentOverrideStore(directory)).failure());
                server.close();
                assertEquals(ModernEquipmentOverrideStore.Failure.BUSY,
                        assertThrows(ModernEquipmentOverrideStore.StoreException.class,()->new ModernEquipmentOverrideStore(directory)).failure());
                assertTrue(server.awaitTermination(3000));
                try (var reopened=new ModernEquipmentOverrideStore(directory)) { assertFalse(reopened.isQuiescent()); }
            } finally { server.close();assertTrue(server.awaitTermination(3000)); }
        }
    }
    @Test void failedListenerConstructionReleasesOnlyItsUntransferredStore() throws Exception {
        Path directory=Files.createDirectory(temporary.resolve("equipment"));
        try (var occupied=new ServerSocket(0,1,InetAddress.getByAddress(new byte[]{127,0,0,1}));
             var config=config(directory,occupied.getLocalPort())) {
            assertThrows(java.io.IOException.class,()->ModernMain.start(config));
            try (var reopened=new ModernEquipmentOverrideStore(directory)) { assertFalse(reopened.isQuiescent()); }
        }
    }

    @Test void equipmentRepositoryRejectsMismatchedDaoRoutingAndUnauthorizedSourcesBeforeJdbc() throws Exception {
        Path directory=Files.createDirectory(temporary.resolve("equipment"));
        try (var first=config(directory,0);var second=config(directory,0)) {
            var calls=new java.util.concurrent.atomic.AtomicInteger();
            var dao=new ModernJdbcBackend(first.sources().get(0).database(),1000,1000,(url,properties)->{
                calls.incrementAndGet();throw new java.sql.SQLException("must not open for rejected routing");
            });
            var source=new ModernEquipmentRepository.Source(first.sources().get(0),dao);
            assertThrows(IllegalArgumentException.class,()->new ModernEquipmentRepository.Source(second.sources().get(0),dao));
            assertThrows(IllegalArgumentException.class,()->ModernEquipmentRepository.jdbc(java.util.Map.of(42,source)));
            var provider=ModernEquipmentRepository.jdbc(java.util.Map.of(7,source));
            var other=new ModernAccountAuthentication.Principal("login",java.util.Set.of(new ModernAccountAuthentication.Source(42,"Moon")),java.util.Map.of(42,"owner"));
            assertThrows(SecurityException.class,()->provider.read(other,7,3333));
            assertEquals(0,calls.get());
        }
    }
}
