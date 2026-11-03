package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BattleCounterBarrierTest {
    private static final BattleSpellCatalog.CounterBarrierSpell SPELL =
            BattleSpellCatalog.counterBarrierSpell(
                    BattleSpellCatalog.COUNTER_BARRIER);
    private static final SourceWeaponPhysicalStats TWO_HAND_SWORD =
            new SourceWeaponPhysicalStats(52, 3, 17, 21, 1,
                    0, 0, 0, 0).withNameId("$3");

    @Test void retainedProbabilityUsesLevelBandMagicHitAndAttackerMr() {
        assertEquals(55, SPELL.triggerProbability(52, 51, 1, 20));
        assertEquals(45, SPELL.triggerProbability(52, 52, 1, 20));
        assertEquals(35, SPELL.triggerProbability(51, 52, 1, 20));
        assertEquals(90, SPELL.triggerProbability(52, 51, 100, 0));
        assertEquals(1, SPELL.triggerProbability(51, 52, 0, 1_000));
    }

    @Test void triggerSuppressesOriginalAttackAndReturnsExactSourceDamage() {
        BattleCounterBarrier.Decision trigger = BattleCounterBarrier.decide(
                SPELL, true, TWO_HAND_SWORD, 52, 1, 1, 0, false, 1);
        assertEquals(new BattleCounterBarrier.Decision(
                true, 56, 1, true, 42), trigger);

        BattleCounterBarrier.Decision miss = BattleCounterBarrier.decide(
                SPELL, true, TWO_HAND_SWORD, 52, 1, 1, 0, false, 57);
        assertEquals(new BattleCounterBarrier.Decision(
                true, 56, 57, false, 0), miss);
    }

    @Test void bossWrongWeaponAndInactiveStateCannotReflect() {
        SourceWeaponPhysicalStats sword = new SourceWeaponPhysicalStats(
                1, 1, 5, 5, 1, 0, 0, 0, 0);
        assertEquals(BattleCounterBarrier.Decision.inactive(),
                BattleCounterBarrier.decide(
                        SPELL, false, TWO_HAND_SWORD,
                        52, 1, 1, 0, false, 1));
        assertEquals(BattleCounterBarrier.Decision.inactive(),
                BattleCounterBarrier.decide(
                        SPELL, true, sword,
                        52, 1, 1, 0, false, 1));
        assertEquals(BattleCounterBarrier.Decision.inactive(),
                BattleCounterBarrier.decide(
                        SPELL, true, TWO_HAND_SWORD,
                        52, 1, 1, 0, true, 1));
    }

    @Test void exactDamageFormulaIncludesLargeEnchantAndModifierBeforeDoubling() {
        SourceWeaponPhysicalStats enchanted = new SourceWeaponPhysicalStats(
                52, 3, 17, 21, 1, 0, 3, 6, 0);
        assertEquals(60, SPELL.reflectedSourceDamage(enchanted));
        assertFalse(SPELL.supportsWeapon(SourceWeaponPhysicalStats.none()));
        assertTrue(SPELL.supportsWeapon(TWO_HAND_SWORD));
        assertThrows(IllegalArgumentException.class,
                () -> BattleCounterBarrier.decide(
                        SPELL, true, TWO_HAND_SWORD,
                        52, 1, 1, 0, false, 0));
    }
}
