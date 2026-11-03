package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class RentalTownHotelRegistryTest {
    @Test
    void nativeKeysRemainLeaseCredentialsAcrossPublicGroundTransfer() {
        RentalTownHotelRegistry registry = new RentalTownHotelRegistry();
        RuntimeInventory owner = new RuntimeInventory();
        InventoryCatalog.Definition definition = InventoryCatalog.require(
                NativeLitoRentalHotelCatalog.KEY_ITEM_ID);
        assertTrue(owner.add(definition, 3));
        List<Long> objectIds = owner.items().stream()
                .map(RuntimeInventory.Item::objectId).toList();
        assertEquals(3, objectIds.size());
        assertEquals(3, Set.copyOf(objectIds).size());
        assertTrue(owner.items().stream().allMatch(item ->
                item.quantity() == 1 && item.definition().useType() == 0xFF
                        && item.definition().icon() == 59
                        && item.definition().groundGfx() == 151));

        long now = System.nanoTime();
        NativeLitoRentalHotelCatalog.Room room =
                NativeLitoRentalHotelCatalog.forKeyCount(3);
        RentalTownHotelRegistry.Lease lease = registry.register(
                71, 0x6000_0001L, 3, room, now + 1_000_000_000L,
                objectIds);
        assertNotNull(lease);
        assertTrue(registry.inventoryOwnsCredential(
                lease.token(), owner.items(), now));

        RuntimeInventory.Item shared = owner.item(objectIds.get(1));
        RentalTownHotelRegistry.GroundItem ground = registry.putGround(
                0x5000_0001L, 500, 0, shared, 32780, 32830, now);
        assertNotNull(ground);
        assertEquals(shared.objectId(), ground.objectId());
        assertTrue(owner.remove(shared.objectId(), 1));
        assertEquals(ground, registry.groundItem(
                0x5000_0001L, 500, 0, ground.objectId()));

        RentalTownHotelRegistry.GroundItem taken = registry.takeGround(
                0x5000_0001L, 500, 0, ground.objectId());
        RuntimeInventory guest = new RuntimeInventory();
        assertTrue(guest.addTransferred(taken.inventoryItem(), 1));
        assertEquals(shared.objectId(), guest.items().get(0).objectId());
        assertEquals(lease.token(), registry.leaseForInventory(
                guest.items(), now).token());
        assertFalse(registry.inventoryOwnsCredential(
                lease.token(), owner.items().stream()
                        .filter(item -> item.objectId() == shared.objectId()).toList(),
                now));
    }

    @Test
    void privateGroundScopeAndInvalidationAreLeaseIsolated() {
        RentalTownHotelRegistry registry = new RentalTownHotelRegistry();
        RuntimeInventory inventory = new RuntimeInventory();
        assertTrue(inventory.add(InventoryCatalog.require(
                NativeLitoRentalHotelCatalog.KEY_ITEM_ID), 1));
        RuntimeInventory.Item key = inventory.items().get(0);
        long now = System.nanoTime();
        NativeLitoRentalHotelCatalog.Room room =
                NativeLitoRentalHotelCatalog.forKeyCount(1);
        RentalTownHotelRegistry.Lease lease = registry.register(
                81, 0x6000_0002L, 1, room, now + 1_000_000_000L,
                List.of(key.objectId()));
        assertNotNull(lease);
        assertNull(registry.putGround(0x5000_0001L, room.mapId(),
                lease.token() + 1, key, room.entryX(), room.entryY(), now));
        RentalTownHotelRegistry.GroundItem ground = registry.putGround(
                0x5000_0001L, room.mapId(), lease.token(), key,
                room.entryX(), room.entryY(), now);
        assertNotNull(ground);
        assertTrue(registry.groundItems(0x5000_0001L, room.mapId(),
                lease.token() + 1).isEmpty());

        RentalTownHotelRegistry.Invalidation invalidation =
                registry.invalidate(lease.token());
        assertNotNull(invalidation);
        assertEquals(Set.of(key.objectId()), invalidation.keyObjectIds());
        assertEquals(List.of(ground), invalidation.removedGroundItems());
        assertNull(registry.leaseForKey(key.objectId(), now));
        assertTrue(registry.groundItems(0x5000_0001L, room.mapId(),
                lease.token()).isEmpty());
        assertNull(registry.invalidate(lease.token()));
    }
}
