// ported from candidate 50635f7e; B07 三方移植（base-0.6.71 / candidate / src/main）。
// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/cancellation-material/build-repro/inputs/sources/NativeCancellationMaterialAction.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.io.IOException;
/** Preserve existing cancellation/poison scopes; order caster and explicit PC target before material commit. */
final class NativeCancellationMaterialAction {
 static void nativeControlledOriginalBattle(ClientSession clientSession2, byte[] byArray, BattleSpellCatalog.CancellationSpell cancellationSpell, CombatPoisonLifecycle.Scope scope) throws IOException {
        if (NativeCancellationBattleAction.cast(clientSession2, byArray, cancellationSpell, scope)) {
            return;
        }
        Protocol.ObjectSpellRequest objectSpellRequest = Protocol.objectSpellRequest(byArray);
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession2, "hub");
        ClientSession clientSession3 = gameHub.roomCombatants((Long)CombatAccess.field(clientSession2, "currentRoomId")).stream().filter(clientSession -> clientSession.playerObjectId() == objectSpellRequest.targetObjectId()).findFirst().orElse(clientSession2);
        NativeCancellationMaterialAction.prepared(clientSession2, clientSession3, () -> clientSession2.nativeMaterialCancellationBody(byArray, cancellationSpell, scope));
    }
 static void world(ClientSession clientSession, Protocol.ObjectSpellRequest objectSpellRequest, ClientSession clientSession2, BattleSpellCatalog.CancellationSpell cancellationSpell, CombatPoisonLifecycle.Scope scope) throws IOException {
        NativeCancellationMaterialAction.prepared(clientSession, clientSession2, () -> clientSession.nativeMaterialWorldCancellationBody(objectSpellRequest, clientSession2, cancellationSpell, scope));
    }
 static boolean admit(ClientSession pc,ClientSession target,int skill,int action,int reuse){
  if(skill!=44)throw new IllegalStateException("unreviewed cancellation material skill");
  return NativeLightAction.admitPair(pc,target==null?pc:target,skill,action,reuse);
 }
 private NativeCancellationMaterialAction(){}

    static void battle(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.CancellationSpell cancellationSpell, CombatPoisonLifecycle.Scope scope) throws IOException {
        if (NativeBattleControlledCancellation.cast(clientSession, byArray, cancellationSpell, scope)) {
            return;
        }
        NativeCancellationMaterialAction.nativeControlledOriginalBattle(clientSession, byArray, cancellationSpell, scope);
    }

    private static void prepared(ClientSession clientSession, ClientSession clientSession2, NativeSelfCastAction.Task task) throws IOException {
        NativePlayerCommit.Binding binding = NativePlayerCommit.Binding.capture(clientSession);
        NativePlayerCommit.Binding binding2 = NativePlayerCommit.Binding.capture(clientSession2);
        NativeCancellationProbability.prepare(clientSession2);
        NativeLightAction.commitPair(clientSession, clientSession2, () -> {
            if (binding.matches(clientSession) && binding2.matches(clientSession2)) {
                task.run();
            }
        });
    }
}
