package local.lito381.server;
import java.util.*;

/** Ordinary WORLD line and area targets use the existing signed NPC commit lane. */
final class CombatWorldMultiMagic {
 private static final String P="local/lito381/server/";
 record Multi(int skill,BattleEraseMagicStatus.Lease erase,int damage) implements CombatWorldCommit.Plan {}
 /** These Actor references end with the native cast invocation, before queued execution. */
 record Pending(CombatWorldMagic.Attempt attempt,BattleEraseMagicStatus.Lease erase) {}
 static final class Batch {final Map<Long,Pending> targets=new HashMap<>();}
 static Batch create(){return CombatRules.loaded()?new Batch():null;}
 static int line(ClientSession pc,BattleSpellCatalog.DirectionalLineSpell spell,int source,int elemental,int mr,int armor,String description,ClassicWorldMonsterPopulation.Snapshot target){
  if(!CombatRules.loaded())return CombatNpcMagic.line(pc,spell,source,elemental,mr,armor,description);
  return signed(pc,spell.skillId(),spell.attribute(),source,elemental,mr,armor,description,target);
 }
 static int area(ClientSession pc,BattleSpellCatalog.AreaAttackSpell spell,int source,int elemental,int mr,int armor,String description,ClassicWorldMonsterPopulation.Snapshot target){
  if(!CombatRules.loaded())return CombatNpcMagic.area(pc,spell,source,elemental,mr,armor,description);
  return signed(pc,spell.skillId(),spell.attribute(),source,elemental,mr,armor,description,target);
 }
 private static int signed(ClientSession pc,int skill,int attribute,int source,int elemental,int mr,int armor,String description,ClassicWorldMonsterPopulation.Snapshot target){
  if(CombatWorldMagic.protectedAtCalculation(pc,target))return 0;
  var attempt=CombatNpcMagic.ordinary(pc,skill,attribute,source,elemental,mr,armor,description,false);
  return CombatNpcMagic.resistance(attempt.damage(),attempt.magicResistance(),attempt.originalMagicHit());
 }
 static Object capturePut(Map<Object,Object> damages,Object key,Object damage,Batch batch,ClientSession pc,ClassicWorldMonsterPopulation.Snapshot target,BattleEraseMagicStatus.Lease erase){
  if(batch!=null){var attempt=CombatWorldMagic.prepare(pc,true,target,null,null,0);if(attempt!=null)batch.targets.put((Long)key,new Pending(attempt,erase));}
  return damages.put(key,damage);
 }
 static boolean enqueue(Batch batch,ClientSession pc,Map.Entry<Long,Integer> entry,int skill){
  if(batch==null)return false;var prepared=batch.targets.get(entry.getKey());if(prepared==null)return false;var attempt=prepared.attempt;var a=attempt.actor;
  synchronized(a.hub()){synchronized(a.population()){synchronized(a.timed()){
   if(!a.valid(attempt.epoch)||!attempt.owner.valid(pc,a.map(),a.instance()))return true;
   int damage=entry.getValue();if(damage==0)return false;
   if(damage<0)a.hub().consumeClassicWorldMonsterEraseMagic(a.map(),a.instance(),a.id(),prepared.erase);
   CombatMonsterCurseLifecycle.enqueue(a,attempt.owner,attempt.epoch,damage,new Multi(skill,prepared.erase,damage));return true;
  }}}
 }
 static void publish(ClientSession viewer,ClassicWorldMonsterPopulation.PoisonUpdate update,CombatPoisonLifecycle.Scope scope,Multi plan){
  var c=CombatMonsterCurseLifecycle.damageContext(update,scope);if(c==null||!c.valid())return;var a=c.actor;var hit=update.hits().get(0);
  if(plan.damage>0){
   if(!hit.diedNow())a.hub().consumeClassicWorldMonsterEraseMagic(a.map(),a.instance(),a.id(),plan.erase);
   if(c.valid())a.hub().provokeClassicWorldMonster(a.map(),a.instance(),a.id(),c.event.attacker());
   for(var companion:CombatWorldCommit.companions(viewer,update,scope)){var controlled=CombatWorldCommit.owned(viewer,companion.objectId(),update,scope);if(controlled!=null)controlled.selectMasterTarget(a.id());}
  }
  if(c.valid()&&viewer!=null)CombatAccess.call(viewer,"sourceWorldMagicPublished","(IL"+P+"ClassicWorldMonsterPopulation$Hit;L"+P+"ClassicWorldMonsterPopulation$PoisonUpdate;L"+P+"CombatPoisonLifecycle$Scope;)V",plan.skill,hit,update,scope);
 }
}
