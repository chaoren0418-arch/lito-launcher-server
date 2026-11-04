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

/** Read-only proof for the selected and secondary skill-113 database rows. */
final class LegacyTrueTargetIntegrationTest {
    @BeforeAll
    static void requireMySqlIntegration() {
        Assumptions.assumeTrue(Boolean.getBoolean("lito.mysql.integration"));
    }

    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void everyRetainedSourceHasTheExactTrueTargetContract() throws Exception {
        String configPath = System.getProperty(
                "lito.legacy.config",
                "src/test/resources/server-config/config/server.source-router-legacy-probe.properties");
        ServerConfig config = ServerConfig.load(Path.of(configPath));
        try (AccountRepository routed = AccountRepository.open(config)) {
            routed.healthCheck();
            int verified = 0;
            for (String source : config.classicServerNames()) {
                AccountRepository repository = routed.repositoryForClassicSource(source);
                String database = stringField(repository, "resourceDatabase");
                assertTrue(database.matches("[A-Za-z0-9_]+"));
                assertExactSkillRow(repository, database);
                assertCount(repository, "SELECT COUNT(*) FROM " + database
                        + ".`skills_item` WHERE skill_id=113"
                        + " AND name='精準目標' AND itemids IS NULL"
                        + " AND counts IS NULL", 1);

                Map<String, Integer> auxiliary = skillAuxiliaryRows(
                        repository, database, BattleSpellCatalog.TRUE_TARGET);
                Set<String> allowed = Set.of("skills_item");
                auxiliary.forEach((table, count) -> {
                    if (count != 0) {
                        assertTrue(allowed.contains(table),
                                "unexpected skill113 auxiliary row in " + table);
                        assertEquals(1, count,
                                "duplicate skill113 auxiliary row in " + table);
                    }
                });
                assertEquals(1, auxiliary.getOrDefault("skills_item", 0));
                verified++;
                System.out.printf("[LiTo-test] source=%s true-target-row=exact"
                                + " schema=%s selector=14/0 MP1 action19 gfx1765"
                                + " target-to=PC|NPC range=native-screen auxiliary=%s%n",
                        source, database, auxiliary);
            }
            assertEquals(config.classicServerNames().size(), verified);
        }
    }

    private static void assertExactSkillRow(
            AccountRepository repository, String database) throws Exception {
        boolean effectSkillIdColumn = hasColumn(
                repository, database, "skills", "effect_skill_id");
        String sql = "SELECT skill_id"
                + (effectSkillIdColumn ? ",effect_skill_id" : "")
                + " FROM " + database + ".`skills`"
                + " WHERE skill_id=113 AND name='精準目標'"
                + " AND skill_level=15 AND skill_number=0"
                + " AND mpConsume=1 AND hpConsume=0"
                + " AND itemConsumeId=0 AND itemConsumeCount=0"
                + " AND reuseDelay=0 AND buffDuration=0"
                + " AND target='buff' AND target_to=3"
                + " AND damage_value=0 AND damage_dice=0"
                + " AND damage_dice_count=0 AND probability_value=0"
                + " AND probability_dice=0 AND attr=0 AND type=2"
                + " AND lawful=0 AND ranged=-1 AND area=0 AND through=0"
                + " AND id=1 AND nameid='$1995'"
                + " AND action_id=19 AND castgfx=1765 AND castgfx2=0"
                + " AND sysmsgID_happen=0 AND sysmsgID_stop=0"
                + " AND sysmsgID_fail=0";
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill113 True Target row");
            assertEquals(113, row.getInt("skill_id"));
            if (effectSkillIdColumn) assertEquals(0, row.getInt("effect_skill_id"));
            assertFalse(row.next(), "duplicate exact skill113 True Target row");
        }
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
