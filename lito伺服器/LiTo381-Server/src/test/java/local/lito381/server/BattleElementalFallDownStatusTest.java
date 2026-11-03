package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BattleElementalFallDownStatusTest {
    @Test void activationUsesLowestElfAttributeBitAndRecastOnlyRefreshes() {
        BattleElementalFallDownStatus status =
                new BattleElementalFallDownStatus();
        long start = 1_000_000_000L;

        BattleElementalFallDownStatus.Activation first =
                status.activate(start, 32, 0b1010);
        assertTrue(first.applied());
        assertFalse(first.refreshed());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_FIRE, first.attributeKind());
        assertEquals(-50, status.modifier(
                BattleSpellCatalog.ATTRIBUTE_FIRE, start));
        assertEquals(0, status.modifier(
                BattleSpellCatalog.ATTRIBUTE_WIND, start));

        BattleElementalFallDownStatus.Activation refresh =
                status.activate(start + 10_000_000_000L, 32,
                        BattleSpellCatalog.ATTRIBUTE_EARTH);
        assertFalse(refresh.applied());
        assertTrue(refresh.refreshed());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_FIRE,
                refresh.attributeKind());
        assertEquals(-50, status.modifier(
                BattleSpellCatalog.ATTRIBUTE_FIRE,
                start + 41_000_000_000L));
        assertFalse(status.expire(start + 41_000_000_000L));
        assertTrue(status.expire(start + 42_000_000_000L));
        assertEquals(0, status.modifier(
                BattleSpellCatalog.ATTRIBUTE_FIRE,
                start + 42_000_000_000L));
    }

    @Test void noAttributeStillCreatesRefreshableLeaseAndCancellationRestores() {
        BattleElementalFallDownStatus status =
                new BattleElementalFallDownStatus();
        long start = 5_000_000_000L;
        assertEquals(0, status.activate(start, 32, 0).attributeKind());
        BattleElementalFallDownStatus.Snapshot snapshot = status.snapshot(start);
        assertTrue(snapshot.active());
        assertEquals(0, snapshot.modifier());

        BattleElementalFallDownStatus.Cancellation cancellation = status.cancel();
        assertTrue(cancellation.present());
        assertEquals(0, cancellation.attributeKind());
        assertFalse(status.snapshot(start).active());
        assertFalse(status.cancel().present());
    }

    @Test void allRetainedAttributeBitsAreStable() {
        assertEquals(0, BattleElementalFallDownStatus.retainedAttribute(0));
        assertEquals(1, BattleElementalFallDownStatus.retainedAttribute(1));
        assertEquals(2, BattleElementalFallDownStatus.retainedAttribute(2));
        assertEquals(4, BattleElementalFallDownStatus.retainedAttribute(4));
        assertEquals(8, BattleElementalFallDownStatus.retainedAttribute(8));
        assertEquals(1, BattleElementalFallDownStatus.retainedAttribute(15));
    }
}
