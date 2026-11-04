package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Read-only proof for selected/secondary skill-112 database contracts. */
final class LegacyArmorBreakIntegrationTest {
    @BeforeAll
    static void requireMySqlIntegration() {
        Assumptions.assumeTrue(Boolean.getBoolean("lito.mysql.integration"));
    }

    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void everyRetainedSourceHasTheExactArmorBreakContract() throws Exception {
        String configPath = System.getProperty(
                "lito.legacy.config",
                "src/test/resources/server-config/config/server.source-router-legacy-probe.properties");
        ServerConfig config = ServerConfig.load(Path.of(configPath));
        int selectedReuseSources = 0;
        int counterMagicBypassSources = 0;
        try (AccountRepository routed = AccountRepository.open(config)) {
            routed.healthCheck();
            int verified = 0;
            for (String source : config.classicServerNames()) {
                AccountRepository repository = routed.repositoryForClassicSource(source);
                String database = stringField(repository, "resourceDatabase");
                assertTrue(database.matches("[A-Za-z0-9_]+"));
                int reuse = exactSkillRow(repository, database);
                if (reuse == 2_000) selectedReuseSources++;
                assertExactProbabilityRow(repository, database);
                assertCount(repository, "SELECT COUNT(*) FROM " + database
                        + ".`skills_負面boss免疫` WHERE skill_id=112"
                        + " AND note='破壞盔甲' AND modifier=0", 1);
                assertCount(repository, "SELECT COUNT(*) FROM " + database
                        + ".`skills_item` WHERE skill_id=112 AND name='none'"
                        + " AND itemids IS NULL AND counts IS NULL", 1);

                Map<String, Integer> auxiliary = skillAuxiliaryRows(
                        repository, database, BattleSpellCatalog.ARMOR_BREAK);
                Set<String> allowed = Set.of(
                        "skills_item", "skills_負面技能控制",
                        "skills_負面boss免疫", "skills_魔屏無效技能");
                auxiliary.forEach((table, count) -> {
                    if (count != 0) {
                        assertTrue(allowed.contains(table),
                                "unexpected skill112 auxiliary row in " + table);
                        assertEquals(1, count, "duplicate skill112 row in " + table);
                    }
                });
                if (auxiliary.getOrDefault("skills_魔屏無效技能", 0) == 1) {
                    counterMagicBypassSources++;
                }
                verified++;
                System.out.printf("[LiTo-test] source=%s armor-break-row=exact"
                                + " schema=%s reuse=%d probability=50/40/30"
                                + " boss=immune counter-bypass=%s auxiliary=%s%n",
                        source, database, reuse,
                        auxiliary.getOrDefault("skills_魔屏無效技能", 0) == 1,
                        auxiliary);
            }
            assertEquals(config.classicServerNames().size(), verified);
        }
        assertTrue(selectedReuseSources >= 1,
                "selected 2000ms Armor Break source is absent");
        assertTrue(counterMagicBypassSources >= 1,
                "selected Counter Magic bypass row is absent");
    }

    private static int exactSkillRow(AccountRepository repository,
                                     String database) throws Exception {
        boolean effectSkillIdColumn = hasColumn(
                repository, database, "skills", "effect_skill_id");
        String sql = "SELECT reuseDelay"
                + (effectSkillIdColumn ? ",effect_skill_id" : "")
                + " FROM " + database + ".`skills`"
                + " WHERE skill_id=112 AND name='破壞盔甲'"
                + " AND skill_level=14 AND skill_number=7"
                + " AND mpConsume=40 AND hpConsume=30"
                + " AND itemConsumeId=40321 AND itemConsumeCount=2"
                + " AND reuseDelay IN (2000,3000) AND buffDuration=8"
                + " AND target='buff' AND target_to=1"
                + " AND damage_value=0 AND damage_dice=0"
                + " AND damage_dice_count=0 AND probability_value=50"
                + " AND probability_dice=50 AND attr=0 AND type=4"
                + " AND lawful=0 AND ranged=1 AND area=0 AND through=0"
                + " AND id=128 AND nameid='$13934'"
                + " AND action_id=19 AND castgfx=3400 AND castgfx2=0"
                + " AND sysmsgID_happen=0 AND sysmsgID_stop=0"
                + " AND sysmsgID_fail=280";
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill112 Armor Break row");
            int reuse = row.getInt("reuseDelay");
            if (effectSkillIdColumn) assertEquals(0, row.getInt("effect_skill_id"));
            assertFalse(row.next(), "duplicate exact skill112 Armor Break row");
            return reuse;
        }
    }

    private static void assertExactProbabilityRow(
            AccountRepository repository, String database) throws Exception {
        assertCount(repository, "SELECT COUNT(*) FROM " + database
                + ".`skills_負面技能控制` WHERE skill_id=112"
                + " AND note='破壞盔甲' AND prob_high=50"
                + " AND prob_equal=40 AND prob_low=30"
                + " AND bonus_str=0 AND bonus_dex=0 AND bonus_int=0"
                + " AND check_resist_type=0 AND check_magic_level=0"
                + " AND check_magic_hit=0 AND mr_multiplier=0"
                + " AND con_multiplier=0 AND wis_multiplier=0", 1);
    }

    private static boolean hasColumn(AccountRepository repository, String database,
                                     String table, String column) throws Exception {
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND TABLE_NAME=?"
                             + " AND COLUMN_NAME=?")) {
            statement.setString(1, database);
            statement.setString(2, table);
            statement.setString(3, column);
            try (ResultSet row = statement.executeQuery()) {
                return row.next() && row.getInt(1) == 1;
            }
        }
    }

    private static Map<String, Integer> skillAuxiliaryRows(
            AccountRepository repository, String database, int skillId) throws Exception {
        Map<String, Integer> result = new LinkedHashMap<>();
        try (Connection connection = connection(repository);
             PreparedStatement tables = connection.prepareStatement(
                     "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND COLUMN_NAME='skill_id'"
                             + " AND TABLE_NAME LIKE 'skills%'"
                             + " AND TABLE_NAME<>'skills' ORDER BY TABLE_NAME")) {
            tables.setString(1, database);
            try (ResultSet rows = tables.executeQuery()) {
                while (rows.next()) {
                    String table = rows.getString(1);
                    String trusted = table.replace("`", "``");
                    try (PreparedStatement count = connection.prepareStatement(
                            "SELECT COUNT(*) FROM " + database + ".`" + trusted
                                    + "` WHERE skill_id=?")) {
                        count.setInt(1, skillId);
                        try (ResultSet value = count.executeQuery()) {
                            assertTrue(value.next());
                            result.put(table, value.getInt(1));
                        }
                    }
                }
            }
        }
        return result;
    }

    private static void assertCount(AccountRepository repository, String sql,
                                    int expected) throws Exception {
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            assertEquals(expected, row.getInt(1));
        }
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
