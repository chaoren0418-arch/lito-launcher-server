package local.lito381.server;

import java.util.concurrent.TimeUnit;

/** Server-authoritative leases for the three native inventory-weight spells. */
final class BattleWeightStatus {
    record Snapshot(int decreaseWeightSeconds, int exoticVitalizeSeconds,
                    int additionalFireSeconds) {
        boolean decreaseWeightActive() { return decreaseWeightSeconds > 0; }
        boolean regenerationBypassActive() {
            return exoticVitalizeSeconds > 0 || additionalFireSeconds > 0;
        }
    }

    private long decreaseWeightExpiresAtNanos;
    private long exoticVitalizeExpiresAtNanos;
    private long additionalFireExpiresAtNanos;

    synchronized void activate(int skillId, long nowNanos, int durationSeconds) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF) {
            throw new IllegalArgumentException("invalid native weight-spell lease");
        }
        long expiresAt = Math.addExact(nowNanos,
                TimeUnit.SECONDS.toNanos(durationSeconds));
        switch (skillId) {
            case BattleSpellCatalog.DECREASE_WEIGHT ->
                    decreaseWeightExpiresAtNanos = expiresAt;
            case BattleSpellCatalog.EXOTIC_VITALIZE ->
                    exoticVitalizeExpiresAtNanos = expiresAt;
            case BattleSpellCatalog.ADDITIONAL_FIRE ->
                    additionalFireExpiresAtNanos = expiresAt;
            default -> throw new IllegalArgumentException("unknown native weight spell");
        }
    }

    synchronized Snapshot snapshot(long nowNanos) {
        return new Snapshot(
                remainingSeconds(nowNanos, decreaseWeightExpiresAtNanos),
                remainingSeconds(nowNanos, exoticVitalizeExpiresAtNanos),
                remainingSeconds(nowNanos, additionalFireExpiresAtNanos));
    }

    synchronized boolean expire(long nowNanos) {
        boolean changed = false;
        if (expired(nowNanos, decreaseWeightExpiresAtNanos)) {
            decreaseWeightExpiresAtNanos = 0;
            changed = true;
        }
        if (expired(nowNanos, exoticVitalizeExpiresAtNanos)) {
            exoticVitalizeExpiresAtNanos = 0;
            changed = true;
        }
        if (expired(nowNanos, additionalFireExpiresAtNanos)) {
            additionalFireExpiresAtNanos = 0;
            changed = true;
        }
        return changed;
    }

    synchronized void clear() {
        decreaseWeightExpiresAtNanos = 0;
        exoticVitalizeExpiresAtNanos = 0;
        additionalFireExpiresAtNanos = 0;
    }

    synchronized boolean cancelMagicEffects() {
        boolean present = decreaseWeightExpiresAtNanos != 0
                || exoticVitalizeExpiresAtNanos != 0
                || additionalFireExpiresAtNanos != 0;
        clear();
        return present;
    }

    private static boolean expired(long nowNanos, long expiresAtNanos) {
        return expiresAtNanos != 0 && nowNanos >= expiresAtNanos;
    }

    private static int remainingSeconds(long nowNanos, long expiresAtNanos) {
        if (expiresAtNanos == 0 || nowNanos >= expiresAtNanos) return 0;
        long second = TimeUnit.SECONDS.toNanos(1);
        long remaining = (expiresAtNanos - nowNanos + second - 1) / second;
        return (int) Math.min(0xFFFFL, Math.max(1L, remaining));
    }
}
