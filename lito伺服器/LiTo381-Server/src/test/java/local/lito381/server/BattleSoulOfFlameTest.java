package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class BattleSoulOfFlameTest {
    @Test void forcesArmedMeleeDieToMaximumButLeavesInactiveAndUnarmedAlone() {
        assertEquals(3, BattleSoulOfFlame.meleeWeaponRoll(false, 3, 12));
        assertEquals(12, BattleSoulOfFlame.meleeWeaponRoll(true, 3, 12));
        assertEquals(0, BattleSoulOfFlame.meleeWeaponRoll(true, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> BattleSoulOfFlame.meleeWeaponRoll(true, 13, 12));
    }

    @Test void rangedOverrideAddsWeaponMaximumWithoutReplacingProjectileDie() {
        SourceWeaponPhysicalStats bow = new SourceWeaponPhysicalStats(
                175, 4, 18, 12, 12, 0, 0, 0, 0);
        assertEquals(0, BattleSoulOfFlame.rangedWeaponBase(false, bow, false));
        assertEquals(18, BattleSoulOfFlame.rangedWeaponBase(true, bow, false));
        assertEquals(12, BattleSoulOfFlame.rangedWeaponBase(true, bow, true));
        assertThrows(IllegalArgumentException.class,
                () -> BattleSoulOfFlame.rangedWeaponBase(true, null, false));
    }

    @Test void unconditionalMultiplierUsesRetainedCastTruncation() {
        assertEquals(11, BattleSoulOfFlame.damageAfterMultiplier(false, 11));
        assertEquals(16, BattleSoulOfFlame.damageAfterMultiplier(true, 11));
        assertEquals(1.0D, BattlePhysicalProcBuff.soulOfFlameMultiplier(false));
        assertEquals(1.5D, BattlePhysicalProcBuff.soulOfFlameMultiplier(true));
        assertThrows(IllegalArgumentException.class,
                () -> BattleSoulOfFlame.damageAfterMultiplier(true, -1));
    }
}
