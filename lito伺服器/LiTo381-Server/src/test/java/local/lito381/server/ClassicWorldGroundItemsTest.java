package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.Test;

final class ClassicWorldGroundItemsTest {
    @Test
    void mapScopedStackPreservesNativeStateAcrossPartialPickup() {
        ClassicWorldGroundItems ground = new ClassicWorldGroundItems();
        ClassicWorldDropCatalog.ItemWire adena =
                ClassicWorldDropCatalog.item(4, (int) InventoryCatalog.ADENA);
        ClassicWorldGroundItems.Item item = ground.put(
                4, adena, 10, 33051, 32338, 45009);
        assertNotNull(item);
        assertTrue(item.objectId() >= 0x5800_0000L);
        assertTrue(item.objectId() <= 0x5FFF_FFFFL);
        assertEquals(10, item.quantity());
        assertFalse(item.state().identified());
        assertEquals(adena.blessing(), item.state().blessing());
        assertEquals("$4 (10)", item.displayName());
        assertEquals(1, ground.inScreen(4, 33051, 32337).size());
        assertTrue(ground.inScreen(3, 33051, 32337).isEmpty());

        ClassicWorldGroundItems.Take partial = ground.take(
                4, item.objectId(), 4);
        assertNotNull(partial);
        assertEquals(4, partial.taken().quantity());
        assertEquals(6, partial.remaining().quantity());
        assertEquals(6, ground.item(4, item.objectId()).quantity());
        ClassicWorldGroundItems.Take rest = ground.take(
                4, item.objectId(), 6);
        assertNotNull(rest);
        assertNull(rest.remaining());
        assertNull(ground.item(4, item.objectId()));
    }

    @Test
    void nonStackableDropsAreOneObjectAndClearIsAuthoritative() {
        ClassicWorldGroundItems ground = new ClassicWorldGroundItems();
        ClassicWorldDropCatalog.ItemWire weapon =
                ClassicWorldDropCatalog.item(4, 1);
        assertNotNull(weapon);
        assertFalse(weapon.stackable());
        assertNull(ground.put(4, weapon, 2, 33051, 32338, 45009));
        ClassicWorldGroundItems.Item item = ground.put(
                4, weapon, 1, 33051, 32338, 45009);
        assertNotNull(item);
        assertEquals(1, item.quantity());
        ground.clear();
        assertNull(ground.item(4, item.objectId()));
    }

    @Test
    void petOverflowRetainsRuntimeWeaponState() {
        ClassicWorldGroundItems ground = new ClassicWorldGroundItems();
        RuntimeInventory.Item weapon = new RuntimeInventory.Item(
                0x7000_0456L, InventoryCatalog.require(InventoryCatalog.SHORT_SWORD),
                1, false, 2, 1, false, RuntimeInventory.BLESSING_CURSED);

        ClassicWorldGroundItems.Item item = ground.put(
                4, weapon, 1, 33051, 32338, 45034);
        assertNotNull(item);
        RuntimeInventory.Item restored = item.inventoryItem();
        assertEquals(1, restored.enchantLevel());
        assertEquals(2, restored.durability());
        assertFalse(restored.identified());
        assertEquals(RuntimeInventory.BLESSING_CURSED, restored.blessing());
        assertEquals(45034, item.sourceNpcId());
    }

    @Test
    void sourceTemplateBindingAndDurableBoundarySurviveWorldTransfer() throws Exception {
        ClassicWorldGroundItems ground = new ClassicWorldGroundItems();
        InventoryCatalog.Definition definition = new InventoryCatalog.Definition(
                65_001, 0, 321, 322, 1, true,
                "Source World Token", "retained source detail", 17, 9,
                true, true, BattleActionCodes.WALK, 0,
                InventoryCatalog.Effect.NONE, 87, 1_250, 3, null, null);
        RuntimeInventory.Item source = new RuntimeInventory.Item(
                0x7000_078BL, definition, 3, false, 0, 0,
                true, RuntimeInventory.BLESSING_NORMAL, true);

        assertThrows(IOException.class, () -> ground.putPersisted(
                4, source, 2, 33051, 32338, 1001,
                () -> { throw new IOException("write failed"); }));
        assertTrue(ground.inScreen(4, 33051, 32338).isEmpty());

        ClassicWorldGroundItems.Item item = ground.putPersisted(
                4, source, 2, 33051, 32338, 1001, () -> {});
        assertEquals(definition, item.state().definitionOverride());
        assertTrue(item.state().sourceBound());
        assertThrows(IOException.class, () -> ground.takePersisted(
                4, item.objectId(), 1,
                () -> { throw new IOException("write failed"); }));
        assertEquals(2, ground.item(4, item.objectId()).quantity());

        ClassicWorldGroundItems.Take take = ground.takePersisted(
                4, item.objectId(), 1, () -> {});
        assertTrue(take.taken().inventoryItem().sourceBound());
        assertTrue(take.remaining().inventoryItem().sourceBound());
        assertEquals(definition, take.taken().inventoryItem().definition());
    }

    @Test
    void tournamentRewardProvenanceSurvivesClassicWorldGroundTransfer() {
        RuntimeInventory inventory = new RuntimeInventory();
        InventoryCatalog.Definition scroll = InventoryCatalog.require(
                InventoryCatalog.POLYMORPH_SCROLL);
        assertTrue(inventory.addTournamentReward(scroll, 6));

        ClassicWorldGroundItems ground = new ClassicWorldGroundItems();
        ClassicWorldGroundItems.Item dropped = ground.put(
                4, inventory.items().get(0), 6, 33051, 32338, 45009);
        ClassicWorldGroundItems.Take take = ground.take(
                4, dropped.objectId(), 2);

        assertTrue(take.taken().state().tournamentReward());
        assertTrue(take.remaining().state().tournamentReward());
        RuntimeInventory pickedUp = new RuntimeInventory();
        assertTrue(pickedUp.addTransferred(
                take.taken().inventoryItem(), take.taken().quantity()));
        assertTrue(pickedUp.items().get(0).tournamentReward());
    }

    @Test
    void litLanternKeepsNativeLightingStateAcrossWorldDropAndPickup() {
        InventoryCatalog.Definition lantern = InventoryCatalog.require(
                InventoryCatalog.MAGIC_LANTERN);
        RuntimeInventory.Item lit = new RuntimeInventory.Item(
                0x7000_0791L, lantern, 1, true);

        ClassicWorldGroundItems ground = new ClassicWorldGroundItems();
        ClassicWorldGroundItems.Item dropped = ground.put(
                4, lit, 1, 33051, 32338, 1001);
        assertTrue(dropped.state().equipped());
        assertTrue(dropped.inventoryItem().equipped());

        RuntimeInventory pickedUp = new RuntimeInventory();
        RuntimeInventory.Item restored = ground.take(
                4, dropped.objectId(), 1).taken().inventoryItem();
        assertTrue(pickedUp.addTransferred(restored, 1));
        assertEquals(lantern.lightRange(), pickedUp.activeLightRange());
    }

    @Test
    void hotelKeyKeepsObjectIdentityAndCannotLeakAcrossInstances()
            throws Exception {
        ClassicWorldGroundItems ground = new ClassicWorldGroundItems();
        RuntimeInventory.Item key = new RuntimeInventory.Item(
                0x7000_3810L, InventoryCatalog.require(InventoryCatalog.HOTEL_KEY),
                1, false);

        ClassicWorldGroundItems.Item dropped = ground.putPersisted(
                16384, 0x7000_3810, key, 1,
                32745, 32803, 0x7000_1111, () -> {});
        assertNotNull(dropped);
        assertEquals(key.objectId(), dropped.objectId());
        assertEquals(0x7000_3810, dropped.instanceId());
        assertEquals(1, ground.inScreen(
                16384, 0x7000_3810, 32745, 32803).size());
        assertTrue(ground.inScreen(
                16384, 0x7000_3811, 32745, 32803).isEmpty());
        assertNull(ground.item(16384, 0x7000_3811, key.objectId()));

        ClassicWorldGroundItems.Take take = ground.takePersisted(
                16384, 0x7000_3810, key.objectId(), 1, () -> {});
        assertNotNull(take);
        assertEquals(key.objectId(), take.taken().inventoryItem().objectId());
        assertNull(ground.item(16384, 0x7000_3810, key.objectId()));
    }

    @Test
    void conditionalWholeTakeKeepsItemWhenRecipientRejects() {
        ClassicWorldGroundItems ground = new ClassicWorldGroundItems();
        ClassicWorldGroundItems.Item item = ground.put(
                4, ClassicWorldDropCatalog.item(
                        4, (int) InventoryCatalog.ADENA),
                9, 33051, 32338, 45009);

        assertNull(ground.takeWholeIf(
                4, -1, item.objectId(), ignored -> false));
        assertEquals(9, ground.item(4, item.objectId()).quantity());
        ClassicWorldGroundItems.Take accepted = ground.takeWholeIf(
                4, -1, item.objectId(), ignored -> true);
        assertNotNull(accepted);
        assertEquals(9, accepted.taken().quantity());
        assertNull(ground.item(4, item.objectId()));
    }
}
