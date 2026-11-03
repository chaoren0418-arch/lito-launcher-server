package local.lito381.server;

import static local.lito381.server.ModernRooms.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Real authenticated TLS/Navigation/room ownership; never manufactures a principal or selection lease. */
class ModernWorldTransportDeadlineTest {
    private static SSLContext serverContext, clientContext;
    private static final Scope SCOPE = new Scope(7, 3333, 1, 0);
    private static final int CAPABILITIES = 47; // Rooms + character data, no equipment authority advertised.

    @BeforeAll static void certificates() throws Exception {
        Path material = ModernTlsTestMaterial.defaultDirectory();
        ModernTlsTestMaterial.prepare(material);
        serverContext = ModernTlsTestMaterial.serverContext(material, false);
        clientContext = ModernTlsTestMaterial.clientContext(material);
    }

    @Test void explicitAbortClosesSocketBeforeGuardRetirementAndServerWorkerCannotRetireEarly() throws Exception {
        ExecutorService callers = Executors.newSingleThreadExecutor();
        try (var peer = new Peer(); var client = new Client(peer)) {
            FrozenPreparation ticket = client.prepare();
            FrozenPreparation.Guard guard = ticket.guard();
            Future<?> closing = null;
            try {
                peer.expectedClosure.set(true);
                var started = new CountDownLatch(1);
                closing = callers.submit(() -> { started.countDown(); peer.transport.get().failClosed(); });
                assertTrue(started.await(1, TimeUnit.SECONDS));
                client.assertPeerClosed();
                assertServerSocketClosed(peer);
                assertFalse(closing.isDone(), "ordinary close caller still owns actual cleanup");
                assertFalse(peer.finished.isDone(), "application worker must join cleanup, not just Navigation.closed");
                assertPinned(peer, ticket);
            } finally { guard.close(); }
            assertNotNull(closing); closing.get(3, TimeUnit.SECONDS);
            peer.success(); assertRetired(peer, ticket);
        } finally { callers.shutdownNow(); assertTrue(callers.awaitTermination(3, TimeUnit.SECONDS)); }
    }

    @Test void gracefulCloseAndInterruptedJoiningCallerStillWaitForActualOwnerRetirement() throws Exception {
        ExecutorService callers = Executors.newFixedThreadPool(2);
        try (var peer = new Peer(); var client = new Client(peer)) {
            FrozenPreparation ticket = client.prepare();
            FrozenPreparation.Guard guard = ticket.guard();
            Future<?> closing = null; Future<Boolean> joining = null;
            try {
                peer.expectedClosure.set(true);
                var started = new CountDownLatch(1);
                closing = callers.submit(() -> { started.countDown(); peer.transport.get().close(); return null; });
                assertTrue(started.await(1, TimeUnit.SECONDS));
                client.assertPeerClosed();
                assertServerSocketClosed(peer);
                var joiningStarted = new CountDownLatch(1);
                joining = callers.submit(() -> {
                    Thread.currentThread().interrupt(); joiningStarted.countDown();
                    peer.transport.get().close();
                    return Thread.currentThread().isInterrupted();
                });
                assertTrue(joiningStarted.await(1, TimeUnit.SECONDS));
                assertFalse(closing.isDone()); assertFalse(joining.isDone()); assertFalse(peer.finished.isDone());
                assertPinned(peer, ticket);
            } finally { guard.close(); }
            assertNotNull(closing); closing.get(3, TimeUnit.SECONDS);
            assertNotNull(joining); assertTrue(joining.get(3, TimeUnit.SECONDS), "join must restore the original interrupt");
            peer.success(); assertRetired(peer, ticket);
        } finally { callers.shutdownNow(); assertTrue(callers.awaitTermination(3, TimeUnit.SECONDS)); }
    }

    @Test void actualSharedWatchdogHasNoHeadOfLineBlockingBehindAnotherSessionsHeldGuard() throws Exception {
        try (var first = new Peer(); var firstClient = new Client(first);
             var second = new Peer(); var secondClient = new Client(second)) {
            FrozenPreparation firstTicket = firstClient.prepare();
            FrozenPreparation secondTicket = secondClient.prepare();
            FrozenPreparation.Guard guard = firstTicket.guard();
            try {
                first.expectedClosure.set(true);
                // Arm the actual private production watchdog after real TLS AUTH/
                // SELECT/READY. This isolates its callback from a same-time SO_TIMEOUT;
                // it is not presented as a full wire slow-drip integration test.
                try (var firstDeadline = new ArmedDeadline(first.transport.get(), 150)) {
                    firstClient.assertPeerClosed(); assertTrue(firstDeadline.expired());
                    assertServerSocketClosed(first);
                    assertFalse(first.finished.isDone()); assertPinned(first, firstTicket);
                    second.expectedClosure.set(true);
                    try (var secondDeadline = new ArmedDeadline(second.transport.get(), 300)) {
                        secondClient.assertPeerClosed(); assertTrue(secondDeadline.expired());
                        assertServerSocketClosed(second);
                        second.success(); assertRetired(second, secondTicket);
                        assertFalse(first.finished.isDone(), "second cleanup must not release first's ownership");
                        assertEquals(1, first.presence.ownerCount());
                    }
                }
            } finally { guard.close(); }
            first.success(); assertRetired(first, firstTicket);
        }
    }

    @Test void cleanPeerEofRetiresReadWatchdogBeforeWaitingForFrozenWorldOwner() throws Exception {
        try (var peer = new Peer(500); var client = new Client(peer)) {
            FrozenPreparation ticket = client.prepare();
            var guard = ticket.guard();
            try {
                client.close(); // Real TLS close_notify, not a forced transport abort.
                assertServerSocketClosed(peer);
                assertFalse(peer.finished.isDone(), "clean EOF still owes real world-owner retirement");
                assertPinned(peer, ticket);
                // Deliberately hold ownership beyond the 500 ms read deadline;
                // the read has already completed, so this is not idle ingress.
                assertFalse(new CountDownLatch(1).await(750, TimeUnit.MILLISECONDS));
                assertTrue(peer.transport.get().failurePoint().isEmpty(),
                        "a completed EOF read must not expire during owner cleanup");
                assertFalse(peer.finished.isDone());
            } finally { guard.close(); }
            peer.success();
            assertRetired(peer, ticket);
        }
    }

    private static void assertPinned(Peer peer, FrozenPreparation ticket) {
        assertEquals(ModernTlsSessionTransport.State.CLOSED, peer.transport.get().state());
        assertTrue(peer.transport.get().authenticatedPrincipal().isEmpty());
        assertEquals(1, peer.presence.ownerCount());
        var selection = peer.presence.selected(ticket.participants().get(0).owner());
        assertNotNull(selection);
        var contender = peer.presence.connect();
        try { assertEquals(TournamentPresence.SelectStatus.BUSY, peer.presence.select(contender, selection)); }
        finally { peer.presence.disconnect(contender); }
        // Do not call synchronized Rooms getters while another caller correctly
        // holds that monitor waiting for this test's guard.
    }
    private static void assertServerSocketClosed(Peer peer) throws InterruptedException {
        // Client EOF may observe close_notify just before JSSE publishes its
        // local closed flag. Await that condition, never assume a fixed sleep
        // makes the race disappear, and never release the held owner guard here.
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(1500);
        CountDownLatch pause = new CountDownLatch(1);
        while (!peer.accepted.get().isClosed()) {
            long remaining = deadline - System.nanoTime();
            assertTrue(remaining > 0, "server socket closure must precede owner retirement");
            pause.await(Math.min(remaining, TimeUnit.MILLISECONDS.toNanos(1)), TimeUnit.NANOSECONDS);
        }
    }
    private static void assertRetired(Peer peer, FrozenPreparation ticket) {
        assertFalse(ticket.isLive()); assertFalse(peer.transport.get().ownerCleanupFailed());
        assertEquals(0, peer.presence.ownerCount()); assertEquals(0, peer.rooms.ownerCount());
        assertEquals(0, peer.rooms.roomCount()); assertTrue(peer.backend.isQuiescent());
    }

    private static final class ArmedDeadline implements AutoCloseable {
        final ModernTransportDeadline deadline;
        final ModernTlsSessionTransport transport;
        ArmedDeadline(ModernTlsSessionTransport transport, int millis) throws Exception {
            this.transport = transport;
            var factory = ModernTlsSessionTransport.class.getDeclaredMethod("deadline", int.class, ModernSessionFailures.Stage.class);
            factory.setAccessible(true);
            deadline = (ModernTransportDeadline) factory.invoke(transport, millis, ModernSessionFailures.Stage.APPLICATION_READ);
        }
        boolean expired() {
            return transport.failurePoint().equals(java.util.Optional.of(new ModernSessionFailures.Point(
                    ModernSessionFailures.Stage.APPLICATION_READ, ModernSessionFailures.Kind.TIMEOUT)));
        }
        @Override public void close() throws Exception { deadline.close(); }
    }

    private static final class Peer implements AutoCloseable {
        final TournamentPresence presence = new TournamentPresence();
        final ModernRooms rooms = new ModernRooms();
        final ModernBackendExecutor backend = new ModernBackendExecutor(1, 1);
        final AtomicReference<ModernTlsSessionTransport> transport = new AtomicReference<>();
        final AtomicReference<ModernNavigationSession> navigation = new AtomicReference<>();
        final AtomicReference<SSLSocket> accepted = new AtomicReference<>();
        final AtomicBoolean expectedClosure = new AtomicBoolean();
        final SSLServerSocket listener;
        final ExecutorService worker = Executors.newSingleThreadExecutor();
        final Future<?> finished;
        Peer() throws Exception { this(10_000); }
        Peer(int readDeadlineMillis) throws Exception {
            listener = (SSLServerSocket) serverContext.getServerSocketFactory().createServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
            listener.setSoTimeout(3000);
            finished = worker.submit(() -> {
                try (var socket = (SSLSocket) listener.accept()) {
                    accepted.set(socket);
                    var fixture = new ModernApplicationTestBackend("auth");
                    var connection = new ModernTlsSessionTransport(socket, ModernSessionBoundary.Policy.rooms(false),
                            new ModernTlsSessionTransport.Limits(2000, 2000), fixture.verifier);
                    transport.set(connection);
                    var application = new ModernApplicationSession(connection, fixture.characters, backend, readDeadlineMillis, 3000,
                            presence, ModernCharacterDataRepository.unavailable(), null, rooms);
                    Field field = ModernApplicationSession.class.getDeclaredField("navigation"); field.setAccessible(true);
                    navigation.set((ModernNavigationSession) field.get(application));
                    try { application.run(); }
                    catch (IOException failure) {
                        if (!expectedClosure.get()) throw failure;
                        assertEquals(ModernTlsSessionTransport.State.CLOSED, connection.state());
                    }
                }
                return null;
            });
        }
        void success() throws Exception { finished.get(3, TimeUnit.SECONDS); }
        @Override public void close() throws Exception {
            listener.close();
            try {
                expectedClosure.set(true);
                if (transport.get() != null) transport.get().failClosed();
            } finally {
                worker.shutdown(); backend.close();
                assertTrue(worker.awaitTermination(4, TimeUnit.SECONDS));
                assertTrue(backend.awaitTermination(4000));
            }
            assertEquals(0, presence.ownerCount()); assertEquals(0, rooms.ownerCount());
        }
    }

    private static final class Client implements AutoCloseable {
        final Peer peer; final SSLSocket socket; int next = 2;
        Client(Peer peer) throws Exception {
            this.peer = peer;
            socket = (SSLSocket) clientContext.getSocketFactory().createSocket("localhost", peer.listener.getLocalPort());
            socket.setSoTimeout(3000);
            var parameters = socket.getSSLParameters(); parameters.setEndpointIdentificationAlgorithm("HTTPS"); socket.setSSLParameters(parameters);
        }
        FrozenPreparation prepare() throws Exception {
            socket.startHandshake();
            socket.getOutputStream().write(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1, 1, 1, CAPABILITIES, 7, 1)));
            socket.getOutputStream().flush();
            assertEquals(new ModernSessionProtocol.Ack(1, 1, 1, CAPABILITIES), ModernSessionProtocol.decode(ModernSessionProtocol.readFrame(socket.getInputStream())));
            assertEquals(0, ((ModernApplicationProtocol.AuthResult) exchange(new ModernApplicationProtocol.Auth(next++,
                    ModernApplicationTestBackend.ACCOUNT, ModernApplicationTestBackend.PASSWORD.toCharArray()))).status());
            assertEquals(0, ((ModernApplicationProtocol.CharacterResult) exchange(new ModernApplicationProtocol.CharacterRequest(next++, 7, 1))).status());
            assertEquals(0, ((ModernApplicationProtocol.NavigationResult) exchange(new ModernApplicationProtocol.CharacterSelection(next++, 7, 3333, 1))).status());
            assertEquals(0, ((ModernApplicationProtocol.NavigationResult) exchange(new ModernApplicationProtocol.ChannelEnter(next++, 7, 3333, 1, 0))).status());
            var created = room(new Command(SCOPE, Action.CREATE, 0, 0, 0, new Draft("deadline fixture", "", 8, 4, 540, 16), ""));
            var summary = created.view().joined().summary();
            var ready = room(new Command(SCOPE, Action.READY, summary.id(), summary.revision(), 1, null, ""));
            assertTrue(ready.view().joined().summary().readySatisfied());
            var result = peer.navigation.get().prepareWorld(SCOPE, summary.id(), ready.view().joined().summary().revision());
            assertEquals(Status.OK, result.result().status()); return result.ticket();
        }
        Result room(Command command) throws Exception {
            var result = ((ModernApplicationProtocol.RoomResult) exchange(new ModernApplicationProtocol.RoomRequest(next++, command))).result();
            assertEquals(Status.OK, result.status()); return result;
        }
        ModernApplicationProtocol.Message exchange(ModernApplicationProtocol.Message request) throws Exception {
            byte[] encoded = ModernApplicationProtocol.encode(request);
            try { socket.getOutputStream().write(encoded); socket.getOutputStream().flush(); }
            finally { Arrays.fill(encoded, (byte) 0); request.close(); }
            byte[] response = ModernApplicationProtocol.readFrame(socket.getInputStream()); assertNotNull(response);
            try {
                var decoded = ModernApplicationProtocol.decode(response); assertEquals(request.requestId(), decoded.requestId()); return decoded;
            } finally { Arrays.fill(response, (byte) 0); }
        }
        void assertPeerClosed() throws Exception {
            socket.setSoTimeout(1500);
            long started = System.nanoTime();
            try { assertEquals(-1, socket.getInputStream().read(), "no application bytes may substitute for closure"); }
            catch (SocketTimeoutException timeout) { fail("server socket did not close while guard was held"); }
            catch (IOException resetOrTruncation) { /* Hard abort need not send a TLS close_notify. */ }
            assertTrue(System.nanoTime() - started < TimeUnit.MILLISECONDS.toNanos(1500), "close cannot wait for owner cleanup");
        }
        @Override public void close() throws Exception { socket.close(); }
    }
}
