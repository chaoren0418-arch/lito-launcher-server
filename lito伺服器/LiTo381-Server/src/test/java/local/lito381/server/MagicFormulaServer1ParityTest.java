package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.IntUnaryOperator;

import org.junit.jupiter.api.Test;

/**
 * Issue #304 (T102): LiTo magic damage / MR / skill probability formulas versus
 * the 1-伺服器 reference (D:\lineage開發\1-伺服器, read-only).
 *
 * <p>Every {@code ref*} helper restates the 1-伺服器 formula (expected value);
 * the LiTo production helper is the actual value.  Reference tables are the
 * 1-伺服器 {@code db/380_db.sql} rows (角色_精神成長, 角色_智力成長,
 * skills_負面技能控制, skills_負面boss免疫, skills.skill_level).  Source
 * lines for each item are listed in {@code docs/design/formulas-magic.md}.</p>
 */
final class MagicFormulaServer1ParityTest {

    // ---------------------------------------------------------------- 1-伺服器 reference

    /** L1AttackList.load MRDMG table (mr 0..254) with calcMrDefense's 11/0.5 fallback. */
    private static double[] refMrFloorAndCoefficient(int mr) {
        if (mr < 0 || mr >= 255) return new double[] {11.0, 0.5};
        if (mr == 0) return new double[] {1.0, 1.0};
        if (mr <= 50) return new double[] {2.0, 1.0};
        if (mr <= 100) return new double[] {3.0, 0.9};
        if (mr <= 120) return new double[] {4.0, 0.9};
        if (mr <= 140) return new double[] {5.0, 0.8};
        if (mr <= 160) return new double[] {6.0, 0.8};
        if (mr <= 180) return new double[] {7.0, 0.7};
        if (mr <= 200) return new double[] {8.0, 0.7};
        if (mr <= 220) return new double[] {9.0, 0.6};
        if (mr <= 240) return new double[] {10.0, 0.6};
        return new double[] {11.0, 0.5};
    }

    /** L1MagicPc.calcMrDefense: {@code dmg *= coef - 0.01 * floor((mr - originalMagicHit) / floor)}. */
    private static int refMrDefense(int dmg, int mr, int originalMagicHit) {
        double[] f = refMrFloorAndCoefficient(mr);
        dmg *= (f[1] - (0.01 * Math.floor((mr - originalMagicHit) / f[0])));
        return dmg;
    }

    /** L1MagicMode.calcAttrResistance + L1MagicPc.calcMagicDiceDamage coefficient. */
    private static int refAttributeAndIntelligence(int diceSum, int intPlusItemSp, int resist) {
        int resistFloor = (int) (0.32 * Math.abs(resist));
        if (resist < 0) resistFloor *= -1;
        double attrDefence = resistFloor / 32.0;
        int charaIntelligence = Math.max(intPlusItemSp - 12, 1);
        double coefficient = Math.max(1.0 - attrDefence + charaIntelligence * 3.0 / 32.0, 0.0);
        int magicDamage = diceSum;
        magicDamage *= coefficient;
        return magicDamage;
    }

    /** L1PcOriginal.resetOriginalMr (type order: crown, knight, elf, wizard, darkelf, DK, illusionist). */
    private static int refOriginalMr(int type, int w) {
        return switch (type) {
            case 0 -> (w == 12 || w == 13) ? 1 : w >= 14 ? 2 : 0;
            case 1 -> (w == 10 || w == 11) ? 1 : w >= 12 ? 2 : 0;
            case 2 -> (w >= 13 && w <= 15) ? 1 : w >= 16 ? 2 : 0;
            case 3 -> w >= 15 ? 1 : 0;
            case 4 -> (w >= 11 && w <= 13) ? 1 : w == 14 ? 2 : w == 15 ? 3 : w >= 16 ? 4 : 0;
            case 5 -> w >= 14 ? 2 : 0;
            case 6 -> (w >= 15 && w <= 17) ? 2 : w == 18 ? 4 : 0;
            default -> throw new AssertionError(type);
        };
    }

    /** L1PcOriginal.resetOriginalMagicHit. */
    private static int refOriginalMagicHit(int type, int i) {
        return switch (type) {
            case 0 -> (i == 12 || i == 13) ? 1 : i >= 14 ? 2 : 0;
            case 1 -> (i == 10 || i == 11) ? 1 : i == 12 ? 2 : 0;
            case 2 -> (i == 13 || i == 14) ? 1 : i >= 15 ? 2 : 0;
            case 3 -> i <= 14 ? 0 : i <= 80 ? 1 : 2;
            case 4 -> (i == 12 || i == 13) ? 1 : i >= 14 ? 2 : 0;
            case 5 -> (i == 12 || i == 13) ? 2 : (i == 14 || i == 15) ? 3 : i >= 16 ? 4 : 0;
            case 6 -> i >= 13 ? 1 : 0;
            default -> throw new AssertionError(type);
        };
    }

    /** L1PcOriginal.resetOriginalMagicCritical. */
    private static int refOriginalMagicCritical(int type, int i) {
        return switch (type) {
            case 2 -> (i == 14 || i == 15) ? 2 : i >= 16 ? 4 : 0;
            case 3 -> i == 15 ? 2 : i == 16 ? 4 : i == 17 ? 6 : i == 18 ? 8 : 0;
            case 0, 1, 4, 5, 6 -> 0;
            default -> throw new AssertionError(type);
        };
    }

    /** L1PcOriginal.resetOriginalMagicDamage. */
    private static int refOriginalMagicDamage(int type, int i) {
        return switch (type) {
            case 3 -> i >= 13 ? 1 : 0;
            case 5 -> (i == 13 || i == 14) ? 1 : (i == 15 || i == 16) ? 2 : i == 17 ? 3 : 0;
            case 6 -> i == 16 ? 1 : i == 17 ? 2 : 0;
            case 0, 1, 2, 4 -> 0;
            default -> throw new AssertionError(type);
        };
    }

    /** L1PcInstance.resetBaseMr class part. */
    private static final int[] REF_CLASS_BASE_MR = {10, 0, 25, 15, 10, 18, 20};

    /** 380_db 角色_精神成長.魔防, id 0..50 (id 0 has no row → 0). */
    private static final int[] REF_WIS_MR = {
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 3, 6, 10, 15,
            21, 28, 37, 47, 50, 50, 50, 50, 50, 50, 52, 52, 52, 52, 52, 55, 55, 55, 55, 55,
            59, 59, 59, 59, 62, 62, 62, 64, 64, 66, 66};

    /** 380_db 角色_智力成長.魔法加成, id 1..127 (index 0 = INT 0 → -2 per BaseStatTable.getIntMagicBonus). */
    private static final int[] REF_INT_MAGIC_BONUS = new int[128];
    static {
        int[] head = {-2, -2, -2, -2, -2, -2, -1, -1, -1, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 4,
                5, 6, 7, 8, 9, 10, 10, 10, 11, 11, 11, 12, 12, 12, 13, 13, 13, 14, 14, 14,
                15, 15, 15, 15};
        for (int i = 0; i < head.length; i++) REF_INT_MAGIC_BONUS[i] = head[i];
        for (int i = 44; i <= 127; i++) REF_INT_MAGIC_BONUS[i] = i - 28; // rows 44..127: 16..99
    }

    /** BaseStatTable.getIntMagicBonus. */
    private static int refIntMagicBonus(int intelligence) {
        if (intelligence < 1) return -2;
        return REF_INT_MAGIC_BONUS[Math.min(intelligence, 127)];
    }

    /** L1*ClassFeature.getMagicLevel. */
    private static int refMagicLevel(int type, int level) {
        return switch (type) {
            case 0 -> Math.min(2, level / 10);
            case 1 -> level / 50;
            case 2, 6 -> Math.min(6, level >> 3);
            case 3 -> Math.min(10, level >> 2);
            case 4 -> Math.min(2, level / 12);
            case 5 -> Math.min(4, level / 9);
            default -> throw new AssertionError(type);
        };
    }

    /** One skills_負面技能控制 row (column order of the 380_db INSERT). */
    private record RefRow(int id, int high, int equal, int low, double str, double dex, double intel,
                          int resist, int magicLevel, int magicHit, double mr, double con, double wis) {}

    /** 380_db skills_負面技能控制, all 34 rows. */
    private static final List<RefRow> REF_ROWS = List.of(
            new RefRow(20, 55, 45, 35, 0, 0, 0.35, 4, 0, 1, 0.05, 0, 0),
            new RefRow(27, 55, 45, 35, 0, 0, 0.3, 0, 0, 1, 0.05, 0, 0),
            new RefRow(29, 55, 45, 35, 0, 0, 0.3, 0, 0, 1, 0.05, 0, 0),
            new RefRow(33, 50, 40, 30, 0, 0, 0.4, 2, 0, 1, 0.05, 0, 0),
            new RefRow(36, 40, 35, 30, 0, 0, 0.5, 0, 0, 1, 0.05, 0, 0),
            new RefRow(40, 55, 45, 35, 0, 0, 0.35, 4, 0, 1, 0.05, 0, 0),
            new RefRow(44, 20, 20, 20, 0, 0, 0, 0, 0, 1, 0, 0, 0),
            new RefRow(50, 55, 45, 35, 0, 0, 0.4, 1, 0, 1, 0.05, 0, 0),
            new RefRow(64, 55, 45, 35, 0, 0, 0.35, 0, 0, 1, 0.05, 0, 0),
            new RefRow(66, 50, 42, 34, 0, 0, 0.35, 3, 0, 1, 0.05, 0, 0),
            new RefRow(80, 52, 44, 36, 0, 0, 0.38, 1, 0, 1, 0.05, 0, 0),
            new RefRow(87, 60, 45, 30, 0, 0, 0, 5, 0, 1, 0.05, 0, 0),
            new RefRow(91, 55, 45, 35, 0, 0, 0, 0, 0, 1, 0.05, 0, 0),
            new RefRow(103, 55, 45, 35, 0, 0, 0.35, 4, 0, 1, 0.05, 0, 0),
            new RefRow(112, 50, 40, 30, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            new RefRow(133, 55, 45, 35, 0, 0, 0.3, 0, 0, 1, 0.05, 0, 0),
            new RefRow(145, 55, 45, 35, 0, 0, 0.3, 0, 0, 1, 0.05, 0, 0),
            new RefRow(152, 55, 45, 35, 0, 0, 0.3, 0, 0, 1, 0.05, 0, 0),
            new RefRow(153, 55, 45, 35, 0, 0, 0.35, 0, 0, 1, 0.05, 0, 0),
            new RefRow(157, 50, 42, 35, 0, 0, 0.4, 6, 0, 1, 0, 0, 0),
            new RefRow(161, 55, 45, 35, 0, 0, 0.3, 0, 0, 1, 0.05, 0, 0),
            new RefRow(167, 55, 45, 35, 0, 0, 0.3, 0, 0, 1, 0.05, 0, 0),
            new RefRow(173, 55, 45, 35, 0, 0, 0.3, 0, 0, 1, 0.05, 0, 0),
            new RefRow(174, 55, 45, 35, 0, 0, 0.3, 0, 0, 1, 0.05, 0, 0),
            new RefRow(183, 55, 45, 35, 0, 0, 0.35, 0, 0, 1, 0.05, 0, 0),
            new RefRow(188, 45, 38, 30, 0, 0, 0.4, 0, 0, 1, 0.05, 0, 0),
            new RefRow(191, 35, 25, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            new RefRow(192, 50, 30, 15, 0, 0, 0, 6, 0, 0, 0, 0, 0),
            new RefRow(193, 55, 45, 35, 0, 0, 0.35, 0, 0, 1, 0.05, 0, 0),
            new RefRow(194, 52, 44, 36, 0, 0, 0.38, 1, 0, 1, 0.05, 0, 0),
            new RefRow(202, 25, 25, 25, 0, 0, 0, 0, 0, 1, 0, 0, 0),
            new RefRow(208, 55, 45, 35, 0, 0, 0.35, 5, 0, 1, 0.05, 0, 0),
            new RefRow(212, 35, 35, 35, 0, 0, 0, 2, 0, 1, 0, 0, 0),
            new RefRow(217, 55, 45, 35, 0, 0, 0.25, 0, 0, 1, 0.05, 0, 0));

    /** 380_db skills_負面boss免疫 (skill_id → modifier). */
    private static final Map<Integer, Integer> REF_BOSS = Map.ofEntries(
            Map.entry(10, 0), Map.entry(11, 0), Map.entry(20, 0), Map.entry(22, 0), Map.entry(23, 0),
            Map.entry(27, 3), Map.entry(28, 0), Map.entry(29, 5), Map.entry(30, 0), Map.entry(33, 0),
            Map.entry(36, 0), Map.entry(39, 5), Map.entry(40, 0), Map.entry(47, 5), Map.entry(50, 0),
            Map.entry(56, 5), Map.entry(64, 0), Map.entry(66, 0), Map.entry(67, 0), Map.entry(71, 0),
            Map.entry(76, 3), Map.entry(80, 0), Map.entry(87, 10), Map.entry(91, 0), Map.entry(103, 0),
            Map.entry(105, 0), Map.entry(112, 0), Map.entry(133, 0), Map.entry(145, 0), Map.entry(152, 0),
            Map.entry(153, 0), Map.entry(157, 0), Map.entry(161, 0), Map.entry(167, 0), Map.entry(173, 0),
            Map.entry(174, 0), Map.entry(183, 0), Map.entry(188, 0), Map.entry(191, 0), Map.entry(192, 0));

    /**
     * L1MagicPc.calcProbabilityWithConfig (+ SHOCK_STUN stun-success tail in
     * calcProbability) followed by calcProbabilityMagic's 90 cap, BOSS divide
     * and 1 floor.  Boss modifier 0 short-circuits to failure (returned as 0).
     */
    private static int refConfiguredChance(RefRow row, int attackLevel, int str, int dex, int intel,
                                           int magicLevel, int magicHit, int stunSuccess,
                                           int defenseLevel, int targetMr, int targetCon, int targetWis,
                                           int targetResist, boolean boss, int bossModifier) {
        if (boss && bossModifier == 0) return 0;
        double probability;
        if (attackLevel > defenseLevel) probability = row.high();
        else if (attackLevel == defenseLevel) probability = row.equal();
        else probability = row.low();
        probability += str * row.str();
        probability += dex * row.dex();
        probability += intel * row.intel();
        if (row.magicHit() == 1) probability += magicHit;
        if (row.magicLevel() == 1) probability += magicLevel;
        probability -= targetMr * row.mr();
        if (row.con() > 0) probability -= targetCon / row.con();
        if (row.wis() > 0) probability -= targetWis / row.wis();
        if (row.resist() != 0) probability -= targetResist;
        probability = Math.max(1, Math.min(100, probability));
        int p = (int) probability;
        if (row.id() == 87) {
            p += stunSuccess;
            p = Math.max(1, Math.min(100, p));
        }
        p = Math.min(p, 90);
        if (boss && bossModifier > 0) p /= bossModifier;
        return Math.max(p, 1);
    }

    /** L1MagicPc.calcProbability default branch + calcProbabilityMagic caps (leverage 10). */
    private static int refDefaultChance(int type, int level, int intel, int originalMagicHit, int magicHit,
                                        int targetMr, int dice, int bossModifier, IntUnaryOperator roll) {
        if (bossModifier == 0) return 0;
        int diceCount = refIntMagicBonus(intel) + refMagicLevel(type, level) + (type == 3 ? 1 : -1);
        diceCount = Math.max(diceCount, 1);
        int probability = 0;
        for (int i = 0; i < diceCount; i++) probability += roll.applyAsInt(dice);
        probability = (int) (probability * (10 / 10D));
        probability += (originalMagicHit << 1);
        probability += magicHit;
        probability -= targetMr;
        probability = Math.min(probability, 90);
        if (bossModifier > 0) probability /= bossModifier;
        return Math.max(probability, 1);
    }

    /** L1PcInstance.guardianEncounter: 0..2 lawful guardian, 3..5 evil guardian, -1 none. */
    private static int refGuardianEncounter(long l) {
        if (l >= 10000 && l <= 19999) return 0;
        if (l >= 20000 && l <= 29999) return 1;
        if (l >= 30000 && l <= 39999) return 2;
        if (l >= -19999 && l <= -10000) return 3;
        if (l >= -29999 && l <= -20000) return 4;
        if (l >= -39999 && l <= -30000) return 5;
        return -1;
    }

    /** 380_db skills.skill_level for every non-placeholder row (skill_id:skill_level). */
    private static final String REF_SKILL_LEVELS =
            "1:1,2:1,3:1,4:1,5:1,6:1,7:1,8:1,9:2,10:2,11:2,12:2,13:2,14:2,15:2,16:2,17:3,18:3,19:3,20:3,"
            + "21:3,22:3,23:3,25:4,26:4,27:4,28:4,29:4,30:4,31:4,32:4,33:5,34:5,35:5,36:5,37:5,38:5,39:5,"
            + "40:5,41:6,42:6,43:6,44:6,45:6,46:6,47:6,48:6,49:7,50:7,51:7,52:7,53:7,54:7,55:7,56:7,57:8,"
            + "58:8,59:8,60:8,61:8,62:8,63:8,64:8,65:9,66:9,67:9,68:9,69:9,70:9,71:9,72:9,73:10,74:10,"
            + "75:10,76:10,77:10,78:10,79:10,80:10,87:11,88:11,89:12,90:12,91:12,97:13,98:13,99:13,100:13,"
            + "101:13,102:13,103:13,104:13,105:14,106:14,107:14,108:14,109:14,110:14,111:14,112:14,113:15,"
            + "114:15,115:15,116:15,117:15,118:15,129:17,130:17,131:17,132:17,133:17,134:17,137:18,138:18,"
            + "145:19,146:19,147:19,148:19,149:19,150:19,151:19,152:19,153:20,154:20,155:20,156:20,157:20,"
            + "158:20,159:20,160:20,161:21,162:21,163:21,164:21,165:21,166:21,167:21,168:21,169:22,170:22,"
            + "171:22,172:22,173:22,174:22,175:22,176:22,181:23,182:23,183:23,184:23,185:24,186:24,187:24,"
            + "188:24,189:24,190:24,191:24,192:24,193:25,194:25,195:25,201:26,202:26,203:26,204:26,205:26,"
            + "206:26,207:26,208:26,209:27,210:27,211:27,212:27,213:27,214:27,215:27,216:27,217:28,218:28,"
            + "219:28,220:28";

    // ---------------------------------------------------------------- MR damage reduction

    @Test
    void mrDamageReductionMatchesCalcMrDefenseForEveryMrAndOriginalMagicHit() {
        int checked = 0;
        for (int mr = 0; mr <= 300; mr++) {
            for (int hit = 0; hit <= 4; hit++) {
                for (int dmg : new int[] {0, 1, 2, 7, 33, 50, 123, 260, 999, 4321}) {
                    int expected = refMrDefense(dmg, mr, hit);
                    assertEquals(expected, BattleMagicResistance.damageAfterResistance(dmg, mr, hit),
                            "PC helper mr=" + mr + " hit=" + hit + " dmg=" + dmg);
                    assertEquals(expected, CombatNpcMagic.resistance(dmg, mr, hit),
                            "NPC helper mr=" + mr + " hit=" + hit + " dmg=" + dmg);
                    checked++;
                }
            }
        }
        assertEquals(301 * 5 * 10, checked);
    }

    // ---------------------------------------------------------------- dice damage coefficient

    @Test
    void attributeAndIntelligenceCoefficientMatchesCalcMagicDiceDamage() {
        for (int resist = -128; resist <= 127; resist++) {
            for (int intel = 0; intel <= 90; intel++) {
                for (int dice : new int[] {0, 1, 9, 26, 57, 140, 310}) {
                    assertEquals(Math.max(0, refAttributeAndIntelligence(dice, intel, resist)),
                            BattleElementalResistance.magicDamage(dice, intel, resist),
                            "resist=" + resist + " int+sp=" + intel + " dice=" + dice);
                }
            }
        }
    }

    @Test
    void criticalGateAndOriginalAdditionsMatchCalcMagicDiceDamage() {
        for (int type = 0; type <= 6; type++) {
            for (int originalInt = 0; originalInt <= 30; originalInt++) {
                for (int skillLevel = 1; skillLevel <= 28; skillLevel++) {
                    for (int roll = 1; roll <= 100; roll++) {
                        for (int damage : new int[] {0, 7, 51}) {
                            int crit = refOriginalMagicCritical(type, originalInt);
                            boolean critical = skillLevel <= 6 && roll <= 10 + crit;
                            int expected = damage;
                            if (critical) expected *= 1.5;
                            expected += refOriginalMagicDamage(type, originalInt);
                            expected += 3; // get_magic_modifier_dmg
                            var actual = BattleMagicResistance.attackerBonuses(damage, skillLevel, roll, type, originalInt, 3);
                            assertEquals(expected, actual.damage(),
                                    "type=" + type + " oInt=" + originalInt + " lv=" + skillLevel + " roll=" + roll);
                            assertEquals(critical, actual.critical());
                        }
                    }
                }
            }
        }
    }

    @Test
    void skillLevelMatchesEveryNonPlaceholderSkillsRow() {
        int rows = 0;
        for (String pair : REF_SKILL_LEVELS.split(",")) {
            String[] kv = pair.split(":");
            int id = Integer.parseInt(kv[0]);
            assertEquals(Integer.parseInt(kv[1]), BattleMagicResistance.skillLevel(id), "skill " + id);
            rows++;
        }
        assertEquals(181, rows);
    }

    @Test
    void armorReductionMatchesGetDamageReductionByArmor() {
        for (int base = 0; base <= 60; base++) {
            if (base <= 10) {
                assertEquals(base, BattleMagicResistance.armorDamageReduction(base, 0));
                continue;
            }
            for (int face = 0; face < base - 10; face++) {
                // 1-伺服器: 10 + (nextInt(base - 10) + 1)
                assertEquals(10 + (face + 1), BattleMagicResistance.armorDamageReduction(base, face));
            }
        }
    }

    // ---------------------------------------------------------------- MR sources

    @Test
    void originalIntelligenceAndWisdomBonusesMatchL1PcOriginal() {
        for (int type = 0; type <= 6; type++) {
            for (int stat = 0; stat <= 127; stat++) {
                assertEquals(refOriginalMr(type, stat), BattleMagicResistance.originalMrBonus(type, stat),
                        "originalMr type=" + type + " wis=" + stat);
                assertEquals(refOriginalMagicHit(type, stat), BattleMagicResistance.originalMagicHit(type, stat),
                        "originalMagicHit type=" + type + " int=" + stat);
                assertEquals(refOriginalMagicCritical(type, stat), BattleMagicResistance.originalMagicCritical(type, stat),
                        "originalMagicCritical type=" + type + " int=" + stat);
                assertEquals(refOriginalMagicDamage(type, stat), BattleMagicResistance.originalMagicDamage(type, stat),
                        "originalMagicDamage type=" + type + " int=" + stat);
            }
        }
    }

    @Test
    void classBaseAndWisdomMrMatchResetBaseMrAndTheWisdomTable() {
        for (int type = 0; type <= 6; type++) {
            assertEquals(REF_CLASS_BASE_MR[type], BattleMagicResistance.classBase(type), "class " + type);
        }
        for (int wis = 0; wis <= 80; wis++) {
            // BaseStatTable.getWisMr: key = min(max(wis,0),50)
            assertEquals(REF_WIS_MR[Math.min(wis, 50)], BattleMagicResistance.wisdomMagicResistance(wis), "wis " + wis);
        }
        for (int level = 0; level <= 99; level++) {
            var base = SourceBaseMagicResistance.from(3, level, BattleMagicResistance.wisdomMagicResistance(18), 15);
            // resetBaseMr: class + WIS MR + level/2; resetOriginalMr adds the original-WIS term.
            assertEquals(15 + 10 + level / 2 + 1, base.value(), "wizard level " + level);
        }
    }

    @Test
    void lawfulGuardianMrAndEvilGuardianBonusMatchGuardianEncounter() {
        for (long lawful = -32768; lawful <= 32767; lawful++) {
            int encounter = refGuardianEncounter(lawful);
            int mr = encounter == 0 ? 3 : encounter == 1 ? 6 : encounter == 2 ? 9 : 0;   // getTargetMr
            int evil = encounter == 3 ? 1 : encounter == 4 ? 2 : encounter == 5 ? 3 : 0;  // dice / getTargetSp
            assertEquals(mr, CombatPlayerMagic.guardianMr(lawful), "lawful " + lawful);
            assertEquals(evil, CombatPlayerMagic.evilGuardian(lawful), "lawful " + lawful);
        }
    }

    // ---------------------------------------------------------------- probability

    @Test
    void magicLevelMatchesEveryClassFeature() {
        for (int type = 0; type <= 6; type++) {
            for (int level = 0; level <= 127; level++) {
                assertEquals(refMagicLevel(type, level), CombatRules.magicLevel(type, level),
                        "type=" + type + " level=" + level);
            }
        }
    }

    @Test
    void magicBonusMatchesTheIntelligenceGrowthTableIncludingRowsAboveFortyThree() {
        for (int intel = 0; intel <= 200; intel++) {
            assertEquals(refIntMagicBonus(intel), BattleTurnUndead.magicBonus(intel), "turn-undead INT " + intel);
            assertEquals(refIntMagicBonus(intel), CombatKiringku.magicBonus(intel), "kiringku INT " + intel);
        }
    }

    @Test
    void defaultProbabilityBranchMatchesCalcProbabilityDefault() {
        int checked = 0;
        for (int type = 0; type <= 6; type++) {
            for (int level : new int[] {1, 30, 52, 80}) {
                for (int intel : new int[] {8, 12, 18, 25, 44, 60}) {
                    for (int mr : new int[] {0, 25, 60, 150}) {
                        for (int boss : new int[] {-1, 0, 3, 5}) {
                            for (int dice : new int[] {3, 6, 12}) {
                                int[] a = {0};
                                int[] b = {0};
                                IntUnaryOperator liTo = sides -> (a[0]++ % sides) + 1;
                                IntUnaryOperator ref = sides -> (b[0]++ % sides) + 1;
                                int expected = refDefaultChance(type, level, intel, 2, 3, mr, dice, boss, ref);
                                int actual = BattleTurnUndead.probability(type, level, intel, 2, 3, mr, dice, boss, liTo);
                                assertEquals(expected, actual, "type=" + type + " lv=" + level + " int=" + intel
                                        + " mr=" + mr + " boss=" + boss + " dice=" + dice);
                                checked++;
                            }
                        }
                    }
                }
            }
        }
        assertEquals(7 * 4 * 6 * 4 * 4 * 3, checked);
    }

    @Test
    void configuredProbabilityMatchesCalcProbabilityWithConfigForEveryServer1Row() {
        int checked = 0;
        for (RefRow row : REF_ROWS) {
            var probability = new SourceSkillControls.Probability(row.id(), row.high(), row.equal(), row.low(),
                    row.str(), row.dex(), row.intel(), row.resist(), row.magicLevel() == 1, row.magicHit() == 1,
                    row.mr(), row.con(), row.wis());
            int bossModifier = REF_BOSS.getOrDefault(row.id(), -1);
            for (int casterLevel : new int[] {20, 52, 80}) {
                for (int targetLevel : new int[] {20, 52, 80}) {
                    for (int intel : new int[] {12, 18, 35}) {
                        for (int mr : new int[] {0, 45, 120, 200}) {
                            for (int resist : new int[] {-10, 0, 7, 30}) {
                                for (boolean boss : new boolean[] {false, true}) {
                                    int str = 16, dex = 14, magicLevel = 6, magicHit = 4, stun = 5, con = 18, wis = 16;
                                    var caster = new SourceSkillControls.Caster(casterLevel, str, dex, intel, magicLevel, magicHit, stun);
                                    var resistances = row.resist() == 0 ? Map.<Integer, Integer>of() : Map.of(row.resist(), resist);
                                    var target = new SourceSkillControls.Target(targetLevel, mr, con, wis, resistances, boss);
                                    int expected = refConfiguredChance(row, casterLevel, str, dex, intel, magicLevel, magicHit,
                                            stun, targetLevel, mr, con, wis, row.resist() == 0 ? 0 : resist, boss, bossModifier);
                                    assertEquals(expected, SourceSkillControls.chance(probability, caster, target, bossModifier),
                                            "skill=" + row.id() + " cLv=" + casterLevel + " tLv=" + targetLevel + " int=" + intel
                                                    + " mr=" + mr + " resist=" + resist + " boss=" + boss);
                                    checked++;
                                }
                            }
                        }
                    }
                }
            }
        }
        assertEquals(34 * 3 * 3 * 3 * 4 * 4 * 2, checked);
    }

    @Test
    void configuredProbabilityUsesFractionalConDivisorsAndStatBonusesLikeTheReference() {
        // Not present in the shipped table but supported by both loaders: STR/DEX bonuses and CON/WIS divisors.
        var row = new RefRow(999, 41, 33, 27, 0.15, 0.35, 0.45, 3, 1, 1, 0.07, 2.5, 4.0);
        var probability = new SourceSkillControls.Probability(row.id(), row.high(), row.equal(), row.low(),
                row.str(), row.dex(), row.intel(), row.resist(), true, true, row.mr(), row.con(), row.wis());
        for (int str = 8; str <= 40; str += 4) {
            for (int con = 1; con <= 40; con += 3) {
                for (int mr = 0; mr <= 160; mr += 16) {
                    var caster = new SourceSkillControls.Caster(45, str, 23, 19, 5, 3, 0);
                    var target = new SourceSkillControls.Target(45, mr, con, 17, Map.of(3, 6), false);
                    assertEquals(refConfiguredChance(row, 45, str, 23, 19, 5, 3, 0, 45, mr, con, 17, 6, false, -1),
                            SourceSkillControls.chance(probability, caster, target, -1),
                            "str=" + str + " con=" + con + " mr=" + mr);
                }
            }
        }
    }

    // ---------------------------------------------------------------- controls

    @Test
    void magicClassMultiplierKeysMatchConfigPcDamage() {
        assertEquals(Set.of("Other_ADD_isCrown_MagicPC", "Other_ADD_isKnight_MagicPC", "Other_ADD_isElf_MagicPC",
                        "Other_ADD_isWizard_MagicPC", "Other_ADD_isDarkelf_MagicPC",
                        "Other_ADD_isDragonKnight_MagicPC", "Other_ADD_isIllusionist_MagicPC"),
                CombatMagicDamage.keys());
    }

    /**
     * B12: 1-伺服器 ConfigSkill._loadFromProperties never reads
     * NATURES_BLESSING_HEAL (only _setDefaults assigns 0), so the shipped
     * {@code NATURES_BLESSING_HEAL = 100} line is dead configuration.  LiTo
     * must also resolve it to 0 even when the file says 100.
     */
    @Test
    void naturesBlessingHealIsZeroEvenWhenTheSkillFileSaysOneHundred() throws Exception {
        var skill = new Properties();
        skill.setProperty("NATURES_BLESSING_HEAL", "100");
        var damage = new Properties();
        damage.setProperty("AC_REDUCTION_DIVISOR", "0");
        damage.setProperty("PET_TO_PC_DAMAGE_MULTIPLIER", "1");
        var snapshot = CombatRules.snapshot(skill, damage, Path.of("4-職業_技能設定表.properties"), Path.of("damage.properties"));
        assertEquals(0.0, snapshot.values().get("NATURES_BLESSING_HEAL"));
        assertTrue(snapshot.defaultedKeys().contains("NATURES_BLESSING_HEAL"));
        // Other ConfigSkill defaults that the reference does load stay intact.
        assertEquals(23.0, snapshot.values().get("MORTAL_BODY_ROM"));
        assertEquals(40.0, snapshot.values().get("DISINTEGRATE_CRITICAL_RND"));
        assertEquals(2.0, snapshot.values().get("IMMUNE_TO_HARM"));
    }
}
