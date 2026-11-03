package local.lito381.server;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Bounded retained-NpcMove path probe for ordinary WORLD actors.
 *
 * <p>The first pass includes live-object occupancy.  When that pass cannot
 * move, a second terrain-only pass distinguishes a transient crowd/door race
 * from a route that the retained collision map proves unreachable.  Callers
 * use that distinction for the source path-failure retry lifecycle.</p>
 */
final class ClassicWorldNpcPathfinder {
    static final int MAX_SEARCH_DISTANCE = 20;
    private static final int[] HEADING_X = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final int[] HEADING_Y = {-1, -1, 0, 1, 1, 1, 0, -1};
    /** Retained NpcMove._serchCource firstCource order. */
    private static final int[] SEARCH_ORDER = {2, 4, 6, 0, 1, 3, 5, 7};

    enum Failure {
        NONE,
        TRANSIENT,
        UNREACHABLE
    }

    record Result(int destinationX, int destinationY, int heading,
                  Failure failure) {
        Result {
            if (failure == null
                    || failure == Failure.NONE
                        != (heading >= 0 && heading <= 7)) {
                throw new IllegalArgumentException("invalid NPC path result");
            }
        }

        boolean moved() {
            return failure == Failure.NONE;
        }

        static Result transientFailure() {
            return new Result(0, 0, -1, Failure.TRANSIENT);
        }

        static Result unreachable() {
            return new Result(0, 0, -1, Failure.UNREACHABLE);
        }
    }

    @FunctionalInterface
    interface Occupancy {
        boolean blocked(int x, int y);
    }

    interface Terrain {
        boolean contains(int x, int y);

        boolean canStep(int fromX, int fromY, int toX, int toY);

        boolean attackPosition(
                int fromX, int fromY, int targetX, int targetY, int range);
    }

    private record Node(int x, int y, int depth, int firstHeading) {}

    private ClassicWorldNpcPathfinder() {}

    static Result nextStep(
            ClassicWorldMapCatalog.ClassicMap map,
            int sourceX, int sourceY, int targetX, int targetY,
            int attackRange, Occupancy occupancy) {
        if (map == null) throw new IllegalArgumentException("missing WORLD map");
        return nextStep(new Terrain() {
            @Override
            public boolean contains(int x, int y) {
                return map.contains(x, y);
            }

            @Override
            public boolean canStep(int fromX, int fromY, int toX, int toY) {
                return map.canStep(fromX, fromY, toX, toY);
            }

            @Override
            public boolean attackPosition(
                    int fromX, int fromY, int goalX, int goalY, int range) {
                return map.attackPosition(fromX, fromY, goalX, goalY, range);
            }
        }, sourceX, sourceY, targetX, targetY, attackRange, occupancy);
    }

    static Result nextStep(
            Terrain terrain,
            int sourceX, int sourceY, int targetX, int targetY,
            int attackRange, Occupancy occupancy) {
        if (terrain == null || occupancy == null || attackRange < 0) {
            throw new IllegalArgumentException("invalid NPC path request");
        }
        if (!terrain.contains(sourceX, sourceY)
                || !terrain.contains(targetX, targetY)
                || Math.max(Math.abs(sourceX - targetX),
                        Math.abs(sourceY - targetY)) > 30) {
            return Result.unreachable();
        }
        if (terrain.attackPosition(
                sourceX, sourceY, targetX, targetY, attackRange)) {
            return Result.transientFailure();
        }

        Result livePath = search(terrain, sourceX, sourceY, targetX, targetY,
                attackRange, occupancy, true);
        if (livePath != null) return livePath;

        Result staticPath = search(terrain, sourceX, sourceY, targetX, targetY,
                attackRange, (x, y) -> false, false);
        return staticPath == null
                ? Result.unreachable() : Result.transientFailure();
    }

    private static Result search(
            Terrain terrain,
            int sourceX, int sourceY, int targetX, int targetY,
            int attackRange, Occupancy occupancy, boolean liveObjects) {
        ArrayDeque<Node> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        visited.add(key(sourceX, sourceY));
        queue.addLast(new Node(sourceX, sourceY, 0, -1));

        while (!queue.isEmpty()) {
            Node current = queue.removeFirst();
            if (current.depth() >= MAX_SEARCH_DISTANCE) continue;
            for (int heading : SEARCH_ORDER) {
                int nextX = current.x() + HEADING_X[heading];
                int nextY = current.y() + HEADING_Y[heading];
                long key = key(nextX, nextY);
                if (!visited.add(key)
                        || !terrain.contains(nextX, nextY)
                        || !terrain.canStep(
                                current.x(), current.y(), nextX, nextY)
                        || nextX == targetX && nextY == targetY
                        || liveObjects && occupancy.blocked(nextX, nextY)) {
                    continue;
                }
                int firstHeading = current.firstHeading() >= 0
                        ? current.firstHeading() : heading;
                if (terrain.attackPosition(
                        nextX, nextY, targetX, targetY, attackRange)) {
                    return new Result(
                            sourceX + HEADING_X[firstHeading],
                            sourceY + HEADING_Y[firstHeading],
                            firstHeading, Failure.NONE);
                }
                queue.addLast(new Node(
                        nextX, nextY, current.depth() + 1, firstHeading));
            }
        }
        return null;
    }

    private static long key(int x, int y) {
        return (Integer.toUnsignedLong(x) << 32) | Integer.toUnsignedLong(y);
    }
}
