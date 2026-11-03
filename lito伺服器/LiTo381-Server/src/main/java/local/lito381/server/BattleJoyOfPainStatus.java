package local.lito381.server;

import java.util.concurrent.TimeUnit;

/**
 * Server-authoritative skill218 lease shared by player and non-player actors.
 *
 * <p>The retained server captures the attacker's current skill timer before it
 * commits damage to a player, then consumes that exact timer after the hit was
 * accepted.  A generation therefore matters: a concurrent recast must not be
 * erased by an older in-flight attack.</p>
 */
final class BattleJoyOfPainStatus {
    record Lease(long generation) {
        Lease {
            if (generation <= 0) {
                throw new IllegalArgumentException("invalid Joy of Pain lease");
            }
        }
    }

    record Activation(boolean firstApplication, boolean refreshed,
                      long generation, int remainingSeconds) {
        Activation {
            if (generation <= 0 || remainingSeconds <= 0
                    || firstApplication == refreshed) {
                throw new IllegalArgumentException(
                        "invalid Joy of Pain activation");
            }
        }
    }

    record Snapshot(boolean active, long generation, int remainingSeconds) {}

    private long expiresAtNanos;
    private long generation;

    synchronized Activation activateOrRefresh(
            long nowNanos, int durationSeconds) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF) {
            throw new IllegalArgumentException("invalid native Joy of Pain lease");
        }
        boolean first = expiresAtNanos == 0 || nowNanos >= expiresAtNanos;
        generation = generation == Long.MAX_VALUE ? 1 : generation + 1;
        expiresAtNanos = Math.addExact(
                nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        return new Activation(first, !first, generation, durationSeconds);
    }

    synchronized Lease capture(long nowNanos) {
        if (nowNanos < 0 || expiresAtNanos == 0 || nowNanos >= expiresAtNanos) {
            return null;
        }
        return new Lease(generation);
    }

    /** Consumes only the exact generation captured before the accepted hit. */
    synchronized boolean consume(Lease lease) {
        if (lease == null || expiresAtNanos == 0
                || lease.generation() != generation) return false;
        expiresAtNanos = 0;
        return true;
    }

    synchronized Snapshot snapshot(long nowNanos) {
        if (nowNanos < 0 || expiresAtNanos == 0 || nowNanos >= expiresAtNanos) {
            return new Snapshot(false, generation, 0);
        }
        long second = TimeUnit.SECONDS.toNanos(1);
        long remaining = (expiresAtNanos - nowNanos + second - 1) / second;
        return new Snapshot(true, generation,
                (int) Math.min(0xFFFFL, Math.max(1L, remaining)));
    }

    synchronized boolean expire(long nowNanos) {
        if (nowNanos < 0 || expiresAtNanos == 0 || nowNanos < expiresAtNanos) {
            return false;
        }
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
}
