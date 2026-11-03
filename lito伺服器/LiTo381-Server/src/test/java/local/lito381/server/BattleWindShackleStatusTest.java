package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattleWindShackleStatusTest {
    @Test
    void startsOnceDoesNotRefreshAndThrottlesAttackOrSpellByTwentyFivePercent() {
        long start = TimeUnit.SECONDS.toNanos(10);

        BattleWindShackleStatus status = new BattleWindShackleStatus();
        assertTrue(status.activate(start, 16));
        assertFalse(status.activate(start + TimeUnit.SECONDS.toNanos(1), 16));
        assertTrue(status.tryAction(start, 600));
        assertFalse(status.tryAction(start + TimeUnit.MILLISECONDS.toNanos(749), 600));
        assertTrue(status.tryAction(start + TimeUnit.MILLISECONDS.toNanos(750), 600));

        BattleWindShackleStatus.Snapshot snapshot = status.snapshot(
                start + TimeUnit.SECONDS.toNanos(1));
        assertTrue(snapshot.active());
        assertEquals(15, snapshot.remainingSeconds());
        assertEquals(start + TimeUnit.SECONDS.toNanos(16), snapshot.expiresAtNanos());
    }

    @Test
    void expiresAndCancellationClearTheActionClock() {
        BattleWindShackleStatus status = new BattleWindShackleStatus();
        assertTrue(status.activate(0, 16));
        assertTrue(status.tryAction(0, 800));
        assertTrue(status.cancel());
        assertTrue(status.tryAction(1, 800));
        assertFalse(status.snapshot(1).active());

        assertTrue(status.activate(2, 1));
        assertTrue(status.expire(TimeUnit.SECONDS.toNanos(1) + 2));
        assertFalse(status.snapshot(TimeUnit.SECONDS.toNanos(1) + 2).active());
    }

    @Test
    void exactIntegerActionMultiplierMatchesRetainedCalculator() {
        assertEquals(750, BattleWindShackleStatus.actionIntervalMillis(600));
        assertEquals(1_000, BattleWindShackleStatus.actionIntervalMillis(800));
    }
}
