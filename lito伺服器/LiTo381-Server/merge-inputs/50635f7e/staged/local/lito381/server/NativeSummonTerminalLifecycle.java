// DECOMPILED from candidate-50635f7e.jar, TODO review
package local.lito381.server;
import java.io.*;import java.util.*;
/** Ordinary Source summon death: keep its bag attached until the existing transaction succeeds. */
final class NativeSummonTerminalLifecycle {
 static final String P="local/lito381/server/",PENDING="nativeSummonTerminalStamp";
 static NativeCompanionCallbacks.Stamp pending(BattleCompanion pet){return (NativeCompanionCallbacks.Stamp)CombatAccess.field(pet,PENDING);}
 static boolean registered(ClientSession owner,GameHub hub){return String.valueOf(CombatAccess.field(owner,"phase")).equals("WORLD")?NativeCompanionCommands.registered(owner):String.valueOf(CombatAccess.field(owner,"phase")).equals("BATTLE")&&hub.roomMembers((long)CombatAccess.field(owner,"currentRoomId")).stream().anyMatch(pc->pc==owner);}
 static boolean valid(ClientSession owner,BattleCompanion pet,NativeCompanionCallbacks.Stamp stamp){
  if(stamp==null||!stamp.master.matches(owner)||(boolean)CombatAccess.field(owner,"sourceItemDisconnected")||CombatAccess.call(owner,"ownedCompanion","(J)L"+P+"BattleCompanion;",stamp.companion)!=pet)return false;
  var hub=(GameHub)CombatAccess.field(owner,"hub");if((boolean)CombatAccess.field(hub,"nativeStaticMotionClosed")||!registered(owner,hub))return false;
  var snap=pet.snapshot();return snap.kind()==BattleCompanion.Kind.SUMMON&&!snap.active()&&snap.hp()==0&&stamp.life==NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(pet,"timedStatus"),false);
 }
 static boolean transferable(ClientSession owner,BattleCompanion pet){
  if(pet.snapshot().active())return true;
  var producer=NativeReturnNatureMaterialAction.caster(owner,pet);return producer!=null&&owner.nativePlayerActionLock().isHeldByCurrentThread()&&producer.nativePlayerActionLock().isHeldByCurrentThread()&&NativeLightAction.retainedMaterial(producer)&&(valid(owner,pet,pending(pet))||NativeSummonRetirement.ownerDeletion(owner,pet));
 }
 static boolean releasable(ClientSession owner,BattleCompanion pet,BattleCompanion.Snapshot snap){return snap.active()||transferable(owner,pet);}
 static BattleCompanion arm(BattleCompanion pet,ClientSession owner){
  if(pet==null||pet.snapshot().kind()!=BattleCompanion.Kind.SUMMON||pet.snapshot().expiresAtNanos()<=0)return pet;
  var hub=(GameHub)CombatAccess.field(owner,"hub");long next=NativeSummonOwnerLifecycle.phase(hub,System.nanoTime());if(next!=0)synchronized(pet){CombatAccess.set(pet,"expiresAtNanos",Math.addExact(next,179*NativeSummonOwnerLifecycle.PERIOD));}return pet;
 }
 static boolean finish(ClientSession owner,BattleCompanion pet,long now,boolean expired,NativeCompanionCallbacks.Stamp original)throws IOException{
  var hub=(GameHub)CombatAccess.field(owner,"hub");if((boolean)CombatAccess.field(hub,"nativeStaticMotionClosed"))return false;boolean[] done={false};
  NativeLightAction.commitPair(owner,owner,()->{synchronized(hub){
   if((boolean)CombatAccess.field(hub,"nativeStaticMotionClosed")||!registered(owner,hub))return;
   synchronized(pet){
    var stamp=pending(pet);
    if(stamp==null){
     if(!NativeSummonOwnerLifecycle.same(owner,pet,original)||pet.snapshot().kind()!=BattleCompanion.Kind.SUMMON||expired&&!pet.expired(now))return;
     pet.applyDamage(Integer.MAX_VALUE);stamp=NativeCompanionCallbacks.capture(original.master,pet);CombatAccess.set(pet,PENDING,stamp);
    }
    done[0]=settle(owner,owner,pet,now,stamp);
   }
  }});return done[0];
 }
 static boolean settle(ClientSession producer,ClientSession owner,BattleCompanion pet,long now,NativeCompanionCallbacks.Stamp stamp)throws IOException{
  if(pending(pet)!=stamp||!valid(owner,pet,stamp)||producer!=NativeLightAction.currentMaterialProducer()||!NativeLightAction.retainedMaterial(producer)||!owner.nativePlayerActionLock().isHeldByCurrentThread())return false;
  var slot=NativeReturnNatureMaterialAction.ownerSlot();Object previous=slot.get();slot.set(NativeReturnNatureMaterialAction.ownerContext(producer,owner,pet,stamp));boolean done=false;
  try{return done=NativeReturnNatureTransfer.release(owner,pet,pet.snapshot(),169);}
  finally{if(previous==null)slot.remove();else slot.set(previous);if(done){CombatAccess.set(pet,PENDING,null);CombatAccess.set(pet,NativeSummonRetirement.FALLBACK,null);}else if(valid(owner,pet,stamp)){NativeSummonRetirement.remember(owner,pet,stamp);long next=NativeSummonOwnerLifecycle.phase((GameHub)CombatAccess.field(owner,"hub"),now);if(next!=0)CombatAccess.set(pet,"expiresAtNanos",next);}}
 }
 static ClientSession owner(ClientSession source,BattleCompanion pet,GameHub hub){
  var phase=String.valueOf(CombatAccess.field(source,"phase"));var members=phase.equals("WORLD")?hub.classicWorldMembers((int)CombatAccess.field(source,"classicWorldMapId"),(int)CombatAccess.field(source,"classicWorldInstanceId")):phase.equals("BATTLE")?hub.roomMembers((long)CombatAccess.field(source,"currentRoomId")):List.<ClientSession>of();
  for(var pc:members)if(CombatAccess.call(pc,"ownedCompanion","(J)L"+P+"BattleCompanion;",pet.snapshot().objectId())==pet)return pc;return null;
 }
 static BattleCompanion.Damage combatDamage(ClientSession source,BattleCompanion pet,int amount)throws IOException{
  if(pet==null||pet.snapshot().kind()!=BattleCompanion.Kind.SUMMON)return NativeCombatAttacker.companionDamage(pet,amount);
  var hub=(GameHub)CombatAccess.field(source,"hub");var start=pet.snapshot();var result=new BattleCompanion.Damage[]{new BattleCompanion.Damage(start,start,0,false)};ClientSession owner;NativePlayerCommit.Binding sourceBinding;NativeCompanionCallbacks.Stamp original;
  synchronized(hub){if((boolean)CombatAccess.field(hub,"nativeStaticMotionClosed")||!registered(source,hub))return result[0];owner=owner(source,pet,hub);if(owner==null)return result[0];sourceBinding=NativeCompanionReflectionScene.capture(source);original=NativeCompanionCallbacks.capture(NativeCompanionReflectionScene.capture(owner),pet);}
  NativeSelfCastAction.Task mutation=()->{synchronized(hub){synchronized(pet){
   if((boolean)CombatAccess.field(hub,"nativeStaticMotionClosed")||(boolean)CombatAccess.field(source,"sourceItemDisconnected")||(boolean)CombatAccess.field(owner,"sourceItemDisconnected")||!sourceBinding.matches(source)||!registered(source,hub)||!registered(owner,hub)||!NativeSummonOwnerLifecycle.same(owner,pet,original))return;
   result[0]=NativeCombatAttacker.companionDamage(pet,amount);
   if(result[0].diedNow()){var stamp=NativeCompanionCallbacks.capture(original.master,pet);CombatAccess.set(pet,PENDING,stamp);settle(NativeLightAction.currentMaterialProducer(),owner,pet,System.nanoTime(),stamp);}
  }}};
  var producer=NativeLightAction.currentMaterialProducer();if(producer!=null&&NativeLightAction.retainedMaterial(producer)&&source.nativePlayerActionLock().isHeldByCurrentThread()&&owner.nativePlayerActionLock().isHeldByCurrentThread())mutation.run();else NativeLightAction.commitPair(source,owner,mutation);return result[0];
 }
 // Ordinary Source summons disappear after their claimed cleanup; PET/TAMED tails are unchanged.
 static int legacyApplied(BattleCompanion.Damage result){return result.diedNow()&&result.before().kind()==BattleCompanion.Kind.SUMMON?0:result.appliedDamage();}
 static boolean expiry(BattleCompanion pet,long now,ClientSession owner)throws IOException{
  if(pet.snapshot().kind()!=BattleCompanion.Kind.SUMMON||!pet.expired(now))return false;
  finish(owner,pet,now,true,NativeCompanionCallbacks.capture(NativePlayerCommit.Binding.capture(owner),pet));return true;
 }
 static void retry(ClientSession owner,long now)throws IOException{
  var hub=(GameHub)CombatAccess.field(owner,"hub");List<BattleCompanion> pets;
  synchronized(hub){if((boolean)CombatAccess.field(hub,"nativeStaticMotionClosed"))return;pets=new ArrayList<>(NativeBattleTamedLifecycle.pets(owner));var fixed=(BattleCompanion)CombatAccess.field(owner,"battleCompanion");if(fixed!=null&&!pets.contains(fixed))pets.add(fixed);}
  for(var pet:pets){var stamp=pending(pet);if(stamp!=null&&now>=pet.snapshot().expiresAtNanos())finish(owner,pet,now,false,stamp);}
 }
 private NativeSummonTerminalLifecycle(){}
}
