package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class BattleWeaponBreakTest {
    @Test
    void capsNpcStacksAtTenAndReducesFivePercentPerStack() {
        int stacks = 0;
        for (int index = 0; index < 12; index++) {
            stacks = BattleWeaponBreak.addStack(stacks);
        }
        assertEquals(10, stacks);
        assertEquals(95, BattleWeaponBreak.damageAfterStacks(100, 1));
        assertEquals(50, BattleWeaponBreak.damageAfterStacks(100, 10));
        assertEquals(1, BattleWeaponBreak.damageAfterStacks(1, 10));
    }

    @Test
    void playerWeaponDamageUsesTheRetainedOneThroughIntelligenceThirdBand() {
        assertEquals(1, BattleWeaponBreak.playerWeaponDamage(18, 1));
        assertEquals(6, BattleWeaponBreak.playerWeaponDamage(18, 6));
        assertThrows(IllegalArgumentException.class,
                () -> BattleWeaponBreak.playerWeaponDamage(18, 7));
    }

    @Test
    void playerWeaponDurabilityCapsAtEnchantPlusFiveWithoutLoweringObservedDamage() {
        assertEquals(1, BattleWeaponBreak.durabilityAfterDamage(0, 0, 1));
        assertEquals(5, BattleWeaponBreak.durabilityAfterDamage(4, 0, 6));
        assertEquals(5, BattleWeaponBreak.durabilityAfterDamage(5, 0, 6));
        assertEquals(11, BattleWeaponBreak.durabilityAfterDamage(6, 6, 127));
        assertEquals(7, BattleWeaponBreak.durabilityAfterDamage(7, 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> BattleWeaponBreak.durabilityAfterDamage(0, 0, 0));
    }
}
