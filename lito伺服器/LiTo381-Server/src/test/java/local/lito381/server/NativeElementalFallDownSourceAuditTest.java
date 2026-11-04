package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Cross-source proof for native LiTo selector16/4 and retained skill133. */
final class NativeElementalFallDownSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeLitoBin();
        LocalResources.assumeIdaClient();
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path PRIMARY_SQL = RETAINED.resolve("db/380_db.sql");
    private static final Path SECONDARY_SQL = RETAINED.resolve(
            "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql");
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path SELECTED_DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test void bothResourceDumpsPinExactSkillProbabilityBossAndNoMaterial()
            throws Exception {
        String primary = read(PRIMARY_SQL);
        String secondary = read(SECONDARY_SQL);
        assertTrue(primary.contains("INSERT INTO `skills` VALUES (133, 0, '弱化屬性',"
                + " 17, 4, 10, 0, 0, 0, 100, 32, 'buff', 3, 0, 0, 0,"
                + " 50, 30, 0, 1, 0, -1, 0, 0, 16, '', 19, 4396, 0, 0, 0, 280);"));
        assertTrue(secondary.contains("INSERT INTO `skills` VALUES (133, '弱化屬性',"
                + " 17, 4, 10, 0, 0, 0, 0, 32, 'buff', 3, 0, 0, 0,"
                + " 50, 30, 0, 1, 0, -1, 0, 0, 16, '', 19, 4396, 0, 0, 0, 280);"));
        String control = "INSERT INTO `skills_負面技能控制` VALUES (133, '弱化屬性',"
                + " 55, 45, 35, 0, 0, 0.3, 0, 0, 1, 0.05, 0, 0);";
        String boss = "INSERT INTO `skills_負面boss免疫` VALUES (133, '弱化屬性', 0);";
        assertTrue(primary.contains(control));
        assertTrue(secondary.contains(control));
        assertTrue(primary.contains(boss));
        assertTrue(secondary.contains(boss));
        assertTrue(primary.contains(
                "INSERT INTO `skills_item` VALUES (133, '弱化屬性', NULL, NULL);"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills_item` VALUES (133, '弱化屬性', NULL, NULL);"));
        assertFalse(tableBlock(primary, "skills_魔屏無效技能")
                .contains("VALUES (133,"));
        assertFalse(tableBlock(secondary, "skills_魔屏無效技能")
                .contains("VALUES (133,"));
    }

    @Test void retainedModePinsLowestBitMinusFiftyRefreshAndStopRestoration()
            throws Exception {
        String request = read(RETAINED.resolve(
                "src/com/lineage/server/clientpackets/C_UseSkill.java"));
        assertTrue(request.contains("case 133:// 弱化屬性"));
        assertTrue(request.contains("targetId = readD();"));
        String mode = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/skillmode/ELEMENTAL_FALL_DOWN.java"));
        assertTrue(mode.contains(
                "if (!cha.hasSkillEffect(L1SkillId.ELEMENTAL_FALL_DOWN))"));
        assertTrue(mode.contains("if ((playerAttr & 1) != 0) effectiveAttr = 1;"));
        assertTrue(mode.contains("else if ((playerAttr & 8) != 0) effectiveAttr = 8;"));
        assertTrue(mode.contains("final int i = -50;"));
        assertTrue(mode.contains("mob.addEarth(i);"));
        assertTrue(mode.contains("pc.setAddAttrKind(1);"));
        assertTrue(mode.contains(
                "cha.setSkillEffect(L1SkillId.ELEMENTAL_FALL_DOWN, integer * 1000);"));
        assertTrue(mode.contains("final int i = 50;"));
        assertTrue(mode.contains("pc.sendPackets(new S_OwnCharAttrDef(pc));"));
        assertTrue(read(RETAINED.resolve(
                "src/com/lineage/server/model/L1AttackList.java"))
                .contains("NZONE.put(ELEMENTAL_FALL_DOWN, false)"));
        assertEquals(0xFA, Protocol.S_LITO_CLAN);
    }

    @Test void selectedLiToPinsCase132Range10AndCccdhhProducer()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String selector = windowAfter(SELECTED_DISASSEMBLY, "loc_4CB755:", 28);
        assertTrue(selector.contains(
                "cases 3,5,6,17,28,37,44,49,64,73,76,132,151,152,172,173"));
        assertTrue(selector.contains("mov     edx, 0Ah"));
        assertTrue(selector.contains("call    sub_4CA6E0"));
        String producer = windowAfter(SELECTED_DISASSEMBLY,
                "loc_4CA77C:", 42);
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("push    offset aCccdhh"));
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.isRegularFile(path), "missing source evidence: " + path);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String tableBlock(String sql, String table) {
        String marker = "-- Table structure for " + table;
        int start = sql.indexOf(marker);
        assertTrue(start >= 0, "missing table block " + table);
        int end = sql.indexOf("-- Table structure for ", start + marker.length());
        return sql.substring(start, end < 0 ? sql.length() : end);
    }

    private static String windowAfter(Path path, String marker, int followingLines)
            throws Exception {
        StringBuilder result = new StringBuilder();
        boolean found = false;
        int remaining = 0;
        try (BufferedReader reader = Files.newBufferedReader(
                path, StandardCharsets.UTF_8)) {
            for (String line; (line = reader.readLine()) != null;) {
                if (!found && line.contains(marker)) {
                    found = true;
                    remaining = followingLines;
                }
                if (found) {
                    result.append(line).append('\n');
                    if (remaining-- == 0) break;
                }
            }
        }
        assertTrue(found, "missing IDA marker: " + marker);
        return result.toString();
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var stream = Files.newInputStream(path)) {
            byte[] buffer = new byte[64 * 1024];
            for (int count; (count = stream.read(buffer)) >= 0;) {
                if (count > 0) digest.update(buffer, 0, count);
            }
        }
        return HexFormat.of().withUpperCase().formatHex(digest.digest());
    }
}
