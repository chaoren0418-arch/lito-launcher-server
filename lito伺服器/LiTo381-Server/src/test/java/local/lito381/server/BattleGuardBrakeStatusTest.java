package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattleGuardBrakeStatusTest {
    @Test void activatesRefreshesExpiresAndCancelsExactAcPenalty() {
        BattleGuardBrakeStatus status = new BattleGuardBrakeStatus();
        long start = 1_000_000_000L;
        assertFalse(status.activate(start, 60, 15));
        assertEquals(new BattleGuardBrakeStatus.Snapshot(true, 15, 60),
                status.snapshot(start));
        assertEquals(15, status.armorClass(start));
        assertTrue(status.activate(
                start + TimeUnit.SECONDS.toNanos(20), 60, 15));
        assertFalse(status.expire(start + TimeUnit.SECONDS.toNanos(79)));
        assertTrue(status.expire(start + TimeUnit.SECONDS.toNanos(80)));
        assertEquals(BattleGuardBrakeStatus.Snapshot.none(),
                status.snapshot(start + TimeUnit.SECONDS.toNanos(80)));
        status.activate(start, 60, 15);
        assertTrue(status.cancel());
        assertFalse(status.cancel());
    }

    @Test void invalidLeaseInputsFailClosed() {
        BattleGuardBrakeStatus status = new BattleGuardBrakeStatus();
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(-1, 60, 15));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 0, 15));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 60, 0));
        assertThrows(IllegalArgumentException.class,
                () -> status.snapshot(-1));
    }
}
