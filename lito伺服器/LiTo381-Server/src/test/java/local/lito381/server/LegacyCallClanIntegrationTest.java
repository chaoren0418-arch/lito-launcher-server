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
import java.util.Set;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Read-only proof for retained skill 116 Call Clan and its answer geometry. */
final class LegacyCallClanIntegrationTest {
    @BeforeAll
    static void requireMySqlIntegration() {
        Assumptions.assumeTrue(Boolean.getBoolean("lito.mysql.integration"));
    }

    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void everyRetainedSourceHasTheExactCallClanContract() throws Exception {
        String configPath = System.getProperty(
                "lito.legacy.config",
                "src/test/resources/server-config/config/server.source-router-legacy-probe.properties");
        ServerConfig config = ServerConfig.load(Path.of(configPath));
        try (AccountRepository routed = AccountRepository.open(config)) {
            routed.healthCheck();
            int verified = 0;
            for (String source : config.classicServerNames()) {
                AccountRepository repository = routed.repositoryForClassicSource(source);
                String database = stringField(repository, "resourceDatabase");
                assertTrue(database.matches("[A-Za-z0-9_]+"));
                boolean effectSkillId = hasColumn(
                        repository, database, "skills", "effect_skill_id");
                String sql = "SELECT skill_id"
                        + (effectSkillId ? ",effect_skill_id" : "")
                        + " FROM " + database + ".`skills`"
                        + " WHERE skill_id=116 AND name='呼喚盟友'"
                        + " AND skill_level=15 AND skill_number=3"
                        + " AND mpConsume=30 AND hpConsume=0"
                        + " AND itemConsumeId=0 AND itemConsumeCount=0"
                        + " AND reuseDelay=0 AND buffDuration=0"
                        + " AND target='buff' AND target_to=0"
                        + " AND damage_value=0 AND damage_dice=0"
                        + " AND damage_dice_count=0 AND probability_value=0"
                        + " AND probability_dice=0 AND attr=0 AND type=2"
                        + " AND lawful=0 AND ranged=0 AND area=0 AND through=0"
                        + " AND id=8 AND nameid=''"
                        + " AND action_id=19 AND castgfx=0 AND castgfx2=0"
                        + " AND sysmsgID_happen=0 AND sysmsgID_stop=0"
                        + " AND sysmsgID_fail=280";
                try (Connection connection = connection(repository);
                     PreparedStatement statement = connection.prepareStatement(sql);
                     ResultSet row = statement.executeQuery()) {
                    assertTrue(row.next(), "missing exact skill116 Call Clan row");
                    assertEquals(116, row.getInt("skill_id"));
                    if (effectSkillId) assertEquals(0, row.getInt("effect_skill_id"));
                    assertFalse(row.next(), "duplicate exact skill116 Call Clan row");
                }
                assertCount(repository, "SELECT COUNT(*) FROM " + database
                        + ".`skills_item` WHERE skill_id=116"
                        + " AND name='呼喚盟友' AND itemids IS NULL"
                        + " AND counts IS NULL", 1);
                Map<String, Integer> auxiliary = skillAuxiliaryRows(
                        repository, database, BattleSpellCatalog.CALL_CLAN);
                Set<String> allowed = Set.of("skills_item");
                auxiliary.forEach((table, count) -> {
                    if (count != 0) {
                        assertTrue(allowed.contains(table),
                                "unexpected skill116 auxiliary row in " + table);
                    }
                });
                assertEquals(1, auxiliary.getOrDefault("skills_item", 0));
                verified++;
                System.out.printf("[LiTo-test] source=%s Call-Clan-row=exact"
                                + " schema=%s selector=14/3 cccs MP30 action19"
                                + " question729 adjacent-heading auxiliary=%s%n",
                        source, database, auxiliary);
            }
            assertEquals(config.classicServerNames().size(), verified);
        }
        assertRetainedJavaSemantics();
    }

    private static void assertRetainedJavaSemantics() throws Exception {
        Path retained = Path.of("..", "1-伺服器").toAbsolutePath().normalize();
        String use = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "clientpackets",
                "C_UseSkill.java")), StandardCharsets.UTF_8);
        assertTrue(use.contains("case 116:// 呼喚盟友"));
        assertTrue(use.contains("charName = readS()"));
        assertTrue(use.contains("World.get().getPlayer(charName)"));
        assertTrue(use.contains("new S_ServerMessage(73, charName)"));
        assertTrue(use.contains("new S_ServerMessage(414)"));
        assertTrue(use.contains("pc.publishCallClanDestination(targetId)"));

        String mode = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "model", "skill",
                "skillmode", "CALL_CLAN.java")), StandardCharsets.UTF_8);
        assertTrue(mode.contains("tryPublishIncomingCallClanInvitation"));
        assertTrue(mode.contains("clanPc.setTempID(srcpc.getId())"));
        assertTrue(mode.contains("new S_Message_YN(729)"));

        String attr = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "clientpackets",
                "C_Attr.java")), StandardCharsets.UTF_8);
        assertTrue(attr.contains("case 729:"));
        assertTrue(attr.contains("consumeIncomingCallClanInvitation(callerId)"));
        assertTrue(attr.contains("HEADING_TABLE_X"));
        assertTrue(attr.contains("HEADING_TABLE_Y"));
        assertTrue(attr.contains("heading = (heading + 4) & 7"));
        assertTrue(attr.contains("new S_ServerMessage(627)"));
        assertTrue(attr.contains("new S_ServerMessage(629)"));
        assertTrue(attr.contains("L1Teleport.CALL_CLAN"));

        Path olderRoot = retained.resolve(Path.of(
                "其他src參考用", "381n8src", "com", "lineage", "server"));
        String olderMode = Files.readString(olderRoot.resolve(Path.of(
                "model", "skill", "skillmode", "CALL_CLAN.java")),
                StandardCharsets.UTF_8);
        assertTrue(olderMode.contains("clanPc.setTempID(pc.getId())"));
        assertTrue(olderMode.contains("C_Attr.callClan(clanPc)"));
        String olderAttr = Files.readString(olderRoot.resolve(Path.of(
                "clientpackets", "C_Attr.java")), StandardCharsets.UTF_8);
        assertTrue(olderAttr.contains("public static void callClan"));
        assertTrue(olderAttr.contains("getCallClanHeading()"));
        // The older branch corroborates the adjacent-heading geometry but
        // carries its historical modulo-4 arrival-heading bug.  The selected
        // retained source above fixes the eight-way opposite to & 7.
        assertTrue(olderAttr.contains("heading = (heading + 4) % 4"));
        assertTrue(olderAttr.contains("L1Teleport.CALL_CLAN"));
    }

    private static boolean hasColumn(AccountRepository repository, String database,
                                     String table, String column) throws Exception {
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND TABLE_NAME=?"
                             + " AND COLUMN_NAME=?")) {
            statement.setString(1, database);
            statement.setString(2, table);
            statement.setString(3, column);
            try (ResultSet row = statement.executeQuery()) {
                return row.next() && row.getInt(1) == 1;
            }
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
