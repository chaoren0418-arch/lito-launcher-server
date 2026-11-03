package local.lito381.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** Server-owned temporary NPC 86123 shown for skill208's fixed two seconds. */
final class BattleBoneBreakEffects {
    static final int NPC_ID = 86_123;
    static final int GFX = 6_552;
    private static final long FIRST_OBJECT_ID = 0x7100_0000L;
    private static final AtomicLong NEXT_OBJECT_ID =
            new AtomicLong(FIRST_OBJECT_ID);

    record Effect(long objectId, long targetObjectId, int x, int y,
                  long expiresAtNanos) {
        Effect {
            if (objectId < FIRST_OBJECT_ID || objectId > 0x71FF_FFFFL
                    || targetObjectId == 0 || x < 0 || x > 0xFFFF
                    || y < 0 || y > 0xFFFF || expiresAtNanos <= 0) {
                throw new IllegalArgumentException(
                        "invalid Bone Break effect");
            }
        }
    }

    private final Map<Long, Effect> byTarget = new LinkedHashMap<>();

    synchronized Effect placeIfAbsent(long targetObjectId, int x, int y,
                                      long nowNanos, int durationSeconds) {
        if (targetObjectId == 0 || x < 0 || x > 0xFFFF
                || y < 0 || y > 0xFFFF || nowNanos < 0
                || durationSeconds != 2) {
            throw new IllegalArgumentException(
                    "invalid Bone Break effect placement");
        }
        expire(nowNanos);
        if (byTarget.containsKey(targetObjectId)) return null;
        Effect effect = new Effect(nextObjectId(), targetObjectId, x, y,
                Math.addExact(nowNanos,
                        TimeUnit.SECONDS.toNanos(durationSeconds)));
        byTarget.put(targetObjectId, effect);
        return effect;
    }

    synchronized List<Effect> active(long nowNanos) {
        expire(nowNanos);
        return List.copyOf(byTarget.values());
    }

    synchronized List<Effect> expire(long nowNanos) {
        if (nowNanos < 0) {
            throw new IllegalArgumentException("negative Bone Break clock");
        }
        List<Effect> expired = new ArrayList<>();
        byTarget.values().removeIf(effect -> {
            if (nowNanos < effect.expiresAtNanos()) return false;
            expired.add(effect);
            return true;
        });
        return List.copyOf(expired);
    }

    synchronized Effect remove(long targetObjectId) {
        if (targetObjectId == 0) return null;
        return byTarget.remove(targetObjectId);
    }

    synchronized void clear() {
        byTarget.clear();
    }

    private static long nextObjectId() {
        return NEXT_OBJECT_ID.getAndUpdate(current ->
                current >= 0x71FF_FFFFL ? FIRST_OBJECT_ID : current + 1);
    }
}
