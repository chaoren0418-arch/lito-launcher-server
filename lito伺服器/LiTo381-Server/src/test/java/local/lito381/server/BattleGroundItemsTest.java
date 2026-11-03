package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.Test;

final class BattleGroundItemsTest {
    @Test void ownsBattleScopedWorldIdsAndSupportsPartialStackPickup() {
        BattleGroundItems table = new BattleGroundItems();
        InventoryCatalog.Definition potion = InventoryCatalog.require(
                InventoryCatalog.HEALING_POTION);
        BattleGroundItems.Item ground = table.put(potion, 3, 32730, 32797);

        assertTrue(ground.objectId() >= 0x5000_0000L);
        assertTrue(ground.objectId() < 0x6000_0000L);
        assertEquals("Healing Potion (3)", ground.displayName());
        assertEquals(1, table.items().size());

        BattleGroundItems.Take first = table.take(ground.objectId(), 2);
        assertEquals(2, first.taken().quantity());
        assertEquals(1, first.remaining().quantity());
        assertEquals(ground.objectId(), first.remaining().objectId());
        assertEquals("Healing Potion", first.remaining().displayName());

        BattleGroundItems.Take last = table.take(ground.objectId(), 1);
        assertEquals(1, last.taken().quantity());
        assertNull(last.remaining());
        assertTrue(table.items().isEmpty());
    }

    @Test void enforcesNonStackableQuantityAndClearLifecycle() {
        BattleGroundItems table = new BattleGroundItems();
        InventoryCatalog.Definition whistle = InventoryCatalog.require(InventoryCatalog.WHISTLE);
        assertNull(table.put(whistle, 2, 100, 120));
        BattleGroundItems.Item ground = table.put(whistle, 1, 100, 120);
        assertFalse(table.items().isEmpty());
        assertEquals(1218, ground.definition().groundGfx());
        table.clear();
        assertTrue(table.items().isEmpty());
    }

    @Test void dropAndPickupKeepTheCompleteNativeItemInstance() {
        BattleGroundItems table = new BattleGroundItems();
        RuntimeInventory.Item sword = new RuntimeInventory.Item(
                0x7000_0123L, InventoryCatalog.require(InventoryCatalog.SHORT_SWORD),
                1, false, 2, 1, false, RuntimeInventory.BLESSING_CURSED);

        BattleGroundItems.Item ground = table.put(sword, 1, 32730, 32797);
        assertEquals("$100", ground.displayName());
        assertEquals(1, ground.state().enchantLevel());
        assertEquals(2, ground.state().durability());
        assertFalse(ground.state().identified());
        assertEquals(RuntimeInventory.BLESSING_CURSED, ground.state().blessing());

        BattleGroundItems.Item taken = table.take(ground.objectId(), 1).taken();
        RuntimeInventory restored = new RuntimeInventory();
        assertTrue(restored.add(taken.inventoryItem(), taken.quantity()));
        RuntimeInventory.Item item = restored.items().get(0);
        assertEquals(1, item.enchantLevel());
        assertEquals(2, item.durability());
        assertFalse(item.identified());
        assertEquals(RuntimeInventory.BLESSING_CURSED, item.blessing());
    }

    @Test void sourceTemplateAndBindingSurvivePartialDropAndPickup() {
        BattleGroundItems table = new BattleGroundItems();
        InventoryCatalog.Definition definition = sourceEtc();
        RuntimeInventory.Item source = new RuntimeInventory.Item(
                0x7000_0789L, definition, 3, false, 0, 0,
                true, RuntimeInventory.BLESSING_NORMAL, true);

        BattleGroundItems.Item ground = table.put(source, 2, 32730, 32797);
        assertEquals(definition, ground.definition());
        assertEquals(definition, ground.state().definitionOverride());
        assertTrue(ground.state().sourceBound());
        BattleGroundItems.Take take = table.take(ground.objectId(), 1);
        assertTrue(take.taken().inventoryItem().sourceBound());
        assertTrue(take.remaining().inventoryItem().sourceBound());
        assertEquals(definition, take.taken().inventoryItem().definition());
    }

    @Test void durableFailureDoesNotPublishDropOrConsumePickup() throws Exception {
        BattleGroundItems table = new BattleGroundItems();
        RuntimeInventory.Item source = new RuntimeInventory.Item(
                0x7000_078AL, sourceEtc(), 2, false, 0, 0,
                true, RuntimeInventory.BLESSING_NORMAL, true);

        assertThrows(IOException.class, () -> table.putPersisted(
                source, 1, 32730, 32797,
                () -> { throw new IOException("write failed"); }));
        assertTrue(table.items().isEmpty());

        BattleGroundItems.Item ground = table.putPersisted(
                source, 2, 32730, 32797, () -> {});
        assertThrows(IOException.class, () -> table.takePersisted(
                ground.objectId(), 1,
                () -> { throw new IOException("write failed"); }));
        assertEquals(2, table.item(ground.objectId()).quantity());
        assertEquals(1, table.takePersisted(
                ground.objectId(), 1, () -> {}).taken().quantity());
    }

    @Test void tournamentRewardProvenanceSurvivesPartialGroundTransfer() {
        RuntimeInventory inventory = new RuntimeInventory();
        InventoryCatalog.Definition potion = InventoryCatalog.require(
                InventoryCatalog.NATIVE_HEALING_POTION);
        assertTrue(inventory.addTournamentReward(potion, 30));

        BattleGroundItems table = new BattleGroundItems();
        BattleGroundItems.Item ground = table.put(
                inventory.items().get(0), 20, 32730, 32797);
        BattleGroundItems.Take take = table.take(ground.objectId(), 5);

        assertTrue(take.taken().state().tournamentReward());
        assertTrue(take.remaining().state().tournamentReward());
        RuntimeInventory pickedUp = new RuntimeInventory();
        assertTrue(pickedUp.addTransferred(
                take.taken().inventoryItem(), take.taken().quantity()));
        assertTrue(pickedUp.items().get(0).tournamentReward());
        assertEquals(5, pickedUp.sourceEquipmentOnly().catalogCounts()
                .get(potion.catalogId()));
    }

    @Test void litLanternKeepsNativeLightingStateAcrossDropAndPickup() {
        InventoryCatalog.Definition lantern = InventoryCatalog.require(
                InventoryCatalog.MAGIC_LANTERN);
        RuntimeInventory.Item lit = new RuntimeInventory.Item(
                0x7000_0790L, lantern, 1, true);

        BattleGroundItems table = new BattleGroundItems();
        BattleGroundItems.Item ground = table.put(lit, 1, 32730, 32797);
        assertTrue(ground.state().equipped());
        assertTrue(ground.inventoryItem().equipped());

        RuntimeInventory pickedUp = new RuntimeInventory();
        BattleGroundItems.Item taken = table.take(ground.objectId(), 1).taken();
        assertTrue(pickedUp.addTransferred(taken.inventoryItem(), 1));
        assertEquals(lantern.lightRange(), pickedUp.activeLightRange());
    }

    private static InventoryCatalog.Definition sourceEtc() {
        return new InventoryCatalog.Definition(
                65_001, 0, 321, 322, 1, true,
                "Source Ground Token", "retained source detail", 17, 9,
                true, true, BattleActionCodes.WALK, 0,
                InventoryCatalog.Effect.NONE, 87, 1_250, 3, null, null);
    }
}
