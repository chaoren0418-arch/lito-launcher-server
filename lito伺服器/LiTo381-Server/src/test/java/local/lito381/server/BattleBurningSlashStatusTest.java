package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattleBurningSlashStatusTest {
    @Test void refreshesAndConsumesExactlyOnceBeforeExpiry() {
        BattleBurningSlashStatus status = new BattleBurningSlashStatus();
        long start = 1_000_000_000L;
        assertFalse(status.activate(start, 60));
        assertEquals(new BattleBurningSlashStatus.Snapshot(true, 60),
                status.snapshot(start));
        assertTrue(status.activate(start + TimeUnit.SECONDS.toNanos(20), 60));
        assertTrue(status.consume(start + TimeUnit.SECONDS.toNanos(21)));
        assertFalse(status.consume(start + TimeUnit.SECONDS.toNanos(22)));
        assertEquals(BattleBurningSlashStatus.Snapshot.none(),
                status.snapshot(start + TimeUnit.SECONDS.toNanos(22)));
    }

    @Test void expirationCancellationAndInvalidInputsFailClosed() {
        BattleBurningSlashStatus status = new BattleBurningSlashStatus();
        status.activate(0, 60);
        assertFalse(status.expire(TimeUnit.SECONDS.toNanos(59)));
        assertTrue(status.expire(TimeUnit.SECONDS.toNanos(60)));
        assertFalse(status.cancel());
        status.activate(0, 60);
        assertTrue(status.cancel());
        assertFalse(status.cancel());
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(-1, 60));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> status.consume(-1));
    }
}
