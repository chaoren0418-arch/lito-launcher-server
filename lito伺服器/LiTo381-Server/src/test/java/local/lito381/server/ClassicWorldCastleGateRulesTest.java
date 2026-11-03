package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ClassicWorldCastleGateRulesTest {
    @Test
    void sourceRuleBlocksOnlyDestructibleDoorsDuringWar() {
        assertFalse(ClassicWorldCastleGateRules.blocksToggle(false, 10_000));
        assertFalse(ClassicWorldCastleGateRules.blocksToggle(true, 0));
        assertFalse(ClassicWorldCastleGateRules.blocksToggle(true, 1));
        assertTrue(ClassicWorldCastleGateRules.blocksToggle(true, 3_000));
        assertThrows(IllegalArgumentException.class,
                () -> ClassicWorldCastleGateRules.blocksToggle(true, -1));
    }

    @Test
    void retainedKeepersUseTheirExactSourceDoorMaximumHp() {
        assertFalse(ClassicWorldCastleGateRules.blocksKeeperToggle(5, 70863, true));
        assertTrue(ClassicWorldCastleGateRules.blocksKeeperToggle(5, 70992, true));
        assertTrue(ClassicWorldCastleGateRules.blocksKeeperToggle(5, 70862, true));

        assertFalse(ClassicWorldCastleGateRules.blocksKeeperToggle(6, 70995, true));
        assertTrue(ClassicWorldCastleGateRules.blocksKeeperToggle(6, 70994, true));
        assertTrue(ClassicWorldCastleGateRules.blocksKeeperToggle(6, 70993, true));

        assertFalse(ClassicWorldCastleGateRules.blocksKeeperToggle(7, 70996, true));
        assertFalse(ClassicWorldCastleGateRules.blocksKeeperToggle(5, 70992, false));
        assertThrows(IllegalArgumentException.class,
                () -> ClassicWorldCastleGateRules.blocksKeeperToggle(5, 12345, true));
    }
}
