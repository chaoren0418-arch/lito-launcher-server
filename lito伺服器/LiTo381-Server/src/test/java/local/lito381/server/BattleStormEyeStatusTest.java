package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class BattleStormEyeStatusTest {
    @TempDir Path temporary;

    @Test void refreshReplacesDeadlineAndBothModifiersAreRangedOnly() {
        BattleStormEyeStatus status = new BattleStormEyeStatus();
        long start = 1_000_000_000L;
        assertFalse(status.activate(start, 960, 2, 3));
        assertEquals(new BattleStormEyeStatus.Snapshot(true, 2, 3, 960),
                status.snapshot(start));
        assertEquals(0, status.physicalHit(false, start));
        assertEquals(0, status.physicalDamage(false, start));
        assertEquals(2, status.physicalHit(true, start));
        assertEquals(3, status.physicalDamage(true, start));

        long refresh = start + TimeUnit.SECONDS.toNanos(480);
        assertTrue(status.activate(refresh, 960, 2, 3));
        assertFalse(status.expire(refresh + TimeUnit.SECONDS.toNanos(959)));
        assertTrue(status.expire(refresh + TimeUnit.SECONDS.toNanos(960)));
        assertEquals(BattleStormEyeStatus.Snapshot.none(),
                status.snapshot(refresh + TimeUnit.SECONDS.toNanos(960)));
    }

    @Test void cancellationAndClearAreIdempotent() {
        BattleStormEyeStatus status = new BattleStormEyeStatus();
        status.activate(0, 960, 2, 3);
        assertTrue(status.cancel());
        assertFalse(status.cancel());
        status.activate(0, 960, 2, 3);
        status.clear();
        assertFalse(status.snapshot(0).active());
    }

    @Test void retainedCharBuffRemainingTimeRoundTripsInSidecar()
            throws Exception {
        Path file = temporary.resolve("storm-eye.properties");
        GameStateStore state = new GameStateStore(file, true);
        state.saveTimedStormEyeBuff("PartyElf",
                new GameStateStore.TimedStormEyeBuff(731));
        assertEquals(731, new GameStateStore(file, true)
                .timedStormEyeBuff("PartyElf").remainingSeconds());

        state = new GameStateStore(file, true);
        state.saveTimedStormEyeBuff("PartyElf",
                GameStateStore.TimedStormEyeBuff.none());
        assertFalse(new GameStateStore(file, true)
                .timedStormEyeBuff("PartyElf").active());
    }

    @Test void invalidNativeLeaseAndDurableInputsFailClosed() {
        BattleStormEyeStatus status = new BattleStormEyeStatus();
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(-1, 960, 2, 3));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 0, 2, 3));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 960, 0, 3));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 960, 2, 0));
        assertThrows(IllegalArgumentException.class,
                () -> status.snapshot(-1));
        assertThrows(IllegalArgumentException.class,
                () -> new GameStateStore.TimedStormEyeBuff(961));
    }
}
