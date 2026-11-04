package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skills 47/56 and their ordinary-WORLD retained status flow. */
final class NativeWorldWeaknessSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"));
        LocalResources.assumeIdaClient(
                Path.of("../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
    }

    private static final Path RETAINED = Path.of(
            "../1-伺服器/src/com/lineage/server");

    @Test
    void nativeDispatcherSelectedRowsAndBarrierRulesStayExact()
            throws Exception {
        String database = LocalResources.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (47, 0, '弱化術', 6, 6, 10, 0, 40318, 1, 500, 64, 'buff', 3, 0, 0, 0, 0, 12, 0, 4, -10, 3, 0, 0, 64, '$1984', 19, 2228, 0, 692, 0, 280);"));
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (56, 0, '疾病術', 7, 7, 10, 0, 40318, 1, 500, 64, 'buff', 3, 0, 0, 0, 0, 10, 0, 4, -10, 3, 0, 0, 128, '$1987', 19, 2230, 0, 0, 0, 280);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_負面boss免疫` VALUES (47, '弱化術', 5);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_負面boss免疫` VALUES (56, '疾病術', 5);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (47, '弱化術', NULL, NULL);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (56, '疾病術', NULL, NULL);"));

        String assembly = LocalResources.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        int dispatch = assembly.indexOf("loc_4CB938:");
        String nativeCases = assembly.substring(dispatch, dispatch + 420);
        assertTrue(nativeCases.contains(
                "jumptable 004CB66E cases 10,19,27,39,46,55,63,70,102"));
        assertTrue(nativeCases.contains("mov     edx, 3"));
        assertTrue(nativeCases.contains("call    sub_4CA6E0"));
        String producer = assembly.substring(
                assembly.indexOf("sub_4CA6E0 proc near"),
                assembly.indexOf("sub_4CA6E0 endp"));
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("offset aCccdhh"));

        String skillUse = LocalResources.readString(
                RETAINED.resolve("model/skill/L1SkillUse.java"),
                StandardCharsets.UTF_8);
        int barrier = skillUse.indexOf("if (pc.hasSkillEffect(ABSOLUTE_BARRIER))");
        String barrierCases = skillUse.substring(barrier, barrier + 1_100);
        assertTrue(barrierCases.contains("case WEAKNESS:"));
        assertTrue(barrierCases.contains("case DISEASE:"));
        assertTrue(skillUse.contains("this._skillId == WEAKNESS"));
        assertTrue(skillUse.contains("this._skillId == DISEASE"));
    }

    @Test
    void worldDispatcherProbabilityStatusCancellationAndPacketsStayConnected()
            throws Exception {
        String session = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int worldStart = session.indexOf("if (phase == Phase.WORLD) {");
        int dispatch = session.indexOf(
                "BattleSpellCatalog.WeaknessSpell worldWeakness", worldStart);
        int unsupported = session.indexOf(
                "rejected unsupported classic-world skill", dispatch);
        assertTrue(dispatch > worldStart && unsupported > dispatch);
        assertTrue(session.substring(dispatch, unsupported).contains(
                "useClassicWorldWeaknessSpell(payload, worldWeakness)"));

        int methodStart = session.indexOf(
                "private void useClassicWorldWeaknessSpell(");
        int methodEnd = session.indexOf(
                "private void useWeaknessSpell(", methodStart);
        String method = session.substring(methodStart, methodEnd);
        assertTrue(method.contains("admitClassicWorldSpellFamily("));
        assertTrue(method.contains("BattleAbsoluteBarrierRules.blocksIncomingSpell("));
        assertTrue(method.contains("spell.probability("));
        assertTrue(method.contains("config.weaknessFixtureDieRoll()"));
        assertTrue(method.contains("config.weaknessFixturePercentileRoll()"));
        assertTrue(method.contains("consumeEraseMagicAfterProbability("));
        assertTrue(method.contains("consumeCounterMagic(castNow)"));
        assertTrue(method.contains("hub.weaknessClassicWorldMonster("));
        assertTrue(method.contains("hub.diseaseClassicWorldMonster("));
        assertTrue(method.contains("hub.provokeClassicWorldMonster("));
        assertTrue(method.contains("Protocol.effectObject("));
        assertTrue(method.contains("Protocol.messageCode(spell.failureMessage())"));
        assertCleanupAfterCommit(method);

        int battleStart = session.indexOf(
                "private void useWeaknessSpell(byte[] payload,");
        int battleEnd = session.indexOf(
                "/** Exact selected Earth Bind path", battleStart);
        assertTrue(battleStart >= 0 && battleEnd > battleStart);
        assertCleanupAfterCommit(session.substring(battleStart, battleEnd));

        String population = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/ClassicWorldMonsterPopulation.java"),
                StandardCharsets.UTF_8);
        assertTrue(population.contains("synchronized boolean weakness("));
        assertTrue(population.contains("synchronized boolean disease("));
        assertTrue(population.contains(
                "actor.damagePoisonStatus.activateWeakness(nowNanos, durationSeconds)"));
        assertTrue(population.contains(
                "actor.damagePoisonStatus.activateDisease(nowNanos, durationSeconds)"));
        assertTrue(population.contains(
                "target, haste, slow, timed.blind(),"));
        assertTrue(session.contains(
                "monsterCancellation != null && monsterCancellation.weakness()"));
        assertTrue(session.contains(
                "monsterCancellation != null && monsterCancellation.disease()"));
    }

    private static void assertCleanupAfterCommit(String method) {
        int commit = method.indexOf("spellReadyNanos.put(spell.skillId()");
        int meditation = method.indexOf("cancelMeditation(", commit);
        int barrier = method.indexOf("cancelAbsoluteBarrier(", meditation);
        int invisibility = method.indexOf("revealInvisibility(", barrier);
        assertTrue(commit >= 0 && meditation > commit
                && barrier > meditation && invisibility > barrier);
        assertTrue(!method.contains("commitSpellReady(spell.skillId()"));
    }
}
