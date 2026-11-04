// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  local.lito381.server.CombatAccess
 *  local.lito381.server.CombatMonsterCurseLifecycle$Actor
 *  local.lito381.server.CombatNpcDamageQueue$Event
 *  local.lito381.server.CombatNpcDamageQueue$Queue
 *  local.lito381.server.GameHub
 *  local.lito381.server.NativeSourceCubeNpcQueue
 *  local.lito381.server.NativeSourceDamageCapacity
 *  local.lito381.server.NativeSourcePendingAdmission
 *  local.lito381.server.NativeSourcePhysicalNpc
 *  local.lito381.server.NativeSourcePoisonNpc
 */
package local.lito381.server;

import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatMonsterCurseLifecycle;
import local.lito381.server.CombatNpcDamageQueue;
import local.lito381.server.GameHub;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativePlayerLifetime;
import local.lito381.server.NativeSourceCubeNpcQueue;
import local.lito381.server.NativeSourceDamageCapacity;
import local.lito381.server.NativeSourcePendingAdmission;
import local.lito381.server.NativeSourcePhysicalNpc;
import local.lito381.server.NativeSourcePoisonNpc;

final class NativeNpcLifetime {
    static final String FIELD = "sourceDamageLifetime";

    NativeNpcLifetime() {
    }

    static Lane lane(CombatNpcDamageQueue.Queue queue) {
        return (Lane)CombatAccess.field((Object)queue, (String)FIELD);
    }

    static Lane lane(CombatNpcDamageQueue.Queue queue, GameHub gameHub) {
        Lane lane = NativeNpcLifetime.lane(queue);
        NativePlayerLifetime nativePlayerLifetime = NativePlayerLifetime.of(gameHub);
        if (lane == null) {
            lane = new Lane(queue, nativePlayerLifetime);
            CombatAccess.set((Object)queue, (String)FIELD, (Object)lane);
        }
        if (lane.life != nativePlayerLifetime) {
            throw new IllegalStateException("NPC queue changed applications");
        }
        return lane;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean nativeSourceCubeQueueOriginalOffer(CombatNpcDamageQueue.Queue queue, CombatNpcDamageQueue.Event event, GameHub gameHub) {
        CombatNpcDamageQueue.Queue queue2 = queue;
        synchronized (queue2) {
            Lane lane = NativeNpcLifetime.lane(queue, gameHub);
            Object object = lane.life.admission;
            synchronized (object) {
                if (!lane.life.admits() || lane.life.pending.get() >= NativePlayerCommit.GLOBAL) {
                    return false;
                }
                if (!queue.offer(event)) {
                    return false;
                }
                lane.deadlines.put(event, NativePlayerLifetime.deadline());
                lane.life.pending.incrementAndGet();
                lane.life.npcs.add(lane);
                return true;
            }
        }
    }

    static long globalLimit(long l, CombatNpcDamageQueue.Queue queue) {
        return NativeNpcLifetime.lane(queue) == null ? l : Long.MAX_VALUE;
    }

    static int targetLimit(int n, CombatNpcDamageQueue.Queue queue) {
        return NativeNpcLifetime.lane(queue) == null ? n : n + (CombatAccess.field((Object)queue, (String)"active") == null ? 0 : 1);
    }

    static long delay(Lane lane) {
        if (lane.life.mode != NativePlayerLifetime.Mode.OPEN) {
            return 0L;
        }
        Object e = ((ArrayDeque)CombatAccess.field((Object)lane.queue, (String)"events")).peek();
        Long l = lane.deadlines.get(e);
        return l == null || l == 0L ? 0L : Math.max(0L, l - System.nanoTime());
    }

    static boolean future(CombatNpcDamageQueue.Queue queue) {
        Lane lane = NativeNpcLifetime.lane(queue);
        return lane != null && NativeNpcLifetime.delay(lane) > 0L;
    }

    static CombatNpcDamageQueue.Event claimed(CombatNpcDamageQueue.Event event, CombatNpcDamageQueue.Queue queue) {
        Lane lane = NativeNpcLifetime.lane(queue);
        if (lane != null && event != null) {
            if (lane.deadlines.remove(event) == null) {
                throw new IllegalStateException("NPC admission receipt missing");
            }
            lane.life.pending.decrementAndGet();
            NativeSourceDamageCapacity.release((NativePlayerLifetime)lane.life, (long)1L);
        }
        return event;
    }

    static void completed(CombatNpcDamageQueue.Queue queue) {
        Lane lane = NativeNpcLifetime.lane(queue);
        if (lane != null && queue.pending() == 0 && lane.scheduled == 0) {
            lane.life.npcs.remove(lane);
            lane.task = null;
        }
    }

    static void clearing(CombatNpcDamageQueue.Queue queue, boolean bl) {
        NativeSourcePhysicalNpc.clearing((CombatNpcDamageQueue.Queue)queue);
        NativeSourcePoisonNpc.clearing((CombatNpcDamageQueue.Queue)queue);
        NativeSourceCubeNpcQueue.clearing((CombatNpcDamageQueue.Queue)queue);
        Lane lane = NativeNpcLifetime.lane(queue);
        if (lane == null) {
            return;
        }
        int n = lane.deadlines.size();
        lane.deadlines.clear();
        lane.life.pending.addAndGet(-n);
        NativeSourceDamageCapacity.release((NativePlayerLifetime)lane.life, (long)n);
        lane.life.dropped.addAndGet(n);
        lane.cancel();
        if (lane.scheduled == 0) {
            queue.idle();
            lane.life.npcs.remove(lane);
            lane.task = null;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void submit(Runnable runnable, int n) {
        Lane lane;
        CombatNpcDamageQueue.Queue queue;
        CombatMonsterCurseLifecycle.Actor actor = (CombatMonsterCurseLifecycle.Actor)CombatAccess.field((Object)runnable, (String)"actor");
        CombatNpcDamageQueue.Queue queue2 = queue = (CombatNpcDamageQueue.Queue)CombatAccess.field((Object)runnable, (String)"queue");
        synchronized (queue2) {
            lane = NativeNpcLifetime.lane(queue, actor.hub());
            lane.task = runnable;
            lane.life.npcs.add(lane);
        }
        NativeNpcLifetime.dispatch(lane, runnable, n);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void dispatch(Lane lane, Runnable runnable, int n) {
        CombatNpcDamageQueue.Queue queue = lane.queue;
        synchronized (queue) {
            if (lane.task != runnable || lane.scheduled > 0) {
                return;
            }
            if (!lane.life.processes() || lane.queue.closed()) {
                lane.stop();
                return;
            }
            ++lane.scheduled;
        }
        try {
            lane.life.workers.execute(new Dispatch(lane, runnable));
        }
        catch (RejectedExecutionException rejectedExecutionException) {
            CombatNpcDamageQueue.Queue queue2 = lane.queue;
            synchronized (queue2) {
                --lane.scheduled;
                if (n >= 12) {
                    lane.queue.clear(false);
                    lane.queue.idle();
                    return;
                }
            }
            lane.life.retries.incrementAndGet();
            NativeNpcLifetime.arm(lane, runnable, Math.min(1000L, 5L << Math.min(20, n)), n + 1);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void arm(Lane lane, Runnable runnable, long l, int n) {
        Retry retry = new Retry(lane, runnable, n);
        boolean bl = false;
        CombatNpcDamageQueue.Queue queue = lane.queue;
        synchronized (queue) {
            if (lane.task != runnable || lane.scheduled > 0) {
                return;
            }
            if (!lane.life.processes() || lane.queue.closed() || lane.queue.pending() == 0) {
                lane.queue.idle();
                lane.life.npcs.remove(lane);
                return;
            }
            if (n == 0 && lane.life.mode == NativePlayerLifetime.Mode.DRAINING) {
                bl = true;
            } else {
                if (lane.waiting != null) {
                    throw new IllegalStateException("duplicate NPC retry registration");
                }
                lane.waiting = retry;
            }
        }
        if (bl) {
            NativeNpcLifetime.dispatch(lane, runnable, 0);
            return;
        }
        try {
            retry.future(lane.life.retry.schedule(retry, Math.max(1L, l), TimeUnit.MILLISECONDS));
        }
        catch (RejectedExecutionException rejectedExecutionException) {
            CombatNpcDamageQueue.Queue queue2 = lane.queue;
            synchronized (queue2) {
                if (lane.waiting == retry) {
                    lane.life.timerRejections.incrementAndGet();
                    lane.cancel();
                    lane.queue.clear(false);
                    lane.queue.idle();
                }
            }
        }
    }

    static boolean offer(CombatNpcDamageQueue.Queue queue, CombatNpcDamageQueue.Event event, GameHub gameHub) {
        return NativeSourcePendingAdmission.offer((CombatNpcDamageQueue.Queue)queue, (CombatNpcDamageQueue.Event)event, (GameHub)gameHub);
    }

    static final class Lane {
        final CombatNpcDamageQueue.Queue queue;
        final NativePlayerLifetime life;
        final IdentityHashMap<CombatNpcDamageQueue.Event, Long> deadlines = new IdentityHashMap();
        Runnable task;
        Retry waiting;
        int scheduled;

        Lane(CombatNpcDamageQueue.Queue queue, NativePlayerLifetime nativePlayerLifetime) {
            this.queue = queue;
            this.life = nativePlayerLifetime;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        void requestDrain() {
            Retry retry;
            CombatNpcDamageQueue.Queue queue = this.queue;
            synchronized (queue) {
                retry = this.waiting;
                if (retry == null) {
                    return;
                }
                this.cancel();
            }
            NativeNpcLifetime.dispatch(this, retry.task, 0);
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        void stop() {
            CombatNpcDamageQueue.Queue queue = this.queue;
            synchronized (queue) {
                this.queue.clear(true);
                if (this.scheduled == 0) {
                    this.queue.idle();
                    this.life.npcs.remove(this);
                    this.task = null;
                }
            }
        }

        void cancel() {
            if (this.waiting != null) {
                Retry retry = this.waiting;
                this.waiting = null;
                retry.cancel();
                this.life.cancelledTimers.incrementAndGet();
            }
        }
    }

    static final class Dispatch
    implements Runnable {
        final Lane lane;
        final Runnable task;

        Dispatch(Lane lane, Runnable runnable) {
            this.lane = lane;
            this.task = runnable;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void run() {
            try {
                this.lane.life.executeCommitted(() -> {
                    if (this.lane.life.processes() && !this.lane.queue.closed()) {
                        this.task.run();
                    } else {
                        this.lane.queue.clear(true);
                    }
                });
            }
            finally {
                Runnable runnable = null;
                long l = 0L;
                CombatNpcDamageQueue.Queue queue = this.lane.queue;
                synchronized (queue) {
                    --this.lane.scheduled;
                    if (this.lane.scheduled < 0) {
                        throw new IllegalStateException("NPC dispatch receipt underflow");
                    }
                    if (this.lane.scheduled == 0) {
                        if (this.lane.queue.pending() == 0 || !this.lane.life.processes() || this.lane.queue.closed()) {
                            this.lane.cancel();
                            this.lane.queue.idle();
                            this.lane.life.npcs.remove(this.lane);
                            this.lane.task = null;
                        } else if (this.lane.waiting == null) {
                            runnable = this.lane.task;
                            l = NativeNpcLifetime.delay(this.lane);
                        }
                    }
                }
                if (runnable != null) {
                    if (l > 0L) {
                        NativeNpcLifetime.arm(this.lane, runnable, 1L + (l - 1L) / TimeUnit.MILLISECONDS.toNanos(1L), 0);
                    } else {
                        NativeNpcLifetime.dispatch(this.lane, runnable, 0);
                    }
                }
            }
        }
    }

    static final class Retry
    implements Runnable {
        final Lane lane;
        final Runnable task;
        final int attempt;
        volatile ScheduledFuture<?> future;
        volatile boolean cancelled;

        Retry(Lane lane, Runnable runnable, int n) {
            this.lane = lane;
            this.task = runnable;
            this.attempt = n;
        }

        void future(ScheduledFuture<?> scheduledFuture) {
            this.future = scheduledFuture;
            if (this.cancelled) {
                scheduledFuture.cancel(false);
            }
        }

        void cancel() {
            this.cancelled = true;
            ScheduledFuture<?> scheduledFuture = this.future;
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void run() {
            CombatNpcDamageQueue.Queue queue = this.lane.queue;
            synchronized (queue) {
                if (this.cancelled || this.lane.waiting != this) {
                    return;
                }
                this.lane.waiting = null;
            }
            NativeNpcLifetime.dispatch(this.lane, this.task, this.attempt);
        }
    }
}

