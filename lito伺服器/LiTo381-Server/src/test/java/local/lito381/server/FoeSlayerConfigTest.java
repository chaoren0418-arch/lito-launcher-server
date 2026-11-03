package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class FoeSlayerConfigTest {
    @Test
    void deterministicWeaknessAdvanceFixtureIsIsolatedFromNormalProfiles() throws Exception {
        assertFalse(ServerConfig.load(Path.of("config/server.properties"))
                .chainswordWeaknessAdvanceFixtureEnabled());
        assertFalse(ServerConfig.load(Path.of("src/test/resources/server-config/root/server.release-selftest.properties"))
                .chainswordWeaknessAdvanceFixtureEnabled());
        assertTrue(ServerConfig.load(Path.of("src/test/resources/server-config/config/server.foe-slayer-selftest.properties"))
                .chainswordWeaknessAdvanceFixtureEnabled());
    }
}
