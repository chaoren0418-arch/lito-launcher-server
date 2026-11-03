package local.lito381.server;

import static local.lito381.server.ModernWorldProtocol.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Real TLS + production owned world service + fixed real shared map; no DB and no combat claims. */
class ModernWorldSessionIntegrationTest {
    private static SSLContext serverContext, clientContext;
    private static ModernWorldMap map;
    @BeforeAll static void resourcesAndCertificates() throws Exception {
        String root = System.getenv("LITO_SHARED_RESOURCE_ROOT");
        org.junit.jupiter.api.Assumptions.assumeTrue(root != null && !root.isBlank(), "explicit shared resource root required");
        map = ModernWorldTestPeer.sharedMap(Path.of(root));
        Path fixture = ModernTlsTestMaterial.defaultDirectory(); ModernTlsTestMaterial.prepare(fixture);
        serverContext = ModernTlsTestMaterial.serverContext(fixture, false);
        clientContext = ModernTlsTestMaterial.clientContext(fixture);
    }

    @Test void twoAuthenticatedSourcesPrepareLoadCountdownMoveLeaveAndReenterTheSameLobby() throws Exception {
        try (var harness = new ModernWorldTestPeer.Harness(serverContext, map, "normal");
             var sun = new Client(harness, 7); var moon = new Client(harness, 42)) {
            readyRoom(sun, moon);
            Frame initial = sun.prepare(); assertEquals(Phase.PREPARING, initial.phase());
            Frame loading = sun.awaitPhase(Phase.LOADING);
            assertEquals(ModernWorldTestPeer.MANIFEST_SHA, loading.manifest().digest());
            assertEquals(List.of(7, 42), loading.actors().stream().map(actor -> actor.member().key().sourceId()).toList());
            assertEquals(List.of(1L, 2L), loading.actors().stream().map(ModernBattleEngine.Actor::entityId).toList());
            assertTrue(loading.actors().stream().allMatch(actor -> actor.member().key().objectId() == 3333));
            assertEquals(List.of("太陽神", "月亮"), loading.room().members().stream().map(ModernRooms.Member::sourceName).toList());
            assertEquals(3, harness.sunReads.get()); assertEquals(3, harness.moonReads.get());
            assertEquals(Phase.LOADING, sun.loaded(loading).frame().phase());
            long readyAt = System.nanoTime();
            assertEquals(Phase.COUNTDOWN, moon.loaded(loading).frame().phase());
            Frame active = sun.awaitPhase(Phase.ACTIVE);
            assertTrue(System.nanoTime() - readyAt >= TimeUnit.MILLISECONDS.toNanos(2_800), "real server clock must drive the 3-second countdown");
            assertEquals(ModernWorldTestPeer.ANCHOR, active.actors().get(0).position());
            // The selected real map decides the second connected spawn. If it
            // occupies the tested east edge, first move that authenticated actor
            // to another verified free neighbor, never overwrite its occupancy.
            vacateEastIfNecessary(harness.map, sun, moon, active);
            active = sun.readWorld();
            var moved = sun.move(active, 1, 2, ModernWorldTestPeer.ANCHOR);
            assertEquals(Status.OK, moved.status());
            assertEquals(new ModernWorldMap.Cell(32725, 32784), moved.frame().actors().get(0).position());
            assertEquals(1, moved.frame().actors().get(0).lastMoveSequence());
            assertNull(sun.leaveWorld(moved.frame()).frame());
            Frame terminal = moon.readWorld(); assertEquals(Phase.ABORTED, terminal.phase());
            assertEquals(AbortReason.SERVER_ABORT, terminal.abortReason());
            assertNull(moon.leaveWorld(terminal).frame());
            assertEquals(0, sun.readRoom().view().joined().summary().ready());
            assertTrue(sun.readRoom().view().options().stream().allMatch(option ->
                    option.worldAvailable() == (option.mode() == 1 && option.mapId() == 514)),
                    "only the explicit entry/movement binding is available; this does not claim combat completion");
            sun.roomAction(ModernRooms.Action.LEAVE, 0); moon.roomAction(ModernRooms.Action.LEAVE, 0);
            for (Client client : List.of(sun, moon)) {
                assertEquals(1, client.leaveLobby().phase()); client.enter(1);
                assertNull(client.readRoom().view().joined()); assertNull(client.readWorld());
            }
            assertEquals(0, harness.server.snapshot().failed());
        }
    }

    @Test void changedFreshCharacterCannotEnterWorldAndCanRetryOnlyAfterRealReselection() throws Exception {
        try (var harness = new ModernWorldTestPeer.Harness(serverContext, map, "changed");
             var sun = new Client(harness, 7); var moon = new Client(harness, 42)) {
            readyRoom(sun, moon); Frame initial = sun.prepare();
            Frame terminal = sun.awaitPhase(Phase.ABORTED);
            assertEquals(AbortReason.PREPARATION_FAILED, terminal.abortReason());
            assertTrue(terminal.actors().stream().noneMatch(ModernBattleEngine.Actor::loaded));
            assertEquals(3, harness.sunReads.get()); assertEquals(3, harness.moonReads.get());
            assertEquals(0, sun.readRoom().view().joined().summary().ready());
            assertEquals(Status.DENIED, sun.loaded(initial).status());
            sun.leaveWorld(terminal); moon.leaveWorld(terminal);
            sun.roomAction(ModernRooms.Action.LEAVE, 0); moon.roomAction(ModernRooms.Action.LEAVE, 0);
            // A current selected name is intentionally stale. BACK/roster/select
            // reloads the actual changed row before creating a fresh room ticket.
            sun.returnAndReselect(); moon.returnAndReselect(); readyRoom(sun, moon);
            Frame retry = sun.prepare(); assertTrue(retry.matchId() > initial.matchId());
            Frame loading = sun.awaitPhase(Phase.LOADING);
            assertEquals("Changed Sun Knight", loading.room().members().get(0).name());
            assertEquals(6, harness.sunReads.get()); assertEquals(6, harness.moonReads.get());
            assertEquals(0, harness.server.snapshot().failed());
        }
    }

    @Test void actualTlsDisconnectAbortsOtherParticipantAndRetiresSelectionAndWorldOwnership() throws Exception {
        try (var harness = new ModernWorldTestPeer.Harness(serverContext, map, "disconnect");
             var sun = new Client(harness, 7); var moon = new Client(harness, 42)) {
            readyRoom(sun, moon); Frame initial = sun.prepare(); sun.awaitPhase(Phase.LOADING);
            sun.close();
            Frame abort = moon.awaitPhase(Phase.ABORTED); assertEquals(AbortReason.DISCONNECTED, abort.abortReason());
            assertEquals(initial.matchId(), abort.matchId());
            await(() -> harness.presence.ownerCount() == 1 && harness.worlds.registrationCount() == 1);
            assertEquals(0, moon.readRoom().view().joined().summary().ready());
            assertEquals(new TournamentPresence.CharacterKey(42, 3333), moon.readRoom().view().joined().owner());
            moon.leaveWorld(abort); moon.roomAction(ModernRooms.Action.LEAVE, 0); moon.close();
            await(() -> harness.server.snapshot().sessions() == 0 && harness.worlds.isQuiescent());
            assertEquals(0, harness.presence.ownerCount()); assertEquals(0, harness.rooms.roomCount());
        }
    }

    @Test void wrongManifestOverRealTlsFailsClosedAndCannotLoadAnotherSourcesActor() throws Exception {
        try (var harness = new ModernWorldTestPeer.Harness(serverContext, map, "normal");
             var sun = new Client(harness, 7); var moon = new Client(harness, 42)) {
            readyRoom(sun, moon); sun.prepare(); Frame loading = sun.awaitPhase(Phase.LOADING);
            var wrong = new Request(sun.scope(), Action.LOADED, 0, 0, 0, loading.matchId(), loading.generation(), 0, 0, 0, "a".repeat(64));
            sun.expectClosed(new ModernApplicationProtocol.WorldRequest(sun.next++, wrong));
            Frame abort = moon.awaitPhase(Phase.ABORTED); assertTrue(abort.actors().stream().noneMatch(ModernBattleEngine.Actor::loaded));
            await(() -> harness.server.snapshot().failed() == 1 && harness.presence.ownerCount() == 1);
        }
    }

    private static void readyRoom(Client sun, Client moon) throws Exception {
        var created = sun.room(new ModernRooms.Command(sun.scope(), ModernRooms.Action.CREATE, 0, 0, 0,
                new ModernRooms.Draft("shared world test", "", 4, 1, 514, ModernRooms.ALLOW_UPPER_TIER), ""));
        assertEquals(ModernRooms.Status.OK, created.status());
        var room = created.view().joined().summary();
        assertEquals(ModernRooms.Status.OK, moon.room(new ModernRooms.Command(moon.scope(), ModernRooms.Action.JOIN,
                room.id(), room.revision(), 0, null, "")).status());
        sun.roomAction(ModernRooms.Action.READY, 1);
        assertTrue(moon.roomAction(ModernRooms.Action.READY, 1).view().joined().summary().readySatisfied());
    }
    private static void vacateEastIfNecessary(ModernWorldMap map, Client sun, Client moon, Frame frame) throws Exception {
        ModernWorldMap.Cell east = new ModernWorldMap.Cell(32725, 32784), moonCell = frame.actors().get(1).position();
        if (!moonCell.equals(east)) return;
        int[] dx = {0, 1, 1, 1, 0, -1, -1, -1}, dy = {-1, -1, 0, 1, 1, 1, 0, -1};
        for (int heading = 0; heading < 8; heading++) {
            var next = new ModernWorldMap.Cell(moonCell.x() + dx[heading], moonCell.y() + dy[heading]);
            if (!next.equals(ModernWorldTestPeer.ANCHOR) && map.canStep(moonCell, next)) {
                assertEquals(Status.OK, moon.move(frame, 1, heading, moonCell).status()); return;
            }
        }
        fail("fixed shared spawn needs an explicit unoccupied route before east movement");
    }
    private static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(4);
        while (!condition.getAsBoolean()) { assertTrue(System.nanoTime() - deadline < 0, "owned condition did not finish"); Thread.sleep(5); }
    }

    private static final class Client implements AutoCloseable {
        final SSLSocket socket; final int source;
        int channel = 0, next = 2; long epoch = 1;
        Client(ModernWorldTestPeer.Harness harness, int source) throws Exception {
            this.source = source; socket = (SSLSocket) clientContext.getSocketFactory().createSocket(); socket.setSoTimeout(4_000);
            var parameters = socket.getSSLParameters(); parameters.setEndpointIdentificationAlgorithm("HTTPS"); socket.setSSLParameters(parameters);
            socket.connect(harness.server.address(), 1_000); socket.startHandshake();
            socket.getOutputStream().write(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1, 1, 1, ModernWorldTestPeer.CAPABILITIES, 7, 1)));
            socket.getOutputStream().flush();
            assertEquals(new ModernSessionProtocol.Ack(1, 1, 1, ModernWorldTestPeer.CAPABILITIES),
                    ModernSessionProtocol.decode(ModernSessionProtocol.readFrame(socket.getInputStream())));
            var auth = (ModernApplicationProtocol.AuthResult) exchange(new ModernApplicationProtocol.Auth(next++, ModernApplicationTestBackend.ACCOUNT,
                    ModernApplicationTestBackend.PASSWORD.toCharArray())); assertEquals(0, auth.status());
            select(); enter(0);
        }
        ModernRooms.Scope scope() { return new ModernRooms.Scope(source, 3333, epoch, channel); }
        void select() throws Exception {
            var roster = (ModernApplicationProtocol.CharacterResult) exchange(new ModernApplicationProtocol.CharacterRequest(next++, source, epoch));
            assertEquals(0, roster.status()); assertEquals(3333, roster.characters().get(0).objectId());
            assertEquals(0, ((ModernApplicationProtocol.NavigationResult) exchange(new ModernApplicationProtocol.CharacterSelection(next++, source, 3333, epoch))).status());
        }
        void enter(int value) throws Exception {
            var result = (ModernApplicationProtocol.NavigationResult) exchange(new ModernApplicationProtocol.ChannelEnter(next++, source, 3333, epoch, value));
            assertEquals(0, result.status()); assertEquals(2, result.phase()); channel = value;
        }
        ModernApplicationProtocol.NavigationResult leaveLobby() throws Exception {
            return (ModernApplicationProtocol.NavigationResult) exchange(new ModernApplicationProtocol.LobbyLeave(next++, source, 3333, epoch));
        }
        void returnAndReselect() throws Exception {
            assertEquals(0, ((ModernApplicationProtocol.NavigationResult) exchange(new ModernApplicationProtocol.CharacterReturn(next++, source, 3333, epoch))).phase());
            epoch++; select(); enter(0);
        }
        ModernRooms.Result room(ModernRooms.Command request) throws Exception {
            var result = (ModernApplicationProtocol.RoomResult) exchange(new ModernApplicationProtocol.RoomRequest(next++, request));
            assertEquals(ModernRoomProtocol.Echo.of(request), result.echo()); return result.result();
        }
        ModernRooms.Result readRoom() throws Exception { return room(new ModernRooms.Command(scope(), ModernRooms.Action.READ, 0, 0, 0, null, "")); }
        ModernRooms.Result roomAction(ModernRooms.Action action, int value) throws Exception {
            var room = readRoom().view().joined().summary();
            var result = room(new ModernRooms.Command(scope(), action, room.id(), room.revision(), value, null, ""));
            assertEquals(ModernRooms.Status.OK, result.status()); return result;
        }
        Reply world(Request request) throws Exception {
            var result = (ModernApplicationProtocol.WorldResult) exchange(new ModernApplicationProtocol.WorldRequest(next++, request));
            assertEquals(request, result.echo()); return result.reply();
        }
        Frame prepare() throws Exception {
            var room = readRoom().view().joined().summary();
            Reply result = world(new Request(scope(), Action.PREPARE, 0, room.id(), room.revision(), 0, 0, 0, 0, 0, ZERO_DIGEST));
            assertEquals(Status.OK, result.status()); return result.frame();
        }
        Frame readWorld() throws Exception { return world(new Request(scope(), Action.READ, 0, 0, 0, 0, 0, 0, 0, 0, ZERO_DIGEST)).frame(); }
        Frame awaitPhase(Phase phase) throws Exception {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            for (;;) {
                Frame frame = readWorld(); assertNotNull(frame);
                if (frame.phase() == phase) return frame;
                if (phase != Phase.ABORTED) assertNotEquals(Phase.ABORTED, frame.phase());
                assertTrue(System.nanoTime() - deadline < 0, "world phase deadline"); Thread.sleep(20);
            }
        }
        Reply loaded(Frame frame) throws Exception { return world(new Request(scope(), Action.LOADED, 0, 0, 0, frame.matchId(), frame.generation(), 0, 0, 0, frame.manifest().digest())); }
        Reply move(Frame frame, long sequence, int heading, ModernWorldMap.Cell from) throws Exception {
            return world(new Request(scope(), Action.MOVE, heading, 0, 0, frame.matchId(), frame.generation(), sequence, from.x(), from.y(), ZERO_DIGEST));
        }
        Reply leaveWorld(Frame frame) throws Exception { return world(new Request(scope(), Action.LEAVE, 0, 0, 0, frame.matchId(), frame.generation(), 0, 0, 0, ZERO_DIGEST)); }
        void send(ModernApplicationProtocol.Message request) throws Exception {
            byte[] bytes = ModernApplicationProtocol.encode(request);
            try { socket.getOutputStream().write(bytes); socket.getOutputStream().flush(); }
            finally { Arrays.fill(bytes, (byte) 0); request.close(); }
        }
        ModernApplicationProtocol.Message exchange(ModernApplicationProtocol.Message request) throws Exception {
            send(request); byte[] bytes = ModernApplicationProtocol.readFrame(socket.getInputStream()); assertNotNull(bytes);
            try { var result = ModernApplicationProtocol.decode(bytes); assertEquals(request.requestId(), result.requestId()); return result; }
            finally { Arrays.fill(bytes, (byte) 0); }
        }
        void expectClosed(ModernApplicationProtocol.Message request) throws Exception {
            send(request);
            try { assertNull(ModernApplicationProtocol.readFrame(socket.getInputStream())); }
            catch (java.io.EOFException | java.net.SocketException closed) { /* Actual close, not a read timeout. */ }
        }
        @Override public void close() throws Exception { socket.close(); }
    }
}
