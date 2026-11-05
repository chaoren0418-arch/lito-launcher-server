// DECOMPILED from base-0.6.71.jar, TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import local.lito381.server.CombatNpcDamageQueue;
import local.lito381.server.GameHub;
import local.lito381.server.NativeDeAiState;
import local.lito381.server.NativeDeAiTimeline;
import local.lito381.server.NativeDeAiTimers;
import local.lito381.server.NativeDeAiWorkerProtocol;
import local.lito381.server.NativePlayerLifetime;

final class NativeDeAiWorkers {
    private static final ConcurrentHashMap<Object, Set<Ticket>> timelines = new ConcurrentHashMap();
    private static final ConcurrentHashMap<Object, Set<Ticket>> generations = new ConcurrentHashMap();
    private static final AtomicLong submitted = new AtomicLong();
    private static final AtomicLong rejected = new AtomicLong();
    private static final AtomicLong retries = new AtomicLong();
    private static final AtomicLong notices = new AtomicLong();
    private static final AtomicLong active = new AtomicLong();
    private static final AtomicLong background = new AtomicLong();
    private static final AtomicLong discarded = new AtomicLong();
    private static volatile String lastThread = "";

    NativeDeAiWorkers() {
    }

    private static void index(ConcurrentHashMap<Object, Set<Ticket>> concurrentHashMap, Object object2, Ticket ticket) {
        concurrentHashMap.compute(object2, (object, keySetView) -> {
            if (keySetView == null) {
                keySetView = ConcurrentHashMap.newKeySet();
            }
            keySetView.add(ticket);
            return keySetView;
        });
    }

    private static void unindex(ConcurrentHashMap<Object, Set<Ticket>> concurrentHashMap, Object object2, Ticket ticket) {
        concurrentHashMap.computeIfPresent(object2, (object, set) -> {
            set.remove(ticket);
            return set.isEmpty() ? null : set;
        });
    }

    private static Ticket register(GameHub gameHub, NativeDeAiTimers.Work work, NativeDeAiTimeline.State state, NativeDeAiTimers.Registry registry, NativeDeAiTimers.Credit credit) {
        Ticket ticket = new Ticket(gameHub, work, state, registry, credit);
        registry.workers.add(ticket);
        NativeDeAiWorkers.index(timelines, work.lease().timeline(), ticket);
        NativeDeAiWorkers.index(generations, work.generation(), ticket);
        return ticket;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void enable(GameHub gameHub, boolean bl) {
        NativeDeAiTimers.Registry registry = NativeDeAiTimers.registry(NativePlayerLifetime.of(gameHub));
        synchronized (registry.life.admission) {
            registry.workersEnabled = bl && registry.life.mode == NativePlayerLifetime.Mode.OPEN;
        }
        if (bl) {
            NativeDeAiWorkers.onReady(gameHub);
        }
    }

    static void onReady(GameHub gameHub) {
        NativeDeAiTimers.Registry registry = NativeDeAiTimers.registry(NativePlayerLifetime.of(gameHub));
        while (registry.workersEnabled) {
            NativeDeAiTimers.Entry entry = registry.ready.poll();
            if (entry == null) {
                return;
            }
            NativeDeAiTimers.Work work = entry.work;
            Ticket ticket = NativeDeAiTimers.withTarget(gameHub, work.target(), work.generation(), work.lease(), live -> {
                synchronized (entry.state.lock) {
                    synchronized (registry.life.admission) {
                        if (!registry.workersEnabled || registry.life.mode != NativePlayerLifetime.Mode.OPEN || !NativeDeAiTimeline.owns(entry.state, work.owner()) || entry.state.chain != work.owner() || entry.state.stop || !entry.state.pending || entry.state.pendingBackground != work.background() || entry.state.activeTimer != 0L || entry.state.executing || !entry.detach()) {
                            return null;
                        }
                        return NativeDeAiWorkers.register(gameHub, work, entry.state, registry, entry.credit);
                    }
                }
            });
            if (ticket == null) {
                entry.close(true);
            } else {
                NativeDeAiWorkers.dispatch(ticket);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void dispatch(Ticket ticket) {
        AtomicBoolean disposition = new AtomicBoolean();
        NativeDeAiWorkerProtocol.Port port = NativeDeAiWorkers.port(ticket, disposition);
        try {
            NativeDeAiWorkerProtocol.dispatch(ticket.state, port, ticket.work.owner(), ticket.work.background());
        }
        catch (Throwable throwable) {
            CombatNpcDamageQueue.failed(throwable);
            ticket.close(true);
        }
        finally {
            if (!disposition.get()) {
                ticket.close(true);
            }
        }
    }

    private static NativeDeAiWorkerProtocol.Port port(Ticket ticket) {
        return NativeDeAiWorkers.port(ticket, new AtomicBoolean());
    }

    private static NativeDeAiWorkerProtocol.Port port(final Ticket t, final AtomicBoolean disposition) {
        final NativePlayerLifetime life = t.registry.life;
        return new NativeDeAiWorkerProtocol.Port(){

            @Override
            public boolean dead() {
                return true;
            }

            @Override
            public boolean destroyed() {
                return true;
            }

            @Override
            public int hp() {
                return 0;
            }

            @Override
            public boolean shutdown() {
                return life.mode != NativePlayerLifetime.Mode.OPEN;
            }

            @Override
            public long nowMillis() {
                return System.currentTimeMillis();
            }

            @Override
            public void removeBackground() {
            }

            @Override
            public void addBackground() {
            }

            @Override
            public boolean done(Object object) {
                return NativeDeAiTimers.done(object);
            }

            @Override
            public void cancel(Object object) {
                NativeDeAiTimers.cancelFuture(object);
            }

            @Override
            public void dispatch(long l, boolean bl) {
                throw new IllegalStateException("use qualified worker continuation");
            }

            @Override
            public Object schedule(long l, int n, boolean bl) {
                Object object = NativeDeAiTimers.reschedule(t, n);
                disposition.set(object != null || t.closed.get());
                return object;
            }

            @Override
            public boolean tryExecute(long l, boolean bl) {
                if (t.closed.get()) {
                    return false;
                }
                try {
                    life.workers.execute(new Task(t));
                    submitted.incrementAndGet();
                    disposition.set(true);
                    return true;
                }
                catch (RejectedExecutionException rejectedExecutionException) {
                    return false;
                }
            }

            @Override
            public long rejected() {
                return rejected.incrementAndGet();
            }

            @Override
            public void retried() {
                retries.incrementAndGet();
            }

            @Override
            public void notice(long l, int n) {
                notices.incrementAndGet();
                System.getLogger(NativeDeAiWorkers.class.getName()).log(System.Logger.Level.WARNING, "De worker saturated; retry in {0} ms, rejected={1}", n, l);
            }

            @Override
            public void active(long l) {
                NativeDeAiWorkers.body(t, false);
            }

            @Override
            public void background(long l) {
                NativeDeAiWorkers.body(t, true);
            }
        };
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void body(Ticket ticket, boolean bl) {
        NativeDeAiTimers.Work work = ticket.work;
        NativeDeAiState.Result result = NativeDeAiTimers.perform(ticket.hub, work.target(), work.generation(), work.lease(), new NativeDeAiState.Request(bl ? NativeDeAiState.Op.BEGIN_BACKGROUND : NativeDeAiState.Op.BEGIN_ACTIVE, work.owner(), 0L));
        if (result == null || result.value() != 1L) {
            ticket.close(true);
            return;
        }
        try {
            lastThread = Thread.currentThread().getName();
            if (bl) {
                background.incrementAndGet();
            } else {
                active.incrementAndGet();
            }
        }
        finally {
            AtomicBoolean disposition = new AtomicBoolean();
            Ticket ticket2 = NativeDeAiTimers.withTarget(ticket.hub, work.target(), work.generation(), work.lease(), live -> {
                synchronized (ticket.state.lock) {
                    synchronized (ticket.registry.life.admission) {
                        NativeDeAiState.Result finishResult = NativeDeAiState.perform(live.status(), work.lease(), NativeDeAiTimers.facts(live, ticket.registry.life), new NativeDeAiState.Request(NativeDeAiState.Op.FINISH, work.owner(), 0L));
                        if (finishResult == null) {
                            return null;
                        }
                        disposition.set(true);
                        Ticket nextTicket = null;
                        for (NativeDeAiState.Effect effect : finishResult.effects()) {
                            if (effect.kind() == NativeDeAiState.Kind.WORKER) {
                                if (ticket.registry.life.mode == NativePlayerLifetime.Mode.OPEN && ticket.detach()) {
                                    nextTicket = NativeDeAiWorkers.register(ticket.hub, new NativeDeAiTimers.Work(work.target(), work.generation(), work.lease(), effect.owner(), effect.background()), ticket.state, ticket.registry, ticket.credit);
                                } else {
                                    NativeDeAiTimeline.stop(ticket.state, NativeDeAiTimers.quiet(ticket.registry.life), effect.owner());
                                }
                            }
                        }
                        return nextTicket;
                    }
                }
            });
            if (!disposition.get()) {
                NativeDeAiTimeline.finish(ticket.state, NativeDeAiTimers.quiet(ticket.registry.life), work.owner());
            }
            if (ticket2 != null) {
                NativeDeAiWorkers.dispatch(ticket2);
            }
        }
    }

    static void retireTimeline(Object object) {
        Set<Ticket> set = timelines.get(object);
        if (set != null) {
            for (Ticket ticket : (Ticket[])set.toArray(Ticket[]::new)) {
                ticket.close(true);
            }
        }
    }

    static void retireGeneration(Object object) {
        Set<Ticket> set = generations.get(object);
        if (set != null) {
            for (Ticket ticket : (Ticket[])set.toArray(Ticket[]::new)) {
                ticket.close(true);
            }
        }
    }

    static Snapshot snapshot(GameHub gameHub) {
        NativeDeAiTimers.Registry registry = NativeDeAiTimers.registry(NativePlayerLifetime.of(gameHub));
        return new Snapshot(registry.workers.size(), submitted.get(), rejected.get(), retries.get(), notices.get(), active.get(), background.get(), discarded.get(), lastThread);
    }

    static final class Ticket {
        final GameHub hub;
        final NativeDeAiTimers.Work work;
        final NativeDeAiTimeline.State state;
        final NativeDeAiTimers.Registry registry;
        final NativeDeAiTimers.Credit credit;
        final AtomicBoolean closed = new AtomicBoolean();

        Ticket(GameHub gameHub, NativeDeAiTimers.Work work, NativeDeAiTimeline.State state, NativeDeAiTimers.Registry registry, NativeDeAiTimers.Credit credit) {
            this.hub = gameHub;
            this.work = work;
            this.state = state;
            this.registry = registry;
            this.credit = credit;
        }

        boolean detach() {
            if (!this.closed.compareAndSet(false, true)) {
                return false;
            }
            this.registry.workers.remove(this);
            NativeDeAiWorkers.unindex(timelines, this.work.lease().timeline(), this);
            NativeDeAiWorkers.unindex(generations, this.work.generation(), this);
            return true;
        }

        void close(boolean bl) {
            if (!this.detach()) {
                return;
            }
            this.credit.release();
            if (bl) {
                discarded.incrementAndGet();
                NativeDeAiWorkerProtocol.discard(this.state, NativeDeAiWorkers.port(this), this.work.owner());
            }
        }
    }

    record Snapshot(int registrations, long submitted, long rejected, long retries, long notices, long active, long background, long discarded, String lastThread) {
    }

    private static final class Task
    implements Runnable {
        final Ticket ticket;

        Task(Ticket ticket) {
            this.ticket = ticket;
        }

        @Override
        public void run() {
            Ticket ticket = this.ticket;
            try {
                if (!ticket.closed.get()) {
                    ticket.registry.life.executeCommitted(() -> NativeDeAiWorkerProtocol.run(ticket.state, NativeDeAiWorkers.port(ticket), ticket.work.owner(), ticket.work.background()));
                }
            }
            catch (Throwable throwable) {
                CombatNpcDamageQueue.failed(throwable);
                ticket.close(true);
            }
            finally {
                ticket.close(false);
            }
        }
    }
}
