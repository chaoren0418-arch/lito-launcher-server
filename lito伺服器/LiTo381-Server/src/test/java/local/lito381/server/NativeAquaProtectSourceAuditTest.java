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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Cross-source proof for native Elf skill160 Aqua Protecter. */
final class NativeAquaProtectSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeLitoBin();
        LocalResources.assumeIdaClient(LocalResources.IDA_DISASSEMBLY_ABSOLUTE);
        LocalResources.assumeExisting("LiTo.unpacked", LocalResources.LITO_UNPACKED);
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path PRIMARY_SQL = RETAINED.resolve("db/380_db.sql");
    private static final Path SECONDARY_SQL = RETAINED.resolve(
            "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql");
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path UNPACKED_CLIENT = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    private static final Path DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test void bothDatabasesPinExactPcBuffAndNoAbilityRows() throws Exception {
        String primary = read(PRIMARY_SQL);
        String secondary = read(SECONDARY_SQL);
        assertTrue(primary.contains("INSERT INTO `skills` VALUES (160, 0,"
                + " '水之防護', 20, 7, 30, 0, 0, 0, 0, 960, 'buff', 1,"
                + " 0, 0, 0, 0, 0, 4, 2, 0, -1, 0, 0, 128, '', 19, 5829,"
                + " 0, 0, 0, 0);"));
        assertTrue(secondary.contains("INSERT INTO `skills` VALUES (160,"
                + " '水之防護', 20, 7, 30, 0, 0, 0, 0, 960, 'buff', 1,"
                + " 0, 0, 0, 0, 0, 4, 2, 0, -1, 0, 0, 128, '', 19, 5829,"
                + " 0, 0, 0, 0);"));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (160, '水之防護', NULL, NULL);"));
            assertFalse(sql.contains("INSERT INTO `skills_buff_ability` VALUES"
                    + " (160,"));
        }
    }

    @Test void retainedEnginePinsWaterLearningPcTargetErAndTransientGap()
            throws Exception {
        String list = read(RETAINED.resolve(
                "src/com/lineage/list/PcLvSkillList.java"));
        assertTrue(list.contains("if ((elfAttr & 4) != 0)"));
        assertTrue(list.contains("skillList.add(AQUA_PROTECTER - 1)"));
        String attack = read(RETAINED.resolve(
                "src/com/lineage/server/model/L1AttackList.java"));
        assertTrue(attack.contains("SKU3.put(AQUA_PROTECTER, -5)"));
        String pc = read(RETAINED.resolve(
                "src/com/lineage/server/model/Instance/L1PcInstance.java"));
        String getEr = windowAfterText(pc, "public int getEr()", 2_500);
        assertTrue(getEr.contains("hasSkillEffect(DRESS_EVASION)"));
        assertTrue(getEr.contains("hasSkillEffect(SOLID_CARRIAGE)"));
        assertFalse(getEr.contains("AQUA_PROTECTER"),
                "retained snapshot omission must stay explicit");
        String skills = read(RETAINED.resolve(
                "src/com/lineage/server/templates/L1Skills.java"));
        assertTrue(skills.contains("TARGET_TO_PC = 1"));
        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(use.contains("_skill.getTargetTo() & L1Skills.TARGET_TO_PC"));
        assertTrue(use.contains("cha instanceof L1PcInstance"));
        String buffs = read(RETAINED.resolve(
                "src/com/lineage/server/datatables/user/CharBuffTable.java"));
        assertFalse(buffs.contains("AQUA_PROTECTER"));
    }

    @Test void selectedLiToPinsCase159ObjectOnlyAndZeroCastClock()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String dispatch = windowAfter(DISASSEMBLY, "loc_4CB71D:", 20);
        assertTrue(dispatch.contains("cases 7,8,22,36,42,43,47,54,66,78,147,148,150,157,159,162,164,165,167,169"));
        assertTrue(dispatch.contains("call    sub_4C9880"));
        String producer = windowAfter(DISASSEMBLY, "loc_4C99DB:", 20);
        assertTrue(producer.contains("and     ecx, 7"));
        assertTrue(producer.contains("sar     esi, 3"));
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("offset aCccd"));
        assertEquals(0, readPeWordAtVa(UNPACKED_CLIENT,
                0x635774L + (long) (BattleSpellCatalog.AQUA_PROTECTER - 1) * 2L));
    }

    @Test void implementationRoutesBothPhasesAndFeedsAllRangedErConsumers()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertEquals(2, occurrences(session,
                "BattleSpellCatalog.aquaProtectSpell(selector.skillId())"));
        assertTrue(session.contains("private void useAquaProtectSpell("));
        assertTrue(session.contains("Protocol.objectSpellRequest(payload)"));
        String aqua = windowAfterText(session,
                "private void useAquaProtectSpell(", 9_000);
        assertFalse(aqua.contains("sameNonemptyClan(target)"));
        assertTrue(session.contains("target.aquaProtectStatus.activate("));
        assertTrue(session.contains("aquaProtectStatus.evasionRate(nowNanos)"));
        // Seven consumers plus the method declaration. The retained NPC-arrow
        // rule is now also consumed by source-qualified WORLD companions.
        assertEquals(8, occurrences(session, "effectiveRangedEvasionRate("));
        String questArea = windowAfterText(session,
                "private void serverQuestMonsterAreaPhysicalHit(", 14_000);
        assertTrue(questArea.contains("effectiveRangedEvasionRate("));
        assertTrue(questArea.contains("BattleRangedEvasion.hitsAfterEvasion("));
        String dragonArea = windowAfterText(session,
                "private void serverTimeAttackMonsterAreaPhysicalHit(", 14_000);
        assertTrue(dragonArea.contains("effectiveRangedEvasionRate("));
        assertTrue(dragonArea.contains("BattleRangedEvasion.hitsAfterEvasion("));
        assertFalse(session.contains("S_PacketBox.EVASION_UPDATE"));
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int at = 0; (at = text.indexOf(needle, at)) >= 0;
             at += needle.length()) count++;
        return count;
    }

    private static String windowAfterText(String text, String marker, int chars) {
        int start = text.indexOf(marker);
        assertTrue(start >= 0, "missing text marker: " + marker);
        return text.substring(start, Math.min(text.length(), start + chars));
    }

    private static int readPeWordAtVa(Path path, long virtualAddress)
            throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        ByteBuffer image = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int pe = image.getInt(0x3C);
        assertEquals(0x0000_4550, image.getInt(pe), "invalid PE signature");
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
                int fileOffset = Math.toIntExact(rawOffset + rva - sectionRva);
                return Short.toUnsignedInt(image.getShort(fileOffset));
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
