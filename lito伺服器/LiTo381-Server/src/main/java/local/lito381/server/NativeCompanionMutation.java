// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.BattleCompanion;
import local.lito381.server.BattleTimedStatus;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.NativeCombatAttacker;
import local.lito381.server.NativeCompanionCallbacks;

final class NativeCompanionMutation {
    NativeCompanionMutation() {
    }

    private static boolean life(ClientSession clientSession, Object object, BattleCompanion battleCompanion, String string) {
        if (CombatAccess.field(clientSession, string) != object) {
            return false;
        }
        NativeCompanionCallbacks.Stamp stamp = (NativeCompanionCallbacks.Stamp)CombatAccess.field(object, "sourceCompanionCallback");
        return stamp != null && stamp.claimed.get() && stamp.companion == battleCompanion.snapshot().objectId() && stamp.life == NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(battleCompanion, "timedStatus"), false) && System.nanoTime() - ((Number)CombatAccess.field(object, "expiresAtNanos")).longValue() < 0L;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean selectTarget(BattleCompanion battleCompanion, long l, ClientSession clientSession, Object object) {
        if (!NativeCompanionCallbacks.readySlave(clientSession, object, battleCompanion)) {
            return false;
        }
        BattleCompanion battleCompanion2 = battleCompanion;
        synchronized (battleCompanion2) {
            return NativeCompanionMutation.life(clientSession, object, battleCompanion, "pendingSlaveTarget") && battleCompanion.selectTarget(l);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean renameOnce(BattleCompanion battleCompanion, String string, ClientSession clientSession, Object object) {
        if (!NativeCompanionCallbacks.readyRename(clientSession, object, battleCompanion)) {
            return false;
        }
        BattleCompanion battleCompanion2 = battleCompanion;
        synchronized (battleCompanion2) {
            return NativeCompanionMutation.life(clientSession, object, battleCompanion, "pendingPetRename") && battleCompanion.renameOnce(string);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean current(ClientSession clientSession, Object object, BattleCompanion battleCompanion) {
        if (!NativeCompanionCallbacks.readyRename(clientSession, object, battleCompanion)) {
            return false;
        }
        BattleCompanion battleCompanion2 = battleCompanion;
        synchronized (battleCompanion2) {
            return NativeCompanionMutation.life(clientSession, object, battleCompanion, "pendingPetRename");
        }
    }
}
