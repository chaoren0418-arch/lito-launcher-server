package local.lito381.server;

import java.util.function.IntUnaryOperator;

/** Retained 3.81 skill-39 target gate, probability and transfer calculation. */
final class BattleManaDrain {
    private BattleManaDrain() {}

    static boolean eligibleNpc(SourceNpcPhysicalStats target) {
        return target != null && target.eraseEligible();
    }

    static int probability(int characterType, int playerLevel, int effectiveIntelligence,
                           int originalMagicHit, int equipmentMagicHit,
                           int targetMagicResistance, int probabilityDice,
                           int bossModifier, IntUnaryOperator dieRoll) {
        return BattleTurnUndead.probability(
                characterType, playerLevel, effectiveIntelligence,
                originalMagicHit, equipmentMagicHit, targetMagicResistance,
                probabilityDice, bossModifier, dieRoll);
    }

    static boolean succeeds(int probability, int percentileRoll) {
        return BattleTurnUndead.succeeds(probability, percentileRoll);
    }

    static int requestedDrain(int effectiveIntelligence, IntUnaryOperator dieRoll) {
        if (effectiveIntelligence < 0 || dieRoll == null) {
            throw new IllegalArgumentException("invalid mana-drain inputs");
        }
        int roll = dieRoll.applyAsInt(10);
        if (roll < 1 || roll > 10) {
            throw new IllegalArgumentException("mana-drain roll is outside d10");
        }
        return roll + 4 + effectiveIntelligence / 2;
    }
}
