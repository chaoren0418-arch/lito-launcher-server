// ported from candidate 50635f7e, integration-lifecycle final frame, issue #67
package local.lito381.server;

/** Authoritative cancelable lease for native Elf skill156 Storm Eye. */
import java.util.concurrent.TimeUnit;

final class BattleStormEyeStatus {
    private long expiresAtNanos;
    private int rangedHit;
    private int rangedDamage;
    volatile transient long nativeSourceIcons1Version;

    BattleStormEyeStatus() {
    }

    synchronized boolean activate(long nowNanos, int durationSeconds, int hitModifier, int damageModifier) {
        BattleStormEyeStatus.validate(nowNanos, durationSeconds, hitModifier, damageModifier);
        boolean bl = this.expiresAtNanos != 0L && nowNanos < this.expiresAtNanos;
        BattleStormEyeStatus battleStormEyeStatus;
        (battleStormEyeStatus = this).expiresAtNanos = Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        NativeSourceIcons1Epoch.bump(battleStormEyeStatus);
        this.rangedHit = hitModifier;
        this.rangedDamage = damageModifier;
        return bl;
    }

    synchronized Snapshot snapshot(long nowNanos) {
        if (nowNanos < 0L) {
            throw new IllegalArgumentException("invalid Storm Eye time");
        }
        if (this.expiresAtNanos == 0L || nowNanos >= this.expiresAtNanos) {
            return Snapshot.none();
        }
        long l2 = this.expiresAtNanos - nowNanos;
        long l3 = TimeUnit.SECONDS.toNanos(1L);
        int n = (int)Math.min(65535L, Math.max(1L, (l2 + l3 - 1L) / l3));
        return new Snapshot(true, CombatBuffLeases.value(this.rangedHit, 156, "BOW_HIT"), CombatBuffLeases.value(this.rangedDamage, 156, "BOW_DMG"), n);
    }

    synchronized int physicalHit(boolean ranged, long nowNanos) {
        return ranged ? snapshot(nowNanos).rangedHit() : 0;
    }

    synchronized int physicalDamage(boolean ranged, long nowNanos) {
        return ranged ? snapshot(nowNanos).rangedDamage() : 0;
    }

    synchronized boolean expire(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("invalid Storm Eye time");
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
        BattleStormEyeStatus battleStormEyeStatus;
        (battleStormEyeStatus = this).expiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleStormEyeStatus);
        this.rangedHit = 0;
        this.rangedDamage = 0;
    }

    private static void validate(
            long nowNanos, int durationSeconds,
            int hitModifier, int damageModifier) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || hitModifier <= 0 || hitModifier > 127
                || damageModifier <= 0 || damageModifier > 127) {
            throw new IllegalArgumentException("invalid native Storm Eye lease");
        }
    }

    record Snapshot(boolean active, int rangedHit, int rangedDamage, int remainingSeconds) {
        static Snapshot none() {
            return new Snapshot(false, 0, 0, 0);
        }
    }
}
