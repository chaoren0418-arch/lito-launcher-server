package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattleVenomEnchantStatusTest {
    @Test void retainedWeaponPoisonUsesAThirtySecondLease() {
        assertTrue(BattleVenomEnchantStatus.POISON_DURATION_SECONDS == 30);
        assertTrue(BattleVenomEnchantStatus.POISON_TICK_MILLIS == 3_000);
        assertTrue(BattleVenomEnchantStatus.POISON_TICK_DAMAGE == 5);
    }

    @Test void leaseRefreshesAndExpiresAtTheExactBoundary() {
        BattleVenomEnchantStatus status = new BattleVenomEnchantStatus();
        long start = 10L;
        status.activate(start, 320);
        assertTrue(status.active(start + TimeUnit.SECONDS.toNanos(320) - 1));
        assertTrue(status.expire(start + TimeUnit.SECONDS.toNanos(320)));
        assertFalse(status.active(start + TimeUnit.SECONDS.toNanos(320)));

        status.activate(start, 320);
        status.activate(start + 20, 320);
        assertTrue(status.active(start + TimeUnit.SECONDS.toNanos(320) + 19));
        assertTrue(status.cancel());
        assertFalse(status.cancel());
    }

    @Test void procRequiresAWeaponAndUsesTheRetainedInclusiveTenPercent() {
        assertTrue(BattleVenomEnchantStatus.triggers(true, true, 1));
        assertTrue(BattleVenomEnchantStatus.triggers(true, true, 10));
        assertFalse(BattleVenomEnchantStatus.triggers(true, true, 11));
        assertFalse(BattleVenomEnchantStatus.triggers(false, true, 1));
        assertFalse(BattleVenomEnchantStatus.triggers(true, false, 1));
        assertThrows(IllegalArgumentException.class,
                () -> BattleVenomEnchantStatus.triggers(true, true, 0));
        assertThrows(IllegalArgumentException.class,
                () -> BattleVenomEnchantStatus.triggers(true, true, 101));
    }
}
