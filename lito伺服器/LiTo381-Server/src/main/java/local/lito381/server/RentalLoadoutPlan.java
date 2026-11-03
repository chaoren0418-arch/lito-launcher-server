package local.lito381.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * Session-local staging for one exact native LTItemSet row group.
 *
 * <p>The native tables prove catalog id, enchant, quantity and semantic slot,
 * but do not prove identification or automatic-equip state.  Consequently
 * this class has no implicit/default policy: callers must name one explicitly,
 * and {@link ClientSession} first activates the staged inventory while
 * bootstrapping a rental-town scene.  A configured TeleportMe transition may
 * then carry an authenticated, character-bound copy into one Tournament
 * session; ordinary town exit/restart still removes the session-local objects.</p>
 */
final class RentalLoadoutPlan {
    enum StatePolicy {
        IDENTIFIED_UNEQUIPPED(true, false),
        IDENTIFIED_TABLE_SLOTS(true, true),
        UNIDENTIFIED_UNEQUIPPED(false, false);

        private final boolean identified;
        private final boolean equipTableSlots;

        StatePolicy(boolean identified, boolean equipTableSlots) {
            this.identified = identified;
            this.equipTableSlots = equipTableSlots;
        }
    }

    record Entry(RentalItemSetCatalog.Item source,
                 InventoryCatalog.Definition definition,
                 boolean equipmentSlot) {
        Entry {
            if (source == null || definition == null
                    || definition.catalogId() != source.catalogId()) {
                throw new IllegalArgumentException("invalid rental loadout entry");
            }
            if (equipmentSlot && !definition.weapon()
                    && !definition.sourceArmor()) {
                throw new IllegalArgumentException(
                        "rental equipment slot has no native equipment definition: "
                                + source.catalogId() + " slot=" + source.slot());
            }
        }
    }

    record Staged(RentalItemSetCatalog.Set source,
                  StatePolicy policy,
                  RuntimeInventory inventory,
                  List<Long> tableRowObjectIds) {
        Staged {
            if (source == null || policy == null || inventory == null
                    || tableRowObjectIds == null
                    || tableRowObjectIds.size() != source.items().size()) {
                throw new IllegalArgumentException("invalid staged rental loadout");
            }
            tableRowObjectIds = List.copyOf(tableRowObjectIds);
        }
    }

    private record EquipmentObject(long objectId, Entry entry) {}

    private final RentalItemSetCatalog.Set source;
    private final List<Entry> entries;

    private RentalLoadoutPlan(RentalItemSetCatalog.Set source, List<Entry> entries) {
        this.source = source;
        this.entries = List.copyOf(entries);
    }

    static RentalLoadoutPlan require(int characterClass, int selection) {
        RentalItemSetCatalog.Set source =
                RentalItemSetCatalog.require(characterClass, selection);
        List<Entry> entries = new ArrayList<>();
        for (RentalItemSetCatalog.Item item : source.items()) {
            RentalItemDefinitionCatalog.SourceTemplate exactEquipment =
                    RentalItemDefinitionCatalog.source(Math.toIntExact(item.catalogId()));
            InventoryCatalog.Definition definition = exactEquipment == null
                    ? item.definitionOrNull() : exactEquipment.definition();
            if (definition == null) {
                throw new IllegalStateException(
                        "rental loadout definition is missing: " + item.catalogId());
            }
            entries.add(new Entry(item, definition, !"輔助".equals(item.slot())));
        }
        return new RentalLoadoutPlan(source, entries);
    }

    RentalItemSetCatalog.Set source() {
        return source;
    }

    List<Entry> entries() {
        return entries;
    }

    Staged stage(StatePolicy policy, LongSupplier allocator) {
        if (policy == null || allocator == null) {
            throw new IllegalArgumentException("rental staging policy and allocator are required");
        }
        RuntimeInventory inventory = new RuntimeInventory();
        List<Long> rowObjectIds = new ArrayList<>();
        List<EquipmentObject> equipment = new ArrayList<>();
        for (Entry entry : entries) {
            RentalItemSetCatalog.Item row = entry.source();
            InventoryCatalog.Definition definition = entry.definition();
            int instances = definition.stackable() ? 1 : row.quantity();
            int instanceQuantity = definition.stackable() ? row.quantity() : 1;
            long firstObjectId = 0;
            for (int index = 0; index < instances; index++) {
                RuntimeInventory.PersistentItem state = new RuntimeInventory.PersistentItem(
                        0, instanceQuantity, row.enchantLevel(), 0,
                        policy.identified, definition.blessing(),
                        false, false, false, null,
                        definition.initialChargeCount());
                long objectId = inventory.addDistinctState(definition, state, allocator);
                if (objectId == 0) {
                    throw new IllegalStateException(
                            "cannot stage rental item " + row.catalogId());
                }
                if (firstObjectId == 0) firstObjectId = objectId;
                if (entry.equipmentSlot()) {
                    equipment.add(new EquipmentObject(objectId, entry));
                }
            }
            rowObjectIds.add(firstObjectId);
        }
        if (policy.equipTableSlots) {
            equipment.sort(Comparator.comparingInt(RentalLoadoutPlan::equipOrder));
            for (EquipmentObject object : equipment) {
                List<RuntimeInventory.Item> changed = object.entry().definition().weapon()
                        ? inventory.toggleWeapon(object.objectId())
                        : inventory.toggleNativeArmor(object.objectId());
                if (changed.isEmpty()
                        || !inventory.item(object.objectId()).equipped()) {
                    throw new IllegalStateException(
                            "rental table-slot topology rejected item "
                                    + object.entry().source().catalogId());
                }
            }
        }
        return new Staged(source, policy, inventory, rowObjectIds);
    }

    private static int equipOrder(EquipmentObject object) {
        InventoryCatalog.Definition definition = object.entry().definition();
        if (definition.weapon()) return 100;
        int armorType = definition.sourceArmorStatus().armorType();
        // Retained L1EquipmentSlot dependencies require shirt -> body -> cloak.
        return switch (armorType) {
            case 3 -> 0;
            case 2 -> 1;
            case 4 -> 2;
            default -> 10 + armorType;
        };
    }

    Map<Long, Integer> expectedCatalogCounts() {
        java.util.LinkedHashMap<Long, Integer> expected = new java.util.LinkedHashMap<>();
        for (Entry entry : entries) {
            expected.merge(entry.source().catalogId(), entry.source().quantity(), Math::addExact);
        }
        return Map.copyOf(expected);
    }
}
