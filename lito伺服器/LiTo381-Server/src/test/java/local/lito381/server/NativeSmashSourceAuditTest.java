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

/** Cross-source proof for selected Illusionist skill203 Smash Energy. */
final class NativeSmashSourceAuditTest {
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

    @Test void selectedRowsPinDirectNeutralAttackAndHistoricalBookLabels()
            throws Exception {
        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertEquals("INSERT INTO `skills` VALUES (203, 0, '粉碎能量', 26, 2,"
                + " 7, 0, 0, 0, 600, 0, 'attack', 3, 50, 10, 2, 0, 0, 0,"
                + " 64, 0, 1, 0, 0, 4, '$5937', 18, 6526, 0, 0, 0, 0);",
                skillRow(primary, 203));
        assertEquals("INSERT INTO `skills` VALUES (203, '粉碎能量', 26, 2, 7,"
                + " 0, 0, 0, 0, 0, 'attack', 3, 50, 10, 2, 0, 0, 0, 64,"
                + " 0, 1, 0, 0, 4, '$5937', 18, 6526, 0, 0, 0, 0);",
                skillRow(secondary, 203));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (203, '暴擊', NULL, NULL);"));
            assertTrue(sql.contains("49119, '記憶水晶(暴擊)',"
                    + " 'skill.Skill_Illusionist', '$5683'"));
            assertFalse(sql.contains("INSERT INTO `skills_負面boss免疫` VALUES"
                    + " (203,"));
            assertFalse(sql.contains("INSERT INTO `skills_相消免疫技能` VALUES"
                    + " (203,"));
            assertFalse(sql.contains("INSERT INTO `skills_buff_ability` VALUES"
                    + " (203,"));
        }
        assertTrue(primary.contains("49119, 73, 1500000, 0, -1,"
                + " '技能^幻術=>記憶水晶(粉碎能量)'"));
        assertTrue(primary.contains("INSERT INTO `skill_傷害天賦設置` VALUES"
                + " (1701, 203, '粉碎能量'"),
                "custom talent rows remain evidence, not base native metadata");
    }

    @Test void selectedLiToEmitsOnly141902AndHasZeroCastClock()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String asm = read(DISASSEMBLY).replace("\r\n", "\n");
        String use = asm.substring(asm.indexOf("sub_4CB5F0 proc near"),
                asm.indexOf("sub_4CB5F0 endp") + 16);
        String generic = use.substring(use.indexOf("def_4CB66E:"));
        assertTrue(use.contains("cmp     esi, 0AFh"));
        assertTrue(generic.contains("and     edx, 7"));
        assertTrue(generic.contains("sar     esi, 3"));
        assertTrue(generic.contains("push    edx\npush    esi\npush    14h\n"
                + "push    offset off_5028CC\ncall    sub_4C6AE0"));
        assertEquals(0, readPeWordAtVa(UNPACKED, 0x635908L));
        assertEquals("14 19 02", Protocol.hex(
                new byte[] {0x14, 0x19, 0x02}));
    }

    @Test void retainedRuntimePinsDiceFormulaCounterFirstAndNoLowCircleCritical()
            throws Exception {
        String magic = read(RETAINED.resolve(
                "src/com/lineage/server/model/L1MagicPc.java"));
        for (String fragment : new String[] {
                "final int dice = l1skills.getDamageDice();",
                "final int diceCount = l1skills.getDamageDiceCount();",
                "final int value = l1skills.getDamageValue();",
                "magicDamage += (_random.nextInt(dice) + 1);",
                "magicDamage += value;",
                "if (l1skills.getSkillLevel() <= 6)",
                "magicDamage += _pc.getOriginalMagicDamage();",
                "magicDamage += _pc.get_magic_modifier_dmg();"}) {
            assertTrue(magic.contains(fragment),
                    "missing retained damage evidence: " + fragment);
        }
        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        String exceptCounter = use.substring(use.indexOf("EXCEPT_COUNTER_MAGIC"),
                use.indexOf("};", use.indexOf("EXCEPT_COUNTER_MAGIC")));
        assertFalse(exceptCounter.matches("(?s).*\\b203\\b.*"));
        int attack = use.indexOf("case L1Skills.TYPE_ATTACK://");
        int counter = use.indexOf("if (isUseCounterMagic(cha))", attack);
        int damage = use.indexOf("_dmg = magic.calcMagicDamage(_skillId)", counter);
        int erase = use.indexOf("captureSkillEffectLease(ERASE_MAGIC)", damage);
        assertTrue(attack < counter && counter < damage && damage < erase);
    }

    @Test void implementationPinsBothPhasesSelectorRestorationAndGenericDamage()
            throws Exception {
        String catalog = read(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"));
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        for (String fragment : new String[] {
                "SMASH, new SmashSpell(",
                "new AttackSpell(SMASH, 25, 2, 18, 6_526",
                "7, 600, 1, 50, 2, 10",
                "ATTRIBUTE_UNSPECIFIED, \"smash-energy\""}) {
            assertTrue(catalog.contains(fragment),
                    "missing Smash catalog implementation: " + fragment);
        }
        for (String fragment : new String[] {
                "BattleSpellCatalog.smashSpell(",
                "useSelectorOnlyTargetedAttackSpell(",
                "resolveSelectorOnlyPrimary(",
                "useClassicWorldAttackSpell(request, spell);",
                "useAttackSpell(request, spell, -1,",
                "ordinaryMagicDamage(spell.skillId(), spell.attribute(), sourceDamage",
                "target.timedStatus.consumeCounterMagic(castNow)"}) {
            assertTrue(session.contains(fragment),
                    "missing Smash runtime implementation: " + fragment);
        }
    }

    private static String skillRow(String sql, int skillId) {
        return sql.lines().filter(line -> line.startsWith(
                "INSERT INTO `skills` VALUES (" + skillId + ","))
                .findFirst().orElseThrow();
    }

    private static int readPeWordAtVa(Path path, long va) throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        ByteBuffer image = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int pe = image.getInt(0x3C);
        int sections = Short.toUnsignedInt(image.getShort(pe + 6));
        int optional = pe + 24;
        int table = optional + Short.toUnsignedInt(image.getShort(pe + 20));
        long rva = va - Integer.toUnsignedLong(image.getInt(optional + 28));
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
