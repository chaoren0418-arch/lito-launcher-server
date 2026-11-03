package local.lito381.server;
import java.util.*;

/** Freeze decisions are cast-local; accepted plans contain only immutable values. */
final class CombatWorldFreeze {
 private static final String P="local/lito381/server/";
 record Frozen(int skill,String key,int seconds,boolean success,BattleEraseMagicStatus.Lease erase,int damage) implements CombatWorldCommit.Plan {}
 record Pending(CombatWorldMagic.Attempt target,Frozen plan) {}
 static final class Batch {final Map<Long,Pending> targets=new HashMap<>();}
 private static final ThreadLocal<Integer> visualSeconds=new ThreadLocal<>();
 static Batch create(){return CombatRules.loaded()?new Batch():null;}
 static CombatMagicDamage.Attempt ordinary(ClientSession pc,int skill,int attribute,int source,int elemental,int mr,int armor,String description,ClientSession player,Object companion,boolean complete,ClassicWorldMonsterPopulation.Snapshot target){
  if(!CombatRules.loaded()||player!=null||companion!=null||target==null)return CombatCompanionDamage.ordinary(pc,skill,attribute,source,elemental,mr,armor,description,player,companion,complete);
  return new CombatMagicDamage.Attempt(signed(pc,skill,attribute,source,elemental,mr,armor,description,target),-1,0,null);
 }
 static int ordinaryComplete(ClientSession pc,int skill,int attribute,int source,int elemental,int mr,int armor,String description,ClassicWorldMonsterPopulation.Snapshot target){return CombatRules.loaded()?signed(pc,skill,attribute,source,elemental,mr,armor,description,target):CombatNpcMagic.ordinaryComplete(pc,skill,attribute,source,elemental,mr,armor,description);}
 private static int signed(ClientSession pc,int skill,int attribute,int source,int elemental,int mr,int armor,String description,ClassicWorldMonsterPopulation.Snapshot target){if(CombatWorldMagic.protectedAtCalculation(pc,target))return 0;var raw=CombatNpcMagic.ordinary(pc,skill,attribute,source,elemental,mr,armor,description,false);return CombatNpcMagic.resistance(raw.damage(),raw.magicResistance(),raw.originalMagicHit());}
 static Pending prepare(ClientSession pc,ClassicWorldMonsterPopulation.Snapshot target,BattleSpellCatalog.FreezeAttackSpell spell,BattleEraseMagicStatus.Lease erase,int probability,int roll,SourceNpcPhysicalStats stats,long castNow){
  if(!CombatRules.loaded()||target==null)return null;var binding=CombatWorldMagic.prepare(pc,true,target,null,null,0);if(binding==null)return null;var a=binding.actor;boolean success=probability>=roll&&!CombatProbability.bossGate((stats!=null&&stats.boss())||target.template().boss(),spell)&&(boolean)CombatAccess.call(pc,"classicWorldCkewAttackAllowed","(IJ)Z",target.npcId(),castNow);
  synchronized(a.hub()){synchronized(a.population()){synchronized(a.timed()){success=success&&a.valid(binding.epoch)&&!a.timed().earthBindActive(castNow);}}}
  return new Pending(binding,new Frozen(spell.skillId(),spell.key(),Math.max(0,spell.durationSeconds()),success,erase,0));
 }
 static Object capturePut(Map<Object,Object> leases,Object key,Object erase,Batch batch,ClientSession pc,ClassicWorldMonsterPopulation.Snapshot target,BattleSpellCatalog.FreezeAttackSpell spell,SourceNpcPhysicalStats stats,Map<Long,Integer> damages,Map<Long,Integer> probabilities,Map<Long,Integer> rolls,long now){
  if(batch!=null){var pending=prepare(pc,target,spell,(BattleEraseMagicStatus.Lease)erase,probabilities.get((Long)key),rolls.get((Long)key),stats,now);if(pending!=null)batch.targets.put((Long)key,pending);}return leases.put(key,erase);
 }
 static boolean enqueueBatch(Batch batch,ClientSession pc,ClassicWorldMonsterPopulation.Snapshot target,int damage){return batch!=null&&enqueue(batch.targets.get(target.objectId()),pc,damage);}
 static boolean enqueue(Pending pending,ClientSession pc,int damage){
  if(pending==null)return false;var binding=pending.target;var a=binding.actor;var p=pending.plan;var plan=new Frozen(p.skill,p.key,p.seconds,p.success,p.erase,damage);
  synchronized(a.hub()){synchronized(a.population()){synchronized(a.timed()){
   if(!a.valid(binding.epoch)||!binding.owner.valid(pc,a.map(),a.instance()))return true;
   if(damage!=0)CombatMonsterCurseLifecycle.enqueue(a,binding.owner,binding.epoch,damage,plan);
  }}}
  if(damage<=0)try{apply(a,binding.epoch,plan,null,null);}catch(RuntimeException failure){CombatNpcDamageQueue.failed(failure);}return true;
 }
 static boolean rawFrozen(boolean existing,BattleTimedStatus t){return existing||CombatRules.loaded()&&CombatPoisonLifecycle.rawFreeze(t);}
 static int visualDuration(int seconds){return visualSeconds.get()==null?seconds:Math.max(1,Math.min(32,seconds));}
 static long visualExpiry(long nativeExpiry,long now){var seconds=visualSeconds.get();return seconds==null?nativeExpiry:seconds==0?Long.MAX_VALUE:Math.addExact(now,seconds*1_000_000_000L);}
 private static void apply(CombatMonsterCurseLifecycle.Actor a,long epoch,Frozen plan,ClassicWorldMonsterPopulation.PoisonUpdate update,CombatPoisonLifecycle.Scope scope){
  int x,y;long now=System.nanoTime();boolean freeze;
  synchronized(a.hub()){synchronized(a.population()){synchronized(a.timed()){
   if(!a.valid(epoch)||!(boolean)CombatAccess.field(a.actor(),"alive"))return;
   a.hub().consumeClassicWorldMonsterEraseMagic(a.map(),a.instance(),a.id(),plan.erase);freeze=plan.success;
   if(freeze){if(plan.seconds==0)CombatPoisonLifecycle.rawFreeze(a.timed(),true);else{CombatPoisonLifecycle.rawFreeze(a.timed(),false);long old=((Number)CombatAccess.field(a.timed(),"freezeExpiresAtNanos")).longValue();long remaining=old<=now?0:(old-now+999_999_999L)/1_000_000_000L;if(remaining<plan.seconds)CombatAccess.set(a.timed(),"freezeExpiresAtNanos",Math.addExact(now,plan.seconds*1_000_000_000L));}}
   x=((Number)CombatAccess.field(a.actor(),"x")).intValue();y=((Number)CombatAccess.field(a.actor(),"y")).intValue();
  }}}
  if(!freeze)return;BattleFreezeEffects.Placement placed;var previous=visualSeconds.get();visualSeconds.set(plan.seconds);try{placed=a.hub().placeClassicWorldFreezeEffect(a.map(),a.instance(),a.id(),x,y,now,plan.seconds);}finally{if(previous==null)visualSeconds.remove();else visualSeconds.set(previous);}
  var sent=new LinkedHashMap<ClientSession,CombatMonsterCurseLifecycle.Recipient>();
  try{for(var pc:a.hub().classicWorldMembers(a.map(),a.instance())){
   var identity=CombatMonsterCurseLifecycle.Recipient.take(pc);if(!a.valid(epoch)||!identity.valid(pc,a.map(),a.instance())||!BattleSpellCatalog.isInNativeScreen(((Number)CombatAccess.field(pc,"positionX")).intValue(),((Number)CombatAccess.field(pc,"positionY")).intValue(),x,y))continue;
   if(placed!=null){if(placed.previous()!=null)send(a,epoch,identity,pc,Protocol.removeObject(placed.previous().objectId()),"S_REMOVE_OBJECT refreshed classic-world freeze effect="+placed.previous().objectId(),update,scope);if(send(a,epoch,identity,pc,Protocol.freezeEffect(placed.placed()),"S_PUT_OBJECT classic-world freeze effect="+placed.placed().objectId()+" target="+a.id()+" npc="+BattleFreezeEffects.NPC_ID+" gfx="+BattleFreezeEffects.GFX+" duration="+plan.seconds+" reason="+plan.key,update,scope))sent.put(pc,identity);}
   send(a,epoch,identity,pc,Protocol.poison(a.id(),2),"S_POISON classic-world-monster="+a.id()+" type=2 reason="+plan.key,update,scope);
  }}finally{if(placed!=null&&!a.valid(epoch)){synchronized(a.hub()){for(var current:a.hub().classicWorldFreezeEffects(a.map(),a.instance(),System.nanoTime()))if(current.targetObjectId()==a.id()&&current.objectId()==placed.placed().objectId()){a.hub().removeClassicWorldFreezeEffect(a.map(),a.instance(),a.id());break;}}for(var e:sent.entrySet())if(e.getValue().valid(e.getKey(),a.map(),a.instance()))try{CombatAccess.call(e.getKey(),"sendEncrypted","([BLjava/lang/String;)V",Protocol.removeObject(placed.placed().objectId()),"S_REMOVE_OBJECT retired classic-world freeze effect="+placed.placed().objectId());}catch(RuntimeException failure){var c=update==null?null:CombatMonsterCurseLifecycle.damageContext(update,scope);if(c==null)CombatNpcDamageQueue.failed(failure);else if(c.publicationFailure==null)c.publicationFailure=failure;else c.publicationFailure.addSuppressed(failure);}}}
 }
 private static boolean send(CombatMonsterCurseLifecycle.Actor a,long epoch,CombatMonsterCurseLifecycle.Recipient identity,ClientSession pc,byte[] bytes,String reason,ClassicWorldMonsterPopulation.PoisonUpdate update,CombatPoisonLifecycle.Scope scope){if(!a.valid(epoch)||!identity.valid(pc,a.map(),a.instance()))return false;if(update!=null)CombatWorldCommit.send(pc,bytes,reason,update,scope);else CombatAccess.call(pc,"sendEncrypted","([BLjava/lang/String;)V",bytes,reason);return true;}
 static void publish(ClientSession viewer,ClassicWorldMonsterPopulation.PoisonUpdate update,CombatPoisonLifecycle.Scope scope,Frozen plan){
  var c=CombatMonsterCurseLifecycle.damageContext(update,scope);if(c==null||!c.valid())return;var hit=update.hits().get(0);if(plan.damage>0){if(!hit.diedNow())try{apply(c.actor,c.epoch,plan,update,scope);}catch(RuntimeException failure){if(c.publicationFailure==null)c.publicationFailure=failure;else c.publicationFailure.addSuppressed(failure);}if(c.valid())c.actor.hub().provokeClassicWorldMonster(c.actor.map(),c.actor.instance(),c.actor.id(),c.event.attacker());for(var companion:CombatWorldCommit.companions(viewer,update,scope)){var owned=CombatWorldCommit.owned(viewer,companion.objectId(),update,scope);if(owned!=null)owned.selectMasterTarget(c.actor.id());}}
  if(viewer!=null&&c.valid())CombatAccess.call(viewer,"sourceWorldMagicPublished","(IL"+P+"ClassicWorldMonsterPopulation$Hit;L"+P+"ClassicWorldMonsterPopulation$PoisonUpdate;L"+P+"CombatPoisonLifecycle$Scope;)V",plan.skill,hit,update,scope);
 }
}
