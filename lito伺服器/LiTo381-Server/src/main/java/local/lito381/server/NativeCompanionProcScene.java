package local.lito381.server;
import java.util.function.Function;import java.lang.ref.WeakReference;import java.io.IOException;
/** WORLD Proc custody belongs to the actor; source player actions remain strictly bound. */
final class NativeCompanionProcScene {
 static boolean strict(NativePlayerCommit.Binding binding,ClientSession pc){return binding!=null&&pc!=null&&binding.owner()==NativeCompanionRoleIdentity.currentOwner(pc)&&binding.matchesFast(pc)&&!(boolean)CombatAccess.field(pc,"sourceItemDisconnected");}
 static ClientSession master(GameHub hub,NativePlayerCommit.Binding binding){for(var pc:hub.connectedSessionsSnapshot())if(NativeCompanionRoleIdentity.actorBinding(binding,pc))return pc;return null;}
 static ClientSession caster(GameHub hub,NativePlayerCommit.Binding binding){for(var pc:hub.connectedSessionsSnapshot())if(strict(binding,pc))return pc;return null;}
 static boolean meterBinding(NativePlayerCommit.Binding binding,ClientSession owner){return binding!=null&&binding.phase().equals("WORLD")?NativeCompanionRoleIdentity.actorBinding(binding,owner):binding!=null&&binding.matches(owner);}
 static boolean meterLive(GameHub hub,NativeCombatAttacker.Ref ref,ClientSession owner){synchronized(hub){
  if((boolean)CombatAccess.field(hub,"nativeStaticMotionClosed")||!hub.connectedSessionsSnapshot().contains(owner)||hub.classicWorldLocation(owner)==null||!NativeCompanionRoleIdentity.actorBinding(ref.master(),owner))return false;
  var custody=NativeBattleTamedLifecycle.pets(owner);synchronized(custody){var actor=(BattleCompanion)CombatAccess.call(owner,"ownedCompanion","(J)Llocal/lito381/server/BattleCompanion;",ref.id());if(actor==null)return false;synchronized(actor){var snapshot=actor.snapshot();var timed=(BattleTimedStatus)CombatAccess.field(actor,"timedStatus");var state=NativeCompanionPlacement.state(actor);return snapshot.active()&&snapshot.hp()>0&&snapshot.objectId()==ref.id()&&NativeCombatAttacker.token(timed,false)==ref.owner()&&(state==null||state.role(owner)&&state.scene.alive(hub));}}
 }}
 /** Capture under the owner's action lock; only weak recipients and value proof reach unlocked I/O. */
 static void sendMeter(ClientSession owner,byte[] packet,String reason,GameHub hub,NativeCombatAttacker.Ref ref){
  if(!NativeCompanionRefScene.worldRef(ref)){NativePlayerCommit.safeSend(owner,packet,reason);return;}
  var weakOwner=new WeakReference<>(owner);var weakHub=new WeakReference<>(hub);Runnable[] emit={null};
  NativeSourcePlayerActionLock.run(owner,()->{if(meterLive(hub,ref,owner))emit[0]=NativeRolePublication.completion(owner,()->{var currentOwner=weakOwner.get();var currentHub=weakHub.get();if(currentOwner!=null&&currentHub!=null&&meterLive(currentHub,ref,currentOwner))try{currentOwner.nativeSelfOriginalSend(packet,reason);}catch(IOException failure){NativePlayerCommit.secondaryFailure("companion Proc meter",new RuntimeException(failure));}});});
  if(emit[0]!=null)emit[0].run();
 }
 @SuppressWarnings("unchecked") static <T>T withTarget(GameHub hub,NativeProcHealingCompanion.Plan plan,boolean requireCaster,Function<Object,T> action){
  var ref=plan.target();var owner=master(hub,ref.master());var caster=requireCaster?caster(hub,plan.caster()):owner;if(owner==null||caster==null)return null;Object[] result={null};
  NativePairedCastSafety.run(caster,owner,()->{synchronized(hub){
   if(!caster.nativePlayerActionLock().isHeldByCurrentThread()||!owner.nativePlayerActionLock().isHeldByCurrentThread())throw new IllegalStateException("Proc requires paired source locks");
   if((boolean)CombatAccess.field(hub,"nativeStaticMotionClosed")||!hub.connectedSessionsSnapshot().contains(owner)||!NativeCompanionRoleIdentity.actorBinding(ref.master(),owner)||hub.classicWorldLocation(owner)==null)return;
   if(requireCaster&&(!strict(plan.caster(),caster)||!plan.caster().phase().equals("WORLD")||plan.caster().map()!=ref.map()||plan.caster().instance()!=ref.instance()||!hub.classicWorldMembers(ref.map(),ref.instance()).contains(caster)))return;
   var custody=NativeBattleTamedLifecycle.pets(owner);synchronized(custody){
   var actor=(BattleCompanion)CombatAccess.call(owner,"ownedCompanion","(J)Llocal/lito381/server/BattleCompanion;",ref.id());if(actor==null)return;
   synchronized(actor){var timed=(BattleTimedStatus)CombatAccess.field(actor,"timedStatus");synchronized(timed){
    var snapshot=actor.snapshot();if(!snapshot.active()||snapshot.hp()<=0||snapshot.objectId()!=ref.id()||NativeCombatAttacker.token(timed,false)!=ref.owner())return;
    var state=NativeCompanionPlacement.state(actor);
    if(state==null){if(!NativeCompanionRoleIdentity.actorBinding(ref.master(),owner)||requireCaster&&(NativeCompanionPlacement.map(owner)!=ref.map()||NativeCompanionPlacement.instance(owner)!=ref.instance()))return;}
    else if(!state.role(owner)||!state.scene.alive(hub)||requireCaster&&!state.scene.at(ref.map(),ref.instance()))return;
    result[0]=action.apply(NativeProcHealingCompanion.nativeProcSceneLive(requireCaster?caster:null,owner,actor,timed,snapshot));
   }}}
  }});return (T)result[0];
 }
 private NativeCompanionProcScene(){}
}
