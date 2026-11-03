// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import local.lito381.server.BattleSpellCatalog;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatNpcDebuffs;
import local.lito381.server.GameHub;
import local.lito381.server.NativeCastMaterial;
import local.lito381.server.NativeLightAction;
import local.lito381.server.Protocol;
import local.lito381.server.RuntimeInventory;

final class NativeArmorMaterialAction {
    static void cast(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.ArmorBreakSpell armorBreakSpell) throws IOException {
        Protocol.selectorOnlySpellRequest(byArray);
        Object object = CombatAccess.field(clientSession, "phase");
        String string = String.valueOf(object);
        ClientSession clientSession2 = null;
        if (string.equals("BATTLE") || string.equals("WORLD")) {
            GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
            long l = ((Number)CombatAccess.field(clientSession, "currentRoomId")).longValue();
            int n = string.equals("WORLD") ? ((Integer)CombatAccess.field(clientSession, "classicWorldMapId")).intValue() : gameHub.battleMapId(l, clientSession);
            Object object2 = CombatAccess.call(clientSession, "resolveArmorBreakTarget", "(Llocal/lito381/server/ClientSession$Phase;JIIII)Llocal/lito381/server/ClientSession$ArmorBreakTarget;", object, l, n, (Integer)CombatAccess.field(clientSession, "classicWorldInstanceId"), (Integer)CombatAccess.field(clientSession, "positionX"), (Integer)CombatAccess.field(clientSession, "positionY"));
            if ((object2 = CombatNpcDebuffs.armorTarget(object2, clientSession)) != null) {
                clientSession2 = (ClientSession)CombatAccess.get(object2, "player");
            }
        }
        NativeLightAction.commitPair(clientSession, clientSession2 == null ? clientSession : clientSession2, () -> clientSession.nativeMaterialArmorBreakBody(byArray, armorBreakSpell));
    }

    static boolean admit(ClientSession clientSession, Object object, BattleSpellCatalog.ArmorBreakSpell armorBreakSpell) {
        if (armorBreakSpell.skillId() != 112) {
            throw new IllegalStateException("unreviewed Armor Break material skill");
        }
        ClientSession clientSession2 = (ClientSession)CombatAccess.get(object, "player");
        return NativeLightAction.admitPair(clientSession, clientSession2 == null ? clientSession : clientSession2, armorBreakSpell.skillId(), armorBreakSpell.actionId(), armorBreakSpell.reuseDelayMillis());
    }

    static RuntimeInventory previous(ClientSession clientSession) {
        return NativeLightAction.currentMaterial(clientSession) == null ? (RuntimeInventory)CombatAccess.field(clientSession, "inventory") : NativeCastMaterial.previous(clientSession);
    }

    private NativeArmorMaterialAction() {
    }
}
