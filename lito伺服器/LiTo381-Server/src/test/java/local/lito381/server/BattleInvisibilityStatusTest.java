package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class BattleInvisibilityStatusTest {
    @Test void preservesIndefiniteSkill60UntilExplicitCancellation() {
        BattleInvisibilityStatus status = new BattleInvisibilityStatus();
        status.activate(60, 1_000L, 0);
        assertTrue(status.active());
        assertEquals(60, status.skillId());
        assertEquals(0L, status.expiresAtNanos());
        assertEquals(0, status.expire(Long.MAX_VALUE));
        assertTrue(status.cancel());
        assertFalse(status.active());
        assertFalse(status.cancel());
    }

    @Test void expiresBlindHidingAtItsExactMonotonicDeadline() {
        BattleInvisibilityStatus status = new BattleInvisibilityStatus();
        status.activate(97, 5_000L, 32);
        assertEquals(32_000_005_000L, status.expiresAtNanos());
        assertEquals(0, status.expire(32_000_004_999L));
        assertEquals(97, status.expire(32_000_005_000L));
        assertFalse(status.active());
        assertEquals(0, status.expire(Long.MAX_VALUE));
    }

    @Test void replacementUsesTheNewSkillAndDeadline() {
        BattleInvisibilityStatus status = new BattleInvisibilityStatus();
        status.activate(60, 100L, 0);
        status.activate(97, 200L, 32);
        assertEquals(97, status.skillId());
        assertEquals(32_000_000_200L, status.expiresAtNanos());
    }
}
