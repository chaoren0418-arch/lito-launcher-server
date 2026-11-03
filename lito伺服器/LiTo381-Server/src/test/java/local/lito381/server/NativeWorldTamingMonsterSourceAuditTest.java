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

/** Locks selected skill36 resources, producer and retained probability lifecycle. */
final class NativeWorldTamingMonsterSourceAuditTest {
    private static final Path WORKSPACE = Path.of("..");

    @Test
    void selectedAndSecondaryRowsPreserveTamingResourcesAndOverrides()
            throws Exception {
        String selected = read(WORKSPACE.resolve("1-伺服器/db/380_db.sql"));
        assertTrue(selected.contains(
                "INSERT INTO `skills` VALUES (36, 0, '迷魅術', 5, 3, 30, 20, "
                + "40318, 1, 10, 3600, 'buff', 2, 0, 0, 0, 0, 6, 0, 1, 0, "
                + "-1, 0, 0, 8, '$1459', 19, 228, 0, 0, 324, 280);"));
        assertTrue(selected.contains(
                "INSERT INTO `skills_負面技能控制` VALUES (36, '迷魅術', 40, "
                + "35, 30, 0, 0, 0.5, 0, 0, 1, 0.05, 0, 0);"));
        assertTrue(selected.contains(
                "INSERT INTO `skills_負面boss免疫` VALUES (36, '迷魅術', 0);"));
        assertTrue(selected.contains(
                "INSERT INTO `skills_item` VALUES (36, '迷魅術', NULL, NULL);"));

        String secondary = read(WORKSPACE.resolve(
                "1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills` VALUES (36, '迷魅術', 5, 3, 30, 20, "
                + "40318, 1, 0, 3600, 'buff', 2, 0, 0, 0, 0, 6, 0, 1, 0, "
                + "-1, 0, 0, 8, '$1459', 19, 228, 0, 0, 324, 280);"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills_負面技能控制` VALUES (36, '迷魅術', 40, "
                + "35, 30, 0, 0, 0.5, 0, 0, 1, 0.05, 0, 0);"));
    }

    @Test
    void selectedClientUsesCase35CccdhhAndTenMillisecondClock()
            throws Exception {
        String assembly = read(WORKSPACE.resolve(
                "LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
        String dispatcher = between(assembly, "loc_4CB889:", "loc_4CB8A0:");
        assertTrue(dispatcher.contains("jumptable 004CB66E case 35"));
        assertTrue(dispatcher.contains("call    sub_4CA0B0"));
        String producer = between(assembly,
                "sub_4CA0B0 proc near", "sub_4CA0B0 endp");
        assertTrue(producer.contains("and     edx, 7"));
        assertTrue(producer.contains("sar     edi, 3"));
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("push    offset aCccdhh"));

        byte[] recovered = Files.readAllBytes(WORKSPACE.resolve(
                "LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe"));
        assertEquals(10, peWordAtVirtualAddress(
                recovered, 0x635774 + 35 * 2));

        String parser = read(WORKSPACE.resolve(
                "1-伺服器/src/com/lineage/server/clientpackets/C_UseSkill.java"));
        String targetCases = between(parser,
                "case 1:// 初級治癒術", "case 2:// 日光術");
        assertTrue(targetCases.contains("case 36:// 迷魅術"));
        assertTrue(targetCases.contains("targetId = readD();"));
    }

    @Test
    void retainedProbabilityOrderingAndConversionRulesRemainAuthoritative()
            throws Exception {
        String magic = read(WORKSPACE.resolve(
                "1-伺服器/src/com/lineage/server/model/L1MagicPc.java"));
        String probability = between(magic,
                "public boolean calcProbabilityMagic(", "private int calcProbability(");
        int earthBind = probability.indexOf("_targetNpc.hasSkillEffect(EARTH_BIND)");
        int calculation = probability.indexOf("probability = calcProbability(skillId)");
        assertTrue(0 <= earthBind && earthBind < calculation);
        assertTrue(probability.contains("skillId != WEAPON_BREAK"));
        assertTrue(probability.contains("skillId != CANCELLATION"));

        String use = read(WORKSPACE.resolve(
                "1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"));
        String probabilityType = between(use,
                "case L1Skills.TYPE_CURSE:", "case L1Skills.TYPE_HEAL:");
        int roll = probabilityType.indexOf("calcProbabilityMagic(this._skillId)");
        int erase = probabilityType.indexOf("removeSkillEffect(ERASE_MAGIC)");
        int sleep = probabilityType.indexOf("removeSkillEffect(FOG_OF_SLEEPING)");
        int counter = probabilityType.indexOf("isUseCounterMagic(cha)");
        assertTrue(0 <= roll && roll < erase && erase < sleep && sleep < counter);

        String conversion = between(use,
                "// ★★★ 寵物系技能 ★★★", "} else if (this._skillId == CREATE_ZOMBIE)");
        assertTrue(conversion.contains("_skillId == TAMING_MONSTER"));
        assertTrue(conversion.contains("getNpcTemplate().isTamable()"));
        assertTrue(conversion.contains("_player.canTakePet(6)"));
        assertTrue(conversion.contains("new L1SummonInstance("));
        assertTrue(conversion.contains("this._target = summon"));
        assertTrue(conversion.contains("new S_ServerMessage(319)"));
    }

    @Test
    void catalogAndBattleWorldHandlerPreserveFullAcceptedAttemptLifecycle()
            throws Exception {
        BattleSpellCatalog.TamingSpell spell = BattleSpellCatalog.tamingSpell(
                BattleSpellCatalog.TAMING_MONSTER);
        assertEquals(36, spell.skillId());
        assertEquals(4, spell.row());
        assertEquals(3, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(228, spell.effectGfx());
        assertEquals(30, spell.mpCost());
        assertEquals(20, spell.hpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(-1, spell.maximumRange());
        assertEquals(3_600, spell.durationSeconds());
        assertEquals(280, spell.failureMessage());
        assertEquals(55, spell.probability(52, 18, 18, 5, 3, 40));
        assertEquals(90, spell.probability(99, 1, 99, 20, 20, 0));

        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        String handler = between(session,
                "private void useTamingSpell(",
                "private boolean canTakeControlledCompanion(");
        assertTrue(handler.contains("phase == Phase.WORLD"));
        assertTrue(handler.contains("hub.classicWorldMonsterEarthBindActive("));
        assertTrue(handler.contains("BattleEarthBindRules.blocksProbabilityMagic("));
        assertTrue(handler.contains("consumeEraseMagicAfterProbability("));
        assertTrue(handler.contains("hub.cancelClassicWorldMonsterSleep("));
        assertTrue(handler.contains("breakSleepForStatusSpell("));
        assertTrue(handler.contains("cancelAbsoluteBarrier(\"spell-\""));
        assertTrue(handler.contains("revealInvisibility(\"spell-\""));
        assertTrue(handler.contains("hub.captureClassicWorldMonster("));
        assertTrue(handler.contains("hub.captureQuestMonster("));
        assertTrue(handler.contains("hub.captureTimeAttackActor("));
        assertTrue(handler.contains("Protocol.messageCode(319)"));
        assertTrue(handler.contains("Protocol.messageCode(spell.failureMessage())"));

        String population = read(Path.of(
                "src/main/java/local/lito381/server/ClassicWorldMonsterPopulation.java"));
        String capture = between(population,
                "synchronized Capture captureTamed(",
                "synchronized TamedRelease releaseTamed(");
        assertTrue(capture.contains("Snapshot captured = actor.snapshot()"));
        assertTrue(capture.contains("actor.present = false"));
        assertTrue(capture.contains("actor.damagePoisonStatus.cancelMagicEffects()"));
        assertTrue(capture.contains("vacate(new Cell(mapId, actor.x, actor.y))"));
        assertFalse(capture.contains("actor.alive = false"));
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
