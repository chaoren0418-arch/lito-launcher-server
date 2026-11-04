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
import org.junit.jupiter.api.BeforeAll;

/** Read-only proof for retained skill 104 and the shared poison admission gate. */
final class LegacyVenomResistIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
    }

    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void everyRetainedSourceHasTheExactVenomResistContract() throws Exception {
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
                Map<String, Integer> auxiliary = skillAuxiliaryRows(
                        repository, database, 104);
                assertEquals(1, auxiliary.getOrDefault("skills_item", 0));
                auxiliary.forEach((table, count) -> {
                    if (!table.equals("skills_item")) assertEquals(0, count,
                            "unexpected skill104 auxiliary row in " + table);
                });
                verified++;
                System.out.printf("[LiTo-test] source=%s venom-resist-skill-row=exact"
                                + " schema=%s talent=2505/levels1..9%n",
                        source, database);
            }
            assertEquals(config.classicServerNames().size(), verified);
        }
        assertRetainedPoisonGateAndLifecycle();
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
                + " WHERE skill_id=104 AND name='\u6bd2\u6027\u62b5\u6297'"
                + " AND skill_level=13 AND skill_number=7"
                + " AND mpConsume=20 AND hpConsume=0"
                + " AND itemConsumeId=0 AND itemConsumeCount=0"
                + " AND reuseDelay=0 AND buffDuration=320"
                + " AND target='none' AND target_to=0"
                + " AND damage_value=0 AND damage_dice=0 AND damage_dice_count=0"
                + " AND probability_value=0 AND probability_dice=0"
                + " AND attr=0 AND type=2 AND lawful=0"
                + " AND ranged=0 AND area=0 AND through=0"
                + " AND id=128 AND nameid='$2511'"
                + " AND action_id=19 AND castgfx=2948 AND castgfx2=0"
                + " AND sysmsgID_happen=0 AND sysmsgID_stop=831"
                + " AND sysmsgID_fail=0";
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill104 Venom Resist row");
            assertEquals(0, row.getInt("reuseDelay"));
            if (effectSkillIdColumn) assertEquals(0, row.getInt("effect_skill_id"));
            assertFalse(row.next(), "duplicate exact skill104 Venom Resist row");
        }
    }

    private static void assertExactEmptyItemMarker(
            AccountRepository repository, String database) throws Exception {
        String sql = "SELECT COUNT(*) FROM " + database + ".`skills_item`"
                + " WHERE skill_id=104 AND name='\u6bd2\u6027\u62b5\u6297'"
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
                + " WHERE talent_id=2505 AND skill_id=104"
                + " AND note='\u6bd2\u6027\u62b5\u6297' AND class_limit=16"
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

    private static void assertRetainedPoisonGateAndLifecycle() throws Exception {
        Path retained = Path.of("..", "1-伺服器").toAbsolutePath().normalize();
        String poison = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "model", "poison",
                "L1Poison.java")), StandardCharsets.UTF_8);
        for (String fragment : new String[] {
                "get_venom_resist() > 0",
                "hasSkillEffect(L1SkillId.VENOM_RESIST)",
                "getPoisonResist() > 0"}) {
            assertTrue(poison.contains(fragment), "missing retained gate " + fragment);
        }
        for (String file : new String[] {
                "L1DamagePoison.java", "L1SilencePoison.java",
                "L1ParalysisPoison.java"}) {
            String installer = Files.readString(retained.resolve(Path.of(
                    "src", "com", "lineage", "server", "model", "poison", file)),
                    StandardCharsets.UTF_8);
            assertTrue(installer.contains("isValidTarget"),
                    "poison family bypasses common gate: " + file);
        }
        String use = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "model", "skill",
                "L1SkillUse.java")), StandardCharsets.UTF_8);
        assertTrue(use.contains("cha.setSkillEffect(getEffectSkillId(), _getBuffDuration)"));
        assertFalse(use.contains("case VENOM_RESIST"),
                "retained skill104 unexpectedly has a cure side effect");
        String stop = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "model", "skill",
                "L1SkillStop.java")), StandardCharsets.UTF_8);
        assertTrue(stop.contains("getSysmsgIdStop()"));
        assertTrue(stop.contains("new S_ServerMessage(msgID)"));
        String icons = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "serverpackets",
                "S_Icons.java")), StandardCharsets.UTF_8);
        assertTrue(icons.contains("data[7] = (byte) (pc.getSkillEffectTimeSec(VENOM_RESIST) / 4)"));
        String buffs = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "datatables", "user",
                "CharBuffTable.java")), StandardCharsets.UTF_8);
        assertFalse(buffs.contains("VENOM_RESIST"));
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
