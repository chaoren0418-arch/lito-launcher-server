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

/** Cross-source proof for native Elf skill172 Storm Walk. */
final class NativeStormWalkSourceAuditTest {
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

    @Test void databasesPinExactWindOtherZeroDurationAndZeroGfxRows()
            throws Exception {
        String primary = read(PRIMARY_SQL);
        String secondary = read(SECONDARY_SQL);
        assertTrue(primary.contains("INSERT INTO `skills` VALUES (172, 0,"
                + " '暴風疾走', 22, 3, 10, 0, 0, 0, 0, 0, 'none',"
                + " 0, 0, 0, 0, 0, 0, 8, 128, 0, 0, 0, 0, 8, '', 19,"
                + " 0, 0, 0, 0, 0);"));
        assertTrue(secondary.contains("INSERT INTO `skills` VALUES (172,"
                + " '暴風疾走', 22, 3, 10, 0, 0, 0, 0, 0, 'none',"
                + " 0, 0, 0, 0, 0, 0, 8, 128, 0, 0, 0, 0, 8, '', 19,"
                + " 0, 0, 0, 0, 0);"));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (172, '暴風疾走', NULL, NULL);"));
            assertEquals(0, occurrences(sql,
                    "INSERT INTO `skills_buff_ability` VALUES (172,"));
            assertEquals(0, occurrences(sql,
                    "INSERT INTO `skills_負面技能控制` VALUES (172,"));
        }
    }

    @Test void retainedPrimaryPinsNoAutoAcquisitionNoModeAndNoLeaseOrVisual()
            throws Exception {
        String list = read(RETAINED.resolve(
                "src/com/lineage/list/PcLvSkillList.java"));
        assertFalse(list.contains("STORM_WALK"),
                "selected retained progression does not auto-grant skill172");

        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertFalse(use.contains("case STORM_WALK"),
                "retained executor has no individual Storm Walk effect");
        assertTrue(use.contains("if (_skill.getBuffDuration() == 0)"));
        assertTrue(use.contains("if (castgfx == 0)"));
        assertTrue(use.contains("return; // 無需顯示的圖示"));
        assertTrue(use.contains("((_player.getElfAttr() & magicattr) == 0)"));
        assertTrue(use.contains("this._player.adjustCurrentMp(-((long) this._mpConsume))"));

        String request = read(RETAINED.resolve(
                "src/com/lineage/server/clientpackets/C_UseSkill.java"));
        assertOrdered(request, "case 172:// 暴風疾走", "targetId = readD();");

        String buffs = read(RETAINED.resolve(
                "src/com/lineage/server/datatables/user/CharBuffTable.java"));
        assertFalse(buffs.contains("STORM_WALK"));
    }

    @Test void selectedLiToPinsCase171SelectorOnlyAndZeroClock()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(CLIENT));
        String dispatcher = windowAfter(ASM, "cmp     esi, 0AFh", 12);
        assertTrue(dispatcher.contains("default case, cases 23,80,82-85,87,88,91,93-95,111,120-127,134,135,138-143,171"));
        String producer = windowAfter(ASM, "def_4CB66E:", 22);
        assertTrue(producer.contains("cases 23,80,82-85,87,88,91,93-95,111,120-127,134,135,138-143,171"));
        assertTrue(producer.contains("push    offset off_5028CC"));
        assertTrue(producer.contains("push    14h"));
        assertEquals(0, readPeWordAtVa(UNPACKED,
                0x635774L + (long) (BattleSpellCatalog.STORM_WALK - 1) * 2L));
    }

    @Test void implementationHasExactEconomyOnlyBattleAndWorldRoute()
            throws Exception {
        String catalog = read(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"));
        assertTrue(catalog.contains("STORM_WALK, new OtherNoEffectSpell("));
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertTrue(session.contains("worldOtherNoEffect"));
        String method = session.substring(
                session.indexOf("private void useOtherNoEffectSpell("),
                session.indexOf("private void useDodgeBuffSpell("));
        assertTrue(method.contains("Protocol.selectorOnlySpellRequest(payload)"));
        assertTrue(method.contains("originPhase != Phase.BATTLE"
                + " && originPhase != Phase.WORLD"));
        assertTrue(method.contains("learnedSkills.contains(spell.skillId())"));
        assertTrue(method.contains("playerMp -= mpCost"));
        assertTrue(method.contains("cancelMeditation("));
        assertTrue(method.contains("revealInvisibility("));
        assertTrue(method.contains("cancelAbsoluteBarrier("));
        assertTrue(method.contains("Protocol.manaPoint("));
        assertEquals(0, occurrences(method, "Protocol.objectAction("));
        assertEquals(0, occurrences(method, "Protocol.effectObject("));
        assertEquals(0, occurrences(method, "activate"));
        assertEquals(0, occurrences(method, "saveTimed"));
        assertEquals(0, occurrences(method, "commitSpellReady"));
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int at = 0; (at = text.indexOf(needle, at)) >= 0;
             at += needle.length()) count++;
        return count;
    }

    private static void assertOrdered(String text, String... fragments) {
        int previous = -1;
        for (String fragment : fragments) {
            int found = text.indexOf(fragment, previous + 1);
            assertTrue(found > previous,
                    "missing/out-of-order fragment: " + fragment);
            previous = found;
        }
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
