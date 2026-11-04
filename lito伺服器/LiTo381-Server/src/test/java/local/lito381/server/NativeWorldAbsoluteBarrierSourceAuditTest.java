package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill78 and its ordinary-WORLD player protection lane. */
final class NativeWorldAbsoluteBarrierSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeIdaClient();
    }

    private static final Path RETAINED = Path.of(
            "../1-伺服器/src/com/lineage/server");

    @Test
    void nativeProducerDatabaseAndRetainedRulesStayExact() throws Exception {
        String database = Files.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (78, 0, '絕對屏障', 10, 5, 30, 0, 40318, 2, 12000, 12, 'none', 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 32, '$1992', 19, 2234, 0, 0, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (78, '絕對屏障', NULL, NULL);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_相消免疫技能` VALUES (78, '絕對屏障');"));

        String assembly = Files.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        int routeStart = assembly.indexOf("loc_4CBBD4:");
        String route = assembly.substring(routeStart, routeStart + 800);
        assertTrue(route.contains("cases 1,2,12,13,30,31,48,51,53,71,77"));
        assertTrue(route.contains("push    14h"));
        assertTrue(route.contains("call    sub_4C6AE0"));
        assertTrue(assembly.contains("off_5028CC dd offset byte_636363"));
        assertTrue(assembly.contains("byte_636363 db 0"));

        String skillUse = Files.readString(
                RETAINED.resolve("model/skill/L1SkillUse.java"),
                StandardCharsets.UTF_8);
        assertTrue(skillUse.contains("if (pc.hasSkillEffect(ABSOLUTE_BARRIER))"));
        assertTrue(skillUse.contains("pc.stopHpRegeneration()"));
        assertTrue(skillUse.contains("pc.stopMpRegeneration()"));
        assertTrue(skillUse.contains("_player.killSkillEffectTimer(ABSOLUTE_BARRIER)"));
        assertTrue(skillUse.contains("case CANCELLATION:"));
        assertTrue(skillUse.contains("case COUNTER_DETECTION:"));
        int whitelistStart = skillUse.indexOf(
                "if (pc.hasSkillEffect(ABSOLUTE_BARRIER))");
        int whitelistEnd = skillUse.indexOf("// 目標在隱身狀態", whitelistStart);
        String whitelist = skillUse.substring(whitelistStart, whitelistEnd);
        assertFalse(whitelist.contains("case TRIPLE_ARROW:"));
        assertFalse(whitelist.contains("case IRON_SKIN:"));

        String character = Files.readString(
                RETAINED.resolve("model/L1Character.java"),
                StandardCharsets.UTF_8);
        assertTrue(character.contains("activeSkillIds.contains(ABSOLUTE_BARRIER)"));
    }

    @Test
    void worldHandlerCastsBroadcastsAndProtectsWorldPlayers() throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int castStart = session.indexOf("private void useAbsoluteBarrierSpell(");
        int castEnd = session.indexOf("private void useAdvanceSpiritSpell(", castStart);
        assertTrue(castStart > 0 && castEnd > castStart);
        String cast = session.substring(castStart, castEnd);
        assertTrue(cast.contains("phase != Phase.BATTLE && phase != Phase.WORLD"));
        assertTrue(cast.contains("classicWorldMembers(classicWorldMapId)"));
        assertTrue(cast.contains("sameClassicWorldMap(member)"));
        assertTrue(cast.contains("isInNativeScreen("));
        assertTrue(cast.contains("timedStatus.activateAbsoluteBarrier("));
        assertTrue(cast.contains("revealInvisibility(\"spell-\" + spell.skillId())"));
        assertFalse(cast.contains("List<ClientSession> recipients = hub.roomMembers"));

        int dispatchStart = session.indexOf("if (phase == Phase.WORLD) {");
        int dispatchEnd = session.indexOf(
                "BattleSpellCatalog.CounterMagicSpell worldCounterMagic", dispatchStart);
        assertTrue(dispatchStart > 0 && dispatchEnd > dispatchStart);
        String dispatch = session.substring(dispatchStart, dispatchEnd);
        assertTrue(dispatch.contains("worldAbsoluteBarrier"));
        assertTrue(dispatch.contains("absoluteBarrierBlocksSpellTarget("));
        assertTrue(dispatch.contains("useAbsoluteBarrierSpell(payload, worldAbsoluteBarrier)"));
        assertTrue(dispatch.contains("learnedSkills.contains(selector.skillId())"));

        int gateStart = session.indexOf("private boolean absoluteBarrierBlocksSpellTarget(");
        int gateEnd = session.indexOf("private int npcEarthBindProtectedCombatDamage(", gateStart);
        assertTrue(gateStart > 0 && gateEnd > gateStart);
        String gate = session.substring(gateStart, gateEnd);
        assertTrue(gate.contains("classicWorldMembers(classicWorldMapId)"));
        assertTrue(gate.contains("sameClassicWorldMap(candidate)"));
        assertTrue(gate.contains("hub.roomCombatants(currentRoomId)"));

        int monsterMagicStart = session.indexOf(
                "void serverClassicWorldMonsterMagicAttack(");
        int monsterMagicEnd = session.indexOf(
                "void serverClassicWorldMonsterStatusSkillAttack(", monsterMagicStart);
        String monsterMagic = session.substring(monsterMagicStart, monsterMagicEnd);
        assertTrue(monsterMagic.contains("earthBindProtectedCombatDamage("));

        int monsterPhysicalStart = session.indexOf(
                "private ClassicWorldMonsterPhysicalHit classicWorldMonsterPhysicalSkillHit(");
        int monsterPhysicalEnd = session.indexOf(
                "private ClassicWorldMonsterPhysicalTarget", monsterPhysicalStart);
        String monsterPhysical = session.substring(monsterPhysicalStart, monsterPhysicalEnd);
        assertTrue(monsterPhysical.contains("earthBindProtectedCombatDamage("));
    }
}
