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
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Read-only live-schema proof for retained Storm Shot skill166. */
final class LegacyStormShotIntegrationTest {
    @BeforeAll
    static void requireMySqlIntegration() {
        Assumptions.assumeTrue(Boolean.getBoolean("lito.mysql.integration"));
    }

    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void everyRoutedSourcePinsSkill166AndSignedBowAbilities() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of(System.getProperty(
                "lito.legacy.config",
                "src/test/resources/server-config/config/server.source-router-legacy-probe.properties")));
        try (AccountRepository routed = AccountRepository.open(config)) {
            routed.healthCheck();
            int verified = 0;
            for (String source : config.classicServerNames()) {
                AccountRepository repository = routed.repositoryForClassicSource(source);
                String database = stringField(repository, "resourceDatabase");
                assertTrue(database.matches("[A-Za-z0-9_]+"));
                try (Connection connection = connection(repository);
                     PreparedStatement statement = connection.prepareStatement(
                             "SELECT * FROM " + database
                                     + ".`skills` WHERE skill_id=166");
                     ResultSet row = statement.executeQuery()) {
                    assertTrue(row.next());
                    assertEquals("暴風神射", row.getString("name"));
                    assertEquals(21, row.getInt("skill_level"));
                    assertEquals(5, row.getInt("skill_number"));
                    assertEquals(30, row.getInt("mpConsume"));
                    assertEquals(0, row.getInt("hpConsume"));
                    assertEquals(0, row.getInt("itemConsumeId"));
                    assertEquals(0, row.getInt("itemConsumeCount"));
                    assertEquals(0, row.getInt("reuseDelay"));
                    assertEquals(960, row.getInt("buffDuration"));
                    assertEquals("none", row.getString("target"));
                    assertEquals(0, row.getInt("target_to"));
                    assertEquals(0, row.getInt("attr"));
                    assertEquals(2, row.getInt("type"));
                    assertEquals(0, row.getInt("ranged"));
                    assertEquals(0, row.getInt("area"));
                    assertEquals(32, row.getInt("id"));
                    assertEquals(19, row.getInt("action_id"));
                    assertEquals(2248, row.getInt("castgfx"));
                    assertEquals(724, row.getInt("sysmsgID_stop"));
                    assertEquals(280, row.getInt("sysmsgID_fail"));
                    assertFalse(row.next());
                }
                assertEquals(1, count(repository, database, "skills_item",
                        "skill_id=166 AND itemids IS NULL AND counts IS NULL"));
                assertEquals(1, count(repository, database, "skills_buff_ability",
                        "skill_id=166 AND ability_type='BOW_HIT'"
                                + " AND ability_value=-1"));
                assertEquals(1, count(repository, database, "skills_buff_ability",
                        "skill_id=166 AND ability_type='BOW_DMG'"
                                + " AND ability_value=5"));
                assertEquals(2, count(repository, database,
                        "skills_buff_ability", "skill_id=166"));
                verified++;
                System.out.printf("[LiTo-test] source=%s schema=%s"
                                + " Storm-Shot-row=exact selector=20/5"
                                + " MP30 duration960 target=self attr=none"
                                + " bow-hit=-1 bow-damage=5 action19 gfx2248"
                                + " stop724 failure280%n",
                        source, database);
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
