// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

record SourceEquipmentElementalStats(int itemEarth, int itemFire, int itemWater, int itemWind, int setEarth, int setFire, int setWater, int setWind) {
    static SourceEquipmentElementalStats none() {
        return new SourceEquipmentElementalStats(0, 0, 0, 0, 0, 0, 0, 0);
    }

    int resistance(int attributeKind) {
        return switch (attributeKind) {
            case 1 -> Math.addExact(this.itemEarth, this.setEarth);
            case 2 -> Math.addExact(this.itemFire, this.setFire);
            case 4 -> Math.addExact(this.itemWater, this.setWater);
            case 8 -> Math.addExact(this.itemWind, this.setWind);
            default -> 0;
        };
    }
}
