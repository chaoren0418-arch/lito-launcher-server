// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.util.List;
import local.lito381.server.BattleCompanion;
import local.lito381.server.BattleSpellCatalog;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.GameHub;
import local.lito381.server.NativeLightAction;
import local.lito381.server.Protocol;

final class NativeNegativeMaterialAction {
    private static ClientSession owner(ClientSession clientSession, long l) {
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
        String string = String.valueOf(CombatAccess.field(clientSession, "phase"));
        List<ClientSession> list = string.equals("WORLD") ? (List<ClientSession>)CombatAccess.call(clientSession, "classicWorldMembers", "(I)Ljava/util/List;", (int)((Integer)CombatAccess.field(clientSession, "classicWorldMapId"))) : (string.equals("BATTLE") ? gameHub.roomCombatants((Long)CombatAccess.field(clientSession, "currentRoomId")) : List.of());
        for (ClientSession clientSession2 : list) {
            if (clientSession2 == clientSession || clientSession2.playerObjectId() != l) continue;
            return clientSession2;
        }
        for (ClientSession clientSession2 : list) {
            List<BattleCompanion.Snapshot> list2 = string.equals("WORLD") ? clientSession2.classicWorldCompanionSnapshots() : clientSession2.controlledCompanionSnapshots();
            for (BattleCompanion.Snapshot snapshot : list2) {
                if (snapshot.objectId() != l || CombatAccess.call(clientSession2, "ownedCompanion", "(J)Llocal/lito381/server/BattleCompanion;", l) == null) continue;
                return clientSession2;
            }
        }
        return clientSession;
    }

    static void entangle(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.EntangleSpell entangleSpell, int n) throws IOException {
        Protocol.AttackSpellRequest attackSpellRequest = Protocol.attackSpellRequest(byArray);
        NativeLightAction.commitPair(clientSession, NativeNegativeMaterialAction.owner(clientSession, attackSpellRequest.targetObjectId()), () -> clientSession.nativeMaterialEntangleBody(byArray, entangleSpell, n));
    }

    static void erase(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.EraseMagicSpell eraseMagicSpell, int n) throws IOException {
        Protocol.AttackSpellRequest attackSpellRequest = Protocol.attackSpellRequest(byArray);
        NativeLightAction.commitPair(clientSession, NativeNegativeMaterialAction.owner(clientSession, attackSpellRequest.targetObjectId()), () -> clientSession.nativeMaterialEraseBody(byArray, eraseMagicSpell, n));
    }

    static boolean admit(ClientSession clientSession, ClientSession clientSession2, ClientSession clientSession3, int n, int n2, int n3) {
        if (n != 29 && n != 152 && n != 153) {
            throw new IllegalStateException("unreviewed negative material skill");
        }
        return NativeLightAction.admitPair(clientSession, clientSession2 != null ? clientSession2 : (clientSession3 != null ? clientSession3 : clientSession), n, n2, n3);
    }

    private NativeNegativeMaterialAction() {
    }
}
