package local.lito381.server;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Authoritative non-persistent lease table for skill211 Patience.
 *
 * <p>The retained attack pipeline reads Patience from the PC target's flat
 * physical-damage modifiers.  The selected source comment fixes the original
 * value at {@code -2}; the neighbouring {@code -15} value is a later custom
 * server amplification.  NPCs may own the ordinary status and receive GFX
 * 6507, but only player damage paths query {@link #physicalReduction(long,
 * long)}.</p>
 */
final class BattlePatienceStatus {
    static final int SKILL_ID = 211;
    static final int DURATION_SECONDS = 600;
    static final int PHYSICAL_REDUCTION = 2;

    record Activation(boolean accepted, boolean refreshed,
                      long expiresAtNanos) {
        static Activation rejected() {
            return new Activation(false, false, 0);
        }
    }

    private final Map<Long, Long> expiresAtByObjectId = new LinkedHashMap<>();

    synchronized Activation activate(long objectId, long nowNanos,
                                     int durationSeconds) {
        if (objectId <= 0 || objectId > 0xFFFF_FFFFL || nowNanos < 0
                || durationSeconds != DURATION_SECONDS) {
            return Activation.rejected();
        }
        boolean refreshed = active(objectId, nowNanos);
        long expiresAt = Math.addExact(nowNanos,
                TimeUnit.SECONDS.toNanos(durationSeconds));
        expiresAtByObjectId.put(objectId, expiresAt);
        return new Activation(true, refreshed, expiresAt);
    }

    synchronized boolean active(long objectId, long nowNanos) {
        if (objectId <= 0 || nowNanos < 0) return false;
        Long expiresAt = expiresAtByObjectId.get(objectId);
        if (expiresAt == null) return false;
        if (nowNanos < expiresAt) return true;
        expiresAtByObjectId.remove(objectId);
        return false;
    }

    synchronized int physicalReduction(long playerObjectId, long nowNanos) {
        return active(playerObjectId, nowNanos) ? PHYSICAL_REDUCTION : 0;
    }

    synchronized boolean cancel(long objectId) {
        return objectId > 0 && expiresAtByObjectId.remove(objectId) != null;
    }

    synchronized int activeCount(long nowNanos) {
        if (nowNanos < 0) return 0;
        for (Iterator<Map.Entry<Long, Long>> iterator =
                expiresAtByObjectId.entrySet().iterator(); iterator.hasNext();) {
            if (nowNanos >= iterator.next().getValue()) iterator.remove();
        }
        return expiresAtByObjectId.size();
    }

    synchronized void clear() {
        expiresAtByObjectId.clear();
    }
}
