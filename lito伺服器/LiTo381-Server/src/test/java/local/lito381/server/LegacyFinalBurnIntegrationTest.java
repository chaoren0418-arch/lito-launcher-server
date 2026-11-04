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

/** Read-only proof for the original skill 108 current-MP sacrifice contract. */
final class LegacyFinalBurnIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/src/com/lineage/server/model/L1MagicNpc.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/L1MagicPc.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"),
                Path.of("../1-伺服器/其他src參考用/古老src/com/lineage/server/model/L1MagicPc.java"),
                Path.of("../1-伺服器/其他src參考用/古老src/com/lineage/server/model/skill/L1SkillUse.java"));
    }

    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void everyRetainedSourceHasTheExactFinalBurnContract() throws Exception {
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
                        repository, database, BattleSpellCatalog.FINAL_BURN);
                assertEquals(1, auxiliary.getOrDefault("skills_item", 0));
                auxiliary.forEach((table, count) -> {
                    if (!table.equals("skills_item")) {
                        assertEquals(0, count,
                                "unexpected skill108 auxiliary row in " + table);
                    }
                });
                verified++;
                System.out.printf("[LiTo-test] source=%s final-burn-row=exact"
                                + " schema=%s source=current-mp post=hp1/mp0%n",
                        source, database);
            }
            assertEquals(config.classicServerNames().size(), verified);
        }
        assertRetainedCurrentMpDamageAndPostConsumeOrder();
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
                + " WHERE skill_id=108 AND name='會心一擊'"
                + " AND skill_level=14 AND skill_number=3"
                + " AND mpConsume=1 AND hpConsume=1"
                + " AND itemConsumeId=0 AND itemConsumeCount=0"
                + " AND reuseDelay=0 AND buffDuration=0"
                + " AND target='attack' AND target_to=3"
                + " AND damage_value=0 AND damage_dice=0 AND damage_dice_count=0"
                + " AND probability_value=0 AND probability_dice=0"
                + " AND attr=0 AND type=64 AND lawful=0"
                + " AND ranged=1 AND area=0 AND through=0"
                + " AND id=8 AND nameid='$2515'"
                + " AND action_id=18 AND castgfx=2952 AND castgfx2=0"
                + " AND sysmsgID_happen=0 AND sysmsgID_stop=0"
                + " AND sysmsgID_fail=0";
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill108 Final Burn row");
            assertEquals(0, row.getInt("reuseDelay"));
            if (effectSkillIdColumn) assertEquals(0, row.getInt("effect_skill_id"));
            assertFalse(row.next(), "duplicate exact skill108 Final Burn row");
        }
    }

    private static void assertExactEmptyItemMarker(AccountRepository repository,
                                                   String database) throws Exception {
        assertCount(repository, "SELECT COUNT(*) FROM " + database
                + ".`skills_item` WHERE skill_id=108"
                + " AND name='會心一擊' AND itemids IS NULL AND counts IS NULL", 1);
    }

    private static void assertRetainedCurrentMpDamageAndPostConsumeOrder() throws Exception {
        Path retained = Path.of("..", "1-伺服器").toAbsolutePath().normalize();
        String magicPc = LocalResources.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "model", "L1MagicPc.java")),
                StandardCharsets.UTF_8);
        assertTrue(magicPc.contains("if (skillId == FINAL_BURN)"));
        assertTrue(magicPc.contains("dmg = _pc.getCurrentMp();"));
        assertTrue(magicPc.contains("dmg -= _targetPc.getDamageReductionByArmor();"));
        assertTrue(magicPc.contains("damage = calcMrDefense(damage);"));

        String magicNpc = LocalResources.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "model", "L1MagicNpc.java")),
                StandardCharsets.UTF_8);
        assertTrue(magicNpc.contains("dmg = this._npc.getCurrentMp();"));

        String skillUse = LocalResources.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "model", "skill",
                "L1SkillUse.java")), StandardCharsets.UTF_8);
        assertTrue(skillUse.contains("currentHp < this._hpConsume + 1"));
        assertTrue(skillUse.contains("this._player.setCurrentHp(1);"));
        assertTrue(skillUse.contains("this._player.setCurrentMp(0);"));
        int runSkill = skillUse.indexOf("runSkill();", skillUse.indexOf("case TYPE_NORMAL"));
        int useConsume = skillUse.indexOf("useConsume();", runSkill);
        assertTrue(runSkill >= 0 && useConsume > runSkill,
                "normal cast must resolve damage before exceptional resource consumption");

        Path ancient = retained.resolve(Path.of("其他src參考用", "古老src",
                "com", "lineage", "server", "model"));
        String ancientMagic = LocalResources.readString(ancient.resolve("L1MagicPc.java"),
                StandardCharsets.UTF_8);
        String ancientUse = LocalResources.readString(ancient.resolve(Path.of(
                "skill", "L1SkillUse.java")), StandardCharsets.UTF_8);
        assertTrue(ancientMagic.contains("dmg = _pc.getCurrentMp();"));
        assertTrue(ancientUse.contains("this._player.setCurrentHp(1);"));
        assertTrue(ancientUse.contains("this._player.setCurrentMp(0);"));
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
