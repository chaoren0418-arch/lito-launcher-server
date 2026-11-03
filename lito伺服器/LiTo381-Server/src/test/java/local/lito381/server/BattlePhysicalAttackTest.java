package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BattlePhysicalAttackTest {
    @Test void exactSourceWeaponRangeDistinguishesSwordSpearAndUnarmedReach() {
        SourceWeaponPhysicalStats sword = weapon(3, 1, 1);
        SourceWeaponPhysicalStats spear = weapon(90, 5, 2);

        assertEquals(1, BattlePhysicalAttack.maximumRange(
                SourceWeaponPhysicalStats.none()));
        assertTrue(BattlePhysicalAttack.supportsDistance(sword, 1));
        assertFalse(BattlePhysicalAttack.supportsDistance(sword, 2));
        assertTrue(BattlePhysicalAttack.supportsDistance(spear, 2));
        assertFalse(BattlePhysicalAttack.supportsDistance(spear, 3));
    }

    @Test void exactSourceWeaponRangeRetainsIndependentProjectileReach() {
        SourceWeaponPhysicalStats bow = weapon(19, 4, 15);
        SourceWeaponPhysicalStats gauntlet = weapon(80, 10, 10);

        assertEquals(15, BattlePhysicalAttack.maximumRange(bow));
        assertTrue(BattlePhysicalAttack.supportsDistance(bow, 15));
        assertFalse(BattlePhysicalAttack.supportsDistance(bow, 16));
        assertEquals(10, BattlePhysicalAttack.maximumRange(gauntlet));
        assertThrows(IllegalArgumentException.class,
                () -> BattlePhysicalAttack.supportsDistance(swordFixture(), -1));
        assertThrows(IllegalArgumentException.class,
                () -> BattlePhysicalAttack.maximumRange(null));
    }

    private static SourceWeaponPhysicalStats swordFixture() {
        return weapon(3, 1, 1);
    }

    private static SourceWeaponPhysicalStats weapon(int itemId, int type, int range) {
        return new SourceWeaponPhysicalStats(
                itemId, type, 1, 1, range, 0, 0, 0, 0);
    }

    @Test void pinsClassicStrengthAndDexterityHitTableBoundaries() {
        assertEquals(-2, BattlePhysicalAttack.strengthHit(1));
        assertEquals(-2, BattlePhysicalAttack.strengthHit(8));
        assertEquals(-1, BattlePhysicalAttack.strengthHit(9));
        assertEquals(0, BattlePhysicalAttack.strengthHit(11));
        assertEquals(1, BattlePhysicalAttack.strengthHit(13));
        assertEquals(5, BattlePhysicalAttack.strengthHit(21));
        assertEquals(18, BattlePhysicalAttack.strengthHit(60));

        assertEquals(-2, BattlePhysicalAttack.dexterityHit(1));
        assertEquals(-2, BattlePhysicalAttack.dexterityHit(6));
        assertEquals(-1, BattlePhysicalAttack.dexterityHit(7));
        assertEquals(0, BattlePhysicalAttack.dexterityHit(9));
        assertEquals(4, BattlePhysicalAttack.dexterityHit(17));
        assertEquals(18, BattlePhysicalAttack.dexterityHit(32));
        assertEquals(19, BattlePhysicalAttack.dexterityHit(33));
        assertEquals(28, BattlePhysicalAttack.dexterityHit(60));
    }

    @Test void cookingSkuBonusJoinsTheHitRateBeforeTheAcComparison() {
        int withoutCooking = BattlePhysicalAttack.hitChance(
                1, 1, 13, 12, 8, false, 0, 8, 0);
        int withArianSteak = BattlePhysicalAttack.hitChance(
                1, 1, 13, 12, 8, false, 2, 8, 0);
        assertEquals(15, withoutCooking);
        assertEquals(100, withArianSteak);

        int rangedWithoutCooking = BattlePhysicalAttack.hitChance(
                3, 1, 8, 13, 8, true, 0, 8, 0);
        int rangedWithQuestanux = BattlePhysicalAttack.hitChance(
                3, 1, 8, 13, 8, true, 1, 8, 0);
        assertEquals(15, rangedWithoutCooking);
        assertEquals(100, rangedWithQuestanux);
    }

    @Test void negativeAcAndFinalPercentRollMatchTheSameFamilyEnvelope() {
        assertEquals(15, BattlePhysicalAttack.negativeAcRollMaximum(-10));
        assertEquals(1, BattlePhysicalAttack.negativeAcRollMaximum(-1));
        assertEquals(0, BattlePhysicalAttack.negativeAcRollMaximum(0));
        assertTrue(BattlePhysicalAttack.hits(15, 15));
        assertFalse(BattlePhysicalAttack.hits(15, 16));
        assertTrue(BattlePhysicalAttack.hits(100, 100));
        assertThrows(IllegalArgumentException.class,
                () -> BattlePhysicalAttack.hitChance(
                        1, 1, 13, 12, -10, false, 0, 8, 0));
    }

    @Test void qualifiedEquipmentHitJoinsBeforeTheD20Envelope() {
        assertEquals(15, BattlePhysicalAttack.hitChance(
                1, 1, 13, 12, 8, false, 0, 0, 8, 0));
        assertEquals(100, BattlePhysicalAttack.hitChance(
                1, 1, 13, 12, 8, false, 2, 0, 8, 0));
        assertEquals(15, BattlePhysicalAttack.hitChance(
                1, 1, 13, 12, 8, false, -2, 2, 8, 0));
    }

    @Test void targetDodgePointsDirectlyLowerTheAttackerD20Envelope() {
        assertEquals(100, BattlePhysicalAttack.hitChanceWithStatBonus(
                4, 52, 1, 10, 0, 0, 0, 5, 0));
        assertEquals(15, BattlePhysicalAttack.hitChanceWithStatBonus(
                4, 52, 1, 10, 0, 0, 5, 5, 0));
        assertTrue(BattlePhysicalAttack.hits(100, 16));
        assertFalse(BattlePhysicalAttack.hits(15, 16));
        assertThrows(IllegalArgumentException.class,
                () -> BattlePhysicalAttack.hitChanceWithStatBonus(
                        4, 52, 1, 10, 0, 0, 11, 5, 0));
    }

    @Test void resistFearDodgeDownIndependentlyRaisesTheAttackerD20Envelope() {
        assertEquals(15, BattlePhysicalAttack.hitChanceWithStatBonus(
                4, 52, 1, 10, 0, 0, 5, 0, 5, 0));
        assertEquals(100, BattlePhysicalAttack.hitChanceWithStatBonus(
                4, 52, 1, 10, 0, 0, 5, 5, 5, 0));
    }

    @Test void nativeDragonKnightAndIllusionistUseRetainedClassHitLevels() {
        assertEquals(0, BattlePhysicalAttack.classHitCorrection(5, 24));
        assertEquals(1, BattlePhysicalAttack.classHitCorrection(5, 25));
        assertEquals(0, BattlePhysicalAttack.classHitCorrection(6, 39));
        assertEquals(1, BattlePhysicalAttack.classHitCorrection(6, 40));
        assertEquals(100, BattlePhysicalAttack.hitChanceWithStatBonus(
                5, 25, 0, 8, 0, 0, 0, 8, 0));
        assertEquals(100, BattlePhysicalAttack.hitChanceWithStatBonus(
                6, 40, 0, 8, 0, 0, 0, 8, 0));
    }
}
