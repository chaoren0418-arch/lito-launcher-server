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

/** Cross-source proof for native Dragon Knight skill188 Resist Fear. */
final class NativeResistFearSourceAuditTest {
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

    @Test void databasesPinExactMetadataProbabilityImmunityAndBook()
            throws Exception {
        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertEquals("INSERT INTO `skills` VALUES (188, 0, '恐懼無助', 24, 3,"
                + " 0, 30, 0, 0, 1000, 60, 'buff', 3, 0, 0, 0, 0, 35, 0,"
                + " 1, 0, 3, 0, 0, 8, '$5927', 19, 6586, 0, 0, 0, 280);",
                skillRow(primary, 188));
        assertEquals("INSERT INTO `skills` VALUES (188, '恐懼無助', 24, 3,"
                + " 0, 30, 0, 0, 0, 60, 'buff', 3, 0, 0, 0, 0, 35, 0, 1,"
                + " 0, 3, 0, 0, 8, '$5927', 19, 6586, 0, 0, 0, 280);",
                skillRow(secondary, 188));
        assertTrue(primary.contains("INSERT INTO `skills_負面技能控制` VALUES"
                + " (188, '恐懼無助', 45, 38, 30, 0, 0, 0.4, 0, 0, 1,"
                + " 0.05, 0, 0);"));
        assertTrue(primary.contains("INSERT INTO `skills_負面boss免疫` VALUES"
                + " (188, '恐懼無助', 0);"));
        assertTrue(primary.contains("INSERT INTO `skills_魔屏無效技能` VALUES"
                + " (188, '恐懼無助');"));
        assertTrue(primary.contains("INSERT INTO `skills_item` VALUES"
                + " (188, '恐懼無助', NULL, NULL);"));
        assertTrue(primary.contains("49109, '龍騎士書版(恐懼無助)'"));
        assertTrue(primary.contains("'$5707', 'spellbook', 'book', 'gemstone', 5000"));
    }

    @Test void selectedLiToUsesGenericSelectorOnlyProducerAnd1200Clock()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String asm = read(DISASSEMBLY).replace("\r\n", "\n");
        String use = asm.substring(asm.indexOf("sub_4CB5F0 proc near"),
                asm.indexOf("sub_4CB5F0 endp") + 16);
        assertTrue(use.contains("cmp     esi, 0AFh"));
        assertTrue(use.contains("ja      def_4CB66E"));
        String generic = use.substring(use.indexOf("def_4CB66E:"));
        assertTrue(generic.contains("and     edx, 7"));
        assertTrue(generic.contains("sar     esi, 3"));
        assertTrue(generic.contains("push    edx\npush    esi\npush    14h\n"
                + "push    offset off_5028CC\ncall    sub_4C6AE0"));
        assertEquals(1_200, readPeWordAtVa(UNPACKED,
                0x635774L + (long) (BattleSpellCatalog.RESIST_FEAR - 1) * 2L));

        String receiver = asm.substring(asm.indexOf("sub_4B07C0 proc near"),
                asm.indexOf("sub_4B07C0 endp") + 16);
        assertTrue(receiver.contains("cmp     eax, 28h"));
        assertEquals(0xFA, Protocol.S_LITO_CLAN);
    }

    @Test void retainedCorrectModeTargetsPcAndAddsExactlyFiveAttackDice()
            throws Exception {
        Path classic = RETAINED.resolve("其他src參考用/rk760src");
        String mode = read(classic.resolve(
                "com/lineage/server/model/skill/skillmode/RESIST_FEAR.java"));
        assertTrue(mode.contains("if ((!cha.hasSkillEffect(188))"));
        assertTrue(mode.contains("(cha instanceof L1PcInstance)"));
        assertTrue(mode.contains("pc.setSkillEffect(188, integer * 1000)"));
        assertTrue(mode.contains("pc.add_dodge_down(5)"));
        assertTrue(mode.contains("pc.add_dodge_down(-5)"));
        String attack = read(classic.resolve(
                "com/lineage/server/model/L1AttackMode.java"));
        assertTrue(attack.contains("attackerDice -= character.get_dodge()"));
        assertTrue(attack.contains("attackerDice += character.get_dodge_down()"));
        String character = read(classic.resolve(
                "com/lineage/server/model/L1Character.java"));
        assertTrue(character.contains("if (_dodge_down > 10)"));
        assertTrue(character.contains("else if (_dodge_down < 0)"));

        String customized = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/skillmode/RESIST_FEAR.java"));
        assertTrue(customized.contains("srcpc.add_dodge_down(5)"));
        assertFalse(mode.contains("srcpc.add_dodge_down(5)"));
        String buffs = read(RETAINED.resolve(
                "src/com/lineage/server/datatables/user/CharBuffTable.java"));
        String buffArray = buffs.substring(buffs.indexOf("_buffSkill = {"),
                buffs.indexOf("};", buffs.indexOf("_buffSkill = {")));
        assertTrue(buffArray.contains("RESIST_FEAR"));
    }

    @Test void implementationConnectsBothPhasesHitCoreAndLifecycle()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertEquals(2, occurrences(session,
                "BattleSpellCatalog.resistFearSpell(selector.skillId())"));
        for (String fragment : new String[] {
                "useResistFearSpell(payload, worldResistFear)",
                "useResistFearSpell(payload, resistFearSpell)",
                "resolveResistFearTarget(",
                "resistFearStatus.activate(",
                "resistFearStatus.cancel()",
                "resistFearStatus.expire(",
                "effectivePhysicalDodgeDown(",
                "saveTimedResistFearBuff(",
                "counter-bypass=",
                "npc-mechanical-dodge-down=0",
                "selected-client-icon=unavailable"}) {
            assertTrue(session.contains(fragment), "missing implementation: " + fragment);
        }
        String physical = read(Path.of(
                "src/main/java/local/lito381/server/BattlePhysicalAttack.java"));
        String npcPhysical = read(Path.of(
                "src/main/java/local/lito381/server/BattleNpcPhysicalAttack.java"));
        assertTrue(physical.contains("- targetDodgeModifier + targetDodgeDown"));
        assertTrue(npcPhysical.contains("- targetDodgeModifier + targetDodgeDown"));
    }

    private static String skillRow(String sql, int skillId) {
        String prefix = "INSERT INTO `skills` VALUES (" + skillId + ",";
        return sql.lines().filter(line -> line.startsWith(prefix))
                .findFirst().orElseThrow();
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
