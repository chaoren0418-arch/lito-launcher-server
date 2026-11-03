package local.lito381.server;
import java.io.*;
/** 反射沿用既有雙方收據／成對動作邊界；原 HP 核心不另行實作。 */
final class NativeCompanionReflectionScene {
 static NativePlayerCommit.Binding capture(ClientSession pc){
  if(!NativeCompanionRefScene.worldRef(NativeCombatAttacker.nativeReflectionRef())||!pc.nativePlayerActionLock().isHeldByCurrentThread())return NativePlayerCommit.Binding.capture(pc);
  // Source 動作鎖已固定身分；SUMMON 入口不在 Hub 內重新取得玩家狀態 monitor。
  return new NativePlayerCommit.Binding(NativeCompanionRoleIdentity.currentOwner(pc),pc.playerObjectId(),pc.sourceCharacterIdentity(),pc.sourceAccountIdentity(),pc.selectedCharacterName(),String.valueOf(CombatAccess.field(pc,"phase")),NativePlayerCommit.number(pc,"currentRoomId"),NativeCompanionPlacement.map(pc),NativeCompanionPlacement.instance(pc));
 }
 static void damage(ClientSession pc,NativeCombatAttacker.Ref ref,int amount,String note){
  if(!NativeCompanionRefScene.worldRef(ref)){NativeCombatAttacker.nativeReflectionOriginalDamage(pc,ref,amount,note);return;}
  if(amount<=0||!NativeCompanionPlacement.world(pc))return;var frame=NativePlayerCommit.frame();var binding=frame==null?NativePlayerCommit.Binding.capture(pc):frame.event.target();if(frame!=null&&(frame.pc!=pc||!NativePlayerCommit.validFrame(pc)))return;var owner=NativePlayerCommit.nativeRefCompanionCaster(pc,ref.master());if(owner==null)return;var hub=NativeCompanionPlacement.hub(pc);
  try{NativeLightAction.commitPair(pc,owner,()->{
   if(!pc.nativePlayerActionLock().isHeldByCurrentThread()||!owner.nativePlayerActionLock().isHeldByCurrentThread())throw new IllegalStateException("reflection requires the actual source player pair");
   if(frame!=null&&(NativePlayerCommit.frame()!=frame||!NativePlayerCommit.validFrame(pc))||!NativeCompanionProcScene.strict(binding,pc)||!binding.phase().equals("WORLD")||binding.map()!=ref.map()||binding.instance()!=ref.instance()||!NativeCompanionRoleIdentity.actorBinding(ref.master(),owner))return;
   synchronized(hub){if(!hub.classicWorldMembers(ref.map(),ref.instance()).contains(pc)||!hub.connectedSessionsSnapshot().contains(owner)||hub.classicWorldLocation(owner)==null)return;}
   NativeCombatAttacker.nativeReflectionOriginalDamage(pc,ref,amount,note);
  });}catch(IOException failure){throw new UncheckedIOException(failure);}
 }
 static BattleCompanion.Damage companionDamage(BattleCompanion pet,int amount){
  var ref=NativeCombatAttacker.nativeReflectionRef();var frame=NativePlayerCommit.frame();if(!NativeCompanionRefScene.worldRef(ref)||frame==null)return NativeCombatAttacker.nativeReflectionOriginalCompanionDamage(pet,amount);
  var pc=frame.pc;var owner=NativePlayerCommit.nativeRefCompanionCaster(pc,ref.master());if(owner==null)return zero(pet);var hub=NativeCompanionPlacement.hub(pc);var binding=frame.event.target();
  // 此入口亦由 SUMMON 的既有 Hub／物件臨界區呼叫；不反向取得容器鎖。
  if(!pc.nativePlayerActionLock().isHeldByCurrentThread()||!owner.nativePlayerActionLock().isHeldByCurrentThread())throw new IllegalStateException("numeric reflection lost its original source pair");
  synchronized(hub){synchronized(pet){var timed=(BattleTimedStatus)CombatAccess.field(pet,"timedStatus");synchronized(timed){var life=frame.queue.life;synchronized(life.admission){
   if(NativePlayerCommit.frame()!=frame||!life.processes()||!NativePlayerCommit.validFrame(pc)||!NativeCompanionProcScene.strict(binding,pc)||binding.map()!=ref.map()||binding.instance()!=ref.instance()||!hub.classicWorldMembers(ref.map(),ref.instance()).contains(pc)||!hub.connectedSessionsSnapshot().contains(owner)||hub.classicWorldLocation(owner)==null||!NativeCompanionRefScene.liveSame(ref,pet,timed,owner))return zero(pet);
   return NativeCombatAttacker.nativeReflectionOriginalCompanionDamage(pet,amount);
  }}}}
 }
 static BattleCompanion.Damage zero(BattleCompanion pet){var snapshot=pet.snapshot();return new BattleCompanion.Damage(snapshot,snapshot,0,false);}
 private NativeCompanionReflectionScene(){}
}
