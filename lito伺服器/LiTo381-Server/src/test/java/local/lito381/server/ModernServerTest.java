package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ModernServerTest {
    private static SSLContext serverContext;
    private static SSLContext clientContext;
    @BeforeAll static void certificates() throws Exception {
        Path directory = ModernTlsTestMaterial.defaultDirectory();
        ModernTlsTestMaterial.prepare(directory);
        char[] password = "modern-lito-fixture-only".toCharArray();
        serverContext = ModernTlsIdentity.load(directory.resolve("server.p12"), password);
        assertArrayEquals(new char[password.length], password);
        clientContext = ModernTlsTestMaterial.clientContext(directory);
    }
    private static ModernServer.Limits limits(int maximum, int perIp) {
        return new ModernServer.Limits(maximum, perIp, 600, maximum, 1, 4, 2000, 2000, 2000, 5000);
    }
    private static ModernServer start(ModernServer.Limits limits, ModernApplicationTestBackend backend) throws Exception {
        return new ModernServer(new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 0), serverContext,
                limits, backend.verifier, backend.characters);
    }
    private static SSLSocket client(ModernServer server) throws Exception {
        var socket = (SSLSocket) clientContext.getSocketFactory().createSocket();
        var parameters = socket.getSSLParameters();
        parameters.setEndpointIdentificationAlgorithm("HTTPS");
        socket.setSSLParameters(parameters);
        socket.setSoTimeout(3000);
        // Fixtures contain IP SAN 127.0.0.1 as well as localhost.
        socket.connect(server.address(), 1000);
        socket.startHandshake();
        int v1Capabilities = ModernSessionProtocol.CAP_EXPLICIT_I64_STATS | ModernSessionProtocol.CAP_SOURCE_SCOPED_IDENTITIES
                | ModernSessionProtocol.CAP_TOURNAMENT_NAVIGATION | ModernSessionProtocol.CAP_CHARACTER_DATA;
        var hello = new ModernSessionProtocol.Hello(1, ModernSessionProtocol.VERSION,
                ModernSessionProtocol.VERSION, v1Capabilities, v1Capabilities,
                ModernSessionProtocol.NUMERIC_SCHEMA_I64_V1);
        socket.getOutputStream().write(ModernSessionProtocol.encode(hello));
        socket.getOutputStream().flush();
        assertNotNull(ModernSessionProtocol.readFrame(socket.getInputStream()));
        return socket;
    }
    private static ModernApplicationProtocol.Message exchange(SSLSocket socket, ModernApplicationProtocol.Message request)
            throws Exception {
        byte[] encoded = ModernApplicationProtocol.encode(request);
        try {
            socket.getOutputStream().write(encoded);
            socket.getOutputStream().flush();
        } finally { Arrays.fill(encoded, (byte) 0); request.close(); }
        byte[] response = ModernApplicationProtocol.readFrame(socket.getInputStream());
        if (response == null) throw new java.io.EOFException("peer closed before application response");
        return ModernApplicationProtocol.decode(response);
    }
    private static void await(BooleanSupplier condition) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (!condition.getAsBoolean()) {
            assertTrue(System.nanoTime() < end, "listener condition deadline");
            Thread.sleep(5);
        }
    }

    @Test void productionListenerAuthenticatesAndKeepsSourceCanonicalOwnersDistinct() throws Exception {
        var repository = new ModernApplicationTestBackend("auth");
        try (var server = start(limits(4, 2), repository); var socket = client(server)) {
            var authentication = (ModernApplicationProtocol.AuthResult) exchange(socket,
                    new ModernApplicationProtocol.Auth(2, ModernApplicationTestBackend.ACCOUNT,
                            ModernApplicationTestBackend.PASSWORD.toCharArray()));
            assertEquals(0, authentication.status());
            assertEquals(ModernApplicationTestBackend.SUN_ACCOUNT, authentication.account());
            var sun = (ModernApplicationProtocol.CharacterResult) exchange(socket,
                    new ModernApplicationProtocol.CharacterRequest(3, 7, 1));
            var moon = (ModernApplicationProtocol.CharacterResult) exchange(socket,
                    new ModernApplicationProtocol.CharacterRequest(4, 42, 2));
            assertEquals(3333, sun.characters().get(0).objectId());
            assertEquals(3333, moon.characters().get(0).objectId());
            assertNotEquals(sun.characters().get(0).name(), moon.characters().get(0).name());
            assertEquals(new ModernApplicationProtocol.Pong(5), exchange(socket, new ModernApplicationProtocol.Ping(5)));
            socket.close();
            await(() -> server.snapshot().sessions() == 0);
            server.close();
            assertTrue(server.awaitTermination(3000));
            assertEquals(0, server.snapshot().backendActive());
            assertEquals(0, server.snapshot().backendQueued());
            assertFalse(server.listenerFailed());
            assertEquals(0, server.snapshot().failed()); assertTrue(server.failureCounts().isEmpty());
        }
    }

    @Test void admissionRejectsExcessConnectionsBeforeAnyCredentialWorkAndCloseRetiresHandshake() throws Exception {
        var repository = new ModernApplicationTestBackend("auth");
        try (var server = start(limits(2, 1), repository);
             var first = new Socket(server.address().getAddress(), server.address().getPort())) {
            await(() -> server.snapshot().sessions() == 1);
            try (var second = new Socket(server.address().getAddress(), server.address().getPort())) {
                second.setSoTimeout(1000);
                await(() -> server.snapshot().rejected() == 1);
                try { assertEquals(-1, second.getInputStream().read()); }
                catch (java.net.SocketException closed) { /* Zero-linger reset is also a completed rejection; a read timeout is not. */ }
            }
            assertEquals(0, repository.authenticationCalls.get());
            server.close();
            assertTrue(server.awaitTermination(3000), "stalled TLS handshake must retire on owned shutdown");
            assertEquals(0, server.snapshot().sessions());
            assertEquals(0, repository.authenticationCalls.get());
            server.close();
            assertTrue(server.awaitTermination(0));
            assertEquals(0, server.snapshot().failed()); assertTrue(server.failureCounts().isEmpty());
        }
    }

    @Test void partialTlsApplicationFrameRetainsReadEofDiagnosisAfterOwnerCleanup() throws Exception {
        try (var server = start(limits(2, 1), new ModernApplicationTestBackend("auth")); var socket = client(server)) {
            socket.getOutputStream().write(new byte[]{0, 0, 0}); socket.getOutputStream().flush();
            socket.close(); await(() -> server.snapshot().sessions() == 0);
            assertEquals(1, server.snapshot().failed());
            assertEquals(java.util.List.of(new ModernSessionFailures.Count(new ModernSessionFailures.Point(
                    ModernSessionFailures.Stage.APPLICATION_READ, ModernSessionFailures.Kind.EOF), 1)), server.failureCounts());
            server.close(); assertTrue(server.awaitTermination(3000));
            assertEquals(1, server.snapshot().failed());
        }
    }
    @Test void duplicateRequestRetainsDispatchProtocolDiagnosisRatherThanCleanupOrIo() throws Exception {
        try (var server = start(limits(2, 1), new ModernApplicationTestBackend("auth")); var socket = client(server)) {
            socket.getOutputStream().write(ModernApplicationProtocol.encode(new ModernApplicationProtocol.Ping(1)));
            socket.getOutputStream().flush(); await(() -> server.snapshot().sessions() == 0);
            assertEquals(1, server.snapshot().failed());
            assertEquals(java.util.List.of(new ModernSessionFailures.Count(new ModernSessionFailures.Point(
                    ModernSessionFailures.Stage.DISPATCH, ModernSessionFailures.Kind.PROTOCOL), 1)), server.failureCounts());
            server.close(); assertTrue(server.awaitTermination(3000));
        }
    }

    @Test void productionMainWiresScopedJdbcWithoutOpeningDatabaseBeforeAuthentication() throws Exception {
        var loopback = InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
        try (var database = new java.net.ServerSocket(0, 1, loopback)) {
            database.setSoTimeout(200);
            String settings = "schema=1\nbind.port=0\ntls.pkcs12="
                    + ModernTlsTestMaterial.defaultDirectory().resolve("server.p12").toAbsolutePath().toString().replace('\\', '/')
                    + "\ntls.password.env=TEST_TLS\nsources=7\nsource.7.name=fixture\n"
                    + "source.7.mysql.host=127.0.0.1\nsource.7.mysql.port=" + database.getLocalPort()
                    + "\nsource.7.mysql.database=synthetic_fixture\nsource.7.mysql.username.env=TEST_DB_USER\n"
                    + "source.7.mysql.password.env=TEST_DB_PASSWORD\nsource.7.mysql.passwordScheme=plain\n"
                    + "source.7.mysql.tls=disabled\nlimits.maxSessions=1\nlimits.backendDeadlineMillis=500\n"
                    + "limits.dbConnectMillis=200\nlimits.dbNetworkMillis=500\n";
            var environment = java.util.Map.of("TEST_TLS", "modern-lito-fixture-only", "TEST_DB_USER", "fixture",
                    "TEST_DB_PASSWORD", "synthetic-only");
            try (var config = ModernServerConfig.parse(settings.getBytes(java.nio.charset.StandardCharsets.UTF_8), environment::get);
                 var server = ModernMain.start(config); var socket = client(server)) {
                assertThrows(java.net.SocketTimeoutException.class, database::accept,
                        "startup and TLS/HELLO must not open or migrate a source database");
                database.setSoTimeout(3000);
                var peer = new java.util.concurrent.FutureTask<Void>(() -> {
                    try (var connection = database.accept()) {
                        connection.setSoTimeout(3000);
                        // A real Connector/J client waits for the absent MySQL greeting.
                        // Deadline cancellation must close TCP, not only return to UI.
                        try { assertEquals(-1, connection.getInputStream().read()); }
                        catch (java.net.SocketException reset) { /* actual close, not read timeout */ }
                    }
                    return null;
                });
                Thread peerThread = new Thread(peer, "modern-production-jdbc-test-peer");
                peerThread.start();
                try {
                    long started = System.nanoTime();
                    try {
                        var response = (ModernApplicationProtocol.AuthResult) exchange(socket,
                                new ModernApplicationProtocol.Auth(2, "fixture", "synthetic-only".toCharArray()));
                        assertEquals(2, response.status());
                        assertTrue(response.sources().isEmpty());
                        assertEquals("", response.account());
                    } catch (java.io.EOFException | java.net.SocketException closed) {
                        // A total backend deadline may fail-close before any AUTH_RESULT.
                    }
                    assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) < 2000);
                    peer.get(3, TimeUnit.SECONDS);
                    await(() -> server.snapshot().sessions() == 0 && server.snapshot().backendActive() == 0);
                    server.close();
                    assertTrue(server.awaitTermination(3000));
                } finally {
                    database.close();
                    peerThread.join(3000);
                    assertFalse(peerThread.isAlive(), "test-owned MySQL peer must retire");
                }
            }
        }
    }

    @Test void listenerShutdownDoesNotClaimTerminationWhileBackendStillOwnsCredentials() throws Exception {
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var borrowed = new java.util.concurrent.atomic.AtomicReference<char[]>();
        var server = new ModernServer(new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 0),
                serverContext, limits(2, 1), (account, password) -> {
                    borrowed.set(password);
                    entered.countDown();
                    assertTrue(release.await(5, TimeUnit.SECONDS));
                    return null;
                }, (principal, sourceId) -> { throw new AssertionError("no character lookup before auth"); });
        try (server; var socket = client(server)) {
            var auth = new ModernApplicationProtocol.Auth(2, "fixture", "owned-test-password".toCharArray());
            byte[] encoded = ModernApplicationProtocol.encode(auth);
            try { socket.getOutputStream().write(encoded); socket.getOutputStream().flush(); }
            finally { auth.close(); Arrays.fill(encoded, (byte) 0); }
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            server.close();
            await(() -> server.snapshot().sessions() == 0);
            assertFalse(server.awaitTermination(50), "closing sockets is not actual backend retirement");
            assertEquals(1, server.snapshot().backendActive());
            assertArrayEquals("owned-test-password".toCharArray(), borrowed.get(),
                    "running verifier keeps its own valid copy until it really exits");
            release.countDown();
            assertTrue(server.awaitTermination(3000));
            assertArrayEquals(new char[borrowed.get().length], borrowed.get());
            assertEquals(0, server.snapshot().backendActive());
        } finally {
            release.countDown();
            server.close();
            assertTrue(server.awaitTermination(3000));
        }
    }

    @Test void rapidReconnectDoesNotRefillAdmissionAndAddressTrackingIsBounded() throws Exception {
        var time = new AtomicLong();
        var admission = new ModernServer.Admission(limits(2, 1), time::get);
        var first = InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
        var second = InetAddress.getByAddress(new byte[]{127, 0, 0, 2});
        assertTrue(admission.reserve(first));
        assertFalse(admission.reserve(first), "concurrent per-IP limit");
        admission.release(first);
        assertTrue(admission.reserve(first)); admission.release(first);
        assertFalse(admission.reserve(first), "disconnect must not refill spent login tokens");
        assertFalse(admission.reserve(second), "a new address cannot bypass the global rate cap");
        time.addAndGet(TimeUnit.MILLISECONDS.toNanos(101));
        assertTrue(admission.reserve(first)); admission.release(first);
        for (int value = 2; value < 250; value++) {
            time.addAndGet(TimeUnit.SECONDS.toNanos(1));
            var address = InetAddress.getByAddress(new byte[]{127, 0, 0, (byte)value});
            assertTrue(admission.reserve(address)); admission.release(address);
            assertTrue(admission.addressCount() <= 16);
        }
    }
}
