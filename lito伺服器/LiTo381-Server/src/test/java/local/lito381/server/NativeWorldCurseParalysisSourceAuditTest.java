package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks selected skill33 resources, timing source and shared phase handlers. */
final class NativeWorldCurseParalysisSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeIdaClient(LocalResources.IDA_DISASSEMBLY);
    }

    private static final Path WORKSPACE = Path.of("..");

    @Test
    void selectedRowsAndLiToObjectCoordinateProducerStayExact() throws Exception {
        String primary = read(WORKSPACE.resolve("1-伺服器/db/380_db.sql"));
        assertTrue(primary.contains(
                "INSERT INTO `skills` VALUES (33, 0, '木乃伊的詛咒', 5, 0, 150, 0, "
                + "40318, 2, 5000, 20, 'buff', 3, 0, 0, 0, 0, 4, 1, 4, -500, "
                + "2, 0, 0, 1, '$1456', 19, 746, 0, 0, 0, 280);"));
        assertTrue(primary.contains(
                "INSERT INTO `skills_item` VALUES (33, '木乃伊的詛咒', NULL, NULL);"));
        assertTrue(primary.contains(
                "INSERT INTO `skills_負面技能控制` VALUES (33, "
                + "'木乃伊的詛咒-石化耐性', 50, 40, 30, 0, 0, 0.4, 2, 0, 1, "
                + "0.05, 0, 0);"));

        String secondary = read(WORKSPACE.resolve(
                "1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills` VALUES (33, '木乃伊的詛咒', 5, 0, 150, 0, "
                + "40318, 2, 5000, 20, 'buff', 3, 0, 0, 0, 0, 4, 1, 4, -500, "
                + "2, 0, 0, 1, '$1456', 19, 746, 0, 0, 0, 280);"));

        String assembly = read(WORKSPACE.resolve(
                "LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
        String dispatch = between(assembly, "loc_4CB952:", "loc_4CB96F:");
        assertTrue(dispatch.contains("jumptable 004CB66E case 32"));
        assertTrue(dispatch.contains("mov     ecx, 20h"));
        assertTrue(dispatch.contains("mov     edx, 1"));
        assertTrue(dispatch.contains("call    sub_4CA6E0"));
        String producer = between(assembly, "sub_4CA6E0 proc near",
                "sub_4CA6E0 endp");
        assertTrue(producer.contains("push    offset aCccdhh"));
    }

    @Test
    void selectedServerTimingAndRetainedLifecycleStayExact() throws Exception {
        String configSkill = read(WORKSPACE.resolve(
                "1-伺服器/src/com/lineage/config/ConfigSkill.java"));
        assertTrue(configSkill.contains(
                "CURSE_READY_TIME = _getInt(p, \"CURSE_READY_TIME\", 5);"));
        assertTrue(configSkill.contains(
                "CURSE_TIME = _getInt(p, \"CURSE_TIME\", 10);"));
        String deployed = read(WORKSPACE.resolve(
                "1-伺服器/config/其他控制端/4-職業_技能設定表.properties"));
        assertTrue(deployed.contains("CURSE_READY_TIME = 5"));
        assertTrue(deployed.contains("CURSE_TIME = 10"));

        String mode = read(WORKSPACE.resolve(
                "1-伺服器/src/com/lineage/server/model/skill/skillmode/CURSE_PARALYZE.java"));
        assertTrue(mode.contains("ConfigSkill.CURSE_READY_TIME * 1000"));
        assertTrue(mode.contains("ConfigSkill.CURSE_TIME * 1000"));
        assertTrue(mode.contains("L1CurseParalysis.curse(cha, delay, time, 1)"));
        assertTrue(mode.contains("L1CurseParalysis.curse(cha, 0, time, 0)"));

        String lifecycle = read(WORKSPACE.resolve(
                "1-伺服器/src/com/lineage/server/model/L1CurseParalysis.java"));
        assertTrue(lifecycle.contains("new S_ServerMessage(212)"));
        assertTrue(lifecycle.contains("new S_Paralysis(1, true)"));
        assertTrue(lifecycle.contains("_target.setParalyzed(true)"));
        assertTrue(lifecycle.contains("new S_Paralysis(1, false)"));

        String magic = read(WORKSPACE.resolve(
                "1-伺服器/src/com/lineage/server/model/L1MagicPc.java"));
        assertTrue(magic.contains("probability -= getTargetResistValue(resistType);"));
        assertTrue(magic.contains("probability -= targetMr * config.getMrMultiplier();"));
    }

    @Test
    void catalogAndBothBattleWorldHandlersUseTheSelectedTiming() throws Exception {
        BattleSpellCatalog.ParalysisSpell spell =
                BattleSpellCatalog.paralysisSpell(BattleSpellCatalog.CURSE_PARALYZE);
        assertEquals(5_000, spell.clientCastClockMillis());
        assertEquals(5_000, spell.reuseDelayMillis());
        assertEquals(5_000, spell.warningDelayMillis());
        assertEquals(10_000, spell.paralysisDurationMillis());
        assertEquals(144, spell.effectiveMpCost(16, 2));

        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        String world = between(session, "private void useClassicWorldParalysisSpell(",
                "private void useParalysisSpell(");
        String battle = between(session, "private void useParalysisSpell(",
                "private void useClassicWorldCurseRemovalSpell(");
        for (String handler : new String[] {world, battle}) {
            assertTrue(handler.contains("Protocol.attackSpellRequest(payload)"));
            assertTrue(handler.contains("spell.effectiveMpCost("));
            assertTrue(handler.contains("spell.warningDelayMillis()"));
            assertTrue(handler.contains("spell.paralysisDurationMillis()"));
            assertTrue(handler.contains("spell.failureMessage()"));
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
}
