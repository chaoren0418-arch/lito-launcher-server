package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks the selected 3.81C weapon-range source into Honor objective combat. */
final class NativeHonorWaterCurtainRangeSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
    }

    @Test
    void map512AndSelectedSwordSpearRowsRemainExact() throws Exception {
        assertEquals(List.of(510, 528, 532, 512, 517, 524, 530, 535),
                BattleRules.Mode.HONOR_MATCH.maps());
        assertEquals(512, ArenaMapCatalog.forMap(512).mapId());

        String database = Files.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `weapon` VALUES (24, '0', '短劍', '0', '$100', "
                + "'sword', 'iron', 30000, 106, 26, 0, 6, 8, 1,"));
        assertTrue(database.contains(
                "INSERT INTO `weapon` VALUES (90, '0', '巴迪須', '0', '$62', "
                + "'spear', 'steel', 120000, 11, 5, 0, 10, 12, 2,"));
    }

    @Test
    void roomAttackUsesTheActiveSourceWeaponRangeBeforeDamageResolution()
            throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int weapon = session.indexOf(
                "SourceWeaponPhysicalStats activeWeapon = sourceWeaponActive");
        int exactRange = session.indexOf(
                "BattlePhysicalAttack.supportsDistance(activeWeapon, range)", weapon);
        int projectile = session.indexOf(
                "SourceProjectilePhysicalStats projectile = activeWeapon.projectile()",
                exactRange);
        int reveal = session.indexOf(
                "revealInvisibility(\"physical-attack-\"", projectile);
        assertTrue(weapon >= 0 && exactRange > weapon && projectile > exactRange
                && reveal > projectile);
        assertTrue(session.substring(exactRange, projectile).contains(
                "rejected attack outside source weapon range"));
    }
}
