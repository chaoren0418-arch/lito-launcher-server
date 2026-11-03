package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BattleAbsoluteBarrierRulesTest {
    @Test void damageIsZeroOnlyWhileBarrierIsActive() {
        assertEquals(0, BattleAbsoluteBarrierRules.protectedCombatDamage(true, 912));
        assertEquals(912, BattleAbsoluteBarrierRules.protectedCombatDamage(false, 912));
        assertEquals(0, BattleAbsoluteBarrierRules.protectedCombatDamage(false, -1));
    }

    @Test void retainedIncomingSpellWhitelistIsExactForImplementedFamilies() {
        int[] allowed = {
                BattleSpellCatalog.CURSE_BLIND, 27, BattleSpellCatalog.DARKNESS,
                BattleSpellCatalog.WEAKNESS, BattleSpellCatalog.DISEASE,
                BattleSpellCatalog.FOG_OF_SLEEPING, BattleSpellCatalog.MASS_SLOW, 29,
                BattleSpellCatalog.CANCELLATION, BattleSpellCatalog.SILENCE,
                BattleSpellCatalog.DECAY_POTION, BattleSpellCatalog.MASS_TELEPORT,
                BattleSpellCatalog.DETECTION,
                BattleSpellCatalog.COUNTER_DETECTION, 152,
                BattleSpellCatalog.PHYSICAL_ENCHANT_DEX,
                BattleSpellCatalog.PHYSICAL_ENCHANT_STR,
                BattleSpellCatalog.BLESS_WEAPON, BattleSpellCatalog.EARTH_SKIN,
                BattleSpellCatalog.IMMUNE_TO_HARM, BattleSpellCatalog.REMOVE_CURSE
        };
        for (int skillId : allowed) {
            assertFalse(BattleAbsoluteBarrierRules.blocksIncomingSpell(true, skillId),
                    "retained whitelist skill " + skillId);
        }
        assertTrue(BattleAbsoluteBarrierRules.blocksIncomingSpell(
                true, BattleSpellCatalog.BASIC_HEAL));
        assertTrue(BattleAbsoluteBarrierRules.blocksIncomingSpell(
                true, BattleSpellCatalog.EARTH_BIND));
        assertTrue(BattleAbsoluteBarrierRules.blocksIncomingSpell(
                true, BattleSpellCatalog.WIND_SHOT));
        assertTrue(BattleAbsoluteBarrierRules.blocksIncomingSpell(
                true, BattleSpellCatalog.TRIPLE_ARROW));
        assertTrue(BattleAbsoluteBarrierRules.blocksIncomingSpell(
                true, BattleSpellCatalog.IRON_SKIN));
        assertTrue(BattleAbsoluteBarrierRules.blocksIncomingSpell(
                true, BattleSpellCatalog.DISINTEGRATE));
        assertFalse(BattleAbsoluteBarrierRules.blocksIncomingSpell(
                false, BattleSpellCatalog.DISINTEGRATE));
    }
}
