package local.lito381.server;

/** Retained NPC-to-PC d20/AC hit envelope, including target dodge points. */
final class BattleNpcPhysicalAttack {
    private BattleNpcPhysicalAttack() {}

    static int hitChance(int level, int hitUp, int targetArmorClass,
                         int targetDodgeModifier, int d20,
                         int negativeAcRoll) {
        return hitChance(level, hitUp, targetArmorClass,
                targetDodgeModifier, 0, d20, negativeAcRoll);
    }

    static int hitChance(int level, int hitUp, int targetArmorClass,
                         int targetDodgeModifier, int targetDodgeDown,
                         int d20, int negativeAcRoll) {
        if (level < 1 || hitUp < -127 || hitUp > 127
                || targetDodgeModifier < 0 || targetDodgeModifier > 10
                || targetDodgeDown < 0 || targetDodgeDown > 10
                || d20 < 1 || d20 > 20) {
            throw new IllegalArgumentException("invalid NPC physical-hit input");
        }
        int hitRate = Math.addExact(Math.addExact(level, 5), hitUp);
        int attackerDice = d20 + hitRate - 3
                - targetDodgeModifier + targetDodgeDown;
        int defenderDice;
        if (targetArmorClass >= 0) {
            if (negativeAcRoll != 0) {
                throw new IllegalArgumentException(
                        "positive AC cannot have a defense roll");
            }
            defenderDice = 10 - targetArmorClass;
        } else {
            int maximum = negativeAcRollMaximum(targetArmorClass);
            if (negativeAcRoll < 1 || negativeAcRoll > maximum) {
                throw new IllegalArgumentException("invalid NPC negative-AC roll");
            }
            defenderDice = 10 + negativeAcRoll;
        }
        if (attackerDice <= hitRate) return 15;
        if (attackerDice >= hitRate + 19) return 100;
        return attackerDice > defenderDice ? 100 : 15;
    }

    static boolean hits(int hitChance, int d100) {
        if ((hitChance != 15 && hitChance != 100)
                || d100 < 1 || d100 > 100) {
            throw new IllegalArgumentException("invalid NPC physical-hit roll");
        }
        return d100 <= hitChance;
    }

    static int negativeAcRollMaximum(int targetArmorClass) {
        return targetArmorClass < 0 ? -targetArmorClass : 0;
    }
}
