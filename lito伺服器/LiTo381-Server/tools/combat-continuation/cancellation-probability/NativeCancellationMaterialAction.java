package local.lito381.server;
import java.io.IOException;
/** Prepare the target role policy before the ordered pair; preserve the installed material ownership. */
final class NativeCancellationMaterialAction {
 static void battle(ClientSession pc,byte[]packet,BattleSpellCatalog.CancellationSpell spell,CombatPoisonLifecycle.Scope scope)throws IOException{
  var request=Protocol.objectSpellRequest(packet);var hub=(GameHub)CombatAccess.field(pc,"hub");
  var target=hub.roomCombatants((long)CombatAccess.field(pc,"currentRoomId")).stream().filter(p->p.playerObjectId()==request.targetObjectId()).findFirst().orElse(pc);
  prepared(pc,target,()->pc.nativeMaterialCancellationBody(packet,spell,scope));
 }
 static void world(ClientSession pc,Protocol.ObjectSpellRequest request,ClientSession target,BattleSpellCatalog.CancellationSpell spell,CombatPoisonLifecycle.Scope scope)throws IOException{
  prepared(pc,target,()->pc.nativeMaterialWorldCancellationBody(request,target,spell,scope));
 }
 private static void prepared(ClientSession pc,ClientSession target,NativeSelfCastAction.Task body)throws IOException{
  var casterBinding=NativePlayerCommit.Binding.capture(pc);var targetBinding=NativePlayerCommit.Binding.capture(target);
  NativeCancellationProbability.prepare(target);
  NativeLightAction.commitPair(pc,target,()->{if(casterBinding.matches(pc)&&targetBinding.matches(target))body.run();});
 }
 static boolean admit(ClientSession pc,ClientSession target,int skill,int action,int reuse){
  if(skill!=44)throw new IllegalStateException("unreviewed cancellation material skill");
  return NativeLightAction.admitPair(pc,target==null?pc:target,skill,action,reuse);
 }
 private NativeCancellationMaterialAction(){}
}
