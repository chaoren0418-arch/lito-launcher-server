package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Cross-source proof for native Elf skill167 Wind Shackle. */
final class NativeWindShackleSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/datatables/user/CharBuffTable.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/ActionIntervalCalculator.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/skillmode/WIND_SHACKLE.java"),
                Path.of("../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        LocalResources.assumeLitoBin(
                Path.of("D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin"));
        LocalResources.assumeIdaClient(
                Path.of("D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
        LocalResources.assumeFiles(
                Path.of("D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe"));
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path PRIMARY = RETAINED.resolve("db/380_db.sql");
    private static final Path SECONDARY = RETAINED.resolve(
            "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql");
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path UNPACKED = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    private static final Path DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test void databasesPinPrimaryAndSecondaryReusePlusControlRows()
            throws Exception {
        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertTrue(primary.contains("INSERT INTO `skills` VALUES (167, 0,"
                + " '風之枷鎖', 21, 6, 50, 0, 40319, 1, 100, 16, 'buff',"
                + " 3, 0, 0, 0, 35, 30, 8, 1, 0, -1, 0, 0, 64, '', 19,"
                + " 1799, 0, 1001, 0, 280);"));
        assertTrue(secondary.contains("INSERT INTO `skills` VALUES (167,"
                + " '風之枷鎖', 21, 6, 50, 0, 40319, 1, 0, 16, 'buff',"
                + " 3, 0, 0, 0, 35, 30, 8, 1, 0, -1, 0, 0, 64, '', 19,"
                + " 1799, 0, 1001, 0, 280);"));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_負面技能控制` VALUES"
                    + " (167, '風之枷鎖', 55, 45, 35, 0, 0, 0.3, 0, 0, 1,"
                    + " 0.05, 0, 0);"));
            assertTrue(sql.contains("INSERT INTO `skills_負面boss免疫` VALUES"
                    + " (167, '風之枷鎖', 0);"));
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (167, '風之枷鎖', NULL, NULL);"));
            assertFalse(sql.contains("INSERT INTO `skills_魔屏無效技能` VALUES"
                    + " (167, '風之枷鎖');"));
        }
    }

    @Test void selectedLiToPinsCase166CccdhhClickRangeAndSixHundredMsClock()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String asm = read(DISASSEMBLY).replace("\r\n", "\n");
        assertTrue(asm.contains("loc_4CB78B:"));
        assertTrue(asm.contains("jumptable 004CB66E cases 156,166"));
        assertTrue(asm.contains("mov     edx, 8\nmov     ecx, esi"));
        String producer = asm.substring(asm.indexOf("sub_4CA6E0 proc near"),
                asm.indexOf("sub_4CA6E0 endp") + 16);
        assertTrue(producer.contains("push    offset aCccdhh"));
        assertTrue(producer.contains("push    14h"));
        assertEquals(600, readPeWordAtVa(UNPACKED,
                0x635774L + (long) (BattleSpellCatalog.WIND_SHACKLE - 1) * 2L));
    }

    @Test void retainedEnginePinsNoRefreshAndAttackSpellOnlyMultiplier()
            throws Exception {
        String mode = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/skillmode/WIND_SHACKLE.java"));
        assertTrue(mode.contains("if (cha.hasSkillEffect(WIND_SHACKLE))"));
        assertTrue(mode.contains("cha.setSkillEffect(WIND_SHACKLE, integer * 1000)"));
        String calculator = read(RETAINED.resolve(
                "src/com/lineage/server/model/ActionIntervalCalculator.java"));
        assertTrue(calculator.contains("type == ActionType.ATTACK || type == ActionType.SPELL"));
        assertTrue(calculator.contains("interval = multiply(interval, 1.25)"));
        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(use.contains("(this._skillId == WIND_SHACKLE)) && (isErase == false)"));
        String buffs = read(RETAINED.resolve(
                "src/com/lineage/server/datatables/user/CharBuffTable.java"));
        assertFalse(buffs.contains("WIND_SHACKLE"));
    }

    @Test void implementationRoutesBothPhasesAllActorsLifecycleAndIntervals()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertEquals(2, occurrences(session,
                "BattleSpellCatalog.windShackleSpell(selector.skillId())"));
        assertTrue(session.contains("Protocol.attackSpellRequest(payload)"));
        assertTrue(session.contains("target.windShackleStatus.activate("));
        assertTrue(session.contains("windShackleQuestMonster("));
        assertTrue(session.contains("windShackleTimeAttackActor("));
        assertTrue(session.contains("windShackleClassicWorldMonster("));
        assertTrue(session.contains("windShackleStatus.cancel()"));
        assertTrue(session.contains("windShackleStatus.expire("));
        assertTrue(session.contains("admitWindShackleAction("));
        String action = read(Path.of(
                "src/main/java/local/lito381/server/BattleActionInterval.java"));
        assertTrue(action.contains("WIND_SHACKLE_MULTIPLIER = 1.25d"));
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int at = 0; (at = text.indexOf(needle, at)) >= 0;
             at += needle.length()) count++;
        return count;
    }

    private static int readPeWordAtVa(Path path, long va) throws Exception {
        byte[] bytes = LocalResources.readAllBytes(path);
        ByteBuffer image = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int pe = image.getInt(0x3C);
        int sections = Short.toUnsignedInt(image.getShort(pe + 6));
        int optionalSize = Short.toUnsignedInt(image.getShort(pe + 20));
        int optional = pe + 24;
        long imageBase = Integer.toUnsignedLong(image.getInt(optional + 28));
        long rva = va - imageBase;
        int table = optional + optionalSize;
        for (int i = 0; i < sections; i++) {
            int section = table + i * 40;
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
        throw new IllegalArgumentException("unmapped PE VA");
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.isRegularFile(LocalResources.assumeExternalFile(path)), "missing source: " + path);
        return LocalResources.readString(path, StandardCharsets.UTF_8);
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var stream = LocalResources.newInputStream(path)) {
            byte[] buffer = new byte[64 * 1024];
            for (int count; (count = stream.read(buffer)) >= 0;) {
                if (count > 0) digest.update(buffer, 0, count);
            }
        }
        return HexFormat.of().withUpperCase().formatHex(digest.digest());
    }
}
