package local.lito381.server;
/** WORLD non-player side effects share the original source player/custody/actor boundaries. */
final class NativeCompanionCombatScene {
 static int debit(ClientSession pc,NativeCombatAttacker.Ref ref,int requested){
  if(!NativeCompanionRefScene.worldRef(ref))return NativeCombatAttacker.nativeCombatSceneOriginalDebit(pc,ref,requested);
  if(requested<=0||!NativeCompanionPlacement.world(pc))return 0;var frame=NativePlayerCommit.frame();var binding=frame==null?NativePlayerCommit.Binding.capture(pc):NativePlayerCommit.frameBinding(pc);if(frame!=null&&(!NativePlayerCommit.validFrame(pc)||binding==null))return 0;var owner=NativePlayerCommit.nativeRefCompanionCaster(pc,ref.master());if(owner==null)return 0;var hub=NativeCompanionPlacement.hub(pc);int[] result={0};
  NativePairedCastSafety.run(pc,owner,()->{synchronized(hub){
   if(!pc.nativePlayerActionLock().isHeldByCurrentThread()||!owner.nativePlayerActionLock().isHeldByCurrentThread())throw new IllegalStateException("NPC debit requires paired source locks");
   if(frame!=null&&(NativePlayerCommit.frame()!=frame||!NativePlayerCommit.validFrame(pc))||!NativeCompanionProcScene.strict(binding,pc)||!binding.phase().equals("WORLD")||binding.map()!=ref.map()||binding.instance()!=ref.instance()||!hub.classicWorldMembers(ref.map(),ref.instance()).contains(pc)||!hub.connectedSessionsSnapshot().contains(owner)||hub.classicWorldLocation(owner)==null||!NativeCompanionRoleIdentity.actorBinding(ref.master(),owner))return;
   var custody=NativeBattleTamedLifecycle.pets(owner);synchronized(custody){var actor=(BattleCompanion)CombatAccess.call(owner,"ownedCompanion","(J)Llocal/lito381/server/BattleCompanion;",ref.id());if(actor==null)return;synchronized(actor){var timed=(BattleTimedStatus)CombatAccess.field(actor,"timedStatus");synchronized(timed){if(!NativeCompanionRefScene.liveSame(ref,actor,timed,owner))return;result[0]=NativeCombatAttacker.nativeCombatSceneOriginalDebit(pc,ref,requested);}}}
  }});return result[0];
 }
 private NativeCompanionCombatScene(){}
}
