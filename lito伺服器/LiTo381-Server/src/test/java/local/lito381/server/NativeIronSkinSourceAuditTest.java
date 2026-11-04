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

/** Cross-source proof for native Elf skill168 Iron Skin. */
final class NativeIronSkinSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeLitoBin();
        LocalResources.assumeIdaClient(LocalResources.IDA_DISASSEMBLY_ABSOLUTE);
        LocalResources.assumeExisting("LiTo.unpacked", LocalResources.LITO_UNPACKED);
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

    @Test void databasesPinExactRowsAbilityMaterialAndCoexistMode()
            throws Exception {
        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertTrue(primary.contains("INSERT INTO `skills` VALUES (168, 0,"
                + " '鋼鐵防護', 21, 7, 30, 0, 0, 0, 0, 960, 'none', 0,"
                + " 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 128, '', 19, 2252,"
                + " 0, 714, 725, 280);"));
        assertTrue(secondary.contains("INSERT INTO `skills` VALUES (168,"
                + " '鋼鐵防護', 21, 7, 30, 0, 0, 0, 0, 960, 'none', 0,"
                + " 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 128, '', 19, 2252,"
                + " 0, 714, 725, 280);"));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_buff_ability` VALUES"
                    + " (168, '鋼鐵防護', 'AC', -10);"));
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (168, '鋼鐵防護', NULL, NULL);"));
        }
        assertTrue(primary.contains("'SkillCoexistSet', 1, '0'"));
    }

    @Test void selectedLiToPinsCase167ObjectProducerAndZeroClock()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String asm = read(DISASSEMBLY).replace("\r\n", "\n");
        assertTrue(asm.contains("loc_4CB71D:"));
        assertTrue(asm.contains("jumptable 004CB66E cases"
                + " 7,8,22,36,42,43,47,54,66,78,147,148,150,157,159,162,164,165,167,169"));
        String producer = asm.substring(asm.indexOf("sub_4C9880 proc near"),
                asm.indexOf("sub_4C9880 endp") + 16);
        assertTrue(producer.contains("push    offset aCccd"));
        assertTrue(producer.contains("push    14h"));
        assertEquals(0, readPeWordAtVa(UNPACKED,
                0x635774L + (long) (BattleSpellCatalog.IRON_SKIN - 1) * 2L));
    }

    @Test void retainedEnginePinsCoexistAcIconStopAndPersistence()
            throws Exception {
        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(use.contains("{ SHIELD, SHADOW_ARMOR, EARTH_SKIN,"
                + " EARTH_BLESS, IRON_SKIN }"));
        assertTrue(use.contains("if (!SkillCoexistEventTable.get().isCoexistEnabled())"));
        assertTrue(use.contains("this._skillId == IRON_SKIN"));
        String icon = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/IndependentSkillIcon.java"));
        assertTrue(icon.contains("case IRON_SKIN:"));
        assertTrue(icon.contains("return 528;"));
        assertTrue(icon.contains("new S_SkillIconShield(10, timeSec)"));
        String stop = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillStop.java"));
        assertTrue(stop.contains("case IRON_SKIN:"));
        String buffs = read(RETAINED.resolve(
                "src/com/lineage/server/datatables/user/CharBuffTable.java"));
        assertTrue(buffs.contains("IRON_SKIN, // 鋼鐵防護"));
    }

    @Test void implementationRoutesBothPhasesPersistenceCancellationAndAc()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertEquals(2, occurrences(session,
                "BattleSpellCatalog.ironSkinSpell(selector.skillId())"));
        assertTrue(session.contains("Protocol.objectSpellRequest(payload)"));
        assertTrue(session.contains("ironSkinStatus.activate("));
        assertTrue(session.contains("ironSkinStatus.cancel()"));
        assertTrue(session.contains("ironSkinStatus.expire("));
        assertTrue(session.contains("state.saveTimedIronSkinBuff("));
        assertTrue(session.contains("+ ironSkinStatus.armorClass(nowNanos)"));
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int at = 0; (at = text.indexOf(needle, at)) >= 0;
             at += needle.length()) count++;
        return count;
    }

    private static int readPeWordAtVa(Path path, long va) throws Exception {
        byte[] bytes = Files.readAllBytes(path);
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
        assertTrue(Files.isRegularFile(path), "missing source: " + path);
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
