package local.lito381.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;

final class RentalTownDirectoryConfigTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @TempDir Path temporary;

    @Test void remainsOffByDefaultAndAcceptsOnlyExactTownMaps() throws Exception {
        assertFalse(ServerConfig.load(Path.of("config/server.properties"))
                .rentalTownDirectoryEnabled());
        assertNull(ServerConfig.load(Path.of("config/server.properties"))
                .rentalBattleHandoffFile());
        assertFalse(ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"))
                .rentalTownDirectoryEnabled());

        String base = Files.readString(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        Path enabled = temporary.resolve("enabled.properties");
        Files.writeString(enabled, base + "\n"
                + "lito.rental.town.directory.enabled=true\n"
                + "lito.rental.town.room.id=1342177281\n"
                + "lito.rental.town.directory.name=Rental Village Probe\n"
                + "lito.rental.town.directory.host=127.0.0.9\n"
                + "lito.rental.town.directory.port=32123\n"
                + "lito.rental.town.map=560\n"
                + "lito.rental.handoff.file=shared/rental-handoff.properties\n");
        ServerConfig config = ServerConfig.load(enabled);
        assertTrue(config.rentalTownDirectoryEnabled());
        assertEquals(0x5000_0001L, config.rentalTownDirectoryRoomId());
        assertEquals("Rental Village Probe", config.rentalTownDirectoryName());
        assertEquals("127.0.0.9", config.rentalTownDirectoryHost());
        assertEquals(32123, config.rentalTownDirectoryPort());
        assertEquals(560, config.rentalTownMapId());
        assertEquals(temporary.resolve("shared/rental-handoff.properties")
                        .toAbsolutePath().normalize(),
                config.rentalBattleHandoffFile());

        Path invalid = temporary.resolve("invalid.properties");
        Files.writeString(invalid, base + "\nlito.rental.town.map=559\n");
        assertThrows(IllegalArgumentException.class,
                () -> ServerConfig.load(invalid));
    }
}
