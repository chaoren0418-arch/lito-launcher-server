// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.CombatRules;
import local.lito381.server.InventoryCatalog;
import local.lito381.server.RuntimeInventory;

final class SolidCarriageRules {
    SolidCarriageRules() {
    }

    static boolean equipment(boolean bl, boolean bl2) {
        return bl || (int)CombatRules.number("SOLID_CARRIAGE_MODE", 1.0) == 1 && bl2;
    }

    static boolean runtime(RuntimeInventory runtimeInventory) {
        for (RuntimeInventory.Item item : runtimeInventory.items()) {
            if (!item.equipped()) continue;
            InventoryCatalog.NativeArmorStatus nativeArmorStatus = item.definition().sourceArmorStatus();
            if (nativeArmorStatus != null && SolidCarriageRules.equipment(nativeArmorStatus.armorType() == 7, nativeArmorStatus.armorType() == 13)) {
                return true;
            }
            if (CombatRules.loaded() || nativeArmorStatus != null || item.definition().useType() != 25) continue;
            return true;
        }
        return false;
    }
}
