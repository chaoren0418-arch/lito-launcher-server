package local.lito381.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runtime-only LiTo rental-hotel leases, credentials and ground transfers.
 *
 * <p>The selected LiTo resources make the room free and explicitly say that
 * several keys may be handed to friends.  The retained 3.81 inn implementation
 * identifies every key by its runtime object id and carries the lease identity
 * on that object.  This registry preserves that capability across a native
 * drop/pickup without importing the classic inn's price or four-hour lease.</p>
 */
final class RentalTownHotelRegistry {
    record Lease(
            long token, long ownerObjectId, int keyCount,
            NativeLitoRentalHotelCatalog.Room room, long expiresAtNanos,
            Set<Long> keyObjectIds) {
        Lease {
            keyObjectIds = Set.copyOf(keyObjectIds);
            if (token <= 0 || ownerObjectId <= 0 || keyCount <= 0
                    || room == null || expiresAtNanos <= 0
                    || keyObjectIds.size() != keyCount
                    || keyObjectIds.stream().anyMatch(id -> id == null || id <= 0)) {
                throw new IllegalArgumentException("invalid rental-hotel lease");
            }
        }

        boolean active(long nowNanos) {
            return nowNanos < expiresAtNanos;
        }

        boolean owner(long playerObjectId) {
            return ownerObjectId == playerObjectId;
        }
    }

    private record Scope(long roomId, int mapId, long instanceToken) {
        Scope {
            if (roomId <= 0 || mapId < 0 || instanceToken < 0) {
                throw new IllegalArgumentException("invalid rental-hotel ground scope");
            }
        }
    }

    record GroundItem(
            long roomId, int mapId, long instanceToken, long objectId,
            InventoryCatalog.Definition definition,
            RuntimeInventory.PersistentItem state, int x, int y) {
        GroundItem {
            if (roomId <= 0 || mapId < 0 || instanceToken < 0
                    || objectId <= 0 || objectId > 0xFFFF_FFFFL
                    || definition == null
                    || definition.catalogId() != ClassicWorldHotelCatalog.KEY_ITEM_ID
                    || definition.groundGfx() <= 0 || state == null
                    || state.quantity() != 1 || x < 0 || x > 0xFFFF
                    || y < 0 || y > 0xFFFF) {
                throw new IllegalArgumentException("invalid rental-hotel ground key");
            }
        }

        RuntimeInventory.Item inventoryItem() {
            return new RuntimeInventory.Item(
                    objectId, definition, 1, false, state.durability(),
                    state.enchantLevel(), state.identified(), state.blessing(),
                    state.sourceBound(), state.tournamentReward(),
                    state.chargeCount(), state.expiresAtEpochMillis(), state.sealed());
        }

        String displayName() {
            return inventoryItem().nativeViewName();
        }
    }

    record Invalidation(Lease lease, Set<Long> keyObjectIds,
                        List<GroundItem> removedGroundItems) {
        Invalidation {
            if (lease == null) throw new IllegalArgumentException("missing invalidated lease");
            keyObjectIds = Set.copyOf(keyObjectIds);
            removedGroundItems = List.copyOf(removedGroundItems);
        }
    }

    private final Map<Long, Lease> leases = new LinkedHashMap<>();
    private final Map<Long, Long> leaseByKeyObject = new LinkedHashMap<>();
    private final Map<Scope, LinkedHashMap<Long, GroundItem>> groundByScope =
            new LinkedHashMap<>();

    synchronized Lease register(
            long token, long ownerObjectId, int keyCount,
            NativeLitoRentalHotelCatalog.Room room, long expiresAtNanos,
            Collection<Long> keyObjectIds) {
        Lease lease = new Lease(token, ownerObjectId, keyCount, room,
                expiresAtNanos, new LinkedHashSet<>(keyObjectIds));
        if (leases.containsKey(token)
                || lease.keyObjectIds().stream().anyMatch(leaseByKeyObject::containsKey)) {
            return null;
        }
        leases.put(token, lease);
        for (long objectId : lease.keyObjectIds()) {
            leaseByKeyObject.put(objectId, token);
        }
        return lease;
    }

    synchronized Lease lease(long token, long nowNanos) {
        Lease lease = leases.get(token);
        return lease != null && lease.active(nowNanos) ? lease : null;
    }

    synchronized Lease leaseForKey(long objectId, long nowNanos) {
        Long token = leaseByKeyObject.get(objectId);
        return token == null ? null : lease(token, nowNanos);
    }

    synchronized Lease leaseForInventory(
            Collection<RuntimeInventory.Item> items, long nowNanos) {
        Lease found = null;
        for (RuntimeInventory.Item item : items) {
            if (item.definition().catalogId() != ClassicWorldHotelCatalog.KEY_ITEM_ID) {
                continue;
            }
            Lease lease = leaseForKey(item.objectId(), nowNanos);
            if (lease == null) continue;
            if (found != null && found.token() != lease.token()) return null;
            found = lease;
        }
        return found;
    }

    synchronized boolean inventoryOwnsCredential(
            long token, Collection<RuntimeInventory.Item> items, long nowNanos) {
        Lease lease = lease(token, nowNanos);
        if (lease == null) return false;
        return items.stream().anyMatch(item ->
                item.definition().catalogId() == ClassicWorldHotelCatalog.KEY_ITEM_ID
                        && lease.keyObjectIds().contains(item.objectId()));
    }

    synchronized GroundItem putGround(
            long roomId, int mapId, long instanceToken,
            RuntimeInventory.Item source, int x, int y, long nowNanos) {
        if (source == null || source.quantity() != 1
                || source.definition().catalogId()
                    != ClassicWorldHotelCatalog.KEY_ITEM_ID) return null;
        Lease lease = leaseForKey(source.objectId(), nowNanos);
        if (lease == null || (NativeLitoRentalHotelCatalog.supportsMap(mapId)
                && instanceToken != lease.token())
                || (!NativeLitoRentalHotelCatalog.supportsMap(mapId)
                    && instanceToken != 0)
                || containsGroundObject(source.objectId())) {
            return null;
        }
        Scope scope = new Scope(roomId, mapId, instanceToken);
        RuntimeInventory.PersistentItem state = new RuntimeInventory.PersistentItem(
                source.objectId(), 1, source.enchantLevel(), source.durability(),
                source.identified(), source.blessing(), source.sourceBound(),
                source.tournamentReward(), false,
                source.persistentItem().definitionOverride(), source.chargeCount(),
                source.expiresAtEpochMillis(), source.sealed());
        GroundItem item = new GroundItem(roomId, mapId, instanceToken,
                source.objectId(), source.definition(), state, x, y);
        groundByScope.computeIfAbsent(scope, ignored -> new LinkedHashMap<>())
                .put(item.objectId(), item);
        return item;
    }

    synchronized GroundItem groundItem(
            long roomId, int mapId, long instanceToken, long objectId) {
        Map<Long, GroundItem> items = groundByScope.get(
                new Scope(roomId, mapId, instanceToken));
        return items == null ? null : items.get(objectId);
    }

    synchronized List<GroundItem> groundItems(
            long roomId, int mapId, long instanceToken) {
        Map<Long, GroundItem> items = groundByScope.get(
                new Scope(roomId, mapId, instanceToken));
        return items == null ? List.of() : List.copyOf(items.values());
    }

    synchronized GroundItem takeGround(
            long roomId, int mapId, long instanceToken, long objectId) {
        Scope scope = new Scope(roomId, mapId, instanceToken);
        Map<Long, GroundItem> items = groundByScope.get(scope);
        GroundItem item = items == null ? null : items.remove(objectId);
        if (items != null && items.isEmpty()) groundByScope.remove(scope);
        return item;
    }

    synchronized Invalidation invalidate(long token) {
        Lease lease = leases.remove(token);
        if (lease == null) return null;
        for (long objectId : lease.keyObjectIds()) {
            leaseByKeyObject.remove(objectId, token);
        }
        List<GroundItem> removed = new ArrayList<>();
        for (Map.Entry<Scope, LinkedHashMap<Long, GroundItem>> entry
                : List.copyOf(groundByScope.entrySet())) {
            for (long objectId : lease.keyObjectIds()) {
                GroundItem item = entry.getValue().remove(objectId);
                if (item != null) removed.add(item);
            }
            if (entry.getValue().isEmpty()) groundByScope.remove(entry.getKey());
        }
        return new Invalidation(lease, lease.keyObjectIds(), removed);
    }

    synchronized void discardKeys(Collection<Long> objectIds) {
        if (objectIds == null || objectIds.isEmpty()) return;
        for (Long objectId : objectIds) {
            if (objectId == null) continue;
            leaseByKeyObject.remove(objectId);
            // The original key count is part of the lease contract.  A discarded
            // runtime object is simply no longer a credential; it is not reused.
        }
    }

    synchronized int activeLeaseCount(long nowNanos) {
        return Math.toIntExact(leases.values().stream()
                .filter(lease -> lease.active(nowNanos)).count());
    }

    private boolean containsGroundObject(long objectId) {
        return groundByScope.values().stream()
                .anyMatch(items -> items.containsKey(objectId));
    }
}
