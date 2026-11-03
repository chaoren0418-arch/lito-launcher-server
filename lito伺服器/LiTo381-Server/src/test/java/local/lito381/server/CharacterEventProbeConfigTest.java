package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class CharacterEventProbeConfigTest {
    @TempDir Path temporary;

    @Test void remainsDisabledOutsideTheDedicatedLoopbackFixture() throws Exception {
        assertFalse(ServerConfig.load(Path.of("config/server.properties"))
                .nativeCharacterEventProbeEnabled());
        assertFalse(ServerConfig.load(Path.of("src/test/resources/server-config/config/server.perfect2.properties"))
                .nativeCharacterEventProbeEnabled());
        assertFalse(ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"))
                .nativeCharacterEventProbeEnabled());

        ServerConfig fixture = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.character-event-probe.properties"));
        assertTrue(fixture.nativeCharacterEventProbeEnabled());
        assertTrue(InetAddress.getByName(fixture.bind()).isLoopbackAddress());
        assertTrue(InetAddress.getByName(fixture.gameBind()).isLoopbackAddress());
        assertTrue(InetAddress.getByName(fixture.advertisedHost()).isLoopbackAddress());
        assertEquals(32100, fixture.port());
        assertEquals("memory", fixture.databaseMode());
        assertTrue(fixture.databaseReadOnly());
        assertFalse(fixture.statePersistenceEnabled());
        assertTrue(fixture.loginBootstrapSequence().contains("classic_server_list"));

        ServerConfig sourceUiFixture = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.native-source-ui-probe.properties"));
        assertFalse(sourceUiFixture.nativeCharacterEventProbeEnabled(),
                "the source selector must not receive the modal-closing S_04 event");
        assertTrue(sourceUiFixture.nativeSourceSelectionProbeEnabled());
        assertTrue(InetAddress.getByName(sourceUiFixture.bind()).isLoopbackAddress());
        assertTrue(InetAddress.getByName(sourceUiFixture.gameBind()).isLoopbackAddress());
        assertTrue(InetAddress.getByName(
                sourceUiFixture.advertisedHost()).isLoopbackAddress());
        assertEquals(32100, sourceUiFixture.port());
        assertEquals("memory", sourceUiFixture.databaseMode());
        assertTrue(sourceUiFixture.databaseReadOnly());
        assertFalse(sourceUiFixture.statePersistenceEnabled());
        assertTrue(sourceUiFixture.loginBootstrapSequence()
                .contains("classic_server_list"));
    }

    @Test void rejectsEveryUnsafeProbeConfiguration() throws Exception {
        String source = Files.readString(
                Path.of("src/test/resources/server-config/config/server.character-event-probe.properties"),
                StandardCharsets.UTF_8);
        assertRejected(source.replace("port=32100", "port=32000"), "wrong-port");
        assertRejected(source.replace("database.mode=memory", "database.mode=legacy_mysql"),
                "legacy-database");
        assertRejected(source.replace("database.read.only=true", "database.read.only=false"),
                "writable-database");
        assertRejected(source.replace("state.persistence.enabled=false",
                "state.persistence.enabled=true"), "persistent-state");
        assertRejected(source.replace("advertised.host=127.0.0.2",
                "advertised.host=192.0.2.1"), "external-advertisement");
        assertRejected(source.replace("login_check,classic_server_list", "login_check"),
                "missing-source-directory");
    }

    @Test void noProductionConfigurationEnablesTheProbe() throws Exception {
        Set<String> characterEventFixtures = Set.of(
                "server.character-event-probe.properties");
        Set<String> retainedSourceFixtures = Set.of(
                "server.native-source-ui-probe.properties",
                "server.clone-appearance-physical-probe.properties",
                "server.numeric-schema2-source-e2e.properties");
        try (var configs = Files.list(Path.of("config"))) {
            for (Path path : configs.filter(Files::isRegularFile)
                    .filter(candidate -> candidate.getFileName().toString()
                            .endsWith(".properties"))
                    .toList()) {
                String source = Files.readString(path, StandardCharsets.UTF_8);
                String name = path.getFileName().toString();
                if (!characterEventFixtures.contains(name)) {
                    assertFalse(source.contains(
                                    "native.character.event.probe.enabled=true"),
                            "non-fixture config enables S_04: " + path);
                }
                if (!retainedSourceFixtures.contains(name)) {
                    assertFalse(source.contains(
                                    "native.source.selection.probe.enabled=true"),
                            "non-source-UI config enables retained-source refresh: " + path);
                }
            }
        }
    }

    private void assertRejected(String source, String name) throws Exception {
        Path path = temporary.resolve(name + ".properties");
        Files.writeString(path, source, StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> ServerConfig.load(path));
    }
}
