package local.lito381.server;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Actor-owned value FIFO. The bounded worker queue retains only admitted, finite dispatches. */
final class CombatNpcDamageQueue {
 static final int TARGET_LIMIT=Math.max(64,Math.min(65536,Integer.getInteger("lineage.damage.maxTargetPending",4096)));
 static final long GLOBAL_LIMIT=Math.max(TARGET_LIMIT,Math.min(1048576L,Long.getLong("lineage.damage.maxGlobalPending",65536L)));
 static final int CORES=Math.max(1,Runtime.getRuntime().availableProcessors()),WORKERS=bounded("lineage.damage.commitWorkers",Math.max(1,Math.min(4,CORES/2)),1,Math.min(4,CORES)),DISPATCH_CAPACITY=bounded("lineage.damage.commitDispatchCapacity",8192,256,65536);
 private static int bounded(String key,int fallback,int minimum,int maximum){return Math.max(minimum,Math.min(maximum,Integer.getInteger(key,fallback)));}
 private static final AtomicLong pending=new AtomicLong(),failures=new AtomicLong(),rejections=new AtomicLong();
 private static final ThreadPoolExecutor executor=new ThreadPoolExecutor(WORKERS,WORKERS,30,TimeUnit.SECONDS,new ArrayBlockingQueue<>(DISPATCH_CAPACITY),task->{var thread=new Thread(task,"LiTo-NPC-damage-commit");thread.setDaemon(true);return thread;},new ThreadPoolExecutor.AbortPolicy());
 static {executor.allowCoreThreadTimeOut(true);}
 record Event(long epoch,long now,ClassicWorldMonsterPopulation.ContributorKind contributor,long attacker,long player,int damage,long owner,String source,String account,String name,CombatWorldCommit.Plan physical){
  Event(long epoch,long now,ClassicWorldMonsterPopulation.ContributorKind contributor,long attacker,long player,int damage,long owner,String source,String account,String name){this(epoch,now,contributor,attacker,player,damage,owner,source,account,name,null);}
 }
 static final class Queue {
  private final ArrayDeque<Event> events=new ArrayDeque<>();private Event active;private boolean dispatching,closed;private int count;
  synchronized boolean offer(Event event){if(event==null||closed||count>=TARGET_LIMIT)return false;while(true){long old=pending.get();if(old>=GLOBAL_LIMIT)return false;if(pending.compareAndSet(old,old+1))break;}events.add(Objects.requireNonNull(event));count++;return true;}
  synchronized boolean start(){if(closed||dispatching||events.isEmpty())return false;dispatching=true;return true;}
  synchronized void idle(){dispatching=false;notifyAll();}
  synchronized boolean needsDispatch(){return !closed&&!dispatching&&!events.isEmpty();}
  synchronized Event claim(){if(active!=null)throw new IllegalStateException("overlapping NPC HP commit");active=closed?null:events.poll();if(active==null){dispatching=false;notifyAll();}return active;}
  synchronized void complete(Event event){if(active!=event||event==null)throw new IllegalStateException("NPC commit receipt ownership");active=null;count--;pending.decrementAndGet();notifyAll();}
  synchronized void clear(boolean terminal){if(terminal){closed=true;if(active==null)dispatching=false;}int dropped=events.size();events.clear();count-=dropped;pending.addAndGet(-dropped);notifyAll();}
  synchronized int pending(){return count;}
  synchronized boolean closed(){return closed;}
  synchronized boolean awaitIdle(long millis)throws InterruptedException{long until=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(millis);while(dispatching||active!=null){long remaining=until-System.nanoTime();if(remaining<=0)return false;TimeUnit.NANOSECONDS.timedWait(this,remaining);}return true;}
 }
 static void cancel(java.util.function.Predicate<Runnable> owned){executor.getQueue().removeIf(owned);}
 static boolean submit(Runnable task){try{executor.execute(task);return true;}catch(RejectedExecutionException e){rejections.incrementAndGet();return false;}}
 static void failed(Throwable failure){long count=failures.incrementAndGet();if((count&(count-1))==0)System.err.println("[LiTo] NPC accepted damage publication/commit failed count="+count+": "+failure);}
 static long pending(){return pending.get();}static long failures(){return failures.get();}static long rejections(){return rejections.get();}
}
