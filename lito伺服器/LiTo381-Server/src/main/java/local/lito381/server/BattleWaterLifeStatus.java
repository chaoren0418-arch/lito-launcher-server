package local.lito381.server;

import java.util.concurrent.TimeUnit;

/** Minimal authoritative lease needed by retained heal/Nature's Touch consumption. */
final class BattleWaterLifeStatus {
    private long expiresAtNanos;

    synchronized boolean activate(long nowNanos, int durationSeconds) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF) {
            throw new IllegalArgumentException("invalid native Water Life lease");
        }
        boolean refreshed = expiresAtNanos != 0 && nowNanos < expiresAtNanos;
        expiresAtNanos = Math.addExact(
                nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        return refreshed;
    }

    synchronized boolean active(long nowNanos) {
        if (nowNanos < 0) throw new IllegalArgumentException("invalid Water Life time");
        return expiresAtNanos != 0 && nowNanos < expiresAtNanos;
    }

    synchronized boolean expire(long nowNanos) {
        if (!active(nowNanos) && expiresAtNanos != 0 && nowNanos >= expiresAtNanos) {
            expiresAtNanos = 0;
            return true;
        }
        return false;
    }

    synchronized boolean cancel() {
        if (expiresAtNanos == 0) return false;
        expiresAtNanos = 0;
        return true;
    }
}
