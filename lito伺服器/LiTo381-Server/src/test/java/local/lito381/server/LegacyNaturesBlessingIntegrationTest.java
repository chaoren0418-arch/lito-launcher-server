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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Read-only live-schema proof for retained Nature's Blessing skill164. */
final class LegacyNaturesBlessingIntegrationTest {
    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void everyRoutedSourcePinsSkill164PartyPetWaterHealRow() throws Exception {
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
                                     + ".`skills` WHERE skill_id=164");
                     ResultSet row = statement.executeQuery()) {
                    assertTrue(row.next(), "missing exact skill164 row");
                    assertEquals("生命的祝福", row.getString("name"));
                    assertEquals(21, row.getInt("skill_level"));
                    assertEquals(3, row.getInt("skill_number"));
                    assertEquals(30, row.getInt("mpConsume"));
                    assertEquals(0, row.getInt("hpConsume"));
                    assertEquals(0, row.getInt("itemConsumeId"));
                    assertEquals(0, row.getInt("itemConsumeCount"));
                    assertEquals(primary ? 300 : 0, row.getInt("reuseDelay"));
                    assertEquals(0, row.getInt("buffDuration"));
                    assertEquals("none", row.getString("target"));
                    assertEquals(24, row.getInt("target_to"));
                    assertEquals(10, row.getInt("damage_value"));
                    assertEquals(12, row.getInt("damage_dice"));
                    assertEquals(0, row.getInt("damage_dice_count"));
                    assertEquals(4, row.getInt("attr"));
                    assertEquals(16, row.getInt("type"));
                    assertEquals(0, row.getInt("ranged"));
                    assertEquals(-1, row.getInt("area"));
                    assertEquals(8, row.getInt("id"));
                    assertEquals(19, row.getInt("action_id"));
                    assertEquals(2244, row.getInt("castgfx"));
                    if (effectSkillId) assertEquals(0, row.getInt("effect_skill_id"));
                    assertFalse(row.next(), "duplicate skill164 row");
                }
                assertEquals(1, count(repository, database, "skills_item",
                        "skill_id=164 AND itemids IS NULL AND counts IS NULL"));
                assertEquals(0, count(repository, database, "skills_buff_ability",
                        "skill_id=164"));
                verified++;
                System.out.printf("[LiTo-test] source=%s schema=%s"
                                + " Natures-Blessing-row=exact selector=20/3"
                                + " MP30 reuse=%d target=party|pet area=screen"
                                + " healing=(10+MB)d12 attr=water action19 gfx2244%n",
                        source, database, primary ? 300 : 0);
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
