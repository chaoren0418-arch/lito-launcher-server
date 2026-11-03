package local.lito381.server;

import java.util.concurrent.TimeUnit;

/** Authoritative cancelable lease for native Elf skill148 Fire Weapon. */
final class BattleFireWeaponStatus {
    record Snapshot(boolean active, int meleeDamage, int remainingSeconds) {
        static Snapshot none() {
            return new Snapshot(false, 0, 0);
        }
    }

    private long expiresAtNanos;
    private int meleeDamage;

    synchronized boolean activate(
            long nowNanos, int durationSeconds, int damageModifier) {
        validate(nowNanos, durationSeconds, damageModifier);
        boolean refreshed = expiresAtNanos != 0 && nowNanos < expiresAtNanos;
        expiresAtNanos = Math.addExact(
                nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        meleeDamage = damageModifier;
        return refreshed;
    }

    synchronized Snapshot snapshot(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("invalid Fire Weapon time");
        }
        if (expiresAtNanos == 0 || nowNanos >= expiresAtNanos) {
            return Snapshot.none();
        }
        long remainingNanos = expiresAtNanos - nowNanos;
        long second = TimeUnit.SECONDS.toNanos(1);
        int remaining = (int) Math.min(0xFFFFL,
                Math.max(1L, (remainingNanos + second - 1) / second));
        return new Snapshot(true, meleeDamage, remaining);
    }

    synchronized int meleeDamage(long nowNanos) {
        return snapshot(nowNanos).meleeDamage();
    }

    synchronized int physicalDamage(boolean ranged, long nowNanos) {
        return ranged ? 0 : snapshot(nowNanos).meleeDamage();
    }

    synchronized boolean expire(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("invalid Fire Weapon time");
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
        meleeDamage = 0;
    }

    private static void validate(
            long nowNanos, int durationSeconds, int damageModifier) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || damageModifier <= 0 || damageModifier > 127) {
            throw new IllegalArgumentException("invalid native Fire Weapon lease");
        }
    }
}
