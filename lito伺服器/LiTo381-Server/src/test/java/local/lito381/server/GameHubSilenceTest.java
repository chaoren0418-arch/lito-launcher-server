package local.lito381.server;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class GameHubSilenceTest {
    @Test
    void keepsNamedSilenceAcrossSessionIdentityAndExpiresAtServerTime() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            long now = 1_000L;
            long expiresAt = hub.setChatSilence("TestMage", 60, now);

            assertEquals(now + 60_000_000_000L, expiresAt);
            assertTrue(hub.chatSilenced("testmage", expiresAt - 1));
            assertFalse(hub.chatSilenced("TESTMAGE", expiresAt));
            assertFalse(hub.chatSilenced("TestMage", expiresAt + 1));
        }
    }

    @Test
    void zeroClearsAndOutOfContractStateIsRejectedBeforeMutation() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            hub.setChatSilence("TestMage", 0x7D00, 10L);
            assertTrue(hub.chatSilenced("TestMage", 11L));
            assertEquals(0, hub.setChatSilence("testmage", 0, 12L));
            assertFalse(hub.chatSilenced("TestMage", 13L));

            assertThrows(IllegalArgumentException.class,
                    () -> hub.setChatSilence("", 60, 0));
            assertThrows(IllegalArgumentException.class,
                    () -> hub.setChatSilence("TestMage", 0x7D01, 0));
        }
    }

    @Test
    void sameDisplayNameSilenceIsIsolatedByOriginalSource() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            String sun = GameStateStore.sourceCharacter("Sun", "SameHero");
            String moon = GameStateStore.sourceCharacter("Moon", "SameHero");
            hub.setChatSilence(sun, 60, 1_000L);

            assertTrue(hub.chatSilenced(sun, 1_001L));
            assertFalse(hub.chatSilenced(moon, 1_001L));
        }
    }
}
