package local.lito381.server;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Opt-in, TEST CLASSPATH ONLY: production TLS/session/navigation loop with the
 * modern read-only JDBC adapters. Never starts native Main, opens Accounts,
 * runs health checks/migrations, or issues SQL writes. Test credentials arrive
 * only in AUTH over the verified TLS connection; this peer never auto-authenticates
 * or reads the test account/password from its own CLI/environment.
 *
 * Required env: LITO_MODERN_REAL_DB_SERVER_CONFIG (absolute native server.properties).
 * Usage: serve <absolute prepared TLS fixture directory> readonly <1000..30000 ms>.
 */
public final class ModernDatabaseNavigationTestPeer {
    private static final int CLEANUP_MILLIS = 3000;
    private static final String CONFIG_ENV = "LITO_MODERN_REAL_DB_SERVER_CONFIG";
    private ModernDatabaseNavigationTestPeer() {}

    private enum Failure {
        ARGUMENTS, ENVIRONMENT, CONFIGURATION, TLS_IDENTITY, DATABASE_BINDINGS,
        LISTENER_START, LISTENER_FAILED, LIFETIME_EXPIRED, CONNECTION_COUNT,
        SESSION_FAILED, AUTHENTICATION_NOT_EXERCISED, SOURCE_ROSTERS_NOT_EXERCISED, SOURCE_DATA_NOT_EXERCISED,
        SCHEMA_ROUTING_NOT_EXERCISED, INTERRUPTED, CLEANUP_FAILED
    }
    private static final class PeerFailure extends Exception {
        final Failure reason;
        PeerFailure(Failure reason) { super("read-only database peer failed"); this.reason = reason; }
    }

    public static void main(String[] arguments) {
        PrintWriter output = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
        PrintWriter errors = new PrintWriter(new OutputStreamWriter(System.err, StandardCharsets.UTF_8), true);
        ModernDatabaseReadProbe.Settings settings = null;
        ModernServerConfig config = null;
        ModernServer server = null;
        TournamentPresence presence = new TournamentPresence();
        Failure stage = Failure.ARGUMENTS;
        Failure failure = null;
        try {
            if (arguments.length != 4 || !"serve".equals(arguments[0]) || !"readonly".equals(arguments[2])
                    || !arguments[3].matches("[1-9][0-9]{3,4}")) throw new PeerFailure(Failure.ARGUMENTS);
            int lifetimeMillis = Integer.parseInt(arguments[3]);
            if (lifetimeMillis < 1000 || lifetimeMillis > 30_000) throw new PeerFailure(Failure.ARGUMENTS);
            Path fixtures = Path.of(arguments[1]);
            if (!fixtures.isAbsolute()) throw new PeerFailure(Failure.ARGUMENTS);

            stage = Failure.ENVIRONMENT;
            String configured = System.getenv(CONFIG_ENV);
            if (configured == null || configured.isBlank()) throw new PeerFailure(Failure.ENVIRONMENT);
            stage = Failure.CONFIGURATION;
            // Strict external sources, bounded UTF-8, no missing-file/primary
            // fallback, numeric loopback only (exact localhost is normalized
            // without DNS). Legacy URL options are never given to the driver.
            settings = ModernDatabaseReadProbe.load(Path.of(configured));
            config = ModernDatabaseReadProbe.modernConfig(settings);
            // lito_mysql uses the explicitly configured static catalog, not
            // either account schema and not a guessed source resource fallback.
            String catalogDatabase = ModernCharacterDataReadProbe.catalogDatabase(Path.of(configured));

            stage = Failure.TLS_IDENTITY;
            // The probe model's unused listener identity MUST NOT be opened.
            // Only the existing, test-owned fixture certificate is used here.
            var tls = ModernTlsIdentity.load(fixtures.resolve("server.p12"),
                    "modern-lito-fixture-only".toCharArray());

            stage = Failure.DATABASE_BINDINGS;
            var repositories = new TreeMap<Integer, ModernJdbcBackend>();
            var dataSources = new TreeMap<Integer, ModernCharacterDataRepository.Source>();
            var bindings = new TreeMap<Integer, ModernAccountAuthentication.SourceBinding>();
            AtomicInteger successfulRoutingChecks = new AtomicInteger();
            for (var source : config.sources()) {
                String expectedDatabase = source.database().database();
                int networkMillis = config.dbNetworkMillis();
                ModernJdbcBackend.Connector connector = (url, properties) -> {
                    Connection connection = new com.mysql.cj.jdbc.Driver().connect(url, properties);
                    if (connection == null) throw routingUnavailable();
                    return inspectDatabaseOwned(connection, ModernBackendScope.current(), expectedDatabase,
                            networkMillis, successfulRoutingChecks);
                };
                var repository = new ModernJdbcBackend(source.database(), config.dbConnectMillis(), networkMillis, connector);
                repositories.put(source.id(), repository);
                dataSources.put(source.id(), new ModernCharacterDataRepository.Source(repository, catalogDatabase));
                bindings.put(source.id(), ModernAccountAuthentication.SourceBinding.direct(source.name(), repository::authenticate));
            }
            // Same concrete modern factory as production: each source verifies
            // independently and keeps its own canonical account. No native
            // GameHub/global moderation store is silently imported by this peer.
            var actualVerifier = ModernAccountAuthentication.repositories(Map.copyOf(bindings), account -> false);
            var actualCharacters = ModernJdbcBackend.characters(Map.copyOf(repositories));
            var actualCharacterData = ModernCharacterDataRepository.jdbc(Map.copyOf(dataSources));
            AtomicInteger authenticated = new AtomicInteger();
            Set<Integer> queriedSources = ConcurrentHashMap.newKeySet();
            Set<Integer> queriedDataSources = ConcurrentHashMap.newKeySet();
            ModernAccountAuthentication.CredentialVerifier verifier = (account, password) -> {
                var principal = actualVerifier.verify(account, password);
                authenticated.incrementAndGet();
                return principal;
            };
            ModernCharacterRepository characters = (principal, sourceId) -> {
                var rows = actualCharacters.characters(principal, sourceId);
                queriedSources.add(sourceId);
                return rows;
            };
            ModernCharacterDataRepository characterData = (principal, sourceId, objectId) -> {
                var snapshot = actualCharacterData.character(principal, sourceId, objectId);
                if (snapshot.isPresent()) queriedDataSources.add(sourceId);
                return snapshot;
            };

            stage = Failure.LISTENER_START;
            // One token refills only after 60 s, longer than this peer's maximum
            // lifetime. A second accepted connection cannot silently replace
            // the first. All AUTH/source queries share each operation's 5 s scope.
            var limits = new ModernServer.Limits(1, 1, 1, 1, 1, 1,
                    config.backendDeadlineMillis(), 2000, 2000, 10_000);
            server = new ModernServer(new InetSocketAddress(
                    InetAddress.getByAddress(new byte[] {127, 0, 0, 1}), 0), tls, limits, verifier, characters, presence, characterData);
            output.println("READY " + server.address().getPort());
            output.flush();

            stage = Failure.SESSION_FAILED;
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(lifetimeMillis);
            for (;;) {
                var state = server.snapshot();
                if (server.listenerFailed()) throw new PeerFailure(Failure.LISTENER_FAILED);
                if (state.accepted() > 1 || state.rejected() != 0) throw new PeerFailure(Failure.CONNECTION_COUNT);
                if (System.nanoTime() - deadline >= 0) throw new PeerFailure(Failure.LIFETIME_EXPIRED);
                if (state.accepted() == 1 && state.sessions() == 0) {
                    // A server deadline/protocol failure is not successful EOF.
                    // The Rust caller separately requires explicit TLS close()
                    // success; this counter is not proof of received close_notify.
                    if (state.failed() != 0) throw new PeerFailure(Failure.SESSION_FAILED);
                    if (authenticated.get() != 1) throw new PeerFailure(Failure.AUTHENTICATION_NOT_EXERCISED);
                    if (!queriedSources.equals(repositories.keySet())) throw new PeerFailure(Failure.SOURCE_ROSTERS_NOT_EXERCISED);
                    if (!queriedDataSources.equals(repositories.keySet())) throw new PeerFailure(Failure.SOURCE_DATA_NOT_EXERCISED);
                    if (successfulRoutingChecks.get() == 0) throw new PeerFailure(Failure.SCHEMA_ROUTING_NOT_EXERCISED);
                    break;
                }
                Thread.sleep(10);
            }
        } catch (PeerFailure failed) {
            failure = failed.reason;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            failure = Failure.INTERRUPTED;
        } catch (Throwable failed) {
            // Driver/config/provider exceptions may include SQL, URLs or
            // secrets. Never print their messages, causes or suppressed values.
            failure = stage;
        } finally {
            boolean workersRetired = server == null;
            if (server != null) {
                try {
                    workersRetired = stopAndJoin(server);
                    var end = server.snapshot();
                    if (!workersRetired || end.sessions() != 0 || end.backendActive() != 0 || end.backendQueued() != 0
                            || presence.ownerCount() != 0 || presence.userCount() != 0) failure = Failure.CLEANUP_FAILED;
                } catch (Throwable cleanupFailed) {
                    failure = Failure.CLEANUP_FAILED;
                }
            }
            // Each DAO holds this config, not a copied secret lifetime. Never
            // erase its secrets while an owned worker can still read them. On a
            // failed retirement, the test process exits nonzero below with the
            // owners retained; it never reports successful cleanup or hangs in
            // an unbounded join. No unrelated process is terminated.
            if (workersRetired) {
                try {
                    try { if (config != null) config.close(); }
                    finally { if (settings != null) settings.close(); }
                } catch (Throwable cleanupFailed) {
                    failure = Failure.CLEANUP_FAILED;
                }
            }
        }
        if (failure != null) {
            errors.println("MODERN_DATABASE_NAVIGATION_PEER_FAILED " + failure.name());
            errors.flush();
            System.exit(2);
        }
    }

    /** One absolute cleanup budget, including interrupted waits; no detached worker. */
    private static boolean stopAndJoin(ModernServer server) {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(CLEANUP_MILLIS);
        server.close();
        boolean interrupted = false;
        try {
            for (;;) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) return false;
                try {
                    return server.awaitTermination(Math.max(1, (remaining + 999_999) / 1_000_000));
                } catch (InterruptedException waitInterrupted) { interrupted = true; }
            }
        } finally {
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    /**
     * Every physical connection proves its actual schema, even when two source
     * databases happen to contain identical characters. The DAO Attempt already
     * owns the raw SocketLease across connect, this check and normal DAO use.
     * On success this temporary JDBC lease is retired before ownership transfers
     * to the DAO's lease. On failure it is retired before closing the connection.
     */
    private static Connection inspectDatabaseOwned(Connection connection, ModernBackendScope scope,
            String expectedDatabase, int networkMillis, AtomicInteger successfulChecks) throws SQLException {
        boolean transferred = false;
        try {
            try (var lease = scope.register(connection)) {
                scope.checkActive();
                connection.setNetworkTimeout(Runnable::run, Math.min(networkMillis, scope.remainingMillis()));
                connection.setReadOnly(true);
                try (PreparedStatement statement = prepareDatabaseCheck(connection, scope);
                     ResultSet rows = statement.executeQuery()) {
                    if (!rows.next() || !expectedDatabase.equals(rows.getString(1)) || rows.next()) throw routingUnavailable();
                }
                scope.checkActive();
            }
            scope.checkActive();
            successfulChecks.incrementAndGet();
            transferred = true;
            return connection;
        } catch (Exception failure) {
            // No selected schema, original SQLException or SQL URL is exposed.
            throw routingUnavailable();
        } finally {
            if (!transferred) try { connection.close(); } catch (SQLException | RuntimeException ignored) {}
        }
    }

    private static PreparedStatement prepareDatabaseCheck(Connection connection, ModernBackendScope scope) throws Exception {
        scope.checkActive();
        PreparedStatement statement = connection.prepareStatement("SELECT DATABASE()");
        try {
            statement.setMaxRows(2);
            statement.setQueryTimeout(Math.max(1, (scope.remainingMillis() + 999) / 1000));
            return statement;
        } catch (Exception failure) {
            try { statement.close(); } catch (SQLException ignored) {}
            throw failure;
        }
    }

    private static SQLException routingUnavailable() { return new SQLException("read-only schema routing verification failed"); }
}
