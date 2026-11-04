package local.lito381.server;

/** Authoritative cancelable lease for native Elf skill155 Fire Bless. */
import java.util.concurrent.TimeUnit;

final class BattleFireBlessStatus {
    private long expiresAtNanos;
    private int meleeDamage;
    volatile transient long nativeSourceIcons1Version;

    BattleFireBlessStatus() {
    }

    synchronized boolean activate(long l, int n, int n2) {
        BattleFireBlessStatus.validate(l, n, n2);
        boolean bl = this.expiresAtNanos != 0L && l < this.expiresAtNanos;
        long l2 = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        BattleFireBlessStatus battleFireBlessStatus = this;
        this.expiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleFireBlessStatus);
        this.meleeDamage = n2;
        return bl;
    }

    synchronized Snapshot snapshot(long l) {
        if (l < 0L) {
            throw new IllegalArgumentException("invalid Fire Bless time");
        }
        if (this.expiresAtNanos == 0L || l >= this.expiresAtNanos) {
            return Snapshot.none();
        }
        long l2 = this.expiresAtNanos - l;
        long l3 = TimeUnit.SECONDS.toNanos(1L);
        int n = (int)Math.min(65535L, Math.max(1L, (l2 + l3 - 1L) / l3));
        return new Snapshot(true, CombatBuffLeases.value(this.meleeDamage, 155, "DMG"), n);
    }

    synchronized int physicalDamage(boolean bl, long l) {
        return bl ? 0 : this.snapshot(l).meleeDamage();
    }

    synchronized boolean expire(long l) {
        if (l < 0L) {
            throw new IllegalArgumentException("invalid Fire Bless time");
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
        BattleFireBlessStatus battleFireBlessStatus = this;
        this.expiresAtNanos = l;
        NativeSourceIcons1Epoch.bump(battleFireBlessStatus);
        this.meleeDamage = 0;
    }

    private static void validate(long l, int n, int n2) {
        if (l < 0L || n <= 0 || n > 65535 || n2 <= 0 || n2 > 127) {
            throw new IllegalArgumentException("invalid native Fire Bless lease");
        }
    }

    record Snapshot(boolean active, int meleeDamage, int remainingSeconds) {
        static Snapshot none() {
            return new Snapshot(false, 0, 0);
        }
    }
}
