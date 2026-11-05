package local.lito381.server;
import java.io.*;import java.util.*;
/** Source ten-second phase and owner/expiry use of the existing liberation core. */
final class NativeSummonOwnerLifecycle {
 static final String P="local/lito381/server/",PHASE="nativeSummonNextPhaseNanos",OWNER="nativeSummonOwnerContext",PUBLICATION="nativeSummonPublication";
 static final long PERIOD=10_000_000_000L;
 static long phase(GameHub hub,long now){synchronized(hub){if((boolean)CombatAccess.field(hub,"nativeStaticMotionClosed"))return 0;long next=(long)CombatAccess.field(hub,PHASE);if(next==0)next=Math.addExact(now,PERIOD);else if(now>=next)next=Math.addExact(next,Math.multiplyExact(1+(now-next)/PERIOD,PERIOD));CombatAccess.set(hub,PHASE,next);return next;}}
 static void clock(GameHub hub){phase(hub,System.nanoTime());}
 static BattleCompanion arm(BattleCompanion pet,ClientSession owner){if(pet==null||pet.snapshot().kind()!=BattleCompanion.Kind.TAMED)return pet;var hub=(GameHub)CombatAccess.field(owner,"hub");long next=phase(hub,System.nanoTime());if(next!=0)synchronized(pet){CombatAccess.set(pet,"expiresAtNanos",Math.addExact(next,359*PERIOD));}return pet;}
 static boolean same(ClientSession owner,BattleCompanion pet,NativeCompanionCallbacks.Stamp stamp){if(stamp==null||!stamp.master.matches(owner)||CombatAccess.call(owner,"ownedCompanion","(J)L"+P+"BattleCompanion;",stamp.companion)!=pet)return false;synchronized(pet){return pet.snapshot().active()&&pet.snapshot().hp()>0&&stamp.life==NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(pet,"timedStatus"),false);}}
 @SuppressWarnings("unchecked") static boolean release(ClientSession owner,BattleCompanion pet,long now,boolean expired)throws IOException{
  var hub=(GameHub)CombatAccess.field(owner,"hub");if((boolean)CombatAccess.field(hub,"nativeStaticMotionClosed"))return false;return release(owner,pet,now,expired,NativeCompanionCallbacks.capture(NativePlayerCommit.Binding.capture(owner),pet));
 }
 static boolean release(ClientSession owner,BattleCompanion pet,long now,boolean expired,NativeCompanionCallbacks.Stamp stamp)throws IOException{
  var hub=(GameHub)CombatAccess.field(owner,"hub");if((boolean)CombatAccess.field(hub,"nativeStaticMotionClosed")||stamp==null)return false;boolean[] done={false};
  NativeLightAction.commitPair(owner,owner,()->{synchronized(hub){if((boolean)CombatAccess.field(hub,"nativeStaticMotionClosed")||!same(owner,pet,stamp)||expired&&!pet.expired(now))return;
   ThreadLocal<Object> slot=NativeReturnNatureMaterialAction.ownerSlot();Object previous=slot.get();slot.set(NativeReturnNatureMaterialAction.ownerContext(owner,pet,stamp));
   try{done[0]=(boolean)CombatAccess.call(owner,"releaseCompanionToNature","(L"+P+"BattleCompanion;L"+P+"BattleCompanion$Snapshot;I)Z",pet,pet.snapshot(),0);}
   finally{if(previous==null)slot.remove();else slot.set(previous);if(expired&&!done[0]&&same(owner,pet,stamp)){long next=phase(hub,now);if(next!=0)CombatAccess.set(pet,"expiresAtNanos",next);}}
  }});return done[0];
 }
 static String statusAction(String action){if(action==null)return null;try{return switch(Integer.parseInt(action.trim())){case 1->"aggressive";case 2->"defensive";case 3->"stay";case 4->"extend";case 5->"alert";default->null;};}catch(NumberFormatException ignored){return switch(action){case "aggressive","defensive","stay","extend","alert"->action;default->null;};}}
 static BattleCompanion.CommandResult group(ClientSession owner,BattleCompanion selected,String action,NativeCompanionCallbacks.Stamp selectedStamp)throws IOException{
  if(selectedStamp==null)return BattleCompanion.CommandResult.REJECTED;var hub=(GameHub)CombatAccess.field(owner,"hub");NativeAreaMaterialAction.Context scene;var frozen=new LinkedHashMap<BattleCompanion,NativeCompanionCallbacks.Stamp>();
  synchronized(hub){if((boolean)CombatAccess.field(hub,"nativeStaticMotionClosed"))return BattleCompanion.CommandResult.REJECTED;scene=new NativeAreaMaterialAction.Context(owner);var candidates=new ArrayList<>(NativeBattleTamedLifecycle.pets(owner));var fixed=(BattleCompanion)CombatAccess.field(owner,"battleCompanion");if(fixed!=null)candidates.add(fixed);for(var candidate:candidates){var kind=candidate.snapshot().kind();if(kind==BattleCompanion.Kind.SUMMON||kind==BattleCompanion.Kind.TAMED)frozen.put(candidate,NativeCompanionCallbacks.capture(selectedStamp.master,candidate));}}
  var result=new BattleCompanion.CommandResult[]{BattleCompanion.CommandResult.REJECTED};NativeLightAction.commitPair(owner,owner,()->{synchronized(scene.hub){
   boolean member=scene.phase.equals("WORLD")?NativeCompanionCommands.registered(owner):scene.phase.equals("BATTLE")&&scene.hub.roomMembers(scene.room).stream().anyMatch(pc->pc==owner);
   if((boolean)CombatAccess.field(scene.hub,"nativeStaticMotionClosed")||!member||!scene.scene()||!same(owner,selected,selectedStamp))return;
   for(var entry:frozen.entrySet())if(same(owner,entry.getKey(),entry.getValue())){var changed=entry.getKey().apply(action);if(entry.getKey()==selected)result[0]=changed;}
  }});return result[0];
 }
 static BattleCompanion.CommandResult apply(BattleCompanion pet,String action,ClientSession owner,Protocol.HypertextActionRequest request)throws IOException{return apply(pet,action,owner,request,NativeCompanionCallbacks.capture(NativePlayerCommit.Binding.capture(owner),pet));}
 static BattleCompanion.CommandResult apply(BattleCompanion pet,String action,ClientSession owner,Protocol.HypertextActionRequest request,NativeCompanionCallbacks.Stamp selected)throws IOException{
  if(pet.snapshot().kind()==BattleCompanion.Kind.PET)return pet.apply(action);String status=statusAction(action);if(status!=null)return group(owner,pet,status,selected);
  if(pet.snapshot().kind()==BattleCompanion.Kind.SUMMON&&(request.kind()==Protocol.HypertextActionKind.DISMISS||action.equals("6")))return NativeSummonTerminalLifecycle.finish(owner,pet,System.nanoTime(),false,selected)?BattleCompanion.CommandResult.DISMISSED:BattleCompanion.CommandResult.REJECTED;
  if(pet.snapshot().kind()!=BattleCompanion.Kind.TAMED||(request.kind()!=Protocol.HypertextActionKind.DISMISS&&!action.equals("6")))return pet.apply(action);
  return release(owner,pet,System.nanoTime(),false,selected)?BattleCompanion.CommandResult.DISMISSED:BattleCompanion.CommandResult.REJECTED;
 }
 static boolean liberated(ClientSession owner,BattleCompanion pet){return pet.snapshot().kind()!=BattleCompanion.Kind.PET&&!pet.snapshot().active()&&CombatAccess.call(owner,"ownedCompanion","(J)L"+P+"BattleCompanion;",pet.snapshot().objectId())!=pet;}
 static boolean expiry(BattleCompanion pet,long now,ClientSession owner)throws IOException{if(pet.snapshot().kind()==BattleCompanion.Kind.SUMMON)return NativeSummonTerminalLifecycle.expiry(pet,now,owner);if(pet.snapshot().kind()!=BattleCompanion.Kind.TAMED||!pet.expired(now))return false;release(owner,pet,now,true);return true;}
 // Only this owner scope may omit the spell visual; ordinary spell validation is unchanged.
 static byte[] effectObject(long objectId,int gfx){Object target=NativeReturnNatureMaterialAction.ownerSlot().get();if(gfx==0&&target!=null&&(boolean)CombatAccess.field(target,OWNER))return new byte[0];return Protocol.effectObject(objectId,gfx);}
 static boolean objectPacket(int opcode){return opcode==Protocol.S_PUT_OBJECT||opcode==Protocol.S_REMOVE_OBJECT||opcode==Protocol.S_MOVE_OBJECT||opcode==Protocol.S_CHANGE_DIRECTION||opcode==Protocol.S_ACTION||opcode==Protocol.S_EFFECT||opcode==Protocol.S_POISON;}
 static boolean captureOutput(ClientSession receiver,byte[] packet,String note){
  Object target=NativeReturnNatureMaterialAction.ownerSlot().get();if(target==null||!(boolean)CombatAccess.field(target,OWNER))return NativeReturnNatureMaterialAction.nativeSummonOriginalCaptureOutput(receiver,packet,note);
  // Source tamed liberation has no Return-to-Nature spell visual.
  if(((BattleCompanion)CombatAccess.field(target,"pet")).snapshot().kind()==BattleCompanion.Kind.TAMED&&note.contains("Return-to-Nature")&&(packet.length==0||(packet[0]&255)==Protocol.S_EFFECT))return true;
  var owner=(ClientSession)CombatAccess.field(target,"owner");var context=(NativeAreaMaterialAction.Context)CombatAccess.field(target,PUBLICATION);if(context==null){context=new NativeAreaMaterialAction.Context(owner);CombatAccess.set(target,PUBLICATION,context);}var scene=context;var binding=scene.bindings.get(receiver);if(binding==null)return true;byte[] frozen=packet.clone();int offset=frozen.length>0&&(frozen[0]&255)==Protocol.S_PUT_OBJECT?5:1;long id=frozen.length>=offset+4&&objectPacket(frozen[0]&255)?Integer.toUnsignedLong((frozen[offset]&255)|(frozen[offset+1]&255)<<8|(frozen[offset+2]&255)<<16|(frozen[offset+3]&255)<<24):-1;boolean npc=scene.npcs.containsKey(id);
  NativeLightAction.deferScoped(()->{Runnable[] completion={null};NativeSourcePlayerActionLock.run(receiver,()->{if(binding.matches(receiver))completion[0]=NativeRolePublication.completion(receiver,()->{
   boolean valid;synchronized(scene.hub){valid=!(boolean)CombatAccess.field(scene.hub,"nativeStaticMotionClosed")&&binding.matches(receiver)&&scene.scene()&&scene.player(receiver)&&NativeReturnNatureMaterialAction.ownerCanPublish(target)&&(!npc||scene.npc(id));}
   if(valid)try{receiver.nativeSelfOriginalSend(frozen,note);}catch(IOException error){throw new UncheckedIOException(error);}
  });});if(completion[0]!=null)completion[0].run();});return true;
 }
 private NativeSummonOwnerLifecycle(){}
}
