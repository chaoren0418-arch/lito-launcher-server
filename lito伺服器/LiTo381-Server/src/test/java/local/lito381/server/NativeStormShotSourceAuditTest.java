package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

/** Cross-source proof for native Elf skill166 Storm Shot. */
final class NativeStormShotSourceAuditTest {
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
    private static final Path UNPACKED_CLIENT = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    private static final Path DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test void bothDatabasesPinExactWindBuffAbilitiesAndCoexistence()
            throws Exception {
        String primary = read(PRIMARY_SQL);
        String secondary = read(SECONDARY_SQL);
        assertTrue(primary.contains("INSERT INTO `skills` VALUES (166, 0,"
                + " '暴風神射', 21, 5, 30, 0, 0, 0, 0, 960, 'none', 0,"
                + " 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 32, '', 19, 2248,"
                + " 0, 0, 724, 280);"));
        assertTrue(secondary.contains("INSERT INTO `skills` VALUES (166,"
                + " '暴風神射', 21, 5, 30, 0, 0, 0, 0, 960, 'none', 0,"
                + " 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 32, '', 19, 2248,"
                + " 0, 0, 724, 280);"));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_buff_ability` VALUES"
                    + " (166, '暴風神射', 'BOW_DMG', 5);"));
            assertTrue(sql.contains("INSERT INTO `skills_buff_ability` VALUES"
                    + " (166, '暴風神射', 'BOW_HIT', -1);"));
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (166, '暴風神射', NULL, NULL);"));
        }
        assertTrue(primary.contains("'SkillCoexistSet', 1, '0',"
                + " '0=原廠同群組覆蓋；1=新技能共存系統"));
    }

    @Test void retainedEnginePinsWindGatePersistenceAndSignedConsumers()
            throws Exception {
        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(use.contains("this._skillId == STORM_SHOT"));
        assertTrue(use.contains("if (!this.isAttrAgrees())"));
        assertTrue(use.contains("(magicattr != 0)"));
        assertTrue(use.contains("((_player.getElfAttr() & magicattr) == 0)"));
        assertTrue(use.contains("{ FIRE_WEAPON, WIND_SHOT, FIRE_BLESS,"
                + " STORM_EYE, BURNING_WEAPON, STORM_SHOT }"));
        String list = read(RETAINED.resolve(
                "src/com/lineage/list/PcLvSkillList.java"));
        assertTrue(list.contains("if ((elfAttr & 8) != 0)"));
        assertTrue(list.contains("skillList.add(STORM_SHOT - 1)"));
        String ability = read(RETAINED.resolve(
                "src/com/lineage/server/model/Instance/L1PcBuffAbilityCalc.java"));
        assertTrue(ability.contains(
                "AbilityType.BOW_HIT, v -> pc.addBowHitup(v * multiplier)"));
        assertTrue(ability.contains(
                "AbilityType.BOW_DMG, v -> pc.addBowDmgup(v * multiplier)"));
        String stop = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillStop.java"));
        assertTrue(stop.contains("case STORM_SHOT"));
        String buffs = read(RETAINED.resolve(
                "src/com/lineage/server/datatables/user/CharBuffTable.java"));
        assertTrue(buffs.contains("STORM_SHOT, // 暴風神射"));
    }

    @Test void selectedLiToPinsCase165ObjectOnlyAndZeroCastClock()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String disassembly = read(DISASSEMBLY);
        assertTrue(disassembly.contains("loc_4CB71D:"));
        assertTrue(disassembly.contains("cases 7,8,22,36,42,43,47,54,66,78,147,148,150,157,159,162,164,165,167,169"));
        assertTrue(disassembly.contains("call    sub_4C9880"));
        String producer = disassembly.substring(
                disassembly.indexOf("sub_4C9880 proc near"),
                disassembly.indexOf("sub_4C9880 endp") + 16);
        assertTrue(producer.contains("push    offset aCccd"));
        assertTrue(producer.contains("push    14h"));
        assertEquals(0, readPeWordAtVa(UNPACKED_CLIENT,
                0x635774L + (long) (BattleSpellCatalog.STORM_SHOT - 1) * 2L));
    }

    @Test void implementationRoutesBothPhasesLifecycleAndAllConsumers()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertEquals(2, occurrences(session,
                "BattleSpellCatalog.stormShotSpell(selector.skillId())"));
        assertTrue(session.contains("private void useStormShotSpell("));
        assertTrue(session.contains("Protocol.objectSpellRequest(payload)"));
        assertTrue(session.contains(
                "spell.attribute() != BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED"));
        assertTrue(session.contains("stormShotStatus.activate("));
        assertTrue(session.contains("stormShotStatus.cancel()"));
        assertTrue(session.contains("stormShotStatus.expire("));
        assertTrue(session.contains("saveTimedStormShotBuff("));
        assertEquals(1, occurrences(session, "stormShotStatus.physicalHit"));
        assertEquals(1, occurrences(session, "stormShotStatus.physicalDamage"));
        assertEquals(3, occurrences(session,
                "NativeWeaponBuffModifiers nativeWeaponBuffs ="));
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
