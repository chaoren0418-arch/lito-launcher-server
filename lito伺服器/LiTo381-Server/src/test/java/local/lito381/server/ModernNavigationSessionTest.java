package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import org.junit.jupiter.api.io.TempDir;

class ModernNavigationSessionTest {
    private static SSLContext serverContext, clientContext;
    @BeforeAll static void certificates() throws Exception {
        Path fixtures = ModernTlsTestMaterial.defaultDirectory(); ModernTlsTestMaterial.prepare(fixtures);
        serverContext = ModernTlsTestMaterial.serverContext(fixtures, false);
        clientContext = ModernTlsTestMaterial.clientContext(fixtures);
    }

    @Test void realTlsNavigationFreshlyQualifiesI64RowsAndReturnsBeforeChangingSource() throws Exception {
        var fixture = new ModernApplicationTestBackend("auth"); var calls = new AtomicInteger();
        var presence = new TournamentPresence();
        ModernCharacterRepository rows = (principal, source) -> {
            calls.incrementAndGet();
            assertEquals(source == 7 ? "sun-owner" : "moon-owner", principal.accountForSource(source));
            return List.of(new ModernApplicationProtocol.CharacterRow(Long.MAX_VALUE,
                    source == 7 ? "Sun Knight" : "Moon Mage", source == 7 ? 1 : 3, 0, Long.MAX_VALUE));
        };
        try (var server = new Peer(fixture, rows, presence); var client = client(server.port())) {
            hello(client, 7); login(client);
            var roster = (ModernApplicationProtocol.CharacterResult) exchange(client, new ModernApplicationProtocol.CharacterRequest(3, 7, -1));
            assertEquals(Long.MAX_VALUE, roster.characters().get(0).objectId());
            var selected = nav(client, new ModernApplicationProtocol.CharacterSelection(4, 7, Long.MAX_VALUE, -1));
            assertEquals(0, selected.status()); assertEquals(1, selected.phase()); assertEquals(255, selected.channelId());
            assertEquals(2, calls.get(), "selection must do a fresh owned-row lookup");
            assertEquals(Long.MAX_VALUE, selected.selected().level()); assertEquals(4, selected.channels().size());
            int request = 5;
            for (int channel = 0; channel < 4; channel++) {
                var lobby = nav(client, new ModernApplicationProtocol.ChannelEnter(request++, 7, Long.MAX_VALUE, -1, channel));
                assertEquals(0, lobby.status()); assertEquals(2, lobby.phase()); assertEquals(channel, lobby.channelId());
                assertEquals(1, lobby.channels().get(channel).population()); assertEquals(1, presence.userCount());
            }
            var returned = nav(client, new ModernApplicationProtocol.CharacterReturn(request++, 7, Long.MAX_VALUE, -1));
            assertEquals(0, returned.phase()); assertNull(returned.selected()); assertTrue(returned.channels().isEmpty());
            assertEquals(0, presence.userCount());
            exchange(client, new ModernApplicationProtocol.CharacterRequest(request++, 42, 2));
            var moon = nav(client, new ModernApplicationProtocol.CharacterSelection(request++, 42, Long.MAX_VALUE, 2));
            assertEquals("Moon Mage", moon.selected().name()); assertEquals(42, moon.sourceId());
            assertEquals(new ModernApplicationProtocol.Pong(request), exchange(client, new ModernApplicationProtocol.Ping(request)));
            client.close(); server.success(); assertEquals(0, presence.ownerCount());
        }
    }

    @Test void removedOrMalformedFreshRowsCannotTurnAPublishedRosterIntoAuthority() throws Exception {
        for (boolean malformed : List.of(false, true)) {
            var fixture = new ModernApplicationTestBackend("auth"); var calls = new AtomicInteger();
            ModernCharacterRepository rows = (principal, source) -> calls.incrementAndGet() == 1
                    ? fixture.characters.characters(principal, source)
                    : malformed ? List.of(new ModernApplicationProtocol.CharacterRow(0, "bad", 1, 0, 80)) : List.of();
            var presence = new TournamentPresence();
            try (var server = new Peer(fixture, rows, presence); var client = client(server.port())) {
                hello(client, 7); login(client); roster(client, 3, 7, 1);
                var result = nav(client, new ModernApplicationProtocol.CharacterSelection(4, 7, 3333, 1));
                assertEquals(malformed ? 2 : 1, result.status()); assertEquals(0, result.phase());
                assertNull(result.selected()); assertEquals(0, presence.userCount());
                assertEquals(new ModernApplicationProtocol.Pong(5), exchange(client, new ModernApplicationProtocol.Ping(5)));
                client.close(); server.success();
            }
        }
    }

    @Test void lobbyLeaveRejoinsAllFourChannelsWithoutQueryingOrReleasingTheSelectedRole() throws Exception {
        var fixture = new ModernApplicationTestBackend("auth"); var presence = new TournamentPresence();
        var contender = presence.connect();
        try (var server = new Peer(fixture, fixture.characters, presence); var client = client(server.port())) {
            hello(client, 7); login(client); roster(client, 3, 7, -1);
            var selected = nav(client, new ModernApplicationProtocol.CharacterSelection(4, 7, 3333, -1));
            var row = selected.selected();
            var held = new TournamentPresence.Selection(new TournamentPresence.CharacterKey(7, row.objectId()),
                    "太陽神", row.name(), row.level(), row.characterClass(), row.sex());
            int request = 5;
            for (int channel = 0; channel < 4; channel++) {
                for (int reentry = 0; reentry < 2; reentry++) {
                    var joined = nav(client, new ModernApplicationProtocol.ChannelEnter(request++, 7, 3333, -1, channel));
                    assertEquals(0, joined.status()); assertEquals(2, joined.phase());
                    assertEquals(channel, joined.channelId()); assertEquals(1, presence.userCount());
                    var left = nav(client, new ModernApplicationProtocol.LobbyLeave(request++, 7, 3333, -1));
                    assertEquals(13, left.action()); assertEquals(0, left.status());
                    assertEquals(1, left.phase()); assertEquals(255, left.channelId());
                    assertEquals(7, left.sourceId()); assertEquals(3333, left.objectId()); assertEquals(-1, left.selectionEpoch());
                    assertEquals(row, left.selected()); assertEquals(4, left.channels().size());
                    assertTrue(left.channels().stream().allMatch(snapshot -> snapshot.population() == 0));
                    assertEquals(0, presence.userCount());
                    assertEquals(TournamentPresence.SelectStatus.BUSY, presence.select(contender, held),
                            "leaving the lobby must not release the selected role");
                    assertEquals(2, fixture.characterCalls.get(), "leave and rejoin do not query a new roster");
                }
            }
            var returned = nav(client, new ModernApplicationProtocol.CharacterReturn(request++, 7, 3333, -1));
            assertEquals(0, returned.phase()); assertNull(returned.selected());
            assertEquals(TournamentPresence.SelectStatus.ACCEPTED, presence.select(contender, held),
                    "BACK from channels must actually release the role");
            presence.returnToCharacters(contender);
            roster(client, request++, 42, 2);
            assertEquals(0, nav(client, new ModernApplicationProtocol.CharacterSelection(request++, 42, 3333, 2)).status());
            assertEquals(new ModernApplicationProtocol.Pong(request), exchange(client, new ModernApplicationProtocol.Ping(request)));
            client.close(); server.success();
        } finally { presence.disconnect(contender); }
        assertEquals(0, presence.ownerCount()); assertEquals(0, presence.userCount());
    }

    @Test void lobbyLeaveRejectsUnselectedChannelsRepeatedLeaveAndStaleSourceObjectEpoch() throws Exception {
        for (int scenario = 0; scenario < 7; scenario++) {
            var fixture = new ModernApplicationTestBackend("auth"); var presence = new TournamentPresence();
            try (var server = new Peer(fixture, fixture.characters, presence); var client = client(server.port())) {
                hello(client, 7); login(client); roster(client, 3, 7, 1);
                int request = 4;
                if (scenario != 0) nav(client, new ModernApplicationProtocol.CharacterSelection(request++, 7, 3333, 1));
                if (scenario >= 2) nav(client, new ModernApplicationProtocol.ChannelEnter(request++, 7, 3333, 1, 0));
                if (scenario == 5) nav(client, new ModernApplicationProtocol.LobbyLeave(request++, 7, 3333, 1));
                if (scenario == 6) {
                    nav(client, new ModernApplicationProtocol.CharacterReturn(request++, 7, 3333, 1));
                    roster(client, request++, 7, 2);
                    nav(client, new ModernApplicationProtocol.CharacterSelection(request++, 7, 3333, 2));
                    nav(client, new ModernApplicationProtocol.ChannelEnter(request++, 7, 3333, 2, 0));
                }
                send(client, new ModernApplicationProtocol.LobbyLeave(request, scenario == 2 ? 42 : 7,
                        scenario == 3 ? 3334 : 3333, scenario == 4 ? 2 : 1));
                assertInstanceOf(ModernApplicationProtocol.ProtocolException.class, server.failure());
                assertEquals(scenario == 0 ? 1 : scenario == 6 ? 4 : 2, fixture.characterCalls.get());
                assertEquals(0, presence.ownerCount()); assertEquals(0, presence.userCount());
            }
        }
    }

    @Test void lobbyLeaveRequiresNegotiatedNavigationAndStillForbidsRosterQueriesWhileSelected() throws Exception {
        for (boolean missingCapability : List.of(true, false)) {
            var fixture = new ModernApplicationTestBackend("auth"); var presence = new TournamentPresence();
            try (var server = new Peer(fixture, fixture.characters, presence); var client = client(server.port())) {
                hello(client, missingCapability ? 3 : 7); login(client); roster(client, 3, 7, 1);
                if (missingCapability) send(client, new ModernApplicationProtocol.LobbyLeave(4, 7, 3333, 1));
                else {
                    nav(client, new ModernApplicationProtocol.CharacterSelection(4, 7, 3333, 1));
                    nav(client, new ModernApplicationProtocol.ChannelEnter(5, 7, 3333, 1, 0));
                    nav(client, new ModernApplicationProtocol.LobbyLeave(6, 7, 3333, 1));
                    send(client, new ModernApplicationProtocol.CharacterRequest(7, 42, 2));
                }
                assertInstanceOf(ModernApplicationProtocol.ProtocolException.class, server.failure());
                assertEquals(missingCapability ? 1 : 2, fixture.characterCalls.get());
                assertEquals(0, presence.ownerCount()); assertEquals(0, presence.userCount());
            }
        }
    }

    @Test void unadvertisedObjectIsDeniedWithoutASecondLookup() throws Exception {
        var fixture = new ModernApplicationTestBackend("auth"); var presence = new TournamentPresence();
        try (var server = new Peer(fixture, fixture.characters, presence); var client = client(server.port())) {
            hello(client, 7); login(client); roster(client, 3, 7, 1);
            var result = nav(client, new ModernApplicationProtocol.CharacterSelection(4, 7, 9999, 1));
            assertEquals(1, result.status()); assertEquals(0, result.phase()); assertEquals(1, fixture.characterCalls.get());
            client.close(); server.success();
        }
    }

    @Test void stalePageEpochAndSourceAreProtocolFailuresAndDoNotQueryTheBackend() throws Exception {
        for (var selection : List.of(new ModernApplicationProtocol.CharacterSelection(4, 7, 3333, 2),
                new ModernApplicationProtocol.CharacterSelection(4, 42, 3333, 1))) {
            var fixture = new ModernApplicationTestBackend("auth"); var presence = new TournamentPresence();
            try (var server = new Peer(fixture, fixture.characters, presence); var client = client(server.port())) {
                hello(client, 7); login(client); roster(client, 3, 7, 1); send(client, selection);
                assertInstanceOf(ModernApplicationProtocol.ProtocolException.class, server.failure());
                assertEquals(1, fixture.characterCalls.get()); assertEquals(0, presence.ownerCount());
            }
        }
    }

    @Test void navigationRequiresActualNegotiatedCapabilityAndSelectionBeforeChannel() throws Exception {
        for (int capability : List.of(3, 7)) {
            var fixture = new ModernApplicationTestBackend("auth"); var presence = new TournamentPresence();
            try (var server = new Peer(fixture, fixture.characters, presence); var client = client(server.port())) {
                hello(client, capability); login(client); roster(client, 3, 7, 1);
                send(client, capability == 3 ? new ModernApplicationProtocol.CharacterSelection(4, 7, 3333, 1)
                        : new ModernApplicationProtocol.ChannelEnter(4, 7, 3333, 1, 0));
                assertInstanceOf(ModernApplicationProtocol.ProtocolException.class, server.failure());
                assertEquals(1, fixture.characterCalls.get()); assertEquals(0, presence.ownerCount());
            }
        }
    }

    @Test void selectedStateRejectsDirectoryQueriesAndMismatchedContext() throws Exception {
        for (boolean query : List.of(true, false)) {
            var fixture = new ModernApplicationTestBackend("auth"); var presence = new TournamentPresence();
            try (var server = new Peer(fixture, fixture.characters, presence); var client = client(server.port())) {
                hello(client, 7); login(client); roster(client, 3, 7, 1);
                assertEquals(0, nav(client, new ModernApplicationProtocol.CharacterSelection(4, 7, 3333, 1)).status());
                send(client, query ? new ModernApplicationProtocol.CharacterRequest(5, 42, 2)
                        : new ModernApplicationProtocol.CharacterReturn(5, 42, 3333, 1));
                assertInstanceOf(ModernApplicationProtocol.ProtocolException.class, server.failure());
                assertEquals(2, fixture.characterCalls.get()); assertEquals(0, presence.ownerCount());
            }
        }
    }

    @Test void closingDuringFreshSelectionRejectsLateRowsWithoutLeakingCharacterOrChannelLocks() throws Exception {
        var fixture = new ModernApplicationTestBackend("auth"); var presence = new TournamentPresence();
        var calls = new AtomicInteger(); var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        ModernCharacterRepository rows = (principal, source) -> {
            if (calls.incrementAndGet() > 1) {
                entered.countDown(); if (!release.await(3, TimeUnit.SECONDS)) throw new IOException("fixture release timeout");
            }
            return fixture.characters.characters(principal, source);
        };
        try (var server = new Peer(fixture, rows, presence); var client = client(server.port())) {
            try {
                hello(client, 7); login(client); roster(client, 3, 7, 1);
                send(client, new ModernApplicationProtocol.CharacterSelection(4, 7, 3333, 1));
                assertTrue(entered.await(2, TimeUnit.SECONDS));
                server.transport.get().failClosed();
                assertEquals(0, presence.ownerCount()); assertEquals(0, presence.userCount());
                release.countDown(); assertInstanceOf(IOException.class, server.failure());
                assertTrue(server.transport.get().authenticatedPrincipal().isEmpty());
            } finally { release.countDown(); }
        }
    }

    @Test void completedNativeDeletionRejectsAnAlreadyReadRowAndCloseCannotReclaimIt() throws Exception {
        for (boolean closeBeforeRelease : List.of(false, true)) {
            var fixture = new ModernApplicationTestBackend("auth"); var presence = new TournamentPresence();
            var nativeOwner = presence.connect(); var calls = new AtomicInteger(); var deleted = new AtomicBoolean();
            var rowRead = new CountDownLatch(1); var release = new CountDownLatch(1);
            ModernCharacterRepository rows = (principal, source) -> {
                // Materialize the row BEFORE signalling; this is a delayed DB
                // result, not a repository that reads only after deletion.
                var snapshot = deleted.get() ? List.<ModernApplicationProtocol.CharacterRow>of()
                        : List.copyOf(fixture.characters.characters(principal, source));
                if (calls.incrementAndGet() == 2) {
                    rowRead.countDown();
                    awaitLateResult(release);
                }
                return snapshot;
            };
            try (var server = new Peer(fixture, rows, presence); var client = client(server.port())) {
                try {
                    hello(client, 7); login(client); roster(client, 3, 7, 1);
                    send(client, new ModernApplicationProtocol.CharacterSelection(4, 7, 3333, 1));
                    assertTrue(rowRead.await(2, TimeUnit.SECONDS));
                    String alias = GameStateStore.sourceCharacter("太陽神", "Sun Knight");
                    assertTrue(presence.beginDeletion(alias, nativeOwner));
                    deleted.set(true);
                    presence.endDeletion(alias);
                    if (closeBeforeRelease) {
                        server.transport.get().failClosed();
                        assertEquals(1, presence.ownerCount(), "only the native deletion requester remains");
                        release.countDown();
                        assertInstanceOf(IOException.class, server.failure());
                        assertTrue(server.transport.get().authenticatedPrincipal().isEmpty());
                    } else {
                        release.countDown();
                        byte[] frame = ModernApplicationProtocol.readFrame(client.getInputStream());
                        ModernApplicationProtocol.NavigationResult result;
                        try { result = (ModernApplicationProtocol.NavigationResult) ModernApplicationProtocol.decode(frame); }
                        finally { if (frame != null) Arrays.fill(frame, (byte) 0); }
                        assertEquals(3, result.status()); assertEquals(0, result.phase());
                        assertNull(result.selected()); assertEquals(0, presence.userCount());
                        // Retry actually re-queries the now-deleted row. A stale
                        // ticket failure does not strand selecting=true or TLS.
                        assertEquals(1, nav(client, new ModernApplicationProtocol.CharacterSelection(5, 7, 3333, 1)).status());
                        assertEquals(3, calls.get());
                        assertEquals(new ModernApplicationProtocol.Pong(6), exchange(client, new ModernApplicationProtocol.Ping(6)));
                        client.close(); server.success();
                    }
                    assertEquals(0, presence.userCount());
                    assertTrue(presence.beginDeletion(alias, nativeOwner), "no selected alias survived the late result");
                    presence.endDeletion(alias);
                } finally { release.countDown(); }
            } finally { presence.disconnect(nativeOwner); }
            assertEquals(0, presence.ownerCount()); assertEquals(0, presence.userCount());
        }
    }

    /** Model a bounded backend that can return late even after cancellation. */
    private static void awaitLateResult(CountDownLatch release) throws IOException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        boolean interrupted = false;
        try {
            for (;;) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) throw new IOException("fixture release timeout");
                try {
                    if (!release.await(remaining, TimeUnit.NANOSECONDS)) throw new IOException("fixture release timeout");
                    return;
                } catch (InterruptedException ignored) { interrupted = true; }
            }
        } finally { if (interrupted) Thread.currentThread().interrupt(); }
    }

    /*
     * #138 B-land: candidate 50635f7e GameHub no longer accepts an injected TournamentPresence (that
     * four-argument constructor was a src/main-only test seam never wired by Main). Native and modern
     * adapters therefore keep separate presence/capacity. This pins the candidate contract; whether the
     * shared reservation must be restored is recorded as an open item in docs/reports/issue-138.md.
     */
    @Test void nativeHubAndModernAdapterKeepSeparateCapacityAfterBLand(@TempDir Path directory) throws Exception {
        Path configuration = directory.resolve("native.properties");
        Files.writeString(configuration, "database.mode=memory\nlogin.classic.server.names=太陽神,月亮\nlogin.classic.server.ids=7,42\n");
        ServerConfig config = ServerConfig.load(configuration);
        var fixture = new ModernApplicationTestBackend("auth"); var presence = new TournamentPresence();
        try (var hub = new GameHub(config);
             var server = new Peer(fixture, fixture.characters, presence); var client = client(server.port())) {
            var occupants = new ArrayList<ClientSession>();
            for (int i = 0; i < GameHub.CHANNEL_CAPACITY; i++) {
                var player = new ClientSession(null, ClientSession.Endpoint.GAME, config, null, null, LocalResources.discardCapture(), hub);
                occupants.add(player); hub.connect(player); assertNotNull(hub.enterChannel(0, player));
            }
            assertEquals(GameHub.CHANNEL_CAPACITY, hub.channelSnapshot(0).users());
            hello(client, 7); login(client); roster(client, 3, 7, 1);
            assertEquals(0, nav(client, new ModernApplicationProtocol.CharacterSelection(4, 7, 3333, 1)).status());
            var joined = nav(client, new ModernApplicationProtocol.ChannelEnter(5, 7, 3333, 1, 0));
            assertEquals(0, joined.status(), "a full native channel does not consume modern capacity");
            assertEquals(1, presence.snapshot(0).users());
            assertEquals(GameHub.CHANNEL_CAPACITY, hub.channelSnapshot(0).users(), "modern admission does not touch the native hub");
            assertEquals(GameHub.CHANNEL_CAPACITY, hub.channelMembers(0).size());
            var left = nav(client, new ModernApplicationProtocol.LobbyLeave(6, 7, 3333, 1));
            assertEquals(1, left.phase());
            assertEquals(0, presence.snapshot(0).users());
            assertEquals(GameHub.CHANNEL_CAPACITY, hub.channelSnapshot(0).users());
            nav(client, new ModernApplicationProtocol.CharacterReturn(7, 7, 3333, 1));
            for (var player : occupants) hub.disconnect(player);
            assertEquals(0, hub.channelSnapshot(0).users());
            client.close(); server.success();
        }
    }

    private static void roster(SSLSocket client, int request, int source, long epoch) throws Exception {
        assertEquals(0, ((ModernApplicationProtocol.CharacterResult) exchange(client,
                new ModernApplicationProtocol.CharacterRequest(request, source, epoch))).status());
    }
    private static ModernApplicationProtocol.NavigationResult nav(SSLSocket client, ModernApplicationProtocol.Message request) throws Exception {
        return (ModernApplicationProtocol.NavigationResult) exchange(client, request);
    }
    private static void login(SSLSocket client) throws Exception {
        try (var auth = new ModernApplicationProtocol.Auth(2, ModernApplicationTestBackend.ACCOUNT,
                ModernApplicationTestBackend.PASSWORD.toCharArray())) {
            assertEquals(0, ((ModernApplicationProtocol.AuthResult) exchange(client, auth)).status());
        }
    }
    private static SSLSocket client(int port) throws Exception {
        var socket = (SSLSocket) clientContext.getSocketFactory().createSocket(new Socket("127.0.0.1", port), "localhost", port, true);
        var parameters = socket.getSSLParameters(); parameters.setEndpointIdentificationAlgorithm("HTTPS");
        socket.setSSLParameters(parameters); socket.setSoTimeout(3_000); return socket;
    }
    private static void hello(SSLSocket client, int capabilities) throws Exception {
        client.startHandshake();
        client.getOutputStream().write(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1, 1, 1, capabilities, 3, 1)));
        client.getOutputStream().flush();
        var ack = (ModernSessionProtocol.Ack) ModernSessionProtocol.decode(ModernSessionProtocol.readFrame(client.getInputStream()));
        assertEquals(capabilities, ack.capabilities());
    }
    private static void send(SSLSocket client, ModernApplicationProtocol.Message message) throws Exception {
        byte[] frame = ModernApplicationProtocol.encode(message);
        try { client.getOutputStream().write(frame); client.getOutputStream().flush(); }
        finally { Arrays.fill(frame, (byte) 0); }
    }
    private static ModernApplicationProtocol.Message exchange(SSLSocket client, ModernApplicationProtocol.Message message) throws Exception {
        send(client, message); byte[] frame = ModernApplicationProtocol.readFrame(client.getInputStream());
        try { return ModernApplicationProtocol.decode(frame); }
        finally { if (frame != null) Arrays.fill(frame, (byte) 0); }
    }
    private static final class Peer implements AutoCloseable {
        final SSLServerSocket listener;
        final ModernBackendExecutor backend = new ModernBackendExecutor(2, 2);
        final java.util.concurrent.ExecutorService worker = Executors.newSingleThreadExecutor();
        final java.util.concurrent.Future<?> future;
        final AtomicReference<ModernTlsSessionTransport> transport = new AtomicReference<>();
        Peer(ModernApplicationTestBackend fixture, ModernCharacterRepository rows, TournamentPresence presence) throws Exception {
            listener = (SSLServerSocket) serverContext.getServerSocketFactory().createServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
            listener.setSoTimeout(3_000);
            future = worker.submit(() -> {
                try (var socket = (SSLSocket) listener.accept()) {
                    var peer = new ModernTlsSessionTransport(socket, ModernSessionBoundary.Policy.current(),
                            new ModernTlsSessionTransport.Limits(2_000, 2_000), fixture.verifier);
                    transport.set(peer);
                    new ModernApplicationSession(peer, rows, backend, 10_000, 2_000, presence).run();
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
            assertTrue(worker.awaitTermination(3, TimeUnit.SECONDS));
            assertTrue(backend.awaitTermination(3_000));
        }
    }
}
