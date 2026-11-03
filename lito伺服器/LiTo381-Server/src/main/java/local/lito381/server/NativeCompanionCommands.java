// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.GameHub;
import local.lito381.server.NativePlayerCommit;

final class NativeCompanionCommands {
    NativeCompanionCommands() {
    }

    static boolean registered(ClientSession clientSession) {
        if (clientSession == null || clientSession.playerObjectId() <= 0L) {
            return false;
        }
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
        int n = NativePlayerCommit.integer(clientSession, "classicWorldMapId");
        int n2 = NativePlayerCommit.integer(clientSession, "classicWorldInstanceId");
        for (ClientSession clientSession2 : gameHub.classicWorldMembers(n, n2)) {
            if (clientSession2 != clientSession) continue;
            return true;
        }
        return false;
    }
}
