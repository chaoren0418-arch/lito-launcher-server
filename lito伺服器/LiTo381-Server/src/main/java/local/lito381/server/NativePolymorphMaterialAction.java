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

final class NativePolymorphMaterialAction {
    static void cast(ClientSession clientSession2, byte[] byArray, BattleSpellCatalog.PolymorphSpell polymorphSpell) throws IOException {
        Protocol.ObjectSpellRequest objectSpellRequest = Protocol.objectSpellRequest(byArray);
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession2, "hub");
        boolean bl = String.valueOf(CombatAccess.field(clientSession2, "phase")).equals("WORLD");
        List list = bl ? (List)CombatAccess.call(clientSession2, "classicWorldMembers", "(I)Ljava/util/List;", (int)((Integer)CombatAccess.field(clientSession2, "classicWorldMapId"))) : gameHub.roomCombatants((Long)CombatAccess.field(clientSession2, "currentRoomId"));
        ClientSession clientSession3 = list.stream().filter(clientSession -> clientSession.playerObjectId() == objectSpellRequest.targetObjectId()).findFirst().orElse(clientSession2);
        NativeLightAction.commitPair(clientSession2, clientSession3, () -> clientSession2.nativeMaterialPolymorphBody(byArray, polymorphSpell));
    }

    static boolean admit(ClientSession clientSession, ClientSession clientSession2, int n, int n2, int n3) {
        if (n != 67) {
            throw new IllegalStateException("unreviewed polymorph skill");
        }
        return NativeLightAction.admitPair(clientSession, clientSession2 == null ? clientSession : clientSession2, n, n2, n3);
    }

    private NativePolymorphMaterialAction() {
    }
}
