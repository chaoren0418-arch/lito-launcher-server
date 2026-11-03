package local.lito381.server;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocket;

/**
 * Server-side transport for ONE accepted SSLSocket; creates no listener and is
 * not connected to Main/native ClientSession. Requires a normally configured
 * SSLContext with a server key/certificate. Clients must verify its chain/name.
 * TLS 1.2+ precedes all LTM1 bytes; native XOR/integrity is never involved.
 *
 * Both socket idle-read timeout and a whole-operation close deadline are used
 * (SO_TIMEOUT alone does not bound slow-drip records). One caller owns session
 * operations; the watchdog can only close it. Backend verifier operations must
 * have their own database/service deadlines. ModernApplicationSession owns the
 * post-HELLO application loop; no unprotected stream is exposed to that loop.
 */
final class ModernTlsSessionTransport implements AutoCloseable {
    enum State { NEW, TLS_HANDSHAKING, TLS_READY, NEGOTIATED, AUTHENTICATING, AUTHENTICATED, AUTH_REJECTED, CLOSED }
    record Limits(int handshakeMillis, int frameMillis) {
        Limits {
            if (handshakeMillis < 50 || handshakeMillis > 30_000 || frameMillis < 50 || frameMillis > 30_000) {
                throw new IllegalArgumentException("transport deadlines must be 50..30000 ms");
            }
        }
        static Limits defaults() { return new Limits(5_000, 5_000); }
    }
    record TlsInfo(String protocol, String cipherSuite) {}

    private static final Set<String> PROTOCOLS = Set.of("TLSv1.2", "TLSv1.3");
    static final int CLOSE_DEADLINE_MILLIS = 250;
    private static final ScheduledThreadPoolExecutor DEADLINES = deadlines();
    private static final ModernTransportDeadline.Scheduler SCHEDULER = (expiration, delay) -> {
        var future = DEADLINES.schedule(expiration, delay, TimeUnit.MILLISECONDS);
        return () -> future.cancel(false);
    };
    private final SSLSocket socket;
    private final Limits limits;
    private final ModernSessionBoundary boundary;
    private final ModernAccountAuthentication.CredentialVerifier verifier;
    private final AtomicReference<State> state = new AtomicReference<>(State.NEW);
    private final AtomicBoolean gracefulCloseStarted = new AtomicBoolean();
    private final AtomicBoolean closeDeadlineExpired = new AtomicBoolean();
    private final AtomicBoolean ownerCleanupFailed = new AtomicBoolean();
    private final AtomicReference<ModernSessionFailures.Point> firstFailure = new AtomicReference<>();
    private final AtomicReference<Thread> retirementOwner = new AtomicReference<>();
    private final CountDownLatch retirementDone = new CountDownLatch(1);
    private record BackendBinding(ModernBackendExecutor.SessionGate gate, int timeoutMillis) {}
    private record AuthenticationAttempt(ModernAccountAuthentication.Principal principal,
                                         ModernAccountAuthentication.Failure failure) {}
    private final AtomicReference<BackendBinding> backend = new AtomicReference<>();
    private final AtomicReference<ModernNavigationSession> navigation = new AtomicReference<>();
    private volatile ModernAccountAuthentication.Principal principal;
    private volatile TlsInfo tlsInfo;

    ModernTlsSessionTransport(SSLSocket acceptedSocket, ModernSessionBoundary.Policy policy,
                              Limits limits, ModernAccountAuthentication.CredentialVerifier verifier) {
        this(acceptedSocket,policy,ModernSessionExtensions.NONE,limits,verifier);
    }
    ModernTlsSessionTransport(SSLSocket acceptedSocket, ModernSessionBoundary.Policy policy,
                              ModernSessionExtensions.Offer extensions,Limits limits,
                              ModernAccountAuthentication.CredentialVerifier verifier) {
        socket = Objects.requireNonNull(acceptedSocket);
        this.limits = Objects.requireNonNull(limits);
        this.verifier = Objects.requireNonNull(verifier);
        boundary = new ModernSessionBoundary(Objects.requireNonNull(policy),extensions);
        if (!socket.isConnected() || socket.isClosed() || socket.getUseClientMode()) {
            throw new IllegalArgumentException("an accepted server-mode SSLSocket is required");
        }
    }

    State state() { return state.get(); }
    /** Close completion is not proof of close_notify exchange if this flag is true. */
    boolean closeDeadlineExceeded() { return closeDeadlineExpired.get(); }
    /** Socket closure is not evidence that an exceptional authority release succeeded. */
    boolean ownerCleanupFailed() { return ownerCleanupFailed.get(); }
    Optional<ModernSessionFailures.Point> failurePoint() { return Optional.ofNullable(firstFailure.get()); }
    void noteApplicationFailure(Throwable failure) { noteFailure(ModernSessionFailures.Stage.DISPATCH, failure); }
    private void noteFailure(ModernSessionFailures.Stage stage, Throwable failure) {
        firstFailure.compareAndSet(null, ModernSessionFailures.point(stage, failure));
    }
    Optional<TlsInfo> tlsInfo() { return Optional.ofNullable(tlsInfo); }
    Optional<ModernAccountAuthentication.Principal> authenticatedPrincipal() {
        return state.get() == State.AUTHENTICATED ? Optional.ofNullable(principal) : Optional.empty();
    }

    /** One modern session backend owner; registration after close cancels immediately. */
    void attachBackend(ModernBackendExecutor.SessionGate gate, int timeoutMillis) {
        Objects.requireNonNull(gate);
        if (timeoutMillis < 50 || timeoutMillis > 30_000) throw new IllegalArgumentException("invalid backend deadline");
        if (!backend.compareAndSet(null, new BackendBinding(gate, timeoutMillis))) {
            throw new IllegalStateException("modern transport backend already attached");
        }
        // No TLS/boundary monitor is held while calling the external owner.
        if (state.get() == State.CLOSED) gate.close();
    }

    private void cancelBackend() {
        BackendBinding binding = backend.get();
        if (binding != null) binding.gate().close();
    }

    void attachNavigation(ModernNavigationSession session) {
        if (!navigation.compareAndSet(null, Objects.requireNonNull(session))) {
            throw new IllegalStateException("modern navigation already attached");
        }
        if (state.get() == State.CLOSED) session.close();
    }

    private void closeNavigation() {
        ModernNavigationSession session = navigation.get();
        if (session != null) session.close();
    }

    ModernSessionBoundary.Negotiated negotiate() throws IOException {
        if (!state.compareAndSet(State.NEW, State.TLS_HANDSHAKING)) {
            abort();
            throw new IOException("modern TLS negotiation is accepted exactly once");
        }
        try {
            String[] allowed = Arrays.stream(socket.getEnabledProtocols()).filter(PROTOCOLS::contains).toArray(String[]::new);
            if (allowed.length == 0) throw new SSLHandshakeException("TLS 1.2 or newer is required");
            socket.setEnabledProtocols(allowed);
            // Keep JSSE's secure defaults, never turn on an otherwise disabled suite.
            String[] ciphers = Arrays.stream(socket.getEnabledCipherSuites())
                    .filter(cipher -> !cipher.contains("_anon_") && !cipher.contains("_NULL_")
                            && !cipher.contains("_EXPORT_") && !cipher.contains("RC4")
                            && !cipher.contains("DES"))
                    .toArray(String[]::new);
            if (ciphers.length == 0) throw new SSLHandshakeException("no secure TLS cipher suite");
            socket.setEnabledCipherSuites(ciphers);
            socket.setSoTimeout(limits.handshakeMillis());
            try (var deadline = deadline(limits.handshakeMillis(), ModernSessionFailures.Stage.NEGOTIATION)) {
                socket.startHandshake();
                deadline.complete();
            }
            var session = socket.getSession();
            if (!PROTOCOLS.contains(session.getProtocol()) || !session.isValid() || socket.isClosed()) {
                throw new SSLHandshakeException("TLS handshake did not establish a valid protected session");
            }
            tlsInfo = new TlsInfo(session.getProtocol(), session.getCipherSuite());
            if (!state.compareAndSet(State.TLS_HANDSHAKING, State.TLS_READY)) {
                throw new SocketTimeoutException("modern TLS connection closed during handshake");
            }
            socket.setSoTimeout(limits.frameMillis());
            try (var deadline = deadline(limits.frameMillis(), ModernSessionFailures.Stage.NEGOTIATION)) {
                byte[] hello = ModernSessionProtocol.readFrame(socket.getInputStream());
                byte[] ack = boundary.acceptHello(hello);
                socket.getOutputStream().write(ack);
                socket.getOutputStream().flush();
                deadline.complete();
                if (!state.compareAndSet(State.TLS_READY, State.NEGOTIATED)) {
                    throw new SocketTimeoutException("modern negotiation connection closed");
                }
            }
            return boundary.negotiated().orElseThrow(() -> new IOException("modern negotiation closed"));
        } catch (IOException | RuntimeException failure) {
            noteFailure(ModernSessionFailures.Stage.NEGOTIATION, failure);
            abort();
            throw failure;
        }
    }

    /**
     * For an authenticated-TLS credential decoder, NOT plaintext ingress.
     * One attempt per connection. Consumes/zeroes password even before TLS or
     * after closure. Negotiation never invokes the verifier automatically.
     */
    ModernAccountAuthentication.Principal authenticate(String account, char[] password)
            throws ModernAccountAuthentication.AuthenticationException {
        return authenticate(account, password, false);
    }

    /** A rejected attempt may send ONE generic result before the session loop closes. */
    ModernAccountAuthentication.Principal authenticateForApplication(String account, char[] password)
            throws ModernAccountAuthentication.AuthenticationException {
        return authenticate(account, password, true);
    }

    private ModernAccountAuthentication.Principal authenticate(String account, char[] password,
                                                                boolean allowFailureReply)
            throws ModernAccountAuthentication.AuthenticationException {
        try {
            BackendBinding binding = backend.get();
            if (socket.isClosed() || tlsInfo == null || binding == null
                    || !state.compareAndSet(State.NEGOTIATED, State.AUTHENTICATING)) {
                throw new ModernAccountAuthentication.AuthenticationException(ModernAccountAuthentication.Failure.INVALID_STATE);
            }
            // The worker owns a separate secret even when the caller times out,
            // closes, or zeroes the decoded AUTH object before JDBC has retired.
            char[] ownedPassword = password == null ? null : password.clone();
            ModernAccountAuthentication.clear(password);
            AuthenticationAttempt attempt;
            try {
                attempt = binding.gate().call(binding.timeoutMillis(), new ModernBackendExecutor.Work<AuthenticationAttempt>() {
                    @Override public AuthenticationAttempt run(ModernBackendScope scope) {
                        try {
                            return new AuthenticationAttempt(ModernAccountAuthentication.verify(verifier, account, ownedPassword), null);
                        } catch (ModernAccountAuthentication.AuthenticationException failure) {
                            return new AuthenticationAttempt(null, failure.failure());
                        }
                    }
                    @Override public void close() { ModernAccountAuthentication.clear(ownedPassword); }
                });
            } catch (ModernBackendExecutor.BackendException failed) {
                var reason = failed.failure();
                throw new ModernAccountAuthentication.AuthenticationException(
                        reason == ModernBackendExecutor.Failure.CANCELLED || reason == ModernBackendExecutor.Failure.CLOSED
                                || reason == ModernBackendExecutor.Failure.DEADLINE
                                ? ModernAccountAuthentication.Failure.INVALID_STATE
                                : ModernAccountAuthentication.Failure.BACKEND_UNAVAILABLE);
            }
            if (attempt.failure() != null) throw new ModernAccountAuthentication.AuthenticationException(attempt.failure());
            var verified = attempt.principal();
            principal = verified;
            if (!state.compareAndSet(State.AUTHENTICATING, State.AUTHENTICATED)) {
                principal = null;
                throw new ModernAccountAuthentication.AuthenticationException(ModernAccountAuthentication.Failure.INVALID_STATE);
            }
            return verified;
        } catch (ModernAccountAuthentication.AuthenticationException failure) {
            if (!allowFailureReply || failure.failure() == ModernAccountAuthentication.Failure.INVALID_STATE
                    || !state.compareAndSet(State.AUTHENTICATING, State.AUTH_REJECTED)) abort();
            throw failure;
        } finally {
            ModernAccountAuthentication.clear(password);
        }
    }

    /** Whole-frame deadline, including first byte: PINGs renew idle life, partial frames do not. */
    ModernApplicationProtocol.Message readApplication(int deadlineMillis) throws IOException {
        if (deadlineMillis < 50 || deadlineMillis > 30_000) throw new IllegalArgumentException("invalid application deadline");
        try {
            requireApplicationState(false);
            socket.setSoTimeout(deadlineMillis);
            try (var deadline = deadline(deadlineMillis, ModernSessionFailures.Stage.APPLICATION_READ)) {
                var peer=boundary.negotiated().orElseThrow(()->new IOException("modern negotiation closed"));
                byte[] frame = ModernApplicationProtocol.readFrame(socket.getInputStream(),peer);
                try {
                    if (frame == null) {
                        // A complete clean EOF has finished the read. Owner
                        // retirement may wait on another thread, but must not
                        // remain subject to this already-completed frame timer.
                        deadline.complete();
                        close();
                        return null;
                    }
                    ModernApplicationProtocol.Message message = ModernApplicationProtocol.decode(frame,peer);
                    try { deadline.complete(); return message; }
                    catch (IOException failure) { message.close(); throw failure; }
                } finally { if (frame != null) Arrays.fill(frame, (byte) 0); }
            }
        } catch (IOException | RuntimeException failure) {
            noteFailure(ModernSessionFailures.Stage.APPLICATION_READ, failure);
            abort();
            throw failure;
        }
    }

    void writeApplication(ModernApplicationProtocol.Message message) throws IOException {
        try {
            requireApplicationState(true);
            var peer=boundary.negotiated().orElseThrow(()->new IOException("modern negotiation closed"));
            if(message instanceof ModernApplicationProtocol.BattleReadResult&&!ModernBattleReadProtocol.negotiated(peer))
                throw new ModernApplicationProtocol.ProtocolException();
            byte[] frame = ModernApplicationProtocol.encode(ModernMatchDescriptor.forPeer(message,peer),peer);
            try (var deadline = deadline(limits.frameMillis(), ModernSessionFailures.Stage.APPLICATION_WRITE)) {
                socket.getOutputStream().write(frame);
                socket.getOutputStream().flush();
                deadline.complete();
            } finally { Arrays.fill(frame, (byte) 0); }
        } catch (IOException | RuntimeException failure) {
            noteFailure(ModernSessionFailures.Stage.APPLICATION_WRITE, failure);
            abort();
            throw failure;
        }
    }

    private void requireApplicationState(boolean writing) throws IOException {
        State current = state.get();
        if (socket.isClosed() || tlsInfo == null || (current != State.NEGOTIATED && current != State.AUTHENTICATED
                && !(writing && current == State.AUTH_REJECTED))) {
            throw new IOException("application frame requires negotiated TLS session");
        }
    }

    /** Protocol failures must not wait for a malicious peer's TLS close_notify. */
    void failClosed() { abort(); }

    /**
     * Best-effort graceful TLS close with one absolute 250 ms deadline, then
     * hard abort. SO_TIMEOUT alone would let a slow peer extend JSSE's duplex
     * close indefinitely. Repeated/concurrent close cannot start another wait.
     * A deadline fallback completes resource closure. The diagnostic flag only
     * records that fallback; false does not prove the peer sent close_notify.
     */
    @Override public void close() throws IOException {
        state.set(State.CLOSED);
        principal = null;
        try {
            // Bound/finish TLS before callbacks can wait for an in-flight world
            // guard or an owned backend retirement. The watchdog does no cleanup.
            closeTlsGracefully();
        } finally {
            try { boundary.close(); }
            finally { retireApplicationOwners(); }
        }
        if (ownerCleanupFailed.get()) throw new IOException("modern session owner cleanup failed");
    }

    private void closeTlsGracefully() throws IOException {
        if (socket.isClosed()) return;
        if (!gracefulCloseStarted.compareAndSet(false, true)) {
            hardCloseSocket();
            return;
        }
        try (var watchdog = new ModernTransportDeadline(CLOSE_DEADLINE_MILLIS, System::nanoTime, SCHEDULER, () -> {
            closeDeadlineExpired.set(true);
            closeSocketOnly();
        }, () -> false)) {
            socket.close();
        } catch (IOException failure) {
            noteFailure(ModernSessionFailures.Stage.TLS_CLOSE, failure);
            closeSocketOnly();
            throw failure;
        }
    }

    private void abort() {
        // The caller may wait for real owner cleanup, but the network must
        // already be closed so neither the peer nor another deadline depends on it.
        closeSocketOnly();
        try { boundary.close(); }
        finally { retireApplicationOwners(); }
    }

    /** Deadline-thread safe: no boundary, navigation, backend or external callback. */
    private void closeSocketOnly() {
        state.set(State.CLOSED);
        principal = null;
        hardCloseSocket();
    }

    private void retireApplicationOwners() {
        if (retirementDone.getCount() == 0) return;
        Thread current = Thread.currentThread();
        if (retirementOwner.compareAndSet(null, current)) {
            try {
                try { cancelBackend(); }
                catch (RuntimeException | Error failure) { ownerCleanupFailed.set(true); noteFailure(ModernSessionFailures.Stage.OWNER_CLEANUP, failure); }
                finally {
                    try { closeNavigation(); }
                    catch (RuntimeException | Error failure) { ownerCleanupFailed.set(true); noteFailure(ModernSessionFailures.Stage.OWNER_CLEANUP, failure); }
                }
            } finally {
                // Even exceptional cleanup must wake ordinary joining callers;
                // its diagnostic is retained rather than reporting success.
                retirementDone.countDown();
            }
        } else if (retirementOwner.get() != current) {
            // A second close must not mistake Navigation.closed (admission is
            // sealed) for complete room/presence retirement. The owning server
            // worker remains counted until the first caller REALLY finishes.
            boolean interrupted = false;
            while (retirementDone.getCount() != 0) {
                try { retirementDone.await(); }
                catch (InterruptedException ignored) { interrupted = true; }
            }
            if (interrupted) current.interrupt();
        }
        // Same-thread callback reentry cannot join itself; the outer owner is
        // still responsible for its finally countdown and failure diagnostics.
    }

    private void hardCloseSocket() {
        // Hard-close on deadlines/protocol errors; do not wait for a peer's TLS close_notify.
        try { socket.setSoLinger(true, 0); } catch (IOException ignored) {}
        // JSSE close() may still perform a duplex TLS read even with zero TCP linger.
        // Shutdown inbound first. JSSE can report missing close_notify here while
        // nevertheless closing inbound; that truncation is intentional on this path.
        try { socket.shutdownInput(); } catch (IOException ignored) {}
        try { socket.close(); } catch (IOException ignored) {}
    }

    private ModernTransportDeadline deadline(int milliseconds, ModernSessionFailures.Stage stage) {
        return new ModernTransportDeadline(milliseconds, System::nanoTime, SCHEDULER, () -> {
            // Record the cause before abort wakes the blocked I/O with a generic
            // SocketException. Never retain raw exception text or frame data.
            noteFailure(stage, new SocketTimeoutException("modern transport deadline exceeded"));
            closeSocketOnly();
        }, () -> state.get() == State.CLOSED);
    }

    private static ScheduledThreadPoolExecutor deadlines() {
        var executor = new ScheduledThreadPoolExecutor(1, task -> {
            Thread thread = new Thread(task, "modern-tls-deadline");
            thread.setDaemon(true);
            return thread;
        });
        executor.setRemoveOnCancelPolicy(true);
        return executor;
    }
}
