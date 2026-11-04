package local.lito381.server;

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

/** Regression for a compact C_CC created by a character with an ordinary clan. */
final class ClientSessionLitoCreateRoomClanIntegrationTest {
    @TempDir Path temporary;

    @Test void compactCreateAutomaticallyJoinsAClannedCreatorAndEmitsSuccess()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("state.properties"), false);
        try (GameHub hub = new GameHub(config);
             MemoryAccountRepository accounts =
                     new MemoryAccountRepository("test", "test");
             ServerSocket listener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, accounts, state,
                    new CaptureLog(temporary.resolve("capture")), hub);
            CharacterSummary character = new CharacterSummary(
                    "3332", "3336", 1, 0, 60, 544, 179,
                    2, 16, 10, 10, 18, 10, 11, 10, 11,
                    2, 0, 0, 0, 0, 0, 0, 0);
            setField(session, "account", "3333");
            setField(session, "selectedClassicServerName", "太陽神");
            setField(session, "selectedCharacter", character.name());
            setField(session, "characters", List.of(character));
            setField(session, "currentChannelId", 0);
            setField(session, "hubChannelId", 0);
            setPhase(session, "CHANNEL");
            hub.connect(session);
            hub.enterChannel(0, session);

            int seed = 0x3810_0906;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            byte[] request = new Protocol.Writer()
                    .c(Protocol.C_LITO_CREATE_ROOM)
                    .s("1234").c(GameHub.RoomSettings.ALLOW_OBSERVERS)
                    .s("").h(10).bytes();
            invokeCreateRoom(session, request);

            byte[] response = receiver.decrypt(
                    Protocol.readFrame(client.getInputStream(), 8_192));
            Protocol.Reader in = new Protocol.Reader(response);
            assertEquals(Protocol.S_LITO_JOIN_RESULT, in.c());
            assertEquals(0, in.c(), "native S_CE result must be success");
            long roomId = in.d();
            assertTrue(roomId > 0);
            assertEquals("ROOM", field(session, "phase").toString());
            assertEquals(roomId, field(session, "currentRoomId"));
            assertEquals(List.of(session), hub.roomMembers(roomId));
            assertEquals("3333(太陽神)", hub.roomOwnerDisplayName(roomId));

            // The physical room-return control emits opcode-only C_D0.  Once
            // create/join keeps the server in ROOM, that same request must no
            // longer be ignored as the CHANNEL desynchronization seen live.
            invokeHandle(session, new byte[] {(byte) Protocol.C_LITO_EXIT_ROOM});
            assertEquals("CHANNEL", field(session, "phase").toString());
            assertEquals(0L, field(session, "currentRoomId"));
            assertTrue(hub.roomMembers(roomId).isEmpty());

            // The native in-battle Exit choice emits opcode-only C_D9.  Its
            // S_F0 transition must release this player and return the server
            // to CHANNEL instead of leaving a ghost room membership behind.
            long battleRoomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Exit regression", "", 10,
                    GameHub.RoomSettings.ALLOW_OBSERVERS,
                    0, 0, 0, 0, 0, List.of()));
            assertTrue(hub.tryJoinRoom(battleRoomId, session).joined());
            setField(session, "currentRoomId", battleRoomId);
            setPhase(session, "BATTLE");
            invokeHandle(session, new byte[] {(byte) Protocol.C_LITO_QUIT_FIGHT});
            assertEquals("RESULT", field(session, "phase").toString());
            assertEquals(battleRoomId, field(session, "currentRoomId"));
            assertEquals(List.of(session), hub.roomMembers(battleRoomId));
        }
    }

    private static void invokeCreateRoom(ClientSession session, byte[] payload)
            throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(
                "createRoom", byte[].class);
        method.setAccessible(true);
        method.invoke(session, payload);
    }

    private static void invokeHandle(ClientSession session, byte[] payload)
            throws Exception {
        Method method = ClientSession.class.getDeclaredMethod("handle", byte[].class);
        method.setAccessible(true);
        method.invoke(session, payload);
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setField(Object target, String name, Object value)
            throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setPhase(ClientSession session, String value)
            throws Exception {
        Class phaseType = Class.forName(ClientSession.class.getName() + "$Phase");
        setField(session, "phase", Enum.valueOf(phaseType, value));
    }
}
