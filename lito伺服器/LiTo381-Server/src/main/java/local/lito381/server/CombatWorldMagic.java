package local.lito381.server;
import java.util.*;

/** Ordinary WORLD magic uses the existing NPC lane; queued plans contain values only. */
final class CombatWorldMagic {
 private static final String P="local/lito381/server/";
 record Spell(BattleSpellCatalog.AttackSpell spell,BattleEraseMagicStatus.Lease erase,int thunderSeconds,int damage,long ownerToken) implements CombatWorldCommit.Plan {}
 /** Only the casting stack retains Actor, through action publication and admission. */
 static final class Attempt {
  final CombatMonsterCurseLifecycle.Actor actor;final CombatMonsterCurseLifecycle.Recipient owner;final long epoch;final int thunderSeconds;final long ownerToken;
  Attempt(CombatMonsterCurseLifecycle.Actor actor,CombatMonsterCurseLifecycle.Recipient owner,long epoch,int seconds,ClientSession pc){this.actor=actor;this.owner=owner;this.epoch=epoch;thunderSeconds=seconds;ownerToken=CombatPoisonLifecycle.ownerToken(pc,true);}
 }
 static int critical(BattleSpellCatalog.AttackSpell spell,int damage,boolean critical,ClientSession player,CombatMagicDamage.Attempt attempt,boolean ordinary,int mr,ClientSession caster,ClassicWorldMonsterPopulation.Snapshot target){
  if(CombatRules.loaded()&&ordinary&&protectedAtCalculation(caster,target))return 0;
  if(CombatRules.loaded()&&ordinary&&attempt!=null&&attempt.npc()){if(critical)damage*=2;return CombatNpcMagic.resistance(damage,attempt.magicResistance(),attempt.originalMagicHit());}
  if(CombatRules.loaded()&&ordinary&&attempt!=null&&attempt.finalBurn()!=null){int raw=attempt.finalBurn().preMrDamage();if(critical)raw*=2;return CombatNpcMagic.resistance(raw,mr,attempt.originalMagicHit());}
  return CombatCompanionDamage.beforeCritical(spell,damage,critical,player,attempt);
 }
 static boolean protectedAtCalculation(ClientSession pc,ClassicWorldMonsterPopulation.Snapshot target){
  if(target==null)return false;var hub=(GameHub)CombatAccess.field(pc,"hub");synchronized(hub){var a=CombatMonsterCurseLifecycle.actor(hub,(int)number(pc,"classicWorldMapId"),(int)number(pc,"classicWorldInstanceId"),target.objectId());if(a==null)return false;synchronized(a.population()){synchronized(a.timed()){long now=System.nanoTime();return a.timed().earthBindActive(now)||a.timed().freezeActive(now)||a.timed().absoluteBarrierActive(now);}}}
 }
 static Attempt prepare(ClientSession pc,boolean ordinary,ClassicWorldMonsterPopulation.Snapshot target,BattleSpellCatalog.ThunderGrabSpell thunder,SourceNpcPhysicalStats stats,int level){
  if(!CombatRules.loaded()||!ordinary||target==null)return null;var hub=(GameHub)CombatAccess.field(pc,"hub");CombatMonsterCurseLifecycle.Actor a;CombatMonsterCurseLifecycle.Recipient owner;long epoch;boolean bound;
  synchronized(hub){a=CombatMonsterCurseLifecycle.actor(hub,(int)number(pc,"classicWorldMapId"),(int)number(pc,"classicWorldInstanceId"),target.objectId());if(a==null)return null;synchronized(a.population()){synchronized(a.timed()){CombatPoisonLifecycle.monsterDamage(a.timed(),true);owner=CombatMonsterCurseLifecycle.Recipient.take(pc);epoch=CombatPoisonLifecycle.monsterEpoch(a.timed(),false);bound=a.timed().thunderGrabActive(System.nanoTime());}}}
  int seconds=0;if(thunder!=null&&!bound){var config=(ServerConfig)CombatAccess.field(pc,"config");seconds=CombatProbability.npcThunderDuration(pc,thunder,level,target.template().level(),target.template().boss()||stats!=null&&stats.boss(),config.thunderGrabFixtureD100()>0?config.thunderGrabFixtureD100():-1,config.thunderGrabFixtureDurationOffset(),target.objectId(),"classic-world-monster",new Object[]{target,stats});}
  return new Attempt(a,owner,epoch,seconds,pc);
 }
 static boolean enqueue(Attempt attempt,ClientSession pc,int damage,BattleSpellCatalog.AttackSpell spell,BattleEraseMagicStatus.Lease erase){
  if(attempt==null)return false;var a=attempt.actor;
  synchronized(a.hub()){synchronized(a.population()){synchronized(a.timed()){
   if(!a.valid(attempt.epoch)||!attempt.owner.valid(pc,a.map(),a.instance()))return true;
   if(damage==0)return false;
   // Source nonpositive skill mutations are independent of HP acceptance; zero stays native.
   if(damage<0){a.hub().consumeClassicWorldMonsterEraseMagic(a.map(),a.instance(),a.id(),erase);if(attempt.thunderSeconds>0)a.hub().thunderGrabClassicWorldMonster(a.map(),a.instance(),a.id(),System.nanoTime(),attempt.thunderSeconds);}
   CombatMonsterCurseLifecycle.enqueue(a,attempt.owner,attempt.epoch,damage,new Spell(spell,erase,attempt.thunderSeconds,damage,attempt.ownerToken));return true;
  }}}
 }
 private static long number(Object o,String f){return ((Number)CombatAccess.field(o,f)).longValue();}
 private static ClientSession healingOwner(CombatMonsterCurseLifecycle.Context c,Spell plan){
  if(!c.valid())return null;for(var pc:c.actor.hub().connectedSessionsSnapshot())if(String.valueOf(CombatAccess.field(pc,"phase")).equals("WORLD")&&pc.playerObjectId()==c.event.owner()&&pc.sourceCharacterIdentity().equals(c.event.source())&&pc.sourceAccountIdentity().equals(c.event.account())&&pc.selectedCharacterName().equals(c.event.name())&&CombatPoisonLifecycle.ownerToken(pc,false)==plan.ownerToken)return pc;return null;
 }
 private static void effect(CombatMonsterCurseLifecycle.Context c,Runnable action){try{action.run();}catch(RuntimeException e){if(c.publicationFailure==null)c.publicationFailure=e;else c.publicationFailure.addSuppressed(e);}}
 static void publish(ClientSession viewer,ClassicWorldMonsterPopulation.PoisonUpdate update,CombatPoisonLifecycle.Scope scope,Spell plan){
  var c=CombatMonsterCurseLifecycle.damageContext(update,scope);if(c==null||!c.valid())return;var hit=update.hits().get(0);var a=c.actor;var owner=healingOwner(c,plan);
  if(plan.damage>0){
   if(owner!=null&&plan.spell.drainsLife())effect(c,()->CombatAccess.call(owner,"applyAcceptedLifeDrain","(L"+P+"BattleSpellCatalog$AttackSpell;ILjava/lang/String;)I",plan.spell,plan.damage,"classic-world-monster"));
   if(!hit.diedNow())effect(c,()->{if(c.valid()){a.hub().consumeClassicWorldMonsterEraseMagic(a.map(),a.instance(),a.id(),plan.erase);if(plan.thunderSeconds>0)a.hub().thunderGrabClassicWorldMonster(a.map(),a.instance(),a.id(),System.nanoTime(),plan.thunderSeconds);}});
   if(c.valid())a.hub().provokeClassicWorldMonster(a.map(),a.instance(),a.id(),c.event.attacker());
   if(owner!=null)for(var companion:CombatWorldCommit.companions(owner,update,scope)){var controlled=CombatWorldCommit.owned(owner,companion.objectId(),update,scope);if(controlled!=null)controlled.selectMasterTarget(a.id());}
  }
  if(c.valid()&&viewer!=null)CombatAccess.call(viewer,"sourceWorldMagicPublished","(IL"+P+"ClassicWorldMonsterPopulation$Hit;L"+P+"ClassicWorldMonsterPopulation$PoisonUpdate;L"+P+"CombatPoisonLifecycle$Scope;)V",plan.spell.skillId(),hit,update,scope);
 }
}
