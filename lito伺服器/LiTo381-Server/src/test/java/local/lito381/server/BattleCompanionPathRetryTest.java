package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BattleCompanionPathRetryTest {
    @Test
    void transientAndSameMasterCrowdingNeverAdvanceGiveUpCounter() {
        BattleCompanion companion = companion();
        assertTrue(companion.selectTarget(99));

        assertFalse(companion.retryPath(
                1_000_000_000L,
                ClassicWorldNpcPathfinder.Failure.TRANSIENT, false));
        assertEquals(0, companion.pathFailureCount());
        assertFalse(companion.ready(1_199_999_999L));
        assertTrue(companion.ready(1_200_000_000L));

        assertFalse(companion.retryPath(
                1_200_000_000L,
                ClassicWorldNpcPathfinder.Failure.UNREACHABLE, true));
        assertEquals(0, companion.pathFailureCount());
        assertEquals(99, companion.snapshot().targetObjectId());
        assertFalse(companion.ready(1_799_999_999L));
        assertTrue(companion.ready(1_800_000_000L));
    }

    @Test
    void twentiethProvenUnreachableAttemptClearsCompanionTarget() {
        BattleCompanion companion = companion();
        assertTrue(companion.selectTarget(99));
        long now = 2_000_000_000L;
        for (int attempt = 1;
             attempt < BattleCompanion.PATH_FAIL_GIVE_UP; attempt++) {
            assertFalse(companion.retryPath(
                    now, ClassicWorldNpcPathfinder.Failure.UNREACHABLE, false));
            assertEquals(attempt, companion.pathFailureCount());
            assertEquals(99, companion.snapshot().targetObjectId());
            now += BattleCompanion.CROWD_RETRY_MILLIS * 1_000_000L;
        }
        assertTrue(companion.retryPath(
                now, ClassicWorldNpcPathfinder.Failure.UNREACHABLE, false));
        assertEquals(0, companion.pathFailureCount());
        assertEquals(0, companion.snapshot().targetObjectId());
    }

    private static BattleCompanion companion() {
        return BattleCompanion.tamed(
                88, 45034, 936, "PathPet", 5,
                30, 30, 5, 9, 15, 15, 100, 100);
    }
}
