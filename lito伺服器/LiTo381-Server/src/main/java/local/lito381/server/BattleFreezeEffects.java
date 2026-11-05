package local.lito381.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** Server-owned temporary NPC 81168 shown for the active freeze lease. */
final class BattleFreezeEffects {
    static final int NPC_ID = 81168;
    static final int GFX = 176;
    private static final long FIRST_OBJECT_ID = 0x7100_0000L;
    private static final AtomicLong NEXT_OBJECT_ID = new AtomicLong(FIRST_OBJECT_ID);

    record Effect(long objectId, long targetObjectId, int x, int y,
                  long expiresAtNanos) {
        Effect {
            if (objectId < FIRST_OBJECT_ID || objectId > 0x71FF_FFFFL
                    || targetObjectId == 0 || x < 0 || x > 0xFFFF
                    || y < 0 || y > 0xFFFF || expiresAtNanos <= 0) {
                throw new IllegalArgumentException("invalid freeze effect");
            }
        }
    }

    record Placement(Effect previous, Effect placed) {
        Placement {
            if (placed == null) {
                throw new IllegalArgumentException("placed freeze effect is null");
            }
        }
    }

    private final Map<Long, Effect> byTarget = new LinkedHashMap<>();

    synchronized Placement place(long targetObjectId, int x, int y,
                                 long nowNanos, int durationSeconds) {
        durationSeconds = CombatWorldFreeze.visualDuration(durationSeconds);
        if (targetObjectId == 0 || x < 0 || x > 0xFFFF || y < 0 || y > 0xFFFF
                || nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 32) {
            throw new IllegalArgumentException("invalid freeze effect placement");
        }
        Effect previous = byTarget.remove(targetObjectId);
        Effect placed = new Effect(nextObjectId(), targetObjectId, x, y,
                CombatWorldFreeze.visualExpiry(
                        Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds)),
                        nowNanos));
        byTarget.put(targetObjectId, placed);
        return new Placement(previous, placed);
    }

    synchronized Effect remove(long targetObjectId) {
        return byTarget.remove(targetObjectId);
    }

    synchronized List<Effect> active(long nowNanos) {
        expire(nowNanos);
        return List.copyOf(byTarget.values());
    }

    synchronized List<Effect> expire(long nowNanos) {
        if (nowNanos < 0) throw new IllegalArgumentException("negative freeze clock");
        List<Effect> expired = new ArrayList<>();
        byTarget.values().removeIf(effect -> {
            if (nowNanos < effect.expiresAtNanos()) return false;
            expired.add(effect);
            return true;
        });
        return List.copyOf(expired);
    }

    synchronized void clear() {
        byTarget.clear();
    }

    private static long nextObjectId() {
        return NEXT_OBJECT_ID.getAndUpdate(current ->
                current >= 0x71FF_FFFFL ? FIRST_OBJECT_ID : current + 1);
    }
}
