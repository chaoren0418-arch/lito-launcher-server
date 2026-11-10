// ported from candidate 50635f7e, integration-lifecycle final frame, issue #67
package local.lito381.server;

/** Exact retained healing-spell modifier order while healing is negative damage. */
final class BattleHealingRecovery {
    private BattleHealingRecovery() {
    }

    static int adjustedSingleTargetAmount(int healingAmount, boolean waterLife, boolean polluteWater, boolean dungeonWater, boolean healingErosion) {
        if (NativeSourceHealingFormula.enabled()) {
            return NativeSourceHealingFormula.single(healingAmount, waterLife, polluteWater, dungeonWater, healingErosion);
        }
        if (healingAmount < 0) {
            throw new IllegalArgumentException("negative spell healing");
        }
        int n2 = waterLife ? Math.multiplyExact(healingAmount, 2) : healingAmount;
        return BattleHealingRecovery.adjustedAmount(n2, polluteWater, dungeonWater, healingErosion);
    }

    static int adjustedAmount(int healingAmount, boolean polluteWater, boolean dungeonWater, boolean healingErosion) {
        if (NativeSourceHealingFormula.enabled()) {
            return NativeSourceHealingFormula.adjusted(healingAmount, polluteWater, dungeonWater, healingErosion);
        }
        if (healingAmount < 0) {
            throw new IllegalArgumentException("negative spell healing");
        }
        int n2 = Math.negateExact(healingAmount);
        if (polluteWater) {
            n2 >>= 1;
        }
        if (dungeonWater) {
            n2 >>= 1;
        }
        if (healingErosion) {
            n2 = Math.negateExact(n2);
        }
        return Math.negateExact(n2);
    }
}
