package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ProtocolRoomControlTest {
    @Test void acceptsOnlyTheNativeReadyEnvelopeAndProvenValues() {
        assertEquals(new Protocol.LitoReadyRequest(
                        Protocol.LitoReadyOperation.OWNER_START, null),
                Protocol.litoReadyRequest(new byte[] {(byte) 0xD2}));
        assertEquals(new Protocol.LitoReadyRequest(
                        Protocol.LitoReadyOperation.PARTICIPANT_STATE,
                        Protocol.LitoParticipantState.NOT_READY),
                Protocol.litoReadyRequest(new byte[] {(byte) 0xD2, 0}));
        assertEquals(new Protocol.LitoReadyRequest(
                        Protocol.LitoReadyOperation.PARTICIPANT_STATE,
                        Protocol.LitoParticipantState.READY),
                Protocol.litoReadyRequest(new byte[] {(byte) 0xD2, 1}));
        assertEquals(new Protocol.LitoReadyRequest(
                        Protocol.LitoReadyOperation.PARTICIPANT_STATE,
                        Protocol.LitoParticipantState.SHOP_LIST),
                Protocol.litoReadyRequest(new byte[] {(byte) 0xD2, 2}));

        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoReadyRequest(new byte[] {(byte) 0xD2, 3}));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoReadyRequest(new byte[] {(byte) 0xD2, 1, 0}));
    }

    @Test void preservesTheNativeRoomShopStateInReadyUpdates() {
        assertEquals("D1 54 65 73 74 00 02", Protocol.hex(Protocol.readyState(
                "Test", Protocol.LitoParticipantState.SHOP_LIST)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.readyState("Test", (Protocol.LitoParticipantState) null));
    }

    @Test void acceptsOnlyTheTwoNativeForceBranches() {
        assertEquals(Protocol.LitoForce.FORCE_0,
                Protocol.litoForceRequest(new byte[] {(byte) 0xD3, 0}));
        assertEquals(Protocol.LitoForce.FORCE_1,
                Protocol.litoForceRequest(new byte[] {(byte) 0xD3, 1}));

        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoForceRequest(new byte[] {(byte) 0xD3, 2}));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoForceRequest(new byte[] {(byte) 0xD3}));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoForceRequest(new byte[] {(byte) 0xD3, 1, 0}));
    }

    @Test void kickRequestRequiresTheExactNativeCsEnvelope() {
        assertEquals("ArenaBot", Protocol.litoKickRequest(new byte[] {
                (byte) 0xD4, 'A', 'r', 'e', 'n', 'a', 'B', 'o', 't', 0
        }));
        assertEquals("", Protocol.litoKickRequest(new byte[] {(byte) 0xD4, 0}));

        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoKickRequest(new byte[] {(byte) 0xD4}));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoKickRequest(new byte[] {(byte) 0xD4, 'A', 0, 0}));
    }

    @Test void roomWhoRequiresTheExactTwoNativeCdEnvelope() {
        assertEquals(0, Protocol.litoRoomWhoRequest(
                new Protocol.Writer().c(Protocol.C_LITO_ROOM_WHO).d(0).bytes()));
        assertEquals(0x1122_3344L, Protocol.litoRoomWhoRequest(
                new Protocol.Writer().c(Protocol.C_LITO_ROOM_WHO).d(0x1122_3344L).bytes()));

        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomWhoRequest(
                        new byte[] {(byte) Protocol.C_LITO_ROOM_WHO}));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoRoomWhoRequest(
                        new Protocol.Writer().c(Protocol.C_LITO_ROOM_WHO).d(1).c(0).bytes()));
    }

    @Test void hubDefensivelyClampsForceToAReceiverSupportedRoster() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            ClientSession session = new ClientSession(null, ClientSession.Endpoint.GAME,
                    config, null, null, LocalResources.discardCapture(), hub);
            hub.connect(session);
            hub.enterChannel(0, session);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Force Test", "", 4, 0, 0, 0, 0, 0, List.of()));
            hub.joinRoom(roomId, session);

            hub.setForce(roomId, session, 2);
            assertEquals(1, hub.forceOf(roomId, session));
            hub.setForce(roomId, session, -1);
            assertEquals(0, hub.forceOf(roomId, session));
        }
    }
}
