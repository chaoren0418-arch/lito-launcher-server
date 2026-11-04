package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks selected skill38 producer, resources and shared WORLD damage path. */
final class NativeWorldConeOfColdSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/clientpackets/C_UseSkill.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/Instance/L1NpcInstance.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/L1MagicMode.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/L1MagicPc.java"),
                Path.of("../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        LocalResources.assumeIdaClient(
                Path.of("../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
        LocalResources.assumeFiles(
                Path.of("../LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe"));
    }

    private static final Path WORKSPACE = Path.of("..");

    @Test
    void selectedAndSecondaryRowsKeepTheirQualifiedReuseDifference()
            throws Exception {
        String selected = read(WORKSPACE.resolve("1-伺服器/db/380_db.sql"));
        assertTrue(selected.contains(
                "INSERT INTO `skills` VALUES (38, 0, '冰錐', 5, 5, 18, 0, "
                + "0, 0, 10, 0, 'attack', 3, 32, 6, 5, 0, 0, 4, 64, 0, 6, "
                + "0, 0, 32, '$1972', 18, 11738, 0, 0, 0, 0);"));
        assertTrue(selected.contains(
                "INSERT INTO `skills_item` VALUES (38, '冰錐', NULL, NULL);"));

        String secondary = read(WORKSPACE.resolve(
                "1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills` VALUES (38, '冰錐', 5, 5, 18, 0, 0, "
                + "0, 400, 0, 'attack', 3, 32, 6, 5, 0, 0, 4, 64, 0, 6, "
                + "0, 0, 32, '$1972', 18, 11738, 0, 0, 0, 0);"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills_item` VALUES (38, '冰錐', NULL, NULL);"));
    }

    @Test
    void selectedClientUsesCase37CccdhhAndHundredMillisecondClock()
            throws Exception {
        String assembly = read(WORKSPACE.resolve(
                "LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
        String dispatch = between(assembly, "loc_4CB755:", "loc_4CB78B:");
        assertTrue(dispatch.contains(
                "cases 3,5,6,17,28,37,44,49,64,73,76,132,151,152,172,173"));
        assertTrue(dispatch.contains("mov     edx, 0Ah"));
        assertTrue(dispatch.contains("call    sub_4CA6E0"));
        String producer = between(assembly, "sub_4CA6E0 proc near",
                "sub_4CA6E0 endp");
        assertTrue(producer.contains("and     ecx, 7"));
        assertTrue(producer.contains("sar     ebx, 3"));
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("push    offset aCccdhh"));

        byte[] recovered = LocalResources.readAllBytes(WORKSPACE.resolve(
                "LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe"));
        assertEquals(100, peWordAtVirtualAddress(
                recovered, 0x635774 + 37 * 2));

        String parser = read(WORKSPACE.resolve(
                "1-伺服器/src/com/lineage/server/clientpackets/C_UseSkill.java"));
        String coordinateCases = between(parser,
                "case 4:// 光箭", "case 1:// 初級治癒術");
        assertTrue(coordinateCases.contains("case 38:// 冰錐"));
        assertTrue(coordinateCases.contains("targetId = readD();"));
        assertTrue(coordinateCases.contains("targetX = readH();"));
        assertTrue(coordinateCases.contains("targetY = readH();"));
    }

    @Test
    void retainedWaterWeakAttributeAndMagicOrderRemainAuthoritative()
            throws Exception {
        String npc = read(WORKSPACE.resolve(
                "1-伺服器/src/com/lineage/server/model/Instance/L1NpcInstance.java"));
        assertTrue(npc.contains("getNpcTemplate().get_weakAttr()"));
        assertTrue(npc.contains("case 4:// 水"));
        assertTrue(npc.contains("this.addWater(-50);"));
        assertTrue(npc.contains("case -4:// 抗水"));
        assertTrue(npc.contains("this.addWater(50);"));

        String magicMode = read(WORKSPACE.resolve(
                "1-伺服器/src/com/lineage/server/model/L1MagicMode.java"));
        assertTrue(magicMode.contains("resist = _targetNpc.getWater();"));
        assertTrue(magicMode.contains(
                "int resistFloor = (int) (0.32 * Math.abs(resist));"));

        String magic = read(WORKSPACE.resolve(
                "1-伺服器/src/com/lineage/server/model/L1MagicPc.java"));
        assertTrue(magic.contains("magicDamage += (_random.nextInt(dice) + 1);"));
        assertTrue(magic.contains("final double attrDeffence = calcAttrResistance"));
        assertTrue(magic.contains("private int calcMrDefense(int dmg)"));
    }

    @Test
    void catalogAndBattleWorldPathsShareSelectedWaterDamage() throws Exception {
        BattleSpellCatalog.AttackSpell spell = BattleSpellCatalog.attackSpell(
                BattleSpellCatalog.CONE_OF_COLD);
        assertEquals(38, spell.skillId());
        assertEquals(4, spell.row());
        assertEquals(5, spell.column());
        assertEquals(18, spell.actionId());
        assertEquals(11738, spell.effectGfx());
        assertEquals(18, spell.mpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(6, spell.maximumRange());
        assertEquals(32, spell.baseDamage());
        assertEquals(5, spell.diceCount());
        assertEquals(6, spell.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_WATER, spell.attribute());
        assertEquals(12, spell.effectiveMpCost(16, 2));

        assertEquals(-50, ClassicWorldMonsterMagicDamage
                .npcElementalResistance(4, spell.attribute(), 0));
        assertEquals(50, ClassicWorldMonsterMagicDamage
                .npcElementalResistance(-4, spell.attribute(), 0));
        assertEquals(0, ClassicWorldMonsterMagicDamage
                .npcElementalResistance(0, spell.attribute(), 0));

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
            assertTrue(handler.contains("consumeCounterMagic"));
        }
        assertTrue(world.contains("hub.damageClassicWorldMonster("));
        assertTrue(world.contains("takeClassicWorldPlayerDamage("));
        assertTrue(world.contains("map.attackPosition("));
    }

    private static String read(Path path) throws Exception {
        return LocalResources.readString(path, StandardCharsets.UTF_8);
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
