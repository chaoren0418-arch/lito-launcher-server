package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BattleManaDrainTest {
    @Test
    void npcEligibilityComesOnlyFromTheSelectedIsEraseColumn() {
        SourceNpcPhysicalStats ordinary = new SourceNpcPhysicalStats(
                45262, -4, true, 0, true, 0, 8, 0,
                false, -22, false, false, 10, 16, 15, true);
        SourceNpcPhysicalStats boss = new SourceNpcPhysicalStats(
                45601, -75, false, 0, true, 3, 2, 150,
                false, -120, false, true, 250, 77, 17, false);
        assertTrue(BattleManaDrain.eligibleNpc(ordinary));
        assertFalse(BattleManaDrain.eligibleNpc(boss));
        assertFalse(BattleManaDrain.eligibleNpc(null));
    }

    @Test
    void genericProbabilityUsesWizardDiceMagicHitMrCapAndBossDivisor() {
        int ordinary = BattleManaDrain.probability(
                1, 47, 8, 1, 3, 15, 12, -1, sides -> 6);
        assertEquals(50, ordinary);
        assertEquals(10, BattleManaDrain.probability(
                1, 47, 8, 1, 3, 15, 12, 5, sides -> 6));
        assertEquals(0, BattleManaDrain.probability(
                1, 47, 8, 1, 3, 15, 12, 0, sides -> 6));
        assertTrue(BattleManaDrain.succeeds(50, 50));
        assertFalse(BattleManaDrain.succeeds(50, 51));
    }

    @Test
    void requestedTransferIsRetainedD10PlusFourPlusHalfIntelligence() {
        assertEquals(9, BattleManaDrain.requestedDrain(8, sides -> 1));
        assertEquals(18, BattleManaDrain.requestedDrain(8, sides -> 10));
        assertThrows(IllegalArgumentException.class,
                () -> BattleManaDrain.requestedDrain(8, sides -> 0));
    }
}
