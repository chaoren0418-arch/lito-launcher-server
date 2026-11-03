package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattleElementalProtectionStatusTest {
    @Test void castTimeMaskSupportsDualAttributesWithoutStackingOnRefresh() {
        BattleElementalProtectionStatus status =
                new BattleElementalProtectionStatus();
        long start = 1_000_000_000L;
        int earthAndFire = BattleSpellCatalog.ATTRIBUTE_EARTH
                | BattleSpellCatalog.ATTRIBUTE_FIRE;
        assertFalse(status.activate(start, 64, earthAndFire, 50));
        assertEquals(50, status.modifier(
                BattleSpellCatalog.ATTRIBUTE_EARTH, start));
        assertEquals(50, status.modifier(
                BattleSpellCatalog.ATTRIBUTE_FIRE, start));
        assertEquals(0, status.modifier(
                BattleSpellCatalog.ATTRIBUTE_WATER, start));
        assertEquals(new BattleElementalProtectionStatus.Snapshot(
                        true, earthAndFire, 50, 64),
                status.snapshot(start));

        long refresh = start + TimeUnit.SECONDS.toNanos(32);
        assertTrue(status.activate(refresh, 64, earthAndFire, 50));
        assertEquals(50, status.modifier(
                BattleSpellCatalog.ATTRIBUTE_EARTH, refresh));
        assertFalse(status.expire(
                refresh + TimeUnit.SECONDS.toNanos(63)));
        assertTrue(status.expire(
                refresh + TimeUnit.SECONDS.toNanos(64)));
        assertEquals(new BattleElementalProtectionStatus.Snapshot(
                        false, 0, 0, 0),
                status.snapshot(refresh + TimeUnit.SECONDS.toNanos(64)));
    }

    @Test void cancellationReturnsTheCastTimeMaskAndIsIdempotent() {
        BattleElementalProtectionStatus status =
                new BattleElementalProtectionStatus();
        status.activate(0, 64, BattleSpellCatalog.ATTRIBUTE_WATER, 50);
        assertEquals(new BattleElementalProtectionStatus.Cancellation(
                        true, BattleSpellCatalog.ATTRIBUTE_WATER),
                status.cancel());
        assertEquals(new BattleElementalProtectionStatus.Cancellation(false, 0),
                status.cancel());
        status.activate(0, 64, BattleSpellCatalog.ATTRIBUTE_WIND, 50);
        status.clear();
        assertFalse(status.snapshot(0).active());
    }

    @Test void invalidNativeLeaseInputsFailClosed() {
        BattleElementalProtectionStatus status =
                new BattleElementalProtectionStatus();
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(-1, 64, 1, 50));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 0, 1, 50));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 64, 0, 50));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 64, 0x10, 50));
        assertThrows(IllegalArgumentException.class,
                () -> status.activate(0, 64, 1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> status.snapshot(-1));
    }
}
