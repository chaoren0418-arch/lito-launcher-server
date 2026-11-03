package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class BattleIronSkinStatusTest {
    @TempDir Path temporary;

    @Test void refreshesWithoutStackingAcAndExpiresAt960Seconds() {
        BattleIronSkinStatus status = new BattleIronSkinStatus();
        long start = 1_000_000_000L;
        assertFalse(status.activate(start, 960, -10));
        assertEquals(new BattleIronSkinStatus.Snapshot(true, -10, 960),
                status.snapshot(start));
        long refresh = start + TimeUnit.SECONDS.toNanos(480);
        assertTrue(status.activate(refresh, 960, -10));
        assertEquals(-10, status.armorClass(refresh));
        assertFalse(status.expire(refresh + TimeUnit.SECONDS.toNanos(959)));
        assertTrue(status.expire(refresh + TimeUnit.SECONDS.toNanos(960)));
        assertEquals(BattleIronSkinStatus.Snapshot.none(),
                status.snapshot(refresh + TimeUnit.SECONDS.toNanos(960)));
    }

    @Test void cancellationAndInputsFailClosed() {
        BattleIronSkinStatus status = new BattleIronSkinStatus();
        status.activate(0, 960, -10);
        assertTrue(status.cancel());
        assertFalse(status.cancel());
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(-1, 960, -10));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 0, -10));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 960, 0));
    }

    @Test void retainedCharBuffRemainingTimeRoundTripsInSidecar()
            throws Exception {
        Path file = temporary.resolve("iron-skin.properties");
        GameStateStore state = new GameStateStore(file, true);
        state.saveTimedIronSkinBuff("IronElf",
                new GameStateStore.TimedIronSkinBuff(731));
        assertEquals(731, new GameStateStore(file, true)
                .timedIronSkinBuff("IronElf").remainingSeconds());
        state.saveTimedIronSkinBuff("IronElf",
                GameStateStore.TimedIronSkinBuff.none());
        assertFalse(new GameStateStore(file, true)
                .timedIronSkinBuff("IronElf").active());
    }
}
