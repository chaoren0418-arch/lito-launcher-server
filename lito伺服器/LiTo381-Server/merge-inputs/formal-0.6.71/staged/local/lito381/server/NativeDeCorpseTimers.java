// DECOMPILED from base-0.6.71.jar, TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;
import local.lito381.server.CombatNpcDamageQueue;
import local.lito381.server.GameHub;
import local.lito381.server.NativeDeAiTimers;
import local.lito381.server.NativeDeCorpseClock;
import local.lito381.server.NativeDeCorpseCore;
import local.lito381.server.NativeDeCorpseState;
import local.lito381.server.NativeDeNpcVitalState;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativePlayerLifetime;

final class NativeDeCorpseTimers {
    private static final ConcurrentHashMap<NativePlayerLifetime, Owner> owners = new ConcurrentHashMap();

    NativeDeCorpseTimers() {
    }

    static Owner of(GameHub gameHub, Deletion deletion) {
        return NativeDeCorpseTimers.of(NativePlayerLifetime.of(gameHub), deletion, System::nanoTime);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Owner of(NativePlayerLifetime nativePlayerLifetime, Deletion deletion, LongSupplier longSupplier) {
        Owner owner;
        boolean bl = false;
        synchronized (nativePlayerLifetime.admission) {
            if (nativePlayerLifetime.mode != NativePlayerLifetime.Mode.OPEN) {
                throw new RejectedExecutionException("corpse application closed");
            }
            owner = owners.get(nativePlayerLifetime);
            if (owner == null) {
                owner = new Owner(nativePlayerLifetime, deletion, longSupplier);
                owners.put(nativePlayerLifetime, owner);
                bl = true;
            } else if (owner.deletion != deletion) {
                throw new IllegalArgumentException("corpse application deletion binding is immutable");
            }
        }
        if (bl) {
            owner.start();
        }
        return owner;
    }

    static void closeAll(NativePlayerLifetime nativePlayerLifetime) {
        try {
            Owner owner = owners.get(nativePlayerLifetime);
            if (owner != null) {
                owner.close(nativePlayerLifetime.mode == NativePlayerLifetime.Mode.STOPPED);
            }
        }
        finally {
            NativeDeAiTimers.close(nativePlayerLifetime);
        }
    }

    static void stop(NativePlayerLifetime nativePlayerLifetime) {
        Owner owner = owners.get(nativePlayerLifetime);
        if (owner != null) {
            owner.close(true);
        }
    }

    static Snapshot snapshot(NativePlayerLifetime nativePlayerLifetime) {
        Owner owner = owners.get(nativePlayerLifetime);
        return owner == null ? new Snapshot(false, true, 0, 0L, 0, null) : owner.snapshot();
    }

    static interface Deletion {
        public void auditDeletionStalls();

        public void delete(NativeDeCorpseState var1);

        public short mapId(NativeDeCorpseState var1);
    }

    static final class Owner
    implements NativeDeCorpseClock {
        final NativePlayerLifetime life;
        final Deletion deletion;
        final LongSupplier clock;
        final NativeDeCorpseCore core;
        final Set<Ticket> tasks = ConcurrentHashMap.newKeySet();
        final AtomicReference<ScheduledFuture<?>> ticker = new AtomicReference();
        final AtomicLong deferred = new AtomicLong();
        volatile boolean closed;

        Owner(final NativePlayerLifetime life, Deletion deletion, LongSupplier longSupplier) {
            this.life = life;
            this.deletion = Objects.requireNonNull(deletion);
            this.clock = Objects.requireNonNull(longSupplier);
            this.core = new NativeDeCorpseCore(new NativeDeCorpseCore.Port(){

                @Override
                public void auditDeletionStalls() {
                    Owner.this.deletion.auditDeletionStalls();
                }

                @Override
                public long nanoTime() {
                    return clock.getAsLong();
                }

                @Override
                public ScheduledFuture<?> fixed(Runnable runnable, long l, long l2) {
                    if (closed || life.mode != NativePlayerLifetime.Mode.OPEN) {
                        throw new RejectedExecutionException("corpse owner closed");
                    }
                    ScheduledFuture<?> scheduledFuture = life.retry.scheduleAtFixedRate(() -> Owner.this.tick(runnable), l, l2, TimeUnit.MILLISECONDS);
                    if (scheduledFuture != null) {
                        ticker.set(scheduledFuture);
                        if (closed) {
                            ticker.compareAndSet(scheduledFuture, null);
                            Owner.this.cancel(scheduledFuture);
                        }
                    }
                    return scheduledFuture;
                }

                /*
                 * WARNING - Removed try catching itself - possible behaviour change.
                 */
                @Override
                public Object reserve(NativeDeCorpseCore.Npc npc) {
                    synchronized (life.admission) {
                        if (closed || life.mode != NativePlayerLifetime.Mode.OPEN || life.pending.get() >= NativePlayerCommit.GLOBAL) {
                            deferred.incrementAndGet();
                            return null;
                        }
                        Ticket ticket = new Ticket(Owner.this);
                        life.pending.incrementAndGet();
                        tasks.add(ticket);
                        return ticket;
                    }
                }

                /*
                 * WARNING - Removed try catching itself - possible behaviour change.
                 */
                @Override
                public boolean claim(NativeDeCorpseCore.Npc npc, long l, Object object) {
                    Ticket ticket = (Ticket)object;
                    synchronized (npc) {
                        synchronized (life.admission) {
                            return !closed && life.mode == NativePlayerLifetime.Mode.OPEN && !ticket.closed && npc.claimExpiredCorpseDeletion(l);
                        }
                    }
                }

                @Override
                public boolean retained(Object object) {
                    return ((Ticket)object).retained();
                }

                @Override
                public void release(Object object) {
                    ((Ticket)object).close(false);
                }

                /*
                 * WARNING - Removed try catching itself - possible behaviour change.
                 */
                @Override
                public boolean tryExecute(NativeDeCorpseCore.Task task) {
                    Ticket ticket = (Ticket)task.credit;
                    Job job = new Job(ticket, task);
                    synchronized (ticket) {
                        if (ticket.closed) {
                            return ticket.discarded;
                        }
                        if (closed || life.mode != NativePlayerLifetime.Mode.OPEN) {
                            return false;
                        }
                        ticket.task = task;
                        ticket.job = job;
                        ticket.phase = 2;
                    }
                    try {
                        life.workers.execute(job);
                        return true;
                    }
                    catch (RejectedExecutionException rejectedExecutionException) {
                        synchronized (ticket) {
                            if (ticket.closed && ticket.discarded) {
                                return true;
                            }
                            if (ticket.job == job) {
                                ticket.job = null;
                                ticket.phase = 0;
                            }
                        }
                        return false;
                    }
                    catch (Error | RuntimeException throwable) {
                        ticket.close(true);
                        throw throwable;
                    }
                }

                @Override
                public boolean isShutdown() {
                    return closed || life.mode != NativePlayerLifetime.Mode.OPEN;
                }

                /*
                 * WARNING - Removed try catching itself - possible behaviour change.
                 */
                @Override
                public ScheduledFuture<?> retry(NativeDeCorpseCore.Task task, Runnable runnable, long l) {
                    long l2;
                    Ticket ticket = (Ticket)task.credit;
                    synchronized (ticket) {
                        if (ticket.closed || closed || life.mode != NativePlayerLifetime.Mode.OPEN) {
                            ticket.close(true);
                            throw new RejectedExecutionException("corpse retry owner closed");
                        }
                        ticket.task = task;
                        ticket.phase = 1;
                        l2 = ++ticket.epoch;
                    }
                    try {
                        boolean scheduled;
                        ScheduledFuture<?> scheduledFuture = life.retry.schedule(() -> {
                            boolean run;
                            synchronized (ticket) {
                                boolean bl2 = run = !ticket.closed && ticket.phase == 1 && ticket.epoch == l2 && !closed && life.mode == NativePlayerLifetime.Mode.OPEN;
                                if (run) {
                                    ticket.future = null;
                                    ticket.phase = 0;
                                }
                            }
                            if (run) {
                                life.executeCommitted(runnable);
                            } else {
                                ticket.close(true);
                            }
                        }, l, TimeUnit.MILLISECONDS);
                        if (scheduledFuture == null) {
                            ticket.close(false);
                            return null;
                        }
                        synchronized (ticket) {
                            boolean bl2 = scheduled = !ticket.closed && ticket.phase == 1 && ticket.epoch == l2;
                            if (scheduled) {
                                ticket.future = scheduledFuture;
                            }
                        }
                        if (!scheduled) {
                            Owner.this.cancel(scheduledFuture);
                        }
                        return scheduledFuture;
                    }
                    catch (Error | RuntimeException throwable) {
                        ticket.close(false);
                        throw throwable;
                    }
                }

                @Override
                public void log(String string, String string2, Object ... objectArray) {
                    System.getLogger(NativeDeCorpseTimers.class.getName()).log(string.equals("warn") ? System.Logger.Level.WARNING : System.Logger.Level.ERROR, string2 + " " + Arrays.toString(objectArray));
                }
            });
        }

        long nanoTime() {
            return this.clock.getAsLong();
        }

        void start() {
            this.core.start();
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void track(NativeDeNpcVitalState nativeDeNpcVitalState) {
            NativeDeCorpseState nativeDeCorpseState;
            block8: {
                block7: {
                    if (!(nativeDeNpcVitalState instanceof NativeDeCorpseState)) break block7;
                    nativeDeCorpseState = (NativeDeCorpseState)nativeDeNpcVitalState;
                    if (nativeDeCorpseState.owner == this) break block8;
                }
                throw new IllegalArgumentException("corpse state belongs to another application owner");
            }
            synchronized (this.life.admission) {
                if (this.closed || this.life.mode != NativePlayerLifetime.Mode.OPEN) {
                    throw new RejectedExecutionException("corpse application admission closed");
                }
                this.core.track(nativeDeCorpseState);
            }
        }

        void untrack(NativeDeNpcVitalState nativeDeNpcVitalState) {
            if (nativeDeNpcVitalState instanceof NativeDeCorpseState) {
                NativeDeCorpseState nativeDeCorpseState = (NativeDeCorpseState)nativeDeNpcVitalState;
                if (nativeDeCorpseState.owner == this) {
                    this.core.untrack(nativeDeCorpseState);
                }
            }
        }

        @Override
        public void invalidStart(int n, int n2, int n3, long l) {
            System.getLogger(NativeDeCorpseTimers.class.getName()).log(System.Logger.Level.WARNING, "corpse timer start rejected: objectId=" + n + " npcId=" + n2 + " seconds=" + n3 + " count=" + l);
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        void tick(Runnable runnable) {
            synchronized (this.life.admission) {
                if (this.closed || this.life.mode != NativePlayerLifetime.Mode.OPEN || this.life.pending.get() >= NativePlayerCommit.GLOBAL) {
                    this.deferred.incrementAndGet();
                    return;
                }
                this.life.pending.incrementAndGet();
            }
            try {
                this.life.executeCommitted(runnable);
            }
            finally {
                long l = this.life.pending.decrementAndGet();
                if (l < 0L) {
                    throw new IllegalStateException("corpse tick receipt underflow");
                }
            }
        }

        void tick() {
            this.tick(this.core);
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        void close(boolean bl) {
            Object object;
            synchronized (this.life.admission) {
                this.closed = true;
            }
            object = this.ticker.getAndSet(null);
            this.cancel((ScheduledFuture<?>)object);
            this.core.tracked.clear();
            for (Ticket ticket : (Ticket[])this.tasks.toArray(Ticket[]::new)) {
                try {
                    boolean bl2;
                    synchronized (ticket) {
                        bl2 = !bl && (ticket.phase == 2 || ticket.running);
                    }
                    if (bl2) continue;
                    ticket.close(true);
                }
                catch (Throwable throwable) {
                    CombatNpcDamageQueue.failed(throwable);
                }
            }
            this.removeIfIdle();
        }

        void removeIfIdle() {
            if (this.closed && this.tasks.isEmpty()) {
                owners.remove(this.life, this);
            }
        }

        Snapshot snapshot() {
            return new Snapshot(true, this.closed, this.tasks.size(), this.deferred.get(), this.core.tracked.size(), this.core.snapshot());
        }

        void cancel(ScheduledFuture<?> scheduledFuture) {
            if (scheduledFuture != null && !scheduledFuture.isDone() && scheduledFuture.cancel(false)) {
                this.life.cancelledTimers.incrementAndGet();
            }
        }
    }

    record Snapshot(boolean present, boolean closed, int tasks, long deferred, int tracked, NativeDeCorpseCore.Snapshot source) {
    }

    private static final class Job
    implements Runnable {
        final Ticket ticket;
        final NativeDeCorpseCore.Task task;

        Job(Ticket ticket, NativeDeCorpseCore.Task task) {
            this.ticket = ticket;
            this.task = task;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void run() {
            boolean bl;
            Ticket ticket = this.ticket;
            synchronized (ticket) {
                boolean bl2 = bl = !ticket.closed && ticket.job == this && ticket.owner.life.mode != NativePlayerLifetime.Mode.STOPPED;
                if (bl) {
                    ticket.running = true;
                    ticket.phase = 3;
                    ticket.job = null;
                }
            }
            if (!bl) {
                ticket.close(true);
                return;
            }
            try {
                ticket.owner.life.executeCommitted(this.task);
            }
            catch (Throwable throwable) {
                CombatNpcDamageQueue.failed(throwable);
            }
            finally {
                synchronized (ticket) {
                    ticket.running = false;
                    ticket.phase = 0;
                }
                ticket.close(false);
            }
        }
    }

    private static final class Ticket {
        final Owner owner;
        volatile boolean closed;
        boolean running;
        boolean discarded;
        int phase;
        long epoch;
        NativeDeCorpseCore.Task task;
        Job job;
        ScheduledFuture<?> future;

        Ticket(Owner owner) {
            this.owner = owner;
        }

        synchronized boolean retained() {
            return !this.closed && (this.running || this.phase == 1 || this.phase == 2);
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        void close(boolean bl) {
            Job job;
            ScheduledFuture<?> scheduledFuture;
            NativeDeCorpseCore.Task task = null;
            synchronized (this) {
                if (this.closed) {
                    return;
                }
                if (this.running) {
                    return;
                }
                this.closed = true;
                scheduledFuture = this.future;
                this.future = null;
                job = this.job;
                this.job = null;
                if (bl && !this.discarded && this.task != null) {
                    this.discarded = true;
                    task = this.task;
                }
            }
            this.owner.cancel(scheduledFuture);
            if (job != null) {
                this.owner.life.workers.remove(job);
            }
            this.owner.tasks.remove(this);
            long l = this.owner.life.pending.decrementAndGet();
            try {
                if (l < 0L) {
                    throw new IllegalStateException("corpse task receipt underflow");
                }
                if (task != null) {
                    task.discardOnShutdown();
                }
            }
            finally {
                this.owner.removeIfIdle();
            }
        }
    }
}
