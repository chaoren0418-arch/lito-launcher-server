// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

record LitoSourceInventoryMutation(long sourceObjectId, int itemId, int quantity, boolean delete, boolean equipped, boolean sealed, int enchantLevel, boolean identified, int durability, int chargeCount, int blessing, long expiresAtEpochMillis) {
    LitoSourceInventoryMutation {
        if (sourceObjectId <= 0L || sourceObjectId > 0xFFFFFFFFL || itemId <= 0 || !delete && quantity <= 0 || enchantLevel < -128 || enchantLevel > 127 || durability < 0 || durability > 127 || chargeCount < 0 || blessing < 0 || blessing > 3 || expiresAtEpochMillis < 0L) {
            throw new IllegalArgumentException("invalid source inventory mutation");
        }
    }

    static LitoSourceInventoryMutation delete(long sourceObjectId, int itemId) {
        return new LitoSourceInventoryMutation(sourceObjectId, itemId, 0, true, false, false, 0, false, 0, 0, 1, 0L);
    }
}
