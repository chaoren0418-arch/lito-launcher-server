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

/** Read-only proof for original skill 111 Dress Evasion and bow ER semantics. */
final class LegacyDressEvasionIntegrationTest {
    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void everyRetainedSourceHasTheExactDressEvasionContract() throws Exception {
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

                Map<String, Integer> auxiliary = skillAuxiliaryRows(
                        repository, database, BattleSpellCatalog.DRESS_EVASION);
                assertEquals(1, auxiliary.getOrDefault("skills_item", 0));
                auxiliary.forEach((table, count) -> {
                    if (!table.equals("skills_item")) {
                        assertEquals(0, count,
                                "unexpected skill111 auxiliary row in " + table);
                    }
                });
                verified++;
                System.out.printf("[LiTo-test] source=%s dress-evasion-row=exact"
                                + " schema=%s er=12 duration=32 auxiliary=%s%n",
                        source, database, auxiliary);
            }
            assertEquals(config.classicServerNames().size(), verified);
        }
        assertRetainedErAndBowSources();
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
                + " WHERE skill_id=111 AND name='閃避提升'"
                + " AND skill_level=14 AND skill_number=6"
                + " AND mpConsume=20 AND hpConsume=0"
                + " AND itemConsumeId=0 AND itemConsumeCount=0"
                + " AND reuseDelay=0 AND buffDuration=32"
                + " AND target='none' AND target_to=0"
                + " AND damage_value=0 AND damage_dice=0 AND damage_dice_count=0"
                + " AND probability_value=0 AND probability_dice=0"
                + " AND attr=0 AND type=2 AND lawful=0"
                + " AND ranged=0 AND area=0 AND through=0"
                + " AND id=64 AND nameid=''"
                + " AND action_id=19 AND castgfx=11766 AND castgfx2=0"
                + " AND sysmsgID_happen=0 AND sysmsgID_stop=0"
                + " AND sysmsgID_fail=0";
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill111 Dress Evasion row");
            assertEquals(0, row.getInt("reuseDelay"));
            if (effectSkillIdColumn) assertEquals(0, row.getInt("effect_skill_id"));
            assertFalse(row.next(), "duplicate exact skill111 Dress Evasion row");
        }
    }

    private static void assertExactEmptyItemMarker(AccountRepository repository,
                                                   String database) throws Exception {
        assertCount(repository, "SELECT COUNT(*) FROM " + database
                + ".`skills_item` WHERE skill_id=111"
                + " AND name='閃避提升'"
                + " AND itemids IS NULL AND counts IS NULL", 1);
    }

    private static void assertRetainedErAndBowSources() throws Exception {
        Path retained = Path.of("..", "1-伺服器").toAbsolutePath().normalize();
        String currentPc = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "model", "Instance",
                "L1PcInstance.java")), StandardCharsets.UTF_8);
        assertTrue(currentPc.contains("public int getEr()"));
        assertTrue(currentPc.contains("er += (this.getDex() - 8) >> 1"));
        assertTrue(currentPc.contains("this.hasSkillEffect(DRESS_EVASION)"));
        assertTrue(currentPc.contains("er += 12;"));

        String currentAttack = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "model", "L1AttackPc.java")),
                StandardCharsets.UTF_8);
        assertTrue(currentAttack.contains("if (_weaponType == 20)"));
        assertTrue(currentAttack.contains("return calcErEvasion()"));
        String currentMode = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "model", "L1AttackMode.java")),
                StandardCharsets.UTF_8);
        assertTrue(currentMode.contains("final int er = _targetPc.getEr()"));
        assertTrue(currentMode.contains("return er < rnd"));

        String currentIcon = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "serverpackets", "S_Icons.java")),
                StandardCharsets.UTF_8);
        assertTrue(currentIcon.contains("pc.hasSkillEffect(DRESS_EVASION)"));
        assertTrue(currentIcon.contains("data[17] = (byte)"
                + " (pc.getSkillEffectTimeSec(DRESS_EVASION) / 4)"));

        Path ancient = retained.resolve(Path.of("其他src參考用", "古老src"));
        String ancientPc = Files.readString(ancient.resolve(Path.of(
                "com", "lineage", "server", "model", "Instance", "L1PcInstance.java")),
                StandardCharsets.UTF_8);
        assertTrue(ancientPc.contains("this.hasSkillEffect(DRESS_EVASION)"));
        assertTrue(ancientPc.contains("er += 12;"));
        String ancientIcon = Files.readString(ancient.resolve(Path.of(
                "com", "lineage", "server", "serverpackets",
                "S_PacketBoxActiveSpells.java")), StandardCharsets.UTF_8);
        assertTrue(ancientIcon.contains("data[17] ="
                + " pc.getSkillEffectTimeSec(DRESS_EVASION) >> 2"));
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
