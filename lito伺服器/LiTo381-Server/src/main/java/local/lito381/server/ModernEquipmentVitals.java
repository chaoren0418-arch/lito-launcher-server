package local.lito381.server;

/**
 * Pure live-match equipment rule. Increasing a maximum never grants HP/MP;
 * decreasing it clips only the excess. This is not a heal, death, resurrection,
 * equipment permission, durable commit or match publication. Never use saved
 * SQL HP/MP or FULL_NEW_MATCH initial values as the live inputs.
 */
final class ModernEquipmentVitals {
    record Vitals(long hp, long maximumHp, long mp, long maximumMp) {
        Vitals {
            if (maximumHp < 1 || maximumMp < 0 || hp < 0 || hp > maximumHp
                    || mp < 0 || mp > maximumMp) throw invalid();
        }
    }

    static Vitals retain(Vitals live, long maximumHp, long maximumMp) {
        java.util.Objects.requireNonNull(live);
        if (maximumHp < 1 || maximumMp < 0) throw invalid();
        // No arithmetic delta, ratio, narrowing or synthetic minimum-one heal.
        return new Vitals(Math.min(live.hp(), maximumHp), maximumHp,
                Math.min(live.mp(), maximumMp), maximumMp);
    }

    private ModernEquipmentVitals() {}
    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("invalid live equipment vitals");
    }
}
