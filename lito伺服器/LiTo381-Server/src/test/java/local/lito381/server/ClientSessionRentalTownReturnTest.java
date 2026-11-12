package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Issue #368 (lito-modern T040 handover 1/4): the rental-town scene runs in
 * Phase.BATTLE, so the first BATTLE guard of quitFight()/exitRoom() used to
 * swallow the native return chain as an (empty) battle withdrawal.  Only a
 * transition the server itself staged may pass that guard.
 */
final class ClientSessionRentalTownReturnTest {
    private static final long TOWN_ROOM = 0x5000_0001L;
    private static final int SEED_LEAVER = 0x3810_3681;
    private static final int SEED_PEER = 0x3810_3682;

    @TempDir Path temporary;

    @Test
    void restartReturnChainLeavesTheTownWithoutBattleSettlementAndCanReenter()
            throws Exception {
        try (Fixture f = new Fixture(temporary, "return")) {
            String leaverName = f.leaver.litoDisplayName();
            long leaverObject = f.leaver.playerObjectId();
            List<Long> rentalObjectIds =
                    ((RuntimeInventory) field(f.leaver, "rentalTownInventory")).items()
                            .stream().map(RuntimeInventory.Item::objectId).toList();
            assertFalse(rentalObjectIds.isEmpty(), "fixture must hold a rental loadout");

            // C10 -> S65
            handle(f.leaver, bytes(Protocol.C_RESTART));
            assertArrayEquals(Protocol.restart(), f.receiveLeaver());
            assertEquals("RESTART_AWAIT_QUIT_FIGHT",
                    field(f.leaver, "pendingTransition").toString());
            assertEquals("BATTLE", field(f.leaver, "phase").toString());

            // C_D9 -> S_F0(0)
            handle(f.leaver, bytes(Protocol.C_LITO_QUIT_FIGHT));
            assertArrayEquals(new Protocol.Writer()
                            .c(Protocol.S_LITO_QUIT_FIGHT).c(0).bytes(),
                    f.receiveLeaver());
            assertEquals("RESTART_AWAIT_RETURN",
                    field(f.leaver, "pendingTransition").toString());
            assertEquals("BATTLE", field(f.leaver, "phase").toString());
            assertTrue((boolean) field(f.leaver, "rentalTownRoomState"));

            // C_D0 -> S_D0 -> (rental inventory removal) -> S_F5
            handle(f.leaver, bytes(Protocol.C_LITO_EXIT_ROOM));
            byte[] ownExit = new Protocol.Writer().c(Protocol.S_LITO_EXIT_ROOM)
                    .d(TOWN_ROOM).s(leaverName).bytes();
            assertArrayEquals(ownExit, f.receiveLeaver());
            int removeInventory = Protocol.removeInventory(1)[0] & 0xFF;
            List<byte[]> removed = new ArrayList<>();
            byte[] last;
            while (true) {
                last = f.receiveLeaver();
                if ((last[0] & 0xFF) != removeInventory) break;
                removed.add(last);
            }
            assertArrayEquals(Protocol.allowPostTransition(), last,
                    "S_F5 must directly follow the rental inventory removal");
            for (long objectId : rentalObjectIds) {
                byte[] expected = Protocol.removeInventory(objectId);
                assertTrue(removed.stream().anyMatch(
                                packet -> java.util.Arrays.equals(packet, expected)),
                        "rental item must be removed object=" + objectId);
            }
            f.assertSilent(f.leaverClient, "nothing may follow S_F5");

            assertEquals("LOBBY", field(f.leaver, "phase").toString());
            assertEquals("RESTART_AWAIT_SOURCE_REFRESH",
                    field(f.leaver, "pendingTransition").toString());
            assertFalse((boolean) field(f.leaver, "rentalTownRoomState"));
            assertFalse((boolean) field(f.leaver, "rentalTownTransferStoneGranted"));
            assertFalse((boolean) field(f.leaver, "rentalTownTransferPending"));
            assertEquals(0, field(f.leaver, "rentalTownMapId"));
            assertEquals(-1, field(f.leaver, "rentalItemSetSelection"));
            assertEquals(0L, field(f.leaver, "currentRoomId"));
            assertEquals(0,
                    ((RuntimeInventory) field(f.leaver, "rentalTownInventory")).size());
            assertTrue(f.hub.roomMembers(TOWN_ROOM).isEmpty());
            assertFalse(f.hub.battleStarted(TOWN_ROOM));

            // The remaining villager only sees the departure, never a result.
            assertArrayEquals(Protocol.removeObject(leaverObject), f.receivePeer());
            assertArrayEquals(ownExit, f.receivePeer());
            f.assertSilent(f.peerClient, "peer must not receive a battle result");
            assertEquals("BATTLE", field(f.peer, "phase").toString());
            assertTrue((boolean) field(f.peer, "rentalTownRoomState"));

            // Re-entering the village.  The native C_C8 source refresh that
            // clears RESTART_AWAIT_SOURCE_REFRESH is not exercised here.
            setEnum(f.leaver, "pendingTransition", "PendingTransition", "NONE");
            f.resetCiphers();
            invoke(f.leaver, "sendLitoServerList", new Class<?>[0]);
            handle(f.leaver, new Protocol.Writer()
                    .c(Protocol.C_LITO_ENTER_CHANNEL).c(0).bytes());
            assertEquals(0, field(f.leaver, "rentalItemSetSelection"));
            handle(f.leaver, bytes(Protocol.C_LITO_QUIT_FIGHT));
            assertTrue((boolean) field(f.leaver, "rentalTownJoinPending"));
            handle(f.leaver, new Protocol.Writer().c(Protocol.C_LITO_JOIN)
                    .d(TOWN_ROOM).s("").bytes());
            Protocol.Reader accepted = new Protocol.Reader(receiveUntilOpcode(
                    f.leaverClient, f.leaverReceiver, Protocol.S_LITO_JOIN_RESULT, 8));
            assertEquals(Protocol.S_LITO_JOIN_RESULT, accepted.c());
            assertEquals(0, accepted.c());
            assertEquals(TOWN_ROOM, accepted.d());
            assertEquals("BATTLE", field(f.leaver, "phase").toString());
            assertTrue((boolean) field(f.leaver, "rentalTownRoomState"));
            assertEquals(TOWN_ROOM, field(f.leaver, "currentRoomId"));
            assertEquals(500, field(f.leaver, "rentalTownMapId"));
            assertTrue(((RuntimeInventory) field(f.leaver, "rentalTownInventory")).size() > 0);
        }
    }

    @Test
    void unstagedOutOfOrderAndDuplicateRequestsStayOrdinaryWithdrawals()
            throws Exception {
        try (Fixture f = new Fixture(temporary, "negative")) {
            // No pending transition: both entries keep the pre-#368 behaviour.
            handle(f.leaver, bytes(Protocol.C_LITO_QUIT_FIGHT));
            f.assertTownUntouched("NONE", "C_D9 without a staged transition");
            handle(f.leaver, bytes(Protocol.C_LITO_EXIT_ROOM));
            f.assertTownUntouched("NONE", "C_D0 without a staged transition");

            // C_D0 before the restart's C_D9 was acknowledged is not admitted.
            handle(f.leaver, bytes(Protocol.C_RESTART));
            assertArrayEquals(Protocol.restart(), f.receiveLeaver());
            handle(f.leaver, bytes(Protocol.C_LITO_EXIT_ROOM));
            f.assertTownUntouched("RESTART_AWAIT_QUIT_FIGHT", "C_D0 before C_D9");

            // First C_D9 is acknowledged, a duplicate is not acknowledged again.
            handle(f.leaver, bytes(Protocol.C_LITO_QUIT_FIGHT));
            assertArrayEquals(new Protocol.Writer()
                            .c(Protocol.S_LITO_QUIT_FIGHT).c(0).bytes(),
                    f.receiveLeaver());
            handle(f.leaver, bytes(Protocol.C_LITO_QUIT_FIGHT));
            f.assertTownUntouched("RESTART_AWAIT_RETURN", "duplicate C_D9");

            // The staged return still completes exactly once afterwards.
            handle(f.leaver, bytes(Protocol.C_LITO_EXIT_ROOM));
            assertArrayEquals(new Protocol.Writer().c(Protocol.S_LITO_EXIT_ROOM)
                            .d(TOWN_ROOM).s(f.leaver.litoDisplayName()).bytes(),
                    f.receiveLeaver());
            assertArrayEquals(Protocol.allowPostTransition(), receiveUntilOpcode(
                    f.leaverClient, f.leaverReceiver,
                    Protocol.S_LITO_VS_SERVER_ALLOW_JOIN, 64));
            assertEquals("LOBBY", field(f.leaver, "phase").toString());
            assertFalse((boolean) field(f.leaver, "rentalTownRoomState"));

            // A replayed C_D0 after completion finds no town and no room.
            f.resetCiphers();
            handle(f.leaver, bytes(Protocol.C_LITO_EXIT_ROOM));
            f.assertSilent(f.leaverClient, "replayed C_D0 after the return");
            assertEquals("LOBBY", field(f.leaver, "phase").toString());
            assertEquals("RESTART_AWAIT_SOURCE_REFRESH",
                    field(f.leaver, "pendingTransition").toString());
        }
    }

    @Test
    void stagedTransferStoneQuitFightLeavesTheTownFromBattlePhase() throws Exception {
        try (Fixture f = new Fixture(temporary, "transfer")) {
            String leaverName = f.leaver.litoDisplayName();
            long leaverObject = f.leaver.playerObjectId();
            // Current-endpoint destination: no configured teleport endpoint.
            setField(f.leaver, "rentalTownTransferPending", true);

            handle(f.leaver, bytes(Protocol.C_LITO_QUIT_FIGHT));

            assertArrayEquals(new Protocol.Writer()
                            .c(Protocol.S_LITO_QUIT_FIGHT).c(0).bytes(),
                    receiveUntilOpcode(f.leaverClient, f.leaverReceiver,
                            Protocol.S_LITO_QUIT_FIGHT, 64));
            assertEquals("LOBBY", field(f.leaver, "phase").toString());
            assertFalse((boolean) field(f.leaver, "rentalTownRoomState"));
            assertFalse((boolean) field(f.leaver, "rentalTownTransferPending"));
            assertEquals(-1, field(f.leaver, "rentalItemSetSelection"));
            assertEquals(0L, field(f.leaver, "currentRoomId"));

            assertArrayEquals(Protocol.removeObject(leaverObject), f.receivePeer());
            assertArrayEquals(new Protocol.Writer().c(Protocol.S_LITO_EXIT_ROOM)
                            .d(TOWN_ROOM).s(leaverName).bytes(),
                    f.receivePeer());
            f.assertSilent(f.peerClient, "peer must not receive a battle result");
            assertEquals("BATTLE", field(f.peer, "phase").toString());
        }
    }

    /** Two villagers standing in the same BATTLE-phase rental-town scene. */
    private static final class Fixture implements AutoCloseable {
        final ServerConfig config;
        final GameStateStore state;
        final GameHub hub;
        final MemoryAccountRepository accounts;
        final ServerSocket leaverListener, peerListener;
        final Socket leaverClient, leaverServer, peerClient, peerServer;
        final ClientSession leaver, peer;
        Protocol.CipherState leaverReceiver, peerReceiver;
        private int generation;

        Fixture(Path temporary, String name) throws Exception {
            String base = Files.readString(Path.of(
                    "src/test/resources/server-config/config/server.selftest.properties"));
            Path properties = temporary.resolve(name + "-rental.properties");
            Files.writeString(properties, base + "\n"
                    + "lito.rental.town.directory.enabled=true\n"
                    + "lito.rental.town.room.id=1342177281\n"
                    + "lito.rental.town.directory.name=Rental Village Probe\n"
                    + "lito.rental.town.directory.host=127.0.0.1\n"
                    + "lito.rental.town.directory.port=32000\n"
                    + "lito.rental.town.map=500\n");
            config = ServerConfig.load(properties);
            state = new GameStateStore(temporary.resolve(name + "-state.properties"), false);
            hub = new GameHub(config);
            accounts = new MemoryAccountRepository("test", "test");
            leaverListener = listener();
            leaverClient = new Socket("127.0.0.1", leaverListener.getLocalPort());
            leaverServer = leaverListener.accept();
            peerListener = listener();
            peerClient = new Socket("127.0.0.1", peerListener.getLocalPort());
            peerServer = peerListener.accept();
            leaver = new ClientSession(leaverServer, ClientSession.Endpoint.GAME, config,
                    accounts, state, new CaptureLog(temporary.resolve(name + "-leaver")), hub);
            peer = new ClientSession(peerServer, ClientSession.Endpoint.GAME, config,
                    accounts, state, new CaptureLog(temporary.resolve(name + "-peer")), hub);
            hub.connect(leaver);
            hub.connect(peer);
            resetCiphers();
            enterTown(leaver, "ReturnLeaver", 0x3681L);
            enterTown(peer, "ReturnPeer", 0x3682L);
            assertEquals("BATTLE", field(leaver, "phase").toString(),
                    "the rental-town scene must run in Phase.BATTLE");
            assertEquals("BATTLE", field(peer, "phase").toString());
            assertTrue(hub.roomMembers(TOWN_ROOM).isEmpty(),
                    "the synthetic town room never enters GameHub");
            resetCiphers();
        }

        private static void enterTown(ClientSession session, String name, long objectId)
                throws Exception {
            setEnum(session, "phase", "Phase", "ROOM");
            setField(session, "account", name + "-account");
            setField(session, "selectedCharacter", name);
            setField(session, "characters", List.of(new CharacterSummary(
                    name, "", 1, 0, 50, 500, 100,
                    10, 13, 10, 10, 9, 8, 11, 8, 11,
                    objectId, 0, 0, 0, 0, 0, 0, 0)));
            setField(session, "currentRoomId", TOWN_ROOM);
            setField(session, "rentalTownRoomState", true);
            setField(session, "rentalTownMapId", 500);
            setField(session, "rentalItemSetSelection", 0);
            setField(session, "rentalTownDirectoryAdvertised", true);
            invoke(session, "startRentalTownScene", new Class<?>[0]);
        }

        /** Discards queued frames and rotates both ciphers to a fresh seed. */
        void resetCiphers() throws Exception {
            drain(leaverClient);
            drain(peerClient);
            int offset = 0x100 * generation++;
            setField(leaver, "outgoing", new Protocol.CipherState(SEED_LEAVER + offset));
            setField(peer, "outgoing", new Protocol.CipherState(SEED_PEER + offset));
            leaverReceiver = new Protocol.CipherState(SEED_LEAVER + offset);
            peerReceiver = new Protocol.CipherState(SEED_PEER + offset);
        }

        byte[] receiveLeaver() throws Exception {
            return leaverReceiver.decrypt(
                    Protocol.readFrame(leaverClient.getInputStream(), 8_192));
        }

        byte[] receivePeer() throws Exception {
            return peerReceiver.decrypt(
                    Protocol.readFrame(peerClient.getInputStream(), 8_192));
        }

        void assertSilent(Socket client, String message) throws Exception {
            client.setSoTimeout(250);
            try {
                byte[] frame = Protocol.readFrame(client.getInputStream(), 8_192);
                throw new AssertionError(message + ": unexpected frame length="
                        + frame.length);
            } catch (SocketTimeoutException silent) {
                // expected
            } finally {
                client.setSoTimeout(2_000);
            }
        }

        /** The leaver is still a villager and neither side received anything. */
        void assertTownUntouched(String pending, String message) throws Exception {
            assertSilent(leaverClient, message);
            assertSilent(peerClient, message);
            assertEquals("BATTLE", field(leaver, "phase").toString(), message);
            assertEquals(pending, field(leaver, "pendingTransition").toString(), message);
            assertTrue((boolean) field(leaver, "rentalTownRoomState"), message);
            assertEquals(TOWN_ROOM, field(leaver, "currentRoomId"), message);
            assertNotEquals(0,
                    ((RuntimeInventory) field(leaver, "rentalTownInventory")).size(),
                    message);
            assertEquals("BATTLE", field(peer, "phase").toString(), message);
        }

        @Override
        public void close() {
            for (AutoCloseable closeable : List.<AutoCloseable>of(
                    leaverClient, peerClient, leaverServer, peerServer,
                    leaverListener, peerListener, accounts, hub)) {
                try { closeable.close(); } catch (Exception ignored) { }
            }
        }
    }

    private static void drain(Socket client) throws Exception {
        client.setSoTimeout(200);
        try {
            while (true) Protocol.readFrame(client.getInputStream(), 8_192);
        } catch (SocketTimeoutException drained) {
            // queue empty
        } finally {
            client.setSoTimeout(2_000);
        }
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

    private static ServerSocket listener() throws Exception {
        return new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
    }

    private static void handle(ClientSession session, byte[] payload) throws Exception {
        invoke(session, "handle", new Class<?>[] {byte[].class}, (Object) payload);
    }

    private static void invoke(ClientSession session, String name,
                               Class<?>[] parameters, Object... arguments) throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        method.invoke(session, arguments);
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
    private static void setEnum(ClientSession session, String fieldName,
                                String enumName, String value) throws Exception {
        Class type = Class.forName(ClientSession.class.getName() + "$" + enumName);
        setField(session, fieldName, Enum.valueOf(type, value));
    }
}
