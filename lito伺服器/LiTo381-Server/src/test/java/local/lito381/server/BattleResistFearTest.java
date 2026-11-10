package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class BattleResistFearTest {
    @Test void selectorOnlyTargetRecoveryUsesLastExplicitThenUniqueRangeThree() {
        List<BattleResistFear.Candidate> candidates = List.of(
                new BattleResistFear.Candidate(15, 103, 120),
                new BattleResistFear.Candidate(16, 100, 123),
                new BattleResistFear.Candidate(17, 104, 120));
        assertEquals(16, BattleResistFear.resolveTarget(
                16, 100, 120, 3, candidates));
        assertEquals(0, BattleResistFear.resolveTarget(
                0, 100, 120, 3, candidates));
        assertEquals(15, BattleResistFear.resolveTarget(
                0, 100, 120, 3, List.of(candidates.get(0))));
        assertEquals(0, BattleResistFear.resolveTarget(
                17, 100, 120, 3, candidates));
    }

    @Test void catalogPinsExactSelectedRowAndConfiguredProbabilityFormula() {
        BattleSpellCatalog.ResistFearSpell spell =
                BattleSpellCatalog.resistFearSpell(BattleSpellCatalog.RESIST_FEAR);
        assertEquals(188, spell.skillId());
        assertEquals(23, spell.row());
        assertEquals(3, spell.column());
        assertEquals(24, spell.level());
        assertEquals(3, spell.number());
        assertEquals(19, spell.actionId());
        assertEquals(6_586, spell.effectGfx());
        assertEquals(0, spell.mpCost());
        assertEquals(30, spell.hpCost());
        assertEquals(1_000, spell.reuseDelayMillis());
        assertEquals(1_200, spell.clientCastClockMillis());
        assertEquals(60, spell.durationSeconds());
        assertEquals(3, spell.maximumRange());
        assertEquals(3, spell.targetTo());
        assertEquals(5, spell.dodgeDownModifier());
        assertEquals(280, spell.failureMessage());
        assertTrue(spell.counterMagicBypass());
        assertEquals(0, spell.effectiveMpCost(0));
        assertEquals(0, spell.effectiveMpCost(2));
        // 45 + INT20*0.40 + magic-hit3 - MR40*0.05 = 54.
        assertEquals(54, spell.probabilityForTarget(
                53, 52, 20, 3, 40, false));
        assertEquals(47, spell.probabilityForTarget(
                52, 52, 20, 3, 40, false));
        assertEquals(39, spell.probabilityForTarget(
                51, 52, 20, 3, 40, false));
        assertEquals(0, spell.probabilityForTarget(
                99, 1, 20, 3, 0, true));
        assertTrue(BattleResistFear.succeeds(54, 54));
        assertFalse(BattleResistFear.succeeds(54, 55));
        assertNull(BattleSpellCatalog.resistFearSpell(187));
    }

    @Test void invalidTargetAndProbabilityInputsFailClosed() {
        assertThrows(IllegalArgumentException.class,
                () -> BattleResistFear.resolveTarget(0, 0, 0, 0, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new BattleResistFear.Candidate(0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> BattleResistFear.succeeds(101, 1));
    }
}
