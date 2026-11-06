package local.lito381.server;

import java.util.function.IntUnaryOperator;

/** Retained 3.81 Turn Undead target gate and probability calculation. */
final class BattleTurnUndead {
    private BattleTurnUndead() {}

    static boolean eligible(SourceNpcPhysicalStats target) {
        if (target == null || !target.turnUndeadEligible()) return false;
        return target.undead() == 1 || target.undead() == 3;
    }

    static int magicBonus(int intelligence) {
        if (intelligence < 0) {
            throw new IllegalArgumentException("intelligence must not be negative");
        }
        if (intelligence <= 5) return -2;
        if (intelligence <= 8) return -1;
        if (intelligence <= 11) return 0;
        if (intelligence <= 14) return 1;
        if (intelligence <= 17) return 2;
        if (intelligence <= 24) return intelligence - 15;
        if (intelligence <= 27) return 10;
        if (intelligence <= 30) return 11;
        if (intelligence <= 33) return 12;
        if (intelligence <= 36) return 13;
        if (intelligence <= 39) return 14;
        return 15;
    }

    static int magicLevel(int characterType, int playerLevel) {
        return CombatRules.magicLevel(characterType, playerLevel);
    }

    static int probability(int characterType, int playerLevel, int effectiveIntelligence,
                           int originalMagicHit, int equipmentMagicHit,
                           int targetMagicResistance, int probabilityDice,
                           int bossModifier, IntUnaryOperator dieRoll) {
        if (originalMagicHit < 0 || targetMagicResistance < 0 || probabilityDice <= 0
                || bossModifier < -1 || dieRoll == null) {
            throw new IllegalArgumentException("invalid turn-undead probability inputs");
        }
        if (bossModifier == 0) return 0;
        int diceCount = magicBonus(effectiveIntelligence)
                + magicLevel(characterType, playerLevel)
                + (characterType == 3 ? 1 : -1);
        diceCount = Math.max(diceCount, 1);
        int probability = 0;
        for (int index = 0; index < diceCount; index++) {
            int roll = dieRoll.applyAsInt(probabilityDice);
            if (roll < 1 || roll > probabilityDice) {
                throw new IllegalArgumentException("turn-undead die roll is outside source die");
            }
            probability += roll;
        }
        probability += originalMagicHit << 1;
        probability += equipmentMagicHit;
        probability -= targetMagicResistance;
        probability = Math.min(probability, 90);
        if (bossModifier > 0) probability /= bossModifier;
        return Math.max(probability, 1);
    }

    static boolean succeeds(int probability, int percentileRoll) {
        if (probability < 0 || probability > 90
                || percentileRoll < 1 || percentileRoll > 100) {
            throw new IllegalArgumentException("invalid turn-undead success roll");
        }
        return probability >= percentileRoll;
    }
}
