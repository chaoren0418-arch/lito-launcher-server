package local.lito381.server;

import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLSocket;

/** One modern backend operation, with one monotonic deadline across every source. */
final class ModernBackendScope {
    interface AbortScheduler {
        boolean execute(Runnable action);
        void remove(Runnable action);
    }
    private static final ThreadLocal<ModernBackendScope> CURRENT = new ThreadLocal<>();
    private final long deadlineNanos;
    private final AbortScheduler aborts;
    private final Object leaseLock = new Object();
    private final AtomicReference<ModernBackendExecutor.Failure> cancelled = new AtomicReference<>();
    private volatile boolean finished;
    private ConnectionLease connection;
    private SocketLease socket;

    ModernBackendScope(long deadlineNanos, AbortScheduler aborts) {
        this.deadlineNanos = deadlineNanos;
        this.aborts = Objects.requireNonNull(aborts);
    }
    static ModernBackendScope current() {
        ModernBackendScope scope = CURRENT.get();
        if (scope == null) throw new IllegalStateException("modern JDBC requires a scoped backend worker");
        return scope;
    }
    static void bind(ModernBackendScope scope) {
        if (CURRENT.get() != null) throw new IllegalStateException("nested modern backend scope");
        CURRENT.set(scope);
    }
    static void unbind() { CURRENT.remove(); }
    long deadlineNanos() { return deadlineNanos; }
    ModernBackendExecutor.Failure cancellation() { return cancelled.get(); }

    void checkActive() throws ModernBackendExecutor.BackendException {
        if (System.nanoTime() - deadlineNanos >= 0) cancel(ModernBackendExecutor.Failure.DEADLINE);
        var reason = cancelled.get();
        if (reason != null) throw new ModernBackendExecutor.BackendException(reason);
        if (finished) throw new ModernBackendExecutor.BackendException(ModernBackendExecutor.Failure.CLOSED);
    }
    /** Ceil milliseconds, never zero (zero can mean infinite to JDBC/socket APIs). */
    int remainingMillis() throws ModernBackendExecutor.BackendException {
        checkActive();
        long remaining = deadlineNanos - System.nanoTime();
        if (remaining <= 0) {
            cancel(ModernBackendExecutor.Failure.DEADLINE);
            throw new ModernBackendExecutor.BackendException(ModernBackendExecutor.Failure.DEADLINE);
        }
        return (int) Math.max(1, Math.min(Integer.MAX_VALUE,
                TimeUnit.NANOSECONDS.toMillis(remaining - 1) + 1));
    }

    ConnectionLease register(Connection ownedConnection) throws ModernBackendExecutor.BackendException {
        Objects.requireNonNull(ownedConnection);
        checkActive();
        synchronized (leaseLock) {
            ensureRegisterable();
            if (connection != null) throw new IllegalStateException("only one active JDBC connection per operation");
            return connection = new ConnectionLease(ownedConnection);
        }
    }
    SocketLease registerSocket(Socket ownedRawSocket) throws ModernBackendExecutor.BackendException {
        Objects.requireNonNull(ownedRawSocket);
        if (ownedRawSocket instanceof SSLSocket) throw new IllegalArgumentException("register the physical raw JDBC socket");
        checkActive();
        synchronized (leaseLock) {
            ensureRegisterable();
            if (socket != null) throw new IllegalStateException("only one active physical JDBC socket per operation");
            return socket = new SocketLease(ownedRawSocket);
        }
    }
    private void ensureRegisterable() throws ModernBackendExecutor.BackendException {
        var reason = cancelled.get();
        if (reason != null) throw new ModernBackendExecutor.BackendException(reason);
        if (finished || System.nanoTime() - deadlineNanos >= 0) {
            throw new ModernBackendExecutor.BackendException(finished
                    ? ModernBackendExecutor.Failure.CLOSED : ModernBackendExecutor.Failure.DEADLINE);
        }
    }

    /** Nonblocking with respect to JDBC monitors: first close raw TCP, then queue JDBC abort. */
    void cancel(ModernBackendExecutor.Failure reason) {
        if (!cancelled.compareAndSet(null, Objects.requireNonNull(reason))) return;
        ConnectionLease ownedConnection;
        SocketLease ownedSocket;
        synchronized (leaseLock) { ownedConnection = connection; ownedSocket = socket; }
        if (ownedSocket != null) ownedSocket.abortIfOwned();
        if (ownedConnection != null) ownedConnection.requestAbort();
    }

    /** Worker-finally safety net. Leaked leases cannot turn into a successful result. */
    void finish() {
        ConnectionLease ownedConnection;
        SocketLease ownedSocket;
        synchronized (leaseLock) { ownedConnection = connection; ownedSocket = socket; }
        if (ownedConnection != null || ownedSocket != null) {
            cancel(ModernBackendExecutor.Failure.BACKEND);
            if (ownedSocket != null) ownedSocket.abortIfOwned();
            if (ownedConnection != null) {
                // Remains inside the counted backend worker until abort really exits.
                ownedConnection.abortIfOwned();
                ownedConnection.close();
            }
            if (ownedSocket != null) ownedSocket.close();
        }
        finished = true;
    }

    /**
     * Retire BEFORE Connection.close/return-to-pool. If abort has started,
     * retirement waits for that actual abort, never just its Future cancellation.
     */
    final class ConnectionLease implements AutoCloseable {
        private final Connection owned;
        private final Object ownership = new Object();
        private final AtomicBoolean abortRequested = new AtomicBoolean();
        private final Runnable abortAction = this::abortIfOwned;
        private boolean active = true;
        private boolean abortFinished;
        private ConnectionLease(Connection owned) { this.owned = owned; }
        private void requestAbort() {
            if (abortRequested.compareAndSet(false, true)) {
                // Saturation never retires a live job. The raw socket is already
                // closed and driver deadlines remain required by the DAO.
                aborts.execute(abortAction);
            }
        }
        private void abortIfOwned() {
            synchronized (ownership) {
                if (!active || abortFinished) return;
                try { owned.abort(Runnable::run); }
                catch (SQLException | RuntimeException failure) {
                    try { owned.close(); } catch (SQLException | RuntimeException ignored) {}
                } finally { abortFinished = true; }
            }
        }
        @Override public void close() {
            synchronized (ownership) { active = false; }
            aborts.remove(abortAction);
            synchronized (leaseLock) { if (connection == this) connection = null; }
        }
    }

    /** Retire LAST, after the owned JDBC connection has actually finished closing. */
    final class SocketLease implements AutoCloseable {
        private final Socket owned;
        private final Object ownership = new Object();
        private boolean active = true;
        private SocketLease(Socket owned) { this.owned = owned; }
        private void abortIfOwned() {
            synchronized (ownership) {
                if (!active) return;
                try { owned.close(); } catch (IOException ignored) {}
            }
        }
        @Override public void close() {
            synchronized (ownership) { active = false; }
            synchronized (leaseLock) { if (socket == this) socket = null; }
        }
    }
}
