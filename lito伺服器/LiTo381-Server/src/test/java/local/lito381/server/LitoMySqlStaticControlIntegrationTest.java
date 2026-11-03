package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/** Read-only proof that the deployed shared static schema backs LiTo controls. */
final class LitoMySqlStaticControlIntegrationTest {
    @Test
    void liveStaticSchemaPublishesShopAndBattleControls() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("lito.mysql.integration"));
        ServerConfig config = ServerConfig.load(
                Path.of("config", "server.properties"), "standard");
        try (AccountRepository accounts = AccountRepository.open(config)) {
            accounts.healthCheck();
            RepositoryBattleConfiguration battle = accounts.battleConfiguration();
            assertEquals(40, battle.rules().size());
            assertEquals(26, battle.objects().size());
            assertEquals(25, battle.spawns().size());
            AccountRepository selected = accounts.repositoryForClassicSource(
                    config.classicServerNames().get(0));
            assertEquals(16, selected.tournamentShopOffers().size());
            assertEquals(40, selected.tournamentShopOffers().get(0).buyPrice());
        }
    }
}
