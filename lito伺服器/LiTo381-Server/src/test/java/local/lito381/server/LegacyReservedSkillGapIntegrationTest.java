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
import org.junit.jupiter.api.BeforeAll;

/** Read-only proof that native selector slots 119..128 are not real skills. */
final class LegacyReservedSkillGapIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeIdaClient(LocalResources.IDA_DISASSEMBLY);
        LocalResources.assumeExisting("TW13081901bin拆解 full-disassembly.asm",
                Path.of("..", "TW13081901bin拆解", "analysis-final", "full-disassembly.asm"));
    }

    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void retainedDatabasesAndNativeProducersPinTheSecondReservedGap()
            throws Exception {
        String configPath = System.getProperty("lito.legacy.config",
                "src/test/resources/server-config/config/server.source-router-legacy-probe.properties");
        ServerConfig config = ServerConfig.load(Path.of(configPath));
        try (AccountRepository routed = AccountRepository.open(config)) {
            routed.healthCheck();
            int verified = 0;
            for (String source : config.classicServerNames()) {
                AccountRepository repository =
                        routed.repositoryForClassicSource(source);
                String database = stringField(repository, "resourceDatabase");
                assertTrue(database.matches("[A-Za-z0-9_]+"));
                try (Connection connection = connection(repository)) {
                    for (int skillId = 119; skillId <= 128; skillId++) {
                        int expectedRows = skillId == 122 ? 0 : 1;
                        try (PreparedStatement statement = connection.prepareStatement(
                                "SELECT * FROM " + database
                                        + ".`skills` WHERE skill_id=?")) {
                            statement.setInt(1, skillId);
                            try (ResultSet row = statement.executeQuery()) {
                                assertEquals(expectedRows == 1, row.next(),
                                        "reserved skill row presence " + skillId);
                                if (expectedRows == 1) {
                                    assertEquals("none", row.getString("name"));
                                    assertEquals(skillId <= 120 ? 15 : 0,
                                            row.getInt("skill_level"));
                                    assertEquals(skillId == 119 ? 6
                                                    : skillId == 120 ? 7 : 0,
                                            row.getInt("skill_number"));
                                    assertEquals(skillId == 119 ? 1 : 0,
                                            row.getInt("mpConsume"));
                                    assertEquals(0, row.getInt("buffDuration"));
                                    assertEquals("none", row.getString("target"));
                                    assertEquals(19, row.getInt("action_id"));
                                    assertEquals(skillId == 120 ? 2280 : 0,
                                            row.getInt("castgfx"));
                                    assertEquals(expectedNativeId(skillId),
                                            row.getInt("id"));
                                    assertFalse(row.next(),
                                            "duplicate reserved skill row " + skillId);
                                }
                            }
                        }
                        try (PreparedStatement statement = connection.prepareStatement(
                                "SELECT COUNT(*) FROM " + database
                                        + ".`skills_item` WHERE skill_id=?"
                                        + " AND name='none' AND itemids IS NULL"
                                        + " AND counts IS NULL")) {
                            statement.setInt(1, skillId);
                            try (ResultSet row = statement.executeQuery()) {
                                assertTrue(row.next());
                                assertEquals(expectedRows, row.getInt(1));
                            }
                        }
                    }
                }
                verified++;
                System.out.printf("[LiTo-test] source=%s reserved119-128=exact"
                                + " schema=%s rows=9 missing=122"
                                + " requests=selector-only import=blocked%n",
                        source, database);
            }
            assertEquals(config.classicServerNames().size(), verified);
        }

        Path retained = Path.of("..", "1-伺服器")
                .toAbsolutePath().normalize();
        String skillIds = Files.readString(retained.resolve(Path.of(
                "src", "com", "lineage", "server", "model", "skill",
                "L1SkillId.java")), StandardCharsets.UTF_8);
        assertTrue(skillIds.contains("unknown = 119 - 120"));
        assertTrue(skillIds.contains("none = 121 - 128"));

        String selected = Files.readString(Path.of("..", "LiTo381-開發",
                "ida-client-oep-v2", "full-disassembly.asm")
                .toAbsolutePath().normalize(), StandardCharsets.UTF_8);
        assertTrue(selected.contains(
                "jumptable 004CB66E cases 1,2,12,13,30,31,48,51,53,71,77,81,96-98,100,101,103-105,108-110,113,114,116,118,119,128,129"));
        assertTrue(selected.contains(
                "jumptable 004CB10B default case, cases 23,53,80,82-95,100,105,108-111,117,120-127"));

        String official = Files.readString(Path.of("..",
                "TW13081901bin拆解", "analysis-final", "full-disassembly.asm")
                .toAbsolutePath().normalize(), StandardCharsets.UTF_8);
        assertTrue(official.contains(
                "cases 1,2,12,13,30,31,48,51,71,77,81,96-98,101,103,104,108-110,113,114,116,118,119,128,129"));
    }

    private static int expectedNativeId(int skillId) {
        if (skillId == 119) return 64;
        if (skillId == 120) return 128;
        if (skillId == 121) return 1;
        return 1 << (skillId - 122);
    }

    private static Connection connection(AccountRepository repository)
            throws Exception {
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
