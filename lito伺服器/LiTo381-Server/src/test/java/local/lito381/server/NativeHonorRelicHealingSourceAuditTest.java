package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks the historical Honor-relic healing rule into the BATTLE dispatcher. */
final class NativeHonorRelicHealingSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"));
    }

    @Test
    void selectedBasicHealAndHonorMapRemainExact() throws Exception {
        assertTrue(BattleRules.Mode.HONOR_MATCH.maps().contains(512));
        assertEquals(
                "LineageCompendium-LiTo-Honor-relic-has-HP-and-can-be-healed",
                BattleGuardianTowers.HONOR_RELIC_HEALING_RULE_SOURCE);
        BattleSpellCatalog.HealingSpell heal = BattleSpellCatalog.healingSpell(1);
        assertEquals(4, heal.mpCost());
        assertEquals(19, heal.actionId());
        assertEquals(11722, heal.effectGfx());
        assertEquals(77, heal.happenMessage());

        String database = LocalResources.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (1, 0, '初級治癒術', 1, 0, 4,"));
        assertTrue(database.contains(
                "'$1436', 19, 11722, 0, 77, 0, 280);"));
    }

    @Test
    void towerAdmissionPrecedesCostAndHealingPublishesNativeStatePackets()
            throws Exception {
        String session = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int admission = session.indexOf("boolean healableTowerTarget");
        int cost = session.indexOf("playerMp -= mpCost", admission);
        int transfer = session.indexOf("hub.healGuardianTower(", cost);
        int ratio = session.indexOf(
                "S_HIT_RATIO honor-relic-healing=", transfer);
        int action = session.indexOf(
                "S_ACTION honor-relic-healing=", ratio);
        assertTrue(admission >= 0 && cost > admission && transfer > cost
                && ratio > transfer && action > ratio);

        String tower = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/BattleGuardianTower.java"),
                StandardCharsets.UTF_8);
        assertTrue(tower.contains(
                "mode == BattleRules.Mode.HONOR_MATCH"));
        assertTrue(tower.contains("tower.force() == healerForce"));
        assertTrue(tower.contains("hp = Math.min(maximumHp"));
    }
}
