// ported from candidate 50635f7e, integration-lifecycle final frame, issue #67
package local.lito381.server;

/** Transient cancelable player AC penalty for native skill183. */
import java.util.concurrent.TimeUnit;

final class BattleGuardBrakeStatus {
    private long expiresAtNanos;
    private int armorClass;
    volatile transient long nativeSourceIcons1Version;

    BattleGuardBrakeStatus() {
    }

    synchronized boolean activate(long nowNanos, int durationSeconds, int armorClassModifier) {
        if (nowNanos < 0L || durationSeconds <= 0 || durationSeconds > 65535 || armorClassModifier <= 0 || armorClassModifier > 127) {
            throw new IllegalArgumentException("invalid native Guard Brake lease");
        }
        boolean bl = this.expiresAtNanos != 0L && nowNanos < this.expiresAtNanos;
        BattleGuardBrakeStatus battleGuardBrakeStatus;
        (battleGuardBrakeStatus = this).expiresAtNanos = Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        NativeSourceIcons1Epoch.bump(battleGuardBrakeStatus);
        this.armorClass = armorClassModifier;
        return bl;
    }

    synchronized Snapshot snapshot(long nowNanos) {
        if (nowNanos < 0L) {
            throw new IllegalArgumentException("invalid Guard Brake time");
        }
        if (this.expiresAtNanos == 0L || nowNanos >= this.expiresAtNanos) {
            return Snapshot.none();
        }
        long l2 = this.expiresAtNanos - nowNanos;
        long l3 = TimeUnit.SECONDS.toNanos(1L);
        int n = (int)Math.min(65535L, Math.max(1L, (l2 + l3 - 1L) / l3));
        return new Snapshot(true, CombatBuffLeases.value(this.armorClass, 183, "AC"), n);
    }

    synchronized int armorClass(long nowNanos) {
        return snapshot(nowNanos).armorClass();
    }

    synchronized boolean expire(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("invalid Guard Brake time");
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
        BattleGuardBrakeStatus battleGuardBrakeStatus;
        (battleGuardBrakeStatus = this).expiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleGuardBrakeStatus);
        this.armorClass = 0;
    }

    record Snapshot(boolean active, int armorClass, int remainingSeconds) {
        static Snapshot none() {
            return new Snapshot(false, 0, 0);
        }
    }
}
