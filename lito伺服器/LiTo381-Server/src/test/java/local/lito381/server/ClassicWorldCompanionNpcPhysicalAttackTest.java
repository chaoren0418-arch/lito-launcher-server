package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ClassicWorldCompanionNpcPhysicalAttackTest {
    private static final long RETAINED_EPOCH_MILLIS = 1_735_689_600_000L;

    @Test
    void nativeNpcHitUsesLevelPlusThreeAndFifteenPercentFumbleFloor() {
        var floorHit = ClassicWorldCompanionNpcPhysicalAttack.hit(
                5, 0, 0, false, 9, 1, 0, 15);
        assertEquals(8, floorHit.baseHitRate());
        assertEquals(6, floorHit.attackerDice());
        assertEquals(1, floorHit.defenderDice());
        assertEquals(15, floorHit.normalizedHitRate());
        assertTrue(floorHit.hit());

        var floorMiss = ClassicWorldCompanionNpcPhysicalAttack.hit(
                5, 0, 0, false, 9, 1, 0, 16);
        assertFalse(floorMiss.hit());
    }

    @Test
    void nativeNpcHitUsesNegativeAcDiceAndBossThirtyPointAddition() {
        var ordinary = ClassicWorldCompanionNpcPhysicalAttack.hit(
                5, 0, 0, false, -50, 20, 50, 16);
        assertEquals(60, ordinary.defenderDice());
        assertEquals(15, ordinary.normalizedHitRate());
        assertFalse(ordinary.hit());

        var boss = ClassicWorldCompanionNpcPhysicalAttack.hit(
                5, 0, 0, true, -50, 20, 50, 100);
        assertEquals(55, boss.attackerDice());
        assertEquals(100, boss.normalizedHitRate());
        assertTrue(boss.hit());
    }

    @Test
    void petAndTamedDamageRollAgainstTemplateLevelButSummonRollsRuntimeLevel() {
        SourceNpcPhysicalStats target = target(10, 0);
        var pet = ClassicWorldCompanionNpcPhysicalAttack.damage(
                BattleCompanion.Kind.PET, 28, 5, 16, 2,
                4, 99, 0, 0, 0, false, 10, 0,
                target, 0, 1, RETAINED_EPOCH_MILLIS + 3_600_000L);
        assertEquals(5, pet.sourceRollBound());
        assertEquals(15, pet.damage());

        var tamed = ClassicWorldCompanionNpcPhysicalAttack.damage(
                BattleCompanion.Kind.TAMED, 28, 5, 16, 2,
                4, 99, 0, 0, 0, false, 10, 0,
                target, 0, 1, RETAINED_EPOCH_MILLIS + 3_600_000L);
        assertEquals(15, tamed.damage());

        var summon = ClassicWorldCompanionNpcPhysicalAttack.damage(
                BattleCompanion.Kind.SUMMON, 28, 5, 16, 2,
                4, 99, 0, 0, 0, false, 10, 0,
                target, 0, 1, RETAINED_EPOCH_MILLIS + 3_600_000L);
        assertEquals(28, summon.sourceRollBound());
        assertEquals(14, summon.damage());
    }

    @Test
    void nativeDamagePreservesCriticalDmgupNightLeverageBossBreakAndDefenseOrder() {
        SourceNpcPhysicalStats target = target(-4, 2);
        var outcome = ClassicWorldCompanionNpcPhysicalAttack.damage(
                BattleCompanion.Kind.PET, 28, 5, 16, 2,
                4, 14, 0, 3, 1, true, 10, 2,
                target, 1, 2, RETAINED_EPOCH_MILLIS);
        assertTrue(outcome.critical());
        assertTrue(outcome.undeadNightBonus());
        assertEquals(2, outcome.targetAcMaximum());
        assertEquals(5, outcome.targetReduction());
        assertEquals(58.32D, outcome.beforeTargetReduction(), 0.000_001D);
        assertEquals(26, outcome.damage());
    }

    @Test
    void physicalHitCanStillBecomeZeroDamageAfterDefenseForPoisonOrdering() {
        var hit = ClassicWorldCompanionNpcPhysicalAttack.hit(
                5, 0, 0, true, 10, 1, 0, 100);
        var damage = ClassicWorldCompanionNpcPhysicalAttack.damage(
                BattleCompanion.Kind.SUMMON, 1, 1, 1, 0,
                0, 99, 0, 0, 0, false, 10, 0,
                target(10, 5), 0, 1, RETAINED_EPOCH_MILLIS);
        assertTrue(hit.hit());
        assertEquals(0, damage.damage());
    }

    @Test
    void weaponBreakRetainsOnePointFloorBeforeTargetReduction() {
        var damage = ClassicWorldCompanionNpcPhysicalAttack.damage(
                BattleCompanion.Kind.SUMMON, 1, 1, 1, 0,
                0, 99, 0, 0, 0, false, 10, 10,
                target(10, 0), 0, 1, RETAINED_EPOCH_MILLIS);
        assertEquals(1, damage.damage());
    }

    private static SourceNpcPhysicalStats target(
            int armorClass, int damageReduction) {
        return new SourceNpcPhysicalStats(
                45001, armorClass, false, damageReduction, false);
    }
}
