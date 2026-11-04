// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatNpcDamageQueue;
import local.lito381.server.NativeDeClientViewer;
import local.lito381.server.NativeDePerceptionState;
import local.lito381.server.NativeDeWorldContext;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativePlayerLifetime;

final class NativeDePerceptionJobs {
    static final String FIELD = "sourceDePerceptionJobs";

    NativeDePerceptionJobs() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Owner owner(NativeDeWorldContext nativeDeWorldContext) {
        NativePlayerLifetime nativePlayerLifetime = nativeDeWorldContext.life;
        synchronized (nativePlayerLifetime.admission) {
            Owner owner = (Owner)CombatAccess.field(nativePlayerLifetime, FIELD);
            if (owner != null) {
                if (owner.context != nativeDeWorldContext) {
                    throw new IllegalArgumentException("perception World owner is immutable");
                }
                return owner;
            }
            if (!nativePlayerLifetime.admits() || nativeDeWorldContext.closed) {
                throw new RejectedExecutionException("perception application closed");
            }
            owner = new Owner(nativeDeWorldContext);
            CombatAccess.set(nativePlayerLifetime, FIELD, owner);
            return owner;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean request(NativeDePerceptionState nativeDePerceptionState, NativeDeClientViewer nativeDeClientViewer) {
        Task task;
        if (nativeDeClientViewer == null || nativeDePerceptionState.deletionOwner.timer.life != nativeDeClientViewer.life) {
            return false;
        }
        NativeDeWorldContext nativeDeWorldContext = nativeDePerceptionState.context();
        if (!NativeDePerceptionJobs.current(nativeDePerceptionState, nativeDeClientViewer)) {
            return false;
        }
        Owner owner = NativeDePerceptionJobs.owner(nativeDeWorldContext);
        Object object = owner.life.admission;
        synchronized (object) {
            Key key = new Key(nativeDePerceptionState, nativeDeClientViewer);
            if (owner.closed || !owner.life.admits() || nativeDeWorldContext.closed || owner.life.pending.get() >= NativePlayerCommit.GLOBAL || owner.keys.contains(key)) {
                owner.rejected.incrementAndGet();
                return false;
            }
            task = new Task(owner, key);
            owner.keys.add(key);
            owner.tasks.add(task);
            owner.life.pending.incrementAndGet();
            owner.accepted.incrementAndGet();
        }
        object = task;
        synchronized (object) {
            if (task.closed) {
                return true;
            }
            try {
                owner.life.workers.execute(task);
                return true;
            }
            catch (RejectedExecutionException rejectedExecutionException) {
                task.finish();
                owner.rejected.incrementAndGet();
                return false;
            }
            catch (Error | RuntimeException throwable) {
                task.finish();
                throw throwable;
            }
        }
    }

    static boolean current(NativeDePerceptionState nativeDePerceptionState, NativeDeClientViewer nativeDeClientViewer) {
        if (nativeDeClientViewer.retired || nativeDePerceptionState.destroyed() || nativeDePerceptionState.deletionStarted() || nativeDePerceptionState.context().closed) {
            return false;
        }
        NativeDeWorldContext.Entry entry = (NativeDeWorldContext.Entry)CombatAccess.field(nativeDePerceptionState, "sourceDeWorldEntry");
        NativeDeWorldContext.Entry entry2 = (NativeDeWorldContext.Entry)CombatAccess.field(nativeDeClientViewer, "sourceDeWorldEntry");
        return entry != null && entry2 != null && entry.context == entry2.context && entry.showId == entry2.showId && entry.location.getMapId() == entry2.location.getMapId() && entry.location.isInScreen(entry2.location) && entry.context.world().isObjectRegistered(nativeDePerceptionState) && entry.context.world().isObjectRegistered(nativeDeClientViewer);
    }

    static void closeAll(NativePlayerLifetime nativePlayerLifetime) {
        if (nativePlayerLifetime.mode == NativePlayerLifetime.Mode.STOPPED) {
            NativeDePerceptionJobs.stop(nativePlayerLifetime);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void stop(NativePlayerLifetime nativePlayerLifetime) {
        Object admissionLock = nativePlayerLifetime.admission;
        synchronized (admissionLock) {
            Owner owner = (Owner)CombatAccess.field(nativePlayerLifetime, FIELD);
            if (owner == null) {
                // ** MonitorExit[var3_1] (shouldn't be in output)
                return;
            }
            owner.closed = true;
            Task[] taskArray2 = (Task[])owner.tasks.toArray(Task[]::new);
            // ** MonitorExit[var3_1] (shouldn't be in output)
            for (Task task : taskArray2) {
                task.cancel();
            }
            return;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Snapshot snapshot(NativePlayerLifetime nativePlayerLifetime) {
        Object object = nativePlayerLifetime.admission;
        synchronized (object) {
            Owner owner = (Owner)CombatAccess.field(nativePlayerLifetime, FIELD);
            return owner == null ? new Snapshot(false, true, 0, 0L, 0L, 0L, 0L) : new Snapshot(true, owner.closed, owner.tasks.size(), owner.accepted.get(), owner.rejected.get(), owner.stale.get(), owner.failed.get());
        }
    }

    static final class Owner {
        final NativeDeWorldContext context;
        final NativePlayerLifetime life;
        final Set<Task> tasks = new HashSet<Task>();
        final Set<Key> keys = new HashSet<Key>();
        final AtomicLong accepted = new AtomicLong();
        final AtomicLong rejected = new AtomicLong();
        final AtomicLong stale = new AtomicLong();
        final AtomicLong failed = new AtomicLong();
        boolean closed;

        Owner(NativeDeWorldContext nativeDeWorldContext) {
            this.context = nativeDeWorldContext;
            this.life = nativeDeWorldContext.life;
        }
    }

    record Key(NativeDePerceptionState actor, NativeDeClientViewer viewer) {
    }

    static final class Task
    implements Runnable {
        final Owner owner;
        final Key key;
        boolean running;
        boolean closed;

        Task(Owner owner, Key key) {
            this.owner = owner;
            this.key = key;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void run() {
            boolean bl;
            Task task = this;
            synchronized (task) {
                boolean bl2 = bl = !this.closed && this.owner.life.mode != NativePlayerLifetime.Mode.STOPPED;
                if (bl) {
                    this.running = true;
                }
            }
            if (!bl) {
                this.finish();
                return;
            }
            try {
                this.owner.life.executeCommitted(() -> {
                    if (NativeDePerceptionJobs.current(this.key.actor(), this.key.viewer())) {
                        this.key.actor().onPerceive(this.key.viewer());
                    } else {
                        this.owner.stale.incrementAndGet();
                    }
                });
            }
            catch (Throwable throwable) {
                this.owner.failed.incrementAndGet();
                CombatNpcDamageQueue.failed(throwable);
            }
            finally {
                task = this;
                synchronized (task) {
                    this.running = false;
                }
                this.finish();
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        void cancel() {
            synchronized (this) {
                if (this.running || this.closed) {
                    return;
                }
                this.owner.life.workers.remove(this);
                this.finish();
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        void finish() {
            synchronized (this) {
                if (this.closed) {
                    return;
                }
                this.closed = true;
            }
            synchronized (this.owner.life.admission) {
                this.owner.tasks.remove(this);
                this.owner.keys.remove(this.key);
                if (this.owner.life.pending.decrementAndGet() < 0L) {
                    throw new IllegalStateException("perception credit underflow");
                }
            }
        }
    }

    record Snapshot(boolean present, boolean closed, int pending, long accepted, long rejected, long stale, long failed) {
    }
}
