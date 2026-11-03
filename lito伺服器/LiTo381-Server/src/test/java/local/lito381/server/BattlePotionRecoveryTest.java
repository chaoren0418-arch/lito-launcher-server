package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class BattlePotionRecoveryTest {
    @Test
    void appliesRetainedUserAddHpModifierOrder() {
        assertEquals(150, BattlePotionRecovery.adjustedAmount(
                100, 20, 30, false, false, false));
        assertEquals(75, BattlePotionRecovery.adjustedAmount(
                100, 20, 30, true, false, false));
        assertEquals(37, BattlePotionRecovery.adjustedAmount(
                100, 20, 30, true, true, false));
        assertEquals(-37, BattlePotionRecovery.adjustedAmount(
                100, 20, 30, true, true, true));
    }

    @Test
    void ignoresNonpositiveBonusesExactlyLikeRetainedExecutor() {
        assertEquals(11, BattlePotionRecovery.adjustedAmount(
                11, 0, 0, false, false, false));
        assertEquals(11, BattlePotionRecovery.adjustedAmount(
                11, -10, -20, false, false, false));
    }

    @Test
    void sourceEquipmentVitalStatsKeepsItemAndSetRecoverySeparate() {
        SourceEquipmentVitalStats stats = new SourceEquipmentVitalStats(
                1, 2, 3, 4, 5, 6, 17, 19);
        assertEquals(36, stats.totalPotionRecovery());
        assertEquals(0, SourceEquipmentVitalStats.none().totalPotionRecovery());
    }

    @Test
    void rejectsInvalidOrOverflowingRecoveryInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> BattlePotionRecovery.adjustedAmount(
                        -1, 0, 0, false, false, false));
        assertThrows(ArithmeticException.class,
                () -> BattlePotionRecovery.adjustedAmount(
                        Integer.MAX_VALUE, 100, 0, false, false, false));
    }
}
