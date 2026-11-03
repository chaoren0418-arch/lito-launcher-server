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

/** Read-only proof for original item-local skill 107, not the later player buff. */
final class LegacyShadowFangIntegrationTest {
    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void everyRetainedSourceHasTheExactShadowFangContract() throws Exception {
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
                assertExactCancellationImmunity(repository, database);

                Map<String, Integer> auxiliary = skillAuxiliaryRows(
                        repository, database, BattleSpellCatalog.SHADOW_FANG);
                assertEquals(1, auxiliary.getOrDefault("skills_buff_ability", 0));
                assertEquals(1, auxiliary.getOrDefault("skills_item", 0));
                assertEquals(1, auxiliary.getOrDefault(
                        "skills_\u76f8\u6d88\u514d\u75ab\u6280\u80fd", 0));
                auxiliary.forEach((table, count) -> {
                    if (!table.equals("skills_buff_ability")
                            && !table.equals("skills_item")
                            && !table.equals("skills_\u76f8\u6d88\u514d\u75ab\u6280\u80fd")) {
                        assertEquals(0, count,
                                "unexpected skill107 auxiliary row in " + table);
                    }
                });
                verified++;
                System.out.printf("[LiTo-test] source=%s shadow-fang-row=exact"
                                + " schema=%s item-local=true dmg=5 immunity=true%n",
                        source, database);
            }
            assertEquals(config.classicServerNames().size(), verified);
        }
        assertRetainedItemLocalSourceAndDamageFormula();
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
                + " WHERE skill_id=107 AND name='\u6697\u5f71\u4e4b\u7259'"
                + " AND skill_level=14 AND skill_number=2"
                + " AND mpConsume=30 AND hpConsume=0"
                + " AND itemConsumeId=40321 AND itemConsumeCount=1"
                + " AND reuseDelay=0 AND buffDuration=192"
                + " AND target='none' AND target_to=0"
                + " AND damage_value=0 AND damage_dice=0 AND damage_dice_count=0"
                + " AND probability_value=0 AND probability_dice=0"
                + " AND attr=0 AND type=2 AND lawful=0"
                + " AND ranged=0 AND area=0 AND through=0"
                + " AND id=4 AND nameid='$2514'"
                + " AND action_id=19 AND castgfx=2951 AND castgfx2=0"
                + " AND sysmsgID_happen=0 AND sysmsgID_stop=0"
                + " AND sysmsgID_fail=0";
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill107 Shadow Fang row");
            assertEquals(0, row.getInt("reuseDelay"));
            if (effectSkillIdColumn) assertEquals(0, row.getInt("effect_skill_id"));
            assertFalse(row.next(), "duplicate exact skill107 Shadow Fang row");
        }
    }

    private static void assertExactAbilityRow(AccountRepository repository,
                                              String database) throws Exception {
        assertCount(repository, "SELECT COUNT(*) FROM " + database
                + ".`skills_buff_ability` WHERE skill_id=107"
                + " AND name='\u6697\u5f71\u4e4b\u7259'"
                + " AND ability_type='DMG' AND ability_value=5", 1);
    }

    private static void assertExactEmptyItemMarker(AccountRepository repository,
                                                   String database) throws Exception {
        assertCount(repository, "SELECT COUNT(*) FROM " + database
                + ".`skills_item` WHERE skill_id=107"
                + " AND name='\u6697\u5f71\u4e4b\u7259'"
                + " AND itemids IS NULL AND counts IS NULL", 1);
    }

    private static void assertExactCancellationImmunity(
            AccountRepository repository, String database) throws Exception {
        assertCount(repository, "SELECT COUNT(*) FROM " + database
                + ".`skills_\u76f8\u6d88\u514d\u75ab\u6280\u80fd` WHERE skill_id=107"
                + " AND note='\u6697\u5f71\u4e4b\u7259'", 1);
    }

    private static void assertRetainedItemLocalSourceAndDamageFormula() throws Exception {
        Path retained = Path.of("..", "1-\u4f3a\u670d\u5668")
                .toAbsolutePath().normalize();
        String currentSkillUse = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "model", "skill",
                "L1SkillUse.java")), StandardCharsets.UTF_8);
        assertTrue(currentSkillUse.contains("case SHADOW_FANG:"));
        assertTrue(currentSkillUse.contains("_itemobjid = target_id"));

        Path variants = retained.resolve("\u5176\u4ed6src\u53c3\u8003\u7528");
        String mode = Files.readString(variants.resolve(Path.of(
                "\u7fa4\u7248src", "com", "lineage", "server", "model", "skill",
                "skillmode", "SHADOW_FANG.java")), StandardCharsets.UTF_8);
        for (String fragment : new String[] {
                "getuseitemobjid()", "getType2() == 1",
                "new S_ServerMessage(79)", "itemTimer.start(item, skillId, skillTime)"}) {
            assertTrue(mode.contains(fragment), "missing item-local mode " + fragment);
        }

        Path classic381 = variants.resolve("381n8src");
        String item = Files.readString(classic381.resolve(Path.of(
                "com", "lineage", "server", "model", "Instance",
                "L1ItemInstance.java")), StandardCharsets.UTF_8);
        for (String fragment : new String[] {
                "setSkillWeaponEnchant", "case SHADOW_FANG:",
                "setDmgByMagic(5)", "new S_ServerMessage(308, getLogName())"}) {
            assertTrue(item.contains(fragment), "missing item lease " + fragment);
        }
        String attack = Files.readString(classic381.resolve(Path.of(
                "com", "lineage", "server", "model", "L1AttackPc.java")),
                StandardCharsets.UTF_8);
        assertTrue(attack.contains("this._weapon.getDmgByMagic()"));
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
