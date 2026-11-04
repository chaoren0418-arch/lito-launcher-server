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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.BeforeAll;

/** Read-only proof for retained skill 115 Shining Aura and clan/party semantics. */
final class LegacyShiningAuraIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
    }

    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void everyRetainedSourceHasTheExactShiningAuraContract() throws Exception {
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
                assertExactAbilityRow(repository, database);
                assertCount(repository, "SELECT COUNT(*) FROM " + database
                        + ".`skills_item` WHERE skill_id=115"
                        + " AND name='鋼鐵士氣' AND itemids IS NULL"
                        + " AND counts IS NULL", 1);

                Map<String, Integer> auxiliary = skillAuxiliaryRows(
                        repository, database, BattleSpellCatalog.SHINING_AURA);
                Set<String> allowed = Set.of("skills_buff_ability", "skills_item");
                auxiliary.forEach((table, count) -> {
                    if (count != 0) {
                        assertTrue(allowed.contains(table),
                                "unexpected skill115 auxiliary row in " + table);
                    }
                });
                assertEquals(1, auxiliary.getOrDefault("skills_buff_ability", 0));
                assertEquals(1, auxiliary.getOrDefault("skills_item", 0));
                verified++;
                System.out.printf("[LiTo-test] source=%s shining-aura-row=exact"
                                + " schema=%s selector=14/2 MP40 duration640"
                                + " clan-or-party-screen AC-8 auxiliary=%s%n",
                        source, database, auxiliary);
            }
            assertEquals(config.classicServerNames().size(), verified);
        }
        assertRetainedJavaSemantics();
    }

    private static void assertExactSkillRow(
            AccountRepository repository, String database) throws Exception {
        boolean effectSkillIdColumn = hasColumn(
                repository, database, "skills", "effect_skill_id");
        String sql = "SELECT skill_id"
                + (effectSkillIdColumn ? ",effect_skill_id" : "")
                + " FROM " + database + ".`skills`"
                + " WHERE skill_id=115 AND name='鋼鐵士氣'"
                + " AND skill_level=15 AND skill_number=2"
                + " AND mpConsume=40 AND hpConsume=0"
                + " AND itemConsumeId=0 AND itemConsumeCount=0"
                + " AND reuseDelay=0 AND buffDuration=640"
                + " AND target='none' AND target_to=12"
                + " AND damage_value=0 AND damage_dice=0"
                + " AND damage_dice_count=0 AND probability_value=0"
                + " AND probability_dice=0 AND attr=0 AND type=2"
                + " AND lawful=0 AND ranged=0 AND area=-1 AND through=0"
                + " AND id=4 AND nameid='$1977'"
                + " AND action_id=19 AND castgfx=3941 AND castgfx2=0"
                + " AND sysmsgID_happen=0 AND sysmsgID_stop=0"
                + " AND sysmsgID_fail=0";
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill115 Shining Aura row");
            assertEquals(115, row.getInt("skill_id"));
            if (effectSkillIdColumn) assertEquals(0, row.getInt("effect_skill_id"));
            assertFalse(row.next(), "duplicate exact skill115 Shining Aura row");
        }
    }

    private static void assertExactAbilityRow(
            AccountRepository repository, String database) throws Exception {
        String sql = "SELECT ability_type,ability_value FROM " + database
                + ".`skills_buff_ability` WHERE skill_id=115";
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            assertEquals("AC", row.getString("ability_type"));
            assertEquals(-8, row.getInt("ability_value"));
            assertFalse(row.next());
        }
    }

    private static void assertRetainedJavaSemantics() throws Exception {
        Path retained = Path.of("..", "1-伺服器").toAbsolutePath().normalize();
        String currentUse = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "model", "skill",
                "L1SkillUse.java")), StandardCharsets.UTF_8);
        assertTrue(currentUse.contains("this._skillId == SHINING_AURA"));
        assertTrue(currentUse.contains("{ GLOWING_AURA, SHINING_AURA }"));
        assertTrue(currentUse.contains("L1Skills.TARGET_TO_CLAN"));
        assertTrue(currentUse.contains("_player.getClanid() != 0"));
        assertTrue(currentUse.contains("L1Skills.TARGET_TO_PARTY"));
        assertTrue(currentUse.contains("_player.getParty().isMember(xpc)"));
        assertTrue(currentUse.contains("World.get().getVisibleObjects(this._user)"));
        assertTrue(currentUse.contains("114, 115, 116, 117, 118"));
        assertTrue(currentUse.contains("pc.updateBuffStatus()"));

        String currentStop = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "model", "skill",
                "L1SkillStop.java")), StandardCharsets.UTF_8);
        assertTrue(currentStop.contains("case SHINING_AURA"));
        assertTrue(currentStop.contains("pc.updateBuffStatus()"));

        Path older = retained.resolve(Path.of(
                "其他src參考用", "381n8src", "com", "lineage", "server",
                "model", "skill"));
        String olderUse = Files.readString(
                older.resolve("L1SkillUse.java"), StandardCharsets.UTF_8);
        assertTrue(olderUse.contains("this._skillId == SHINING_AURA"));
        assertTrue(olderUse.contains("pc.addAc(-8)"));
        assertTrue(olderUse.contains("new S_PacketBoxIconAura(114"));
        String olderStop = Files.readString(
                older.resolve("L1SkillStop.java"), StandardCharsets.UTF_8);
        assertTrue(olderStop.contains("case SHINING_AURA"));
        assertTrue(olderStop.contains("cha.addAc(8)"));
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
