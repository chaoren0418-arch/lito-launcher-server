// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import local.lito381.server.BattleSpellCatalog;
import local.lito381.server.ClientSession;
import local.lito381.server.NativeCastMaterial;
import local.lito381.server.NativeLightAction;

final class NativeMagicalWeaponAction {
    static void cast(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.MagicalWeaponSpell magicalWeaponSpell) throws IOException {
        NativeLightAction.commitSelf(clientSession, () -> clientSession.nativeMaterialMagicalWeaponBody(byArray, magicalWeaponSpell));
    }

    static boolean admit(ClientSession clientSession, BattleSpellCatalog.MagicalWeaponSpell magicalWeaponSpell, long l) {
        if (magicalWeaponSpell.skillId() != 73) {
            throw new IllegalStateException("unreviewed magical weapon skill");
        }
        NativeCastMaterial.Cast cast = NativeLightAction.material(clientSession, 73);
        if (cast.effect != null || cast.resolved) {
            throw new IllegalStateException("duplicate item effect");
        }
        cast.effect = runtimeInventory -> runtimeInventory.createMagicalWeapon(l);
        return NativeLightAction.admitSelf(clientSession, 73, magicalWeaponSpell.actionId(), magicalWeaponSpell.reuseDelayMillis());
    }

    static boolean enchanted(ClientSession clientSession) {
        NativeCastMaterial.Cast cast = NativeLightAction.currentMaterial(clientSession);
        if (cast == null || !cast.resolved || cast.effect == null || !clientSession.nativePlayerActionLock().isHeldByCurrentThread()) {
            throw new IllegalStateException("uncommitted item effect");
        }
        return cast.effectResult;
    }

    private NativeMagicalWeaponAction() {
    }
}
