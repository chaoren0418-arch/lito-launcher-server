// ported from candidate 50635f7e, integration-lifecycle final frame, issue #67
package local.lito381.server;

/** Authoritative cancelable lease for native Elf skill155 Fire Bless. */
import java.util.concurrent.TimeUnit;

final class BattleFireBlessStatus {
    private long expiresAtNanos;
    private int meleeDamage;
    volatile transient long nativeSourceIcons1Version;

    BattleFireBlessStatus() {
    }

    synchronized boolean activate(long nowNanos, int durationSeconds, int damageModifier) {
        BattleFireBlessStatus.validate(nowNanos, durationSeconds, damageModifier);
        boolean bl = this.expiresAtNanos != 0L && nowNanos < this.expiresAtNanos;
        BattleFireBlessStatus battleFireBlessStatus;
        (battleFireBlessStatus = this).expiresAtNanos = Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        NativeSourceIcons1Epoch.bump(battleFireBlessStatus);
        this.meleeDamage = damageModifier;
        return bl;
    }

    synchronized Snapshot snapshot(long nowNanos) {
        if (nowNanos < 0L) {
            throw new IllegalArgumentException("invalid Fire Bless time");
        }
        if (this.expiresAtNanos == 0L || nowNanos >= this.expiresAtNanos) {
            return Snapshot.none();
        }
        long l2 = this.expiresAtNanos - nowNanos;
        long l3 = TimeUnit.SECONDS.toNanos(1L);
        int n = (int)Math.min(65535L, Math.max(1L, (l2 + l3 - 1L) / l3));
        return new Snapshot(true, CombatBuffLeases.value(this.meleeDamage, 155, "DMG"), n);
    }

    synchronized int physicalDamage(boolean ranged, long nowNanos) {
        return ranged ? 0 : snapshot(nowNanos).meleeDamage();
    }

    synchronized boolean expire(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("invalid Fire Bless time");
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
        BattleFireBlessStatus battleFireBlessStatus;
        (battleFireBlessStatus = this).expiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleFireBlessStatus);
        this.meleeDamage = 0;
    }

    private static void validate(
            long nowNanos, int durationSeconds, int damageModifier) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || damageModifier <= 0 || damageModifier > 127) {
            throw new IllegalArgumentException("invalid native Fire Bless lease");
        }
    }

    record Snapshot(boolean active, int meleeDamage, int remainingSeconds) {
        static Snapshot none() {
            return new Snapshot(false, 0, 0);
        }
    }
}
