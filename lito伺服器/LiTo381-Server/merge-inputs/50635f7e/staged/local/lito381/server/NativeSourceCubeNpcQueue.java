package local.lito381.server;
import java.util.*;
/** Cube HP joins the actual actor's existing encounter proc FIFO and existing owned worker. */
final class NativeSourceCubeNpcQueue {
 record Ticket(NativeSourceCubeNpc.Commit commit,CombatNpcDamageQueue.Queue queue)implements CombatWorldCommit.Plan{}
 static boolean ticket(CombatNpcDamageQueue.Event event){return event!=null&&event.physical()instanceof Ticket;}
 static boolean envelope(CombatNpcDamageQueue.Event event,NativeSourceCubeNpc.Commit p){return p!=null&&event!=null&&event.physical()instanceof Ticket t&&t.commit()==p&&event.epoch()==0&&event.contributor()==null&&event.player()==0&&event.owner()==0&&event.attacker()==p.lease.effect().objectId()&&event.damage()==p.damage&&"source-cube-effect".equals(event.source())&&"".equals(event.account())&&"".equals(event.name());}
 static void enqueue(NativeSourceCubeNpc.Commit p){
  if(p==null||p.released.get()||!p.admitted)return;
  var scene=p.lease.effect().scene();CombatNpcDamageQueue.Queue queue;
  synchronized(scene.hub()){synchronized(p.target.owner()){
   if(!NativeSourceCubeNpc.nativeSourceCubeQueueOriginalBefore(scene.hub(),scene.id(),p,p.target.id(),p.damage)){cancel(p);return;}
   // This is the original actor's status-owned queue, not a queue selected again by numerical id.
   queue=CombatPoisonLifecycle.monsterDamage((BattleTimedStatus)CombatAccess.field(p.target.actor(),"damagePoison"),true);
  }}
  var event=new CombatNpcDamageQueue.Event(0,System.nanoTime(),null,p.lease.effect().objectId(),0,p.damage,0,"source-cube-effect","","",new Ticket(p,queue));
  synchronized(queue){
   p.nativeSourceCubeQueued=true;
   if(!NativeNpcLifetime.offer(queue,event,scene.hub())){p.nativeSourceCubeQueued=false;cancel(p);return;}
   if(queue.start()){
    var lane=NativeNpcLifetime.lane(queue,scene.hub());var task=NativeProcHealingEncounter.nativeSourceCubeDrain(scene.hub(),queue);lane.task=task;
    NativeNpcLifetime.dispatch(lane,task,0);
   }
  }
 }
 static boolean before(GameHub hub,long room,NativeSourceCubeNpc.Commit p,long id,int damage){return p!=null&&p.nativeSourceCubeQueued&&p.nativeSourceCubeQueueClaimed&&NativeSourceCubeNpc.nativeSourceCubeQueueOriginalBefore(hub,room,p,id,damage);}
 static void cancel(NativeSourceCubeNpc.Commit p){if(p!=null){p.admitted=false;p.released.compareAndSet(false,true);}}
 /** Producer completion does not release an admitted queue event or decrement its independently owned slot. */
 static void producerFinished(NativeSourceCubeNpc.Commit p){if(p!=null&&!p.nativeSourceCubeQueued)cancel(p);}
 /** Executed inside the original drainer's try/finally; original Queue.complete remains its sole completion. */
 static boolean consume(GameHub hub,CombatNpcDamageQueue.Queue queue,CombatNpcDamageQueue.Event event){
  if(!ticket(event))return false;var p=((Ticket)event.physical()).commit();
  if(p==null)return true;
  try{if(envelope(event,p)&&((Ticket)event.physical()).queue()==queue&&queue!=null&&CombatAccess.field(queue,"active")==event&&p.nativeSourceCubeQueued&&!p.released.get()&&p.admitted&&p.lease.effect().scene().hub()==hub){p.nativeSourceCubeQueueClaimed=true;NativeSourceCubeNpc.complete(p);}}
  finally{p.nativeSourceCubeQueueClaimed=false;p.released.compareAndSet(false,true);}
  return true;
 }
 /** Original queue monitor only; no output, HP, actor/Hub/admission lock or new lifetime authority. */
 @SuppressWarnings("unchecked")static void clearing(CombatNpcDamageQueue.Queue queue){
  if(!Thread.holdsLock(queue))throw new IllegalStateException("original NPC queue monitor required");
  for(var event:(ArrayDeque<CombatNpcDamageQueue.Event>)CombatAccess.field(queue,"events"))if(event.physical()instanceof Ticket t)cancel(t.commit());
 }
 /** A reward output failure must not skip the original accepted death/wave/completion tail. */
 static void lawful(ClientSession unused,int npc,String note,NativeSourceCubeNpc.Commit p){try{NativeSourceCubeNpc.nativeSourceCubeQueueOriginalLawful(unused,npc,note,p);}catch(RuntimeException failure){if(p!=null)NativeSourceCubeNpc.failed(p.lifetime);}}
 private NativeSourceCubeNpcQueue(){}
}
