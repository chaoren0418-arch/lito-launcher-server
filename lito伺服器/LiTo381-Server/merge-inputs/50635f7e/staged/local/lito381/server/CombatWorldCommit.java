package local.lito381.server;

import java.util.*;

/** WORLD physical commits freeze values after action and join the NPC target lane. */
final class CombatWorldCommit {
 private static final String P="local/lito381/server/";
 sealed interface Plan permits Physical,Doll,CombatWorldMagic.Spell,CombatWorldMultiMagic.Multi,CombatWorldFreeze.Frozen,NativeCombatAttacker.Reflection,NativeProcHealingWorld.Plan,NativeProcHealingCompanion.Plan,NativeProcHealingEncounter.Plan,NativeSourceCubeNpcQueue.Ticket,NativeSourcePoisonNpc.Ticket,NativeSourcePhysicalNpc.Ticket {}
 record Physical(ClassicWorldMonsterPopulation.Snapshot target,SourceNpcPhysicalStats stats,NativeMagicDollEffects.AttackProc dolls,boolean weapon,boolean hit) implements Plan {}
 record Doll(String source) implements Plan {}
 static final class Attempt {
  final CombatMonsterCurseLifecycle.Actor actor;final CombatMonsterCurseLifecycle.Recipient owner;final long epoch;final int damage;final boolean ranged;
  boolean weapon,hit;
  Attempt(CombatMonsterCurseLifecycle.Actor actor,CombatMonsterCurseLifecycle.Recipient owner,long epoch,int damage,boolean ranged){this.actor=actor;this.owner=owner;this.epoch=epoch;this.damage=damage;this.ranged=ranged;}
 }
 private static final ThreadLocal<CombatMonsterCurseLifecycle.Context> effects=new ThreadLocal<>();
 static Attempt prepare(int damage,boolean captured,ClientSession pc,ClassicWorldMonsterPopulation.Snapshot target,int weapon){
  var hub=(GameHub)CombatAccess.field(pc,"hub");synchronized(hub){var a=CombatRules.loaded()?CombatMonsterCurseLifecycle.actor(hub,(int)number(pc,"classicWorldMapId"),(int)number(pc,"classicWorldInstanceId"),target.objectId()):null;
   if(a==null)return new Attempt(null,null,0,CombatRules.strikerAfterClass(damage,captured),false);
   synchronized(a.population()){synchronized(a.timed()){CombatPoisonLifecycle.monsterDamage(a.timed(),true);return new Attempt(a,CombatMonsterCurseLifecycle.Recipient.take(pc),CombatPoisonLifecycle.monsterEpoch(a.timed(),false),damage,BattleStrikerGale.playerWeaponEligible(weapon));}}
  }
 }
 static int shown(Attempt attempt){return attempt.damage;}
 static void venom(ClientSession pc,ClientSession player,BattleQuestEncounter.Snapshot quest,BattleTimeAttackEncounter.Snapshot time,ClassicWorldMonsterPopulation.Snapshot target,boolean weapon,boolean hit,long id,Attempt attempt){
  if(attempt!=null&&attempt.actor!=null){attempt.weapon=weapon;attempt.hit=hit;return;}
  CombatAccess.call(pc,"attemptEnchantVenomPoison","(L"+P+"ClientSession;L"+P+"BattleQuestEncounter$Snapshot;L"+P+"BattleTimeAttackEncounter$Snapshot;L"+P+"ClassicWorldMonsterPopulation$Snapshot;ZZJ)V",player,quest,time,target,weapon,hit,id);
 }
 static boolean enqueue(Attempt attempt,ClientSession pc,ClassicWorldMonsterPopulation.Snapshot target,SourceNpcPhysicalStats stats,NativeMagicDollEffects.AttackProc dolls){
  if(attempt==null||attempt.actor==null)return false;var a=attempt.actor;
  synchronized(a.hub()){synchronized(a.population()){synchronized(a.timed()){
   if(!a.valid(attempt.epoch)||!attempt.owner.valid(pc,a.map(),a.instance()))return true;
   int damage=BattleStrikerGale.damageAfterStatus(attempt.damage,a.population().strikerGaleActive(a.map(),a.id(),System.nanoTime()),attempt.ranged);
   CombatMonsterCurseLifecycle.enqueue(a,attempt.owner,attempt.epoch,damage,new Physical(target,stats,dolls,attempt.weapon,attempt.hit));return true;
  }}}
 }
 static boolean enqueueDoll(ClientSession pc,long id,int damage,String source){
  if(!CombatRules.loaded())return false;if(damage<=0||!String.valueOf(CombatAccess.field(pc,"phase")).equals("WORLD"))return true;var parent=effects.get();var owner=CombatMonsterCurseLifecycle.Recipient.take(pc);var hub=(GameHub)CombatAccess.field(pc,"hub");int map=(int)number(pc,"classicWorldMapId"),instance=(int)number(pc,"classicWorldInstanceId");
  if(parent!=null){if(!parent.valid())return true;owner=new CombatMonsterCurseLifecycle.Recipient(parent.event.owner(),parent.event.source(),parent.event.account(),parent.event.name());map=parent.actor.map();instance=parent.actor.instance();if(!owner.valid(pc,map,instance))return true;}
  synchronized(hub){var a=CombatMonsterCurseLifecycle.actor(hub,map,instance,id);if(a==null)return parent!=null;synchronized(a.population()){synchronized(a.timed()){int hidden=(int)number(a.actor(),"hiddenStatus");if(hidden==1||hidden==2)return true;CombatMonsterCurseLifecycle.enqueue(a,owner,CombatPoisonLifecycle.monsterEpoch(a.timed(),false),damage,new Doll(source));return true;}}}
 }
 static int input(int damage,Object actor){return damage<0&&CombatMonsterCurseLifecycle.acceptedTarget(actor)?1:damage;}
 static int delta(int current,int damage,Object actor){if(damage>=0)return Math.min(current,damage);int max=((ClassicWorldMonsterCatalog.SpawnDefinition)CombatAccess.field(actor,"definition")).template().hp();long after=Math.min(max,(long)current-damage);return after>current?(int)(current-after):0;}
 static boolean sleep(BattleTimedStatus timed,int damage){return damage>0&&timed.cancelSleep();}
 static ClassicWorldMonsterPopulation.Contribution hate(ClassicWorldMonsterPopulation.Contribution source,int damage){return damage<0?null:source;}
 static int applied(int delta){return Math.max(0,delta);}
 private static long number(Object o,String name){return ((Number)CombatAccess.field(o,name)).longValue();}
 private static CombatMonsterCurseLifecycle.Context context(ClassicWorldMonsterPopulation.PoisonUpdate update,CombatPoisonLifecycle.Scope scope){return CombatMonsterCurseLifecycle.damageContext(update,scope);}
 private static ClientSession owner(CombatMonsterCurseLifecycle.Context c){if(c==null||!c.valid())return NativeCombatAttacker.owner(null,c);var signature=new CombatMonsterCurseLifecycle.Recipient(c.event.owner(),c.event.source(),c.event.account(),c.event.name());for(var pc:c.actor.hub().classicWorldMembers(c.actor.map(),c.actor.instance()))if(signature.valid(pc,c.actor.map(),c.actor.instance()))return NativeCombatAttacker.owner(pc,c);return NativeCombatAttacker.owner(null,c);}
 static void publish(ClientSession viewer,ClassicWorldMonsterPopulation.PoisonUpdate update,CombatPoisonLifecycle.Scope scope){
  var c=context(update,scope);var plan=c.event.physical();var previous=effects.get();effects.set(c);
  try{if(plan instanceof Physical p){var pc=owner(c);if(pc!=null&&p.weapon&&p.hit)try{venom(pc,null,null,null,p.target,p.weapon,p.hit,p.target.objectId(),null);}catch(RuntimeException failure){if(c.publicationFailure==null)c.publicationFailure=failure;else c.publicationFailure.addSuppressed(failure);}CombatAccess.call(viewer,"sourceWorldPhysicalCommitted","(L"+P+"ClassicWorldMonsterPopulation$Hit;L"+P+"ClassicWorldMonsterPopulation$Snapshot;L"+P+"SourceNpcPhysicalStats;L"+P+"NativeMagicDollEffects$AttackProc;ZL"+P+"ClassicWorldMonsterPopulation$PoisonUpdate;L"+P+"CombatPoisonLifecycle$Scope;)V",update.hits().get(0),p.target,p.stats,p.dolls,c.sleepBefore,update,scope);}
   else if(plan instanceof Doll d)CombatAccess.call(viewer,"sourceWorldDollCommitted","(L"+P+"ClassicWorldMonsterPopulation$Hit;Ljava/lang/String;JL"+P+"ClassicWorldMonsterPopulation$PoisonUpdate;L"+P+"CombatPoisonLifecycle$Scope;)V",update.hits().get(0),d.source,update.target().objectId(),update,scope);
   else if(plan instanceof CombatWorldMagic.Spell magic)CombatWorldMagic.publish(viewer,update,scope,magic);
   else if(plan instanceof CombatWorldMultiMagic.Multi magic)CombatWorldMultiMagic.publish(viewer,update,scope,magic);
   else if(plan instanceof CombatWorldFreeze.Frozen frozen)CombatWorldFreeze.publish(viewer,update,scope,frozen);
  }finally{if(previous==null)effects.remove();else effects.set(previous);}
 }
 static List<ClientSession> members(ClientSession pc,int map,ClassicWorldMonsterPopulation.PoisonUpdate update,CombatPoisonLifecycle.Scope scope){return CombatMonsterCurseLifecycle.damageMembers(pc,map,update,scope);}
 static boolean same(ClientSession pc,ClientSession member,ClassicWorldMonsterPopulation.PoisonUpdate update,CombatPoisonLifecycle.Scope scope){return CombatMonsterCurseLifecycle.damageSameMap(pc,member,update,scope);}
 static void send(ClientSession pc,byte[] bytes,String reason,ClassicWorldMonsterPopulation.PoisonUpdate update,CombatPoisonLifecycle.Scope scope){CombatMonsterCurseLifecycle.damageSend(pc,bytes,reason,update,scope);}
 static String name(ClientSession pc,ClassicWorldMonsterPopulation.PoisonUpdate update,CombatPoisonLifecycle.Scope scope){return context(update,scope).event.name();}
 static boolean provoke(GameHub hub,int map,int instance,long id,long player,ClassicWorldMonsterPopulation.PoisonUpdate update,CombatPoisonLifecycle.Scope scope){var c=context(update,scope);return c.valid()&&update.hits().get(0).appliedDamage()>0&&hub.provokeClassicWorldMonster(c.actor.map(),c.actor.instance(),c.actor.id(),c.event.attacker());}
 static List<BattleCompanion.Snapshot> companions(ClientSession pc,ClassicWorldMonsterPopulation.PoisonUpdate update,CombatPoisonLifecycle.Scope scope){var c=context(update,scope);var original=owner(c);return original==null||update.hits().get(0).appliedDamage()==0?List.of():(List<BattleCompanion.Snapshot>)CombatAccess.call(original,"classicWorldCompanionSnapshots","()Ljava/util/List;");}
 static BattleCompanion owned(ClientSession pc,long id,ClassicWorldMonsterPopulation.PoisonUpdate update,CombatPoisonLifecycle.Scope scope){var original=owner(context(update,scope));return original==null?null:(BattleCompanion)CombatAccess.call(original,"ownedCompanion","(J)L"+P+"BattleCompanion;",id);}
 static void boss(ClientSession pc,ClassicWorldMonsterPopulation.Snapshot target,List<ClassicWorldMonsterPopulation.Contribution> contributions,ClassicWorldMonsterPopulation.PoisonUpdate update,CombatPoisonLifecycle.Scope scope){CombatMonsterCurseLifecycle.damageBoss(pc,target,contributions,update,scope);}
 static void rewards(ClientSession pc,ClassicWorldMonsterPopulation.Snapshot target,List<ClassicWorldMonsterPopulation.Contribution> contributions,ClassicWorldMonsterPopulation.PoisonUpdate update,CombatPoisonLifecycle.Scope scope){var original=owner(context(update,scope));if(original!=null)CombatAccess.call(original,"distributeClassicWorldMonsterRewards","(L"+P+"ClassicWorldMonsterPopulation$Snapshot;Ljava/util/List;)V",target,contributions);}
 static void schedule(GameHub hub,int instance,ClassicWorldMonsterPopulation.Snapshot target,ClassicWorldMonsterPopulation.PoisonUpdate update,CombatPoisonLifecycle.Scope scope){CombatMonsterCurseLifecycle.damageSchedule(hub,instance,target,update,scope);}
 static void dolls(ClientSession pc,NativeMagicDollEffects.AttackProc dolls,ClassicWorldMonsterPopulation.Snapshot target,SourceNpcPhysicalStats stats,ClassicWorldMonsterPopulation.PoisonUpdate update,CombatPoisonLifecycle.Scope scope){var original=owner(context(update,scope));if(original!=null)CombatAccess.call(original,"applyNativeMagicDollClassicWorldAttackProcs","(L"+P+"NativeMagicDollEffects$AttackProc;L"+P+"ClassicWorldMonsterPopulation$Snapshot;L"+P+"SourceNpcPhysicalStats;)V",dolls,target,stats);}
}
