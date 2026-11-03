// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import local.lito381.server.BattleSpellCatalog;
import local.lito381.server.ClientSession;
import local.lito381.server.NativeLightAction;
import local.lito381.server.NativePetWorldLifetime;

final class NativePetMaterialAction {
    static void taming(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.TamingSpell tamingSpell) throws IOException {
        NativePetWorldLifetime.run(clientSession, byArray, () -> clientSession.nativeMaterialTamingBody(byArray, tamingSpell));
    }

    static void zombie(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.CreateZombieSpell createZombieSpell) throws IOException {
        NativePetWorldLifetime.run(clientSession, byArray, () -> clientSession.nativeMaterialZombieBody(byArray, createZombieSpell));
    }

    static boolean admit(ClientSession clientSession, ClientSession clientSession2, int n, int n2, int n3) {
        if (clientSession2 != clientSession || n != 36 && n != 41) {
            throw new IllegalStateException("unreviewed pet material root");
        }
        return NativeLightAction.admitPair(clientSession, clientSession, n, n2, n3);
    }

    private NativePetMaterialAction() {
    }
}
