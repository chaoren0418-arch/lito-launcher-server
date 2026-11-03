package local.lito381.server;

import java.util.concurrent.TimeUnit;

/** Server-authoritative non-player lease for retained skill217 Panic. */
final class BattlePanicStatus {
    record Snapshot(int abilityScoreModifier, int remainingSeconds) {
        boolean active() {
            return remainingSeconds > 0;
        }
    }

    private long expiresAtNanos;
    private int abilityScoreModifier;

    /** Returns true only for the first application; an active lease is unchanged. */
    synchronized boolean activateIfAbsent(
            long nowNanos, int durationSeconds, int modifier) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || modifier >= 0 || modifier < -127) {
            throw new IllegalArgumentException("invalid native Panic lease");
        }
        if (expiresAtNanos != 0 && nowNanos < expiresAtNanos) return false;
        expiresAtNanos = Math.addExact(
                nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        abilityScoreModifier = modifier;
        return true;
    }

    synchronized Snapshot snapshot(long nowNanos) {
        if (expiresAtNanos == 0 || nowNanos >= expiresAtNanos) {
            return new Snapshot(0, 0);
        }
        long remainingNanos = expiresAtNanos - nowNanos;
        long remainingSeconds = (remainingNanos + TimeUnit.SECONDS.toNanos(1) - 1)
                / TimeUnit.SECONDS.toNanos(1);
        return new Snapshot(abilityScoreModifier,
                (int) Math.min(0xFFFFL, Math.max(1L, remainingSeconds)));
    }

    synchronized boolean expire(long nowNanos) {
        if (expiresAtNanos == 0 || nowNanos < expiresAtNanos) return false;
        clear();
        return true;
    }

    synchronized boolean cancel() {
        if (expiresAtNanos == 0) return false;
        clear();
        return true;
    }

    synchronized void clear() {
        expiresAtNanos = 0;
        abilityScoreModifier = 0;
    }
}
