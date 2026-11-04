package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

/** Cross-source proof for native Elf skill156 Storm Eye. */
final class NativeStormEyeSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/list/PcLvSkillList.java"),
                Path.of("../1-伺服器/src/com/lineage/server/datatables/user/CharBuffTable.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/IndependentSkillIcon.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillStop.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"),
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
    private static final Path PRIMARY_SQL = RETAINED.resolve("db/380_db.sql");
    private static final Path SECONDARY_SQL = RETAINED.resolve(
            "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql");
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path UNPACKED_CLIENT = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    private static final Path DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test void bothDatabasesPinExactPartyBuffAndTwoAbilityRows()
            throws Exception {
        String primary = read(PRIMARY_SQL);
        String secondary = read(SECONDARY_SQL);
        assertTrue(primary.contains("INSERT INTO `skills` VALUES (156, 0,"
                + " '暴風之眼', 20, 3, 40, 0, 0, 0, 0, 960, 'none', 8,"
                + " 0, 0, 0, 0, 0, 0, 2, 0, 0, -1, 0, 8, '', 19, 2288,"
                + " 0, 0, 724, 0);"));
        assertTrue(secondary.contains("INSERT INTO `skills` VALUES (156,"
                + " '暴風之眼', 20, 3, 40, 0, 0, 0, 0, 960, 'none', 8,"
                + " 0, 0, 0, 0, 0, 0, 2, 0, 0, -1, 0, 8, '', 19, 2288,"
                + " 0, 0, 724, 0);"));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_buff_ability` VALUES"
                    + " (156, '暴風之眼', 'BOW_DMG', 3);"));
            assertTrue(sql.contains("INSERT INTO `skills_buff_ability` VALUES"
                    + " (156, '暴風之眼', 'BOW_HIT', 2);"));
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (156, '暴風之眼', NULL, NULL);"));
        }
        assertTrue(primary.contains("'SkillCoexistSet', 1, '0',"
                + " '0=原廠同群組覆蓋；1=新技能共存系統"));
    }

    @Test void retainedEnginePinsPartyPersistenceWindMasteryAndCounterBypass()
            throws Exception {
        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(use.contains("(_skill.getTargetTo() & L1Skills.TARGET_TO_PARTY)"));
        assertTrue(use.contains("_player.getParty().isMember(xpc)"));
        assertTrue(use.contains("this._skillId == STORM_EYE"));
        assertTrue(use.contains("{ FIRE_WEAPON, WIND_SHOT, FIRE_BLESS,"));
        assertTrue(use.contains("149, 150, 151, 155, 156"));
        String list = read(RETAINED.resolve(
                "src/com/lineage/list/PcLvSkillList.java"));
        assertTrue(list.contains("if ((elfAttr & 8) != 0)"));
        assertTrue(list.contains("skillList.add(STORM_EYE - 1)"));
        String stop = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillStop.java"));
        assertTrue(stop.contains("case STORM_EYE"));
        String buffs = read(RETAINED.resolve(
                "src/com/lineage/server/datatables/user/CharBuffTable.java"));
        assertTrue(buffs.contains("STORM_EYE, // 暴風之眼"));
        String icon = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/IndependentSkillIcon.java"));
        assertTrue(icon.contains("new S_PacketBoxIconAura(155, timeSec)"));
    }

    @Test void selectedLiToPinsCase155SelectorOnlyAndZeroCastClock()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String selector = windowAfter(DISASSEMBLY, "loc_4CBBD4:", 24);
        assertTrue(selector.contains("cases 1,2,12,13,30,31,48,51,53,71,77,81,96-98,100,101,103-105,108-110,113,114,116,118,119,128,129,133,136,137,145,146,149,153-155,158,160,161,163,168,170"));
        assertTrue(selector.contains("and     ecx, 7"));
        assertTrue(selector.contains("sar     esi, 3"));
        assertTrue(selector.contains("push    14h"));
        assertTrue(selector.contains("call    sub_4C6AE0"));
        assertEquals(0, readPeWordAtVa(UNPACKED_CLIENT,
                0x635774L + (long) (BattleSpellCatalog.STORM_EYE - 1) * 2L));
    }

    @Test void implementationRoutesBothPhasesAndFeedsAllPhysicalConsumers()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertEquals(2, occurrences(session,
                "BattleSpellCatalog.stormEyeSpell(selector.skillId())"));
        assertTrue(session.contains("private void useStormEyeSpell("));
        assertTrue(session.contains("Protocol.selectorOnlySpellRequest(payload)"));
        assertTrue(session.contains("sameClassicParty(candidate)"));
        assertTrue(session.contains("target.stormEyeStatus.activate("));
        assertTrue(session.contains("state.saveTimedStormEyeBuff("));
        assertEquals(3, occurrences(session,
                "NativeWeaponBuffModifiers nativeWeaponBuffs ="));
        assertEquals(1, occurrences(session, "stormEyeStatus.physicalHit"));
        assertEquals(1, occurrences(session, "stormEyeStatus.physicalDamage"));
        assertEquals(1, occurrences(session, "windShotStatus.physicalHit"));
        assertEquals(1, occurrences(session, "fireWeaponStatus.physicalDamage"));
        assertEquals(1, occurrences(session, "fireBlessStatus.physicalDamage"));
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int at = 0; (at = text.indexOf(needle, at)) >= 0;
             at += needle.length()) count++;
        return count;
    }

    private static int readPeWordAtVa(Path path, long virtualAddress)
            throws Exception {
        byte[] bytes = LocalResources.readAllBytes(path);
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
        assertTrue(Files.isRegularFile(LocalResources.assumeExternalFile(path)), "missing source evidence: " + path);
        return LocalResources.readString(path, StandardCharsets.UTF_8);
    }

    private static String windowAfter(Path path, String marker, int followingLines)
            throws Exception {
        StringBuilder result = new StringBuilder();
        boolean found = false;
        int remaining = 0;
        try (BufferedReader reader = LocalResources.newBufferedReader(
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
        try (var stream = LocalResources.newInputStream(path)) {
            byte[] buffer = new byte[64 * 1024];
            for (int count; (count = stream.read(buffer)) >= 0;) {
                if (count > 0) digest.update(buffer, 0, count);
            }
        }
        return HexFormat.of().withUpperCase().formatHex(digest.digest());
    }
}
