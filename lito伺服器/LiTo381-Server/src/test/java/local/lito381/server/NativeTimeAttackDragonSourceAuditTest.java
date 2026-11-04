package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Cross-source guard for the documented Time Attack random-dragon stage. */
final class NativeTimeAttackDragonSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/model/L1MobSkillUse.java"),
                Path.of("../1-伺服器/其他src參考用/古老src/com/lineage/server/model/L1MobSkillUse.java"),
                Path.of("../1-伺服器/其他src參考用/古老src/com/lineage/server/model/skill/L1SkillUse.java"));
    }

    private static final Path SOURCE_DATABASE = Path.of("..", "1-伺服器", "db",
            "380_db.sql").toAbsolutePath().normalize();
    private static final Path RETAINED_MOB_SKILL_USE = Path.of("..", "1-伺服器",
            "src", "com", "lineage", "server", "model", "L1MobSkillUse.java")
            .toAbsolutePath().normalize();
    private static final Path OLD_MOB_SKILL_USE = Path.of("..", "1-伺服器",
            "其他src參考用", "古老src", "com", "lineage", "server", "model",
            "L1MobSkillUse.java").toAbsolutePath().normalize();
    private static final Path OLD_SKILL_USE = Path.of("..", "1-伺服器",
            "其他src參考用", "古老src", "com", "lineage", "server", "model",
            "skill", "L1SkillUse.java").toAbsolutePath().normalize();

    @Test
    void sameFamilyRowsPinAllThreeClassicDragonIdentitiesAndCombatStats()
            throws Exception {
        String sql = read(SOURCE_DATABASE);
        assertTrue(sql.contains("INSERT INTO `npc` VALUES (45682, '安塔瑞斯',"
                + " '$1116', '0', '', 'L1Monster', 1245, 90, 33500, 800, -109, 85"));
        assertTrue(sql.contains("INSERT INTO `npc` VALUES (45683, '法利昂',"
                + " '$1426', '0', '', 'L1Monster', 1590, 93, 35500, 5000, -109, 83"));
        assertTrue(sql.contains("INSERT INTO `npc` VALUES (45684, '巴拉卡斯',"
                + " '$1605', '0', '', 'L1Monster', 2001, 95, 38800, 4500, -109, 83"));

        for (int npcId : List.of(45682, 45683, 45684)) {
            SourceNpcPhysicalStats stats = SourceNpcPhysicalStats.memoryFixture(npcId);
            assertEquals(-109, stats.armorClass());
            assertTrue(stats.large());
            assertTrue(stats.hard());
            assertTrue(stats.boss());
            assertEquals(180, stats.magicResistance());
            assertEquals(2, stats.ranged());
        }
    }

    @Test
    void selectedClientSpriteTablePinsTimedWalkAndAttackForEveryCandidate()
            throws Exception {
        String actions = read(Path.of(
                "src/main/resources/battle/spr-action-380.csv"));
        for (int gfx : List.of(1245, 1590, 2001)) {
            assertTrue(actions.contains(gfx + ",0,"), "missing walk for gfx " + gfx);
            assertTrue(actions.contains(gfx + ",1,"), "missing attack for gfx " + gfx);
        }
    }

    @Test
    void selectedDatabasePinsDragonMobskillAndDamageSkillRows() throws Exception {
        String sql = read(SOURCE_DATABASE);
        for (String row : List.of(
                "(45682, 0, '安塔瑞斯>安塔瑞斯(修改后-巨岩炮)', 2, 60",
                "(45682, 1, '安塔瑞斯>安塔瑞斯(安息)', 2, 10",
                "(45682, 2, '安塔瑞斯>安塔瑞斯-地裂术', 2, 20",
                "(45682, 3, '安塔瑞斯>安塔瑞斯-大地怒吼', 2, 60",
                "(45682, 4, '安塔瑞斯>安塔瑞斯-毒雾', 2, 45",
                "(45682, 5, '安塔瑞斯>物理攻擊', 1, 50",
                "(45682, 6, '安塔瑞斯>物理攻擊', 1, 80",
                "(45682, 7, '安塔瑞斯>安塔瑞斯(修改后-巨岩炮)', 2, 100",
                "(45683, 0, '法利昂>法利昂(修改后-巨风炮)', 2, 60",
                "(45683, 1, '法利昂>法利昂(安息)', 2, 10",
                "(45683, 2, '法利昂>法利昂-水压', 2, 60",
                "(45683, 3, '法利昂>法利昂-寒冰喷吐', 2, 45",
                "(45683, 4, '法利昂>物理攻擊', 1, 50",
                "(45684, 0, '巴拉卡斯>安塔瑞斯(修改后-巨岩炮)', 2, 60",
                "(45684, 1, '巴拉卡斯>巴拉卡斯(安息)', 2, 10",
                "(45684, 2, '巴拉卡斯>巴拉卡斯-火牢', 2, 20",
                "(45684, 3, '巴拉卡斯>巴拉卡斯-流星雨', 2, 60",
                "(45684, 4, '巴拉卡斯>巴拉卡斯-火焰喷吐', 2, 45",
                "(45684, 5, '巴拉卡斯>物理攻擊', 1, 50",
                "(45684, 6, '巴拉卡斯>物理攻擊', 1, 80",
                "(45684, 7, '巴拉卡斯>技能31000', 2, 100")) {
            assertTrue(sql.contains("INSERT INTO `mobskill` VALUES " + row),
                    "missing selected mobskill row " + row);
        }

        for (int skillId : List.of(10027, 10028, 10029, 30002, 30003,
                30004, 31001, 31002, 31003, 31004, 33001, 33002, 33003)) {
            assertTrue(sql.contains("INSERT INTO `skills` VALUES (" + skillId + ","),
                    "missing selected skills row " + skillId);
        }
        assertTrue(!sql.contains("INSERT INTO `skills` VALUES (31000,"),
                "skill31000 must remain an explicit selected-source orphan");
    }

    @Test
    void retainedTypeTwoDispatchProvesMissingSkillTemplateIsInert()
            throws Exception {
        String activeMob = read(RETAINED_MOB_SKILL_USE);
        String oldMob = read(OLD_MOB_SKILL_USE);
        String oldSkill = read(OLD_SKILL_USE);
        String dispatch = "final int skillid = getMobSkillTemplate().getSkillId(idx);";
        String check = "canUseSkill = skillUse.checkUseSkill(";
        assertTrue(activeMob.contains(dispatch));
        assertTrue(activeMob.contains(check));
        assertTrue(oldMob.contains(dispatch));
        assertTrue(oldMob.contains(check));
        assertTrue(oldSkill.contains("_skill = SkillsTable.get().getTemplate(skillid);"));
        assertTrue(oldSkill.contains("if (_skill == null) {\n\t\t\treturn false;\n\t\t}"));
    }

    @Test
    void implementationKeepsHistoricalFactSeparateFromProvisionalWeights() {
        assertEquals("GameMeca-LWC-2005-random-boss-then-random-dragon",
                BattleTimeAttackEncounter.DRAGON_STAGE_SOURCE);
        assertEquals("provisional-equal-none+antharas+fafurion+valakas",
                BattleTimeAttackEncounter.DRAGON_SELECTION_SOURCE);
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.isRegularFile(LocalResources.assumeExternalFile(path)), "missing source evidence: " + path);
        return LocalResources.readString(path, StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
}
