package local.lito381.server;
/** One accepted WORLD companion drain and its original player recovery share one lifetime fence. */
final class NativeCompanionManaTransfer {
 static NativePlayerCommit.Queue queue(ClientSession pc,NativePlayerCommit.Binding binding){var f=NativePlayerCommit.frame();if(f==null||f.pc!=pc||!f.event.target().equals(binding)||!f.queue.vitalLock.isHeldByCurrentThread()||!Thread.holdsLock(f.queue.life.admission)||!pc.nativePlayerActionLock().isHeldByCurrentThread())throw new IllegalStateException("MP transfer recovery requires the owned Frame/queue/admission/source boundary");return f.queue;}
 static int drain(ClientSession pc,NativeCombatAttacker.Ref ref,int requested){
  if(!NativeCompanionRefScene.worldRef(ref))return NativeCombatAttacker.debit(pc,ref,requested);
  var f=NativePlayerCommit.frame();if(f==null||f.pc!=pc||requested<=0||!NativePlayerCommit.validFrame(pc))return 0;var owner=NativePlayerCommit.nativeRefCompanionCaster(pc,ref.master());if(owner==null)return 0;var hub=NativeCompanionPlacement.hub(pc);var binding=f.event.target();var life=NativePlayerLifetime.of(hub);var receipt=NativePlayerVitalCompletions.reserve(pc);if(receipt==null)return 0;
  try{NativePairedCastSafety.run(pc,owner,()->{synchronized(hub){
   if(!NativeCompanionProcScene.strict(binding,pc)||!binding.phase().equals("WORLD")||binding.map()!=ref.map()||binding.instance()!=ref.instance()||!hub.classicWorldMembers(ref.map(),ref.instance()).contains(pc)||!hub.connectedSessionsSnapshot().contains(owner)||hub.classicWorldLocation(owner)==null||!NativeCompanionRoleIdentity.actorBinding(ref.master(),owner))return;
   var custody=NativeBattleTamedLifecycle.pets(owner);synchronized(custody){var actor=(BattleCompanion)CombatAccess.call(owner,"ownedCompanion","(J)Llocal/lito381/server/BattleCompanion;",ref.id());if(actor==null)return;synchronized(actor){var timed=(BattleTimedStatus)CombatAccess.field(actor,"timedStatus");synchronized(timed){synchronized(life.admission){f.queue.vitalLock.lock();try{
    if(NativePlayerCommit.frame()!=f||f.queue.life!=life||!life.processes()||!NativePlayerCommit.validFrame(pc)||!NativeCompanionProcScene.strict(binding,pc)||!NativeCompanionRefScene.liveSame(ref,actor,timed,owner))return;
    int actual=NativeCombatAttacker.nativeCombatSceneOriginalDebit(pc,ref,requested);if(actual>0)NativePlayerCommit.nativeManaTransferRecover(receipt,pc,binding,0,actual,"equipment-drain");
   }finally{f.queue.vitalLock.unlock();}}}}}
  }});}finally{receipt.close();NativePlayerVitalCompletions.drain(pc);}
  // WORLD recovery already used the same original numeric/completion body; do not credit again in the caller.
  return 0;
 }
 private NativeCompanionManaTransfer(){}
}
