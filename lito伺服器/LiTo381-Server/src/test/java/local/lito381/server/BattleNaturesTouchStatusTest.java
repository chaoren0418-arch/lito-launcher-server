package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattleNaturesTouchStatusTest {
    @Test void refreshesWithoutStackingAndExpiresAt320Seconds() {
        BattleNaturesTouchStatus status = new BattleNaturesTouchStatus();
        long start = 1_000_000_000L;
        assertFalse(status.activate(start, 320));
        assertEquals(new BattleNaturesTouchStatus.Snapshot(true, 15, 320),
                status.snapshot(start));
        assertEquals(15, status.hpRegen(start));

        long refresh = start + TimeUnit.SECONDS.toNanos(160);
        assertTrue(status.activate(refresh, 320));
        assertEquals(15, status.hpRegen(refresh));
        assertFalse(status.expire(refresh + TimeUnit.SECONDS.toNanos(319)));
        assertTrue(status.expire(refresh + TimeUnit.SECONDS.toNanos(320)));
        assertEquals(BattleNaturesTouchStatus.Snapshot.none(),
                status.snapshot(refresh + TimeUnit.SECONDS.toNanos(320)));
    }

    @Test void cancellationIsIdempotentAndInputsFailClosed() {
        BattleNaturesTouchStatus status = new BattleNaturesTouchStatus();
        status.activate(0, 320);
        assertTrue(status.cancel());
        assertFalse(status.cancel());
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(-1, 320));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> status.snapshot(-1));
    }

    @Test void applyingNatureTouchCanConsumeWaterLifeLease() {
        BattleWaterLifeStatus waterLife = new BattleWaterLifeStatus();
        assertFalse(waterLife.activate(0, 64));
        assertTrue(waterLife.active(0));
        assertTrue(waterLife.cancel());
        assertFalse(waterLife.active(0));
        assertFalse(waterLife.cancel());
    }
}
