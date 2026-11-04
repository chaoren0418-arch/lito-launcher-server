package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class DedicatedServerLayoutTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @Test void operationalConfigContainsOnlyServerAndDatabaseProperties()
            throws Exception {
        try (var files = Files.list(Path.of("config"))) {
            Set<String> properties = files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".properties"))
                    .collect(Collectors.toSet());
            assertEquals(Set.of("server.properties", "database.properties",
                    "source-servers.properties"), properties);
        }
    }

    @Test void serverConfigIsDedicatedAndDoesNotReachIntoReferenceProject()
            throws Exception {
        String config = Files.readString(
                Path.of("config", "server.properties"), StandardCharsets.UTF_8);
        assertTrue(config.contains("database.connection.properties=database.properties"));
        assertFalse(config.contains("1-伺服器"));
        assertFalse(config.contains("server.lito-shared"));
        assertFalse(config.contains("maps.directory"));
        assertTrue(config.contains("classic.world.compatibility.enabled=false"));
        assertTrue(config.contains("classic.world.monster.ai.enabled=false"));
    }

    @Test void deploymentContainsNoLegacyTextMapsOrHistoricalRootReports()
            throws Exception {
        Path maps = Path.of("maps");
        assertTrue(Files.notExists(maps) || directoryIsEmpty(maps));
        try (var files = Files.list(Path.of("."))) {
            Set<String> reports = files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".txt") || name.endsWith(".json"))
                    .collect(Collectors.toSet());
            assertEquals(Set.of("SERVER-PROTOCOL-COVERAGE.json"), reports);
        }
    }

    @Test void launcherProfileUsesTheSameConfigForEveryServiceMode()
            throws Exception {
        String profile = Files.readString(Path.of("..", "..", "lito登入器", "LiTo381-Launcher",
                "server-profiles", "lito-shared.json"), StandardCharsets.UTF_8);
        assertEquals(3, occurrences(profile, "\"server.properties\""));
        assertFalse(profile.contains("server.lito-shared.properties"));
    }

    private static int occurrences(String source, String needle) {
        int count = 0;
        for (int at = 0; (at = source.indexOf(needle, at)) >= 0;
             at += needle.length()) count++;
        return count;
    }

    private static boolean directoryIsEmpty(Path directory) throws Exception {
        try (var entries = Files.list(directory)) {
            return entries.findAny().isEmpty();
        }
    }
}
