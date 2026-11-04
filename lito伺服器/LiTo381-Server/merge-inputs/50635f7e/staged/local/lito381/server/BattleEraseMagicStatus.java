package local.lito381.server;

/** Native skill153 lease: quarter MR until expiry or one qualified magic consumes it. */
import java.util.concurrent.TimeUnit;

final class BattleEraseMagicStatus {
    private long generation;
    private long expiresAtNanos;
    volatile transient long nativeSourceIcons1Version;

    BattleEraseMagicStatus() {
    }

    synchronized Lease activate(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid Erase Magic activation");
        }
        this.generation = BattleEraseMagicStatus.nextGeneration(this.generation);
        long l2 = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        BattleEraseMagicStatus battleEraseMagicStatus = this;
        this.expiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleEraseMagicStatus);
        return new Lease(this.generation, this.expiresAtNanos);
    }

    synchronized boolean active(long l) {
        if (l < 0L) {
            return false;
        }
        if (this.expiresAtNanos != 0L && l >= this.expiresAtNanos) {
            this.clearWithNewGeneration();
            return false;
        }
        return this.expiresAtNanos != 0L;
    }

    synchronized int effectiveMagicResistance(int n, long l) {
        if (n < 0 || l < 0L) {
            throw new IllegalArgumentException("invalid Erase Magic MR input");
        }
        return this.active(l) ? n >> 2 : n;
    }

    synchronized Lease capture(long l) {
        return this.active(l) ? new Lease(this.generation, this.expiresAtNanos) : Lease.none();
    }

    synchronized boolean consume(Lease lease) {
        if (lease == null || !lease.present() || this.generation != lease.generation() || this.expiresAtNanos != lease.expiresAtNanos()) {
            return false;
        }
        this.clearWithNewGeneration();
        return true;
    }

    synchronized boolean consume(long l) {
        Lease lease = this.capture(l);
        return this.consume(lease);
    }

    synchronized boolean expire(long l) {
        if (l < 0L || this.expiresAtNanos == 0L || l < this.expiresAtNanos) {
            return false;
        }
        this.clearWithNewGeneration();
        return true;
    }

    synchronized boolean cancel() {
        if (this.expiresAtNanos == 0L) {
            return false;
        }
        this.clearWithNewGeneration();
        return true;
    }

    synchronized void clear() {
        if (this.expiresAtNanos != 0L) {
            this.clearWithNewGeneration();
        }
    }

    private void clearWithNewGeneration() {
        this.generation = BattleEraseMagicStatus.nextGeneration(this.generation);
        long l = 0L;
        BattleEraseMagicStatus battleEraseMagicStatus = this;
        this.expiresAtNanos = l;
        NativeSourceIcons1Epoch.bump(battleEraseMagicStatus);
    }

    private static long nextGeneration(long l) {
        return l == Long.MAX_VALUE ? 1L : l + 1L;
    }

    record Lease(long generation, long expiresAtNanos) {
        Lease {
            if (generation < 0L || expiresAtNanos < 0L || generation == 0L != (expiresAtNanos == 0L)) {
                throw new IllegalArgumentException("invalid Erase Magic lease");
            }
        }

        static Lease none() {
            return new Lease(0L, 0L);
        }

        boolean present() {
            return this.generation != 0L;
        }
    }
}
