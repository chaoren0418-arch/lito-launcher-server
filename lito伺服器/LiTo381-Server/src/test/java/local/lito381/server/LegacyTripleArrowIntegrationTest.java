package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Read-only proof for native skill132 Triple Arrow and both retained rows. */
final class LegacyTripleArrowIntegrationTest {
    @BeforeAll
    static void requireMySqlIntegration() {
        Assumptions.assumeTrue(Boolean.getBoolean("lito.mysql.integration"));
    }

    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void everyRetainedSourceHasTheExactTripleArrowContract() throws Exception {
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
                int expectedReuse = switch (database) {
                    case "380_db" -> 100;
                    case "380yiwei_db" -> 400;
                    default -> {
                        fail("unexpected retained resource database " + database);
                        yield -1;
                    }
                };
                boolean effectSkillId = hasColumn(
                        repository, database, "skills", "effect_skill_id");
                try (Connection connection = connection(repository);
                     PreparedStatement statement = connection.prepareStatement(
                             "SELECT * FROM " + database
                                     + ".`skills` WHERE skill_id=132");
                     ResultSet row = statement.executeQuery()) {
                    assertTrue(row.next(), "missing exact skill132 row");
                    assertEquals(132, row.getInt("skill_id"));
                    assertEquals("三重矢", row.getString("name"));
                    assertEquals(17, row.getInt("skill_level"));
                    assertEquals(3, row.getInt("skill_number"));
                    assertEquals(11, row.getInt("mpConsume"));
                    assertEquals(0, row.getInt("hpConsume"));
                    assertEquals(0, row.getInt("itemConsumeId"));
                    assertEquals(0, row.getInt("itemConsumeCount"));
                    assertEquals(expectedReuse, row.getInt("reuseDelay"));
                    assertEquals(0, row.getInt("buffDuration"));
                    assertEquals("attack", row.getString("target"));
                    assertEquals(3, row.getInt("target_to"));
                    assertEquals(0, row.getInt("damage_value"));
                    assertEquals(0, row.getInt("damage_dice"));
                    assertEquals(0, row.getInt("damage_dice_count"));
                    assertEquals(0, row.getInt("probability_value"));
                    assertEquals(0, row.getInt("probability_dice"));
                    assertEquals(0, row.getInt("attr"));
                    assertEquals(64, row.getInt("type"));
                    assertEquals(0, row.getInt("lawful"));
                    assertEquals(-1, row.getInt("ranged"));
                    assertEquals(0, row.getInt("area"));
                    assertEquals(0, row.getInt("through"));
                    assertEquals(8, row.getInt("id"));
                    assertEquals("", row.getString("nameid"));
                    assertEquals(18, row.getInt("action_id"));
                    assertEquals(0, row.getInt("castgfx"));
                    assertEquals(0, row.getInt("castgfx2"));
                    assertEquals(0, row.getInt("sysmsgID_happen"));
                    assertEquals(0, row.getInt("sysmsgID_stop"));
                    assertEquals(0, row.getInt("sysmsgID_fail"));
                    if (effectSkillId) {
                        assertEquals(0, row.getInt("effect_skill_id"));
                    }
                    assertFalse(row.next(), "duplicate skill132 row");
                }
                try (Connection connection = connection(repository);
                     PreparedStatement statement = connection.prepareStatement(
                             "SELECT COUNT(*) FROM " + database
                                     + ".`skills_item` WHERE skill_id=132"
                                     + " AND name='三重矢' AND itemids IS NULL"
                                     + " AND counts IS NULL");
                     ResultSet row = statement.executeQuery()) {
                    assertTrue(row.next());
                    assertEquals(1, row.getInt(1));
                }
                verified++;
                System.out.printf("[LiTo-test] source=%s Triple-Arrow-row=exact"
                                + " schema=%s selector=16/3 MP11 reuse=%d"
                                + " action18 mode=3x-onAction+gfx4394%n",
                        source, database, expectedReuse);
            }
            assertEquals(config.classicServerNames().size(), verified);
        }
        assertRetainedJavaSemantics();
    }

    private static void assertRetainedJavaSemantics() throws Exception {
        Path retained = Path.of("..", "1-伺服器")
                .toAbsolutePath().normalize();
        String client = read(retained,
                "src/com/lineage/server/clientpackets/C_UseSkill.java");
        assertTrue(client.contains("case 132"));

        String skillIds = read(retained,
                "src/com/lineage/server/model/skill/L1SkillId.java");
        assertTrue(skillIds.contains("TRIPLE_ARROW = 132"));

        String spr = read(retained,
                "src/com/lineage/server/datatables/SprTable.java");
        assertTrue(spr.contains("containsTripleArrowSpr"));
        assertTrue(spr.contains("containsKey(ACTION_BowAttack)"));

        String mode = read(retained,
                "src/com/lineage/server/model/skill/skillmode/TRIPLE_ARROW.java");
        assertTrue(mode.contains("containsTripleArrowSpr(srcpc.getTempCharGfx())"));
        assertTrue(mode.contains("setTripleArrowHitsRemaining(3)"));
        assertTrue(mode.contains("for (int i = 0; i < 3; i++)"));
        assertTrue(mode.contains("cha.onAction(srcpc)"));
        assertTrue(mode.contains("setTripleArrowHitsRemaining(0)"));
        assertTrue(mode.contains("new S_SkillSound(srcpc.getId(), 4394)"));

        String use = read(retained,
                "src/com/lineage/server/model/skill/L1SkillUse.java");
        assertTrue(use.contains("129, 130, 131, 132"));

        Path older = retained.resolve(Path.of("其他src參考用", "381n8src"));
        String olderMode = read(older,
                "com/lineage/server/model/skill/skillmode/TRIPLE_ARROW.java");
        assertTrue(olderMode.contains("containsTripleArrowSpr(playerGFX)"));
        assertTrue(olderMode.contains("for (int i = 0; i < 3; i++)"));
        assertTrue(olderMode.contains("cha.onAction(srcpc)"));
        assertTrue(olderMode.contains("new S_SkillSound(srcpc.getId(), 4394)"));
    }

    private static String read(Path root, String relative) throws Exception {
        return Files.readString(root.resolve(relative.replace('/', '\\')),
                StandardCharsets.UTF_8);
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
