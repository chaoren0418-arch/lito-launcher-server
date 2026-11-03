package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class NativeSourceUiProbeProfileTest {
    @Test
    void advertisesBothRetainedSourceNamesWithoutOpeningLegacyDatabases()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.native-source-ui-probe.properties"));

        assertEquals(List.of("完美天堂", "完美天堂2"),
                config.classicServerNames());
        assertEquals(List.of(1, 2), config.classicServerIds());
        assertEquals("memory", config.databaseMode());
        assertTrue(config.databaseReadOnly());
        assertFalse(config.nativeCharacterEventProbeEnabled(),
                "S_04 closes the retained-source modal and belongs only to the /ltcharsvr fixture");
        assertTrue(config.nativeSourceSelectionProbeEnabled());
        assertEquals(List.of("login_check", "classic_server_list"),
                config.loginBootstrapSequence());

        try (AccountRepository repository = AccountRepository.open(config)) {
            repository.healthCheck();
            assertTrue(repository instanceof SourceRoutedAccountRepository);
            assertEquals(1, repository.repositoryForClassicSource("完美天堂")
                    .characters("test").size());
            assertEquals(1, repository.repositoryForClassicSource("完美天堂2")
                    .characters("test").size());
        }
    }
}
