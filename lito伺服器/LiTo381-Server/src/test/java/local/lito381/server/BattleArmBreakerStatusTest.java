package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattleArmBreakerStatusTest {
    @Test void exactLeaseIsMeleeOnlyNonRefreshingCancelableAndExpiring() {
        BattleArmBreakerStatus status = new BattleArmBreakerStatus();
        long start = 1_000_000L;
        BattleArmBreakerStatus.Activation first =
                status.activateIfAbsent(start, 12);
        assertTrue(first.accepted());
        assertFalse(first.alreadyActive());
        assertEquals(-2, status.meleeDamageModifier(start));

        long recast = start + TimeUnit.SECONDS.toNanos(5);
        BattleArmBreakerStatus.Activation second =
                status.activateIfAbsent(recast, 12);
        assertTrue(second.accepted());
        assertTrue(second.alreadyActive());
        assertEquals(first.expiresAtNanos(), second.expiresAtNanos(),
                "native if-absent status must not refresh on a damaging recast");
        assertEquals(-2, status.meleeDamageModifier(
                start + TimeUnit.SECONDS.toNanos(11)));
        assertEquals(0, status.meleeDamageModifier(
                start + TimeUnit.SECONDS.toNanos(12)));

        status.activateIfAbsent(recast, 12);
        assertTrue(status.cancel());
        assertFalse(status.cancel());
        assertEquals(0, status.meleeDamageModifier(recast));
    }

    @Test void rejectsInvalidClockAndNonNativeDuration() {
        BattleArmBreakerStatus status = new BattleArmBreakerStatus();
        assertFalse(status.activateIfAbsent(-1, 12).accepted());
        assertFalse(status.activateIfAbsent(1, 11).accepted());
        assertFalse(status.activateIfAbsent(1, 13).accepted());
    }
}
