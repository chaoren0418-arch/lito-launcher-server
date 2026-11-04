package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class NativeHouseBoardPersistenceSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/src/com/lineage/data/npc/other/Npc_Board.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/BoardPersistenceService.java"));
    }

    @Test
    void retainedBoardTableIsTheSharedAuthorityAndUnsafeRowsStayOffWire()
            throws Exception {
        String repository = LocalResources.readString(Path.of("src", "main", "java", "local",
                "lito381", "server", "Accounts.java"), StandardCharsets.UTF_8);
        String retainedPersistence = LocalResources.readString(Path.of("..", "1-伺服器", "src",
                "com", "lineage", "server", "model", "BoardPersistenceService.java"),
                StandardCharsets.UTF_8);
        String retainedNpc = LocalResources.readString(Path.of("..", "1-伺服器", "src", "com",
                "lineage", "data", "npc", "other", "Npc_Board.java"),
                StandardCharsets.UTF_8);

        assertTrue(retainedPersistence.contains(
                "SELECT * FROM `server_盟屋_佈告` ORDER BY `id`"));
        assertTrue(retainedPersistence.contains(
                "INSERT INTO `server_盟屋_佈告` SET `id`=?"));
        assertTrue(retainedPersistence.contains(
                "DELETE FROM `server_盟屋_佈告` WHERE `id`=?"));
        assertTrue(retainedNpc.contains("consumeItem(L1ItemId.ADENA, 300)"));

        assertTrue(repository.contains(
                "FROM `server_盟屋_佈告` WHERE id<?"));
        assertTrue(repository.contains(
                "INSERT INTO `server_盟屋_佈告` "));
        assertTrue(repository.contains(
                "LOCK TABLES characters READ, `server_盟屋_佈告` WRITE"));
        assertTrue(repository.contains(
                "DELETE FROM `server_盟屋_佈告` WHERE id=? AND name=?"));
        assertTrue(repository.contains(
                "skipped unsafe shared native-board row"));
        assertTrue(repository.contains(
                "board post exceeds retained server_盟屋_佈告 columns"));
    }
}
