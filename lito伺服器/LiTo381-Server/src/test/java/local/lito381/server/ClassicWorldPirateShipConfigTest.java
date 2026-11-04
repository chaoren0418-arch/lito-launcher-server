package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeAll;

final class ClassicWorldPirateShipConfigTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @Test
    void productionUsesNativeGameClockAndIsolatedSelftestPinsOpenBoundary()
            throws Exception {
        assertEquals(-1, ServerConfig.load(Path.of("config/server.properties"))
                .classicWorldPirateShipFixtureSecondOfDay());
        assertEquals(5_400, ServerConfig.load(Path.of(
                        "src/test/resources/server-config/config/server.world-pirate-boarding-selftest.properties"))
                .classicWorldPirateShipFixtureSecondOfDay());
        ServerConfig production = ServerConfig.load(Path.of("config/server.properties"));
        assertEquals(-1, production.classicWorldShipFixtureSecondOfDay());
        assertFalse(production.classicWorldShipFixtureWindowOpening());
        ServerConfig mainland = ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.world-mainland-ship-selftest.properties"));
        assertEquals(21_600, mainland.classicWorldShipFixtureSecondOfDay());
        assertTrue(mainland.classicWorldShipFixtureWindowOpening());
    }

    @Test
    void fixtureRejectsValuesOutsideOneNativeDay(@TempDir Path directory)
            throws Exception {
        Path source = Path.of("src/test/resources/server-config/config/server.world-pirate-boarding-selftest.properties");
        String base = Files.readString(source)
                .replace("classic.world.pirate.ship.fixture.second.of.day=5400\n", "");
        Path negative = directory.resolve("negative.properties");
        Files.writeString(negative, base
                + "\nclassic.world.pirate.ship.fixture.second.of.day=-2\n");
        assertThrows(IllegalArgumentException.class,
                () -> ServerConfig.load(negative));
        Path overflow = directory.resolve("overflow.properties");
        Files.writeString(overflow, base
                + "\nclassic.world.pirate.ship.fixture.second.of.day=86400\n");
        assertThrows(IllegalArgumentException.class,
                () -> ServerConfig.load(overflow));
    }
}
