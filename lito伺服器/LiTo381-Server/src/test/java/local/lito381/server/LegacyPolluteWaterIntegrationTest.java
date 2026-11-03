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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Read-only live-schema proof for retained Pollute Water skill 173. */
final class LegacyPolluteWaterIntegrationTest {
    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void everyRoutedSourcePinsSkill173AndItsOwnCounterMagicProfile()
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
                                     + ".`skills` WHERE skill_id=173");
                     ResultSet row = statement.executeQuery()) {
                    assertTrue(row.next(), "missing exact skill173 row");
                    assertEquals("污濁之水", row.getString("name"));
                    assertEquals(22, row.getInt("skill_level"));
                    assertEquals(4, row.getInt("skill_number"));
                    assertEquals(50, row.getInt("mpConsume"));
                    assertEquals(0, row.getInt("hpConsume"));
                    assertEquals(40319, row.getInt("itemConsumeId"));
                    assertEquals(1, row.getInt("itemConsumeCount"));
                    assertEquals(primary ? 1_000 : 0, row.getInt("reuseDelay"));
                    assertEquals(192, row.getInt("buffDuration"));
                    assertEquals("buff", row.getString("target"));
                    assertEquals(3, row.getInt("target_to"));
                    assertEquals(50, row.getInt("probability_value"));
                    assertEquals(50, row.getInt("probability_dice"));
                    assertEquals(4, row.getInt("attr"));
                    assertEquals(1, row.getInt("type"));
                    assertEquals(3, row.getInt("ranged"));
                    assertEquals(0, row.getInt("area"));
                    assertEquals(16, row.getInt("id"));
                    assertEquals(19, row.getInt("action_id"));
                    assertEquals(5830, row.getInt("castgfx"));
                    assertEquals(280, row.getInt("sysmsgID_fail"));
                    if (effectSkillId) assertEquals(0, row.getInt("effect_skill_id"));
                    assertFalse(row.next(), "duplicate skill173 row");
                }

                try (Connection connection = connection(repository);
                     PreparedStatement statement = connection.prepareStatement(
                             "SELECT * FROM " + database
                                     + ".`skills_負面技能控制` WHERE skill_id=173");
                     ResultSet row = statement.executeQuery()) {
                    assertTrue(row.next());
                    assertEquals(55, row.getInt("prob_high"));
                    assertEquals(45, row.getInt("prob_equal"));
                    assertEquals(35, row.getInt("prob_low"));
                    assertEquals(0.3, row.getDouble("bonus_int"), 0.000_001);
                    assertEquals(0, row.getInt("check_magic_level"));
                    assertEquals(1, row.getInt("check_magic_hit"));
                    assertEquals(0.05, row.getDouble("mr_multiplier"), 0.000_001);
                    assertFalse(row.next());
                }

                assertEquals(1, count(repository, database,
                        "skills_負面boss免疫", "skill_id=173 AND modifier=0"));
                assertEquals(1, count(repository, database,
                        "skills_item", "skill_id=173 AND itemids IS NULL"
                                + " AND counts IS NULL"));
                int bypassRows = count(repository, database,
                        "skills_魔屏無效技能", "skill_id=173");
                assertEquals(primary ? 1 : 0, bypassRows);
                Set<Integer> loadedBypasses = repository.counterMagicBypassSkills();
                assertEquals(primary, loadedBypasses.contains(
                        BattleSpellCatalog.POLLUTE_WATER));
                if (!primary) assertEquals(Set.of(4), loadedBypasses);

                verified++;
                System.out.printf("[LiTo-test] source=%s schema=%s"
                                + " Pollute-Water-row=exact selector=21/4"
                                + " MP50 SpiritCrystal40319x1 duration192"
                                + " action19 gfx5830 probability=55/45/35+INT*.3"
                                + " +MagicHit-MR*.05 boss-immune=true"
                                + " counter-magic-bypass=%s%n",
                        source, database, primary);
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
