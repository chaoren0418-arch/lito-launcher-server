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

/** Cross-source proof for native slot189 Shock Skin / Magma Arrow. */
final class NativeShockSkinSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeLitoBin();
        LocalResources.assumeIdaClient();
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

    @Test void selectedRowPinsInstantCenteredWindAttackDespiteNames()
            throws Exception {
        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertEquals("INSERT INTO `skills` VALUES (189, 0, '岩漿之箭', 24, 4,"
                + " 0, 40, 0, 0, 10, 0, 'none', 3, 45, 0, 0, 0, 0, 8, 64,"
                + " 0, 0, 2, 0, 16, '$5928', 18, 6532, 0, 0, 0, 0);",
                skillRow(primary, 189));
        assertEquals("INSERT INTO `skills` VALUES (189, '岩漿之箭', 24, 4,"
                + " 0, 40, 0, 0, 0, 0, 'none', 3, 45, 0, 0, 0, 0, 8, 64,"
                + " 0, 0, 2, 0, 16, '$5928', 18, 6532, 0, 0, 0, 0);",
                skillRow(secondary, 189));
        assertTrue(primary.contains("INSERT INTO `skills_item` VALUES"
                + " (189, '衝擊之膚', NULL, NULL);"));
        assertTrue(primary.contains("NULL, 49110, '龍騎士書版(衝擊之膚)'"));
        assertFalse(primary.contains(
                "INSERT INTO `skills_負面技能控制` VALUES (189,"));
        assertFalse(primary.contains(
                "INSERT INTO `skills_魔屏無效技能` VALUES (189,"));
        assertFalse(primary.contains(
                "INSERT INTO `skills_buff_ability` VALUES (189,"));
        assertFalse(primary.contains(
                "INSERT INTO `CharBuffTable` VALUES (189,"));
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
                0x635774L + (long) (BattleSpellCatalog.SHOCK_SKIN - 1) * 2L));
    }

    @Test void retainedDurationZeroMakesTeleportBranchDormant() throws Exception {
        String packet = read(RETAINED.resolve(
                "src/com/lineage/server/clientpackets/C_UseSkill.java"));
        int shock = packet.indexOf("case 189:// 衝擊之膚");
        String targetFamily = packet.substring(shock,
                packet.indexOf("break;", shock));
        assertTrue(targetFamily.contains("targetId = readD();"));
        assertFalse(targetFamily.contains("targetX = readH();"));
        String olderPacket = read(RETAINED.resolve(
                "其他src參考用/381n8src/com/lineage/server/clientpackets/C_UseSkill.java"));
        int olderShock = olderPacket.indexOf("case 189:// 衝擊之膚");
        String olderTargetFamily = olderPacket.substring(olderShock,
                olderPacket.indexOf("break;", olderShock));
        assertTrue(olderTargetFamily.contains("targetId = readD();"));
        assertFalse(olderTargetFamily.contains("targetX = readH();"));

        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(use.contains("_skill.getTarget().equals(\"none\")"
                + " && (_skill.getType() == L1Skills.TYPE_ATTACK)"));
        assertTrue(use.contains("World.get().getVisibleObjects(this._target,"));
        assertTrue(use.contains("this._skill.getArea()"));
        String addMagic = use.substring(use.indexOf("private void addMagicList("),
                use.indexOf("private void sendIcon(",
                        use.indexOf("private void addMagicList(")));
        assertTrue(addMagic.contains("if (_skill.getBuffDuration() == 0)"));
        assertTrue(addMagic.contains("return;"));

        String buff = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1BuffUtil.java"));
        assertTrue(buff.contains("pc.hasSkillEffect(SHOCK_SKIN)"));
        assertTrue(buff.contains("public static boolean getUseItemTeleport"));
        assertFalse(addMagic.contains("case SHOCK_SKIN:"));
    }

    @Test void implementationUsesBothCenteredAreaPipelinesWithoutStatus()
            throws Exception {
        BattleSpellCatalog.AreaAttackSpell spell =
                BattleSpellCatalog.areaAttackSpell(
                        BattleSpellCatalog.SHOCK_SKIN);
        assertEquals(23, spell.row());
        assertEquals(4, spell.column());
        assertEquals(0, spell.mpCost());
        assertEquals(40, spell.hpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(0, spell.maximumRange());
        assertEquals(2, spell.areaRadius());
        assertEquals(45, spell.baseDamage());
        assertEquals(0, spell.diceCount());
        assertEquals(0, spell.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_WIND, spell.attribute());
        assertEquals(18, spell.actionId());
        assertEquals(6_532, spell.effectGfx());
        assertEquals(BattleSpellCatalog.RequestShape.SELECTOR_ONLY,
                spell.requestShape());
        assertEquals("shock-skin-magma-arrow", spell.key());

        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertTrue(session.contains("useCenteredAreaAttackSpell("));
        assertTrue(session.contains("useClassicWorldCenteredAreaAttackSpell("));
        assertTrue(session.contains("rollAreaSpellSourceDamage(spell)"));
        assertTrue(session.contains("consumeCounterMagic(castNow)"));
        assertFalse(session.contains("activateShockSkin"));
    }

    private static String skillRow(String sql, int skillId) {
        String prefix = "INSERT INTO `skills` VALUES (" + skillId + ",";
        return sql.lines().filter(line -> line.startsWith(prefix))
                .findFirst().orElseThrow();
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
