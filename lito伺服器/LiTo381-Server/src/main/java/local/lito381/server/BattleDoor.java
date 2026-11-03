package local.lito381.server;

import java.util.concurrent.atomic.AtomicLong;

/** One server-owned interactive door in each active Tournament battle world. */
final class BattleDoor {
    static final int GFX = 2083;
    static final int ACTION_OPEN = 28;
    static final int ACTION_CLOSED = 29;
    static final int DIRECTION_DIAGONAL = 0;
    static final int PASSABLE = 0x00;
    static final int NOT_PASSABLE = 0x41;

    private static final long FIRST_OBJECT_ID = 0x6200_0000L;
    private static final AtomicLong NEXT_OBJECT_ID = new AtomicLong(FIRST_OBJECT_ID);

    record Snapshot(long objectId, int x, int y, int action, boolean open,
                    int direction, int passable) {}

    private final long objectId = NEXT_OBJECT_ID.getAndUpdate(current ->
            current >= 0x62FF_FFFFL ? FIRST_OBJECT_ID : current + 1);
    private int x;
    private int y;
    private boolean active;
    private boolean open;

    synchronized void reset(ArenaMapCatalog.Arena arena) {
        if (arena == null) throw new IllegalArgumentException("door arena is null");
        reset(arena, arena.featurePoint());
    }

    synchronized void reset(ArenaMapCatalog.Arena arena,
                            ArenaMapCatalog.Point point) {
        if (arena == null || point == null
                || !arena.contains(point.x(), point.y())) {
            throw new IllegalArgumentException("door placement is outside arena");
        }
        x = point.x();
        y = point.y();
        active = true;
        open = false;
    }

    synchronized void deactivate() {
        active = false;
        open = false;
    }

    synchronized Snapshot snapshot() {
        return active ? new Snapshot(objectId, x, y,
                open ? ACTION_OPEN : ACTION_CLOSED, open, DIRECTION_DIAGONAL,
                open ? PASSABLE : NOT_PASSABLE) : null;
    }

    synchronized Snapshot toggle() {
        if (!active) return null;
        open = !open;
        return snapshot();
    }

    synchronized Snapshot open() {
        if (!active) return null;
        open = true;
        return snapshot();
    }

    synchronized boolean blocks(int targetX, int targetY) {
        return active && !open && targetX == x && targetY == y;
    }
}
