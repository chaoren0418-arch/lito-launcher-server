package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class DeathmatchFatalityConfigTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void productionDefaultKeepsRandomSelection() throws Exception {
        ServerConfig config = load("");
        assertEquals(-1, config.deathmatchFatalityFixtureSlot());
    }

    @Test
    void testFixtureCanSelectEveryDocumentedEffectVariant() throws Exception {
        assertEquals(-2, load("deathmatch.fatality.fixture.slot=-2\n")
                .deathmatchFatalityFixtureSlot());
        assertEquals(0, load("deathmatch.fatality.fixture.slot=0\n")
                .deathmatchFatalityFixtureSlot());
        assertEquals(8, load("deathmatch.fatality.fixture.slot=8\n")
                .deathmatchFatalityFixtureSlot());
    }

    @Test
    void fixtureRejectsAnUndocumentedTenthVariant() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> load("deathmatch.fatality.fixture.slot=9\n"));
        assertThrows(IllegalArgumentException.class,
                () -> load("deathmatch.fatality.fixture.slot=-3\n"));
    }

    private ServerConfig load(String properties) throws Exception {
        Path source = temporaryDirectory.resolve("server.properties");
        Files.writeString(source, "database.mode=memory\n" + properties);
        return ServerConfig.load(source);
    }
}
