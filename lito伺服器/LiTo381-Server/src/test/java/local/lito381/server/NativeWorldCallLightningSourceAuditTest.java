package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Locks selected skill34 producer, resources and shared NPC elemental flow. */
final class NativeWorldCallLightningSourceAuditTest {
    private static final Path WORKSPACE = Path.of("..");

    @Test
    void selectedAndSecondaryRowsKeepTheirQualifiedReuseDifference()
            throws Exception {
        String selected = read(WORKSPACE.resolve("1-伺服器/db/380_db.sql"));
        assertTrue(selected.contains(
                "INSERT INTO `skills` VALUES (34, 0, '極道落雷', 5, 1, 18, 0, "
                + "0, 0, 10, 0, 'attack', 3, 32, 8, 5, 0, 0, 8, 64, 0, 6, "
                + "0, 0, 2, '$1457', 18, 11736, 0, 0, 0, 0);"));
        assertTrue(selected.contains(
                "INSERT INTO `skills_item` VALUES (34, '極道落雷', NULL, NULL);"));

        String secondary = read(WORKSPACE.resolve(
                "1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills` VALUES (34, '極道落雷', 5, 1, 18, 0, 0, "
                + "0, 1000, 0, 'attack', 3, 32, 8, 5, 0, 0, 8, 64, 0, 6, "
                + "0, 0, 2, '$1457', 18, 11736, 0, 0, 0, 0);"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills_item` VALUES (34, '極道落雷', NULL, NULL);"));
    }

    @Test
    void selectedClientUsesCase33CccdhhAndTenMillisecondClock()
            throws Exception {
        String assembly = read(WORKSPACE.resolve(
                "LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
        String dispatch = between(assembly, "loc_4CB989:", "loc_4CB9A3:");
        assertTrue(dispatch.contains(
                "jumptable 004CB66E cases 14,15,33,65,75"));
        assertTrue(dispatch.contains("mov     edx, 0Ah"));
        assertTrue(dispatch.contains("call    sub_4CA6E0"));
        String producer = between(assembly, "sub_4CA6E0 proc near",
                "sub_4CA6E0 endp");
        assertTrue(producer.contains("and     ecx, 7"));
        assertTrue(producer.contains("sar     ebx, 3"));
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("push    offset aCccdhh"));

        byte[] recovered = Files.readAllBytes(WORKSPACE.resolve(
                "LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe"));
        assertEquals(10, peWordAtVirtualAddress(
                recovered, 0x635774 + 33 * 2));

        String parser = read(WORKSPACE.resolve(
                "1-伺服器/src/com/lineage/server/clientpackets/C_UseSkill.java"));
        String coordinateCases = between(parser,
                "case 4:// 光箭", "case 1:// 初級治癒術");
        assertTrue(coordinateCases.contains("case 34:// 極道落雷"));
        assertTrue(coordinateCases.contains("targetId = readD();"));
        assertTrue(coordinateCases.contains("targetX = readH();"));
        assertTrue(coordinateCases.contains("targetY = readH();"));
    }

    @Test
    void retainedNpcWeakAttributeAndMagicOrderRemainAuthoritative()
            throws Exception {
        String npc = read(WORKSPACE.resolve(
                "1-伺服器/src/com/lineage/server/model/Instance/L1NpcInstance.java"));
        assertTrue(npc.contains("getNpcTemplate().get_weakAttr()"));
        assertTrue(npc.contains("case 8:// 風"));
        assertTrue(npc.contains("this.addWind(-50);"));
        assertTrue(npc.contains("case -8:// 抗風"));
        assertTrue(npc.contains("this.addWind(50);"));

        String magicMode = read(WORKSPACE.resolve(
                "1-伺服器/src/com/lineage/server/model/L1MagicMode.java"));
        assertTrue(magicMode.contains("resist = _targetNpc.getWind();"));
        assertTrue(magicMode.contains("int resistFloor = (int) (0.32 * Math.abs(resist));"));

        String magic = read(WORKSPACE.resolve(
                "1-伺服器/src/com/lineage/server/model/L1MagicPc.java"));
        assertTrue(magic.contains("magicDamage += (_random.nextInt(dice) + 1);"));
        assertTrue(magic.contains("final double attrDeffence = calcAttrResistance"));
        assertTrue(magic.contains("private int calcMrDefense(int dmg)"));
    }

    @Test
    void catalogAndBattleWorldNpcPathsUseSelectedWeakAttribute() throws Exception {
        BattleSpellCatalog.AttackSpell spell = BattleSpellCatalog.attackSpell(
                BattleSpellCatalog.CALL_LIGHTNING);
        assertEquals(34, spell.skillId());
        assertEquals(4, spell.row());
        assertEquals(1, spell.column());
        assertEquals(18, spell.actionId());
        assertEquals(11736, spell.effectGfx());
        assertEquals(18, spell.mpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(6, spell.maximumRange());
        assertEquals(32, spell.baseDamage());
        assertEquals(5, spell.diceCount());
        assertEquals(8, spell.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_WIND, spell.attribute());
        assertEquals(12, spell.effectiveMpCost(16, 2));

        assertEquals(-50, ClassicWorldMonsterMagicDamage
                .npcElementalResistance(8, spell.attribute(), 0));
        assertEquals(50, ClassicWorldMonsterMagicDamage
                .npcElementalResistance(-8, spell.attribute(), 0));

        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        String battle = between(session,
                "private void useAttackSpell(byte[] payload,",
                "private void useClassicWorldAttackSpell(");
        String world = between(session,
                "private void useClassicWorldAttackSpell(",
                "private BoneBreakApplication applyClassicWorldBoneBreak(");
        for (String handler : new String[] {battle, world}) {
            assertTrue(handler.contains("Protocol.attackSpellRequest"));
            assertTrue(handler.contains("spell.effectiveMpCost("));
            assertTrue(handler.contains(
                    "ClassicWorldMonsterMagicDamage.npcElementalResistance("));
            assertTrue(handler.contains("ordinaryMagicDamage("));
        }
    }

    private static String read(Path path) throws Exception {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String between(String source, String start, String end) {
        int begin = source.indexOf(start);
        int finish = source.indexOf(end, begin + start.length());
        assertTrue(begin >= 0 && finish > begin, start + " .. " + end);
        return source.substring(begin, finish);
    }

    private static int peWordAtVirtualAddress(byte[] data, int address) {
        ByteBuffer bytes = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        int pe = bytes.getInt(0x3C);
        int sections = Short.toUnsignedInt(bytes.getShort(pe + 6));
        int optionalSize = Short.toUnsignedInt(bytes.getShort(pe + 20));
        int optional = pe + 24;
        int imageBase = bytes.getInt(optional + 28);
        int rva = address - imageBase;
        int table = optional + optionalSize;
        for (int index = 0; index < sections; index++) {
            int section = table + index * 40;
            int virtualSize = bytes.getInt(section + 8);
            int sectionRva = bytes.getInt(section + 12);
            int rawSize = bytes.getInt(section + 16);
            int rawOffset = bytes.getInt(section + 20);
            long upper = Integer.toUnsignedLong(sectionRva)
                    + Math.max(Integer.toUnsignedLong(virtualSize),
                            Integer.toUnsignedLong(rawSize));
            if (Integer.toUnsignedLong(rva) >= Integer.toUnsignedLong(sectionRva)
                    && Integer.toUnsignedLong(rva) + 2 <= upper) {
                return Short.toUnsignedInt(bytes.getShort(
                        rawOffset + rva - sectionRva));
            }
        }
        throw new IllegalArgumentException("unmapped PE address " + address);
    }
}
