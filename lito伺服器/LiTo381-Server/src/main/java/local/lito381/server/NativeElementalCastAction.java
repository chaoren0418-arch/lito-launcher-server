// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import local.lito381.server.BattleSpellCatalog;
import local.lito381.server.ClientSession;
import local.lito381.server.NativeLightAction;

final class NativeElementalCastAction {
    static void cast(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.ElementalSummonSpell elementalSummonSpell) throws IOException {
        NativeLightAction.commitSelf(clientSession, () -> clientSession.nativeMaterialElementalSummonBody(byArray, elementalSummonSpell));
    }

    static boolean admit(ClientSession clientSession, BattleSpellCatalog.ElementalSummonSpell elementalSummonSpell) {
        if (elementalSummonSpell.skillId() != 154 && elementalSummonSpell.skillId() != 162) {
            throw new IllegalStateException("unreviewed elemental skill");
        }
        return NativeLightAction.admitSelf(clientSession, elementalSummonSpell.skillId(), elementalSummonSpell.actionId(), elementalSummonSpell.reuseDelayMillis());
    }

    private NativeElementalCastAction() {
    }
}
