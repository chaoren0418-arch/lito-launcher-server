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

    synchronized void activate(int n, long l, int n2) {
        if (l < 0L || n2 <= 0 || n2 > 65535) {
            throw new IllegalArgumentException("invalid native weight-spell lease");
        }
        long l2 = Math.addExact(l, TimeUnit.SECONDS.toNanos(n2));
        switch (n) {
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

    synchronized Snapshot snapshot(long l) {
        return new Snapshot(BattleWeightStatus.remainingSeconds(l, this.decreaseWeightExpiresAtNanos), BattleWeightStatus.remainingSeconds(l, this.exoticVitalizeExpiresAtNanos), BattleWeightStatus.remainingSeconds(l, this.additionalFireExpiresAtNanos));
    }

    synchronized boolean expire(long l) {
        BattleWeightStatus battleWeightStatus;
        long l2;
        boolean bl = false;
        if (BattleWeightStatus.expired(l, this.decreaseWeightExpiresAtNanos)) {
            l2 = 0L;
            battleWeightStatus = this;
            this.decreaseWeightExpiresAtNanos = l2;
            NativeSourceIcons1Epoch.bump(battleWeightStatus);
            bl = true;
        }
        if (BattleWeightStatus.expired(l, this.exoticVitalizeExpiresAtNanos)) {
            this.exoticVitalizeExpiresAtNanos = 0L;
            bl = true;
        }
        if (BattleWeightStatus.expired(l, this.additionalFireExpiresAtNanos)) {
            l2 = 0L;
            battleWeightStatus = this;
            this.additionalFireExpiresAtNanos = l2;
            NativeSourceIcons1Epoch.bump(battleWeightStatus);
            bl = true;
        }
        return bl;
    }

    synchronized void clear() {
        long l = 0L;
        BattleWeightStatus battleWeightStatus = this;
        this.decreaseWeightExpiresAtNanos = l;
        NativeSourceIcons1Epoch.bump(battleWeightStatus);
        this.exoticVitalizeExpiresAtNanos = 0L;
        l = 0L;
        battleWeightStatus = this;
        this.additionalFireExpiresAtNanos = l;
        NativeSourceIcons1Epoch.bump(battleWeightStatus);
    }

    synchronized boolean cancelMagicEffects() {
        boolean bl = this.decreaseWeightExpiresAtNanos != 0L || this.exoticVitalizeExpiresAtNanos != 0L || this.additionalFireExpiresAtNanos != 0L;
        this.clear();
        return bl;
    }

    private static boolean expired(long l, long l2) {
        return l2 != 0L && l >= l2;
    }

    private static int remainingSeconds(long l, long l2) {
        if (l2 == 0L || l >= l2) {
            return 0;
        }
        long l3 = TimeUnit.SECONDS.toNanos(1L);
        long l4 = (l2 - l + l3 - 1L) / l3;
        return (int)Math.min(65535L, Math.max(1L, l4));
    }

    record Snapshot(int decreaseWeightSeconds, int exoticVitalizeSeconds, int additionalFireSeconds) {
        boolean decreaseWeightActive() {
            return this.decreaseWeightSeconds > 0;
        }

        boolean regenerationBypassActive() {
            return this.exoticVitalizeSeconds > 0 || this.additionalFireSeconds > 0;
        }
    }
}
