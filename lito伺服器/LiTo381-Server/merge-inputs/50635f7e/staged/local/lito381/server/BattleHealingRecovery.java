package local.lito381.server;

/** Exact retained healing-spell modifier order while healing is negative damage. */
final class BattleHealingRecovery {
    private BattleHealingRecovery() {
    }

    static int adjustedSingleTargetAmount(int n, boolean bl, boolean bl2, boolean bl3, boolean bl4) {
        if (NativeSourceHealingFormula.enabled()) {
            return NativeSourceHealingFormula.single(n, bl, bl2, bl3, bl4);
        }
        if (n < 0) {
            throw new IllegalArgumentException("negative spell healing");
        }
        int n2 = bl ? Math.multiplyExact(n, 2) : n;
        return BattleHealingRecovery.adjustedAmount(n2, bl2, bl3, bl4);
    }

    static int adjustedAmount(int n, boolean bl, boolean bl2, boolean bl3) {
        if (NativeSourceHealingFormula.enabled()) {
            return NativeSourceHealingFormula.adjusted(n, bl, bl2, bl3);
        }
        if (n < 0) {
            throw new IllegalArgumentException("negative spell healing");
        }
        int n2 = Math.negateExact(n);
        if (bl) {
            n2 >>= 1;
        }
        if (bl2) {
            n2 >>= 1;
        }
        if (bl3) {
            n2 = Math.negateExact(n2);
        }
        return Math.negateExact(n2);
    }
}
