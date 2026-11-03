package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ClientSessionGmDeIntegrationTest {
    @TempDir Path temporary;

    @Test void databaseAuthorizedDeChatCreatesReadyRoomRowsAndStartsCountdown()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(temporary.resolve("state.properties"), false);
        try (AccountRepository accounts = AccountRepository.open(config);
             GameHub hub = new GameHub(config);
             ServerSocket listener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(server, ClientSession.Endpoint.GAME,
                    config, accounts, state,
                    new CaptureLog(temporary.resolve("capture")), hub);
            prepareRoomOwner(session, config.classicServerNames().get(0));
            hub.connect(session);
            hub.enterChannel(0, session);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "GM integration", "", 4, GameHub.RoomSettings.EQUAL_SIDES,
                    BattleRules.Mode.DEATHMATCH.wireId(), 0, 0, 0, 0, List.of()));
            assertTrue(hub.tryJoinRoom(roomId, session).joined());
            setPhase(session, "ROOM");
            setField(session, "currentRoomId", roomId);

            int seed = 0x3810_0DE0;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            invokeHandle(session, new Protocol.Writer().c(Protocol.C_SAY)
                    .c(0).s(".de 20").bytes());

            assertEquals(4, hub.roomCombatantCount(roomId));
            List<GameHub.RoomVirtualPlayer> virtuals = hub.roomVirtualPlayers(roomId);
            assertEquals(3, virtuals.size());
            assertTrue(virtuals.stream().allMatch(player -> player.name().contains("(")));
            List<byte[]> commandPackets = new ArrayList<>();
            for (int index = 0; index < 5; index++) {
                commandPackets.add(receive(client, receiver));
            }
            assertEquals(Protocol.S_LITO_EXIT_ROOM, commandPackets.get(0)[0] & 0xFF);
            assertEquals(List.of(Protocol.S_LITO_ROOM_USER, Protocol.S_LITO_ROOM_USER,
                            Protocol.S_LITO_ROOM_USER),
                    commandPackets.subList(1, 4).stream()
                            .map(packet -> packet[0] & 0xFF).toList());
            assertEquals(Protocol.S_MESSAGE, commandPackets.get(4)[0] & 0xFF);

            invokeHandle(session, new Protocol.Writer().c(Protocol.C_LITO_READY_GAME)
                    .c(1).bytes());
            assertEquals(Protocol.S_LITO_READY_GAME, receive(client, receiver)[0] & 0xFF);
            byte[] countdown = receive(client, receiver);
            assertEquals(Protocol.S_LITO_COUNTDOWN, countdown[0] & 0xFF);
            assertEquals(3, countdown[1] & 0xFF);
        }
    }

    private static void prepareRoomOwner(ClientSession session, String source)
            throws Exception {
        setField(session, "account", "test");
        setField(session, "selectedClassicServerName", source);
        setField(session, "selectedCharacter", "TestKnight");
        setField(session, "characters", List.of(new CharacterSummary(
                "TestKnight", "", 1, 0, 52, 500, 100,
                10, 13, 10, 10, 9, 8, 11, 8, 11,
                1, 0, 0, 0, 0, 0, 0, 0)));
        setField(session, "currentChannelId", 0);
        setField(session, "hubChannelId", 0);
    }

    private static void invokeHandle(ClientSession session, byte[] packet) throws Exception {
        Method method = ClientSession.class.getDeclaredMethod("handle", byte[].class);
        method.setAccessible(true);
        method.invoke(session, packet);
    }

    private static byte[] receive(Socket client, Protocol.CipherState receiver)
            throws Exception {
        return receiver.decrypt(Protocol.readFrame(client.getInputStream(), 8_192));
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
