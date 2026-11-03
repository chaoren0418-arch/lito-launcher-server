package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattleCounterMirrorStatusTest {
    @Test void retainedZeroBasedWisdomRollTriggersAtEqualityAndConsumesLease() {
        BattleCounterMirrorStatus status = new BattleCounterMirrorStatus();
        long start = 1_000_000_000L;
        status.activate(start, 16);

        BattleCounterMirrorStatus.Decision failed = status.resolve(
                start, 12, 13, 41, true);
        assertTrue(failed.active());
        assertTrue(failed.eligible());
        assertFalse(failed.triggered());
        assertEquals(0, failed.reflectedDamage());
        assertTrue(status.active(start));

        BattleCounterMirrorStatus.Decision equality = status.resolve(
                start, 12, 12, 41, true);
        assertTrue(equality.triggered());
        assertEquals(12, equality.wisdom());
        assertEquals(12, equality.roll());
        assertEquals(41, equality.reflectedDamage());
        assertFalse(status.active(start));
    }

    @Test void ineligibleNpcZeroDamageAndFailedRollNeverConsumeLease() {
        BattleCounterMirrorStatus status = new BattleCounterMirrorStatus();
        long start = 3_000_000_000L;
        status.activate(start, 16);

        BattleCounterMirrorStatus.Decision ineligible = status.resolve(
                start, 255, 0, 99, false);
        assertTrue(ineligible.active());
        assertFalse(ineligible.eligible());
        assertFalse(ineligible.triggered());
        assertTrue(status.active(start));

        assertFalse(status.resolve(start, 255, 0, 0, true).triggered());
        assertTrue(status.active(start));
        assertFalse(status.resolve(start, 0, 99, 99, true).triggered());
        assertTrue(status.active(start));
    }

    @Test void recastRefreshesExpiryAndCancellationIsOneShot() {
        BattleCounterMirrorStatus status = new BattleCounterMirrorStatus();
        long start = 5_000_000_000L;
        status.activate(start, 16);
        status.activate(start + TimeUnit.SECONDS.toNanos(10), 16);

        assertFalse(status.expire(start + TimeUnit.SECONDS.toNanos(25)));
        assertTrue(status.active(start + TimeUnit.SECONDS.toNanos(25)));
        assertTrue(status.expire(start + TimeUnit.SECONDS.toNanos(26)));
        assertFalse(status.active(start + TimeUnit.SECONDS.toNanos(26)));

        status.activate(start, 16);
        assertTrue(status.cancel());
        assertFalse(status.cancel());
        assertFalse(status.active(start));
    }

    @Test void invalidNativeClockRollDamageAndDurationAreRejected() {
        BattleCounterMirrorStatus status = new BattleCounterMirrorStatus();
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(-1, 16));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> status.resolve(0, 256, 0, 1, true));
        assertThrows(IllegalArgumentException.class,
                () -> status.resolve(0, 1, 100, 1, true));
        assertThrows(IllegalArgumentException.class,
                () -> status.resolve(0, 1, 0, -1, true));
    }
}
