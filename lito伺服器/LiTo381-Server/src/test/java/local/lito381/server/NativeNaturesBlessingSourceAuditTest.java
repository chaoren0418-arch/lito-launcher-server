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

/** Cross-source proof for native Elf skill164 Nature's Blessing. */
final class NativeNaturesBlessingSourceAuditTest {
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

    @Test
    void retainedRowsPinPartyPetWaterHealAndPrimaryReuse() throws Exception {
        String primary = read(PRIMARY_SQL);
        String secondary = read(SECONDARY_SQL);
        assertTrue(primary.contains("INSERT INTO `skills` VALUES (164, 0,"
                + " '生命的祝福', 21, 3, 30, 0, 0, 0, 300, 0, 'none', 24,"
                + " 10, 12, 0, 0, 0, 4, 16, 0, 0, -1, 0, 8, '', 19, 2244,"
                + " 0, 0, 0, 0);"));
        assertTrue(secondary.contains("INSERT INTO `skills` VALUES (164,"
                + " '生命的祝福', 21, 3, 30, 0, 0, 0, 0, 0, 'none', 24,"
                + " 10, 12, 0, 0, 0, 4, 16, 0, 0, -1, 0, 8, '', 19, 2244,"
                + " 0, 0, 0, 0);"));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (164, '生命的祝福', NULL, NULL);"));
            assertFalse(tableBlock(sql, "skills_buff_ability")
                    .contains("VALUES (164,"));
        }
    }

    @Test
    void retainedExecutorPinsTargetSetDiceModifiersAndWaterLifeConsumption()
            throws Exception {
        String skills = read(RETAINED.resolve(
                "src/com/lineage/server/templates/L1Skills.java"));
        assertTrue(skills.contains("TARGET_TO_PARTY = 8"));
        assertTrue(skills.contains("TARGET_TO_PET = 16"));
        assertTrue(skills.contains("TYPE_HEAL = 16"));

        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(use.contains("if (!this.isAttrAgrees())"));
        assertTrue(use.contains("this._skill.getArea() == -1"));
        assertTrue(use.contains("World.get().getVisibleObjects(this._user)"));
        assertTrue(use.contains("_player.getParty().isMember(xpc)"));
        assertTrue(use.contains("_player.getId() == summon.getMaster().getId()"));
        assertTrue(use.contains("_player.getId() == pet.getMaster().getId()"));
        assertTrue(use.contains("magic.calcHealing(this._skillId)"));
        assertTrue(use.contains("cha.hasSkillEffect(WATER_LIFE)"));
        assertTrue(use.contains("this._dmg = (this._dmg << 1);"));
        assertTrue(use.contains("cha.hasSkillEffect(POLLUTE_WATER)"));
        assertTrue(use.contains("cha.removeSkillEffect(WATER_LIFE);"));

        String magic = read(RETAINED.resolve(
                "src/com/lineage/server/model/L1MagicPc.java"));
        assertTrue(magic.contains("final int diceCount = value + healDiceBonus;"));
        assertTrue(magic.contains("magicDamage += (_random.nextInt(dice) + 1);"));
        assertTrue(magic.contains("alignmentRevision += (getLawful() / 32768.0);"));
    }

    @Test
    void selectedLiToPinsSelectorOnlyCase163AndThreeHundredMillisecondClock()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String selector = windowAfter(DISASSEMBLY, "loc_4CBBD4:", 20);
        assertTrue(selector.contains("cases 1,2,12,13,30,31,48,51,53,71,77,81,96-98,100,101,103-105,108-110,113,114,116,118,119,128,129,133,136,137,145,146,149,153-155,158,160,161,163,168,170"));
        assertTrue(selector.contains("and     ecx, 7"));
        assertTrue(selector.contains("sar     esi, 3"));
        assertTrue(selector.contains("push    14h"));
        assertTrue(selector.contains("push    offset off_5028CC"));
        assertEquals(300, readPeWordAtVa(UNPACKED_CLIENT,
                0x635774L + (long) (BattleSpellCatalog.NATURES_BLESSING - 1) * 2L));
    }

    @Test
    void implementationRoutesBothPhasesAndExcludesRetainedCustomBonus()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertTrue(session.contains(
                "BattleSpellCatalog.naturesBlessingSpell(selector.skillId())"));
        assertTrue(session.contains(
                "useNaturesBlessingSpell(payload, worldNaturesBlessing)"));
        assertTrue(session.contains(
                "admitClassicWorldSpellFamily("));
        assertTrue(session.contains("originPhase != Phase.BATTLE"
                + " && originPhase != Phase.WORLD"));
        assertTrue(session.contains("sameClassicParty(candidate)"));
        assertTrue(session.contains("classicWorldCompanionSnapshots()"));
        assertTrue(session.contains("controlledCompanionSnapshots()"));
        assertTrue(session.contains("target.waterLifeStatus.active(castNow)"));
        assertTrue(session.contains("target.waterLifeStatus.cancel()"));
        assertTrue(session.contains("target.timedStatus.polluteWaterActive(castNow)"));
        assertTrue(session.contains("target.takeClassicWorldPlayerDamage("));
        assertTrue(session.contains("custom-extra-heal=excluded"));
        assertFalse(session.contains("NATURES_BLESSING_HEAL"));

        String custom = read(RETAINED.resolve(
                "config/其他控制端/4-職業_技能設定表.properties"));
        assertTrue(custom.contains("NATURES_BLESSING_HEAL = 100"));
        String defaults = read(RETAINED.resolve(
                "src/com/lineage/config/ConfigSkill.java"));
        assertTrue(defaults.contains("NATURES_BLESSING_HEAL = 0;"));
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

    private static String windowAfter(Path path, String marker, int followingLines)
            throws Exception {
        assertTrue(Files.isRegularFile(path), "missing source evidence: " + path);
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

    private static String tableBlock(String sql, String table) {
        String marker = "-- Table structure for " + table;
        int start = sql.indexOf(marker);
        assertTrue(start >= 0, "missing table block " + table);
        int end = sql.indexOf("-- Table structure for ", start + marker.length());
        return sql.substring(start, end < 0 ? sql.length() : end);
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.isRegularFile(path), "missing source evidence: " + path);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String sha256(Path path) throws Exception {
        assertTrue(Files.isRegularFile(path), "missing selected client: " + path);
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
