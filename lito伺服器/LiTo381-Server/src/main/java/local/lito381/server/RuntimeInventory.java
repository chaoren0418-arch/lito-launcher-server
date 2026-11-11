package local.lito381.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/** Native runtime inventory keyed by object id, separate from durable catalog counts. */
final class RuntimeInventory {
    static final int MAX_SLOTS = 0x100;
    static final int BLESSING_BLESSED = 0;
    static final int BLESSING_NORMAL = 1;
    static final int BLESSING_CURSED = 2;
    static final int BLESSING_UNIDENTIFIED = 3;
    static final int NATIVE_RING_SLOT_LIMIT = 2;
    private static final long FIRST_OBJECT_ID = 0x7000_0000L;
    private static final AtomicLong NEXT_OBJECT_ID = new AtomicLong(FIRST_OBJECT_ID);

    /**
     * Durable per-object state. Quantity is part of the record because two
     * native stacks with the same catalog id may carry different blessing or
     * identification state and therefore must not be collapsed on restart.
     */
    record PersistentItem(long objectId, int quantity, int enchantLevel, int durability,
                          boolean identified, int blessing, boolean sourceBound,
                          boolean tournamentReward,
                          boolean equipped,
                          InventoryCatalog.Definition definitionOverride,
                          int chargeCount,
                          long expiresAtEpochMillis,
                          boolean sealed,
                          boolean litoOwned) {
        /** Format-39/runtime compatibility before writable SQL provenance. */
        PersistentItem(long objectId, int quantity, int enchantLevel, int durability,
                       boolean identified, int blessing, boolean sourceBound,
                       boolean tournamentReward, boolean equipped,
                       InventoryCatalog.Definition definitionOverride, int chargeCount,
                       long expiresAtEpochMillis, boolean sealed) {
            this(objectId, quantity, enchantLevel, durability, identified, blessing,
                    sourceBound, tournamentReward, equipped, definitionOverride,
                    chargeCount, expiresAtEpochMillis, sealed, false);
        }
        /** Format-34..37 compatibility: timed/sealed instance state was absent. */
        PersistentItem(long objectId, int quantity, int enchantLevel, int durability,
                       boolean identified, int blessing, boolean sourceBound,
                       boolean tournamentReward,
                       boolean equipped,
                       InventoryCatalog.Definition definitionOverride,
                       int chargeCount) {
            this(objectId, quantity, enchantLevel, durability, identified, blessing,
                    sourceBound, tournamentReward, equipped, definitionOverride,
                    chargeCount, 0L, false);
        }

        /** Format-31..33 compatibility: the trailing reward provenance defaults false. */
        PersistentItem(long objectId, int quantity, int enchantLevel, int durability,
                       boolean identified, int blessing, boolean sourceBound,
                       boolean equipped,
                       InventoryCatalog.Definition definitionOverride,
                       int chargeCount) {
            this(objectId, quantity, enchantLevel, durability, identified, blessing,
                    sourceBound, false, equipped, definitionOverride, chargeCount);
        }

        PersistentItem(long objectId, int quantity, int enchantLevel, int durability,
                       boolean identified, int blessing, boolean sourceBound,
                       boolean equipped,
                       InventoryCatalog.Definition definitionOverride) {
            this(objectId, quantity, enchantLevel, durability, identified, blessing,
                    sourceBound, false, equipped, definitionOverride, 0);
        }

        PersistentItem(long objectId, int quantity, int enchantLevel, int durability,
                       boolean identified, int blessing, boolean sourceBound,
                       boolean equipped) {
            this(objectId, quantity, enchantLevel, durability, identified, blessing,
                    sourceBound, false, equipped, null, 0);
        }

        PersistentItem(long objectId, int quantity, int enchantLevel, int durability,
                       boolean identified, int blessing) {
            this(objectId, quantity, enchantLevel, durability, identified, blessing,
                    false, false, false, null, 0);
        }

        PersistentItem(long objectId, int quantity, int enchantLevel, int durability,
                       boolean identified, int blessing, boolean sourceBound) {
            this(objectId, quantity, enchantLevel, durability, identified, blessing,
                    sourceBound, false, false, null, 0);
        }

        /** Format-24 and source-call compatibility: zero requests a fresh id. */
        PersistentItem(int quantity, int enchantLevel, int durability,
                       boolean identified, int blessing) {
            this(0, quantity, enchantLevel, durability, identified, blessing,
                    false, false, false, null, 0);
        }

        PersistentItem {
            if (objectId < 0 || objectId > 0xFFFF_FFFFL
                    || quantity <= 0 || enchantLevel < -128 || enchantLevel > 127
                    || durability < 0 || durability > 0x7F
                    || chargeCount < 0 || chargeCount > 0xFF
                    || expiresAtEpochMillis < 0 || litoOwned && !sourceBound
                    || blessing < BLESSING_BLESSED
                    || blessing > BLESSING_UNIDENTIFIED) {
                throw new IllegalArgumentException("invalid durable inventory item state");
            }
        }

        static PersistentItem defaults(InventoryCatalog.Definition definition,
                                       int quantity) {
            if (definition == null) {
                throw new IllegalArgumentException("item definition is required");
            }
            return new PersistentItem(0, quantity, 0, 0,
                    definition.identified(), definition.blessing(), false, false, false, null,
                    definition.initialChargeCount(), 0L, false);
        }

        boolean isDefaultFor(InventoryCatalog.Definition definition) {
            return objectId == 0 && enchantLevel == 0 && durability == 0
                    && chargeCount == definition.initialChargeCount()
                    && expiresAtEpochMillis == 0L && !sealed
                    && !sourceBound && !tournamentReward && !litoOwned
                    && !equipped && definitionOverride == null
                    && identified == definition.identified()
                    && blessing == definition.blessing();
        }

        InventoryCatalog.Definition effectiveDefinition(
                InventoryCatalog.Definition fallback, long catalogId) {
            InventoryCatalog.Definition effective = definitionOverride == null
                    ? fallback : definitionOverride;
            if (effective == null || effective.catalogId() != catalogId) {
                throw new IllegalArgumentException(
                        "durable inventory definition/catalog id mismatch");
            }
            return effective;
        }
    }

    record Item(long objectId, InventoryCatalog.Definition definition,
                int quantity, boolean equipped, int durability, int enchantLevel,
                boolean identified, int blessing, boolean sourceBound,
                boolean tournamentReward,
                int chargeCount,
                long expiresAtEpochMillis,
                boolean sealed,
                boolean litoOwned) {
        /** Format-39/runtime compatibility before writable SQL provenance. */
        Item(long objectId, InventoryCatalog.Definition definition,
             int quantity, boolean equipped, int durability, int enchantLevel,
             boolean identified, int blessing, boolean sourceBound,
             boolean tournamentReward, int chargeCount,
             long expiresAtEpochMillis, boolean sealed) {
            this(objectId, definition, quantity, equipped, durability, enchantLevel,
                    identified, blessing, sourceBound, tournamentReward, chargeCount,
                    expiresAtEpochMillis, sealed, false);
        }
        /** Format-34..37/runtime call compatibility. */
        Item(long objectId, InventoryCatalog.Definition definition,
             int quantity, boolean equipped, int durability, int enchantLevel,
             boolean identified, int blessing, boolean sourceBound,
             boolean tournamentReward,
             int chargeCount) {
            this(objectId, definition, quantity, equipped, durability, enchantLevel,
                    identified, blessing, sourceBound, tournamentReward, chargeCount,
                    0L, false);
        }

        /** Pre-format-34 call compatibility. */
        Item(long objectId, InventoryCatalog.Definition definition,
             int quantity, boolean equipped, int durability, int enchantLevel,
             boolean identified, int blessing, boolean sourceBound,
             int chargeCount) {
            this(objectId, definition, quantity, equipped, durability, enchantLevel,
                    identified, blessing, sourceBound, false, chargeCount);
        }

        Item(long objectId, InventoryCatalog.Definition definition,
             int quantity, boolean equipped, int durability, int enchantLevel,
             boolean identified, int blessing, boolean sourceBound) {
            this(objectId, definition, quantity, equipped, durability, enchantLevel,
                    identified, blessing, sourceBound, false,
                    definition == null ? 0 : definition.initialChargeCount());
        }

        Item(long objectId, InventoryCatalog.Definition definition,
             int quantity, boolean equipped, int durability, int enchantLevel,
             boolean identified, int blessing) {
            this(objectId, definition, quantity, equipped, durability, enchantLevel,
                    identified, blessing, false, false,
                    definition == null ? 0 : definition.initialChargeCount());
        }

        Item(long objectId, InventoryCatalog.Definition definition,
             int quantity, boolean equipped) {
            this(objectId, definition, quantity, equipped, 0, 0,
                    definition.identified(), definition.blessing(), false, false,
                    definition.initialChargeCount());
        }

        Item(long objectId, InventoryCatalog.Definition definition,
             int quantity, boolean equipped, int durability) {
            this(objectId, definition, quantity, equipped, durability, 0,
                    definition.identified(), definition.blessing(), false, false,
                    definition.initialChargeCount());
        }

        Item(long objectId, InventoryCatalog.Definition definition,
             int quantity, boolean equipped, int durability, boolean identified) {
            this(objectId, definition, quantity, equipped, durability, 0, identified,
                    definition.blessing(), false, false,
                    definition.initialChargeCount());
        }

        Item {
            if (objectId <= 0 || objectId > 0xFFFF_FFFFL || definition == null
                    || quantity <= 0 || durability < 0 || durability > 0x7F
                    || enchantLevel < -128 || enchantLevel > 127
                    || chargeCount < 0 || chargeCount > definition.maximumChargeCount()
                    || litoOwned && !sourceBound
                    || blessing < BLESSING_BLESSED || blessing > BLESSING_UNIDENTIFIED
                    || ((durability > 0 || enchantLevel != 0)
                        && !definition.weapon() && !definition.armor())) {
                throw new IllegalArgumentException("invalid runtime inventory item");
            }
        }

        Item withQuantity(int value) {
            return new Item(objectId, definition, value, equipped, durability, enchantLevel,
                    identified, blessing, sourceBound, tournamentReward, chargeCount,
                    expiresAtEpochMillis, sealed, litoOwned);
        }

        Item withEquipped(boolean value) {
            return new Item(objectId, definition, quantity, value, durability, enchantLevel,
                    identified, blessing, sourceBound, tournamentReward, chargeCount,
                    expiresAtEpochMillis, sealed, litoOwned);
        }

        Item withDurability(int value) {
            return new Item(objectId, definition, quantity, equipped, value, enchantLevel,
                    identified, blessing, sourceBound, tournamentReward, chargeCount,
                    expiresAtEpochMillis, sealed, litoOwned);
        }

        Item withEnchantLevel(int value) {
            return new Item(objectId, definition, quantity, equipped, durability, value,
                    identified, blessing, sourceBound, tournamentReward, chargeCount,
                    expiresAtEpochMillis, sealed, litoOwned);
        }

        Item withIdentified(boolean value) {
            return new Item(objectId, definition, quantity, equipped, durability, enchantLevel,
                    value, blessing, sourceBound, tournamentReward, chargeCount,
                    expiresAtEpochMillis, sealed, litoOwned);
        }

        Item withBlessing(int value) {
            return new Item(objectId, definition, quantity, equipped, durability, enchantLevel,
                    identified, value, sourceBound, tournamentReward, chargeCount,
                    expiresAtEpochMillis, sealed, litoOwned);
        }

        Item withDefinition(InventoryCatalog.Definition value) {
            return new Item(objectId, value, quantity, equipped, durability, enchantLevel,
                    identified, blessing, sourceBound, tournamentReward,
                    Math.min(chargeCount, value.maximumChargeCount()),
                    expiresAtEpochMillis, sealed, litoOwned);
        }

        Item withChargeCount(int value) {
            return new Item(objectId, definition, quantity, equipped, durability, enchantLevel,
                    identified, blessing, sourceBound, tournamentReward, value,
                    expiresAtEpochMillis, sealed, litoOwned);
        }

        /**
         * A partial transfer of an immutable source stack creates a new object
         * id.  The original row keeps its source provenance, while the derived
         * object must be marked LiTo-owned so it can be written safely without
         * impersonating another TW380 source row.
         */
        Item asDerivedLitoOwned() {
            return new Item(objectId, definition, quantity, equipped, durability,
                    enchantLevel, identified, blessing, true, tournamentReward,
                    chargeCount, expiresAtEpochMillis, sealed, true);
        }

        /** A death drop becomes LiTo-owned after the source row is tombstoned. */
        Item withoutSourceBinding() {
            return new Item(objectId, definition, quantity, equipped, durability,
                    enchantLevel, identified, blessing, false, tournamentReward,
                    chargeCount, expiresAtEpochMillis, sealed, false);
        }

        PersistentItem persistentItem() {
            return new PersistentItem(objectId, quantity, enchantLevel, durability,
                    identified, blessing, sourceBound, tournamentReward, equipped,
                    durableDefinitionOverride(definition), chargeCount,
                    expiresAtEpochMillis, sealed, litoOwned);
        }

        /** Retained L1ItemInstance.get_time() != null transfer gate. */
        boolean timed() {
            return expiresAtEpochMillis != 0L;
        }

        boolean transferable() {
            return definition.tradable() && (!sourceBound || litoOwned
                    || (!definition.weapon() && !definition.sourceArmor()
                            && !definition.projectile()));
        }

        /**
         * The retained item model keeps a light's {@code isNowLighting}
         * separate from equipment.  LiTo's compact durable representation
         * shares the bit, so an active light must not trigger the native
         * "equipped item" transfer rejection.
         */
        boolean blocksNativeTransferAsEquipped() {
            return equipped && !definition.light();
        }

        /**
         * Same-family 3.81 S_ItemName/getViewName markers used when the
         * native equipped/light state changes. The base inventory record
         * retains the unadorned catalog name.
         */
        String nativeViewName() {
            String base = nativeLogName();
            String equippedName = !equipped ? base
                    : definition.weapon() ? base + " ($9)"
                    : definition.light() ? base + " ($10)"
                    : definition.armor() ? base + " ($117)"
                    : base;
            String charged = definition.maximumChargeCount() > 0 && identified
                    ? equippedName + " (" + chargeCount + ")" : equippedName;
            return quantity > 1 ? charged + " (" + quantity + ")" : charged;
        }

        String nativeLogName() {
            return identified && (definition.weapon() || definition.armor())
                    ? (enchantLevel >= 0 ? "+" + enchantLevel : enchantLevel) + " "
                            + definition.name()
                    : definition.name();
        }

        byte[] nativeStatusDetail() {
            InventoryCatalog.NativeArmorStatus armor = definition.sourceArmorStatus();
            if (armor != null) {
                return armor.detail(
                        definition.useType(), quantity, enchantLevel, durability);
            }
            InventoryCatalog.NativeProjectileStatus projectile =
                    InventoryCatalog.nativeProjectileStatus(definition.catalogId());
            if (definition.projectile() && projectile != null) {
                long weight = Math.min(0xFFFF_FFFFL,
                        (long) definition.unitWeight() * quantity / 1000L);
                // Same native damage/material/weight tuple as weapons, without
                // equipment-only enchantment, durability or class restrictions.
                return new Protocol.Writer().c(0x01)
                        .c(projectile.damageSmall()).c(projectile.damageLarge())
                        .c(projectile.material()).d(weight).bytes();
            }
            InventoryCatalog.NativeWeaponStatus status = definition.effectiveWeaponStatus();
            if (status == null) {
                // LiTo sub_4A2090 parses typed status bytes, not a text string.
                long weight = Math.min(0xFFFF_FFFFL,
                        (long) definition.unitWeight() * quantity / 1000L);
                return new Protocol.Writer().c(0x17).c(0).d(weight).bytes();
            }
            Protocol.Writer detail = new Protocol.Writer()
                    .c(0x01).c(status.damageSmall()).c(status.damageLarge())
                    .c(status.material()).d(status.packetWeight(quantity));
            if (enchantLevel != 0) detail.c(0x02).c(enchantLevel);
            if (durability != 0) detail.c(0x03).c(durability);
            if (status.hitModifier() != 0) {
                detail.c(0x05).c(NativeCharacterStats.signedByte(status.hitModifier()));
            }
            if (status.damageModifier() != 0) {
                detail.c(0x06).c(NativeCharacterStats.signedByte(status.damageModifier()));
            }
            return detail.c(0x07).c(status.classMask()).bytes();
        }

        private static String signed(int value) {
            return value >= 0 ? "+" + value : Integer.toString(value);
        }

        Protocol.InventoryWireItem wireItem() {
            return new Protocol.InventoryWireItem(
                    objectId, definition.useType(), chargeCount,
                    definition.icon(), blessing, quantity,
                    identified ? 1 : 0, nativeViewName(),
                    identified ? nativeStatusDetail() : new byte[0]);
        }
    }

    private Map<Long, Item> items;
    private final Map<Long, SourceWeaponPhysicalStats> sourcePhysicalWeapons;

    RuntimeInventory() {
        this.items = new LinkedHashMap<>();
        this.sourcePhysicalWeapons = new LinkedHashMap<>();
    }

    private RuntimeInventory(Map<Long, Item> items,
                             Map<Long, SourceWeaponPhysicalStats> sourcePhysicalWeapons) {
        this.items = items;
        this.sourcePhysicalWeapons = sourcePhysicalWeapons == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(sourcePhysicalWeapons);
    }

    static RuntimeInventory fromCatalogCounts(Map<Long, Integer> counts) {
        return fromCatalogCounts(counts, Map.of(), RuntimeInventory::nextObjectId);
    }

    static RuntimeInventory fromCatalogCounts(Map<Long, Integer> counts, LongSupplier allocator) {
        return fromCatalogCounts(counts, Map.of(), allocator);
    }

    static RuntimeInventory fromCatalogCounts(
            Map<Long, Integer> counts, Map<Long, List<Integer>> enchantments) {
        return fromCatalogCounts(counts, enchantments, RuntimeInventory::nextObjectId);
    }

    static RuntimeInventory fromCatalogCounts(Map<Long, Integer> counts,
                                              Map<Long, List<Integer>> enchantments,
                                              LongSupplier allocator) {
        RuntimeInventory inventory = new RuntimeInventory();
        if (counts == null) return inventory;
        for (Map.Entry<Long, Integer> entry : counts.entrySet()) {
            long catalogId = entry.getKey() == null ? 0 : entry.getKey();
            int quantity = entry.getValue() == null ? 0 : entry.getValue();
            if (catalogId <= 0 || catalogId > 0xFFFF_FFFFL || quantity <= 0) continue;
            InventoryCatalog.Definition definition = InventoryCatalog.find(catalogId);
            if (definition == null) definition = InventoryCatalog.retainedUnknown(catalogId);
            if (!inventory.add(definition, quantity, allocator)) {
                throw new IllegalStateException("durable inventory exceeds native 256-slot capacity");
            }
        }
        inventory.applyCatalogEnchantments(enchantments);
        return inventory;
    }

    static RuntimeInventory fromDurableState(
            Map<Long, Integer> counts,
            Map<Long, List<PersistentItem>> itemStates) {
        return fromDurableState(counts, itemStates, Map.of(), RuntimeInventory::nextObjectId);
    }

    static RuntimeInventory fromDurableState(
            Map<Long, Integer> counts,
            Map<Long, List<PersistentItem>> itemStates,
            Map<Long, InventoryCatalog.Definition> sourceDefinitions) {
        return fromDurableState(
                counts, itemStates, sourceDefinitions, Map.of(),
                RuntimeInventory::nextObjectId);
    }

    static RuntimeInventory fromDurableState(
            Map<Long, Integer> counts,
            Map<Long, List<PersistentItem>> itemStates,
            Map<Long, InventoryCatalog.Definition> sourceDefinitions,
            Map<Long, SourceWeaponPhysicalStats> sourcePhysicalWeapons) {
        return fromDurableState(
                counts, itemStates, sourceDefinitions, sourcePhysicalWeapons,
                RuntimeInventory::nextObjectId);
    }

    static RuntimeInventory fromDurableState(
            Map<Long, Integer> counts,
            Map<Long, List<PersistentItem>> itemStates,
            LongSupplier allocator) {
        return fromDurableState(counts, itemStates, Map.of(), allocator);
    }

    static RuntimeInventory fromDurableState(
            Map<Long, Integer> counts,
            Map<Long, List<PersistentItem>> itemStates,
            Map<Long, InventoryCatalog.Definition> sourceDefinitions,
            LongSupplier allocator) {
        return fromDurableState(
                counts, itemStates, sourceDefinitions, Map.of(), allocator);
    }

    static RuntimeInventory fromDurableState(
            Map<Long, Integer> counts,
            Map<Long, List<PersistentItem>> itemStates,
            Map<Long, InventoryCatalog.Definition> sourceDefinitions,
            Map<Long, SourceWeaponPhysicalStats> sourcePhysicalWeapons,
            LongSupplier allocator) {
        RuntimeInventory inventory = new RuntimeInventory(
                new LinkedHashMap<>(), sourcePhysicalWeapons);
        if (counts == null) {
            inventory.validateSourcePhysicalWeapons();
            return inventory;
        }
        for (Map.Entry<Long, Integer> entry : counts.entrySet()) {
            long catalogId = entry.getKey() == null ? 0 : entry.getKey();
            int quantity = entry.getValue() == null ? 0 : entry.getValue();
            if (catalogId <= 0 || catalogId > 0xFFFF_FFFFL || quantity <= 0) continue;
            InventoryCatalog.Definition definition = sourceDefinitions == null
                    ? null : sourceDefinitions.get(catalogId);
            if (definition != null && definition.catalogId() != catalogId) {
                throw new IllegalArgumentException(
                        "source inventory definition/catalog id mismatch");
            }
            if (definition == null) definition = InventoryCatalog.find(catalogId);
            if (definition == null) definition = InventoryCatalog.retainedUnknown(catalogId);
            long restored = 0;
            List<PersistentItem> states = itemStates == null
                    ? null : itemStates.get(catalogId);
            if (states != null) {
                for (PersistentItem state : states) {
                    InventoryCatalog.Definition stateDefinition;
                    try {
                        stateDefinition = state == null ? null
                                : state.effectiveDefinition(definition, catalogId);
                    } catch (IllegalArgumentException damaged) {
                        continue;
                    }
                    if (state == null || restored + state.quantity() > quantity
                            || (!stateDefinition.stackable() && state.quantity() != 1)) {
                        continue;
                    }
                    boolean admitted = state.objectId() == 0
                            ? inventory.addState(stateDefinition, state, allocator)
                            : inventory.addDistinctState(stateDefinition, state, allocator) != 0;
                    if (!admitted) {
                        throw new IllegalStateException(
                                "durable inventory state exceeds native capacity");
                    }
                    restored += state.quantity();
                }
            }
            if (restored < quantity && !inventory.add(
                    definition, quantity - restored, allocator)) {
                throw new IllegalStateException(
                        "durable inventory exceeds native 256-slot capacity");
            }
        }
        inventory.validateSourcePhysicalWeapons();
        return inventory;
    }

    synchronized RuntimeInventory copy() {
        return NativeDeItemInventory.preserveCopy(this, new RuntimeInventory(
                new LinkedHashMap<>(items), sourcePhysicalWeapons));
    }

    /**
     * Native Tournament transfer boundary: retain equipment imported
     * read-only from the selected source character plus source-proven
     * Tournament prizes. Source consumables, projectiles and every bought
     * LiTo match item are excluded.
     */
    synchronized RuntimeInventory sourceEquipmentOnly() {
        Map<Long, Item> retained = new LinkedHashMap<>();
        for (Map.Entry<Long, Item> entry : items.entrySet()) {
            Item item = entry.getValue();
            if ((item.sourceBound()
                    && (item.definition().weapon() || item.definition().armor()))
                    || item.tournamentReward()
                    // 0.7.387 wrote unopened bags before format-34 added the
                    // explicit per-object reward-provenance bit.
                    || BattlePetRaceRewardCatalog.isDurablePrize(
                            item.definition().catalogId())) {
                retained.put(entry.getKey(), item);
            }
        }
        Map<Long, SourceWeaponPhysicalStats> retainedWeapons = new LinkedHashMap<>();
        for (Map.Entry<Long, SourceWeaponPhysicalStats> entry
                : sourcePhysicalWeapons.entrySet()) {
            if (retained.containsKey(entry.getKey())) {
                SourceWeaponPhysicalStats weapon = entry.getValue();
                // A source bow snapshot may carry the source character's arrow.
                // That arrow is outside the Tournament transfer boundary; bind a
                // newly purchased Tournament projectile at attack time instead.
                retainedWeapons.put(entry.getKey(), weapon.ranged()
                        ? weapon.withoutProjectile() : weapon);
            }
        }
        RuntimeInventory result = new RuntimeInventory(retained, retainedWeapons);
        result.validateSourcePhysicalWeapons();
        return NativeDeItemInventory.preserveCopy(this, result);
    }

    /**
     * Native quest cleanup may destroy retained source-bound objects even
     * though ordinary inventory mutations correctly protect them.  The
     * caller must atomically persist matching source tombstones before using
     * the returned inventory.
     */
    synchronized RuntimeInventory withoutObjects(Set<Long> objectIds) {
        if (objectIds == null || objectIds.isEmpty()) return NativeDeItemInventory.preserveCopy(this, copy());
        Map<Long, Item> retained = new LinkedHashMap<>(items);
        for (Long objectId : objectIds) {
            if (objectId != null) retained.remove(objectId);
        }
        Map<Long, SourceWeaponPhysicalStats> retainedWeapons =
                new LinkedHashMap<>(sourcePhysicalWeapons);
        for (Long objectId : objectIds) {
            if (objectId != null) retainedWeapons.remove(objectId);
        }
        return NativeDeItemInventory.preserveCopy(this, new RuntimeInventory(retained, retainedWeapons));
    }

    private synchronized void validateSourcePhysicalWeapons() {
        for (Map.Entry<Long, SourceWeaponPhysicalStats> entry
                : sourcePhysicalWeapons.entrySet()) {
            Item item = items.get(entry.getKey());
            SourceWeaponPhysicalStats weapon = entry.getValue();
            if (item == null || weapon == null || !weapon.present()
                    || !item.sourceBound() || !item.definition().weapon()
                    || item.definition().catalogId() != weapon.itemId()
                    || item.enchantLevel() != (byte) weapon.enchantLevel()) {
                throw new IllegalArgumentException(
                        "invalid per-instance selected-source weapon provenance");
            }
        }
    }

    synchronized List<Item> items() {
        return List.copyOf(items.values());
    }

    synchronized long countCatalog(long catalogId) {
        if (catalogId <= 0) return 0;
        return items.values().stream()
                .filter(item -> item.definition().catalogId() == catalogId)
                .mapToLong(Item::quantity).sum();
    }

    synchronized Item item(long objectId) {
        return items.get(objectId);
    }

    synchronized int size() {
        return items.size();
    }

    /** Exact L1Inventory saturating aggregate for LiTo-owned runtime items. */
    synchronized int totalWeight() {
        int total = 0;
        for (Item item : items.values()) {
            total = BattleInventoryWeight.addWeight(total,
                    BattleInventoryWeight.itemWeight(
                            item.quantity(), item.definition().unitWeight()));
        }
        return total;
    }

    synchronized void clear() {
        items.clear();
    }

    /** Retained {@code L1Inventory.shuffle}; affects subsequent first-stack lookup. */
    synchronized void shuffle(Random random) {
        if (NativeDeItemInventory.shuffle(this, random)) return;
        if (random == null) throw new IllegalArgumentException("missing inventory random");
        List<Map.Entry<Long, Item>> shuffled = new ArrayList<>(items.entrySet());
        Collections.shuffle(shuffled, random);
        items.clear();
        for (Map.Entry<Long, Item> entry : shuffled) {
            items.put(entry.getKey(), entry.getValue());
        }
    }

    synchronized boolean add(InventoryCatalog.Definition definition, long quantity) {
        return add(definition, quantity, RuntimeInventory::nextObjectId);
    }

    synchronized boolean add(InventoryCatalog.Definition definition, long quantity, LongSupplier allocator) {
        if (definition == null || quantity <= 0 || quantity > Integer.MAX_VALUE || allocator == null) {
            return false;
        }
        return addState(definition, PersistentItem.defaults(definition, (int) quantity), allocator);
    }

    synchronized boolean addTournamentReward(InventoryCatalog.Definition definition, long quantity) {
        return addTournamentReward(definition, quantity, RuntimeInventory::nextObjectId);
    }

    synchronized boolean addTournamentReward(InventoryCatalog.Definition definition, long quantity,
                                LongSupplier allocator) {
        if (definition == null || quantity <= 0 || quantity > Integer.MAX_VALUE
                || allocator == null) {
            return false;
        }
        PersistentItem state = new PersistentItem(
                0, (int) quantity, 0, 0,
                definition.identified(), definition.blessing(),
                false, true, false, null, definition.initialChargeCount());
        return addState(definition, state, allocator);
    }

    /** Native one-way system grant: identified, exact enchant, and merge-safe. */
    synchronized boolean addSystemReward(InventoryCatalog.Definition definition, long quantity,
                            int enchantLevel) {
        if (definition == null || quantity <= 0 || quantity > Integer.MAX_VALUE
                || enchantLevel < -128 || enchantLevel > 127
                || enchantLevel != 0 && !definition.weapon() && !definition.armor()) {
            return false;
        }
        PersistentItem state = new PersistentItem(
                0, (int) quantity, enchantLevel, 0, true,
                definition.blessing(), false, false, false, null,
                definition.initialChargeCount(), 0L, false, false);
        return addState(definition, state, RuntimeInventory::nextObjectId);
    }

    /**
     * Adds exactly one durable native object without applying the retained
     * stack-merging rule. This is used for source-provenance objects whose
     * legacy row identity must survive alongside LiTo-owned stacks.
     *
     * @return admitted/allocated object id, or zero when the state cannot be admitted
     */
    synchronized long addDistinctState(InventoryCatalog.Definition definition, PersistentItem state) {
        return addDistinctState(definition, state, RuntimeInventory::nextObjectId);
    }

    synchronized long addDistinctState(InventoryCatalog.Definition definition, PersistentItem state,
                          LongSupplier allocator) {
        if (definition == null || state == null || allocator == null
                || state.quantity() <= 0
                || (!definition.stackable() && state.quantity() != 1)
                || ((state.durability() > 0 || state.enchantLevel() != 0)
                    && !definition.weapon() && !definition.armor())
                || (state.equipped() && !equipStateSupported(definition))
                || items.size() >= MAX_SLOTS) {
            return 0;
        }
        long objectId = state.objectId() == 0
                ? uniqueObjectId(allocator) : admittedStableObjectId(state.objectId());
        if (objectId == 0) return 0;
        items.put(objectId, new Item(objectId, definition, state.quantity(), state.equipped(),
                state.durability(), state.enchantLevel(), state.identified(),
                state.blessing(), state.sourceBound(), state.tournamentReward(),
                state.chargeCount(), state.expiresAtEpochMillis(), state.sealed(),
                state.litoOwned()));
        return objectId;
    }

    /** Transfers one native object without discarding its mutable item state. */
    synchronized boolean add(Item source, long quantity) {
        return add(source, quantity, RuntimeInventory::nextObjectId);
    }

    synchronized boolean add(Item source, long quantity, LongSupplier allocator) {
        if (source == null || quantity <= 0 || quantity > source.quantity()
                || quantity > Integer.MAX_VALUE || allocator == null) {
            return false;
        }
        return addState(source.definition(), new PersistentItem(
                quantity == source.quantity() ? source.objectId() : 0,
                (int) quantity, source.enchantLevel(), source.durability(),
                source.identified(), source.blessing(), source.sourceBound(),
                source.tournamentReward(), source.equipped(),
                durableDefinitionOverride(source.definition()),
                source.chargeCount(), source.expiresAtEpochMillis(), source.sealed(),
                source.litoOwned()
                        || quantity < source.quantity() && source.sourceBound()), allocator);
    }

    /**
     * Retained {@code L1Inventory.storeTradeItem}: a transferred stackable
     * object merges with the first owned object of the same item id.  The
     * destination object's mutable state wins; identification and blessing
     * do not split the stack in the original server.  A new stack keeps the
     * transferred object id and state when the whole ground object moves.
     */
    synchronized boolean addTransferred(Item source, long quantity) {
        return addTransferred(source, quantity, RuntimeInventory::nextObjectId);
    }

    /**
     * Same retained trade admission as {@link #addTransferred(Item, long)},
     * but returns the destination object whose identity must be used by the
     * native NPC digest timer.  A stack merge therefore returns the already
     * owned stack rather than the sender's object id.
     */
    synchronized Item addTransferredResult(Item source, long quantity) {
        return addTransferredResult(source, quantity, RuntimeInventory::nextObjectId);
    }

    synchronized Item addTransferredResult(Item source, long quantity, LongSupplier allocator) {
        return addTransferResult(source, quantity, allocator, true);
    }

    /** Native ground pickup uses storeTradeItem without a tradability gate. */
    synchronized Item addGroundTransferredResult(Item source, long quantity) {
        return addTransferResult(
                source, quantity, RuntimeInventory::nextObjectId, false);
    }

    synchronized boolean addGroundTransferred(Item source, long quantity) {
        return addGroundTransferredResult(source, quantity) != null;
    }

    private synchronized Item addTransferResult(Item source, long quantity,
                                   LongSupplier allocator,
                                   boolean requireTransferable) {
        if (source == null || quantity <= 0 || quantity > source.quantity()
                || quantity > Integer.MAX_VALUE || allocator == null
                || requireTransferable && !source.transferable()) {
            return null;
        }
        Item existing = source.definition().stackable()
                ? items.values().stream()
                    .filter(item -> item.definition().catalogId()
                            == source.definition().catalogId())
                    .filter(item -> item.definition().equals(source.definition()))
                    .filter(item -> item.tournamentReward()
                            == source.tournamentReward())
                    .findFirst().orElse(null)
                : null;
        if (existing != null) {
            if (existing.objectId() == source.objectId()) return null;
            long updated = (long) existing.quantity() + quantity;
            if (updated > Integer.MAX_VALUE) return null;
            Item merged = existing.withQuantity((int) updated);
            items.put(existing.objectId(), merged);
            return merged;
        }
        Set<Long> before = Set.copyOf(items.keySet());
        if (!add(source, quantity, allocator)) return null;
        if (source.quantity() == quantity && !before.contains(source.objectId())) {
            Item admitted = items.get(source.objectId());
            if (admitted != null) return admitted;
        }
        for (Item item : items.values()) {
            if (!before.contains(item.objectId())) return item;
        }
        throw new IllegalStateException("transferred item admitted without destination object");
    }

    synchronized boolean addTransferred(Item source, long quantity, LongSupplier allocator) {
        return addTransferredResult(source, quantity, allocator) != null;
    }

    private synchronized boolean addState(InventoryCatalog.Definition definition,
                             PersistentItem state, LongSupplier allocator) {
        if (definition == null || state == null || allocator == null
                || ((state.durability() > 0 || state.enchantLevel() != 0)
                    && !definition.weapon() && !definition.armor())
                || (state.equipped() && !equipStateSupported(definition))) {
            return false;
        }
        if (definition.stackable()) {
            Item existing = items.values().stream()
                    .filter(item -> item.definition().catalogId() == definition.catalogId())
                    .filter(item -> item.definition().equals(definition))
                    .filter(item -> item.enchantLevel() == state.enchantLevel()
                            && item.durability() == state.durability()
                            && item.identified() == state.identified()
                            && item.blessing() == state.blessing()
                            && item.sourceBound() == state.sourceBound()
                            && item.tournamentReward() == state.tournamentReward()
                            && item.equipped() == state.equipped()
                            && item.chargeCount() == state.chargeCount()
                            && item.expiresAtEpochMillis() == state.expiresAtEpochMillis()
                            && item.sealed() == state.sealed())
                    .findFirst().orElse(null);
            if (existing != null) {
                long updated = (long) existing.quantity() + state.quantity();
                if (updated > Integer.MAX_VALUE) return false;
                items.put(existing.objectId(), existing.withQuantity((int) updated));
                return true;
            }
        }
        if (definition.stackable()) {
            if (items.size() >= MAX_SLOTS) return false;
            long objectId = state.objectId() == 0
                    ? uniqueObjectId(allocator) : admittedStableObjectId(state.objectId());
            if (objectId == 0) return false;
            items.put(objectId, new Item(objectId, definition, state.quantity(),
                    state.equipped(),
                    state.durability(), state.enchantLevel(), state.identified(),
                    state.blessing(), state.sourceBound(), state.tournamentReward(),
                    state.chargeCount(), state.expiresAtEpochMillis(), state.sealed(),
                    state.litoOwned()));
            return true;
        }

        if (state.quantity() > MAX_SLOTS - items.size()
                || (state.objectId() != 0 && state.quantity() != 1)) return false;
        for (int index = 0; index < state.quantity(); index++) {
            long objectId = state.objectId() == 0
                    ? uniqueObjectId(allocator) : admittedStableObjectId(state.objectId());
            if (objectId == 0) return false;
            items.put(objectId, new Item(objectId, definition, 1, state.equipped(),
                    state.durability(), state.enchantLevel(), state.identified(),
                    state.blessing(), state.sourceBound(), state.tournamentReward(),
                    state.chargeCount(), state.expiresAtEpochMillis(), state.sealed(),
                    state.litoOwned()));
        }
        return true;
    }

    synchronized boolean decrementCharge(long objectId) {
        Item existing = items.get(objectId);
        if (existing == null || existing.chargeCount() <= 0) return false;
        items.put(objectId, existing.withChargeCount(existing.chargeCount() - 1));
        return true;
    }

    private static boolean equipStateSupported(InventoryCatalog.Definition definition) {
        return definition.weapon() || definition.ring() || definition.light()
                || definition.sourceArmor();
    }

    synchronized boolean remove(long objectId, long quantity) {
        Item existing = items.get(objectId);
        if (existing == null || sourcePhysicalWeapons.containsKey(objectId)
                || existing.sourceBound() && !existing.litoOwned()
                    && (existing.definition().weapon()
                        || existing.definition().sourceArmor()
                        || existing.definition().projectile())
                || quantity <= 0 || quantity > existing.quantity()) return false;
        int updated = existing.quantity() - (int) quantity;
        if (updated == 0) items.remove(objectId);
        else items.put(objectId, existing.withQuantity(updated));
        return true;
    }

    synchronized boolean removeCatalog(long catalogId, long quantity) {
        if (catalogId <= 0 || quantity <= 0) return false;
        long available = items.values().stream()
                .filter(item -> item.definition().catalogId() == catalogId)
                .filter(item -> !(item.sourceBound() && !item.litoOwned()
                        && (item.definition().weapon()
                            || item.definition().sourceArmor()
                            || item.definition().projectile())))
                .mapToLong(Item::quantity).sum();
        if (available < quantity) return false;
        long remaining = quantity;
        for (Item item : List.copyOf(items.values())) {
            if (remaining == 0) break;
            if (item.definition().catalogId() != catalogId) continue;
            if (item.sourceBound() && !item.litoOwned() && (item.definition().weapon()
                    || item.definition().sourceArmor()
                    || item.definition().projectile())) continue;
            long consumed = Math.min(remaining, item.quantity());
            if (!remove(item.objectId(), consumed)) {
                throw new IllegalStateException("validated catalog removal failed");
            }
            remaining -= consumed;
        }
        return remaining == 0;
    }

    synchronized Item removeEntire(long objectId) {
        Item item = items.get(objectId);
        if (item != null && item.sourceBound() && !item.litoOwned()
                && (item.definition().weapon() || item.definition().sourceArmor()
                    || item.definition().projectile())) {
            return null;
        }
        return items.remove(objectId);
    }

    /** Native death lifecycle removal; caller must persist source tombstones. */
    synchronized Item removeEntireForDeath(long objectId) {
        Item removed = items.remove(objectId);
        if (removed != null) sourcePhysicalWeapons.remove(objectId);
        return removed;
    }

    /**
     * Retained {@code ServerItemUserTimer}: an absolute item deadline expires
     * only when it is strictly earlier than the scan timestamp. This internal
     * lifecycle removal deliberately bypasses the ordinary source-bound
     * transfer gate; callers must persist source tombstones before publishing
     * the resulting inventory.
     */
    synchronized List<Item> expireTimedItems(long nowEpochMillis) {
        if (nowEpochMillis < 0) {
            throw new IllegalArgumentException("negative timed-item scan timestamp");
        }
        List<Item> expired = new ArrayList<>();
        for (Item item : List.copyOf(items.values())) {
            if (item.expiresAtEpochMillis() == 0L
                    || item.expiresAtEpochMillis() >= nowEpochMillis) {
                continue;
            }
            Item removed = items.remove(item.objectId());
            if (removed != null) {
                sourcePhysicalWeapons.remove(removed.objectId());
                expired.add(removed);
            }
        }
        return List.copyOf(expired);
    }

    /** Internal retained attack debit; generic inventory mutations remain blocked. */
    synchronized Item consumeSourceProjectile(long objectId) {
        Item existing = items.get(objectId);
        if (existing == null || !existing.sourceBound()
                || !existing.definition().projectile() || existing.equipped()) {
            return null;
        }
        int updated = existing.quantity() - 1;
        if (updated == 0) items.remove(objectId);
        else items.put(objectId, existing.withQuantity(updated));
        return existing;
    }

    /** Preserves the native object id while a closed physical letter becomes open. */
    synchronized boolean replaceDefinition(long objectId, InventoryCatalog.Definition definition) {
        Item item = items.get(objectId);
        if (item == null || definition == null || item.quantity() != 1
                || definition.stackable()) {
            return false;
        }
        items.put(objectId, item.withDefinition(definition));
        return true;
    }

    synchronized List<Item> repairableWeapons() {
        return items.values().stream()
                .filter(item -> item.definition().weapon() && item.durability() > 0)
                .toList();
    }

    synchronized boolean damageWeapon(long objectId, int durability) {
        Item item = items.get(objectId);
        if (item == null || !item.definition().weapon()
                || durability <= 0 || durability > 0x7F) {
            return false;
        }
        items.put(objectId, item.withDurability(durability));
        return true;
    }

    synchronized boolean damageFirstWeapon(int durability) {
        Item item = items.values().stream()
                .filter(candidate -> candidate.definition().weapon())
                .findFirst().orElse(null);
        return item != null && damageWeapon(item.objectId(), durability);
    }

    synchronized boolean repairWeapon(long objectId) {
        Item item = items.get(objectId);
        if (item == null || !item.definition().weapon() || item.durability() <= 0) {
            return false;
        }
        items.put(objectId, item.withDurability(0));
        return true;
    }

    /** Retained Hone executor recovers one point, for damaged weapons or armor. */
    synchronized boolean recoverDamage(long objectId) {
        Item item = items.get(objectId);
        if (item == null || (!item.definition().weapon() && !item.definition().armor())
                || item.durability() <= 0) {
            return false;
        }
        items.put(objectId, item.withDurability(item.durability() - 1));
        return true;
    }

    synchronized boolean createMagicalWeapon(long objectId) {
        Item item = items.get(objectId);
        InventoryCatalog.NativeWeaponStatus status = item == null ? null
                : InventoryCatalog.nativeWeaponStatus(item.definition().catalogId());
        if (item == null || !item.definition().weapon() || status == null
                || status.safeEnchant() <= 0 || item.enchantLevel() != 0) {
            return false;
        }
        items.put(objectId, item.withEnchantLevel(1));
        return true;
    }

    synchronized boolean identify(long objectId) {
        Item item = items.get(objectId);
        if (item == null) return false;
        if (!item.identified()) items.put(objectId, item.withIdentified(true));
        return true;
    }

    synchronized boolean setBlessing(long objectId, int blessing) {
        Item item = items.get(objectId);
        if (item == null || blessing < BLESSING_BLESSED
                || blessing > BLESSING_UNIDENTIFIED) {
            return false;
        }
        if (item.blessing() != blessing) items.put(objectId, item.withBlessing(blessing));
        return true;
    }

    /** Converts every unsealed native cursed display state to ordinary. */
    synchronized List<Item> removeCurses() {
        List<Item> changed = new ArrayList<>();
        for (Map.Entry<Long, Item> entry : List.copyOf(items.entrySet())) {
            Item item = entry.getValue();
            if (item.blessing() != BLESSING_CURSED) continue;
            Item replacement = item.withBlessing(BLESSING_NORMAL);
            items.put(entry.getKey(), replacement);
            changed.add(replacement);
        }
        return List.copyOf(changed);
    }

    synchronized List<Item> toggleWeapon(long objectId) {
        return toggleWeaponInternal(objectId, false);
    }

    /**
     * Stages one retained source weapon for an authoritative SQL equipment
     * evaluation.  The caller must bind the exact returned physical row with
     * {@link #qualifySourceWeapon(long, SourceWeaponPhysicalStats)} before the
     * candidate inventory can become live.
     */
    synchronized List<Item> toggleSourceWeaponForEvaluation(long objectId) {
        Item selected = items.get(objectId);
        if (selected == null || !selected.sourceBound() || selected.litoOwned()
                || !selected.definition().weapon()) {
            return List.of();
        }
        return toggleWeaponInternal(objectId, true);
    }

    private synchronized List<Item> toggleWeaponInternal(long objectId,
                                            boolean allowUnqualifiedSource) {
        Item selected = items.get(objectId);
        if (selected == null || !selected.definition().weapon()
                || selected.sourceBound() && !selected.litoOwned()
                    && !allowUnqualifiedSource
                    && !sourcePhysicalWeapons.containsKey(objectId)) {
            return List.of();
        }
        boolean equip = !selected.equipped();
        List<Item> changed = new ArrayList<>();
        for (Map.Entry<Long, Item> entry : List.copyOf(items.entrySet())) {
            Item item = entry.getValue();
            boolean next = item.objectId() == objectId ? equip
                    : item.definition().weapon() && item.equipped() ? false : item.equipped();
            if (next == item.equipped()) continue;
            Item replacement = item.withEquipped(next);
            items.put(entry.getKey(), replacement);
            changed.add(replacement);
        }
        return List.copyOf(changed);
    }

    /** Binds exact source combat provenance to a staged runtime object. */
    synchronized void qualifySourceWeapon(long objectId, SourceWeaponPhysicalStats weapon) {
        Item item = items.get(objectId);
        if (item == null || weapon == null || !weapon.present()
                || !item.sourceBound() || item.litoOwned()
                || !item.definition().weapon()
                || item.definition().catalogId() != weapon.itemId()
                || item.enchantLevel() != (byte) weapon.enchantLevel()) {
            throw new IllegalArgumentException(
                    "source weapon qualification does not match runtime instance");
        }
        sourcePhysicalWeapons.put(objectId, weapon);
    }

    /** Native polymorph equipment pass: remove every currently wielded weapon. */
    synchronized List<Item> unequipWeapons() {
        List<Item> changed = new ArrayList<>();
        for (Map.Entry<Long, Item> entry : List.copyOf(items.entrySet())) {
            Item item = entry.getValue();
            if (!item.equipped() || !item.definition().weapon()) continue;
            Item replacement = item.withEquipped(false);
            items.put(entry.getKey(), replacement);
            changed.add(replacement);
        }
        return List.copyOf(changed);
    }

    synchronized List<Item> toggleLight(long objectId) {
        Item selected = items.get(objectId);
        if (selected == null || !selected.definition().light()) return List.of();
        Item replacement = selected.withEquipped(!selected.equipped());
        items.put(objectId, replacement);
        return List.of(replacement);
    }

    /**
     * Toggles one native ring without inventing expansion slots.
     * LiTo 3.81's base equipment model exposes two ring positions; a third
     * equip request is rejected without replacing either authoritative item.
     */
    synchronized List<Item> toggleRing(long objectId) {
        Item selected = items.get(objectId);
        if (selected == null || !selected.definition().ring()
                || selected.definition().sourceArmor()) return List.of();
        if (!selected.equipped() && equippedRingCount() >= NATIVE_RING_SLOT_LIMIT) {
            return List.of();
        }
        Item replacement = selected.withEquipped(!selected.equipped());
        items.put(objectId, replacement);
        return List.of(replacement);
    }

    synchronized int equippedRingCount() {
        return (int) items.values().stream()
                .filter(Item::equipped)
                .filter(item -> item.definition().ring())
                .count();
    }

    /**
     * Applies the retained L1EquipmentSlot armor topology to one exact
     * source-bound armor instance.  The client wire use type intentionally
     * collapses several positions, so decisions use NativeArmorStatus's
     * retained armor type instead.
     */
    synchronized List<Item> toggleSourceArmor(long objectId) {
        Item selected = items.get(objectId);
        if (selected == null || !selected.sourceBound()
                || !selected.definition().sourceArmor()) return List.of();
        return toggleNativeArmor(objectId);
    }

    /** Applies the same retained armor-slot topology to a LiTo-owned armor. */
    synchronized List<Item> toggleNativeArmor(long objectId) {
        Item selected = items.get(objectId);
        if (selected == null || !selected.definition().sourceArmor()) return List.of();
        InventoryCatalog.NativeArmorStatus status =
                selected.definition().sourceArmorStatus();
        int armorType = status.armorType();
        if (selected.equipped()) {
            if (selected.blessing() == BLESSING_CURSED
                    || nativeArmorRemovalBlocked(armorType)) return List.of();
        } else if (nativeArmorEquipBlocked(selected, armorType)) {
            return List.of();
        }
        Item replacement = selected.withEquipped(!selected.equipped());
        items.put(objectId, replacement);
        return List.of(replacement);
    }

    synchronized int equippedSourceArmorCount() {
        return (int) items.values().stream()
                .filter(Item::equipped)
                .filter(item -> item.sourceBound()
                        && item.definition().sourceArmor())
                .count();
    }

    /** Exact retained {@code getHasteItemEquipped() > 0} armor predicate. */
    synchronized boolean hasEquippedHasteArmor() {
        return items.values().stream()
                .filter(Item::equipped)
                .map(Item::definition)
                .map(InventoryCatalog.Definition::sourceArmorStatus)
                .filter(Objects::nonNull)
                .anyMatch(InventoryCatalog.NativeArmorStatus::haste);
    }

    synchronized Item equippedSourceArmor(int armorType) {
        return items.values().stream()
                .filter(Item::equipped)
                .filter(item -> item.sourceBound()
                        && item.definition().sourceArmor())
                .filter(item -> item.definition().sourceArmorStatus().armorType()
                        == armorType)
                .findFirst().orElse(null);
    }

    /** LiTo-owned armor effects not already represented in the source snapshot. */
    synchronized NativeArmorBonuses equippedNativeArmorBonuses() {
        int armorClass = 0;
        int hitPoints = 0;
        int manaPoints = 0;
        int charisma = 0;
        for (Item item : items.values()) {
            if (!item.equipped() || item.sourceBound()
                    || !item.definition().sourceArmor()) continue;
            InventoryCatalog.NativeArmorStatus armor =
                    item.definition().sourceArmorStatus();
            armorClass = Math.addExact(armorClass,
                    Math.subtractExact(-armor.displayArmorClass(),
                            item.enchantLevel()));
            hitPoints = Math.addExact(hitPoints, armor.hitPoints());
            manaPoints = Math.addExact(manaPoints, armor.manaPoints());
            charisma = Math.addExact(charisma, armor.charisma());
        }
        return new NativeArmorBonuses(
                armorClass, hitPoints, manaPoints, charisma);
    }

    synchronized RentalEquipmentBonuses equippedRentalBonuses() {
        return RentalEquipmentBonuses.from(items());
    }

    record NativeArmorBonuses(
            int armorClassModifier, int maximumHp,
            int maximumMp, int charisma) {}

    private synchronized boolean nativeArmorEquipBlocked(Item selected, int armorType) {
        if (armorType == 3 && (hasEquippedNativeArmorType(2)
                || hasEquippedNativeArmorType(4))) return true;
        if (armorType == 2 && hasEquippedNativeArmorType(4)) return true;
        return switch (armorType) {
            case 7, 13 -> hasEquippedNativeArmorType(7)
                    || hasEquippedNativeArmorType(13);
            case 9, 11 -> equippedRingCount() >= NATIVE_RING_SLOT_LIMIT;
            case 12 -> hasEquippedNativeArmorType(12);
            case 14, 15 -> equippedNativeArmorTypeCount(14, 15) >= 2
                    || hasEquippedNativeArmorCatalog(selected.definition().catalogId());
            case 19, 20, 21, 22, 23, 24 -> hasEquippedNativeArmorType(armorType);
            case 25 -> equippedNativeArmorTypeCount(25) >= 4;
            default -> hasEquippedNativeArmorType(armorType);
        };
    }

    private synchronized boolean nativeArmorRemovalBlocked(int armorType) {
        if (armorType == 3 && hasEquippedNativeArmorType(2)) return true;
        return (armorType == 2 || armorType == 3)
                && hasEquippedNativeArmorType(4);
    }

    private synchronized boolean hasEquippedNativeArmorType(int armorType) {
        return equippedNativeArmorTypeCount(armorType) > 0;
    }

    private synchronized int equippedNativeArmorTypeCount(int... armorTypes) {
        int count = 0;
        for (Item item : items.values()) {
            if (!item.equipped() || !item.definition().sourceArmor()) continue;
            int equippedType = item.definition().sourceArmorStatus().armorType();
            for (int armorType : armorTypes) {
                if (equippedType == armorType) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    private synchronized boolean hasEquippedNativeArmorCatalog(long catalogId) {
        return items.values().stream()
                .anyMatch(item -> item.equipped()
                        && item.definition().sourceArmor()
                        && item.definition().catalogId() == catalogId);
    }

    /** Compatibility name retained for existing control-ability callers. */
    synchronized List<Item> toggleControlRing(long objectId) {
        return toggleRing(objectId);
    }

    /** Compatibility name; the physical native limit applies to all rings. */
    synchronized int equippedControlRingCount() {
        return equippedRingCount();
    }

    synchronized boolean controlAbilityEnabled(int abilityType) {
        if (abilityType < 0 || abilityType >= 6) return false;
        return items.values().stream()
                .filter(Item::equipped)
                .anyMatch(item -> item.definition().controlAbilityType() == abilityType);
    }

    synchronized int activeLightRange() {
        return items.values().stream()
                .filter(Item::equipped)
                .map(Item::definition)
                .filter(InventoryCatalog.Definition::light)
                .mapToInt(InventoryCatalog.Definition::lightRange)
                .max().orElse(0);
    }

    synchronized int activeFireResistance() {
        int total = items.values().stream()
                .filter(Item::equipped)
                .filter(item -> !item.definition().sourceArmor())
                .mapToInt(item -> item.definition().fireResistance())
                .sum();
        return Math.max(-128, Math.min(127, total));
    }

    synchronized int equippedWalkAction() {
        return items.values().stream()
                .filter(Item::equipped)
                .map(Item::definition)
                .filter(InventoryCatalog.Definition::weapon)
                .mapToInt(InventoryCatalog.Definition::weaponWalkAction)
                .findFirst().orElse(BattleActionCodes.WALK);
    }

    synchronized boolean hasEquippedWeapon() {
        return items.values().stream()
                .anyMatch(item -> item.equipped() && item.definition().weapon());
    }

    /** Exact equipped-instance lookup used by retained item-specific spell costs. */
    synchronized boolean hasEquippedCatalog(long catalogId) {
        if (catalogId <= 0 || catalogId > 0xFFFF_FFFFL) return false;
        return items.values().stream()
                .anyMatch(item -> item.equipped()
                        && item.definition().catalogId() == catalogId);
    }

    synchronized Item equippedWeaponItem() {
        return items.values().stream()
                .filter(item -> item.equipped() && item.definition().weapon())
                .findFirst().orElse(null);
    }

    synchronized SourceWeaponPhysicalStats equippedPhysicalWeapon() {
        Item item = equippedWeaponItem();
        if (item == null) return SourceWeaponPhysicalStats.none();
        SourceWeaponPhysicalStats exactSource = sourcePhysicalWeapons.get(item.objectId());
        if (exactSource != null) {
            SourceWeaponPhysicalStats result = exactSource
                    .withDurability(item.durability());
            if (result.ranged()) {
                // Equipment re-evaluation can refresh an old source arrow
                // snapshot after the Tournament boundary removed that stack.
                // Select ammunition from the live inventory on every attack.
                result = result.withoutProjectile();
                SourceProjectilePhysicalStats projectile =
                        qualifiedRuntimeProjectile(result.weaponType());
                if (projectile.present()) result = result.withProjectile(projectile);
            }
            return result;
        }
        if (item.sourceBound() && !item.litoOwned()) {
            throw new IllegalStateException(
                    "equipped source weapon has no per-instance combat provenance: "
                            + item.objectId());
        }
        InventoryCatalog.NativeWeaponStatus status =
                item.definition().effectiveWeaponStatus();
        int damageSmall = status == null ? syntheticWeaponDamage(item) : status.damageSmall();
        int damageLarge = status == null ? syntheticWeaponDamage(item) : status.damageLarge();
        int weaponType = weaponTypeForWalk(item.definition().weaponWalkAction());
        RentalItemDefinitionCatalog.SourceTemplate rentalSource =
                RentalItemDefinitionCatalog.source((int) item.definition().catalogId());
        int range = rentalSource != null && "weapon".equals(rentalSource.kind())
                ? nativeRuntimeWeaponRange(weaponType, rentalSource.integer("range"))
                : switch (weaponType) {
                    case 4, 13 -> 15;
                    case 10 -> 10;
                    default -> 1;
                };
        SourceWeaponPhysicalStats result = new SourceWeaponPhysicalStats(
                (int) item.definition().catalogId(),
                weaponType, damageSmall, damageLarge, range,
                status == null ? 0 : status.hitModifier(),
                status == null ? 0 : status.damageModifier(),
                item.enchantLevel(), item.durability());
        if (rentalSource != null && "weapon".equals(rentalSource.kind())) {
            result = result.withCombatTraits(new SourceWeaponCombatTraits(
                    ClassicWorldDropCatalog.materialCode(
                            rentalSource.fields().get("material")),
                    // T120 (#339): template bless (1-伺服器 L1AttackPc.java:113),
                    // stored in the 1-伺服器 internal convention like 380_db.
                    BattlePhysicalOffense.nativeBlessingFromSource(
                            rentalSource.integer("bless")),
                    0, 0, 0,
                    rentalSource.integer("dmg_cri_chance"),
                    rentalSource.decimal("dmg_cri"),
                    rentalSource.integer("dmg_max_chance"),
                    rentalSource.integer("dmg_cri_effect") == 1,
                    rentalSource.integer("dmg_max_effect") == 1,
                    rentalSource.integer("invgfx")));
        }
        if (result.ranged()) {
            SourceProjectilePhysicalStats projectile =
                    qualifiedRuntimeProjectile(result.weaponType());
            if (projectile.present()) result = result.withProjectile(projectile);
        }
        return result.withNameId(item.definition().name());
    }

    /**
     * The retained weapon table uses {@code range=-1} for the gauntlet.  The
     * source server converts that sentinel to the native type-10 projectile
     * envelope; it is not a negative world distance.
     */
    private static int nativeRuntimeWeaponRange(int weaponType, int sourceRange) {
        if (sourceRange >= 0) return sourceRange;
        if (weaponType == 10) return 10;
        throw new IllegalStateException(
                "unsupported negative rental weapon range " + sourceRange
                        + " for type " + weaponType);
    }

    private synchronized SourceProjectilePhysicalStats qualifiedRuntimeProjectile(int weaponType) {
        int nativeUseType = weaponType == 10 ? 0xFD : 0xFE;
        for (Item item : items.values()) {
            if (item.equipped() || item.quantity() <= 0
                    || item.definition().useType() != nativeUseType) continue;
            InventoryCatalog.NativeProjectileStatus status =
                    InventoryCatalog.nativeProjectileStatus(
                            item.definition().catalogId());
            if (status == null) continue;
            return new SourceProjectilePhysicalStats(
                    item.objectId(), (int) item.definition().catalogId(),
                    weaponType == 10 ? -3 : -2,
                    status.damageSmall(), status.damageLarge(),
                    status.material(), item.blessing(), status.arrowGfx(),
                    status.consumeOnAttack(), item.quantity());
        }
        return SourceProjectilePhysicalStats.none();
    }

    synchronized boolean hasQualifiedPhysicalWeapon(long objectId) {
        Item item = items.get(objectId);
        if (item == null || !item.definition().weapon()) return false;
        return !item.sourceBound() || sourcePhysicalWeapons.containsKey(objectId);
    }

    private static int syntheticWeaponDamage(Item item) {
        // This catalog row exists only in isolated development fixtures. It
        // retains the historical ten-point die until assigned a source row.
        return item.definition().catalogId() == InventoryCatalog.TRAINING_SWORD ? 10 : 0;
    }

    private static int weaponTypeForWalk(int walkAction) {
        return switch (walkAction) {
            case BattleActionCodes.SWORD_WALK -> 1;
            case BattleActionCodes.DAGGER_WALK -> 2;
            case BattleActionCodes.TWO_HAND_SWORD_WALK -> 3;
            case BattleActionCodes.BOW_WALK -> 4;
            case BattleActionCodes.SPEAR_WALK -> 5;
            case BattleActionCodes.AXE_WALK -> 6;
            case BattleActionCodes.STAFF_WALK -> 7;
            case BattleActionCodes.THROWING_KNIFE_WALK -> 10;
            case BattleActionCodes.CLAW_WALK -> 11;
            case BattleActionCodes.EDORYU_WALK -> 12;
            case BattleActionCodes.CHAINSWORD_WALK -> 18;
            default -> throw new IllegalStateException(
                    "runtime weapon has unsupported native walk action " + walkAction);
        };
    }

    synchronized Map<Long, Integer> catalogCounts() {
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (Item item : items.values()) {
            result.merge(item.definition().catalogId(), item.quantity(), Math::addExact);
        }
        return result;
    }

    synchronized Map<Long, List<Integer>> catalogEnchantments() {
        Map<Long, List<Integer>> result = new LinkedHashMap<>();
        Map<Long, List<Integer>> staged = new LinkedHashMap<>();
        for (Item item : items.values()) {
            if (!item.definition().weapon()) continue;
            staged.computeIfAbsent(item.definition().catalogId(), ignored -> new ArrayList<>())
                    .add(item.enchantLevel());
        }
        staged.forEach((catalogId, levels) -> {
            int lastNonZero = -1;
            for (int index = 0; index < levels.size(); index++) {
                if (levels.get(index) != 0) lastNonZero = index;
            }
            if (lastNonZero >= 0) {
                result.put(catalogId, List.copyOf(levels.subList(0, lastNonZero + 1)));
            }
        });
        return Map.copyOf(result);
    }

    synchronized Map<Long, List<PersistentItem>> catalogItemStates() {
        Map<Long, List<PersistentItem>> staged = new LinkedHashMap<>();
        Map<Long, Boolean> nonDefault = new LinkedHashMap<>();
        for (Item item : items.values()) {
            long catalogId = item.definition().catalogId();
            PersistentItem state = item.persistentItem();
            staged.computeIfAbsent(catalogId, ignored -> new ArrayList<>()).add(state);
            if (!state.isDefaultFor(item.definition())) nonDefault.put(catalogId, true);
        }
        Map<Long, List<PersistentItem>> result = new LinkedHashMap<>();
        staged.forEach((catalogId, states) -> {
            if (nonDefault.getOrDefault(catalogId, false)) {
                result.put(catalogId, List.copyOf(states));
            }
        });
        return Map.copyOf(result);
    }

    private static InventoryCatalog.Definition durableDefinitionOverride(
            InventoryCatalog.Definition definition) {
        if (definition == null) return null;
        InventoryCatalog.Definition builtIn = InventoryCatalog.find(definition.catalogId());
        return definition.equals(builtIn) ? null : definition;
    }

    private synchronized void applyCatalogEnchantments(Map<Long, List<Integer>> enchantments) {
        if (enchantments == null || enchantments.isEmpty()) return;
        Map<Long, Integer> indexes = new LinkedHashMap<>();
        for (Map.Entry<Long, Item> entry : List.copyOf(items.entrySet())) {
            Item item = entry.getValue();
            List<Integer> levels = enchantments.get(item.definition().catalogId());
            if (levels == null) continue;
            int index = indexes.getOrDefault(item.definition().catalogId(), 0);
            indexes.put(item.definition().catalogId(), index + 1);
            if (index >= levels.size()) continue;
            Integer level = levels.get(index);
            if (level == null || level == 0 || !item.definition().weapon()
                    || level < -128 || level > 127) continue;
            items.put(entry.getKey(), item.withEnchantLevel(level));
        }
    }

    private synchronized long uniqueObjectId(LongSupplier allocator) {
        for (int attempt = 0; attempt < 1_024; attempt++) {
            long candidate = allocator.getAsLong();
            if (candidate > 0 && candidate <= 0xFFFF_FFFFL && !items.containsKey(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("cannot allocate a unique native inventory object id");
    }

    /** Admits one persisted/transferred native id without silently renumbering it. */
    private synchronized long admittedStableObjectId(long objectId) {
        if (objectId <= 0 || objectId > 0xFFFF_FFFFL || items.containsKey(objectId)) {
            return 0;
        }
        reserveDefaultObjectId(objectId);
        return objectId;
    }

    private static void reserveDefaultObjectId(long objectId) {
        if (objectId < FIRST_OBJECT_ID || objectId >= 0x7FFF_FFFFL) return;
        long next = objectId + 1;
        NEXT_OBJECT_ID.getAndUpdate(current ->
                current >= next && current <= 0x7FFF_FFFFL ? current : next);
    }

    private static long nextObjectId() {
        return NEXT_OBJECT_ID.getAndUpdate(current ->
                current >= 0x7FFF_FFFFL ? FIRST_OBJECT_ID : current + 1);
    }

    synchronized Map nativeDeItems() {
        return this.items;
    }

    synchronized Map nativeDePhysical() {
        return this.sourcePhysicalWeapons;
    }

    synchronized void nativeDeBind(Map nativeItems) {
        items = nativeItems;
    }
}
