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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Read-only proof for retained skill 105, its weapon gate and multiplier. */
final class LegacyDoubleBrakeIntegrationTest {
    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void everyRetainedSourceHasTheExactDoubleBrakeContract() throws Exception {
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
                assertExactEmptyItemMarker(repository, database);
                assertExactOptionalTalentRows(repository, database);
                assertExactBossImmunityMarker(repository, database);
                Map<String, Integer> auxiliary = skillAuxiliaryRows(
                        repository, database, 105);
                assertEquals(1, auxiliary.getOrDefault("skills_item", 0));
                assertEquals(1, auxiliary.getOrDefault(
                        "skills_\u8ca0\u9762boss\u514d\u75ab", 0));
                auxiliary.forEach((table, count) -> {
                    if (!table.equals("skills_item")
                            && !table.equals("skills_\u8ca0\u9762boss\u514d\u75ab")) {
                        assertEquals(0, count,
                            "unexpected skill105 auxiliary row in " + table);
                    }
                });
                verified++;
                System.out.printf("[LiTo-test] source=%s double-brake-skill-row=exact"
                                + " schema=%s talent=2506/levels1..9%n",
                        source, database);
            }
            assertEquals(config.classicServerNames().size(), verified);
        }
        assertRetainedWeaponGateAndLifecycle();
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
                + " WHERE skill_id=105 AND name='\u96d9\u91cd\u7834\u58de'"
                + " AND skill_level=14 AND skill_number=0"
                + " AND mpConsume=30 AND hpConsume=0"
                + " AND itemConsumeId=40321 AND itemConsumeCount=1"
                + " AND reuseDelay=0 AND buffDuration=192"
                + " AND target='none' AND target_to=0"
                + " AND damage_value=0 AND damage_dice=0 AND damage_dice_count=0"
                + " AND probability_value=0 AND probability_dice=0"
                + " AND attr=0 AND type=2 AND lawful=0"
                + " AND ranged=0 AND area=0 AND through=0"
                + " AND id=1 AND nameid='$2512'"
                + " AND action_id=19 AND castgfx=2949 AND castgfx2=0"
                + " AND sysmsgID_happen=823 AND sysmsgID_stop=829"
                + " AND sysmsgID_fail=0";
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill105 Double Brake row");
            assertEquals(0, row.getInt("reuseDelay"));
            if (effectSkillIdColumn) assertEquals(0, row.getInt("effect_skill_id"));
            assertFalse(row.next(), "duplicate exact skill105 Double Brake row");
        }
    }

    private static void assertExactEmptyItemMarker(
            AccountRepository repository, String database) throws Exception {
        String sql = "SELECT COUNT(*) FROM " + database + ".`skills_item`"
                + " WHERE skill_id=105 AND name='\u96d9\u91cd\u7834\u58de'"
                + " AND itemids IS NULL AND counts IS NULL";
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            assertEquals(1, row.getInt(1));
        }
    }

    private static void assertExactOptionalTalentRows(
            AccountRepository repository, String database) throws Exception {
        String table = "skill_\u72c0\u614b\u5929\u8ce6\u8a2d\u7f6e";
        String sql = "SELECT COUNT(*),MIN(talent_level),MAX(talent_level),"
                + "SUM(talent_point),SUM(add_hp),SUM(add_mp+add_sp+add_str+add_dex"
                + "+add_con+add_int+add_wis+add_cha+add_mr+add_ac+add_dmg"
                + "+add_bow_dmg+add_hit+add_bow_hit+add_hpr+add_mpr)"
                + " FROM " + database + ".`" + table + "`"
                + " WHERE talent_id=2506 AND skill_id=105"
                + " AND note='\u96d9\u91cd\u7834\u58de' AND class_limit=16"
                + " AND upgrade_item_id=0 AND upgrade_item_count=0"
                + " AND upgrade_char_level=0";
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            assertEquals(9, row.getInt(1));
            assertEquals(1, row.getInt(2));
            assertEquals(9, row.getInt(3));
            assertEquals(45, row.getInt(4));
            assertEquals(450, row.getInt(5));
            assertEquals(0, row.getInt(6));
        }
    }

    private static void assertExactBossImmunityMarker(
            AccountRepository repository, String database) throws Exception {
        String table = "skills_\u8ca0\u9762boss\u514d\u75ab";
        String sql = "SELECT COUNT(*) FROM " + database + ".`" + table + "`"
                + " WHERE skill_id=105 AND note='\u96d9\u5200\u7834\u58de'"
                + " AND modifier=0";
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            assertEquals(1, row.getInt(1));
        }
    }

    private static void assertRetainedWeaponGateAndLifecycle() throws Exception {
        Path retained = Path.of("..", "1-\u4f3a\u670d\u5668")
                .toAbsolutePath().normalize();
        String attack = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "model", "L1AttackPc.java")),
                StandardCharsets.UTF_8);
        for (String fragment : new String[] {
                "case 11:// \u92fc\u722a", "case 12:// \u96d9\u5200",
                "if (_pc.hasSkillEffect(DOUBLE_BRAKE))",
                "if ((ThreadLocalRandom.current().nextInt(100) + 1) <= ConfigSkill.DOUBLE_BREAK_CHANCE)",
                "_damageMultiplier = Math.max(_damageMultiplier, ConfigSkill.DOUBLE_BREAK_DMG)",
                "dmg *= _damageMultiplier;"}) {
            assertTrue(attack.contains(fragment), "missing retained proc " + fragment);
        }

        String skillConfig = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "config", "ConfigSkill.java")),
                StandardCharsets.UTF_8);
        assertTrue(skillConfig.contains(
                "DOUBLE_BREAK_CHANCE = _getInt(p, \"DOUBLE_BREAK_CHANCE\", 15)"));
        assertTrue(skillConfig.contains(
                "DOUBLE_BREAK_DMG = _getDouble(p, \"DOUBLE_BREAK_DMG\", 1.5)"));

        String use = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "model", "skill",
                "L1SkillUse.java")), StandardCharsets.UTF_8);
        assertTrue(use.contains("getSysmsgIdHappen()"));
        assertTrue(use.contains("new S_ServerMessage(msgID)"));
        String stop = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "model", "skill",
                "L1SkillStop.java")), StandardCharsets.UTF_8);
        assertTrue(stop.contains("getSysmsgIdStop()"));
        assertTrue(stop.contains("new S_ServerMessage(msgID)"));

        String buffs = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "datatables", "user",
                "CharBuffTable.java")), StandardCharsets.UTF_8);
        assertFalse(buffs.contains("DOUBLE_BRAKE"));
        String icons = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "serverpackets",
                "S_Icons.java")), StandardCharsets.UTF_8);
        assertFalse(icons.contains("DOUBLE_BRAKE"));
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
