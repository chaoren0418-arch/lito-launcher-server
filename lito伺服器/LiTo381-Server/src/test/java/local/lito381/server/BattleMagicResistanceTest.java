package local.lito381.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class BattleMagicResistanceTest {
    @Test void reproducesWisdomResourceTableBoundaries() {
        assertEquals(0, BattleMagicResistance.wisdomMagicResistance(14));
        assertEquals(3, BattleMagicResistance.wisdomMagicResistance(15));
        assertEquals(50, BattleMagicResistance.wisdomMagicResistance(24));
        assertEquals(52, BattleMagicResistance.wisdomMagicResistance(30));
        assertEquals(66, BattleMagicResistance.wisdomMagicResistance(50));
        assertEquals(66, BattleMagicResistance.wisdomMagicResistance(99));
    }

    @Test void reproducesClassAndOriginalWisdomMr() {
        assertEquals(10, BattleMagicResistance.classBase(0));
        assertEquals(0, BattleMagicResistance.classBase(1));
        assertEquals(25, BattleMagicResistance.classBase(2));
        assertEquals(15, BattleMagicResistance.classBase(3));
        assertEquals(4, BattleMagicResistance.originalMrBonus(4, 16));
        assertEquals(4, BattleMagicResistance.originalMrBonus(6, 18));
        assertEquals(0, BattleMagicResistance.originalMrBonus(6, 19));
    }

    @Test void reproducesOriginalIntelligenceMagicHit() {
        assertEquals(2, BattleMagicResistance.originalMagicHit(0, 14));
        assertEquals(0, BattleMagicResistance.originalMagicHit(1, 13));
        assertEquals(2, BattleMagicResistance.originalMagicHit(2, 15));
        assertEquals(1, BattleMagicResistance.originalMagicHit(3, 80));
        assertEquals(2, BattleMagicResistance.originalMagicHit(3, 81));
        assertEquals(4, BattleMagicResistance.originalMagicHit(5, 16));
    }

    @Test void reproducesOriginalIntelligenceCriticalAndDamageBonuses() {
        assertEquals(0, BattleMagicResistance.originalMagicCritical(2, 13));
        assertEquals(2, BattleMagicResistance.originalMagicCritical(2, 14));
        assertEquals(4, BattleMagicResistance.originalMagicCritical(2, 16));
        assertEquals(2, BattleMagicResistance.originalMagicCritical(3, 15));
        assertEquals(8, BattleMagicResistance.originalMagicCritical(3, 18));
        assertEquals(0, BattleMagicResistance.originalMagicCritical(3, 19));

        assertEquals(1, BattleMagicResistance.originalMagicDamage(3, 13));
        assertEquals(1, BattleMagicResistance.originalMagicDamage(5, 13));
        assertEquals(2, BattleMagicResistance.originalMagicDamage(5, 16));
        assertEquals(3, BattleMagicResistance.originalMagicDamage(5, 17));
        assertEquals(1, BattleMagicResistance.originalMagicDamage(6, 16));
        assertEquals(2, BattleMagicResistance.originalMagicDamage(6, 17));
        assertEquals(0, BattleMagicResistance.originalMagicDamage(6, 18));
    }

    @Test void appliesCriticalThenOriginalAndEquipmentDamageInSourceOrder() {
        BattleMagicResistance.MagicAttackerResult critical =
                BattleMagicResistance.attackerBonuses(21, 4, 18, 3, 18, 3);
        assertEquals(35, critical.damage()); // trunc(21*1.5)=31, then +1+3
        assertEquals(true, critical.critical());
        assertEquals(8, critical.originalCriticalBonus());
        assertEquals(1, critical.originalDamage());
        assertEquals(3, critical.equipmentDamage());

        BattleMagicResistance.MagicAttackerResult highLevel =
                BattleMagicResistance.attackerBonuses(21, 7, 1, 3, 18, 3);
        assertEquals(25, highLevel.damage());
        assertEquals(false, highLevel.critical());
    }

    @Test void reproducesRandomizedArmorReductionBoundary() {
        assertEquals(-2, BattleMagicResistance.armorDamageReduction(-2, 99));
        assertEquals(10, BattleMagicResistance.armorDamageReduction(10, 99));
        assertEquals(11, BattleMagicResistance.armorDamageReduction(11, 0));
        assertEquals(11, BattleMagicResistance.armorDamageReduction(15, 0));
        assertEquals(15, BattleMagicResistance.armorDamageReduction(15, 4));
        assertThrows(IllegalArgumentException.class,
                () -> BattleMagicResistance.armorDamageReduction(15, 5));
    }

    @Test void reproducesEveryMrCoefficientBoundaryAndJavaTruncation() {
        assertEquals(100, BattleMagicResistance.damageAfterResistance(100, 0, 0));
        assertEquals(75, BattleMagicResistance.damageAfterResistance(100, 50, 0));
        assertEquals(73, BattleMagicResistance.damageAfterResistance(100, 51, 0));
        assertEquals(57, BattleMagicResistance.damageAfterResistance(100, 100, 0));
        assertEquals(65, BattleMagicResistance.damageAfterResistance(100, 101, 0));
        assertEquals(60, BattleMagicResistance.damageAfterResistance(100, 120, 0));
        assertEquals(56, BattleMagicResistance.damageAfterResistance(100, 121, 0));
        assertEquals(52, BattleMagicResistance.damageAfterResistance(100, 140, 0));
        assertEquals(57, BattleMagicResistance.damageAfterResistance(100, 141, 0));
        assertEquals(54, BattleMagicResistance.damageAfterResistance(100, 160, 0));
        assertEquals(47, BattleMagicResistance.damageAfterResistance(100, 161, 0));
        assertEquals(44, BattleMagicResistance.damageAfterResistance(100, 180, 0));
        assertEquals(48, BattleMagicResistance.damageAfterResistance(100, 181, 0));
        assertEquals(44, BattleMagicResistance.damageAfterResistance(100, 200, 0));
        assertEquals(38, BattleMagicResistance.damageAfterResistance(100, 201, 0));
        assertEquals(36, BattleMagicResistance.damageAfterResistance(100, 220, 0));
        assertEquals(38, BattleMagicResistance.damageAfterResistance(100, 221, 0));
        assertEquals(36, BattleMagicResistance.damageAfterResistance(100, 240, 0));
        assertEquals(29, BattleMagicResistance.damageAfterResistance(100, 241, 0));
        assertEquals(27, BattleMagicResistance.damageAfterResistance(100, 254, 0));
        assertEquals(27, BattleMagicResistance.damageAfterResistance(100, 255, 0));
    }

    @Test void appliesAttackerOriginalMagicHitInsideTheMrFormula() {
        assertEquals(104, BattleMagicResistance.damageAfterResistance(100, 0, 4));
        assertEquals(76, BattleMagicResistance.damageAfterResistance(100, 50, 2));
    }

    @Test void rejectsUnqualifiedInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> BattleMagicResistance.damageAfterResistance(-1, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> BattleMagicResistance.classBase(7));
    }
}
