package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ModernCharacterDataSessionTest {
    private static SSLContext serverContext, clientContext;
    @BeforeAll static void certificates() throws Exception {
        Path material = ModernTlsTestMaterial.defaultDirectory(); ModernTlsTestMaterial.prepare(material);
        serverContext = ModernTlsTestMaterial.serverContext(material, false);
        clientContext = ModernTlsTestMaterial.clientContext(material);
    }

    private static ModernCharacterData.Snapshot snapshot(int source, long object) {
        var row = new ModernApplicationProtocol.CharacterRow(object, source == 7 ? "Sun Knight" : "Moon Mage",
                source == 7 ? 1 : 3, source == 7 ? 0 : 1, source == 7 ? 80 : 120_000);
        var inventory = ModernCharacterDataProtocolTest.snapshot(source, object).inventory();
        return new ModernCharacterData.Snapshot(source, row, ModernCharacterDataProtocolTest.persisted(), inventory);
    }
    private static void verifyPrincipal(ModernAccountAuthentication.Principal principal, int source) {
        assertTrue(principal.authorizesSource(source));
        assertEquals(source == 7 ? "sun-owner" : "moon-owner", principal.accountForSource(source));
    }

    @Test void dataReadsPreserveChannelsLobbyAndSelectedRoleAcrossExactSourceCanonicalAccounts() throws Exception {
        var fixture = new ModernApplicationTestBackend("auth"); var presence = new TournamentPresence();
        var calls = new AtomicInteger();
        ModernCharacterDataRepository repository = (principal, source, object) -> {
            verifyPrincipal(principal, source); calls.incrementAndGet(); return Optional.of(snapshot(source, object));
        };
        try (var server = new Peer(fixture, repository, presence); var client = client(server.port())) {
            hello(client, 15); login(client); roster(client, 3, 7, -1);
            var selected = nav(client, new ModernApplicationProtocol.CharacterSelection(4, 7, 3333, -1));
            var first = data(client, 5, 7, 3333, -1);
            assertEquals(0, first.status()); assertEquals(snapshot(7, 3333), first.snapshot());
            assertEquals(0, presence.userCount()); assertRoleHeld(presence, 7);
            assertEquals(2, fixture.characterCalls.get(), "data reads must not repeat or replace the selected roster");
            var joined = nav(client, new ModernApplicationProtocol.ChannelEnter(6, 7, 3333, -1, 2));
            assertEquals(2, joined.phase()); assertEquals(1, presence.userCount());
            var beforeData = presence.snapshots();
            assertEquals(first.snapshot(), data(client, 7, 7, 3333, -1).snapshot());
            assertEquals(beforeData, presence.snapshots()); assertRoleHeld(presence, 7);
            var left = nav(client, new ModernApplicationProtocol.LobbyLeave(8, 7, 3333, -1));
            assertEquals(selected.selected(), left.selected()); assertEquals(1, left.phase());
            assertEquals(first.snapshot(), data(client, 9, 7, 3333, -1).snapshot());
            assertEquals(0, presence.userCount()); assertRoleHeld(presence, 7);
            nav(client, new ModernApplicationProtocol.CharacterReturn(10, 7, 3333, -1));
            roster(client, 11, 42, 2); nav(client, new ModernApplicationProtocol.CharacterSelection(12, 42, 3333, 2));
            var moon = data(client, 13, 42, 3333, 2);
            assertEquals(0, moon.status()); assertEquals(snapshot(42, 3333), moon.snapshot());
            assertNotEquals(first.snapshot().sourceId(), moon.snapshot().sourceId());
            assertEquals(first.snapshot().character().objectId(), moon.snapshot().character().objectId());
            assertEquals(4, calls.get()); assertEquals(4, fixture.characterCalls.get());
            assertEquals(new ModernApplicationProtocol.Pong(14), exchange(client, new ModernApplicationProtocol.Ping(14)));
            client.close(); server.success();
        }
        assertEquals(0, presence.ownerCount()); assertEquals(0, presence.userCount());
    }

    @Test void unselectedUnauthenticatedReturnedAndMismatchedContextsFailBeforeDataRepository() throws Exception {
        for (int scenario = 0; scenario < 8; scenario++) {
            var fixture = new ModernApplicationTestBackend("auth"); var presence = new TournamentPresence();
            var calls = new AtomicInteger();
            ModernCharacterDataRepository repository = (principal, source, object) -> {
                calls.incrementAndGet(); return Optional.of(snapshot(source, object));
            };
            try (var server = new Peer(fixture, repository, presence); var client = client(server.port())) {
                hello(client, 15); int request = 2;
                if (scenario != 0) { login(client); request = 3; roster(client, request++, 7, 1); }
                if (scenario >= 2) nav(client, new ModernApplicationProtocol.CharacterSelection(request++, 7, 3333, 1));
                if (scenario == 6 || scenario == 7) {
                    nav(client, new ModernApplicationProtocol.CharacterReturn(request++, 7, 3333, 1));
                    if (scenario == 7) {
                        roster(client, request++, 7, 2);
                        nav(client, new ModernApplicationProtocol.CharacterSelection(request++, 7, 3333, 2));
                    }
                }
                int source = scenario == 2 ? 42 : scenario == 5 ? 99 : 7;
                send(client, new ModernApplicationProtocol.CharacterDataRequest(request, source,
                        scenario == 3 ? 3334 : 3333, scenario == 4 ? 2 : 1));
                assertInstanceOf(ModernApplicationProtocol.ProtocolException.class, server.failure());
                assertEquals(0, calls.get()); assertEquals(0, presence.ownerCount()); assertEquals(0, presence.userCount());
            }
        }
    }

    @Test void actualCapabilityAndMonotonicRequestIdAreRequiredEvenForSelectedCharacters() throws Exception {
        for (boolean missingCapability : List.of(true, false)) {
            var fixture = new ModernApplicationTestBackend("auth"); var presence = new TournamentPresence();
            var calls = new AtomicInteger();
            ModernCharacterDataRepository repository = (principal, source, object) -> {
                calls.incrementAndGet(); return Optional.of(snapshot(source, object));
            };
            try (var server = new Peer(fixture, repository, presence); var client = client(server.port())) {
                hello(client, missingCapability ? 7 : 15); login(client); roster(client, 3, 7, 1);
                nav(client, new ModernApplicationProtocol.CharacterSelection(4, 7, 3333, 1));
                if (!missingCapability) assertEquals(0, data(client, 5, 7, 3333, 1).status());
                send(client, new ModernApplicationProtocol.CharacterDataRequest(5, 7, 3333, 1));
                assertInstanceOf(ModernApplicationProtocol.ProtocolException.class, server.failure());
                assertEquals(missingCapability ? 0 : 1, calls.get()); assertEquals(0, presence.ownerCount());
            }
        }
    }

    @Test void absentUnavailableBusyAndMismatchedBackendSnapshotsStayGenericAndPreserveSelection() throws Exception {
        for (int scenario = 0; scenario < 6; scenario++) {
            final int mode = scenario;
            var fixture = new ModernApplicationTestBackend("auth"); var presence = new TournamentPresence();
            ModernCharacterDataRepository repository = (principal, source, object) -> {
                verifyPrincipal(principal, source);
                return switch (mode) {
                    case 0 -> Optional.empty();
                    case 1 -> throw new IOException("synthetic private backend information");
                    case 2 -> Optional.of(snapshot(42, object));
                    case 3 -> Optional.of(snapshot(source, object + 1));
                    case 4 -> throw new ModernBackendExecutor.BackendException(ModernBackendExecutor.Failure.BUSY);
                    default -> null;
                };
            };
            try (var server = new Peer(fixture, repository, presence); var client = client(server.port())) {
                hello(client, 15); login(client); roster(client, 3, 7, 1);
                var selected = nav(client, new ModernApplicationProtocol.CharacterSelection(4, 7, 3333, 1));
                nav(client, new ModernApplicationProtocol.ChannelEnter(5, 7, 3333, 1, 0));
                var beforeData = presence.snapshots();
                var failed = data(client, 6, 7, 3333, 1);
                assertEquals(mode == 0 ? 1 : mode == 4 ? 3 : 2, failed.status()); assertNull(failed.snapshot());
                assertEquals(beforeData, presence.snapshots()); assertRoleHeld(presence, 7);
                assertEquals(new ModernApplicationProtocol.Pong(7), exchange(client, new ModernApplicationProtocol.Ping(7)));
                var left = nav(client, new ModernApplicationProtocol.LobbyLeave(8, 7, 3333, 1));
                assertEquals(selected.selected(), left.selected());
                nav(client, new ModernApplicationProtocol.CharacterReturn(9, 7, 3333, 1));
                client.close(); server.success();
            }
        }
    }

    @Test void actualBoundedExecutorSaturationReturnsBusyWithoutCallingDataAndAllowsRetry() throws Exception {
        var fixture = new ModernApplicationTestBackend("auth"); var presence = new TournamentPresence();
        var dataCalls = new AtomicInteger();
        ModernCharacterDataRepository repository = (principal, source, object) -> {
            dataCalls.incrementAndGet(); return Optional.of(snapshot(source, object));
        };
        var release = new CountDownLatch(1); var entered = new CountDownLatch(1);
        var callers = Executors.newFixedThreadPool(2);
        try (var server = new Peer(fixture, repository, presence); var client = client(server.port());
             var firstGate = server.backend.openSession(); var queuedGate = server.backend.openSession()) {
            try {
                hello(client, 15); login(client); roster(client, 3, 7, 1);
                nav(client, new ModernApplicationProtocol.CharacterSelection(4, 7, 3333, 1));
                var running = callers.submit(() -> firstGate.call(5000, scope -> {
                    entered.countDown(); awaitRelease(release); return 1;
                }));
                assertTrue(entered.await(2, TimeUnit.SECONDS));
                var queued = callers.submit(() -> queuedGate.call(5000, scope -> { awaitRelease(release); return 2; }));
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
                while (server.backend.queuedCount() != 1) {
                    assertTrue(System.nanoTime() < deadline, "second owned operation must actually be queued");
                    Thread.sleep(1);
                }
                var busy = data(client, 5, 7, 3333, 1);
                assertEquals(3, busy.status()); assertNull(busy.snapshot()); assertEquals(0, dataCalls.get());
                assertRoleHeld(presence, 7); assertEquals(0, presence.userCount());
                release.countDown(); assertEquals(1, running.get(3, TimeUnit.SECONDS)); assertEquals(2, queued.get(3, TimeUnit.SECONDS));
                assertEquals(0, data(client, 6, 7, 3333, 1).status()); assertEquals(1, dataCalls.get());
                client.close(); server.success();
            } finally { release.countDown(); }
        } finally {
            callers.shutdownNow(); assertTrue(callers.awaitTermination(3, TimeUnit.SECONDS));
        }
    }

    @Test void closingWhileDataIsInFlightRejectsLateSnapshotAndActuallyReleasesRoleAndWorkers() throws Exception {
        var fixture = new ModernApplicationTestBackend("auth"); var presence = new TournamentPresence();
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1); var returned = new AtomicBoolean();
        ModernCharacterDataRepository repository = (principal, source, object) -> {
            var snapshot = snapshot(source, object); entered.countDown(); awaitRelease(release);
            returned.set(true); return Optional.of(snapshot);
        };
        try (var server = new Peer(fixture, repository, presence); var client = client(server.port())) {
            try {
                hello(client, 15); login(client); roster(client, 3, 7, 1);
                nav(client, new ModernApplicationProtocol.CharacterSelection(4, 7, 3333, 1));
                nav(client, new ModernApplicationProtocol.ChannelEnter(5, 7, 3333, 1, 2));
                send(client, new ModernApplicationProtocol.CharacterDataRequest(6, 7, 3333, 1));
                assertTrue(entered.await(2, TimeUnit.SECONDS)); assertRoleHeld(presence, 7);
                server.transport.get().failClosed();
                assertEquals(0, presence.ownerCount()); assertEquals(0, presence.userCount());
                assertTrue(server.transport.get().authenticatedPrincipal().isEmpty());
                assertFalse(server.backend.isQuiescent(), "cancellation is not proof that the blocked backend exited");
                assertInstanceOf(IOException.class, server.failure());
                release.countDown(); server.backend.close(); assertTrue(server.backend.awaitTermination(3000));
                assertTrue(returned.get(), "fixture must actually return its already-materialized late snapshot");
                assertTrue(server.backend.isQuiescent());
                try {
                    assertNull(ModernApplicationProtocol.readFrame(client.getInputStream()), "closed connection cannot publish late data");
                } catch (java.net.SocketTimeoutException timeout) {
                    fail("an unresponsive live socket is not evidence of cancellation", timeout);
                } catch (IOException closedTransport) { /* A TLS/TCP abort is also an expected closed outcome. */ }
                var contender = presence.connect();
                try { assertEquals(TournamentPresence.SelectStatus.ACCEPTED, presence.select(contender, choice(7))); }
                finally { presence.disconnect(contender); }
            } finally { release.countDown(); }
        }
    }

    private static TournamentPresence.Selection choice(int source) {
        var row = snapshot(source, 3333).character();
        return new TournamentPresence.Selection(new TournamentPresence.CharacterKey(source, row.objectId()),
                source == 7 ? "太陽神" : "月亮", row.name(), row.level(), row.characterClass(), row.sex());
    }
    private static void assertRoleHeld(TournamentPresence presence, int source) {
        var contender = presence.connect();
        try { assertEquals(TournamentPresence.SelectStatus.BUSY, presence.select(contender, choice(source))); }
        finally { presence.disconnect(contender); }
    }
    private static void awaitRelease(CountDownLatch release) throws IOException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5); boolean interrupted = false;
        try {
            for (;;) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) throw new IOException("synthetic release deadline");
                try {
                    if (!release.await(remaining, TimeUnit.NANOSECONDS)) throw new IOException("synthetic release deadline");
                    return;
                } catch (InterruptedException ignored) { interrupted = true; }
            }
        } finally { if (interrupted) Thread.currentThread().interrupt(); }
    }
    private static ModernApplicationProtocol.CharacterDataResult data(SSLSocket client, int request, int source, long object, long epoch) throws Exception {
        var result = (ModernApplicationProtocol.CharacterDataResult) exchange(client,
                new ModernApplicationProtocol.CharacterDataRequest(request, source, object, epoch));
        assertEquals(request, result.requestId()); assertEquals(source, result.sourceId());
        assertEquals(object, result.objectId()); assertEquals(epoch, result.selectionEpoch()); return result;
    }
    private static ModernApplicationProtocol.NavigationResult nav(SSLSocket client, ModernApplicationProtocol.Message request) throws Exception {
        return (ModernApplicationProtocol.NavigationResult) exchange(client, request);
    }
    private static void roster(SSLSocket client, int request, int source, long epoch) throws Exception {
        assertEquals(0, ((ModernApplicationProtocol.CharacterResult) exchange(client,
                new ModernApplicationProtocol.CharacterRequest(request, source, epoch))).status());
    }
    private static void login(SSLSocket client) throws Exception {
        try (var auth = new ModernApplicationProtocol.Auth(2, ModernApplicationTestBackend.ACCOUNT, ModernApplicationTestBackend.PASSWORD.toCharArray())) {
            assertEquals(0, ((ModernApplicationProtocol.AuthResult) exchange(client, auth)).status());
        }
    }
    private static void hello(SSLSocket client, int capabilities) throws Exception {
        client.startHandshake();
        client.getOutputStream().write(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1, 1, 1, capabilities, 3, 1)));
        client.getOutputStream().flush();
        assertEquals(capabilities, ((ModernSessionProtocol.Ack) ModernSessionProtocol.decode(
                ModernSessionProtocol.readFrame(client.getInputStream()))).capabilities());
    }
    private static SSLSocket client(int port) throws Exception {
        var socket = (SSLSocket) clientContext.getSocketFactory().createSocket(new Socket("127.0.0.1", port), "localhost", port, true);
        var parameters = socket.getSSLParameters(); parameters.setEndpointIdentificationAlgorithm("HTTPS");
        socket.setSSLParameters(parameters); socket.setSoTimeout(3000); return socket;
    }
    private static void send(SSLSocket client, ModernApplicationProtocol.Message message) throws Exception {
        byte[] bytes = ModernApplicationProtocol.encode(message);
        try { client.getOutputStream().write(bytes); client.getOutputStream().flush(); }
        finally { Arrays.fill(bytes, (byte) 0); }
    }
    private static ModernApplicationProtocol.Message exchange(SSLSocket client, ModernApplicationProtocol.Message message) throws Exception {
        send(client, message); byte[] bytes = ModernApplicationProtocol.readFrame(client.getInputStream());
        try { return ModernApplicationProtocol.decode(bytes); }
        finally { if (bytes != null) Arrays.fill(bytes, (byte) 0); }
    }
    private static final class Peer implements AutoCloseable {
        final SSLServerSocket listener;
        final ModernBackendExecutor backend = new ModernBackendExecutor(1, 1);
        final java.util.concurrent.ExecutorService worker = Executors.newSingleThreadExecutor();
        final java.util.concurrent.Future<?> future;
        final AtomicReference<ModernTlsSessionTransport> transport = new AtomicReference<>();
        Peer(ModernApplicationTestBackend fixture, ModernCharacterDataRepository data, TournamentPresence presence) throws Exception {
            listener = (SSLServerSocket) serverContext.getServerSocketFactory().createServerSocket(
                    0, 1, InetAddress.getByAddress(new byte[] {127,0,0,1}));
            listener.setSoTimeout(3000);
            future = worker.submit(() -> {
                try (var socket = (SSLSocket) listener.accept()) {
                    var peer = new ModernTlsSessionTransport(socket, ModernSessionBoundary.Policy.current(),
                            new ModernTlsSessionTransport.Limits(2000,2000), fixture.verifier);
                    transport.set(peer);
                    new ModernApplicationSession(peer, fixture.characters, backend, 10_000, 2000, presence, data).run();
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
            listener.close(); if (transport.get() != null) transport.get().failClosed();
            worker.shutdownNow(); backend.close();
            assertTrue(worker.awaitTermination(3, TimeUnit.SECONDS)); assertTrue(backend.awaitTermination(3000));
        }
    }
}
