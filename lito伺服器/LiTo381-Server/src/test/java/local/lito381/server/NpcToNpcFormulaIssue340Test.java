package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Issue #340 (T121): WORLD monster -> pet/summon (P3), WORLD monster -> Dspy
 * escort (P4) and arena summon -> NPC (P7) now use the 1-伺服器
 * {@code L1AttackNpc} NPC_NPC formula ({@code calcNpcHit} Lv + 3 / BOSS +30,
 * {@code calcNpcDamage} summon branch STR/2 + STRD) instead of the NPC -> PC
 * envelope (Lv + 5, STR*0.8).
 *
 * <p>The {@code ref*} helpers restate the 1-伺服器 source (D:\lineage開發\1-伺服器,
 * read-only); every {@code ThreadLocalRandom.nextInt(n)} face is enumerated.
 * Line references: {@code docs/design/formulas-physical.md} §8.</p>
 */
final class NpcToNpcFormulaIssue340Test {

    /** L1AttackList.java:517-543 STRD, keys 0..254. */
    private static final int[] REF_STRD = new int[255];
    static {
        int dmgStr = -6;
        for (int str = 0; str <= 22; str++) {
            if (str % 2 == 1) dmgStr++;
            REF_STRD[str] = dmgStr;
        }
        for (int str = 23; str <= 28; str++) {
            if (str % 3 == 2) dmgStr++;
            REF_STRD[str] = dmgStr;
        }
        for (int str = 29; str <= 32; str++) {
            if (str % 2 == 1) dmgStr++;
            REF_STRD[str] = dmgStr;
        }
        for (int str = 33; str <= 34; str++) {
            dmgStr++;
            REF_STRD[str] = dmgStr;
        }
        for (int str = 35; str <= 254; str++) {
            if (str % 4 == 1) dmgStr++;
            REF_STRD[str] = dmgStr;
        }
    }

    /** L1AttackNpc.calcNpcHit:201-250 (target skill dodge 0, no pet weapon). */
    private static int refNpcHit(int level, boolean boss, int ac, int r20, int rAc) {
        int hitRate = level + 3;
        int attackerDice = r20 + 1 + hitRate - 3;
        if (boss) attackerDice += 30;
        int defenderDice = ac >= 0 ? 10 - ac : 10 + rAc + 1;
        if (attackerDice <= hitRate) return 15;
        if (attackerDice >= hitRate + 19) return 100;
        return attackerDice > defenderDice ? 100 : 15;
    }

    /** L1AttackNpc.npcDmgMode:274-302 (dmgup 0, leverage 10). */
    private static double refMode(double dmg, int crit, boolean undeadNight,
                                  boolean boss, int stacks) {
        if (crit < 15) dmg *= 1.80;
        if (undeadNight) dmg *= 1.20;
        dmg = (int) (dmg * (10 / 10D));
        if (boss) dmg *= 1.80;
        if (stacks > 0) {
            dmg = dmg * (100 - stacks * 5) / 100.0;
            if (dmg < 1) dmg = 1;
        }
        return dmg;
    }

    /** L1AttackNpc.calcNpcDamage:409-415, non-pet branch. */
    private static double refNonPetBeforeReduction(int str, int rLvl, int crit,
                                                   boolean undeadNight, boolean boss,
                                                   int stacks) {
        double dmg = rLvl + str / 2 + REF_STRD[str];
        return refMode(dmg, crit, undeadNight, boss, stacks);
    }

    /**
     * L1AttackNpc.calcNpcDamage:417-440 with L1AttackMode.calcNpcDamageReduction:275-293
     * (no IMMUNE_TO_HARM, no DE multiplier).
     */
    private static int refNpcDamage(double beforeReduction, int targetAc,
                                    int damageReduction, int rAc) {
        int acDefMax = Math.max(0, 10 - targetAc) / 7;
        int reduction = acDefMax != 0
                ? rAc + Math.max(1, acDefMax) + damageReduction : damageReduction;
        double dmg = beforeReduction - reduction;
        if (dmg <= 0) return 0;
        return (int) dmg;
    }

    private static SourceNpcPhysicalStats target(int ac, int damageReduction) {
        return new SourceNpcPhysicalStats(
                45000, ac, false, damageReduction, false, 0, 0, 0,
                false, 0, false, false, 0, 10, 10, false);
    }

    private static final int[] LEVELS = {1, 2, 5, 13, 30, 60};
    private static final int[] STRENGTHS = {0, 1, 5, 10, 11, 12, 15, 20, 25, 33, 40, 60, 120, 254};
    private static final int[] STACKS = {0, 1, 5, 10};

    @Test
    void npcToNpcHitRateMatchesCalcNpcHitForEveryFace() {
        for (int level : LEVELS) {
            for (int ac = -25; ac <= 20; ac++) {
                for (boolean boss : new boolean[] {false, true}) {
                    for (int r20 = 0; r20 < 20; r20++) {
                        int faces = ac < 0 ? -ac : 1;
                        for (int rAc = 0; rAc < faces; rAc++) {
                            int negativeAcRoll = ac < 0 ? rAc + 1 : 0;
                            int expected = refNpcHit(level, boss, ac, r20, rAc);
                            assertEquals(expected,
                                    ClassicWorldCompanionNpcPhysicalAttack.hitRate(
                                            level, 0, boss, ac, r20 + 1, negativeAcRoll),
                                    "level=" + level + " ac=" + ac + " boss=" + boss
                                            + " r20=" + r20 + " rAc=" + rAc);
                            assertEquals(expected,
                                    ClientSession.companionNpcTargetHitChance(
                                            level, boss, ac, r20 + 1, negativeAcRoll));
                        }
                    }
                }
            }
        }
    }

    @Test
    void nonPetDamagePrefixMatchesCalcNpcDamageSummonBranchForEveryFace() {
        for (int level : LEVELS) {
            for (int str : STRENGTHS) {
                for (int rLvl = 0; rLvl < level; rLvl++) {
                    for (int crit = 0; crit < 100; crit++) {
                        for (boolean night : new boolean[] {false, true}) {
                            for (boolean boss : new boolean[] {false, true}) {
                                for (int stacks : STACKS) {
                                    double expected = refNonPetBeforeReduction(
                                            str, rLvl, crit, night, boss, stacks);
                                    double actual = ClassicWorldCompanionNpcPhysicalAttack
                                            .nonPetBeforeReduction(level, str, REF_STRD[str],
                                                    rLvl, crit, 0, night, boss, 10, stacks);
                                    assertEquals(expected, actual, 0.0,
                                            "level=" + level + " str=" + str + " rLvl=" + rLvl
                                                    + " crit=" + crit + " night=" + night
                                                    + " boss=" + boss + " stacks=" + stacks);
                                    assertEquals(Math.max(0, (int) expected),
                                            ClientSession.companionNpcTargetDamage(
                                                    level, str, rLvl, crit, night, boss, stacks));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * P3/P4 pipeline: prefix without stacks -> NativeNpcWeaponBreakDamage.afterStacks ->
     * BattlePhysicalDefense.againstNpc equals calcNpcDamage end to end.
     */
    @Test
    void worldMonsterToNpcPipelineMatchesCalcNpcDamageEndToEnd() {
        int[] targetAcs = {10, 3, -4, -15, -40};
        int[] reductions = {0, 2, 7};
        for (int level : new int[] {1, 5, 30}) {
            for (int str : new int[] {0, 7, 15, 40}) {
                for (int rLvl = 0; rLvl < level; rLvl++) {
                    for (int crit : new int[] {0, 14, 15, 99}) {
                        for (boolean boss : new boolean[] {false, true}) {
                            for (int stacks : STACKS) {
                                double prefix = ClassicWorldCompanionNpcPhysicalAttack
                                        .nonPetBeforeReduction(level, str,
                                                CombatNpcPhysicalSource.npcStrengthDamage(str),
                                                rLvl, crit, 0, false, boss, 10, 0);
                                int raw = NativeNpcWeaponBreakDamage.afterStacks(prefix, stacks);
                                double refBefore = refNonPetBeforeReduction(
                                        str, rLvl, crit, false, boss, stacks);
                                for (int ac : targetAcs) {
                                    for (int dr : reductions) {
                                        SourceNpcPhysicalStats t = target(ac, dr);
                                        int acMax = BattlePhysicalDefense.npcAcMaximum(t);
                                        for (int rAc = 0; rAc < Math.max(1, acMax); rAc++) {
                                            int acRoll = acMax == 0 ? 0 : rAc;
                                            assertEquals(refNpcDamage(refBefore, ac, dr, acRoll),
                                                    BattlePhysicalDefense.againstNpc(
                                                            raw, t, acRoll, false, false).damage(),
                                                    "level=" + level + " str=" + str
                                                            + " rLvl=" + rLvl + " crit=" + crit
                                                            + " boss=" + boss + " stacks=" + stacks
                                                            + " ac=" + ac + " dr=" + dr
                                                            + " rAc=" + rAc);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** The shared npcDmgMode refactor keeps the P6 pet/summon path unchanged. */
    @Test
    void companionDamageSummonBranchEqualsNonPetPrefixMinusReduction() {
        SourceNpcPhysicalStats t = target(-4, 1);
        for (int str : new int[] {0, 9, 15, 30}) {
            for (int rLvl = 0; rLvl < 12; rLvl++) {
                for (int crit = 0; crit < 100; crit++) {
                    ClassicWorldCompanionNpcPhysicalAttack.DamageOutcome outcome =
                            ClassicWorldCompanionNpcPhysicalAttack.damage(
                                    BattleCompanion.Kind.SUMMON, 12, 12, str,
                                    CombatNpcPhysicalSource.npcStrengthDamage(str),
                                    rLvl, crit, 0, 0, 0, false, 10, 2, t, 1, 1, 0L);
                    double prefix = ClassicWorldCompanionNpcPhysicalAttack.nonPetBeforeReduction(
                            12, str, CombatNpcPhysicalSource.npcStrengthDamage(str),
                            rLvl, crit, 0, false, false, 10, 2);
                    assertEquals(prefix, outcome.beforeTargetReduction(), 0.0);
                    assertEquals(refNpcDamage(refNonPetBeforeReduction(str, rLvl, crit,
                            false, false, 2), -4, 1, 1), outcome.damage());
                }
            }
        }
    }

    /** Balance anchor for the report: arena summon Lv 5 / STR 15 striking an NPC. */
    @Test
    void arenaSummonAnchorUsesStrOverTwoInsteadOfStrTimesPointEight() {
        // nextInt(5)=0, no critical: 0 + 15/2 + STRD(15)=2 -> 9 (was 0 + 12 + 2 = 14).
        assertEquals(9, ClientSession.companionNpcTargetDamage(5, 15, 0, 99, false, false, 0));
        // critical: (int)(9 * 1.8) = 16 (was (int)(14 * 1.8) = 25).
        assertEquals(16, ClientSession.companionNpcTargetDamage(5, 15, 0, 0, false, false, 0));
        // Lv 5 vs AC 10: hit rate 8, d20=1 -> attackerDice 6 <= 8 -> 15 (fumble).
        assertEquals(15, ClientSession.companionNpcTargetHitChance(5, false, 10, 1, 0));
        // BOSS +30 is applied before the defender comparison in calcNpcHit.
        assertEquals(100, ClientSession.companionNpcTargetHitChance(5, true, -30, 1, 30));
    }

    @Test
    void undeadNightUsesSourceUndeadTypesOnly() {
        long night = 0L;
        // Pick a time that the retained clock calls night.
        for (long t = System.currentTimeMillis(); ; t += 60_000L) {
            if (ClassicWorldNpcDamage.retainedNight(t)) { night = t; break; }
        }
        for (int undead = 0; undead <= 5; undead++) {
            boolean expected = undead == 1 || undead == 3 || undead == 4;
            assertEquals(expected,
                    ClassicWorldCompanionNpcPhysicalAttack.undeadNight(undead, night),
                    "undead=" + undead);
        }
    }

    @Test
    void nonPetPrefixRejectsOutOfRangeRolls() {
        assertThrows(IllegalArgumentException.class, () ->
                ClassicWorldCompanionNpcPhysicalAttack.nonPetBeforeReduction(
                        5, 15, 2, 5, 0, 0, false, false, 10, 0));
        assertThrows(IllegalArgumentException.class, () ->
                ClassicWorldCompanionNpcPhysicalAttack.nonPetBeforeReduction(
                        5, 15, -7, 0, 0, 0, false, false, 10, 0));
        assertThrows(IllegalArgumentException.class, () ->
                ClassicWorldCompanionNpcPhysicalAttack.nonPetBeforeReduction(
                        5, 15, 2, 0, 100, 0, false, false, 10, 0));
    }
}
