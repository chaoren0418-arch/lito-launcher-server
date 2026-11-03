package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BattleEarthBindRulesTest {
    @Test
    void blocksProbabilityMagicExceptTheTwoRetainedSourceExceptions() {
        assertTrue(BattleEarthBindRules.blocksProbabilityMagic(true, 11));
        assertTrue(BattleEarthBindRules.blocksProbabilityMagic(true, 157));
        assertFalse(BattleEarthBindRules.blocksProbabilityMagic(
                true, BattleEarthBindRules.WEAPON_BREAK));
        assertFalse(BattleEarthBindRules.blocksProbabilityMagic(
                true, BattleEarthBindRules.CANCELLATION));
        assertFalse(BattleEarthBindRules.blocksProbabilityMagic(false, 11));
    }

    @Test
    void nullifiesCombatDamageWithoutInventingNegativeDamage() {
        assertEquals(0, BattleEarthBindRules.protectedCombatDamage(true, 99));
        assertEquals(99, BattleEarthBindRules.protectedCombatDamage(false, 99));
        assertEquals(0, BattleEarthBindRules.protectedCombatDamage(false, -1));
    }
}
