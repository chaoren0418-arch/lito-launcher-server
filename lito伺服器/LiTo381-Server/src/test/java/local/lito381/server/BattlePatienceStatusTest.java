package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattlePatienceStatusTest {
    @Test void exactOriginalReductionRefreshCancellationAndExpiry() {
        BattlePatienceStatus status = new BattlePatienceStatus();
        long start = 1_000_000L;
        BattlePatienceStatus.Activation first = status.activate(
                0x3810_2110L, start, 600);
        assertTrue(first.accepted());
        assertFalse(first.refreshed());
        assertEquals(2, status.physicalReduction(
                0x3810_2110L, start));

        long refresh = start + TimeUnit.SECONDS.toNanos(300);
        BattlePatienceStatus.Activation second = status.activate(
                0x3810_2110L, refresh, 600);
        assertTrue(second.accepted());
        assertTrue(second.refreshed());
        assertEquals(refresh + TimeUnit.SECONDS.toNanos(600),
                second.expiresAtNanos());
        assertEquals(2, status.physicalReduction(
                0x3810_2110L,
                refresh + TimeUnit.SECONDS.toNanos(599)));
        assertEquals(0, status.physicalReduction(
                0x3810_2110L,
                refresh + TimeUnit.SECONDS.toNanos(600)));

        status.activate(0x3810_2110L, refresh, 600);
        assertTrue(status.cancel(0x3810_2110L));
        assertFalse(status.cancel(0x3810_2110L));
        assertEquals(0, status.activeCount(refresh));
    }

    @Test void rejectsInvalidIdentityClockAndNonNativeDuration() {
        BattlePatienceStatus status = new BattlePatienceStatus();
        assertFalse(status.activate(0, 1, 600).accepted());
        assertFalse(status.activate(1, -1, 600).accepted());
        assertFalse(status.activate(1, 1, 599).accepted());
    }
}
