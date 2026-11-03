// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativeProcEncounterCatalog;

final class NativeProcFollowerReceiver {
    private NativeProcFollowerReceiver() {
    }

    static boolean blocked(NativeProcEncounterCatalog.Npc npc, Object object, int n) {
        return npc.receiver() == NativeProcEncounterCatalog.Receiver.FOLLOWER && (n == 0 || NativePlayerCommit.integer(object, "maxHp") <= 100);
    }
}
