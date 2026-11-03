// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.BattleTimedStatus;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatRules;
import local.lito381.server.NativeCancellationProbability;
import local.lito381.server.NativeLightAction;
import local.lito381.server.Protocol;

final class NativeCancellationSleep {
    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean activate(BattleTimedStatus battleTimedStatus, long l, int n, int n2) {
        if (n2 != 66 && n2 != 212) {
            throw new IllegalArgumentException("unreviewed sleep producer");
        }
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
        synchronized (battleTimedStatus2) {
            boolean bl = battleTimedStatus.activateSleep(l, n);
            if (bl && CombatRules.loaded()) {
                CombatAccess.set(battleTimedStatus, "sourceSleepLease", new Origin(n2, CombatAccess.field(battleTimedStatus, "sourceSleepLease")));
            }
            return bl;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void removeFog(ClientSession clientSession) {
        Object object;
        BattleTimedStatus battleTimedStatus;
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus = (BattleTimedStatus)CombatAccess.field(clientSession, "timedStatus");
        synchronized (battleTimedStatus2) {
            Origin origin;
            Object object2 = CombatAccess.field(battleTimedStatus, "sourceSleepLease");
            if (!(object2 instanceof Origin) || (origin = (Origin)object2).skill() != 66 || !battleTimedStatus.cancelSleep()) {
                return;
            }
            object = CombatAccess.field(battleTimedStatus, "sourceSleepLease");
        }
        NativeLightAction.deferScoped(() -> {
            BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
            synchronized (battleTimedStatus2) {
                if (CombatAccess.field(battleTimedStatus, "sourceSleepLease") != object) {
                    return;
                }
            }
            NativeCancellationProbability.send(clientSession, Protocol.paralysis(Protocol.ParalysisMode.SLEEP_RELEASE), "S_PARALYSE Cancellation probability fog-release");
        });
    }

    private NativeCancellationSleep() {
    }

    private record Origin(int skill, Object generation) {
    }
}
