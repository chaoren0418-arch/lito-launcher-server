package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BattleBounceAttackTest {
    private static SourceWeaponPhysicalStats meleeWeapon() {
        return new SourceWeaponPhysicalStats(69, 12, 6, 6, 1,
                0, 0, 0, 0);
    }

    @Test void exactTenPercentBoundaryDamagesOnlyEligibleMeleeWeapons() {
        BattleAbilityStatus.BounceAttack active =
                new BattleAbilityStatus.BounceAttack(-2, 6, 10, 64);

        BattleBounceAttack.Decision ten =
                BattleBounceAttack.decide(active, meleeWeapon(), 10);
        assertTrue(ten.eligible());
        assertEquals(10, ten.chance());
        assertTrue(ten.damagesWeapon());
        assertFalse(BattleBounceAttack.decide(
                active, meleeWeapon(), 11).damagesWeapon());

        SourceWeaponPhysicalStats bow = new SourceWeaponPhysicalStats(
                172, 10, 5, 5, 20, 0, 0, 0, 0);
        assertEquals(BattleBounceAttack.Decision.ineligible(),
                BattleBounceAttack.decide(active, bow, 1));
        assertEquals(BattleBounceAttack.Decision.ineligible(),
                BattleBounceAttack.decide(
                        new BattleAbilityStatus.BounceAttack(0, 0, 0, 0),
                        meleeWeapon(), 1));
    }

    @Test void invalidEligiblePercentileRollIsRejected() {
        BattleAbilityStatus.BounceAttack active =
                new BattleAbilityStatus.BounceAttack(-2, 6, 10, 64);
        assertThrows(IllegalArgumentException.class,
                () -> BattleBounceAttack.decide(active, meleeWeapon(), 0));
        assertThrows(IllegalArgumentException.class,
                () -> BattleBounceAttack.decide(active, meleeWeapon(), 101));
    }
}
