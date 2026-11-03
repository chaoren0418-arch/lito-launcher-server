package local.lito381.server;

import java.util.concurrent.TimeUnit;

/** Persistent, cancelable player dodge-down lease for native skill188. */
final class BattleResistFearStatus {
    record Snapshot(boolean active, int dodgeDown, int remainingSeconds) {
        static Snapshot none() {
            return new Snapshot(false, 0, 0);
        }
    }

    private long expiresAtNanos;
    private int dodgeDown;

    synchronized boolean activate(
            long nowNanos, int durationSeconds, int dodgeDownModifier) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || dodgeDownModifier <= 0 || dodgeDownModifier > 10) {
            throw new IllegalArgumentException("invalid native Resist Fear lease");
        }
        boolean alreadyActive = expiresAtNanos != 0 && nowNanos < expiresAtNanos;
        // Retained RESIST_FEAR checks !hasSkillEffect before installing the
        // lease, so a successful recast spends resources but does not refresh.
        if (alreadyActive) return true;
        expiresAtNanos = Math.addExact(
                nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        dodgeDown = dodgeDownModifier;
        return false;
    }

    synchronized Snapshot snapshot(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("invalid Resist Fear time");
        }
        if (expiresAtNanos == 0 || nowNanos >= expiresAtNanos) {
            return Snapshot.none();
        }
        long remainingNanos = expiresAtNanos - nowNanos;
        long second = TimeUnit.SECONDS.toNanos(1);
        int remaining = (int) Math.min(0xFFFFL,
                Math.max(1L, (remainingNanos + second - 1) / second));
        return new Snapshot(true, dodgeDown, remaining);
    }

    synchronized int dodgeDown(long nowNanos) {
        return snapshot(nowNanos).dodgeDown();
    }

    synchronized boolean expire(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("invalid Resist Fear time");
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
        dodgeDown = 0;
    }
}
