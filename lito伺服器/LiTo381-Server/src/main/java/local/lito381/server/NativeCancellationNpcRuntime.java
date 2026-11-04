// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/cancellation-npc-runtime/NativeCancellationNpcRuntime.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/** WORLD replacement leases belong to the hub, never to the casting player's connection. */
final class NativeCancellationNpcRuntime {
 static final String P="local/lito381/server/";
 static final class State {
  final GameHub hub;
  final List<Lease> leases=new ArrayList<>();
  final List<Notice> pending=new ArrayList<>();
  ScheduledFuture<?> task;
  boolean closed;
  State(GameHub hub){this.hub=hub;}
 }
 static final class Lease {
  final ClassicWorldMonsterPopulation population;
  final int map,instance;
  final long id,deadline;
  final Object actor;
  final boolean respawn;
  Lease(ClassicWorldMonsterPopulation population,int map,int instance,long id,long deadline,boolean respawn){
   this.population=population;this.map=map;this.instance=instance;this.id=id;this.deadline=deadline;this.respawn=respawn;
   actor=NativeCancellationNpcMaterialAction.actor(population,id);
  }
 }
 record Viewer(ClientSession pc,NativePlayerCommit.Binding binding) {}
 record Notice(GameHub hub,ClassicWorldMonsterPopulation population,int map,int instance,
               ClassicWorldMonsterPopulation.Snapshot snapshot,Object actor,boolean removed,List<Viewer> viewers) {
  void publish() {
   if(Thread.holdsLock(hub)||Thread.holdsLock(population))throw new IllegalStateException("NPC publication under scene ownership");
   for(var viewer:viewers) {
    // Take no hub/population monitor across a socket write.
    boolean valid;
    synchronized(hub){synchronized(population){
     Object canonical=NativeCancellationNpcMaterialAction.actor(population,snapshot.objectId());
     valid=!state(hub).closed&&NativeCancellationNpcMaterialAction.population(hub,map,instance)==population
      &&viewer.binding.matches(viewer.pc)&&String.valueOf(CombatAccess.field(viewer.pc,"phase")).equals("WORLD")
      &&(int)CombatAccess.field(viewer.pc,"classicWorldMapId")==map
      &&(int)CombatAccess.field(viewer.pc,"classicWorldInstanceId")==instance
      &&(removed?(canonical==null||canonical==actor&&!(boolean)CombatAccess.field(canonical,"present"))
         :canonical==actor&&(boolean)CombatAccess.field(canonical,"present")&&(boolean)CombatAccess.field(canonical,"alive"));
    }}
    if(!valid)continue;
    try{if(removed)viewer.pc.serverClassicWorldMonsterRemoved(snapshot);else viewer.pc.serverClassicWorldMonsterSpawned(snapshot);}
    catch(IOException failure){System.err.println("[LiTo] cancellation NPC publication failed: "+failure.getClass().getSimpleName());}
   }
  }
 }
 static State state(GameHub hub){
  if(hub.nativeCancellationNpcRuntime==null)hub.nativeCancellationNpcRuntime=new State(hub);
  return hub.nativeCancellationNpcRuntime;
 }
 static Notice notice(GameHub hub,ClassicWorldMonsterPopulation population,int map,int instance,ClassicWorldMonsterPopulation.Snapshot snapshot,Object actor,boolean removed){
  return new Notice(hub,population,map,instance,snapshot,actor,removed,
   hub.classicWorldMembers(map,instance).stream().map(pc->new Viewer(pc,NativePlayerCommit.Binding.capture(pc))).toList());
 }
 static boolean start(State state){
  if(state.closed)return false;
  if(state.task!=null&&!state.task.isDone())return true;
  try{state.task=((ScheduledExecutorService)CombatAccess.field(state.hub,"battleClock")).scheduleWithFixedDelay(()->{try{tick(state);}catch(RuntimeException failure){System.err.println("[LiTo] cancellation NPC cleanup failed: "+failure);}},1,1,TimeUnit.SECONDS);return true;}
  catch(RejectedExecutionException stopped){return false;}
 }
 static void stopEmpty(State state){if(state.leases.isEmpty()&&state.pending.isEmpty()&&state.task!=null){state.task.cancel(false);state.task=null;}}
 static void tick(State state){
  var notices=new ArrayList<Notice>();
  synchronized(state.hub){
   if(state.closed)return;
   notices.addAll(state.pending);state.pending.clear();
   long now=System.nanoTime();
   for(var it=state.leases.iterator();it.hasNext();){
    var lease=it.next();var population=lease.population;
    synchronized(population){
     Object actor=NativeCancellationNpcMaterialAction.actor(population,lease.id);
     if(NativeCancellationNpcMaterialAction.population(state.hub,lease.map,lease.instance)!=population||actor!=lease.actor){it.remove();continue;}
     var snapshot=population.actor(lease.map,lease.id);
     if(lease.respawn){
      if(snapshot==null||snapshot.present()||snapshot.alive()){it.remove();continue;}
      if(now-lease.deadline<0)continue;
      var result=population.respawn(lease.map,lease.id);it.remove();
      if(result!=null){notices.add(notice(state.hub,population,lease.map,lease.instance,result.after(),actor,false));
       for(var member:result.groupMembers())notices.add(notice(state.hub,population,lease.map,lease.instance,member,NativeCancellationNpcMaterialAction.actor(population,member.objectId()),false));}
     }else{
      // Death uses the existing corpse/loot lifecycle. A deadline must not delete its corpse early.
      if(snapshot==null||!snapshot.alive()||!snapshot.present()){it.remove();continue;}
      if(lease.deadline==0||now-lease.deadline<0)continue;
      var removed=population.nativeCancellationDelete(lease.map,lease.id);it.remove();
      if(removed!=null)notices.add(notice(state.hub,population,lease.map,lease.instance,removed,actor,true));
     }
    }
   }
   stopEmpty(state);
  }
  notices.forEach(Notice::publish);
 }
 static void close(GameHub hub){synchronized(hub){var state=state(hub);state.closed=true;
  if(state.task!=null){state.task.cancel(false);state.task=null;}
  // Runtime-created one-shot actors have no persistent rewards or source-table ownership.
  for(var lease:state.leases)if(!lease.respawn)synchronized(lease.population){
   if(NativeCancellationNpcMaterialAction.actor(lease.population,lease.id)==lease.actor)
    lease.population.nativeCancellationDelete(lease.map,lease.id);
  }
  state.leases.clear();state.pending.clear();
 }}
 static ClassicWorldMonsterCatalog.MonsterTemplate template(int id){
  var snapshot=NativeDeNpcTemplateResources.snapshot();var n=snapshot==null?null:snapshot.npcs().get(id);
  if(n==null)return null;
  // Unsupported constructor/executor semantics cannot silently create an approximate NPC.
  if(n.get_randomlevel()!=0||n.get_randomhp()!=0||n.get_randommp()!=0||n.get_randomac()!=0||n.get_randomexp()!=0||n.get_randomlawful()!=0
     ||n.get_classname()!=null&&!n.get_classname().isBlank()&&!n.get_classname().equals("0"))return null;
  try{return new ClassicWorldMonsterCatalog.MonsterTemplate(n.get_npcId(),n.get_nameid(),n.getImpl(),n.get_gfxid(),n.get_level(),n.get_hp(),n.get_mp(),n.get_ac(),n.get_str(),n.get_con(),n.get_dex(),n.get_wis(),n.get_int(),n.get_mr(),n.get_exp(),n.get_lawful(),n.get_size(),n.get_weakAttr(),n.get_ranged(),n.get_passispeed(),n.get_atkspeed(),n.getBowActId(),n.get_undead(),n.get_poisonatk(),n.get_paralysisatk(),n.is_agro(),n.get_damagereduction(),n.is_hard(),n.is_doppel(),n.isBoss(),n.getLightSize(),n.getChangeHead());}
  catch(IllegalArgumentException unsupported){return null;}
 }
 static ClassicWorldMonsterPopulation.MagicCancellation magicWorld(GameHub hub,int map,int instance,long id,ClientSession pc,CombatPoisonLifecycle.Scope scope){
  var population=NativeCancellationNpcMaterialAction.population(hub,map,instance);
  if(population!=null&&Thread.holdsLock(hub)&&Thread.holdsLock(population)){
   var old=population.actor(map,id);var rule=old==null?null:NativeCancellationNpcRules.lookup(old.npcId());
   if(old!=null&&old.alive()&&old.present()&&rule!=null&&rule.type()==2){
    var template=template(rule.targetId());var state=state(hub);
    if(template!=null&&start(state)){
     Object oldActor=NativeCancellationNpcMaterialAction.actor(population,id);
     var replacement=population.nativeCancellationSpawn(map,id,template);
     if(replacement!=null){
      var removed=population.nativeCancellationDelete(map,id);
      if(removed==null){population.nativeCancellationDelete(map,replacement.objectId());throw new IllegalStateException("locked replacement lost original NPC");}
      long now=System.nanoTime();
      state.leases.add(new Lease(population,map,instance,replacement.objectId(),rule.durationSeconds()==0?0:now+TimeUnit.SECONDS.toNanos(rule.durationSeconds()),false));
      int delay=population.nextRespawnDelaySeconds(map,id);
      if(delay>=0)state.leases.add(new Lease(population,map,instance,id,now+TimeUnit.SECONDS.toNanos(delay),true));
      var spawned=notice(hub,population,map,instance,replacement,NativeCancellationNpcMaterialAction.actor(population,replacement.objectId()),false);
      var retired=notice(hub,population,map,instance,removed,oldActor,true);
      NativeLightAction.finishScoped((published,failure)->{
       if(NativeRolePublication.allowPublication(pc)){spawned.publish();retired.publish();}
       else synchronized(hub){if(!state.closed){state.pending.add(spawned);state.pending.add(retired);}}
      });
      return new ClassicWorldMonsterPopulation.MagicCancellation(old,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,new BattleElementalFallDownStatus.Cancellation(false,0),false,false,false,false);
     }
     stopEmpty(state);
    }
   }
  }
  return CombatMonsterCurseLifecycle.magicWorld(hub,map,instance,id,pc,scope);
 }
 private NativeCancellationNpcRuntime(){}
}
