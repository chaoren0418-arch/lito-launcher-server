package local.lito381.server;
/** Source setSkillEffect replacement decision on the already locked, accepted custom68 producer. */
final class NativeSourceProcTimedRefresh {
 static boolean replace(boolean present,int remainingSeconds,int timeMillis){
  if(!present)return true;
  long remainingMillis=(long)remainingSeconds*1000L;
  return remainingSeconds>=0&&(remainingMillis<timeMillis||timeMillis==0);
 }
 static boolean live(NativeSourceEquipmentProcLife.Proof proof,BattleAbilityStatus status){
  var frame=NativePlayerCommit.frame();return frame!=null&&frame.pc==proof.caster().pc()&&frame.accepted&&!frame.event.world()&&frame.nativeSourceAcceptedPlayers==proof.accepted()&&CombatRules.loaded()&&CombatAccess.field(proof.hub(),"sourcePlayerCommitLifetime")==proof.lifetime()&&proof.lifetime().mode!=NativePlayerLifetime.Mode.STOPPED&&!(boolean)CombatAccess.field(proof.hub(),"nativeStaticMotionClosed")&&CombatAccess.field(proof.target().pc(),"abilityStatus")==status&&CombatAccess.field(proof.caster().pc(),"timedStatus")!=null&&CombatAccess.field(proof.target().pc(),"timedStatus")!=null&&!(boolean)CombatAccess.field(proof.caster().pc(),"observer")&&!(boolean)CombatAccess.field(proof.target().pc(),"observer")&&NativeSourceEquipmentProcLife.held(proof)&&NativeSourceEquipmentProcLife.participant(proof.caster(),proof.hub())&&NativeSourceEquipmentProcLife.participant(proof.target(),proof.hub());
 }
 static java.util.function.BooleanSupplier guarded(NativeSourceEquipmentProcLife.Proof proof,java.util.function.BooleanSupplier original,BattleAbilityStatus status,NativeArmorProc.Rule rule){
  if(rule==null||rule.skill()!=68||rule.seconds()<=0)return NativeSourceEquipmentProcLife.guarded(proof,original);
  return ()->{if(!NativeSourceEquipmentProcLife.held(proof)||!proof.current())return false;synchronized(status){return live(proof,status)&&original.getAsBoolean();}};
 }
 static void activate68(BattleAbilityStatus status,long now,int seconds,int divisor,NativeArmorProc.Rule rule){
  var frame=NativePlayerCommit.frame();
  if(rule==null||rule.skill()!=68||rule.seconds()<=0||frame==null||!frame.accepted||frame.event.world()||!NativeSourceEquipmentProcLife.enabled(frame.pc)||now<0||seconds<=0||seconds>65535||divisor<=1||divisor>127){status.activateImmuneToHarm(now,seconds,divisor);return;}
  synchronized(status){
   var previous=status.immuneToHarm(System.nanoTime());
   if(replace(previous.remainingSeconds()>0,previous.remainingSeconds(),seconds*1000))status.activateImmuneToHarm(now,seconds,divisor);
  }
 }
 private NativeSourceProcTimedRefresh(){}
}
