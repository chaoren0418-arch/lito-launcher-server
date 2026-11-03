package local.lito381.server;

import java.util.concurrent.TimeUnit;

/** Native skill167 lease: attack and spell actions take 25 percent longer. */
final class BattleWindShackleStatus {
    static final double ACTION_MULTIPLIER = 1.25d;

    record Snapshot(boolean active, int remainingSeconds,
                    long expiresAtNanos, long nextActionNanos) {}

    private long expiresAtNanos;
    private long nextActionNanos;

    synchronized boolean activate(long nowNanos, int durationSeconds) {
        validateTime(nowNanos, durationSeconds);
        if (active(nowNanos)) return false;
        expiresAtNanos = Math.addExact(nowNanos,
                TimeUnit.SECONDS.toNanos(durationSeconds));
        nextActionNanos = 0;
        return true;
    }

    synchronized boolean tryAction(
            long nowNanos, long nativeDurationMillis) {
        if (nowNanos < 0 || nativeDurationMillis <= 0) {
            throw new IllegalArgumentException("invalid Wind Shackle action time");
        }
        if (!active(nowNanos)) return true;
        if (nowNanos < nextActionNanos) return false;
        nextActionNanos = Math.addExact(nowNanos,
                TimeUnit.MILLISECONDS.toNanos(
                        actionIntervalMillis(nativeDurationMillis)));
        return true;
    }

    synchronized boolean cancel() {
        boolean changed = expiresAtNanos != 0 || nextActionNanos != 0;
        expiresAtNanos = 0;
        nextActionNanos = 0;
        return changed;
    }

    synchronized boolean expire(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("invalid Wind Shackle expiry time");
        }
        if (expiresAtNanos == 0 || nowNanos < expiresAtNanos) return false;
        expiresAtNanos = 0;
        nextActionNanos = 0;
        return true;
    }

    synchronized Snapshot snapshot(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("invalid Wind Shackle snapshot time");
        }
        if (!active(nowNanos)) {
            if (expiresAtNanos != 0) {
                expiresAtNanos = 0;
                nextActionNanos = 0;
            }
            return new Snapshot(false, 0, 0, 0);
        }
        long remaining = expiresAtNanos - nowNanos;
        long seconds = remaining / 1_000_000_000L;
        if (remaining % 1_000_000_000L != 0) seconds++;
        return new Snapshot(true, Math.toIntExact(seconds),
                expiresAtNanos, nextActionNanos);
    }

    static long actionIntervalMillis(long nativeDurationMillis) {
        if (nativeDurationMillis <= 0) {
            throw new IllegalArgumentException("invalid native action interval");
        }
        return Math.max(1L, (long) (nativeDurationMillis * ACTION_MULTIPLIER));
    }

    private boolean active(long nowNanos) {
        return expiresAtNanos != 0 && nowNanos < expiresAtNanos;
    }

    private static void validateTime(long nowNanos, int durationSeconds) {
        if (nowNanos < 0 || durationSeconds <= 0) {
            throw new IllegalArgumentException("invalid Wind Shackle lease");
        }
    }
}
