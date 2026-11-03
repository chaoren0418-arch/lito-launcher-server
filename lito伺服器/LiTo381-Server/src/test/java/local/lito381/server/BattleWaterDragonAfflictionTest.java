package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattleWaterDragonAfflictionTest {
    @Test
    void pinsRetainedIdsAliasesEffectsAndTimerOrder() {
        assertEquals(12, BattleWaterDragonAffliction.DURATION_SECONDS);
        assertEquals(5_000, BattleWaterDragonAffliction.TIMER_INTERVAL_MILLIS);
        assertEquals(BattleWaterDragonAffliction.Kind.POTION_EROSION,
                BattleWaterDragonAffliction.timerKind(0));
        assertEquals(BattleWaterDragonAffliction.Kind.DIRTY_CURRENT,
                BattleWaterDragonAffliction.timerKind(1));
        assertEquals(BattleWaterDragonAffliction.Kind.HEALING_EROSION,
                BattleWaterDragonAffliction.timerKind(2));
        assertEquals(4011,
                BattleWaterDragonAffliction.Kind.POTION_EROSION.skillId());
        assertEquals(49011,
                BattleWaterDragonAffliction.Kind.POTION_EROSION.aliasSkillId());
        assertEquals(7781,
                BattleWaterDragonAffliction.Kind.POTION_EROSION.effectGfx());
        assertEquals(4012,
                BattleWaterDragonAffliction.Kind.DIRTY_CURRENT.skillId());
        assertEquals(49012,
                BattleWaterDragonAffliction.Kind.DIRTY_CURRENT.aliasSkillId());
        assertEquals(7782,
                BattleWaterDragonAffliction.Kind.DIRTY_CURRENT.effectGfx());
        assertEquals(4013,
                BattleWaterDragonAffliction.Kind.HEALING_EROSION.skillId());
        assertEquals(49013,
                BattleWaterDragonAffliction.Kind.HEALING_EROSION.aliasSkillId());
        assertEquals(7780,
                BattleWaterDragonAffliction.Kind.HEALING_EROSION.effectGfx());
        assertThrows(IllegalArgumentException.class,
                () -> BattleWaterDragonAffliction.timerKind(3));
    }

    @Test
    void timerAreaUsesRetainedStrictBounds() {
        assertTrue(BattleWaterDragonAffliction.insideTimerArea(32943, 32811));
        assertTrue(BattleWaterDragonAffliction.insideTimerArea(32979, 32870));
        assertFalse(BattleWaterDragonAffliction.insideTimerArea(32942, 32811));
        assertFalse(BattleWaterDragonAffliction.insideTimerArea(32980, 32811));
        assertFalse(BattleWaterDragonAffliction.insideTimerArea(32943, 32810));
        assertFalse(BattleWaterDragonAffliction.insideTimerArea(32943, 32871));
    }

    @Test
    void leasesAreMutuallyExclusiveExpireAndSurviveOrdinaryCancellation() {
        BattleTimedStatus status = new BattleTimedStatus();
        long now = TimeUnit.SECONDS.toNanos(100);
        assertTrue(status.activateWaterDragonAffliction(
                BattleWaterDragonAffliction.Kind.POTION_EROSION, now, 12));
        assertFalse(status.activateWaterDragonAffliction(
                BattleWaterDragonAffliction.Kind.DIRTY_CURRENT, now, 12));
        assertTrue(status.waterDragonPotionErosionActive(now));

        status.cancelMagicEffects();
        assertTrue(status.waterDragonPotionErosionActive(now));
        assertFalse(status.expireWaterDragonAffliction(
                now + TimeUnit.SECONDS.toNanos(12) - 1));
        assertTrue(status.expireWaterDragonAffliction(
                now + TimeUnit.SECONDS.toNanos(12)));
        assertNull(status.waterDragonAffliction(
                now + TimeUnit.SECONDS.toNanos(12)));

        assertTrue(status.activateWaterDragonAffliction(
                BattleWaterDragonAffliction.Kind.DIRTY_CURRENT,
                now + TimeUnit.SECONDS.toNanos(12), 12));
        assertEquals(BattleWaterDragonAffliction.Kind.DIRTY_CURRENT,
                status.clearWaterDragonAffliction());
        assertNull(status.waterDragonAffliction(
                now + TimeUnit.SECONDS.toNanos(12)));

        assertTrue(status.activateWaterDragonAffliction(
                BattleWaterDragonAffliction.Kind.HEALING_EROSION,
                now + TimeUnit.SECONDS.toNanos(13), 12));
        status.clear();
        assertNull(status.waterDragonAffliction(
                now + TimeUnit.SECONDS.toNanos(13)));
    }
}
