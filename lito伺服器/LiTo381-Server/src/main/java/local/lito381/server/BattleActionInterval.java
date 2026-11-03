package local.lito381.server;

/** Applies classic server status multipliers to native sprite action intervals. */
final class BattleActionInterval {
    static final double HASTE_MULTIPLIER = 0.755d;
    static final double SLOW_MULTIPLIER = 2.0d;
    static final double FAST_MOVEMENT_MULTIPLIER = 0.665d;
    static final double FAST_ATTACK_MULTIPLIER = 0.775d;
    static final double WIND_SHACKLE_MULTIPLIER = 1.25d;
    private static final int MINIMUM_NATIVE_INTERVAL_MILLIS = 40;

    private BattleActionInterval() {
    }

    static int npcMillis(int spriteId, int action, boolean haste) {
        return npcMillis(spriteId, action, haste, false);
    }

    static int npcMillis(int spriteId, int action, boolean haste, boolean slowed) {
        return npcMillis(spriteId, action, haste, slowed, false);
    }

    static int npcMillis(int spriteId, int action, boolean haste,
                         boolean slowed, boolean windShackle) {
        int interval = nativeMillis(spriteId, action);
        return npcSourceMillis(interval, haste, slowed, windShackle);
    }

    /**
     * Applies the retained status multipliers to a source NPC template's
     * {@code atkspeed/passisspeed}.  Some native monster sprites deliberately
     * have no row for action 1 in the selected client SPR table, while their
     * authoritative source template still supplies a usable interval.
     */
    static int npcSourceMillis(int sourceMillis, boolean haste, boolean slowed) {
        return npcSourceMillis(sourceMillis, haste, slowed, false);
    }

    static int npcSourceMillis(int sourceMillis, boolean haste,
                               boolean slowed, boolean windShackle) {
        if (sourceMillis <= 0) {
            throw new IllegalArgumentException("invalid source NPC interval");
        }
        int interval = Math.max(MINIMUM_NATIVE_INTERVAL_MILLIS, sourceMillis);
        if (slowed) {
            interval = (int) (interval * SLOW_MULTIPLIER);
        } else if (haste) {
            // Selected classic server ActionIntervalCalculator.multiply uses
            // this exact cast, including truncation toward zero.
            interval = (int) (interval * HASTE_MULTIPLIER);
        }
        if (windShackle) {
            interval = (int) (interval * WIND_SHACKLE_MULTIPLIER);
        }
        return Math.max(1, interval);
    }

    /**
     * Retained player movement order: Haste/Slow family first, then the
     * HOLY_WALK/MOVING_ACCELERATION/WIND_WALK movement-only family.
     */
    static int movementMillis(int spriteId, int walkAction,
                              boolean haste, boolean fastMovement) {
        return movementMillis(spriteId, walkAction, haste, false, fastMovement);
    }

    static int movementMillis(int spriteId, int walkAction,
                              boolean haste, boolean slowed,
                              boolean fastMovement) {
        int interval = nativeMillis(spriteId, walkAction);
        if (slowed) interval = (int) (interval * SLOW_MULTIPLIER);
        else if (haste) interval = (int) (interval * HASTE_MULTIPLIER);
        if (fastMovement) interval = (int) (interval * FAST_MOVEMENT_MULTIPLIER);
        return Math.max(1, interval);
    }

    /** Retained player order: Haste/Slow first, then Bloodlust on ATTACK only. */
    static int attackMillis(int spriteId, int attackAction,
                            boolean haste, boolean slowed,
                            boolean fastAttack) {
        int interval = nativeMillis(spriteId, attackAction);
        if (slowed) interval = (int) (interval * SLOW_MULTIPLIER);
        else if (haste) interval = (int) (interval * HASTE_MULTIPLIER);
        if (fastAttack) interval = (int) (interval * FAST_ATTACK_MULTIPLIER);
        return Math.max(1, interval);
    }

    private static int nativeMillis(int spriteId, int action) {
        long nativeMillis = BattleAnimationCatalog.get()
                .requireTimedAction(spriteId, action).durationMillis();
        return Math.max(MINIMUM_NATIVE_INTERVAL_MILLIS,
                Math.toIntExact(nativeMillis));
    }
}
