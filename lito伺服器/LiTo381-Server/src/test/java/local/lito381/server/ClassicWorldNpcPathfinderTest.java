package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class ClassicWorldNpcPathfinderTest {
    @Test
    void openTerrainReturnsRetainedEastFirstStep() {
        Grid terrain = new Grid(8, 8);
        ClassicWorldNpcPathfinder.Result result =
                ClassicWorldNpcPathfinder.nextStep(
                        terrain, 1, 3, 6, 3, 1, (x, y) -> false);

        assertTrue(result.moved());
        assertEquals(2, result.destinationX());
        assertEquals(3, result.destinationY());
        assertEquals(2, result.heading());
    }

    @Test
    void staticObstacleUsesBoundedBreadthFirstDetour() {
        Grid terrain = new Grid(8, 8);
        terrain.block(2, 3);
        ClassicWorldNpcPathfinder.Result result =
                ClassicWorldNpcPathfinder.nextStep(
                        terrain, 1, 3, 6, 3, 1, (x, y) -> false);

        assertTrue(result.moved());
        assertFalse(result.destinationX() == 2 && result.destinationY() == 3);
        assertTrue(terrain.canStep(
                1, 3, result.destinationX(), result.destinationY()));
    }

    @Test
    void liveOccupancyIsTransientWhenStaticRouteStillExists() {
        Grid terrain = new Grid(8, 8);
        Set<Long> occupied = new HashSet<>();
        for (int x = 0; x <= 2; x++) {
            for (int y = 2; y <= 4; y++) {
                if (x != 1 || y != 3) occupied.add(key(x, y));
            }
        }
        ClassicWorldNpcPathfinder.Result result =
                ClassicWorldNpcPathfinder.nextStep(
                        terrain, 1, 3, 6, 3, 1,
                        (x, y) -> occupied.contains(key(x, y)));

        assertFalse(result.moved());
        assertEquals(ClassicWorldNpcPathfinder.Failure.TRANSIENT,
                result.failure());
    }

    @Test
    void completeStaticBarrierIsProvenUnreachable() {
        Grid terrain = new Grid(8, 8);
        for (int y = 0; y < 8; y++) terrain.block(3, y);
        ClassicWorldNpcPathfinder.Result result =
                ClassicWorldNpcPathfinder.nextStep(
                        terrain, 1, 3, 6, 3, 1, (x, y) -> false);

        assertFalse(result.moved());
        assertEquals(ClassicWorldNpcPathfinder.Failure.UNREACHABLE,
                result.failure());
    }

    private static long key(int x, int y) {
        return (Integer.toUnsignedLong(x) << 32) | Integer.toUnsignedLong(y);
    }

    private static final class Grid
            implements ClassicWorldNpcPathfinder.Terrain {
        private final int width;
        private final int height;
        private final Set<Long> blocked = new HashSet<>();

        Grid(int width, int height) {
            this.width = width;
            this.height = height;
        }

        void block(int x, int y) {
            blocked.add(key(x, y));
        }

        @Override
        public boolean contains(int x, int y) {
            return x >= 0 && y >= 0 && x < width && y < height;
        }

        @Override
        public boolean canStep(int fromX, int fromY, int toX, int toY) {
            return contains(fromX, fromY) && contains(toX, toY)
                    && Math.max(Math.abs(fromX - toX),
                            Math.abs(fromY - toY)) == 1
                    && !blocked.contains(key(toX, toY));
        }

        @Override
        public boolean attackPosition(
                int fromX, int fromY, int targetX, int targetY, int range) {
            return Math.max(Math.abs(fromX - targetX),
                    Math.abs(fromY - targetY)) <= Math.max(1, range);
        }
    }
}
