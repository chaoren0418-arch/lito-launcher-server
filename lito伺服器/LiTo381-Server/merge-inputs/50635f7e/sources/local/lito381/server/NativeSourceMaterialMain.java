package local.lito381.server;
/** BATTLE Main/lifetime safety is independent of the current Source-controls loaded switch. */
final class NativeSourceMaterialMain {
 static boolean ready(ClientSession pc){
  if(pc==null||!"BATTLE".equals(String.valueOf(CombatAccess.field(pc,"phase"))))return true;
  Object hub=CombatAccess.field(pc,"hub");Object value=CombatAccess.field(hub,"sourcePlayerCommitLifetime");
  return value instanceof NativePlayerLifetime life&&life.mode!=NativePlayerLifetime.Mode.STOPPED&&!(boolean)CombatAccess.field(hub,"nativeStaticMotionClosed");
 }
 static boolean locked(Object ingress,ClientSession pc){
  var binding=(NativePlayerCommit.Binding)CombatAccess.field(ingress,"binding");if(binding==null||!"BATTLE".equals(binding.phase()))return true;
  var hub=(GameHub)CombatAccess.field(ingress,"hub");var r=(NativePlayerVitalCompletions.Receipt)CombatAccess.field(ingress,"receipt");
  return CombatAccess.field(ingress,"pc")==pc&&CombatAccess.field(pc,"hub")==hub&&r!=null&&r.life!=null&&CombatAccess.field(hub,"sourcePlayerCommitLifetime")==r.life&&r.life.mode!=NativePlayerLifetime.Mode.STOPPED&&!(boolean)CombatAccess.field(hub,"nativeStaticMotionClosed");
 }
 private NativeSourceMaterialMain(){}
}
