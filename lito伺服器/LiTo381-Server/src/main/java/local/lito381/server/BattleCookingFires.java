package local.lito381.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Room-scoped NPC 81170 / GFX 5943 leases created by classic fuel item 41260. */
final class BattleCookingFires {
    static final int NPC_ID = 81170;
    static final int GFX = BattleCookingCraftingCatalog.FIRE_EFFECT_GFX;
    static final int DURATION_SECONDS = 600;
    static final int NEARBY_RADIUS = 3;

    // Every existing Tournament world/runtime range is below 0x80000000.
    // Keep fire actors in their own unsigned-dword band so they cannot alias
    // companions, doors, NPCs, encounters, created characters or inventory.
    private static final long FIRST_OBJECT_ID = 0x8000_0000L;
    private static final AtomicLong NEXT_OBJECT_ID = new AtomicLong(FIRST_OBJECT_ID);

    record Fire(long objectId, int x, int y, long expiresAtNanos) {
        Fire {
            if (objectId < FIRST_OBJECT_ID || objectId >= 0x9000_0000L
                    || x < 0 || x > 0xFFFF || y < 0 || y > 0xFFFF
                    || expiresAtNanos <= 0) {
                throw new IllegalArgumentException("invalid cooking fire");
            }
        }
    }

    private final List<Fire> fires = new ArrayList<>();

    boolean near(int x, int y, long nowNanos) {
        if (x < 0 || x > 0xFFFF || y < 0 || y > 0xFFFF || nowNanos < 0) return false;
        for (Fire fire : fires) {
            if (nowNanos >= fire.expiresAtNanos()) continue;
            int distance = Math.max(Math.abs(x - fire.x()), Math.abs(y - fire.y()));
            if (distance <= NEARBY_RADIUS) return true;
        }
        return false;
    }

    Fire place(int x, int y, long nowNanos) {
        if (x < 0 || x > 0xFFFF || y < 0 || y > 0xFFFF || nowNanos < 0) return null;
        long expiresAt = Math.addExact(nowNanos,
                Math.multiplyExact(DURATION_SECONDS, 1_000_000_000L));
        Fire fire = new Fire(nextObjectId(), x, y, expiresAt);
        fires.add(fire);
        return fire;
    }

    List<Fire> active(long nowNanos) {
        if (nowNanos < 0) return List.of();
        return fires.stream().filter(fire -> nowNanos < fire.expiresAtNanos()).toList();
    }

    List<Fire> expire(long nowNanos) {
        if (nowNanos < 0) return List.of();
        List<Fire> expired = new ArrayList<>();
        for (Iterator<Fire> iterator = fires.iterator(); iterator.hasNext();) {
            Fire fire = iterator.next();
            if (nowNanos < fire.expiresAtNanos()) continue;
            iterator.remove();
            expired.add(fire);
        }
        return List.copyOf(expired);
    }

    void clear() {
        fires.clear();
    }

    private static long nextObjectId() {
        return NEXT_OBJECT_ID.getAndUpdate(current ->
                current >= 0x8FFF_FFFFL ? FIRST_OBJECT_ID : current + 1);
    }
}
