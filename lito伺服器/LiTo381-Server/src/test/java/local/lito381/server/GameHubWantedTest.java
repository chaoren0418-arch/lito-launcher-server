package local.lito381.server;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GameHubWantedTest {
    @Test
    void tracksCaseInsensitiveAccountWatchAndLastAuthenticatedSource() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            assertEquals(GameHub.WantedWatchMutation.ADDED,
                    hub.addWantedAccount("Test7"));
            assertEquals(GameHub.WantedWatchMutation.EXISTS,
                    hub.addWantedAccount("test7"));
            assertEquals(List.of(new GameHub.WantedWatchEntry("Test7", "")),
                    hub.wantedAccountSnapshot());

            assertFalse(hub.recordWantedLogin("Other", "127.0.0.1"));
            assertTrue(hub.recordWantedLogin("TEST7", "127.0.0.1"));
            assertEquals(List.of(new GameHub.WantedWatchEntry("TEST7", "127.0.0.1")),
                    hub.wantedAccountSnapshot());

            assertEquals(GameHub.WantedWatchMutation.REMOVED,
                    hub.removeWantedAccount("test7"));
            assertEquals(GameHub.WantedWatchMutation.NOT_FOUND,
                    hub.removeWantedAccount("Test7"));
            assertFalse(hub.recordWantedLogin("Test7", "127.0.0.1"));
        }
    }

    @Test
    void rejectsEmptyWatchKeysAndInvalidLoginSources() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            assertThrows(IllegalArgumentException.class,
                    () -> hub.addWantedAccount(""));
            assertThrows(IllegalArgumentException.class,
                    () -> hub.removeWantedAccount(null));
            assertThrows(IllegalArgumentException.class,
                    () -> hub.recordWantedLogin("Test7", ""));
        }
    }
}
