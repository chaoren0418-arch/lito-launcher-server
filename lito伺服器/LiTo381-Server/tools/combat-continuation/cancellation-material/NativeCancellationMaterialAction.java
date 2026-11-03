package local.lito381.server;
import java.io.IOException;
/** Preserve existing cancellation/poison scopes; order caster and explicit PC target before material commit. */
final class NativeCancellationMaterialAction {
 static void battle(ClientSession pc,byte[]packet,BattleSpellCatalog.CancellationSpell spell,CombatPoisonLifecycle.Scope scope)throws IOException{
  var request=Protocol.objectSpellRequest(packet);var hub=(GameHub)CombatAccess.field(pc,"hub");
  var target=hub.roomCombatants((long)CombatAccess.field(pc,"currentRoomId")).stream().filter(p->p.playerObjectId()==request.targetObjectId()).findFirst().orElse(pc);
  NativeLightAction.commitPair(pc,target,()->pc.nativeMaterialCancellationBody(packet,spell,scope));
 }
 static void world(ClientSession pc,Protocol.ObjectSpellRequest request,ClientSession target,BattleSpellCatalog.CancellationSpell spell,CombatPoisonLifecycle.Scope scope)throws IOException{
  NativeLightAction.commitPair(pc,target,()->pc.nativeMaterialWorldCancellationBody(request,target,spell,scope));
 }
 static boolean admit(ClientSession pc,ClientSession target,int skill,int action,int reuse){
  if(skill!=44)throw new IllegalStateException("unreviewed cancellation material skill");
  return NativeLightAction.admitPair(pc,target==null?pc:target,skill,action,reuse);
 }
 private NativeCancellationMaterialAction(){}
}
