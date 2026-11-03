package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattleResistElementalStatusTest {
    @Test void exactFourElementLeaseRefreshesAndExpiresAtRetainedBoundary() {
        BattleResistElementalStatus status = new BattleResistElementalStatus();
        long start = 1_000_000_000L;
        assertFalse(status.activate(start, 1_200, 10));
        assertEquals(new BattleResistElementalStatus.Snapshot(10, 1_200),
                status.snapshot(start));
        assertEquals(10, status.modifier(start));

        long refresh = start + TimeUnit.SECONDS.toNanos(600);
        assertTrue(status.activate(refresh, 1_200, 10));
        assertFalse(status.expire(
                refresh + TimeUnit.SECONDS.toNanos(1_199)));
        assertEquals(10, status.modifier(
                refresh + TimeUnit.SECONDS.toNanos(1_199)));
        assertTrue(status.expire(
                refresh + TimeUnit.SECONDS.toNanos(1_200)));
        assertEquals(new BattleResistElementalStatus.Snapshot(0, 0),
                status.snapshot(refresh + TimeUnit.SECONDS.toNanos(1_200)));
    }

    @Test void cancellationAndClearAreIdempotent() {
        BattleResistElementalStatus status = new BattleResistElementalStatus();
        status.activate(0, 1_200, 10);
        assertTrue(status.cancel());
        assertFalse(status.cancel());
        status.activate(0, 1_200, 10);
        status.clear();
        assertFalse(status.snapshot(0).active());
    }

    @Test void invalidNativeLeaseInputsFailClosed() {
        BattleResistElementalStatus status = new BattleResistElementalStatus();
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(-1, 1_200, 10));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 0, 10));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 1_200, 0));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 1_200, 128));
        assertThrows(IllegalArgumentException.class,
                () -> status.snapshot(-1));
    }
}
