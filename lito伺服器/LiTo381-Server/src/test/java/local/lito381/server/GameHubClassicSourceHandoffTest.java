package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
class GameHubClassicSourceHandoffTest {
    @TempDir Path temporary;

    @Test
    void stagesOverwritesAndConsumesExactSourceOnce() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config,
                new GameStateStore(temporary.resolve("handoff-state.properties"), false))) {
            assertNull(hub.pendingClassicSourceHandoff("test"));

            hub.stageClassicSourceHandoff("Test", "Sun");
            assertEquals("Sun", hub.pendingClassicSourceHandoff("test"));
            assertFalse(hub.consumeClassicSourceHandoff("test", "Moon"));
            assertEquals("Sun", hub.pendingClassicSourceHandoff("TEST"));

            hub.stageClassicSourceHandoff("test", "Moon");
            assertEquals("Moon", hub.pendingClassicSourceHandoff("Test"));
            assertTrue(hub.consumeClassicSourceHandoff("TEST", "moon"));
            assertNull(hub.pendingClassicSourceHandoff("test"));
            assertFalse(hub.consumeClassicSourceHandoff("test", "Moon"));
        }
    }

    @Test
    void freshLoginCanClearAbandonedHandoff() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config,
                new GameStateStore(temporary.resolve("clear-state.properties"), false))) {
            hub.stageClassicSourceHandoff("test", "Sun");
            hub.clearClassicSourceHandoff("TEST");
            assertNull(hub.pendingClassicSourceHandoff("test"));
        }
    }
}
