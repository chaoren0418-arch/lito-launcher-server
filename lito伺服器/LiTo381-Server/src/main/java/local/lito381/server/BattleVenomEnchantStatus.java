package local.lito381.server;

import java.util.concurrent.TimeUnit;

/** Authoritative lease and retained 10-percent proc rule for skill 98. */
final class BattleVenomEnchantStatus {
    static final int PROC_PERCENT = 10;
    static final int POISON_DURATION_SECONDS = 30;
    static final int POISON_TICK_MILLIS = 3_000;
    static final int POISON_TICK_DAMAGE = 5;

    private long expiresAtNanos;

    synchronized void activate(long nowNanos, int durationSeconds) {
        if (nowNanos < 0 || durationSeconds <= 0) {
            throw new IllegalArgumentException("invalid Enchant Venom lease");
        }
        expiresAtNanos = Math.addExact(
                nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
    }

    synchronized boolean active(long nowNanos) {
        return nowNanos >= 0 && expiresAtNanos != 0 && nowNanos < expiresAtNanos;
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

    static boolean triggers(boolean enchantActive, boolean weaponPresent,
                            int oneBasedRoll) {
        if (oneBasedRoll < 1 || oneBasedRoll > 100) {
            throw new IllegalArgumentException("Enchant Venom roll must be 1..100");
        }
        return enchantActive && weaponPresent && oneBasedRoll <= PROC_PERCENT;
    }
}
