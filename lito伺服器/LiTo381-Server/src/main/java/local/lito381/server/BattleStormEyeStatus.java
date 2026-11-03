package local.lito381.server;

import java.util.concurrent.TimeUnit;

/** Authoritative cancelable lease for native Elf skill156 Storm Eye. */
final class BattleStormEyeStatus {
    record Snapshot(
            boolean active, int rangedHit, int rangedDamage,
            int remainingSeconds) {
        static Snapshot none() {
            return new Snapshot(false, 0, 0, 0);
        }
    }

    private long expiresAtNanos;
    private int rangedHit;
    private int rangedDamage;

    synchronized boolean activate(
            long nowNanos, int durationSeconds,
            int hitModifier, int damageModifier) {
        validate(nowNanos, durationSeconds, hitModifier, damageModifier);
        boolean refreshed = expiresAtNanos != 0 && nowNanos < expiresAtNanos;
        expiresAtNanos = Math.addExact(
                nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        rangedHit = hitModifier;
        rangedDamage = damageModifier;
        return refreshed;
    }

    synchronized Snapshot snapshot(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("invalid Storm Eye time");
        }
        if (expiresAtNanos == 0 || nowNanos >= expiresAtNanos) {
            return Snapshot.none();
        }
        long remainingNanos = expiresAtNanos - nowNanos;
        long second = TimeUnit.SECONDS.toNanos(1);
        int remaining = (int) Math.min(0xFFFFL,
                Math.max(1L, (remainingNanos + second - 1) / second));
        return new Snapshot(true, rangedHit, rangedDamage, remaining);
    }

    synchronized int physicalHit(boolean ranged, long nowNanos) {
        return ranged ? snapshot(nowNanos).rangedHit() : 0;
    }

    synchronized int physicalDamage(boolean ranged, long nowNanos) {
        return ranged ? snapshot(nowNanos).rangedDamage() : 0;
    }

    synchronized boolean expire(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("invalid Storm Eye time");
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
        rangedDamage = 0;
    }

    private static void validate(
            long nowNanos, int durationSeconds,
            int hitModifier, int damageModifier) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || hitModifier <= 0 || hitModifier > 127
                || damageModifier <= 0 || damageModifier > 127) {
            throw new IllegalArgumentException("invalid native Storm Eye lease");
        }
    }
}
