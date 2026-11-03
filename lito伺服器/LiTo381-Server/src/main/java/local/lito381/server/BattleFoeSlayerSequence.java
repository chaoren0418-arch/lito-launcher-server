package local.lito381.server;

/**
 * Synchronous three-onAction lease used by the retained 3.81 Foe Slayer mode.
 *
 * <p>The exposure stack is consumed once before the mode starts, while its
 * +20/+40/+60 damage is reused by each of the three ordinary attack calls.
 * Keeping this as an explicit short-lived lease prevents those calls from
 * advancing a new chainsword exposure while Foe Slayer is active.
 */
final class BattleFoeSlayerSequence {
    private long targetObjectId;
    private int exposureLevel;
    private int damageBonus;
    private int totalStrikes;
    private int remainingStrikes;

    synchronized Start begin(
            long target, int consumedExposureLevel, int consumedDamageBonus,
            int strikes) {
        if (target <= 0 || consumedExposureLevel < 0
                || consumedExposureLevel > 3 || consumedDamageBonus < 0
                || strikes <= 0) {
            throw new IllegalArgumentException("invalid Foe Slayer sequence");
        }
        targetObjectId = target;
        exposureLevel = consumedExposureLevel;
        damageBonus = consumedDamageBonus;
        totalStrikes = strikes;
        remainingStrikes = strikes;
        return new Start(targetObjectId, exposureLevel, damageBonus,
                totalStrikes);
    }

    synchronized Strike next(long target) {
        if (target <= 0) {
            throw new IllegalArgumentException("invalid Foe Slayer strike target");
        }
        if (remainingStrikes == 0) return Strike.none();
        if (targetObjectId != target) {
            clearInternal();
            return new Strike(false, true, 0, 0, 0, 0, true);
        }
        int strikeIndex = totalStrikes - remainingStrikes + 1;
        remainingStrikes--;
        boolean finalStrike = remainingStrikes == 0;
        Strike strike = new Strike(true, false, strikeIndex, totalStrikes,
                exposureLevel, damageBonus, finalStrike);
        if (finalStrike) clearInternal();
        return strike;
    }

    synchronized Snapshot snapshot() {
        return new Snapshot(targetObjectId, exposureLevel, damageBonus,
                totalStrikes, remainingStrikes);
    }

    synchronized void clear() {
        clearInternal();
    }

    private void clearInternal() {
        targetObjectId = 0;
        exposureLevel = 0;
        damageBonus = 0;
        totalStrikes = 0;
        remainingStrikes = 0;
    }

    record Start(long targetObjectId, int exposureLevel, int damageBonus,
                 int totalStrikes) {}

    record Strike(
            boolean active, boolean targetMismatch, int strikeIndex,
            int totalStrikes, int exposureLevel, int damageBonus,
            boolean finalStrike) {
        static Strike none() {
            return new Strike(false, false, 0, 0, 0, 0, false);
        }
    }

    record Snapshot(
            long targetObjectId, int exposureLevel, int damageBonus,
            int totalStrikes, int remainingStrikes) {}
}
