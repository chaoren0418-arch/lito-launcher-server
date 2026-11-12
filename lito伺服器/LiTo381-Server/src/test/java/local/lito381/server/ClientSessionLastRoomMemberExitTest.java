package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Issue #370 (lito-modern#299, T040 3/4): the last member's room exit must
 * finish with the S_CC room list even when the room's directory row is
 * already gone, and an emptied default room must still publish its reset row.
 */
final class ClientSessionLastRoomMemberExitTest {
    @TempDir Path temporary;

    private static final int SEED_A = 0x3810_3701;
    private static final int SEED_B = 0x3810_3702;
    private static final int SEED_P = 0x3810_3703;

    @Test
    void lastResultMemberLeavesAfterTheEmptyCustomRoomWasAlreadyRemoved() throws Exception {
        try (Fixture f = new Fixture(temporary, "removed")) {
            long defaultRoomId = f.defaultRoomId();
            long roomId = f.settleBattleBetweenAAndB();
            assertEquals("RESULT", field(f.a, "phase").toString());
            assertEquals("RESULT", field(f.b, "phase").toString());

            // First result member returns: the room still holds A, so it stays.
            f.resetCiphers();
            invokeHandle(f.b, bytes(Protocol.C_LITO_EXIT_ROOM));
            assertArrayEquals(exitRow(roomId, f.b), f.receiveUntil(f.clientB, f.receiverB,
                    Protocol.S_LITO_EXIT_ROOM));
            assertEquals(defaultRoomId, roomInfoId(f.receiveUntil(f.clientB, f.receiverB,
                    Protocol.S_LITO_ROOM_INFO)));
            assertEquals("CHANNEL", field(f.b, "phase").toString());
            assertEquals(List.of(f.a), f.hub.roomMembers(roomId));
            assertTrue(f.hub.roomExists(0, roomId));

            // The hub drops the now-empty custom room before A's own result
            // return is processed.  A still carries the stale room id.
            assertTrue(f.hub.leaveRoom(roomId, f.a), "empty custom room is deleted");
            assertFalse(f.hub.roomExists(0, roomId));
            assertEquals("RESULT", field(f.a, "phase").toString());
            assertEquals(roomId, field(f.a, "currentRoomId"));

            // Last member returns: no directory row to reset, the exit still
            // completes through CHANNEL and the full room-list publication.
            f.resetCiphers();
            invokeHandle(f.a, bytes(Protocol.C_LITO_EXIT_ROOM));
            assertArrayEquals(exitRow(roomId, f.a), f.receiveUntil(f.clientA, f.receiverA,
                    Protocol.S_LITO_EXIT_ROOM));
            assertEquals(defaultRoomId, roomInfoId(f.receiveUntil(f.clientA, f.receiverA,
                    Protocol.S_LITO_ROOM_INFO)));
            assertEquals("CHANNEL", field(f.a, "phase").toString());
            assertEquals(0L, field(f.a, "currentRoomId"));
            assertEquals(List.of(defaultRoomId), f.hub.roomSnapshots(0).stream()
                    .map(GameHub.RoomSnapshot::id).toList());
        }
    }

    @Test
    void lastMemberLeavingTheDefaultRoomStillBroadcastsItsResetRow() throws Exception {
        try (Fixture f = new Fixture(temporary, "default")) {
            long defaultRoomId = f.defaultRoomId();
            f.enterChannel();
            assertTrue(f.hub.tryJoinRoom(defaultRoomId, f.a).joined());
            setPhase(f.a, "ROOM");
            setField(f.a, "currentRoomId", defaultRoomId);
            // The peer waits inside its own room: it is a channel member but
            // not in the CHANNEL phase, so the generic lobby refresh skips it
            // and every S_CC it receives is the default-room reset broadcast.
            long peerRoomId = f.hub.createRoom(0, new GameHub.RoomSettings(
                    "Peer room", "", 2, GameHub.RoomSettings.ALLOW_OBSERVERS,
                    BattleRules.Mode.DEATHMATCH.wireId(), 0, 0, 0, 0, List.of()));
            assertTrue(f.hub.tryJoinRoom(peerRoomId, f.peer).joined());
            setPhase(f.peer, "ROOM");
            setField(f.peer, "currentRoomId", peerRoomId);

            f.resetCiphers();
            invokeHandle(f.a, bytes(Protocol.C_LITO_EXIT_ROOM));

            assertArrayEquals(exitRow(defaultRoomId, f.a), f.receiveUntil(f.clientA, f.receiverA,
                    Protocol.S_LITO_EXIT_ROOM));
            assertEquals("CHANNEL", field(f.a, "phase").toString());
            assertEquals(0L, field(f.a, "currentRoomId"));
            assertTrue(f.hub.roomExists(0, defaultRoomId), "default room is reset, not removed");
            assertTrue(f.hub.roomMembers(defaultRoomId).isEmpty());

            // Peer: the complete six-frame reset row of the default room.
            byte[] first = f.receiveUntil(f.clientP, f.receiverP, Protocol.S_LITO_ROOM_INFO);
            assertEquals(0, first[1] & 0xFF, "reset row starts with the add-if-missing frame");
            assertEquals(defaultRoomId, roomInfoId(first));
            for (int subOpcode = 1; subOpcode <= 5; subOpcode++) {
                byte[] frame = f.receiverP.decrypt(frame(f.clientP));
                assertEquals(Protocol.S_LITO_ROOM_INFO, frame[0] & 0xFF);
                assertEquals(subOpcode, frame[1] & 0xFF);
                assertEquals(defaultRoomId, roomInfoId(frame));
            }
            assertEquals("ROOM", field(f.peer, "phase").toString());
            assertEquals(List.of(f.peer), f.hub.roomMembers(peerRoomId));
        }
    }

    private static byte[] exitRow(long roomId, ClientSession session) {
        return new Protocol.Writer().c(Protocol.S_LITO_EXIT_ROOM)
                .d(roomId).s(session.litoDisplayName()).bytes();
    }

    /** S_CC frames are {@code opcode, sub-opcode, room id, ...}. */
    private static long roomInfoId(byte[] packet) {
        Protocol.Reader in = new Protocol.Reader(packet);
        assertEquals(Protocol.S_LITO_ROOM_INFO, in.c());
        in.c();
        return in.d();
    }

    private static final class Fixture implements AutoCloseable {
        final ServerConfig config;
        final GameStateStore state;
        final GameHub hub;
        final MemoryAccountRepository accounts;
        final ServerSocket listenerA, listenerB, listenerP;
        final Socket clientA, clientB, clientP, serverA, serverB, serverP;
        final ClientSession a, b, peer;
        Protocol.CipherState receiverA, receiverB, receiverP;
        private int generation;

        Fixture(Path temporary, String name) throws Exception {
            config = ServerConfig.load(Path.of(
                    "src/test/resources/server-config/config/server.selftest.properties"));
            state = new GameStateStore(temporary.resolve(name + "-state.properties"), false);
            hub = new GameHub(config);
            accounts = new MemoryAccountRepository("test", "test");
            listenerA = listener(); clientA = connect(listenerA); serverA = listenerA.accept();
            listenerB = listener(); clientB = connect(listenerB); serverB = listenerB.accept();
            listenerP = listener(); clientP = connect(listenerP); serverP = listenerP.accept();
            for (Socket client : List.of(clientA, clientB, clientP)) client.setSoTimeout(2_000);
            String source = config.classicServerNames().get(0);
            a = session(serverA, temporary.resolve(name + "-a"), "LastExitA", source, 0x3810_3701L);
            b = session(serverB, temporary.resolve(name + "-b"), "LastExitB", source, 0x3810_3702L);
            peer = session(serverP, temporary.resolve(name + "-p"), "LastExitP", source,
                    0x3810_3703L);
            resetCiphers();
        }

        private ClientSession session(Socket socket, Path capture, String name,
                                      String source, long objectId) throws Exception {
            ClientSession session = new ClientSession(socket, ClientSession.Endpoint.GAME,
                    config, accounts, state, new CaptureLog(capture), hub);
            CharacterSummary character = new CharacterSummary(name, "", 0, 0, 50, 500, 100,
                    10, 12, 12, 12, 12, 18, 12, 18, 12,
                    objectId, 100, 0, 0, 0, 32740, 32798, 4,
                    12, 40, 1, 12, 12, 0);
            setField(session, "account", name + "-account");
            setField(session, "selectedClassicServerName", source);
            setField(session, "selectedCharacter", name);
            setField(session, "characters", List.of(character));
            setField(session, "inventory", new RuntimeInventory());
            SourceEquipmentMagicStats magic = SourceEquipmentMagicStats.none();
            SourceEquipmentVitalStats vital = SourceEquipmentVitalStats.none();
            SourceEquippedFireResistance fire = new SourceEquippedFireResistance(0, 0);
            setField(session, "sourceEquipmentMagicStats", magic);
            setField(session, "sourceEquipmentVitalStats", vital);
            setField(session, "sourceEquippedFireResistance", fire);
            setField(session, "nativeTournamentMagicBaseline", magic);
            setField(session, "nativeTournamentVitalBaseline", vital);
            setField(session, "nativeTournamentFireResistanceBaseline", fire);
            setField(session, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(character));
            setField(session, "sourceInventoryWeightStats",
                    SourceInventoryWeightStats.none(100.0));
            setField(session, "retainedSourceInventoryWeight", 0);
            setField(session, "currentChannelId", 0);
            setField(session, "hubChannelId", 0);
            setPhase(session, "CHANNEL");
            return session;
        }

        void enterChannel() {
            for (ClientSession session : List.of(a, b, peer)) {
                hub.connect(session);
                hub.enterChannel(0, session);
            }
        }

        /** The channel's permanent default room (never removed, only reset). */
        long defaultRoomId() {
            List<Long> ids = hub.roomSnapshots(0).stream().map(GameHub.RoomSnapshot::id)
                    .filter(id -> NativeRoomRules.testRoom(hub, id)).toList();
            assertEquals(1, ids.size(), "channel 0 owns exactly one default room");
            return ids.get(0);
        }

        /** A and B fight a real 1v1 in a custom room and both reach RESULT. */
        long settleBattleBetweenAAndB() throws Exception {
            enterChannel();
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Last exit", "", 2, GameHub.RoomSettings.ALLOW_OBSERVERS,
                    BattleRules.Mode.DEATHMATCH.wireId(), 0, 0, 0, 0, List.of()));
            assertTrue(hub.tryJoinRoom(roomId, a).joined());
            assertTrue(hub.tryJoinRoom(roomId, b).joined());
            hub.setForce(roomId, a, 0);
            hub.setForce(roomId, b, 1);
            hub.setReady(roomId, a, true);
            hub.setReady(roomId, b, true);
            Method countdown = GameHub.class.getDeclaredMethod("countdownWork", long.class);
            countdown.setAccessible(true);
            long future = System.nanoTime() + 60_000_000_000L;
            countdown.invoke(hub, future);
            countdown.invoke(hub, future + 2_000_000_000L);
            countdown.invoke(hub, future + 4_000_000_000L);
            assertTrue(hub.battleStarted(roomId), "fixture battle must be running");
            assertEquals(List.of(a, b), hub.battleResultCombatants(roomId));
            prepareBattle(a, roomId, 32740, 32798);
            prepareBattle(b, roomId, 32741, 32798);

            BattleRules.Resolution defeat = hub.playerDefeated(roomId, a, b);
            assertTrue(defeat.ended(), "1v1 deathmatch kill must end the match");
            assertEquals(Set.of(a.playerObjectId()), defeat.winners());
            for (ClientSession member : hub.roomMembers(roomId)) {
                member.serverEndBattle(defeat);
            }
            hub.endBattle(roomId);
            assertFalse(hub.battleStarted(roomId));
            return roomId;
        }

        /** Discards previously queued frames by rotating every cipher to a new seed. */
        void resetCiphers() throws Exception {
            drain(clientA);
            drain(clientB);
            drain(clientP);
            int offset = 0x100 * generation++;
            setField(a, "outgoing", new Protocol.CipherState(SEED_A + offset));
            setField(b, "outgoing", new Protocol.CipherState(SEED_B + offset));
            setField(peer, "outgoing", new Protocol.CipherState(SEED_P + offset));
            receiverA = new Protocol.CipherState(SEED_A + offset);
            receiverB = new Protocol.CipherState(SEED_B + offset);
            receiverP = new Protocol.CipherState(SEED_P + offset);
        }

        byte[] receiveUntil(Socket client, Protocol.CipherState receiver, int opcode)
                throws Exception {
            for (int index = 0; index < 32; index++) {
                byte[] packet = receiver.decrypt(frame(client));
                if ((packet[0] & 0xFF) == opcode) return packet;
            }
            throw new AssertionError("missing encrypted opcode " + opcode + " within 32 frames");
        }

        @Override
        public void close() throws Exception {
            for (AutoCloseable closeable : List.<AutoCloseable>of(
                    clientA, clientB, clientP, serverA, serverB, serverP,
                    listenerA, listenerB, listenerP, accounts, hub)) {
                try { closeable.close(); } catch (Exception ignored) { }
            }
        }
    }

    private static void drain(Socket client) throws Exception {
        client.setSoTimeout(150);
        try {
            while (true) Protocol.readFrame(client.getInputStream(), 8_192);
        } catch (SocketTimeoutException drained) {
            // queue empty
        } finally {
            client.setSoTimeout(2_000);
        }
    }

    private static byte[] frame(Socket client) throws Exception {
        return Protocol.readFrame(client.getInputStream(), 8_192);
    }

    private static void prepareBattle(ClientSession session, long roomId, int x, int y)
            throws Exception {
        setPhase(session, "BATTLE");
        setField(session, "currentRoomId", roomId);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        setField(session, "playerHp", 500);
        setField(session, "playerMaxHp", 500);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
    }

    private static ServerSocket listener() throws Exception {
        return new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
    }

    private static Socket connect(ServerSocket listener) throws Exception {
        return new Socket("127.0.0.1", listener.getLocalPort());
    }

    private static void invokeHandle(ClientSession session, byte[] payload) throws Exception {
        Method method = ClientSession.class.getDeclaredMethod("handle", byte[].class);
        method.setAccessible(true);
        method.invoke(session, payload);
    }

    private static byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int index = 0; index < values.length; index++) result[index] = (byte) values[index];
        return result;
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setPhase(ClientSession session, String value) throws Exception {
        Class phaseType = Class.forName(ClientSession.class.getName() + "$Phase");
        setField(session, "phase", Enum.valueOf(phaseType, value));
    }
}
