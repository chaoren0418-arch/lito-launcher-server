package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BattleFoeSlayerSequenceTest {
    @Test void reusesConsumedExposureBonusAcrossExactlyThreeStrikes() {
        BattleFoeSlayerSequence sequence = new BattleFoeSlayerSequence();
        BattleFoeSlayerSequence.Start start = sequence.begin(100, 3, 60, 3);
        assertEquals(3, start.exposureLevel());
        assertEquals(60, start.damageBonus());

        for (int index = 1; index <= 3; index++) {
            BattleFoeSlayerSequence.Strike strike = sequence.next(100);
            assertTrue(strike.active());
            assertEquals(index, strike.strikeIndex());
            assertEquals(3, strike.totalStrikes());
            assertEquals(3, strike.exposureLevel());
            assertEquals(60, strike.damageBonus());
            assertEquals(index == 3, strike.finalStrike());
        }
        assertFalse(sequence.next(100).active());
        assertEquals(0, sequence.snapshot().remainingStrikes());
        assertEquals(0, sequence.snapshot().targetObjectId());
    }

    @Test void zeroExposureStillSuppressesWeaknessForAllThreeOnActionCalls() {
        BattleFoeSlayerSequence sequence = new BattleFoeSlayerSequence();
        sequence.begin(200, 0, 0, 3);
        assertTrue(sequence.next(200).active());
        assertTrue(sequence.next(200).active());
        assertTrue(sequence.next(200).active());
        assertFalse(sequence.next(200).active());
    }

    @Test void mismatchedPhysicalTargetTerminatesTheLease() {
        BattleFoeSlayerSequence sequence = new BattleFoeSlayerSequence();
        sequence.begin(300, 2, 40, 3);
        BattleFoeSlayerSequence.Strike mismatch = sequence.next(301);
        assertFalse(mismatch.active());
        assertTrue(mismatch.targetMismatch());
        assertTrue(mismatch.finalStrike());
        assertEquals(0, sequence.snapshot().remainingStrikes());
    }
}
