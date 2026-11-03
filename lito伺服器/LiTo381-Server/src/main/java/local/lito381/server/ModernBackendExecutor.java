package local.lito381.server;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Server-owned modern backend capacity. Never shares a native executor or connection pool. */
final class ModernBackendExecutor implements AutoCloseable {
    enum Failure { DEADLINE, CANCELLED, CLOSED, OVERLOADED, BUSY, BACKEND }
    static final class BackendException extends IOException {
        private final Failure failure;
        BackendException(Failure failure) {
            super("modern backend operation failed: " + failure); // No credentials/SQL/cause.
            this.failure = failure;
        }
        Failure failure() { return failure; }
    }
    @FunctionalInterface interface Work<T> extends AutoCloseable {
        T run(ModernBackendScope scope) throws Exception;
        /** Nonblocking owned-input cleanup; also called if this job never starts. */
        @Override default void close() {}
    }
    private enum JobState { NEW, QUEUED, RUNNING, RETIRED }
    private static final AtomicInteger EXECUTORS = new AtomicInteger();
    private final ThreadPoolExecutor workers;
    private final ThreadPoolExecutor abortWorkers;
    private final ScheduledThreadPoolExecutor watchdog;
    private final AtomicBoolean closing = new AtomicBoolean();
    private final Set<SessionGate> sessions = ConcurrentHashMap.newKeySet();
    private final Set<Job<?>> jobs = ConcurrentHashMap.newKeySet();
    private final ModernBackendScope.AbortScheduler abortScheduler;

    ModernBackendExecutor(int workerCount, int queueCapacity) {
        if (workerCount < 1 || workerCount > 64 || queueCapacity < 0 || queueCapacity > 1024) {
            throw new IllegalArgumentException("backend workers must be 1..64 and queue 0..1024");
        }
        String prefix = "modern-backend-" + EXECUTORS.incrementAndGet();
        abortWorkers = new ThreadPoolExecutor(Math.min(2, workerCount), Math.min(2, workerCount),
                0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(workerCount), threads(prefix + "-abort"));
        watchdog = new ScheduledThreadPoolExecutor(1, threads(prefix + "-deadline"));
        watchdog.setRemoveOnCancelPolicy(true);
        watchdog.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        abortScheduler = new ModernBackendScope.AbortScheduler() {
            @Override public boolean execute(Runnable action) {
                try { abortWorkers.execute(action); return true; }
                catch (RejectedExecutionException full) { return false; }
            }
            @Override public void remove(Runnable action) { abortWorkers.remove(action); }
        };
        BlockingQueue<Runnable> queue = queueCapacity == 0 ? new SynchronousQueue<>() : new ArrayBlockingQueue<>(queueCapacity);
        workers = new ThreadPoolExecutor(workerCount, workerCount, 0, TimeUnit.MILLISECONDS, queue,
                threads(prefix + "-work")) {
            @Override protected void afterExecute(Runnable runnable, Throwable failure) {
                try { ((Job<?>) runnable).retire(failure); }
                finally { super.afterExecute(runnable, failure); }
            }
            @Override protected void terminated() {
                // Helpers remain alive until every actual backend run has exited.
                abortWorkers.shutdown();
                watchdog.shutdown();
                super.terminated();
            }
        };
    }

    SessionGate openSession() {
        if (closing.get()) throw new IllegalStateException("modern backend executor is closing");
        SessionGate gate = new SessionGate();
        sessions.add(gate);
        if (closing.get()) gate.close();
        return gate;
    }
    int activeCount() { return workers.getActiveCount(); }
    int queuedCount() { return workers.getQueue().size(); }
    boolean isQuiescent() {
        return jobs.isEmpty() && workers.getActiveCount() == 0 && workers.getQueue().isEmpty()
                && abortWorkers.getActiveCount() == 0 && abortWorkers.getQueue().isEmpty();
    }
    @Override public void close() {
        if (!closing.compareAndSet(false, true)) return;
        for (SessionGate session : sessions) session.close();
        for (Job<?> job : jobs) job.cancel(Failure.CLOSED);
        workers.shutdown(); // No shutdownNow()/Future.cancel pretence that running JDBC exited.
    }
    boolean awaitTermination(long timeoutMillis) throws InterruptedException {
        if (timeoutMillis < 0 || timeoutMillis > 300_000) throw new IllegalArgumentException("invalid backend shutdown timeout");
        if (!closing.get()) return false;
        long end = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        if (!workers.awaitTermination(Math.max(0, end - System.nanoTime()), TimeUnit.NANOSECONDS)) return false;
        if (!abortWorkers.awaitTermination(Math.max(0, end - System.nanoTime()), TimeUnit.NANOSECONDS)) return false;
        return watchdog.awaitTermination(Math.max(0, end - System.nanoTime()), TimeUnit.NANOSECONDS);
    }

    /** One active/queued DB operation for this session until it REALLY retires. */
    final class SessionGate implements AutoCloseable {
        private final Object admission = new Object();
        private final AtomicBoolean closed = new AtomicBoolean();
        private final AtomicReference<Job<?>> current = new AtomicReference<>();
        <T> T call(int timeoutMillis, Work<T> work) throws BackendException {
            Objects.requireNonNull(work);
            if (timeoutMillis < 50 || timeoutMillis > 30_000) {
                discard(work);
                throw new IllegalArgumentException("backend deadline must be 50..30000 ms");
            }
            Job<T> accepted = null;
            Failure admissionFailure = null;
            synchronized (admission) {
                if (closed.get() || closing.get()) admissionFailure = Failure.CLOSED;
                else if (current.get() != null) admissionFailure = Failure.BUSY;
                else {
                    accepted = new Job<>(this, work, System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis));
                    current.set(accepted);
                    // Closing cannot retire NEW between current assignment and
                    // registry insertion, leaving a retired job permanently tracked.
                    jobs.add(accepted);
                }
            }
            if (admissionFailure != null) { discard(work); throw new BackendException(admissionFailure); }
            Job<T> job = accepted;
            if (closed.get() || closing.get()) job.cancel(Failure.CLOSED);
            job.submit();
            try {
                long remaining = job.scope.deadlineNanos() - System.nanoTime();
                if (remaining <= 0) throw new TimeoutException();
                T result = job.result.get(remaining, TimeUnit.NANOSECONDS);
                if (closed.get() || closing.get()) throw new BackendException(Failure.CLOSED);
                if (System.nanoTime() - job.scope.deadlineNanos() >= 0) throw new TimeoutException();
                return result;
            } catch (TimeoutException timeout) {
                job.cancel(Failure.DEADLINE);
                throw new BackendException(Failure.DEADLINE);
            } catch (InterruptedException interrupted) {
                job.cancel(Failure.CANCELLED);
                Thread.currentThread().interrupt();
                throw new BackendException(Failure.CANCELLED);
            } catch (ExecutionException failed) {
                if (failed.getCause() instanceof BackendException backend) throw backend;
                throw new BackendException(Failure.BACKEND);
            }
        }
        boolean isBusy() { return current.get() != null; }
        @Override public void close() {
            Job<?> job;
            synchronized (admission) {
                closed.set(true);
                job = current.get();
            }
            if (job != null) job.cancel(Failure.CANCELLED);
            sessions.remove(this);
        }
    }

    private final class Job<T> implements Runnable {
        private final SessionGate gate;
        private final Work<T> work;
        private final ModernBackendScope scope;
        private final CompletableFuture<T> result = new CompletableFuture<>();
        private final Object lifecycle = new Object();
        private final AtomicBoolean cleaned = new AtomicBoolean();
        private final AtomicBoolean retired = new AtomicBoolean();
        private JobState state = JobState.NEW;
        private ScheduledFuture<?> timer;
        private T value;
        private Failure failure;
        Job(SessionGate gate, Work<T> work, long deadline) {
            this.gate = gate; this.work = work;
            scope = new ModernBackendScope(deadline, abortScheduler);
        }
        void submit() {
            boolean rejected = false;
            synchronized (lifecycle) {
                if (state == JobState.RETIRED) return;
                if (closing.get()) rejected = true;
                else {
                    try {
                        timer = watchdog.schedule(() -> cancel(Failure.DEADLINE),
                                Math.max(0, scope.deadlineNanos() - System.nanoTime()), TimeUnit.NANOSECONDS);
                        state = JobState.QUEUED;
                        // The worker takes this same lock before starting, so a
                        // queue removal cannot race a claimed running operation.
                        workers.execute(this);
                    } catch (RejectedExecutionException full) { rejected = true; }
                }
                if (rejected) state = JobState.RETIRED;
            }
            if (rejected) {
                scope.cancel(closing.get() ? Failure.CLOSED : Failure.OVERLOADED);
                cleanup();
                retire(null);
            }
        }
        void cancel(Failure reason) {
            scope.cancel(reason);
            boolean removed = false;
            synchronized (lifecycle) {
                if (state == JobState.NEW || (state == JobState.QUEUED && workers.remove(this))) {
                    state = JobState.RETIRED;
                    removed = true;
                }
            }
            if (removed) { cleanup(); retire(null); }
            result.completeExceptionally(new BackendException(scope.cancellation()));
        }
        @Override public void run() {
            synchronized (lifecycle) {
                if (state != JobState.QUEUED) return;
                state = JobState.RUNNING;
            }
            try {
                ModernBackendScope.bind(scope);
                scope.checkActive();
                value = work.run(scope);
                scope.checkActive();
            } catch (BackendException failed) { failure = failed.failure(); }
            catch (Throwable ignored) { failure = Failure.BACKEND; }
            finally {
                try { cleanup(); }
                finally { ModernBackendScope.unbind(); }
            }
        }
        private void cleanup() {
            if (!cleaned.compareAndSet(false, true)) return;
            try { work.close(); }
            catch (Throwable ignored) { failure = Failure.BACKEND; }
            finally { scope.finish(); }
        }
        /** Called by afterExecute, not when a Future merely becomes cancelled. */
        private void retire(Throwable unexpected) {
            if (!retired.compareAndSet(false, true)) return;
            synchronized (lifecycle) { state = JobState.RETIRED; if (timer != null) timer.cancel(false); }
            if (unexpected != null) failure = Failure.BACKEND;
            if (System.nanoTime() - scope.deadlineNanos() >= 0) scope.cancel(Failure.DEADLINE);
            if (scope.cancellation() != null) failure = scope.cancellation();
            gate.current.compareAndSet(this, null);
            jobs.remove(this);
            if (failure == null) result.complete(value);
            else result.completeExceptionally(new BackendException(failure));
        }
    }
    private static void discard(Work<?> work) {
        try { work.close(); } catch (Throwable ignored) { /* Never log an owned secret or backend cause. */ }
    }
    private static ThreadFactory threads(String prefix) {
        AtomicInteger ids = new AtomicInteger();
        return action -> new Thread(action, prefix + "-" + ids.incrementAndGet());
    }
}
