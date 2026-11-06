package local.lito381.server;
import java.io.*;import java.util.*;
/** Typed doll HP consumers: skill accepted-hit presentation versus pre-HP extra-proc presentation. */
final class NativeSourceDollCommit {
 static final String P="local/lito381/server/",PC="L"+P+"ClientSession;";
 /** Pre-HP presentation proof is deliberately distinct from an accepted NPC HP receipt. */
 static final class NpcProof {
  final Object actor,encounter,generation;final int x,y;volatile Object life;
  NpcProof(NativeSourceMonsterHp.Aim aim){actor=aim.npc().actor();encounter=aim.npc().owner();generation=aim.generation();life=aim.npc().epoch();x=aim.x();y=aim.y();}
  boolean current(){var state=CombatAccess.field(CombatAccess.field(actor,"damagePoison"),"sourcePoisonLifecycle");return state!=null&&CombatAccess.field(state,"sourceNpcOwner")==life&&CombatAccess.field(encounter,"sourceProcEncounterGeneration")==generation&&(int)CombatAccess.field(actor,"x")==x&&(int)CombatAccess.field(actor,"y")==y;}
 }
 record PreVisual(NativeSourceDamageDisplay.Plan plan){}
 static boolean valid(Object proof){return !(proof instanceof NpcProof npc)||npc.current();}
 static void accepted(NativePlayerCommit.Frame frame){var plan=frame.event.sourceDisplay();if(plan!=null&&frame.accepted&&frame.fatal&&plan.nativeSourceDollProof instanceof PreVisual pre&&pre.plan.target!=null&&pre.plan.target.pc()==frame.pc)pre.plan.targetLife=plan.targetLife;}
 static boolean enabled(ClientSession pc){return CombatRules.loaded()&&String.valueOf(CombatAccess.field(pc,"phase")).equals("BATTLE");}
 static boolean skill(String reason){return reason!=null&&reason.startsWith("skill-");}
 static void commit(ClientSession caster,int damage,long id,ClientSession target,BattleQuestEncounter.Snapshot quest,BattleTimeAttackEncounter.Snapshot time,String reason)throws IOException{
  if(!enabled(caster)){CombatAccess.call(caster,"commitNativeMagicDollProcDamage","(IJ"+PC+"L"+P+"BattleQuestEncounter$Snapshot;L"+P+"BattleTimeAttackEncounter$Snapshot;Ljava/lang/String;)V",damage,id,target,quest,time,reason);return;}
  if(damage<=0)return;if(!skill(reason)&&!"active-damage".equals(reason))throw new IllegalArgumentException("unreviewed doll proc kind");
  if(target!=null){if(target.playerObjectId()!=id)return;player(target,damage,caster,skill(reason));return;}
  if(quest!=null&&quest.objectId()==id)npc(caster,id,damage,reason,true,skill(reason));
  else if(time!=null&&time.objectId()==id)npc(caster,id,damage,reason,false,skill(reason));
 }

 static NativeSourceDamageDisplay.Plan playerPlan(ClientSession caster,ClientSession target,int damage){
  var captured=NativeSourceDamageDisplay.capture(caster,target,damage,false,false,false);
  var plan=captured!=null?captured:new NativeSourceDamageDisplay.Plan(NativeSourceDamageDisplay.Actor.capture(caster),NativeSourceDamageDisplay.Actor.capture(target),List.of());
  NativeSourceDamageDisplay.Actor victimActor,sourceActor;
  var victim=NativeSourceAcceptedPlayers.fromActor(target,plan.target.binding(),victimActor=plan.target,victimActor.life());var source=NativeSourceAcceptedPlayers.fromActor(caster,plan.caster.binding(),sourceActor=plan.caster,sourceActor.life());
  plan.nativeRawGuard=new NativeSourceRawDamage.Guard(victim,source,true,plan.owner);return plan;
 }
 static void player(ClientSession target,int damage,ClientSession caster,boolean skill)throws IOException{
  var visual=playerPlan(caster,target,damage);var guard=visual.nativeRawGuard;
  try{NativeSourcePairAdmission.doll(caster,target,()->{synchronized(visual.owner.admission){if(!guard.before())return;try{NativeLightAction.commitPair(caster,target,()->{
   if(!guard.before())return;
   if(skill){NativeSourceDamageDisplay.damage(target,damage,caster,true,false,0,visual);return;}
   // UnifiedProcExecutor.applyProcDamage shows magic figures before receiveDamage(false,false).
   NativeSourceDamageDisplay.publish(visual);var hp=new NativeSourceDamageDisplay.Plan(visual.caster,visual.target,List.of());hp.nativeRawGuard=guard;hp.nativeSourceDollProof=new PreVisual(visual);NativeSourceDamageDisplay.damage(target,damage,caster,false,false,0,hp);
  });}catch(IOException failure){throw new UncheckedIOException(failure);}}});}catch(UncheckedIOException failure){throw failure.getCause();}
 }
 static void npc(ClientSession caster,long id,int damage,String reason,boolean quest,boolean skill)throws IOException{
  var aim=NativeSourceMonsterHp.capture(caster,id,quest?1:2);if(aim==null)return;
  if(skill){NativeSourceNpcDamageDisplay.run(caster,id,damage,"magic-doll:"+reason,quest,2,false,aim);return;}
  var settings=NativeSourceOtherControls.settings();var effects=NativeSourceDisplayPlan.magic(settings,caster.playerObjectId(),id,NativeSourceDamageDisplay.shown(caster),NativeSourceDamageDisplay.n(caster,"playerHp")<=0,damage);
  var proof=new NpcProof(aim);var visual=new NativeSourceDamageDisplay.Plan(aim.caster(),null,effects);visual.nativeSourceDollProof=proof;
  var receipt=new NativeSourceNpcDamageDisplay.Receipt(aim,caster,damage,0,false);if(!receipt.before())return;
  NativeSourceDamageDisplay.publish(visual);
  try{CombatAccess.call(caster,quest?"nativeSourceHpOriginalQuestDamage":"nativeSourceHpOriginalTimeDamage","(JILjava/lang/String;L"+P+"NativeSourceNpcDamageDisplay$Receipt;)I",id,damage,"magic-doll:"+reason,receipt);}
  finally{if(receipt.accepted){proof.life=receipt.life;if(receipt.current())try{NativeSourceMonsterHp.publish(aim);}catch(RuntimeException failure){NativeSourceNpcDamageDisplay.failed(caster);}}}
 }
 private NativeSourceDollCommit(){}
}
