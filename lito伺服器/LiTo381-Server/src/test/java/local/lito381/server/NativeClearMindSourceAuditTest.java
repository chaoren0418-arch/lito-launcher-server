package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Cross-source proof for reserved135/136 and native skill137 Clear Mind. */
final class NativeClearMindSourceAuditTest {
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
    private static final Path DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test void bothDatabasesPinReservedGapAndExactClearMindRows()
            throws Exception {
        String primary = read(PRIMARY_SQL);
        String secondary = read(SECONDARY_SQL);
        for (int skillId : new int[] {135, 136}) {
            assertTrue(primary.contains("INSERT INTO `skills` VALUES (" + skillId
                    + ", 0, 'none', 0, 0, 0, 0, 0, 0, 0, 0, 'none', 0, 0,"
                    + " 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 128, '', 19, 0, 0, 0, 0, 0);"));
            assertTrue(secondary.contains("INSERT INTO `skills` VALUES (" + skillId
                    + ", 'none', 0, 0, 0, 0, 0, 0, 0, 0, 'none', 0, 0, 0,"
                    + " 0, 0, 0, 0, 0, 0, 0, 0, 0, 128, '', 19, 0, 0, 0, 0, 0);"));
        }
        assertTrue(primary.contains("INSERT INTO `skills` VALUES (137, 0,"
                + " '淨化精神', 18, 0, 10, 0, 40319, 1, 0, 1200, 'none', 0,"
                + " 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 1, '', 19, 2180, 0, 0, 722, 0);"));
        assertTrue(secondary.contains("INSERT INTO `skills` VALUES (137,"
                + " '淨化精神', 18, 0, 10, 0, 40319, 1, 0, 1200, 'none', 0,"
                + " 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 1, '', 19, 2180, 0, 0, 722, 0);"));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_buff_ability` VALUES"
                    + " (137, '淨化精神', 'WIS', 3);"));
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (137, '淨化精神', NULL, NULL);"));
        }
    }

    @Test void retainedExecutorPinsWisdomRefreshAndBaseMrRecalculation()
            throws Exception {
        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(use.contains("this._skillId == CLEAR_MIND"));
        assertTrue(use.contains("pc.addWis((byte) 3)"));
        assertTrue(use.contains("pc.updateBuffStatus()"));
        assertTrue(use.contains("pc.resetBaseMr()"));

        String stop = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillStop.java"));
        assertTrue(stop.contains("case CLEAR_MIND"));
        assertTrue(stop.contains("cha.addWis((byte) -3)"));
        assertTrue(stop.contains("pc.updateBuffStatus()"));
        assertTrue(stop.contains("pc.resetBaseMr()"));
    }

    @Test void selectedLiToPinsNoProducerGapAndCase136SelectorProducer()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String disassembly = read(DISASSEMBLY);
        assertTrue(disassembly.contains("jumptable 004CB66E default case, cases"
                + " 23,80,82-85,87,88,91,93-95,111,120-127,134,135,138-143,171"));
        assertTrue(disassembly.contains("cases 1,2,12,13,30,31,48,51,53,71,77,81,"
                + "96-98,100,101,103-105,108-110,113,114,116,118,119,128,129,"
                + "133,136,137,145,146,149,153-155,158,160,161,163,168,170"));
        assertTrue(disassembly.contains("loc_4CBBD4:"));
        assertTrue(disassembly.contains("push    14h"));
        assertTrue(disassembly.contains("push    offset off_5028CC"));
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.isRegularFile(path), "missing source evidence: " + path);
        return Files.readString(path, StandardCharsets.UTF_8);
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
