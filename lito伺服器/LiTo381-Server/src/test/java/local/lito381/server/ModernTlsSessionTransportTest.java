package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ModernTlsSessionTransportTest {
    private static Path fixtures;
    private static SSLContext serverContext;
    private static SSLContext clientContext;
    private static final ModernTlsSessionTransport.Limits NORMAL = new ModernTlsSessionTransport.Limits(2_000, 2_000);
    private static final ModernAccountAuthentication.Principal ALICE = new ModernAccountAuthentication.Principal(
            "alice", Set.of(new ModernAccountAuthentication.Source(1, "Sun")));

    @BeforeAll static void certificates() throws Exception {
        fixtures = ModernTlsTestMaterial.defaultDirectory();
        ModernTlsTestMaterial.prepare(fixtures);
        serverContext = ModernTlsTestMaterial.serverContext(fixtures, false);
        clientContext = ModernTlsTestMaterial.clientContext(fixtures);
    }

    @Test void realTls12HelloAckNeverCallsCredentialVerifier() throws Exception {
        var calls = new AtomicInteger();
        try (var server = new RunningServer(serverContext, NORMAL, (account, password) -> {
            calls.incrementAndGet(); return ALICE;
        }, transport -> {
            var selected = transport.negotiate();
            assertEquals(0xfedcba98, selected.requestId());
            assertEquals(ModernTlsSessionTransport.State.NEGOTIATED, transport.state());
            assertTrue(transport.authenticatedPrincipal().isEmpty());
            assertEquals("TLSv1.2", transport.tlsInfo().orElseThrow().protocol());
        }); var client = client(server.port(), clientContext, "localhost")) {
            client.setEnabledProtocols(new String[] {"TLSv1.2"});
            exchange(client);
            server.success();
            assertEquals(0, calls.get());
        }
    }

    @Test void verifiedTlsTransportUsesSelectedVersionForEquipmentAndKeepsOtherFramesUnchanged() throws Exception {
        for (int maximum : new int[] {1, 2}) {
            var before = ModernEquipmentProtocolTest.snapshot();
            var source = new ModernCharacterData.Snapshot(1, before.source().character(), before.source().persisted(), before.source().inventory());
            var snapshot = new ModernEquipmentAuthority.Snapshot(source, before.revision(), before.viewToken(), before.catalogGeneration(),
                    before.evaluatedAtMillis(), before.nextStoneExpiry(), before.modifiers(), before.magicReductionBase(), before.equipped(),
                    before.stale(), java.util.List.of(new ModernItemLifetime.View(99, ModernItemLifetime.State.PERMANENT, -1)));
            try (var server = new RunningServer(serverContext, NORMAL, (account, password) -> ALICE, transport -> {
                assertEquals(maximum, transport.negotiate().version());
                assertEquals(ALICE, transport.authenticate("alice", "fixture-only".toCharArray()));
                var received = transport.readApplication(2_000);
                assertInstanceOf(ModernApplicationProtocol.EquipmentRequest.class, received);
                var request = (ModernApplicationProtocol.EquipmentRequest) received;
                transport.writeApplication(new ModernApplicationProtocol.EquipmentResult(request.requestId(), request.request(),
                        new ModernEquipmentAuthority.Result(ModernEquipmentAuthority.OK, false, 0, 0, snapshot)));
            }, true, ModernSessionBoundary.Policy.equipment()); var client = client(server.port(), clientContext, "localhost")) {
                client.startHandshake();
                client.getOutputStream().write(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(19, 1, maximum, 31, 7, 1)));
                client.getOutputStream().flush();
                var ack = (ModernSessionProtocol.Ack) ModernSessionProtocol.decode(ModernSessionProtocol.readFrame(client.getInputStream()));
                assertEquals(maximum, ack.version()); assertEquals(31, ack.capabilities());
                var request = new ModernEquipmentAuthority.Request(1, 3333, 1, ModernEquipmentAuthority.READ, false, 0, 0, ModernEquipmentAuthority.ZERO);
                var message = new ModernApplicationProtocol.EquipmentRequest(20, request);
                assertArrayEquals(ModernApplicationProtocol.encode(message), ModernApplicationProtocol.encode(message, maximum));
                client.getOutputStream().write(ModernApplicationProtocol.encode(message, maximum)); client.getOutputStream().flush();
                var bytes = ModernApplicationProtocol.readFrame(client.getInputStream());
                var result = (ModernApplicationProtocol.EquipmentResult) ModernApplicationProtocol.decode(bytes, maximum);
                assertEquals(request, result.echo());
                assertEquals(maximum == 1 ? 0 : 1, result.result().snapshot().lifetimes().size());
                assertEquals(snapshot.source(), result.result().snapshot().source());
                if (maximum == 2) assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(bytes));
                client.close(); server.success();
            }
        }
    }

    @Test void dispatchFailureIsRetainedBeforeAbortAndLaterTransportErrors() throws Exception {
        try (var server = new RunningServer(serverContext, NORMAL, (account, password) -> ALICE, transport -> {
            transport.noteApplicationFailure(new ModernApplicationProtocol.ProtocolException());
            transport.failClosed();
            assertThrows(IOException.class, () -> transport.readApplication(100));
            assertEquals(new ModernSessionFailures.Point(ModernSessionFailures.Stage.DISPATCH, ModernSessionFailures.Kind.PROTOCOL),
                    transport.failurePoint().orElseThrow());
        }); var raw = new Socket("127.0.0.1", server.port())) { server.success(); }
    }
    @Test void transportFailureCannotBeOverwrittenByApplicationUnwinding() throws Exception {
        try (var server = new RunningServer(serverContext, NORMAL, (account, password) -> ALICE, transport -> {
            assertThrows(IOException.class, () -> transport.readApplication(100));
            transport.noteApplicationFailure(new IllegalStateException("private diagnostic must not be retained"));
            assertEquals(new ModernSessionFailures.Point(ModernSessionFailures.Stage.APPLICATION_READ, ModernSessionFailures.Kind.IO),
                    transport.failurePoint().orElseThrow());
        }); var raw = new Socket("127.0.0.1", server.port())) { server.success(); }
    }
    @Test void credentialsBeforeTlsNeverReachVerifierAndAreCleared() throws Exception {
        var calls = new AtomicInteger();
        char[] password = "fixture-secret".toCharArray();
        try (var server = new RunningServer(serverContext, NORMAL, (account, secret) -> {
            calls.incrementAndGet(); return ALICE;
        }, transport -> {
            assertEquals(ModernAccountAuthentication.Failure.INVALID_STATE,
                    assertThrows(ModernAccountAuthentication.AuthenticationException.class,
                            () -> transport.authenticate("alice", password)).failure());
            assertEquals(ModernTlsSessionTransport.State.CLOSED, transport.state());
        }); var raw = new Socket("127.0.0.1", server.port())) {
            server.success();
            assertEquals(0, calls.get());
            assertArrayEquals(new char[password.length], password);
        }
    }

    @Test void backendAttachedAfterTransportClosedIsImmediatelyCancelled() throws Exception {
        var backend = new ModernBackendExecutor(1, 1);
        var calls = new AtomicInteger();
        try (var server = new RunningServer(serverContext, NORMAL, (account, secret) -> ALICE, transport -> {
            transport.close();
            var gate = backend.openSession();
            transport.attachBackend(gate, 1_000);
            assertEquals(ModernBackendExecutor.Failure.CLOSED,
                    assertThrows(ModernBackendExecutor.BackendException.class,
                            () -> gate.call(1_000, scope -> { calls.incrementAndGet(); return 1; })).failure());
        }, false); var raw = new Socket("127.0.0.1", server.port())) {
            server.success(); assertEquals(0, calls.get());
        } finally { backend.close(); assertTrue(backend.awaitTermination(3_000)); }
    }

    @Test void explicitAuthenticationSucceedsOnlyAfterTlsNegotiationAndCannotRepeat() throws Exception {
        var calls = new AtomicInteger();
        char[] password = "fixture-secret".toCharArray();
        try (var server = new RunningServer(serverContext, NORMAL, (account, secret) -> {
            calls.incrementAndGet(); assertEquals("fixture-secret", new String(secret)); return ALICE;
        }, transport -> {
            transport.negotiate();
            assertEquals(ALICE, transport.authenticate("alice", password));
            assertEquals(ModernTlsSessionTransport.State.AUTHENTICATED, transport.state());
            assertEquals(ALICE, transport.authenticatedPrincipal().orElseThrow());
            assertThrows(ModernAccountAuthentication.AuthenticationException.class,
                    () -> transport.authenticate("alice", "again".toCharArray()));
            assertEquals(ModernTlsSessionTransport.State.CLOSED, transport.state());
            assertTrue(transport.authenticatedPrincipal().isEmpty());
        }); var client = client(server.port(), clientContext, "localhost")) {
            exchange(client);
            server.success();
            assertEquals(1, calls.get());
            assertArrayEquals(new char[password.length], password);
        }
    }

    @Test void closeWhileBackendWorksPreventsLateAuthentication() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        char[] password = "fixture-secret".toCharArray();
        var workerSecret = new AtomicReference<char[]>();
        try (var server = new RunningServer(serverContext, NORMAL, (account, secret) -> {
            assertNotSame(password, secret, "worker must not borrow the caller's cleared AUTH array");
            workerSecret.set(secret);
            entered.countDown();
            if (!release.await(3, TimeUnit.SECONDS)) throw new IOException("test backend deadline");
            assertEquals("fixture-secret", new String(secret), "cancelled caller cannot erase a still-running verifier input");
            return ALICE;
        }, transport -> {
            transport.negotiate();
            assertEquals(ModernAccountAuthentication.Failure.INVALID_STATE,
                    assertThrows(ModernAccountAuthentication.AuthenticationException.class,
                            () -> transport.authenticate("alice", password)).failure());
            assertEquals(ModernTlsSessionTransport.State.CLOSED, transport.state());
            assertTrue(transport.authenticatedPrincipal().isEmpty());
        }); var client = client(server.port(), clientContext, "localhost")) {
            exchange(client);
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            assertArrayEquals(new char[password.length], password);
            server.transport.get().close();
            assertEquals("fixture-secret", new String(workerSecret.get()));
            release.countDown();
            server.success();
            assertArrayEquals(new char[password.length], password);
            long secretDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (!server.backendExecutor.isQuiescent()) {
                assertTrue(System.nanoTime() < secretDeadline, "backend really retires after release");
                Thread.sleep(1);
            }
            assertArrayEquals(new char[workerSecret.get().length], workerSecret.get());
        } finally {
            release.countDown();
        }
    }

    @Test void badChainHostnameAndExpiredLeafAreRejectedByRealClientVerification() throws Exception {
        for (String variant : new String[] {"untrusted", "hostname", "expired"}) {
            SSLContext selectedServer = variant.equals("expired")
                    ? ModernTlsTestMaterial.serverContext(fixtures, true) : serverContext;
            SSLContext selectedClient = variant.equals("untrusted") ? SSLContext.getDefault() : clientContext;
            try (var server = new RunningServer(selectedServer, NORMAL, (account, secret) -> ALICE,
                    transport -> transport.negotiate());
                 var client = client(server.port(), selectedClient, variant.equals("hostname") ? "wrong.invalid" : "localhost")) {
                assertThrows(SSLException.class, client::startHandshake, variant);
                assertInstanceOf(IOException.class, server.failure());
                assertEquals(ModernTlsSessionTransport.State.CLOSED, server.transport.get().state());
            }
        }
    }

    @Test void unknownCapabilityOverTlsClosesWithoutAuthenticating() throws Exception {
        var calls = new AtomicInteger();
        try (var server = new RunningServer(serverContext, NORMAL, (account, secret) -> {
            calls.incrementAndGet(); return ALICE;
        }, transport -> transport.negotiate()); var client = client(server.port(), clientContext, "localhost")) {
            client.startHandshake();
            byte[] frame = hello();
            frame[20] = (byte) 0x80;
            client.getOutputStream().write(frame);
            client.getOutputStream().flush();
            assertInstanceOf(ModernSessionProtocol.ProtocolException.class, server.failure());
            assertEquals(0, calls.get());
            assertEquals(ModernTlsSessionTransport.State.CLOSED, server.transport.get().state());
            assertFalse(server.transport.get().closeDeadlineExceeded(), "fatal protocol errors must abort immediately");
        }
    }

    @Test void gracefulCloseHasAbsoluteDeadlineWhenPeerNeverReadsOrRepliesCloseNotify() throws Exception {
        var beganClose = new CountDownLatch(1);
        try (var server = new RunningServer(serverContext, NORMAL, (account, secret) -> ALICE, transport -> {
            transport.negotiate();
            transport.authenticate("alice", "fixture-close-secret".toCharArray());
            assertTrue(transport.authenticatedPrincipal().isPresent());
            long started = System.nanoTime();
            beganClose.countDown();
            transport.close();
            assertTrue(transport.closeDeadlineExceeded(), "silent peer must trigger the close watchdog");
            assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) < 1_500,
                    "close must not inherit the 2000 ms idle timeout");
            assertEquals(ModernTlsSessionTransport.State.CLOSED, transport.state());
            assertTrue(transport.authenticatedPrincipal().isEmpty());
            transport.close(); // Idempotent: no second network wait.
        }); var client = client(server.port(), clientContext, "localhost")) {
            client.setEnabledProtocols(new String[]{"TLSv1.3"});
            exchange(client);
            assertTrue(beganClose.await(2, TimeUnit.SECONDS));
            // Deliberately neither read the server's close_notify nor send one.
            server.success();
        }
    }

    @Test void slowlySentPartialEncryptedRecordCannotPreventBoundedClose() throws Exception {
        var beganClose = new CountDownLatch(1);
        var partialRecordStarted = new CountDownLatch(1);
        try (var server = new RunningServer(serverContext, NORMAL, (account, secret) -> ALICE, transport -> {
            transport.negotiate();
            assertTrue(partialRecordStarted.await(2, TimeUnit.SECONDS));
            long started = System.nanoTime();
            beganClose.countDown();
            transport.close();
            // JSSE may drain already-available bytes and close immediately.
            // That is valid; the separate silent-peer test proves the watchdog
            // actually fires on a waiting close, not merely that this is fast.
            assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) < 1_500);
            assertEquals(ModernTlsSessionTransport.State.CLOSED, transport.state());
            assertTrue(transport.authenticatedPrincipal().isEmpty());
        }); var raw = new Socket("127.0.0.1", server.port());
             var client = (SSLSocket) clientContext.getSocketFactory().createSocket(raw, "localhost", server.port(), true)) {
            var parameters = client.getSSLParameters();
            parameters.setEndpointIdentificationAlgorithm("HTTPS");
            client.setSSLParameters(parameters);
            client.setEnabledProtocols(new String[]{"TLSv1.3"});
            client.setSoTimeout(2_000);
            exchange(client);
            // After verified TLS/HELLO, the adversarial peer starts but never
            // finishes an encrypted TLS record. It is not a plaintext ingress.
            raw.getOutputStream().write(new byte[]{0x17, 0x03, 0x03, 0, 64});
            raw.getOutputStream().flush();
            raw.getOutputStream().write(0);
            raw.getOutputStream().flush();
            Thread.sleep(40);
            raw.getOutputStream().write(0);
            raw.getOutputStream().flush();
            partialRecordStarted.countDown();
            assertTrue(beganClose.await(2, TimeUnit.SECONDS));
            int sent = 2;
            while (!server.future.isDone() && sent < 20) {
                try { raw.getOutputStream().write(0); raw.getOutputStream().flush(); sent++; }
                catch (IOException expected) { break; }
                Thread.sleep(40); // Each partial read is well below SO_TIMEOUT.
            }
            server.success();
            assertTrue(sent >= 2, "fixture must actually send a slow partial record");
        }
    }

    @Test void slowDripApplicationFrameCannotResetTheWholeOperationDeadline() throws Exception {
        try (var server = new RunningServer(serverContext, new ModernTlsSessionTransport.Limits(2_000, 300),
                (account, secret) -> ALICE, transport -> transport.negotiate());
             var client = client(server.port(), clientContext, "localhost")) {
            client.startHandshake();
            long started = System.nanoTime();
            for (byte value : hello()) {
                if (server.future.isDone()) break;
                try {
                    client.getOutputStream().write(value & 0xff);
                    client.getOutputStream().flush();
                } catch (IOException expected) { break; }
                Thread.sleep(75); // Less than SO_TIMEOUT, but total frame time exceeds 300 ms.
            }
            assertInstanceOf(IOException.class, server.failure());
            assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) < 2_000);
            assertEquals(ModernTlsSessionTransport.State.CLOSED, server.transport.get().state());
        }
    }

    @Test void idleApplicationTimeoutRetainsTimeoutCauseBeforeSocketAbort() throws Exception {
        try (var server = new RunningServer(serverContext, NORMAL, (account, secret) -> ALICE, transport -> {
            transport.negotiate();
            transport.readApplication(200);
        }); var client = client(server.port(), clientContext, "localhost")) {
            exchange(client);
            assertInstanceOf(IOException.class, server.failure());
            assertEquals(new ModernSessionFailures.Point(ModernSessionFailures.Stage.APPLICATION_READ,
                            ModernSessionFailures.Kind.TIMEOUT), server.transport.get().failurePoint().orElseThrow());
            assertEquals(ModernTlsSessionTransport.State.CLOSED, server.transport.get().state());
        }
    }

    @Test void partialTlsRecordSlowDripCannotKeepHandshakeAlive() throws Exception {
        try (var server = new RunningServer(serverContext, new ModernTlsSessionTransport.Limits(300, 2_000),
                (account, secret) -> ALICE, transport -> transport.negotiate());
             var raw = new Socket("127.0.0.1", server.port())) {
            long started = System.nanoTime();
            raw.getOutputStream().write(new byte[] {0x16, 0x03, 0x03, 0x01, 0x00});
            for (int count = 0; count < 24 && !server.future.isDone(); count++) {
                try { raw.getOutputStream().write(0); raw.getOutputStream().flush(); }
                catch (IOException expected) { break; }
                Thread.sleep(75);
            }
            assertInstanceOf(IOException.class, server.failure());
            assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) < 2_000);
            assertEquals(ModernTlsSessionTransport.State.CLOSED, server.transport.get().state());
        }
    }

    private static byte[] hello() throws Exception {
        return ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(0xfedcba98, 1, 1, 3, 3, 1));
    }
    private static void exchange(SSLSocket client) throws Exception {
        client.startHandshake();
        client.getOutputStream().write(hello());
        client.getOutputStream().flush();
        assertEquals(new ModernSessionProtocol.Ack(0xfedcba98, 1, 1, 3),
                ModernSessionProtocol.decode(ModernSessionProtocol.readFrame(client.getInputStream())));
    }
    private static SSLSocket client(int port, SSLContext context, String name) throws Exception {
        Socket raw = new Socket();
        raw.connect(new InetSocketAddress("127.0.0.1", port), 2_000);
        SSLSocket socket = (SSLSocket) context.getSocketFactory().createSocket(raw, name, port, true);
        var parameters = socket.getSSLParameters();
        parameters.setEndpointIdentificationAlgorithm("HTTPS");
        socket.setSSLParameters(parameters);
        socket.setSoTimeout(2_000);
        return socket;
    }
    @FunctionalInterface private interface ServerAction { void run(ModernTlsSessionTransport transport) throws Exception; }
    private static final class RunningServer implements AutoCloseable {
        final SSLServerSocket listener;
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final ModernBackendExecutor backendExecutor = new ModernBackendExecutor(2, 2);
        final AtomicReference<ModernTlsSessionTransport> transport = new AtomicReference<>();
        final Future<?> future;
        RunningServer(SSLContext context, ModernTlsSessionTransport.Limits limits,
                      ModernAccountAuthentication.CredentialVerifier verifier, ServerAction action) throws Exception {
            this(context, limits, verifier, action, true);
        }
        RunningServer(SSLContext context, ModernTlsSessionTransport.Limits limits,
                      ModernAccountAuthentication.CredentialVerifier verifier, ServerAction action,
                      boolean attachBackend) throws Exception {
            this(context, limits, verifier, action, attachBackend, ModernSessionBoundary.Policy.current());
        }
        RunningServer(SSLContext context, ModernTlsSessionTransport.Limits limits,
                      ModernAccountAuthentication.CredentialVerifier verifier, ServerAction action,
                      boolean attachBackend, ModernSessionBoundary.Policy policy) throws Exception {
            listener = (SSLServerSocket) context.getServerSocketFactory().createServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
            listener.setSoTimeout(3_000);
            future = executor.submit(() -> {
                try (var accepted = (SSLSocket) listener.accept();
                     var connection = new ModernTlsSessionTransport(accepted,
                             policy, limits, verifier)) {
                    transport.set(connection);
                    if (attachBackend) connection.attachBackend(backendExecutor.openSession(), 2_000);
                    action.run(connection);
                }
                return null;
            });
        }
        int port() { return listener.getLocalPort(); }
        void success() throws Exception { future.get(5, TimeUnit.SECONDS); }
        Throwable failure() throws Exception {
            try { future.get(5, TimeUnit.SECONDS); fail("server unexpectedly succeeded"); return null; }
            catch (ExecutionException failure) { return failure.getCause(); }
        }
        @Override public void close() throws Exception {
            listener.close();
            if (transport.get() != null) transport.get().close();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS), "server test worker leaked");
            backendExecutor.close();
            assertTrue(backendExecutor.awaitTermination(3_000), "backend test worker leaked");
        }
    }
}
