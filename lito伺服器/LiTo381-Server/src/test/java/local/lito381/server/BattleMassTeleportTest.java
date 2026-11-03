package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

final class BattleMassTeleportTest {
    @Test void randomDestinationStaysInsideClampedStandingArena() {
        ArenaMapCatalog.Arena arena = new ArenaMapCatalog.Arena(
                999, 0, 10, 0, 10, Set.of(),
                List.of(new ArenaMapCatalog.Point(5, 5)),
                new ArenaMapCatalog.Point(5, 5));

        assertEquals(new ArenaMapCatalog.Point(0, 0),
                BattleMassTeleport.randomDestination(arena, 5, 5, bound -> 0));
        assertEquals(new ArenaMapCatalog.Point(10, 10),
                BattleMassTeleport.randomDestination(arena, 5, 5, bound -> bound - 1));
        assertThrows(IllegalArgumentException.class,
                () -> BattleMassTeleport.randomDestination(
                        arena, 5, 5, bound -> bound));
    }

    @Test void inviteRangeUsesRetainedChebyshevTileLineDistance() {
        assertTrue(BattleMassTeleport.withinInviteRange(100, 100, 103, 103));
        assertTrue(BattleMassTeleport.withinInviteRange(100, 100, 97, 101));
        assertFalse(BattleMassTeleport.withinInviteRange(100, 100, 104, 100));
        assertFalse(BattleMassTeleport.withinInviteRange(100, 100, 100, 96));
    }

    @Test void classicWorldRandomDestinationUsesRetainedMapBoundsAndStandingBits() {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(4);
        int x = ClassicWorldMapCatalog.WORLD_TREE_X;
        int y = ClassicWorldMapCatalog.WORLD_TREE_Y;
        ArenaMapCatalog.Point destination = BattleMassTeleport.randomDestination(
                map, x, y, bound -> bound / 2);
        assertTrue(map.contains(destination.x(), destination.y()));
        assertTrue(map.standingPassable(destination.x(), destination.y()));
        assertTrue(Math.abs(destination.x() - x) <= BattleMassTeleport.RANDOM_RANGE);
        assertTrue(Math.abs(destination.y() - y) <= BattleMassTeleport.RANDOM_RANGE);
        assertThrows(IllegalArgumentException.class,
                () -> BattleMassTeleport.randomDestination(
                        map, x, y, bound -> bound));
    }

    @Test void selectedWorldBookmarkFlagIsDistinctFromEscapeAndTeleportFlags() {
        assertTrue(ClassicWorldMapCatalog.forMap(4).allowsBookmark());
        assertFalse(ClassicWorldMapCatalog.forMap(3).allowsBookmark());
        assertTrue(ClassicWorldMapCatalog.forMap(3).allowsEscape());
        assertTrue(ClassicWorldMapCatalog.forMap(3).allowsRandomTeleport());
    }
}
