package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Cross-source guard for every selected Quest Battle mobskill-bearing species. */
final class NativeQuestBattleMobskillSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();

    @Test
    void selectedSqlPinsOgreAreaPhysicalAndAllDeathKnightRows() throws Exception {
        String sql = read(RETAINED.resolve("db/380_db.sql"));
        assertTrue(sql.contains("INSERT INTO `mobskill` VALUES (45278, 0,"
                + " '歐吉>物理攻擊', 1, 30, 0, 0, -2, 0, 0, 2, 1, 2, 20,"));
        assertTrue(sql.contains("INSERT INTO `mobskill` VALUES (45601, 0,"
                + " '死亡騎士>吸血鬼之吻', 2, 20, 0, 0, -3"));
        assertTrue(sql.contains("INSERT INTO `mobskill` VALUES (45601, 1,"
                + " '死亡騎士>デスナイト(アースクエイク)', 2, 60, 0, 0, -6"));
        assertTrue(sql.contains("INSERT INTO `mobskill` VALUES (45601, 2,"
                + " '死亡騎士>召喚', 3, 5, 0, 0, -3, 2"));
        assertTrue(sql.contains("INSERT INTO `mobskill` VALUES (45601, 3,"
                + " '死亡騎士>物理攻擊', 1, 100, 0, 0, -1"));
        assertTrue(sql.contains("INSERT INTO `npc` VALUES (45278, '歐吉',"
                + " '$953', '0', '', 'L1Monster', 1020, 28, 500, 20"));
        assertTrue(sql.contains("INSERT INTO `npc` VALUES (45161, '史巴托',"
                + " '$318', '0', '普通', 'L1Monster', 145, 16, 120, 30"));
    }

    @Test
    void remainingQuestSpeciesHaveNoSelectedMobskillRows() throws Exception {
        String sql = read(RETAINED.resolve("db/380_db.sql"));
        for (int npcId : List.of(45262, 45207, 45289, 45259,
                45322, 45390, 45380)) {
            assertFalse(sql.contains("INSERT INTO `mobskill` VALUES ("
                    + npcId + ","), "unexpected Quest mobskill row: " + npcId);
        }
    }

    @Test
    void retainedCodePinsStrictTriggerSelectionAreaAndSummonSemantics()
            throws Exception {
        String source = read(RETAINED.resolve(
                "src/com/lineage/server/model/L1MobSkillUse.java"));
        assertTrue(source.contains("if (isSkillUseble(i, isTriRnd) == false)"));
        assertTrue(source.contains("final int num = _rnd.nextInt(skillSizeCounter);"));
        assertTrue(source.contains("if (chance < getMobSkillTemplate()"
                + ".getTriggerRandom(skillIdx))"));
        assertTrue(source.contains("World.get().getVisibleBoxObjects"));
        assertTrue(source.contains("count = _rnd.nextInt(max) + min;"));
        assertTrue(source.contains("skillUseSpawn.mobspawn(_attacker, _target,"
                + " summonId, count);"));
        String spawnSource = read(RETAINED.resolve(
                "src/com/lineage/server/model/L1MobSkillUseSpawn.java"));
        assertTrue(spawnSource.contains(
                "attacker.getLocation().randomLocation(6, false)"));
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.isRegularFile(path), "missing source evidence: " + path);
        return Files.readString(path, StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
    }
}
