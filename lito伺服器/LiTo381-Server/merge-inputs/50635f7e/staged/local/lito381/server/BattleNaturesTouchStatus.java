package local.lito381.server;

/** Authoritative cancelable lease for native Elf skill158 Nature's Touch. */
import java.util.concurrent.TimeUnit;

final class BattleNaturesTouchStatus {
    static final int NATIVE_HPR_BONUS = 15;
    private long expiresAtNanos;
    volatile transient long nativeSourceIcons1Version;

    BattleNaturesTouchStatus() {
    }

    synchronized boolean activate(long l, int n) {
        BattleNaturesTouchStatus.validate(l, n);
        boolean bl = this.expiresAtNanos != 0L && l < this.expiresAtNanos;
        long l2 = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        BattleNaturesTouchStatus battleNaturesTouchStatus = this;
        this.expiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleNaturesTouchStatus);
        return bl;
    }

    synchronized Snapshot snapshot(long l) {
        if (l < 0L) {
            throw new IllegalArgumentException("invalid Nature's Touch time");
        }
        if (this.expiresAtNanos == 0L || l >= this.expiresAtNanos) {
            return Snapshot.none();
        }
        long l2 = this.expiresAtNanos - l;
        long l3 = TimeUnit.SECONDS.toNanos(1L);
        int n = (int)Math.min(65535L, Math.max(1L, (l2 + l3 - 1L) / l3));
        return new Snapshot(true, 15, n);
    }

    synchronized int hpRegen(long l) {
        return this.snapshot(l).hpRegen();
    }

    synchronized boolean expire(long l) {
        if (l < 0L) {
            throw new IllegalArgumentException("invalid Nature's Touch time");
        }
        if (this.expiresAtNanos == 0L || l < this.expiresAtNanos) {
            return false;
        }
        this.clear();
        return true;
    }

    synchronized boolean cancel() {
        if (this.expiresAtNanos == 0L) {
            return false;
        }
        this.clear();
        return true;
    }

    synchronized void clear() {
        long l = 0L;
        BattleNaturesTouchStatus battleNaturesTouchStatus = this;
        this.expiresAtNanos = l;
        NativeSourceIcons1Epoch.bump(battleNaturesTouchStatus);
    }

    private static void validate(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid native Nature's Touch lease");
        }
    }

    record Snapshot(boolean active, int hpRegen, int remainingSeconds) {
        static Snapshot none() {
            return new Snapshot(false, 0, 0);
        }
    }
}
