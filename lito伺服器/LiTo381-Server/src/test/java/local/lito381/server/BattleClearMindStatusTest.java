package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattleClearMindStatusTest {
    @Test void exactWisdomLeaseRefreshesAndExpiresAtRetainedBoundary() {
        BattleClearMindStatus status = new BattleClearMindStatus();
        long start = 1_000_000_000L;
        assertFalse(status.activate(start, 1_200, 3));
        assertEquals(new BattleClearMindStatus.Snapshot(3, 1_200),
                status.snapshot(start));

        long refresh = start + TimeUnit.SECONDS.toNanos(600);
        assertTrue(status.activate(refresh, 1_200, 3));
        assertFalse(status.expire(
                refresh + TimeUnit.SECONDS.toNanos(1_199)));
        assertEquals(3, status.snapshot(
                refresh + TimeUnit.SECONDS.toNanos(1_199)).wisdomModifier());
        assertTrue(status.expire(
                refresh + TimeUnit.SECONDS.toNanos(1_200)));
        assertEquals(new BattleClearMindStatus.Snapshot(0, 0),
                status.snapshot(refresh + TimeUnit.SECONDS.toNanos(1_200)));
    }

    @Test void cancellationAndClearAreIdempotent() {
        BattleClearMindStatus status = new BattleClearMindStatus();
        status.activate(0, 1_200, 3);
        assertTrue(status.cancel());
        assertFalse(status.cancel());
        status.activate(0, 1_200, 3);
        status.clear();
        assertFalse(status.snapshot(0).active());
    }

    @Test void invalidNativeLeaseInputsFailClosed() {
        BattleClearMindStatus status = new BattleClearMindStatus();
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(-1, 1_200, 3));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 0, 3));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 1_200, 0));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 1_200, 128));
        assertThrows(IllegalArgumentException.class,
                () -> status.snapshot(-1));
    }
}
