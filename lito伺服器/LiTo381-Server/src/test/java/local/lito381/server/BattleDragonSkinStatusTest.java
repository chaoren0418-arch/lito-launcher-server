package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattleDragonSkinStatusTest {
    @Test void refreshesWithoutStackingAndReducesOnlyThroughExplicitPhysicalHook() {
        BattleDragonSkinStatus status = new BattleDragonSkinStatus();
        long start = 1_000_000_000L;
        assertFalse(status.activate(start, 1_200, 10));
        BattleDragonSkinStatus.Snapshot active = status.snapshot(start);
        assertEquals(new BattleDragonSkinStatus.Snapshot(true, 10, 1_200), active);
        assertEquals(30, active.applyTo(40));
        assertEquals(0, active.applyTo(7));

        long refresh = start + TimeUnit.SECONDS.toNanos(600);
        assertTrue(status.activate(refresh, 1_200, 10));
        assertEquals(10, status.snapshot(refresh).damageReduction());
        assertFalse(status.expire(refresh + TimeUnit.SECONDS.toNanos(1_199)));
        assertTrue(status.expire(refresh + TimeUnit.SECONDS.toNanos(1_200)));
        assertEquals(BattleDragonSkinStatus.Snapshot.none(),
                status.snapshot(refresh + TimeUnit.SECONDS.toNanos(1_200)));
    }

    @Test void cancellationAndInvalidInputsFailClosed() {
        BattleDragonSkinStatus status = new BattleDragonSkinStatus();
        status.activate(0, 1_200, 10);
        assertTrue(status.cancel());
        assertFalse(status.cancel());
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(-1, 1_200, 10));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 0, 10));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 1_200, 0));
        assertThrows(IllegalArgumentException.class,
                () -> BattleDragonSkinStatus.Snapshot.none().applyTo(-1));
    }
}
