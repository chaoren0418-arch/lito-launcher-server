package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

final class SourceInventoryConfigTest {
    @Test
    void sourceInventoryFixtureIsExplicitlyOptIn() throws Exception {
        ServerConfig ordinary = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        assertFalse(ordinary.memorySourceInventoryFixtureEnabled());
        assertFalse(ordinary.memorySourceProjectileFixtureEnabled());
        assertTrue(ServerConfig.load(Path.of(
                        "src/test/resources/server-config/config/server.world-gunter-selftest.properties"))
                .memorySourceInventoryFixtureEnabled());
        ServerConfig projectile = ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.source-projectile-selftest.properties"));
        assertFalse(projectile.memorySourceInventoryFixtureEnabled());
        assertTrue(projectile.memorySourceProjectileFixtureEnabled());
    }
}
