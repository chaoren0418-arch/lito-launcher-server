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

/** Read-only proof for retained skill 106 and the qualified 3.81 dodge formula. */
final class LegacyUncannyDodgeIntegrationTest {
    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void everyRetainedSourceHasTheExactUncannyDodgeContract() throws Exception {
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
                        repository, database, 106);
                assertEquals(1, auxiliary.getOrDefault("skills_item", 0));
                auxiliary.forEach((table, count) -> {
                    if (!table.equals("skills_item")) {
                        assertEquals(0, count,
                                "unexpected skill106 auxiliary row in " + table);
                    }
                });
                verified++;
                System.out.printf("[LiTo-test] source=%s uncanny-dodge-row=exact"
                                + " schema=%s auxiliary=skills_item-only%n",
                        source, database);
            }
            assertEquals(config.classicServerNames().size(), verified);
        }
        assertRetainedLeaseAndQualifiedHitFormula();
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
                + " WHERE skill_id=106 AND name='暗影閃避'"
                + " AND skill_level=14 AND skill_number=1"
                + " AND mpConsume=20 AND hpConsume=0"
                + " AND itemConsumeId=0 AND itemConsumeCount=0"
                + " AND reuseDelay=0 AND buffDuration=192"
                + " AND target='none' AND target_to=0"
                + " AND damage_value=0 AND damage_dice=0 AND damage_dice_count=0"
                + " AND probability_value=0 AND probability_dice=0"
                + " AND attr=0 AND type=2 AND lawful=0"
                + " AND ranged=0 AND area=0 AND through=0"
                + " AND id=2 AND nameid='$2513'"
                + " AND action_id=19 AND castgfx=11765 AND castgfx2=0"
                + " AND sysmsgID_happen=0 AND sysmsgID_stop=0"
                + " AND sysmsgID_fail=0";
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill106 Uncanny Dodge row");
            assertEquals(0, row.getInt("reuseDelay"));
            if (effectSkillIdColumn) assertEquals(0, row.getInt("effect_skill_id"));
            assertFalse(row.next(), "duplicate exact skill106 Uncanny Dodge row");
        }
    }

    private static void assertExactEmptyItemMarker(
            AccountRepository repository, String database) throws Exception {
        String sql = "SELECT COUNT(*) FROM " + database + ".`skills_item`"
                + " WHERE skill_id=106 AND name='暗影閃避'"
                + " AND itemids IS NULL AND counts IS NULL";
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            assertEquals(1, row.getInt(1));
        }
    }

    private static void assertRetainedLeaseAndQualifiedHitFormula() throws Exception {
        Path retained = Path.of("..", "1-伺服器").toAbsolutePath().normalize();
        Path secondary = retained.resolve(Path.of(
                "其他src參考用", "0507備份", "yiwei380", "src"));
        for (Path sourceRoot : new Path[] {retained.resolve("src"), secondary}) {
            String mode = Files.readString(sourceRoot.resolve(Path.of(
                    "com", "lineage", "server", "model", "skill", "skillmode",
                    "UNCANNY_DODGE.java")), StandardCharsets.UTF_8);
            for (String fragment : new String[] {
                    "if (!srcpc.hasSkillEffect(UNCANNY_DODGE))",
                    "srcpc.setSkillEffect(UNCANNY_DODGE, integer * 1000)",
                    "srcpc.add_dodge(5)", "cha.add_dodge(-5)"}) {
                assertTrue(mode.contains(fragment), "missing retained lease " + fragment);
            }
            String currentAttack = Files.readString(sourceRoot.resolve(Path.of(
                    "com", "lineage", "server", "model", "L1AttackMode.java")),
                    StandardCharsets.UTF_8);
            assertTrue(currentAttack.contains("double divisor = 8.0"));
            assertTrue(currentAttack.contains("attackerDice -= hitReduction"));
        }

        Path variants = retained.resolve("其他src參考用");
        for (String variant : new String[] {
                "群版src", "381n8src", "rk760src", "korsrc"}) {
            String attackMode = Files.readString(variants.resolve(Path.of(
                    variant, "com", "lineage", "server", "model",
                    "L1AttackMode.java")), StandardCharsets.UTF_8);
            assertTrue(attackMode.contains("attackerDice -= character.get_dodge()"),
                    "qualified direct-dodge formula changed in " + variant);
        }
        for (String variant : new String[] {"群版src", "381n8src"}) {
            Path model = variants.resolve(Path.of(
                    variant, "com", "lineage", "server", "model"));
            String pc = Files.readString(model.resolve("L1AttackPc.java"),
                    StandardCharsets.UTF_8);
            String npc = Files.readString(model.resolve("L1AttackNpc.java"),
                    StandardCharsets.UTF_8);
            assertTrue(pc.contains("attackerDice += attackerDice(_targetPc)"));
            assertTrue(npc.contains("attackerDice += attackerDice(_targetPc)"));
        }

        String buffs = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "datatables", "user",
                "CharBuffTable.java")), StandardCharsets.UTF_8);
        assertFalse(buffs.contains("UNCANNY_DODGE"));
        String icons = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "serverpackets",
                "S_Icons.java")), StandardCharsets.UTF_8);
        assertFalse(icons.contains("UNCANNY_DODGE"));
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
