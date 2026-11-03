package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Locks the historical Honor five-second same-position respawn into BATTLE. */
final class NativeHonorRespawnSourceAuditTest {
    @Test
    void exactRuleIsLimitedToHonorMatch() {
        assertEquals(5_000L, GameHub.HONOR_RESPAWN_DELAY_MS);
        assertEquals(
                "https://lineagecompendium.com/www.lineagecompendium.com/lito.html",
                GameHub.HONOR_RESPAWN_RULE_SOURCE);
        assertTrue(BattleRules.Mode.HONOR_MATCH.respawn());
    }

    @Test
    void delayedPathPreservesDeathCoordinatesAndOmitsSpawnMovement()
            throws Exception {
        String hub = Files.readString(Path.of(
                "src/main/java/local/lito381/server/GameHub.java"),
                StandardCharsets.UTF_8);
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int honorBranch = session.indexOf(
                "== BattleRules.Mode.HONOR_MATCH");
        int schedule = session.indexOf("hub.scheduleHonorRespawn(", honorBranch);
        int ordinary = session.indexOf("respawnAtBattleSpawn();", schedule);
        int samePosition = session.indexOf("positionX = deathX;", ordinary);
        int resurrect = session.indexOf("S_RESURRECT honor target=", samePosition);
        assertTrue(honorBranch >= 0 && schedule > honorBranch && ordinary > schedule
                && samePosition > ordinary && resurrect > samePosition);
        String delayedMethod = session.substring(
                session.indexOf("void serverHonorRespawnDue("),
                session.indexOf("private void respawnAtBattleSpawn()"));
        assertTrue(!delayedMethod.contains("S_MOVE_OBJECT"));
        assertTrue(hub.contains(
                "HONOR_RESPAWN_DELAY_MS, TimeUnit.MILLISECONDS"));
        assertTrue(hub.contains(
                "room.match.rules().mode() != BattleRules.Mode.HONOR_MATCH"));
    }
}
