package local.lito381.server;

import java.util.concurrent.TimeUnit;

/** Independent transient ER lease for native Elf skill160 Aqua Protecter. */
final class BattleAquaProtectStatus {
    static final int NATIVE_ER_BONUS = 5;

    record Snapshot(boolean active, int evasionRate, int remainingSeconds) {
        static Snapshot none() {
            return new Snapshot(false, 0, 0);
        }
    }

    private long expiresAtNanos;
    private int evasionRate;

    synchronized boolean activate(
            long nowNanos, int durationSeconds, int evasionRateModifier) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || evasionRateModifier <= 0 || evasionRateModifier > 127) {
            throw new IllegalArgumentException("invalid native Aqua Protecter lease");
        }
        boolean refreshed = expiresAtNanos != 0 && nowNanos < expiresAtNanos;
        expiresAtNanos = Math.addExact(
                nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        evasionRate = evasionRateModifier;
        return refreshed;
    }

    synchronized Snapshot snapshot(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("invalid Aqua Protecter time");
        }
        if (expiresAtNanos == 0 || nowNanos >= expiresAtNanos) {
            return Snapshot.none();
        }
        long remainingNanos = expiresAtNanos - nowNanos;
        long second = TimeUnit.SECONDS.toNanos(1);
        int remaining = (int) Math.min(0xFFFFL,
                Math.max(1L, (remainingNanos + second - 1) / second));
        return new Snapshot(true, evasionRate, remaining);
    }

    synchronized int evasionRate(long nowNanos) {
        return snapshot(nowNanos).evasionRate();
    }

    synchronized boolean expire(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("invalid Aqua Protecter time");
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
        evasionRate = 0;
    }
}
