package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Read-only proof for the retained skill 98 and reagent rows. */
final class LegacyEnchantVenomIntegrationTest {
    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void everyRetainedSourceHasTheExactEnchantVenomContract() throws Exception {
        String configPath = System.getProperty(
                "lito.legacy.config", "src/test/resources/server-config/config/server.source-router-legacy-probe.properties");
        ServerConfig config = ServerConfig.load(Path.of(configPath));
        try (AccountRepository routed = AccountRepository.open(config)) {
            routed.healthCheck();
            int verified = 0;
            for (String source : config.classicServerNames()) {
                AccountRepository repository = routed.repositoryForClassicSource(source);
                String database = stringField(repository, "resourceDatabase");
                assertTrue(database.matches("[A-Za-z0-9_]+"),
                        "invalid retained resource database");

                assertExactSkillRow(repository, database);
                assertExactReagentRow(repository, database);
                Map<String, Integer> auxiliaryRows = skillAuxiliaryRows(
                        repository, database, 98);
                assertEquals(1, auxiliaryRows.getOrDefault("skills_item", 0),
                        "skill 98 must retain its single skills_item marker");
                auxiliaryRows.forEach((table, count) -> {
                    if (!table.equals("skills_item")) {
                        assertEquals(0, count,
                                "unexpected skill 98 auxiliary row in " + table);
                    }
                });
                verified++;
                System.out.printf(
                        "[LiTo-test] source=%s enchant-venom-skill-row=exact"
                                + " schema=%s reagent=40321 auxiliary=%s%n",
                        source, database, auxiliaryRows);
            }
            assertEquals(config.classicServerNames().size(), verified,
                    "not every retained source has the exact skill 98 contract");
        }
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
                + " WHERE skill_id=98 AND name='\u9644\u52a0\u5287\u6bd2'"
                + " AND skill_level=13 AND skill_number=1"
                + " AND mpConsume=10 AND hpConsume=1"
                + " AND itemConsumeId=40321 AND itemConsumeCount=1"
                + " AND reuseDelay=0 AND buffDuration=320"
                + " AND target='none' AND target_to=0"
                + " AND damage_value=0 AND damage_dice=0 AND damage_dice_count=0"
                + " AND probability_value=0 AND probability_dice=0"
                + " AND attr=0 AND type=2 AND lawful=0"
                + " AND ranged=0 AND area=0 AND through=0"
                + " AND id=2 AND nameid='$2505'"
                + " AND action_id=19 AND castgfx=2942 AND castgfx2=0"
                + " AND sysmsgID_happen=814 AND sysmsgID_stop=827"
                + " AND sysmsgID_fail=0";
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill 98 Enchant Venom source row");
            assertEquals(0, row.getInt("reuseDelay"));
            if (effectSkillIdColumn) assertEquals(0, row.getInt("effect_skill_id"));
            assertTrue(!row.next(), "duplicate exact skill 98 Enchant Venom source row");
        }
    }

    private static void assertExactReagentRow(AccountRepository repository,
                                              String database) throws Exception {
        String sql = "SELECT COUNT(*) FROM " + database + ".`etcitem`"
                + " WHERE item_id=40321 AND name='\u4e8c\u7d1a\u9ed1\u9b54\u77f3'"
                + " AND classname='0' AND name_id='$2475'"
                + " AND item_type='material' AND use_type='none'"
                + " AND material='mineral' AND weight=600"
                + " AND invgfx=1054 AND grdgfx=2898 AND itemdesc_id=837"
                + " AND stackable=1 AND max_charge_count=0"
                + " AND min_lvl=0 AND max_lvl=0 AND bless=0"
                + " AND trade=0 AND cant_delete=0 AND delay_id=0"
                + " AND delay_time=0 AND delay_effect=0 AND food_volume=0"
                + " AND save_at_once=0 AND limit_class=127";
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            assertEquals(1, row.getInt(1),
                    "missing or duplicate exact 40321 second-level dark stone row");
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
