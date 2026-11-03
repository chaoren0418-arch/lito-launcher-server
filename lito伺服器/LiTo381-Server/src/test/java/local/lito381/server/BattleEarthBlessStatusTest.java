package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class BattleEarthBlessStatusTest {
    @TempDir Path temporary;

    @Test void refreshesWithoutStackingAcAndExpiresAt960Seconds() {
        BattleEarthBlessStatus status = new BattleEarthBlessStatus();
        long start = 1_000_000_000L;
        assertFalse(status.activate(start, 960, -7));
        assertEquals(new BattleEarthBlessStatus.Snapshot(true, -7, 960),
                status.snapshot(start));
        long refresh = start + TimeUnit.SECONDS.toNanos(480);
        assertTrue(status.activate(refresh, 960, -7));
        assertEquals(-7, status.armorClass(refresh));
        assertFalse(status.expire(refresh + TimeUnit.SECONDS.toNanos(959)));
        assertTrue(status.expire(refresh + TimeUnit.SECONDS.toNanos(960)));
        assertEquals(BattleEarthBlessStatus.Snapshot.none(),
                status.snapshot(refresh + TimeUnit.SECONDS.toNanos(960)));
    }

    @Test void cancellationAndInputsFailClosed() {
        BattleEarthBlessStatus status = new BattleEarthBlessStatus();
        status.activate(0, 960, -7);
        assertTrue(status.cancel());
        assertFalse(status.cancel());
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(-1, 960, -7));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 0, -7));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 960, 0));
    }

    @Test void retainedCharBuffRemainingTimeRoundTripsInSidecar()
            throws Exception {
        Path file = temporary.resolve("earth-bless.properties");
        GameStateStore state = new GameStateStore(file, true);
        state.saveTimedEarthBlessBuff("EarthElf",
                new GameStateStore.TimedEarthBlessBuff(731));
        assertEquals(731, new GameStateStore(file, true)
                .timedEarthBlessBuff("EarthElf").remainingSeconds());
        state.saveTimedEarthBlessBuff("EarthElf",
                GameStateStore.TimedEarthBlessBuff.none());
        assertFalse(new GameStateStore(file, true)
                .timedEarthBlessBuff("EarthElf").active());
    }
}
