package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattlePanicStatusTest {
    @Test void leaseDoesNotStackOrRefreshAndExpiresExactly() {
        BattlePanicStatus status = new BattlePanicStatus();
        long now = 1_000_000L;
        assertTrue(status.activateIfAbsent(now, 64, -1));
        assertEquals(-1, status.snapshot(now).abilityScoreModifier());
        assertEquals(64, status.snapshot(now).remainingSeconds());
        assertFalse(status.activateIfAbsent(now + TimeUnit.SECONDS.toNanos(10),
                64, -1));
        assertEquals(54, status.snapshot(
                now + TimeUnit.SECONDS.toNanos(10)).remainingSeconds());
        assertFalse(status.expire(now + TimeUnit.SECONDS.toNanos(63)));
        assertTrue(status.expire(now + TimeUnit.SECONDS.toNanos(64)));
        assertEquals(0, status.snapshot(
                now + TimeUnit.SECONDS.toNanos(64)).abilityScoreModifier());
    }

    @Test void cancellationReportsOnlyAnActiveLease() {
        BattlePanicStatus status = new BattlePanicStatus();
        assertFalse(status.cancel());
        assertTrue(status.activateIfAbsent(10, 64, -1));
        assertTrue(status.cancel());
        assertFalse(status.cancel());
    }
}
