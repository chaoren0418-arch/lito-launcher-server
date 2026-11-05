// ported from candidate 50635f7e, integration-lifecycle final frame, issue #67
package local.lito381.server;

/** Authoritative retained skill-134 one-shot magic-reflection lease. */
import java.util.concurrent.TimeUnit;

final class BattleCounterMirrorStatus {
    private long expiresAtNanos;
    volatile transient long nativeSourceIcons1Version;

    BattleCounterMirrorStatus() {
    }

    synchronized void activate(long nowNanos, int durationSeconds) {
        if (nowNanos < 0L || durationSeconds <= 0 || durationSeconds > 65535) {
            throw new IllegalArgumentException("invalid native Counter Mirror lease");
        }
        BattleCounterMirrorStatus battleCounterMirrorStatus;
        (battleCounterMirrorStatus = this).expiresAtNanos = Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        NativeSourceIcons1Epoch.bump(battleCounterMirrorStatus);
    }

    synchronized boolean active(long nowNanos) {
        if (nowNanos < 0) throw new IllegalArgumentException("negative clock");
        return expiresAtNanos != 0 && nowNanos < expiresAtNanos;
    }

    synchronized Decision resolve(long nowNanos, int wisdom, int zeroBasedRoll, int incomingDamage, boolean eligibleSource) {
        boolean bl2;
        if (nowNanos < 0L || wisdom < 0 || wisdom > 255 || zeroBasedRoll < 0 || zeroBasedRoll > 99 || incomingDamage < 0) {
            throw new IllegalArgumentException("invalid Counter Mirror resolution input");
        }
        boolean bl3 = this.active(nowNanos);
        if (!bl3) {
            if (this.expiresAtNanos != 0L && nowNanos >= this.expiresAtNanos) {
                long l2 = 0L;
                BattleCounterMirrorStatus battleCounterMirrorStatus = this;
                this.expiresAtNanos = l2;
                NativeSourceIcons1Epoch.bump(battleCounterMirrorStatus);
            }
            return Decision.inactive(wisdom, zeroBasedRoll);
        }
        boolean bl4 = bl2 = eligibleSource && incomingDamage > 0 && wisdom >= zeroBasedRoll;
        if (bl2) {
            long l3 = 0L;
            BattleCounterMirrorStatus battleCounterMirrorStatus = this;
            this.expiresAtNanos = l3;
            NativeSourceIcons1Epoch.bump(battleCounterMirrorStatus);
        }
        return new Decision(true, eligibleSource, bl2, wisdom, zeroBasedRoll, bl2 ? incomingDamage : 0);
    }

    synchronized boolean expire(long nowNanos) {
        if (this.expiresAtNanos == 0L || nowNanos < this.expiresAtNanos) {
            return false;
        }
        BattleCounterMirrorStatus battleCounterMirrorStatus;
        (battleCounterMirrorStatus = this).expiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleCounterMirrorStatus);
        return true;
    }

    synchronized boolean cancel() {
        if (this.expiresAtNanos == 0L) {
            return false;
        }
        BattleCounterMirrorStatus battleCounterMirrorStatus;
        (battleCounterMirrorStatus = this).expiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleCounterMirrorStatus);
        return true;
    }

    synchronized void clear() {
        BattleCounterMirrorStatus battleCounterMirrorStatus;
        (battleCounterMirrorStatus = this).expiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleCounterMirrorStatus);
    }

    record Decision(boolean active, boolean eligible, boolean triggered, int wisdom, int roll, int reflectedDamage) {
        Decision {
            if (wisdom < 0 || wisdom > 255 || roll < 0 || roll > 99 || reflectedDamage < 0 || triggered != (active && eligible && wisdom >= roll && reflectedDamage > 0)) {
                throw new IllegalArgumentException("invalid Counter Mirror decision");
            }
        }

        static Decision inactive(int wisdom, int roll) {
            return new Decision(false, false, false,
                    wisdom, roll, 0);
        }
    }
}
