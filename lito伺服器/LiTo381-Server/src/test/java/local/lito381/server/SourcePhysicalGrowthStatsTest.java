package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

final class SourcePhysicalGrowthStatsTest {
    @Test void reproducesTheQualifiedSourceTableBoundaries() {
        SourcePhysicalGrowthStats stats = SourcePhysicalGrowthStats.classic381();
        assertEquals(-5, stats.damage(false, 1));
        assertEquals(0, stats.damage(false, 11));
        assertEquals(5, stats.damage(false, 21));
        assertEquals(6, stats.damage(false, 23));
        assertEquals(10, stats.damage(false, 33));
        assertEquals(34, stats.damage(false, 127));

        assertEquals(0, stats.damage(true, 14));
        assertEquals(1, stats.damage(true, 15));
        assertEquals(4, stats.damage(true, 20));
        assertEquals(5, stats.damage(true, 24));
        assertEquals(32, stats.damage(true, 127));
        assertEquals(40, stats.hit(false, 127));
        assertEquals(50, stats.hit(true, 127));
    }

    @Test void clampsLiveStatsButRejectsIncompleteSourceTables() {
        SourcePhysicalGrowthStats stats = SourcePhysicalGrowthStats.classic381();
        assertEquals(stats.damage(false, 1), stats.damage(false, -20));
        assertEquals(stats.damage(true, 127), stats.damage(true, 254));
        assertThrows(IllegalArgumentException.class,
                () -> new SourcePhysicalGrowthStats(
                        List.of(1), List.of(1), List.of(1), List.of(1)));
    }
}
