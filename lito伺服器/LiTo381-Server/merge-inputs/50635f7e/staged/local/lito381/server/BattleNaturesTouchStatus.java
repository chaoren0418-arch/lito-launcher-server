// ported from candidate 50635f7e, integration-lifecycle final frame, issue #67
package local.lito381.server;

/** Authoritative cancelable lease for native Elf skill158 Nature's Touch. */
import java.util.concurrent.TimeUnit;

final class BattleNaturesTouchStatus {
    static final int NATIVE_HPR_BONUS = 15;
    private long expiresAtNanos;
    volatile transient long nativeSourceIcons1Version;

    BattleNaturesTouchStatus() {
    }

    synchronized boolean activate(long nowNanos, int durationSeconds) {
        BattleNaturesTouchStatus.validate(nowNanos, durationSeconds);
        boolean bl = this.expiresAtNanos != 0L && nowNanos < this.expiresAtNanos;
        BattleNaturesTouchStatus battleNaturesTouchStatus;
        (battleNaturesTouchStatus = this).expiresAtNanos = Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        NativeSourceIcons1Epoch.bump(battleNaturesTouchStatus);
        return bl;
    }

    synchronized Snapshot snapshot(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("invalid Nature's Touch time");
        }
        if (expiresAtNanos == 0 || nowNanos >= expiresAtNanos) {
            return Snapshot.none();
        }
        long remainingNanos = expiresAtNanos - nowNanos;
        long second = TimeUnit.SECONDS.toNanos(1);
        int remaining = (int) Math.min(0xFFFFL,
                Math.max(1L, (remainingNanos + second - 1) / second));
        return new Snapshot(true, NATIVE_HPR_BONUS, remaining);
    }

    synchronized int hpRegen(long nowNanos) {
        return snapshot(nowNanos).hpRegen();
    }

    synchronized boolean expire(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("invalid Nature's Touch time");
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
        BattleNaturesTouchStatus battleNaturesTouchStatus;
        (battleNaturesTouchStatus = this).expiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleNaturesTouchStatus);
    }

    private static void validate(long nowNanos, int durationSeconds) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF) {
            throw new IllegalArgumentException("invalid native Nature's Touch lease");
        }
    }

    record Snapshot(boolean active, int hpRegen, int remainingSeconds) {
        static Snapshot none() {
            return new Snapshot(false, 0, 0);
        }
    }
}
