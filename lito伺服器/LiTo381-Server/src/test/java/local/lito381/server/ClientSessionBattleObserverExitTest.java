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
 * Issue #366 (lito-modern#189): a BATTLE observer's voluntary C_D0/C_D9
 * removes only that observer.  The combatants keep their running match,
 * roster and result flow, and the observer can rejoin the same battle.
 */
final class ClientSessionBattleObserverExitTest {
    @TempDir Path temporary;

    private static final int SEED_A = 0x3810_3661;
    private static final int SEED_B = 0x3810_3662;
    private static final int SEED_O = 0x3810_3663;

    @Test
    void observerExitRoomLeavesAloneAndCombatantsStillSettleNormally() throws Exception {
        try (Fixture f = new Fixture(temporary, "c-d0")) {
            long roomId = f.enterBattleWithObserver();
            String observerName = f.observer.litoDisplayName();
            byte[] observerExit = new Protocol.Writer().c(Protocol.S_LITO_EXIT_ROOM)
                    .d(roomId).s(observerName).bytes();

            invokeHandle(f.observer, bytes(Protocol.C_LITO_EXIT_ROOM));

            // Observer: own S_D0 row first, then it is back in the channel lobby.
            assertArrayEquals(observerExit, f.receiveOUntil(Protocol.S_LITO_EXIT_ROOM));
            assertEquals("CHANNEL", field(f.observer, "phase").toString());
            assertEquals(0L, field(f.observer, "currentRoomId"));
            assertTrue(f.observer.isObserver(), "observer toggle is preserved for rejoin");

            // Combatants: only the observer's S_D0 row; their battle is intact.
            assertArrayEquals(observerExit, f.receiveA());
            assertArrayEquals(observerExit, f.receiveB());
            f.assertBattleIntact(roomId);

            // Combatants keep fighting: an authoritative kill still resolves.
            f.resetCiphers();
            BattleRules.Resolution defeat = f.hub.playerDefeated(roomId, f.a, f.b);
            assertTrue(defeat.ended(), "1v1 deathmatch kill must end the match");
            assertEquals(Set.of(f.a.playerObjectId()), defeat.winners());
            assertTrue(defeat.losers().contains(f.b.playerObjectId()));
            for (ClientSession member : f.hub.roomMembers(roomId)) {
                member.serverEndBattle(defeat);
            }
            f.hub.endBattle(roomId);

            assertEquals("RESULT", field(f.a, "phase").toString());
            assertEquals("RESULT", field(f.b, "phase").toString());
            assertEquals(Protocol.S_LITO_END_GAME,
                    receiveUntilOpcode(f.clientA, f.receiverA, Protocol.S_LITO_END_GAME, 16)[0] & 0xFF);
            assertEquals(Protocol.S_LITO_END_GAME,
                    receiveUntilOpcode(f.clientB, f.receiverB, Protocol.S_LITO_END_GAME, 16)[0] & 0xFF);
            f.assertObserverGotNoResult();
            assertEquals("CHANNEL", field(f.observer, "phase").toString());
        }
    }

    @Test
    void observerQuitFightLeavesAloneAndCanRejoinTheSameBattleAsObserver() throws Exception {
        try (Fixture f = new Fixture(temporary, "c-d9")) {
            long roomId = f.enterBattleWithObserver();
            String observerName = f.observer.litoDisplayName();
            byte[] observerExit = new Protocol.Writer().c(Protocol.S_LITO_EXIT_ROOM)
                    .d(roomId).s(observerName).bytes();
            byte[] quitAccepted = new Protocol.Writer().c(Protocol.S_LITO_QUIT_FIGHT).c(0).bytes();

            invokeHandle(f.observer, bytes(Protocol.C_LITO_QUIT_FIGHT));
            assertArrayEquals(quitAccepted, f.receiveO());
            assertArrayEquals(observerExit, f.receiveOUntil(Protocol.S_LITO_EXIT_ROOM));
            assertEquals("CHANNEL", field(f.observer, "phase").toString());
            assertArrayEquals(observerExit, f.receiveA());
            assertArrayEquals(observerExit, f.receiveB());
            f.assertBattleIntact(roomId);

            // Rejoin the still-running battle as an observer.
            GameHub.RoomJoinOutcome rejoin = f.hub.tryJoinRoom(roomId, f.observer);
            assertTrue(rejoin.joined(), "running battle must re-admit the observer");
            assertEquals(List.of(f.a, f.b), f.hub.battleResultCombatants(roomId));
            assertEquals(2, f.hub.roomCombatantCount(roomId));
            setPhase(f.observer, "BATTLE");
            setField(f.observer, "currentRoomId", roomId);

            // Leaving a second time is equally isolated.
            f.resetCiphers();
            invokeHandle(f.observer, bytes(Protocol.C_LITO_EXIT_ROOM));
            assertArrayEquals(observerExit, f.receiveOUntil(Protocol.S_LITO_EXIT_ROOM));
            assertArrayEquals(observerExit, f.receiveA());
            assertArrayEquals(observerExit, f.receiveB());
            f.assertBattleIntact(roomId);

            // A combatant's own withdrawal still settles only the two combatants.
            f.resetCiphers();
            invokeHandle(f.b, bytes(Protocol.C_LITO_QUIT_FIGHT));
            assertEquals("RESULT", field(f.a, "phase").toString());
            assertEquals("RESULT", field(f.b, "phase").toString());
            assertFalse(f.hub.battleStarted(roomId));
            f.assertObserverGotNoResult();
        }
    }

    private static final class Fixture implements AutoCloseable {
        final ServerConfig config;
        final GameStateStore state;
        final GameHub hub;
        final MemoryAccountRepository accounts;
        final ServerSocket listenerA, listenerB, listenerO;
        final Socket clientA, clientB, clientO, serverA, serverB, serverO;
        final ClientSession a, b, observer;
        Protocol.CipherState receiverA, receiverB, receiverO;
        private int generation;

        Fixture(Path temporary, String name) throws Exception {
            config = ServerConfig.load(Path.of(
                    "src/test/resources/server-config/config/server.selftest.properties"));
            state = new GameStateStore(temporary.resolve(name + "-state.properties"), false);
            hub = new GameHub(config);
            accounts = new MemoryAccountRepository("test", "test");
            listenerA = listener(); clientA = connect(listenerA); serverA = listenerA.accept();
            listenerB = listener(); clientB = connect(listenerB); serverB = listenerB.accept();
            listenerO = listener(); clientO = connect(listenerO); serverO = listenerO.accept();
            for (Socket client : List.of(clientA, clientB, clientO)) client.setSoTimeout(2_000);
            String source = config.classicServerNames().get(0);
            a = session(serverA, temporary.resolve(name + "-a"), "ObsExitA", source, 0x3810_3661L);
            b = session(serverB, temporary.resolve(name + "-b"), "ObsExitB", source, 0x3810_3662L);
            observer = session(serverO, temporary.resolve(name + "-o"), "ObsExitO", source,
                    0x3810_3663L);
            setField(observer, "observer", true);
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

        long enterBattleWithObserver() throws Exception {
            for (ClientSession session : List.of(a, b, observer)) {
                hub.connect(session);
                hub.enterChannel(0, session);
            }
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Observer exit", "", 2, GameHub.RoomSettings.ALLOW_OBSERVERS,
                    BattleRules.Mode.DEATHMATCH.wireId(), 0, 0, 0, 0, List.of()));
            assertTrue(hub.tryJoinRoom(roomId, a).joined());
            assertTrue(hub.tryJoinRoom(roomId, b).joined());
            assertTrue(hub.tryJoinRoom(roomId, observer).joined());
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
            prepareBattle(observer, roomId, 32742, 32798);
            return roomId;
        }

        void assertBattleIntact(long roomId) throws Exception {
            assertTrue(hub.battleStarted(roomId), "observer exit must not end the battle");
            assertEquals(BattleRules.Phase.RUNNING, hub.matchSnapshot(roomId).phase());
            assertEquals(List.of(a, b), hub.roomMembers(roomId));
            assertEquals(List.of(a, b), hub.battleResultCombatants(roomId));
            assertEquals("BATTLE", field(a, "phase").toString());
            assertEquals("BATTLE", field(b, "phase").toString());
            assertEquals(roomId, field(a, "currentRoomId"));
            assertEquals(roomId, field(b, "currentRoomId"));
            assertEquals(0, state.tournamentStatistics(a.sourceCharacterIdentity()).wins());
            assertEquals(0, state.tournamentStatistics(b.sourceCharacterIdentity()).losses());
        }

        /** Drains whatever the observer received and rejects any result controller. */
        void assertObserverGotNoResult() throws Exception {
            clientO.setSoTimeout(300);
            try {
                for (int index = 0; index < 64; index++) {
                    byte[] packet = receiverO.decrypt(
                            Protocol.readFrame(clientO.getInputStream(), 8_192));
                    int opcode = packet[0] & 0xFF;
                    assertFalse(opcode == Protocol.S_LITO_END_GAME,
                            "observer that already left must not receive S_D3");
                    assertFalse(opcode == Protocol.S_LITO_QUIT_FIGHT,
                            "observer that already left must not receive S_F0");
                }
            } catch (SocketTimeoutException drained) {
                // No further frames: expected.
            } finally {
                clientO.setSoTimeout(2_000);
            }
        }

        /** Discards previously queued frames by rotating every cipher to a new seed. */
        void resetCiphers() throws Exception {
            drain(clientA);
            drain(clientB);
            drain(clientO);
            int offset = 0x100 * generation++;
            setField(a, "outgoing", new Protocol.CipherState(SEED_A + offset));
            setField(b, "outgoing", new Protocol.CipherState(SEED_B + offset));
            setField(observer, "outgoing", new Protocol.CipherState(SEED_O + offset));
            receiverA = new Protocol.CipherState(SEED_A + offset);
            receiverB = new Protocol.CipherState(SEED_B + offset);
            receiverO = new Protocol.CipherState(SEED_O + offset);
        }

        byte[] receiveA() throws Exception { return receiverA.decrypt(frame(clientA)); }
        byte[] receiveB() throws Exception { return receiverB.decrypt(frame(clientB)); }
        byte[] receiveO() throws Exception { return receiverO.decrypt(frame(clientO)); }
        byte[] receiveOUntil(int opcode) throws Exception {
            return receiveUntilOpcode(clientO, receiverO, opcode, 8);
        }

        @Override
        public void close() throws Exception {
            for (AutoCloseable closeable : List.<AutoCloseable>of(
                    clientA, clientB, clientO, serverA, serverB, serverO,
                    listenerA, listenerB, listenerO, accounts, hub)) {
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

    private static byte[] receiveUntilOpcode(Socket client, Protocol.CipherState receiver,
                                             int opcode, int maximumFrames) throws Exception {
        for (int index = 0; index < maximumFrames; index++) {
            byte[] packet = receiver.decrypt(Protocol.readFrame(client.getInputStream(), 8_192));
            if ((packet[0] & 0xFF) == opcode) return packet;
        }
        throw new AssertionError("missing encrypted opcode " + opcode
                + " within " + maximumFrames + " frames");
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
