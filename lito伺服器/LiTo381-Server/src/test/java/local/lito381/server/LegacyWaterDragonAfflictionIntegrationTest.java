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

/** Read-only selected/secondary schema proof for Water Dragon encounter rows. */
final class LegacyWaterDragonAfflictionIntegrationTest {
    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void selectedSchemaOwnsExactRowsAndSecondaryAbsenceIsExplicit() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of(System.getProperty(
                "lito.legacy.config",
                "src/test/resources/server-config/config/server.source-router-legacy-probe.properties")));
        try (AccountRepository routed = AccountRepository.open(config)) {
            routed.healthCheck();
            int verified = 0;
            for (String source : config.classicServerNames()) {
                AccountRepository repository = routed.repositoryForClassicSource(source);
                String database = stringField(repository, "resourceDatabase");
                boolean selected = database.equals("380_db");
                int baseRows = count(repository, database, "skills",
                        "skill_id BETWEEN 4011 AND 4013");
                int aliasRows = count(repository, database, "skills",
                        "skill_id BETWEEN 49011 AND 49013");
                if (!selected) {
                    assertEquals(0, baseRows);
                    assertEquals(0, aliasRows);
                    System.out.printf("[LiTo-test] source=%s schema=%s"
                                    + " Water-Dragon-afflictions=absent-older-source%n",
                            source, database);
                    verified++;
                    continue;
                }

                assertEquals(3, baseRows);
                assertEquals(3, aliasRows);
                int[] ids = {4011, 4012, 4013};
                int[] aliases = {49011, 49012, 49013};
                int[] gfx = {7781, 7782, 7780};
                String[] names = {"藥水侵蝕術", "污濁的水流", "治癒侵蝕術"};
                for (int index = 0; index < ids.length; index++) {
                    assertSkill(repository, database, ids[index], 0,
                            names[index], gfx[index]);
                    assertSkill(repository, database, aliases[index], ids[index],
                            names[index], gfx[index]);
                }
                assertEquals(1, count(repository, database, "mobskill",
                        "mobid=110006 AND actNo=3 AND skillid=49013"));
                assertEquals(1, count(repository, database, "mobskill",
                        "mobid=110010 AND actNo=5 AND skillid=49011"));
                assertEquals(1, count(repository, database, "mobskill",
                        "mobid=110010 AND actNo=6 AND skillid=49013"));
                System.out.printf("[LiTo-test] source=%s schema=%s"
                                + " Water-Dragon-afflictions=4011..4013"
                                + " aliases=49011..49013 duration=12000ms"
                                + " gfx=7781/7782/7780 mob-producers=exact%n",
                        source, database);
                verified++;
            }
            assertEquals(config.classicServerNames().size(), verified);
        }
    }

    private static void assertSkill(AccountRepository repository, String database,
                                    int id, int effectId, String name, int gfx)
            throws Exception {
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM " + database + ".`skills` WHERE skill_id=?")) {
            statement.setInt(1, id);
            try (ResultSet row = statement.executeQuery()) {
                assertTrue(row.next());
                assertEquals(name, row.getString("name"));
                assertEquals(effectId, row.getInt("effect_skill_id"));
                assertEquals(12_000, row.getInt("buffDuration"));
                assertEquals("buff", row.getString("target"));
                assertEquals(3, row.getInt("target_to"));
                assertEquals(128, row.getInt("type"));
                assertEquals(0, row.getInt("id"));
                assertEquals(0, row.getInt("action_id"));
                assertEquals(gfx, row.getInt("castgfx"));
                assertFalse(row.next());
            }
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
