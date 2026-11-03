// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import local.lito381.server.BattleRules;
import local.lito381.server.BattleTimedStatus;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatNpcDamageQueue;
import local.lito381.server.GameHub;
import local.lito381.server.NativeCombatAttacker;
import local.lito381.server.NativeDeAiState;
import local.lito381.server.NativeDeAiTimeline;
import local.lito381.server.NativeDeAiTimerProtocol;
import local.lito381.server.NativeDeAiWorkers;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativePlayerLifetime;
import local.lito381.server.NativeProcHealingEncounter;

final class NativeDeAiTimers {
    static final String FIELD = "sourceDeAiTimers";
    static final String P = "local/lito381/server/";
    private static final ConcurrentHashMap<Object, Entry> tickets = new ConcurrentHashMap();
    private static final ConcurrentHashMap<Object, Set<Entry>> timelines = new ConcurrentHashMap();
    private static final ConcurrentHashMap<Object, Set<Entry>> generations = new ConcurrentHashMap();
    private static final AtomicLong scheduled = new AtomicLong();
    private static final AtomicLong fired = new AtomicLong();
    private static final AtomicLong cancelled = new AtomicLong();
    private static final AtomicLong rejected = new AtomicLong();
    private static final AtomicLong stale = new AtomicLong();

    NativeDeAiTimers() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static <T> T withTarget(GameHub gameHub, NativeCombatAttacker.Ref ref, Object object, NativeDeAiState.Lease lease, Function<Live, T> function) {
        if (ref == null || ref.kind() != 3 && ref.kind() != 4) {
            return null;
        }
        GameHub gameHub2 = gameHub;
        synchronized (gameHub2) {
            Object object2;
            Object object3 = NativeProcHealingEncounter.room(gameHub, ref.room());
            if (!NativeProcHealingEncounter.mode(object3, ref) || ((BattleRules.MatchState)CombatAccess.field(object3, "match")).phase() != BattleRules.Phase.RUNNING) {
                return null;
            }
            Object object4 = object2 = NativeProcHealingEncounter.container(object3, ref);
            synchronized (object4) {
                BattleTimedStatus battleTimedStatus;
                if (NativeProcHealingEncounter.generation(object2) != object) {
                    return null;
                }
                Object object5 = NativeProcHealingEncounter.actor(object2, ref);
                if (object5 == null) {
                    return null;
                }
                BattleTimedStatus battleTimedStatus2 = battleTimedStatus = (BattleTimedStatus)CombatAccess.field(object5, "damagePoison");
                synchronized (battleTimedStatus2) {
                    if (NativeCombatAttacker.token(battleTimedStatus, false) != ref.owner() || NativePlayerCommit.integer(object5, "hp") <= 0 || !NativeDeAiState.current(battleTimedStatus, lease)) {
                        return null;
                    }
                    return function.apply(new Live(object5, battleTimedStatus));
                }
            }
        }
    }

    static NativeDeAiState.Facts facts(Live live, NativePlayerLifetime nativePlayerLifetime) {
        return new NativeDeAiState.Facts(false, false, NativePlayerCommit.integer(live.actor(), "hp"), nativePlayerLifetime.mode != NativePlayerLifetime.Mode.OPEN, System.currentTimeMillis());
    }

    static NativeDeAiTimeline.State state(BattleTimedStatus battleTimedStatus) {
        NativeDeAiState.acquire(battleTimedStatus);
        Object object = CombatAccess.field(battleTimedStatus, "sourceDeActorState");
        Object object2 = CombatAccess.field(object, "sourceAiTimeline");
        return (NativeDeAiTimeline.State)CombatAccess.field(object2, "core");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Registry registry(NativePlayerLifetime nativePlayerLifetime) {
        Object object = nativePlayerLifetime.admission;
        synchronized (object) {
            Registry registry = (Registry)CombatAccess.field(nativePlayerLifetime, FIELD);
            if (registry == null) {
                registry = new Registry(nativePlayerLifetime);
                CombatAccess.set(nativePlayerLifetime, FIELD, registry);
            }
            return registry;
        }
    }

    private static void index(ConcurrentHashMap<Object, Set<Entry>> concurrentHashMap, Object object2, Entry entry) {
        concurrentHashMap.compute(object2, (object, keySetView) -> {
            if (keySetView == null) {
                keySetView = ConcurrentHashMap.newKeySet();
            }
            keySetView.add(entry);
            return keySetView;
        });
    }

    private static void unindex(ConcurrentHashMap<Object, Set<Entry>> concurrentHashMap, Object object2, Entry entry) {
        concurrentHashMap.computeIfPresent(object2, (object, set) -> {
            set.remove(entry);
            return set.isEmpty() ? null : set;
        });
    }

    static NativeDeAiTimeline.Port quiet(final NativePlayerLifetime nativePlayerLifetime) {
        return new NativeDeAiTimeline.Port(){

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
                return nativePlayerLifetime.mode != NativePlayerLifetime.Mode.OPEN;
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
            public void dispatch(long l, boolean bl) {
                throw new IllegalStateException("AI worker admission not connected");
            }

            @Override
            public Object schedule(long l, int n, boolean bl) {
                throw new IllegalStateException("use timer protocol");
            }

            @Override
            public boolean done(Object object) {
                return NativeDeAiTimers.done(object);
            }

            @Override
            public void cancel(Object object) {
                NativeDeAiTimers.cancelFuture(object);
            }
        };
    }

    static Object schedule(GameHub gameHub, NativeCombatAttacker.Ref ref, Object object, NativeDeAiState.Lease lease, long l, int n, boolean bl) {
        return NativeDeAiTimers.schedule(gameHub, ref, object, lease, l, n, bl, null);
    }

    static Object reschedule(NativeDeAiWorkers.Ticket ticket, int n) {
        Work work = ticket.work;
        Object object = NativeDeAiTimers.schedule(ticket.hub, work.target(), work.generation(), work.lease(), work.owner(), n, work.background(), ticket);
        if (!ticket.closed.get()) {
            ticket.close(object == null);
        }
        return object;
    }

    private static Object schedule(final GameHub gameHub, final NativeCombatAttacker.Ref ref, final Object object, final NativeDeAiState.Lease lease, long l, int n, boolean bl, final NativeDeAiWorkers.Ticket ticket) {
        final NativePlayerLifetime nativePlayerLifetime = NativePlayerLifetime.of(gameHub);
        final NativeDeAiTimeline.State state = NativeDeAiTimers.withTarget(gameHub, ref, object, lease, live -> NativeDeAiTimers.state(live.status()));
        if (state == null) {
            return null;
        }
        final Registry registry = NativeDeAiTimers.registry(nativePlayerLifetime);
        return NativeDeAiTimerProtocol.schedule(state, new NativeDeAiTimerProtocol.Port(){

            @Override
            public boolean dead() {
                return false;
            }

            @Override
            public boolean destroyed() {
                return false;
            }

            @Override
            public int hp() {
                return 1;
            }

            @Override
            public boolean shutdown() {
                return nativePlayerLifetime.mode != NativePlayerLifetime.Mode.OPEN;
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
            public void dispatch(long l, boolean bl) {
                throw new IllegalStateException("AI worker admission not connected");
            }

            @Override
            public Object schedule(long l, int n, boolean bl) {
                throw new IllegalStateException("recursive timer protocol");
            }

            @Override
            public boolean done(Object object2) {
                return NativeDeAiTimers.done(object2);
            }

            @Override
            public void cancel(Object object2) {
                NativeDeAiTimers.cancelFuture(object2);
            }

            @Override
            public void failure(NativeDeAiTimerProtocol.Failure failure) {
                rejected.incrementAndGet();
            }

            @Override
            public Object install(long l, long l2, boolean bl, int n) {
                Entry entry = NativeDeAiTimers.withTarget(gameHub, ref, object, lease, live -> {
                    Object object2 = state2.lock;
                    synchronized (object2) {
                        if (!NativeDeAiTimeline.owns(state, l) || state2.chain != l || state2.activeTimer != l2) {
                            return null;
                        }
                        Object object3 = nativePlayerLifetime2.admission;
                        synchronized (object3) {
                            Credit credit;
                            if (nativePlayerLifetime2.mode != NativePlayerLifetime.Mode.OPEN || ticket == null && nativePlayerLifetime2.pending.get() >= NativePlayerCommit.GLOBAL || ticket != null && ticket2.closed.get()) {
                                return null;
                            }
                            if (ticket == null) {
                                nativePlayerLifetime2.pending.incrementAndGet();
                                credit = new Credit(nativePlayerLifetime);
                            } else {
                                credit = ticket2.credit;
                                if (!ticket.detach()) {
                                    return null;
                                }
                            }
                            Entry entry = new Entry(registry, gameHub, new Work(ref, object, lease, l, bl), state, l2, credit);
                            registry2.entries.add(entry);
                            tickets.put(entry.id, entry);
                            NativeDeAiTimers.index(timelines, lease.timeline(), entry);
                            NativeDeAiTimers.index(generations, object, entry);
                            return entry;
                        }
                    }
                });
                if (entry == null) {
                    return null;
                }
                try {
                    ScheduledFuture<?> scheduledFuture = nativePlayerLifetime.retry.schedule(entry, (long)n, TimeUnit.MILLISECONDS);
                    if (scheduledFuture == null) {
                        entry.close(false);
                        return null;
                    }
                    entry.future(scheduledFuture);
                    scheduled.incrementAndGet();
                    return entry.id;
                }
                catch (RuntimeException runtimeException) {
                    entry.close(false);
                    throw runtimeException;
                }
            }
        }, l, n, bl);
    }

    static boolean done(Object object) {
        Entry entry = tickets.get(object);
        return entry == null || entry.done();
    }

    static void cancelFuture(Object object) {
        Entry entry = tickets.get(object);
        if (entry != null && !entry.ready) {
            entry.close(false);
        }
    }

    static List<Work> claimReady(GameHub gameHub) {
        Entry entry;
        Registry registry = NativeDeAiTimers.registry(NativePlayerLifetime.of(gameHub));
        ArrayList<Work> arrayList = new ArrayList<Work>();
        while ((entry = registry.ready.poll()) != null) {
            Work work;
            if (entry.closed.get()) continue;
            Entry entry2 = entry;
            Work work2 = work = registry.life.mode != NativePlayerLifetime.Mode.OPEN ? null : NativeDeAiTimers.withTarget(gameHub, entry2.work.target(), entry2.work.generation(), entry2.work.lease(), live -> {
                NativeDeAiTimeline.State state = NativeDeAiTimers.state(live.status());
                Object object = state.lock;
                synchronized (object) {
                    return NativeDeAiTimeline.owns(state, entry.work.owner()) && state.chain == entry.work.owner() && state.pending && state.pendingBackground == entry.work.background() ? entry.work : null;
                }
            });
            if (work != null) {
                arrayList.add(work);
            } else {
                stale.incrementAndGet();
            }
            entry2.close(false);
        }
        return List.copyOf(arrayList);
    }

    static NativeDeAiState.Result perform(GameHub gameHub, NativeCombatAttacker.Ref ref, Object object, NativeDeAiState.Lease lease, NativeDeAiState.Request request) {
        NativePlayerLifetime nativePlayerLifetime = NativePlayerLifetime.of(gameHub);
        NativeDeAiState.Result result = NativeDeAiTimers.withTarget(gameHub, ref, object, lease, live -> NativeDeAiState.perform(live.status(), lease, NativeDeAiTimers.facts(live, nativePlayerLifetime), request));
        if (result != null) {
            for (NativeDeAiState.Effect effect : result.effects()) {
                if (effect.kind() != NativeDeAiState.Kind.CANCEL_TIMER) continue;
                NativeDeAiTimers.cancelFuture(effect.timer());
            }
        }
        return result;
    }

    static void retireActorState(Object object) {
        if (object == null) {
            return;
        }
        Object object2 = CombatAccess.field(object, "sourceAiTimeline");
        if (object2 == null) {
            return;
        }
        Object object3 = CombatAccess.field(object2, "timeline");
        NativeDeAiWorkers.retireTimeline(object3);
        Set<Entry> set = timelines.get(object3);
        if (set != null) {
            for (Entry entry : (Entry[])set.toArray(Entry[]::new)) {
                entry.close(true);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void retireStatus(BattleTimedStatus battleTimedStatus) {
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
        synchronized (battleTimedStatus2) {
            NativeDeAiTimers.retireActorState(CombatAccess.field(battleTimedStatus, "sourceDeActorState"));
        }
    }

    static void retireContainer(Object object) {
        Object object2 = CombatAccess.field(object, "sourceProcEncounterGeneration");
        if (object2 != null) {
            NativeDeAiWorkers.retireGeneration(object2);
            Set<Entry> set = generations.get(object2);
            if (set != null) {
                for (Entry entry : (Entry[])set.toArray(Entry[]::new)) {
                    entry.close(true);
                }
            }
        }
    }

    static void close(NativePlayerLifetime nativePlayerLifetime) {
        Registry registry = (Registry)CombatAccess.field(nativePlayerLifetime, FIELD);
        if (registry != null) {
            registry.workersEnabled = false;
            for (Entry object : (Entry[])registry.entries.toArray(Entry[]::new)) {
                object.close(true);
            }
            for (NativeDeAiWorkers.Ticket ticket : (NativeDeAiWorkers.Ticket[])registry.workers.toArray(NativeDeAiWorkers.Ticket[]::new)) {
                ticket.close(true);
            }
        }
    }

    static Snapshot snapshot(GameHub gameHub) {
        Registry registry = NativeDeAiTimers.registry(NativePlayerLifetime.of(gameHub));
        return new Snapshot(registry.entries.size(), registry.ready.size(), scheduled.get(), fired.get(), cancelled.get(), rejected.get(), stale.get());
    }

    record Live(Object actor, BattleTimedStatus status) {
    }

    static final class Registry {
        final NativePlayerLifetime life;
        volatile boolean workersEnabled;
        final Set<NativeDeAiWorkers.Ticket> workers = ConcurrentHashMap.newKeySet();
        final Set<Entry> entries = ConcurrentHashMap.newKeySet();
        final ConcurrentLinkedQueue<Entry> ready = new ConcurrentLinkedQueue();

        Registry(NativePlayerLifetime nativePlayerLifetime) {
            this.life = nativePlayerLifetime;
        }
    }

    static final class Entry
    implements Runnable {
        final Object id = new Object();
        final Registry registry;
        final GameHub hub;
        final Work work;
        final NativeDeAiTimeline.State state;
        final long timer;
        final Credit credit;
        final AtomicBoolean closed = new AtomicBoolean();
        volatile ScheduledFuture<?> future;
        volatile boolean ready;

        Entry(Registry registry, GameHub gameHub, Work work, NativeDeAiTimeline.State state, long l, Credit credit) {
            this.credit = credit;
            this.registry = registry;
            this.hub = gameHub;
            this.work = work;
            this.state = state;
            this.timer = l;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        void future(ScheduledFuture<?> scheduledFuture) {
            boolean bl;
            Entry entry = this;
            synchronized (entry) {
                boolean bl2 = bl = !this.closed.get() && !this.ready;
                if (bl) {
                    this.future = scheduledFuture;
                }
            }
            if (!bl && scheduledFuture != null && scheduledFuture.cancel(false)) {
                cancelled.incrementAndGet();
                this.registry.life.cancelledTimers.incrementAndGet();
            }
        }

        boolean done() {
            ScheduledFuture<?> scheduledFuture = this.future;
            return this.closed.get() || this.ready || scheduledFuture != null && scheduledFuture.isDone();
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        void close(boolean bl) {
            ScheduledFuture<?> scheduledFuture;
            if (!this.closed.compareAndSet(false, true)) {
                return;
            }
            Entry entry = this;
            synchronized (entry) {
                scheduledFuture = this.future;
                this.future = null;
            }
            if (scheduledFuture != null && !scheduledFuture.isDone() && scheduledFuture.cancel(false)) {
                cancelled.incrementAndGet();
                this.registry.life.cancelledTimers.incrementAndGet();
            }
            tickets.remove(this.id, this);
            this.registry.entries.remove(this);
            this.registry.ready.remove(this);
            NativeDeAiTimers.unindex(timelines, this.work.lease().timeline(), this);
            NativeDeAiTimers.unindex(generations, this.work.generation(), this);
            this.credit.release();
            if (bl) {
                NativeDeAiTimeline.stop(this.state, NativeDeAiTimers.quiet(this.registry.life), this.work.owner());
            }
        }

        boolean detach() {
            if (!this.closed.compareAndSet(false, true)) {
                return false;
            }
            this.future = null;
            tickets.remove(this.id, this);
            this.registry.entries.remove(this);
            this.registry.ready.remove(this);
            NativeDeAiTimers.unindex(timelines, this.work.lease().timeline(), this);
            NativeDeAiTimers.unindex(generations, this.work.generation(), this);
            return true;
        }

        @Override
        public void run() {
            try {
                this.fire();
            }
            catch (Throwable throwable) {
                CombatNpcDamageQueue.failed(throwable);
                this.close(true);
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        private void fire() {
            if (this.closed.get()) {
                return;
            }
            if (this.registry.life.mode != NativePlayerLifetime.Mode.OPEN) {
                this.close(true);
                return;
            }
            NativeDeAiState.Result result = NativeDeAiTimers.withTarget(this.hub, this.work.target(), this.work.generation(), this.work.lease(), live -> NativeDeAiState.perform(live.status(), this.work.lease(), NativeDeAiTimers.facts(live, this.registry.life), new NativeDeAiState.Request(this.work.background() ? NativeDeAiState.Op.TIMER_BACKGROUND : NativeDeAiState.Op.TIMER_ACTIVE, this.work.owner(), this.timer)));
            if (result == null) {
                stale.incrementAndGet();
                this.close(true);
                return;
            }
            fired.incrementAndGet();
            boolean bl = result.effects().stream().anyMatch(effect -> effect.kind() == NativeDeAiState.Kind.WORKER);
            if (!bl) {
                this.close(false);
                return;
            }
            Entry entry = this;
            synchronized (entry) {
                if (this.closed.get()) {
                    return;
                }
                this.future = null;
                this.ready = true;
            }
            this.registry.ready.add(this);
            if (this.closed.get()) {
                this.registry.ready.remove(this);
            } else if (this.registry.workersEnabled) {
                NativeDeAiWorkers.onReady(this.hub);
            }
        }
    }

    record Work(NativeCombatAttacker.Ref target, Object generation, NativeDeAiState.Lease lease, long owner, boolean background) {
    }

    record Snapshot(int registrations, int ready, long scheduled, long fired, long cancelled, long rejected, long stale) {
    }

    static final class Credit {
        final NativePlayerLifetime life;
        final AtomicBoolean released = new AtomicBoolean();

        Credit(NativePlayerLifetime nativePlayerLifetime) {
            this.life = nativePlayerLifetime;
        }

        void release() {
            if (this.released.compareAndSet(false, true)) {
                this.life.pending.decrementAndGet();
            }
        }
    }
}
