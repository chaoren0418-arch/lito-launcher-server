// DECOMPILED from candidate-50635f7e.jar, TODO review
package local.lito381.server;
import java.io.IOException;import java.util.*;
/** Skill27 NPC targets are captured before the existing pair wait and checked before admission. */
final class NativeWeaponBreakTargetLifecycle {
 static boolean targetPublication(NativePlayerCommit.Binding binding,ClientSession target,NativeCastMaterial.Cast material){return material!=null&&material.skill==27||NativeSourceControlledRoleFrame.current(binding,target);}
 static void nativeSourceWeaponOriginalDispatch(ClientSession pc,byte[] packet,NativeSelfCastAction.Task body)throws IOException{
  String phase=String.valueOf(CombatAccess.field(pc,"phase"));
  if(Protocol.spellSelector(packet).skillId()!=27||!Set.of("WORLD","BATTLE").contains(phase)||pc.selectedCharacterName().isBlank()){NativeCancellationNpcMaterialAction.dispatch(pc,packet,body);return;}
  long id=Protocol.attackSpellRequest(packet).targetObjectId();var hub=(GameHub)CombatAccess.field(pc,"hub");var players=phase.equals("WORLD")?hub.classicWorldMembers((int)CombatAccess.field(pc,"classicWorldMapId"),NativeStaticNpcScene.instance(pc)):hub.roomCombatants((long)CombatAccess.field(pc,"currentRoomId"));
  var selected=players.stream().filter(p->p.playerObjectId()==id).findFirst().orElse(pc.playerObjectId()==id?pc:null);
  var captured=new NativeAreaMaterialAction.Context(pc);var npc=captured.npcs.get(id);if(selected==null&&npc==null)return;CombatAccess.set(captured,"nativeWeaponBreakTarget",1);CombatAccess.set(captured,"nativeWeaponBreakTargetId",id);
  var owner=selected!=null?selected:npc.kind()==3?(ClientSession)npc.owner():pc;var receipt=owner==pc?null:NativePlayerVitalCompletions.reserve(owner);if(owner!=pc&&receipt==null)return;CombatAccess.set(captured,"nativeWeaponBreakPeerReceipt",receipt);
  var prior=NativeAreaMaterialAction.current.get();NativeAreaMaterialAction.current.set(captured);
  try{NativeCancellationNpcMaterialAction.dispatch(pc,packet,body);}finally{if(prior==null)NativeAreaMaterialAction.current.remove();else NativeAreaMaterialAction.current.set(prior);if(receipt!=null&&!receipt.released.get())receipt.close();}
 }
 static void dispatch(ClientSession pc,byte[] packet,NativeSelfCastAction.Task body)throws IOException{NativeSourceWeaponTargetLife.dispatch(pc,packet,body);}
 static NativePlayerCommit.Binding targetBinding(ClientSession target){var c=NativeAreaMaterialAction.current.get();if(c!=null&&(int)CombatAccess.field(c,"nativeWeaponBreakTarget")!=0){var receipt=(NativePlayerVitalCompletions.Receipt)CombatAccess.field(c,"nativeWeaponBreakPeerReceipt");if(target==c.pc||receipt!=null&&receipt.pc==target){var binding=c.bindings.get(target);if(binding!=null)return binding;}}return NativeSourceWeaponTargetLife.binding(target,c);}
 static NativePlayerVitalCompletions.Receipt reserveTarget(ClientSession target){var c=NativeAreaMaterialAction.current.get();if(c!=null&&(int)CombatAccess.field(c,"nativeWeaponBreakTarget")!=0){var receipt=(NativePlayerVitalCompletions.Receipt)CombatAccess.field(c,"nativeWeaponBreakPeerReceipt");if(receipt!=null&&receipt.pc==target&&!receipt.transferred&&!receipt.released.get()){CombatAccess.set(c,"nativeWeaponBreakPeerReceipt",null);return NativeSourceWeaponTargetLife.reuse(receipt,target,c);}}return NativeSourceWeaponTargetLife.reserve(target,c);}
 static boolean admit(ClientSession pc,ClientSession target,int skill,int action,int reuse){
  if(!NativeSourceWeaponTargetLife.admit(pc,target,skill))return false;
  var c=NativeAreaMaterialAction.current.get();if(c!=null&&c.pc==pc&&(int)CombatAccess.field(c,"nativeWeaponBreakTarget")==2&&target==null){var npc=c.npcs.get((long)CombatAccess.field(c,"nativeWeaponBreakTargetId"));if(npc==null||!c.npc((long)CombatAccess.field(c,"nativeWeaponBreakTargetId")))return false;return NativeLightAction.admitPair(pc,npc.kind()==3?(ClientSession)npc.owner():pc,skill,action,reuse);}
  return NativeWeaponBreakMaterialAction.nativeTargetOriginalAdmit(pc,target,skill,action,reuse);
 }
 static void run(ClientSession pc,byte[] packet,BattleSpellCatalog.WeaponBreakSpell spell,boolean worldRoot)throws IOException{
  boolean world=worldRoot||String.valueOf(CombatAccess.field(pc,"phase")).equals("WORLD");long id=NativeSourceWeaponTargetLife.runRequest(Protocol.attackSpellRequest(packet),pc).targetObjectId();var hub=(GameHub)CombatAccess.field(pc,"hub");
  var players=world?hub.classicWorldMembers((int)CombatAccess.field(pc,"classicWorldMapId"),NativeStaticNpcScene.instance(pc)):hub.roomCombatants((long)CombatAccess.field(pc,"currentRoomId"));
  if(players.stream().anyMatch(p->p.playerObjectId()==id)||pc.playerObjectId()==id){
   if(world)NativeWeaponBreakMaterialAction.nativeTargetOriginalWorld(pc,packet,spell);else NativeWeaponBreakMaterialAction.nativeTargetOriginalBattle(pc,packet,spell);return;
  }
  var inherited=NativeAreaMaterialAction.current.get();var captured=inherited!=null&&inherited.pc==pc&&(int)CombatAccess.field(inherited,"nativeWeaponBreakTarget")!=0?inherited:new NativeAreaMaterialAction.Context(pc);var npc=captured.npcs.get(id);if(npc==null)return;CombatAccess.set(captured,"nativeWeaponBreakTarget",1);CombatAccess.set(captured,"nativeWeaponBreakTargetId",id);
  var owner=npc.kind()==3?(ClientSession)npc.owner():pc;var monitor=npc.kind()==3?npc.actor():npc.owner();if(monitor==null)return;
  NativeLightAction.commitPair(pc,owner,()->NativeWorldMaterialPolicy.run(pc,true,27,()->{var stats=!world&&npc.kind()==3?NativeSourceWeaponTargetLife.prepare(pc,(BattleCompanion)npc.actor(),captured):null;synchronized(hub){
   if(!NativeSourceWeaponTargetLife.scene(captured)||!captured.npc(id))return;
   synchronized(monitor){if(!NativeSourceWeaponTargetLife.scene(captured)||!captured.npc(id))return;
    NativeAreaMaterialAction.owned(captured,()->{CombatAccess.set(captured,"nativeWeaponBreakTarget",2);try{if(world)pc.nativeMaterialWorldWeaponBreakBody(packet,spell);else if(npc.kind()==3)NativeWeaponBreakBattleCompanion.cast(pc,owner,(BattleCompanion)npc.actor(),spell,stats);else pc.nativeMaterialWeaponBreakBody(packet,spell);}finally{CombatAccess.set(captured,"nativeWeaponBreakTarget",1);}});
   }
  }}));
 }
 static boolean captureOutput(ClientSession receiver,byte[] packet,String note)throws IOException{
  // Preserve the original IOException; the candidate has no Exceptions attribute here.
  if(NativeSourceWeaponFinalFrame.capture(receiver,packet,note))return true;
  var context=NativeAreaMaterialAction.current.get();if(context==null)return NativeAreaMaterialAction.nativeTargetOriginalCaptureOutput(receiver,packet,note);
  int stage=(int)CombatAccess.field(context,"nativeWeaponBreakTarget");if(stage==0)return NativeAreaMaterialAction.nativeTargetOriginalCaptureOutput(receiver,packet,note);if(stage!=2)return false;
  var binding=context.bindings.get(receiver);if(binding==null)return true;byte[] frozen=packet.clone();
  int opcode=frozen[0]&255;boolean objectPacket=opcode==Protocol.S_ACTION||opcode==Protocol.S_EFFECT||opcode==Protocol.S_POISON;
  objectPacket=NativeCompanionNegative.objectPacket(opcode);
  long id=!objectPacket||frozen.length<5?-1:Integer.toUnsignedLong((frozen[1]&255)|(frozen[2]&255)<<8|(frozen[3]&255)<<16|(frozen[4]&255)<<24);boolean npc=context.npcs.containsKey(id);var targets=context.players.stream().filter(p->p.playerObjectId()==id).toList();
  // A single-target cast owns the caster/owner pair. Other viewers acquire their own lock
  // only after that pair and the hub/population have been released.
  NativeLightAction.deferWeaponTargetOutput(()->{var completion=new Runnable[1];NativeSourcePlayerActionLock.run(receiver,()->{
   if(NativeSourceControlledRoleFrame.current(binding,receiver))completion[0]=NativeRolePublication.completion(receiver,()->{
    boolean valid;synchronized(context.hub){if(context.population!=null)synchronized(context.population){valid=NativeSourceControlledRoleFrame.current(binding,receiver)&&context.player(receiver)&&targets.stream().allMatch(p->NativeSourceIndirectPlayerProof.player(context,p))&&(!npc||context.npc(id));}
     else valid=NativeSourceControlledRoleFrame.current(binding,receiver)&&context.player(receiver)&&targets.stream().allMatch(p->NativeSourceIndirectPlayerProof.player(context,p))&&(!npc||context.npc(id));}
    if(valid)try{receiver.nativeSelfOriginalSend(frozen,note);}catch(IOException e){throw new java.io.UncheckedIOException(e);}
   });
  });if(completion[0]!=null)completion[0].run();});return true;
 }
 static void nativeSourceWeaponFirstDispatch(ClientSession pc,byte[] packet,NativeSelfCastAction.Task body,NativeAreaMaterialAction.Context context,Protocol.AttackSpellRequest request)throws IOException{
  String phase=NativeSourceWeaponTargetLife.phase(String.valueOf(CombatAccess.field(pc,"phase")),context);
  if(Protocol.spellSelector(packet).skillId()!=27||!Set.of("WORLD","BATTLE").contains(phase)||pc.selectedCharacterName().isBlank()){NativeSourceWeaponTargetLife.next(pc,packet,body,context);return;}
  long id=NativeSourceWeaponTargetLife.firstRequest(packet,context,request).targetObjectId();var hub=(GameHub)CombatAccess.field(pc,"hub");var players=phase.equals("WORLD")?hub.classicWorldMembers((int)CombatAccess.field(pc,"classicWorldMapId"),NativeStaticNpcScene.instance(pc)):NativeSourceWeaponTargetLife.members(hub,(long)CombatAccess.field(pc,"currentRoomId"),context);
  var selected=players.stream().filter(p->p.playerObjectId()==id).findFirst().orElse(pc.playerObjectId()==id?pc:null);
  var captured=NativeSourceWeaponTargetLife.context(context);var npc=captured.npcs.get(id);if(selected==null&&npc==null)return;CombatAccess.set(captured,"nativeWeaponBreakTarget",1);CombatAccess.set(captured,"nativeWeaponBreakTargetId",id);
  var owner=selected!=null?selected:npc.kind()==3?(ClientSession)npc.owner():pc;var receipt=owner==pc?null:NativeSourceTargetNegativeReserve.reserve(owner,context);if(owner!=pc&&receipt==null)return;CombatAccess.set(captured,"nativeWeaponBreakPeerReceipt",receipt);
  var prior=NativeAreaMaterialAction.current.get();NativeAreaMaterialAction.current.set(captured);
  try{NativeSourceWeaponTargetLife.next(pc,packet,body,context);}finally{if(prior==null)NativeAreaMaterialAction.current.remove();else NativeAreaMaterialAction.current.set(prior);if(receipt!=null&&!receipt.released.get())receipt.close();}
 }
 static NativePlayerVitalCompletions.Receipt nativeSourceMaterialPairTarget(ClientSession target,NativeSelfCastAction.Task body){var c=NativeAreaMaterialAction.current.get();if(c!=null&&(int)CombatAccess.field(c,"nativeWeaponBreakTarget")!=0){var receipt=(NativePlayerVitalCompletions.Receipt)CombatAccess.field(c,"nativeWeaponBreakPeerReceipt");if(receipt!=null&&receipt.pc==target&&!receipt.transferred&&!receipt.released.get()){CombatAccess.set(c,"nativeWeaponBreakPeerReceipt",null);return NativeSourceMaterialPairIngress.reuse(receipt,target,c,body);}}return NativeSourceMaterialPairIngress.targetReserve(target,c,body);}
 private NativeWeaponBreakTargetLifecycle(){}
}
