// ported from candidate 50635f7e; B07 三方移植（base-0.6.71 / candidate / src/main）。
// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/resurrection-material/NativeResurrectionMaterialAction.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.io.IOException;
import java.util.List;
/** Preserve both existing roots and their target checks under ordered player locks. */
final class NativeResurrectionMaterialAction {
 private static Target target(ClientSession clientSession, byte[] byArray, boolean bl) {
        Protocol.ObjectSpellRequest objectSpellRequest = Protocol.objectSpellRequest(byArray);
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
        List<ClientSession> list = bl ? (List<ClientSession>)CombatAccess.call(clientSession, "classicWorldMembers", "(I)Ljava/util/List;", (int)((Integer)CombatAccess.field(clientSession, "classicWorldMapId"))) : gameHub.roomCombatants((Long)CombatAccess.field(clientSession, "currentRoomId"));
        for (ClientSession clientSession2 : list) {
            if (clientSession2.playerObjectId() == objectSpellRequest.targetObjectId()) {
                return new Target(clientSession, clientSession2, null, null);
            }
        }
        if (bl) {
            for (ClientSession clientSession2 : NativeCompanionNegative.members(list, clientSession)) {
                for (BattleCompanion.Snapshot snapshot : NativeCompanionResurrection.snapshots(clientSession2, clientSession)) {
                    if(snapshot.objectId() == objectSpellRequest.targetObjectId()) {
                        var pet=(BattleCompanion)CombatAccess.call(clientSession2,"ownedCompanion","(J)Llocal/lito381/server/BattleCompanion;",objectSpellRequest.targetObjectId());
                        if(pet!=null)return new Target(clientSession,clientSession2,pet,NativeCompanionCallbacks.capture(NativePlayerCommit.Binding.capture(clientSession2),pet));
                    }
                }
            }
        }
        return new Target(clientSession, clientSession, null, null);
    }
 static void battle(ClientSession pc,byte[] packet,BattleSpellCatalog.ResurrectionSpell spell)throws IOException{
  run(pc,packet,false,()->pc.nativeMaterialResurrectionBody(packet,spell));
 }
 static void nativeSceneOriginalWorld(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.ResurrectionSpell resurrectionSpell) throws IOException {
        NativeResurrectionMaterialAction.run(clientSession, byArray, true, () -> clientSession.nativeMaterialWorldResurrectionBody(byArray, resurrectionSpell));
    }
 static boolean nativeSceneOriginalAdmit(ClientSession clientSession, ClientSession clientSession2, int n, int n2, int n3) {
        if (n != 61 && n != 75) {
            throw new IllegalStateException("unreviewed Resurrection material skill");
        }
        Target target = current.get();
        if (target == null || target.caster != clientSession || !target.valid() || clientSession2 != null && target.owner != clientSession2) {
            return false;
        }
        return NativeLightAction.admitPair(clientSession, target.owner, n, n2, n3);
    }
 private NativeResurrectionMaterialAction(){}

    private static final ThreadLocal<Target> current = new ThreadLocal<>(), ingress = new ThreadLocal<>();

    static void nativeSceneOriginalDispatch(ClientSession clientSession, byte[] byArray, NativeSelfCastAction.Task task) throws IOException {
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

    private static void run(ClientSession clientSession, byte[] byArray, boolean bl, NativeSelfCastAction.Task task) throws IOException {
        Target target = ingress.get();
        Target target2 = target != null && target.caster == clientSession ? target : NativeResurrectionMaterialAction.target(clientSession, byArray, bl);
        ingress.remove();
        try {
            NativeLightAction.commitPair(clientSession, target2.owner, () -> {
                if (!target2.valid()) {
                    return;
                }
                Target previous = current.get();
                current.set(target2);
                try {
                    task.run();
                }
                finally {
                    if (previous == null) {
                        current.remove();
                    } else {
                        current.set(previous);
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

    static void dispatch(ClientSession clientSession, byte[] byArray, NativeSelfCastAction.Task task) throws IOException {
        NativeCompanionResurrection.dispatch(clientSession, byArray, task);
    }

    static void world(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.ResurrectionSpell resurrectionSpell) throws IOException {
        NativeCompanionResurrection.world(clientSession, byArray, resurrectionSpell);
    }

    static boolean admit(ClientSession clientSession, ClientSession clientSession2, int n, int n2, int n3) {
        return NativeCompanionResurrection.admit(clientSession, clientSession2, n, n2, n3);
    }
}
