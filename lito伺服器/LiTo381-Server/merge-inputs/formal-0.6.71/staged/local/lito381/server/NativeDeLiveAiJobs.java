// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;

final class NativeDeLiveAiJobs {
   static final String FIELD = "sourceDeLiveAiJobs";
   static final String BINDING = "sourceDeLiveAiBinding";
   private static final ThreadLocal<NativeDeLiveAiJobs.Ticket> current = new ThreadLocal<>();
   private static final ThreadLocal<NativeDeLiveAiJobs.PulseCredit> pulseCredit = new ThreadLocal<>();

   static NativeDeLiveAiJobs.Binding find(NativeDeInstanceState var0) {
      return (NativeDeLiveAiJobs.Binding)CombatAccess.field(var0, "sourceDeLiveAiBinding");
   }

   static NativeDeLiveAiJobs.Binding binding(NativeDeInstanceState var0) {
      return Objects.requireNonNull(find(var0), "De AI environment not bound");
   }

   static void configure(NativeDeInstanceState var0, NativeDeLiveAiPorts.Environment var1, NativeDeLiveAiPorts.Config var2) {
      Objects.requireNonNull(var1);
      Objects.requireNonNull(var2);
      if (var0.deletionOwner.services instanceof NativeDeWorldContext var3) {
         ReadLock var14 = var3.admission.readLock();
         var14.lock();

         try {
            var3.admit();
            synchronized (var3.life.admission) {
               if (find(var0) != null) {
                  throw new IllegalStateException("De AI binding is immutable");
               }

               NativeDeLiveAiJobs.Owner var6 = (NativeDeLiveAiJobs.Owner)CombatAccess.field(var3, "sourceDeLiveAiJobs");
               if (var6 == null) {
                  var6 = new NativeDeLiveAiJobs.Owner(var3);
                  CombatAccess.set(var3, "sourceDeLiveAiJobs", var6);
               }

               if (var6.closed) {
                  throw new RejectedExecutionException("De AI closed");
               }

               NativeDeLiveAiJobs.Binding var7 = new NativeDeLiveAiJobs.Binding(var0, var6, var1, var2);
               var6.bindings.add(var7);
               CombatAccess.set(var0, "sourceDeLiveAiBinding", var7);
            }
         } finally {
            var14.unlock();
         }
      } else {
         throw new IllegalArgumentException("concrete World required");
      }
   }

   static boolean admits(NativeDeInstanceState var0) {
      NativeDeLiveAiJobs.Binding var1 = find(var0);
      return var1 != null
         && !var1.closed
         && !var1.owner.closed
         && !var1.owner.context.closed
         && var1.owner.life.admits()
         && !var0.destroyed()
         && !var0.deletionStarted()
         && CombatAccess.field(var0, "sourceDeWorldEntry") != null
         && var1.owner.context.world().isObjectRegistered(var0);
   }

   private static NativeDeLiveAiJobs.Credit credit(NativeDeLiveAiJobs.Binding var0) {
      NativeDeLiveAiJobs.Ticket var1 = current.get();
      if (var1 != null && var1.binding == var0 && !var1.finished) {
         var1.credit.refs++;
         return var1.credit;
      } else {
         NativeDeLiveAiJobs.PulseCredit var2 = pulseCredit.get();
         if (var2 != null && var2.binding() == var0) {
            var2.credit().refs++;
            return var2.credit();
         } else if (var0.owner.life.pending.get() >= NativePlayerCommit.GLOBAL) {
            return null;
         } else {
            var0.owner.life.pending.incrementAndGet();
            return new NativeDeLiveAiJobs.Credit(var0.owner);
         }
      }
   }

   private static NativeDeLiveAiJobs.Ticket reserve(NativeDeInstanceState var0, Runnable var1, boolean var2, long var3) {
      NativeDeLiveAiJobs.Binding var5 = find(var0);
      if (var5 == null) {
         return null;
      } else {
         synchronized (var5.owner.life.admission) {
            if (!admits(var0)) {
               var5.owner.rejected++;
               return null;
            } else {
               NativeDeLiveAiJobs.Credit var7 = credit(var5);
               if (var7 == null) {
                  var5.owner.rejected++;
                  return null;
               } else {
                  NativeDeLiveAiJobs.Ticket var8 = new NativeDeLiveAiJobs.Ticket(var5, var1, var7, var2, var3);
                  var5.owner.tickets.add(var8);
                  var5.tasks++;
                  return var8;
               }
            }
         }
      }
   }

   static boolean execute(NativeDeInstanceState var0, Runnable var1) {
      NativeDeLiveAiJobs.Ticket var2 = reserve(var0, var1, false, 0L);
      if (var2 == null) {
         return false;
      } else {
         synchronized (var2) {
            if (var2.finished) {
               return true;
            } else {
               boolean var10000;
               try {
                  var2.binding.owner.life.workers.execute(var2);
                  synchronized (var2.binding.owner.life.admission) {
                     var2.binding.owner.submitted++;
                  }

                  var10000 = true;
               } catch (RejectedExecutionException var8) {
                  var2.finish();
                  return false;
               } catch (RuntimeException | Error var9) {
                  var2.finish();
                  throw var9;
               }

               return var10000;
            }
         }
      }
   }

   static ScheduledFuture<?> schedule(NativeDeInstanceState var0, Runnable var1, long var2, long var4) {
      NativeDeLiveAiJobs.Ticket var6 = reserve(var0, var1, true, var4);
      if (var6 == null) {
         throw new RejectedExecutionException("De AI timer closed or budget exhausted");
      } else {
         try {
            ScheduledFuture var7 = var6.binding.owner.life.retry.schedule(var6, Math.max(0L, var2), TimeUnit.MILLISECONDS);
            var6.install(var7);
            synchronized (var6.binding.owner.life.admission) {
               var6.binding.owner.scheduled++;
            }

            return var6;
         } catch (RuntimeException | Error var11) {
            var6.finish();
            throw var11;
         }
      }
   }

   static void background(NativeDeInstanceState var0, boolean var1) {
      NativeDeLiveAiJobs.Binding var2 = find(var0);
      if (var2 != null) {
         NativeDeLiveAiJobs.Owner var3 = var2.owner;
         ScheduledFuture var4 = null;
         NativeDeLiveAiJobs.Credit var5 = null;
         synchronized (var3.life.admission) {
            if (var1) {
               if (var2.closed || var3.closed || !var3.life.admits()) {
                  return;
               }

               var3.background.add(var2);
               if (var3.heartbeat == null) {
                  NativeDeLiveAiJobs.Credit var7 = credit(var2);
                  if (var7 == null) {
                     var3.background.remove(var2);
                     throw new RejectedExecutionException("De AI heartbeat budget exhausted");
                  }

                  try {
                     var3.heartbeat = var3.life.retry.scheduleAtFixedRate(() -> pulse(var3), 10L, 10L, TimeUnit.SECONDS);
                     var3.heartbeatCredit = var7;
                  } catch (RuntimeException | Error var10) {
                     var3.background.remove(var2);
                     var7.release();
                     throw var10;
                  }
               }
            } else {
               var3.background.remove(var2);
               if (var3.background.isEmpty()) {
                  var4 = var3.heartbeat;
                  var5 = var3.heartbeatCredit;
                  var3.heartbeat = null;
                  var3.heartbeatCredit = null;
               }
            }
         }

         if (var4 != null) {
            var4.cancel(false);
         }

         if (var5 != null) {
            var5.release();
         }
      }
   }

   static void pulse(NativeDeLiveAiJobs.Owner var0) {
      NativeDeLiveAiJobs.Binding[] var1;
      NativeDeLiveAiJobs.Credit var2;
      synchronized (var0.life.admission) {
         if (var0.closed || !var0.life.admits()) {
            return;
         }

         var2 = var0.heartbeatCredit;
         if (var2 == null) {
            return;
         }

         var2.refs++;
         var1 = var0.background.toArray(NativeDeLiveAiJobs.Binding[]::new);
      }

      try {
         for (NativeDeLiveAiJobs.Binding var6 : var1) {
            if (!var6.actor.isDead() && var6.actor.getMap() != null && admits(var6.actor)) {
               NativeDeLiveAiJobs.Credit var7;
               synchronized (var0.life.admission) {
                  if (var0.closed || var6.closed || !var0.life.admits()) {
                     continue;
                  }

                  var7 = credit(var6);
               }

               if (var7 != null) {
                  NativeDeLiveAiJobs.PulseCredit var28 = pulseCredit.get();
                  pulseCredit.set(new NativeDeLiveAiJobs.PulseCredit(var6, var7));

                  try {
                     var6.actor.onBackgroundTick();
                  } catch (Exception var22) {
                     CombatNpcDamageQueue.failed(var22);
                  } finally {
                     if (var28 == null) {
                        pulseCredit.remove();
                     } else {
                        pulseCredit.set(var28);
                     }

                     var7.release();
                  }
               }
            } else {
               background(var6.actor, false);
            }
         }
      } finally {
         var2.release();
      }
   }

   static void retireDeleted(NativeDeInstanceState var0) {
      if (var0.deletionStarted() || var0.destroyed()) {
         retire(var0);
      }
   }

   static void stop(NativeDeDeletionPorts.ObjectRef var0) {
      if (var0 instanceof NativeDeInstanceState var1) {
         retire(var1);
      }
   }

   static void retire(NativeDeInstanceState var0) {
      var0.stopAI();
      NativeDeLiveAiJobs.Binding var1 = find(var0);
      if (var1 != null) {
         background(var0, false);
         NativeDeLiveAiJobs.Ticket[] var2;
         synchronized (var1.owner.life.admission) {
            var1.closed = true;
            var1.owner.bindings.remove(var1);
            var2 = var1.owner.tickets.stream().filter(var1x -> var1x.binding == var1).toArray(NativeDeLiveAiJobs.Ticket[]::new);
         }

         for (NativeDeLiveAiJobs.Ticket var6 : var2) {
            var6.cancel(false);
         }

         synchronized (var1.owner.life.admission) {
            if (var1.tasks == 0) {
               CombatAccess.set(var0, "sourceDeLiveAiBinding", null);
            }
         }
      }
   }

   static void close(NativeDeWorldContext var0) {
      NativeDeLiveAiJobs.Owner var1 = (NativeDeLiveAiJobs.Owner)CombatAccess.field(var0, "sourceDeLiveAiJobs");
      if (var1 != null) {
         NativeDeLiveAiJobs.Binding[] var2;
         NativeDeLiveAiJobs.Ticket[] var3;
         ScheduledFuture var4;
         NativeDeLiveAiJobs.Credit var5;
         synchronized (var1.life.admission) {
            var1.closed = true;
            var2 = var1.bindings.toArray(NativeDeLiveAiJobs.Binding[]::new);

            for (NativeDeLiveAiJobs.Binding var10 : var2) {
               var10.closed = true;
            }

            var3 = var1.tickets.toArray(NativeDeLiveAiJobs.Ticket[]::new);
            var1.bindings.clear();
            var1.background.clear();
            var4 = var1.heartbeat;
            var5 = var1.heartbeatCredit;
            var1.heartbeat = null;
            var1.heartbeatCredit = null;
         }

         if (var4 != null) {
            var4.cancel(false);
         }

         if (var5 != null) {
            var5.release();
         }

         for (NativeDeLiveAiJobs.Binding var24 : var2) {
            NativeDeLiveAiScope.call(var24.actor, () -> {
               var24.actor.stopAI();
               return null;
            });
         }

         for (NativeDeLiveAiJobs.Ticket var25 : var3) {
            var25.cancel(false);
         }

         synchronized (var1.life.admission) {
            for (NativeDeLiveAiJobs.Binding var27 : var2) {
               if (var27.tasks == 0) {
                  CombatAccess.set(var27.actor, "sourceDeLiveAiBinding", null);
               }
            }
         }

         CombatAccess.set(var0, "sourceDeLiveAiJobs", null);
      }
   }

   static NativeDeLiveAiJobs.Snapshot snapshot(NativeDeWorldContext var0) {
      synchronized (var0.life.admission) {
         NativeDeLiveAiJobs.Owner var2 = (NativeDeLiveAiJobs.Owner)CombatAccess.field(var0, "sourceDeLiveAiJobs");
         return var2 == null
            ? new NativeDeLiveAiJobs.Snapshot(false, true, 0, 0, 0, 0L, 0L, 0L, 0L)
            : new NativeDeLiveAiJobs.Snapshot(
               true,
               var2.closed,
               var2.bindings.size(),
               var2.tickets.size(),
               var2.background.size(),
               var2.submitted,
               var2.scheduled,
               var2.completed,
               var2.rejected
            );
      }
   }

   private NativeDeLiveAiJobs() {
   }

   static final class Binding {
      final NativeDeInstanceState actor;
      final NativeDeLiveAiJobs.Owner owner;
      final NativeDeLiveAiPorts.Environment environment;
      final NativeDeLiveAiPorts.Config config;
      volatile boolean closed;
      int tasks;

      Binding(NativeDeInstanceState var1, NativeDeLiveAiJobs.Owner var2, NativeDeLiveAiPorts.Environment var3, NativeDeLiveAiPorts.Config var4) {
         this.actor = var1;
         this.owner = var2;
         this.environment = var3;
         this.config = var4;
      }
   }

   static final class Credit {
      final NativeDeLiveAiJobs.Owner owner;
      int refs = 1;

      Credit(NativeDeLiveAiJobs.Owner var1) {
         this.owner = var1;
      }

      void release() {
         synchronized (this.owner.life.admission) {
            if (--this.refs == 0 && this.owner.life.pending.decrementAndGet() < 0L) {
               throw new IllegalStateException("De AI credit underflow");
            } else if (this.refs < 0) {
               throw new IllegalStateException("De AI reference underflow");
            }
         }
      }
   }

   static final class Owner {
      final NativeDeWorldContext context;
      final NativePlayerLifetime life;
      final Set<NativeDeLiveAiJobs.Ticket> tickets = new HashSet<>();
      final Set<NativeDeLiveAiJobs.Binding> bindings = Collections.newSetFromMap(new IdentityHashMap<>());
      final Set<NativeDeLiveAiJobs.Binding> background = Collections.newSetFromMap(new IdentityHashMap<>());
      boolean closed;
      ScheduledFuture<?> heartbeat;
      NativeDeLiveAiJobs.Credit heartbeatCredit;
      long submitted;
      long scheduled;
      long completed;
      long rejected;

      Owner(NativeDeWorldContext var1) {
         this.context = var1;
         this.life = var1.life;
      }
   }

   private record PulseCredit(NativeDeLiveAiJobs.Binding binding, NativeDeLiveAiJobs.Credit credit) {
   }

   record Snapshot(boolean present, boolean closed, int bindings, int tasks, int background, long submitted, long scheduled, long completed, long rejected) {
   }

   static final class Ticket implements Runnable, ScheduledFuture<Object> {
      final NativeDeLiveAiJobs.Binding binding;
      final Runnable action;
      final NativeDeLiveAiJobs.Credit credit;
      final boolean timer;
      final long ownerToken;
      volatile ScheduledFuture<?> delegate;
      boolean running;
      boolean cancelled;
      volatile boolean finished;
      volatile Throwable failure;

      Ticket(NativeDeLiveAiJobs.Binding var1, Runnable var2, NativeDeLiveAiJobs.Credit var3, boolean var4, long var5) {
         this.binding = var1;
         this.action = var2;
         this.credit = var3;
         this.timer = var4;
         this.ownerToken = var5;
      }

      void install(ScheduledFuture<?> var1) {
         boolean var2;
         synchronized (this) {
            this.delegate = var1;
            var2 = this.finished;
         }

         if (var2) {
            var1.cancel(false);
         }
      }

      @Override
      public void run() {
         synchronized (this) {
            if (this.finished) {
               return;
            }

            this.running = true;
         }

         NativeDeLiveAiJobs.Ticket var1 = NativeDeLiveAiJobs.current.get();
         NativeDeLiveAiJobs.current.set(this);

         try {
            this.binding.owner.life.executeCommitted(() -> {
               if (NativeDeLiveAiJobs.admits(this.binding.actor)) {
                  NativeDeLiveAiScope.call(this.binding.actor, () -> {
                     this.action.run();
                     return null;
                  });
               } else {
                  this.discard();
                  NativeDeLiveAiJobs.retire(this.binding.actor);
               }
            });
         } catch (Throwable var15) {
            this.failure = var15;
            CombatNpcDamageQueue.failed(var15);
         } finally {
            if (var1 == null) {
               NativeDeLiveAiJobs.current.remove();
            } else {
               NativeDeLiveAiJobs.current.set(var1);
            }

            synchronized (this) {
               this.running = false;
            }

            this.finish();
         }
      }

      void discard() {
         if (this.action instanceof NativeDeLiveAiScope.Pool.ShutdownDiscardable var1) {
            NativeDeLiveAiScope.call(this.binding.actor, () -> {
               var1.discardOnShutdown();
               return null;
            });
         } else if (this.timer) {
            NativeDeLiveAiScope.call(this.binding.actor, () -> {
               CombatAccess.call(this.binding.actor, "stopAiOwner", "(J)V", this.ownerToken);
               return null;
            });
         }
      }

      void finish() {
         synchronized (this) {
            if (this.finished) {
               return;
            }

            this.finished = true;
            this.notifyAll();
         }

         synchronized (this.binding.owner.life.admission) {
            this.binding.owner.tickets.remove(this);
            this.binding.tasks--;
            this.binding.owner.completed++;
            if (this.binding.closed && this.binding.tasks == 0) {
               CombatAccess.set(this.binding.actor, "sourceDeLiveAiBinding", null);
            }
         }

         this.credit.release();
      }

      @Override
      public boolean cancel(boolean var1) {
         synchronized (this) {
            if (this.finished || this.running) {
               return false;
            }

            this.cancelled = true;
            ScheduledFuture var3 = this.delegate;
            if (var3 != null && var3.cancel(false)) {
               this.binding.owner.life.cancelledTimers.incrementAndGet();
            }

            if (!this.timer) {
               this.binding.owner.life.workers.remove(this);
            }

            this.finish();
         }

         if (!this.timer) {
            this.discard();
         }

         return true;
      }

      @Override
      public synchronized boolean isDone() {
         return this.finished;
      }

      @Override
      public synchronized boolean isCancelled() {
         return this.cancelled;
      }

      @Override
      public long getDelay(TimeUnit var1) {
         ScheduledFuture var2 = this.delegate;
         return var2 == null ? 0L : var2.getDelay(var1);
      }

      public int compareTo(Delayed var1) {
         return Long.compare(this.getDelay(TimeUnit.NANOSECONDS), var1.getDelay(TimeUnit.NANOSECONDS));
      }

      @Override
      public synchronized Object get() throws InterruptedException, ExecutionException {
         while (!this.finished) {
            this.wait();
         }

         if (this.cancelled) {
            throw new CancellationException();
         } else if (this.failure != null) {
            throw new ExecutionException(this.failure);
         } else {
            return null;
         }
      }

      @Override
      public synchronized Object get(long var1, TimeUnit var3) throws InterruptedException, TimeoutException, ExecutionException {
         long var4 = System.nanoTime() + var3.toNanos(var1);

         while (!this.finished) {
            long var6 = var4 - System.nanoTime();
            if (var6 <= 0L) {
               throw new TimeoutException();
            }

            TimeUnit.NANOSECONDS.timedWait(this, var6);
         }

         if (this.cancelled) {
            throw new CancellationException();
         } else if (this.failure != null) {
            throw new ExecutionException(this.failure);
         } else {
            return null;
         }
      }
   }
}
