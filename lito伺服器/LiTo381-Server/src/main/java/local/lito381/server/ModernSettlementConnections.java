package local.lito381.server;

import com.mysql.cj.conf.PropertySet;
import com.mysql.cj.protocol.StandardSocketFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dedicated modern settlement writer connections, not a source DAO or a pool.
 * The caller already owns a bounded settlement worker; this class owns only
 * bounded physical-connection admission, deadline timers and JDBC abort helpers.
 * A deadline closes raw TCP even during Driver.connect or JDBC commit/QUIT.
 * A cancelled task remains counted until its actual connect/close/abort retires.
 * Secrets are never logged. Connector/J necessarily retains immutable String
 * copies; clearing our char[]/Properties does not claim to erase those copies.
 */
public final class ModernSettlementConnections implements ModernMatchSettlementJdbc.ConnectionFactory, AutoCloseable {
    @FunctionalInterface interface Connector { Connection connect(String url, Properties properties) throws SQLException; }
    private static final int MAX_TRUST_BYTES = 1024 * 1024;
    private static final AtomicInteger IDS = new AtomicInteger();
    private static final ThreadLocal<Entry> ATTEMPT = new ThreadLocal<>();
    private final ModernServerConfig.DbConfig config;
    private final int connectMillis, networkMillis, capacity;
    private final Connector connector;
    private final Object gate = new Object();
    private final Set<Entry> active = new HashSet<>();
    private final ScheduledThreadPoolExecutor deadlines;
    private final ThreadPoolExecutor aborts;
    private final ModernBackendScope.AbortScheduler abortScheduler;
    private boolean closed;

    ModernSettlementConnections(ModernServerConfig.DbConfig config, int connectMillis, int networkMillis, int capacity) throws SQLException {
        this(config, connectMillis, networkMillis, capacity, (url, properties) -> new com.mysql.cj.jdbc.Driver().connect(url, properties));
    }
    /** Synthetic JDBC tests only; production always uses the actual Connector/J. */
    ModernSettlementConnections(ModernServerConfig.DbConfig config, int connectMillis, int networkMillis, int capacity, Connector connector) throws SQLException {
        this.config = Objects.requireNonNull(config); this.connector = Objects.requireNonNull(connector);
        if (connectMillis < 100 || connectMillis > 30_000 || networkMillis < 100 || networkMillis > 30_000
                || capacity < 1 || capacity > 32) throw new IllegalArgumentException("invalid settlement connection limits");
        this.connectMillis = connectMillis; this.networkMillis = networkMillis; this.capacity = capacity;
        if ("VERIFY_IDENTITY".equals(config.sslMode())) validateTrustStore(config);
        else if (!"DISABLED".equals(config.sslMode()) || config.trustStorePath() != null) throw unavailable();
        String prefix = "modern-settlement-jdbc-" + IDS.incrementAndGet();
        aborts = new ThreadPoolExecutor(Math.min(2, capacity), Math.min(2, capacity), 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(capacity), task -> new Thread(task, prefix + "-abort"));
        deadlines = new ScheduledThreadPoolExecutor(1, task -> new Thread(task, prefix + "-deadline"));
        deadlines.setRemoveOnCancelPolicy(true);
        deadlines.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        abortScheduler = new ModernBackendScope.AbortScheduler() {
            @Override public boolean execute(Runnable action) {
                try { aborts.execute(action); return true; }
                catch (RejectedExecutionException full) { return false; }
            }
            @Override public void remove(Runnable action) { aborts.remove(action); }
        };
    }

    @Override public Connection open(long absoluteDeadlineNanos) throws Exception {
        long remaining = absoluteDeadlineNanos - System.nanoTime();
        if (remaining <= 0 || remaining > ModernMatchSettlementJdbc.MAX_DEADLINE_NANOS
                || Thread.currentThread().isInterrupted() || ATTEMPT.get() != null) throw unavailable();
        Entry entry;
        synchronized (gate) {
            if (closed || active.size() >= capacity) throw unavailable();
            entry = new Entry(absoluteDeadlineNanos); active.add(entry);
            // The registration and timer submission cannot race helper shutdown.
            entry.timer = deadlines.schedule(() -> entry.scope.cancel(ModernBackendExecutor.Failure.DEADLINE),
                    Math.max(0, absoluteDeadlineNanos - System.nanoTime()), TimeUnit.NANOSECONDS);
        }
        Properties properties = new Properties(); boolean transferred = false;
        try {
            entry.scope.checkActive();
            configure(properties, entry.scope);
            ATTEMPT.set(entry);
            try { entry.connection = connector.connect(jdbcUrl(), properties); }
            finally { ATTEMPT.remove(); }
            if (entry.connection == null) throw unavailable();
            // Production must have used our pre-connect socket factory, not a
            // connection with an untracked physical transport or driver fallback.
            if (entry.socket == null) throw unavailable();
            entry.lease = entry.scope.register(entry.connection);
            entry.scope.checkActive();
            entry.connection.setNetworkTimeout(Runnable::run, Math.min(networkMillis, entry.scope.remainingMillis()));
            if (entry.connection.isClosed() || !entry.connection.getAutoCommit()) throw unavailable();
            entry.scope.checkActive();
            Connection proxy = entry.proxy();
            synchronized (gate) {
                if (closed) throw unavailable();
                entry.scope.checkActive();
                transferred = true; return proxy;
            }
        } catch (Exception failure) { throw unavailable(); }
        finally {
            properties.clear();
            if (!transferred) entry.closeOwned();
        }
    }

    int activeCount() { synchronized (gate) { return active.size(); } }
    boolean isQuiescent() {
        synchronized (gate) {
            return active.isEmpty() && aborts.getActiveCount() == 0 && aborts.getQueue().isEmpty()
                    && deadlines.getActiveCount() == 0 && deadlines.getQueue().isEmpty()
                    && (!closed || aborts.isTerminated() && deadlines.isTerminated());
        }
    }
    @Override public void close() {
        List<Entry> owned;
        synchronized (gate) { if (closed) return; closed = true; owned = List.copyOf(active); }
        // No external driver callback occurs while holding admission's monitor.
        for (Entry entry : owned) entry.scope.cancel(ModernBackendExecutor.Failure.CLOSED);
        synchronized (gate) { stopHelpersIfRetired(); }
    }
    boolean awaitTermination(long timeoutMillis) throws InterruptedException {
        if (timeoutMillis < 0 || timeoutMillis > 300_000) throw new IllegalArgumentException("invalid settlement shutdown deadline");
        long end = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        synchronized (gate) {
            if (!closed) return false;
            while (!active.isEmpty()) {
                long remaining = end - System.nanoTime(); if (remaining <= 0) return false;
                TimeUnit.NANOSECONDS.timedWait(gate, remaining);
            }
            stopHelpersIfRetired();
        }
        if (!aborts.awaitTermination(Math.max(0, end - System.nanoTime()), TimeUnit.NANOSECONDS)) return false;
        return deadlines.awaitTermination(Math.max(0, end - System.nanoTime()), TimeUnit.NANOSECONDS);
    }
    private void stopHelpersIfRetired() {
        if (closed && active.isEmpty()) { aborts.shutdown(); deadlines.shutdown(); }
    }

    /** Driver-instantiable, only inside this connection owner's single connect attempt. */
    public static final class ScopedSocketFactory extends StandardSocketFactory {
        public ScopedSocketFactory() {}
        @Override protected Socket createSocket(PropertySet properties) {
            Entry entry = ATTEMPT.get();
            if (entry == null) throw new IllegalStateException("settlement socket outside owned attempt");
            return entry.createSocket();
        }
        // Inherit Connector/J's actual TLS handshake, chain and host verification.
    }

    private final class Entry {
        final ModernBackendScope scope;
        final AtomicBoolean closing = new AtomicBoolean();
        final CountDownLatch retired = new CountDownLatch(1);
        ScheduledFuture<?> timer;
        Socket socket;
        ModernBackendScope.SocketLease socketLease;
        Connection connection;
        ModernBackendScope.ConnectionLease lease;
        volatile boolean closeFailed;
        Entry(long deadline) { scope = new ModernBackendScope(deadline, abortScheduler); }
        Socket createSocket() {
            if (socket != null) throw new IllegalStateException("settlement reconnect is forbidden");
            Socket created = new Socket();
            try { socketLease = scope.registerSocket(created); socket = created; return created; }
            catch (Exception failure) {
                try { created.close(); } catch (IOException ignored) {}
                throw new IllegalStateException("settlement socket unavailable");
            }
        }
        Connection proxy() {
            return (Connection) Proxy.newProxyInstance(ModernSettlementConnections.class.getClassLoader(), new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                String name = method.getName();
                if (name.equals("toString")) return "ModernSettlementConnection[redacted]";
                if (name.equals("hashCode")) return System.identityHashCode(proxy);
                if (name.equals("equals")) return proxy == args[0];
                if (name.equals("close")) { closeOwned(); return null; }
                if (name.equals("isClosed") && closing.get()) return true;
                if (name.equals("isWrapperFor")) return false;
                if (name.equals("unwrap")) throw unavailable(); // Never release the owned raw connection.
                if (closing.get()) throw unavailable();
                try {
                    scope.checkActive();
                    if (name.equals("setNetworkTimeout")) {
                        args = new Object[]{(java.util.concurrent.Executor) Runnable::run, Math.min(networkMillis, scope.remainingMillis())};
                    }
                    return method.invoke(connection, args);
                } catch (InvocationTargetException failure) { throw unavailable(); }
                catch (Exception failure) { throw unavailable(); }
            });
        }
        void closeOwned() throws SQLException {
            if (!closing.compareAndSet(false, true)) {
                boolean interrupted = false;
                for (;;) try { retired.await(); break; } catch (InterruptedException ignored) { interrupted = true; }
                if (interrupted) Thread.currentThread().interrupt();
                if (closeFailed) throw unavailable();
                return;
            }
            try {
                // If an abort already started, retiring this lease joins that
                // actual abort. The raw socket deadline stays armed during QUIT.
                if (lease != null) lease.close();
                if (connection != null) connection.close();
            } catch (Exception failure) { closeFailed = true; }
            finally {
                try { if (socket != null) socket.close(); }
                catch (IOException failure) { closeFailed = true; }
                finally {
                    if (socketLease != null) socketLease.close();
                    scope.finish();
                    if (timer != null) timer.cancel(false);
                    synchronized (gate) { active.remove(this); stopHelpersIfRetired(); gate.notifyAll(); }
                    retired.countDown();
                }
            }
            if (closeFailed) throw unavailable();
        }
    }

    private String jdbcUrl() {
        String host = config.host().contains(":") ? "[" + config.host() + "]" : config.host();
        return "jdbc:mysql://" + host + ":" + config.port() + "/" + config.database();
    }
    private void configure(Properties properties, ModernBackendScope scope) throws Exception {
        char[] password = null, trust = null;
        try {
            password = config.password();
            properties.setProperty("user", config.username()); properties.setProperty("password", new String(password));
            properties.setProperty("connectTimeout", Integer.toString(Math.min(connectMillis, scope.remainingMillis())));
            properties.setProperty("socketTimeout", Integer.toString(Math.min(networkMillis, scope.remainingMillis())));
            properties.setProperty("socketFactory", ScopedSocketFactory.class.getName());
            for (String key : List.of("autoReconnect", "allowMultiQueries", "allowLoadLocalInfile", "allowUrlInLocalInfile",
                    "allowPublicKeyRetrieval", "autoDeserialize", "useCompression", "useServerPrepStmts", "profileSQL",
                    "traceProtocol", "enablePacketDebug", "fallbackToSystemTrustStore", "fallbackToSystemKeyStore")) properties.setProperty(key, "false");
            properties.setProperty("characterEncoding", "UTF-8");
            properties.setProperty("queryTimeoutKillsConnection", "true");
            properties.setProperty("logger", "com.mysql.cj.log.NullLogger");
            properties.setProperty("sslMode", config.sslMode()); properties.setProperty("tlsVersions", "TLSv1.2,TLSv1.3");
            if ("VERIFY_IDENTITY".equals(config.sslMode())) {
                trust = config.trustStorePassword();
                properties.setProperty("trustCertificateKeyStoreUrl", config.trustStorePath().toUri().toASCIIString());
                properties.setProperty("trustCertificateKeyStoreType", "PKCS12");
                properties.setProperty("trustCertificateKeyStorePassword", new String(trust));
            }
        } finally {
            if (password != null) Arrays.fill(password, '\0'); if (trust != null) Arrays.fill(trust, '\0');
        }
    }
    private static void validateTrustStore(ModernServerConfig.DbConfig config) throws SQLException {
        char[] password = null; byte[] bytes = null;
        try {
            var path = ModernServerConfig.checkedLocalPath(Objects.requireNonNull(config.trustStorePath()), true);
            password = Objects.requireNonNull(config.trustStorePassword());
            try (var input = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) { bytes = input.readNBytes(MAX_TRUST_BYTES + 1); }
            if (bytes.length == 0 || bytes.length > MAX_TRUST_BYTES) throw unavailable();
            var store = KeyStore.getInstance("PKCS12"); store.load(new ByteArrayInputStream(bytes), password);
            if (store.size() == 0 || store.size() > 64) throw unavailable();
            for (var aliases = store.aliases(); aliases.hasMoreElements();) {
                String alias = aliases.nextElement();
                if (!store.isCertificateEntry(alias) || !(store.getCertificate(alias) instanceof X509Certificate certificate)
                        || certificate.getBasicConstraints() < 0) throw unavailable();
                certificate.checkValidity();
            }
        } catch (Exception failure) { throw unavailable(); }
        finally { if (password != null) Arrays.fill(password, '\0'); if (bytes != null) Arrays.fill(bytes, (byte) 0); }
    }
    private static SQLException unavailable() { return new SQLException("modern settlement connection unavailable"); }
    @Override public String toString() { return "ModernSettlementConnections[redacted]"; }
}
