// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.GameHub;
import local.lito381.server.NativeDeCorpseTimers;
import local.lito381.server.NativeDePerceptionJobs;
import local.lito381.server.NativeNpcLifetime;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativePlayerVitalCompletions;
import local.lito381.server.NativeReuseShutdown;
import local.lito381.server.NativeServerLifetime;

final class NativePlayerLifetime {
    private static final String FIELD = "sourcePlayerCommitLifetime";
    private static final Set<NativePlayerLifetime> lives = ConcurrentHashMap.newKeySet();
    private static final AtomicInteger serial = new AtomicInteger();
    private static final AtomicInteger threads = new AtomicInteger();
    private static final ThreadLocal<NativePlayerLifetime> worker = new ThreadLocal();
    private static final ThreadLocal<NativePlayerLifetime> commit = new ThreadLocal();
    private static final ThreadLocal<Long> minimumDeadline = new ThreadLocal();
    final Object admission = new Object();
    final Set<NativePlayerCommit.Queue> active = ConcurrentHashMap.newKeySet();
    final Set<NativeNpcLifetime.Lane> npcs = ConcurrentHashMap.newKeySet();
    final AtomicLong pending = new AtomicLong();
    final AtomicLong dropped = new AtomicLong();
    final AtomicLong retries = new AtomicLong();
    final AtomicLong timerRejections = new AtomicLong();
    final AtomicLong cancelledTimers = new AtomicLong();
    final ThreadPoolExecutor workers;
    final ScheduledThreadPoolExecutor retry;
    volatile Mode mode = Mode.OPEN;
    volatile NativeServerLifetime application;
    private Object sourceDeAiTimers;
    private volatile Object sourceDePerceptionJobs;

    private NativePlayerLifetime() {
        int n = serial.incrementAndGet();
        this.workers = new ThreadPoolExecutor(NativePlayerCommit.WORKERS, NativePlayerCommit.WORKERS, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(NativePlayerCommit.CAPACITY), runnable -> new Thread(runnable, "LiTo-PC-commit-" + n + "-" + threads.incrementAndGet()), new ThreadPoolExecutor.AbortPolicy());
        this.retry = new ScheduledThreadPoolExecutor(1, runnable -> new Thread(runnable, "LiTo-PC-retry-" + n));
        this.retry.setRemoveOnCancelPolicy(true);
        this.retry.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    }

    private static void purge() {
        lives.removeIf(nativePlayerLifetime -> nativePlayerLifetime.mode == Mode.STOPPED && nativePlayerLifetime.terminated());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void openHub(GameHub gameHub) {
        NativePlayerLifetime.purge();
        GameHub gameHub2 = gameHub;
        synchronized (gameHub2) {
            if (CombatAccess.field(gameHub, FIELD) != null) {
                throw new IllegalStateException("PC application lifetime already initialized");
            }
            NativePlayerLifetime nativePlayerLifetime = new NativePlayerLifetime();
            CombatAccess.set(gameHub, FIELD, nativePlayerLifetime);
            lives.add(nativePlayerLifetime);
        }
    }

    static NativePlayerLifetime of(GameHub gameHub) {
        NativePlayerLifetime nativePlayerLifetime = (NativePlayerLifetime)CombatAccess.field(gameHub, FIELD);
        if (nativePlayerLifetime == null) {
            throw new IllegalStateException("PC application lifetime missing");
        }
        return nativePlayerLifetime;
    }

    static NativePlayerLifetime of(ClientSession clientSession) {
        return NativePlayerLifetime.of((GameHub)CombatAccess.field(clientSession, "hub"));
    }

    static boolean onWorker(GameHub gameHub) {
        return worker.get() == NativePlayerLifetime.of(gameHub);
    }

    boolean admits() {
        return this.mode == Mode.OPEN || this.mode == Mode.DRAINING && commit.get() == this;
    }

    boolean processes() {
        return this.mode != Mode.STOPPED;
    }

    boolean idle() {
        return NativePlayerVitalCompletions.pending(this) == 0L && this.pending.get() == 0L && this.active.isEmpty() && this.npcs.isEmpty() && this.workers.getActiveCount() == 0 && this.workers.getQueue().isEmpty();
    }

    boolean terminated() {
        return NativePlayerVitalCompletions.pending(this) == 0L && this.workers.isTerminated() && this.retry.isTerminated();
    }

    static boolean allIdle() {
        NativePlayerLifetime.purge();
        for (NativePlayerLifetime nativePlayerLifetime : lives) {
            if (nativePlayerLifetime.idle()) continue;
            return false;
        }
        return true;
    }

    static Snapshot snapshot(GameHub gameHub) {
        NativePlayerLifetime nativePlayerLifetime = NativePlayerLifetime.of(gameHub);
        return new Snapshot(nativePlayerLifetime.mode.name(), nativePlayerLifetime.pending.get(), nativePlayerLifetime.workers.getActiveCount(), nativePlayerLifetime.workers.getQueue().size(), nativePlayerLifetime.retry.getQueue().size(), nativePlayerLifetime.retries.get(), nativePlayerLifetime.timerRejections.get(), nativePlayerLifetime.cancelledTimers.get(), nativePlayerLifetime.terminated());
    }

    static long deadline() {
        Long l = minimumDeadline.get();
        return l == null ? 0L : l;
    }

    static boolean deadlineIdle() {
        return minimumDeadline.get() == null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void withMinimumCommitDelay(long l, Runnable runnable) {
        if (runnable == null) {
            return;
        }
        if (l <= 0L) {
            runnable.run();
            return;
        }
        Long l2 = minimumDeadline.get();
        long l3 = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(l);
        minimumDeadline.set(l2 == null || l3 - l2 > 0L ? l3 : l2);
        try {
            runnable.run();
        }
        finally {
            if (l2 == null) {
                minimumDeadline.remove();
            } else {
                minimumDeadline.set(l2);
            }
        }
    }

    static void dispatch(ClientSession clientSession, NativePlayerCommit.Queue queue, int n) {
        NativePlayerLifetime nativePlayerLifetime = queue.life;
        if (queue.closed || queue.retired || !nativePlayerLifetime.processes()) {
            queue.stop();
            return;
        }
        try {
            nativePlayerLifetime.workers.execute(new Dispatch(clientSession, queue));
        }
        catch (RejectedExecutionException rejectedExecutionException) {
            if (n >= 12) {
                queue.drop(false);
                queue.finish();
                return;
            }
            nativePlayerLifetime.retries.incrementAndGet();
            NativePlayerCommit.retried();
            NativePlayerLifetime.arm(clientSession, queue, Math.min(1000L, 5L << Math.min(20, n)), n + 1);
        }
    }

    static void arm(ClientSession clientSession, NativePlayerCommit.Queue queue, long l, int n) {
        block3: {
            Registration registration = new Registration(clientSession, queue, n);
            if (!queue.waitFor(registration)) {
                return;
            }
            try {
                registration.future(queue.life.retry.schedule(registration, Math.max(1L, l), TimeUnit.MILLISECONDS));
            }
            catch (RejectedExecutionException rejectedExecutionException) {
                queue.life.timerRejections.incrementAndGet();
                if (!queue.removeWait(registration)) break block3;
                queue.drop(false);
                queue.finish();
            }
        }
    }

    static void deadline(ClientSession clientSession, NativePlayerCommit.Queue queue, long l) {
        NativePlayerLifetime.arm(clientSession, queue, 1L + (l - 1L) / TimeUnit.MILLISECONDS.toNanos(1L), 0);
    }

    static void executeFixture(GameHub gameHub, Runnable runnable) {
        NativePlayerLifetime nativePlayerLifetime = NativePlayerLifetime.of(gameHub);
        nativePlayerLifetime.workers.execute(() -> {
            NativePlayerLifetime nativePlayerLifetime2 = worker.get();
            worker.set(nativePlayerLifetime);
            try {
                runnable.run();
            }
            finally {
                if (nativePlayerLifetime2 == null) {
                    worker.remove();
                } else {
                    worker.set(nativePlayerLifetime2);
                }
            }
        });
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void executeCommitted(Runnable runnable) {
        NativePlayerLifetime nativePlayerLifetime = worker.get();
        NativePlayerLifetime nativePlayerLifetime2 = commit.get();
        worker.set(this);
        commit.set(this);
        try {
            runnable.run();
        }
        finally {
            if (nativePlayerLifetime2 == null) {
                commit.remove();
            } else {
                commit.set(nativePlayerLifetime2);
            }
            if (nativePlayerLifetime == null) {
                worker.remove();
            } else {
                worker.set(nativePlayerLifetime);
            }
        }
    }

    static void executeFixture(Runnable runnable) {
        NativePlayerLifetime nativePlayerLifetime = null;
        for (NativePlayerLifetime nativePlayerLifetime2 : lives) {
            if (nativePlayerLifetime2.mode == Mode.STOPPED) continue;
            if (nativePlayerLifetime != null) {
                throw new IllegalStateException("fixture must choose its GameHub when multiple applications are live");
            }
            nativePlayerLifetime = nativePlayerLifetime2;
        }
        if (nativePlayerLifetime == null) {
            throw new IllegalStateException("no live PC fixture application");
        }
        NativePlayerLifetime selected = nativePlayerLifetime;
        selected.workers.execute(() -> {
            NativePlayerLifetime nativePlayerLifetime2 = worker.get();
            worker.set(selected);
            try {
                runnable.run();
            }
            finally {
                if (nativePlayerLifetime2 == null) {
                    worker.remove();
                } else {
                    worker.set(nativePlayerLifetime2);
                }
            }
        });
    }

    static Shutdown shutdown(GameHub gameHub, boolean bl, long l) {
        return NativePlayerLifetime.of(gameHub).shutdown(bl, l);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private synchronized Shutdown shutdown(boolean bl, long l) {
        if (worker.get() == this) {
            throw new IllegalStateException("PC application cannot synchronously close from its own worker");
        }
        long l2 = this.dropped.get();
        boolean bl2 = false;
        boolean bl3 = false;
        Object object = this.admission;
        synchronized (object) {
            if (this.mode == Mode.OPEN) {
                this.mode = bl ? Mode.DRAINING : Mode.STOPPED;
            } else if (!bl) {
                this.mode = Mode.STOPPED;
            }
        }
        NativeDeCorpseTimers.closeAll(this);
        NativeDePerceptionJobs.closeAll(this);
        long l3 = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(Math.max(0L, l));
        if (bl && this.mode == Mode.DRAINING) {
            while (true) {
                for (NativePlayerCommit.Queue queue : (NativePlayerCommit.Queue[])this.active.toArray(NativePlayerCommit.Queue[]::new)) {
                    queue.requestDrain();
                }
                for (NativeNpcLifetime.Lane lane : (NativeNpcLifetime.Lane[])this.npcs.toArray(NativeNpcLifetime.Lane[]::new)) {
                    lane.requestDrain();
                }
                if (this.idle()) {
                    bl3 = true;
                    break;
                }
                if (System.nanoTime() - l3 >= 0L) break;
                try {
                    Thread.sleep(1L);
                }
                catch (InterruptedException interruptedException) {
                    bl2 = true;
                    break;
                }
            }
        }
        Object objectArray = this.admission;
        synchronized (this.admission) {
            boolean bl4;
            this.mode = Mode.STOPPED;
            // ** MonitorExit[objectArray] (shouldn't be in output)
            NativeDeCorpseTimers.stop(this);
            NativeDePerceptionJobs.stop(this);
            for (NativePlayerCommit.Queue queue : (NativePlayerCommit.Queue[])this.active.toArray(NativePlayerCommit.Queue[]::new)) {
                queue.stop();
            }
            for (NativeNpcLifetime.Lane lane : (NativeNpcLifetime.Lane[])this.npcs.toArray(NativeNpcLifetime.Lane[]::new)) {
                lane.stop();
            }
            if (!NativeReuseShutdown.defer(this)) {
                this.retry.shutdown();
                this.workers.shutdown();
                if (l > 0L && !bl2) {
                    try {
                        if (!this.retry.awaitTermination(2L, TimeUnit.SECONDS)) {
                            this.retry.shutdownNow();
                            this.retry.awaitTermination(2L, TimeUnit.SECONDS);
                        }
                        if (!this.workers.awaitTermination(3L, TimeUnit.SECONDS)) {
                            this.workers.shutdownNow();
                            this.workers.awaitTermination(3L, TimeUnit.SECONDS);
                        }
                    }
                    catch (InterruptedException interruptedException) {
                        bl2 = true;
                    }
                }
                if (!this.retry.isTerminated()) {
                    this.retry.shutdownNow();
                }
                if (!this.workers.isTerminated()) {
                    this.workers.shutdownNow();
                }
            }
            if (bl4 = this.terminated()) {
                lives.remove(this);
            }
            if (bl2) {
                Thread.currentThread().interrupt();
            }
            return new Shutdown(bl3, bl4, this.dropped.get() - l2);
        }
    }

    static void closeHub(GameHub gameHub) {
        if (worker.get() == NativePlayerLifetime.of(gameHub)) {
            throw new IllegalStateException("PC application cannot synchronously close from its own worker");
        }
        ((ScheduledExecutorService)CombatAccess.field(gameHub, "battleClock")).shutdownNow();
        Shutdown shutdown = NativePlayerLifetime.shutdown(gameHub, true, 10000L);
        if (!shutdown.terminated) {
            throw new IllegalStateException("PC workers did not terminate; GameHub teardown fenced");
        }
    }

    static void closeFixture() {
        for (NativePlayerLifetime nativePlayerLifetime : (NativePlayerLifetime[])lives.toArray(NativePlayerLifetime[]::new)) {
            Shutdown shutdown = nativePlayerLifetime.shutdown(false, 5000L);
            if (shutdown.terminated) continue;
            throw new IllegalStateException("PC fixture workers remain live");
        }
    }

    static enum Mode {
        OPEN,
        DRAINING,
        STOPPED;

    }

    record Snapshot(String mode, long pending, int active, int queued, int retryQueued, long retries, long timerRejections, long cancelledTimers, boolean terminated) {
    }

    static final class Dispatch
    implements Runnable {
        final ClientSession pc;
        final NativePlayerCommit.Queue queue;

        Dispatch(ClientSession clientSession, NativePlayerCommit.Queue queue) {
            this.pc = clientSession;
            this.queue = queue;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void run() {
            NativePlayerLifetime nativePlayerLifetime = this.queue.life;
            NativePlayerLifetime nativePlayerLifetime2 = worker.get();
            NativePlayerLifetime nativePlayerLifetime3 = commit.get();
            worker.set(nativePlayerLifetime);
            commit.set(nativePlayerLifetime);
            try {
                if (this.queue.beginRun()) {
                    NativePlayerCommit.drain(this.pc, this.queue);
                } else {
                    this.queue.finish();
                }
            }
            finally {
                if (nativePlayerLifetime3 == null) {
                    commit.remove();
                } else {
                    commit.set(nativePlayerLifetime3);
                }
                if (nativePlayerLifetime2 == null) {
                    worker.remove();
                } else {
                    worker.set(nativePlayerLifetime2);
                }
            }
        }
    }

    static final class Registration
    implements Runnable {
        final ClientSession pc;
        final NativePlayerCommit.Queue queue;
        final int attempt;
        volatile ScheduledFuture<?> future;
        volatile boolean cancelled;

        Registration(ClientSession clientSession, NativePlayerCommit.Queue queue, int n) {
            this.pc = clientSession;
            this.queue = queue;
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

        @Override
        public void run() {
            if (!this.cancelled && this.queue.wake(this)) {
                NativePlayerLifetime.dispatch(this.pc, this.queue, this.attempt);
            }
        }
    }

    record Shutdown(boolean drained, boolean terminated, long dropped) {
    }
}
