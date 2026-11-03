package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Locks selected skill37 resources, native producer and retained cure boundary. */
final class NativeWorldRemoveCurseSourceAuditTest {
    private static final Path WORKSPACE = Path.of("..");

    @Test
    void selectedAndSecondaryRowsPreserveHolyLightResources() throws Exception {
        String selected = read(WORKSPACE.resolve("1-伺服器/db/380_db.sql"));
        assertTrue(selected.contains(
                "INSERT INTO `skills` VALUES (37, 0, '聖潔之光', 5, 4, 20, 0, "
                + "0, 0, 10, 0, 'buff', 3, 0, 0, 0, 0, 0, 8, 2, 0, -1, 0, "
                + "0, 16, '$1460', 19, 227, 0, 155, 0, 280);"));
        assertTrue(selected.contains(
                "INSERT INTO `skills_item` VALUES (37, '聖潔之光', NULL, NULL);"));

        String secondary = read(WORKSPACE.resolve(
                "1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills` VALUES (37, '聖潔之光', 5, 4, 20, 0, 0, "
                + "0, 0, 0, 'buff', 3, 0, 0, 0, 0, 0, 8, 2, 0, -1, 0, 0, 16, "
                + "'$1460', 19, 227, 0, 155, 0, 280);"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills_item` VALUES (37, '聖潔之光', NULL, NULL);"));
    }

    @Test
    void selectedClientUsesCase36CccdAndTenMillisecondClock() throws Exception {
        String assembly = read(WORKSPACE.resolve(
                "LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
        String initial = between(assembly, "loc_4CB112:", "loc_4CB11C:");
        assertTrue(initial.contains("jumptable 004CB10B cases 0,7,8,18,22,34,36"));
        assertTrue(initial.contains("jmp     sub_4C9880"));
        String producer = between(assembly,
                "sub_4C9880 proc near", "sub_4C9880 endp");
        assertTrue(producer.contains("and     ecx, 7"));
        assertTrue(producer.contains("sar     esi, 3"));
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("push    offset aCccd"));

        byte[] recovered = Files.readAllBytes(WORKSPACE.resolve(
                "LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe"));
        assertEquals(10, peWordAtVirtualAddress(
                recovered, 0x635774 + 36 * 2));

        String parser = read(WORKSPACE.resolve(
                "1-伺服器/src/com/lineage/server/clientpackets/C_UseSkill.java"));
        String cases = between(parser, "case 1:// 初級治癒術", "case 2:// 日光術");
        assertTrue(cases.contains("case 37:// 聖潔之光"));
        assertTrue(cases.contains("targetId = readD();"));
    }

    @Test
    void retainedModeCuresInstalledPoisonAndOnlyCurseParalysis() throws Exception {
        String mode = read(WORKSPACE.resolve(
                "1-伺服器/src/com/lineage/server/model/skill/skillmode/REMOVE_CURSE.java"));
        String pc = between(mode,
                "public int start(final L1PcInstance srcpc",
                "public int start(final L1NpcInstance npc");
        assertTrue(pc.contains("cha.curePoison();"));
        assertTrue(pc.contains("cha.hasSkillEffect(STATUS_CURSE_PARALYZING)"));
        assertTrue(pc.contains("cha.hasSkillEffect(STATUS_CURSE_PARALYZED)"));
        assertTrue(pc.contains("cha.cureParalaysis();"));
        assertTrue(!pc.contains("FOG_OF_SLEEPING"));
        assertTrue(!pc.contains("EARTH_BIND"));
        assertTrue(!pc.contains("STATUS_FREEZE"));
        assertTrue(!pc.contains("SHOCK_STUN"));
    }

    @Test
    void battleAndWorldHandlersShareAdmissionCommitAndCureBoundary()
            throws Exception {
        BattleSpellCatalog.CurseRemovalSpell spell =
                BattleSpellCatalog.curseRemovalSpell(BattleSpellCatalog.REMOVE_CURSE);
        assertEquals(37, spell.skillId());
        assertEquals(4, spell.row());
        assertEquals(4, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(227, spell.effectGfx());
        assertEquals(20, spell.mpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(-1, spell.maximumRange());
        assertTrue(spell.curesCurseParalysis());

        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        String world = between(session,
                "private void useClassicWorldCurseRemovalSpell(",
                "private void useCurseRemovalSpell(");
        String battle = between(session,
                "private void useCurseRemovalSpell(",
                "private void useElementalSummonSpell(");
        for (String handler : new String[] {world, battle}) {
            assertTrue(handler.contains("Protocol.objectSpellRequest(payload)"));
            assertTrue(handler.contains("polymorphStatus.active("));
            assertTrue(handler.contains("timedStatus.silenceActive("));
            assertTrue(handler.contains("currentWeight240("));
            assertTrue(handler.contains("learnedSkills.contains(spell.skillId())"));
            assertTrue(handler.contains("spell.effectiveMpCost("));
            assertTrue(handler.contains("commitSpellReady("));
            assertTrue(handler.contains("cancelMeditation("));
            assertTrue(handler.contains("cancelAbsoluteBarrier("));
            assertTrue(handler.contains("revealInvisibility("));
            assertTrue(handler.contains("Protocol.objectAction("));
            assertTrue(handler.contains("Protocol.effectObject("));
            assertTrue(handler.contains("Protocol.manaPoint("));
        }
        assertTrue(world.contains("playerTarget.timedStatus.removeCurse()"));
        assertTrue(world.contains("hub.cureClassicWorldMonsterPoison("));
        assertTrue(world.contains("hub.cureClassicWorldMonsterParalysis("));
        assertTrue(battle.contains("playerTarget.timedStatus.removeCurse()"));
        assertTrue(battle.contains("hub.cureQuestMonsterPoison("));
        assertTrue(battle.contains("hub.cureTimeAttackActorPoison("));
    }

    @Test
    void battleAcceptanceUsesTheNativeNorthEdgeAfterParalysisCure() {
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(510);
        assertTrue(arena.nativeCanStep(32765, 32819, 32765, 32818));
        assertTrue(!arena.nativeCanStep(32765, 32819, 32764, 32819));
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
