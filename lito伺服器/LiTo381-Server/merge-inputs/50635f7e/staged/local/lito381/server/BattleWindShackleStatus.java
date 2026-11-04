package local.lito381.server;

/** Native skill167 lease: attack and spell actions take 25 percent longer. */
import java.util.concurrent.TimeUnit;

final class BattleWindShackleStatus {
    static final double ACTION_MULTIPLIER = 1.25;
    private long expiresAtNanos;
    private long nextActionNanos;
    volatile transient long nativeSourceIcons1Version;

    BattleWindShackleStatus() {
    }

    synchronized boolean activate(long l, int n) {
        BattleWindShackleStatus.validateTime(l, n);
        if (this.active(l)) {
            return false;
        }
        long l2 = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        BattleWindShackleStatus battleWindShackleStatus = this;
        this.expiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleWindShackleStatus);
        this.nextActionNanos = 0L;
        return true;
    }

    synchronized boolean tryAction(long l, long l2) {
        if (l < 0L || l2 <= 0L) {
            throw new IllegalArgumentException("invalid Wind Shackle action time");
        }
        if (!this.active(l)) {
            return true;
        }
        if (l < this.nextActionNanos) {
            return false;
        }
        this.nextActionNanos = Math.addExact(l, TimeUnit.MILLISECONDS.toNanos(BattleWindShackleStatus.actionIntervalMillis(l2)));
        return true;
    }

    synchronized boolean cancel() {
        boolean bl = this.expiresAtNanos != 0L || this.nextActionNanos != 0L;
        long l = 0L;
        BattleWindShackleStatus battleWindShackleStatus = this;
        this.expiresAtNanos = l;
        NativeSourceIcons1Epoch.bump(battleWindShackleStatus);
        this.nextActionNanos = 0L;
        return bl;
    }

    synchronized boolean expire(long l) {
        if (l < 0L) {
            throw new IllegalArgumentException("invalid Wind Shackle expiry time");
        }
        if (this.expiresAtNanos == 0L || l < this.expiresAtNanos) {
            return false;
        }
        long l2 = 0L;
        BattleWindShackleStatus battleWindShackleStatus = this;
        this.expiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleWindShackleStatus);
        this.nextActionNanos = 0L;
        return true;
    }

    synchronized Snapshot snapshot(long l) {
        if (l < 0L) {
            throw new IllegalArgumentException("invalid Wind Shackle snapshot time");
        }
        if (!this.active(l)) {
            if (this.expiresAtNanos != 0L) {
                long l2 = 0L;
                BattleWindShackleStatus battleWindShackleStatus = this;
                this.expiresAtNanos = l2;
                NativeSourceIcons1Epoch.bump(battleWindShackleStatus);
                this.nextActionNanos = 0L;
            }
            return new Snapshot(false, 0, 0L, 0L);
        }
        long l3 = this.expiresAtNanos - l;
        long l4 = l3 / 1000000000L;
        if (l3 % 1000000000L != 0L) {
            ++l4;
        }
        return new Snapshot(true, Math.toIntExact(l4), this.expiresAtNanos, this.nextActionNanos);
    }

    static long actionIntervalMillis(long l) {
        if (l <= 0L) {
            throw new IllegalArgumentException("invalid native action interval");
        }
        return Math.max(1L, (long)((double)l * 1.25));
    }

    private boolean active(long l) {
        return this.expiresAtNanos != 0L && l < this.expiresAtNanos;
    }

    private static void validateTime(long l, int n) {
        if (l < 0L || n <= 0) {
            throw new IllegalArgumentException("invalid Wind Shackle lease");
        }
    }

    record Snapshot(boolean active, int remainingSeconds, long expiresAtNanos, long nextActionNanos) {
    }
}
