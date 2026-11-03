// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.util.List;
import local.lito381.server.BattleSpellCatalog;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.GameHub;
import local.lito381.server.NativeLightAction;
import local.lito381.server.Protocol;

final class NativeEarthBindMaterialAction {
    private static ClientSession target(ClientSession clientSession2, byte[] byArray, boolean bl) {
        Protocol.AttackSpellRequest attackSpellRequest = Protocol.attackSpellRequest(byArray);
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession2, "hub");
        List list = bl ? (List)CombatAccess.call(clientSession2, "classicWorldMembers", "(I)Ljava/util/List;", (int)((Integer)CombatAccess.field(clientSession2, "classicWorldMapId"))) : gameHub.roomCombatants((Long)CombatAccess.field(clientSession2, "currentRoomId"));
        return list.stream().filter(clientSession -> clientSession.playerObjectId() == attackSpellRequest.targetObjectId()).findFirst().orElse(clientSession2);
    }

    static void battle(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.BindSpell bindSpell) throws IOException {
        NativeLightAction.commitPair(clientSession, NativeEarthBindMaterialAction.target(clientSession, byArray, false), () -> clientSession.nativeMaterialEarthBindBody(byArray, bindSpell));
    }

    static void world(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.BindSpell bindSpell) throws IOException {
        NativeLightAction.commitPair(clientSession, NativeEarthBindMaterialAction.target(clientSession, byArray, true), () -> clientSession.nativeMaterialWorldEarthBindBody(byArray, bindSpell));
    }

    static boolean admit(ClientSession clientSession, ClientSession clientSession2, int n, int n2, int n3) {
        if (n != 157) {
            throw new IllegalStateException("unreviewed Earth Bind material skill");
        }
        return NativeLightAction.admitPair(clientSession, clientSession2 == null ? clientSession : clientSession2, n, n2, n3);
    }

    private NativeEarthBindMaterialAction() {
    }
}
