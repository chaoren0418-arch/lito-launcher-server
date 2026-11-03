package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Real TLS/APP/navigation/backend ownership, with synthetic totals and no SQL/world/equipment provider. */
class ModernCharacterRecordNavigationTest {
    private static SSLContext serverTls, clientTls;
    private static final long SUN_WINS = 0x1_0000_0001L;
    private static final long MOON_LOSSES = 0x2_0000_0002L;

    @BeforeAll static void certificates() throws Exception {
        Path fixtures = ModernTlsTestMaterial.defaultDirectory();
        ModernTlsTestMaterial.prepare(fixtures);
        serverTls = ModernTlsTestMaterial.serverContext(fixtures, false);
        clientTls = ModernTlsTestMaterial.clientContext(fixtures);
    }

    private static ModernApplicationProtocol.CharacterRow row(int source) {
        return new ModernApplicationProtocol.CharacterRow(3333, source == 7 ? "Sun Knight" : "Moon Mage",
                source == 7 ? 1 : 3, source == 7 ? 0 : 1, source == 7 ? 80 : 120_000);
    }
    private static TournamentPresence.CharacterKey key(int source) {
        return new TournamentPresence.CharacterKey(source, 3333);
    }
    private static ModernCharacterRecord.Snapshot totals(int source) {
        return source == 7 ? new ModernCharacterRecord.Snapshot(key(source), true, SUN_WINS, 2, SUN_WINS + 2)
                : new ModernCharacterRecord.Snapshot(key(source), true, 3, MOON_LOSSES, MOON_LOSSES + 3);
    }
    private static void principal(ModernAccountAuthentication.Principal principal, int source, long object) throws IOException {
        assertTrue(principal.authorizesSource(source));
        assertEquals(source == 7 ? ModernApplicationTestBackend.SUN_ACCOUNT : ModernApplicationTestBackend.MOON_ACCOUNT,
                principal.accountForSource(source));
        assertEquals(3333, object);
        ModernBackendScope.current().checkActive();
    }

    @Test void versionFiveWithoutCombatOrDataReadsExactSourceTotalsAcrossChannelsLobbyAndSameObjectSwitch() throws Exception {
        var calls = new AtomicInteger();
        try (var peer = new Peer((p, s, o) -> { principal(p, s, o); calls.incrementAndGet(); return totals(s); });
                var client = new Client(peer.port(), 5, 7)) {
            client.login();
            client.select(7, -1);
            int rosterReads = peer.fixture.characterCalls.get();
            assertEquals(totals(7), client.record(7, 3333, -1).snapshot());
            assertEquals(0, peer.presence.userCount()); assertRoleHeld(peer.presence, 7);
            assertEquals(2, client.nav(new ModernApplicationProtocol.ChannelEnter(client.next(), 7, 3333, -1, 2)).phase());
            var before = peer.presence.snapshots();
            assertEquals(totals(7), client.record(7, 3333, -1).snapshot());
            assertEquals(before, peer.presence.snapshots());
            assertEquals(1, client.nav(new ModernApplicationProtocol.LobbyLeave(client.next(), 7, 3333, -1)).phase());
            assertEquals(totals(7), client.record(7, 3333, -1).snapshot());
            assertEquals(rosterReads, peer.fixture.characterCalls.get());
            client.back(7, -1); client.select(42, Long.MIN_VALUE);
            assertEquals(totals(42), client.record(42, 3333, Long.MIN_VALUE).snapshot());
            assertRoleHeld(peer.presence, 42);
            client.back(42, Long.MIN_VALUE); client.select(7, 2);
            assertEquals(totals(7), client.record(7, 3333, 2).snapshot());
            assertEquals(5, calls.get()); client.ping(); client.close(); peer.success();
            assertEquals(0, peer.presence.ownerCount());
        }
    }

    @Test void versionFourRejectsRecordBeforeRepositoryButFiveAcceptsWithoutCombat() throws Exception {
        var calls = new AtomicInteger();
        try (var peer = new Peer((p, s, o) -> { calls.incrementAndGet(); return totals(s); });
                var client = new Client(peer.port(), 4, 15)) {
            client.login(); client.select(7, 1);
            client.send(new ModernApplicationProtocol.RecordRequest(client.next(), new ModernCharacterRecordProtocol.Context(7, 3333, 1)), 5);
            assertInstanceOf(IOException.class, peer.failure()); assertEquals(0, calls.get());
            assertEquals(0, peer.presence.ownerCount());
        }
        try (var peer = new Peer((p, s, o) -> { principal(p, s, o); calls.incrementAndGet(); return totals(s); });
                var client = new Client(peer.port(), 5, 15)) {
            client.login(); client.select(7, 1);
            assertEquals(ModernCharacterRecordProtocol.Status.OK, client.record(7, 3333, 1).status());
            assertEquals(1, calls.get()); client.close(); peer.success();
        }
    }

    @Test void unauthenticatedUnselectedWrongSourceObjectEpochAndReturnedRequestNeverCallsRepository() throws Exception {
        for (int scenario = 0; scenario < 7; scenario++) {
            var calls = new AtomicInteger();
            try (var peer = new Peer((p, s, o) -> { calls.incrementAndGet(); return totals(s); });
                    var client = new Client(peer.port(), 5, 7)) {
                if (scenario != 0) client.login();
                if (scenario >= 2) client.select(7, 1);
                if (scenario == 6) client.back(7, 1);
                var context = new ModernCharacterRecordProtocol.Context(scenario == 2 ? 42 : scenario == 5 ? 99 : 7,
                        scenario == 3 ? 3334 : 3333, scenario == 4 ? 2 : 1);
                client.send(new ModernApplicationProtocol.RecordRequest(client.next(), context), 5);
                assertInstanceOf(IOException.class, peer.failure()); assertEquals(0, calls.get());
                assertEquals(0, peer.presence.ownerCount());
            }
        }
    }

    @Test void confirmedMissingRowAndStoredZeroRemainDistinctFromUnavailableAndBusy() throws Exception {
        for (int scenario = 0; scenario < 7; scenario++) {
            final int selected = scenario;
            var calls = new AtomicInteger();
            try (var peer = new Peer((p, s, o) -> {
                principal(p, s, o); calls.incrementAndGet();
                return switch (selected) {
                    case 0 -> ModernCharacterRecord.Snapshot.absent(key(s));
                    case 1 -> new ModernCharacterRecord.Snapshot(key(s), true, 0, 0, 0);
                    case 2 -> throw new IOException("synthetic private backend detail");
                    case 3 -> throw new ModernBackendExecutor.BackendException(ModernBackendExecutor.Failure.BUSY);
                    case 4 -> totals(42);
                    case 5 -> new ModernCharacterRecord.Snapshot(new TournamentPresence.CharacterKey(7, 3334), true, 1, 0, 1);
                    default -> null;
                };
            }); var client = new Client(peer.port(), 5, 7)) {
                client.login(); client.select(7, 1);
                var reply = client.record(7, 3333, 1);
                assertEquals(selected <= 1 ? ModernCharacterRecordProtocol.Status.OK
                        : selected == 3 ? ModernCharacterRecordProtocol.Status.BUSY : ModernCharacterRecordProtocol.Status.UNAVAILABLE,
                        reply.status());
                if (selected <= 1) {
                    assertNotNull(reply.snapshot()); assertEquals(selected == 1, reply.snapshot().rowPresent());
                    assertEquals(0, reply.snapshot().wins()); assertEquals(0, reply.snapshot().losses()); assertEquals(0, reply.snapshot().revision());
                } else assertNull(reply.snapshot());
                assertEquals(1, calls.get()); assertRoleHeld(peer.presence, 7); client.ping();
                client.back(7, 1); client.close(); peer.success();
            }
        }
    }

    @Test void actualBackendQueueSaturationReturnsBusyAndRetryKeepsTheSelectedOwner() throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        var callers = Executors.newFixedThreadPool(2); var calls = new AtomicInteger();
        try (var peer = new Peer((p, s, o) -> { principal(p, s, o); calls.incrementAndGet(); return totals(s); });
                var client = new Client(peer.port(), 5, 7); var first = peer.backend.openSession(); var second = peer.backend.openSession()) {
            try {
                client.login(); client.select(7, 1);
                var active = callers.submit(() -> first.call(5000, scope -> { entered.countDown(); await(release); return 1; }));
                assertTrue(entered.await(2, TimeUnit.SECONDS));
                var queued = callers.submit(() -> second.call(5000, scope -> { await(release); return 2; }));
                waitFor(() -> peer.backend.queuedCount() == 1);
                var reply = client.record(7, 3333, 1);
                assertEquals(ModernCharacterRecordProtocol.Status.BUSY, reply.status()); assertNull(reply.snapshot());
                assertEquals(0, calls.get()); assertRoleHeld(peer.presence, 7);
                release.countDown(); int firstResult = active.get(3, TimeUnit.SECONDS), secondResult = queued.get(3, TimeUnit.SECONDS);
                assertEquals(1, firstResult); assertEquals(2, secondResult);
                assertEquals(totals(7), client.record(7, 3333, 1).snapshot()); assertEquals(1, calls.get());
                client.close(); peer.success();
            } finally { release.countDown(); }
        } finally { callers.shutdownNow(); assertTrue(callers.awaitTermination(3, TimeUnit.SECONDS)); }
    }

    @Test void closeCancelsInFlightReadWithoutReturningWorkerCapacityUntilProviderFinallyExits() throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        var capturedScope = new AtomicReference<ModernBackendScope>(); var completed = new AtomicInteger();
        try (var peer = new Peer((p, s, o) -> {
            principal(p, s, o); capturedScope.set(ModernBackendScope.current()); entered.countDown();
            try { await(release); return totals(s); } finally { completed.incrementAndGet(); }
        }); var client = new Client(peer.port(), 5, 7)) {
            try {
                client.login(); client.select(7, 1);
                client.send(new ModernApplicationProtocol.RecordRequest(client.next(), new ModernCharacterRecordProtocol.Context(7, 3333, 1)), 5);
                assertTrue(entered.await(2, TimeUnit.SECONDS)); peer.transport.get().failClosed();
                assertThrows(ModernBackendExecutor.BackendException.class, () -> capturedScope.get().checkActive());
                assertEquals(1, peer.backend.activeCount()); assertEquals(0, completed.get());
                // This provider intentionally ignores interruption: cancellation is not retirement.
                release.countDown(); assertInstanceOf(IOException.class, peer.failure());
                waitFor(() -> peer.backend.activeCount() == 0); assertEquals(1, completed.get());
                assertEquals(0, peer.presence.ownerCount());
            } finally { release.countDown(); }
        }
    }

    @Test void backendDeadlineFailsClosedRatherThanPublishingLateTotalsOrInventingZero() throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        try (var peer = new Peer((p, s, o) -> { principal(p, s, o); entered.countDown(); await(release); return totals(s); });
                var client = new Client(peer.port(), 5, 7)) {
            try {
                client.login(); client.select(7, 1);
                client.send(new ModernApplicationProtocol.RecordRequest(client.next(), new ModernCharacterRecordProtocol.Context(7, 3333, 1)), 5);
                assertTrue(entered.await(2, TimeUnit.SECONDS));
                var failure = assertInstanceOf(ModernBackendExecutor.BackendException.class, peer.failure());
                assertEquals(ModernBackendExecutor.Failure.DEADLINE, failure.failure());
                assertEquals(1, peer.backend.activeCount());
                release.countDown(); waitFor(() -> peer.backend.activeCount() == 0);
                assertEquals(0, peer.presence.ownerCount());
            } finally { release.countDown(); }
        }
    }

    @Test void backRejectsLateSuccessfulAndGenericFailedReadBeforeAnyResponseCanReachNewSelection() throws Exception {
        for (boolean failing : List.of(false, true)) {
            var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
            var caller = Executors.newSingleThreadExecutor(); var calls = new AtomicInteger();
            try (var peer = new Peer((p, s, o) -> {
                principal(p, s, o);
                if (calls.incrementAndGet() == 1) {
                    entered.countDown(); await(release);
                    if (failing) throw new IOException("synthetic private late failure");
                }
                return totals(s);
            }); var client = new Client(peer.port(), 5, 7)) {
                try {
                    client.login(); client.select(7, 1);
                    // The TLS owner serializes wire requests. Invoke the same real navigation seam
                    // independently only to place BACK precisely while its backend read is running.
                    var navigation = peer.navigation();
                    var pending = caller.submit(() -> navigation.record(new ModernApplicationProtocol.RecordRequest(90,
                            new ModernCharacterRecordProtocol.Context(7, 3333, 1))));
                    assertTrue(entered.await(2, TimeUnit.SECONDS)); client.back(7, 1);
                    assertEquals(1, peer.backend.activeCount());
                    release.countDown();
                    assertInstanceOf(IOException.class, assertThrows(ExecutionException.class,
                            () -> pending.get(3, TimeUnit.SECONDS)).getCause());
                    waitFor(() -> peer.backend.activeCount() == 0);
                    // Even reusing all public identity fields must obtain a fresh opaque attachment.
                    client.select(7, 1); assertEquals(totals(7), client.record(7, 3333, 1).snapshot());
                    assertEquals(2, calls.get()); client.ping(); client.close(); peer.success();
                } finally { release.countDown(); }
            } finally { caller.shutdownNow(); assertTrue(caller.awaitTermination(3, TimeUnit.SECONDS)); }
        }
    }

    @Test void recordNegotiationCannotBeReenabledAfterSelectionOrRetirement() throws Exception {
        var calls = new AtomicInteger();
        try (var peer = new Peer((p, s, o) -> { calls.incrementAndGet(); return totals(s); });
                var client = new Client(peer.port(), 5, 7)) {
            client.login();
            assertThrows(IOException.class, () -> peer.navigation().negotiateRecords(true));
            client.select(7, 1);
            assertThrows(IOException.class, () -> peer.navigation().negotiateRecords(false));
            client.close(); peer.success();
            assertThrows(IOException.class, () -> peer.navigation().record(new ModernApplicationProtocol.RecordRequest(90,
                    new ModernCharacterRecordProtocol.Context(7, 3333, 1))));
            assertEquals(0, calls.get());
        }
    }

    @Test void duplicateUnsignedRequestIdIsFatalAndNeverReplaysTheReader() throws Exception {
        var calls = new AtomicInteger();
        try (var peer = new Peer((p, s, o) -> { calls.incrementAndGet(); return totals(s); });
                var client = new Client(peer.port(), 5, 7)) {
            client.login(); client.select(7, 1);
            var request = new ModernApplicationProtocol.RecordRequest(client.next(), new ModernCharacterRecordProtocol.Context(7, 3333, 1));
            assertEquals(totals(7), ((ModernApplicationProtocol.RecordResult) client.exchange(request)).result().snapshot());
            client.send(request, 5); assertInstanceOf(IOException.class, peer.failure());
            assertEquals(1, calls.get()); assertEquals(0, peer.presence.ownerCount());
        }
    }

    @Test void productionServerLongestConstructorForwardsRecordRepositoryWithoutWorldOrEquipment() throws Exception {
        var fixture = new ModernApplicationTestBackend("auth"); var presence = new TournamentPresence(); var calls = new AtomicInteger();
        ModernServer server = null;
        try {
            server = new ModernServer(new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 0), serverTls,
                    new ModernServer.Limits(2, 2, 60, 2, 1, 1, 2000, 2000, 2000, 10000), fixture.verifier, fixture.characters,
                    presence, ModernCharacterDataRepository.unavailable(), null, null, null, null, 5,
                    ModernSkillsRepository.unavailable(), (p, s, o) -> { principal(p, s, o); calls.incrementAndGet(); return totals(s); }, null);
            try (var client = new Client(server.address().getPort(), 5, 7)) {
                client.login(); client.select(7, 1); assertEquals(totals(7), client.record(7, 3333, 1).snapshot());
                client.back(7, 1); client.select(42, 2); assertEquals(totals(42), client.record(42, 3333, 2).snapshot()); client.ping();
            }
            final ModernServer running = server;
            waitFor(() -> running.snapshot().sessions() == 0);
            assertEquals(0, running.snapshot().failed()); assertEquals(2, calls.get());
        } finally { if (server != null) { server.close(); assertTrue(server.awaitTermination(5000)); } }
        assertEquals(0, presence.ownerCount());
    }

    private static void assertRoleHeld(TournamentPresence presence, int source) {
        var contender = presence.connect();
        try {
            assertEquals(TournamentPresence.SelectStatus.BUSY, presence.select(contender,
                    new TournamentPresence.Selection(key(source), source == 7 ? "太陽神" : "月亮", row(source).name(),
                            row(source).level(), row(source).characterClass(), row(source).sex())));
        } finally { presence.disconnect(contender); }
    }
    private static void await(CountDownLatch latch) throws IOException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(7); boolean interrupted = false;
        try {
            for (;;) {
                long left = deadline - System.nanoTime(); if (left <= 0) throw new IOException("fixture barrier deadline");
                try { if (!latch.await(left, TimeUnit.NANOSECONDS)) throw new IOException("fixture barrier deadline"); return; }
                catch (InterruptedException failure) { interrupted = true; }
            }
        } finally { if (interrupted) Thread.currentThread().interrupt(); }
    }
    private static void waitFor(java.util.function.BooleanSupplier ready) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (!ready.getAsBoolean()) { assertTrue(System.nanoTime() < deadline, "fixture retirement deadline"); Thread.sleep(1); }
    }
    private static final class Peer implements AutoCloseable {
        final ModernApplicationTestBackend fixture = new ModernApplicationTestBackend("auth");
        final TournamentPresence presence = new TournamentPresence();
        final ModernBackendExecutor backend = new ModernBackendExecutor(1, 1);
        final ExecutorService worker = Executors.newSingleThreadExecutor();
        final SSLServerSocket listener;
        final Future<?> future;
        final AtomicReference<ModernTlsSessionTransport> transport = new AtomicReference<>();
        final AtomicReference<ModernApplicationSession> session = new AtomicReference<>();

        Peer(ModernCharacterRecordRepository records) throws Exception {
            SSLServerSocket opened = null;
            try {
                opened = (SSLServerSocket) serverTls.getServerSocketFactory().createServerSocket(0, 1,
                        InetAddress.getByAddress(new byte[]{127, 0, 0, 1}));
                opened.setSoTimeout(3000); listener = opened;
                future = worker.submit(() -> {
                    try (var socket = (SSLSocket) listener.accept()) {
                        var peer = new ModernTlsSessionTransport(socket, ModernSessionBoundary.Policy.current().withMaxVersion(5),
                                new ModernTlsSessionTransport.Limits(2000, 2000), fixture.verifier);
                        transport.set(peer);
                        var app = new ModernApplicationSession(peer, fixture.characters, backend, 10000, 2000, presence,
                                ModernCharacterDataRepository.unavailable(), null, null, null, ModernSkillsRepository.unavailable(), records);
                        session.set(app); app.run(); return null;
                    }
                });
            } catch (Exception | Error failure) {
                if (opened != null) try { opened.close(); } catch (IOException suppressed) { failure.addSuppressed(suppressed); }
                worker.shutdownNow(); backend.close();
                assertTrue(worker.awaitTermination(5000, TimeUnit.MILLISECONDS)); assertTrue(backend.awaitTermination(5000));
                throw failure;
            }
        }
        int port() { return listener.getLocalPort(); }
        ModernNavigationSession navigation() throws Exception {
            Field field = ModernApplicationSession.class.getDeclaredField("navigation"); field.setAccessible(true);
            return (ModernNavigationSession) field.get(session.get());
        }
        void success() throws Exception { future.get(5, TimeUnit.SECONDS); }
        Throwable failure() throws Exception { return assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS)).getCause(); }
        @Override public void close() throws Exception {
            try { listener.close(); }
            finally {
                if (transport.get() != null) transport.get().failClosed();
                worker.shutdownNow(); backend.close();
                assertTrue(worker.awaitTermination(5000, TimeUnit.MILLISECONDS)); assertTrue(backend.awaitTermination(5000));
                assertEquals(0, presence.ownerCount());
            }
        }
    }
    private static final class Client implements AutoCloseable {
        final SSLSocket socket;
        final int version;
        int request = 2;
        Client(int port, int version, int capabilities) throws Exception {
            this.version = version;
            Socket raw = new Socket(); SSLSocket opened = null;
            try {
                raw.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), port), 2000);
                opened = (SSLSocket) clientTls.getSocketFactory().createSocket(raw, "localhost", port, true);
                var parameters = opened.getSSLParameters(); parameters.setEndpointIdentificationAlgorithm("HTTPS");
                opened.setSSLParameters(parameters); opened.setSoTimeout(5000); opened.startHandshake();
                opened.getOutputStream().write(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1, version, version, capabilities, 7, 1)));
                opened.getOutputStream().flush();
                var ack = (ModernSessionProtocol.Ack) ModernSessionProtocol.decode(ModernSessionProtocol.readFrame(opened.getInputStream()));
                assertEquals(version, ack.version()); assertEquals(capabilities, ack.capabilities());
                socket = opened;
            } catch (Exception | Error failure) {
                try { if (opened != null) opened.close(); else raw.close(); }
                catch (IOException suppressed) { failure.addSuppressed(suppressed); }
                throw failure;
            }
        }
        int next() { return request++; }
        void login() throws Exception {
            try (var auth = new ModernApplicationProtocol.Auth(next(), ModernApplicationTestBackend.ACCOUNT,
                    ModernApplicationTestBackend.PASSWORD.toCharArray())) {
                assertEquals(0, ((ModernApplicationProtocol.AuthResult) exchange(auth)).status());
            }
        }
        void select(int source, long epoch) throws Exception {
            assertEquals(0, ((ModernApplicationProtocol.CharacterResult) exchange(new ModernApplicationProtocol.CharacterRequest(next(), source, epoch))).status());
            assertEquals(1, nav(new ModernApplicationProtocol.CharacterSelection(next(), source, 3333, epoch)).phase());
        }
        void back(int source, long epoch) throws Exception {
            assertEquals(0, nav(new ModernApplicationProtocol.CharacterReturn(next(), source, 3333, epoch)).phase());
        }
        ModernApplicationProtocol.NavigationResult nav(ModernApplicationProtocol.Message request) throws Exception {
            var result = (ModernApplicationProtocol.NavigationResult) exchange(request);
            assertEquals(request.requestId(), result.requestId()); assertEquals(0, result.status()); return result;
        }
        ModernCharacterRecordProtocol.Reply record(int source, long object, long epoch) throws Exception {
            var request = new ModernApplicationProtocol.RecordRequest(next(), new ModernCharacterRecordProtocol.Context(source, object, epoch));
            var result = (ModernApplicationProtocol.RecordResult) exchange(request);
            assertEquals(request.requestId(), result.requestId()); assertEquals(request.context(), result.result().context()); return result.result();
        }
        void ping() throws Exception {
            int id = next(); assertEquals(new ModernApplicationProtocol.Pong(id), exchange(new ModernApplicationProtocol.Ping(id)));
        }
        void send(ModernApplicationProtocol.Message request, int encodedVersion) throws Exception {
            byte[] bytes = ModernApplicationProtocol.encode(request, encodedVersion);
            try { socket.getOutputStream().write(bytes); socket.getOutputStream().flush(); }
            finally { Arrays.fill(bytes, (byte) 0); }
        }
        ModernApplicationProtocol.Message exchange(ModernApplicationProtocol.Message request) throws Exception {
            send(request, version); byte[] bytes = ModernApplicationProtocol.readFrame(socket.getInputStream());
            try { return ModernApplicationProtocol.decode(bytes, version); }
            finally { if (bytes != null) Arrays.fill(bytes, (byte) 0); }
        }
        @Override public void close() throws IOException { socket.close(); }
    }
}
