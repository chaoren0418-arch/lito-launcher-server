// ported from candidate 50635f7e, integration-lifecycle final frame, issue #67
package local.lito381.server;

/** Authoritative cancelable lease for native Elf skill149 Wind Shot. */
import java.util.concurrent.TimeUnit;

final class BattleWindShotStatus {
    private long expiresAtNanos;
    private int rangedHit;
    volatile transient long nativeSourceIcons1Version;

    BattleWindShotStatus() {
    }

    synchronized boolean activate(long nowNanos, int durationSeconds, int hitModifier) {
        BattleWindShotStatus.validate(nowNanos, durationSeconds, hitModifier);
        boolean bl = this.expiresAtNanos != 0L && nowNanos < this.expiresAtNanos;
        BattleWindShotStatus battleWindShotStatus;
        (battleWindShotStatus = this).expiresAtNanos = Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        NativeSourceIcons1Epoch.bump(battleWindShotStatus);
        this.rangedHit = hitModifier;
        return bl;
    }

    synchronized Snapshot snapshot(long nowNanos) {
        if (nowNanos < 0L) {
            throw new IllegalArgumentException("invalid Wind Shot time");
        }
        if (this.expiresAtNanos == 0L || nowNanos >= this.expiresAtNanos) {
            return Snapshot.none();
        }
        long l2 = this.expiresAtNanos - nowNanos;
        long l3 = TimeUnit.SECONDS.toNanos(1L);
        int n = (int)Math.min(65535L, Math.max(1L, (l2 + l3 - 1L) / l3));
        return new Snapshot(true, CombatBuffLeases.value(this.rangedHit, 149, "BOW_HIT"), n);
    }

    synchronized int physicalHit(boolean ranged, long nowNanos) {
        return ranged ? snapshot(nowNanos).rangedHit() : 0;
    }

    synchronized boolean expire(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("invalid Wind Shot time");
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
        BattleWindShotStatus battleWindShotStatus;
        (battleWindShotStatus = this).expiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleWindShotStatus);
        this.rangedHit = 0;
    }

    private static void validate(
            long nowNanos, int durationSeconds, int hitModifier) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || hitModifier <= 0 || hitModifier > 127) {
            throw new IllegalArgumentException("invalid native Wind Shot lease");
        }
    }

    record Snapshot(boolean active, int rangedHit, int remainingSeconds) {
        static Snapshot none() {
            return new Snapshot(false, 0, 0);
        }
    }
}
