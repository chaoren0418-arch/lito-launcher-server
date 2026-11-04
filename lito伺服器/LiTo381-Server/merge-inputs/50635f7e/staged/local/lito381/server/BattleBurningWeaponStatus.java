package local.lito381.server;

/** Authoritative cancelable lease for native Elf skill163 Burning Weapon. */
import java.util.concurrent.TimeUnit;

final class BattleBurningWeaponStatus {
    private long expiresAtNanos;
    private int meleeHit;
    private int meleeDamage;
    volatile transient long nativeSourceIcons1Version;

    BattleBurningWeaponStatus() {
    }

    synchronized boolean activate(long l, int n, int n2, int n3) {
        BattleBurningWeaponStatus.validate(l, n, n2, n3);
        boolean bl = this.expiresAtNanos != 0L && l < this.expiresAtNanos;
        long l2 = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        BattleBurningWeaponStatus battleBurningWeaponStatus = this;
        this.expiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleBurningWeaponStatus);
        this.meleeHit = n2;
        this.meleeDamage = n3;
        return bl;
    }

    synchronized Snapshot snapshot(long l) {
        if (l < 0L) {
            throw new IllegalArgumentException("invalid Burning Weapon time");
        }
        if (this.expiresAtNanos == 0L || l >= this.expiresAtNanos) {
            return Snapshot.none();
        }
        long l2 = this.expiresAtNanos - l;
        long l3 = TimeUnit.SECONDS.toNanos(1L);
        int n = (int)Math.min(65535L, Math.max(1L, (l2 + l3 - 1L) / l3));
        return new Snapshot(true, CombatBuffLeases.value(this.meleeHit, 163, "HIT"), CombatBuffLeases.value(this.meleeDamage, 163, "DMG"), n);
    }

    synchronized int physicalHit(boolean bl, long l) {
        return bl ? 0 : this.snapshot(l).meleeHit();
    }

    synchronized int physicalDamage(boolean bl, long l) {
        return bl ? 0 : this.snapshot(l).meleeDamage();
    }

    synchronized boolean expire(long l) {
        if (l < 0L) {
            throw new IllegalArgumentException("invalid Burning Weapon time");
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
        BattleBurningWeaponStatus battleBurningWeaponStatus = this;
        this.expiresAtNanos = l;
        NativeSourceIcons1Epoch.bump(battleBurningWeaponStatus);
        this.meleeHit = 0;
        this.meleeDamage = 0;
    }

    private static void validate(long l, int n, int n2, int n3) {
        if (l < 0L || n <= 0 || n > 65535 || n2 <= 0 || n2 > 127 || n3 <= 0 || n3 > 127) {
            throw new IllegalArgumentException("invalid native Burning Weapon lease");
        }
    }

    record Snapshot(boolean active, int meleeHit, int meleeDamage, int remainingSeconds) {
        static Snapshot none() {
            return new Snapshot(false, 0, 0, 0);
        }
    }
}
