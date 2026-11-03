package local.lito381.server;

import java.util.concurrent.TimeUnit;

/** Authoritative retained skill-134 one-shot magic-reflection lease. */
final class BattleCounterMirrorStatus {
    record Decision(boolean active, boolean eligible, boolean triggered,
                    int wisdom, int roll, int reflectedDamage) {
        Decision {
            if (wisdom < 0 || wisdom > 255 || roll < 0 || roll > 99
                    || reflectedDamage < 0
                    || triggered != (active && eligible
                        && wisdom >= roll && reflectedDamage > 0)) {
                throw new IllegalArgumentException(
                        "invalid Counter Mirror decision");
            }
        }

        static Decision inactive(int wisdom, int roll) {
            return new Decision(false, false, false,
                    wisdom, roll, 0);
        }
    }

    private long expiresAtNanos;

    synchronized void activate(long nowNanos, int durationSeconds) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF) {
            throw new IllegalArgumentException(
                    "invalid native Counter Mirror lease");
        }
        expiresAtNanos = Math.addExact(nowNanos,
                TimeUnit.SECONDS.toNanos(durationSeconds));
    }

    synchronized boolean active(long nowNanos) {
        if (nowNanos < 0) throw new IllegalArgumentException("negative clock");
        return expiresAtNanos != 0 && nowNanos < expiresAtNanos;
    }

    /**
     * Retained comparison is {@code wisdom >= random.nextInt(100)}.  The
     * lease is consumed only by an eligible, positive-damage successful roll.
     */
    synchronized Decision resolve(
            long nowNanos, int wisdom, int zeroBasedRoll,
            int incomingDamage, boolean eligibleSource) {
        if (nowNanos < 0 || wisdom < 0 || wisdom > 255
                || zeroBasedRoll < 0 || zeroBasedRoll > 99
                || incomingDamage < 0) {
            throw new IllegalArgumentException(
                    "invalid Counter Mirror resolution input");
        }
        boolean leaseActive = active(nowNanos);
        if (!leaseActive) {
            if (expiresAtNanos != 0 && nowNanos >= expiresAtNanos) {
                expiresAtNanos = 0;
            }
            return Decision.inactive(wisdom, zeroBasedRoll);
        }
        boolean triggered = eligibleSource && incomingDamage > 0
                && wisdom >= zeroBasedRoll;
        if (triggered) expiresAtNanos = 0;
        return new Decision(true, eligibleSource, triggered,
                wisdom, zeroBasedRoll, triggered ? incomingDamage : 0);
    }

    synchronized boolean expire(long nowNanos) {
        if (expiresAtNanos == 0 || nowNanos < expiresAtNanos) return false;
        expiresAtNanos = 0;
        return true;
    }

    synchronized boolean cancel() {
        if (expiresAtNanos == 0) return false;
        expiresAtNanos = 0;
        return true;
    }

    synchronized void clear() {
        expiresAtNanos = 0;
    }
}
