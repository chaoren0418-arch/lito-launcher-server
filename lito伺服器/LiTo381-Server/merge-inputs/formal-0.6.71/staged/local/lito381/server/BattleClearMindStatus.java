package local.lito381.server;

import java.util.concurrent.TimeUnit;

/** Authoritative cancelable lease for native Elf skill137 Clear Mind. */
final class BattleClearMindStatus {
    record Snapshot(int wisdomModifier, int remainingSeconds) {
        boolean active() {
            return remainingSeconds > 0;
        }
    }

    private long expiresAtNanos;
    private int wisdomModifier;

    synchronized boolean activate(
            long nowNanos, int durationSeconds, int modifier) {
        validate(nowNanos, durationSeconds, modifier);
        boolean refreshed = expiresAtNanos != 0 && nowNanos < expiresAtNanos;
        expiresAtNanos = Math.addExact(
                nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        wisdomModifier = modifier;
        return refreshed;
    }

    synchronized Snapshot snapshot(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("invalid Clear Mind time");
        }
        if (expiresAtNanos == 0 || nowNanos >= expiresAtNanos) {
            return new Snapshot(0, 0);
        }
        long remainingNanos = expiresAtNanos - nowNanos;
        long second = TimeUnit.SECONDS.toNanos(1);
        int remaining = (int) Math.min(0xFFFFL,
                Math.max(1L, (remainingNanos + second - 1) / second));
        return new Snapshot(CombatBuffLeases.value(wisdomModifier, 137, "WIS"), remaining);
    }

    synchronized boolean expire(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("invalid Clear Mind time");
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
        wisdomModifier = 0;
    }

    private static void validate(
            long nowNanos, int durationSeconds, int modifier) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || modifier <= 0 || modifier > 127) {
            throw new IllegalArgumentException("invalid native Clear Mind lease");
        }
    }
}
