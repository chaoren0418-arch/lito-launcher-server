package local.lito381.server;

/** Exact retained healing-spell modifier order while healing is negative damage. */
final class BattleHealingRecovery {
    private BattleHealingRecovery() {}

    /**
     * Retained single-target order: Water Life doubles the negative healing
     * damage first, then Pollute Water and the Water Dragon modifiers run.
     * The caller owns the one-shot Water Life lease and consumes it only
     * after the cast has passed admission.
     */
    static int adjustedSingleTargetAmount(
            int healingAmount, boolean waterLife, boolean polluteWater,
            boolean dungeonWater, boolean healingErosion) {
        if (healingAmount < 0) {
            throw new IllegalArgumentException("negative spell healing");
        }
        int beforeRecovery = waterLife
                ? Math.multiplyExact(healingAmount, 2) : healingAmount;
        return adjustedAmount(
                beforeRecovery, polluteWater, dungeonWater, healingErosion);
    }

    static int adjustedAmount(
            int healingAmount, boolean polluteWater,
            boolean dungeonWater, boolean healingErosion) {
        if (healingAmount < 0) {
            throw new IllegalArgumentException("negative spell healing");
        }
        int damage = Math.negateExact(healingAmount);
        if (polluteWater) damage >>= 1;
        if (dungeonWater) damage >>= 1;
        if (healingErosion) damage = Math.negateExact(damage);
        return Math.negateExact(damage);
    }
}
