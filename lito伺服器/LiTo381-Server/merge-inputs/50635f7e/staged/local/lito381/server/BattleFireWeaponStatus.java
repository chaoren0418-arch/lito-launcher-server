// ported from candidate 50635f7e, integration-lifecycle final frame, issue #67
package local.lito381.server;

/** Authoritative cancelable lease for native Elf skill148 Fire Weapon. */
import java.util.concurrent.TimeUnit;

final class BattleFireWeaponStatus {
    private long expiresAtNanos;
    private int meleeDamage;
    volatile transient long nativeSourceIcons1Version;

    BattleFireWeaponStatus() {
    }

    synchronized boolean activate(long nowNanos, int durationSeconds, int damageModifier) {
        BattleFireWeaponStatus.validate(nowNanos, durationSeconds, damageModifier);
        boolean bl = this.expiresAtNanos != 0L && nowNanos < this.expiresAtNanos;
        BattleFireWeaponStatus battleFireWeaponStatus;
        (battleFireWeaponStatus = this).expiresAtNanos = Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        NativeSourceIcons1Epoch.bump(battleFireWeaponStatus);
        this.meleeDamage = damageModifier;
        return bl;
    }

    synchronized Snapshot snapshot(long nowNanos) {
        if (nowNanos < 0L) {
            throw new IllegalArgumentException("invalid Fire Weapon time");
        }
        if (this.expiresAtNanos == 0L || nowNanos >= this.expiresAtNanos) {
            return Snapshot.none();
        }
        long l2 = this.expiresAtNanos - nowNanos;
        long l3 = TimeUnit.SECONDS.toNanos(1L);
        int n = (int)Math.min(65535L, Math.max(1L, (l2 + l3 - 1L) / l3));
        return new Snapshot(true, CombatBuffLeases.value(this.meleeDamage, 148, "DMG"), n);
    }

    synchronized int meleeDamage(long nowNanos) {
        return snapshot(nowNanos).meleeDamage();
    }

    synchronized int physicalDamage(boolean ranged, long nowNanos) {
        return ranged ? 0 : snapshot(nowNanos).meleeDamage();
    }

    synchronized boolean expire(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("invalid Fire Weapon time");
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
        BattleFireWeaponStatus battleFireWeaponStatus;
        (battleFireWeaponStatus = this).expiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleFireWeaponStatus);
        this.meleeDamage = 0;
    }

    private static void validate(
            long nowNanos, int durationSeconds, int damageModifier) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || damageModifier <= 0 || damageModifier > 127) {
            throw new IllegalArgumentException("invalid native Fire Weapon lease");
        }
    }

    record Snapshot(boolean active, int meleeDamage, int remainingSeconds) {
        static Snapshot none() {
            return new Snapshot(false, 0, 0);
        }
    }
}
