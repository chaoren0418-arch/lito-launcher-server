package local.lito381.server;
import java.util.function.BooleanSupplier;
/** Custom68 borrows its original mutation Proof and ability through the original first-status queue span. */
final class NativeSourceProc68FirstQueue {
 static boolean current(NativePlayerCommit.Binding binding,ClientSession pc,BattleTimedStatus timed,NativeSourceEquipmentProcLife.Proof proof,NativeProcSkillCatalog.Catalog catalog,BattleAbilityStatus ability){return NativeSourceProcScalarSkills.queueCurrent(binding,pc,timed,proof,catalog)&&CombatAccess.field(pc,"abilityStatus")==ability;}
 static boolean mutate(ClientSession pc,NativePlayerCommit.Binding binding,BooleanSupplier original,NativeSourceEquipmentProcLife.Proof proof,BattleAbilityStatus ability,NativeArmorProc.Rule rule){
  if(rule==null||rule.skill()!=68||rule.seconds()<=0)return NativePlayerCommit.mutateOwned(pc,binding,original);
  var frame=NativePlayerCommit.frame();if(proof==null||frame==null||frame.queue==null||CombatAccess.field(frame.queue,"life")!=proof.lifetime()||pc!=proof.target().pc()||!proof.target().binding().equals(binding)||!NativeSourceEquipmentProcLife.held(proof)||!proof.current()||CombatAccess.field(pc,"abilityStatus")!=ability)return false;
  Object state=CombatAccess.field(pc,"timedStatus");if(!(state instanceof BattleTimedStatus timed))return false;
  NativeProcSkillCatalog.Catalog catalog;try{catalog=NativeProcSkillCatalog.catalog();}catch(IllegalStateException missing){return false;}
  return NativePlayerCommit.nativeSourceProc68MutateOwned(pc,binding,original,timed,proof,catalog,ability);
 }
 private NativeSourceProc68FirstQueue(){}
}
