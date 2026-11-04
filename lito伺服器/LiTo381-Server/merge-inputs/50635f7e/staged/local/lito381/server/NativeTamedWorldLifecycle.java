package local.lito381.server;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
/** Existing skill145 WORLD transition. All state is confined to the retained memory scene. */
final class NativeTamedWorldLifecycle {
 static final String LIBERATED="source-liberated-tamed";
 private static final Class<?> ACTOR=type("ClassicWorldMonsterPopulation$Actor"),CELL=type("ClassicWorldMonsterPopulation$Cell");
 private static Class<?> type(String name){try{return Class.forName("local.lito381.server."+name);}catch(ClassNotFoundException e){throw new ExceptionInInitializerError(e);}}
 private static Object construct(Class<?> type,Class<?>[] signature,Object... args){try{var c=type.getDeclaredConstructor(signature);c.setAccessible(true);return c.newInstance(args);}catch(ReflectiveOperationException e){throw new IllegalStateException("native tamed construction",e);}}
 private static ClassicWorldMonsterCatalog.MonsterTemplate zeroExperience(ClassicWorldMonsterCatalog.MonsterTemplate original){
  var parts=original.getClass().getRecordComponents();var types=new Class<?>[parts.length];var values=new Object[parts.length];
  try{for(int i=0;i<parts.length;i++){types[i]=parts[i].getType();var get=parts[i].getAccessor();get.setAccessible(true);values[i]=parts[i].getName().equals("experience")?0L:get.invoke(original);}}
  catch(ReflectiveOperationException e){throw new IllegalStateException("native tamed template",e);}
  return (ClassicWorldMonsterCatalog.MonsterTemplate)construct(original.getClass(),types,values);
 }
 private static ClassicWorldMonsterPopulation population(GameHub hub,int map,int instance){return (ClassicWorldMonsterPopulation)CombatAccess.call(hub,"classicWorldMonsterPopulation","(II)Llocal/lito381/server/ClassicWorldMonsterPopulation;",map,instance);}
 /** Finish the existing WORLD factory before its owner-list publication. */
 static BattleCompanion captured(BattleCompanion pet,ClientSession owner,ClassicWorldMonsterPopulation.Snapshot captured,int instance){
  if(!owner.nativePlayerActionLock().isHeldByCurrentThread())throw new IllegalStateException("tamed capture requires the admitted player action");
  if(NativeTamedCaptureLifecycle.attach(pet,owner,captured,instance))return pet;
  var hub=(GameHub)CombatAccess.field(owner,"hub");var binding=NativePlayerCommit.Binding.capture(owner);
  synchronized(hub){var pop=population(hub,captured.mapId(),instance);if(pop==null)throw new IllegalStateException("captured population retired");
   synchronized(pop){var actor=((Map<?,?>)CombatAccess.field(pop,"actors")).get(captured.objectId());
    if(!owner.nativePlayerActionLock().isHeldByCurrentThread()||!binding.matches(owner)||actor==null||CombatAccess.field(actor,"definition")!=captured.definition()
      ||!(boolean)CombatAccess.field(actor,"alive")||(boolean)CombatAccess.field(actor,"present")||!pet.carriedItems().isEmpty())throw new IllegalStateException("captured actor changed before owner publication");
    synchronized(pet){var empty=new RuntimeInventory();var bag=(RuntimeInventory)CombatAccess.field(actor,"releasedItems");
     CombatAccess.set(pet,"carriedItems",bag);CombatAccess.set(actor,"releasedItems",empty);
     CombatAccess.set(pet,"hp",captured.currentHp());CombatAccess.set(pet,"mp",captured.currentMp());CombatAccess.set(pet,"heading",captured.heading());return pet;
    }
   }
  }
 }
 @SuppressWarnings("unchecked")
 static ClassicWorldMonsterPopulation.TamedRelease release(GameHub hub,int map,int instance,long originId,BattleCompanion.Snapshot expected,ClientSession owner,BattleCompanion pet){
  if(owner==null||pet==null||expected==null||expected.kind()!=BattleCompanion.Kind.TAMED||CombatAccess.field(owner,"hub")!=hub)return null;
  var binding=NativePlayerCommit.Binding.capture(owner);var caster=NativeReturnNatureMaterialAction.caster(owner,pet);
  if(caster==null||!NativeLightAction.retainedMaterial(caster)||!owner.nativePlayerActionLock().isHeldByCurrentThread())return null;
  var pop=population(hub,map,instance);if(pop==null)return null;
  synchronized(hub){synchronized(pet){synchronized(pop){
   var live=pet.snapshot();var origin=pet.classicWorldTamedOrigin();
   if(!binding.matches(owner)||!NativeLightAction.validTargets(caster)||!live.active()||live.hp()<=0||live.objectId()!=expected.objectId()
     ||origin==null||origin.objectId()!=originId||origin.mapId()!=map||pet.classicWorldTamedInstanceId()!=instance
     ||CombatAccess.field(owner,"phase").toString().equals("WORLD")==false||(int)CombatAccess.field(owner,"classicWorldMapId")!=map
     ||(int)CombatAccess.field(owner,"classicWorldInstanceId")!=instance||population(hub,map,instance)!=pop)return null;
   var actors=(Map<Long,Object>)CombatAccess.field(pop,"actors");var old=actors.get(originId);boolean prepared=NativeTamedCaptureLifecycle.prepared(pet,pop);
   if(CombatAccess.field(pet,NativeTamedCaptureLifecycle.POP)!=null&&!prepared)return null;
   if((!prepared&&(old==null||CombatAccess.field(old,"definition")!=origin.definition()||!(boolean)CombatAccess.field(old,"alive")||(boolean)CombatAccess.field(old,"present")))||live.npcId()!=origin.npcId())return null;
   long id=(long)CombatAccess.field(pop,"nextQuestDynamicObjectId"),end=(long)CombatAccess.field(pop,"questDynamicObjectEnd");
   if(id<=0||id>end||actors.containsKey(id)||id==live.objectId()||id==originId)return null;
   var template=zeroExperience(origin.template());
   var definition=new ClassicWorldMonsterCatalog.SpawnDefinition(map,origin.spawnId(),LIBERATED,1,0,live.x(),live.y(),0,0,0,0,0,0,live.heading(),0,0,false,30,false,0,template);
   var bag=(RuntimeInventory)CombatAccess.field(pet,"carriedItems");var empty=new RuntimeInventory();
   var actor=construct(ACTOR,new Class<?>[]{ClassicWorldMonsterCatalog.SpawnDefinition.class,int.class,long.class,Random.class,boolean.class,List.class},definition,1,id,new Random(id),true,List.of());
   CombatAccess.set(actor,"x",live.x());CombatAccess.set(actor,"y",live.y());CombatAccess.set(actor,"homeX",live.x());CombatAccess.set(actor,"homeY",live.y());CombatAccess.set(actor,"heading",live.heading());
   CombatAccess.set(actor,"currentHp",live.hp());CombatAccess.set(actor,"currentMp",live.mp());CombatAccess.set(actor,"releasedItems",bag);
   var hidden=prepared?hidden(origin):(ClassicWorldMonsterPopulation.Snapshot)CombatAccess.call(old,"snapshot","()Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;");
   var released=(ClassicWorldMonsterPopulation.Snapshot)CombatAccess.call(actor,"snapshot","()Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;");
   var result=new ClassicWorldMonsterPopulation.TamedRelease(hidden,released);var cell=construct(CELL,new Class<?>[]{int.class,int.class,int.class},map,live.x(),live.y());
   int delay=prepared?-1:pop.nextRespawnDelaySeconds(map,originId);ScheduledFuture<?> respawn=null;
   // Schedule first: an OPEN executor rejection leaves both actor and bag untouched.
   // Weak references and the exact population/actor guard retire obsolete private scenes.
   if(delay>=0){try{respawn=scheduleOrigin(hub,pop,old,map,instance,originId,delay);}catch(RejectedExecutionException failure){NativeReuseLog.getLogger(NativeTamedWorldLifecycle.class).error("Tamed release respawn admission failed; controlled actor retained",failure);return null;}}
   boolean occupied=false,committed=false;
   try{
    if(!binding.matches(owner)||!NativeLightAction.validTargets(caster)||population(hub,map,instance)!=pop)return null;
    actors.put(id,actor);
    CombatAccess.call(pop,"occupy","(Llocal/lito381/server/ClassicWorldMonsterPopulation$Cell;)V",cell);occupied=true;
    if(!prepared&&pop.abandonTamed(map,originId)==null)throw new IllegalStateException("tamed origin changed under population monitor");
    CombatAccess.set(pet,"carriedItems",empty);pet.deactivate();
    CombatAccess.set(pop,"nextQuestDynamicObjectId",id+1);committed=true;return result;
   }finally{if(!committed){if(respawn!=null)respawn.cancel(false);if(occupied)CombatAccess.call(pop,"vacate","(Llocal/lito381/server/ClassicWorldMonsterPopulation$Cell;)V",cell);actors.remove(id,actor);if(!prepared){CombatAccess.set(old,"currentHp",hidden.currentHp());CombatAccess.set(old,"alive",hidden.alive());}CombatAccess.set(pet,"carriedItems",bag);}}
  }}}
 }
 static boolean initializedDrop(ClassicWorldMonsterPopulation.Snapshot snapshot){return LIBERATED.equals(snapshot.definition().location());}
 private static ClassicWorldMonsterPopulation.Snapshot hidden(ClassicWorldMonsterPopulation.Snapshot s){return new ClassicWorldMonsterPopulation.Snapshot(s.mapId(),s.spawnId(),s.spawnNumber(),s.objectId(),s.npcId(),s.x(),s.y(),s.heading(),s.currentHp(),s.maximumHp(),s.currentMp(),s.maximumMp(),true,false,s.hiddenStatus(),s.actorStatus(),s.definition(),s.gfxId(),s.nameId(),s.title(),s.lawful(),s.passispeed(),s.atkspeed());}
 static ScheduledFuture<?> scheduleOrigin(GameHub hub,ClassicWorldMonsterPopulation pop,Object actor,int map,int instance,long id,int delay){long old=(long)CombatAccess.field(actor,"nativeTamedRespawnGeneration");if(old==Long.MAX_VALUE)throw new RejectedExecutionException("tamed origin generation exhausted");long generation=old+1;var future=((ScheduledExecutorService)CombatAccess.field(hub,"battleClock")).schedule(new Respawn(hub,pop,actor,map,instance,id,generation),delay,TimeUnit.SECONDS);CombatAccess.set(actor,"nativeTamedRespawnGeneration",generation);return future;}
 static void retireRespawn(Object actor){long old=(long)CombatAccess.field(actor,"nativeTamedRespawnGeneration");if(old!=Long.MAX_VALUE)CombatAccess.set(actor,"nativeTamedRespawnGeneration",old+1);}
 private static final class Respawn implements Runnable {
  final WeakReference<GameHub> hub;final WeakReference<ClassicWorldMonsterPopulation> population;final WeakReference<Object> actor;final int map,instance;final long id,generation;
  Respawn(GameHub h,ClassicWorldMonsterPopulation p,Object a,int m,int i,long n,long g){hub=new WeakReference<>(h);population=new WeakReference<>(p);actor=new WeakReference<>(a);map=m;instance=i;id=n;generation=g;}
  public void run(){
   var h=hub.get();var p=population.get();var a=actor.get();if(h==null||p==null||a==null)return;
   ClassicWorldMonsterPopulation.Respawn result;
   synchronized(h){if(population(h,map,instance)!=p)return;var app=(NativeDeItemApplication)CombatAccess.field(h,NativeDeItemApplication.FIELD);if(app!=null&&app.closed)return;
    synchronized(p){if(((Map<?,?>)CombatAccess.field(p,"actors")).get(id)!=a||(long)CombatAccess.field(a,"nativeTamedRespawnGeneration")!=generation||(boolean)CombatAccess.field(a,"alive")||(boolean)CombatAccess.field(a,"present"))return;result=p.respawn(map,id);}}
   if(result==null)return;
   // Network publication runs after every hub/population monitor has been released.
   for(var member:h.classicWorldMembers(map,instance))try{member.serverClassicWorldMonsterRespawned(result.after());for(var minion:result.groupMembers())member.serverClassicWorldMonsterRespawned(minion);}
   catch(java.io.IOException failure){NativeReuseLog.getLogger(NativeTamedWorldLifecycle.class).error("Tamed origin respawn publication failed",failure);}
  }
 }
 private NativeTamedWorldLifecycle(){}
}
