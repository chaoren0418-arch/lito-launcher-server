package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ClientSessionLitoModeStepIntegrationTest {
    @TempDir Path temporary;

    @Test void encryptedRightNextAndLeftPreviousEmitExactS_D5ModeMapPairs()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("state.properties"), false);
        try (GameHub hub = new GameHub(config);
             ServerSocket listener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("capture")), hub);
            prepareChannelMember(session, "ModeOwner", 0);
            hub.connect(session);
            hub.enterChannel(0, session);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Mode Step", "", 4, GameHub.RoomSettings.ALLOW_UPPER_TIER,
                    BattleRules.Mode.DEATHMATCH.wireId(), 0, 0, 0, 0, List.of()));
            assertTrue(hub.tryJoinRoom(roomId, session).joined());
            setPhase(session, "ROOM");
            setField(session, "currentRoomId", roomId);

            int seed = 0x3810_0692;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            invokeGameInfo(session, new byte[] {(byte) 0xD1, 2, 1});
            assertArrayEquals(Protocol.litoRoomMode(roomId, BattleRules.Mode.HONOR_MATCH),
                    receive(client, receiver));
            assertArrayEquals(Protocol.litoRoomMap(roomId, 510), receive(client, receiver));
            assertEquals(BattleRules.Mode.HONOR_MATCH.wireId(),
                    hub.roomSettings(roomId).option0() & 0x7F);

            invokeGameInfo(session, new byte[] {(byte) 0xD1, 2, 2});
            assertArrayEquals(Protocol.litoRoomMode(roomId, BattleRules.Mode.DEATHMATCH),
                    receive(client, receiver));
            assertArrayEquals(Protocol.litoRoomMap(roomId, 564), receive(client, receiver));
            assertEquals(BattleRules.Mode.DEATHMATCH.wireId(),
                    hub.roomSettings(roomId).option0() & 0x7F);
            assertTrue(hub.roomSettings(roomId).requiresEqualSides());

            invokeGameInfo(session, new byte[] {(byte) 0xD1, 2, 2});
            assertArrayEquals(Protocol.litoRoomMode(roomId, BattleRules.Mode.PET_RACE),
                    receive(client, receiver));
            assertArrayEquals(Protocol.litoRoomMap(roomId, 565), receive(client, receiver));
            assertTrue(!hub.roomSettings(roomId).requiresEqualSides());
            // ArenaBot exit is only emitted for the channel default test room.
            // This user-created room has no structural bot to remove.

            invokeGameInfo(session, new byte[] {(byte) 0xD1, 2, 1});
            assertArrayEquals(Protocol.litoRoomMode(roomId, BattleRules.Mode.DEATHMATCH),
                    receive(client, receiver));
            assertArrayEquals(Protocol.litoRoomMap(roomId, 564), receive(client, receiver));
            assertTrue(hub.roomSettings(roomId).requiresEqualSides());
        }
    }

    private static void invokeGameInfo(ClientSession session, byte[] payload)
            throws Exception {
        Method method = ClientSession.class.getDeclaredMethod("gameInfo", byte[].class);
        method.setAccessible(true);
        method.invoke(session, payload);
    }

    private static byte[] receive(Socket client, Protocol.CipherState receiver)
            throws Exception {
        return receiver.decrypt(Protocol.readFrame(client.getInputStream(), 8_192));
    }

    private static void prepareChannelMember(
            ClientSession session, String name, int channelId) throws Exception {
        setPhase(session, "CHANNEL");
        setField(session, "selectedCharacter", name);
        setField(session, "characters", List.of(new CharacterSummary(
                name, "", 1, 0, 52, 500, 100,
                10, 13, 10, 10, 9, 8, 11, 8, 11,
                1, 0, 0, 0, 0, 0, 0, 0)));
        setField(session, "currentChannelId", channelId);
        setField(session, "hubChannelId", channelId);
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
