// DECOMPILED from base-0.6.71.jar, TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import local.lito381.server.NativeDeVisibilityJobs.Credit;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatNpcDamageQueue;
import local.lito381.server.NativeDeChatScope;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeNpcConstructionState;
import local.lito381.server.NativeDeVisibilityJobs;
import local.lito381.server.NativeDeVisibilityRuntime;
import local.lito381.server.NativeDeWorldContext;

final class NativeDeChatJobs {
    static final String FIELD = "sourceNpcChatJobs";
    private static final ThreadLocal<NativeDeVisibilityJobs.Credit> current = new ThreadLocal();

    static Owner find(NativeDeWorldContext nativeDeWorldContext) {
        return (Owner)CombatAccess.field(nativeDeWorldContext, FIELD);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Owner owner(NativeDeWorldContext nativeDeWorldContext) {
        synchronized (nativeDeWorldContext) {
            nativeDeWorldContext.admit();
            Owner owner = NativeDeChatJobs.find(nativeDeWorldContext);
            if (owner == null) {
                owner = new Owner(nativeDeWorldContext);
                CombatAccess.set(nativeDeWorldContext, FIELD, owner);
            }
            return owner;
        }
    }

    static boolean admits(Owner owner, NativeDeNpcConstructionState nativeDeNpcConstructionState) {
        return !owner.closed && !owner.context.closed && owner.context.life.admits() && !nativeDeNpcConstructionState.destroyed() && !nativeDeNpcConstructionState.deletionStarted() && NativeDeWorldContext.entry((NativeDeDeletionPorts.ObjectRef)nativeDeNpcConstructionState).context == owner.context && owner.context.world.isObjectRegistered(nativeDeNpcConstructionState);
    }

    static NativeDeVisibilityJobs.Credit acquire(Owner owner) {
        NativeDeVisibilityJobs.Credit credit = current.get();
        if (credit != null && credit.owner == owner.visibility && credit.retain()) {
            return credit;
        }
        return NativeDeVisibilityJobs.acquire(owner.visibility);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static <T> T with(Owner owner, NativeDeNpcConstructionState nativeDeNpcConstructionState, NativeDeVisibilityJobs.Credit credit, Supplier<T> supplier) {
        NativeDeVisibilityJobs.Credit credit2 = current.get();
        current.set(credit);
        try {
            return (T)NativeDeVisibilityJobs.with(credit, () -> NativeDeChatScope.call(owner, nativeDeNpcConstructionState, supplier));
        }
        finally {
            if (credit2 == null) {
                current.remove();
            } else {
                current.set(credit2);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static <T> T call(Owner owner, NativeDeNpcConstructionState nativeDeNpcConstructionState, Supplier<T> supplier) {
        if (!NativeDeChatJobs.admits(owner, nativeDeNpcConstructionState)) {
            throw new RejectedExecutionException("NPC chat actor retired/unpublished");
        }
        NativeDeVisibilityJobs.Credit credit = NativeDeChatJobs.acquire(owner);
        try {
            return NativeDeChatJobs.with(owner, nativeDeNpcConstructionState, credit, supplier);
        }
        finally {
            credit.release();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Ticket reserve(Owner owner, NativeDeNpcConstructionState nativeDeNpcConstructionState, Runnable runnable, boolean bl, long l) {
        ReentrantReadWriteLock.ReadLock readLock = owner.context.admission.readLock();
        readLock.lock();
        try {
            owner.context.admit();
            if (!NativeDeChatJobs.admits(owner, nativeDeNpcConstructionState)) {
                throw new RejectedExecutionException("NPC chat closed");
            }
            Ticket ticket = new Ticket(owner, nativeDeNpcConstructionState, Objects.requireNonNull(runnable), NativeDeChatJobs.acquire(owner), bl, l);
            owner.tickets.add(ticket);
            return ticket;
        }
        finally {
            readLock.unlock();
        }
    }

    static boolean execute(Owner owner, NativeDeNpcConstructionState nativeDeNpcConstructionState, Runnable runnable) {
        Ticket ticket;
        try {
            ticket = NativeDeChatJobs.reserve(owner, nativeDeNpcConstructionState, runnable, false, 0L);
        }
        catch (RejectedExecutionException rejectedExecutionException) {
            return false;
        }
        try {
            owner.context.life.workers.execute(ticket);
            return true;
        }
        catch (RejectedExecutionException rejectedExecutionException) {
            ticket.cancel(false);
            return false;
        }
        catch (RuntimeException | Error throwable) {
            ticket.cancel(false);
            throw throwable;
        }
    }

    static Ticket schedule(Owner owner, NativeDeNpcConstructionState nativeDeNpcConstructionState, Runnable runnable, long l, long l2) {
        Ticket ticket = NativeDeChatJobs.reserve(owner, nativeDeNpcConstructionState, runnable, true, l2);
        try {
            ticket.install(l2 > 0L ? owner.context.life.retry.scheduleAtFixedRate(ticket, Math.max(0L, l), l2, TimeUnit.MILLISECONDS) : owner.context.life.retry.schedule(ticket, Math.max(0L, l), TimeUnit.MILLISECONDS));
            return ticket;
        }
        catch (RuntimeException | Error throwable) {
            ticket.cancel(false);
            throw throwable;
        }
    }

    static void retire(NativeDeNpcConstructionState nativeDeNpcConstructionState) {
        Owner owner = NativeDeChatJobs.find(NativeDeWorldContext.entry((NativeDeDeletionPorts.ObjectRef)nativeDeNpcConstructionState).context);
        if (owner != null) {
            for (Ticket ticket : (Ticket[])owner.tickets.toArray(Ticket[]::new)) {
                if (ticket.actor == nativeDeNpcConstructionState) {
                    ticket.cancel(false);
                }
            }
        }
    }

    static void close(NativeDeWorldContext nativeDeWorldContext) {
        Owner owner = NativeDeChatJobs.find(nativeDeWorldContext);
        if (owner == null) {
            return;
        }
        owner.closed = true;
        for (Ticket ticket : (Ticket[])owner.tickets.toArray(Ticket[]::new)) {
            ticket.cancel(false);
        }
        CombatAccess.set(nativeDeWorldContext, FIELD, null);
    }

    private NativeDeChatJobs() {
    }

   static final class Owner {
      final NativeDeWorldContext context;
      final NativeDeVisibilityRuntime.Owner visibility;
      final Set<NativeDeChatJobs.Ticket> tickets = ConcurrentHashMap.newKeySet();
      volatile boolean closed;

      Owner(NativeDeWorldContext var1) {
         this.context = var1;
         this.visibility = NativeDeVisibilityRuntime.owner(var1);
      }
   }

   static final class Ticket implements Runnable, ScheduledFuture<Object> {
      final NativeDeChatJobs.Owner owner;
      final NativeDeNpcConstructionState actor;
      final Runnable action;
      final Credit credit;
      final boolean timer;
      final long period;
      volatile ScheduledFuture<?> delegate;
      boolean running;
      boolean ended;
      boolean cancelled;
      boolean discarded;
      Throwable failure;

      Ticket(NativeDeChatJobs.Owner var1, NativeDeNpcConstructionState var2, Runnable var3, Credit var4, boolean var5, long var6) {
         this.owner = var1;
         this.actor = var2;
         this.action = var3;
         this.credit = var4;
         this.timer = var5;
         this.period = var6;
      }

      void install(ScheduledFuture<?> var1) {
         synchronized (this) {
            this.delegate = var1;
            if (this.ended) {
               var1.cancel(false);
            }
         }
      }

      @Override
      public void run() {
         synchronized (this) {
            if (this.ended || this.running) {
               return;
            }

            if (!this.credit.retain()) {
               throw new IllegalStateException("NPC chat receipt lost");
            }

            this.running = true;
         }

         Throwable var1 = null;

         try {
            NativeDeChatJobs.with(this.owner, this.actor, this.credit, () -> {
               if (NativeDeChatJobs.admits(this.owner, this.actor)) {
                  this.action.run();
               } else {
                  this.discard();
               }

               return null;
            });
         } catch (Throwable var15) {
            var1 = var15;
            CombatNpcDamageQueue.failed(var15);
         } finally {
            synchronized (this) {
               this.running = false;
            }

            if (this.period <= 0L || var1 != null || !NativeDeChatJobs.admits(this.owner, this.actor)) {
               this.end(false, var1);
            }

            this.credit.release();
         }
      }

      void discard() {
         synchronized (this) {
            if (this.discarded) {
               return;
            }

            this.discarded = true;
         }

         if (this.action instanceof NativeDeChatScope.Pool.ShutdownDiscardable var1) {
            NativeDeChatScope.call(this.owner, this.actor, () -> {
               var1.discardOnShutdown();
               return null;
            });
         }
      }

      boolean end(boolean var1, Throwable var2) {
         boolean var3;
         synchronized (this) {
            if (this.ended) {
               return false;
            }

            this.ended = true;
            this.cancelled = var1;
            this.failure = var2;
            var3 = !this.running;
            if (this.delegate != null) {
               this.delegate.cancel(false);
            }

            if (!this.timer) {
               this.owner.context.life.workers.remove(this);
            }

            this.notifyAll();
         }

         this.owner.tickets.remove(this);
         this.credit.release();
         if (var3) {
            this.discard();
         }

         return true;
      }

      @Override
      public boolean cancel(boolean var1) {
         return this.end(true, null);
      }

      @Override
      public synchronized boolean isDone() {
         return this.ended;
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
         while (!this.ended) {
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
      public synchronized Object get(long var1, TimeUnit var3) throws InterruptedException, ExecutionException, TimeoutException {
         long var4 = System.nanoTime() + var3.toNanos(var1);

         while (!this.ended) {
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
