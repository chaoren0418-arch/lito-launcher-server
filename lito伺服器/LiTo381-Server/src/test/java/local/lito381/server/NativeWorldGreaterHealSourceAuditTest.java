package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Locks selected skill35 producer, healing arithmetic and live target modifiers. */
final class NativeWorldGreaterHealSourceAuditTest {
    private static final Path WORKSPACE = Path.of("..");

    @Test
    void selectedAndSecondaryRowsAgreeOnGreaterHealResources() throws Exception {
        String selected = read(WORKSPACE.resolve("1-伺服器/db/380_db.sql"));
        assertTrue(selected.contains(
                "INSERT INTO `skills` VALUES (35, 0, '高級治癒術', 5, 2, 20, 0, "
                + "0, 0, 200, 0, 'buff', 3, 8, 8, 0, 0, 0, 4, 16, 0, -1, "
                + "0, 0, 4, '$1458', 19, 11724, 0, 77, 0, 280);"));
        assertTrue(selected.contains(
                "INSERT INTO `skills_item` VALUES (35, '高級治癒術', NULL, NULL);"));

        String secondary = read(WORKSPACE.resolve(
                "1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills` VALUES (35, '高級治癒術', 5, 2, 20, 0, 0, "
                + "0, 200, 0, 'buff', 3, 8, 8, 0, 0, 0, 4, 16, 0, -1, 0, "
                + "0, 4, '$1458', 19, 11724, 0, 77, 0, 280);"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills_item` VALUES (35, '高級治癒術', NULL, NULL);"));
    }

    @Test
    void selectedClientUsesCase34CccdAndTwoHundredMillisecondClock()
            throws Exception {
        String assembly = read(WORKSPACE.resolve(
                "LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
        String producer = between(assembly, "loc_4CB675:", "loc_4CB71D:");
        assertTrue(producer.contains("jumptable 004CB66E cases 0,18,34,56"));
        assertTrue(producer.contains("and     edx, 7"));
        assertTrue(producer.contains("sar     eax, 3"));
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("push    offset aCccd"));

        byte[] recovered = Files.readAllBytes(WORKSPACE.resolve(
                "LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe"));
        assertEquals(200, peWordAtVirtualAddress(
                recovered, 0x635774 + 34 * 2));

        String parser = read(WORKSPACE.resolve(
                "1-伺服器/src/com/lineage/server/clientpackets/C_UseSkill.java"));
        String objectCases = between(parser,
                "case 1:// 初級治癒術", "case 2:// 日光術");
        assertTrue(objectCases.contains("case 35:// 高級治癒術"));
        assertTrue(objectCases.contains("targetId = readD();"));
        assertFalse(objectCases.contains("targetX = readH();"));
        assertFalse(objectCases.contains("targetY = readH();"));
    }

    @Test
    void retainedHealingOrderAndUndeadRulesRemainAuthoritative()
            throws Exception {
        String magic = read(WORKSPACE.resolve(
                "1-伺服器/src/com/lineage/server/model/L1MagicPc.java"));
        String healing = between(magic, "public int calcHealing(",
                "private int calcMrDefense(");
        assertTrue(healing.contains("final int diceCount = value + healDiceBonus"));
        assertTrue(healing.contains("magicDamage += (_random.nextInt(dice) + 1)"));
        assertTrue(healing.contains("alignmentRevision += (getLawful() / 32768.0)"));

        String use = read(WORKSPACE.resolve(
                "1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"));
        String typeHeal = between(use, "case L1Skills.TYPE_HEAL:",
                "// TODO SKILL移轉");
        int waterLife = typeHeal.indexOf("cha.hasSkillEffect(WATER_LIFE)");
        int polluteWater = typeHeal.indexOf("cha.hasSkillEffect(POLLUTE_WATER)");
        int dirtyCurrent = typeHeal.indexOf("cha.hasSkillEffect(ADLV80_2_2)");
        int erosion = typeHeal.indexOf("cha.hasSkillEffect(ADLV80_2_3)");
        assertTrue(0 <= waterLife && waterLife < polluteWater
                && polluteWater < dirtyCurrent && dirtyCurrent < erosion);
        assertTrue(use.contains("cha.removeSkillEffect(WATER_LIFE)"));
        assertTrue(use.contains("(undeadType == 1)"));
        assertTrue(use.contains("this._dmg *= -1"));
        assertTrue(use.contains("(undeadType == 3)"));
        assertTrue(use.contains("this._dmg = 0"));
    }

    @Test
    void catalogAndBattleWorldPathsApplyOneShotAndMonsterModifiers()
            throws Exception {
        BattleSpellCatalog.HealingSpell spell = BattleSpellCatalog.healingSpell(
                BattleSpellCatalog.GREATER_HEAL);
        assertEquals(35, spell.skillId());
        assertEquals(4, spell.row());
        assertEquals(2, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(11724, spell.effectGfx());
        assertEquals(20, spell.mpCost());
        assertEquals(200, spell.reuseDelayMillis());
        assertEquals(-1, spell.maximumRange());
        assertEquals(7, spell.diceCount(8));
        assertEquals(14, spell.effectiveMpCost(16, 2, true));
        assertEquals(-40, spell.npcHpDelta(1, 40));
        assertEquals(0, spell.npcHpDelta(3, 40));

        assertEquals(20, BattleHealingRecovery.adjustedSingleTargetAmount(
                20, false, false, false, false));
        assertEquals(40, BattleHealingRecovery.adjustedSingleTargetAmount(
                20, true, false, false, false));
        assertEquals(20, BattleHealingRecovery.adjustedSingleTargetAmount(
                20, true, true, false, false));

        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        String world = between(session,
                "private void useClassicWorldHealingSpell(",
                "private void useHealingSpell(");
        String battle = between(session,
                "private void useHealingSpell(",
                "private void notifyPeersOfDisconnect(");
        for (String handler : new String[] {world, battle}) {
            assertTrue(handler.contains(
                    "BattleHealingRecovery.adjustedSingleTargetAmount("));
            assertTrue(handler.contains("waterLifeStatus.active("));
            assertTrue(handler.contains("waterLifeStatus.cancel()"));
            assertTrue(handler.contains("polluteWater"));
        }
        assertTrue(world.contains(
                "hub.classicWorldMonsterPolluteWaterActive("));
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
