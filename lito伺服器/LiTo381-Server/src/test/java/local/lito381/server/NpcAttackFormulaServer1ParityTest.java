package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Calendar;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

/**
 * Issue #317 (T112): LiTo NPC / pet / summon physical attack formulas versus
 * the 1-伺服器 reference {@code L1AttackNpc} (D:\lineage開發\1-伺服器, read-only).
 *
 * <p>Every {@code ref*} helper restates the 1-伺服器 formula in its own words
 * (expected value); the LiTo production helper is the actual value.  Random
 * draws are fixed by enumerating every face of each reference
 * {@code ThreadLocalRandom.nextInt(n)} call.  Line references are listed in
 * {@code docs/design/formulas-physical.md} §8.</p>
 */
final class NpcAttackFormulaServer1ParityTest {

    // ---------------------------------------------------------------- 1-伺服器 reference

    /** L1AttackList.java:517-543 力量傷害補正 STRD, keys 0..254. */
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

    /** L1AttackNpc.calcPcHit:126-171 (target dodge 0, no evasion). */
    private static int refPcHit(int level, int hitUp, int ac, int r20, int rAc) {
        int hitRate = level + 5 + hitUp;
        int attackerDice = r20 + 1 + hitRate - 3;
        int defenderDice = ac >= 0 ? 10 - ac : 10 + rAc + 1;
        int fumble = hitRate;
        int critical = hitRate + 19;
        if (attackerDice <= fumble) return 15;
        if (attackerDice >= critical) return 100;
        return attackerDice > defenderDice ? 100 : 15;
    }

    /** L1AttackNpc.calcNpcHit:201-250 (target dodge 0). */
    private static int refNpcHit(int level, int hitUp, int petHit, boolean boss,
                                 int ac, int r20, int rAc) {
        int hitRate = level + 3 + petHit + hitUp;
        int attackerDice = r20 + 1 + hitRate - 3;
        if (boss) attackerDice += 30;
        int defenderDice = ac >= 0 ? 10 - ac : 10 + rAc + 1;
        if (attackerDice <= hitRate) return 15;
        if (attackerDice >= hitRate + 19) return 100;
        return attackerDice > defenderDice ? 100 : 15;
    }

    /** L1AttackNpc.npcDmgMode:274-302 with the nextInt(100) face {@code crit}. */
    private static double refMode(double dmg, int crit, int dmgUp, boolean undeadNight,
                                  int leverage, boolean boss, int stacks) {
        if (crit < 15) dmg *= 1.80;
        dmg += dmgUp;
        if (undeadNight) dmg *= 1.20;
        dmg = (int) (dmg * (leverage / 10D));
        if (boss) dmg *= 1.80;
        if (stacks > 0) {
            dmg = dmg * (100 - stacks * 5) / 100.0;
            if (dmg < 1) dmg = 1;
        }
        return dmg;
    }

    /** L1AttackNpc.calcPcDamage:318-327 base. */
    private static double refPcBase(int lvl, int str, int rLvl, boolean pet, int petWeapon) {
        double dmg = rLvl + (str * 0.8) + REF_STRD[str];
        if (pet) {
            dmg += (lvl / 7);
            dmg += petWeapon;
        }
        return dmg;
    }

    /** L1*ClassFeature.getAcDefenseMax. */
    private static int refAcDefenseMax(int type, int ac) {
        return switch (type) {
            case 0, 2, 5 -> ac / 3;
            case 1 -> ac >> 1;
            case 3 -> ac / 5;
            case 4, 6 -> ac >> 2;
            default -> throw new AssertionError(type);
        };
    }

    /** L1AttackMode.calcPcDefense:250-269 with the nextInt(acDefMax) face {@code r}. */
    private static int refPcDefense(int type, int targetAc, int r) {
        int ac = Math.max(0, 10 - targetAc);
        int max = refAcDefenseMax(type, ac);
        if (max == 0) return 0;
        return r + Math.max(1, max >> 3);
    }

    /** L1AttackMode.calcNpcDamageReduction:275-293 with the nextInt(acDefMax) face {@code r}. */
    private static int refNpcReduction(int npcAc, int damageReduction, int r) {
        int ac = Math.max(0, 10 - npcAc);
        int max = ac / 7;
        if (max == 0) return damageReduction;
        return r + Math.max(1, max) + damageReduction;
    }

    /** L1AttackNpc.calcNpcDamage:400-440 (no poison, no doll). */
    private static int refNpcDamage(boolean pet, int lvl, int templateLvl, int str, int rSrc,
                                    int crit, int petWeapon, int dmgUp, int leverage,
                                    boolean boss, int stacks, int npcAc, int dr, int rAc,
                                    int immune) {
        double dmg;
        if (pet) {
            dmg = rSrc + (str / 2) + 1;
            dmg += (lvl / 14);
            dmg += petWeapon;
        } else {
            dmg = rSrc + str / 2 + REF_STRD[str];
        }
        dmg = refMode(dmg, crit, dmgUp, false, leverage, boss, stacks);
        dmg -= refNpcReduction(npcAc, dr, rAc);
        dmg /= immune;
        if (dmg <= 0) dmg = 0.0;
        return (int) dmg;
    }

    /** L1GameTime.valueOf:30-38 + isNight:66-69 (UTC calendar). */
    private static boolean refNight(long millis) {
        long t1 = millis - 1_735_689_600_000L;
        int t2 = (int) ((t1 * 6) / 1000L);
        int t3 = t2 % 3;
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(0);
        cal.add(Calendar.SECOND, t2 - t3);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        return !(hour >= 6 && hour <= 17);
    }

    // ---------------------------------------------------------------- STRD

    @Test void strdTableMatchesL1AttackListForEveryKey() {
        for (int str = 0; str <= 254; str++) {
            assertEquals(REF_STRD[str], CombatNpcPhysicalSource.strengthDamage(str), "STRD " + str);
            assertEquals(REF_STRD[str], CombatNpcPhysicalSource.npcStrengthDamage(str), "npc STRD " + str);
        }
        // Out-of-key values (source would NPE on unboxing) clamp to the nearest key.
        assertEquals(REF_STRD[0], CombatNpcPhysicalSource.npcStrengthDamage(-3));
        assertEquals(REF_STRD[254], CombatNpcPhysicalSource.npcStrengthDamage(300));
    }

    @Test void pcGrowthTableEqualsStrdOnlyForStrengthOneThroughOneTwentySeven() {
        SourcePhysicalGrowthStats growth = SourcePhysicalGrowthStats.classic381();
        for (int str = 1; str <= 127; str++) {
            assertEquals(REF_STRD[str], growth.damage(false, str), "STR " + str);
        }
        // 角色_力量成長 has no STR 0 row: it clamps to row 1 (-5), STRD(0) = -6.
        // This is why the NPC paths now call CombatNpcPhysicalSource.npcStrengthDamage.
        assertNotEquals(REF_STRD[0], growth.damage(false, 0));
        assertEquals(-6, CombatNpcPhysicalSource.npcStrengthDamage(0));
    }

    // ---------------------------------------------------------------- 命中

    @Test void npcToPlayerHitEnvelopeMatchesCalcPcHitForEveryFixedDie() {
        int cases = 0;
        for (int level = 1; level <= 99; level += 7) {
            for (int hitUp : new int[] {0, 3}) {
                for (int ac = -40; ac <= 20; ac += 3) {
                    int faces = ac < 0 ? -ac : 1;
                    for (int r20 = 0; r20 < 20; r20++) {
                        for (int rAc = 0; rAc < faces; rAc++) {
                            int expected = refPcHit(level, hitUp, ac, r20, rAc);
                            int roll = ac < 0 ? rAc + 1 : 0;
                            int monster = BattleNpcPhysicalAttack.hitChance(
                                    level, hitUp, ac, 0, 0, r20 + 1, roll);
                            if (expected != monster) {
                                fail("monster lv=" + level + " hu=" + hitUp + " ac=" + ac
                                        + " r20=" + r20 + " rAc=" + rAc
                                        + " expected=" + expected + " actual=" + monster);
                            }
                            if (hitUp == 0) {
                                int companion = ClientSession.companionPlayerHitChance(
                                        level, ac, 0, 0, r20 + 1, roll);
                                assertEquals(expected, companion, "companion lv=" + level
                                        + " ac=" + ac + " r20=" + r20 + " rAc=" + rAc);
                            }
                            cases++;
                        }
                    }
                }
            }
        }
        assertEquals(true, cases > 50_000, "cases=" + cases);
        // The legacy 3-arg arena helper is the same envelope at BattleCompanion.LEVEL.
        for (int ac = -20; ac <= 10; ac++) {
            int faces = ac < 0 ? -ac : 1;
            for (int r20 = 0; r20 < 20; r20++) {
                for (int rAc = 0; rAc < faces; rAc++) {
                    assertEquals(refPcHit(BattleCompanion.LEVEL, 0, ac, r20, rAc),
                            ClientSession.companionHitChance(ac, r20 + 1, ac < 0 ? rAc + 1 : 0),
                            "arena ac=" + ac + " r20=" + r20 + " rAc=" + rAc);
                }
            }
        }
    }

    @Test void companionPlayerHitNowAppliesTargetDodgeLikeTheMonsterPath() {
        for (int dodge = 0; dodge <= 10; dodge++) {
            for (int down = 0; down <= 10; down += 5) {
                for (int r20 = 1; r20 <= 20; r20++) {
                    assertEquals(
                            BattleNpcPhysicalAttack.hitChance(30, 0, -5, dodge, down, r20, 3),
                            ClientSession.companionPlayerHitChance(30, -5, dodge, down, r20, 3),
                            "dodge=" + dodge + " down=" + down + " r20=" + r20);
                }
            }
        }
        // Stacked dodge sources above the retained 0..10 range clamp instead of throwing.
        assertEquals(ClientSession.companionPlayerHitChance(30, 4, 10, 0, 12, 0),
                ClientSession.companionPlayerHitChance(30, 4, 15, 0, 12, 0));
        // Positive AC ignores any stray negative-AC roll argument.
        assertEquals(ClientSession.companionPlayerHitChance(30, 4, 0, 0, 12, 0),
                ClientSession.companionPlayerHitChance(30, 4, 0, 0, 12, 7));
    }

    @Test void npcToNpcHitEnvelopeMatchesCalcNpcHitIncludingBossBeforeDefense() {
        for (int level = 1; level <= 70; level += 3) {
            for (boolean boss : new boolean[] {false, true}) {
                for (int ac = -30; ac <= 15; ac += 3) {
                    int faces = ac < 0 ? -ac : 1;
                    for (int r20 = 0; r20 < 20; r20++) {
                        for (int rAc = 0; rAc < faces; rAc++) {
                            int expected = refNpcHit(level, 0, 0, boss, ac, r20, rAc);
                            var actual = ClassicWorldCompanionNpcPhysicalAttack.hit(
                                    level, 0, 0, boss, ac, r20 + 1, ac < 0 ? rAc + 1 : 0, 1);
                            if (expected != actual.normalizedHitRate()) {
                                fail("npc lv=" + level + " boss=" + boss + " ac=" + ac
                                        + " r20=" + r20 + " rAc=" + rAc + " expected="
                                        + expected + " actual=" + actual.normalizedHitRate());
                            }
                        }
                    }
                }
            }
        }
        // _hitRate >= rnd(1..100)
        assertEquals(true, ClassicWorldCompanionNpcPhysicalAttack.hit(5, 0, 0, false, 9, 1, 0, 15).hit());
        assertEquals(false, ClassicWorldCompanionNpcPhysicalAttack.hit(5, 0, 0, false, 9, 1, 0, 16).hit());
    }

    @Test void npcHitRollAndRangedErUseTheReferenceComparisons() {
        for (int d100 = 1; d100 <= 100; d100++) {
            assertEquals(15 >= d100, BattleNpcPhysicalAttack.hits(15, d100));
            assertEquals(true, BattleNpcPhysicalAttack.hits(100, d100));
        }
        // L1AttackMode.calcErEvasion:224-228 → er < rnd(1..100) means hit.
        for (int er = 0; er <= 100; er += 5) {
            for (int rnd = 1; rnd <= 100; rnd++) {
                assertEquals(er < rnd, BattleRangedEvasion.hitsAfterEvasion(er, rnd),
                        "er=" + er + " rnd=" + rnd);
            }
        }
    }

    // ---------------------------------------------------------------- 傷害

    @Test void npcDamageModeMatchesNpcDmgModeForEveryCriticalFace() {
        for (double dmg : new double[] {-3, 0, 1, 7.6, 14, 33.2, 80}) {
            for (int crit = 0; crit < 100; crit++) {
                for (int dmgUp : new int[] {0, 4}) {
                    for (boolean undead : new boolean[] {false, true}) {
                        for (int leverage : new int[] {10, 15}) {
                            for (boolean boss : new boolean[] {false, true}) {
                                for (int stacks = 0; stacks <= 10; stacks += 5) {
                                    assertEquals(
                                            refMode(dmg, crit, dmgUp, undead, leverage, boss, stacks),
                                            CombatNpcPhysicalSource.mode(dmg, crit, dmgUp, undead,
                                                    boss, leverage, stacks),
                                            "dmg=" + dmg + " crit=" + crit);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test void worldMonsterSourcePrefixMatchesCalcPcDamageBaseAndNpcDmgMode() {
        for (int lvl : new int[] {1, 5, 20, 45, 70}) {
            for (int str : new int[] {0, 4, 10, 11, 16, 25, 37, 60, 127}) {
                for (int rLvl = 0; rLvl < lvl; rLvl += Math.max(1, lvl / 6)) {
                    for (int crit = 0; crit < 100; crit += 7) {
                        for (boolean undead : new boolean[] {false, true}) {
                            for (boolean boss : new boolean[] {false, true}) {
                                double expected = refMode(refPcBase(lvl, str, rLvl, false, 0),
                                        crit, 0, undead, 10, boss, 0);
                                int strd = CombatNpcPhysicalSource.npcStrengthDamage(str);
                                assertEquals(expected, NativeNpcWeaponBreakDamage.sourceExact(
                                        lvl, str, strd, rLvl, crit, undead, boss), "lv" + lvl + " str" + str);
                                assertEquals(expected, CombatNpcPhysicalSource.mode(
                                        CombatNpcPhysicalSource.base(lvl, str, rLvl, false, 0),
                                        crit, 0, undead, boss, 10, 0));
                                for (int stacks = 1; stacks <= 10; stacks += 3) {
                                    double ref = refMode(refPcBase(lvl, str, rLvl, false, 0),
                                            crit, 0, undead, 10, boss, stacks);
                                    assertEquals(Math.max(0, (int) ref), NativeNpcWeaponBreakDamage.afterStacks(
                                            NativeNpcWeaponBreakDamage.sourceExact(lvl, str, strd, rLvl, crit, undead, boss),
                                            stacks));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test void petBaseAddsLevelOverSevenAndWeaponDamage() {
        for (int lvl = 1; lvl <= 60; lvl += 4) {
            for (int str : new int[] {5, 15, 30}) {
                for (int rLvl = 0; rLvl < lvl; rLvl += 3) {
                    assertEquals(refPcBase(lvl, str, rLvl, true, 2),
                            CombatNpcPhysicalSource.base(lvl, str, rLvl, true, 2));
                    assertEquals(refPcBase(lvl, str, rLvl, false, 0),
                            CombatNpcPhysicalSource.base(lvl, str, rLvl, false, 0));
                }
            }
        }
    }

    @Test void playerAcDefenseMatchesCalcPcDefense() {
        for (int type = 0; type < 7; type++) {
            for (int ac = -60; ac <= 10; ac++) {
                int max = refAcDefenseMax(type, Math.max(0, 10 - ac));
                assertEquals(max, BattlePhysicalDefense.playerAcMaximum(type, ac));
                for (int r = 0; r < Math.max(1, max); r++) {
                    double expected = 40.5 - refPcDefense(type, ac, r);
                    assertEquals(expected, CombatNpcPhysicalSource.defense(40.5, type, ac, r),
                            "type=" + type + " ac=" + ac + " r=" + r);
                }
            }
        }
    }

    @Test void companionFallbackDamageMatchesCalcPcDamageThroughThePetMultiplier() {
        // ClientSession.companionPhysicalDamage(type, ac, levelRoll, critRoll, defenseRoll):
        // level 5, STR 15, no pet weapon (native 3.81 has no pet-equipment receiver).
        double multiplier = CombatRules.replace(0.3, "PET_TO_PC_DAMAGE_MULTIPLIER");
        int cases = 0;
        for (int type = 0; type < 7; type++) {
            for (int ac = -40; ac <= 10; ac += 2) {
                int max = refAcDefenseMax(type, Math.max(0, 10 - ac));
                for (int rLvl = 0; rLvl < 5; rLvl++) {
                    for (int crit = 0; crit < 100; crit += 3) {
                        for (int rDef = 0; rDef < Math.max(1, max); rDef++) {
                            double dmg = refMode(refPcBase(5, 15, rLvl, false, 0), crit, 0, false, 10, false, 0);
                            dmg -= refPcDefense(type, ac, rDef);
                            dmg *= multiplier;
                            int expected = dmg <= 0 ? 0 : (int) dmg;
                            int actual = ClientSession.companionPhysicalDamage(type, ac, rLvl, crit, rDef);
                            if (expected != actual) {
                                fail("type=" + type + " ac=" + ac + " rLvl=" + rLvl + " crit=" + crit
                                        + " rDef=" + rDef + " expected=" + expected + " actual=" + actual);
                            }
                            cases++;
                        }
                    }
                }
            }
        }
        assertEquals(true, cases > 10_000, "cases=" + cases);
    }

    @Test void companionToNpcDamageMatchesCalcNpcDamageIncludingNegativeStrd() {
        int cases = 0;
        for (BattleCompanion.Kind kind : new BattleCompanion.Kind[] {
                BattleCompanion.Kind.SUMMON, BattleCompanion.Kind.PET, BattleCompanion.Kind.TAMED}) {
            boolean pet = kind != BattleCompanion.Kind.SUMMON;
            for (int lvl : new int[] {1, 12, 28, 45}) {
                int templateLvl = Math.max(1, lvl - 6);
                int bound = pet ? templateLvl : lvl;
                // STR 0..10 has a negative STRD; this used to throw (summonStrengthDamage < 0).
                for (int str : new int[] {0, 4, 10, 11, 16, 25, 40}) {
                    for (int rSrc = 0; rSrc < bound; rSrc += Math.max(1, bound / 4)) {
                        for (int crit : new int[] {0, 14, 15, 99}) {
                            for (int npcAc : new int[] {10, 0, -11, -25}) {
                                int max = Math.max(0, 10 - npcAc) / 7;
                                for (int rAc = 0; rAc < Math.max(1, max); rAc++) {
                                    for (int immune : new int[] {1, 2}) {
                                        int expected = refNpcDamage(pet, lvl, templateLvl, str, rSrc,
                                                crit, 0, 0, 10, false, 0, npcAc, 3, rAc, immune);
                                        var target = new SourceNpcPhysicalStats(45001, npcAc, false, 3, false);
                                        var actual = ClassicWorldCompanionNpcPhysicalAttack.damage(
                                                kind, lvl, templateLvl, str,
                                                CombatNpcPhysicalSource.npcStrengthDamage(str),
                                                rSrc, crit, 0, 0, 0, false, 10, 0,
                                                target, rAc, immune, 1_735_689_600_000L);
                                        if (expected != actual.damage()) {
                                            fail(kind + " lv=" + lvl + " str=" + str + " rSrc=" + rSrc
                                                    + " crit=" + crit + " ac=" + npcAc + " rAc=" + rAc
                                                    + " immune=" + immune + " expected=" + expected
                                                    + " actual=" + actual.damage());
                                        }
                                        cases++;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        assertEquals(true, cases > 1_000, "cases=" + cases);
        // STRD has no key below -6.
        assertThrows(IllegalArgumentException.class, () -> ClassicWorldCompanionNpcPhysicalAttack.damage(
                BattleCompanion.Kind.SUMMON, 5, 5, 3, -7, 0, 99, 0, 0, 0, false, 10, 0,
                new SourceNpcPhysicalStats(45001, 10, false, 0, false), 0, 1, 1_735_689_600_000L));
    }

    @Test void undeadNightClockMatchesL1GameTime() {
        long base = 1_735_689_600_000L;
        // 4 game days at 1 real-second steps of 6 game seconds would be slow; step 37 s.
        for (long t = base; t < base + 4L * 86_400_000L / 6L; t += 37_000L) {
            boolean expected = refNight(t);
            assertEquals(expected, CombatNpcPhysicalSource.night(t), "t=" + t);
            assertEquals(expected, ClassicWorldNpcDamage.retainedNight(t), "retained t=" + t);
        }
        assertThrows(IllegalArgumentException.class, () -> CombatNpcPhysicalSource.night(base - 1));
    }
}
