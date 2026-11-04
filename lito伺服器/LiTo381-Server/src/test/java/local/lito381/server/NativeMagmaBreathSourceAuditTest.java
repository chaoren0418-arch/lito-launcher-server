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

/** Cross-source proof for native Dragon Knight skill184 Magma Breath. */
final class NativeMagmaBreathSourceAuditTest {
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

    @Test void databasesPinFixedFireAreaHpCostAndNoReagent() throws Exception {
        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        String schema = primary.substring(primary.indexOf("CREATE TABLE `skills`"),
                primary.indexOf("-- Records of skills"));
        assertTrue(schema.indexOf("`mpConsume`") < schema.indexOf("`hpConsume`"));
        assertEquals("INSERT INTO `skills` VALUES (184, 0, '岩漿噴吐', 23, 7,"
                + " 0, 35, 0, 0, 1000, 0, 'attack', 3, 45, 0, 0, 0, 0, 2,"
                + " 64, 0, 10, 3, 0, 128, '$5923', 18, 6516, 0, 0, 0, 0);",
                skillRow(primary, 184));
        assertEquals("INSERT INTO `skills` VALUES (184, '岩漿噴吐', 23, 7,"
                + " 0, 35, 0, 0, 0, 0, 'attack', 3, 45, 0, 0, 0, 0, 2, 64,"
                + " 0, 10, 3, 0, 128, '$5923', 18, 6516, 0, 0, 0, 0);",
                skillRow(secondary, 184));
        assertTrue(primary.contains("INSERT INTO `skills_item` VALUES"
                + " (184, '岩漿噴吐', NULL, NULL);"));
        assertFalse(primary.contains(
                "INSERT INTO `skills_負面技能控制` VALUES (184,"));
        assertFalse(primary.contains(
                "INSERT INTO `skills_負面boss免疫` VALUES (184,"));
    }

    @Test void selectedLiToUsesPost175SelectorOnlyProducerAndZeroClock()
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
        assertEquals(0, readPeWordAtVa(UNPACKED,
                0x635774L + (long) (BattleSpellCatalog.MAGMA_BREATH - 1) * 2L));
    }

    @Test void retainedRuntimePinsTargetTailAreaFormulaCounterAndAwakeAdmission()
            throws Exception {
        String packet = read(RETAINED.resolve(
                "其他src參考用/381n8src/com/lineage/server/clientpackets/C_UseSkill.java"));
        int magma = packet.lastIndexOf("case 184:// 岩漿噴吐");
        String targetFamily = packet.substring(magma,
                packet.indexOf("break;", magma));
        assertTrue(targetFamily.contains("targetId = readD();"));
        assertTrue(targetFamily.contains("targetX = readH();"));
        assertTrue(targetFamily.contains("targetY = readH();"));

        String use = read(RETAINED.resolve(
                "其他src參考用/381n8src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(use.contains("World.get().getVisibleObjects(this._target,"));
        assertTrue(use.contains("this._skill.getArea()"));
        assertTrue(use.contains("_dmg = magic.calcMagicDamage(_skillId);"));
        String exceptCounter = use.substring(use.indexOf("EXCEPT_COUNTER_MAGIC"),
                use.indexOf("};", use.indexOf("EXCEPT_COUNTER_MAGIC")));
        assertFalse(exceptCounter.contains("MAGMA_BREATH"));

        String magic = read(RETAINED.resolve(
                "其他src參考用/381n8src/com/lineage/server/model/L1MagicPc.java"));
        assertTrue(magic.contains("final int dice = l1skills.getDamageDice();"));
        assertTrue(magic.contains(
                "final int diceCount = l1skills.getDamageDiceCount();"));
        assertTrue(magic.contains("final int value = l1skills.getDamageValue();"));
        assertTrue(magic.contains("magicDamage += value;"));
        assertTrue(magic.contains("calcAttrResistance(l1skills.getAttr())"));
        assertTrue(magic.contains("return calcMrDefense(damage);"));

        String mode = read(RETAINED.resolve(
                "src/com/lineage/server/model/skillUse/L1SkillUseMode.java"));
        assertTrue(occurrences(mode, "case MAGMA_BREATH:") >= 3);
    }

    @Test void implementationRestoresTargetThenUsesBothAreaPipelines() throws Exception {
        BattleSpellCatalog.AreaAttackSpell spell =
                BattleSpellCatalog.areaAttackSpell(
                        BattleSpellCatalog.MAGMA_BREATH);
        assertEquals(22, spell.row());
        assertEquals(7, spell.column());
        assertEquals(0, spell.mpCost());
        assertEquals(35, spell.hpCost());
        assertEquals(1_000, spell.reuseDelayMillis());
        assertEquals(10, spell.maximumRange());
        assertEquals(3, spell.areaRadius());
        assertEquals(45, spell.baseDamage());
        assertEquals(0, spell.diceCount());
        assertEquals(0, spell.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_FIRE, spell.attribute());
        assertEquals(18, spell.actionId());
        assertEquals(6_516, spell.effectGfx());
        assertEquals(BattleSpellCatalog.RequestShape.SELECTOR_ONLY,
                spell.requestShape());
        assertEquals(0, spell.effectiveMpCost(23, 2));

        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertTrue(session.contains("useSelectorOnlyTargetedAreaAttackSpell("));
        assertTrue(session.contains("resolveSelectorOnlyAreaPrimary("));
        assertTrue(session.contains("lastCombatTargetObjectId"));
        assertTrue(session.contains("unique-range-target"));
        assertTrue(session.contains("useClassicWorldAreaAttackSpell(request, spell)"));
        assertTrue(session.contains("useAreaAttackSpell(request, spell)"));
        assertTrue(session.contains("rollAreaSpellSourceDamage(spell)"));
        assertTrue(session.contains("consumeCounterMagic(castNow)"));
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
