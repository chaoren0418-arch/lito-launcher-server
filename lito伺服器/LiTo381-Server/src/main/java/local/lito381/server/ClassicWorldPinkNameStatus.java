package local.lito381.server;

/**
 * Retained {@code L1PinkName} timer state for a classic-WORLD player.
 *
 * <p>The native server publishes {@code S_PinkName(..., 180)} for every
 * eligible accepted hit, but only the first hit owns the timer.  That timer
 * keeps its original map and position and clears the state when the player
 * dies, changes world, leaves the original native screen, or reaches the
 * retained 180-tick limit.</p>
 */
final class ClassicWorldPinkNameStatus {
    static final int WIRE_SECONDS = 180;
    private static final long TICK_NANOS = 1_000_000_000L;
    /* PinkNameTimer checks the limit before incrementing, so tick 181 clears. */
    private static final long RETAINED_EXPIRY_NANOS =
            (WIRE_SECONDS + 1L) * TICK_NANOS;

    record Snapshot(boolean active, long generation, int mapId, int instanceId,
                    int originX, int originY, long startedNanos,
                    long deadlineNanos) {
        static Snapshot inactive() {
            return new Snapshot(false, 0, -1, -1, 0, 0, 0, 0);
        }
    }

    enum StopReason {
        NONE,
        PHASE_LEFT_WORLD,
        MAP_CHANGED,
        LEFT_NATIVE_SCREEN,
        PLAYER_DIED,
        TIMER_EXPIRED
    }

    record Publication(boolean started, Snapshot snapshot) {}
    record Expiration(boolean cleared, StopReason reason, Snapshot previous) {
        static Expiration unchanged(Snapshot snapshot) {
            return new Expiration(false, StopReason.NONE, snapshot);
        }
    }

    private long generation;
    private Snapshot active = Snapshot.inactive();

    synchronized Publication publishActive(
            int mapId, int instanceId, int x, int y, long nowNanos) {
        if (active.active()) return new Publication(false, active);
        generation = Math.addExact(generation, 1L);
        active = new Snapshot(true, generation, mapId, instanceId, x, y,
                nowNanos, Math.addExact(nowNanos, RETAINED_EXPIRY_NANOS));
        return new Publication(true, active);
    }

    synchronized boolean active() {
        return active.active();
    }

    synchronized Snapshot snapshot() {
        return active;
    }

    synchronized Expiration expire(
            boolean inClassicWorld, boolean alive,
            int currentMapId, int currentInstanceId, int currentX, int currentY,
            long nowNanos) {
        Snapshot before = active;
        if (!before.active()) return Expiration.unchanged(before);
        StopReason reason = StopReason.NONE;
        if (!inClassicWorld) {
            reason = StopReason.PHASE_LEFT_WORLD;
        } else if (before.mapId() != currentMapId
                || before.instanceId() != currentInstanceId) {
            reason = StopReason.MAP_CHANGED;
        } else if (!alive) {
            reason = StopReason.PLAYER_DIED;
        } else if (!BattleSpellCatalog.isInNativeScreen(
                currentX, currentY, before.originX(), before.originY())) {
            reason = StopReason.LEFT_NATIVE_SCREEN;
        } else if (nowNanos >= before.deadlineNanos()) {
            reason = StopReason.TIMER_EXPIRED;
        }
        if (reason == StopReason.NONE) return Expiration.unchanged(before);
        active = Snapshot.inactive();
        return new Expiration(true, reason, before);
    }
}
