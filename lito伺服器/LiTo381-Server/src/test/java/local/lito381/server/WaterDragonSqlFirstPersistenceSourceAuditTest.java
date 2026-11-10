package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class WaterDragonSqlFirstPersistenceSourceAuditTest {
    @Test
    void rentalPublishesOnlyAfterRouteQualifiedSqlCommit() throws Exception {
        String session = Files.readString(Path.of("src", "main", "java", "local",
                "lito381", "server", "ClientSession.java"), StandardCharsets.UTF_8);
        String repository = Files.readString(Path.of("src", "main", "java", "local",
                "lito381", "server", "Accounts.java"), StandardCharsets.UTF_8)
                // #138 B-land: LegacyMySqlRepository now lives in its own file.
                + Files.readString(Path.of(
                        "src/main/java/local/lito381/server/LegacyMySqlRepository.java"),
                        StandardCharsets.UTF_8);
        int reserve = session.indexOf("hub.rentClassicWorldWaterDragonPass(");
        int sql = session.indexOf("persistSqlWorldInventory(next,", reserve);
        int publish = session.indexOf("inventory = next;", sql);
        assertTrue(reserve >= 0 && sql > reserve && publish > sql);
        assertTrue(session.contains("new RepositoryWaterDragonPassChange(durablePass)"));
        assertTrue(session.contains("selectedClassicServerName, passObjects"));
        assertTrue(repository.contains("lito_water_dragon_pass WRITE"));
        assertTrue(repository.contains("persistLitoWaterDragonPassInventory"));
        assertTrue(repository.contains("privateById.get(pass.itemObjectId())"));
    }
}
