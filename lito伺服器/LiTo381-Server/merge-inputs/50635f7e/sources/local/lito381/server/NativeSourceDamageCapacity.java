package local.lito381.server;
import java.util.concurrent.atomic.AtomicLong;
/** Damage-only queued credits are owned by the original application, separately from AI/in-flight work. */
final class NativeSourceDamageCapacity {
 static long pending(NativePlayerLifetime owner){return owner.nativeSourceDamagePending.get();}
 static boolean empty(AtomicLong damage){return damage.get()==0;}
 static boolean idle(NativePlayerLifetime owner){return empty(owner.nativeSourceDamagePending);}
 static void releaseCounter(AtomicLong damage,long count){for(;;){long n=damage.get();if(count<0||count>n)throw new IllegalStateException("damage capacity receipt mismatch");if(damage.compareAndSet(n,n-count))return;}}
 static void release(NativePlayerLifetime owner,long count){releaseCounter(owner.nativeSourceDamagePending,count);}
 static boolean offer(CombatNpcDamageQueue.Queue queue,CombatNpcDamageQueue.Event event,GameHub hub){
  if(queue==null||event==null)return false;
  synchronized(queue){
   var lane=NativeNpcLifetime.lane(queue,hub);var life=lane.life;var damage=life.nativeSourceDamagePending;
   if(!life.admits()||!NativeSourcePendingAdmission.reserve(damage,NativePlayerCommit.GLOBAL))return false;
   boolean published=false;
   try{
    if(!life.admits()||!queue.offer(event))return false;
    life.pending.incrementAndGet();
    lane.deadlines.put(event,NativePlayerLifetime.deadline());life.npcs.add(lane);published=true;
    if(life.mode==NativePlayerLifetime.Mode.STOPPED){queue.clear(false);return false;}
    return true;
   }finally{if(!published)releaseCounter(damage,1);}
  }
 }
 private NativeSourceDamageCapacity(){}
}
