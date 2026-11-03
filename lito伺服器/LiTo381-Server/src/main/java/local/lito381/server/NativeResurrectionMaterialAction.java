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
import local.lito381.server.NativeCompanionCallbacks;
import local.lito381.server.NativeLightAction;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativeSelfCastAction;
import local.lito381.server.Protocol;

final class NativeResurrectionMaterialAction {
    private static final ThreadLocal<Target> current = new ThreadLocal();
    private static final ThreadLocal<Target> ingress = new ThreadLocal();

    private static Target target(ClientSession clientSession, byte[] byArray, boolean bl) {
        Protocol.ObjectSpellRequest objectSpellRequest = Protocol.objectSpellRequest(byArray);
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
        List list = bl ? (List)CombatAccess.call(clientSession, "classicWorldMembers", "(I)Ljava/util/List;", (int)((Integer)CombatAccess.field(clientSession, "classicWorldMapId"))) : gameHub.roomCombatants((Long)CombatAccess.field(clientSession, "currentRoomId"));
        for (ClientSession clientSession2 : list) {
            if (clientSession2.playerObjectId() != objectSpellRequest.targetObjectId()) continue;
            return new Target(clientSession, clientSession2, null, null);
        }
        if (bl) {
            for (ClientSession clientSession2 : list) {
                for (BattleCompanion.Snapshot snapshot : clientSession2.classicWorldCompanionSnapshots()) {
                    BattleCompanion battleCompanion;
                    if (snapshot.objectId() != objectSpellRequest.targetObjectId() || (battleCompanion = (BattleCompanion)CombatAccess.call(clientSession2, "ownedCompanion", "(J)Llocal/lito381/server/BattleCompanion;", objectSpellRequest.targetObjectId())) == null) continue;
                    return new Target(clientSession, clientSession2, battleCompanion, NativeCompanionCallbacks.capture(NativePlayerCommit.Binding.capture(clientSession2), battleCompanion));
                }
            }
        }
        return new Target(clientSession, clientSession, null, null);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void dispatch(ClientSession clientSession, byte[] byArray, NativeSelfCastAction.Task task) throws IOException {
        int n = Protocol.spellSelector(byArray).skillId();
        String string = String.valueOf(CombatAccess.field(clientSession, "phase"));
        if (n != 61 && n != 75 || !string.equals("WORLD") && !string.equals("BATTLE") || clientSession.selectedCharacterName().isBlank()) {
            task.run();
            return;
        }
        Target target = ingress.get();
        ingress.set(NativeResurrectionMaterialAction.target(clientSession, byArray, string.equals("WORLD")));
        try {
            task.run();
        }
        finally {
            if (target == null) {
                ingress.remove();
            } else {
                ingress.set(target);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void run(ClientSession clientSession, byte[] byArray, boolean bl, NativeSelfCastAction.Task task) throws IOException {
        Target target = ingress.get();
        Target target2 = target != null && target.caster == clientSession ? target : NativeResurrectionMaterialAction.target(clientSession, byArray, bl);
        ingress.remove();
        try {
            NativeLightAction.commitPair(clientSession, target2.owner, () -> {
                if (!target2.valid()) {
                    return;
                }
                Target target2 = current.get();
                current.set(target2);
                try {
                    task.run();
                }
                finally {
                    if (target2 == null) {
                        current.remove();
                    } else {
                        current.set(target2);
                    }
                }
            });
        }
        finally {
            if (target == null) {
                ingress.remove();
            } else {
                ingress.set(target);
            }
        }
    }

    static void battle(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.ResurrectionSpell resurrectionSpell) throws IOException {
        NativeResurrectionMaterialAction.run(clientSession, byArray, false, () -> clientSession.nativeMaterialResurrectionBody(byArray, resurrectionSpell));
    }

    static void world(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.ResurrectionSpell resurrectionSpell) throws IOException {
        NativeResurrectionMaterialAction.run(clientSession, byArray, true, () -> clientSession.nativeMaterialWorldResurrectionBody(byArray, resurrectionSpell));
    }

    static boolean admit(ClientSession clientSession, ClientSession clientSession2, int n, int n2, int n3) {
        if (n != 61 && n != 75) {
            throw new IllegalStateException("unreviewed Resurrection material skill");
        }
        Target target = current.get();
        if (target == null || target.caster != clientSession || !target.valid() || clientSession2 != null && target.owner != clientSession2) {
            return false;
        }
        return NativeLightAction.admitPair(clientSession, target.owner, n, n2, n3);
    }

    private NativeResurrectionMaterialAction() {
    }

    private record Target(ClientSession caster, ClientSession owner, BattleCompanion pet, NativeCompanionCallbacks.Stamp stamp) {
        boolean valid() {
            if (this.pet == null) {
                return true;
            }
            if (!this.stamp.master.matches(this.owner) || ((Boolean)CombatAccess.field(this.owner, "sourceItemDisconnected")).booleanValue()) {
                return false;
            }
            if (CombatAccess.call(this.owner, "ownedCompanion", "(J)Llocal/lito381/server/BattleCompanion;", this.stamp.companion) != this.pet) {
                return false;
            }
            return NativeCompanionCallbacks.capture((NativePlayerCommit.Binding)this.stamp.master, (BattleCompanion)this.pet).life == this.stamp.life;
        }
    }
}
