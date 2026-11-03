package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BattlePhysicalProcBuffTest {
    @Test void burningSpiritUsesInclusiveThirtyThreePercentAndOnePointFive() {
        BattlePhysicalProcBuff.Outcome triggered =
                BattlePhysicalProcBuff.burningSpirit(true, true, 33);
        BattlePhysicalProcBuff.Outcome missed =
                BattlePhysicalProcBuff.burningSpirit(true, true, 34);
        assertTrue(triggered.triggered());
        assertEquals(33, triggered.chance());
        assertEquals(1.5D, triggered.multiplier());
        assertFalse(missed.triggered());
        assertEquals(1.0D, missed.multiplier());
    }

    @Test void excludedAndInactiveAttacksConsumeNoProcRoll() {
        BattlePhysicalProcBuff.Outcome inactive =
                BattlePhysicalProcBuff.burningSpirit(false, true, 0);
        BattlePhysicalProcBuff.Outcome ranged =
                BattlePhysicalProcBuff.burningSpirit(true, false, 0);
        assertFalse(inactive.active());
        assertTrue(ranged.active());
        assertFalse(ranged.eligible());
        assertFalse(ranged.triggered());
        assertThrows(IllegalArgumentException.class,
                () -> BattlePhysicalProcBuff.burningSpirit(false, true, 1));
        assertThrows(IllegalArgumentException.class,
                () -> BattlePhysicalProcBuff.burningSpirit(true, true, 0));
    }

    @Test void elementalFireUsesInclusiveSharedThirtyThreePercentMeleeProc() {
        BattlePhysicalProcBuff.Outcome triggered =
                BattlePhysicalProcBuff.elementalFire(true, true, 33);
        BattlePhysicalProcBuff.Outcome missed =
                BattlePhysicalProcBuff.elementalFire(true, true, 34);
        assertTrue(triggered.active());
        assertTrue(triggered.eligible());
        assertTrue(triggered.triggered());
        assertEquals(33, triggered.chance());
        assertEquals(1.5D, triggered.multiplier());
        assertFalse(missed.triggered());
        assertEquals(1.0D, missed.multiplier());
        assertFalse(BattlePhysicalProcBuff.elementalFire(
                false, true, 0).active());
        assertFalse(BattlePhysicalProcBuff.elementalFire(
                true, false, 0).eligible());
        assertThrows(IllegalArgumentException.class,
                () -> BattlePhysicalProcBuff.elementalFire(false, true, 1));
        assertThrows(IllegalArgumentException.class,
                () -> BattlePhysicalProcBuff.elementalFire(true, true, 0));
    }

    @Test void doubleBrakeIsInclusiveFifteenPercentForClawAndEdoryuOnly() {
        BattlePhysicalProcBuff.Outcome claw =
                BattlePhysicalProcBuff.doubleBrake(true, 11, true, 15);
        BattlePhysicalProcBuff.Outcome edoryuMiss =
                BattlePhysicalProcBuff.doubleBrake(true, 12, true, 16);
        assertTrue(claw.triggered());
        assertEquals(15, claw.chance());
        assertEquals(1.5D, claw.multiplier());
        assertFalse(edoryuMiss.triggered());
        assertEquals(1.0D, edoryuMiss.multiplier());

        for (int weaponType : new int[] {0, 1, 4, 10, 13, 17}) {
            BattlePhysicalProcBuff.Outcome excluded =
                    BattlePhysicalProcBuff.doubleBrake(true, weaponType, true, 0);
            assertTrue(excluded.active());
            assertFalse(excluded.eligible());
            assertFalse(excluded.triggered());
        }
        assertFalse(BattlePhysicalProcBuff.doubleBrake(
                false, 11, true, 0).active());
        assertFalse(BattlePhysicalProcBuff.doubleBrake(
                true, 11, false, 0).eligible());
        assertThrows(IllegalArgumentException.class,
                () -> BattlePhysicalProcBuff.doubleBrake(false, 11, true, 1));
        assertThrows(IllegalArgumentException.class,
                () -> BattlePhysicalProcBuff.doubleBrake(true, 11, true, 0));
    }
}
