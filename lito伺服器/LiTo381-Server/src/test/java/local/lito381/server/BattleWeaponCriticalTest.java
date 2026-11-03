package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BattleWeaponCriticalTest {
    @Test void clawIsZeroOrMaximumThenAddsItsNativeFivePoints() {
        SourceWeaponPhysicalStats claw = weapon(
                11, 8, 0, 0.0D, 33, false, false);
        BattleWeaponCritical.RollOutcome maximum =
                BattleWeaponCritical.roll(claw, 8, 0, 33);
        BattleWeaponCritical.RollOutcome zero =
                BattleWeaponCritical.roll(claw, 8, 0, 34);
        assertEquals(8, maximum.damage());
        assertTrue(maximum.triggered());
        assertEquals(0, zero.damage());
        assertFalse(zero.triggered());
        BattleWeaponCritical.DamageOutcome damage =
                BattleWeaponCritical.apply(claw, 20, 0);
        assertEquals(25, damage.damage());
        assertEquals(5, damage.fixedBonus());
    }

    @Test void edoryuUsesSelectedTenPercentOnePointTwoMultiplier() {
        SourceWeaponPhysicalStats edoryu = weapon(
                12, 8, 10, 1.2D, 0, false, false);
        assertEquals(4, BattleWeaponCritical.roll(edoryu, 8, 4, 0).damage());
        BattleWeaponCritical.DamageOutcome triggered =
                BattleWeaponCritical.apply(edoryu, 25, 10);
        BattleWeaponCritical.DamageOutcome normal =
                BattleWeaponCritical.apply(edoryu, 25, 11);
        assertEquals(30, triggered.damage());
        assertEquals(1.2D, triggered.multiplier());
        assertTrue(triggered.triggered());
        assertEquals(25, normal.damage());
        assertFalse(normal.triggered());
    }

    @Test void nonClawMaximumControlExcludesNaturalMaximumOnMiss() {
        SourceWeaponPhysicalStats sword = weapon(
                1, 10, 0, 0.0D, 5, false, true);
        BattleWeaponCritical.RollOutcome triggered =
                BattleWeaponCritical.roll(sword, 10, 0, 5);
        assertEquals(10, triggered.damage());
        assertTrue(triggered.effectEnabled());
        assertEquals(9, BattleWeaponCritical.roll(sword, 10, 9, 6).damage());
        assertThrows(IllegalArgumentException.class,
                () -> BattleWeaponCritical.roll(sword, 10, 10, 6));
    }

    @Test void burningSpiritAndWeaponDoubleDamageUseOnlyTheHighestMultiplier() {
        SourceWeaponPhysicalStats edoryu = weapon(
                12, 8, 10, 1.2D, 0, false, false);
        BattleWeaponCritical.DamageOutcome both =
                BattleWeaponCritical.apply(edoryu, 25, 10, 1.5D);
        BattleWeaponCritical.DamageOutcome burningOnly =
                BattleWeaponCritical.apply(edoryu, 25, 11, 1.5D);
        assertTrue(both.triggered());
        assertEquals(37, both.damage());
        assertEquals(37, burningOnly.damage());
        assertEquals(1.5D, both.multiplier());
        assertEquals(1.5D, burningOnly.multiplier());

        SourceWeaponPhysicalStats unarmed = new SourceWeaponPhysicalStats(
                0, 0, 0, 0, 0, 0, 0, 0, 0);
        assertEquals(15, BattleWeaponCritical.apply(
                unarmed, 10, 0, 1.5D).damage());
    }

    @Test void rangedMaximumControlSelectsTheProjectileMaximum() {
        SourceWeaponPhysicalStats bow = weapon(
                4, 0, 0, 0.0D, 7, false, false);
        assertEquals(13, BattleWeaponCritical.roll(bow, 13, 0, 7).damage());
        assertEquals(13, BattleWeaponCritical.roll(bow, 13, 13, 8).damage());
    }

    @Test void rejectsIncompleteTraitsAndInvalidDice() {
        assertThrows(IllegalArgumentException.class,
                () -> new SourceWeaponCombatTraits(
                        11, 1, 0, 0, 0, 10, 0.0D, 0, false, false));
        SourceWeaponPhysicalStats claw = weapon(
                11, 8, 0, 0.0D, 33, false, false);
        assertThrows(IllegalArgumentException.class,
                () -> BattleWeaponCritical.roll(claw, 8, 1, 33));
        assertThrows(IllegalArgumentException.class,
                () -> BattleWeaponCritical.apply(claw, 10, 1));
    }

    private static SourceWeaponPhysicalStats weapon(
            int type, int maximum,
            int doubleChance, double doubleMultiplier, int maximumChance,
            boolean doubleEffect, boolean maximumEffect) {
        return new SourceWeaponPhysicalStats(
                1000 + type, type, maximum, maximum, 1,
                0, 0, 0, 0).withCombatTraits(new SourceWeaponCombatTraits(
                        11, 1, 0, 0, 0,
                        doubleChance, doubleMultiplier, maximumChance,
                        doubleEffect, maximumEffect));
    }
}
