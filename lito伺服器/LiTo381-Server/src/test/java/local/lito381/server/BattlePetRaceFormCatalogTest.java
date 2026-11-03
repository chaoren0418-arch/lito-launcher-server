package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

final class BattlePetRaceFormCatalogTest {
    @Test
    void everyRecoveredFormHasASelectedClientWalkAnimation() {
        for (BattlePetRaceFormCatalog.Form form
                : BattlePetRaceFormCatalog.baseForms()) {
            assertEquals(BattleActionCodes.WALK,
                    BattleAnimationCatalog.get().requireWalkAction(
                            form.gfx(), BattleActionCodes.WALK));
        }
        assertEquals(17, BattlePetRaceFormCatalog.temporaryFormsList().size());
        for (BattlePetRaceFormCatalog.Form form
                : BattlePetRaceFormCatalog.temporaryFormsList()) {
            assertEquals(BattleActionCodes.WALK,
                    BattleAnimationCatalog.get().requireWalkAction(
                            form.gfx(), BattleActionCodes.WALK));
            assertDoesNotThrow(() -> Protocol.petRacePolymorph(
                    1, form.gfx(), BattleActionCodes.WALK));
        }
    }

    @Test
    void raceSelectsOneCommonBaseFormAndBothPanelFamiliesUseExactLayer8Cells() {
        BattlePetRaceCourseCatalog.State state = new BattlePetRaceCourseCatalog.State();
        state.begin(566, List.of(1L, 2L), 0, 300_000_000_000L, 17);
        assertTrue(BattlePetRaceFormCatalog.baseForms().contains(state.baseForm()));

        BattlePetRaceCourseCatalog.PanelTransition transform = state.panelMoved(
                1L, 32660, 32807, 32661, 32807, 1);
        assertEquals(BattlePetRaceCourseCatalog.PanelType.TRANSFORM,
                transform.type());
        assertEquals(BattlePetRaceFormCatalog.TRANSFORM_DURATION_SECONDS,
                transform.durationSeconds());
        assertTrue(BattlePetRaceFormCatalog.temporaryFormsList()
                .contains(transform.form()));

        BattlePetRaceCourseCatalog.PanelTransition speed = state.panelMoved(
                1L, 32670, 32806, 32671, 32806, 2);
        assertEquals(BattlePetRaceCourseCatalog.PanelType.SPEED, speed.type());
        assertEquals(BattlePetRaceFormCatalog.SPEED_DURATION_SECONDS,
                speed.durationSeconds());
        assertEquals(null, speed.form());

        assertFalse(state.panelMoved(1L, 32661, 32807,
                32661, 32807, 3).activated());
        assertFalse(state.panelMoved(1L, 32660, 32807,
                32661, 32807, 4).activated(), "four-second cooldown must block re-entry");
        assertTrue(state.panelMoved(1L, 32660, 32807,
                32661, 32807, 4_000_000_001L).activated());
    }

    @Test
    void reverseCourseHasItsIndependentRecoveredPanels() {
        BattlePetRaceCourseCatalog.State state = new BattlePetRaceCourseCatalog.State();
        state.begin(570, List.of(1L, 2L), 0, 300_000_000_000L);
        assertEquals(BattlePetRaceCourseCatalog.PanelType.TRANSFORM,
                state.panelMoved(1L, 32731, 32849,
                        32732, 32849, 1).type());
        assertEquals(BattlePetRaceCourseCatalog.PanelType.SPEED,
                state.panelMoved(2L, 32742, 32837,
                        32743, 32837, 1).type());
        assertFalse(state.panelMoved(2L, 32700, 32800,
                32701, 32800, 2).activated());
    }

    @Test
    void trailingBiasRaisesFastFormFrequencyWithoutRemovingAnyCategory() {
        int ordinaryFast = 0;
        int trailingFast = 0;
        java.util.Set<BattlePetRaceFormCatalog.Pace> trailingPaces =
                new java.util.HashSet<>();
        for (long activation = 1; activation <= 20_000; activation++) {
            BattlePetRaceFormCatalog.Form ordinary =
                    BattlePetRaceFormCatalog.temporaryForm(9, activation, false);
            BattlePetRaceFormCatalog.Form trailing =
                    BattlePetRaceFormCatalog.temporaryForm(9, activation, true);
            if (ordinary.pace() == BattlePetRaceFormCatalog.Pace.FAST) ordinaryFast++;
            if (trailing.pace() == BattlePetRaceFormCatalog.Pace.FAST) trailingFast++;
            trailingPaces.add(trailing.pace());
        }
        assertTrue(trailingFast > ordinaryFast);
        assertEquals(java.util.Set.of(
                BattlePetRaceFormCatalog.Pace.FAST,
                BattlePetRaceFormCatalog.Pace.STANDARD,
                BattlePetRaceFormCatalog.Pace.SLOW,
                BattlePetRaceFormCatalog.Pace.SPECIAL), trailingPaces);
        assertNotEquals(BattlePetRaceFormCatalog.baseForm(566, 1),
                BattlePetRaceFormCatalog.baseForm(566, 2));
    }
}
