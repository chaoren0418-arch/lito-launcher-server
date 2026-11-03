package local.lito381.server;

import java.util.concurrent.TimeUnit;

/** Transient cancelable one-shot melee lease for native skill182. */
final class BattleBurningSlashStatus {
    record Snapshot(boolean active, int remainingSeconds) {
        static Snapshot none() {
            return new Snapshot(false, 0);
        }
    }

    private long expiresAtNanos;

    synchronized boolean activate(long nowNanos, int durationSeconds) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF) {
            throw new IllegalArgumentException("invalid native Burning Slash lease");
        }
        boolean refreshed = expiresAtNanos != 0 && nowNanos < expiresAtNanos;
        expiresAtNanos = Math.addExact(
                nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        return refreshed;
    }

    synchronized Snapshot snapshot(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("invalid Burning Slash time");
        }
        if (expiresAtNanos == 0 || nowNanos >= expiresAtNanos) {
            return Snapshot.none();
        }
        long remainingNanos = expiresAtNanos - nowNanos;
        long second = TimeUnit.SECONDS.toNanos(1);
        int remaining = (int) Math.min(0xFFFFL,
                Math.max(1L, (remainingNanos + second - 1) / second));
        return new Snapshot(true, remaining);
    }

    synchronized boolean consume(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("invalid Burning Slash time");
        }
        if (expiresAtNanos == 0) return false;
        if (nowNanos >= expiresAtNanos) {
            clear();
            return false;
        }
        clear();
        return true;
    }

    synchronized boolean expire(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("invalid Burning Slash time");
        }
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
    }
}
