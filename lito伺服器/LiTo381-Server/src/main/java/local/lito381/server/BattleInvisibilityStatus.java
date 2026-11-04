package local.lito381.server;

/**
 * Authoritative skill-owned invisibility lease.
 *
 * <p>A zero expiry represents classic skill 60's indefinite state. Timed
 * leases use a monotonic nano-time deadline so wall-clock changes cannot
 * extend or shorten Blind Hiding.</p>
 */
final class BattleInvisibilityStatus {
    private int skillId;
    private long expiresAtNanos;

    synchronized boolean active() {
        return skillId != 0;
    }

    synchronized int skillId() {
        return skillId;
    }

    synchronized long expiresAtNanos() {
        return expiresAtNanos;
    }

    synchronized void activate(int nextSkillId, long nowNanos, int durationSeconds) {
        if (nextSkillId <= 0 || nowNanos < 0 || durationSeconds < 0) {
            throw new IllegalArgumentException("invalid invisibility lease");
        }
        skillId = nextSkillId;
        expiresAtNanos = durationSeconds == 0 ? 0L
                : Math.addExact(nowNanos, durationSeconds * 1_000_000_000L);
    }

    synchronized boolean cancel() {
        if (skillId == 0) return false;
        skillId = 0;
        expiresAtNanos = 0L;
        return true;
    }

    /** Official JAR 0.6.71: drop the lease captured at admission. */
    synchronized boolean cancelCaptured(Object captured) {
        if (captured == null || skillId == 0) return false;
        return cancel();
    }

    synchronized int expire(long nowNanos) {
        if (nowNanos < 0) throw new IllegalArgumentException("negative nano time");
        if (skillId == 0 || expiresAtNanos == 0L || nowNanos < expiresAtNanos) return 0;
        int expiredSkillId = skillId;
        skillId = 0;
        expiresAtNanos = 0L;
        return expiredSkillId;
    }
}
