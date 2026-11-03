package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class BattleWindShotStatusTest {
    @TempDir Path temporary;

    @Test void refreshDoesNotStackAndOnlyRangedPhysicalGetsHitBonus() {
        BattleWindShotStatus status = new BattleWindShotStatus();
        long start = 1_000_000_000L;
        assertFalse(status.activate(start, 960, 6));
        assertEquals(new BattleWindShotStatus.Snapshot(true, 6, 960),
                status.snapshot(start));
        assertEquals(0, status.physicalHit(false, start));
        assertEquals(6, status.physicalHit(true, start));

        long refresh = start + TimeUnit.SECONDS.toNanos(480);
        assertTrue(status.activate(refresh, 960, 6));
        assertEquals(6, status.physicalHit(true, refresh));
        assertFalse(status.expire(refresh + TimeUnit.SECONDS.toNanos(959)));
        assertTrue(status.expire(refresh + TimeUnit.SECONDS.toNanos(960)));
        assertEquals(BattleWindShotStatus.Snapshot.none(),
                status.snapshot(refresh + TimeUnit.SECONDS.toNanos(960)));
    }

    @Test void cancellationAndClearAreIdempotent() {
        BattleWindShotStatus status = new BattleWindShotStatus();
        status.activate(0, 960, 6);
        assertTrue(status.cancel());
        assertFalse(status.cancel());
        status.activate(0, 960, 6);
        status.clear();
        assertFalse(status.snapshot(0).active());
    }

    @Test void retainedCharBuffRemainingTimeRoundTripsInSidecar()
            throws Exception {
        Path file = temporary.resolve("wind-shot.properties");
        GameStateStore state = new GameStateStore(file, true);
        state.saveTimedWindShotBuff("WindElf",
                new GameStateStore.TimedWindShotBuff(731));
        assertEquals(731, new GameStateStore(file, true)
                .timedWindShotBuff("WindElf").remainingSeconds());

        state = new GameStateStore(file, true);
        state.saveTimedWindShotBuff("WindElf",
                GameStateStore.TimedWindShotBuff.none());
        assertFalse(new GameStateStore(file, true)
                .timedWindShotBuff("WindElf").active());
    }

    @Test void invalidNativeLeaseAndDurableInputsFailClosed() {
        BattleWindShotStatus status = new BattleWindShotStatus();
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(-1, 960, 6));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 0, 6));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 961, 0));
        assertThrows(IllegalArgumentException.class,
                () -> status.snapshot(-1));
        assertThrows(IllegalArgumentException.class,
                () -> new GameStateStore.TimedWindShotBuff(961));
    }
}
