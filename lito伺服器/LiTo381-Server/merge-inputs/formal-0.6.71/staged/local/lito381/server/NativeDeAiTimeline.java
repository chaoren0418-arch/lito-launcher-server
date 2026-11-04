// DECOMPILED from base-0.6.71.jar, TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.concurrent.atomic.AtomicLong;

final class NativeDeAiTimeline {
    private NativeDeAiTimeline() {
    }

    static long acquire(State state) {
        if (state.owner.get() != 0L) {
            return 0L;
        }
        long l = state.generation.incrementAndGet();
        if (l == 0L) {
            l = state.generation.incrementAndGet();
        }
        return state.owner.compareAndSet(0L, l) ? l : 0L;
    }

    static boolean owns(State state, long l) {
        return l != 0L && state.owner.get() == l;
    }

    static boolean release(State state, long l) {
        return l != 0L && state.owner.compareAndSet(l, 0L);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void start(State state, Port port) {
        long l;
        if (port.dead() || port.destroyed() || port.hp() <= 0) {
            return;
        }
        synchronized (state.lock) {
            l = NativeDeAiTimeline.acquire(state);
            if (l == 0L) {
                if (state.stop && state.owner.get() == state.chain) {
                    state.restart = true;
                }
                return;
            }
            state.chain = l;
            state.pending = true;
            state.executing = false;
            state.stop = false;
            state.wake = false;
            state.restart = false;
            state.activeTimer = 0L;
            state.pendingBackground = false;
            state.retry = 0;
            state.future = null;
            state.background = false;
        }
        port.dispatch(l, false);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void stop(State state, Port port, long l) {
        if (l == 0L) {
            return;
        }
        synchronized (state.lock) {
            if (!NativeDeAiTimeline.owns(state, l) || state.chain != l) {
                return;
            }
            state.stop = true;
            state.wake = false;
            state.pending = false;
            state.pendingBackground = false;
            state.activeTimer = 0L;
            state.retry = 0;
            Object object2 = state.future;
            state.future = null;
            if (object2 != null && !port.done(object2)) {
                port.cancel(object2);
            }
            if (!state.executing) {
                NativeDeAiTimeline.completeStop(state, port, l);
            }
        }
    }

    static boolean completeStop(State state, Port port, long l) {
        if (!Thread.holdsLock(state.lock)) {
            throw new IllegalStateException("complete stop requires AI monitor");
        }
        if (!NativeDeAiTimeline.owns(state, l) || state.chain != l) {
            return false;
        }
        boolean bl = state.restart;
        state.future = null;
        state.pending = false;
        state.executing = false;
        state.pendingBackground = false;
        state.activeTimer = 0L;
        state.retry = 0;
        state.stop = false;
        state.wake = false;
        state.restart = false;
        state.chain = 0L;
        state.background = false;
        state.nextActionMillis = 0L;
        port.removeBackground();
        NativeDeAiTimeline.release(state, l);
        return bl;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void finish(State state, Port port, long l) {
        boolean bl = false;
        boolean bl2 = false;
        synchronized (state.lock) {
            if (state.chain != l) {
                return;
            }
            state.executing = false;
            if (state.stop) {
                bl2 = NativeDeAiTimeline.completeStop(state, port, l);
            } else if (state.wake) {
                state.wake = false;
                if (!state.pending) {
                    state.background = false;
                    port.removeBackground();
                    state.pending = true;
                    state.pendingBackground = false;
                    bl = true;
                }
            }
        }
        if (bl2) {
            NativeDeAiTimeline.start(state, port);
        } else if (bl) {
            port.dispatch(l, false);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean begin(State state, long l, boolean bl) {
        synchronized (state.lock) {
            if (!NativeDeAiTimeline.owns(state, l) || state.chain != l || !state.pending || state.pendingBackground != bl || state.executing || state.stop || state.activeTimer != 0L) {
                return false;
            }
            state.pending = false;
            state.executing = true;
            state.retry = 0;
            return true;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void background(State state, Port port, long l) {
        synchronized (state.lock) {
            if (!NativeDeAiTimeline.owns(state, l) || state.chain != l || state.stop) {
                return;
            }
            state.background = true;
            state.pending = false;
            state.pendingBackground = false;
            state.activeTimer = 0L;
            state.future = null;
            port.addBackground();
        }
    }

    static long nextTimer(State state) {
        long l;
        ++state.timerSequence;
        if ((l = state.timerSequence++) == 0L) {
            l = state.timerSequence;
        }
        return l;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void timer(State state, Port port, long l, long l2, boolean bl) {
        synchronized (state.lock) {
            if (!NativeDeAiTimeline.owns(state, l) || state.chain != l || state.stop || state.activeTimer != l2 || !state.pending || state.pendingBackground != bl) {
                return;
            }
            state.activeTimer = 0L;
            state.future = null;
            if (state.executing) {
                state.pending = false;
                state.wake = true;
                return;
            }
        }
        port.dispatch(l, bl);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void failTimer(State state, Port port, long l, long l2) {
        boolean bl = false;
        synchronized (state.lock) {
            if (NativeDeAiTimeline.owns(state, l) && state.chain == l && state.activeTimer == l2) {
                state.activeTimer = 0L;
                state.future = null;
                bl = true;
            }
        }
        if (bl) {
            NativeDeAiTimeline.stop(state, port, l);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static int retry(State state, long l, boolean bl) {
        synchronized (state.lock) {
            if (!NativeDeAiTimeline.owns(state, l) || state.chain != l || state.stop || !state.pending || state.pendingBackground != bl) {
                return -1;
            }
            state.retry = Math.min(state.retry + 1, 31);
            int n = Math.min(5, state.retry - 1);
            return Math.min(1000, 25 << n);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void next(State state, Port port, long l) {
        if (port.shutdown()) {
            NativeDeAiTimeline.stop(state, port, l);
            return;
        }
        boolean bl = false;
        synchronized (state.lock) {
            if (NativeDeAiTimeline.owns(state, l) && state.chain == l && !state.stop && !state.pending && state.activeTimer == 0L) {
                state.pending = true;
                state.pendingBackground = false;
                bl = true;
            }
        }
        if (bl) {
            port.schedule(l, state.sleep, false);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void ensure(State state, Port port) {
        long l = state.owner.get();
        if (l == 0L) {
            NativeDeAiTimeline.start(state, port);
            return;
        }
        boolean bl = false;
        synchronized (state.lock) {
            if (!NativeDeAiTimeline.owns(state, l) || state.chain != l || state.stop) {
                return;
            }
            if (state.executing) {
                state.wake = true;
                return;
            }
            if (!state.pending) {
                state.pending = true;
                state.pendingBackground = false;
                bl = true;
            }
        }
        if (bl) {
            state.sleep = 300;
            port.schedule(l, state.sleep, false);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void wakeup(State state, Port port) {
        if (port.nowMillis() < state.nextActionMillis) {
            return;
        }
        long l = state.owner.get();
        if (l == 0L) {
            NativeDeAiTimeline.start(state, port);
            return;
        }
        boolean bl = false;
        synchronized (state.lock) {
            if (!NativeDeAiTimeline.owns(state, l) || state.chain != l || state.stop) {
                return;
            }
            state.background = false;
            port.removeBackground();
            if (state.executing) {
                state.wake = true;
                return;
            }
            if (state.pending) {
                if (state.pendingBackground) {
                    state.wake = true;
                }
                return;
            }
            state.pending = true;
            state.pendingBackground = false;
            bl = true;
        }
        if (bl) {
            state.sleep = 300;
            port.schedule(l, state.sleep, false);
        }
    }

    static final class State {
        final Object lock = new Object();
        final AtomicLong owner = new AtomicLong();
        final AtomicLong generation = new AtomicLong();
        long chain;
        long timerSequence;
        long activeTimer;
        long nextActionMillis;
        int retry;
        int sleep;
        boolean pending;
        boolean executing;
        boolean stop;
        boolean wake;
        boolean restart;
        boolean pendingBackground;
        boolean background;
        Object future;

        State() {
        }
    }

    static interface Port {
        public boolean dead();

        public boolean destroyed();

        public int hp();

        public boolean shutdown();

        public long nowMillis();

        public void removeBackground();

        public void addBackground();

        public void dispatch(long var1, boolean var3);

        public Object schedule(long var1, int var3, boolean var4);

        public boolean done(Object var1);

        public void cancel(Object var1);
    }
}
