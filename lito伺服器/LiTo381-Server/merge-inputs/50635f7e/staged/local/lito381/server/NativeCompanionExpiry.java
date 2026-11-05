package local.lito381.server;
import java.io.*;import java.util.*;
/** Source timer is actor-owned even when a failed portal leaves its master elsewhere. */
final class NativeCompanionExpiry {
 record Due(BattleCompanion pet,NativeCompanionPlacement.State placement,NativeCompanionPlacement.Scene scene,Object pose,NativeCompanionCallbacks.Stamp stamp){}
 static void tick(ClientSession owner,long now)throws IOException{
  var hub=NativeCompanionPlacement.hub(owner);var due=new ArrayList<Due>();
  synchronized(hub){
   if(!NativeCompanionPlacement.world(owner)||(boolean)CombatAccess.field(hub,"nativeStaticMotionClosed")||!NativeCompanionCommands.registered(owner))return;
   for(var pet:NativeCompanionPlacement.pets(owner))synchronized(pet){var state=NativeCompanionPlacement.state(pet);var snap=pet.snapshot();
    if(state==null||state.pending!=null||!state.role(owner)||!state.scene.alive(hub)||state.scene.at(NativeCompanionPlacement.map(owner),NativeCompanionPlacement.instance(owner))||snap.kind()!=BattleCompanion.Kind.SUMMON||!snap.active()||snap.hp()<=0||snap.expiresAtNanos()<=0||!pet.expired(now)||NativeSummonTerminalLifecycle.pending(pet)!=null)continue;
    due.add(new Due(pet,state,state.scene,state.pose,NativeCompanionCallbacks.capture(NativePlayerCommit.Binding.capture(owner),pet)));
   }
  }
  // Player ownership precedes Hub/actor ownership. No Hub is retained while waiting for the player.
  for(var captured:due)NativeLightAction.commitPair(owner,owner,()->{synchronized(hub){synchronized(captured.pet()){
   var pet=captured.pet();var state=NativeCompanionPlacement.state(pet);
   if((boolean)CombatAccess.field(hub,"nativeStaticMotionClosed")||!NativeCompanionPlacement.world(owner)||!NativeCompanionCommands.registered(owner)||state!=captured.placement()||state.pending!=null||state.scene!=captured.scene()||state.pose!=captured.pose()||!state.scene.alive(hub)||!state.role(owner)||!NativeSummonOwnerLifecycle.same(owner,pet,captured.stamp())||pet.snapshot().kind()!=BattleCompanion.Kind.SUMMON||!pet.expired(now)||NativeSummonTerminalLifecycle.pending(pet)!=null)return;
   pet.applyDamage(Integer.MAX_VALUE);var terminal=NativeCompanionCallbacks.capture(captured.stamp().master,pet);CombatAccess.set(pet,NativeSummonTerminalLifecycle.PENDING,terminal);NativeSummonTerminalLifecycle.settle(owner,owner,pet,now,terminal);
  }}});
 }
 private NativeCompanionExpiry(){}
}
