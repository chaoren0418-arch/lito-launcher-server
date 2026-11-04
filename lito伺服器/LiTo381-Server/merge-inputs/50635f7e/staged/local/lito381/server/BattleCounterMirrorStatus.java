package local.lito381.server;

/** Authoritative retained skill-134 one-shot magic-reflection lease. */
import java.util.concurrent.TimeUnit;

final class BattleCounterMirrorStatus {
    private long expiresAtNanos;
    volatile transient long nativeSourceIcons1Version;

    BattleCounterMirrorStatus() {
    }

    synchronized void activate(long l, int n) {
        if (l < 0L || n <= 0 || n > 65535) {
            throw new IllegalArgumentException("invalid native Counter Mirror lease");
        }
        long l2 = Math.addExact(l, TimeUnit.SECONDS.toNanos(n));
        BattleCounterMirrorStatus battleCounterMirrorStatus = this;
        this.expiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleCounterMirrorStatus);
    }

    synchronized boolean active(long l) {
        if (l < 0L) {
            throw new IllegalArgumentException("negative clock");
        }
        return this.expiresAtNanos != 0L && l < this.expiresAtNanos;
    }

    synchronized Decision resolve(long l, int n, int n2, int n3, boolean bl) {
        boolean bl2;
        if (l < 0L || n < 0 || n > 255 || n2 < 0 || n2 > 99 || n3 < 0) {
            throw new IllegalArgumentException("invalid Counter Mirror resolution input");
        }
        boolean bl3 = this.active(l);
        if (!bl3) {
            if (this.expiresAtNanos != 0L && l >= this.expiresAtNanos) {
                long l2 = 0L;
                BattleCounterMirrorStatus battleCounterMirrorStatus = this;
                this.expiresAtNanos = l2;
                NativeSourceIcons1Epoch.bump(battleCounterMirrorStatus);
            }
            return Decision.inactive(n, n2);
        }
        boolean bl4 = bl2 = bl && n3 > 0 && n >= n2;
        if (bl2) {
            long l3 = 0L;
            BattleCounterMirrorStatus battleCounterMirrorStatus = this;
            this.expiresAtNanos = l3;
            NativeSourceIcons1Epoch.bump(battleCounterMirrorStatus);
        }
        return new Decision(true, bl, bl2, n, n2, bl2 ? n3 : 0);
    }

    synchronized boolean expire(long l) {
        if (this.expiresAtNanos == 0L || l < this.expiresAtNanos) {
            return false;
        }
        long l2 = 0L;
        BattleCounterMirrorStatus battleCounterMirrorStatus = this;
        this.expiresAtNanos = l2;
        NativeSourceIcons1Epoch.bump(battleCounterMirrorStatus);
        return true;
    }

    synchronized boolean cancel() {
        if (this.expiresAtNanos == 0L) {
            return false;
        }
        long l = 0L;
        BattleCounterMirrorStatus battleCounterMirrorStatus = this;
        this.expiresAtNanos = l;
        NativeSourceIcons1Epoch.bump(battleCounterMirrorStatus);
        return true;
    }

    synchronized void clear() {
        long l = 0L;
        BattleCounterMirrorStatus battleCounterMirrorStatus = this;
        this.expiresAtNanos = l;
        NativeSourceIcons1Epoch.bump(battleCounterMirrorStatus);
    }

    record Decision(boolean active, boolean eligible, boolean triggered, int wisdom, int roll, int reflectedDamage) {
        Decision {
            if (wisdom < 0 || wisdom > 255 || roll < 0 || roll > 99 || reflectedDamage < 0 || triggered != (active && eligible && wisdom >= roll && reflectedDamage > 0)) {
                throw new IllegalArgumentException("invalid Counter Mirror decision");
            }
        }

        static Decision inactive(int wisdom, int roll) {
            return new Decision(false, false, false, wisdom, roll, 0);
        }
    }
}
