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

final class NativeReturnNatureMaterialAction {
    private static final ThreadLocal<Target> ingress = new ThreadLocal();
    private static final ThreadLocal<Target> current = new ThreadLocal();

    private static Target target(ClientSession clientSession, byte[] byArray) {
        Protocol.AttackSpellRequest attackSpellRequest = Protocol.attackSpellRequest(byArray);
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
        boolean bl = String.valueOf(CombatAccess.field(clientSession, "phase")).equals("WORLD");
        List list = bl ? (List)CombatAccess.call(clientSession, "classicWorldMembers", "(I)Ljava/util/List;", (int)((Integer)CombatAccess.field(clientSession, "classicWorldMapId"))) : gameHub.roomCombatants((Long)CombatAccess.field(clientSession, "currentRoomId"));
        for (ClientSession clientSession2 : list) {
            List<BattleCompanion.Snapshot> list2 = bl ? clientSession2.classicWorldCompanionSnapshots() : clientSession2.controlledCompanionSnapshots();
            for (BattleCompanion.Snapshot snapshot : list2) {
                BattleCompanion battleCompanion;
                if (snapshot.objectId() != attackSpellRequest.targetObjectId() || (battleCompanion = (BattleCompanion)CombatAccess.call(clientSession2, "ownedCompanion", "(J)Llocal/lito381/server/BattleCompanion;", attackSpellRequest.targetObjectId())) == null) continue;
                return new Target(clientSession, clientSession2, battleCompanion, NativeCompanionCallbacks.capture(NativePlayerCommit.Binding.capture(clientSession2), battleCompanion));
            }
        }
        return new Target(clientSession, clientSession, null, null);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void dispatch(ClientSession clientSession, byte[] byArray, NativeSelfCastAction.Task task) throws IOException {
        String string = String.valueOf(CombatAccess.field(clientSession, "phase"));
        if (Protocol.spellSelector(byArray).skillId() != 145 || !string.equals("BATTLE") && !string.equals("WORLD") || clientSession.selectedCharacterName().isBlank()) {
            task.run();
            return;
        }
        Target target = ingress.get();
        ingress.set(NativeReturnNatureMaterialAction.target(clientSession, byArray));
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
    static void cast(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.ReturnToNatureSpell returnToNatureSpell, int n) throws IOException {
        Target target = ingress.get();
        Target target2 = target != null && target.caster == clientSession ? target : NativeReturnNatureMaterialAction.target(clientSession, byArray);
        ingress.remove();
        try {
            NativeLightAction.commitPair(clientSession, target2.owner, () -> {
                if (!target2.valid()) {
                    return;
                }
                Target target2 = current.get();
                current.set(target2);
                try {
                    clientSession.nativeMaterialReturnNatureBody(byArray, returnToNatureSpell, n);
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

    static boolean admit(ClientSession clientSession, ClientSession clientSession2, int n, int n2, int n3) {
        if (n != 145) {
            throw new IllegalStateException("unreviewed Return to Nature skill");
        }
        Target target = current.get();
        if (target == null || target.caster != clientSession || target.owner != clientSession2 || !target.valid()) {
            return false;
        }
        return NativeLightAction.admitPair(clientSession, clientSession2, n, n2, n3);
    }

    static boolean captureOutput(ClientSession clientSession, byte[] byArray, String string) {
        Target target = current.get();
        if (target == null) {
            current.remove();
            return false;
        }
        if (!string.contains("Return-to-Nature") || !string.startsWith("S_EFFECT ") && !string.startsWith("S_REMOVE_OBJECT ") && !string.startsWith("S_PUT_OBJECT ")) {
            return false;
        }
        byte[] byArray2 = (byte[])byArray.clone();
        NativeLightAction.deferScoped(() -> {
            if (target.canPublish()) {
                clientSession.nativeSelfOriginalSend(byArray2, string);
            }
        });
        return true;
    }

    static ClientSession caster(ClientSession clientSession, BattleCompanion battleCompanion) {
        Target target = current.get();
        if (target == null) {
            current.remove();
            return null;
        }
        return target.owner == clientSession && target.pet == battleCompanion && target.valid() && clientSession.nativePlayerActionLock().isHeldByCurrentThread() && target.caster.nativePlayerActionLock().isHeldByCurrentThread() ? target.caster : null;
    }

    private NativeReturnNatureMaterialAction() {
    }

    private record Target(ClientSession caster, ClientSession owner, BattleCompanion pet, NativeCompanionCallbacks.Stamp stamp) {
        BattleCompanion owned() {
            return this.pet == null ? null : (BattleCompanion)CombatAccess.call(this.owner, "ownedCompanion", "(J)Llocal/lito381/server/BattleCompanion;", this.stamp.companion);
        }

        boolean valid() {
            return this.pet == null || this.stamp.master.matches(this.owner) && (Boolean)CombatAccess.field(this.owner, "sourceItemDisconnected") == false && this.owned() == this.pet && NativeCompanionCallbacks.capture((NativePlayerCommit.Binding)this.stamp.master, (BattleCompanion)this.pet).life == this.stamp.life;
        }

        boolean canPublish() {
            if (this.pet == null) {
                return true;
            }
            BattleCompanion battleCompanion = this.owned();
            return this.stamp.master.matches(this.owner) && (Boolean)CombatAccess.field(this.owner, "sourceItemDisconnected") == false && (battleCompanion == null || battleCompanion == this.pet);
        }
    }
}
