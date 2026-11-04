// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import local.lito381.server.CombatNpcDamageQueue;
import local.lito381.server.NativeDeVisibilityRuntime;
import local.lito381.server.NativeDeVisibilityScope;
import local.lito381.server.NativeDeVisibilityTimer;
import local.lito381.server.NativePlayerCommit;

final class NativeDeVisibilityJobs {
    private static final ThreadLocal<Credit> current = new ThreadLocal();

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static <T> T with(Credit credit, Supplier<T> supplier) {
        Credit credit2 = current.get();
        current.set(credit);
        try {
            T t = supplier.get();
            return t;
        }
        finally {
            if (credit2 == null) {
                current.remove();
            } else {
                current.set(credit2);
            }
        }
    }

    static Credit acquire(NativeDeVisibilityRuntime.Owner owner) {
        Credit credit = current.get();
        if (credit != null && credit.owner == owner && credit.retain()) {
            return credit;
        }
        return Credit.reserve(owner);
    }

    static Ticket schedule(NativeDeVisibilityRuntime.Owner owner, Runnable runnable, long l, long l2) {
        if (runnable == null || l2 <= 0L) {
            throw new IllegalArgumentException("source periodic timer");
        }
        ReentrantReadWriteLock.ReadLock readLock = owner.context.admission.readLock();
        readLock.lock();
        try {
            Object object;
            owner.context.admit();
            Credit credit = NativeDeVisibilityJobs.acquire(owner);
            Ticket ticket = new Ticket(owner, runnable, credit);
            owner.tickets.add(ticket);
            if (runnable instanceof NativeDeVisibilityTimer) {
                object = (NativeDeVisibilityTimer)runnable;
                owner.timer = (NativeDeVisibilityTimer)object;
            }
            try {
                ticket.future(owner.context.life.retry.scheduleAtFixedRate(ticket::pulse, Math.max(0L, l), l2, TimeUnit.MILLISECONDS));
                object = ticket;
                return (Ticket)object;
            }
            catch (RuntimeException | Error throwable) {
                ticket.cancel(false);
                throw throwable;
            }
        }
        finally {
            readLock.unlock();
        }
    }

    private NativeDeVisibilityJobs() {
    }

    static final class Credit {
        final NativeDeVisibilityRuntime.Owner owner;
        int refs = 1;

        private Credit(NativeDeVisibilityRuntime.Owner owner) {
            this.owner = owner;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        static Credit reserve(NativeDeVisibilityRuntime.Owner owner) {
            synchronized (owner.context.life.admission) {
                if (owner.closed || owner.context.closed || !owner.context.life.admits() || owner.context.life.pending.get() >= NativePlayerCommit.GLOBAL) {
                    throw new RejectedExecutionException("visibility closed or shared budget exhausted");
                }
                owner.context.life.pending.incrementAndGet();
                return new Credit(owner);
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        boolean retain() {
            synchronized (this.owner.context.life.admission) {
                if (this.refs == 0) {
                    return false;
                }
                ++this.refs;
                return true;
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        void release() {
            synchronized (this.owner.context.life.admission) {
                if (--this.refs == 0 && this.owner.context.life.pending.decrementAndGet() < 0L) {
                    throw new IllegalStateException("visibility shared credit underflow");
                }
                if (this.refs < 0) {
                    throw new IllegalStateException("visibility credit reference underflow");
                }
            }
        }
    }

    static final class Ticket
    implements ScheduledFuture<Object> {
        final NativeDeVisibilityRuntime.Owner owner;
        final Runnable task;
        final Credit credit;
        volatile ScheduledFuture<?> future;
        volatile boolean ended;
        volatile boolean cancelled;
        volatile Throwable failure;
        Dispatch work;

        Ticket(NativeDeVisibilityRuntime.Owner owner, Runnable runnable, Credit credit) {
            this.owner = owner;
            this.task = runnable;
            this.credit = credit;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        void future(ScheduledFuture<?> scheduledFuture) {
            synchronized (this) {
                this.future = scheduledFuture;
                if (this.ended) {
                    scheduledFuture.cancel(false);
                }
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        void pulse() {
            synchronized (this) {
                block9: {
                    Dispatch dispatch;
                    if (this.ended) {
                        return;
                    }
                    if (this.owner.closed || this.owner.context.closed || !this.owner.context.life.admits()) {
                        this.cancel(false);
                        return;
                    }
                    if (this.work != null) {
                        return;
                    }
                    if (!this.credit.retain()) {
                        return;
                    }
                    this.work = dispatch = new Dispatch(this);
                    try {
                        this.owner.context.life.workers.execute(dispatch);
                    }
                    catch (Error | RuntimeException throwable) {
                        this.work = null;
                        this.credit.release();
                        this.owner.context.count("visibility.workerRejected").incrementAndGet();
                        if (throwable instanceof RejectedExecutionException) break block9;
                        throw throwable;
                    }
                }
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        private boolean end(boolean bl, Throwable throwable) {
            ScheduledFuture<?> scheduledFuture;
            synchronized (this) {
                if (this.ended) {
                    return false;
                }
                this.ended = true;
                this.cancelled = bl;
                this.failure = throwable;
                scheduledFuture = this.future;
                Dispatch dispatch = this.work;
                if (dispatch != null && dispatch.state.compareAndSet(0, 2)) {
                    this.owner.context.life.workers.remove(dispatch);
                    this.work = null;
                    this.credit.release();
                }
            }
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
            }
            this.owner.tickets.remove(this);
            this.credit.release();
            return true;
        }

        @Override
        public boolean cancel(boolean bl) {
            return this.end(true, null);
        }

        void fail(Throwable throwable) {
            this.end(false, throwable);
        }

        @Override
        public boolean isCancelled() {
            return this.cancelled;
        }

        @Override
        public boolean isDone() {
            return this.ended || this.future != null && this.future.isDone();
        }

        @Override
        public long getDelay(TimeUnit timeUnit) {
            ScheduledFuture<?> scheduledFuture = this.future;
            return scheduledFuture == null ? 0L : scheduledFuture.getDelay(timeUnit);
        }

        @Override
        public int compareTo(Delayed delayed) {
            return Long.compare(this.getDelay(TimeUnit.NANOSECONDS), delayed.getDelay(TimeUnit.NANOSECONDS));
        }

        @Override
        public Object get() throws InterruptedException, ExecutionException {
            if (this.failure != null) {
                throw new ExecutionException(this.failure);
            }
            ScheduledFuture<?> scheduledFuture = this.future;
            if (scheduledFuture == null) {
                throw new IllegalStateException("source timer not installed");
            }
            return scheduledFuture.get();
        }

        @Override
        public Object get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
            if (this.failure != null) {
                throw new ExecutionException(this.failure);
            }
            ScheduledFuture<?> scheduledFuture = this.future;
            if (scheduledFuture == null) {
                throw new IllegalStateException("source timer not installed");
            }
            return scheduledFuture.get(l, timeUnit);
        }
    }

    static final class Dispatch
    implements Runnable {
        final Ticket ticket;
        final AtomicInteger state = new AtomicInteger();

        Dispatch(Ticket ticket) {
            this.ticket = ticket;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void run() {
            if (!this.state.compareAndSet(0, 1)) {
                return;
            }
            try {
                if (!this.ticket.owner.closed && !this.ticket.owner.context.closed && this.ticket.owner.context.life.admits()) {
                    NativeDeVisibilityJobs.with(this.ticket.credit, () -> NativeDeVisibilityScope.call(this.ticket.owner, () -> {
                        this.ticket.task.run();
                        return null;
                    }));
                }
            }
            catch (Throwable throwable) {
                this.ticket.fail(throwable);
                CombatNpcDamageQueue.failed(throwable);
            }
            finally {
                synchronized (this.ticket) {
                    this.state.set(2);
                    this.ticket.work = null;
                }
                this.ticket.credit.release();
            }
        }
    }
}

