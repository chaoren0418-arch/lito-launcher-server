package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class RoiPouchCatalogTest {
    @Test
    void retainedMathRandomTimes56TableHasNoGapOrOverlap() {
        assertEquals(56, RoiPouchCatalog.coveredSlots());
        for (int roll = 0; roll < RoiPouchCatalog.ROLL_BOUND; roll++) {
            int selected = 0;
            for (RoiPouchCatalog.Outcome outcome : RoiPouchCatalog.all()) {
                if (outcome.contains(roll)) selected++;
            }
            assertEquals(1, selected, "roll " + roll);
        }
        assertThrows(IllegalArgumentException.class,
                () -> RoiPouchCatalog.outcome(-1));
        assertThrows(IllegalArgumentException.class,
                () -> RoiPouchCatalog.outcome(56));
    }

    @Test
    void exactBoundariesAndSlotWeightsMatchRoyPouchExecutor() {
        assertEquals(List.of(40308L, 40016L, 40015L, 40089L, 40088L,
                        26000L, 120317L, 40074L),
                RoiPouchCatalog.all().stream()
                        .map(RoiPouchCatalog.Outcome::itemId).toList());
        assertEquals(List.of(10, 10, 10, 10, 8, 1, 1, 6),
                RoiPouchCatalog.all().stream()
                        .map(RoiPouchCatalog.Outcome::slots).toList());
        assertEquals(1_000, RoiPouchCatalog.outcome(0).quantity());
        assertEquals(40074, RoiPouchCatalog.outcome(55).itemId());
    }

    @Test
    void selectedDatabaseAbsenceIsExplicitOnlyForTwoBrokenRareBranches() {
        for (RoiPouchCatalog.Outcome outcome : RoiPouchCatalog.all()) {
            if (outcome.selectedTemplatePresent()) {
                assertNotNull(InventoryCatalog.find(outcome.itemId()),
                        "item " + outcome.itemId());
            } else {
                assertNull(InventoryCatalog.find(outcome.itemId()),
                        "source defect item " + outcome.itemId());
            }
        }
        assertFalse(RoiPouchCatalog.outcome(48).selectedTemplatePresent());
        assertFalse(RoiPouchCatalog.outcome(49).selectedTemplatePresent());
        assertTrue(RoiPouchCatalog.outcome(50).selectedTemplatePresent());
    }
}
