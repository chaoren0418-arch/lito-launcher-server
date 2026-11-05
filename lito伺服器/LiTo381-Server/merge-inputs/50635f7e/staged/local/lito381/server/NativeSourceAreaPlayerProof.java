package local.lito381.server;
/** Only the original area roster predicate and pre-body source checks consume this helper. */
final class NativeSourceAreaPlayerProof {
 static boolean player(NativeAreaMaterialAction.Context c,ClientSession pc){
  if(!c.player(pc))return false;
  if(!NativeSourcePlayerFactories.scoped(c))return true;
  var binding=c.bindings.get(pc);var m=NativeSourcePlayerFactories.retained(pc,binding,c);
  return m.binding!=null&&m.binding.equals(binding)&&NativeAcceptedFreezeMutation.roleFrame(m.binding,pc)&&!(boolean)CombatAccess.field(pc,"sourceItemDisconnected")&&NativeSourceControlledDisplay.rawLife(pc)==m.acceptedLife;
 }
 static boolean scene(NativeAreaMaterialAction.Context c){
  if(!c.scene())return false;
  if(!NativeSourcePlayerFactories.scoped(c))return true;
  var m=NativeSourcePlayerFactories.retained(c.pc,c.caster,c);
  return m.current(true)&&NativeSourceControlledDisplay.rawLife(c.pc)==m.life;
 }
 private NativeSourceAreaPlayerProof(){}
}
