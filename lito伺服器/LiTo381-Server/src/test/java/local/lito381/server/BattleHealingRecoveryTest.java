package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class BattleHealingRecoveryTest {
    @Test
    void preservesRetainedNegativeDamageShiftRounding() {
        assertEquals(3, BattleHealingRecovery.adjustedAmount(
                5, true, false, false));
        assertEquals(2, BattleHealingRecovery.adjustedAmount(
                5, true, true, false));
        assertEquals(-2, BattleHealingRecovery.adjustedAmount(
                5, true, true, true));
    }

    @Test
    void leavesOrdinaryHealingAndZeroUnchanged() {
        assertEquals(100, BattleHealingRecovery.adjustedAmount(
                100, false, false, false));
        assertEquals(0, BattleHealingRecovery.adjustedAmount(
                0, true, true, true));
    }

    @Test
    void rejectsNegativeHealing() {
        assertThrows(IllegalArgumentException.class,
                () -> BattleHealingRecovery.adjustedAmount(
                        -1, false, false, false));
    }

    @Test
    void appliesWaterLifeBeforeRetainedRecoveryShifts() {
        assertEquals(10, BattleHealingRecovery.adjustedSingleTargetAmount(
                5, true, false, false, false));
        assertEquals(5, BattleHealingRecovery.adjustedSingleTargetAmount(
                5, true, true, false, false));
        // Retained arithmetic shifts negative damage: -5 >> 1 is -3.
        assertEquals(3, BattleHealingRecovery.adjustedSingleTargetAmount(
                5, true, true, true, false));
        assertEquals(-3, BattleHealingRecovery.adjustedSingleTargetAmount(
                5, true, true, true, true));
    }

    @Test
    void singleTargetWaterLifeRejectsNegativeAndOverflowingInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> BattleHealingRecovery.adjustedSingleTargetAmount(
                        -1, true, false, false, false));
        assertThrows(ArithmeticException.class,
                () -> BattleHealingRecovery.adjustedSingleTargetAmount(
                        Integer.MAX_VALUE, true, false, false, false));
    }
}
