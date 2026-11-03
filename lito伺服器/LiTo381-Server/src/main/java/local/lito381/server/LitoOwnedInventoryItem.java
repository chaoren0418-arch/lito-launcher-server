// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.RuntimeInventory;

record LitoOwnedInventoryItem(long objectId, int itemId, String itemName, int quantity, boolean equipped, boolean sealed, int enchantLevel, boolean identified, int durability, int chargeCount, int blessing, long expiresAtEpochMillis, boolean tournamentReward) {
    LitoOwnedInventoryItem {
        String string = itemName = itemName == null ? "" : itemName.trim();
        if (objectId <= 0L || objectId > Integer.MAX_VALUE || itemId <= 0 || itemName.isBlank() || quantity <= 0 || enchantLevel < -128 || enchantLevel > 127 || durability < 0 || durability > 127 || chargeCount < 0 || blessing < 0 || blessing > 3 || expiresAtEpochMillis < 0L) {
            throw new IllegalArgumentException("invalid LiTo-owned inventory item");
        }
    }

    static LitoOwnedInventoryItem from(RuntimeInventory.Item item) {
        if (item == null || item.sourceBound() && !item.litoOwned()) {
            throw new IllegalArgumentException("item is not LiTo-owned");
        }
        return new LitoOwnedInventoryItem(item.objectId(), Math.toIntExact(item.definition().catalogId()), item.definition().name(), item.quantity(), item.equipped(), item.sealed(), item.enchantLevel(), item.identified(), item.durability(), item.chargeCount(), item.blessing(), item.expiresAtEpochMillis(), item.tournamentReward());
    }
}
