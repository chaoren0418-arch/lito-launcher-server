package local.lito381.server;
import java.util.ArrayDeque;import java.util.concurrent.atomic.AtomicLong;
/** One CAS reservation for the existing application capacity; no queue-to-admission lock edge. */
final class NativeSourcePendingAdmission {
 static boolean reserve(AtomicLong counter,long limit){for(;;){long n=counter.get();if(n<0||n>=limit)return false;if(counter.compareAndSet(n,n+1))return true;}}
 @SuppressWarnings({"rawtypes","unchecked"})static boolean add(ArrayDeque events,Object event,AtomicLong reservation){try{return events.add(event);}catch(RuntimeException|Error failure){reservation.decrementAndGet();throw failure;}}
 static boolean offer(CombatNpcDamageQueue.Queue queue,CombatNpcDamageQueue.Event event,GameHub hub){
  if(queue==null||event==null)return false;
  synchronized(queue){
   var lane=NativeNpcLifetime.lane(queue,hub);var life=lane.life;
   if(!life.admits()||!reserve(life.pending,NativePlayerCommit.GLOBAL))return false;
   boolean published=false;
   try{
    if(!life.admits()||!queue.offer(event))return false;
    lane.deadlines.put(event,NativePlayerLifetime.deadline());life.npcs.add(lane);published=true;
    // Registration precedes the terminal fence. An in-flight admission joins a causal drain;
    // STOPPED drops the published tail using the original accounting/cancellation path.
    if(life.mode==NativePlayerLifetime.Mode.STOPPED){queue.clear(false);return false;}
    return true;
   }finally{if(!published)life.pending.decrementAndGet();}
  }
 }
 private NativeSourcePendingAdmission(){}
}
