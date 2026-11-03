package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Read-only proof for retained skill 99, its AC row and original coexist mode. */
final class LegacyShadowArmorIntegrationTest {
    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void everyRetainedSourceHasTheExactShadowArmorContract() throws Exception {
        String configPath = System.getProperty(
                "lito.legacy.config", "src/test/resources/server-config/config/server.source-router-legacy-probe.properties");
        ServerConfig config = ServerConfig.load(Path.of(configPath));
        int coexistOverrides = 0;
        int coexistFallbacks = 0;
        try (AccountRepository routed = AccountRepository.open(config)) {
            routed.healthCheck();
            int verified = 0;
            for (String source : config.classicServerNames()) {
                AccountRepository repository = routed.repositoryForClassicSource(source);
                String database = stringField(repository, "resourceDatabase");
                assertTrue(database.matches("[A-Za-z0-9_]+"),
                        "invalid retained resource database");

                assertExactSkillRow(repository, database);
                Map<String, Integer> auxiliaryRows = skillAuxiliaryRows(
                        repository, database, BattleSpellCatalog.SHADOW_ARMOR);
                assertEquals(1, auxiliaryRows.getOrDefault("skills_buff_ability", 0));
                assertEquals(1, auxiliaryRows.getOrDefault("skills_item", 0));
                auxiliaryRows.forEach((table, count) -> {
                    if (!table.equals("skills_buff_ability")
                            && !table.equals("skills_item")) {
                        assertEquals(0, count,
                                "unexpected skill 99 auxiliary row in " + table);
                    }
                });
                assertExactAbilityRow(repository, database);
                assertExactEmptyItemMarker(repository, database);

                int eventState = coexistEventState(repository, database);
                coexistOverrides += eventState == 1 ? 1 : 0;
                coexistFallbacks += eventState == -1 ? 1 : 0;
                verified++;
                System.out.printf(
                        "[LiTo-test] source=%s shadow-armor-skill-row=exact"
                                + " schema=%s ac=-3 coexist-event-state=%d"
                                + " auxiliary=%s%n",
                        source, database, eventState, auxiliaryRows);
            }
            assertEquals(config.classicServerNames().size(), verified,
                    "not every retained source has the exact skill 99 contract");
        }
        assertTrue(coexistOverrides >= 1,
                "primary retained source no longer proves its custom coexist override");
        if (config.classicServerNames().size() > 1) {
            assertTrue(coexistFallbacks >= 1,
                    "secondary retained source no longer proves missing-row fallback");
        }
        assertRetainedOriginalReplacementSource();
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
                + " WHERE skill_id=99 AND name='\u5f71\u4e4b\u9632\u8b77'"
                + " AND skill_level=13 AND skill_number=2"
                + " AND mpConsume=10 AND hpConsume=5"
                + " AND itemConsumeId=0 AND itemConsumeCount=0"
                + " AND reuseDelay=0 AND buffDuration=960"
                + " AND target='none' AND target_to=0"
                + " AND damage_value=0 AND damage_dice=0 AND damage_dice_count=0"
                + " AND probability_value=0 AND probability_dice=0"
                + " AND attr=0 AND type=2 AND lawful=0"
                + " AND ranged=0 AND area=0 AND through=0"
                + " AND id=4 AND nameid='$2506'"
                + " AND action_id=19 AND castgfx=2943 AND castgfx2=0"
                + " AND sysmsgID_happen=0 AND sysmsgID_stop=0"
                + " AND sysmsgID_fail=0";
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill 99 Shadow Armor source row");
            assertEquals(0, row.getInt("reuseDelay"));
            if (effectSkillIdColumn) assertEquals(0, row.getInt("effect_skill_id"));
            assertTrue(!row.next(), "duplicate exact skill 99 Shadow Armor source row");
        }
    }

    private static void assertExactAbilityRow(AccountRepository repository,
                                              String database) throws Exception {
        String sql = "SELECT COUNT(*) FROM " + database + ".`skills_buff_ability`"
                + " WHERE skill_id=99 AND name='\u5f71\u4e4b\u9632\u8b77'"
                + " AND ability_type='AC' AND ability_value=-3";
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            assertEquals(1, row.getInt(1),
                    "missing or duplicate exact Shadow Armor AC row");
        }
    }

    private static void assertExactEmptyItemMarker(AccountRepository repository,
                                                   String database) throws Exception {
        String sql = "SELECT COUNT(*) FROM " + database + ".`skills_item`"
                + " WHERE skill_id=99 AND name='\u5f71\u4e4b\u9632\u8b77'"
                + " AND itemids IS NULL AND counts IS NULL";
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            assertEquals(1, row.getInt(1),
                    "missing or duplicate empty Shadow Armor item marker");
        }
    }

    /** -1 means missing row (loader's original-mode fallback), otherwise eventstart. */
    private static int coexistEventState(AccountRepository repository,
                                         String database) throws Exception {
        String sql = "SELECT eventstart FROM " + database + ".`server_event`"
                + " WHERE eventclass='SkillCoexistSet'";
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            if (!row.next()) return -1;
            int eventState = row.getInt(1);
            assertTrue(eventState == 0 || eventState == 1,
                    "invalid SkillCoexistSet eventstart");
            assertTrue(!row.next(), "duplicate SkillCoexistSet rows");
            return eventState;
        }
    }

    private static void assertRetainedOriginalReplacementSource() throws Exception {
        Path retained = Path.of("..", "1-\u4f3a\u670d\u5668").toAbsolutePath().normalize();
        String skillUse = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "model", "skill", "L1SkillUse.java")),
                StandardCharsets.UTF_8);
        assertTrue(skillUse.contains(
                "{ SHIELD, SHADOW_ARMOR, EARTH_SKIN, EARTH_BLESS, IRON_SKIN }"));
        assertTrue(skillUse.contains(
                "if (!SkillCoexistEventTable.get().isCoexistEnabled())"));

        String coexist = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "datatables",
                "SkillCoexistEventTable.java")), StandardCharsets.UTF_8);
        assertTrue(coexist.contains("boolean enabled = false"),
                "missing-row fallback must retain original replacement mode");
        assertTrue(coexist.contains("enabled = rs.getInt(\"eventstart\") == 1"));
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
