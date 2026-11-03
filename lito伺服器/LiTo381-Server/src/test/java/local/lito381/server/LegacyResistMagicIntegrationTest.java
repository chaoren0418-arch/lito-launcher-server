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

/** Read-only proof for retained skill 129 Resist Magic and MR packet semantics. */
final class LegacyResistMagicIntegrationTest {
    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void everyRetainedSourceHasTheExactResistMagicContract() throws Exception {
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
                boolean effectSkillId = hasColumn(
                        repository, database, "skills", "effect_skill_id");
                try (Connection connection = connection(repository);
                     PreparedStatement statement = connection.prepareStatement(
                             "SELECT * FROM " + database
                                     + ".`skills` WHERE skill_id=129");
                     ResultSet row = statement.executeQuery()) {
                    assertTrue(row.next(), "missing exact skill129 row");
                    assertEquals(129, row.getInt("skill_id"));
                    assertEquals("魔法防禦", row.getString("name"));
                    assertEquals(17, row.getInt("skill_level"));
                    assertEquals(0, row.getInt("skill_number"));
                    assertEquals(5, row.getInt("mpConsume"));
                    assertEquals(0, row.getInt("hpConsume"));
                    assertEquals(40319, row.getInt("itemConsumeId"));
                    assertEquals(1, row.getInt("itemConsumeCount"));
                    assertEquals(0, row.getInt("reuseDelay"));
                    assertEquals(1_200, row.getInt("buffDuration"));
                    assertEquals("none", row.getString("target"));
                    assertEquals(0, row.getInt("target_to"));
                    assertEquals(0, row.getInt("ranged"));
                    assertEquals(0, row.getInt("area"));
                    assertEquals(0, row.getInt("attr"));
                    assertEquals(2, row.getInt("type"));
                    assertEquals(1, row.getInt("id"));
                    assertEquals(19, row.getInt("action_id"));
                    assertEquals(2186, row.getInt("castgfx"));
                    assertEquals(0, row.getInt("sysmsgID_happen"));
                    assertEquals(719, row.getInt("sysmsgID_stop"));
                    assertEquals(0, row.getInt("sysmsgID_fail"));
                    if (effectSkillId) {
                        assertEquals(0, row.getInt("effect_skill_id"));
                    }
                    assertFalse(row.next(), "duplicate skill129 row");
                }
                try (Connection connection = connection(repository);
                     PreparedStatement statement = connection.prepareStatement(
                             "SELECT COUNT(*) FROM " + database
                                     + ".`skills_item` WHERE skill_id=129"
                                     + " AND name='魔法防禦' AND itemids IS NULL"
                                     + " AND counts IS NULL");
                     ResultSet row = statement.executeQuery()) {
                    assertTrue(row.next());
                    assertEquals(1, row.getInt(1));
                }
                verified++;
                System.out.printf("[LiTo-test] source=%s Resist-Magic-row=exact"
                                + " schema=%s selector=16/0 MP5 SpiritCrystal40319x1"
                                + " duration1200 action19 gfx2186 MR10 stop719%n",
                        source, database);
            }
            assertEquals(config.classicServerNames().size(), verified);
        }
        assertRetainedJavaSemantics();
    }

    private static void assertRetainedJavaSemantics() throws Exception {
        Path retained = Path.of("..", "1-伺服器")
                .toAbsolutePath().normalize();
        String client = read(retained, "src/com/lineage/server/clientpackets/C_UseSkill.java");
        assertTrue(client.contains("case 129"));

        String skillIds = read(retained,
                "src/com/lineage/server/model/skill/L1SkillId.java");
        assertTrue(skillIds.contains("RESIST_MAGIC = 129"));

        String use = read(retained,
                "src/com/lineage/server/model/skill/L1SkillUse.java");
        assertTrue(use.contains("this._skillId == RESIST_MAGIC"));
        assertTrue(use.contains("pc.addMr(10)"));
        assertTrue(use.contains("new S_SPMR(pc)"));
        assertTrue(use.contains("114, 115, 116, 117, 118, 129"));

        String stop = read(retained,
                "src/com/lineage/server/model/skill/L1SkillStop.java");
        assertTrue(stop.contains("case RESIST_MAGIC"));
        assertTrue(stop.contains("cha.addMr(-10)"));
        assertTrue(stop.contains("getSysmsgIdStop()"));

        String status = read(retained,
                "src/com/lineage/server/serverpackets/S_SPMR.java");
        assertTrue(status.contains("writeC(S_OPCODE_SPMR)"));
        assertTrue(status.contains("pc.getTrueMr() - pc.getBaseMr()"));

        String buffs = read(retained,
                "src/com/lineage/server/datatables/user/CharBuffTable.java");
        assertFalse(buffs.contains("RESIST_MAGIC"),
                "selected source does not persist this lease");

        Path older = retained.resolve(Path.of("其他src參考用", "381n8src"));
        String olderUse = read(older,
                "com/lineage/server/model/skill/L1SkillUse.java");
        String olderStop = read(older,
                "com/lineage/server/model/skill/L1SkillStop.java");
        assertTrue(olderUse.contains("this._skillId == RESIST_MAGIC"));
        assertTrue(olderUse.contains("pc.addMr(10)"));
        assertTrue(olderStop.contains("case RESIST_MAGIC"));
        assertTrue(olderStop.contains("cha.addMr(-10)"));
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
