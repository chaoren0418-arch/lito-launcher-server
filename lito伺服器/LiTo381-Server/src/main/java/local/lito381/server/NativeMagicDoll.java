package local.lito381.server;

import java.util.concurrent.atomic.AtomicLong;

/** One retained {@code L1DollInstance} projected into the LiTo world. */
record NativeMagicDoll(
        long objectId, long backingItemObjectId, SourceMagicDollType type,
        int x, int y, int mapId, long instanceId, int heading,
        long expiresAtNanos) {
    private static final long FIRST_OBJECT_ID = 0x7A10_0000L;
    private static final long LAST_OBJECT_ID = 0x7A1F_FFFFL;
    private static final AtomicLong NEXT_OBJECT_ID = new AtomicLong(FIRST_OBJECT_ID);

    NativeMagicDoll {
        if (objectId <= 0 || objectId > 0xFFFF_FFFFL
                || backingItemObjectId <= 0 || type == null
                || x < 0 || x > 0xFFFF || y < 0 || y > 0xFFFF
                || mapId < 0 || mapId > 0xFFFF || instanceId < -1
                || heading < 0 || heading > 7 || expiresAtNanos <= 0) {
            throw new IllegalArgumentException("invalid native magic doll instance");
        }
    }

    static NativeMagicDoll summon(
            long backingItemObjectId, SourceMagicDollType type,
            int ownerX, int ownerY, int mapId, long instanceId, long nowNanos) {
        int offsetX = java.util.concurrent.ThreadLocalRandom.current().nextInt(-2, 3);
        int offsetY = java.util.concurrent.ThreadLocalRandom.current().nextInt(-2, 3);
        int x = Math.max(0, Math.min(0xFFFF, ownerX + offsetX));
        int y = Math.max(0, Math.min(0xFFFF, ownerY + offsetY));
        return new NativeMagicDoll(nextObjectId(), backingItemObjectId, type,
                x, y, mapId, instanceId, 5,
                Math.addExact(nowNanos,
                        Math.multiplyExact(type.durationSeconds(), 1_000_000_000L)));
    }

    int remainingSeconds(long nowNanos) {
        if (nowNanos >= expiresAtNanos) return 0;
        long remaining = expiresAtNanos - nowNanos;
        return (int) Math.min(0xFFFFL,
                Math.max(1L, (remaining + 999_999_999L) / 1_000_000_000L));
    }

    private static long nextObjectId() {
        while (true) {
            long candidate = NEXT_OBJECT_ID.getAndUpdate(value ->
                    value >= LAST_OBJECT_ID ? FIRST_OBJECT_ID : value + 1);
            if (candidate >= FIRST_OBJECT_ID && candidate <= LAST_OBJECT_ID) {
                return candidate;
            }
        }
    }
}
