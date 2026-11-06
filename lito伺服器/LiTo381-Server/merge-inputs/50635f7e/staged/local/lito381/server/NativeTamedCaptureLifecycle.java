package local.lito381.server;
import java.lang.ref.WeakReference;import java.util.*;import java.util.concurrent.*;
/** Source tame constructor: initialize loot once, transfer the canonical bag, retire the original spawn. */
final class NativeTamedCaptureLifecycle {
 static final String PREP="nativeTamedCapturePreparation",POP="nativeTamedCapturePopulation",DROP="nativeTamedDropInitialized";
 static final class Preparation {
  final WeakReference<ClassicWorldMonsterPopulation> population;final WeakReference<Object> actor;final ScheduledFuture<?> respawn;
  Preparation(ClassicWorldMonsterPopulation p,Object a,ScheduledFuture<?> r){population=new WeakReference<>(p);actor=new WeakReference<>(a);respawn=r;}
 }
 static ClassicWorldMonsterPopulation population(GameHub h,int map,int instance){return (ClassicWorldMonsterPopulation)CombatAccess.call(h,"classicWorldMonsterPopulation","(II)Llocal/lito381/server/ClassicWorldMonsterPopulation;",map,instance);}
 static ClassicWorldMonsterPopulation.Capture capture(GameHub hub,int map,int instance,long id,ClientSession pc){
  if(CombatAccess.field(pc,"hub")!=hub||!pc.nativePlayerActionLock().isHeldByCurrentThread()||!String.valueOf(CombatAccess.field(pc,"phase")).equals("WORLD")
    ||(int)CombatAccess.field(pc,"classicWorldMapId")!=map||(int)CombatAccess.field(pc,"classicWorldInstanceId")!=instance)return null;
  var binding=NativePlayerCommit.Binding.capture(pc);
  synchronized(hub){var pop=population(hub,map,instance);if(pop==null||!binding.matches(pc))return null;
   synchronized(pop){var actor=((Map<?,?>)CombatAccess.field(pop,"actors")).get(id);if(actor==null||!(boolean)CombatAccess.field(actor,"alive")||!(boolean)CombatAccess.field(actor,"present"))return null;
    var original=(ClassicWorldMonsterPopulation.Snapshot)CombatAccess.call(actor,"snapshot","()Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;");if(original.mapId()!=map)return null;
    var animations=BattleAnimationCatalog.get();if(!animations.supportsTimedAction(original.gfxId(),BattleActionCodes.WALK)||!animations.supportsTimedAction(original.gfxId(),BattleActionCodes.ATTACK))return null;
    var before=(RuntimeInventory)CombatAccess.field(actor,"releasedItems");var bag=before;
    if(!(boolean)CombatAccess.field(actor,DROP)&&!NativeTamedWorldLifecycle.initializedDrop(original)){
     var cfg=(ServerConfig)CombatAccess.field(pc,"config");var drops=new ArrayList<ClassicWorldDropCatalog.RolledItem>(ClassicWorldDropCatalog.roll(map,original.npcId(),cfg.classicWorldDropItemRate(),cfg.classicWorldDropAdenaRate(),1.0,ThreadLocalRandom.current()));
     drops.addAll(ClassicWorldRoyalTrialCatalog.rollOrdinaryDrops(map,original.npcId(),cfg.classicWorldDropItemRate(),cfg.classicWorldDropAdenaRate(),1.0,ThreadLocalRandom.current()));
     if(!drops.isEmpty()){bag=before.copy();for(var drop:drops)if(!bag.add(drop.item().inventoryDefinition(),drop.quantity())){NativeReuseLog.getLogger(NativeTamedCaptureLifecycle.class).error("Taming rejected: complete initialized NPC bag cannot fit; original actor and every item retained",(Throwable)null);return null;}}
    }
    int delay=pop.nextRespawnDelaySeconds(map,id);ScheduledFuture<?> future=null;
    if(delay>=0)try{future=NativeTamedWorldLifecycle.scheduleOrigin(hub,pop,actor,map,instance,id,delay);}catch(RejectedExecutionException failure){NativeReuseLog.getLogger(NativeTamedCaptureLifecycle.class).error("Taming respawn admission rejected; original actor retained",failure);return null;}
    var prep=new Preparation(pop,actor,future);boolean committed=false;
    try{if(!binding.matches(pc))return null;var result=pop.captureTamed(map,id);if(result==null)return null;
     CombatAccess.set(actor,"releasedItems",bag);CombatAccess.set(actor,DROP,true);CombatAccess.set(result.captured(),PREP,prep);committed=true;return result;
    }finally{if(!committed&&future!=null)future.cancel(false);}
   }
  }
 }
 static BattleCompanion create(long id,ClassicWorldMonsterPopulation.Snapshot captured,int instance,SourceNpcPhysicalStats stats,ClientSession owner){
  try{return NativeTamedWorldLifecycle.captured(BattleCompanion.tamedFromClassicWorld(id,captured,instance,stats),owner,captured,instance);}
  catch(RuntimeException|Error failure){try{rollback(owner,captured,instance);}catch(RuntimeException|Error cleanup){failure.addSuppressed(cleanup);}throw failure;}
 }
 private static void rollback(ClientSession owner,ClassicWorldMonsterPopulation.Snapshot captured,int instance){
  var prep=(Preparation)CombatAccess.field(captured,PREP);if(prep==null)return;var hub=(GameHub)CombatAccess.field(owner,"hub");var pop=prep.population.get();var actor=prep.actor.get();
  if(prep.respawn!=null)prep.respawn.cancel(false);
  synchronized(hub){if(pop==null||actor==null||population(hub,captured.mapId(),instance)!=pop)return;synchronized(pop){
   if(((Map<?,?>)CombatAccess.field(pop,"actors")).get(captured.objectId())!=actor||(boolean)CombatAccess.field(actor,"present"))return;
   var type=actor.getClass().getEnclosingClass();try{var cell=Class.forName(type.getName()+"$Cell").getDeclaredConstructor(int.class,int.class,int.class);cell.setAccessible(true);var at=cell.newInstance(captured.mapId(),captured.x(),captured.y());
    CombatAccess.call(pop,"occupy","(Llocal/lito381/server/ClassicWorldMonsterPopulation$Cell;)V",at);
   }catch(ReflectiveOperationException e){throw new IllegalStateException("restore failed native tame capture",e);}
   CombatAccess.set(actor,"present",true);CombatAccess.set(actor,"alive",true);CombatAccess.set(actor,"currentHp",captured.currentHp());CombatAccess.set(captured,PREP,null);
  }}
 }
 static boolean attach(BattleCompanion pet,ClientSession owner,ClassicWorldMonsterPopulation.Snapshot captured,int instance){
  var prep=(Preparation)CombatAccess.field(captured,PREP);if(prep==null)return false;
  var hub=(GameHub)CombatAccess.field(owner,"hub");var pop=prep.population.get();var actor=prep.actor.get();
  synchronized(hub){if(pop==null||actor==null||population(hub,captured.mapId(),instance)!=pop)throw new IllegalStateException("prepared tamed scene retired");
   synchronized(pop){if(((Map<?,?>)CombatAccess.field(pop,"actors")).get(captured.objectId())!=actor||!(boolean)CombatAccess.field(actor,"alive")||(boolean)CombatAccess.field(actor,"present"))throw new IllegalStateException("prepared tamed actor retired");
    synchronized(pet){var empty=new RuntimeInventory();var stamp=new WeakReference<>(pop);var bag=(RuntimeInventory)CombatAccess.field(actor,"releasedItems");
     if(pop.abandonTamed(captured.mapId(),captured.objectId())==null)throw new IllegalStateException("prepared tamed origin changed");
     CombatAccess.set(pet,"carriedItems",bag);CombatAccess.set(actor,"releasedItems",empty);CombatAccess.set(pet,"hp",captured.currentHp());CombatAccess.set(pet,"mp",captured.currentMp());CombatAccess.set(pet,"heading",captured.heading());CombatAccess.set(pet,POP,stamp);CombatAccess.set(captured,PREP,null);return true;
    }
   }
  }
 }
 static boolean prepared(BattleCompanion pet,ClassicWorldMonsterPopulation pop){var stamp=(WeakReference<?>)CombatAccess.field(pet,POP);return stamp!=null&&stamp.get()==pop;}
 static boolean initializedDrop(ClientSession pc,ClassicWorldMonsterPopulation.Snapshot dead){
  if(NativeTamedWorldLifecycle.initializedDrop(dead))return true;var h=(GameHub)CombatAccess.field(pc,"hub");
  synchronized(h){var pop=population(h,dead.mapId(),(int)CombatAccess.field(pc,"classicWorldInstanceId"));if(pop==null)return false;synchronized(pop){var actor=((Map<?,?>)CombatAccess.field(pop,"actors")).get(dead.objectId());return actor!=null&&CombatAccess.field(actor,"definition")==dead.definition()&&(boolean)CombatAccess.field(actor,DROP);}}
 }
 private NativeTamedCaptureLifecycle(){}
}
