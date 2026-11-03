package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ClassicWorldPinkNameStatusTest {
    private static final long SECOND = 1_000_000_000L;

    @Test
    void repeatedPublicationKeepsTheFirstNativeTimerLease() {
        ClassicWorldPinkNameStatus status = new ClassicWorldPinkNameStatus();
        ClassicWorldPinkNameStatus.Publication first =
                status.publishActive(4, -1, 33313, 32339, 10 * SECOND);
        ClassicWorldPinkNameStatus.Publication repeated =
                status.publishActive(4, -1, 33320, 32340, 50 * SECOND);

        assertTrue(first.started());
        assertFalse(repeated.started());
        assertEquals(first.snapshot(), repeated.snapshot());
        assertEquals(33313, status.snapshot().originX());
    }

    @Test
    void retainedTimerClearsOnTheOneHundredEightyFirstTick() {
        ClassicWorldPinkNameStatus status = new ClassicWorldPinkNameStatus();
        status.publishActive(4, -1, 33313, 32339, 0);

        assertFalse(status.expire(true, true, 4, -1, 33313, 32339,
                180 * SECOND).cleared());
        ClassicWorldPinkNameStatus.Expiration expiration = status.expire(
                true, true, 4, -1, 33313, 32339, 181 * SECOND);
        assertTrue(expiration.cleared());
        assertEquals(ClassicWorldPinkNameStatus.StopReason.TIMER_EXPIRED,
                expiration.reason());
    }

    @Test
    void mapDeathAndNativeScreenRulesClearTheLease() {
        ClassicWorldPinkNameStatus map = active();
        assertEquals(ClassicWorldPinkNameStatus.StopReason.MAP_CHANGED,
                map.expire(true, true, 5, -1, 33313, 32339, SECOND).reason());

        ClassicWorldPinkNameStatus dead = active();
        assertEquals(ClassicWorldPinkNameStatus.StopReason.PLAYER_DIED,
                dead.expire(true, false, 4, -1, 33313, 32339, SECOND).reason());

        ClassicWorldPinkNameStatus moved = active();
        assertEquals(ClassicWorldPinkNameStatus.StopReason.LEFT_NATIVE_SCREEN,
                moved.expire(true, true, 4, -1, 33340, 32339, SECOND).reason());

        ClassicWorldPinkNameStatus phase = active();
        assertEquals(ClassicWorldPinkNameStatus.StopReason.PHASE_LEFT_WORLD,
                phase.expire(false, true, 4, -1, 33313, 32339, SECOND).reason());
    }

    private static ClassicWorldPinkNameStatus active() {
        ClassicWorldPinkNameStatus status = new ClassicWorldPinkNameStatus();
        status.publishActive(4, -1, 33313, 32339, 0);
        return status;
    }
}
