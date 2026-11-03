package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattleJoyOfPainTest {
    @Test void refreshCreatesNewGenerationAndOldHitCannotConsumeIt() {
        BattleJoyOfPainStatus status = new BattleJoyOfPainStatus();
        BattleJoyOfPainStatus.Activation first = status.activateOrRefresh(0, 2);
        BattleJoyOfPainStatus.Lease oldHit = status.capture(1);
        BattleJoyOfPainStatus.Activation refresh =
                status.activateOrRefresh(2, 2);

        assertTrue(first.firstApplication());
        assertTrue(refresh.refreshed());
        assertFalse(status.consume(oldHit));
        BattleJoyOfPainStatus.Lease current = status.capture(3);
        assertNotNull(current);
        assertTrue(status.consume(current));
        assertNull(status.capture(4));
    }

    @Test void expiryAndCancellationAreAuthoritative() {
        BattleJoyOfPainStatus status = new BattleJoyOfPainStatus();
        status.activateOrRefresh(0, 2);
        assertEquals(2, status.snapshot(1).remainingSeconds());
        assertNull(status.capture(TimeUnit.SECONDS.toNanos(2)));
        assertTrue(status.expire(TimeUnit.SECONDS.toNanos(2)));
        status.activateOrRefresh(TimeUnit.SECONDS.toNanos(3), 2);
        assertTrue(status.cancel());
        assertFalse(status.cancel());
    }

    @Test void retainedFormulaUsesPreHitMissingHpEquipmentAndCap() {
        assertEquals(0, BattleJoyOfPain.feedbackDamage(100, 100, 0));
        assertEquals(13, BattleJoyOfPain.feedbackDamage(100, 60, 0));
        assertEquals(10, BattleJoyOfPain.feedbackDamage(100, 40, 30));
        assertEquals(0, BattleJoyOfPain.feedbackDamage(100, 90, 30));
        assertEquals(1_500,
                BattleJoyOfPain.feedbackDamage(10_000, 0, 0));
    }
}
