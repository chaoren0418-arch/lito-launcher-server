package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class BattleChainswordWeaknessTest {
    @Test void advancesAndRefreshesTheThreeOriginalExposureLevels() {
        BattleChainswordWeakness state = new BattleChainswordWeakness();
        BattleChainswordWeakness.AttackOutcome level1 = state.attack(5, 18, 100, 1, 19);
        BattleChainswordWeakness.AttackOutcome level2 = state.attack(5, 18, 100, 2, 39);
        BattleChainswordWeakness.AttackOutcome level3 = state.attack(5, 18, 100, 3, 39);
        BattleChainswordWeakness.AttackOutcome miss = state.attack(5, 18, 100, 4, 20);
        BattleChainswordWeakness.AttackOutcome refresh = state.attack(5, 18, 100, 5, 0);

        assertEquals(1, level1.afterLevel());
        assertEquals(3, level1.damageBonus());
        assertEquals(2, level2.afterLevel());
        assertEquals(6, level2.damageBonus());
        assertEquals(3, level3.afterLevel());
        assertEquals(9, level3.damageBonus());
        assertFalse(miss.triggered());
        assertEquals(0, miss.damageBonus());
        assertEquals(3, miss.afterLevel());
        assertTrue(refresh.triggered());
        assertEquals(9, refresh.damageBonus());
        assertEquals(BattleChainswordWeakness.DURATION_NANOS,
                refresh.remainingNanos());
    }

    @Test void expiresAtSixteenSecondsAndBindsAStackToOneTarget() {
        BattleChainswordWeakness state = new BattleChainswordWeakness();
        state.attack(5, 18, 100, 0, 0);
        BattleChainswordWeakness.AttackOutcome switched =
                state.attack(5, 18, 200, 1, 0);
        assertTrue(switched.targetReset());
        assertEquals(1, switched.afterLevel());
        assertEquals(200, switched.targetObjectId());

        BattleChainswordWeakness.AttackOutcome expired = state.attack(
                5, 18, 200, BattleChainswordWeakness.DURATION_NANOS + 1, 99);
        assertTrue(expired.expired());
        assertEquals(0, expired.afterLevel());
        assertEquals(0, expired.targetObjectId());
    }

    @Test void foeSlayerConsumesTwentyDamagePerExposureLevel() {
        BattleChainswordWeakness state = new BattleChainswordWeakness();
        state.attack(5, 18, 100, 1, 0);
        state.attack(5, 18, 100, 2, 39);
        state.attack(5, 18, 100, 3, 39);

        BattleChainswordWeakness.ConsumeOutcome consumed =
                state.consumeFoeSlayer(5, 100, 4);
        assertTrue(consumed.consumed());
        assertEquals(3, consumed.consumedLevel());
        assertEquals(60, consumed.damageBonus());
        assertEquals(0, state.snapshot(4).level());
    }

    @Test void rejectsIneligibleActorsAndDoesNotConsumeAnotherTargetsStack() {
        BattleChainswordWeakness state = new BattleChainswordWeakness();
        assertFalse(state.attack(4, 18, 100, 0, 0).eligible());
        assertFalse(state.attack(5, 17, 100, 0, 0).eligible());
        state.attack(5, 18, 100, 1, 0);

        BattleChainswordWeakness.ConsumeOutcome mismatch =
                state.consumeFoeSlayer(5, 200, 2);
        assertTrue(mismatch.targetMismatch());
        assertFalse(mismatch.consumed());
        assertEquals(0, mismatch.damageBonus());
        assertEquals(0, state.snapshot(2).level());
    }
}
