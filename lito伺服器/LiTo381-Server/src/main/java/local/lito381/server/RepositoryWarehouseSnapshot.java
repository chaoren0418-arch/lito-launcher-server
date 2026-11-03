// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import local.lito381.server.InventoryCatalog;
import local.lito381.server.RuntimeInventory;
import local.lito381.server.SourceInventoryItem;

record RepositoryWarehouseSnapshot(boolean authoritative, List<SourceInventoryItem> items) {
    RepositoryWarehouseSnapshot {
        items = items == null ? List.of() : List.copyOf(items);
    }

    static RepositoryWarehouseSnapshot unavailable() {
        return new RepositoryWarehouseSnapshot(false, List.of());
    }

    RuntimeInventory runtimeInventory() {
        if (!this.authoritative) {
            return new RuntimeInventory();
        }
        LinkedHashMap<Long, Integer> counts = new LinkedHashMap<Long, Integer>();
        LinkedHashMap<Long, List> states = new LinkedHashMap<Long, List>();
        LinkedHashMap<Long, InventoryCatalog.Definition> definitions = new LinkedHashMap<Long, InventoryCatalog.Definition>();
        for (SourceInventoryItem item : this.items) {
            long catalogId = Integer.toUnsignedLong(item.catalogId());
            counts.merge(catalogId, item.quantity(), Math::addExact);
            InventoryCatalog.Definition definition = item.definition();
            InventoryCatalog.Definition previous = definitions.putIfAbsent(catalogId, definition);
            if (previous != null && !previous.equals(definition)) {
                throw new IllegalStateException("warehouse catalog id resolves to multiple templates: " + catalogId);
            }
            states.computeIfAbsent(catalogId, ignored -> new ArrayList()).add(new RuntimeInventory.PersistentItem(item.sourceObjectId(), item.quantity(), item.nativeEnchantLevel(), item.durability(), item.identified(), item.blessing(), true, item.tournamentReward(), false, definition, Math.min(item.chargeCount(), definition.maximumChargeCount()), item.expiresAtEpochMillis(), item.sealed(), item.litoOwned()));
        }
        LinkedHashMap<Long, List<RuntimeInventory.PersistentItem>> immutable = new LinkedHashMap<Long, List<RuntimeInventory.PersistentItem>>();
        states.forEach((id, rows) -> immutable.put((Long)id, List.copyOf(rows)));
        return RuntimeInventory.fromDurableState(counts, immutable, definitions);
    }
}
