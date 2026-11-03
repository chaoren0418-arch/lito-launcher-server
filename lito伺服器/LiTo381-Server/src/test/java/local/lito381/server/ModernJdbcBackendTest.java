package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

class ModernJdbcBackendTest {
    @TempDir Path temporary;
    private static final String DB_PASSWORD = "synthetic-database-secret";
    private static final String TRUST_PASSWORD = "synthetic-trust-secret";

    private ModernServerConfig config(String host, int port, Path trust) throws Exception {
        String text = "schema=1\nbind.port=0\ntls.pkcs12=" + portable(temporary.resolve("listener.p12"))
                + "\ntls.password.env=LISTENER\nsources=7\nsource.7.name=Sun\n"
                + "source.7.mysql.host=" + host + "\nsource.7.mysql.port=" + port
                + "\nsource.7.mysql.database=modern_fixture\nsource.7.mysql.username.env=DB_USER\n"
                + "source.7.mysql.password.env=DB_PASSWORD\nsource.7.mysql.passwordScheme=plain\n"
                + "source.7.mysql.tls=" + (trust == null ? "disabled" : "verify_identity") + "\n"
                + (trust == null ? "" : "source.7.mysql.trustStore.pkcs12=" + portable(trust)
                + "\nsource.7.mysql.trustStore.password.env=TRUST_PASSWORD\n");
        return ModernServerConfig.parse(text.getBytes(StandardCharsets.UTF_8), key -> switch (key) {
            case "DB_USER" -> "synthetic-readonly-user";
            case "DB_PASSWORD" -> DB_PASSWORD;
            case "TRUST_PASSWORD" -> TRUST_PASSWORD;
            case "LISTENER" -> "synthetic-listener-secret";
            default -> null;
        });
    }
    private static String portable(Path path) { return path.toAbsolutePath().toString().replace('\\', '/'); }

    private static <T> T scoped(Callable<T> work) throws Exception {
        var scope = new ModernBackendScope(System.nanoTime() + TimeUnit.SECONDS.toNanos(10),
                new ModernBackendScope.AbortScheduler() {
                    public boolean execute(Runnable action) { action.run(); return true; }
                    public void remove(Runnable action) {}
                });
        ModernBackendScope.bind(scope);
        try { return work.call(); }
        finally { scope.finish(); ModernBackendScope.unbind(); }
    }

    @Test void exactPlainAuthenticationIsReadOnlyBoundedAndClearsDriverProperties() throws Exception {
        try (var config = config("127.0.0.1", 3306, null)) {
            Jdbc fixture = new Jdbc(List.of(account("sun-owner", " raw password ", false)));
            var dao = new ModernJdbcBackend(config.sources().get(0).database(), 1000, 3000, fixture);
            AuthResult accepted = scoped(() -> dao.authenticate("alias", " raw password "));
            assertTrue(accepted.accepted());
            assertEquals("sun-owner", accepted.account());
            assertFalse(scoped(() -> dao.authenticate("alias", "raw password")).accepted(), "must not trim passwords");
            assertEquals("alias", fixture.owner);
            assertEquals(ModernJdbcBackend.AUTH_SQL, fixture.sql);
            assertEquals(2, fixture.maxRows);
            assertTrue(fixture.queryTimeoutSeconds >= 1 && fixture.queryTimeoutSeconds <= 10);
            assertTrue(fixture.readOnly);
            assertTrue(fixture.networkMillis > 0 && fixture.networkMillis <= 3000);
            assertTrue(fixture.receivedProperties.isEmpty(), "owned Properties must be cleared after each connection");
            assertEquals("jdbc:mysql://127.0.0.1:3306/modern_fixture", fixture.url);
            assertEquals("1000", fixture.snapshot.getProperty("connectTimeout"));
            assertEquals("3000", fixture.snapshot.getProperty("socketTimeout"));
            for (String key : List.of("autoReconnect", "allowMultiQueries", "allowLoadLocalInfile", "allowUrlInLocalInfile",
                    "allowPublicKeyRetrieval", "autoDeserialize", "useCompression", "traceProtocol", "enablePacketDebug",
                    "fallbackToSystemTrustStore", "fallbackToSystemKeyStore")) {
                assertEquals("false", fixture.snapshot.getProperty(key), key);
            }
            assertEquals("true", fixture.snapshot.getProperty("queryTimeoutKillsConnection"));
            assertEquals("DISABLED", fixture.snapshot.getProperty("sslMode"));
            assertEquals(ModernJdbcBackend.ScopedSocketFactory.class.getName(), fixture.snapshot.getProperty("socketFactory"));
            assertEquals("com.mysql.cj.log.NullLogger", fixture.snapshot.getProperty("logger"));
            assertFalse(dao.toString().contains(DB_PASSWORD));
            assertEquals(List.of("result-close", "statement-close", "connection-close",
                    "result-close", "statement-close", "connection-close"), fixture.events);
        }
    }

    @Test void bansNullSecretsAndOverlongStoredSecretsCannotAuthenticate() throws Exception {
        try (var config = config("127.0.0.1", 3306, null)) {
            for (var row : List.of(account("owner", "p", true), account("owner", null, false),
                    account("owner", "x".repeat(257), false))) {
                Jdbc fixture = new Jdbc(List.of(row));
                var dao = new ModernJdbcBackend(config.sources().get(0).database(), 1000, 3000, fixture);
                assertFalse(scoped(() -> dao.authenticate("a", "p")).accepted());
            }
            Jdbc whitespace = new Jdbc(List.of(account("owner", "   ", false)));
            var dao = new ModernJdbcBackend(config.sources().get(0).database(), 1000, 3000, whitespace);
            assertTrue(scoped(() -> dao.authenticate("a", "   ")).accepted());
        }
    }

    @Test void duplicateAccountsAndInvalidCanonicalFailWithoutLeakingDriverSecrets() throws Exception {
        try (var config = config("127.0.0.1", 3306, null)) {
            for (var rows : List.of(List.of(account("a", "p", false), account("b", "p", false)),
                    List.of(account("界".repeat(22), "p", false)), List.of(account("\ud800", "p", false)))) {
                var dao = new ModernJdbcBackend(config.sources().get(0).database(), 1000, 3000, new Jdbc(rows));
                assertThrows(SQLException.class, () -> scoped(() -> dao.authenticate("a", "p")));
            }
            var dao = new ModernJdbcBackend(config.sources().get(0).database(), 1000, 3000,
                    (url, props) -> { throw new SQLException(url + DB_PASSWORD, new Exception(DB_PASSWORD)); });
            var error = assertThrows(SQLException.class, () -> scoped(() -> dao.authenticate("a", "p")));
            assertEquals("modern JDBC backend unavailable", error.getMessage());
            assertNull(error.getCause());
            assertEquals(0, error.getSuppressed().length);
            assertThrows(IllegalStateException.class, () -> dao.authenticate("a", "p"));
        }
    }

    @Test void authenticationAcceptsIntegralDecimalsAndExactTinyintFlagsWhilePreservingNullSecrets() throws Exception {
        try (var config = config("127.0.0.1", 3306, null)) {
            for (int type : new int[] {Types.BIGINT, Types.DECIMAL, Types.NUMERIC, Types.BIT, Types.BOOLEAN}) {
                for (int banned : new int[] {0, 1}) {
                    var row = account("owner", "p", false);
                    row.put("login_bytes", new BigDecimal("5.000"));
                    row.put("password_bytes", new BigDecimal("1.000"));
                    row.put("banned", BigDecimal.valueOf(banned).setScale(2));
                    Jdbc fixture = new Jdbc(List.of(row));
                    fixture.numericTypes.put("banned", type);
                    var dao = new ModernJdbcBackend(config.sources().get(0).database(), 1000, 3000, fixture);
                    AuthResult result = scoped(() -> dao.authenticate("alias", "p"));
                    assertEquals(banned == 0, result.accepted());
                    assertEquals(banned == 1, result.banned());
                    assertEquals("owner", result.account());
                    assertReadRetired(fixture);
                }
            }
            // A nullable password length remains an ordinary authentication rejection,
            // not a zero-length secret or a backend failure.
            for (String stored : new String[] {null, "p"}) {
                var row = account("owner", stored, false);
                row.put("password_bytes", null);
                Jdbc fixture = new Jdbc(List.of(row));
                var dao = new ModernJdbcBackend(config.sources().get(0).database(), 1000, 3000, fixture);
                AuthResult result = scoped(() -> dao.authenticate("alias", "p"));
                assertFalse(result.accepted());
                assertFalse(result.banned());
                assertReadRetired(fixture);
            }
        }
    }

    @Test void authenticationRejectsFractionalOverflowApproximateTextAndNonBooleanBanValues() throws Exception {
        try (var config = config("127.0.0.1", 3306, null)) {
            for (String column : List.of("login_bytes", "password_bytes", "banned")) {
                for (Object value : List.of(new BigDecimal("1.5"), new BigInteger("18446744073709551615"),
                        Float.valueOf(1), Double.valueOf(1), "1")) {
                    var row = account("owner", "p", false);
                    row.put(column, value);
                    Jdbc fixture = new Jdbc(List.of(row));
                    if (value instanceof Float) fixture.numericTypes.put(column, Types.FLOAT);
                    var dao = new ModernJdbcBackend(config.sources().get(0).database(), 1000, 3000, fixture);
                    assertReadFailure(fixture, () -> dao.authenticate("alias", "p"));
                    if (value instanceof Float || value instanceof Double || value instanceof String) {
                        assertFalse(fixture.numericReads.contains(column), "reject unsupported SQL type before materialization");
                    }
                }
            }
            for (String column : List.of("login_bytes", "banned")) {
                var row = account("owner", "p", false);
                row.put(column, null);
                Jdbc fixture = new Jdbc(List.of(row));
                var dao = new ModernJdbcBackend(config.sources().get(0).database(), 1000, 3000, fixture);
                assertReadFailure(fixture, () -> dao.authenticate("alias", "p"));
            }
            for (int type : new int[] {Types.BIGINT, Types.DECIMAL, Types.BIT, Types.BOOLEAN}) {
                for (long value : new long[] {-1, 2, 255}) {
                    var row = account("owner", "p", false);
                    row.put("banned", BigDecimal.valueOf(value));
                    Jdbc fixture = new Jdbc(List.of(row));
                    fixture.numericTypes.put("banned", type);
                    var dao = new ModernJdbcBackend(config.sources().get(0).database(), 1000, 3000, fixture);
                    assertReadFailure(fixture, () -> dao.authenticate("alias", "p"));
                    assertTrue(fixture.numericReads.contains("banned"), "raw tinyint must be checked without boolean coercion");
                }
            }
        }
    }

    @Test void rosterRetainsI64LevelAndExactPerSourceCanonicalWithoutFallback() throws Exception {
        try (var config = config("127.0.0.1", 3306, null)) {
            Jdbc sun = new Jdbc(List.of(character(3333, "Sun Knight", 1, 0, 80)));
            Jdbc moon = new Jdbc(List.of(character(3333, "Moon Mage", 3, 1, 4_000_000_000L)));
            var sunDao = new ModernJdbcBackend(config.sources().get(0).database(), 1000, 3000, sun);
            var moonDao = new ModernJdbcBackend(config.sources().get(0).database(), 1000, 3000, moon);
            var principal = new ModernAccountAuthentication.Principal("sun-owner", Set.of(
                    new ModernAccountAuthentication.Source(7, "Sun"), new ModernAccountAuthentication.Source(42, "Moon")),
                    Map.of(7, "sun-owner", 42, "moon-owner"));
            var repository = ModernJdbcBackend.characters(Map.of(7, sunDao, 42, moonDao));
            assertEquals(80, scoped(() -> repository.characters(principal, 7)).get(0).level());
            assertEquals(4_000_000_000L, scoped(() -> repository.characters(principal, 42)).get(0).level());
            assertEquals("sun-owner", sun.owner);
            assertEquals("moon-owner", moon.owner);
            assertEquals(ModernJdbcBackend.CHARACTERS_SQL, moon.sql);
            assertEquals(65, moon.maxRows);
            assertThrows(SecurityException.class, () -> scoped(() -> repository.characters(principal, 99)));
            assertEquals(1, moon.connections);
            var missing = ModernJdbcBackend.characters(Map.of(7, sunDao));
            assertThrows(IllegalStateException.class, () -> scoped(() -> missing.characters(principal, 42)));
        }
    }

    @Test void rosterRejectsOverflowDuplicatesUnsortedNullAndInvalidTypedRows() throws Exception {
        try (var config = config("127.0.0.1", 3306, null)) {
            List<List<Map<String, Object>>> cases = new ArrayList<>();
            cases.add(java.util.stream.LongStream.rangeClosed(1, 65).mapToObj(id -> character(id, "n", 1, 0, 1)).toList());
            cases.add(List.of(character(2, "n", 1, 0, 1), character(2, "n", 1, 0, 1)));
            cases.add(List.of(character(2, "n", 1, 0, 1), character(1, "n", 1, 0, 1)));
            for (var row : List.of(character(0, "n", 1, 0, 1), character(1, "n", 65536, 0, 1),
                    character(1, "n", 1, 2, 1), character(1, "n", 1, 0, 0), character(1, "界".repeat(86), 1, 0, 1))) {
                cases.add(List.of(row));
            }
            Map<String, Object> nullLevel = character(1, "n", 1, 0, 1);
            nullLevel.put("level", null);
            cases.add(List.of(nullLevel));
            for (var rows : cases) {
                var dao = new ModernJdbcBackend(config.sources().get(0).database(), 1000, 3000, new Jdbc(rows));
                assertThrows(SQLException.class, () -> scoped(() -> dao.characters("owner")));
            }
        }
    }

    @Test void rosterKeepsExactIntegralDecimalI64AndExistingUnsignedClassContract() throws Exception {
        try (var config = config("127.0.0.1", 3306, null)) {
            var row = character(Long.MAX_VALUE, "n", 65_535, 1, Long.MAX_VALUE);
            for (String column : List.of("name_bytes", "character_type", "character_sex", "level")) {
                row.put(column, new BigDecimal(row.get(column).toString()).setScale(4));
            }
            row.put("objid", BigInteger.valueOf(Long.MAX_VALUE));
            Jdbc fixture = new Jdbc(List.of(row));
            var dao = new ModernJdbcBackend(config.sources().get(0).database(), 1000, 3000, fixture);
            assertEquals(List.of(new ModernApplicationProtocol.CharacterRow(Long.MAX_VALUE, "n", 65_535, 1, Long.MAX_VALUE)),
                    scoped(() -> dao.characters("owner")));
            assertReadRetired(fixture);
        }
    }

    @Test void rosterRejectsInexactOrCoercedNumericColumnsWithoutReturningPartialRows() throws Exception {
        try (var config = config("127.0.0.1", 3306, null)) {
            for (String column : List.of("objid", "name_bytes", "character_type", "character_sex", "level")) {
                for (Object value : new Object[] {null, new BigDecimal("1.5"), new BigInteger("18446744073709551615"),
                        new BigDecimal("-9223372036854775809"), Float.valueOf(1), Double.valueOf(1), "1"}) {
                    var row = character(2, "n", 1, 0, 1);
                    row.put(column, value);
                    Jdbc fixture = new Jdbc(List.of(character(1, "first", 1, 0, 1), row));
                    var dao = new ModernJdbcBackend(config.sources().get(0).database(), 1000, 3000, fixture);
                    assertReadFailure(fixture, () -> dao.characters("owner"));
                    if (value instanceof Float || value instanceof Double || value instanceof String) {
                        assertEquals(1, fixture.numericReads.stream().filter(column::equals).count(),
                                "unsupported second-row SQL value must not be materialized");
                    }
                }
            }
        }
    }

    @Test void concreteTypedAuthenticatorSupportsCanonicalModernFactory() throws Exception {
        try (var config = config("127.0.0.1", 3306, null)) {
            var dao = new ModernJdbcBackend(config.sources().get(0).database(), 1000, 3000,
                    new Jdbc(List.of(account("source-owner", "pw", false))));
            var verifier = ModernAccountAuthentication.repositories(Map.of(7,
                    ModernAccountAuthentication.SourceBinding.direct("Sun", dao::authenticate)), ignored -> false);
            char[] supplied = "pw".toCharArray();
            var principal = scoped(() -> ModernAccountAuthentication.verify(verifier, "alias", supplied));
            assertEquals("source-owner", principal.accountForSource(7));
            assertArrayEquals(new char[2], supplied);
        }
    }

    @Test void trustStoreIsValidatedAndVerifyIdentityPropertiesCannotDowngrade() throws Exception {
        Path material = ModernTlsTestMaterial.defaultDirectory();
        ModernTlsTestMaterial.prepare(material);
        var certificate = CertificateFactory.getInstance("X.509").generateCertificate(
                new ByteArrayInputStream(Files.readAllBytes(material.resolve("root.der"))));
        Path storePath = temporary.resolve("trusted roots.p12");
        KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(null, TRUST_PASSWORD.toCharArray());
        store.setCertificateEntry("root", certificate);
        try (var output = Files.newOutputStream(storePath)) { store.store(output, TRUST_PASSWORD.toCharArray()); }
        // Exercise the actual Connector/J verifier, not a look-alike test verifier.
        // This is still a TLS component test, not a complete MySQL/TLS server fixture.
        var certificateFactory = CertificateFactory.getInstance("X.509");
        var leaf = (X509Certificate) certificateFactory.generateCertificate(
                new ByteArrayInputStream(Files.readAllBytes(material.resolve("server.der"))));
        var expired = (X509Certificate) certificateFactory.generateCertificate(
                new ByteArrayInputStream(Files.readAllBytes(material.resolve("expired.der"))));
        TrustManagerFactory trustManagers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagers.init(store);
        X509TrustManager trust = java.util.Arrays.stream(trustManagers.getTrustManagers())
                .filter(X509TrustManager.class::isInstance).map(X509TrustManager.class::cast).findFirst().orElseThrow();
        var chain = new X509Certificate[] {leaf, (X509Certificate) certificate};
        new com.mysql.cj.protocol.ExportControlled.X509TrustManagerWrapper(trust, true, "127.0.0.1")
                .checkServerTrusted(chain, "RSA");
        assertThrows(CertificateException.class,
                () -> new com.mysql.cj.protocol.ExportControlled.X509TrustManagerWrapper(trust, true, "192.0.2.7")
                        .checkServerTrusted(chain, "RSA"));
        assertThrows(CertificateException.class,
                () -> new com.mysql.cj.protocol.ExportControlled.X509TrustManagerWrapper(trust, true, "127.0.0.1")
                        .checkServerTrusted(new X509Certificate[] {expired, (X509Certificate) certificate}, "RSA"));
        try (var config = config("192.0.2.7", 3306, storePath)) {
            Jdbc fixture = new Jdbc(List.of()); // Property contract only: no remote network access.
            var dao = new ModernJdbcBackend(config.sources().get(0).database(), 1000, 3000, fixture);
            assertFalse(scoped(() -> dao.authenticate("a", "p")).accepted());
            assertEquals("VERIFY_IDENTITY", fixture.snapshot.getProperty("sslMode"));
            assertEquals(storePath.toUri().toASCIIString(), fixture.snapshot.getProperty("trustCertificateKeyStoreUrl"));
            assertEquals("PKCS12", fixture.snapshot.getProperty("trustCertificateKeyStoreType"));
            assertEquals("TLSv1.2,TLSv1.3", fixture.snapshot.getProperty("tlsVersions"));
            assertEquals("false", fixture.snapshot.getProperty("fallbackToSystemTrustStore"));
        }
        Files.write(storePath, new byte[] {1, 2, 3});
        try (var config = config("127.0.0.1", 3306, storePath)) {
            assertThrows(SQLException.class, () -> new ModernJdbcBackend(config.sources().get(0).database(), 1000, 3000));
        }
        try (var config = config("127.0.0.1", 3306, temporary.resolve("missing.p12"))) {
            assertThrows(SQLException.class, () -> new ModernJdbcBackend(config.sources().get(0).database(), 1000, 3000));
        }
    }

    @Test void rawSocketRemainsArmedDuringConnectionCloseAfterJdbcLeaseRetirement() throws Exception {
        try (var config = config("127.0.0.1", 3306, null)) {
            Jdbc fixture = new Jdbc(List.of());
            fixture.registerSocket = true;
            fixture.cancelWhileClosing = true;
            var dao = new ModernJdbcBackend(config.sources().get(0).database(), 1000, 3000, fixture);
            var failure = assertThrows(ModernBackendExecutor.BackendException.class,
                    () -> scoped(() -> dao.authenticate("a", "p")));
            assertEquals(ModernBackendExecutor.Failure.CANCELLED, failure.failure());
            assertTrue(fixture.socket.isClosed());
            assertEquals(0, fixture.aborts, "retired JDBC lease must not abort the closing connection");
        }
    }

    @Test void realConnectorGreetingStallIsTerminatedByTotalDeadlineBeforeConnectionExists() throws Exception {
        try (ServerSocket peer = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
             var config = config("127.0.0.1", peer.getLocalPort(), null)) {
            peer.setSoTimeout(5000);
            var threads = Executors.newSingleThreadExecutor();
            var accepted = new CountDownLatch(1);
            var peerClosed = threads.submit(() -> {
                try (Socket socket = peer.accept()) {
                    accepted.countDown();
                    socket.setSoTimeout(5000);
                    return socket.getInputStream().read(); // No greeting; never accepts credentials or SQL.
                }
            });
            var executor = new ModernBackendExecutor(1, 0);
            try {
                var dao = new ModernJdbcBackend(config.sources().get(0).database(), 3000, 3000);
                long start = System.nanoTime();
                var error = assertThrows(ModernBackendExecutor.BackendException.class,
                        () -> executor.openSession().call(1200, scope -> dao.authenticate("fixture", "fixture")));
                assertEquals(ModernBackendExecutor.Failure.DEADLINE, error.failure());
                assertTrue(accepted.await(1, TimeUnit.SECONDS), "real Connector/J must reach our loopback peer");
                assertEquals(-1, peerClosed.get(3, TimeUnit.SECONDS));
                assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 3000);
            } finally {
                executor.close();
                assertTrue(executor.awaitTermination(3000), "cancelled driver connect must REALLY leave worker");
                threads.shutdownNow();
                assertTrue(threads.awaitTermination(5000, TimeUnit.MILLISECONDS));
            }
        }
    }

    @Test void realConnectorConnectCancellationClosesRawSocketAndRetiresCapacity() throws Exception {
        try (ServerSocket peer = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
             var config = config("127.0.0.1", peer.getLocalPort(), null)) {
            peer.setSoTimeout(5000);
            var threads = Executors.newFixedThreadPool(2);
            var accepted = new CountDownLatch(1);
            var executor = new ModernBackendExecutor(1, 0);
            var gate = executor.openSession();
            var peerClosed = threads.submit(() -> {
                try (Socket socket = peer.accept()) {
                    accepted.countDown();
                    socket.setSoTimeout(5000);
                    return socket.getInputStream().read();
                }
            });
            try {
                var dao = new ModernJdbcBackend(config.sources().get(0).database(), 3000, 3000);
                var call = threads.submit(() -> assertThrows(ModernBackendExecutor.BackendException.class,
                        () -> gate.call(5000, scope -> dao.authenticate("fixture", "fixture"))));
                assertTrue(accepted.await(3, TimeUnit.SECONDS));
                gate.close();
                assertEquals(ModernBackendExecutor.Failure.CANCELLED, call.get(3, TimeUnit.SECONDS).failure());
                assertEquals(-1, peerClosed.get(3, TimeUnit.SECONDS));
            } finally {
                executor.close();
                assertTrue(executor.awaitTermination(3000));
                assertTrue(executor.isQuiescent());
                threads.shutdownNow();
                assertTrue(threads.awaitTermination(5000, TimeUnit.MILLISECONDS));
            }
        }
    }

    private static Map<String, Object> account(String canonical, String password, boolean banned) {
        Map<String, Object> row = new HashMap<>();
        row.put("login", canonical);
        row.put("login_bytes", canonical.getBytes(StandardCharsets.UTF_8).length);
        row.put("password", password);
        row.put("password_bytes", password == null ? null : password.getBytes(StandardCharsets.UTF_8).length);
        row.put("banned", banned);
        return row;
    }

    private static void assertReadFailure(Jdbc fixture, Callable<?> call) {
        SQLException failure = assertThrows(SQLException.class, () -> scoped(call));
        assertEquals("modern JDBC backend unavailable", failure.getMessage());
        assertNull(failure.getCause());
        assertNull(failure.getNextException());
        assertEquals(0, failure.getSuppressed().length);
        assertReadRetired(fixture);
    }

    private static void assertReadRetired(Jdbc fixture) {
        assertEquals(List.of("result-close", "statement-close", "connection-close"), fixture.events);
        assertTrue(fixture.receivedProperties.isEmpty());
        assertTrue(fixture.readOnly);
    }
    private static Map<String, Object> character(long id, String name, long type, long sex, long level) {
        return new HashMap<>(Map.of("objid", id, "char_name", name, "name_bytes", name.getBytes(StandardCharsets.UTF_8).length,
                "character_type", type, "character_sex", sex, "level", level));
    }

    /** Synthetic JDBC objects deliberately implement no writes or unknown operations. */
    private static final class Jdbc implements ModernJdbcBackend.Connector {
        final List<Map<String, Object>> rows;
        final List<String> events = new ArrayList<>();
        final Map<String, Integer> numericTypes = new HashMap<>();
        final List<String> numericReads = new ArrayList<>();
        String url, sql, owner;
        Properties receivedProperties, snapshot;
        int connections, maxRows, queryTimeoutSeconds, networkMillis, aborts;
        boolean readOnly, registerSocket, cancelWhileClosing;
        Socket socket;
        Jdbc(List<Map<String, Object>> rows) { this.rows = rows; }
        @Override public Connection connect(String url, Properties properties) {
            connections++;
            this.url = url;
            this.receivedProperties = properties;
            this.snapshot = new Properties();
            this.snapshot.putAll(properties);
            if (registerSocket) socket = new ModernJdbcBackend.ScopedSocketFactory().createSocket(null);
            return proxy(Connection.class, (method, args) -> switch (method) {
                case "setReadOnly" -> { readOnly = (boolean) args[0]; yield null; }
                case "setNetworkTimeout" -> { networkMillis = (int) args[1]; yield null; }
                case "prepareStatement" -> { sql = (String) args[0]; yield statement(); }
                case "abort" -> { aborts++; yield null; }
                case "close" -> {
                    events.add("connection-close");
                    if (cancelWhileClosing) {
                        assertFalse(socket.isClosed());
                        ModernBackendScope.current().cancel(ModernBackendExecutor.Failure.CANCELLED);
                        assertTrue(socket.isClosed());
                    }
                    yield null;
                }
                default -> throw new AssertionError("unexpected JDBC connection operation: " + method);
            });
        }
        private PreparedStatement statement() {
            return proxy(PreparedStatement.class, (method, args) -> switch (method) {
                case "setMaxRows" -> { maxRows = (int) args[0]; yield null; }
                case "setQueryTimeout" -> { queryTimeoutSeconds = (int) args[0]; yield null; }
                case "setString" -> { assertEquals(1, args[0]); owner = (String) args[1]; yield null; }
                case "executeQuery" -> result();
                case "close" -> { events.add("statement-close"); yield null; }
                default -> throw new AssertionError("unexpected JDBC statement operation: " + method);
            });
        }
        private ResultSet result() {
            AtomicInteger index = new AtomicInteger(-1);
            boolean[] wasNull = {false};
            var columns = new ModernJdbcNumbersTest.Columns(column -> rows.get(index.get()).get(column), numericTypes);
            return proxy(ResultSet.class, (method, args) -> switch (method) {
                case "next" -> index.incrementAndGet() < rows.size();
                case "wasNull" -> wasNull[0];
                case "findColumn" -> columns.find((String) args[0]);
                case "getMetaData" -> columns.metadata();
                case "getString", "getBigDecimal" -> {
                    String column = columns.name(args[0]);
                    Object value = rows.get(index.get()).get(column);
                    wasNull[0] = value == null;
                    if (method.equals("getString")) yield value;
                    assertEquals(1, args.length, "rounding overload is forbidden");
                    numericReads.add(column);
                    yield ModernJdbcNumbersTest.Columns.decimal(value);
                }
                case "close" -> { events.add("result-close"); yield null; }
                default -> throw new AssertionError("unexpected JDBC result operation: " + method);
            });
        }
    }
    @FunctionalInterface private interface Invocation { Object call(String method, Object[] arguments) throws Throwable; }
    private static <T> T proxy(Class<T> type, Invocation implementation) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (instance, method, arguments) -> implementation.call(method.getName(), arguments)));
    }
}
