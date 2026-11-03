package local.lito381.server;

/** Retained L1AttackNpc poison_atk roll and timing contract. */
final class ClassicWorldMonsterPoisonAttack {
    static final int PROC_PERCENT = 15;
    static final int DAMAGE_DURATION_SECONDS = 30;
    static final int DAMAGE_TICK_MILLIS = 3_000;
    static final int DAMAGE_PER_TICK = 5;
    static final int PARALYSIS_WARNING_MILLIS = 20_000;
    static final int PARALYSIS_DURATION_MILLIS = 45_000;

    enum Kind {
        NONE,
        DAMAGE,
        SILENCE,
        PARALYSIS
    }

    private ClassicWorldMonsterPoisonAttack() {}

    static Kind resolve(int poisonAttack, int oneBasedRoll) {
        if (poisonAttack < 0 || oneBasedRoll < 1 || oneBasedRoll > 100) {
            throw new IllegalArgumentException("invalid retained NPC poison roll");
        }
        if (oneBasedRoll > PROC_PERCENT) return Kind.NONE;
        return switch (poisonAttack) {
            case 1 -> Kind.DAMAGE;
            case 2 -> Kind.SILENCE;
            case 4 -> Kind.PARALYSIS;
            default -> Kind.NONE;
        };
    }
}
