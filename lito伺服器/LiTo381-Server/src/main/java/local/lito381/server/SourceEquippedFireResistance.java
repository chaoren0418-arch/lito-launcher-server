// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

record SourceEquippedFireResistance(int value, int matchedArmorRows) {
    SourceEquippedFireResistance {
        if (value < -999999 || value > 999999 || matchedArmorRows < 0) {
            throw new IllegalArgumentException("invalid source equipped fire resistance");
        }
    }

    static SourceEquippedFireResistance none() {
        return new SourceEquippedFireResistance(0, 0);
    }
}
