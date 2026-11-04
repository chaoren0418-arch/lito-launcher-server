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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.BeforeAll;

/** Read-only proof for native skill 130 Body to Mind and its source split. */
final class LegacyBodyToMindIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/src/com/lineage/server/clientpackets/C_UseSkill.java"),
                Path.of("../1-伺服器/src/com/lineage/server/datatables/user/CharBuffTable.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillId.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/skillmode/BODY_TO_MIND.java"),
                Path.of("../1-伺服器/其他src參考用/381n8src/com/lineage/server/model/skill/L1SkillUse.java"),
                Path.of("../1-伺服器/其他src參考用/381n8src/com/lineage/server/model/skill/skillmode/BODY_TO_MIND.java"));
    }

    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void everyRetainedSourceHasTheExactBodyToMindContract() throws Exception {
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
                                     + ".`skills` WHERE skill_id=130");
                     ResultSet row = statement.executeQuery()) {
                    assertTrue(row.next(), "missing exact skill130 row");
                    assertEquals(130, row.getInt("skill_id"));
                    assertEquals("心靈轉換", row.getString("name"));
                    assertEquals(17, row.getInt("skill_level"));
                    assertEquals(1, row.getInt("skill_number"));
                    assertEquals(0, row.getInt("mpConsume"));
                    assertEquals(8, row.getInt("hpConsume"));
                    assertEquals(0, row.getInt("itemConsumeId"));
                    assertEquals(0, row.getInt("itemConsumeCount"));
                    assertEquals(expectedReuse, row.getInt("reuseDelay"));
                    assertEquals(0, row.getInt("buffDuration"));
                    assertEquals("none", row.getString("target"));
                    assertEquals(0, row.getInt("target_to"));
                    assertEquals(0, row.getInt("damage_value"));
                    assertEquals(0, row.getInt("damage_dice"));
                    assertEquals(0, row.getInt("damage_dice_count"));
                    assertEquals(0, row.getInt("probability_value"));
                    assertEquals(0, row.getInt("probability_dice"));
                    assertEquals(0, row.getInt("attr"));
                    assertEquals(2, row.getInt("type"));
                    assertEquals(0, row.getInt("lawful"));
                    assertEquals(0, row.getInt("ranged"));
                    assertEquals(0, row.getInt("area"));
                    assertEquals(0, row.getInt("through"));
                    assertEquals(2, row.getInt("id"));
                    assertEquals("", row.getString("nameid"));
                    assertEquals(19, row.getInt("action_id"));
                    assertEquals(2179, row.getInt("castgfx"));
                    assertEquals(0, row.getInt("castgfx2"));
                    assertEquals(702, row.getInt("sysmsgID_happen"));
                    assertEquals(0, row.getInt("sysmsgID_stop"));
                    assertEquals(0, row.getInt("sysmsgID_fail"));
                    if (effectSkillId) {
                        assertEquals(0, row.getInt("effect_skill_id"));
                    }
                    assertFalse(row.next(), "duplicate skill130 row");
                }
                try (Connection connection = connection(repository);
                     PreparedStatement statement = connection.prepareStatement(
                             "SELECT COUNT(*) FROM " + database
                                     + ".`skills_item` WHERE skill_id=130"
                                     + " AND name='心靈轉換' AND itemids IS NULL"
                                     + " AND counts IS NULL");
                     ResultSet row = statement.executeQuery()) {
                    assertTrue(row.next());
                    assertEquals(1, row.getInt(1));
                }
                verified++;
                System.out.printf("[LiTo-test] source=%s Body-to-Mind-row=exact"
                                + " schema=%s selector=16/1 HP8 MP+2 reuse=%d"
                                + " action19 gfx2179 happen702%n",
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
        assertTrue(client.contains("case 130"));

        String skillIds = read(retained,
                "src/com/lineage/server/model/skill/L1SkillId.java");
        assertTrue(skillIds.contains("BODY_TO_MIND = 130"));

        String mode = read(retained,
                "src/com/lineage/server/model/skill/skillmode/BODY_TO_MIND.java");
        assertTrue(mode.contains("srcpc.adjustCurrentMp(2)"));

        String use = read(retained,
                "src/com/lineage/server/model/skill/L1SkillUse.java");
        assertTrue(use.contains("currentHp < this._hpConsume + 1"));
        assertTrue(use.contains("this._player.adjustCurrentHp(-((long) this._hpConsume))"));
        assertTrue(use.contains("114, 115, 116, 117, 118, 129, 130"));

        String charBuffs = read(retained,
                "src/com/lineage/server/datatables/user/CharBuffTable.java");
        assertFalse(charBuffs.contains("BODY_TO_MIND"),
                "instant conversion must not be persisted as a lease");

        Path older = retained.resolve(Path.of("其他src參考用", "381n8src"));
        String olderMode = read(older,
                "com/lineage/server/model/skill/skillmode/BODY_TO_MIND.java");
        String olderUse = read(older,
                "com/lineage/server/model/skill/L1SkillUse.java");
        assertTrue(olderMode.contains(
                "srcpc.setCurrentMp(srcpc.getCurrentMp() + 2)"));
        assertTrue(olderUse.contains("currentHp < this._hpConsume + 1"));
        assertTrue(olderUse.contains(
                "final int current_hp = this._player.getCurrentHp()"));
        assertTrue(olderUse.contains("- this._hpConsume;"));
    }

    private static String read(Path root, String relative) throws Exception {
        return LocalResources.readString(root.resolve(relative.replace('/', '\\')),
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
