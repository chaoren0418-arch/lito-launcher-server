package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Cross-source guard for the documented Tokyo Time Attack final bosses. */
final class NativeTimeAttackTokyoBossMobskillSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/model/L1MobSkillUse.java"));
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();

    @Test
    void selectedSqlPinsBothBossesAllSixCombatRowsAndSpartoi() throws Exception {
        String sql = read(RETAINED.resolve("db/380_db.sql"));
        assertTrue(sql.contains("INSERT INTO `mobskill` VALUES (45601, 0,"
                + " '死亡騎士>吸血鬼之吻', 2, 20, 0, 0, -3"));
        assertTrue(sql.contains("INSERT INTO `mobskill` VALUES (45601, 1,"
                + " '死亡騎士>デスナイト(アースクエイク)', 2, 60, 0, 0, -6"));
        assertTrue(sql.contains("INSERT INTO `mobskill` VALUES (45601, 2,"
                + " '死亡騎士>召喚', 3, 5, 0, 0, -3, 2"));
        assertTrue(sql.contains("INSERT INTO `mobskill` VALUES (45601, 3,"
                + " '死亡騎士>物理攻擊', 1, 100, 0, 0, -1"));
        assertTrue(sql.contains("INSERT INTO `mobskill` VALUES (45650, 0,"
                + " '殭屍王>ゾンビロード(広範囲魔法)', 2, 30, 0, 0, -6"));
        assertTrue(sql.contains("INSERT INTO `mobskill` VALUES (45650, 1,"
                + " '殭屍王>物理攻擊', 1, 40, 0, 0, -2"));
        assertTrue(sql.contains("INSERT INTO `skills` VALUES (28, 0,"
                + " '吸血鬼之吻', 4, 3, 12"));
        assertTrue(sql.contains("INSERT INTO `skills` VALUES (10000, 0,"
                + " 'ゾンビロード(広範囲魔法)', 0, 0, 5"));
        assertTrue(sql.contains("INSERT INTO `skills` VALUES (10056, 0,"
                + " 'デスナイト(アースクエイク)', 0, 0, 5"));
        assertTrue(sql.contains("INSERT INTO `npc` VALUES (45601, '死亡騎士',"
                + " '$371', '0', '', 'L1Monster', 240, 55, 7500, 250"));
        assertTrue(sql.contains("INSERT INTO `npc` VALUES (45650, '殭屍王',"
                + " '$2398', '0', '', 'L1Monster', 2803, 62, 9500, 100"));
        assertTrue(sql.contains("INSERT INTO `npc` VALUES (45161, '史巴托',"
                + " '$318', '0', '普通', 'L1Monster', 145, 16, 120, 30"));
    }

    @Test
    void retainedMobskillCodePinsIndependentEligibilitySelectionAndSummonOrder()
            throws Exception {
        String source = read(RETAINED.resolve(
                "src/com/lineage/server/model/L1MobSkillUse.java"));
        assertTrue(source.contains("if (isSkillUseble(i, isTriRnd) == false)"));
        assertTrue(source.contains("final int num = _rnd.nextInt(skillSizeCounter);"));
        assertTrue(source.contains("if (chance < getMobSkillTemplate()"
                + ".getTriggerRandom(skillIdx))"));
        assertTrue(source.contains("getSkillUseCount(skillIdx) < getMobSkillTemplate()"
                + ".getTriggerCount(skillIdx)"));
        assertTrue(source.contains("count = _rnd.nextInt(max) + min;"));
        assertTrue(source.contains("skillUseSpawn.mobspawn(_attacker, _target,"
                + " summonId, count);"));
        assertTrue(source.contains("new S_SkillSound(_attacker.getId(), 761)"));
        assertTrue(source.contains("ActionCodes.ACTION_SkillBuff"));
        assertTrue(source.contains("_sleepTime = _attacker.getNpcTemplate()"
                + ".getSubMagicSpeed();"));
    }

    @Test
    void sameSourceAnimationRowsExposeZombieActionConflictAndSafeAction()
            throws Exception {
        String actions = read(Path.of(
                "src/main/resources/battle/spr-action-380.csv"));
        assertTrue(actions.contains("2803,0,36,24\n"));
        assertTrue(actions.contains("2803,1,47,24\n"));
        assertTrue(actions.contains("2803,19,44,24\n"));
        assertTrue(!actions.contains("2803,18,"));
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.isRegularFile(LocalResources.assumeExternalFile(path)), "missing source evidence: " + path);
        return LocalResources.readString(path, StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
}
