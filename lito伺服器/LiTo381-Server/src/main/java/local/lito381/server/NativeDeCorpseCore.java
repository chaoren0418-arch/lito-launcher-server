// DECOMPILED from base-0.6.71.jar, TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;

final class NativeDeCorpseCore
implements Runnable {
    final Set<Npc> tracked = ConcurrentHashMap.newKeySet();
    final Port port;
    ScheduledFuture<?> timer;
    final AtomicLong scheduleRejected = new AtomicLong();
    final AtomicLong scheduleError = new AtomicLong();
    final AtomicLong duplicateStart = new AtomicLong();
    final AtomicLong claims = new AtomicLong();
    final AtomicLong dispatchRejected = new AtomicLong();
    final AtomicLong deleteFailures = new AtomicLong();
    final AtomicLong shutdownDrop = new AtomicLong();
    final AtomicLong tickFailures = new AtomicLong();

    NativeDeCorpseCore(Port port) {
        this.port = Objects.requireNonNull(port);
    }

    void track(Npc npc) {
        if (npc != null) {
            this.tracked.add(npc);
        }
    }

    void untrack(Npc npc) {
        if (npc != null) {
            this.tracked.remove(npc);
        }
    }

    synchronized void start() {
        if (this.timer != null && !this.timer.isCancelled() && !this.timer.isDone()) {
            long l = this.duplicateStart.incrementAndGet();
            this.port.log("warn", "[NpcLifecycle] duplicate corpse timer start suppressed: count={}", l);
            return;
        }
        try {
            this.timer = this.port.fixed(this, 5000L, 5000L);
            if (this.timer == null) {
                this.scheduleError.incrementAndGet();
                this.port.log("error", "[NpcLifecycle] corpse timer schedule returned null: interval={}", 5000);
            }
        }
        catch (RejectedExecutionException rejectedExecutionException) {
            this.scheduleRejected.incrementAndGet();
            this.port.log("error", "[NpcLifecycle] corpse timer schedule rejected: interval={}", 5000, rejectedExecutionException);
        }
        catch (RuntimeException runtimeException) {
            this.scheduleError.incrementAndGet();
            this.port.log("error", "[NpcLifecycle] corpse timer schedule failed: interval={}", 5000, runtimeException);
        }
    }

    Snapshot snapshot() {
        return new Snapshot(this.scheduleRejected.get(), this.scheduleError.get(), this.duplicateStart.get(), this.claims.get(), this.dispatchRejected.get(), this.deleteFailures.get(), this.shutdownDrop.get(), this.tickFailures.get(), this.tracked.size());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void run() {
        this.port.auditDeletionStalls();
        if (this.tracked.isEmpty()) {
            return;
        }
        long l = this.port.nanoTime();
        for (Npc npc : this.tracked) {
            if (npc.getMaxHp() <= 0) {
                this.tracked.remove(npc);
                continue;
            }
            Object object = this.port.reserve(npc);
            if (object == null) continue;
            try {
                if (!this.port.claim(npc, l, object)) {
                    if (npc.destroyed()) {
                        this.tracked.remove(npc);
                    }
                    continue;
                }
                this.tracked.remove(npc);
                this.claims.incrementAndGet();
                this.dispatch(new Task(this, npc, object), 0);
            }
            catch (RuntimeException runtimeException) {
                long l2 = this.deleteFailures.incrementAndGet();
                this.port.log("error", "[NpcLifecycle] corpse delete failed: objectId={} npcId={} map={} x={} y={} count={}", npc.getId(), npc.getNpcId(), (int) npc.getMapId(), npc.getX(), npc.getY(), l2, runtimeException);
            }
            finally {
                if (!this.port.retained(object)) {
                    this.port.release(object);
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void dispatch(Task task, int n) {
        try {
            if (this.port.tryExecute(task)) {
                return;
            }
            long l = this.dispatchRejected.incrementAndGet();
            if (this.port.isShutdown()) {
                task.discardOnShutdown();
                return;
            }
            long l2 = Math.min(1000L, 10L << Math.min(n, 6));
            if (n == 0 || (n & n - 1) == 0) {
                this.port.log("warn", "[NpcLifecycle] corpse delete worker saturated; retry retained: objectId={} npcId={} attempt={} delay={} rejected={}", task.npc.getId(), task.npc.getNpcId(), n, l2, l);
            }
            try {
                ScheduledFuture<?> scheduledFuture = this.port.retry(task, () -> this.dispatch(task, n + 1), l2);
                if (scheduledFuture == null) {
                    long l3 = this.deleteFailures.incrementAndGet();
                    this.port.log("error", "[NpcLifecycle] corpse delete retry schedule returned null: objectId={} npcId={} attempt={} count={}", task.npc.getId(), task.npc.getNpcId(), n, l3);
                }
            }
            catch (RejectedExecutionException rejectedExecutionException) {
                long l4 = this.deleteFailures.incrementAndGet();
                this.port.log("error", "[NpcLifecycle] corpse delete retry schedule rejected: objectId={} npcId={} attempt={} count={}", task.npc.getId(), task.npc.getNpcId(), n, l4, rejectedExecutionException);
            }
            catch (RuntimeException runtimeException) {
                long l5 = this.deleteFailures.incrementAndGet();
                this.port.log("error", "[NpcLifecycle] corpse delete retry schedule failed: objectId={} npcId={} attempt={} count={}", task.npc.getId(), task.npc.getNpcId(), n, l5, runtimeException);
            }
        }
        finally {
            if (!this.port.retained(task.credit)) {
                this.port.release(task.credit);
            }
        }
    }

    static interface Port {
        public void auditDeletionStalls();

        public long nanoTime();

        public ScheduledFuture<?> fixed(Runnable var1, long var2, long var4);

        public boolean tryExecute(Task var1);

        public boolean isShutdown();

        public ScheduledFuture<?> retry(Task var1, Runnable var2, long var3);

        public void log(String var1, String var2, Object ... var3);

        default public Object reserve(Npc npc) {
            return new Object();
        }

        default public boolean claim(Npc npc, long l, Object object) {
            return npc.claimExpiredCorpseDeletion(l);
        }

        default public boolean retained(Object object) {
            return true;
        }

        default public void release(Object object) {
        }
    }

    record Snapshot(long scheduleRejected, long scheduleError, long duplicateStart, long claims, long dispatchRejected, long deleteFailures, long shutdownDrop, long tickFailures, int tracked) {
    }

    static interface Npc {
        public int getMaxHp();

        public boolean claimExpiredCorpseDeletion(long var1);

        public boolean destroyed();

        public boolean isDead();

        public void deleteMe();

        public int getId();

        public int getNpcId();

        public short getMapId();

        public int getX();

        public int getY();
    }

    static final class Task
    implements Runnable {
        final NativeDeCorpseCore owner;
        final Npc npc;
        final Object credit;

        Task(NativeDeCorpseCore nativeDeCorpseCore, Npc npc, Object object) {
            this.owner = nativeDeCorpseCore;
            this.npc = npc;
            this.credit = object;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void run() {
            try {
                try {
                    this.npc.deleteMe();
                    if (!this.npc.destroyed() && this.npc.isDead()) {
                        long l = this.owner.deleteFailures.incrementAndGet();
                        this.owner.port.log("error", "[NpcLifecycle] corpse delete returned without terminal removal: objectId={} npcId={} map={} x={} y={} count={}", this.npc.getId(), this.npc.getNpcId(), (int) this.npc.getMapId(), this.npc.getX(), this.npc.getY(), l);
                    }
                }
                catch (RuntimeException runtimeException) {
                    long l = this.owner.deleteFailures.incrementAndGet();
                    this.owner.port.log("error", "[NpcLifecycle] corpse delete failed: objectId={} npcId={} map={} x={} y={} count={}", this.npc.getId(), this.npc.getNpcId(), (int) this.npc.getMapId(), this.npc.getX(), this.npc.getY(), l, runtimeException);
                }
            }
            finally {
                this.owner.port.release(this.credit);
            }
        }

        void discardOnShutdown() {
            try {
                long l = this.owner.shutdownDrop.incrementAndGet();
                this.owner.port.log("warn", "[NpcLifecycle] corpse delete discarded during NPC worker shutdown: objectId={} npcId={} count={}", this.npc.getId(), this.npc.getNpcId(), l);
            }
            finally {
                this.owner.port.release(this.credit);
            }
        }
    }
}
