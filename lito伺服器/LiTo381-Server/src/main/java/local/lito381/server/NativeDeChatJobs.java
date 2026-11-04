// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

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
        NativeDeWorldContext nativeDeWorldContext2 = nativeDeWorldContext;
        synchronized (nativeDeWorldContext2) {
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
            Object object = NativeDeVisibilityJobs.with(credit, () -> NativeDeChatScope.call(owner, nativeDeNpcConstructionState, supplier));
            return (T)object;
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
            T t = NativeDeChatJobs.with(owner, nativeDeNpcConstructionState, credit, supplier);
            return t;
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
            Ticket ticket2 = ticket;
            return ticket2;
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
        catch (Error | RuntimeException throwable) {
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
        catch (Error | RuntimeException throwable) {
            ticket.cancel(false);
            throw throwable;
        }
    }

    static void retire(NativeDeNpcConstructionState nativeDeNpcConstructionState) {
        Owner owner = NativeDeChatJobs.find(NativeDeWorldContext.entry((NativeDeDeletionPorts.ObjectRef)nativeDeNpcConstructionState).context);
        if (owner != null) {
            for (Ticket ticket : (Ticket[])owner.tickets.toArray(Ticket[]::new)) {
                if (ticket.actor != nativeDeNpcConstructionState) continue;
                ticket.cancel(false);
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
        final Set<Ticket> tickets = ConcurrentHashMap.newKeySet();
        volatile boolean closed;

        Owner(NativeDeWorldContext nativeDeWorldContext) {
            this.context = nativeDeWorldContext;
            this.visibility = NativeDeVisibilityRuntime.owner(nativeDeWorldContext);
        }
    }

    static final class Ticket
    implements Runnable,
    ScheduledFuture<Object> {
        final Owner owner;
        final NativeDeNpcConstructionState actor;
        final Runnable action;
        final NativeDeVisibilityJobs.Credit credit;
        final boolean timer;
        final long period;
        volatile ScheduledFuture<?> delegate;
        boolean running;
        boolean ended;
        boolean cancelled;
        boolean discarded;
        Throwable failure;

        Ticket(Owner owner, NativeDeNpcConstructionState nativeDeNpcConstructionState, Runnable runnable, NativeDeVisibilityJobs.Credit credit, boolean bl, long l) {
            this.owner = owner;
            this.actor = nativeDeNpcConstructionState;
            this.action = runnable;
            this.credit = credit;
            this.timer = bl;
            this.period = l;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        void install(ScheduledFuture<?> scheduledFuture) {
            Ticket ticket = this;
            synchronized (ticket) {
                this.delegate = scheduledFuture;
                if (this.ended) {
                    scheduledFuture.cancel(false);
                }
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void run() {
            Object object = this;
            synchronized (object) {
                if (this.ended || this.running) {
                    return;
                }
                if (!this.credit.retain()) {
                    throw new IllegalStateException("NPC chat receipt lost");
                }
                this.running = true;
            }
            object = null;
            try {
                NativeDeChatJobs.with(this.owner, this.actor, this.credit, () -> {
                    if (NativeDeChatJobs.admits(this.owner, this.actor)) {
                        this.action.run();
                    } else {
                        this.discard();
                    }
                    return null;
                });
            }
            catch (Throwable throwable) {
                object = throwable;
                CombatNpcDamageQueue.failed(throwable);
            }
            finally {
                Ticket ticket = this;
                synchronized (ticket) {
                    this.running = false;
                }
                if (this.period <= 0L || object != null || !NativeDeChatJobs.admits(this.owner, this.actor)) {
                    this.end(false, (Throwable)object);
                }
                this.credit.release();
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        void discard() {
            Runnable runnable = this;
            synchronized (runnable) {
                if (this.discarded) {
                    return;
                }
                this.discarded = true;
            }
            Runnable runnable2 = this.action;
            if (runnable2 instanceof NativeDeChatScope.Pool.ShutdownDiscardable) {
                NativeDeChatScope.Pool.ShutdownDiscardable discardable = (NativeDeChatScope.Pool.ShutdownDiscardable)runnable2;
                NativeDeChatScope.call(this.owner, this.actor, () -> {
                    discardable.discardOnShutdown();
                    return null;
                });
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        boolean end(boolean bl, Throwable throwable) {
            boolean bl2;
            Ticket ticket = this;
            synchronized (ticket) {
                if (this.ended) {
                    return false;
                }
                this.ended = true;
                this.cancelled = bl;
                this.failure = throwable;
                boolean bl3 = bl2 = !this.running;
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
            if (bl2) {
                this.discard();
            }
            return true;
        }

        @Override
        public boolean cancel(boolean bl) {
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
        public long getDelay(TimeUnit timeUnit) {
            ScheduledFuture<?> scheduledFuture = this.delegate;
            return scheduledFuture == null ? 0L : scheduledFuture.getDelay(timeUnit);
        }

        @Override
        public int compareTo(Delayed delayed) {
            return Long.compare(this.getDelay(TimeUnit.NANOSECONDS), delayed.getDelay(TimeUnit.NANOSECONDS));
        }

        @Override
        public synchronized Object get() throws InterruptedException, ExecutionException {
            while (!this.ended) {
                this.wait();
            }
            if (this.cancelled) {
                throw new CancellationException();
            }
            if (this.failure != null) {
                throw new ExecutionException(this.failure);
            }
            return null;
        }

        @Override
        public synchronized Object get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
            long l2 = System.nanoTime() + timeUnit.toNanos(l);
            while (!this.ended) {
                long l3 = l2 - System.nanoTime();
                if (l3 <= 0L) {
                    throw new TimeoutException();
                }
                TimeUnit.NANOSECONDS.timedWait(this, l3);
            }
            if (this.cancelled) {
                throw new CancellationException();
            }
            if (this.failure != null) {
                throw new ExecutionException(this.failure);
            }
            return null;
        }

    }
}
