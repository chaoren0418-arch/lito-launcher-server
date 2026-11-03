package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class BattleResistFearStatusTest {
    @TempDir Path temporary;

    @Test void activatesWithoutRefreshThenExpiresAndCancelsExactDodgeDown() {
        BattleResistFearStatus status = new BattleResistFearStatus();
        long start = 1_000_000_000L;
        assertFalse(status.activate(start, 60, 5));
        assertEquals(new BattleResistFearStatus.Snapshot(true, 5, 60),
                status.snapshot(start));
        assertEquals(5, status.dodgeDown(start));
        assertTrue(status.activate(
                start + TimeUnit.SECONDS.toNanos(20), 60, 5));
        assertFalse(status.expire(start + TimeUnit.SECONDS.toNanos(59)));
        assertTrue(status.expire(start + TimeUnit.SECONDS.toNanos(60)));
        assertEquals(BattleResistFearStatus.Snapshot.none(),
                status.snapshot(start + TimeUnit.SECONDS.toNanos(60)));
        status.activate(start, 60, 5);
        assertTrue(status.cancel());
        assertFalse(status.cancel());
    }

    @Test void invalidLeaseInputsFailClosed() {
        BattleResistFearStatus status = new BattleResistFearStatus();
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(-1, 60, 5));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 0, 5));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 60, 0));
        assertThrows(IllegalArgumentException.class,
                () -> status.snapshot(-1));
    }

    @Test void retainedCharBuffRemainingTimeRoundTripsInSidecar()
            throws Exception {
        Path file = temporary.resolve("resist-fear.properties");
        GameStateStore state = new GameStateStore(file, true);
        state.saveTimedResistFearBuff("FearTarget",
                new GameStateStore.TimedResistFearBuff(47));
        assertEquals(47, new GameStateStore(file, true)
                .timedResistFearBuff("FearTarget").remainingSeconds());
        state.saveTimedResistFearBuff("FearTarget",
                GameStateStore.TimedResistFearBuff.none());
        assertFalse(new GameStateStore(file, true)
                .timedResistFearBuff("FearTarget").active());
    }
}
