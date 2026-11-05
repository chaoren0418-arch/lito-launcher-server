package local.lito381.server;

import java.util.concurrent.TimeUnit;

/** Native skill213's non-persistent, non-refreshing DMGUP2/status1026 lease. */
final class BattleArmBreakerStatus {
    static final int SKILL_ID = 213;
    static final int RETAINED_STATUS_ID = 1_026;
    static final int DURATION_SECONDS = 12;
    static final int MELEE_DAMAGE_MODIFIER = -2;

    record Activation(boolean accepted, boolean alreadyActive,
                      long expiresAtNanos) {
        static Activation rejected() {
            return new Activation(false, false, 0);
        }
    }

    private long expiresAtNanos;

    synchronized Activation activateIfAbsent(long nowNanos,
                                             int durationSeconds) {
        if (nowNanos < 0 || !CombatBuffLeases.validArmDuration(durationSeconds)) {
            return Activation.rejected();
        }
        if (nowNanos < expiresAtNanos) {
            return new Activation(true, true, expiresAtNanos);
        }
        expiresAtNanos = Math.addExact(nowNanos,
                TimeUnit.SECONDS.toNanos(durationSeconds));
        return new Activation(true, false, expiresAtNanos);
    }

    synchronized boolean active(long nowNanos) {
        if (nowNanos < 0) return false;
        if (expiresAtNanos == 0) return false;
        if (nowNanos < expiresAtNanos) return true;
        expiresAtNanos = 0;
        return false;
    }

    synchronized int meleeDamageModifier(long nowNanos) {
        return active(nowNanos)
                ? CombatBuffLeases.value(MELEE_DAMAGE_MODIFIER, SKILL_ID, "DMG")
                : 0;
    }

    synchronized boolean cancel() {
        if (expiresAtNanos == 0) return false;
        expiresAtNanos = 0;
        return true;
    }

    synchronized void clear() {
        expiresAtNanos = 0;
    }
}
