package local.lito381.server;

import java.util.concurrent.TimeUnit;

/** Authoritative cancelable lease for native Elf skill149 Wind Shot. */
final class BattleWindShotStatus {
    record Snapshot(boolean active, int rangedHit, int remainingSeconds) {
        static Snapshot none() {
            return new Snapshot(false, 0, 0);
        }
    }

    private long expiresAtNanos;
    private int rangedHit;

    synchronized boolean activate(
            long nowNanos, int durationSeconds, int hitModifier) {
        validate(nowNanos, durationSeconds, hitModifier);
        boolean refreshed = expiresAtNanos != 0 && nowNanos < expiresAtNanos;
        expiresAtNanos = Math.addExact(
                nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        rangedHit = hitModifier;
        return refreshed;
    }

    synchronized Snapshot snapshot(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("invalid Wind Shot time");
        }
        if (expiresAtNanos == 0 || nowNanos >= expiresAtNanos) {
            return Snapshot.none();
        }
        long remainingNanos = expiresAtNanos - nowNanos;
        long second = TimeUnit.SECONDS.toNanos(1);
        int remaining = (int) Math.min(0xFFFFL,
                Math.max(1L, (remainingNanos + second - 1) / second));
        return new Snapshot(true, rangedHit, remaining);
    }

    synchronized int physicalHit(boolean ranged, long nowNanos) {
        return ranged ? snapshot(nowNanos).rangedHit() : 0;
    }

    synchronized boolean expire(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("invalid Wind Shot time");
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
        rangedHit = 0;
    }

    private static void validate(
            long nowNanos, int durationSeconds, int hitModifier) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || hitModifier <= 0 || hitModifier > 127) {
            throw new IllegalArgumentException("invalid native Wind Shot lease");
        }
    }
}
