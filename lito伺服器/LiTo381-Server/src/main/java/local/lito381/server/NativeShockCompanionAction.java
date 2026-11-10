package local.lito381.server;
import java.io.*;import java.util.function.IntUnaryOperator;
/** Actual BATTLE controlled actor is frozen before the ordered owner pair wait. */
final class NativeShockCompanionAction {
 static final String T="(J)Llocal/lito381/server/ClientSession$MassSlowCompanionTarget;";
 record Death(Object life,Object bag,int x,int y,boolean active){}
 static boolean nativeSourceControlledOriginalShock(ClientSession pc,byte[] packet,BattleSpellCatalog.ShockStunSpell spell,IntUnaryOperator roll){
  if(!CombatRules.loaded()||spell.skillId()!=87)return false;var request=Protocol.attackSpellRequest(packet);if(request.skillId()!=87)return false;
  var c=new NativeAreaMaterialAction.Context(pc);if(!c.phase.equals("BATTLE"))return false;long id=request.targetObjectId();var n=c.npcs.get(id);if(n==null||n.kind()!=3)return false;
  var target=CombatAccess.call(pc,"resolveBattleSpellCompanion",T,id);if(target==null)return true;var owner=(ClientSession)n.owner();var pet=(BattleCompanion)n.actor();if(CombatAccess.get(target,"owner")!=owner||CombatAccess.get(target,"controlled")!=pet)return true;
  var receipt=owner==pc?null:NativePlayerVitalCompletions.reserve(owner);if(owner!=pc&&receipt==null)return true;
  CombatAccess.set(c,NativeCompanionNegative.STAGE,1);CombatAccess.set(c,NativeCompanionNegative.ID,id);CombatAccess.set(c,NativeCompanionNegative.RECEIPT,receipt);
  try{NativeAreaMaterialAction.owned(c,()->NativeLightAction.commitPair(pc,owner,()->{
   if(!valid(c))return;NativeWorldMaterialPolicy.run(pc,true,87,()->{synchronized(c.hub){synchronized(pet){var timed=(BattleTimedStatus)CombatAccess.field(pet,"timedStatus");synchronized(timed){synchronized(NativePlayerLifetime.of(c.hub).admission){
    if(!valid(c))return;CombatAccess.set(c,NativeCompanionNegative.STAGE,2);try{CombatCompanionSpells.nativeShockCompanionBody(pc,packet,spell,roll,target);}finally{finish(c);CombatAccess.set(c,NativeCompanionNegative.STAGE,1);}
   }}}}});
  }));}catch(IOException failure){throw new UncheckedIOException(failure);}finally{if(receipt!=null&&!receipt.released.get())receipt.close();}return true;
 }
 static boolean valid(NativeAreaMaterialAction.Context c){return c!=null&&c.scene()&&c.npc((long)CombatAccess.field(c,NativeCompanionNegative.ID));}
 static boolean available(ClientSession pc,Object target){var c=NativeAreaMaterialAction.current.get();if(c==null||c.pc!=pc||!valid(c))return false;var n=c.npcs.get((long)CombatAccess.field(c,NativeCompanionNegative.ID));return n!=null&&n.kind()==3&&n.actor()==CombatAccess.get(target,"controlled")&&n.owner()==CombatAccess.get(target,"owner")&&pc.nativePlayerActionLock().isHeldByCurrentThread()&&((ClientSession)n.owner()).nativePlayerActionLock().isHeldByCurrentThread()&&CombatCompanionSpells.available(pc,target);}
 static boolean admit(ClientSession pc,Object target,BattleSpellCatalog.ShockStunSpell spell){return available(pc,target)&&NativeLightAction.admitPair(pc,(ClientSession)CombatAccess.get(target,"owner"),87,BattleActionCodes.attackForWalk(pc.battleWalkAction()),spell.reuseDelayMillis());}
 static void finish(NativeAreaMaterialAction.Context c){long id=(long)CombatAccess.field(c,NativeCompanionNegative.ID);var n=c.npcs.get(id);var pet=(BattleCompanion)n.actor();var snap=pet.snapshot();if(snap.hp()!=0)return;c.npcs.put(id,new NativeAreaMaterialAction.Npc(n.kind(),n.id(),n.owner(),pet,new Death(NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(pet,"timedStatus"),false),CombatAccess.field(pet,"carriedItems"),snap.x(),snap.y(),snap.active())));}
 static boolean publication(NativeAreaMaterialAction.Context c){
  if(!c.scene())return false;long id=(long)CombatAccess.field(c,NativeCompanionNegative.ID);var n=c.npcs.get(id);if(n==null||n.kind()!=3)return false;if(!(n.epoch()instanceof Death proof))return c.npc(id);var owner=(ClientSession)n.owner();var pet=(BattleCompanion)n.actor();var binding=c.bindings.get(owner);var current=CombatAccess.call(owner,"ownedCompanion","(J)Llocal/lito381/server/BattleCompanion;",id);var snap=pet.snapshot();return binding!=null&&binding.matches(owner)&&!(boolean)CombatAccess.field(owner,"sourceItemDisconnected")&&String.valueOf(CombatAccess.field(owner,"phase")).equals("BATTLE")&&(long)CombatAccess.field(owner,"currentRoomId")==c.room&&c.hub.roomCombatants(c.room).contains(owner)&&(current==pet||current==null&&snap.kind()==BattleCompanion.Kind.SUMMON)&&snap.objectId()==id&&snap.hp()==0&&snap.active()==proof.active()&&snap.x()==proof.x()&&snap.y()==proof.y()&&CombatAccess.field(pet,"carriedItems")==proof.bag()&&NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(pet,"timedStatus"),false)==proof.life();
 }
 static boolean captureOutput(ClientSession receiver,byte[] packet,String note) throws IOException{
  var c=NativeAreaMaterialAction.current.get();if(c==null||(int)CombatAccess.field(c,NativeCompanionNegative.STAGE)!=2)return NativeAreaMaterialAction.nativeShockOriginalCaptureOutput(receiver,packet,note);var material=NativeLightAction.currentMaterial(c.pc);if(material==null||material.skill!=87)return NativeAreaMaterialAction.nativeShockOriginalCaptureOutput(receiver,packet,note);var binding=c.bindings.get(receiver);if(binding==null)return true;byte[] frozen=packet.clone();
  NativeLightAction.deferWeaponTargetOutput(()->{var completion=new Runnable[1];NativeSourcePlayerActionLock.run(receiver,()->{if(binding.matches(receiver))completion[0]=NativeRolePublication.completion(receiver,()->{boolean allowed;synchronized(c.hub){allowed=binding.matches(receiver)&&c.player(receiver)&&publication(c);}if(allowed)try{receiver.nativeSelfOriginalSend(frozen,note);}catch(IOException failure){throw new UncheckedIOException(failure);}});});if(completion[0]!=null)completion[0].run();});return true;
 }
 private NativeShockCompanionAction(){}

 static boolean shock(ClientSession pc,byte[] packet,BattleSpellCatalog.ShockStunSpell spell,java.util.function.IntUnaryOperator roll){return NativeSourceControlledDisplay.shock(pc,packet,spell,roll);}

}
