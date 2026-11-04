package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill66 and the ordinary-WORLD sleep/wake lifecycle. */
final class NativeWorldFogSleepingSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeIdaClient(LocalResources.IDA_DISASSEMBLY);
    }

    private static final Path RETAINED = Path.of(
            "../1-伺服器/src/com/lineage/server");

    @Test
    void nativeProducerDatabaseAndRetainedSleepRulesStayExact()
            throws Exception {
        String database = Files.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (66, 0, '沉睡之霧', 9, 1, 50, 0, 40318, 1, 1500, 32, 'buff', 3, 0, 0, 0, 0, 7, 4, 4, -50, 10, 3, 0, 2, '$1477', 19, 760, 0, 0, 0, 280);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_負面技能控制` VALUES (66, '沉睡之霧-睡眠耐性', 50, 42, 34, 0, 0, 0.35, 3, 0, 1, 0.05, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_負面boss免疫` VALUES (66, '沉睡之霧', 0);"));

        String assembly = Files.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        String dispatch = between(assembly, "loc_4CB989:", "loc_4CB9A3:");
        assertTrue(dispatch.contains("cases 14,15,33,65,75"));
        assertTrue(dispatch.contains("mov     edx, 0Ah"));
        assertTrue(dispatch.contains("call    sub_4CA6E0"));
        String producer = between(
                assembly, "sub_4CA6E0 proc near", "sub_4CA6E0 endp");
        assertTrue(producer.contains("push    offset aCccdhh"));

        String skillUse = retained("model/skill/L1SkillUse.java");
        assertTrue(skillUse.contains("this._skillId != FOG_OF_SLEEPING"));
        assertTrue(skillUse.contains("new S_ServerMessage(297)"));
        assertTrue(skillUse.contains(
                "new S_Paralysis(S_Paralysis.TYPE_SLEEP, true)"));
        assertTrue(skillUse.contains("cha.setSleeped(true)"));
        String skillStop = retained("model/skill/L1SkillStop.java");
        assertTrue(skillStop.contains("case FOG_OF_SLEEPING"));
        assertTrue(skillStop.contains("cha.setSleeped(false)"));
        assertTrue(skillStop.contains(
                "new S_Paralysis(S_Paralysis.TYPE_SLEEP, false)"));
        String incoming = retained("model/combat/PcIncomingHitCommitPlan.java");
        assertTrue(incoming.contains("boolean positiveHit"));
        assertTrue(incoming.contains("fogOfSleepingLease"));
        assertTrue(incoming.contains("sleep removal"));
    }

    @Test
    void worldDispatcherAreaActorsAndWakeLifecycleStayConnected()
            throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        String dispatch = between(session,
                "BattleSpellCatalog.MassSlowSpell worldMassSlow",
                "BattleSpellCatalog.PoisonSpell worldPoison");
        assertTrue(dispatch.contains(
                "BattleSpellCatalog.SleepSpell worldSleep"));
        assertTrue(dispatch.contains(
                "BattleSpellCatalog.sleepSpell(selector.skillId())"));
        assertTrue(dispatch.contains(
                "useClassicWorldSleepSpell(payload, worldSleep)"));

        String handler = between(session,
                "private void useClassicWorldSleepSpell(",
                "private void useSleepSpell(");
        assertTrue(handler.contains("Protocol.attackSpellRequest(payload)"));
        assertTrue(handler.contains("phase != Phase.WORLD"));
        assertTrue(handler.contains("phantasmTargets("));
        assertTrue(handler.contains(
                "spell.maximumRange() + spell.areaRadius()"));
        assertTrue(handler.contains("classicWorldMembers(originMapId)"));
        assertTrue(handler.contains("owner.ownedCompanion(identity.objectId())"));
        assertTrue(handler.contains("hub.classicWorldMonster("));
        assertTrue(handler.contains("positionX != originX"));
        assertTrue(handler.contains("currentPrimary.kind() == primary.kind()"));
        assertTrue(handler.contains("totalSleepResistance()"));
        assertTrue(handler.contains("sleepFixturePercentileRoll()"));
        assertTrue(handler.contains("consumeCounterMagic(castNow)"));
        assertTrue(handler.contains("companion.activateSleep("));
        assertTrue(handler.contains("hub.sleepClassicWorldMonster("));
        assertTrue(handler.contains(
                "Protocol.paralysis(Protocol.ParalysisMode.SLEEP_APPLY)"));
        assertTrue(handler.contains("classic-world-sleep-resisted"));
        assertTrue(handler.contains("classic-world-sleep-failure"));

        String companion = Files.readString(Path.of(
                "src/main/java/local/lito381/server/BattleCompanion.java"),
                StandardCharsets.UTF_8);
        assertTrue(companion.contains("timedStatus.sleepActive(nowNanos)"));
        assertTrue(companion.contains("timedStatus.cancelSleep()"));
        String population = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClassicWorldMonsterPopulation.java"),
                StandardCharsets.UTF_8);
        assertTrue(population.contains(
                "actor.damagePoisonStatus.activateSleep("));
        assertTrue(population.contains("actor.damagePoisonStatus.cancelSleep()"));
        assertTrue(population.contains("actor.damagePoisonStatus.sleepActive(nowNanos)"));
        assertTrue(session.contains(
                "timedStatus.expireSleep(nowNanos) && (inBattle || inClassicWorld)"));
        assertTrue(session.contains("cancelSleep(playerHp == 0"));
        assertTrue(session.contains("sleepActiveBeforeDamage"));
        assertTrue(session.contains("sleepActiveAfterDamage"));
        assertTrue(session.contains("sleep-before="));
        assertTrue(session.contains("sleep-after="));

        String config = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ServerConfig.java"),
                StandardCharsets.UTF_8);
        assertTrue(config.contains("int sleepFixturePercentileRoll"));
        assertTrue(config.contains("sleep.fixture.percentile.roll"));
        String acceptanceConfig = Files.readString(Path.of(
                "src/test/resources/server-config/root/server.world-fog-sleeping-selftest.properties"),
                StandardCharsets.UTF_8);
        assertTrue(acceptanceConfig.contains("database.read.only=true"));
        assertTrue(acceptanceConfig.contains("sleep.spell.fixture.enabled=true"));
        assertTrue(acceptanceConfig.contains("sleep.fixture.percentile.roll=1"));
    }

    private static String retained(String relative) throws Exception {
        return Files.readString(RETAINED.resolve(relative).normalize(),
                StandardCharsets.UTF_8);
    }

    private static String between(String source, String start, String end) {
        int begin = source.indexOf(start);
        int finish = source.indexOf(end, begin);
        assertTrue(begin >= 0, start);
        assertTrue(finish > begin, end);
        return source.substring(begin, finish);
    }
}
