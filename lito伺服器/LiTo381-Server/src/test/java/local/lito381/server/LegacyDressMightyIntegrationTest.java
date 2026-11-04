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
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Read-only proof for original skill 109 Strength Enhancement. */
final class LegacyDressMightyIntegrationTest {
    @BeforeAll
    static void requireMySqlIntegration() {
        Assumptions.assumeTrue(Boolean.getBoolean("lito.mysql.integration"));
    }

    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void everyRetainedSourceHasTheExactDressMightyContract() throws Exception {
        String configPath = System.getProperty(
                "lito.legacy.config", "src/test/resources/server-config/config/server.source-router-legacy-probe.properties");
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
                assertExactEmptyItemMarker(repository, database);

                Map<String, Integer> auxiliary = skillAuxiliaryRows(
                        repository, database, BattleSpellCatalog.DRESS_MIGHTY);
                assertEquals(1, auxiliary.getOrDefault("skills_buff_ability", 0));
                assertEquals(1, auxiliary.getOrDefault("skills_item", 0));
                auxiliary.forEach((table, count) -> {
                    if (!table.equals("skills_buff_ability")
                            && !table.equals("skills_item")) {
                        assertEquals(0, count,
                                "unexpected skill109 auxiliary row in " + table);
                    }
                });
                verified++;
                System.out.printf("[LiTo-test] source=%s dress-mighty-row=exact"
                                + " schema=%s str=2 duration=960 auxiliary=%s%n",
                        source, database, auxiliary);
            }
            assertEquals(config.classicServerNames().size(), verified);
        }
        assertRetainedReplacementAndNativeIconSources();
    }

    private static void assertExactSkillRow(AccountRepository repository,
                                            String database) throws Exception {
        boolean effectSkillIdColumn;
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND TABLE_NAME='skills'"
                             + " AND COLUMN_NAME='effect_skill_id'")) {
            statement.setString(1, database);
            try (ResultSet row = statement.executeQuery()) {
                effectSkillIdColumn = row.next() && row.getInt(1) == 1;
            }
        }
        String sql = "SELECT reuseDelay"
                + (effectSkillIdColumn ? ",effect_skill_id" : "")
                + " FROM " + database + ".`skills`"
                + " WHERE skill_id=109 AND name='\u529b\u91cf\u63d0\u5347'"
                + " AND skill_level=14 AND skill_number=4"
                + " AND mpConsume=10 AND hpConsume=0"
                + " AND itemConsumeId=0 AND itemConsumeCount=0"
                + " AND reuseDelay=0 AND buffDuration=960"
                + " AND target='none' AND target_to=0"
                + " AND damage_value=0 AND damage_dice=0 AND damage_dice_count=0"
                + " AND probability_value=0 AND probability_dice=0"
                + " AND attr=0 AND type=2 AND lawful=0"
                + " AND ranged=0 AND area=0 AND through=0"
                + " AND id=16 AND nameid=''"
                + " AND action_id=19 AND castgfx=3909 AND castgfx2=0"
                + " AND sysmsgID_happen=292 AND sysmsgID_stop=293"
                + " AND sysmsgID_fail=0";
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill109 Dress Mighty row");
            assertEquals(0, row.getInt("reuseDelay"));
            if (effectSkillIdColumn) assertEquals(0, row.getInt("effect_skill_id"));
            assertFalse(row.next(), "duplicate exact skill109 Dress Mighty row");
        }
    }

    private static void assertExactAbilityRow(AccountRepository repository,
                                              String database) throws Exception {
        assertCount(repository, "SELECT COUNT(*) FROM " + database
                + ".`skills_buff_ability` WHERE skill_id=109"
                + " AND name='\u529b\u91cf\u63d0\u5347'"
                + " AND ability_type='STR' AND ability_value=2", 1);
    }

    private static void assertExactEmptyItemMarker(AccountRepository repository,
                                                   String database) throws Exception {
        assertCount(repository, "SELECT COUNT(*) FROM " + database
                + ".`skills_item` WHERE skill_id=109"
                + " AND name='\u529b\u91cf\u63d0\u5347'"
                + " AND itemids IS NULL AND counts IS NULL", 1);
    }

    private static void assertRetainedReplacementAndNativeIconSources() throws Exception {
        Path retained = Path.of("..", "1-\u4f3a\u670d\u5668").toAbsolutePath().normalize();
        String currentUse = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "model", "skill",
                "L1SkillUse.java")), StandardCharsets.UTF_8);
        assertTrue(currentUse.contains("{ PHYSICAL_ENCHANT_STR, DRESS_MIGHTY }"));
        assertTrue(currentUse.contains("this._skillId == DRESS_MIGHTY"));
        assertTrue(currentUse.contains("IndependentSkillIcon.sendStart(pc, this._skillId"));

        String currentIcon = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "model", "skill",
                "IndependentSkillIcon.java")), StandardCharsets.UTF_8);
        assertTrue(currentIcon.contains("case DRESS_MIGHTY:"));
        assertTrue(currentIcon.contains("new S_Strup(pc, 2, timeSec)"));

        String currentStop = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "model", "skill",
                "L1SkillStop.java")), StandardCharsets.UTF_8);
        assertTrue(currentStop.contains("case DRESS_MIGHTY:"));
        assertTrue(currentStop.contains("IndependentSkillIcon.sendStop(pc, skillId)"));

        String writer = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "serverpackets",
                "S_Strup.java")), StandardCharsets.UTF_8);
        assertTrue(writer.contains("this.writeH(time);"));
        assertTrue(writer.contains("this.writeC(pc.getStr());"));
        assertTrue(writer.contains("this.writeC(pc.getInventory().getWeight240());"));
        assertTrue(writer.contains("this.writeC(type);"));

        Path ancient = retained.resolve(Path.of("\u5176\u4ed6src\u53c3\u8003\u7528", "\u53e4\u8001src"));
        String ancientUse = Files.readString(ancient.resolve(Path.of(
                "com", "lineage", "server", "model", "skill", "L1SkillUse.java")),
                StandardCharsets.UTF_8);
        String ancientStop = Files.readString(ancient.resolve(Path.of(
                "com", "lineage", "server", "model", "skill", "L1SkillStop.java")),
                StandardCharsets.UTF_8);
        assertTrue(ancientUse.contains("{ PHYSICAL_ENCHANT_STR, DRESS_MIGHTY }"));
        assertTrue(ancientUse.contains("new S_Strup(pc, 2, this._getBuffIconDuration)"));
        assertTrue(ancientStop.contains("new S_Strup(pc, 2, 0)"));
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
