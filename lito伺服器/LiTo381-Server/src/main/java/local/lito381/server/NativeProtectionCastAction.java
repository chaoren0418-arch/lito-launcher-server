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

final class NativeProtectionCastAction {
    static void cast(ClientSession clientSession2, byte[] byArray, BattleSpellCatalog.DamageProtectionSpell damageProtectionSpell) throws IOException {
        Protocol.ObjectSpellRequest objectSpellRequest = Protocol.objectSpellRequest(byArray);
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession2, "hub");
        String string = String.valueOf(CombatAccess.field(clientSession2, "phase"));
        List list = string.equals("WORLD") ? (List)CombatAccess.call(clientSession2, "classicWorldMembers", "(I)Ljava/util/List;", (int)((Integer)CombatAccess.field(clientSession2, "classicWorldMapId"))) : (string.equals("BATTLE") ? gameHub.roomCombatants(((Number)CombatAccess.field(clientSession2, "currentRoomId")).longValue()) : List.of());
        ClientSession clientSession3 = list.stream().filter(clientSession -> clientSession.playerObjectId() == objectSpellRequest.targetObjectId()).findFirst().orElse(null);
        NativeLightAction.commitPair(clientSession2, clientSession3 == null ? clientSession2 : clientSession3, () -> clientSession2.nativeMaterialProtectionBody(byArray, damageProtectionSpell));
    }

    static boolean admit(ClientSession clientSession, ClientSession clientSession2, BattleSpellCatalog.DamageProtectionSpell damageProtectionSpell) {
        if (damageProtectionSpell.skillId() != 68) {
            throw new IllegalStateException("unreviewed target material skill");
        }
        return NativeLightAction.admitPair(clientSession, clientSession2, damageProtectionSpell.skillId(), damageProtectionSpell.actionId(), damageProtectionSpell.reuseDelayMillis());
    }

    private NativeProtectionCastAction() {
    }
}
