// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

record SourceInventoryWeightStats(int rawWeight, int itemRows, int armorRows, int weaponRows, int etcRows, double serverWeightRate) {
    SourceInventoryWeightStats {
        if (rawWeight < 0 || itemRows < 0 || armorRows < 0 || weaponRows < 0 || etcRows < 0 || itemRows != armorRows + weaponRows + etcRows || !Double.isFinite(serverWeightRate) || serverWeightRate <= 0.0) {
            throw new IllegalArgumentException("invalid source inventory-weight snapshot");
        }
    }

    static SourceInventoryWeightStats none(double serverWeightRate) {
        return new SourceInventoryWeightStats(0, 0, 0, 0, 0, serverWeightRate);
    }
}
