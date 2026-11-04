package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skills 20/40/103 and their ordinary-WORLD DARKNESS state. */
final class NativeWorldBlindSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeIdaClient(LocalResources.IDA_DISASSEMBLY);
    }

    private static final Path RETAINED = Path.of(
            "../1-伺服器/src/com/lineage/server");

    @Test
    void nativeDispatcherSelectedRowsAndRetainedModeStayExact()
            throws Exception {
        String database = Files.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (20, 0, '闇盲咒術', 3, 3, 20, 0, 0, 0, 500, 16, 'buff', 3, 0, 0, 0, 0, 11, 0, 4, -10, 1, 0, 0, 8, '$1449', 19, 746, 0, 0, 0, 280);"));
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (40, 0, '黑闇之影', 5, 7, 15, 0, 40318, 1, 500, 32, 'buff', 3, 0, 0, 0, 0, 11, 0, 1, -10, 3, 0, 0, 128, '$1983', 19, 2175, 0, 0, 0, 280);"));
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (103, 0, '暗黑盲咒', 13, 6, 20, 0, 40321, 1, 990, 64, 'buff', 3, 0, 0, 0, 0, 0, 0, 4, -10, 0, 0, 0, 64, '$2510', 19, 2947, 0, 147, 148, 280);"));
        for (int skillId : new int[] {20, 40, 103}) {
            assertTrue(database.contains(
                    "INSERT INTO `skills_負面技能控制` VALUES (" + skillId));
            assertTrue(database.contains(
                    "INSERT INTO `skills_負面boss免疫` VALUES (" + skillId));
        }

        String assembly = Files.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        int dispatch = assembly.indexOf("loc_4CB938:");
        String nativeCases = assembly.substring(dispatch, dispatch + 420);
        assertTrue(nativeCases.contains(
                "jumptable 004CB66E cases 10,19,27,39,46,55,63,70,102"));
        assertTrue(nativeCases.contains("mov     ecx, [ebp+arg_4]"));
        assertTrue(nativeCases.contains("mov     edx, 3"));
        assertTrue(nativeCases.contains("mov     [edi+68h], esi"));
        assertTrue(nativeCases.contains("call    sub_4CA6E0"));
        String producer = assembly.substring(
                assembly.indexOf("sub_4CA6E0 proc near"),
                assembly.indexOf("sub_4CA6E0 endp"));
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("offset aCccdhh"));

        String skillMode = Files.readString(
                RETAINED.resolve("model/skill/L1SkillMode.java"),
                StandardCharsets.UTF_8);
        assertTrue(skillMode.contains(
                "_skillMode.put(CURSE_BLIND, new CURSE_BLIND())"));
        assertTrue(skillMode.contains(
                "_skillMode.put(DARKNESS, new CURSE_BLIND())"));
        assertTrue(skillMode.contains(
                "_skillMode.put(DARK_BLIND, new CURSE_BLIND())"));
        String blindMode = Files.readString(RETAINED.resolve(
                "model/skill/skillmode/CURSE_BLIND.java"),
                StandardCharsets.UTF_8);
        assertTrue(blindMode.contains("new S_CurseBlind(2)"));
        assertTrue(blindMode.contains("new S_CurseBlind(1)"));
        assertTrue(blindMode.contains(
                "cha.setSkillEffect(DARKNESS, integer * 1000)"));

        String skillUse = Files.readString(
                RETAINED.resolve("model/skill/L1SkillUse.java"),
                StandardCharsets.UTF_8);
        int barrier = skillUse.indexOf("if (pc.hasSkillEffect(ABSOLUTE_BARRIER))");
        String barrierCases = skillUse.substring(barrier, barrier + 1_100);
        assertTrue(barrierCases.contains("case CURSE_BLIND:"));
        assertTrue(barrierCases.contains("case DARKNESS:"));
        assertTrue(!barrierCases.contains("case DARK_BLIND:"));
    }

    @Test
    void worldDispatcherStatusMovementCancellationAndPacketsStayConnected()
            throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int worldStart = session.indexOf("if (phase == Phase.WORLD) {");
        int dispatch = session.indexOf(
                "BattleSpellCatalog.BlindSpell worldBlind", worldStart);
        int unsupported = session.indexOf(
                "rejected unsupported classic-world skill", dispatch);
        assertTrue(dispatch > worldStart && unsupported > dispatch);
        assertTrue(session.substring(dispatch, unsupported).contains(
                "useClassicWorldBlindSpell(payload, worldBlind)"));

        int methodStart = session.indexOf(
                "private void useClassicWorldBlindSpell(");
        int methodEnd = session.indexOf(
                "private void useBlindSpell(", methodStart);
        String method = session.substring(methodStart, methodEnd);
        assertTrue(method.contains("admitClassicWorldSpellFamily("));
        assertTrue(method.contains("BattleAbsoluteBarrierRules.blocksIncomingSpell("));
        assertTrue(method.contains("spell.probabilityForTarget("));
        assertTrue(method.contains("totalBlindResistance()"));
        assertTrue(method.contains("config.darkBlindFixtureRoll()"));
        assertTrue(method.contains(
                "spell.skillId() == BattleSpellCatalog.DARKNESS"));
        assertTrue(method.contains("cancelAbsoluteBarrier("));
        assertTrue(method.contains("revealInvisibility("));
        assertTrue(method.contains("consumeEraseMagicAfterProbability("));
        assertTrue(method.contains("consumeCounterMagic(castNow)"));
        assertTrue(method.contains("hub.blindClassicWorldMonster("));
        assertTrue(method.contains("Protocol.blind(1)"));
        assertTrue(method.contains("Protocol.effectObject("));
        assertTrue(method.contains("Protocol.messageCode(spell.failureMessage())"));
        assertTrue(!method.contains("provokeClassicWorldMonster("));

        int battleStart = session.indexOf("private void useBlindSpell(");
        int battleEnd = session.indexOf(
                "private void useClassicWorldSilenceSpell(", battleStart);
        String battle = session.substring(battleStart, battleEnd);
        assertTrue(battle.contains(
                "spell.skillId() == BattleSpellCatalog.DARKNESS"));
        assertTrue(battle.contains("cancelAbsoluteBarrier("));
        assertTrue(battle.contains("revealInvisibility("));

        String population = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClassicWorldMonsterPopulation.java"),
                StandardCharsets.UTF_8);
        assertTrue(population.contains("synchronized boolean blind("));
        assertTrue(population.contains(
                "actor.damagePoisonStatus.activateBlind(nowNanos, durationSeconds)"));
        assertTrue(population.contains(
                "|| actor.damagePoisonStatus.blindActive(nowNanos)"));
        assertTrue(population.contains(
                "target, haste, slow, timed.blind(),"));
        assertTrue(session.contains(
                "monsterCancellation != null && monsterCancellation.blind()"));
    }
}
