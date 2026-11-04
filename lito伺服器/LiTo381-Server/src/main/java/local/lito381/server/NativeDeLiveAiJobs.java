// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
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
import java.util.concurrent.locks.ReentrantReadWriteLock;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatNpcDamageQueue;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeInstanceState;
import local.lito381.server.NativeDeLiveAiPorts;
import local.lito381.server.NativeDeLiveAiScope;
import local.lito381.server.NativeDeWorldContext;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativePlayerLifetime;

final class NativeDeLiveAiJobs {
    static final String FIELD = "sourceDeLiveAiJobs";
    static final String BINDING = "sourceDeLiveAiBinding";
    private static final ThreadLocal<Ticket> current = new ThreadLocal();
    private static final ThreadLocal<PulseCredit> pulseCredit = new ThreadLocal();

    static Binding find(NativeDeInstanceState nativeDeInstanceState) {
        return (Binding)CombatAccess.field(nativeDeInstanceState, BINDING);
    }

    static Binding binding(NativeDeInstanceState nativeDeInstanceState) {
        return Objects.requireNonNull(NativeDeLiveAiJobs.find(nativeDeInstanceState), "De AI environment not bound");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void configure(NativeDeInstanceState nativeDeInstanceState, NativeDeLiveAiPorts.Environment environment, NativeDeLiveAiPorts.Config config) {
        Objects.requireNonNull(environment);
        Objects.requireNonNull(config);
        Object object = nativeDeInstanceState.deletionOwner.services;
        if (!(object instanceof NativeDeWorldContext)) {
            throw new IllegalArgumentException("concrete World required");
        }
        NativeDeWorldContext nativeDeWorldContext = (NativeDeWorldContext)object;
        object = nativeDeWorldContext.admission.readLock();
        ((ReentrantReadWriteLock.ReadLock)object).lock();
        try {
            nativeDeWorldContext.admit();
            Object object2 = nativeDeWorldContext.life.admission;
            synchronized (object2) {
                if (NativeDeLiveAiJobs.find(nativeDeInstanceState) != null) {
                    throw new IllegalStateException("De AI binding is immutable");
                }
                Owner owner = (Owner)CombatAccess.field(nativeDeWorldContext, FIELD);
                if (owner == null) {
                    owner = new Owner(nativeDeWorldContext);
                    CombatAccess.set(nativeDeWorldContext, FIELD, owner);
                }
                if (owner.closed) {
                    throw new RejectedExecutionException("De AI closed");
                }
                Binding binding = new Binding(nativeDeInstanceState, owner, environment, config);
                owner.bindings.add(binding);
                CombatAccess.set(nativeDeInstanceState, BINDING, binding);
            }
        }
        finally {
            ((ReentrantReadWriteLock.ReadLock)object).unlock();
        }
    }

    static boolean admits(NativeDeInstanceState nativeDeInstanceState) {
        Binding binding = NativeDeLiveAiJobs.find(nativeDeInstanceState);
        return binding != null && !binding.closed && !binding.owner.closed && !binding.owner.context.closed && binding.owner.life.admits() && !nativeDeInstanceState.destroyed() && !nativeDeInstanceState.deletionStarted() && CombatAccess.field(nativeDeInstanceState, "sourceDeWorldEntry") != null && binding.owner.context.world().isObjectRegistered(nativeDeInstanceState);
    }

    private static Credit credit(Binding binding) {
        Ticket ticket = current.get();
        if (ticket != null && ticket.binding == binding && !ticket.finished) {
            ++ticket.credit.refs;
            return ticket.credit;
        }
        PulseCredit pulseCredit = NativeDeLiveAiJobs.pulseCredit.get();
        if (pulseCredit != null && pulseCredit.binding() == binding) {
            ++pulseCredit.credit().refs;
            return pulseCredit.credit();
        }
        if (binding.owner.life.pending.get() >= NativePlayerCommit.GLOBAL) {
            return null;
        }
        binding.owner.life.pending.incrementAndGet();
        return new Credit(binding.owner);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static Ticket reserve(NativeDeInstanceState nativeDeInstanceState, Runnable runnable, boolean bl, long l) {
        Binding binding = NativeDeLiveAiJobs.find(nativeDeInstanceState);
        if (binding == null) {
            return null;
        }
        Object object = binding.owner.life.admission;
        synchronized (object) {
            if (!NativeDeLiveAiJobs.admits(nativeDeInstanceState)) {
                ++binding.owner.rejected;
                return null;
            }
            Credit credit = NativeDeLiveAiJobs.credit(binding);
            if (credit == null) {
                ++binding.owner.rejected;
                return null;
            }
            Ticket ticket = new Ticket(binding, runnable, credit, bl, l);
            binding.owner.tickets.add(ticket);
            ++binding.tasks;
            return ticket;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean execute(NativeDeInstanceState nativeDeInstanceState, Runnable runnable) {
        Ticket ticket = NativeDeLiveAiJobs.reserve(nativeDeInstanceState, runnable, false, 0L);
        if (ticket == null) {
            return false;
        }
        Ticket ticket2 = ticket;
        synchronized (ticket2) {
            if (ticket.finished) {
                return true;
            }
            try {
                ticket.binding.owner.life.workers.execute(ticket);
                Object object = ticket.binding.owner.life.admission;
                synchronized (object) {
                    ++ticket.binding.owner.submitted;
                }
                return true;
            }
            catch (RejectedExecutionException rejectedExecutionException) {
                ticket.finish();
                return false;
            }
            catch (Error | RuntimeException throwable) {
                ticket.finish();
                throw throwable;
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static ScheduledFuture<?> schedule(NativeDeInstanceState nativeDeInstanceState, Runnable runnable, long l, long l2) {
        Ticket ticket = NativeDeLiveAiJobs.reserve(nativeDeInstanceState, runnable, true, l2);
        if (ticket == null) {
            throw new RejectedExecutionException("De AI timer closed or budget exhausted");
        }
        try {
            ScheduledFuture<?> scheduledFuture = ticket.binding.owner.life.retry.schedule(ticket, Math.max(0L, l), TimeUnit.MILLISECONDS);
            ticket.install(scheduledFuture);
            Object object = ticket.binding.owner.life.admission;
            synchronized (object) {
                ++ticket.binding.owner.scheduled;
            }
            return ticket;
        }
        catch (Error | RuntimeException throwable) {
            ticket.finish();
            throw throwable;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void background(NativeDeInstanceState nativeDeInstanceState, boolean bl) {
        Binding binding = NativeDeLiveAiJobs.find(nativeDeInstanceState);
        if (binding == null) {
            return;
        }
        Owner owner = binding.owner;
        ScheduledFuture<?> scheduledFuture = null;
        Credit credit = null;
        Object object = owner.life.admission;
        synchronized (object) {
            if (bl) {
                if (binding.closed || owner.closed || !owner.life.admits()) {
                    return;
                }
                owner.background.add(binding);
                if (owner.heartbeat == null) {
                    Credit credit2 = NativeDeLiveAiJobs.credit(binding);
                    if (credit2 == null) {
                        owner.background.remove(binding);
                        throw new RejectedExecutionException("De AI heartbeat budget exhausted");
                    }
                    try {
                        owner.heartbeat = owner.life.retry.scheduleAtFixedRate(() -> NativeDeLiveAiJobs.pulse(owner), 10L, 10L, TimeUnit.SECONDS);
                        owner.heartbeatCredit = credit2;
                    }
                    catch (Error | RuntimeException throwable) {
                        owner.background.remove(binding);
                        credit2.release();
                        throw throwable;
                    }
                }
            } else {
                owner.background.remove(binding);
                if (owner.background.isEmpty()) {
                    scheduledFuture = owner.heartbeat;
                    credit = owner.heartbeatCredit;
                    owner.heartbeat = null;
                    owner.heartbeatCredit = null;
                }
            }
        }
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        if (credit != null) {
            credit.release();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void pulse(Owner owner) {
        Object admissionLock = owner.life.admission;
        synchronized (admissionLock) {
            if (owner.closed || !owner.life.admits()) {
                // ** MonitorExit[var3_1] (shouldn't be in output)
                return;
            }
            Credit credit = owner.heartbeatCredit;
            if (credit == null) {
                // ** MonitorExit[var3_1] (shouldn't be in output)
                return;
            }
            ++credit.refs;
            Binding[] bindingArray2 = (Binding[])owner.background.toArray(Binding[]::new);
            // ** MonitorExit[var3_1] (shouldn't be in output)
            try {
                for (Binding binding : bindingArray2) {
                    Credit credit2;
                    if (binding.actor.isDead() || binding.actor.getMap() == null || !NativeDeLiveAiJobs.admits(binding.actor)) {
                        NativeDeLiveAiJobs.background(binding.actor, false);
                        continue;
                    }
                    Object object = owner.life.admission;
                    synchronized (object) {
                        if (owner.closed || binding.closed || !owner.life.admits()) {
                            continue;
                        }
                        credit2 = NativeDeLiveAiJobs.credit(binding);
                    }
                    if (credit2 == null) continue;
                    object = pulseCredit.get();
                    pulseCredit.set(new PulseCredit(binding, credit2));
                    try {
                        binding.actor.onBackgroundTick();
                    }
                    catch (Exception exception) {
                        CombatNpcDamageQueue.failed(exception);
                    }
                    finally {
                        if (object == null) {
                            pulseCredit.remove();
                        } else {
                            pulseCredit.set((PulseCredit)object);
                        }
                        credit2.release();
                    }
                }
            }
            finally {
                credit.release();
            }
            return;
        }
    }

    static void retireDeleted(NativeDeInstanceState nativeDeInstanceState) {
        if (nativeDeInstanceState.deletionStarted() || nativeDeInstanceState.destroyed()) {
            NativeDeLiveAiJobs.retire(nativeDeInstanceState);
        }
    }

    static void stop(NativeDeDeletionPorts.ObjectRef objectRef) {
        if (objectRef instanceof NativeDeInstanceState) {
            NativeDeInstanceState nativeDeInstanceState = (NativeDeInstanceState)objectRef;
            NativeDeLiveAiJobs.retire(nativeDeInstanceState);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void retire(NativeDeInstanceState nativeDeInstanceState) {
        Ticket[] ticketArray;
        nativeDeInstanceState.stopAI();
        Binding binding = NativeDeLiveAiJobs.find(nativeDeInstanceState);
        if (binding == null) {
            return;
        }
        NativeDeLiveAiJobs.background(nativeDeInstanceState, false);
        Object object = binding.owner.life.admission;
        synchronized (object) {
            binding.closed = true;
            binding.owner.bindings.remove(binding);
            ticketArray = (Ticket[])binding.owner.tickets.stream().filter(ticket -> ticket.binding == binding).toArray(Ticket[]::new);
        }
        for (Ticket ticket2 : ticketArray) {
            ticket2.cancel(false);
        }
        Object object2 = binding.owner.life.admission;
        synchronized (object2) {
            if (binding.tasks == 0) {
                CombatAccess.set(nativeDeInstanceState, BINDING, null);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void close(NativeDeWorldContext nativeDeWorldContext) {
        Credit credit;
        ScheduledFuture<?> scheduledFuture;
        Ticket[] ticketArray;
        Binding[] bindingArray;
        Owner owner = (Owner)CombatAccess.field(nativeDeWorldContext, FIELD);
        if (owner == null) {
            return;
        }
        Object object = owner.life.admission;
        synchronized (object) {
            owner.closed = true;
            Binding[] bindingArray2 = bindingArray = (Binding[])owner.bindings.toArray(Binding[]::new);
            int n = bindingArray2.length;
            for (int i = 0; i < n; ++i) {
                Binding binding = bindingArray2[i];
                binding.closed = true;
            }
            ticketArray = (Ticket[])owner.tickets.toArray(Ticket[]::new);
            owner.bindings.clear();
            owner.background.clear();
            scheduledFuture = owner.heartbeat;
            credit = owner.heartbeatCredit;
            owner.heartbeat = null;
            owner.heartbeatCredit = null;
        }
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        if (credit != null) {
            credit.release();
        }
        for (Binding binding : bindingArray) {
            NativeDeLiveAiScope.call(binding.actor, () -> {
                binding.actor.stopAI();
                return null;
            });
        }
        for (Ticket ticket : ticketArray) {
            ticket.cancel(false);
        }
        Object object2 = owner.life.admission;
        synchronized (object2) {
            for (Binding binding : bindingArray) {
                if (binding.tasks != 0) continue;
                CombatAccess.set(binding.actor, BINDING, null);
            }
        }
        CombatAccess.set(nativeDeWorldContext, FIELD, null);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Snapshot snapshot(NativeDeWorldContext nativeDeWorldContext) {
        Object object = nativeDeWorldContext.life.admission;
        synchronized (object) {
            Owner owner = (Owner)CombatAccess.field(nativeDeWorldContext, FIELD);
            return owner == null ? new Snapshot(false, true, 0, 0, 0, 0L, 0L, 0L, 0L) : new Snapshot(true, owner.closed, owner.bindings.size(), owner.tickets.size(), owner.background.size(), owner.submitted, owner.scheduled, owner.completed, owner.rejected);
        }
    }

    private NativeDeLiveAiJobs() {
    }

    static final class Binding {
        final NativeDeInstanceState actor;
        final Owner owner;
        final NativeDeLiveAiPorts.Environment environment;
        final NativeDeLiveAiPorts.Config config;
        volatile boolean closed;
        int tasks;

        Binding(NativeDeInstanceState nativeDeInstanceState, Owner owner, NativeDeLiveAiPorts.Environment environment, NativeDeLiveAiPorts.Config config) {
            this.actor = nativeDeInstanceState;
            this.owner = owner;
            this.environment = environment;
            this.config = config;
        }
    }

    static final class Owner {
        final NativeDeWorldContext context;
        final NativePlayerLifetime life;
        final Set<Ticket> tickets = new HashSet<Ticket>();
        final Set<Binding> bindings = Collections.newSetFromMap(new IdentityHashMap());
        final Set<Binding> background = Collections.newSetFromMap(new IdentityHashMap());
        boolean closed;
        ScheduledFuture<?> heartbeat;
        Credit heartbeatCredit;
        long submitted;
        long scheduled;
        long completed;
        long rejected;

        Owner(NativeDeWorldContext nativeDeWorldContext) {
            this.context = nativeDeWorldContext;
            this.life = nativeDeWorldContext.life;
        }
    }

    static final class Ticket
    implements Runnable,
    ScheduledFuture<Object> {
        final Binding binding;
        final Runnable action;
        final Credit credit;
        final boolean timer;
        final long ownerToken;
        volatile ScheduledFuture<?> delegate;
        boolean running;
        boolean cancelled;
        volatile boolean finished;
        volatile Throwable failure;

        Ticket(Binding binding, Runnable runnable, Credit credit, boolean bl, long l) {
            this.binding = binding;
            this.action = runnable;
            this.credit = credit;
            this.timer = bl;
            this.ownerToken = l;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        void install(ScheduledFuture<?> scheduledFuture) {
            boolean bl;
            Ticket ticket = this;
            synchronized (ticket) {
                this.delegate = scheduledFuture;
                bl = this.finished;
            }
            if (bl) {
                scheduledFuture.cancel(false);
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void run() {
            Ticket ticket = this;
            synchronized (ticket) {
                if (this.finished) {
                    return;
                }
                this.running = true;
            }
            ticket = current.get();
            current.set(this);
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
            }
            catch (Throwable throwable) {
                this.failure = throwable;
                CombatNpcDamageQueue.failed(throwable);
            }
            finally {
                if (ticket == null) {
                    current.remove();
                } else {
                    current.set(ticket);
                }
                Ticket ticket2 = this;
                synchronized (ticket2) {
                    this.running = false;
                }
                this.finish();
            }
        }

        void discard() {
            Runnable runnable = this.action;
            if (runnable instanceof NativeDeLiveAiScope.Pool.ShutdownDiscardable) {
                NativeDeLiveAiScope.Pool.ShutdownDiscardable shutdownDiscardable = (NativeDeLiveAiScope.Pool.ShutdownDiscardable)runnable;
                NativeDeLiveAiScope.call(this.binding.actor, () -> {
                    shutdownDiscardable.discardOnShutdown();
                    return null;
                });
            } else if (this.timer) {
                NativeDeLiveAiScope.call(this.binding.actor, () -> {
                    CombatAccess.call(this.binding.actor, "stopAiOwner", "(J)V", this.ownerToken);
                    return null;
                });
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        void finish() {
            Object object = this;
            synchronized (object) {
                if (this.finished) {
                    return;
                }
                this.finished = true;
                this.notifyAll();
            }
            object = this.binding.owner.life.admission;
            synchronized (object) {
                this.binding.owner.tickets.remove(this);
                --this.binding.tasks;
                ++this.binding.owner.completed;
                if (this.binding.closed && this.binding.tasks == 0) {
                    CombatAccess.set(this.binding.actor, NativeDeLiveAiJobs.BINDING, null);
                }
            }
            this.credit.release();
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public boolean cancel(boolean bl) {
            Ticket ticket = this;
            synchronized (ticket) {
                if (this.finished || this.running) {
                    return false;
                }
                this.cancelled = true;
                ScheduledFuture<?> scheduledFuture = this.delegate;
                if (scheduledFuture != null && scheduledFuture.cancel(false)) {
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
            while (!this.finished) {
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
        public synchronized Object get(long l, TimeUnit timeUnit) throws InterruptedException, TimeoutException, ExecutionException {
            long l2 = System.nanoTime() + timeUnit.toNanos(l);
            while (!this.finished) {
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

    static final class Credit {
        final Owner owner;
        int refs = 1;

        Credit(Owner owner) {
            this.owner = owner;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        void release() {
            Object object = this.owner.life.admission;
            synchronized (object) {
                if (--this.refs == 0 && this.owner.life.pending.decrementAndGet() < 0L) {
                    throw new IllegalStateException("De AI credit underflow");
                }
                if (this.refs < 0) {
                    throw new IllegalStateException("De AI reference underflow");
                }
            }
        }
    }

    private record PulseCredit(Binding binding, Credit credit) {
    }

    record Snapshot(boolean present, boolean closed, int bindings, int tasks, int background, long submitted, long scheduled, long completed, long rejected) {
    }
}
