package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class BattleStormShotStatusTest {
    @TempDir Path temporary;

    @Test void refreshAndExpiryKeepNativeRangedOnlySignedModifiers() {
        BattleStormShotStatus status = new BattleStormShotStatus();
        long start = 1_000_000_000L;
        assertFalse(status.activate(start, 960, -1, 5));
        assertEquals(new BattleStormShotStatus.Snapshot(true, -1, 5, 960),
                status.snapshot(start));
        assertEquals(0, status.physicalHit(false, start));
        assertEquals(0, status.physicalDamage(false, start));
        assertEquals(-1, status.physicalHit(true, start));
        assertEquals(5, status.physicalDamage(true, start));

        long refresh = start + TimeUnit.SECONDS.toNanos(480);
        assertTrue(status.activate(refresh, 960, -1, 5));
        assertFalse(status.expire(refresh + TimeUnit.SECONDS.toNanos(959)));
        assertTrue(status.expire(refresh + TimeUnit.SECONDS.toNanos(960)));
        assertEquals(BattleStormShotStatus.Snapshot.none(),
                status.snapshot(refresh + TimeUnit.SECONDS.toNanos(960)));
    }

    @Test void cancellationAndClearAreIdempotent() {
        BattleStormShotStatus status = new BattleStormShotStatus();
        status.activate(0, 960, -1, 5);
        assertTrue(status.cancel());
        assertFalse(status.cancel());
        status.activate(0, 960, -1, 5);
        status.clear();
        assertEquals(BattleStormShotStatus.Snapshot.none(), status.snapshot(0));
    }

    @Test void retainedCharBuffRemainingTimeRoundTripsInSidecar()
            throws Exception {
        Path file = temporary.resolve("storm-shot.properties");
        GameStateStore state = new GameStateStore(file, true);
        state.saveTimedStormShotBuff("WindElf",
                new GameStateStore.TimedStormShotBuff(731));
        assertEquals(731, new GameStateStore(file, true)
                .timedStormShotBuff("WindElf").remainingSeconds());

        state = new GameStateStore(file, true);
        state.saveTimedStormShotBuff("WindElf",
                GameStateStore.TimedStormShotBuff.none());
        assertFalse(new GameStateStore(file, true)
                .timedStormShotBuff("WindElf").active());
    }

    @Test void invalidNativeLeaseAndDurableInputsFailClosed() {
        BattleStormShotStatus status = new BattleStormShotStatus();
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(-1, 960, -1, 5));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 0, -1, 5));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 960, 0, 5));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 960, -1, 4));
        assertThrows(IllegalArgumentException.class, () -> status.snapshot(-1));
        assertThrows(IllegalArgumentException.class,
                () -> new GameStateStore.TimedStormShotBuff(961));
    }
}
