package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class BattleCallClanTest {
    @Test
    void everyCapturedHeadingUsesTheRetainedAdjacentTileAndFacesBack() {
        int[][] expected = {
                {100, 99, 4}, {101, 99, 5}, {101, 100, 6}, {101, 101, 7},
                {100, 101, 0}, {99, 101, 1}, {99, 100, 2}, {99, 99, 3}
        };
        for (int heading = 0; heading < 8; heading++) {
            BattleCallClan.Destination destination =
                    BattleCallClan.adjacentDestination(
                            554, 100, 100, heading,
                            (x, y) -> true, (x, y) -> false);
            assertEquals(new BattleCallClan.Destination(
                    554, expected[heading][0], expected[heading][1],
                    expected[heading][2]), destination);
        }
        assertEquals(new BattleCallClan.Destination(554, 101, 100, 6),
                BattleCallClan.adjacentDestination(
                        554, 100, 100, 10,
                        (x, y) -> true, (x, y) -> false));
    }

    @Test
    void blockedOrOccupiedDestinationFailsClosed() {
        assertNull(BattleCallClan.adjacentDestination(
                554, 100, 100, 2,
                (x, y) -> false, (x, y) -> false));
        assertNull(BattleCallClan.adjacentDestination(
                554, 100, 100, 2,
                (x, y) -> true, (x, y) -> true));
        assertThrows(IllegalArgumentException.class,
                () -> BattleCallClan.adjacentDestination(
                        -1, 100, 100, 2,
                        (x, y) -> true, (x, y) -> false));
    }
}
