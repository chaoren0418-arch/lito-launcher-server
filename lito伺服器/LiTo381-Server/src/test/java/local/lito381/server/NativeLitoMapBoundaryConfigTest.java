package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeAll;

class NativeLitoMapBoundaryConfigTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @TempDir
    Path directory;

    @Test
    void everyProductionServiceModeStaysInsideNativeLitoMaps() throws Exception {
        assertFalse(ServerConfig.load(Path.of("config/server.properties"), "standard")
                .classicWorldCompatibilityEnabled());
        assertFalse(ServerConfig.load(Path.of("config/server.properties"), "rental-source")
                .classicWorldCompatibilityEnabled());
        assertFalse(ServerConfig.load(Path.of("config/server.properties"), "rental-battle")
                .classicWorldCompatibilityEnabled());
        assertTrue(ServerConfig.load(Path.of("src/test/resources/server-config/config/server.dev.properties"))
                .classicWorldCompatibilityEnabled());
        assertTrue(ServerConfig.load(Path.of(
                        "src/test/resources/server-config/config/server.world-tree-call-selftest.properties"))
                .classicWorldCompatibilityEnabled());
    }

    @Test
    void databaseModeProvidesSafeDefaults() throws Exception {
        Path legacy = directory.resolve("legacy.properties");
        Files.writeString(legacy, Files.readString(Path.of("config/server.properties"))
                .replace("classic.world.compatibility.enabled=false\n", ""));
        Path memory = directory.resolve("memory.properties");
        Files.writeString(memory, Files.readString(
                Path.of("src/test/resources/server-config/config/server.world-tree-call-selftest.properties"))
                .replace("classic.world.compatibility.enabled=true\n", ""));

        assertFalse(ServerConfig.load(legacy).classicWorldCompatibilityEnabled());
        assertTrue(ServerConfig.load(memory).classicWorldCompatibilityEnabled());
    }

    @Test
    void nativeProductionDoesNotOpenLegacyTextMapRepository() throws Exception {
        try (LitoMapRepository maps = LitoMapRepository.open(
                ServerConfig.load(Path.of("config/server.properties"), "standard"))) {
            assertTrue(maps.description().startsWith("native-litmap-embedded"));
        }
    }
}
