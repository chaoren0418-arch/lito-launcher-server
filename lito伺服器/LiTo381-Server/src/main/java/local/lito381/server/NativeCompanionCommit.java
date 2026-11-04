// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.List;
import local.lito381.server.BattleCompanion;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.NativeCompanionCommands;
import local.lito381.server.NativePlayerCommit;

final class NativeCompanionCommit {
    NativeCompanionCommit() {
    }

    static NativePlayerCommit.Binding captureMaster(ClientSession clientSession) {
        return NativePlayerCommit.Binding.capture(clientSession);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean valid(ClientSession clientSession, BattleCompanion battleCompanion, NativePlayerCommit.Binding binding) {
        boolean bl;
        if (clientSession == null || battleCompanion == null || binding == null) {
            return false;
        }
        boolean bl2 = bl = CombatAccess.field(clientSession, "battleCompanion") == battleCompanion;
        if (!bl) {
            List list;
            List list2 = list = (List)CombatAccess.field(clientSession, "tamedCompanions");
            synchronized (list2) {
                for (int i = 0; i < list.size(); ++i) {
                    if (list.get(i) != battleCompanion) continue;
                    bl = true;
                    break;
                }
            }
        }
        return bl && binding.matches(clientSession) && NativeCompanionCommands.registered(clientSession);
    }
}
