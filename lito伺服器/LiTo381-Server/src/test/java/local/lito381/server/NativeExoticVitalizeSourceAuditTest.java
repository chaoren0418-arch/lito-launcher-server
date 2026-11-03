package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/** Cross-source proof for native Elf skill169 Exotic Vitalize. */
final class NativeExoticVitalizeSourceAuditTest {
    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path PRIMARY_SQL = RETAINED.resolve("db/380_db.sql");
    private static final Path SECONDARY_SQL = RETAINED.resolve(
            "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql");
    private static final Path CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path UNPACKED = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    private static final Path ASM = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test void databasesPinExactReagentTimingAndNoAuxiliaryEffectRows()
            throws Exception {
        String primary = read(PRIMARY_SQL);
        String secondary = read(SECONDARY_SQL);
        assertTrue(primary.contains("INSERT INTO `skills` VALUES (169, 0,"
                + " '體能激發', 22, 0, 30, 0, 40319, 1, 100, 960, 'none',"
                + " 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 1, '', 19, 4400,"
                + " 0, 0, 0, 0);"));
        assertTrue(secondary.contains("INSERT INTO `skills` VALUES (169,"
                + " '體能激發', 22, 0, 30, 0, 40319, 1, 0, 960, 'none',"
                + " 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 1, '', 19, 4400,"
                + " 0, 0, 0, 0);"));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (169, '體能激發', NULL, NULL);"));
            assertEquals(0, occurrences(sql,
                    "INSERT INTO `skills_buff_ability` VALUES (169,"));
        }
    }

    @Test void retainedEnginePinsEarthAcquisitionAndWeightRegenBypassOnly()
            throws Exception {
        String list = read(RETAINED.resolve("src/com/lineage/list/PcLvSkillList.java"));
        assertTrue(list.contains("if ((elfAttr & 1) != 0)"));
        assertTrue(list.contains("skillList.add(EXOTIC_VITALIZE - 1)"));
        String pc = read(RETAINED.resolve(
                "src/com/lineage/server/model/Instance/L1PcInstance.java"));
        assertTrue(pc.contains("hasSkillEffect(EXOTIC_VITALIZE)"
                + " || hasSkillEffect(ADDITIONAL_FIRE)"));
        assertTrue(pc.contains("return _hpRegenType >= regenMax();"));
        assertTrue(pc.contains("return this._mpRegenType >= 64;"));
        String buffs = read(RETAINED.resolve(
                "src/com/lineage/server/datatables/user/CharBuffTable.java"));
        assertFalse(buffs.contains("EXOTIC_VITALIZE"),
                "skill169 is a transient lease, not a reconnect buff");
    }

    @Test void selectedLiToPinsCase168SelectorOnlyAndHundredMillisecondClock()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(CLIENT));
        String producer = windowAfter(ASM, "loc_4CBBD4:", 24);
        assertTrue(producer.contains("cases 1,2,12,13,30,31,48,51,53,71,77,81,96-98,100,101,103-105,108-110,113,114,116,118,119,128,129,133,136,137,145,146,149,153-155,158,160,161,163,168,170"));
        assertTrue(producer.contains("and     ecx, 7"));
        assertTrue(producer.contains("sar     esi, 3"));
        assertTrue(producer.contains("push    14h"));
        assertEquals(100, readPeWordAtVa(UNPACKED,
                0x635774L + (long) (BattleSpellCatalog.EXOTIC_VITALIZE - 1) * 2L));
    }

    @Test void implementationHasExactTwoPhaseRouteAndTransientConsumers()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertEquals(2, occurrences(session,
                "BattleSpellCatalog.exoticVitalizeSpell(selector.skillId())"));
        assertTrue(session.contains("private void useExoticVitalizeSpell("));
        String method = session.substring(
                session.indexOf("private void useExoticVitalizeSpell("),
                session.indexOf("private void useWeightSpell("));
        assertTrue(method.contains("originPhase != Phase.BATTLE"
                + " && originPhase != Phase.WORLD"));
        assertTrue(method.contains("weightStatus.activate(spell.skillId()"));
        assertTrue(method.contains("spell.effectiveMpCost(originalReduction)"));
        assertTrue(method.contains("S_MESSAGE_CODE Exotic-Vitalize-missing-reagent=299"));
        assertTrue(method.contains("persisted=false"));
        assertTrue(session.contains(".regenerationBypassActive()"));
        assertTrue(session.contains("weightStatus.cancelMagicEffects()"));
        assertEquals(0, occurrences(method, "saveTimed"));
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int at = 0; (at = text.indexOf(needle, at)) >= 0;
             at += needle.length()) count++;
        return count;
    }

    private static int readPeWordAtVa(Path path, long virtualAddress)
            throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        ByteBuffer image = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int pe = image.getInt(0x3C);
        int sectionCount = Short.toUnsignedInt(image.getShort(pe + 6));
        int optionalSize = Short.toUnsignedInt(image.getShort(pe + 20));
        int optional = pe + 24;
        long imageBase = Integer.toUnsignedLong(image.getInt(optional + 28));
        long rva = virtualAddress - imageBase;
        int sections = optional + optionalSize;
        for (int index = 0; index < sectionCount; index++) {
            int section = sections + index * 40;
            long virtualSize = Integer.toUnsignedLong(image.getInt(section + 8));
            long sectionRva = Integer.toUnsignedLong(image.getInt(section + 12));
            long rawSize = Integer.toUnsignedLong(image.getInt(section + 16));
            long rawOffset = Integer.toUnsignedLong(image.getInt(section + 20));
            if (rva >= sectionRva
                    && rva + 2 <= sectionRva + Math.max(virtualSize, rawSize)) {
                return Short.toUnsignedInt(image.getShort(Math.toIntExact(
                        rawOffset + rva - sectionRva)));
            }
        }
        throw new IllegalArgumentException("unmapped PE virtual address");
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.isRegularFile(path), "missing source evidence: " + path);
        return Files.readString(path, StandardCharsets.UTF_8);
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
