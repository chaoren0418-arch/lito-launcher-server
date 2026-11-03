package local.lito381.server;

import java.util.concurrent.TimeUnit;

/** Native skill153 lease: quarter MR until expiry or one qualified magic consumes it. */
final class BattleEraseMagicStatus {
    record Lease(long generation, long expiresAtNanos) {
        Lease {
            if (generation < 0 || expiresAtNanos < 0
                    || (generation == 0) != (expiresAtNanos == 0)) {
                throw new IllegalArgumentException("invalid Erase Magic lease");
            }
        }

        static Lease none() {
            return new Lease(0, 0);
        }

        boolean present() {
            return generation != 0;
        }
    }

    private long generation;
    private long expiresAtNanos;

    synchronized Lease activate(long nowNanos, int durationSeconds) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF) {
            throw new IllegalArgumentException("invalid Erase Magic activation");
        }
        generation = nextGeneration(generation);
        expiresAtNanos = Math.addExact(
                nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        return new Lease(generation, expiresAtNanos);
    }

    synchronized boolean active(long nowNanos) {
        if (nowNanos < 0) return false;
        if (expiresAtNanos != 0 && nowNanos >= expiresAtNanos) {
            clearWithNewGeneration();
            return false;
        }
        return expiresAtNanos != 0;
    }

    synchronized int effectiveMagicResistance(int baseMagicResistance,
                                              long nowNanos) {
        if (baseMagicResistance < 0 || nowNanos < 0) {
            throw new IllegalArgumentException("invalid Erase Magic MR input");
        }
        return active(nowNanos) ? baseMagicResistance >> 2 : baseMagicResistance;
    }

    synchronized Lease capture(long nowNanos) {
        return active(nowNanos)
                ? new Lease(generation, expiresAtNanos) : Lease.none();
    }

    /** Consume only the exact captured lease; a stale hit cannot delete a refresh. */
    synchronized boolean consume(Lease lease) {
        if (lease == null || !lease.present() || generation != lease.generation()
                || expiresAtNanos != lease.expiresAtNanos()) {
            return false;
        }
        clearWithNewGeneration();
        return true;
    }

    synchronized boolean consume(long nowNanos) {
        Lease lease = capture(nowNanos);
        return consume(lease);
    }

    synchronized boolean expire(long nowNanos) {
        if (nowNanos < 0 || expiresAtNanos == 0 || nowNanos < expiresAtNanos) {
            return false;
        }
        clearWithNewGeneration();
        return true;
    }

    synchronized boolean cancel() {
        if (expiresAtNanos == 0) return false;
        clearWithNewGeneration();
        return true;
    }

    synchronized void clear() {
        if (expiresAtNanos != 0) clearWithNewGeneration();
    }

    private void clearWithNewGeneration() {
        generation = nextGeneration(generation);
        expiresAtNanos = 0;
    }

    private static long nextGeneration(long value) {
        return value == Long.MAX_VALUE ? 1 : value + 1;
    }
}
