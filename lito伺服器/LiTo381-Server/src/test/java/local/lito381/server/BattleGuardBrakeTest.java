package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class BattleGuardBrakeTest {
    @Test void selectorOnlyTargetRecoveryUsesLastExplicitThenUniqueRangeThree() {
        List<BattleGuardBrake.Candidate> candidates = List.of(
                new BattleGuardBrake.Candidate(15, 103, 120),
                new BattleGuardBrake.Candidate(16, 100, 123),
                new BattleGuardBrake.Candidate(17, 104, 120));
        assertEquals(16, BattleGuardBrake.resolveTarget(
                16, 100, 120, 3, candidates));
        assertEquals(0, BattleGuardBrake.resolveTarget(
                0, 100, 120, 3, candidates));
        assertEquals(15, BattleGuardBrake.resolveTarget(
                0, 100, 120, 3, List.of(candidates.get(0))));
        assertEquals(0, BattleGuardBrake.resolveTarget(
                17, 100, 120, 3, candidates));
    }

    @Test void catalogPinsExactSelectedRowAndConfiguredProbabilityFormula() {
        BattleSpellCatalog.GuardBrakeSpell spell =
                BattleSpellCatalog.guardBrakeSpell(BattleSpellCatalog.GUARD_BRAKE);
        assertEquals(183, spell.skillId());
        assertEquals(22, spell.row());
        assertEquals(6, spell.column());
        assertEquals(23, spell.level());
        assertEquals(6, spell.number());
        assertEquals(19, spell.actionId());
        assertEquals(6_508, spell.effectGfx());
        assertEquals(0, spell.mpCost());
        assertEquals(25, spell.hpCost());
        assertEquals(1_000, spell.reuseDelayMillis());
        assertEquals(1_800, spell.clientCastClockMillis());
        assertEquals(60, spell.durationSeconds());
        assertEquals(3, spell.maximumRange());
        assertEquals(3, spell.targetTo());
        assertEquals(15, spell.armorClassModifier());
        assertEquals(280, spell.failureMessage());
        assertFalse(spell.counterMagicBypass());
        assertEquals(0, spell.effectiveMpCost(0));
        assertEquals(0, spell.effectiveMpCost(2));
        // 55 + INT20*0.35 + magic-hit3 - MR40*0.05 = 63.
        assertEquals(63, spell.probabilityForTarget(
                53, 52, 20, 3, 40, false));
        assertEquals(53, spell.probabilityForTarget(
                52, 52, 20, 3, 40, false));
        assertEquals(43, spell.probabilityForTarget(
                51, 52, 20, 3, 40, false));
        assertEquals(0, spell.probabilityForTarget(
                99, 1, 20, 3, 0, true));
        assertTrue(BattleGuardBrake.succeeds(63, 63));
        assertFalse(BattleGuardBrake.succeeds(63, 64));
        assertNull(BattleSpellCatalog.guardBrakeSpell(182));
    }

    @Test void invalidTargetAndProbabilityInputsFailClosed() {
        assertThrows(IllegalArgumentException.class,
                () -> BattleGuardBrake.resolveTarget(
                        0, 0, 0, 0, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new BattleGuardBrake.Candidate(0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> BattleGuardBrake.succeeds(101, 1));
    }

    @Test void horrorOfDeathSharesProbabilityFamilyButPinsItsOwnNativeRow() {
        BattleSpellCatalog.GuardBrakeSpell spell =
                BattleSpellCatalog.guardBrakeSpell(
                        BattleSpellCatalog.HORROR_OF_DEATH);
        assertEquals(193, spell.skillId());
        assertEquals(24, spell.row());
        assertEquals(0, spell.column());
        assertEquals(25, spell.level());
        assertEquals(0, spell.number());
        assertEquals(19, spell.actionId());
        assertEquals(6_588, spell.effectGfx());
        assertEquals(0, spell.mpCost());
        assertEquals(50, spell.hpCost());
        assertEquals(1_000, spell.reuseDelayMillis());
        assertEquals(0, spell.clientCastClockMillis());
        assertEquals(60, spell.durationSeconds());
        assertEquals(3, spell.maximumRange());
        assertEquals(3, spell.targetTo());
        assertEquals(1, spell.nativeId());
        assertEquals(0, spell.armorClassModifier());
        assertTrue(spell.horrorOfDeath());
        assertEquals(-5, spell.strengthModifier());
        assertEquals(-5, spell.intelligenceModifier());
        assertFalse(spell.counterMagicBypass());
        // Same selected negative-control row as Guard Brake.
        assertEquals(53, spell.probabilityForTarget(
                52, 52, 20, 3, 40, false));
        assertEquals(0, spell.probabilityForTarget(
                99, 1, 20, 3, 0, true));
    }
}
