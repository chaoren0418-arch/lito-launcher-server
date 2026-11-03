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

/** Read-only proof for retained skill 102, its proc and optional talent rows. */
final class LegacyBurningSpiritIntegrationTest {
    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void everyRetainedSourceHasTheExactBurningSpiritContract() throws Exception {
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
                int reuse = assertExactSkillRow(repository, database);
                assertExactEmptyItemMarker(repository, database);
                assertExactOptionalTalentRows(repository, database);
                verified++;
                System.out.printf(
                        "[LiTo-test] source=%s burning-spirit-skill-row=exact"
                                + " schema=%s reuse=%d talent=2504/levels1..9%n",
                        source, database, reuse);
            }
            assertEquals(config.classicServerNames().size(), verified);
        }
        assertRetainedProcAndConfiguration();
    }

    private static int assertExactSkillRow(AccountRepository repository,
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
        int expectedReuse = effectSkillIdColumn ? 2_990 : 3_000;
        String sql = "SELECT reuseDelay"
                + (effectSkillIdColumn ? ",effect_skill_id" : "")
                + " FROM " + database + ".`skills`"
                + " WHERE skill_id=102 AND name='燃燒鬥志'"
                + " AND skill_level=13 AND skill_number=5"
                + " AND mpConsume=20 AND hpConsume=0"
                + " AND itemConsumeId=0 AND itemConsumeCount=0"
                + " AND reuseDelay=" + expectedReuse + " AND buffDuration=320"
                + " AND target='none' AND target_to=0"
                + " AND damage_value=0 AND damage_dice=0 AND damage_dice_count=0"
                + " AND probability_value=0 AND probability_dice=0"
                + " AND attr=0 AND type=2 AND lawful=0"
                + " AND ranged=0 AND area=0 AND through=0"
                + " AND id=32 AND nameid='$2509'"
                + " AND action_id=19 AND castgfx=2946 AND castgfx2=0"
                + " AND sysmsgID_happen=0 AND sysmsgID_stop=0"
                + " AND sysmsgID_fail=0";
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill 102 source row");
            assertEquals(expectedReuse, row.getInt("reuseDelay"));
            if (effectSkillIdColumn) assertEquals(0, row.getInt("effect_skill_id"));
            assertFalse(row.next(), "duplicate exact skill 102 source row");
        }
        return expectedReuse;
    }

    private static void assertExactEmptyItemMarker(
            AccountRepository repository, String database) throws Exception {
        String sql = "SELECT COUNT(*) FROM " + database + ".`skills_item`"
                + " WHERE skill_id=102 AND name='燃燒鬥志'"
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
        String sql = "SELECT COUNT(*),MIN(talent_level),MAX(talent_level),"
                + "SUM(talent_point),SUM(add_hp),SUM(add_mp+add_sp+add_str+add_dex"
                + "+add_con+add_int+add_wis+add_cha+add_mr+add_ac+add_dmg"
                + "+add_bow_dmg+add_hit+add_bow_hit+add_hpr+add_mpr)"
                + " FROM " + database + ".`skill_狀態天賦設置`"
                + " WHERE talent_id=2504 AND skill_id=102"
                + " AND note='燃燒鬥志' AND class_limit=16"
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

    private static void assertRetainedProcAndConfiguration() throws Exception {
        Path retained = Path.of("..", "1-伺服器").toAbsolutePath().normalize();
        String attack = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "model", "L1AttackPc.java")),
                StandardCharsets.UTF_8);
        for (String fragment : new String[] {
                "if (_weaponType == 20)", "if (_weaponType == 62)",
                "if (_weaponType2 == 17)",
                "final int random = ThreadLocalRandom.current().nextInt(100) + 1;",
                "if (_pc.hasSkillEffect(BURNING_SPIRIT))",
                "if (random <= ConfigSkill.BURNING_CHANCE)",
                "_damageMultiplier = Math.max(_damageMultiplier, ConfigSkill.BURNING_DMG)",
                "dmg *= _damageMultiplier;"}) {
            assertTrue(attack.contains(fragment), "missing retained proc " + fragment);
        }

        String config = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "config", "ConfigSkill.java")),
                StandardCharsets.UTF_8);
        assertTrue(config.contains(
                "BURNING_CHANCE = _getInt(p, \"BURNING_CHANCE\", 33)"));
        assertTrue(config.contains(
                "BURNING_DMG = _getDouble(p, \"BURNING_DMG\", 1.5)"));

        String buffs = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "datatables", "user",
                "CharBuffTable.java")), StandardCharsets.UTF_8);
        assertFalse(buffs.contains("BURNING_SPIRIT"),
                "retained CharBuffTable unexpectedly persists skill 102");
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
