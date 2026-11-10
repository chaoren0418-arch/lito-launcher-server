package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

/**
 * Issue #303 (T101): LiTo physical hit / damage / AC-reduction formulas versus
 * the 1-伺服器 reference (D:\lineage開發\1-伺服器, read-only).
 *
 * <p>Every {@code ref*} helper below restates the 1-伺服器 formula in its
 * own words (expected value); the LiTo production helper is the actual value.
 * Random draws are fixed by enumerating every face of the reference
 * {@code ThreadLocalRandom.nextInt(n)} call.  Formula-by-formula source lines
 * are listed in {@code docs/design/formulas-physical.md}.</p>
 */
final class PhysicalFormulaServer1ParityTest {

    // ---------------------------------------------------------------- 1-伺服器 reference

    /** L1*ClassFeature.getHitLevel (royal..illusionist). */
    private static final int[] REF_HIT_LEVEL_DIVISOR = {15, 10, 30, 40, 30, 25, 40};
    /** L1*ClassFeature.getAttackLevel (royal..illusionist). */
    private static final int[] REF_ATTACK_LEVEL_DIVISOR = {10, 6, 12, 20, 16, 13, 16};

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

    /**
     * L1AttackPc.calcPcHit/calcNpcHit envelope (target dodge 0).
     * {@code r20} is the {@code nextInt(20)} face; {@code rAc} the
     * {@code nextInt(defenderValue)} face used only for a player with AC < 0.
     */
    private static int refHitRate(boolean npc, int type, int elfAttr, int hitRate,
                                  int ac, int r20, int rAc) {
        int attackerDice = r20 + 2 + hitRate - 10;
        int defenderDice;
        if (npc) {
            defenderDice = 10 - ac;
        } else if (ac >= 0) {
            defenderDice = 10 - ac;
        } else {
            defenderDice = 10 + rAc + 1;
        }
        int fumble = hitRate - 9;
        int critical = hitRate + 10;
        if (type == 5) attackerDice *= 1.01;
        if (type == 2 && (elfAttr & 2) != 0) attackerDice *= 1.02;
        if (attackerDice <= fumble) return 15;
        if (attackerDice >= critical) return 100;
        return attackerDice > defenderDice ? 100 : 15;
    }

    /** L1AttackMode.calcPcDefense with the nextInt(acDefMax) face {@code r}. */
    private static int refPcDefense(int type, int targetAc, int r) {
        int ac = Math.max(0, 10 - targetAc);
        int max = refAcDefenseMax(type, ac);
        if (max == 0) return 0;
        return r + Math.max(1, max >> 3);
    }

    /** L1AttackMode.calcNpcDamageReduction with the nextInt(acDefMax) face {@code r}. */
    private static int refNpcReduction(int npcAc, int damageReduction, int r) {
        int ac = Math.max(0, 10 - npcAc);
        int max = ac / 7;
        if (max == 0) return damageReduction;
        return r + Math.max(1, max) + damageReduction;
    }

    /** L1AttackPc.calcPcDamage:979-983 AC extra reduction. */
    private static int refExtraAc(int ac, double divisor) {
        if (divisor > 0 && ac < 0) return (int) (Math.abs(ac) / divisor);
        return 0;
    }

    /** L1AttackPc.calcAttrEnchantDmg:1504-1513. */
    private static int refAttrDamage(int damage, int resist) {
        int floor = (int) (0.32 * Math.abs(resist));
        if (resist < 0) floor *= -1;
        damage *= 1 - floor / 32.0;
        return damage;
    }

    /** 380_db.sql `角色_力量成長` (id, 命中, 傷害) dumped verbatim, ids 1..127. */
    private static final String REF_STR_TABLE =
            "1:-2:-5;2:-2:-5;3:-2:-4;4:-2:-4;5:-2:-3;6:-2:-3;7:-2:-2;8:-2:-2;9:-1:-1;10:-1:-1;"
            + "11:0:0;12:0:0;13:1:1;14:1:1;15:2:2;16:2:2;17:3:3;18:3:3;19:4:4;20:4:4;21:5:5;"
            + "22:5:5;23:5:6;24:6:6;25:6:6;26:6:7;27:7:7;28:7:7;29:7:8;30:8:8;31:8:9;32:8:9;"
            + "33:9:10;34:9:11;35:9:11;36:10:11;37:10:12;38:10:12;39:11:12;40:11:12;41:11:13;"
            + "42:12:13;43:12:13;44:12:13;45:13:14;46:13:14;47:13:14;48:14:14;49:14:15;50:14:15;"
            + "51:15:15;52:15:15;53:15:16;54:16:16;55:16:16;56:16:16;57:17:17;58:17:17;59:17:17;"
            + "60:18:17;61:18:18;62:18:18;63:19:18;64:19:18;65:19:19;66:20:19;67:20:19;68:20:19;"
            + "69:21:20;70:21:20;71:21:20;72:22:20;73:22:21;74:22:21;75:23:21;76:23:21;77:23:22;"
            + "78:24:22;79:24:22;80:24:22;81:25:23;82:25:23;83:25:23;84:26:23;85:26:24;86:26:24;"
            + "87:27:24;88:27:24;89:27:25;90:28:25;91:28:25;92:28:25;93:29:26;94:29:26;95:29:26;"
            + "96:30:26;97:30:27;98:30:27;99:31:27;100:31:27;101:31:28;102:32:28;103:32:28;"
            + "104:32:28;105:33:29;106:33:29;107:33:29;108:34:29;109:34:30;110:34:30;111:35:30;"
            + "112:35:30;113:35:31;114:36:31;115:36:31;116:36:31;117:37:32;118:37:32;119:37:32;"
            + "120:38:32;121:38:33;122:38:33;123:39:33;124:39:33;125:39:34;126:40:34;127:40:34";

    /** 380_db.sql `角色_敏捷成長` (id, 命中, 傷害) dumped verbatim, ids 1..127. */
    private static final String REF_DEX_TABLE =
            "1:-2:0;2:-2:0;3:-2:0;4:-2:0;5:-2:0;6:-2:0;7:-1:0;8:-1:0;9:0:0;10:0:0;11:1:0;"
            + "12:1:0;13:2:0;14:2:0;15:3:1;16:3:2;17:4:3;18:4:4;19:5:4;20:6:4;21:7:5;22:8:5;"
            + "23:9:5;24:10:5;25:11:6;26:12:6;27:13:6;28:14:7;29:15:7;30:16:7;31:17:8;32:18:8;"
            + "33:19:8;34:19:9;35:19:9;36:20:9;37:20:10;38:20:10;39:21:10;40:21:10;41:21:11;"
            + "42:22:11;43:22:11;44:22:11;45:23:12;46:23:12;47:23:12;48:24:12;49:24:13;50:24:13;"
            + "51:25:13;52:25:13;53:25:14;54:26:14;55:26:14;56:26:14;57:27:15;58:27:15;59:27:15;"
            + "60:28:15;61:28:16;62:28:16;63:29:16;64:29:16;65:29:17;66:30:17;67:30:17;68:30:17;"
            + "69:31:18;70:31:18;71:31:18;72:32:18;73:32:19;74:32:19;75:33:19;76:33:19;77:33:20;"
            + "78:34:20;79:34:20;80:34:20;81:35:21;82:35:21;83:35:21;84:36:21;85:36:22;86:36:22;"
            + "87:37:22;88:37:22;89:37:23;90:38:23;91:38:23;92:38:23;93:39:24;94:39:24;95:39:24;"
            + "96:40:24;97:40:25;98:40:25;99:41:25;100:41:25;101:41:26;102:42:26;103:42:26;"
            + "104:42:26;105:43:27;106:43:27;107:43:27;108:44:27;109:44:28;110:44:28;111:45:28;"
            + "112:45:28;113:45:29;114:46:29;115:46:29;116:46:29;117:47:30;118:47:30;119:47:30;"
            + "120:48:30;121:48:31;122:48:31;123:49:31;124:49:31;125:49:32;126:50:32;127:50:32";

    private static int[][] table(String text) {
        String[] rows = text.split(";");
        assertEquals(127, rows.length);
        int[][] out = new int[128][];
        for (String row : rows) {
            String[] f = row.split(":");
            out[Integer.parseInt(f[0])] = new int[] {Integer.parseInt(f[1]), Integer.parseInt(f[2])};
        }
        return out;
    }

    // ---------------------------------------------------------------- 素質補正

    @Test void strengthAndDexterityTablesEqualThe380DbRowsForEveryStat() {
        int[][] str = table(REF_STR_TABLE);
        int[][] dex = table(REF_DEX_TABLE);
        SourcePhysicalGrowthStats lito = SourcePhysicalGrowthStats.classic381();
        for (int stat = 1; stat <= 127; stat++) {
            assertEquals(str[stat][0], BattlePhysicalAttack.strengthHit(stat), "STR hit " + stat);
            assertEquals(str[stat][0], lito.hit(false, stat), "STR hit table " + stat);
            assertEquals(str[stat][1], lito.damage(false, stat), "STR dmg " + stat);
            assertEquals(dex[stat][0], BattlePhysicalAttack.dexterityHit(stat), "DEX hit " + stat);
            assertEquals(dex[stat][0], lito.hit(true, stat), "DEX hit table " + stat);
            assertEquals(dex[stat][1], lito.damage(true, stat), "DEX dmg " + stat);
        }
        // CalcStat/BaseStatTable clamp id at 127 (BaseStatTable.java:208/223/240/255).
        assertEquals(str[127][0], lito.hit(false, 200));
        assertEquals(dex[127][1], lito.damage(true, 200));
    }

    // ---------------------------------------------------------------- 職業補正

    @Test void classHitAndAttackLevelCorrectionsEqualTheClassFeatureDivisors() {
        for (int type = 0; type < 7; type++) {
            for (int level = 1; level <= 127; level++) {
                assertEquals(level / REF_HIT_LEVEL_DIVISOR[type],
                        BattlePhysicalAttack.classHitCorrection(type, level), "hit " + type + "/" + level);
                assertEquals(level / REF_ATTACK_LEVEL_DIVISOR[type],
                        BattleKiringkuDamage.classAttackLevel(type, level), "atk " + type + "/" + level);
            }
        }
    }

    // ---------------------------------------------------------------- 命中

    @Test void playerTargetHitEnvelopeMatchesCalcPcHitForEveryFixedDie() {
        int cases = 0;
        for (int type = 0; type < 7; type++) {
            for (int elfAttr : type == 2 ? new int[] {0, 2, 3, 4} : new int[] {0}) {
                for (int level : new int[] {1, 25, 52, 99}) {
                    for (int stat : new int[] {-2, 7, 30, 60}) {
                        int hitRate = level + BattlePhysicalAttack.classHitCorrection(type, level) + stat;
                        for (int ac = -40; ac <= 20; ac += 4) {
                            int faces = ac < 0 ? -((int) (ac * 1.5)) : 1;
                            for (int r20 = 0; r20 < 20; r20++) {
                                for (int rAc = 0; rAc < faces; rAc++) {
                                    int expected = refHitRate(false, type, elfAttr, hitRate, ac, r20, rAc);
                                    int actual = BattlePhysicalAttack.hitChanceAgainst(
                                            false, type, elfAttr, level, stat, ac, 0, 0, 0, 0,
                                            r20 + 1, ac < 0 ? rAc + 1 : 0);
                                    if (expected != actual) {
                                        fail("PC t=" + type + " e=" + elfAttr + " lv=" + level
                                                + " st=" + stat + " ac=" + ac + " r20=" + r20
                                                + " rAc=" + rAc + " expected=" + expected + " actual=" + actual);
                                    }
                                    cases++;
                                }
                            }
                        }
                    }
                }
            }
        }
        assertTrue(cases > 100_000);
    }

    @Test void npcTargetHitEnvelopeUsesTenMinusAcWithoutTheNegativeAcRoll() {
        for (int type = 0; type < 7; type++) {
            for (int level : new int[] {1, 30, 52, 90}) {
                for (int stat : new int[] {-2, 0, 10, 40}) {
                    int hitRate = level + BattlePhysicalAttack.classHitCorrection(type, level) + stat;
                    for (int ac = -120; ac <= 10; ac++) {
                        for (int r20 = 0; r20 < 20; r20++) {
                            int expected = refHitRate(true, type, 2, hitRate, ac, r20, 0);
                            int actual = BattlePhysicalAttack.hitChanceAgainst(
                                    true, type, 2, level, stat, ac, 0, 0, 0, 0, r20 + 1, 0);
                            if (expected != actual) {
                                fail("NPC t=" + type + " lv=" + level + " st=" + stat
                                        + " ac=" + ac + " r20=" + r20);
                            }
                        }
                    }
                }
            }
        }
        // NPC AC never consumes a defense roll (L1AttackPc.java:487).
        assertThrows(IllegalArgumentException.class, () -> BattlePhysicalAttack.hitChanceAgainst(
                true, 1, 0, 52, 0, -10, 0, 0, 0, 0, 10, 1));
        // Concrete pin: lv52 knight, hitRate 57, AC -20 NPC, d20 face 9 -> 58 vs defender 30 = 100.
        assertEquals(100, BattlePhysicalAttack.hitChanceAgainst(
                true, 1, 0, 52, 0, -20, 0, 0, 0, 0, 10, 0));
    }

    @Test void dragonKnightAndFireElfAttackerDieScalesTruncateLikeTheCompoundAssignment() {
        assertEquals(101, BattlePhysicalAttack.classAttackerDice(5, 0, 100));
        assertEquals(99, BattlePhysicalAttack.classAttackerDice(5, 0, 99));
        assertEquals(-5, BattlePhysicalAttack.classAttackerDice(5, 0, -5));
        assertEquals(51, BattlePhysicalAttack.classAttackerDice(2, 2, 50));
        assertEquals(51, BattlePhysicalAttack.classAttackerDice(2, 3, 50)); // dual attribute with fire bit
        assertEquals(50, BattlePhysicalAttack.classAttackerDice(2, 4, 50)); // water elf
        assertEquals(50, BattlePhysicalAttack.classAttackerDice(1, 2, 50)); // non-elf ignores ElfAttr
    }

    @Test void finalPercentRollAndBowErMatchTheReferenceComparisons() {
        // calcPcHit:396-402: hit when _hitRate >= rnd(1..100); calcErEvasion: er < rnd.
        for (int rnd = 1; rnd <= 100; rnd++) {
            assertEquals(15 >= rnd, BattlePhysicalAttack.hits(15, rnd));
            assertEquals(true, BattlePhysicalAttack.hits(100, rnd));
            for (int er : new int[] {0, 1, 50, 99, 100}) {
                assertEquals(er < rnd, BattleRangedEvasion.hitsAfterEvasion(er, rnd));
            }
        }
    }

    // ---------------------------------------------------------------- AC 減傷

    @Test void playerAcReductionMatchesCalcPcDefenseForEveryClassAcAndRoll() {
        for (int type = 0; type < 7; type++) {
            for (int ac = -150; ac <= 15; ac++) {
                int max = refAcDefenseMax(type, Math.max(0, 10 - ac));
                assertEquals(max, BattlePhysicalDefense.playerAcMaximum(type, ac));
                int faces = Math.max(1, max);
                for (int r = 0; r < faces; r++) {
                    BattlePhysicalDefense.PlayerOutcome outcome =
                            BattlePhysicalDefense.againstPlayer(1000, type, ac, r, 0, 0, 0, 0);
                    assertEquals(refPcDefense(type, ac, r), outcome.acReduction(),
                            "type=" + type + " ac=" + ac + " r=" + r);
                    // CombatRules is not loaded in unit tests: AC_REDUCTION_DIVISOR defaults to 0.
                    assertEquals(1000 - refPcDefense(type, ac, r), outcome.damage());
                }
            }
        }
    }

    @Test void npcAcReductionMatchesCalcNpcDamageReductionIncludingDamageReduction() {
        for (int ac = -150; ac <= 15; ac++) {
            for (int dr = 0; dr <= 6; dr += 3) {
                SourceNpcPhysicalStats npc = new SourceNpcPhysicalStats(45000, ac, false, dr, false);
                int max = Math.max(0, 10 - ac) / 7;
                assertEquals(max, BattlePhysicalDefense.npcAcMaximum(npc));
                for (int r = 0; r < Math.max(1, max); r++) {
                    BattlePhysicalDefense.NpcOutcome outcome =
                            BattlePhysicalDefense.againstNpc(1000, npc, r, false, false);
                    assertEquals(refNpcReduction(ac, dr, r), outcome.totalReduction(),
                            "ac=" + ac + " dr=" + dr + " r=" + r);
                    assertEquals(1000 - refNpcReduction(ac, dr, r), outcome.damage());
                }
            }
        }
    }

    @Test void hardSkinHalvesRangedDamageAfterReductionUnlessPierced() {
        SourceNpcPhysicalStats hard = new SourceNpcPhysicalStats(45000, 10, false, 3, true);
        // calcNpcDamage:1170 then :1188-1199 (dmg -= reduction; ranged && !pierce -> /2).
        for (int raw = 0; raw <= 40; raw++) {
            int afterReduction = Math.max(0, raw - 3);
            assertEquals(afterReduction / 2, BattlePhysicalDefense.againstNpc(raw, hard, 0, true, false).damage());
            assertEquals(afterReduction, BattlePhysicalDefense.againstNpc(raw, hard, 0, true, true).damage());
            assertEquals(afterReduction, BattlePhysicalDefense.againstNpc(raw, hard, 0, false, false).damage());
        }
    }

    @Test void acExtraReductionOnlyAppliesAStrictlyPositiveDivisor() {
        for (double divisor : new double[] {-3.0, -0.5, 0.0, 0.5, 2.0, 3.0, 7.5}) {
            for (int ac = -150; ac <= 10; ac++) {
                assertEquals(refExtraAc(ac, divisor), CombatRules.extraAcReduction(ac, divisor),
                        "ac=" + ac + " divisor=" + divisor);
            }
        }
    }

    @Test void armorReductionIgnoreAndDirectionalReductionSubtractLikeCalcPcDamage() {
        // calcPcDamage:976-1013: AC dice, effective (redux - ignore >= 0), near/far redux.
        int raw = 60;
        for (int base = 0; base <= 14; base++) {
            int faces = base <= 10 ? 1 : base - 10;
            for (int roll = 0; roll < faces; roll++) {
                for (int ignore = 0; ignore <= 6; ignore += 3) {
                    for (int directional = 0; directional <= 4; directional += 2) {
                        int armor = base <= 10 ? base : 11 + roll;
                        int expected = Math.max(0, raw - refPcDefense(1, -10, 2)
                                - Math.max(0, armor - ignore) - directional);
                        assertEquals(expected, BattlePhysicalDefense.againstPlayer(
                                raw, 1, -10, 2, base, base <= 10 ? 0 : roll, ignore, directional).damage());
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- 武器骰／暴擊

    @Test void meleeWeaponDieMatchesWeaponDamage1WithAndWithoutMaximumControl() {
        for (int maximum = 1; maximum <= 20; maximum++) {
            for (int chance : new int[] {0, 5, 100}) {
                SourceWeaponPhysicalStats sword = weapon(1, maximum, 0, 0.0D, chance);
                for (int chanceRoll = chance == 0 ? 0 : 1; chanceRoll <= (chance == 0 ? 0 : 100); chanceRoll++) {
                    boolean triggered = chance > 0 && chanceRoll <= chance;
                    if (triggered) {
                        assertEquals(maximum, BattleWeaponCritical.roll(sword, maximum, 0, chanceRoll).damage());
                        continue;
                    }
                    // weaponDamage1:654-666: nextInt(max-1)+1 under control (1 for max==1), else nextInt(max)+1.
                    int highest = chance > 0 ? (maximum > 1 ? maximum - 1 : 1) : maximum;
                    for (int face = 1; face <= highest; face++) {
                        assertEquals(face, BattleWeaponCritical.roll(sword, maximum, face, chanceRoll).damage());
                    }
                    if (highest < maximum) {
                        final int roll = chanceRoll;
                        final int max = maximum;
                        assertThrows(IllegalArgumentException.class,
                                () -> BattleWeaponCritical.roll(sword, max, max, roll));
                    }
                }
            }
        }
    }

    @Test void clawIsZeroUntilMaximumControlTriggersThenAddsFiveBeforeTheMultiplier() {
        SourceWeaponPhysicalStats claw = weapon(11, 8, 0, 0.0D, 30);
        for (int chanceRoll = 1; chanceRoll <= 100; chanceRoll++) {
            assertEquals(chanceRoll <= 30 ? 8 : 0, BattleWeaponCritical.roll(claw, 8, 0, chanceRoll).damage());
        }
        // weaponDamage2:727-734 (dmg += 5.0) then pcDmgMode:902 (dmg *= multiplier).
        SourceWeaponPhysicalStats doubled = weapon(11, 8, 20, 1.5D, 0);
        assertEquals((int) ((10 + 5) * 1.5D), BattleWeaponCritical.apply(doubled, 10, 20).damage());
        assertEquals(10 + 5, BattleWeaponCritical.apply(doubled, 10, 21).damage());
    }

    @Test void multipliersTakeTheHighestCandidateInsteadOfMultiplying() {
        // pcDmgMode:901-902 and weaponDamage2:810-823 use Math.max over candidates.
        SourceWeaponPhysicalStats edoryu = weapon(12, 8, 10, 1.2D, 0);
        for (double external : new double[] {1.0D, 1.1D, 1.3D, 1.8D}) {
            for (int subtotal : new int[] {0, 1, 17, 40}) {
                assertEquals((int) (subtotal * Math.max(external, 1.2D)),
                        BattleWeaponCritical.apply(edoryu, subtotal, 10, external).damage());
                assertEquals((int) (subtotal * external),
                        BattleWeaponCritical.apply(edoryu, subtotal, 11, external).damage());
            }
        }
    }

    @Test void unarmedDamageIsTheShiftedFourThroughEightRoll() {
        // weaponDamage2:744-746 (nextInt(5)+4) >> 2.
        for (int face = 0; face < 5; face++) {
            assertEquals((face + 4) >> 2, BattlePhysicalDamage.unarmedDamage(face + 4));
        }
    }

    // ---------------------------------------------------------------- 屬性／材質

    @Test void passiveAttributeDamageMatchesCalcAttrEnchantDmgCoefficient() {
        for (int base = 0; base <= 9; base++) {
            for (int resist = -100; resist <= 150; resist++) {
                assertEquals(refAttrDamage(base, resist), BattlePhysicalOffense.attributeDamage(base, resist),
                        "base=" + base + " resist=" + resist);
            }
        }
    }

    @Test void undeadMaterialBonusMatchesCalcMaterialBlessDmgDieSizes() {
        // calcMaterialBlessDmg:1360-1390: undead 1/3/5 silver family d20, demon(2) mithril/ori d3.
        int[][] expected = {
                // undead, silver(14), mithril(17), oriharukon(22), iron(9)
                {0, 0, 0, 0, 0}, {1, 20, 20, 20, 0}, {2, 0, 3, 3, 0},
                {3, 20, 20, 20, 0}, {4, 0, 0, 0, 0}, {5, 20, 20, 20, 0}};
        int[] materials = {14, 17, 22, 9};
        for (int[] row : expected) {
            SourceNpcPhysicalStats npc = new SourceNpcPhysicalStats(45000, 10, false, 0, false, row[0], 0, 0);
            for (int i = 0; i < materials.length; i++) {
                SourceWeaponPhysicalStats w = materialWeapon(materials[i], 1);
                assertEquals(row[i + 1], BattlePhysicalOffense.materialMaximum(w, npc),
                        "undead=" + row[0] + " material=" + materials[i]);
            }
            // Blessing d4 only for undead 1/2/3 (werewolf 5 has none).
            boolean blessedFamily = row[0] == 1 || row[0] == 2 || row[0] == 3;
            assertEquals(blessedFamily ? 4 : 0,
                    BattlePhysicalOffense.blessingMaximum(materialWeapon(9, 0), npc));
        }
    }

    // ---------------------------------------------------------------- 職業倍率（ConfigPcDamage）

    @Test void classDamageMultiplierKeysAndFallbackMatchConfigPcDamage() throws Exception {
        String[] names = {"Crown", "Knight", "Elf", "Wizard", "Darkelf", "DragonKnight", "Illusionist"};
        for (int type = 0; type < 7; type++) {
            assertEquals("Other_To_is" + names[type] + "_pc", CombatClassDamage.key(type, true));
            assertEquals("Other_To_is" + names[type] + "_npc", CombatClassDamage.key(type, false));
        }
        assertEquals(null, CombatClassDamage.key(7, true));
        var out = new java.util.HashMap<String, Double>();
        var defaults = new java.util.HashSet<String>();
        var props = new java.util.Properties();
        props.setProperty("Other_To_isKnight_pc", " 1.25 ");
        props.setProperty("Other_To_isElf_pc", "bad");
        CombatClassDamage.parse(out, props, "Other_To_isKnight_pc", java.nio.file.Path.of("fixture"), defaults);
        CombatClassDamage.parse(out, props, "Other_To_isElf_pc", java.nio.file.Path.of("fixture"), defaults);
        CombatClassDamage.parse(out, props, "Other_To_isWizard_pc", java.nio.file.Path.of("fixture"), defaults);
        // ConfigPcDamage._getDouble: trim+parse, malformed or missing -> 1.0.
        assertEquals(1.25D, out.get("Other_To_isKnight_pc"));
        assertEquals(1.0D, out.get("Other_To_isElf_pc"));
        assertEquals(1.0D, out.get("Other_To_isWizard_pc"));
        assertFalse(defaults.contains("Other_To_isKnight_pc"));
        // Without loaded rules the multiplier is 1.0 and non-positive damage stays 0.
        if (!CombatRules.loaded()) assertEquals(17, CombatClassDamage.scale(17, 1, true));
        assertEquals(0, CombatClassDamage.scale(-3, 1, true));
    }

    // ---------------------------------------------------------------- fixtures

    private static SourceWeaponPhysicalStats weapon(
            int type, int maximum, int doubleChance, double doubleMultiplier, int maximumChance) {
        return new SourceWeaponPhysicalStats(
                2000 + type, type, maximum, maximum, 1, 0, 0, 0, 0)
                .withCombatTraits(new SourceWeaponCombatTraits(
                        11, 1, 0, 0, 0,
                        doubleChance, doubleMultiplier, maximumChance, false, false));
    }

    private static SourceWeaponPhysicalStats materialWeapon(int material, int blessing) {
        return new SourceWeaponPhysicalStats(
                3000 + material, 1, 8, 8, 1, 0, 0, 0, 0)
                .withCombatTraits(new SourceWeaponCombatTraits(
                        material, blessing, 0, 0, 0, 0, 0.0D, 0, false, false));
    }
}
