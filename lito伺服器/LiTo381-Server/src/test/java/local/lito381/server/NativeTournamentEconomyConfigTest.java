package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class NativeTournamentEconomyConfigTest {
    @Test
    void exactSourceProfilesEnableTheIsolatedPerMatchBoundary() throws IOException {
        for (String profile : new String[] {
                "config/server.properties",
                "src/test/resources/server-config/config/server.perfect1.properties",
                "src/test/resources/server-config/config/server.perfect2.properties"}) {
            ServerConfig config = ServerConfig.load(Path.of(profile));
            assertTrue(config.nativeTournamentEconomyEnabled(), profile);
            assertEquals(20_000, config.nativeTournamentAllowance(), profile);
        }
    }

    @Test
    void compatibilitySelftestRetainsTheLegacyDevelopmentEconomy() throws IOException {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        assertFalse(config.nativeTournamentEconomyEnabled());
        assertEquals(20_000, config.nativeTournamentAllowance());
    }
}
