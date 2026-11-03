package local.lito381.server;
import java.io.*;import java.util.*;
/** Actual Light commit uses Source action ordering; original reveal and original output tail retain FIFO. */
final class NativeLightAction {
 private interface Output {void run()throws IOException;}
 private static final class Batch implements Runnable {
  boolean captureSelf;ClientSession target;NativePlayerCommit.Binding targetBinding;NativeCastMaterial.Cast material;final ClientSession pc;final NativeCastTimeline.Arrival arrival;final List<Output> outputs=new ArrayList<>();
  final Ingress accepted;Batch(ClientSession pc,NativeCastTimeline.Arrival arrival,Ingress accepted){this.pc=pc;this.arrival=arrival;this.accepted=accepted;}
  public void run(){try{for(var output:outputs){if(!NativeRolePublication.allowPublication(pc))return;output.run();}}catch(IOException failure){throw new UncheckedIOException(failure);}}
 }
 private static final ThreadLocal<Batch> current=new ThreadLocal<>();
 private record Ingress(ClientSession pc,NativePlayerCommit.Binding binding,GameHub hub,NativeDeItemApplication application,NativePlayerVitalCompletions.Receipt receipt,NativeCastTimeline.Arrival arrival){}
 private static final ThreadLocal<Ingress> ingress=new ThreadLocal<>();
 private static Ingress reserve(ClientSession pc){var binding=NativePlayerCommit.Binding.capture(pc);var hub=(GameHub)CombatAccess.field(pc,"hub");var app=(NativeDeItemApplication)CombatAccess.field(hub,NativeDeItemApplication.FIELD);var receipt=NativePlayerVitalCompletions.reserve(pc);return receipt==null?null:new Ingress(pc,binding,hub,app,receipt,NativeCastTimeline.capture(pc));}
 static void dispatch(ClientSession pc,byte[] packet)throws IOException{
  int skill=Protocol.spellSelector(packet).skillId();String phase=String.valueOf(CombatAccess.field(pc,"phase"));
  if((skill!=2&&!NativeSelfCastAction.supported(skill)&&!NativeCubeCastAction.supported(skill)&&!NativeItemWeaponCastAction.supported(skill)&&skill!=73&&skill!=170&&skill!=68&&skill!=112&&skill!=29&&skill!=152&&skill!=153&&skill!=47&&skill!=56&&skill!=58&&skill!=63&&skill!=64&&skill!=71&&skill!=173&&skill!=174&&skill!=40&&skill!=103&&skill!=51&&skill!=154&&skill!=162)||(!phase.equals("BATTLE")&&!phase.equals("WORLD"))||pc.selectedCharacterName().isBlank()){pc.nativeLightSpellDispatch(packet);return;}
  var accepted=reserve(pc);if(accepted==null)return;var previous=ingress.get();ingress.set(accepted);
  try{pc.nativeLightSpellDispatch(packet);}finally{if(previous==null)ingress.remove();else ingress.set(previous);accepted.receipt.close();}
 }
 static void commit(ClientSession pc,BattleSpellCatalog.LightSpell spell,Object phase,boolean world,long now)throws IOException {
  var parent=ingress.get();boolean owns=parent==null||parent.pc!=pc;var accepted=owns?reserve(pc):parent;if(accepted==null)return;
  // The dispatcher token is producer metadata, never inherited by callbacks drained at Source exit.
  ingress.remove();var packetContext=NativeCastTimeline.detach();
  try{NativeSourcePlayerActionLock.run(pc,()->{
   if(!accepted.binding.matches(pc)||(boolean)CombatAccess.field(pc,NativeDeItemApplication.DISCONNECTED)||CombatAccess.field(accepted.hub,NativeDeItemApplication.FIELD)!=accepted.application||accepted.application!=null&&accepted.application.closed)return;
   var batch=new Batch(pc,accepted.arrival,accepted);var previous=current.get();current.set(batch);
   try{var after=pc.nativeLightCommit(spell,phase,System.nanoTime());if(after!=null)batch.outputs.add(()->pc.nativeLightTail(spell,world,after));}
   finally{if(previous==null)current.remove();else current.set(previous);if(!batch.outputs.isEmpty())NativePlayerVitalCompletions.enqueueLocked(accepted.receipt,batch);}
   });}finally{NativeCastTimeline.restore(packetContext);if(parent==null)ingress.remove();else ingress.set(parent);if(owns)accepted.receipt.close();}
 }
 static boolean timelineIngress(ClientSession pc){var accepted=ingress.get();return accepted!=null&&accepted.pc==pc;}
 static boolean admit(ClientSession pc,BattleSpellCatalog.LightSpell spell){var batch=current.get();if(batch==null||batch.pc!=pc)throw new IllegalStateException("missing actual Light batch");return NativeCastTimeline.admit(pc,spell,batch.arrival);}
 static void enterState(ClientSession pc){pc.nativePlayerActionLock().lock();}
 static void exitState(ClientSession pc){pc.nativePlayerActionLock().unlock();}
 static void activate(ClientSession pc,int seconds,long now,int mode){enterState(pc);try{pc.nativeLightActivate(seconds,now,mode);}finally{exitState(pc);}}
 static boolean cancel(ClientSession pc){enterState(pc);try{return pc.nativeLightCancel();}finally{exitState(pc);}}
 static void clear(ClientSession pc){enterState(pc);try{pc.nativeLightClear();}finally{exitState(pc);}}
 static int expire(ClientSession pc,long now){enterState(pc);try{return pc.nativeLightExpire(now);}finally{exitState(pc);}}
 static boolean honorPending(ClientSession pc,long room,long token){enterState(pc);try{return pc.nativeLightHonorPending(room,token);}finally{exitState(pc);}}
 static void storeReady(ClientSession pc,int skill,long ready){enterState(pc);try{pc.nativeLightStoreReady(skill,ready);}finally{exitState(pc);}}
 static void publish(ClientSession pc,String reason)throws IOException{
  var batch=current.get();if(batch!=null&&batch.pc==pc){batch.outputs.add(()->pc.nativeLightPublishReveal(reason));return;}if(batch==null)current.remove();pc.nativeLightPublishReveal(reason);
 }
 // Match the Source stable-before-status order before the original PC monitor is entered.
 static void enterRespawn(ClientSession pc){pc.nativePlayerActionLock().lock();}
 static void exitRespawn(ClientSession pc){pc.nativePlayerActionLock().unlock();NativePlayerVitalCompletions.drain(pc);}
 private NativeLightAction(){}

 static void commitSelf(ClientSession pc,NativeSelfCastAction.Task body)throws IOException {
  var parent=ingress.get();boolean owns=parent==null||parent.pc!=pc;var accepted=owns?reserve(pc):parent;if(accepted==null)return;
  ingress.remove();var packetContext=NativeCastTimeline.detach();
  try{try{NativeSourcePlayerActionLock.run(pc,()->{
   if(!accepted.binding.matches(pc)||(boolean)CombatAccess.field(pc,NativeDeItemApplication.DISCONNECTED)||CombatAccess.field(accepted.hub,NativeDeItemApplication.FIELD)!=accepted.application||accepted.application!=null&&accepted.application.closed)return;
   var batch=new Batch(pc,accepted.arrival,accepted);batch.captureSelf=true;var previous=current.get();current.set(batch);
   try{body.run();}catch(IOException failure){throw new UncheckedIOException(failure);}
   finally{if(previous==null)current.remove();else current.set(previous);if(!batch.outputs.isEmpty())NativePlayerVitalCompletions.enqueueLocked(accepted.receipt,batch);}
  });}catch(UncheckedIOException failure){throw failure.getCause();}}
  finally{NativeCastTimeline.restore(packetContext);if(parent==null)ingress.remove();else ingress.set(parent);if(owns)accepted.receipt.close();}
 }
 static boolean deferSelfSend(ClientSession receiver,byte[] plain,String note){
  var batch=current.get();if(batch==null){current.remove();return false;}if(!batch.captureSelf)return false;
  if(!batch.pc.nativePlayerActionLock().isHeldByCurrentThread())throw new IllegalStateException("self cast output capture requires actual action lock");
  byte[] snapshot=plain.clone();batch.outputs.add(()->{if(batch.target!=null&&(receiver==batch.target||(note.startsWith("S_EFFECT Water-Life target=")||(note.startsWith("S_EFFECT immune-to-harm target=")||(note.startsWith("S_EFFECT Armor Break target=")||note.contains("Entangle target=")||(note.contains("Erase Magic target=")||note.contains("Weakness target=")||(note.contains("Disease target=")||(note.contains("Silence target=")||(note.contains("Decay Potion target=")||note.contains("Pollute Water target=")||(note.contains("Striker Gale target=")||note.contains("Blind target=")||note.contains("blind target="))))))))))&&!batch.targetBinding.matches(batch.target))return;receiver.nativeSelfOriginalSend(snapshot,note);});return true;
 }
 static boolean admitSelf(ClientSession pc,int skill,int action,int reuse){
  var batch=current.get();if(batch==null||batch.pc!=pc||!batch.captureSelf)throw new IllegalStateException("missing actual self cast batch");
  return NativeCastTimeline.admitSelf(pc,skill,action,reuse,batch.arrival);
 }

 static NativeCastMaterial.Cast material(ClientSession pc,int skill){var batch=current.get();if(batch==null||batch.pc!=pc||!pc.nativePlayerActionLock().isHeldByCurrentThread())throw new IllegalStateException("material requires actual owned cast batch");if(batch.material==null)batch.material=new NativeCastMaterial.Cast(skill);if(batch.material.skill!=skill)throw new IllegalStateException("material cast identity changed");return batch.material;}
 static void materialOutput(ClientSession pc,byte[] plain,String note){var batch=current.get();if(batch==null||batch.pc!=pc||!pc.nativePlayerActionLock().isHeldByCurrentThread())throw new IllegalStateException("material output requires actual owned cast batch");byte[] snapshot=plain.clone();batch.outputs.add(()->pc.nativeSelfOriginalSend(snapshot,note));}
 static NativeCastMaterial.Cast currentMaterial(ClientSession pc){var batch=current.get();if(batch==null){current.remove();return null;}return batch.pc==pc?batch.material:null;}
 static boolean retainedMaterial(ClientSession pc){var batch=current.get();if(batch==null){current.remove();return false;}if(batch.pc!=pc||!pc.nativePlayerActionLock().isHeldByCurrentThread())return false;var accepted=batch.accepted;return accepted.pc==pc&&accepted.binding.matches(pc)&&accepted.receipt.pc==pc&&!accepted.receipt.released.get()&&accepted.receipt.life==NativePlayerLifetime.of(pc)&&CombatAccess.field(accepted.hub,NativeDeItemApplication.FIELD)==accepted.application&&(accepted.application==null||!accepted.application.closed)&&!(boolean)CombatAccess.field(pc,NativeDeItemApplication.DISCONNECTED);}
 static void commitPair(ClientSession pc,ClientSession target,NativeSelfCastAction.Task body)throws IOException {
  var targetBinding=NativePlayerCommit.Binding.capture(target);
  var parent=ingress.get();boolean owns=parent==null||parent.pc!=pc;var accepted=owns?reserve(pc):parent;if(accepted==null)return;
  ingress.remove();var packetContext=NativeCastTimeline.detach();
  try{try{NativeSourcePlayerActionLock.runPairOrdered(pc,target,()->{
   if(!accepted.binding.matches(pc)||!targetBinding.matches(target)
    ||(boolean)CombatAccess.field(pc,NativeDeItemApplication.DISCONNECTED)||(boolean)CombatAccess.field(target,NativeDeItemApplication.DISCONNECTED)
    ||CombatAccess.field(accepted.hub,NativeDeItemApplication.FIELD)!=accepted.application||accepted.application!=null&&accepted.application.closed)return;
   var batch=new Batch(pc,accepted.arrival,accepted);batch.captureSelf=true;batch.target=target;batch.targetBinding=targetBinding;
   var previous=current.get();current.set(batch);
   try{body.run();}catch(IOException failure){throw new UncheckedIOException(failure);}
   finally{if(previous==null)current.remove();else current.set(previous);if(!batch.outputs.isEmpty())NativePlayerVitalCompletions.enqueueLocked(accepted.receipt,batch);}
  });}catch(UncheckedIOException failure){throw failure.getCause();}}
  finally{NativeCastTimeline.restore(packetContext);if(parent==null)ingress.remove();else ingress.set(parent);if(owns)accepted.receipt.close();}
 }
 static boolean admitPair(ClientSession pc,ClientSession target,int skill,int action,int reuse){
  var batch=current.get();if(batch==null||batch.pc!=pc||batch.target!=target||!batch.targetBinding.matches(target))return false;
  if(!pc.nativePlayerActionLock().isHeldByCurrentThread()||!target.nativePlayerActionLock().isHeldByCurrentThread())throw new IllegalStateException("target cast requires both ordered action locks");
  return admitSelf(pc,skill,action,reuse);
 }

}
