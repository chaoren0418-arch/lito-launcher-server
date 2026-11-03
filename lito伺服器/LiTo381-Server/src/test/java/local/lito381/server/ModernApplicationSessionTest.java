package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ModernApplicationSessionTest {
    private static SSLContext serverContext;
    private static SSLContext clientContext;
    @BeforeAll static void certificates() throws Exception {
        Path fixtures = ModernTlsTestMaterial.defaultDirectory();
        ModernTlsTestMaterial.prepare(fixtures);
        serverContext = ModernTlsTestMaterial.serverContext(fixtures, false);
        clientContext = ModernTlsTestMaterial.clientContext(fixtures);
    }

    @Test void actualTlsAuthenticationAndSwitchingSourcesKeepsSameObjectIdsSeparate() throws Exception {
        var backend = new ModernApplicationTestBackend("auth");
        try (var server = new RunningServer(backend, backend.characters, 10_000); var client = client(server.port())) {
            hello(client, 1);
            assertEquals(new ModernApplicationProtocol.Pong(2), exchange(client, new ModernApplicationProtocol.Ping(2)));
            assertEquals(0, backend.authenticationCalls.get());
            assertTrue(server.transport.get().authenticatedPrincipal().isEmpty());
            var authentication = login(client, 3, ModernApplicationTestBackend.PASSWORD);
            assertEquals(0, authentication.status());
            assertEquals(ModernApplicationTestBackend.SUN_ACCOUNT, authentication.account());
            assertEquals(List.of(new ModernApplicationProtocol.Source(7, "太陽神"), new ModernApplicationProtocol.Source(42, "月亮")), authentication.sources());
            assertArrayEquals(new char[backend.borrowedPassword.get().length], backend.borrowedPassword.get());
            var sun = (ModernApplicationProtocol.CharacterResult) exchange(client, new ModernApplicationProtocol.CharacterRequest(4, 7, -1L));
            var moon = (ModernApplicationProtocol.CharacterResult) exchange(client, new ModernApplicationProtocol.CharacterRequest(5, 42, 2));
            var sunAgain = (ModernApplicationProtocol.CharacterResult) exchange(client, new ModernApplicationProtocol.CharacterRequest(6, 7, 3));
            assertEquals(-1L, sun.selectionEpoch());
            assertEquals("Sun Knight", sun.characters().get(0).name());
            assertEquals("Moon Mage", moon.characters().get(0).name());
            assertEquals(120_000L, moon.characters().get(0).level());
            assertEquals(3333L, sun.characters().get(0).objectId());
            assertEquals(3333L, moon.characters().get(0).objectId());
            assertEquals(sun.characters(), sunAgain.characters());
            assertEquals(3, backend.characterCalls.get());
            assertEquals(new ModernApplicationProtocol.Pong(7), exchange(client, new ModernApplicationProtocol.Ping(7)));
            client.close(); server.success();
        }
    }

    @Test void deniedSourceDoesNotQueryAnyRepositoryAndAuthenticatedSessionSurvives() throws Exception {
        var backend = new ModernApplicationTestBackend("auth");
        try (var server = new RunningServer(backend, backend.characters, 10_000); var client = client(server.port())) {
            hello(client, 1); login(client, 2, ModernApplicationTestBackend.PASSWORD);
            assertEquals(new ModernApplicationProtocol.CharacterResult(3, 99, 10, 1, List.of()),
                    exchange(client, new ModernApplicationProtocol.CharacterRequest(3, 99, 10)));
            assertEquals(0, backend.characterCalls.get());
            assertEquals(ModernTlsSessionTransport.State.AUTHENTICATED, server.transport.get().state());
            assertEquals(new ModernApplicationProtocol.Pong(4), exchange(client, new ModernApplicationProtocol.Ping(4)));
            client.close(); server.success();
        }
    }

    @Test void wrongPasswordAndBackendFailureReturnGenericFailureAndClose() throws Exception {
        for (String mode : List.of("auth", "auth-backend")) {
            var backend = new ModernApplicationTestBackend(mode);
            try (var server = new RunningServer(backend, backend.characters, 10_000); var client = client(server.port())) {
                hello(client, 1);
                var result = login(client, 2, "wrong-fixture-password");
                assertEquals(mode.equals("auth") ? 1 : 2, result.status());
                assertEquals("", result.account()); assertTrue(result.sources().isEmpty());
                assertEquals(-1, client.getInputStream().read());
                server.success();
                assertArrayEquals(new char[backend.borrowedPassword.get().length], backend.borrowedPassword.get());
                assertTrue(server.transport.get().authenticatedPrincipal().isEmpty());
            }
        }
    }

    @Test void backendFailureAndInvalidRowsCannotLeakPartialCharacterList() throws Exception {
        var backend = new ModernApplicationTestBackend("characters-backend");
        List<ModernCharacterRepository> repositories = List.of(backend.characters,
                (principal, source) -> List.of(new ModernApplicationProtocol.CharacterRow(0, "bad", 1, 0, 80)),
                (principal, source) -> List.of(new ModernApplicationProtocol.CharacterRow(1, "first", 1, 0, 80),
                        new ModernApplicationProtocol.CharacterRow(1, "duplicate", 1, 0, 80)));
        for (var repository : repositories) {
            try (var server = new RunningServer(backend, repository, 10_000); var client = client(server.port())) {
                hello(client, 1); login(client, 2, ModernApplicationTestBackend.PASSWORD);
                assertEquals(new ModernApplicationProtocol.CharacterResult(3, 7, 1, 2, List.of()),
                        exchange(client, new ModernApplicationProtocol.CharacterRequest(3, 7, 1)));
                assertEquals(new ModernApplicationProtocol.Pong(4), exchange(client, new ModernApplicationProtocol.Ping(4)));
                client.close(); server.success();
            }
        }
    }

    @Test void characterRequestBeforeAuthenticationAndWrongDirectionFailClosed() throws Exception {
        for (var message : List.of(new ModernApplicationProtocol.CharacterRequest(2, 7, 1),
                new ModernApplicationProtocol.Pong(2), new ModernApplicationProtocol.AuthResult(2, 1, "", List.of()))) {
            var backend = new ModernApplicationTestBackend("auth");
            try (var server = new RunningServer(backend, backend.characters, 10_000); var client = client(server.port())) {
                hello(client, 1); send(client, message);
                assertInstanceOf(ModernApplicationProtocol.ProtocolException.class, server.failure());
                assertEquals(0, backend.characterCalls.get());
                assertEquals(0, backend.authenticationCalls.get());
                assertEquals(ModernTlsSessionTransport.State.CLOSED, server.transport.get().state());
            }
        }
    }

    @Test void duplicateOrWrappedRequestIdAndSecondAuthFailClosed() throws Exception {
        var backend = new ModernApplicationTestBackend("auth");
        for (int kind = 0; kind < 3; kind++) {
            try (var server = new RunningServer(backend, backend.characters, 10_000); var client = client(server.port())) {
                if (kind == 2) { hello(client, -1); send(client, new ModernApplicationProtocol.Ping(1)); }
                else {
                    hello(client, 1); login(client, 2, ModernApplicationTestBackend.PASSWORD);
                    if (kind == 0) send(client, new ModernApplicationProtocol.Ping(2));
                    else try (var auth = new ModernApplicationProtocol.Auth(3, "fixture-user", "fixture-password".toCharArray())) { send(client, auth); }
                }
                assertInstanceOf(ModernApplicationProtocol.ProtocolException.class, server.failure());
                assertEquals(ModernTlsSessionTransport.State.CLOSED, server.transport.get().state());
            }
        }
    }

    @Test void applicationSlowDripCannotExtendWholeFrameDeadline() throws Exception {
        var backend = new ModernApplicationTestBackend("auth");
        try (var server = new RunningServer(backend, backend.characters, 300); var client = client(server.port())) {
            hello(client, 1);
            byte[] ping = ModernApplicationProtocol.encode(new ModernApplicationProtocol.Ping(2));
            long start = System.nanoTime();
            for (int index = 0; index < 5; index++) {
                try { client.getOutputStream().write(ping[index]); client.getOutputStream().flush(); }
                catch (IOException expected) { break; }
                Thread.sleep(100);
            }
            assertInstanceOf(IOException.class, server.failure());
            assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 2_000);
            assertEquals(0, backend.authenticationCalls.get());
        }
    }

    @Test void closeDuringCharacterBackendPreventsLateResponseAndErasesPrincipal() throws Exception {
        var backend = new ModernApplicationTestBackend("auth");
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        ModernCharacterRepository delayed = (principal, source) -> {
            entered.countDown();
            if (!release.await(3, TimeUnit.SECONDS)) throw new IOException("fixture backend deadline");
            return backend.characters.characters(principal, source);
        };
        try (var server = new RunningServer(backend, delayed, 10_000); var client = client(server.port())) {
            hello(client, 1); login(client, 2, ModernApplicationTestBackend.PASSWORD);
            send(client, new ModernApplicationProtocol.CharacterRequest(3, 7, 1));
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            server.transport.get().close(); release.countDown();
            assertInstanceOf(IOException.class, server.failure());
            assertTrue(server.transport.get().authenticatedPrincipal().isEmpty());
            assertEquals(-1, client.getInputStream().read());
        } finally { release.countDown(); }
    }

    private static ModernApplicationProtocol.AuthResult login(SSLSocket client, int requestId, String password) throws Exception {
        try (var auth = new ModernApplicationProtocol.Auth(requestId, ModernApplicationTestBackend.ACCOUNT, password.toCharArray())) {
            return (ModernApplicationProtocol.AuthResult) exchange(client, auth);
        }
    }
    private static SSLSocket client(int port) throws Exception {
        Socket raw = new Socket("127.0.0.1", port);
        var client = (SSLSocket) clientContext.getSocketFactory().createSocket(raw, "localhost", port, true);
        var parameters = client.getSSLParameters(); parameters.setEndpointIdentificationAlgorithm("HTTPS");
        client.setSSLParameters(parameters); client.setSoTimeout(3_000);
        return client;
    }
    private static void hello(SSLSocket socket, int requestId) throws Exception {
        socket.startHandshake();
        byte[] hello = ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(requestId, 1, 1, 3, 3, 1));
        socket.getOutputStream().write(hello); socket.getOutputStream().flush();
        var ack = (ModernSessionProtocol.Ack) ModernSessionProtocol.decode(ModernSessionProtocol.readFrame(socket.getInputStream()));
        assertEquals(requestId, ack.requestId());
    }
    private static void send(SSLSocket socket, ModernApplicationProtocol.Message message) throws Exception {
        byte[] frame = ModernApplicationProtocol.encode(message);
        try { socket.getOutputStream().write(frame); socket.getOutputStream().flush(); }
        finally { Arrays.fill(frame, (byte) 0); }
    }
    private static ModernApplicationProtocol.Message exchange(SSLSocket socket, ModernApplicationProtocol.Message message) throws Exception {
        send(socket, message);
        byte[] frame = ModernApplicationProtocol.readFrame(socket.getInputStream());
        try { return ModernApplicationProtocol.decode(frame); }
        finally { if (frame != null) Arrays.fill(frame, (byte) 0); }
    }
    private static final class RunningServer implements AutoCloseable {
        final SSLServerSocket listener;
        final java.util.concurrent.ExecutorService executor = Executors.newSingleThreadExecutor();
        final ModernBackendExecutor backendExecutor = new ModernBackendExecutor(2, 2);
        final TournamentPresence presence = new TournamentPresence();
        final java.util.concurrent.Future<?> future;
        final AtomicReference<ModernTlsSessionTransport> transport = new AtomicReference<>();
        RunningServer(ModernApplicationTestBackend backend, ModernCharacterRepository repository, int timeout) throws Exception {
            listener = (SSLServerSocket) serverContext.getServerSocketFactory().createServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
            listener.setSoTimeout(3_000);
            future = executor.submit(() -> {
                try (var socket = (SSLSocket) listener.accept()) {
                    var peer = new ModernTlsSessionTransport(socket, ModernSessionBoundary.Policy.current(),
                            new ModernTlsSessionTransport.Limits(2_000, 2_000), backend.verifier);
                    transport.set(peer);
                    new ModernApplicationSession(peer, repository, backendExecutor, timeout, 2_000, presence).run();
                    return null;
                }
            });
        }
        int port() { return listener.getLocalPort(); }
        void success() throws Exception { future.get(6, TimeUnit.SECONDS); }
        Throwable failure() throws Exception {
            return assertThrows(ExecutionException.class, () -> future.get(6, TimeUnit.SECONDS)).getCause();
        }
        @Override public void close() throws Exception {
            listener.close();
            if (transport.get() != null) transport.get().close();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(3, TimeUnit.SECONDS));
            backendExecutor.close();
            assertTrue(backendExecutor.awaitTermination(3_000), "real backend workers must retire");
        }
    }
}
