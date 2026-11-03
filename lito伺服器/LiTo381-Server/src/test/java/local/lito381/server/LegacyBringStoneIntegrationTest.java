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

/** Read-only proof for retained skill 100 and the five dark-stone tiers. */
final class LegacyBringStoneIntegrationTest {
    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void everyRetainedSourceHasTheExactBringStoneContract() throws Exception {
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
                assertExactStoneRows(repository, database);
                Map<String, Integer> auxiliaryRows = skillAuxiliaryRows(
                        repository, database, BattleSpellCatalog.BRING_STONE);
                assertEquals(1, auxiliaryRows.getOrDefault("skills_item", 0));
                auxiliaryRows.forEach((table, count) -> {
                    if (!table.equals("skills_item")) {
                        assertEquals(0, count,
                                "unexpected skill 100 auxiliary row in " + table);
                    }
                });
                assertExactEmptyItemMarker(repository, database);
                verified++;
                System.out.printf(
                        "[LiTo-test] source=%s bring-stone-skill-row=exact"
                                + " schema=%s stones=40320..40324 auxiliary=%s%n",
                        source, database, auxiliaryRows);
            }
            assertEquals(config.classicServerNames().size(), verified,
                    "not every retained source has the exact skill 100 contract");
        }
        assertRetainedProbabilityAndMutationSource();
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
                + " WHERE skill_id=100 AND name='\u63d0\u7149\u9b54\u77f3'"
                + " AND skill_level=13 AND skill_number=3"
                + " AND mpConsume=5 AND hpConsume=0"
                + " AND itemConsumeId=0 AND itemConsumeCount=0"
                + " AND reuseDelay=0 AND buffDuration=0"
                + " AND target='none' AND target_to=0"
                + " AND damage_value=0 AND damage_dice=0 AND damage_dice_count=0"
                + " AND probability_value=0 AND probability_dice=0"
                + " AND attr=0 AND type=2 AND lawful=0"
                + " AND ranged=0 AND area=0 AND through=0"
                + " AND id=8 AND nameid='$2507'"
                + " AND action_id=19 AND castgfx=2944 AND castgfx2=0"
                + " AND sysmsgID_happen=0 AND sysmsgID_stop=0"
                + " AND sysmsgID_fail=0";
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill 100 Bring Stone source row");
            assertEquals(0, row.getInt("reuseDelay"));
            if (effectSkillIdColumn) assertEquals(0, row.getInt("effect_skill_id"));
            assertTrue(!row.next(), "duplicate exact skill 100 Bring Stone source row");
        }
    }

    private static void assertExactStoneRows(AccountRepository repository,
                                             String database) throws Exception {
        int[] ids = {40320, 40321, 40322, 40323, 40324};
        String[] names = {
                "\u4e00\u7d1a\u9ed1\u9b54\u77f3", "\u4e8c\u7d1a\u9ed1\u9b54\u77f3",
                "\u4e09\u7d1a\u9ed1\u9b54\u77f3", "\u56db\u7d1a\u9ed1\u9b54\u77f3",
                "\u4e94\u7d1a\u9ed1\u9b54\u77f3"
        };
        int[] weights = {300, 600, 900, 1200, 1500};
        int[] icons = {1061, 1054, 1055, 1056, 1057};
        int[] ground = {2897, 2898, 2899, 2900, 2901};
        int[] descriptions = {836, 837, 838, 839, 840};
        for (int index = 0; index < ids.length; index++) {
            String sql = "SELECT COUNT(*) FROM " + database + ".`etcitem`"
                    + " WHERE item_id=? AND name=? AND classname='0'"
                    + " AND name_id=? AND item_type='material' AND use_type='none'"
                    + " AND material='mineral' AND weight=? AND invgfx=?"
                    + " AND grdgfx=? AND itemdesc_id=? AND stackable=1"
                    + " AND max_charge_count=0 AND min_lvl=0 AND max_lvl=0"
                    + " AND bless=0 AND trade=0 AND cant_delete=0"
                    + " AND delay_id=0 AND delay_time=0 AND delay_effect=0"
                    + " AND food_volume=0 AND save_at_once=0 AND limit_class=127";
            try (Connection connection = connection(repository);
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, ids[index]);
                statement.setString(2, names[index]);
                statement.setString(3, "$" + (2474 + index));
                statement.setInt(4, weights[index]);
                statement.setInt(5, icons[index]);
                statement.setInt(6, ground[index]);
                statement.setInt(7, descriptions[index]);
                try (ResultSet row = statement.executeQuery()) {
                    assertTrue(row.next());
                    assertEquals(1, row.getInt(1),
                            "missing or duplicate exact dark-stone row " + ids[index]);
                }
            }
        }
    }

    private static void assertExactEmptyItemMarker(AccountRepository repository,
                                                   String database) throws Exception {
        String sql = "SELECT COUNT(*) FROM " + database + ".`skills_item`"
                + " WHERE skill_id=100 AND name='\u63d0\u7149\u9b54\u77f3'"
                + " AND itemids IS NULL AND counts IS NULL";
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            assertEquals(1, row.getInt(1),
                    "missing or duplicate empty Bring Stone item marker");
        }
    }

    private static void assertRetainedProbabilityAndMutationSource() throws Exception {
        Path source = Path.of("..", "1-\u4f3a\u670d\u5668", "src", "com", "lineage",
                "server", "model", "skill", "L1SkillUse.java")
                .toAbsolutePath().normalize();
        String text = Files.readString(source, StandardCharsets.UTF_8);
        for (String fragment : new String[] {
                "final int dark = (int) (10 + (pc.getLevel() * 0.8) + (pc",
                ".getWis() - 6) * 1.2);",
                "final int brave = (int) (dark / 2.1);",
                "final int wise = (int) (brave / 2.0);",
                "final int kayser = (int) (wise / 1.9);",
                "final int chance = random.nextInt(100) + 1;",
                "case 40320:", "case 40321:", "case 40322:", "case 40323:",
                "bringStone(pc, item, dark, chance, 40321, \"$2475\")",
                "pc.sendPackets(new S_ServerMessage(280))",
                "pc.sendPackets(new S_ServerMessage(403, targetNameId))"
        }) {
            assertTrue(text.contains(fragment), "missing retained source: " + fragment);
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
