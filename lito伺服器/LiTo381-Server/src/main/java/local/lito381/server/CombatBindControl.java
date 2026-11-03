// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.BattleTimedStatus;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatRules;
import local.lito381.server.Protocol;

final class CombatBindControl {
    CombatBindControl() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean actionLocked(BattleTimedStatus battleTimedStatus, long l) {
        if (!CombatRules.loaded()) {
            return battleTimedStatus.actionLocked(l);
        }
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
        synchronized (battleTimedStatus2) {
            return battleTimedStatus.sleepActive(l) || battleTimedStatus.freezeVisualLockActive(l) || battleTimedStatus.paralysisActive(l) || battleTimedStatus.shockStunActive(l) || battleTimedStatus.boneBreakActive(l);
        }
    }

    static boolean blocked(ClientSession clientSession) {
        return CombatRules.number("SLAY_BREAK_NOT_TELEPORT", 1.0) != 0.0 && ((BattleTimedStatus)CombatAccess.field(clientSession, "timedStatus")).thunderGrabActive(System.nanoTime());
    }

    static void rejected(ClientSession clientSession) {
        CombatBindControl.send(clientSession, Protocol.manaPoint((Integer)CombatAccess.field(clientSession, "playerMp"), (Integer)CombatAccess.field(clientSession, "playerMaxMp")), "S_MANA_POINT Thunder-Grab accepted teleport cost");
        CombatBindControl.send(clientSession, Protocol.systemMessage(0, "\\fY\u8eab\u4e0a\u6709\u596a\u547d\u4e4b\u96f7\u7684\u6548\u679c\u7121\u6cd5\u77ac\u79fb"), "S_MESSAGE Thunder-Grab teleport disabled");
        CombatBindControl.send(clientSession, Protocol.paralysis(Protocol.ParalysisMode.TELEPORT_UNLOCK), "S_PARALYSE Thunder-Grab teleport unlock");
    }

    private static void send(ClientSession clientSession, byte[] byArray, String string) {
        CombatAccess.call(clientSession, "sendEncrypted", "([BLjava/lang/String;)V", byArray, string);
    }
}
