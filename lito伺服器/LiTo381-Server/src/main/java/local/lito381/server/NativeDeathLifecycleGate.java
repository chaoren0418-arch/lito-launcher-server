// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.concurrent.atomic.AtomicReference;

public final class NativeDeathLifecycleGate {
    private final AtomicReference<State> _state = new AtomicReference<State>(new State(Phase.ACTIVE, 0L));

    public State snapshot() {
        return this._state.get();
    }

    public boolean isManaged() {
        State state = this.snapshot();
        return state.generation() != 0L || state.phase() != Phase.ACTIVE;
    }

    public boolean markDeadPending(long l) {
        if (l == 0L) {
            return false;
        }
        State state = this.snapshot();
        return state.phase() == Phase.ACTIVE && this._state.compareAndSet(state, new State(Phase.DEAD_PENDING, l));
    }

    public boolean isPending(long l) {
        State state = this.snapshot();
        return l != 0L && state.generation() == l && state.phase() == Phase.DEAD_PENDING;
    }

    public boolean isDeathGeneration(long l) {
        State state = this.snapshot();
        return l != 0L && state.generation() == l && state.phase() != Phase.ACTIVE && state.phase() != Phase.TERMINATED;
    }

    public Ownership ownership(long l) {
        return new Ownership(this, l);
    }

    public ResurrectionClaim tryReserveResurrection() {
        State state = this.snapshot();
        if (state.phase() != Phase.DEAD_PENDING && state.phase() != Phase.DEAD_SETTLED && state.phase() != Phase.DEATH_DROPPED) {
            return null;
        }
        State state2 = new State(Phase.RESURRECTING, state.generation());
        return this._state.compareAndSet(state, state2) ? new ResurrectionClaim(state2) : null;
    }

    public boolean completeResurrection(ResurrectionClaim resurrectionClaim) {
        return resurrectionClaim != null && this._state.compareAndSet(resurrectionClaim.state(), new State(Phase.ACTIVE, resurrectionClaim.generation()));
    }

    public boolean abortResurrection(ResurrectionClaim resurrectionClaim) {
        return resurrectionClaim != null && this._state.compareAndSet(resurrectionClaim.state(), new State(Phase.DEAD_SETTLED, resurrectionClaim.generation()));
    }

    public boolean reviveDirect(State state) {
        if (state == null || state.phase() != Phase.DEAD_PENDING && state.phase() != Phase.DEAD_SETTLED && state.phase() != Phase.DEATH_DROPPED) {
            return false;
        }
        return this._state.compareAndSet(state, new State(Phase.ACTIVE, state.generation()));
    }

    public boolean terminate(long l) {
        State state = this.snapshot();
        if (state.generation() != l || state.phase() == Phase.RESURRECTING) {
            return false;
        }
        return this._state.compareAndSet(state, new State(Phase.TERMINATED, l));
    }

    private State tryClaimFinalizer(long l) {
        State state = this.snapshot();
        if (state.phase() != Phase.DEAD_PENDING || state.generation() != l) {
            return null;
        }
        State state2 = new State(Phase.FINALIZING, l);
        return this._state.compareAndSet(state, state2) ? state2 : null;
    }

    private boolean markDropped(long l) {
        State state = this.snapshot();
        return state.phase() == Phase.DEAD_PENDING && state.generation() == l && this._state.compareAndSet(state, new State(Phase.DEATH_DROPPED, l));
    }

    private boolean completeFinalizer(State state, boolean bl) {
        if (state == null || state.phase() != Phase.FINALIZING) {
            return false;
        }
        Phase phase = bl ? Phase.DEAD_SETTLED : Phase.DEAD_PENDING;
        return this._state.compareAndSet(state, new State(phase, state.generation()));
    }

    public record State(Phase phase, long generation) {
    }

    public static enum Phase {
        ACTIVE,
        DEAD_PENDING,
        RESURRECTING,
        FINALIZING,
        DEAD_SETTLED,
        DEATH_DROPPED,
        TERMINATED;

    }

    public static final class Ownership {
        private final NativeDeathLifecycleGate _gate;
        private final long _generation;
        private final AtomicReference<State> _claim = new AtomicReference();

        private Ownership(NativeDeathLifecycleGate nativeDeathLifecycleGate, long l) {
            this._gate = nativeDeathLifecycleGate;
            this._generation = l;
        }

        public long generation() {
            return this._generation;
        }

        public boolean tryClaim() {
            State state = this._claim.get();
            if (state != null) {
                return this._gate._state.get() == state;
            }
            State state2 = this._gate.tryClaimFinalizer(this._generation);
            if (state2 == null) {
                return false;
            }
            if (this._claim.compareAndSet(null, state2)) {
                return true;
            }
            this._gate.completeFinalizer(state2, false);
            State state3 = this._claim.get();
            return state3 != null && this._gate._state.get() == state3;
        }

        public boolean isOwned() {
            State state = this._claim.get();
            return state != null && this._gate._state.get() == state;
        }

        public boolean complete(boolean bl) {
            State state = this._claim.getAndSet(null);
            return state != null && this._gate.completeFinalizer(state, bl);
        }

        public boolean abandon() {
            State state = this._claim.getAndSet(null);
            if (state != null) {
                this._gate.completeFinalizer(state, false);
            }
            return this._gate.markDropped(this._generation);
        }
    }

    public record ResurrectionClaim(State state) {
        public long generation() {
            return this.state.generation();
        }
    }
}
