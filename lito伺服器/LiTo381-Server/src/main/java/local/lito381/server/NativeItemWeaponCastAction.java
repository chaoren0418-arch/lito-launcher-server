// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import local.lito381.server.BattleSpellCatalog;
import local.lito381.server.ClientSession;
import local.lito381.server.NativeLightAction;

final class NativeItemWeaponCastAction {
    static boolean supported(int n) {
        return n == 12 || n == 107;
    }

    static void cast(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.ItemWeaponEnchantSpell itemWeaponEnchantSpell) throws IOException {
        NativeLightAction.commitSelf(clientSession, () -> clientSession.nativeMaterialItemWeaponEnchantBody(byArray, itemWeaponEnchantSpell));
    }

    static boolean admit(ClientSession clientSession, BattleSpellCatalog.ItemWeaponEnchantSpell itemWeaponEnchantSpell) {
        if (!NativeItemWeaponCastAction.supported(itemWeaponEnchantSpell.skillId())) {
            throw new IllegalStateException("unreviewed item weapon buff");
        }
        return NativeLightAction.admitSelf(clientSession, itemWeaponEnchantSpell.skillId(), itemWeaponEnchantSpell.actionId(), itemWeaponEnchantSpell.reuseDelayMillis());
    }

    private NativeItemWeaponCastAction() {
    }
}
