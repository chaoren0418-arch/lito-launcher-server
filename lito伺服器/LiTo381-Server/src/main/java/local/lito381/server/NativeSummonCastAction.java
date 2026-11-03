// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import local.lito381.server.BattleSpellCatalog;
import local.lito381.server.ClientSession;
import local.lito381.server.NativeLightAction;

final class NativeSummonCastAction {
    static void cast(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.SummonSpell summonSpell) throws IOException {
        NativeLightAction.commitSelf(clientSession, () -> clientSession.nativeMaterialSummonBody(byArray, summonSpell));
    }

    static boolean admit(ClientSession clientSession, BattleSpellCatalog.SummonSpell summonSpell) {
        if (summonSpell.skillId() != 51) {
            throw new IllegalStateException("unreviewed elemental skill");
        }
        return NativeLightAction.admitSelf(clientSession, summonSpell.skillId(), summonSpell.actionId(), summonSpell.reuseDelayMillis());
    }

    private NativeSummonCastAction() {
    }
}
