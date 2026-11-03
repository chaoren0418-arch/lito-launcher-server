package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ClientSessionLitoBuddyErrorIntegrationTest {
    @TempDir Path temporary;

    @Test void duplicateAndCapacityOutcomesEmitExactTypedS_F2Results() throws Exception {
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
            setField(session, "selectedCharacter", "BuddyOwner");
            setField(session, "characters", List.of(new CharacterSummary(
                    "BuddyOwner", "", 1, 0, 52, 500, 100,
                    10, 13, 10, 10, 9, 8, 11, 8, 11,
                    1, 0, 0, 0, 0, 0, 0, 0)));

            int seed = 0x3810_0691;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            String owner = session.sourceCharacterIdentity();
            hub.buddyData(owner, -1); // Native profile includes ArenaGuide.
            assertEquals(GameHub.BuddyAddResult.ADDED,
                    hub.addBuddy(owner, "ManualBuddy", "Local", false));

            invokeAddBuddy(session, "manualbuddy[0]");
            assertArrayEquals(
                    Protocol.buddyError(Protocol.LitoBuddyError.ALREADY_REGISTERED),
                    receive(client, receiver));

            int index = 0;
            while (hub.buddyData(owner, -1).buddies().size() < GameHub.MAX_BUDDIES) {
                assertEquals(GameHub.BuddyAddResult.ADDED,
                        hub.addBuddy(owner, "CapacityBuddy" + index++, "Local", false));
            }
            invokeAddBuddy(session, "OverflowBuddy[0]");
            assertArrayEquals(
                    Protocol.buddyError(Protocol.LitoBuddyError.LIST_FULL),
                    receive(client, receiver));
        }
    }

    private static void invokeAddBuddy(ClientSession session, String encodedTarget)
            throws Exception {
        Method method = ClientSession.class.getDeclaredMethod("addBuddy", byte[].class);
        method.setAccessible(true);
        method.invoke(session, new Protocol.Writer().c(Protocol.C_ADD_BUDDY)
                .s(encodedTarget).bytes());
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
}
