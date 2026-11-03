package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class BattlePhysicalDamageTest {
    @Test void rollsQualifiedSmallAndLargeMeleeWeaponDice() {
        SourceWeaponPhysicalStats sword = new SourceWeaponPhysicalStats(
                24, 1, 6, 8, 1, 0, 0, 0, 0);
        assertEquals(6, BattlePhysicalDamage.weaponDieMaximum(sword, false));
        assertEquals(8, BattlePhysicalDamage.weaponDieMaximum(sword, true));
        assertEquals(13, BattlePhysicalDamage.sourceDamage(
                sword, false, false, 6, 0, 1, 4, 2));
        assertThrows(IllegalArgumentException.class,
                () -> BattlePhysicalDamage.sourceDamage(
                        sword, false, false, 7, 0, 1, 4, 2));
    }

    @Test void preservesTheExactUnarmedOneOrTwoPointExpression() {
        assertEquals(1, BattlePhysicalDamage.unarmedDamage(4));
        assertEquals(1, BattlePhysicalDamage.unarmedDamage(7));
        assertEquals(2, BattlePhysicalDamage.unarmedDamage(8));
        assertEquals(0, BattlePhysicalDamage.sourceDamage(
                SourceWeaponPhysicalStats.none(), false, false,
                0, 1, -5, 0, 0));
    }

    @Test void reservesRangedBaseDiceForQualifiedProjectileData() {
        SourceProjectilePhysicalStats arrow = new SourceProjectilePhysicalStats(
                0x1234_5678L, 40743, -2, 5, 7, 66, true, 20);
        SourceWeaponPhysicalStats bow = new SourceWeaponPhysicalStats(
                172, 4, 3, 3, 15, 1, 2, 7, 0).withProjectile(arrow);
        assertEquals(0, BattlePhysicalDamage.weaponDieMaximum(bow, false));
        assertEquals(19, BattlePhysicalDamage.sourceDamage(
                bow, true, false, 0, 5, 0, 4, 3, 7));
        assertThrows(IllegalArgumentException.class,
                () -> BattlePhysicalDamage.sourceDamage(
                        bow, true, false, 1, 5, 0, 4, 3, 7));
        assertThrows(IllegalArgumentException.class,
                () -> BattlePhysicalDamage.sourceDamage(
                        bow, true, false, 0, 6, 0, 4, 3, 7));
        SourceWeaponPhysicalStats noArrow = new SourceWeaponPhysicalStats(
                172, 4, 3, 3, 15, 1, 2, 7, 0);
        assertThrows(IllegalArgumentException.class,
                () -> BattlePhysicalDamage.sourceDamage(
                        noArrow, true, false, 0, 1, 0, 4, 3, 7));
    }
}
