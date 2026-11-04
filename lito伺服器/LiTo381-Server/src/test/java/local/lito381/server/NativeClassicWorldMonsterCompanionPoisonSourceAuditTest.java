package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Retained monster-to-controlled-NPC poison authority and current mapping. */
final class NativeClassicWorldMonsterCompanionPoisonSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/src/com/lineage/server/model/L1AttackNpc.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/L1MobSkillUse.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/poison/L1DamagePoison.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/poison/L1ParalysisPoison.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/poison/L1Poison.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/poison/L1SilencePoison.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/skillmode/CANCELLATION.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/skillmode/CURE_POISON.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/skillmode/REMOVE_CURSE.java"));
    }

    private static final Path SOURCE = Path.of("..", "1-伺服器", "src",
            "com", "lineage", "server").toAbsolutePath().normalize();
    private static final Path ATTACK = SOURCE.resolve("model/L1AttackNpc.java");
    private static final Path MOB_SKILL = SOURCE.resolve("model/L1MobSkillUse.java");
    private static final Path POISON = SOURCE.resolve("model/poison/L1Poison.java");
    private static final Path DAMAGE = SOURCE.resolve("model/poison/L1DamagePoison.java");
    private static final Path SILENCE = SOURCE.resolve("model/poison/L1SilencePoison.java");
    private static final Path PARALYSIS = SOURCE.resolve("model/poison/L1ParalysisPoison.java");
    private static final Path CANCELLATION = SOURCE.resolve(
            "model/skill/skillmode/CANCELLATION.java");
    private static final Path CURE_POISON = SOURCE.resolve(
            "model/skill/skillmode/CURE_POISON.java");
    private static final Path REMOVE_CURSE = SOURCE.resolve(
            "model/skill/skillmode/REMOVE_CURSE.java");

    @Test void retainedSourcesRemainTheReviewedAuthority() throws Exception {
        assertEquals("7BA897448013D57FD77BA05C4213E1A09E9C709CD9199271A2C14F5D2F65C3E8", sha256(ATTACK));
        assertEquals("7167D0899729DBBA7A12E1EDCEC5D8DDE0570B94DA7539D06A1417C9465C90B7", sha256(MOB_SKILL));
        assertEquals("240FC6C8817AAE970C1A084CE89AA85BC373B0819B786BBDF6C0DBD1BD775FF1", sha256(POISON));
        assertEquals("DEF36EC11CA4A03A0E690FC6F49070D94DDF8B93F29FFD30BD4C2F18A67ECEA8", sha256(DAMAGE));
        assertEquals("2E573AEA691CEC385820899DBB9C557F60375945AC8F2E487B051293384B3C24", sha256(SILENCE));
        assertEquals("7FF6F8023D81EFE9E2AEA6730575B58B595D9751340490DA503D07C78EBD3320", sha256(PARALYSIS));
        assertEquals("BF802004D6538C879EBD71FA192F93B6357CB3FA063CBD3CCBC7F79800359FB7", sha256(CANCELLATION));
        assertEquals("D6F5B05AEBE63EF31AFEE00AEDB3E8512EB8A0571EA077701654A4FB151ECDD3", sha256(CURE_POISON));
        assertEquals("BE5E1E0455CFDFE208D43616BFD135E61C4E8428D63381604B16223F0D058B2A", sha256(REMOVE_CURSE));
    }

    @Test void ordinaryAndMobPhysicalSkillBothEnterNpcPoisonDuringCalcDamage()
            throws Exception {
        String attack = normalized(ATTACK);
        int pcZero = attack.indexOf("if (dmg0(_targetPc))");
        int pcPoison = attack.indexOf("addNpcPoisonAttack(_targetPc)", pcZero);
        int npcZero = attack.indexOf("if (dmg0(_targetNpc))");
        int npcDefense = attack.indexOf("dmg -= calcNpcDamageReduction()", npcZero);
        int npcPoison = attack.indexOf("addNpcPoisonAttack(_targetNpc)", npcDefense);
        int npcFinalZero = attack.indexOf("if (dmg <= 0)", npcPoison);
        assertTrue(pcZero >= 0 && pcPoison > pcZero);
        assertTrue(npcZero >= 0 && npcDefense > npcZero
                && npcPoison > npcDefense && npcFinalZero > npcPoison);

        String skill = normalized(MOB_SKILL);
        int construct = skill.indexOf("new L1AttackNpc(_attacker, (L1Character) object)");
        int hit = skill.indexOf("if (attack.calcHit())", construct);
        int damage = skill.indexOf("attack.calcDamage()", hit);
        int action = skill.indexOf("attack.action()", damage);
        int commit = skill.indexOf("attack.commit()", action);
        assertTrue(construct >= 0 && hit > construct && damage > hit
                && action > damage && commit > action);
    }

    @Test void poisonSubtypeTargetRulesMessagesAndTimersStayExact()
            throws Exception {
        String poison = normalized(POISON);
        assertTrue(poison.contains("if (cha.getPoison() != null)"));
        assertTrue(poison.contains("if (!(cha instanceof L1PcInstance))"));

        String damage = normalized(DAMAGE);
        assertTrue(damage.contains("cha instanceof L1PcInstance"));
        assertTrue(damage.contains("cha instanceof L1MonsterInstance"));
        assertTrue(damage.contains("if (!isDamageTarget(cha) || !isValidTarget(cha))"));
        assertTrue(damage.contains("_target.setSkillEffect(STATUS_POISON, 30000)"));
        assertTrue(damage.contains("_target.setPoisonEffect(1)"));

        String silence = normalized(SILENCE);
        assertTrue(silence.contains("setSkillEffect(STATUS_POISON_SILENCE, 0)"));
        assertTrue(silence.contains("sendMessageIfPlayer(this._target, 310)"));
        assertTrue(silence.contains("sendMessageIfPlayer(this._target, 311)"));

        String paralysis = normalized(PARALYSIS);
        assertTrue(paralysis.contains("sendMessageIfPlayer(this._target, 212)"));
        assertTrue(paralysis.contains("this._target.setPoisonEffect(1)"));
        assertTrue(paralysis.contains("_target.setPoisonEffect(2)"));
        assertTrue(paralysis.contains("_target instanceof L1PcInstance"));
        assertTrue(paralysis.contains("_target.setPoisonEffect(0)"));
    }

    @Test void cureAndCurrentImplementationRetainLifecycleAndWireOrdering()
            throws Exception {
        assertTrue(normalized(CURE_POISON).contains("cha.curePoison()"));
        String remove = normalized(REMOVE_CURSE);
        assertTrue(remove.contains("cha.curePoison()"));
        assertTrue(remove.contains("cha.cureParalaysis()"));
        String cancellation = normalized(CANCELLATION);
        assertTrue(cancellation.contains("cha.curePoison()"));
        assertTrue(cancellation.contains("cha.cureParalaysis()"));

        String current = normalized(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertTrue(current.contains("applyClassicWorldMonsterPoisonToCompanion("));
        assertTrue(current.contains("controlled.activateNpcPoison("));
        assertTrue(current.contains("pollClassicWorldCompanionPoison("));
        assertTrue(current.contains("applyClassicWorldMonsterCompanionPoisonDamage("));
        assertTrue(current.contains("ignores-physical-zero=true"));
        int specialPoison = current.indexOf(
                "applyClassicWorldMonsterPoisonToCompanion(",
                current.indexOf("void serverClassicWorldMonsterPhysicalSkillAttack("));
        int specialPacket = current.indexOf(
                "Protocol.physicalDamageAttack(", specialPoison);
        assertTrue(specialPoison >= 0 && specialPacket > specialPoison);

        String companion = normalized(Path.of(
                "src/main/java/local/lito381/server/BattleCompanion.java"));
        assertTrue(companion.contains("kind == Kind.TAMED"));
        assertTrue(companion.contains("!timedStatus.paralysisActive(nowNanos)"));
        assertTrue(companion.contains("removeCurseNpcPoison("));
    }

    private static String normalized(Path path) throws Exception {
        return LocalResources.readString(path).replace("\r\n", "\n");
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(LocalResources.readAllBytes(path)));
    }
}
