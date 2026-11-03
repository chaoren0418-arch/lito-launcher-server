package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BattleNpcPhysicalAttackTest {
    @Test void retainedNpcEnvelopeAppliesTargetDodgeBeforeAcComparison() {
        assertEquals(100, BattleNpcPhysicalAttack.hitChance(
                1, 0, 10, 0, 5, 0));
        assertEquals(15, BattleNpcPhysicalAttack.hitChance(
                1, 0, 10, 5, 5, 0));
        assertTrue(BattleNpcPhysicalAttack.hits(100, 16));
        assertFalse(BattleNpcPhysicalAttack.hits(15, 16));
    }

    @Test void resistFearDodgeDownIndependentlyRaisesNpcAttackDice() {
        assertEquals(15, BattleNpcPhysicalAttack.hitChance(
                1, 0, 10, 5, 0, 5, 0));
        assertEquals(100, BattleNpcPhysicalAttack.hitChance(
                1, 0, 10, 5, 5, 5, 0));
    }

    @Test void negativeAcAndInputBoundsRemainExact() {
        assertEquals(10, BattleNpcPhysicalAttack.negativeAcRollMaximum(-10));
        assertEquals(0, BattleNpcPhysicalAttack.negativeAcRollMaximum(0));
        assertEquals(15, BattleNpcPhysicalAttack.hitChance(
                1, 0, -10, 5, 5, 10));
        assertThrows(IllegalArgumentException.class,
                () -> BattleNpcPhysicalAttack.hitChance(
                        1, 0, -10, 5, 5, 0));
        assertThrows(IllegalArgumentException.class,
                () -> BattleNpcPhysicalAttack.hitChance(
                        1, 0, 10, 11, 5, 0));
    }
}
