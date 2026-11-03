package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** No live SQL: strict config, owned fake JDBC, and isolated loopback TCP only. */
class ModernSettlementConnectionsTest {
    @TempDir Path temporary;
    private ModernServerConfig config(int port) throws Exception { return config("127.0.0.1", port, null); }
    private ModernServerConfig config(String host, int port, Path trust) throws Exception {
        String body = "schema=1\nbind.port=0\ntls.pkcs12=" + portable(temporary.resolve("listener.p12"))
                + "\ntls.password.env=TLS\nsources=7\nsource.7.name=Sun\nsource.7.mysql.host=127.0.0.1\n"
                + "source.7.mysql.port=3306\nsource.7.mysql.database=source_data\nsource.7.mysql.username.env=SOURCE_USER\n"
                + "source.7.mysql.password.env=SOURCE_PASSWORD\nsource.7.mysql.passwordScheme=plain\nsource.7.mysql.tls=disabled\n"
                + "settlement.enabled=true\nsettlement.outboxDirectory=" + portable(temporary) + "\nsettlement.mysql.host=" + host
                + "\nsettlement.mysql.port=" + port + "\nsettlement.mysql.database=only_settlement\nsettlement.mysql.username.env=WRITER_USER\n"
                + "settlement.mysql.password.env=WRITER_PASSWORD\nsettlement.mysql.tls=" + (trust == null ? "disabled" : "verify_identity") + "\n"
                + (trust == null ? "" : "settlement.mysql.trustStore.pkcs12=" + portable(trust) + "\nsettlement.mysql.trustStore.password.env=TRUST\n");
        return ModernServerConfig.parse(body.getBytes(StandardCharsets.UTF_8), name -> switch (name) {
            case "SOURCE_USER" -> "readonly-user"; case "WRITER_USER" -> "settlement-user";
            case "WRITER_PASSWORD" -> "synthetic-writer-secret"; default -> "synthetic-secret";
        });
    }
    private static String portable(Path path) { return path.toAbsolutePath().toString().replace('\\', '/'); }
    private static long deadline(int millis) { return System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis); }
    private static void safe(SQLException error) {
        assertEquals("modern settlement connection unavailable", error.getMessage());
        assertNull(error.getCause()); assertEquals(0, error.getSuppressed().length);
    }
    private static void stop(ModernSettlementConnections factory) throws Exception {
        factory.close(); assertTrue(factory.awaitTermination(4000)); assertTrue(factory.isQuiescent()); assertEquals(0, factory.activeCount());
    }

    @Test void independentFreshConnectionsHaveStrictPropertiesAndRetireCompletely() throws Exception {
        try (var config = config(3306)) {
            var fixture = new Jdbc(); var factory = new ModernSettlementConnections(config.settlement().database(), 1000, 2000, 2, fixture);
            try {
                try (var connection = factory.open(deadline(5000))) {
                    assertFalse(connection.isClosed()); assertTrue(connection.getAutoCommit());
                    connection.setReadOnly(false); connection.setNetworkTimeout(Runnable::run, Integer.MAX_VALUE);
                    assertEquals(2000, fixture.networkMillis); assertEquals(1, factory.activeCount());
                    assertEquals("ModernSettlementConnection[redacted]", connection.toString());
                    assertFalse(connection.isWrapperFor(Connection.class)); safe(assertThrows(SQLException.class, () -> connection.unwrap(Connection.class)));
                }
                try (var ignored = factory.open(deadline(5000))) { assertEquals(2, fixture.sockets.size()); }
                assertTrue(fixture.sockets.stream().allMatch(Socket::isClosed)); assertEquals(2, fixture.closes.get());
                assertEquals(0, fixture.aborts.get()); assertEquals(0, factory.activeCount()); assertTrue(fixture.properties.isEmpty());
                assertTrue(fixture.url.endsWith("/only_settlement")); assertEquals("settlement-user", fixture.saved.getProperty("user"));
                assertEquals("synthetic-writer-secret", fixture.saved.getProperty("password"));
                assertEquals(ModernSettlementConnections.ScopedSocketFactory.class.getName(), fixture.saved.getProperty("socketFactory"));
                for (String key : List.of("autoReconnect", "allowMultiQueries", "allowLoadLocalInfile", "allowUrlInLocalInfile", "allowPublicKeyRetrieval",
                        "autoDeserialize", "profileSQL", "traceProtocol", "enablePacketDebug", "fallbackToSystemTrustStore", "fallbackToSystemKeyStore"))
                    assertEquals("false", fixture.saved.getProperty(key));
                assertEquals("true", fixture.saved.getProperty("queryTimeoutKillsConnection"));
                assertEquals("DISABLED", fixture.saved.getProperty("sslMode")); assertEquals("TLSv1.2,TLSv1.3", fixture.saved.getProperty("tlsVersions"));
                assertFalse(fixture.saved.containsKey("createDatabaseIfNotExist"));
                assertEquals("readonly-user", config.sources().get(0).database().username());
            } finally { stop(factory); }
        }
    }
    @Test void capacityAndExpiredAdmissionDoNotOpenOrQueueAnExtraConnection() throws Exception {
        try (var config = config(3306)) {
            var fixture = new Jdbc(); var factory = new ModernSettlementConnections(config.settlement().database(), 1000, 2000, 1, fixture);
            try {
                safe(assertThrows(SQLException.class, () -> factory.open(System.nanoTime())));
                safe(assertThrows(SQLException.class, () -> factory.open(deadline(61_000)))); assertEquals(0, fixture.sockets.size());
                try (var connection = factory.open(deadline(5000))) {
                    safe(assertThrows(SQLException.class, () -> factory.open(deadline(5000)))); assertEquals(1, fixture.sockets.size());
                }
                try (var ignored = factory.open(deadline(5000))) { assertEquals(2, fixture.sockets.size()); }
            } finally { stop(factory); }
            safe(assertThrows(SQLException.class, () -> factory.open(deadline(5000))));
        }
    }
    @Test void preparedStatementExecuteKeepsExactDuplicateVendorStateAndExceptionInstanceThroughOwnedConnection() throws Exception {
        try (var config = config(3306)) {
            var fixture = new Jdbc();
            SQLException duplicate = new SQLException("synthetic duplicate only", "23000", 1062);
            fixture.statementFailure = duplicate;
            var factory = new ModernSettlementConnections(config.settlement().database(), 1000, 2000, 1, fixture);
            try {
                try (var connection = factory.open(deadline(5000));
                     var statement = connection.prepareStatement("INSERT INTO synthetic_fixture VALUES (1)")) {
                    var observed = assertThrows(SQLException.class, statement::executeUpdate);
                    assertSame(duplicate, observed, "the owned connection must not replace the statement's duplicate exception");
                    assertEquals(1062, observed.getErrorCode()); assertEquals("23000", observed.getSQLState());
                    assertNull(observed.getCause()); assertNull(observed.getNextException()); assertEquals(0, observed.getSuppressed().length);
                }
                assertEquals(1, fixture.statementsClosed.get()); assertEquals(1, fixture.closes.get());
                assertTrue(fixture.sockets.get(0).isClosed()); assertEquals(0, factory.activeCount());
            } finally { stop(factory); }
        }
    }
    @Test void failedConnectAndNullConnectionCloseTheirPreconnectedSocketsAndProperties() throws Exception {
        for (String fault : List.of("connect", "null", "setup", "existing-transaction")) try (var config = config(3306)) {
            var fixture = new Jdbc(); fixture.fault = fault;
            var factory = new ModernSettlementConnections(config.settlement().database(), 1000, 2000, 1, fixture);
            try {
                safe(assertThrows(SQLException.class, () -> factory.open(deadline(5000))));
                assertTrue(fixture.sockets.stream().allMatch(Socket::isClosed)); assertTrue(fixture.properties.isEmpty()); assertEquals(0, factory.activeCount());
            } finally { stop(factory); }
        }
    }
    @Test void driverBypassWithoutRawSocketIsRejectedAndOwnedConnectionClosed() throws Exception {
        try (var config = config(3306)) {
            var fixture = new Jdbc(); fixture.skipSocket = true;
            var factory = new ModernSettlementConnections(config.settlement().database(), 1000, 2000, 1, fixture);
            try { safe(assertThrows(SQLException.class, () -> factory.open(deadline(5000)))); assertEquals(1, fixture.closes.get()); }
            finally { stop(factory); }
        }
        assertThrows(IllegalStateException.class, () -> new ModernSettlementConnections.ScopedSocketFactory().createSocket(null));
    }
    @Test void cancellationDuringLateConnectNeverPublishesConnectionOrFreesCapacityEarly() throws Exception {
        try (var config = config(3306)) {
            var fixture = new Jdbc(); fixture.blockConnect = true;
            var factory = new ModernSettlementConnections(config.settlement().database(), 1000, 2000, 1, fixture);
            var worker = Executors.newSingleThreadExecutor();
            try {
                var open = worker.submit(() -> assertThrows(SQLException.class, () -> factory.open(deadline(5000))));
                assertTrue(fixture.entered.await(2, TimeUnit.SECONDS)); factory.close();
                assertTrue(fixture.sockets.get(0).isClosed()); assertEquals(1, factory.activeCount());
                assertFalse(factory.awaitTermination(30)); assertFalse(factory.isQuiescent());
                fixture.release.countDown(); safe(open.get(3, TimeUnit.SECONDS));
                assertEquals(1, fixture.closes.get()); assertEquals(0, factory.activeCount());
            } finally { fixture.release.countDown(); stop(factory); worker.shutdownNow(); assertTrue(worker.awaitTermination(3000, TimeUnit.MILLISECONDS)); }
        }
    }
    @Test void rawSocketRemainsArmedDuringQuitAndCloseCapacityIsNotFutureCompletion() throws Exception {
        try (var config = config(3306)) {
            var fixture = new Jdbc(); fixture.blockClose = true;
            var factory = new ModernSettlementConnections(config.settlement().database(), 1000, 2000, 1, fixture);
            var worker = Executors.newSingleThreadExecutor();
            try {
                var connection = factory.open(deadline(5000)); var close = worker.submit(() -> { connection.close(); return true; });
                assertTrue(fixture.entered.await(2, TimeUnit.SECONDS)); factory.close();
                assertTrue(fixture.sockets.get(0).isClosed()); assertEquals(0, fixture.aborts.get(), "JDBC lease was retired before QUIT");
                assertFalse(factory.awaitTermination(30)); assertEquals(1, factory.activeCount()); assertFalse(close.isDone());
                fixture.release.countDown(); assertTrue(close.get(3, TimeUnit.SECONDS));
                connection.close(); assertEquals(1, fixture.closes.get());
            } finally { fixture.release.countDown(); stop(factory); worker.shutdownNow(); assertTrue(worker.awaitTermination(3000, TimeUnit.MILLISECONDS)); }
        }
    }
    @Test void deadlineAloneClosesRawSocketDuringQuitWithoutExternalCloseRequest() throws Exception {
        try (var config = config(3306)) {
            var fixture = new Jdbc(); fixture.closeUntilSocketClosed = true;
            var factory = new ModernSettlementConnections(config.settlement().database(), 3000, 3000, 1, fixture);
            try {
                var connection = factory.open(deadline(400)); long start = System.nanoTime(); connection.close();
                assertTrue(fixture.sockets.get(0).isClosed()); assertEquals(0, fixture.aborts.get());
                assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 2500); assertEquals(0, factory.activeCount());
            } finally { stop(factory); }
        }
    }
    @Test void cleanupFailureIsRedactedAndPhysicalRetirementStillOccurs() throws Exception {
        try (var config = config(3306)) {
            var fixture = new Jdbc(); fixture.fault = "close";
            var factory = new ModernSettlementConnections(config.settlement().database(), 1000, 2000, 1, fixture);
            try {
                var connection = factory.open(deadline(5000)); safe(assertThrows(SQLException.class, connection::close));
                safe(assertThrows(SQLException.class, connection::close)); assertEquals(1, fixture.closes.get());
                assertTrue(fixture.sockets.get(0).isClosed()); assertEquals(0, factory.activeCount());
            } finally { stop(factory); }
        }
    }
    @Test void multipleDeadlineCallbacksDoNotWaitForBlockedJdbcAbort() throws Exception {
        try (var config = config(3306)) {
            var fixture = new Jdbc(); fixture.blockAbort = true;
            var factory = new ModernSettlementConnections(config.settlement().database(), 3000, 3000, 3, fixture);
            var connections = new ArrayList<Connection>();
            try {
                for (int i=0; i<3; i++) connections.add(factory.open(deadline(500)));
                assertTrue(fixture.entered.await(2, TimeUnit.SECONDS));
                long end = deadline(2000);
                while (fixture.sockets.stream().anyMatch(socket -> !socket.isClosed()) && System.nanoTime() < end) Thread.sleep(5);
                assertTrue(fixture.sockets.stream().allMatch(Socket::isClosed));
                assertEquals(3, factory.activeCount()); assertFalse(factory.isQuiescent());
            } finally { fixture.release.countDown(); for (var connection : connections) connection.close(); stop(factory); }
        }
    }
    @Test void realConnectorGreetingStallIsCancelledBeforeJdbcConnectionExists() throws Exception { greeting(false, false); }
    @Test void realConnectorSlowDripCannotExtendOriginalAbsoluteDeadline() throws Exception { greeting(true, false); }
    @Test void closingFactoryCancelsRealDriverConnectAndWaitsForActualRetirement() throws Exception { greeting(false, true); }
    private void greeting(boolean drip, boolean cancel) throws Exception {
        try (var server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")); var config = config(server.getLocalPort())) {
            server.setSoTimeout(5000); var threads = Executors.newFixedThreadPool(2); var accepted = new CountDownLatch(1); var sent = new AtomicInteger();
            var factory = new ModernSettlementConnections(config.settlement().database(), 3000, 3000, 1);
            try {
                var peer = threads.submit(() -> {
                    try (Socket socket = server.accept()) {
                        socket.setSoTimeout(4000); accepted.countDown();
                        if (!drip) return socket.getInputStream().read();
                        // A declared 4096-byte greeting packet is never completed:
                        // real Connector/J must keep reading its body, not parse SQL.
                        socket.getOutputStream().write(new byte[]{0, 16, 0, 0}); socket.getOutputStream().flush();
                        for (int i=0; i<60; i++) {
                            try { socket.getOutputStream().write(i == 0 ? 10 : 1); socket.getOutputStream().flush(); sent.incrementAndGet(); }
                            catch (IOException disconnected) { return -1; }
                            Thread.sleep(50);
                        }
                        return 1;
                    }
                });
                long start = System.nanoTime();
                var call = threads.submit(() -> assertThrows(SQLException.class, () -> factory.open(deadline(cancel ? 5000 : 1200))));
                assertTrue(accepted.await(3, TimeUnit.SECONDS)); if (cancel) factory.close();
                safe(call.get(4, TimeUnit.SECONDS)); assertEquals(-1, peer.get(4, TimeUnit.SECONDS));
                if (drip) assertTrue(sent.get() >= 3, "partial data really arrived before the original deadline");
                assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 3500); assertEquals(0, factory.activeCount());
            } finally { stop(factory); threads.shutdownNow(); assertTrue(threads.awaitTermination(5000, TimeUnit.MILLISECONDS)); }
        }
    }
    @Test void commitAndRollbackIoHaveRawSocketDeadlineNotOnlyNetworkTimeoutSetter() throws Exception {
        for (String operation : List.of("commit", "rollback")) try (var server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")); var config = config(server.getLocalPort())) {
            server.setSoTimeout(4000); var threads = Executors.newSingleThreadExecutor(); var fixture = new Jdbc();
            fixture.address = new InetSocketAddress("127.0.0.1", server.getLocalPort()); fixture.blockOperation = operation;
            var factory = new ModernSettlementConnections(config.settlement().database(), 3000, 3000, 1, fixture);
            try {
                var peer = threads.submit(() -> { try (Socket socket = server.accept()) { socket.setSoTimeout(4000); return socket.getInputStream().read(); } });
                try (var connection = factory.open(deadline(500))) {
                    safe(assertThrows(SQLException.class, () -> { if (operation.equals("commit")) connection.commit(); else connection.rollback(); }));
                    assertTrue(fixture.sockets.get(0).isClosed()); assertEquals(-1, peer.get(3, TimeUnit.SECONDS));
                }
            } finally { stop(factory); threads.shutdownNow(); assertTrue(threads.awaitTermination(4000, TimeUnit.MILLISECONDS)); }
        }
    }
    @Test void verifyIdentityUsesOnlyExplicitCaTrustAndInvalidTrustCannotStartHelpers() throws Exception {
        Path material = ModernTlsTestMaterial.defaultDirectory(); ModernTlsTestMaterial.prepare(material);
        var certificate = CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(Files.readAllBytes(material.resolve("root.der"))));
        var store = KeyStore.getInstance("PKCS12"); store.load(null, "synthetic-secret".toCharArray()); store.setCertificateEntry("root", certificate);
        Path path = temporary.resolve("writer-roots.p12");
        try (var output = Files.newOutputStream(path)) { store.store(output, "synthetic-secret".toCharArray()); }
        try (var config = config("192.0.2.7", 3306, path)) {
            var fixture = new Jdbc(); var factory = new ModernSettlementConnections(config.settlement().database(), 1000, 2000, 1, fixture);
            try {
                try (var ignored = factory.open(deadline(5000))) {
                    assertEquals("VERIFY_IDENTITY", fixture.saved.getProperty("sslMode"));
                    assertEquals(path.toUri().toASCIIString(), fixture.saved.getProperty("trustCertificateKeyStoreUrl"));
                    assertEquals("PKCS12", fixture.saved.getProperty("trustCertificateKeyStoreType"));
                }
            } finally { stop(factory); }
        }
        Files.write(path, new byte[]{1,2,3});
        try (var config = config("127.0.0.1", 3306, path)) {
            safe(assertThrows(SQLException.class, () -> new ModernSettlementConnections(config.settlement().database(), 1000, 2000, 1)));
        }
    }

    private static final class Jdbc implements ModernSettlementConnections.Connector {
        final List<Socket> sockets = new ArrayList<>(); final AtomicInteger closes = new AtomicInteger(), aborts = new AtomicInteger();
        final AtomicInteger statementsClosed = new AtomicInteger(); SQLException statementFailure;
        final CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        Properties properties, saved; String url, fault = "", blockOperation = ""; int networkMillis;
        boolean skipSocket, blockConnect, blockClose, closeUntilSocketClosed, blockAbort; InetSocketAddress address;
        @Override public Connection connect(String url, Properties properties) throws SQLException {
            this.url = url; this.properties = properties; saved = new Properties(); saved.putAll(properties);
            Socket socket = skipSocket ? null : new ModernSettlementConnections.ScopedSocketFactory().createSocket(null);
            if (socket != null) sockets.add(socket);
            try { if (address != null) socket.connect(address, 2000); } catch (IOException failure) { throw secret(); }
            if (blockConnect) awaitRelease();
            if (fault.equals("connect")) throw secret(); if (fault.equals("null")) return null;
            boolean[] closed = {false};
            return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Connection.class}, (proxy, method, args) -> switch (method.getName()) {
                case "getAutoCommit" -> !fault.equals("existing-transaction"); case "isClosed" -> closed[0];
                case "setReadOnly" -> { assertFalse((Boolean) args[0]); yield null; }
                case "setNetworkTimeout" -> { networkMillis = (Integer) args[1]; if (fault.equals("setup")) throw secret(); yield null; }
                case "prepareStatement" -> {
                    assertNotNull(statementFailure, "only the explicit statement-composition test uses this seam");
                    yield (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{PreparedStatement.class}, (statement, call, values) -> switch (call.getName()) {
                        case "executeUpdate" -> throw statementFailure;
                        case "close" -> { statementsClosed.incrementAndGet(); yield null; }
                        default -> throw new AssertionError("unexpected synthetic statement method " + call.getName());
                    });
                }
                case "commit", "rollback" -> {
                    if (blockOperation.equals(method.getName())) { entered.countDown(); try { socket.getInputStream().read(); } catch (IOException failure) { throw secret(); } }
                    yield null;
                }
                case "abort" -> { aborts.incrementAndGet(); if (blockAbort) awaitRelease(); closed[0] = true; yield null; }
                case "close" -> {
                    closes.incrementAndGet(); if (blockClose) awaitRelease();
                    if (closeUntilSocketClosed) {
                        entered.countDown(); long until = deadline(3000);
                        while (!socket.isClosed() && System.nanoTime() < until) Thread.sleep(5);
                        assertTrue(socket.isClosed());
                    }
                    closed[0] = true; if (fault.equals("close")) throw secret(); yield null;
                }
                default -> throw new AssertionError("unexpected owned connection method " + method.getName());
            });
        }
        private void awaitRelease() throws SQLException {
            entered.countDown(); boolean interrupted = false;
            try {
                for (;;) try { if (!release.await(5, TimeUnit.SECONDS)) throw secret(); return; }
                catch (InterruptedException ignored) { interrupted = true; }
            } finally { if (interrupted) Thread.currentThread().interrupt(); }
        }
        private static SQLException secret() { return new SQLException("SYNTHETIC-SECRET writer address/password"); }
    }
}
