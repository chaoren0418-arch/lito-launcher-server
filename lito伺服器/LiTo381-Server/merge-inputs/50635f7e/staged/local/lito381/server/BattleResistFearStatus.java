// ported from candidate 50635f7e, integration-lifecycle final frame, issue #67
package local.lito381.server;

/** Persistent, cancelable player dodge-down lease for native skill188. */
import java.util.concurrent.TimeUnit;

final class BattleResistFearStatus {
    private long expiresAtNanos;
    private int dodgeDown;
    volatile transient long nativeSourceIcons1Version;

    BattleResistFearStatus() {
    }

    synchronized boolean activate(long nowNanos, int durationSeconds, int dodgeDownModifier) {
        boolean bl;
        if (nowNanos < 0L || durationSeconds <= 0 || durationSeconds > 65535 || dodgeDownModifier <= 0 || dodgeDownModifier > 10) {
            throw new IllegalArgumentException("invalid native Resist Fear lease");
        }
        boolean bl2 = bl = this.expiresAtNanos != 0L && nowNanos < this.expiresAtNanos;
        if (bl) {
            return true;
        }
        BattleResistFearStatus battleResistFearStatus;
        (battleResistFearStatus = this).expiresAtNanos = Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        NativeSourceIcons1Epoch.bump(battleResistFearStatus);
        this.dodgeDown = dodgeDownModifier;
        return false;
    }

    synchronized Snapshot snapshot(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("invalid Resist Fear time");
        }
        if (expiresAtNanos == 0 || nowNanos >= expiresAtNanos) {
            return Snapshot.none();
        }
        long remainingNanos = expiresAtNanos - nowNanos;
        long second = TimeUnit.SECONDS.toNanos(1);
        int remaining = (int) Math.min(0xFFFFL,
                Math.max(1L, (remainingNanos + second - 1) / second));
        return new Snapshot(true, dodgeDown, remaining);
    }

    synchronized int dodgeDown(long nowNanos) {
        return snapshot(nowNanos).dodgeDown();
    }

    synchronized boolean expire(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("invalid Resist Fear time");
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
        BattleResistFearStatus battleResistFearStatus;
        (battleResistFearStatus = this).expiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleResistFearStatus);
        this.dodgeDown = 0;
    }

    record Snapshot(boolean active, int dodgeDown, int remainingSeconds) {
        static Snapshot none() {
            return new Snapshot(false, 0, 0);
        }
    }
}
