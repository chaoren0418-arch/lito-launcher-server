package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernEquipmentOverrideStore.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** True TLS navigation plus actual owned disk work. No equipment wire is claimed here. */
class ModernEquipmentNavigationTest {
    private static SSLContext serverContext, clientContext;
    private static final TournamentPresence.CharacterKey KEY = new TournamentPresence.CharacterKey(7, 3333);
    private static final long EPOCH = Long.MIN_VALUE;
    @TempDir Path directory;

    @BeforeAll static void certificates() throws Exception {
        Path fixture = ModernTlsTestMaterial.defaultDirectory(); ModernTlsTestMaterial.prepare(fixture);
        serverContext = ModernTlsTestMaterial.serverContext(fixture, false);
        clientContext = ModernTlsTestMaterial.clientContext(fixture);
    }

    @Test void numericKeysCannotManufactureAuthorityBeforeAuthSelectionOrAcrossSourceEpoch() throws Exception {
        var calls = new AtomicInteger();
        try (var peer = new Peer(); var client = client(peer.port())) {
            assertTrue(peer.created.await(2, TimeUnit.SECONDS));
            var navigation = peer.navigation.get();
            ModernNavigationSession.EquipmentAction<Integer> action = (principal, character, operation) -> {
                calls.incrementAndGet();
                assertEquals("sun-owner", principal.accountForSource(7));
                assertEquals(3333, character.objectId()); assertEquals(KEY, operation.key());
                assertTrue(operation.tryCommit()); return 19;
            };
            assertThrows(IOException.class, () -> navigation.equipment(7, 3333, EPOCH, action));
            hello(client); login(client);
            assertThrows(IOException.class, () -> navigation.equipment(7, 3333, EPOCH, action));
            select(client, 3, 4, EPOCH);
            assertThrows(IOException.class, () -> navigation.equipment(42, 3333, EPOCH, action));
            assertThrows(IOException.class, () -> navigation.equipment(7, 3334, EPOCH, action));
            assertThrows(IOException.class, () -> navigation.equipment(7, 3333, -1, action));
            assertEquals(0, calls.get()); assertEquals(19, navigation.equipment(7, 3333, EPOCH, action));
            assertEquals(1, calls.get());
            assertEquals(0, nav(client, new ModernApplicationProtocol.CharacterReturn(5, 7, 3333, EPOCH)).status());
            assertThrows(IOException.class, () -> navigation.equipment(7, 3333, EPOCH, action));
            client.close(); peer.success();
            assertEquals(0, peer.presence.ownerCount());
        }
    }

    @Test void backBeforeCommitAdmissionReleasesRoleButLatePreparationCannotWrite() throws Exception {
        var prepared = new CountDownLatch(1); var release = new CountDownLatch(1);
        var callers = Executors.newSingleThreadExecutor();
        try (var peer = new Peer(); var client = client(peer.port()); var store = new ModernEquipmentOverrideStore(directory, new FileOps() {
            @Override public void force(FileChannel file) throws IOException {
                FileOps.super.force(file); prepared.countDown(); await(release);
            }
        })) {
            loginSelected(client);
            var holder = new AtomicReference<ModernEquipmentLease.Operation>();
            var pending = callers.submit(() -> peer.navigation.get().equipment(7, 3333, EPOCH, (principal, row, operation) -> {
                holder.set(operation);
                return store.compareAndSet(KEY, Expected.absent(), value(true), operation.scope(), operation);
            }));
            assertTrue(prepared.await(2, TimeUnit.SECONDS));
            var returned = nav(client, new ModernApplicationProtocol.CharacterReturn(5, 7, 3333, EPOCH));
            assertEquals(0, returned.status()); assertEquals(0, returned.phase());
            var competitor = peer.presence.connect();
            try {
                assertEquals(TournamentPresence.SelectStatus.ACCEPTED, peer.presence.select(competitor, choice(7)));
                release.countDown(); assertInstanceOf(IOException.class, failure(pending));
                assertFalse(holder.get().tryCommit()); assertTrue(store.read(KEY, scope()).isEmpty());
            } finally { peer.presence.disconnect(competitor); }
            select(client, 6, 7, EPOCH); // Deliberately reuse numeric epoch: attachment identity still differs.
            int retried = peer.navigation.get().equipment(7, 3333, EPOCH, (principal, row, operation) -> {
                assertNotSame(holder.get(), operation); assertTrue(operation.tryCommit()); return 1;
            });
            assertEquals(1, retried);
            assertFalse(holder.get().tryCommit());
            client.close(); peer.success(); assertEquals(0, peer.presence.ownerCount());
        } finally { release.countDown(); callers.shutdown(); assertTrue(callers.awaitTermination(3, TimeUnit.SECONDS)); }
    }

    @Test void admittedSaveMakesBackBusyButLobbyLeaveAndRejoinKeepTheSameSelection() throws Exception {
        var admitted = new CountDownLatch(1); var release = new CountDownLatch(1);
        var callers = Executors.newSingleThreadExecutor();
        try (var peer = new Peer(); var client = client(peer.port()); var store = new ModernEquipmentOverrideStore(directory, new FileOps() {
            @Override public void move(Path from, Path to) throws IOException {
                admitted.countDown(); await(release); FileOps.super.move(from, to);
            }
        })) {
            loginSelected(client);
            assertEquals(0, nav(client, new ModernApplicationProtocol.ChannelEnter(5, 7, 3333, EPOCH, 0)).status());
            var pending = callers.submit(() -> peer.navigation.get().equipment(7, 3333, EPOCH,
                    (principal, row, operation) -> store.compareAndSet(KEY, Expected.absent(), value(true), operation.scope(), operation)));
            assertTrue(admitted.await(2, TimeUnit.SECONDS));
            var back = nav(client, new ModernApplicationProtocol.CharacterReturn(6, 7, 3333, EPOCH));
            assertEquals(3, back.status()); assertEquals(2, back.phase()); assertEquals(3333, back.selected().objectId());
            assertEquals(1, peer.presence.userCount());
            var left = nav(client, new ModernApplicationProtocol.LobbyLeave(7, 7, 3333, EPOCH));
            assertEquals(0, left.status()); assertEquals(1, left.phase()); assertEquals(0, peer.presence.userCount());
            var competitor = peer.presence.connect();
            try {
                assertEquals(TournamentPresence.SelectStatus.BUSY, peer.presence.select(competitor, choice(7)));
                var own = (TournamentPresence.Owner) field(peer.navigation.get(), "owner");
                assertFalse(peer.presence.beginDeletion(choice(7).legacyAlias(), own));
                assertEquals(0, nav(client, new ModernApplicationProtocol.ChannelEnter(8, 7, 3333, EPOCH, 2)).status());
                assertEquals(3, nav(client, new ModernApplicationProtocol.CharacterReturn(9, 7, 3333, EPOCH)).status());
                release.countDown(); var committed = pending.get(3, TimeUnit.SECONDS);
                assertEquals(1, committed.stored().revision()); assertEquals(committed.stored(), store.read(KEY, scope()).orElseThrow());
                assertEquals(0, nav(client, new ModernApplicationProtocol.CharacterReturn(10, 7, 3333, EPOCH)).status());
                assertEquals(0, peer.presence.userCount());
                assertEquals(TournamentPresence.SelectStatus.ACCEPTED, peer.presence.select(competitor, choice(7)));
            } finally { peer.presence.disconnect(competitor); }
            client.close(); peer.success(); assertEquals(0, peer.presence.ownerCount());
        } finally { release.countDown(); callers.shutdown(); assertTrue(callers.awaitTermination(3, TimeUnit.SECONDS)); }
    }

    @Test void realTransportAbortCancelsCallerButRetainsRoleUntilActualDiskWorkerFinally() throws Exception {
        var admitted = new CountDownLatch(1); var release = new CountDownLatch(1);
        var callers = Executors.newFixedThreadPool(2);
        try (var peer = new Peer(); var client = client(peer.port()); var store = new ModernEquipmentOverrideStore(directory, new FileOps() {
            @Override public void move(Path from, Path to) throws IOException {
                admitted.countDown(); await(release); FileOps.super.move(from, to);
            }
        })) {
            loginSelected(client);
            nav(client, new ModernApplicationProtocol.ChannelEnter(5, 7, 3333, EPOCH, 0));
            var holder = new AtomicReference<ModernEquipmentLease.Operation>();
            var committed = new AtomicReference<Stored>();
            var pending = callers.submit(() -> peer.navigation.get().equipment(7, 3333, EPOCH, (principal, row, operation) -> {
                holder.set(operation);
                var result = store.compareAndSet(KEY, Expected.absent(), value(true), operation.scope(), operation);
                committed.set(result.stored()); return result;
            }));
            assertTrue(admitted.await(2, TimeUnit.SECONDS));
            callers.submit(() -> peer.transport.get().failClosed()).get(1, TimeUnit.SECONDS);
            assertTrue(peer.socket.get().isClosed()); assertEquals(0, peer.presence.userCount());
            assertEquals(1, peer.presence.ownerCount()); assertTrue(holder.get().isCommitAdmitted());
            var cancelled = assertInstanceOf(ModernBackendExecutor.BackendException.class, failure(pending));
            assertTrue(cancelled.failure() == ModernBackendExecutor.Failure.CANCELLED || cancelled.failure() == ModernBackendExecutor.Failure.CLOSED);
            assertFalse(peer.backend.isQuiescent());
            var competitor = peer.presence.connect();
            try {
                assertEquals(TournamentPresence.SelectStatus.BUSY, peer.presence.select(competitor, choice(7)));
                assertFalse(peer.presence.beginDeletion(choice(7).legacyAlias(), competitor));
                assertEquals(TournamentPresence.SelectStatus.ACCEPTED, peer.presence.select(competitor, choice(42)),
                        "same numeric objid in another source is independently available");
                release.countDown(); peer.backend.close(); assertTrue(peer.backend.awaitTermination(3_000));
                assertFalse(holder.get().isCommitAdmitted()); assertEquals(1, peer.presence.ownerCount());
                assertEquals(1, committed.get().revision()); assertEquals(committed.get(), store.read(KEY, scope()).orElseThrow());
                assertEquals(TournamentPresence.SelectStatus.ACCEPTED, peer.presence.select(competitor, choice(7)));
            } finally { peer.presence.disconnect(competitor); }
            assertEquals(0, peer.presence.ownerCount());
            peer.closedOutcome();
        } finally { release.countDown(); callers.shutdown(); assertTrue(callers.awaitTermination(3, TimeUnit.SECONDS)); }
    }

    @Test void lostCommitResultIsReconciledOnReselectionWithoutInverseToggle() throws Exception {
        var moves = new AtomicInteger();
        try (var peer = new Peer(); var client = client(peer.port()); var store = new ModernEquipmentOverrideStore(directory, new FileOps() {
            @Override public void move(Path from, Path to) throws IOException {
                FileOps.super.move(from, to);
                if (moves.incrementAndGet() == 1) throw new IOException("synthetic lost storage acknowledgment");
            }
        })) {
            loginSelected(client);
            assertThrows(ModernBackendExecutor.BackendException.class, () -> peer.navigation.get().equipment(7, 3333, EPOCH,
                    (principal, row, operation) -> store.compareAndSet(KEY, Expected.absent(), value(true), operation.scope(), operation)));
            assertEquals(0, nav(client, new ModernApplicationProtocol.CharacterReturn(5, 7, 3333, EPOCH)).status());
            select(client, 6, 7, -1);
            var actual = peer.navigation.get().equipment(7, 3333, -1, (principal, row, operation) -> store.read(KEY, operation.scope())).orElseThrow();
            assertEquals(1, actual.revision()); assertEquals(value(true), actual.value());
            var retry = peer.navigation.get().equipment(7, 3333, -1, (principal, row, operation) ->
                    store.compareAndSet(KEY, Expected.of(actual), value(true), operation.scope(), operation));
            assertFalse(retry.changed()); assertEquals(1, retry.stored().revision()); assertEquals(1, moves.get());
            client.close(); peer.success();
        }
    }

    @Test void cleanupCallbackFailureStillClosesRealSocketAndDoesNotClaimRoleRelease() throws Exception {
        for (boolean graceful : new boolean[] {false, true}) {
            try (var peer = new Peer(); var client = client(peer.port())) {
                loginSelected(client);
                nav(client, new ModernApplicationProtocol.ChannelEnter(5, 7, 3333, EPOCH, 0));
                Object attachment = field(peer.navigation.get(), "equipmentSelection");
                var lease = (ModernEquipmentLease) field(attachment, "lease");
                Runnable originalRetirement = (Runnable) field(lease, "onRetired");
                // Test-only fault injection into the actual authority callback;
                // no production factory/setter or wire-supplied lease exists.
                setField(lease, "onRetired", (Runnable) () -> { throw new IllegalStateException("private callback detail"); });
                try {
                    if (graceful) {
                        IOException failure = assertThrows(IOException.class, () -> peer.transport.get().close());
                        assertFalse(failure.getMessage().contains("private")); assertNull(failure.getCause());
                    } else peer.transport.get().failClosed();
                    assertTrue(peer.socket.get().isClosed()); assertTrue(peer.transport.get().ownerCleanupFailed());
                    assertTrue(lease.retirementFailed()); assertFalse(lease.isRetired());
                    assertEquals(0, peer.presence.userCount()); assertEquals(1, peer.presence.ownerCount());
                    var competitor = peer.presence.connect();
                    try { assertEquals(TournamentPresence.SelectStatus.BUSY, peer.presence.select(competitor, choice(7))); }
                    finally { peer.presence.disconnect(competitor); }
                    peer.closedOutcome();
                } finally {
                    // Explicit test cleanup of the deliberately failed pin, not
                    // an automatic production retry of unknown partial cleanup.
                    originalRetirement.run();
                }
                assertEquals(0, peer.presence.ownerCount());
            }
        }
    }

    @Test void backEofAndDirectNavigationCleanupFailuresRemainVisibleToTransport() throws Exception {
        for (int route = 0; route < 3; route++) {
            try (var peer = new Peer(); var client = client(peer.port())) {
                loginSelected(client);
                Object attachment = field(peer.navigation.get(), "equipmentSelection");
                var lease = (ModernEquipmentLease) field(attachment, "lease");
                Runnable original = (Runnable) field(lease, "onRetired");
                setField(lease, "onRetired", (Runnable) () -> { throw new IllegalArgumentException("private retirement failure"); });
                try {
                    if (route == 0) {
                        byte[] back = ModernApplicationProtocol.encode(new ModernApplicationProtocol.CharacterReturn(5,7,3333,EPOCH));
                        client.getOutputStream().write(back); client.getOutputStream().flush();
                    } else if (route == 1) client.close();
                    else {
                        assertThrows(IllegalStateException.class, () -> peer.navigation.get().close());
                        peer.transport.get().failClosed();
                    }
                    peer.closedOutcome();
                    assertTrue(peer.socket.get().isClosed()); assertTrue(peer.transport.get().ownerCleanupFailed());
                    assertTrue(lease.retirementFailed()); assertEquals(1,peer.presence.ownerCount());
                } finally { original.run(); }
                assertEquals(0,peer.presence.ownerCount());
            }
        }
    }

    private static Replacement value(boolean equipped) {
        return new Replacement(ModernEquipmentOverrideStoreTest.fp(7),
                List.of(new ItemOverride(Long.MAX_VALUE, 41, ModernEquipmentOverrideStoreTest.fp(41), equipped)));
    }
    private static ModernBackendScope scope() { return ModernEquipmentOverrideStoreTest.scope(); }
    private static TournamentPresence.Selection choice(int source) {
        return new TournamentPresence.Selection(new TournamentPresence.CharacterKey(source,3333),
                source == 7 ? "太陽神" : "月亮", source == 7 ? "Sun Knight" : "Moon Mage", 80, 1, 0);
    }
    private static void await(CountDownLatch latch) throws IOException {
        try { if (!latch.await(4, TimeUnit.SECONDS)) throw new IOException("equipment navigation barrier timeout"); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IOException("equipment navigation interrupted"); }
    }
    private static Throwable failure(java.util.concurrent.Future<?> pending) {
        return assertThrows(ExecutionException.class, () -> pending.get(3, TimeUnit.SECONDS)).getCause();
    }
    private static Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }
    private static void setField(Object object, String name, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); field.set(object,value);
    }
    private static SSLSocket client(int port) throws Exception {
        var client = (SSLSocket) clientContext.getSocketFactory().createSocket(new Socket("127.0.0.1",port),"localhost",port,true);
        var parameters = client.getSSLParameters(); parameters.setEndpointIdentificationAlgorithm("HTTPS");
        client.setSSLParameters(parameters); client.setSoTimeout(3_000); return client;
    }
    private static void hello(SSLSocket client) throws Exception {
        client.startHandshake();
        client.getOutputStream().write(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,1,7,3,1)));
        client.getOutputStream().flush();
        assertInstanceOf(ModernSessionProtocol.Ack.class, ModernSessionProtocol.decode(ModernSessionProtocol.readFrame(client.getInputStream())));
    }
    private static void login(SSLSocket client) throws Exception {
        try (var auth = new ModernApplicationProtocol.Auth(2,ModernApplicationTestBackend.ACCOUNT,ModernApplicationTestBackend.PASSWORD.toCharArray())) {
            assertEquals(0, ((ModernApplicationProtocol.AuthResult) exchange(client,auth)).status());
        }
    }
    private static void loginSelected(SSLSocket client) throws Exception { hello(client); login(client); select(client,3,4,EPOCH); }
    private static void select(SSLSocket client, int roster, int select, long epoch) throws Exception {
        assertEquals(0,((ModernApplicationProtocol.CharacterResult) exchange(client,new ModernApplicationProtocol.CharacterRequest(roster,7,epoch))).status());
        assertEquals(0,nav(client,new ModernApplicationProtocol.CharacterSelection(select,7,3333,epoch)).status());
    }
    private static ModernApplicationProtocol.NavigationResult nav(SSLSocket client, ModernApplicationProtocol.Message request) throws Exception {
        return (ModernApplicationProtocol.NavigationResult) exchange(client,request);
    }
    private static ModernApplicationProtocol.Message exchange(SSLSocket client, ModernApplicationProtocol.Message request) throws Exception {
        byte[] frame = ModernApplicationProtocol.encode(request);
        try { client.getOutputStream().write(frame); client.getOutputStream().flush(); }
        finally { Arrays.fill(frame,(byte)0); }
        byte[] response = ModernApplicationProtocol.readFrame(client.getInputStream());
        try { return ModernApplicationProtocol.decode(response); }
        finally { if(response!=null) Arrays.fill(response,(byte)0); }
    }

    private static final class Peer implements AutoCloseable {
        final TournamentPresence presence = new TournamentPresence();
        final ModernBackendExecutor backend = new ModernBackendExecutor(2,2);
        final AtomicReference<ModernTlsSessionTransport> transport = new AtomicReference<>();
        final AtomicReference<ModernNavigationSession> navigation = new AtomicReference<>();
        final AtomicReference<SSLSocket> socket = new AtomicReference<>();
        final CountDownLatch created = new CountDownLatch(1);
        final SSLServerSocket listener;
        final java.util.concurrent.ExecutorService worker = Executors.newSingleThreadExecutor();
        final java.util.concurrent.Future<?> future;
        Peer() throws Exception {
            var fixture = new ModernApplicationTestBackend("auth");
            listener = (SSLServerSocket) serverContext.getServerSocketFactory().createServerSocket(0,1,InetAddress.getByName("127.0.0.1"));
            listener.setSoTimeout(3_000);
            future = worker.submit(() -> {
                try (var accepted = (SSLSocket) listener.accept()) {
                    socket.set(accepted);
                    var connection = new ModernTlsSessionTransport(accepted,ModernSessionBoundary.Policy.current(),
                            new ModernTlsSessionTransport.Limits(2_000,2_000),fixture.verifier);
                    transport.set(connection);
                    var application = new ModernApplicationSession(connection,fixture.characters,backend,10_000,3_000,presence);
                    navigation.set((ModernNavigationSession)field(application,"navigation")); created.countDown();
                    application.run();
                }
                return null;
            });
        }
        int port() { return listener.getLocalPort(); }
        void success() throws Exception { future.get(4,TimeUnit.SECONDS); }
        void closedOutcome() throws Exception {
            try { future.get(4,TimeUnit.SECONDS); }
            catch (ExecutionException expected) { assertInstanceOf(IOException.class,expected.getCause()); }
        }
        @Override public void close() throws Exception {
            listener.close(); if(transport.get()!=null) transport.get().failClosed();
            worker.shutdown(); backend.close();
            assertTrue(worker.awaitTermination(4,TimeUnit.SECONDS)); assertTrue(backend.awaitTermination(4_000));
        }
    }
}
