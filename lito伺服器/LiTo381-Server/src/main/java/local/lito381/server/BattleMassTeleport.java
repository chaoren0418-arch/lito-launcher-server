package local.lito381.server;

import java.util.function.IntUnaryOperator;

/** Server-authoritative destination and invite geometry for Mass Teleport. */
final class BattleMassTeleport {
    static final int RANDOM_RANGE = 200;
    static final int MAX_RANDOM_ATTEMPTS = 40;
    static final int INVITE_RANGE = 3;

    private BattleMassTeleport() {}

    static ArenaMapCatalog.Point randomDestination(
            ArenaMapCatalog.Arena arena, int originX, int originY,
            IntUnaryOperator nextInt) {
        if (arena == null || nextInt == null || !arena.contains(originX, originY)) {
            throw new IllegalArgumentException("invalid mass-teleport random source");
        }
        int minX = Math.max(arena.minX(), originX - RANDOM_RANGE);
        int maxX = Math.min(arena.maxX(), originX + RANDOM_RANGE);
        int minY = Math.max(arena.minY(), originY - RANDOM_RANGE);
        int maxY = Math.min(arena.maxY(), originY + RANDOM_RANGE);
        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        for (int attempt = 0; attempt < MAX_RANDOM_ATTEMPTS; attempt++) {
            int x = minX + checkedRoll(nextInt, width);
            int y = minY + checkedRoll(nextInt, height);
            if (arena.nativeStandingFloor(x, y)) {
                return new ArenaMapCatalog.Point(x, y);
            }
        }
        if (arena.nativeStandingFloor(originX, originY)) {
            return new ArenaMapCatalog.Point(originX, originY);
        }
        return arena.spawns().stream()
                .filter(point -> arena.nativeStandingFloor(point.x(), point.y()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                "arena has no standing mass-teleport fallback"));
    }

    static ArenaMapCatalog.Point randomDestination(
            ClassicWorldMapCatalog.ClassicMap map, int originX, int originY,
            IntUnaryOperator nextInt) {
        if (map == null || nextInt == null || !map.contains(originX, originY)) {
            throw new IllegalArgumentException(
                    "invalid classic-world mass-teleport random source");
        }
        int minX = Math.max(map.startX(), originX - RANDOM_RANGE);
        int maxX = Math.min(map.endX(), originX + RANDOM_RANGE);
        int minY = Math.max(map.startY(), originY - RANDOM_RANGE);
        int maxY = Math.min(map.endY(), originY + RANDOM_RANGE);
        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        for (int attempt = 0; attempt < MAX_RANDOM_ATTEMPTS; attempt++) {
            int x = minX + checkedRoll(nextInt, width);
            int y = minY + checkedRoll(nextInt, height);
            if (map.standingPassable(x, y)) {
                return new ArenaMapCatalog.Point(x, y);
            }
        }
        if (map.standingPassable(originX, originY)) {
            return new ArenaMapCatalog.Point(originX, originY);
        }
        throw new IllegalStateException(
                "classic-world map has no sampled standing mass-teleport fallback");
    }

    static boolean withinInviteRange(int sourceX, int sourceY, int targetX, int targetY) {
        return Math.max(Math.abs(sourceX - targetX), Math.abs(sourceY - targetY))
                <= INVITE_RANGE;
    }

    private static int checkedRoll(IntUnaryOperator nextInt, int bound) {
        int value = nextInt.applyAsInt(bound);
        if (value < 0 || value >= bound) {
            throw new IllegalArgumentException("mass-teleport random roll outside bound");
        }
        return value;
    }
}
