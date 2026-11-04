package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.BeforeAll;

/** Read-only proof for retained skill 103 Dark Blind and its shared blind mode. */
final class LegacyDarkBlindIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/src/com/lineage/server/datatables/user/CharBuffTable.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/L1MagicPc.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillId.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillMode.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/skillmode/CURSE_BLIND.java"));
    }

    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void everyRetainedSourceHasTheExactDarkBlindContract() throws Exception {
        String configPath = System.getProperty(
                "lito.legacy.config", "src/test/resources/server-config/config/server.source-router-legacy-probe.properties");
        ServerConfig config = ServerConfig.load(Path.of(configPath));
        try (AccountRepository routed = AccountRepository.open(config)) {
            routed.healthCheck();
            int verified = 0;
            for (String source : config.classicServerNames()) {
                AccountRepository repository = routed.repositoryForClassicSource(source);
                String database = stringField(repository, "resourceDatabase");
                assertTrue(database.matches("[A-Za-z0-9_]+"));
                int reuse = assertExactSkillRow(repository, database);
                assertExactAuxiliaryRows(repository, database);
                verified++;
                System.out.printf(
                        "[LiTo-test] source=%s dark-blind-skill-row=exact"
                                + " schema=%s reuse=%d negative-control=exact%n",
                        source, database, reuse);
            }
            assertEquals(config.classicServerNames().size(), verified);
        }
        assertRetainedBlindMode();
    }

    private static int assertExactSkillRow(AccountRepository repository,
                                           String database) throws Exception {
        boolean effectSkillIdColumn;
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND TABLE_NAME='skills'"
                             + " AND COLUMN_NAME='effect_skill_id'")) {
            statement.setString(1, database);
            try (ResultSet row = statement.executeQuery()) {
                effectSkillIdColumn = row.next() && row.getInt(1) == 1;
            }
        }
        int expectedReuse = effectSkillIdColumn ? 990 : 1_000;
        String sql = "SELECT reuseDelay"
                + (effectSkillIdColumn ? ",effect_skill_id" : "")
                + " FROM " + database + ".`skills`"
                + " WHERE skill_id=103 AND name='暗黑盲咒'"
                + " AND skill_level=13 AND skill_number=6"
                + " AND mpConsume=20 AND hpConsume=0"
                + " AND itemConsumeId=40321 AND itemConsumeCount=1"
                + " AND reuseDelay=" + expectedReuse + " AND buffDuration=64"
                + " AND target='buff' AND target_to=3"
                + " AND damage_value=0 AND damage_dice=0 AND damage_dice_count=0"
                + " AND probability_value=0 AND probability_dice=0"
                + " AND attr=0 AND type=4 AND lawful=-10"
                + " AND ranged=0 AND area=0 AND through=0"
                + " AND id=64 AND nameid='$2510'"
                + " AND action_id=19 AND castgfx=2947 AND castgfx2=0"
                + " AND sysmsgID_happen=147 AND sysmsgID_stop=148"
                + " AND sysmsgID_fail=280";
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill 103 source row");
            assertEquals(expectedReuse, row.getInt("reuseDelay"));
            if (effectSkillIdColumn) assertEquals(0, row.getInt("effect_skill_id"));
            assertFalse(row.next(), "duplicate exact skill 103 source row");
        }
        return expectedReuse;
    }

    private static void assertExactAuxiliaryRows(
            AccountRepository repository, String database) throws Exception {
        assertCount(repository, "SELECT COUNT(*) FROM " + database
                + ".`skills_item` WHERE skill_id=103 AND name='暗黑盲咒'"
                + " AND itemids IS NULL AND counts IS NULL", 1);
        assertCount(repository, "SELECT COUNT(*) FROM " + database
                + ".`skills_負面boss免疫` WHERE skill_id=103"
                + " AND note='暗黑盲咒' AND modifier=0", 1);
        assertCount(repository, "SELECT COUNT(*) FROM " + database
                + ".`skills_負面技能控制` WHERE skill_id=103"
                + " AND note='暗黑盲咒-暗黑耐性'"
                + " AND prob_high=55 AND prob_equal=45 AND prob_low=35"
                + " AND bonus_str=0 AND bonus_dex=0 AND bonus_int=0.35"
                + " AND check_resist_type=4 AND check_magic_level=0"
                + " AND check_magic_hit=1 AND mr_multiplier=0.05"
                + " AND con_multiplier=0 AND wis_multiplier=0", 1);
    }

    private static void assertCount(AccountRepository repository,
                                    String sql, int expected) throws Exception {
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            assertEquals(expected, row.getInt(1));
        }
    }

    private static void assertRetainedBlindMode() throws Exception {
        Path retained = Path.of("..", "1-伺服器").toAbsolutePath().normalize();
        String ids = read(retained, "src/com/lineage/server/model/skill/L1SkillId.java");
        assertTrue(ids.contains("public static final int DARK_BLIND = 103"));
        String modes = read(retained, "src/com/lineage/server/model/skill/L1SkillMode.java");
        assertTrue(modes.contains("_skillMode.put(DARK_BLIND, new CURSE_BLIND())"));
        String blind = read(retained,
                "src/com/lineage/server/model/skill/skillmode/CURSE_BLIND.java");
        for (String fragment : new String[] {
                "pc.sendPackets(new S_CurseBlind(2))",
                "pc.sendPackets(new S_CurseBlind(1))",
                "cha.setSkillEffect(DARKNESS, integer * 1000)",
                "pc.sendPackets(new S_CurseBlind(0))"}) {
            assertTrue(blind.contains(fragment), "missing retained blind mode " + fragment);
        }
        String magic = read(retained, "src/com/lineage/server/model/L1MagicPc.java");
        assertTrue(magic.contains("case DARK_BLIND:// 暗黑盲咒"));
        assertTrue(magic.contains("probability -= (_targetPc.getRegistBlind() >> 1)"));
        String buffs = read(retained,
                "src/com/lineage/server/datatables/user/CharBuffTable.java");
        assertFalse(buffs.contains("DARK_BLIND"));
    }

    private static String read(Path root, String relative) throws Exception {
        return LocalResources.readString(root.resolve(relative), StandardCharsets.UTF_8);
    }

    private static Connection connection(AccountRepository repository) throws Exception {
        Connection connection = DriverManager.getConnection(
                stringField(repository, "accountUrl"),
                stringField(repository, "accountLogin"),
                stringField(repository, "accountPassword"));
        connection.setReadOnly(true);
        return connection;
    }

    private static String stringField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (String) field.get(target);
    }
}
