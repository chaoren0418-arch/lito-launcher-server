package local.lito381.server;
/** Retire the dead HP queue only at the successful native resurrection HP assignment. */
final class NativeResurrectionQueueLife {
 static boolean captureMana(ClientSession receiver,byte[] packet,String note){
  if(packet.length==0||(packet[0]&255)!=Protocol.S_MANA_POINT)return false;
  var material=NativeLightAction.currentMaterial(receiver);if(material==null||(material.skill!=61&&material.skill!=75))return false;
  // Keep the original normal output slot; a retired peer must not cancel the caster's own committed cost.
  var binding=NativePlayerCommit.Binding.capture(receiver);byte[] frozen=packet.clone();
  var own=NativeRolePublication.completion(receiver,()->{if(binding.matches(receiver)&&NativeRolePublication.allowPublication(receiver))try{receiver.nativeSelfOriginalSend(frozen,note);}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}});
  NativeLightAction.materialOutput(receiver,frozen,note);
  NativeLightAction.finishScoped((published,failure)->{if(!published&&failure==null)own.run();});return true;
 }
 static void restored(ClientSession pc){
  if(!pc.nativePlayerActionLock().isHeldByCurrentThread())throw new IllegalStateException("resurrection requires owned Source action");
  if((int)CombatAccess.field(pc,"playerHp")<=0)throw new IllegalStateException("resurrection has no restored HP");
  var timed=(BattleTimedStatus)CombatAccess.field(pc,"timedStatus");synchronized(timed){
   var state=CombatPoisonLifecycle.monsterPoisonCallbacks(timed,true);var queue=(NativePlayerCommit.Queue)CombatAccess.field(state,"sourcePlayerQueue");
   if(queue!=null){queue.vitalLock.lock();try{queue.drop(true);CombatAccess.set(state,"sourcePlayerQueue",null);}finally{queue.vitalLock.unlock();}}
   // Keep the selected role binding; its previous HP lane and corpse epoch must never be reused.
   CombatAccess.set(state,"sourceNpcOwner",new Object());
  }
 }
 private NativeResurrectionQueueLife(){}
}
