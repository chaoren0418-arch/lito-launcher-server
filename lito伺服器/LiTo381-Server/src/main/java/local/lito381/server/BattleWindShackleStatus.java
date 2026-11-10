// ported from candidate 50635f7e, integration-lifecycle final frame, issue #67
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

    synchronized boolean activate(long nowNanos, int durationSeconds) {
        BattleWindShackleStatus.validateTime(nowNanos, durationSeconds);
        if (this.active(nowNanos)) {
            return false;
        }
        BattleWindShackleStatus battleWindShackleStatus;
        (battleWindShackleStatus = this).expiresAtNanos = Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        NativeSourceIcons1Epoch.bump(battleWindShackleStatus);
        this.nextActionNanos = 0L;
        return true;
    }

    synchronized boolean tryAction(
            long nowNanos, long nativeDurationMillis) {
        if (nowNanos < 0 || nativeDurationMillis <= 0) {
            throw new IllegalArgumentException("invalid Wind Shackle action time");
        }
        if (!active(nowNanos)) return true;
        if (nowNanos < nextActionNanos) return false;
        nextActionNanos = Math.addExact(nowNanos,
                TimeUnit.MILLISECONDS.toNanos(
                        actionIntervalMillis(nativeDurationMillis)));
        return true;
    }

    synchronized boolean cancel() {
        boolean bl = this.expiresAtNanos != 0L || this.nextActionNanos != 0L;
        BattleWindShackleStatus battleWindShackleStatus;
        (battleWindShackleStatus = this).expiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleWindShackleStatus);
        this.nextActionNanos = 0L;
        return bl;
    }

    synchronized boolean expire(long nowNanos) {
        if (nowNanos < 0L) {
            throw new IllegalArgumentException("invalid Wind Shackle expiry time");
        }
        if (this.expiresAtNanos == 0L || nowNanos < this.expiresAtNanos) {
            return false;
        }
        BattleWindShackleStatus battleWindShackleStatus;
        (battleWindShackleStatus = this).expiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleWindShackleStatus);
        this.nextActionNanos = 0L;
        return true;
    }

    synchronized Snapshot snapshot(long nowNanos) {
        if (nowNanos < 0L) {
            throw new IllegalArgumentException("invalid Wind Shackle snapshot time");
        }
        if (!this.active(nowNanos)) {
            if (this.expiresAtNanos != 0L) {
                long l2 = 0L;
                BattleWindShackleStatus battleWindShackleStatus = this;
                this.expiresAtNanos = l2;
                NativeSourceIcons1Epoch.bump(battleWindShackleStatus);
                this.nextActionNanos = 0L;
            }
            return new Snapshot(false, 0, 0L, 0L);
        }
        long l3 = this.expiresAtNanos - nowNanos;
        long l4 = l3 / 1000000000L;
        if (l3 % 1000000000L != 0L) {
            ++l4;
        }
        return new Snapshot(true, Math.toIntExact(l4), this.expiresAtNanos, this.nextActionNanos);
    }

    static long actionIntervalMillis(long nativeDurationMillis) {
        if (nativeDurationMillis <= 0) {
            throw new IllegalArgumentException("invalid native action interval");
        }
        return Math.max(1L, (long) (nativeDurationMillis * ACTION_MULTIPLIER));
    }

    private boolean active(long nowNanos) {
        return expiresAtNanos != 0 && nowNanos < expiresAtNanos;
    }

    private static void validateTime(long nowNanos, int durationSeconds) {
        if (nowNanos < 0 || durationSeconds <= 0) {
            throw new IllegalArgumentException("invalid Wind Shackle lease");
        }
    }

    record Snapshot(boolean active, int remainingSeconds, long expiresAtNanos, long nextActionNanos) {
    }
}
