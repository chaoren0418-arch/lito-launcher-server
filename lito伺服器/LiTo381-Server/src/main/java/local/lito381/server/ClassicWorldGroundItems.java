package local.lito381.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/** Server-owned map/instance-scoped classic WORLD ground items. */
final class ClassicWorldGroundItems {
    private static final long FIRST_OBJECT_ID = 0x5800_0000L;
    private static final long LAST_OBJECT_ID = 0x5FFF_FFFFL;
    private static final AtomicLong NEXT_OBJECT_ID = new AtomicLong(FIRST_OBJECT_ID);

    private record Scope(int mapId, int instanceId) {
        Scope {
            if (mapId < 0 || instanceId < -1 || instanceId == 0) {
                throw new IllegalArgumentException("invalid classic WORLD ground scope");
            }
        }
    }

    record Item(int mapId, int instanceId, long objectId,
                InventoryCatalog.Definition definition,
                RuntimeInventory.PersistentItem state, int x, int y,
                int sourceNpcId) {
        Item {
            if (mapId < 0 || instanceId < -1 || instanceId == 0
                    || objectId <= 0 || objectId > 0xFFFF_FFFFL
                    || definition == null || definition.groundGfx() <= 0 || state == null
                    || (!definition.stackable() && state.quantity() != 1)
                    || ((state.enchantLevel() != 0 || state.durability() != 0)
                        && !definition.weapon())
                    || x < 0 || x > 0xFFFF || y < 0 || y > 0xFFFF
                    || sourceNpcId <= 0) {
                throw new IllegalArgumentException("invalid classic WORLD ground item");
            }
        }

        int quantity() {
            return state.quantity();
        }

        Item withQuantity(int quantity) {
            return new Item(mapId, instanceId, objectId, definition,
                    new RuntimeInventory.PersistentItem(
                            0, quantity, state.enchantLevel(), state.durability(),
                            state.identified(), state.blessing(), state.sourceBound(),
                            state.tournamentReward(), state.equipped(),
                            state.definitionOverride(), state.chargeCount(),
                            state.expiresAtEpochMillis(), state.sealed()),
                    x, y, sourceNpcId);
        }

        RuntimeInventory.Item inventoryItem() {
            return new RuntimeInventory.Item(objectId, definition, quantity(),
                    definition.light() && state.equipped(),
                    state.durability(), state.enchantLevel(), state.identified(),
                    state.blessing(), state.sourceBound(), state.tournamentReward(),
                    state.chargeCount(), state.expiresAtEpochMillis(), state.sealed());
        }

        String displayName() {
            String name = definition.name();
            return quantity() > 1 ? name + " (" + quantity() + ")" : name;
        }
    }

    record Take(Item taken, Item remaining) {}

    private final Map<Scope, LinkedHashMap<Long, Item>> byScope =
            new LinkedHashMap<>();

    synchronized Item put(int mapId, ClassicWorldDropCatalog.ItemWire wire,
                          int quantity, int x, int y, int sourceNpcId) {
        return put(mapId, -1, wire, quantity, x, y, sourceNpcId);
    }

    synchronized Item put(int mapId, int instanceId,
                          ClassicWorldDropCatalog.ItemWire wire,
                          int quantity, int x, int y, int sourceNpcId) {
        if (wire == null || quantity <= 0 || quantity > ClassicWorldDropCatalog.MAXIMUM_QUANTITY
                || (!wire.stackable() && quantity != 1)) {
            return null;
        }
        InventoryCatalog.Definition definition = wire.inventoryDefinition();
        RuntimeInventory.PersistentItem state = new RuntimeInventory.PersistentItem(
                quantity, 0, 0, false, wire.blessing());
        Item item = new Item(mapId, instanceId, nextObjectId(),
                definition, state, x, y, sourceNpcId);
        byScope.computeIfAbsent(new Scope(mapId, instanceId),
                        ignored -> new LinkedHashMap<>())
                .put(item.objectId(), item);
        return item;
    }

    /** Places a catalog-defined quest reward when inventory overflows. */
    synchronized Item put(int mapId, int instanceId,
                          InventoryCatalog.Definition definition,
                          int quantity, int x, int y, int sourceNpcId) {
        if (definition == null || definition.groundGfx() <= 0
                || quantity <= 0 || quantity > ClassicWorldDropCatalog.MAXIMUM_QUANTITY
                || (!definition.stackable() && quantity != 1)) {
            return null;
        }
        RuntimeInventory.PersistentItem state = new RuntimeInventory.PersistentItem(
                quantity, 0, 0, false, definition.blessing());
        Item item = new Item(mapId, instanceId, nextObjectId(),
                definition, state, x, y, sourceNpcId);
        byScope.computeIfAbsent(new Scope(mapId, instanceId),
                        ignored -> new LinkedHashMap<>())
                .put(item.objectId(), item);
        return item;
    }

    /** Places a native runtime item on a classic map without losing its mutable state. */
    synchronized Item put(int mapId, RuntimeInventory.Item source,
                          int quantity, int x, int y, int sourceNpcId) {
        return put(mapId, -1, source, quantity, x, y, sourceNpcId);
    }

    synchronized Item put(int mapId, int instanceId,
                          RuntimeInventory.Item source,
                          int quantity, int x, int y, int sourceNpcId) {
        if (source == null || quantity <= 0 || quantity > source.quantity()
                || quantity > ClassicWorldDropCatalog.MAXIMUM_QUANTITY
                || (!source.definition().stackable() && quantity != 1)) {
            return null;
        }
        RuntimeInventory.PersistentItem state = new RuntimeInventory.PersistentItem(
                0, quantity, source.enchantLevel(), source.durability(),
                source.identified(), source.blessing(), source.sourceBound(),
                source.tournamentReward(),
                source.definition().light() && source.equipped(),
                source.persistentItem().definitionOverride(), source.chargeCount(),
                source.expiresAtEpochMillis(), source.sealed());
        long objectId = quantity == source.quantity()
                && source.objectId() >= 0x7000_0000L
                && !containsObjectId(source.objectId())
                ? source.objectId() : nextObjectId();
        Item item = new Item(mapId, instanceId, objectId, source.definition(), state,
                x, y, sourceNpcId);
        byScope.computeIfAbsent(new Scope(mapId, instanceId),
                        ignored -> new LinkedHashMap<>())
                .put(item.objectId(), item);
        return item;
    }

    /** Publishes a player/pet drop only after the owner's sidecar commit succeeds. */
    synchronized Item putPersisted(int mapId, RuntimeInventory.Item source,
                                   int quantity, int x, int y, int sourceNpcId,
                                   DurableCommit commit) throws java.io.IOException {
        return putPersisted(mapId, -1, source, quantity, x, y,
                sourceNpcId, commit);
    }

    synchronized Item putPersisted(int mapId, int instanceId,
                                   RuntimeInventory.Item source,
                                   int quantity, int x, int y, int sourceNpcId,
                                   DurableCommit commit) throws java.io.IOException {
        if (commit == null) return null;
        Item item = put(mapId, instanceId, source, quantity, x, y, sourceNpcId);
        if (item == null) return null;
        try {
            commit.commit();
            return item;
        } catch (java.io.IOException | RuntimeException failure) {
            Scope scope = new Scope(mapId, instanceId);
            Map<Long, Item> items = byScope.get(scope);
            if (items == null || !items.remove(item.objectId(), item)) {
                throw new IllegalStateException(
                        "failed classic-world drop remained published after commit failure",
                        failure);
            }
            if (items.isEmpty()) byScope.remove(scope);
            throw failure;
        }
    }

    synchronized Item item(int mapId, long objectId) {
        return item(mapId, -1, objectId);
    }

    synchronized Item item(int mapId, int instanceId, long objectId) {
        Map<Long, Item> items = byScope.get(new Scope(mapId, instanceId));
        return items == null ? null : items.get(objectId);
    }

    synchronized List<Item> inScreen(int mapId, int x, int y) {
        return inScreen(mapId, -1, x, y);
    }

    synchronized List<Item> inScreen(
            int mapId, int instanceId, int x, int y) {
        Map<Long, Item> items = byScope.get(new Scope(mapId, instanceId));
        if (items == null) return List.of();
        List<Item> result = new ArrayList<>();
        for (Item item : items.values()) {
            if (BattleSpellCatalog.isInNativeScreen(x, y, item.x(), item.y())) {
                result.add(item);
            }
        }
        return List.copyOf(result);
    }

    synchronized Take take(int mapId, long objectId, long quantity) {
        return take(mapId, -1, objectId, quantity);
    }

    synchronized Take take(
            int mapId, int instanceId, long objectId, long quantity) {
        Scope scope = new Scope(mapId, instanceId);
        Map<Long, Item> items = byScope.get(scope);
        Item item = items == null ? null : items.get(objectId);
        if (item == null || quantity <= 0 || quantity > item.quantity()) return null;
        Item taken = item.withQuantity((int) quantity);
        int left = item.quantity() - (int) quantity;
        Item remaining = left == 0 ? null : item.withQuantity(left);
        if (remaining == null) {
            items.remove(objectId);
            if (items.isEmpty()) byScope.remove(scope);
        } else {
            items.put(objectId, remaining);
        }
        return new Take(taken, remaining);
    }

    /**
     * Transfers one whole ground object only when the recipient admits it.
     * The callback runs while the ground scope is locked, so a player pickup
     * cannot remove the same object between NPC inventory admission and the
     * native ground-object removal.
     */
    synchronized Take takeWholeIf(
            int mapId, int instanceId, long objectId,
            Predicate<Item> recipient) {
        Scope scope = new Scope(mapId, instanceId);
        Map<Long, Item> items = byScope.get(scope);
        Item item = items == null ? null : items.get(objectId);
        if (item == null || recipient == null || !recipient.test(item)) {
            return null;
        }
        items.remove(objectId);
        if (items.isEmpty()) byScope.remove(scope);
        return new Take(item, null);
    }

    /** Removes a WORLD pickup only after the recipient inventory is durable. */
    synchronized Take takePersisted(int mapId, long objectId, long quantity,
                                    DurableCommit commit) throws java.io.IOException {
        return takePersisted(mapId, -1, objectId, quantity, commit);
    }

    synchronized Take takePersisted(
            int mapId, int instanceId, long objectId, long quantity,
            DurableCommit commit) throws java.io.IOException {
        Map<Long, Item> items = byScope.get(new Scope(mapId, instanceId));
        Item item = items == null ? null : items.get(objectId);
        if (item == null || quantity <= 0 || quantity > item.quantity() || commit == null) {
            return null;
        }
        commit.commit();
        Take result = take(mapId, instanceId, objectId, quantity);
        if (result == null) {
            throw new IllegalStateException(
                    "validated classic-world pickup disappeared after durable commit");
        }
        return result;
    }

    synchronized void clear() {
        byScope.clear();
    }

    private boolean containsObjectId(long objectId) {
        if (objectId <= 0 || objectId > 0xFFFF_FFFFL) return true;
        for (Map<Long, Item> items : byScope.values()) {
            if (items.containsKey(objectId)) return true;
        }
        return false;
    }

    private static long nextObjectId() {
        return NEXT_OBJECT_ID.getAndUpdate(current ->
                current >= LAST_OBJECT_ID ? FIRST_OBJECT_ID : current + 1);
    }
}
