// ported from candidate 50635f7e, integration-lifecycle final frame, issue #67
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

    synchronized boolean activate(long nowNanos, int durationSeconds, int hitModifier, int damageModifier) {
        BattleBurningWeaponStatus.validate(nowNanos, durationSeconds, hitModifier, damageModifier);
        boolean bl = this.expiresAtNanos != 0L && nowNanos < this.expiresAtNanos;
        BattleBurningWeaponStatus battleBurningWeaponStatus = this;
        long expiresAt = Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        battleBurningWeaponStatus.expiresAtNanos = expiresAt;
        NativeSourceIcons1Epoch.bump(battleBurningWeaponStatus);
        this.meleeHit = hitModifier;
        this.meleeDamage = damageModifier;
        return bl;
    }

    synchronized Snapshot snapshot(long nowNanos) {
        if (nowNanos < 0L) {
            throw new IllegalArgumentException("invalid Burning Weapon time");
        }
        if (this.expiresAtNanos == 0L || nowNanos >= this.expiresAtNanos) {
            return Snapshot.none();
        }
        long l2 = this.expiresAtNanos - nowNanos;
        long l3 = TimeUnit.SECONDS.toNanos(1L);
        int n = (int)Math.min(65535L, Math.max(1L, (l2 + l3 - 1L) / l3));
        return new Snapshot(true, CombatBuffLeases.value(this.meleeHit, 163, "HIT"), CombatBuffLeases.value(this.meleeDamage, 163, "DMG"), n);
    }

    synchronized int physicalHit(boolean ranged, long nowNanos) {
        return ranged ? 0 : snapshot(nowNanos).meleeHit();
    }

    synchronized int physicalDamage(boolean ranged, long nowNanos) {
        return ranged ? 0 : snapshot(nowNanos).meleeDamage();
    }

    synchronized boolean expire(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("invalid Burning Weapon time");
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
        BattleBurningWeaponStatus battleBurningWeaponStatus;
        (battleBurningWeaponStatus = this).expiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleBurningWeaponStatus);
        this.meleeHit = 0;
        this.meleeDamage = 0;
    }

    private static void validate(long nowNanos, int durationSeconds,
                                 int hitModifier, int damageModifier) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || hitModifier <= 0 || hitModifier > 127
                || damageModifier <= 0 || damageModifier > 127) {
            throw new IllegalArgumentException(
                    "invalid native Burning Weapon lease");
        }
    }

    record Snapshot(boolean active, int meleeHit, int meleeDamage, int remainingSeconds) {
        static Snapshot none() {
            return new Snapshot(false, 0, 0, 0);
        }
    }
}
