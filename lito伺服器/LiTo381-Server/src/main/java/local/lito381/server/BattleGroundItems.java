package local.lito381.server;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Server-owned, battle-scoped item objects visible through S_PUT_OBJECT. */
final class BattleGroundItems {
    private static final long FIRST_OBJECT_ID = 0x5000_0000L;
    private static final long LAST_OBJECT_ID = 0x57FF_FFFFL;
    private static final AtomicLong NEXT_OBJECT_ID = new AtomicLong(FIRST_OBJECT_ID);

    record Item(long objectId, InventoryCatalog.Definition definition,
                RuntimeInventory.PersistentItem state, int x, int y) {
        Item {
            if (objectId < FIRST_OBJECT_ID || objectId > LAST_OBJECT_ID
                    || definition == null || definition.groundGfx() <= 0
                    || state == null || (!definition.stackable() && state.quantity() != 1)
                    || ((state.enchantLevel() != 0 || state.durability() != 0)
                            && !definition.weapon())
                    || x < 0 || x > 0xFFFF || y < 0 || y > 0xFFFF) {
                throw new IllegalArgumentException("invalid battle ground item");
            }
        }

        Item(long objectId, InventoryCatalog.Definition definition,
             int quantity, int x, int y) {
            this(objectId, definition,
                    RuntimeInventory.PersistentItem.defaults(definition, quantity), x, y);
        }

        int quantity() {
            return state.quantity();
        }

        Item withQuantity(int value) {
            return new Item(objectId, definition, new RuntimeInventory.PersistentItem(
                    0, value, state.enchantLevel(), state.durability(), state.identified(),
                    state.blessing(), state.sourceBound(), state.tournamentReward(),
                    state.equipped(), state.definitionOverride(), state.chargeCount(),
                    state.expiresAtEpochMillis(), state.sealed()), x, y);
        }

        RuntimeInventory.Item inventoryItem() {
            return new RuntimeInventory.Item(objectId, definition, quantity(),
                    definition.light() && state.equipped(),
                    state.durability(), state.enchantLevel(), state.identified(),
                    state.blessing(), state.sourceBound(), state.tournamentReward(),
                    state.chargeCount(), state.expiresAtEpochMillis(), state.sealed());
        }

        String displayName() {
            return inventoryItem().nativeViewName();
        }
    }

    record Take(Item taken, Item remaining) {}

    private final LinkedHashMap<Long, Item> items = new LinkedHashMap<>();

    Item put(InventoryCatalog.Definition definition, long quantity, int x, int y) {
        if (definition == null || quantity <= 0 || quantity > Integer.MAX_VALUE) return null;
        return put(definition, RuntimeInventory.PersistentItem.defaults(
                definition, (int) quantity), x, y);
    }

    Item put(RuntimeInventory.Item source, long quantity, int x, int y) {
        if (source == null || quantity <= 0 || quantity > source.quantity()
                || quantity > Integer.MAX_VALUE) return null;
        return put(source.definition(), new RuntimeInventory.PersistentItem(
                0, (int) quantity, source.enchantLevel(), source.durability(),
                source.identified(), source.blessing(), source.sourceBound(),
                source.tournamentReward(),
                source.definition().light() && source.equipped(),
                source.persistentItem().definitionOverride(), source.chargeCount(),
                source.expiresAtEpochMillis(), source.sealed()), x, y);
    }

    /** Publishes a drop only when the owner's durable inventory commit succeeds. */
    Item putPersisted(RuntimeInventory.Item source, long quantity, int x, int y,
                      DurableCommit commit) throws java.io.IOException {
        if (commit == null) return null;
        Item item = put(source, quantity, x, y);
        if (item == null) return null;
        try {
            commit.commit();
            return item;
        } catch (java.io.IOException | RuntimeException failure) {
            if (!items.remove(item.objectId(), item)) {
                throw new IllegalStateException(
                        "failed battle-ground drop remained published after commit failure",
                        failure);
            }
            throw failure;
        }
    }

    private Item put(InventoryCatalog.Definition definition,
                     RuntimeInventory.PersistentItem state, int x, int y) {
        if (definition == null || definition.groundGfx() <= 0
                || state == null || x < 0 || x > 0xFFFF
                || y < 0 || y > 0xFFFF) {
            return null;
        }
        if (!definition.stackable() && state.quantity() != 1) return null;
        long objectId = nextObjectId();
        Item item = new Item(objectId, definition, state, x, y);
        items.put(objectId, item);
        return item;
    }

    Item item(long objectId) {
        return items.get(objectId);
    }

    List<Item> items() {
        return List.copyOf(items.values());
    }

    Take take(long objectId, long quantity) {
        Item item = items.get(objectId);
        if (item == null || quantity <= 0 || quantity > item.quantity()) return null;
        Item taken = item.withQuantity((int) quantity);
        int left = item.quantity() - (int) quantity;
        Item remaining = left == 0 ? null : item.withQuantity(left);
        if (remaining == null) items.remove(objectId); else items.put(objectId, remaining);
        return new Take(taken, remaining);
    }

    /** Removes a pickup only after the recipient inventory is durable. */
    Take takePersisted(long objectId, long quantity, DurableCommit commit)
            throws java.io.IOException {
        Item item = items.get(objectId);
        if (item == null || quantity <= 0 || quantity > item.quantity() || commit == null) {
            return null;
        }
        commit.commit();
        Take result = take(objectId, quantity);
        if (result == null) {
            throw new IllegalStateException(
                    "validated battle-ground pickup disappeared after durable commit");
        }
        return result;
    }

    void clear() {
        items.clear();
    }

    private static long nextObjectId() {
        return NEXT_OBJECT_ID.getAndUpdate(current ->
                current >= LAST_OBJECT_ID ? FIRST_OBJECT_ID : current + 1);
    }
}
