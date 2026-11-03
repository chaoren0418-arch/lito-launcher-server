// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.ClientSession;
import local.lito381.server.CombatRules;
import local.lito381.server.RuntimeInventory;

final class CombatInvisibilityRules {
    CombatInvisibilityRules() {
    }

    static boolean breakOnCast() {
        return CombatRules.number("INVIS_CAST_BREAK", 1.0) != 0.0;
    }

    static boolean toggle(boolean bl, ClientSession clientSession) {
        if (!bl || !CombatInvisibilityRules.breakOnCast()) {
            return false;
        }
        RuntimeInventory runtimeInventory = clientSession.nativeDeInventory(0);
        return !runtimeInventory.hasEquippedCatalog(20077L) && !runtimeInventory.hasEquippedCatalog(120077L) && !runtimeInventory.hasEquippedCatalog(20062L);
    }
}
