// ported from candidate 50635f7e, integration-lifecycle final frame, issue #67
package local.lito381.server;

/** Server-authoritative leases for the three native inventory-weight spells. */
import java.util.concurrent.TimeUnit;

final class BattleWeightStatus {
    private long decreaseWeightExpiresAtNanos;
    private long exoticVitalizeExpiresAtNanos;
    private long additionalFireExpiresAtNanos;
    volatile transient long nativeSourceIcons1Version;

    BattleWeightStatus() {
    }

    synchronized void activate(int skillId, long nowNanos, int durationSeconds) {
        if (nowNanos < 0L || durationSeconds <= 0 || durationSeconds > 65535) {
            throw new IllegalArgumentException("invalid native weight-spell lease");
        }
        long l2 = Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        switch (skillId) {
            case 14: {
                long l3 = l2;
                BattleWeightStatus battleWeightStatus = this;
                this.decreaseWeightExpiresAtNanos = l3;
                NativeSourceIcons1Epoch.bump(battleWeightStatus);
                break;
            }
            case 169: {
                this.exoticVitalizeExpiresAtNanos = l2;
                break;
            }
            case 176: {
                long l4 = l2;
                BattleWeightStatus battleWeightStatus = this;
                this.additionalFireExpiresAtNanos = l4;
                NativeSourceIcons1Epoch.bump(battleWeightStatus);
                break;
            }
            default: {
                throw new IllegalArgumentException("unknown native weight spell");
            }
        }
    }

    synchronized Snapshot snapshot(long nowNanos) {
        return new Snapshot(
                remainingSeconds(nowNanos, decreaseWeightExpiresAtNanos),
                remainingSeconds(nowNanos, exoticVitalizeExpiresAtNanos),
                remainingSeconds(nowNanos, additionalFireExpiresAtNanos));
    }

    synchronized boolean expire(long nowNanos) {
        BattleWeightStatus battleWeightStatus;
        long l2;
        boolean bl = false;
        if (BattleWeightStatus.expired(nowNanos, this.decreaseWeightExpiresAtNanos)) {
            l2 = 0L;
            battleWeightStatus = this;
            this.decreaseWeightExpiresAtNanos = l2;
            NativeSourceIcons1Epoch.bump(battleWeightStatus);
            bl = true;
        }
        if (BattleWeightStatus.expired(nowNanos, this.exoticVitalizeExpiresAtNanos)) {
            this.exoticVitalizeExpiresAtNanos = 0L;
            bl = true;
        }
        if (BattleWeightStatus.expired(nowNanos, this.additionalFireExpiresAtNanos)) {
            l2 = 0L;
            battleWeightStatus = this;
            this.additionalFireExpiresAtNanos = l2;
            NativeSourceIcons1Epoch.bump(battleWeightStatus);
            bl = true;
        }
        return bl;
    }

    synchronized void clear() {
        BattleWeightStatus battleWeightStatus;
        (battleWeightStatus = this).decreaseWeightExpiresAtNanos = 0L;
        NativeSourceIcons1Epoch.bump(battleWeightStatus);
        this.exoticVitalizeExpiresAtNanos = 0L;
        long l = 0L;
        battleWeightStatus = this;
        this.additionalFireExpiresAtNanos = l;
        NativeSourceIcons1Epoch.bump(battleWeightStatus);
    }

    synchronized boolean cancelMagicEffects() {
        boolean present = decreaseWeightExpiresAtNanos != 0
                || exoticVitalizeExpiresAtNanos != 0
                || additionalFireExpiresAtNanos != 0;
        clear();
        return present;
    }

    private static boolean expired(long nowNanos, long expiresAtNanos) {
        return expiresAtNanos != 0 && nowNanos >= expiresAtNanos;
    }

    private static int remainingSeconds(long nowNanos, long expiresAtNanos) {
        if (expiresAtNanos == 0 || nowNanos >= expiresAtNanos) return 0;
        long second = TimeUnit.SECONDS.toNanos(1);
        long remaining = (expiresAtNanos - nowNanos + second - 1) / second;
        return (int) Math.min(0xFFFFL, Math.max(1L, remaining));
    }

    record Snapshot(int decreaseWeightSeconds, int exoticVitalizeSeconds, int additionalFireSeconds) {
        boolean decreaseWeightActive() { return decreaseWeightSeconds > 0; }

        boolean regenerationBypassActive() {
            return exoticVitalizeSeconds > 0 || additionalFireSeconds > 0;
        }
    }
}
