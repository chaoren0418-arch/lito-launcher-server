package local.lito381.server;
import java.io.*;
import java.lang.ref.WeakReference;
import java.util.*;
/** Arena poison provenance. One weak lease per actual monster; no scheduler or actor registry. */
final class NativeSourceArenaPoison {
 static final String P="local/lito381/server/",FIELD="nativeSourceArenaPoisonLease",STAMP="nativeSourceArenaPoisonProof",OUTPUT="nativeSourceArenaPoisonOutput";
 static final class Lease {
  final WeakReference<ClientSession> source;final WeakReference<GameHub> hub;
  final NativePlayerCommit.Binding binding;final Object sourceLife,actorLife,generation,encounter;final boolean quest;
  Lease(ClientSession pc,GameHub hub,Object encounter,Object actor,boolean quest){source=new WeakReference<>(pc);this.hub=new WeakReference<>(hub);var c=NativeAreaMaterialAction.current.get();binding=c.caster;var raw=NativeSourceRawDamage.current();var controlled=CombatAccess.field(c,NativeSourceControlledDisplay.FIELD);sourceLife=controlled instanceof NativeSourceControlledDisplay.Action action&&action.c.pc==pc?action.caster.life():raw!=null&&raw.c.pc==pc?raw.root.life:null;actorLife=NativeSourceMonsterHp.life(actor);generation=NativeProcHealingEncounter.generation(encounter);this.encounter=encounter;this.quest=quest;}
  ClientSession caster(){var pc=source.get();var h=hub.get();return pc!=null&&h!=null&&CombatAccess.field(pc,"hub")==h&&NativePlayerLifetime.of(h).mode!=NativePlayerLifetime.Mode.STOPPED&&!(boolean)CombatAccess.field(h,"nativeStaticMotionClosed")&&NativeAcceptedFreezeMutation.roleFrame(binding,pc)&&!(boolean)CombatAccess.field(pc,"sourceItemDisconnected")&&!(boolean)CombatAccess.field(pc,"observer")&&NativeSourceControlledDisplay.rawLife(pc)==sourceLife?pc:null;}
  boolean current(Object actor){return caster()!=null&&CombatAccess.field(encounter,"sourceProcEncounterGeneration")==generation&&rawLife(actor)==actorLife;}
 }
 record Viewer(WeakReference<ClientSession> pc,NativePlayerCommit.Binding binding){}
 record Proof(Lease lease,Object actor,Object acceptedLife,List<Viewer> viewers){
  ClientSession caster(){return lease!=null&&lease.caster()!=null&&CombatAccess.field(actor,FIELD)==lease&&CombatAccess.field(lease.encounter,"sourceProcEncounterGeneration")==lease.generation&&rawLife(actor)==acceptedLife?lease.caster():null;}
 }
 static Object rawLife(Object actor){var state=CombatAccess.field(CombatAccess.field(actor,"damagePoison"),"sourcePoisonLifecycle");return state==null?null:CombatAccess.field(state,"sourceNpcOwner");}
 static Object actor(Object encounter,long id,boolean quest){
  if(quest){for(var lane:(Object[])CombatAccess.field(encounter,"lanes"))for(var actor:(List<?>)CombatAccess.field(lane,"monsters"))if((long)CombatAccess.field(actor,"objectId")==id)return actor;}
  else for(var actor:(List<?>)CombatAccess.field(encounter,"actors"))if((long)CombatAccess.field(actor,"objectId")==id)return actor;
  return null;
 }
 static boolean selected(ClientSession pc,Object encounter,Object actor,long id){var c=NativeAreaMaterialAction.current.get();if(c==null||c.pc!=pc||!NativeAcceptedFreezeMutation.roleFrame(c.caster,pc))return false;var raw=NativeSourceRawDamage.current();if(raw!=null){var frozen=raw.npcs.get(id);if(frozen==null||!frozen.current())return false;}var n=c.npcs.get(id);return n!=null&&(n.kind()==1||n.kind()==2)&&n.owner()==encounter&&n.actor()==actor&&n.epoch()==rawLife(actor);}
 static void battle(ClientSession pc,byte[] payload,BattleSpellCatalog.PoisonSpell spell,CombatPoisonLifecycle.Scope scope)throws IOException{
  if(!CombatRules.loaded()||!String.valueOf(CombatAccess.field(pc,"phase")).equals("BATTLE")||NativeSourceRawDamage.current()!=null){NativePoisonMaterialAction.nativeSourceArenaOriginalBattle(pc,payload,spell,scope);return;}
  var request=Protocol.attackSpellRequest(payload);if(request==null){NativePoisonMaterialAction.nativeSourceArenaOriginalBattle(pc,payload,spell,scope);return;}
  var action=new NativeSourceRawDamage.Action(pc,request.targetObjectId(),true,null);var target=action.npcs.get(request.targetObjectId());
  if(target==null||target.npc.kind()!=1&&target.npc.kind()!=2){action.close();NativePoisonMaterialAction.nativeSourceArenaOriginalBattle(pc,payload,spell,scope);return;}
  NativeSourceRawDamage.owned(action,()->NativePoisonMaterialAction.nativeSourceArenaOriginalBattle(pc,payload,spell,scope));
 }
 static boolean quest(BattleQuestEncounter encounter,long id,int force,long attacker,long now,int duration,int interval,ClientSession pc,GameHub hub){
  synchronized(encounter){var actor=actor(encounter,id,true);if(CombatRules.loaded()&&(actor==null||!selected(pc,encounter,actor,id)))return false;var lease=CombatRules.loaded()?new Lease(pc,hub,encounter,actor,true):null;if(lease!=null&&(lease.sourceLife==null||!lease.current(actor)||attacker!=lease.binding.object()))return false;boolean accepted=encounter.poison(id,force,attacker,now,duration,interval);if(accepted&&lease!=null)CombatAccess.set(actor,FIELD,lease);return accepted;}
 }
 static boolean time(BattleTimeAttackEncounter encounter,long id,long attacker,long now,int duration,int interval,ClientSession pc,GameHub hub){
  synchronized(encounter){var actor=actor(encounter,id,false);if(CombatRules.loaded()&&(actor==null||!selected(pc,encounter,actor,id)))return false;var lease=CombatRules.loaded()?new Lease(pc,hub,encounter,actor,false):null;if(lease!=null&&(lease.sourceLife==null||!lease.current(actor)||attacker!=lease.binding.object()))return false;boolean accepted=encounter.poison(id,attacker,now,duration,interval);if(accepted&&lease!=null)CombatAccess.set(actor,FIELD,lease);return accepted;}
 }
 static boolean before(Object encounter,Object actor,boolean poison){if(!poison||!CombatRules.loaded())return true;return CombatAccess.field(actor,FIELD)instanceof Lease lease&&lease.encounter==encounter&&lease.current(actor);}
 static BattleQuestEncounter.Hit questDamage(BattleQuestEncounter encounter,Object lane,Object actor,int damage,boolean poison){return before(encounter,actor,poison)?(BattleQuestEncounter.Hit)CombatAccess.call(encounter,"nativeSourcePoisonOriginalApplyDamage","(L"+P+"BattleQuestEncounter$Lane;L"+P+"BattleQuestEncounter$Monster;IZ)L"+P+"BattleQuestEncounter$Hit;",lane,actor,damage,poison):null;}
 static BattleTimeAttackEncounter.Hit timeDamage(BattleTimeAttackEncounter encounter,Object actor,int damage,boolean poison){return before(encounter,actor,poison)?(BattleTimeAttackEncounter.Hit)CombatAccess.call(encounter,"nativeSourcePoisonOriginalApplyDamage","(L"+P+"BattleTimeAttackEncounter$Monster;IZ)L"+P+"BattleTimeAttackEncounter$Hit;",actor,damage,poison):null;}
 static void stamp(Object update,Object actor){if(CombatRules.loaded()&&CombatAccess.field(actor,FIELD)instanceof Lease lease){var viewers=new ArrayList<Viewer>();var hub=lease.hub.get();if(hub!=null)for(var pc:hub.roomMembers(lease.binding.room()))viewers.add(new Viewer(new WeakReference<>(pc),NativePlayerCommit.Binding.capture(pc)));CombatAccess.set(update,STAMP,new Proof(lease,actor,rawLife(actor),List.copyOf(viewers)));}}
 /** Never recover authority from a newly selected role with the same numerical object id. */
 static ClientSession attacker(Object update,ClientSession original){if(!CombatRules.loaded())return original;return CombatAccess.field(update,STAMP)instanceof Proof proof?proof.caster():null;}
 record Output(Proof proof,ClientSession receiver,NativePlayerCommit.Binding binding){}
 static boolean current(Output out){return out.proof.caster()!=null&&NativeAcceptedFreezeMutation.roleFrame(out.binding,out.receiver)&&!(boolean)CombatAccess.field(out.receiver,"sourceItemDisconnected");}
 static void inherit(NativeAreaMaterialAction.Context child,NativeAreaMaterialAction.Context parent){if(parent!=null)CombatAccess.set(child,OUTPUT,CombatAccess.field(parent,OUTPUT));}
 static void inherit(NativeAreaMaterialAction.Context child){inherit(child,NativeAreaMaterialAction.current.get());}
 static boolean allowFrame(ClientSession pc,byte[] packet){var c=NativeAreaMaterialAction.current.get();var value=c==null?null:CombatAccess.field(c,OUTPUT);return !(value instanceof Output out)||out.receiver==pc&&current(out);}
 static void send(ClientSession pc,byte[] packet,String note,Object update)throws IOException{
  if(!CombatRules.loaded()){CombatAccess.call(pc,"sendEncrypted","([BLjava/lang/String;)V",packet,note);return;}
  if(!(CombatAccess.field(update,STAMP)instanceof Proof proof)||proof.caster()==null)return;
  // Source HPBAR is published once to the actual attacker through its existing bounded cache.
  if(packet.length>0&&(packet[0]&255)==Protocol.S_HIT_RATIO)return;
  NativePlayerCommit.Binding binding=null;for(var viewer:proof.viewers)if(viewer.pc.get()==pc){binding=viewer.binding;break;}if(binding==null)return;
  var previous=NativeAreaMaterialAction.current.get();var c=previous!=null?previous:new NativeAreaMaterialAction.Context(pc);var out=new Output(proof,pc,binding);var frozen=packet.clone();
  NativeLightAction.deferScoped(()->{var prior=CombatAccess.field(c,OUTPUT);try{CombatAccess.set(c,OUTPUT,out);synchronized(pc){if(current(out))NativeAreaMaterialAction.owned(c,()->pc.nativeSelfOriginalSend(frozen,note));}}finally{CombatAccess.set(c,OUTPUT,prior);}});
 }
 static void hpbar(Object update){
  if(CombatRules.loaded()&&CombatAccess.field(update,STAMP)instanceof Proof proof)NativeLightAction.deferScoped(()->hpbar(proof));
 }
 static void hpbar(Proof proof){
  var pc=proof.caster();if(pc==null)return;
  try{var c=new NativeAreaMaterialAction.Context(pc);var lease=proof.lease;var actor=proof.actor;long id=(long)CombatAccess.field(actor,"objectId");var npc=new NativeAreaMaterialAction.Npc(lease.quest?1:2,id,lease.encounter,actor,lease.actorLife);
  var aim=new NativeSourceMonsterHp.Aim(c,npc,(NativeSourceMonsterHp.Cache)CombatAccess.field(actor,NativeSourceMonsterHp.CACHE),new NativeSourceDamageDisplay.Actor(pc,lease.binding,lease.sourceLife,NativeSourceRawDamage.number(pc,"positionX"),NativeSourceRawDamage.number(pc,"positionY")),lease.generation,NativeSourceRawDamage.number(actor,"x"),NativeSourceRawDamage.number(actor,"y"));
  NativeSourceMonsterHp.publish(aim);}catch(RuntimeException failure){NativeSourceNpcDamageDisplay.failed(pc);}
 }
 static void reward(ClientSession pc,int npc,String note,Object update){if(!CombatRules.loaded()){CombatAccess.call(pc,"applyNpcLawfulReward","(ILjava/lang/String;)V",npc,note);return;}if(!(CombatAccess.field(update,STAMP)instanceof Proof proof))return;NativeSourcePlayerActionLock.runPairOrdered(pc,pc,()->{if(proof.caster()==pc)CombatAccess.call(pc,"applyNpcLawfulReward","(ILjava/lang/String;)V",npc,note);});}
 private NativeSourceArenaPoison(){}
}
