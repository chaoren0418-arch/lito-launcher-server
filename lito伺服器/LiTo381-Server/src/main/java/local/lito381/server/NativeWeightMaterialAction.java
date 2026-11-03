// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.BattleSpellCatalog;
import local.lito381.server.ClientSession;
import local.lito381.server.NativeLightAction;

final class NativeWeightMaterialAction {
    static boolean admit(ClientSession clientSession, BattleSpellCatalog.WeightSpell weightSpell) {
        if (weightSpell.skillId() != 14 || weightSpell.actionId() != 19) {
            throw new IllegalArgumentException("unreviewed weight skill contract");
        }
        return NativeLightAction.admitSelf(clientSession, weightSpell.skillId(), weightSpell.actionId(), weightSpell.reuseDelayMillis());
    }

    private NativeWeightMaterialAction() {
    }
}
