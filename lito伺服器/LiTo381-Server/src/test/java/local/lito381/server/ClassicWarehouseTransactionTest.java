package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

final class ClassicWarehouseTransactionTest {
    @Test
    void sourceBoundCombatObjectsCannotEscapeTheirReconciledProfile() {
        RuntimeInventory inventory = new RuntimeInventory();
        InventoryCatalog.Definition weapon = InventoryCatalog.require(
                InventoryCatalog.SHORT_SWORD);
        long objectId = inventory.addDistinctState(weapon,
                new RuntimeInventory.PersistentItem(
                        0, 1, 0, 0, true, 1, true));
        assertTrue(objectId > 0);
        assertFalse(inventory.item(objectId).transferable());

        ClassicWarehouseTransaction.Outcome rejected =
                ClassicWarehouseTransaction.apply(
                        2, Map.of(objectId, 1L), 1, Set.of(objectId),
                        inventory, new RuntimeInventory());
        assertFalse(rejected.accepted());
        assertEquals("invalid-deposit-row", rejected.reason());
        assertEquals(1, rejected.inventory().item(objectId).quantity());
    }

    @Test
    void partialSourceStackCreatesAnExplicitlyLitoOwnedDerivedObject() {
        RuntimeInventory inventory = new RuntimeInventory();
        InventoryCatalog.Definition potion = InventoryCatalog.require(
                InventoryCatalog.NATIVE_HEALING_POTION);
        long sourceObjectId = inventory.addDistinctState(potion,
                new RuntimeInventory.PersistentItem(
                        0, 5, 0, 0, true, 1, true));

        ClassicWarehouseTransaction.Outcome deposited =
                ClassicWarehouseTransaction.apply(
                        2, Map.of(sourceObjectId, 2L), 1, Set.of(sourceObjectId),
                        inventory, new RuntimeInventory());

        assertTrue(deposited.accepted());
        RuntimeInventory.Item original = deposited.inventory().item(sourceObjectId);
        RuntimeInventory.Item derived = deposited.warehouse().items().get(0);
        assertEquals(3, original.quantity());
        assertFalse(original.litoOwned());
        assertTrue(derived.sourceBound());
        assertTrue(derived.litoOwned());
        assertFalse(derived.objectId() == sourceObjectId);
    }

    @Test
    void depositsThenRetrievesByNativeObjectIdAndChargesThirtyAdena() {
        RuntimeInventory inventory = inventory(Map.of(
                InventoryCatalog.ADENA, 100,
                InventoryCatalog.NATIVE_HEALING_POTION, 2), 0x7100_0000L);
        RuntimeInventory warehouse = new RuntimeInventory();
        RuntimeInventory.Item potion = item(inventory, InventoryCatalog.NATIVE_HEALING_POTION);

        ClassicWarehouseTransaction.Outcome deposit = ClassicWarehouseTransaction.apply(
                2, Map.of(potion.objectId(), 1L), 1, Set.of(potion.objectId()),
                inventory, warehouse);
        assertTrue(deposit.accepted());
        assertEquals(1, item(deposit.inventory(),
                InventoryCatalog.NATIVE_HEALING_POTION).quantity());
        RuntimeInventory.Item stored = item(deposit.warehouse(),
                InventoryCatalog.NATIVE_HEALING_POTION);
        assertEquals(1, stored.quantity());

        ClassicWarehouseTransaction.Outcome retrieve = ClassicWarehouseTransaction.apply(
                3, Map.of(stored.objectId(), 1L), 1, Set.of(stored.objectId()),
                deposit.inventory(), deposit.warehouse());
        assertTrue(retrieve.accepted());
        assertEquals(2, item(retrieve.inventory(),
                InventoryCatalog.NATIVE_HEALING_POTION).quantity());
        assertEquals(70, item(retrieve.inventory(), InventoryCatalog.ADENA).quantity());
        assertEquals(0, retrieve.warehouse().size());
    }

    @Test
    void depositAndRetrievePreserveTheExactNativeWeaponInstance() {
        RuntimeInventory inventory = inventory(Map.of(
                InventoryCatalog.ADENA, 100,
                InventoryCatalog.SHORT_SWORD, 1,
                InventoryCatalog.MAGIC_GEM, 10), 0x7110_0000L);
        RuntimeInventory.Item sword = item(inventory, InventoryCatalog.SHORT_SWORD);
        assertTrue(inventory.createMagicalWeapon(sword.objectId()));
        assertTrue(inventory.setBlessing(
                sword.objectId(), RuntimeInventory.BLESSING_CURSED));
        sword = inventory.item(sword.objectId());

        ClassicWarehouseTransaction.Outcome deposited = ClassicWarehouseTransaction.apply(
                2, Map.of(sword.objectId(), 1L), 1, Set.of(sword.objectId()),
                inventory, new RuntimeInventory());
        assertTrue(deposited.accepted());
        RuntimeInventory.Item stored = item(deposited.warehouse(), InventoryCatalog.SHORT_SWORD);
        assertEquals(1, stored.enchantLevel());
        assertEquals(RuntimeInventory.BLESSING_CURSED, stored.blessing());
        assertTrue(stored.identified());

        ClassicWarehouseTransaction.Outcome retrieved = ClassicWarehouseTransaction.apply(
                3, Map.of(stored.objectId(), 1L), 1, Set.of(stored.objectId()),
                deposited.inventory(), deposited.warehouse());
        assertTrue(retrieved.accepted());
        RuntimeInventory.Item returned = item(retrieved.inventory(), InventoryCatalog.SHORT_SWORD);
        assertEquals(1, returned.enchantLevel());
        assertEquals(RuntimeInventory.BLESSING_CURSED, returned.blessing());
        assertTrue(returned.identified());
    }

    @Test
    void rejectsUnofferedUntradableEquippedDamagedAndUnfundedMovesAtomically() {
        RuntimeInventory inventory = inventory(Map.of(
                InventoryCatalog.ADENA, 29,
                InventoryCatalog.SHORT_SWORD, 1,
                0x00FF_FFF0L, 1), 0x7200_0000L);
        RuntimeInventory warehouse = inventory(
                Map.of(InventoryCatalog.NATIVE_HEALING_POTION, 1), 0x7300_0000L);
        RuntimeInventory.Item sword = item(inventory, InventoryCatalog.SHORT_SWORD);
        RuntimeInventory.Item unknown = item(inventory, 0x00FF_FFF0L);
        RuntimeInventory.Item stored = item(warehouse, InventoryCatalog.NATIVE_HEALING_POTION);

        assertFalse(ClassicWarehouseTransaction.apply(
                2, Map.of(sword.objectId(), 1L), 1, Set.of(), inventory, warehouse)
                .accepted());
        assertFalse(ClassicWarehouseTransaction.apply(
                2, Map.of(unknown.objectId(), 1L), 1, Set.of(unknown.objectId()),
                inventory, warehouse).accepted());

        RuntimeInventory equipped = inventory.copy();
        equipped.toggleWeapon(sword.objectId());
        assertFalse(ClassicWarehouseTransaction.apply(
                2, Map.of(sword.objectId(), 1L), 1, Set.of(sword.objectId()),
                equipped, warehouse).accepted());

        RuntimeInventory damaged = inventory.copy();
        damaged.damageWeapon(sword.objectId(), 1);
        assertFalse(ClassicWarehouseTransaction.apply(
                2, Map.of(sword.objectId(), 1L), 1, Set.of(sword.objectId()),
                damaged, warehouse).accepted());

        ClassicWarehouseTransaction.Outcome unfunded = ClassicWarehouseTransaction.apply(
                3, Map.of(stored.objectId(), 1L), 1, Set.of(stored.objectId()),
                inventory, warehouse);
        assertFalse(unfunded.accepted());
        assertEquals("insufficient-adena", unfunded.reason());
        assertEquals(29, item(unfunded.inventory(), InventoryCatalog.ADENA).quantity());
        assertEquals(1, item(unfunded.warehouse(),
                InventoryCatalog.NATIVE_HEALING_POTION).quantity());
    }

    @Test
    void supportsPledgeAndElvenModesWithTheirNativeCurrencies() {
        RuntimeInventory pledgeInventory = inventory(Map.of(
                InventoryCatalog.ADENA, 100,
                InventoryCatalog.NATIVE_HEALING_POTION, 1), 0x7400_0000L);
        RuntimeInventory.Item pledgePotion = item(
                pledgeInventory, InventoryCatalog.NATIVE_HEALING_POTION);
        ClassicWarehouseTransaction.Outcome pledgeDeposit =
                ClassicWarehouseTransaction.apply(
                        4, Map.of(pledgePotion.objectId(), 1L), 1,
                        Set.of(pledgePotion.objectId()), pledgeInventory,
                        new RuntimeInventory());
        RuntimeInventory.Item pledged = item(
                pledgeDeposit.warehouse(), InventoryCatalog.NATIVE_HEALING_POTION);
        ClassicWarehouseTransaction.Outcome pledgeRetrieve =
                ClassicWarehouseTransaction.apply(
                        5, Map.of(pledged.objectId(), 1L), 1,
                        Set.of(pledged.objectId()), pledgeDeposit.inventory(),
                        pledgeDeposit.warehouse());
        assertTrue(pledgeRetrieve.accepted());
        assertEquals(70, item(pledgeRetrieve.inventory(), InventoryCatalog.ADENA).quantity());

        RuntimeInventory elfInventory = inventory(Map.of(
                InventoryCatalog.PURE_MITHRIL, 4,
                InventoryCatalog.NATIVE_HEALING_POTION, 1), 0x7500_0000L);
        RuntimeInventory.Item elfPotion = item(
                elfInventory, InventoryCatalog.NATIVE_HEALING_POTION);
        ClassicWarehouseTransaction.Outcome elfDeposit =
                ClassicWarehouseTransaction.apply(
                        8, Map.of(elfPotion.objectId(), 1L), 1,
                        Set.of(elfPotion.objectId()), elfInventory,
                        new RuntimeInventory());
        RuntimeInventory.Item elven = item(
                elfDeposit.warehouse(), InventoryCatalog.NATIVE_HEALING_POTION);
        ClassicWarehouseTransaction.Outcome elfRetrieve =
                ClassicWarehouseTransaction.apply(
                        9, Map.of(elven.objectId(), 1L), 1,
                        Set.of(elven.objectId()), elfDeposit.inventory(),
                        elfDeposit.warehouse());
        assertTrue(elfRetrieve.accepted());
        assertEquals(2, item(elfRetrieve.inventory(), InventoryCatalog.PURE_MITHRIL).quantity());
    }

    @Test
    void rejectsRetrievalWhenNativeWeightAdmissionFailsWithoutMutatingEitherSide() {
        RuntimeInventory inventory = inventory(Map.of(InventoryCatalog.ADENA, 100),
                0x7600_0000L);
        RuntimeInventory warehouse = inventory(Map.of(InventoryCatalog.SHORT_SWORD, 1),
                0x7700_0000L);
        RuntimeInventory.Item stored = item(warehouse, InventoryCatalog.SHORT_SWORD);

        ClassicWarehouseTransaction.Outcome outcome = ClassicWarehouseTransaction.apply(
                3, Map.of(stored.objectId(), 1L), 1, Set.of(stored.objectId()),
                inventory, warehouse, (current, definition, quantity) -> false);

        assertFalse(outcome.accepted());
        assertEquals("inventory-weight", outcome.reason());
        assertEquals(100, item(outcome.inventory(), InventoryCatalog.ADENA).quantity());
        assertEquals(1, item(outcome.warehouse(), InventoryCatalog.SHORT_SWORD).quantity());
    }

    private static RuntimeInventory inventory(Map<Long, Integer> counts, long firstObjectId) {
        AtomicLong ids = new AtomicLong(firstObjectId);
        return RuntimeInventory.fromCatalogCounts(counts, ids::getAndIncrement);
    }

    private static RuntimeInventory.Item item(RuntimeInventory inventory, long catalogId) {
        return inventory.items().stream()
                .filter(candidate -> candidate.definition().catalogId() == catalogId)
                .findFirst().orElseThrow();
    }
}
