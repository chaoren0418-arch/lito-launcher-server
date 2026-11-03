// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

record SourceDefensiveEquipment(int itemId, int armorType, int icon, int blessing, String nameId) {
    SourceDefensiveEquipment {
        String string = nameId = nameId == null ? "" : nameId;
        if (itemId == 0) {
            if (armorType != 0 || icon != 0 || blessing != 1 || !nameId.isEmpty()) {
                throw new IllegalArgumentException("empty source defensive equipment carries values");
            }
        } else if (itemId < 0 || armorType != 7 && armorType != 13 || icon <= 0 || icon > 65535 || blessing < 0 || blessing > 3 || nameId.isBlank()) {
            throw new IllegalArgumentException("invalid source defensive equipment projection");
        }
    }

    static SourceDefensiveEquipment none() {
        return new SourceDefensiveEquipment(0, 0, 0, 1, "");
    }

    static SourceDefensiveEquipment memoryShield() {
        return new SourceDefensiveEquipment(20220, 7, 508, 1, "$1210");
    }

    static SourceDefensiveEquipment memoryGuarder() {
        return new SourceDefensiveEquipment(20220, 13, 508, 1, "$1210");
    }

    boolean present() {
        return this.itemId != 0;
    }

    int useType() {
        return this.present() ? 25 : 0;
    }
}
