package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

final class SpellMaterialCatalogTest {
    @Test
    void exposesOnlyConfiguredEligibleUnlearnedRows() {
        assertEquals(List.of(1, 2, 4, 5, 6, 7, 8, 9, 11, 12, 13, 14, 15),
                SpellMaterialCatalog.availableWireSkillIds(character(0, 52), Set.of(1, 4, 11)));
        assertEquals(23, SpellMaterialCatalog.availableWireSkillIds(
                character(2, 99), Set.of()).size());
        assertEquals(List.of(), SpellMaterialCatalog.availableWireSkillIds(
                character(5, 99), Set.of()));
    }

    @Test
    void reproducesSkillsItemRequirementsAndAggregatesAtomically() {
        assertEquals(List.of(
                        new SpellMaterialCatalog.Requirement(InventoryCatalog.RYUKEN_WEB, 10),
                        new SpellMaterialCatalog.Requirement(InventoryCatalog.PURE_MITHRIL, 50),
                        new SpellMaterialCatalog.Requirement(InventoryCatalog.ELVEN_DUST, 100),
                        new SpellMaterialCatalog.Requirement(InventoryCatalog.PAN_MANE, 10)),
                SpellMaterialCatalog.requirementsForWireSkillId(1));
        assertEquals(Map.of(
                        InventoryCatalog.RYUKEN_WEB, 20L,
                        InventoryCatalog.PURE_MITHRIL, 100L,
                        InventoryCatalog.ELVEN_DUST, 200L,
                        InventoryCatalog.PAN_MANE, 20L),
                SpellMaterialCatalog.totalRequirements(List.of(1, 2)));
        assertThrows(IllegalArgumentException.class,
                () -> SpellMaterialCatalog.totalRequirements(List.of(1, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> SpellMaterialCatalog.requirementsForWireSkillId(23));
    }

    @Test
    void isolatedFixtureSeedsExactlyOneConfiguredPurchase() {
        AtomicLong ids = new AtomicLong(1);
        RuntimeInventory inventory = RuntimeInventory.fromCatalogCounts(Map.of(), ids::getAndIncrement);
        assertTrue(SpellMaterialCatalog.seedFixture(inventory));
        Map<Long, Integer> counts = inventory.catalogCounts();
        assertEquals(10, counts.get(InventoryCatalog.RYUKEN_WEB));
        assertEquals(50, counts.get(InventoryCatalog.PURE_MITHRIL));
        assertEquals(100, counts.get(InventoryCatalog.ELVEN_DUST));
        assertEquals(10, counts.get(InventoryCatalog.PAN_MANE));
        for (SpellMaterialCatalog.Requirement requirement
                : SpellMaterialCatalog.requirementsForWireSkillId(1)) {
            assertTrue(inventory.removeCatalog(requirement.catalogId(), requirement.quantity()));
        }
        assertTrue(inventory.catalogCounts().isEmpty());
        assertFalse(inventory.removeCatalog(InventoryCatalog.RYUKEN_WEB, 1));
    }

    @Test
    void awakenFixtureSeedsTwoSelectedBoneFragmentsForToggleProof() {
        AtomicLong ids = new AtomicLong(1);
        RuntimeInventory inventory = RuntimeInventory.fromCatalogCounts(
                Map.of(), ids::getAndIncrement);
        assertTrue(SpellMaterialCatalog.seedAwakenAntharasFixture(inventory));
        assertEquals(2, inventory.catalogCounts().get(
                InventoryCatalog.AWAKENING_BONE_FRAGMENT));
        assertTrue(SpellMaterialCatalog.seedAwakenAntharasFixture(inventory));
        assertEquals(2, inventory.catalogCounts().get(
                InventoryCatalog.AWAKENING_BONE_FRAGMENT));
        assertTrue(inventory.removeCatalog(
                InventoryCatalog.AWAKENING_BONE_FRAGMENT, 2));
        assertTrue(inventory.catalogCounts().isEmpty());
    }

    @Test
    void valakasFixtureExtendsSelectedBoneFragmentsToFourForTwoItemCasts() {
        AtomicLong ids = new AtomicLong(1);
        RuntimeInventory inventory = RuntimeInventory.fromCatalogCounts(
                Map.of(InventoryCatalog.AWAKENING_BONE_FRAGMENT, 2),
                ids::getAndIncrement);
        assertTrue(SpellMaterialCatalog.seedAwakenValakasFixture(inventory));
        assertEquals(4, inventory.catalogCounts().get(
                InventoryCatalog.AWAKENING_BONE_FRAGMENT));
        assertTrue(SpellMaterialCatalog.seedAwakenValakasFixture(inventory));
        assertEquals(4, inventory.catalogCounts().get(
                InventoryCatalog.AWAKENING_BONE_FRAGMENT));
    }

    private static CharacterSummary character(int type, int level) {
        return new CharacterSummary("Tester", "", type, 0, level, 100, 10,
                10, 13, 10, 10, 9, 8, 11, 8, 11,
                1, 0, 0, 0, 0, 32600, 32700, 4);
    }
}
