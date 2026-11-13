package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeAll;

@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class LitoDisplayNameTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @TempDir Path temporary;

    @Test
    void presentationUsesAccountAndSelectedSourceWithoutChangingCharacterIdentity()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null,
                    new GameStateStore(temporary.resolve("state.properties"), false),
                    new CaptureLog(temporary.resolve("capture")), new GameHub(config));
            set(session, "account", "3333");
            set(session, "selectedCharacter", "3331");
            set(session, "selectedClassicServerName", "太陽神");

            assertEquals("3331", session.playerName());
            assertEquals("3333(太陽神)", session.litoDisplayName());
            assertEquals(GameStateStore.sourceCharacter("太陽神", "3331"),
                    session.sourceCharacterIdentity());
            assertTrue(session.litoDisplayName().getBytes(Protocol.LEGACY_TEXT).length <= 27);
        }
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
