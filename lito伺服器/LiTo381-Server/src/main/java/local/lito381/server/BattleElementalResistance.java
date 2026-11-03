package local.lito381.server;

/** Same-family elemental arithmetic used by ground and ordinary magic damage. */
final class BattleElementalResistance {
    private BattleElementalResistance() {}

    /**
     * Applies the exact retained {@code EffectFirewallTimer} arithmetic.
     * Negative/zero resistance is intentionally ignored by that timer;
     * positive resistance is quantized through {@code floor(0.32 * value)}
     * and divided by 32 before integer truncation. Damage is clamped at zero.
     */
    static int fireWallDamage(int sourceDamage, int fireResistance) {
        if (sourceDamage < 0) {
            throw new IllegalArgumentException("Fire Wall source damage cannot be negative");
        }
        requireElementalResistance(fireResistance, "fire resistance");
        if (sourceDamage == 0 || fireResistance <= 0) return sourceDamage;
        int resistanceFloor = (int) (0.32 * Math.abs(fireResistance));
        double defence = resistanceFloor / 32.0;
        return Math.max((int) ((1.0 - defence) * sourceDamage), 0);
    }

    /**
     * Applies the retained {@code L1MagicPc.calcMagicDiceDamage} INT/attribute
     * coefficient to an already-rolled spell value.  The selected classic
     * character plus its qualified equipment snapshot are the authoritative
     * input. Callers pass {@code INT + itemSP}, reproducing the same-family
     * {@code max(INT + itemSP - 12, 1)} term.
     *
     * <p>Unlike {@link #fireWallDamage(int, int)}, ordinary magic preserves
     * signed elemental resistance: a negative value increases damage.  Java's
     * compound-assignment truncation is reproduced by the final cast.</p>
     */
    static int magicDamage(int sourceDamage, int casterIntelligence,
                           int elementalResistance) {
        if (sourceDamage < 0) {
            throw new IllegalArgumentException("magic source damage cannot be negative");
        }
        if (casterIntelligence < 0) {
            throw new IllegalArgumentException(
                    "caster INT plus item SP must not be negative");
        }
        requireElementalResistance(elementalResistance, "elemental resistance");
        if (sourceDamage == 0) return 0;

        int resistanceFloor = (int) (0.32 * Math.abs(elementalResistance));
        if (elementalResistance < 0) resistanceFloor *= -1;
        double defence = resistanceFloor / 32.0;
        int intelligenceTerm = Math.max(casterIntelligence - 12, 1);
        double coefficient = Math.max(
                1.0 - defence + intelligenceTerm * 3.0 / 32.0, 0.0);
        return Math.max((int) (sourceDamage * coefficient), 0);
    }

    private static void requireElementalResistance(int resistance, String name) {
        int minimum = NumericBreakthrough.elementalResistanceEnabled()
                ? NumericBreakthrough.MIN_ELEMENTAL_RESISTANCE : Byte.MIN_VALUE;
        int maximum = NumericBreakthrough.elementalResistanceEnabled()
                ? NumericBreakthrough.MAX_ELEMENTAL_RESISTANCE : Byte.MAX_VALUE;
        if (resistance < minimum || resistance > maximum) {
            throw new IllegalArgumentException(name + " is outside negotiated range");
        }
    }
}
