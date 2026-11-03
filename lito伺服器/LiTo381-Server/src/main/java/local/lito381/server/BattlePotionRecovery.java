package local.lito381.server;

/** Exact retained {@code UserAddHp} modifier order after the base potion roll. */
final class BattlePotionRecovery {
    private BattlePotionRecovery() {}

    static int adjustedAmount(
            int baseAmount, int percentageBonus, int fixedBonus,
            boolean polluteWater, boolean dungeonWater,
            boolean potionErosion) {
        if (baseAmount < 0) {
            throw new IllegalArgumentException("negative base potion recovery");
        }
        int amount = baseAmount;
        if (percentageBonus > 0) {
            amount = Math.addExact(amount,
                    Math.multiplyExact(amount, percentageBonus) / 100);
        }
        if (fixedBonus > 0) {
            amount = Math.addExact(amount, fixedBonus);
        }
        if (polluteWater) amount >>= 1;
        if (dungeonWater) amount >>= 1;
        if (potionErosion) amount = Math.negateExact(amount);
        return amount;
    }
}
