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

    synchronized boolean activate(long l, int n, int n2, int n3) {
        BattleStormEyeStatus.validate(l, n, n2, n3);
        boolean bl = this.expiresAtNanos != 0L && l < this.expiresAtNanos;
        long l2 = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        BattleStormEyeStatus battleStormEyeStatus = this;
        this.expiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleStormEyeStatus);
        this.rangedHit = n2;
        this.rangedDamage = n3;
        return bl;
    }

    synchronized Snapshot snapshot(long l) {
        if (l < 0L) {
            throw new IllegalArgumentException("invalid Storm Eye time");
        }
        if (this.expiresAtNanos == 0L || l >= this.expiresAtNanos) {
            return Snapshot.none();
        }
        long l2 = this.expiresAtNanos - l;
        long l3 = TimeUnit.SECONDS.toNanos(1L);
        int n = (int)Math.min(65535L, Math.max(1L, (l2 + l3 - 1L) / l3));
        return new Snapshot(true, CombatBuffLeases.value(this.rangedHit, 156, "BOW_HIT"), CombatBuffLeases.value(this.rangedDamage, 156, "BOW_DMG"), n);
    }

    synchronized int physicalHit(boolean bl, long l) {
        return bl ? this.snapshot(l).rangedHit() : 0;
    }

    synchronized int physicalDamage(boolean bl, long l) {
        return bl ? this.snapshot(l).rangedDamage() : 0;
    }

    synchronized boolean expire(long l) {
        if (l < 0L) {
            throw new IllegalArgumentException("invalid Storm Eye time");
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
        BattleStormEyeStatus battleStormEyeStatus = this;
        this.expiresAtNanos = l;
        NativeSourceIcons1Epoch.bump(battleStormEyeStatus);
        this.rangedHit = 0;
        this.rangedDamage = 0;
    }

    private static void validate(long l, int n, int n2, int n3) {
        if (l < 0L || n <= 0 || n > 65535 || n2 <= 0 || n2 > 127 || n3 <= 0 || n3 > 127) {
            throw new IllegalArgumentException("invalid native Storm Eye lease");
        }
    }

    record Snapshot(boolean active, int rangedHit, int rangedDamage, int remainingSeconds) {
        static Snapshot none() {
            return new Snapshot(false, 0, 0, 0);
        }
    }
}
