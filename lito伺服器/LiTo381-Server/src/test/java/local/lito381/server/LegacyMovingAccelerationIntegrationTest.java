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

/** Read-only proof for retained skill 101 and its mode-four movement family. */
final class LegacyMovingAccelerationIntegrationTest {
    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void everyRetainedSourceHasTheExactMovingAccelerationContract() throws Exception {
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
                Map<String, Integer> auxiliaryRows = skillAuxiliaryRows(
                        repository, database, BattleSpellCatalog.MOVING_ACCELERATION);
                assertEquals(1, auxiliaryRows.getOrDefault("skills_item", 0));
                auxiliaryRows.forEach((table, count) -> {
                    if (!table.equals("skills_item")) {
                        assertEquals(0, count,
                                "unexpected skill 101 auxiliary row in " + table);
                    }
                });
                assertExactEmptyItemMarker(repository, database);
                verified++;
                System.out.printf(
                        "[LiTo-test] source=%s moving-acceleration-skill-row=exact"
                                + " schema=%s mode=4 duration=960 auxiliary=%s%n",
                        source, database, auxiliaryRows);
            }
            assertEquals(config.classicServerNames().size(), verified);
        }
        assertRetainedNativeCompatibilityAndMovementFamily();
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
                + " WHERE skill_id=101 AND name='\u884c\u8d70\u52a0\u901f'"
                + " AND skill_level=13 AND skill_number=4"
                + " AND mpConsume=10 AND hpConsume=0"
                + " AND itemConsumeId=0 AND itemConsumeCount=0"
                + " AND reuseDelay=0 AND buffDuration=960"
                + " AND target='none' AND target_to=0"
                + " AND damage_value=0 AND damage_dice=0 AND damage_dice_count=0"
                + " AND probability_value=0 AND probability_dice=0"
                + " AND attr=0 AND type=2 AND lawful=0"
                + " AND ranged=0 AND area=0 AND through=0"
                + " AND id=16 AND nameid='$2508'"
                + " AND action_id=19 AND castgfx=2945 AND castgfx2=0"
                + " AND sysmsgID_happen=0 AND sysmsgID_stop=0"
                + " AND sysmsgID_fail=0";
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill 101 source row");
            assertEquals(0, row.getInt("reuseDelay"));
            if (effectSkillIdColumn) assertEquals(0, row.getInt("effect_skill_id"));
            assertTrue(!row.next(), "duplicate exact skill 101 source row");
        }
    }

    private static void assertExactEmptyItemMarker(AccountRepository repository,
                                                   String database) throws Exception {
        String sql = "SELECT COUNT(*) FROM " + database + ".`skills_item`"
                + " WHERE skill_id=101 AND name='\u884c\u8d70\u52a0\u901f'"
                + " AND itemids IS NULL AND counts IS NULL";
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            assertEquals(1, row.getInt(1));
        }
    }

    private static void assertRetainedNativeCompatibilityAndMovementFamily()
            throws Exception {
        Path retained = Path.of("..", "1-\u4f3a\u670d\u5668")
                .toAbsolutePath().normalize();
        String usePacket = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "clientpackets",
                "C_UseSkill.java")), StandardCharsets.UTF_8);
        for (String fragment : new String[] {
                "case 101:// \u884c\u8d70\u52a0\u901f101", "targetId = readD();",
                "} catch (final Exception e) {"}) {
            assertTrue(usePacket.contains(fragment), "missing compatibility parser " + fragment);
        }

        String skillUse = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "model", "skill",
                "L1SkillUse.java")), StandardCharsets.UTF_8);
        for (String fragment : new String[] {
                "{ HOLY_WALK, MOVING_ACCELERATION, WIND_WALK, STATUS_BRAVE,",
                "this._skillId == MOVING_ACCELERATION",
                "cha.setBraveSpeed(4);",
                "new S_SkillBrave(pc.getId(), 4, this._getBuffIconDuration)",
                "new S_SkillBrave(cha.getId(), 4, 0)"}) {
            assertTrue(skillUse.contains(fragment), "missing retained behavior " + fragment);
        }

        String stop = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "model", "skill",
                "L1SkillStop.java")), StandardCharsets.UTF_8);
        assertTrue(stop.contains("case MOVING_ACCELERATION:"));
        assertTrue(stop.contains("new S_SkillBrave(pc.getId(), 0, 0)"));

        String buffs = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "datatables", "user",
                "CharBuffTable.java")), StandardCharsets.UTF_8);
        assertTrue(buffs.contains("MOVING_ACCELERATION, // \u884c\u8d70\u52a0\u901f"));

        String interval = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "model",
                "ActionIntervalCalculator.java")), StandardCharsets.UTF_8);
        assertTrue(interval.contains("actor.hasSkillEffect(MOVING_ACCELERATION)"));
        assertTrue(interval.contains("interval = multiply(interval, 0.665);"));
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
