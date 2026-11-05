package local.lito381.server;
/** An installed target-owned damage-poison tick does not reacquire attacker liveness as HP authority. */
final class NativeSourcePoisonPc {
 static ClientSession peer(ClientSession target,ClientSession supplied,CombatPoisonLifecycle.Scope scope){
  if(!NativeSourceDollCommit.enabled(target)||scope==null)return supplied;
  var context=CombatAccess.call(scope,"find","(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/CombatPoisonLifecycle$Context;",target);
  if(context==null||!(boolean)CombatAccess.field(context,"clockActive"))return supplied;
  var snapshot=CombatAccess.field(context,"frozen");
  return snapshot!=null&&CombatAccess.field(snapshot,"kind")==ClassicWorldMonsterPoisonAttack.Kind.DAMAGE?null:supplied;
 }
 private NativeSourcePoisonPc(){}
}
