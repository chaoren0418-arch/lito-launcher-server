package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * T111 (#352): the startup loader against the disposable CI MySQL fixture (T119, #334),
 * whose {@code 380_db} is {@code db/lito_static.sql}. Read-only; runs only when
 * {@code config/server.properties} is the CI fixture account.
 */
final class ConsumableItemParametersMySqlIntegrationTest {
    @BeforeAll
    static void requireCiFixture() throws Exception {
        LocalResources.assumeServerProperties();
        Properties config = new Properties();
        try (Reader in = Files.newBufferedReader(Path.of("config", "server.properties"),
                StandardCharsets.UTF_8)) {
            config.load(in);
        }
        String ciUser = System.getenv("LITO_CI_DB_USER");
        Assumptions.assumeTrue(ciUser != null && !ciUser.isBlank()
                        && ciUser.equals(config.getProperty("database.user")),
                "skipped: config/server.properties is not the CI MySQL fixture");
    }

    @AfterEach
    void restoreBuiltIn() {
        ConsumableItemParameters.resetForTesting();
    }

    @Test
    void ciStaticDatabaseMatchesServer1AndBuiltIn() throws Exception {
        ConsumableItemParameters.resetForTesting();
        ConsumableItemParameters.load(ServerConfig.load(Path.of("config", "server.properties")));
        var snapshot = ConsumableItemParameters.snapshot();
        assertEquals("380_db.etcitem", snapshot.source());
        assertEquals(List.of(), snapshot.fallbacks());
        assertEquals(ConsumableItemParameters.BUILT_IN, snapshot.values());
    }
}
