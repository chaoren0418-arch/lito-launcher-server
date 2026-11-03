// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/cancellation-battle-runtime/NativeCancellationBattleRuntime.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

/** Source type-2 spawn-before-delete, bounded by encounter ownership and hub lifetime. */
final class NativeCancellationBattleRuntime {
 static final String P="local.lito381.server.";
 static final class State {
  final GameHub hub;final List<Lease> leases=new ArrayList<>();final List<Notice> pending=new ArrayList<>();
  ScheduledFuture<?> task;boolean closed;
  State(GameHub hub){this.hub=hub;}
 }
 static final class Lease {
  final long room,id;final Object roomObject,encounter,actor;final int kind,force;final List<Object> actors;final long deadline;
  Lease(long room,Object roomObject,Object encounter,Object actor,int kind,int force,List<Object> actors,long deadline){this.room=room;this.roomObject=roomObject;this.encounter=encounter;this.actor=actor;this.kind=kind;this.force=force;this.actors=actors;this.deadline=deadline;id=(long)CombatAccess.field(actor,"objectId");}
 }
 record Viewer(ClientSession pc,NativePlayerCommit.Binding binding){}
 record Notice(State state,Lease lease,boolean removed,List<Viewer> viewers,byte[] packet) {
  void publish(){
   if(Thread.holdsLock(state.hub)||Thread.holdsLock(lease.encounter))throw new IllegalStateException("battle NPC publication under scene ownership");
   for(var v:viewers){var output=new Runnable[1];NativeSourcePlayerActionLock.run(v.pc,()->{if(v.binding.matches(v.pc))output[0]=NativeRolePublication.completion(v.pc,()->{
    boolean valid;
    synchronized(state.hub){synchronized(lease.encounter){
     valid=!state.closed&&room(state.hub,lease.room)==lease.roomObject&&encounter(lease.roomObject,lease.kind)==lease.encounter
      &&v.binding.matches(v.pc)&&String.valueOf(CombatAccess.field(v.pc,"phase")).equals("BATTLE")&&(long)CombatAccess.field(v.pc,"currentRoomId")==lease.room
      &&(lease.kind==2||state.hub.battleRecipients(lease.room,lease.force).contains(v.pc))
      &&(removed?!lease.actors.contains(lease.actor):lease.actors.contains(lease.actor)&&(int)CombatAccess.field(lease.actor,"hp")>0);
    }}
    if(valid)try{v.pc.nativeSelfOriginalSend(packet,"S_NPC battle cancellation "+(removed?"remove":"spawn")+"="+lease.id);}catch(IOException e){throw new UncheckedIOException(e);}
   });});if(output[0]!=null)output[0].run();}
  }
 }
 static Object room(GameHub hub,long id){return ((Map<?,?>)CombatAccess.field(hub,"rooms")).get(id);}
 static Object encounter(Object room,int kind){return room==null?null:CombatAccess.field(room,kind==1?"questEncounter":"timeAttackEncounter");}
 static State state(GameHub hub){if(hub.nativeCancellationBattleRuntime==null)hub.nativeCancellationBattleRuntime=new State(hub);return hub.nativeCancellationBattleRuntime;}
 static boolean start(State s){
  if(s.closed)return false;if(s.task!=null&&!s.task.isDone())return true;
  try{s.task=((ScheduledExecutorService)CombatAccess.field(s.hub,"battleClock")).scheduleWithFixedDelay(()->{try{tick(s);}catch(RuntimeException e){System.err.println("[LiTo] battle NPC expiry failed: "+e);}},1,1,TimeUnit.SECONDS);return true;}
  catch(RejectedExecutionException stopped){return false;}
 }
 static void stopEmpty(State s){if(s.leases.isEmpty()&&s.pending.isEmpty()&&s.task!=null){s.task.cancel(false);s.task=null;}}
 static List<Viewer> viewers(State s,Lease l){return (l.kind==1?s.hub.battleRecipients(l.room,l.force):s.hub.roomMembers(l.room)).stream().map(pc->new Viewer(pc,NativePlayerCommit.Binding.capture(pc))).toList();}
 static Notice notice(State s,Lease l,boolean removed){
  byte[] packet=removed?Protocol.removeObject(l.id):l.kind==1?Protocol.questMonster((BattleQuestEncounter.Snapshot)snapshot(l.actor,1)):Protocol.timeAttackActor((BattleTimeAttackEncounter.Snapshot)snapshot(l.actor,2));
  return new Notice(s,l,removed,viewers(s,l),packet);
 }
 static Object snapshot(Object actor,int kind){return CombatAccess.call(actor,"snapshot","()Llocal/lito381/server/"+(kind==1?"BattleQuestEncounter":"BattleTimeAttackEncounter")+"$Snapshot;");}
 static void retire(Object actor){
  // Deletion is not a combat kill: no damage, corpse, score, loot or wave-completion producer.
  ((BattleTimedStatus)CombatAccess.field(actor,"damagePoison")).clear();
  ((RuntimeInventory)CombatAccess.field(actor,"inventory")).clear();
  ((Map<?,?>)CombatAccess.field(actor,"skillReadyNanos")).clear();((Map<?,?>)CombatAccess.field(actor,"skillUseCounts")).clear();
  CombatAccess.set(actor,"hp",0);CombatAccess.set(actor,"mp",0);CombatAccess.set(actor,"poisonAttackerObjectId",0L);
 }
 static void tick(State s){
  var outputs=new ArrayList<Notice>();
  synchronized(s.hub){if(s.closed)return;outputs.addAll(s.pending);s.pending.clear();long now=System.nanoTime();
   for(var it=s.leases.iterator();it.hasNext();){var l=it.next();synchronized(l.encounter){
    boolean retired=room(s.hub,l.room)!=l.roomObject||encounter(l.roomObject,l.kind)!=l.encounter||!(boolean)CombatAccess.field(l.roomObject,"battleStarted");
    if(retired){if(l.actors.remove(l.actor))retire(l.actor);it.remove();continue;}
    // Existing death/corpse and encounter-reset paths own their own output and rewards.
    if(!l.actors.contains(l.actor)||(int)CombatAccess.field(l.actor,"hp")<=0){it.remove();continue;}
    if(l.deadline==0||now-l.deadline<0)continue;
    l.actors.remove(l.actor);retire(l.actor);it.remove();outputs.add(notice(s,l,true));
   }}stopEmpty(s);
  }outputs.forEach(Notice::publish);
 }
 static void close(GameHub hub){synchronized(hub){var s=state(hub);s.closed=true;if(s.task!=null){s.task.cancel(false);s.task=null;}
  for(var l:s.leases)synchronized(l.encounter){if(l.actors.remove(l.actor))retire(l.actor);}s.leases.clear();s.pending.clear();
 }}
 static Object construct(String name,Class<?>[] types,Object...args){try{var ctor=Class.forName(P+name).getDeclaredConstructor(types);ctor.setAccessible(true);return ctor.newInstance(args);}catch(ReflectiveOperationException e){throw new IllegalStateException("battle NPC constructor contract",e);}}
 @SuppressWarnings("unchecked") static List<Object> actors(Object encounter,int kind,Object old){
  if(kind==2)return (List<Object>)CombatAccess.field(encounter,"actors");
  for(var lane:(Object[])CombatAccess.field(encounter,"lanes")){var actors=(List<Object>)CombatAccess.field(lane,"monsters");if(actors.contains(old))return actors;}return null;
 }
 static boolean replace(GameHub hub,long roomId,ClientSession pc,long id,int kind){
  var target=NativeCancellationBattleAction.current.get();
  if(target==null||target.id()!=id||target.npc().kind()!=kind||!target.valid())return false;
  var room=room(hub,roomId);var owner=target.npc().owner();var old=target.npc().actor();
  if(room==null||!Thread.holdsLock(hub)||!Thread.holdsLock(owner)||!(boolean)CombatAccess.field(room,"battleStarted")||!((Set<?>)CombatAccess.field(room,"battleRoster")).contains(pc))return false;
  var oldSnapshot=snapshot(old,kind);int npcId=kind==1?((BattleQuestEncounter.Snapshot)oldSnapshot).npcId():((BattleTimeAttackEncounter.Snapshot)oldSnapshot).npcId();
  var rule=NativeCancellationNpcRules.lookup(npcId);if(rule==null||rule.type()!=2)return false;
  var template=NativeCancellationNpcRuntime.template(rule.targetId());
  var raw=NativeDeNpcTemplateResources.snapshot();var n=raw==null?null:raw.npcs().get(rule.targetId());
  if(template==null||n==null||!"L1Monster".equals(n.getImpl()))return false;
  var list=actors(owner,kind,old);if(list==null||!list.contains(old))return false;
  int x=(int)CombatAccess.field(old,"x"),y=(int)CombatAccess.field(old,"y"),force=kind==1?(int)CombatAccess.field(old,"force"):0;
  Object created;
  if(kind==1){var t=new BattleQuestEncounter.Template(n.get_npcId(),n.get_gfxid(),n.get_nameid(),n.get_level(),n.get_hp(),n.get_mp(),n.get_int());
   created=construct("BattleQuestEncounter$Monster",new Class<?>[]{int.class,int.class,BattleQuestEncounter.Template.class,int.class,int.class,boolean.class,String.class},force,(int)CombatAccess.field(old,"roomNumber"),t,x,y,(boolean)CombatAccess.field(old,"summoned"),"Source CANCELLATION type2");
  }else{var t=new BattleTimeAttackEncounter.Template(n.get_npcId(),n.get_gfxid(),n.get_nameid(),n.get_level(),n.get_hp(),n.get_mp(),n.get_int());
   created=construct("BattleTimeAttackEncounter$Monster",new Class<?>[]{BattleTimeAttackEncounter.Template.class,int.class,int.class,String.class,String.class},t,x,y,"Source CANCELLATION type2",CombatAccess.field(old,"waveSource"));
   var a=(BattleTimeAttackEncounter.Snapshot)snapshot(created,2);
   var framed=new BattleTimeAttackEncounter.Snapshot(a.objectId(),a.npcId(),a.x(),a.y(),a.gfx(),a.name(),a.level(),a.hp(),a.maxHp(),a.alive(),(boolean)CombatAccess.field(old,"finalBoss"),a.activeAi(),a.currentMp(),a.maximumMp(),a.intelligence(),a.identitySource(),a.ordinaryWaveSource(),a.placementSource());
   created=construct("BattleTimeAttackEncounter$Monster",new Class<?>[]{BattleTimeAttackEncounter.Snapshot.class},framed);
   CombatAccess.set(created,"hp",n.get_hp());
  }
  long newId=(long)CombatAccess.field(created,"objectId");if(newId<=0||newId>0xffffffffL||list.stream().anyMatch(a->(long)CombatAccess.field(a,"objectId")==newId))return false;
  var s=state(hub);
  var next=new Lease(roomId,room,owner,created,kind,force,list,rule.durationSeconds()==0?0:System.nanoTime()+TimeUnit.SECONDS.toNanos(rule.durationSeconds()));
  var prior=new Lease(roomId,room,owner,old,kind,force,list,0);
  // Serialize and validate the native sprite before publishing either mutation.
  final Notice spawned,removed;
  try{spawned=notice(s,next,false);removed=notice(s,prior,true);}
  catch(IllegalArgumentException|IllegalStateException unsupportedSprite){return false;}
  if(!start(s))return false;
  list.add(created);list.remove(old);retire(old);s.leases.add(next);
  NativeLightAction.finishScoped((published,failure)->{
   if(NativeRolePublication.allowPublication(pc)){spawned.publish();removed.publish();}
   else synchronized(hub){if(!s.closed){s.pending.add(spawned);s.pending.add(removed);}}
  });return true;
 }
 static BattleQuestEncounter.MagicCancellation cancelQuestMonsterMagic(GameHub hub,long room,ClientSession pc,long id){
  var old=hub.questMonster(room,id);if(old!=null&&replace(hub,room,pc,id,1))return new BattleQuestEncounter.MagicCancellation(old,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,new BattleElementalFallDownStatus.Cancellation(false,0),false,false);
  return hub.cancelQuestMonsterMagic(room,pc,id);
 }
 static BattleTimeAttackEncounter.MagicCancellation cancelTimeAttackActorMagic(GameHub hub,long room,ClientSession pc,long id){
  var old=hub.timeAttackActor(room,id);if(old!=null&&replace(hub,room,pc,id,2))return new BattleTimeAttackEncounter.MagicCancellation(old,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,new BattleElementalFallDownStatus.Cancellation(false,0),false,false);
  return hub.cancelTimeAttackActorMagic(room,pc,id);
 }
 private NativeCancellationBattleRuntime(){}
}
