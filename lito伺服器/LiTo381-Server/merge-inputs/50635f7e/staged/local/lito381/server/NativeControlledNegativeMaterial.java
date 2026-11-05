// DECOMPILED from candidate-50635f7e.jar, TODO review
package local.lito381.server;
import java.io.*;import java.util.*;
/** Exact canonical controlled target before pair wait; no physical projection or parallel actor. */
final class NativeControlledNegativeMaterial {
 static void cast(ClientSession pc,byte[] packet,NativeSelfCastAction.Task body)throws IOException{
  int skill=Protocol.spellSelector(packet).skillId();if(skill!=173&&skill!=174){NativeTargetNegativeMaterialAction.nativeControlledOriginalCast(pc,packet,body);return;}
  var c=new NativeAreaMaterialAction.Context(pc);
  // The candidate catch covers every post-context operation, including normal close calls.
  try {
   if(!c.phase.equals("WORLD")&&!c.phase.equals("BATTLE")){NativeSourceTargetNegativeLife.close(c);return;}
   long id=NativeSourceTargetNegativeLife.request(Protocol.attackSpellRequest(packet),c).targetObjectId();
   boolean captured=NativeSourceTargetNegativeLife.capture(c,id);
   NativeSourceControlledNegativeFinal.install(c);
   if(!captured){NativeSourceTargetNegativeLife.close(c);return;}
   var player=c.players.stream().filter(p->c.bindings.get(p).object()==id&&c.player(p)).findFirst().orElse(null);var n=c.npcs.get(id);
   if(player==null&&n==null){NativeSourceTargetNegativeLife.close(c);return;}
   var owner=player!=null?player:n.kind()==3?(ClientSession)n.owner():pc;
   var receipt=owner==pc?null:NativeSourceTargetNegativeReserve.reserve(owner,c);
   if(owner!=pc&&receipt==null){NativeSourceTargetNegativeLife.close(c);return;}
   CombatAccess.set(c,NativeCompanionNegative.STAGE,1);CombatAccess.set(c,NativeCompanionNegative.ID,id);CombatAccess.set(c,NativeCompanionNegative.RECEIPT,receipt);
   try{NativeAreaMaterialAction.owned(c,()->NativeLightAction.commitPair(pc,owner,()->{if(!valid(c))return;body.run();}));}finally{if(receipt!=null&&!receipt.released.get())receipt.close();}
   NativeSourceTargetNegativeLife.close(c);
   return;
  } catch(Throwable failure) {
   NativeSourceTargetNegativeLife.close(c);
   throw failure;
  }
 }
 static boolean valid(NativeAreaMaterialAction.Context c){if(!NativeSourceTargetNegativeLife.current(c))return false;if(c==null||!c.scene())return false;long id=(long)CombatAccess.field(c,NativeCompanionNegative.ID);var n=c.npcs.get(id);return n!=null?c.npc(id):c.players.stream().anyMatch(p->c.bindings.get(p).object()==id&&c.player(p));}
 static boolean negative(ClientSession pc,byte[] packet,BattleSpellCatalog.TargetNegativeStatusSpell spell,int fixture){
  if(!CombatRules.loaded()||spell.skillId()!=173&&spell.skillId()!=174)return false;var request=Protocol.attackSpellRequest(packet);if(request.skillId()!=spell.skillId())return false;var c=NativeAreaMaterialAction.current.get();long id=request.targetObjectId();
  if(c==null||c.pc!=pc||(long)CombatAccess.field(c,NativeCompanionNegative.ID)!=id){boolean[] result={false};try{cast(pc,packet,()->result[0]=negative(pc,packet,spell,fixture));}catch(IOException failure){throw new UncheckedIOException(failure);}return result[0];}
  var n=c.npcs.get(id);if(n==null||n.kind()!=3)return false;if(!valid(c)||id!=(long)CombatAccess.field(c,NativeCompanionNegative.ID))return true;var owner=(ClientSession)n.owner();var pet=(BattleCompanion)n.actor();SourceNpcPhysicalStats stats;
  try{stats=((AccountRepository)CombatAccess.call(pc,"selectedSourceAccounts","()Llocal/lito381/server/AccountRepository;")).npcPhysicalStats(pet.snapshot().npcId());}catch(Exception failure){throw new IllegalStateException("controlled negative Source lookup failed",failure);}if(stats==null)return true;
  boolean administrator=((int)CombatAccess.field(pc,"elfAttribute")&spell.attribute())==0&&(boolean)CombatAccess.call(pc,"isAdministrator","()Z");
  boolean[] result={true};try{NativeWorldMaterialPolicy.run(pc,true,spell.skillId(),()->{var proof=NativeSourceTargetNegativeLife.proof(c);if(!NativeSourceTargetNegativeLife.ready(c,proof,pet,owner))return;synchronized(c.hub){synchronized(pet){var timed=(BattleTimedStatus)NativeSourceTargetNegativeLife.firstTimed(CombatAccess.field(pet,"timedStatus"),proof);synchronized(timed){synchronized(NativeSourceTargetNegativeLife.lifetime(c.hub,proof).admission){if(!valid(c))return;var focus=new CombatNpcDebuffs.Focus(pet,owner,pet,pet.snapshot(),stats,c.phase.equals("WORLD"));CombatAccess.set(c,NativeCompanionNegative.STAGE,2);try{result[0]=NativeSourceControlledNegativeFinal.produce(pc,packet,spell,fixture,focus,administrator);}finally{CombatAccess.set(c,NativeCompanionNegative.STAGE,1);}}}}}});}catch(IOException failure){throw new UncheckedIOException(failure);}return result[0];
 }
 static boolean available(ClientSession pc,CombatNpcDebuffs.Focus focus){var c=NativeAreaMaterialAction.current.get();if(c==null||c.pc!=pc||!valid(c)||focus.handle()!=focus.controlled())return false;var n=c.npcs.get((long)CombatAccess.field(c,NativeCompanionNegative.ID));return n!=null&&n.kind()==3&&n.actor()==focus.controlled()&&n.owner()==focus.owner()&&pc.nativePlayerActionLock().isHeldByCurrentThread()&&focus.owner().nativePlayerActionLock().isHeldByCurrentThread();}
 static boolean admit(ClientSession pc,CombatNpcDebuffs.Focus focus,BattleSpellCatalog.TargetNegativeStatusSpell spell){return available(pc,focus)&&NativeLightAction.admitPair(pc,focus.owner(),spell.skillId(),spell.actionId(),spell.reuseDelayMillis());}
 private NativeControlledNegativeMaterial(){}
}
