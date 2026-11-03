package local.lito381.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BattleInventoryWeightTest {
    @Test void reproducesItemAndInventorySaturationRules() {
        assertEquals(0, BattleInventoryWeight.itemWeight(1, 0));
        assertEquals(1, BattleInventoryWeight.itemWeight(1, 630));
        assertEquals(1, BattleInventoryWeight.itemWeight(2, 630));
        assertEquals(1260, BattleInventoryWeight.itemWeight(2_000, 630));
        assertEquals(Integer.MAX_VALUE,
                BattleInventoryWeight.itemWeight(Long.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE,
                BattleInventoryWeight.addWeight(Integer.MAX_VALUE - 2, 3));
    }

    @Test void reproducesMaxWeightOrderingAndWeight240Rounding() {
        BattleInventoryWeight.Profile ordinary = new BattleInventoryWeight.Profile(
                900, 1.0, 0, 13, 10, 13, 10, 0, 0, false);
        assertEquals(1800.0, BattleInventoryWeight.maxWeight(ordinary));
        assertEquals(120, BattleInventoryWeight.weight240(ordinary));

        BattleInventoryWeight.Profile reduced = new BattleInventoryWeight.Profile(
                1_166, 1.0, 0, 13, 10, 14, 11, 20, 0, true);
        // base 1800 * (1 + .20 + .08) + decrease-weight 180 = 2484
        assertEquals(2484.0, BattleInventoryWeight.maxWeight(reduced));
        assertEquals(113, BattleInventoryWeight.weight240(reduced));

        BattleInventoryWeight.Profile routed = new BattleInventoryWeight.Profile(
                90_000, 100.0, 0, 13, 10, 13, 10, 0, 0, false);
        assertEquals(180_000.0, BattleInventoryWeight.maxWeight(routed));
        assertEquals(120, BattleInventoryWeight.weight240(routed));
    }

    @Test void reproducesCheckAddItemProjectionAndRejectsStage240Equality() {
        BattleInventoryWeight.Profile belowBoundary = new BattleInventoryWeight.Profile(
                1_796, 1.0, 0, 13, 10, 13, 10, 0, 0, false);
        assertEquals(1_797,
                BattleInventoryWeight.projectedAddRawWeight(1_796, 1, 0));
        assertFalse(BattleInventoryWeight.permitsAdd(belowBoundary, 1, 0));

        BattleInventoryWeight.Profile safe = new BattleInventoryWeight.Profile(
                1_790, 1.0, 0, 13, 10, 13, 10, 0, 0, false);
        assertTrue(BattleInventoryWeight.permitsAdd(safe, 1, 0));
        assertEquals(1_821,
                BattleInventoryWeight.projectedAddRawWeight(1_790, 1, 30_000));
        assertFalse(BattleInventoryWeight.permitsAdd(safe, 1, 30_000));

        assertEquals(Integer.MAX_VALUE, BattleInventoryWeight.projectedAddRawWeight(
                1, Long.MAX_VALUE, Integer.MAX_VALUE));
        assertFalse(BattleInventoryWeight.permitsAdd(safe,
                Long.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test void keepsTheNativeWholeShopOrderBoundaryDistinctFromCheckAddItem() {
        BattleInventoryWeight.Profile nearMaximum = new BattleInventoryWeight.Profile(
                1_797, 1.0, 0, 13, 10, 13, 10, 0, 0, false);
        // checkAddItem adds one raw unit and rounds 1798/1800 to stage 240.
        assertFalse(BattleInventoryWeight.permitsAdd(nearMaximum, 1, 0));
        // L1Shop accepts an exact 3-unit order at equality and rejects only >.
        assertTrue(BattleInventoryWeight.permitsShopOrder(nearMaximum, 3_000));
        assertFalse(BattleInventoryWeight.permitsShopOrder(nearMaximum, 3_001));
        assertFalse(BattleInventoryWeight.permitsShopOrder(
                nearMaximum, Long.MAX_VALUE));
    }

    @Test void enforcesTheNative120AndBuffed197RegenerationBoundaries() {
        assertTrue(BattleInventoryWeight.permitsNaturalHpRegen(119, false));
        assertFalse(BattleInventoryWeight.permitsNaturalHpRegen(120, false));
        assertTrue(BattleInventoryWeight.permitsNaturalHpRegen(120, true));
        assertTrue(BattleInventoryWeight.permitsNaturalHpRegen(196, true));
        assertFalse(BattleInventoryWeight.permitsNaturalHpRegen(197, true));
    }

    @Test void reproducesOriginalClassStrengthAndConstitutionReductions() {
        assertEquals(0, BattleInventoryWeight.originalStrengthReduction(0, 13));
        assertEquals(1, BattleInventoryWeight.originalStrengthReduction(0, 14));
        assertEquals(2, BattleInventoryWeight.originalStrengthReduction(0, 17));
        assertEquals(3, BattleInventoryWeight.originalStrengthReduction(0, 20));
        assertEquals(0, BattleInventoryWeight.originalStrengthReduction(1, 30));
        assertEquals(2, BattleInventoryWeight.originalStrengthReduction(2, 16));
        assertEquals(1, BattleInventoryWeight.originalStrengthReduction(3, 10));
        assertEquals(2, BattleInventoryWeight.originalStrengthReduction(4, 13));
        assertEquals(3, BattleInventoryWeight.originalStrengthReduction(4, 16));
        assertEquals(1, BattleInventoryWeight.originalStrengthReduction(5, 16));
        assertEquals(1, BattleInventoryWeight.originalStrengthReduction(6, 18));

        assertEquals(1, BattleInventoryWeight.originalConstitutionReduction(0, 11));
        assertEquals(1, BattleInventoryWeight.originalConstitutionReduction(1, 15));
        assertEquals(2, BattleInventoryWeight.originalConstitutionReduction(2, 15));
        assertEquals(1, BattleInventoryWeight.originalConstitutionReduction(3, 13));
        assertEquals(2, BattleInventoryWeight.originalConstitutionReduction(3, 15));
        assertEquals(1, BattleInventoryWeight.originalConstitutionReduction(4, 9));
        assertEquals(0, BattleInventoryWeight.originalConstitutionReduction(5, 30));
        assertEquals(1, BattleInventoryWeight.originalConstitutionReduction(6, 17));
        assertEquals(2, BattleInventoryWeight.originalConstitutionReduction(6, 18));
    }

    @Test void runtimeInventoryUsesTheSamePerItemWeightFormula() {
        RuntimeInventory inventory = new RuntimeInventory();
        assertTrue(inventory.add(
                InventoryCatalog.require(InventoryCatalog.MAGIC_GEM), 1_000));
        assertTrue(inventory.add(InventoryCatalog.require(InventoryCatalog.SHORT_SWORD), 2));
        assertEquals(2_060, inventory.totalWeight());
    }
}
