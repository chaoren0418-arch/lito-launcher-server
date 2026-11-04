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
import java.util.Set;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Read-only live-schema proof for retained Striker Gale skill174. */
final class LegacyStrikerGaleIntegrationTest {
    @BeforeAll
    static void requireMySqlIntegration() {
        Assumptions.assumeTrue(Boolean.getBoolean("lito.mysql.integration"));
    }

    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void everyRoutedSourcePinsSkill174AndItsOwnCounterMagicProfile()
            throws Exception {
        String configPath = System.getProperty("lito.legacy.config",
                "src/test/resources/server-config/config/server.source-router-legacy-probe.properties");
        ServerConfig config = ServerConfig.load(Path.of(configPath));
        try (AccountRepository routed = AccountRepository.open(config)) {
            routed.healthCheck();
            int verified = 0;
            for (String source : config.classicServerNames()) {
                AccountRepository repository =
                        routed.repositoryForClassicSource(source);
                String database = stringField(repository, "resourceDatabase");
                assertTrue(database.matches("[A-Za-z0-9_]+"));
                boolean primary = database.equals("380_db");
                boolean effectSkillId = hasColumn(
                        repository, database, "skills", "effect_skill_id");
                try (Connection connection = connection(repository);
                     PreparedStatement statement = connection.prepareStatement(
                             "SELECT * FROM " + database
                                     + ".`skills` WHERE skill_id=174");
                     ResultSet row = statement.executeQuery()) {
                    assertTrue(row.next(), "missing exact skill174 row");
                    assertEquals("精準射擊", row.getString("name"));
                    assertEquals(22, row.getInt("skill_level"));
                    assertEquals(5, row.getInt("skill_number"));
                    assertEquals(10, row.getInt("mpConsume"));
                    assertEquals(40319, row.getInt("itemConsumeId"));
                    assertEquals(5, row.getInt("itemConsumeCount"));
                    assertEquals(primary ? 3_000 : 0, row.getInt("reuseDelay"));
                    assertEquals(192, row.getInt("buffDuration"));
                    assertEquals("buff", row.getString("target"));
                    assertEquals(3, row.getInt("target_to"));
                    assertEquals(50, row.getInt("probability_value"));
                    assertEquals(30, row.getInt("probability_dice"));
                    assertEquals(8, row.getInt("attr"));
                    assertEquals(1, row.getInt("type"));
                    assertEquals(-1, row.getInt("ranged"));
                    assertEquals(0, row.getInt("area"));
                    assertEquals(32, row.getInt("id"));
                    assertEquals(19, row.getInt("action_id"));
                    assertEquals(5826, row.getInt("castgfx"));
                    assertEquals(280, row.getInt("sysmsgID_fail"));
                    if (effectSkillId) assertEquals(0, row.getInt("effect_skill_id"));
                    assertFalse(row.next(), "duplicate skill174 row");
                }
                try (Connection connection = connection(repository);
                     PreparedStatement statement = connection.prepareStatement(
                             "SELECT * FROM " + database
                                     + ".`skills_負面技能控制` WHERE skill_id=174");
                     ResultSet row = statement.executeQuery()) {
                    assertTrue(row.next());
                    assertEquals(55, row.getInt("prob_high"));
                    assertEquals(45, row.getInt("prob_equal"));
                    assertEquals(35, row.getInt("prob_low"));
                    assertEquals(0.3, row.getDouble("bonus_int"), 0.000_001);
                    assertEquals(1, row.getInt("check_magic_hit"));
                    assertEquals(0.05, row.getDouble("mr_multiplier"), 0.000_001);
                    assertFalse(row.next());
                }
                assertEquals(1, count(repository, database,
                        "skills_負面boss免疫", "skill_id=174 AND modifier=0"));
                assertEquals(1, count(repository, database,
                        "skills_item", "skill_id=174 AND itemids IS NULL"
                                + " AND counts IS NULL"));
                int bypassRows = count(repository, database,
                        "skills_魔屏無效技能", "skill_id=174");
                assertEquals(primary ? 1 : 0, bypassRows);
                Set<Integer> loadedBypasses = repository.counterMagicBypassSkills();
                assertEquals(primary, loadedBypasses.contains(
                        BattleSpellCatalog.STRIKER_GALE));
                if (!primary) assertEquals(Set.of(4), loadedBypasses);
                verified++;
            }
            assertEquals(config.classicServerNames().size(), verified);
        }
    }

    private static int count(AccountRepository repository, String database,
                             String table, String where) throws Exception {
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + database + ".`" + table
                             + "` WHERE " + where);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            return row.getInt(1);
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

    private static Connection connection(AccountRepository repository)
            throws Exception {
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
