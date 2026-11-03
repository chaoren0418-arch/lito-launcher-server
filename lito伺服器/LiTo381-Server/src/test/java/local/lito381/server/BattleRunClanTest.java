package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class BattleRunClanTest {
    @Test
    void retainedDestinationUsesExactAllyTileAndFixedHeadingFive() {
        assertEquals(new BattleRunClan.Destination(564, 32724, 32809, 5),
                BattleRunClan.exactDestination(564, 32724, 32809));
    }

    @Test
    void invalidWorldCoordinatesFailClosed() {
        assertThrows(IllegalArgumentException.class,
                () -> BattleRunClan.exactDestination(-1, 32724, 32809));
        assertThrows(IllegalArgumentException.class,
                () -> BattleRunClan.exactDestination(564, 0, 32809));
        assertThrows(IllegalArgumentException.class,
                () -> BattleRunClan.exactDestination(564, 32724, 0));
    }
}
