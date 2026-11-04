// DECOMPILED from base-0.6.71.jar, TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import local.lito381.server.BattleTimedStatus;
import local.lito381.server.CombatAccess;
import local.lito381.server.NativeDeActorState;
import local.lito381.server.NativeDeAiTimeline;
import local.lito381.server.NativeDeAiTimers;

final class NativeDeAiState {
    static final String FIELD = "sourceAiTimeline";

    NativeDeAiState() {
    }

    private static Holder owned(BattleTimedStatus battleTimedStatus) {
        if (!Thread.holdsLock(battleTimedStatus)) {
            throw new IllegalStateException("AI state requires NPC status monitor");
        }
        NativeDeActorState.Lease lease = NativeDeActorState.acquire(battleTimedStatus);
        Object object = CombatAccess.field(battleTimedStatus, "sourceDeActorState");
        Holder holder = (Holder)CombatAccess.field(object, FIELD);
        if (holder == null) {
            holder = new Holder(lease);
            CombatAccess.set(object, FIELD, holder);
        }
        return holder;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Lease acquire(BattleTimedStatus battleTimedStatus) {
        synchronized (battleTimedStatus) {
            return NativeDeAiState.owned(battleTimedStatus).lease();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean current(BattleTimedStatus battleTimedStatus, Lease lease) {
        synchronized (battleTimedStatus) {
            return NativeDeAiState.owned(battleTimedStatus).matches(lease);
        }
    }

    private static View view(Holder holder) {
        NativeDeAiTimeline.State state = holder.core;
        return new View(holder.lease(), state.owner.get(), state.generation.get(), state.chain, state.activeTimer, state.timerSequence, state.nextActionMillis, state.retry, state.sleep, state.pending, state.executing, state.stop, state.wake, state.restart, state.pendingBackground, state.background);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static View snapshot(BattleTimedStatus battleTimedStatus) {
        synchronized (battleTimedStatus) {
            return NativeDeAiState.view(NativeDeAiState.owned(battleTimedStatus));
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Result perform(BattleTimedStatus battleTimedStatus, Lease lease, final Facts facts, Request request) {
        Objects.requireNonNull(facts);
        Objects.requireNonNull(request);
        synchronized (battleTimedStatus) {
            Holder holder = NativeDeAiState.owned(battleTimedStatus);
            if (!holder.matches(lease)) {
                return null;
            }
            NativeDeAiTimeline.State state = holder.core;
            final ArrayList<Effect> effects = new ArrayList<Effect>();
            NativeDeAiTimeline.Port port = new NativeDeAiTimeline.Port(){

                @Override
                public boolean dead() {
                    return facts.dead();
                }

                @Override
                public boolean destroyed() {
                    return facts.destroyed();
                }

                @Override
                public int hp() {
                    return facts.hp();
                }

                @Override
                public boolean shutdown() {
                    return facts.shutdown();
                }

                @Override
                public long nowMillis() {
                    return facts.nowMillis();
                }

                @Override
                public void removeBackground() {
                    effects.add(new Effect(Kind.REMOVE_BACKGROUND, 0L, 0, false, null));
                }

                @Override
                public void addBackground() {
                    effects.add(new Effect(Kind.ADD_BACKGROUND, 0L, 0, true, null));
                }

                @Override
                public void dispatch(long l, boolean bl) {
                    effects.add(new Effect(Kind.WORKER, l, 0, bl, null));
                }

                @Override
                public Object schedule(long l, int n, boolean bl) {
                    effects.add(new Effect(Kind.TIMER, l, n, bl, null));
                    return null;
                }

                @Override
                public boolean done(Object object) {
                    return NativeDeAiTimers.done(object);
                }

                @Override
                public void cancel(Object object) {
                    effects.add(new Effect(Kind.CANCEL_TIMER, 0L, 0, false, object));
                }
            };
            long l = 0L;
            switch (request.op()) {
                case START: {
                    NativeDeAiTimeline.start(state, port);
                    break;
                }
                case STOP: {
                    NativeDeAiTimeline.stop(state, port, request.owner());
                    break;
                }
                case FINISH: {
                    NativeDeAiTimeline.finish(state, port, request.owner());
                    break;
                }
                case WAKEUP: {
                    NativeDeAiTimeline.wakeup(state, port);
                    break;
                }
                case BEGIN_ACTIVE: 
                case BEGIN_BACKGROUND: {
                    l = NativeDeAiTimeline.begin(state, request.owner(), request.op() == Op.BEGIN_BACKGROUND) ? 1L : 0L;
                    break;
                }
                case BACKGROUND: {
                    NativeDeAiTimeline.background(state, port, request.owner());
                    break;
                }
                case TIMER_ACTIVE: 
                case TIMER_BACKGROUND: {
                    NativeDeAiTimeline.timer(state, port, request.owner(), request.timer(), request.op() == Op.TIMER_BACKGROUND);
                    break;
                }
                case FAIL_TIMER: {
                    NativeDeAiTimeline.failTimer(state, port, request.owner(), request.timer());
                    break;
                }
                case RETRY_ACTIVE: 
                case RETRY_BACKGROUND: {
                    l = NativeDeAiTimeline.retry(state, request.owner(), request.op() == Op.RETRY_BACKGROUND);
                    break;
                }
                case NEXT: {
                    NativeDeAiTimeline.next(state, port, request.owner());
                    break;
                }
                case ENSURE: {
                    NativeDeAiTimeline.ensure(state, port);
                }
            }
            return new Result(NativeDeAiState.view(holder), l, effects);
        }
    }

    private static final class Holder {
        final Object life;
        final Object state;
        final Object timeline = new Object();
        final NativeDeAiTimeline.State core = new NativeDeAiTimeline.State();

        Holder(NativeDeActorState.Lease lease) {
            this.life = lease.life();
            this.state = lease.state();
        }

        Lease lease() {
            return new Lease(this.life, this.state, this.timeline);
        }

        boolean matches(Lease lease) {
            return lease != null && this.life == lease.life() && this.state == lease.state() && this.timeline == lease.timeline();
        }
    }

    record Lease(Object life, Object state, Object timeline) {
    }

    record View(Lease lease, long owner, long generation, long chain, long activeTimer, long timerSequence, long nextActionMillis, int retry, int sleep, boolean pending, boolean executing, boolean stopped, boolean wake, boolean restart, boolean pendingBackground, boolean background) {
    }

    record Facts(boolean dead, boolean destroyed, int hp, boolean shutdown, long nowMillis) {
    }

    record Request(Op op, long owner, long timer) {
    }

    static enum Op {
        START,
        STOP,
        FINISH,
        WAKEUP,
        BEGIN_ACTIVE,
        BEGIN_BACKGROUND,
        BACKGROUND,
        TIMER_ACTIVE,
        TIMER_BACKGROUND,
        FAIL_TIMER,
        RETRY_ACTIVE,
        RETRY_BACKGROUND,
        NEXT,
        ENSURE;

    }

    record Result(View view, long value, List<Effect> effects) {
        Result {
            effects = List.copyOf(effects);
        }
    }

    record Effect(Kind kind, long owner, int delay, boolean background, Object timer) {
    }

    static enum Kind {
        WORKER,
        TIMER,
        ADD_BACKGROUND,
        REMOVE_BACKGROUND,
        CANCEL_TIMER;

    }
}
