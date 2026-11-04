package local.lito381.server;

/** Transient cancelable player AC penalty for native skill183. */
import java.util.concurrent.TimeUnit;

final class BattleGuardBrakeStatus {
    private long expiresAtNanos;
    private int armorClass;
    volatile transient long nativeSourceIcons1Version;

    BattleGuardBrakeStatus() {
    }

    synchronized boolean activate(long l, int n, int n2) {
        if (l < 0L || n <= 0 || n > 65535 || n2 <= 0 || n2 > 127) {
            throw new IllegalArgumentException("invalid native Guard Brake lease");
        }
        boolean bl = this.expiresAtNanos != 0L && l < this.expiresAtNanos;
        long l2 = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        BattleGuardBrakeStatus battleGuardBrakeStatus = this;
        this.expiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleGuardBrakeStatus);
        this.armorClass = n2;
        return bl;
    }

    synchronized Snapshot snapshot(long l) {
        if (l < 0L) {
            throw new IllegalArgumentException("invalid Guard Brake time");
        }
        if (this.expiresAtNanos == 0L || l >= this.expiresAtNanos) {
            return Snapshot.none();
        }
        long l2 = this.expiresAtNanos - l;
        long l3 = TimeUnit.SECONDS.toNanos(1L);
        int n = (int)Math.min(65535L, Math.max(1L, (l2 + l3 - 1L) / l3));
        return new Snapshot(true, CombatBuffLeases.value(this.armorClass, 183, "AC"), n);
    }

    synchronized int armorClass(long l) {
        return this.snapshot(l).armorClass();
    }

    synchronized boolean expire(long l) {
        if (l < 0L) {
            throw new IllegalArgumentException("invalid Guard Brake time");
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
        BattleGuardBrakeStatus battleGuardBrakeStatus = this;
        this.expiresAtNanos = l;
        NativeSourceIcons1Epoch.bump(battleGuardBrakeStatus);
        this.armorClass = 0;
    }

    record Snapshot(boolean active, int armorClass, int remainingSeconds) {
        static Snapshot none() {
            return new Snapshot(false, 0, 0);
        }
    }
}
