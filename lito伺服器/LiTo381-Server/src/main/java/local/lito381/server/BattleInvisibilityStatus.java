package local.lito381.server;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Authoritative skill-owned invisibility lease table.
 *
 * <p>A zero expiry represents classic skill 60's indefinite state. Timed
 * leases use a monotonic nano-time deadline so wall-clock changes cannot
 * extend or shorten Blind Hiding. Official 0.6.71 keeps a snapshot of the
 * current lease map for cancel-captured.</p>
 */
final class BattleInvisibilityStatus {
    private final LinkedHashMap<Integer, Lease> leases = new LinkedHashMap<>();
    private volatile Object sourceInvisibilityLease;

    synchronized boolean active() {
        return !leases.isEmpty();
    }

    synchronized boolean hasSkill(int skillId) {
        return leases.containsKey(skillId);
    }

    synchronized int skillId() {
        int last = 0;
        for (int id : leases.keySet()) {
            last = id;
        }
        return last;
    }

    synchronized long expiresAtNanos() {
        Lease lease = leases.get(skillId());
        return lease == null ? 0L : lease.deadline();
    }

    synchronized void activate(int nextSkillId, long nowNanos, int durationSeconds) {
        if (nextSkillId <= 0 || nowNanos < 0 || durationSeconds < 0) {
            throw new IllegalArgumentException("invalid invisibility lease");
        }
        long deadline = durationSeconds == 0 ? 0L
                : Math.addExact(nowNanos, durationSeconds * 1_000_000_000L);
        leases.remove(nextSkillId);
        leases.put(nextSkillId, new Lease(deadline));
        rotate();
    }

    private void rotate() {
        sourceInvisibilityLease = new Snapshot(Map.copyOf(leases));
    }

    synchronized boolean cancel() {
        if (!active()) return false;
        leases.clear();
        rotate();
        return true;
    }

    /** Official JAR 0.6.71: drop the lease captured at admission. */
    synchronized boolean cancelCaptured(Object captured) {
        Snapshot snapshot;
        if (captured instanceof Snapshot) {
            snapshot = (Snapshot) captured;
        } else {
            return captured == sourceInvisibilityLease && cancel();
        }
        boolean removed = false;
        for (Map.Entry<Integer, Lease> entry : snapshot.leases().entrySet()) {
            if (leases.get(entry.getKey()) == entry.getValue()) {
                leases.remove(entry.getKey());
                removed = true;
            }
        }
        if (removed) rotate();
        return removed;
    }

    synchronized int expire(long nowNanos) {
        if (nowNanos < 0) throw new IllegalArgumentException("negative nano time");
        int expiredSkillId = 0;
        Iterator<Map.Entry<Integer, Lease>> iterator = leases.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Lease> entry = iterator.next();
            long deadline = entry.getValue().deadline();
            if (deadline != 0L && nowNanos >= deadline) {
                expiredSkillId = entry.getKey();
                iterator.remove();
            }
        }
        if (expiredSkillId != 0) rotate();
        return leases.isEmpty() ? expiredSkillId : 0;
    }

    private record Lease(long deadline) {}

    private record Snapshot(Map<Integer, Lease> leases) {}
}
