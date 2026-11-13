// ported from candidate 50635f7e, integration-lifecycle final frame, issue #67
package local.lito381.server;

/** Authoritative cancelable lease for native Elf skill166 Storm Shot. */
import java.util.concurrent.TimeUnit;

final class BattleStormShotStatus {
    private long expiresAtNanos;
    private int rangedHit;
    private int rangedDamage;
    volatile transient long nativeSourceIcons1Version;

    BattleStormShotStatus() {
    }

    synchronized boolean activate(long nowNanos, int durationSeconds, int hitModifier, int damageModifier) {
        BattleStormShotStatus.validate(nowNanos, durationSeconds, hitModifier, damageModifier);
        boolean bl = this.expiresAtNanos != 0L && nowNanos < this.expiresAtNanos;
        BattleStormShotStatus battleStormShotStatus;
        (battleStormShotStatus = this).expiresAtNanos = Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        NativeSourceIcons1Epoch.bump(battleStormShotStatus);
        this.rangedHit = hitModifier;
        this.rangedDamage = damageModifier;
        return bl;
    }

    synchronized Snapshot snapshot(long nowNanos) {
        if (nowNanos < 0L) {
            throw new IllegalArgumentException("invalid Storm Shot time");
        }
        if (this.expiresAtNanos == 0L || nowNanos >= this.expiresAtNanos) {
            return Snapshot.none();
        }
        long l2 = this.expiresAtNanos - nowNanos;
        long l3 = TimeUnit.SECONDS.toNanos(1L);
        int n = (int)Math.min(65535L, Math.max(1L, (l2 + l3 - 1L) / l3));
        return new Snapshot(true, CombatBuffLeases.value(this.rangedHit, 166, "BOW_HIT"), CombatBuffLeases.value(this.rangedDamage, 166, "BOW_DMG"), n);
    }

    synchronized int physicalHit(boolean ranged, long nowNanos) {
        return ranged ? snapshot(nowNanos).rangedHit() : 0;
    }

    synchronized int physicalDamage(boolean ranged, long nowNanos) {
        return ranged ? snapshot(nowNanos).rangedDamage() : 0;
    }

    synchronized boolean expire(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("invalid Storm Shot time");
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
        BattleStormShotStatus battleStormShotStatus;
        (battleStormShotStatus = this).expiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleStormShotStatus);
        this.rangedHit = 0;
        this.rangedDamage = 0;
    }

    private static void validate(
            long nowNanos, int durationSeconds,
            int hitModifier, int damageModifier) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || hitModifier != -1 || damageModifier != 5) {
            throw new IllegalArgumentException("invalid native Storm Shot lease");
        }
    }

    record Snapshot(boolean active, int rangedHit, int rangedDamage, int remainingSeconds) {
        static Snapshot none() {
            return new Snapshot(false, 0, 0, 0);
        }
    }
}
