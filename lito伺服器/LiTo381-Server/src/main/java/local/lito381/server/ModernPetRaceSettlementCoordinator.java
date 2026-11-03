package local.lito381.server;

import static local.lito381.server.ModernPetRaceSettlement.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Owned bounded outbox replay. A match reserves a durable slot before admission;
 * its server-authoritative result is recorded before FINISHED is published.
 * SQL retries keep the exact UUID/body across process restarts. Local recording
 * and SQL confirmation are separate facts, never a claim about power-loss IO.
 * No disk, JDBC, or executor join occurs while the coordinator gate is held.
 */
final class ModernPetRaceSettlementCoordinator implements JournaledStore {
    private static final int WORKERS = 2;
    private final Object gate = new Object();
    private final ModernPetRaceOutbox outbox;
    private final Store sql;
    private final int deadlineMillis;
    private final ModernSettlementConnections connections;
    private final LinkedHashMap<UUID, Task> pending = new LinkedHashMap<>();
    private final Set<Token> reservations = new HashSet<>();
    private final ThreadPoolExecutor workers;
    private final ScheduledThreadPoolExecutor scheduler;
    private int ownedTasks, recording;
    private boolean closed, fatal, retiring, resourcesClosed;

    private static final class Task {
        final Event event;
        volatile ModernPetRaceProtocol.Settlement state = ModernPetRaceProtocol.Settlement.PENDING;
        boolean running;
        int attempts;
        long nextAttempt;
        Task(Event event) { this.event = event; nextAttempt = System.nanoTime(); }
    }

    ModernPetRaceSettlementCoordinator(ModernPetRaceOutbox outbox, Store sql, int deadlineMillis,
                                ModernSettlementConnections ownedConnections) throws IOException {
        this.outbox = Objects.requireNonNull(outbox); this.sql = Objects.requireNonNull(sql);
        if (deadlineMillis < 50 || deadlineMillis > 60_000) throw new IllegalArgumentException("invalid settlement deadline");
        this.deadlineMillis = deadlineMillis; this.connections = ownedConnections;
        // Validate all committed records before admitting a listener or starting replay.
        for (Event event : outbox.pending(deadline())) pending.put(event.resultId(), new Task(event));
        workers = new ThreadPoolExecutor(WORKERS, WORKERS, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(WORKERS), r -> new Thread(r, "modern-pet-race-settlement-replay"),
                new ThreadPoolExecutor.AbortPolicy());
        scheduler = new ScheduledThreadPoolExecutor(1, r -> new Thread(r, "modern-pet-race-settlement-schedule"));
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.scheduleWithFixedDelay(this::dispatch, 0, 50, TimeUnit.MILLISECONDS);
    }

    @Override public Reservation reserve(long matchId, long generation) throws SettlementException {
        if (matchId < 1 || generation < 1) throw new IllegalArgumentException("invalid settlement reservation");
        boolean healthy = outbox.isHealthy();
        synchronized (gate) {
            if (closed || fatal || !healthy || pending.size() + reservations.size() >= ModernPetRaceOutbox.MAX_PENDING)
                throw failure(Failure.UNAVAILABLE);
            if (reservations.stream().anyMatch(t -> t.matchId == matchId && t.generation == generation))
                throw failure(Failure.CONFLICT);
            Token token = new Token(matchId, generation); reservations.add(token); return token;
        }
    }
    @Override public boolean accepting() {
        boolean healthy = outbox.isHealthy();
        synchronized (gate) { return !closed && !fatal && healthy && pending.size() + reservations.size() < ModernPetRaceOutbox.MAX_PENDING; }
    }

    private final class Token implements Reservation {
        final long matchId, generation;
        Event event;
        Task task;
        boolean writing, released;
        Token(long matchId, long generation) { this.matchId = matchId; this.generation = generation; }
        @Override public void record(Event value, long absoluteDeadlineNanos) throws Exception {
            Objects.requireNonNull(value);
            synchronized (gate) {
                if (value.matchId() != matchId || value.generation() != generation || event != null && !event.sameAuthority(value))
                    throw failure(Failure.CONFLICT);
                if (task != null) return; // This exact event has already crossed the durable boundary.
                if (closed || released || writing || fatal || !reservations.contains(this)) throw failure(Failure.UNAVAILABLE);
                event = value; writing = true; recording++;
            }
            try {
                outbox.record(value, absoluteDeadlineNanos);
                synchronized (gate) {
                    Task existing = pending.get(value.resultId());
                    if (existing != null && !existing.event.sameAuthority(value)) { fatal = true; throw failure(Failure.CONFLICT); }
                    task = existing == null ? new Task(value) : existing;
                    pending.put(value.resultId(), task); reservations.remove(this);
                    gate.notifyAll();
                }
            } catch (Exception failure) {
                // Do not turn an unconfirmed local rename into an ABORTED result.
                // The authority file alone decides replay after reopening.
                synchronized (gate) { fatal = true; }
                throw failure;
            } finally {
                synchronized (gate) {
                    writing = false; recording--;
                    if (released || closed) reservations.remove(this);
                    gate.notifyAll();
                }
                retireResources();
            }
        }
        @Override public ModernPetRaceProtocol.Settlement state() { synchronized (gate) { return task == null ? ModernPetRaceProtocol.Settlement.PENDING : task.state; } }
        @Override public void close() {
            synchronized (gate) { released = true; if (!writing) reservations.remove(this); }
            retireResources();
        }
    }

    /** Compatibility API cannot silently bypass journal admission. Production uses token receipts. */
    @Override public void persist(Event event, long absoluteDeadlineNanos) throws Exception {
        synchronized (gate) {
            Task task = pending.get(event.resultId());
            if (task == null || !task.event.sameAuthority(event)) throw failure(Failure.INVALID_STORAGE);
            for (;;) {
                if (task.state == ModernPetRaceProtocol.Settlement.SAVED) return;
                if (task.state == ModernPetRaceProtocol.Settlement.FAILED) throw failure(Failure.INVALID_STORAGE);
                long left = absoluteDeadlineNanos - System.nanoTime();
                if (left <= 0) throw failure(Failure.DEADLINE);
                if (left > TimeUnit.SECONDS.toNanos(60)) throw new IllegalArgumentException("invalid settlement deadline");
                if (closed) throw failure(Failure.UNAVAILABLE);
                TimeUnit.NANOSECONDS.timedWait(gate, left);
            }
        }
    }

    private void dispatch() {
        // Count ownership before executor handoff, including firstTask/queue gaps.
        for (;;) {
            Task selected = null;
            synchronized (gate) {
                if (closed || fatal || ownedTasks >= WORKERS) return;
                long now = System.nanoTime();
                for (Task task : pending.values()) {
                    if (!task.running && task.state != ModernPetRaceProtocol.Settlement.FAILED && now - task.nextAttempt >= 0) {
                        selected = task; task.running = true; ownedTasks++; break;
                    }
                }
            }
            if (selected == null) return;
            final Task task = selected;
            try { workers.execute(() -> write(task)); }
            catch (RejectedExecutionException rejected) { releaseTask(task); }
        }
    }
    private void write(Task task) {
        try {
            long end = deadline();
            if (task.state != ModernPetRaceProtocol.Settlement.SAVED) {
                sql.persist(task.event, end);
                synchronized (gate) { task.state = ModernPetRaceProtocol.Settlement.SAVED; gate.notifyAll(); }
            }
            // A confirmed commit remains SAVED even if durable garbage collection fails.
            // Retry/boot replay is safe because SQL verifies this same full event UUID.
            outbox.acknowledge(task.event, end);
            synchronized (gate) { pending.remove(task.event.resultId(), task); }
        } catch (Exception problem) {
            boolean permanent = problem instanceof SettlementException typed
                    && (typed.failure() == Failure.CONFLICT || typed.failure() == Failure.INVALID_STORAGE);
            boolean healthy = outbox.isHealthy();
            synchronized (gate) {
                if (permanent && task.state != ModernPetRaceProtocol.Settlement.SAVED) task.state = ModernPetRaceProtocol.Settlement.FAILED;
                if (permanent || !healthy) fatal = true;
                task.attempts = Math.min(task.attempts + 1, 10);
                task.nextAttempt = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(Math.min(30_000, 50L << task.attempts));
                gate.notifyAll();
            }
            // Interruption never means the remote transaction rolled back.
            if (problem instanceof InterruptedException) Thread.currentThread().interrupt();
        } finally { releaseTask(task); }
    }
    private void releaseTask(Task task) {
        synchronized (gate) { task.running = false; ownedTasks--; gate.notifyAll(); }
        retireResources();
    }
    private long deadline() { return System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(deadlineMillis); }

    @Override public void close() {
        synchronized (gate) {
            if (closed) return; closed = true;
            reservations.removeIf(t -> !t.writing); gate.notifyAll();
        }
        scheduler.shutdown(); workers.shutdown();
        retireResources();
    }
    private void retireResources() {
        synchronized (gate) {
            if (!closed || ownedTasks != 0 || recording != 0 || retiring) return;
            retiring = true;
        }
        try {
            if (connections != null) connections.close();
        } finally {
            try { outbox.close(); }
            finally { synchronized (gate) { resourcesClosed = true; gate.notifyAll(); } }
        }
    }
    @Override public boolean awaitTermination(long timeoutMillis) throws InterruptedException {
        if (timeoutMillis < 0 || timeoutMillis > 60_000) throw new IllegalArgumentException("invalid settlement wait");
        long end = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        if (!scheduler.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS)) return false;
        if (!workers.awaitTermination(Math.max(0, end - System.nanoTime()), TimeUnit.NANOSECONDS)) return false;
        synchronized (gate) {
            while (closed && !resourcesClosed && end - System.nanoTime() > 0)
                TimeUnit.NANOSECONDS.timedWait(gate, end - System.nanoTime());
        }
        if (connections != null && !connections.awaitTermination(Math.max(0, TimeUnit.NANOSECONDS.toMillis(end - System.nanoTime())))) return false;
        return isQuiescent();
    }
    @Override public boolean isQuiescent() {
        boolean ioRetired = outbox.isQuiescent() && (connections == null || connections.isQuiescent());
        synchronized (gate) { return closed && resourcesClosed && ownedTasks == 0 && recording == 0 && reservations.isEmpty()
                && scheduler.isTerminated() && workers.isTerminated() && ioRetired; }
    }

    private static SettlementException failure(Failure failure) { return new SettlementException(failure); }
}

