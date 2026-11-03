package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

final class RentalLoadoutPlanTest {
    @Test void allThirteenNativeSetsResolveToClassCompatibleEquipmentOrAuxiliaryRows() {
        assertEquals(13, RentalItemSetCatalog.all().size());
        Set<Long> incompleteEquipmentDefinitions = new TreeSet<>();
        for (RentalItemSetCatalog.Set set : RentalItemSetCatalog.all()) {
            for (RentalItemSetCatalog.Item item : set.items()) {
                RentalItemDefinitionCatalog.SourceTemplate exact =
                        RentalItemDefinitionCatalog.source(
                                Math.toIntExact(item.catalogId()));
                InventoryCatalog.Definition definition = exact == null
                        ? item.definitionOrNull() : exact.definition();
                if (!"輔助".equals(item.slot()) && definition != null
                        && !definition.weapon() && !definition.sourceArmor()) {
                    incompleteEquipmentDefinitions.add(item.catalogId());
                }
            }
        }
        assertEquals(Set.of(), incompleteEquipmentDefinitions);
        for (RentalItemSetCatalog.Set set : RentalItemSetCatalog.all()) {
            RentalLoadoutPlan plan = RentalLoadoutPlan.require(
                    set.key().characterClass(), set.key().selection());
            assertEquals(set.items().size(), plan.entries().size());
            for (RentalLoadoutPlan.Entry entry : plan.entries()) {
                assertEquals(entry.source().catalogId(), entry.definition().catalogId());
                if (entry.equipmentSlot()) {
                    assertTrue(entry.definition().weapon()
                            || entry.definition().sourceArmor());
                    int classMask = entry.definition().weapon()
                            ? entry.definition().effectiveWeaponStatus().classMask()
                            : entry.definition().sourceArmorStatus().classMask();
                    assertTrue((classMask & (1 << set.key().characterClass())) != 0,
                            () -> "class mask rejected " + entry.source().catalogId()
                                    + " for class " + set.key().characterClass());
                } else {
                    assertEquals("輔助", entry.source().slot());
                }
            }
        }
    }

    @Test void stagesExactCountsAsSessionOwnedObjectsWithoutAnImplicitEquipDecision() {
        for (RentalItemSetCatalog.Set set : RentalItemSetCatalog.all()) {
            RentalLoadoutPlan plan = RentalLoadoutPlan.require(
                    set.key().characterClass(), set.key().selection());
            AtomicLong ids = new AtomicLong(0x7100_0000L);
            RentalLoadoutPlan.Staged staged = plan.stage(
                    RentalLoadoutPlan.StatePolicy.IDENTIFIED_UNEQUIPPED,
                    ids::getAndIncrement);
            assertEquals(plan.expectedCatalogCounts(), staged.inventory().catalogCounts());
            assertEquals(set.items().size(), staged.tableRowObjectIds().size());
            for (RuntimeInventory.Item item : staged.inventory().items()) {
                assertTrue(item.identified());
                assertFalse(item.equipped());
                assertFalse(item.sourceBound());
                assertFalse(item.tournamentReward());
            }
        }
    }

    @Test void tableSlotPolicyPassesNativeDependencyAndTwoRingTopologyForEverySet() {
        for (RentalItemSetCatalog.Set set : RentalItemSetCatalog.all()) {
            RentalLoadoutPlan plan = RentalLoadoutPlan.require(
                    set.key().characterClass(), set.key().selection());
            AtomicLong ids = new AtomicLong(0x7200_0000L);
            RentalLoadoutPlan.Staged staged = plan.stage(
                    RentalLoadoutPlan.StatePolicy.IDENTIFIED_TABLE_SLOTS,
                    ids::getAndIncrement);
            long expectedEquipped = plan.entries().stream()
                    .filter(RentalLoadoutPlan.Entry::equipmentSlot)
                    .mapToLong(entry -> entry.source().quantity())
                    .sum();
            assertEquals(expectedEquipped, staged.inventory().items().stream()
                    .filter(RuntimeInventory.Item::equipped).count());
            assertEquals(1, staged.inventory().items().stream()
                    .filter(RuntimeInventory.Item::equipped)
                    .filter(item -> item.definition().weapon()).count());
            assertTrue(staged.inventory().equippedRingCount() <= 2);
            for (RentalLoadoutPlan.Entry entry : plan.entries()) {
                if (!entry.equipmentSlot()) {
                    assertTrue(staged.inventory().items().stream()
                            .filter(item -> item.definition().catalogId()
                                    == entry.source().catalogId())
                            .noneMatch(RuntimeInventory.Item::equipped));
                }
            }
        }
    }
}
