package local.lito381.server;

/** Persistent, cancelable player dodge-down lease for native skill188. */
import java.util.concurrent.TimeUnit;

final class BattleResistFearStatus {
    private long expiresAtNanos;
    private int dodgeDown;
    volatile transient long nativeSourceIcons1Version;

    BattleResistFearStatus() {
    }

    synchronized boolean activate(long l, int n, int n2) {
        boolean bl;
        if (l < 0L || n <= 0 || n > 65535 || n2 <= 0 || n2 > 10) {
            throw new IllegalArgumentException("invalid native Resist Fear lease");
        }
        boolean bl2 = bl = this.expiresAtNanos != 0L && l < this.expiresAtNanos;
        if (bl) {
            return true;
        }
        long l2 = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        BattleResistFearStatus battleResistFearStatus = this;
        this.expiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleResistFearStatus);
        this.dodgeDown = n2;
        return false;
    }

    synchronized Snapshot snapshot(long l) {
        if (l < 0L) {
            throw new IllegalArgumentException("invalid Resist Fear time");
        }
        if (this.expiresAtNanos == 0L || l >= this.expiresAtNanos) {
            return Snapshot.none();
        }
        long l2 = this.expiresAtNanos - l;
        long l3 = TimeUnit.SECONDS.toNanos(1L);
        int n = (int)Math.min(65535L, Math.max(1L, (l2 + l3 - 1L) / l3));
        return new Snapshot(true, this.dodgeDown, n);
    }

    synchronized int dodgeDown(long l) {
        return this.snapshot(l).dodgeDown();
    }

    synchronized boolean expire(long l) {
        if (l < 0L) {
            throw new IllegalArgumentException("invalid Resist Fear time");
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
        BattleResistFearStatus battleResistFearStatus = this;
        this.expiresAtNanos = l;
        NativeSourceIcons1Epoch.bump(battleResistFearStatus);
        this.dodgeDown = 0;
    }

    record Snapshot(boolean active, int dodgeDown, int remainingSeconds) {
        static Snapshot none() {
            return new Snapshot(false, 0, 0);
        }
    }
}
