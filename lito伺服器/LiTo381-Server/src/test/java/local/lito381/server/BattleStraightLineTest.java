package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

final class BattleStraightLineTest {
    @Test void horizontalLineIncludesCasterIntermediateAndPrimaryCellsOnly() {
        List<ArenaMapCatalog.Point> points = BattleStraightLine.points(10, 20, 14, 20);
        assertEquals(List.of(
                new ArenaMapCatalog.Point(10, 20),
                new ArenaMapCatalog.Point(11, 20),
                new ArenaMapCatalog.Point(12, 20),
                new ArenaMapCatalog.Point(13, 20),
                new ArenaMapCatalog.Point(14, 20)), points);
        assertTrue(BattleStraightLine.contains(points, 12, 20));
        assertFalse(BattleStraightLine.contains(points, 15, 20));
        assertFalse(BattleStraightLine.contains(points, 12, 21));
    }

    @Test void sourceTieBreakingMatchesClassicCreateLineMap() {
        assertEquals(List.of(
                new ArenaMapCatalog.Point(3, 7),
                new ArenaMapCatalog.Point(4, 8),
                new ArenaMapCatalog.Point(5, 8),
                new ArenaMapCatalog.Point(6, 9),
                new ArenaMapCatalog.Point(7, 9)),
                BattleStraightLine.points(3, 7, 7, 9));
        assertEquals(List.of(
                new ArenaMapCatalog.Point(7, 9),
                new ArenaMapCatalog.Point(6, 8),
                new ArenaMapCatalog.Point(5, 8),
                new ArenaMapCatalog.Point(4, 7),
                new ArenaMapCatalog.Point(3, 7)),
                BattleStraightLine.points(7, 9, 3, 7));
    }

    @Test void verticalAndSingleCellLinesRemainInclusive() {
        assertEquals(List.of(
                new ArenaMapCatalog.Point(2, 3),
                new ArenaMapCatalog.Point(2, 4),
                new ArenaMapCatalog.Point(2, 5)),
                BattleStraightLine.points(2, 3, 2, 5));
        assertEquals(List.of(new ArenaMapCatalog.Point(9, 9)),
                BattleStraightLine.points(9, 9, 9, 9));
    }
}
