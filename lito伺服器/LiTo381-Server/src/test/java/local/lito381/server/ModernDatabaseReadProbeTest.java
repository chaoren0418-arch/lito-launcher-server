package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernDatabaseReadProbeTest {
    @TempDir Path temporary;
    private static final String SECRET = "synthetic-only-secret-value";
    private Path fixture(String source, String database) throws Exception {
        Files.writeString(temporary.resolve("sources.properties"), source, StandardCharsets.UTF_8);
        Files.writeString(temporary.resolve("database.properties"), database, StandardCharsets.UTF_8);
        return Files.writeString(temporary.resolve("server.properties"),
                "database.mode=lito_mysql\nsource.servers.file=sources.properties\n"
                + "database.connection.properties=database.properties\n"
                + "login.classic.server.names=must-not-fallback\ndatabase.dynamic.databases=must_not_fallback\n",
                StandardCharsets.UTF_8);
    }
    private String sources() {
        return "source.count=2\nsource.1.id=42\nsource.1.name=月亮\nsource.1.database=moon_db\n"
                + "source.2.id=7\nsource.2.name=太陽神\nsource.2.database=sun_db\n";
    }
    private String database() {
        return "Driver_LOGIN=com.mysql.cj.jdbc.Driver\nURL1_LOGIN=jdbc:mysql://127.0.0.1:3306/\n"
                + "URL2_LOGIN=wrong_fallback_db\nURL3_LOGIN=?socketFactory=NOT_ALLOWED&socketTimeout=0&useSSL=false\n"
                + "Login_LOGIN=synthetic-user\nPassword_LOGIN=" + SECRET + "\n";
    }

    @Test void strictSourceIdsAreNotOrdinalsAndLegacyOptionsNeverReachModernConfig() throws Exception {
        Path main = fixture(sources(), database());
        var settings = ModernDatabaseReadProbe.load(main);
        try (settings; var modern = ModernDatabaseReadProbe.modernConfig(settings)) {
            assertEquals(List.of(7, 42), settings.sources.stream().map(ModernDatabaseReadProbe.Source::id).toList());
            assertEquals(List.of("sun_db", "moon_db"), settings.sources.stream().map(ModernDatabaseReadProbe.Source::database).toList());
            assertEquals("太陽神", modern.sources().get(0).name());
            assertEquals("sun_db", modern.sources().get(0).database().database());
            assertEquals("moon_db", modern.sources().get(1).database().database());
            assertEquals("DISABLED", modern.sources().get(0).database().sslMode());
            assertEquals(1000, modern.dbConnectMillis());
            assertEquals(3000, modern.dbNetworkMillis());
            assertEquals(5000, modern.backendDeadlineMillis());
            assertArrayEquals(SECRET.toCharArray(), modern.sources().get(0).database().password());
            assertFalse(settings.toString().contains(SECRET));
            assertFalse(Files.exists(modern.tls().keyStore()), "probe must not create a fake listener key store");
        }
        assertThrows(IllegalStateException.class, settings::password);
        assertThrows(IllegalStateException.class, settings::username);
    }

    @Test void missingExternalSourceOrExactCredentialKeysNeverFallsBack() throws Exception {
        Path main = fixture(sources(), database());
        Files.move(temporary.resolve("sources.properties"), temporary.resolve("saved-sources.properties"));
        rejected(() -> ModernDatabaseReadProbe.load(main));
        Path exact = fixture(sources(), database().replace("Login_LOGIN=", "Login=").replace("Password_LOGIN=", "Password="));
        rejected(() -> ModernDatabaseReadProbe.load(exact));
    }

    @Test void invalidDuplicateAndMissingSourceIdentitiesFailInsteadOfGuessing() throws Exception {
        for (String invalid : List.of(sources().replace("source.2.id=7", "source.2.id=42"),
                sources().replace("source.2.id=7\n", ""), sources().replace("source.2.id=7", "source.2.id=0"),
                sources().replace("source.2.id=7", "source.2.id=256"), sources().replace("source.2.database=sun_db", "source.2.database=../bad"),
                sources().replace("source.count=2", "source.count=17"), sources() + "source.3.id=8\n")) {
            Path main = fixture(invalid, database());
            rejected(() -> ModernDatabaseReadProbe.load(main));
        }
    }

    @Test void numericLoopbackAndExactLocalhostAliasAreAcceptedWithoutDns() throws Exception {
        assertEquals(3306, ModernDatabaseReadProbe.endpoint("jdbc:mysql://127.0.0.1/").port());
        assertEquals(3307, ModernDatabaseReadProbe.endpoint("jdbc:mysql://[::1]:3307/").port());
        assertEquals(new ModernDatabaseReadProbe.Endpoint("127.0.0.1", 3306),
                ModernDatabaseReadProbe.endpoint("jdbc:mysql://localhost/"));
        assertEquals(new ModernDatabaseReadProbe.Endpoint("127.0.0.1", 3307),
                ModernDatabaseReadProbe.endpoint("jdbc:mysql://LOCALhost:3307/"));
        for (String value : List.of("jdbc:mysql://localhost./", "jdbc:mysql://localhost.example/",
                "jdbc:mysql://localh\u043est/", "jdbc:mysql://%6cocalhost/", "jdbc:mysql://localhost,127.0.0.1/",
                "jdbc:mysql://user:secret@localhost/", "jdbc:mysql://localhost/?socketTimeout=0",
                "jdbc:mysql://localhost:0/", "jdbc:mysql://localhost:65536/", "jdbc:mysql://localhost:99999999999999999999/",
                "jdbc:mysql://localhost:/", "jdbc:mysql://localhost/ ", "jdbc:mysql://192.0.2.7/", "jdbc:mysql://0.0.0.0/",
                "jdbc:mysql://127.1/", "jdbc:mysql://0177.0.0.1/", "jdbc:mysql://127.0.0.1:0/", "jdbc:mysql://127.0.0.1:99999/",
                "jdbc:mysql://user:secret@127.0.0.1/", "jdbc:mysql://127.0.0.1/database", "jdbc:mysql://127.0.0.1/?socketTimeout=0",
                "jdbc:mysql://127.0.0.1/#secret", "jdbc:mysql://127.0.0.1,127.0.0.2/", "jdbc:mysql://[fe80::1%25zone]/")) {
            rejected(() -> ModernDatabaseReadProbe.endpoint(value));
        }
    }

    @Test void exactLocalhostAliasBecomesNumericInStrictModelWithoutLegacyFallback() throws Exception {
        Path main = fixture(sources(), database().replace("127.0.0.1:3306", "localhost:3307"));
        try (var settings = ModernDatabaseReadProbe.load(main); var modern = ModernDatabaseReadProbe.modernConfig(settings)) {
            assertEquals(new ModernDatabaseReadProbe.Endpoint("127.0.0.1", 3307), settings.endpoint);
            for (var source : modern.sources()) {
                assertEquals("127.0.0.1", source.database().host());
                assertEquals(3307, source.database().port());
                assertEquals("DISABLED", source.database().sslMode());
            }
            assertEquals(List.of("sun_db", "moon_db"), settings.sources.stream().map(ModernDatabaseReadProbe.Source::database).toList());
        }
    }

    @Test void configurationFailuresReportStaticStageReasonAndOnlyAllowlistedKeys() throws Exception {
        Path main = fixture(sources(), database().replace("127.0.0.1:3306", SECRET));
        diagnostic(() -> ModernDatabaseReadProbe.load(main), ModernDatabaseReadProbe.Failure.CONFIG_ENDPOINT_HOST,
                ModernDatabaseReadProbe.Stage.ENDPOINT, "URL1_LOGIN");
        fixture(sources(), database().replace("127.0.0.1:3306", "127.0.0.1:99999"));
        diagnostic(() -> ModernDatabaseReadProbe.load(main), ModernDatabaseReadProbe.Failure.CONFIG_ENDPOINT_PORT,
                ModernDatabaseReadProbe.Stage.ENDPOINT, "URL1_LOGIN");
        fixture(sources(), database().replace("Password_LOGIN=", "Password="));
        diagnostic(() -> ModernDatabaseReadProbe.load(main), ModernDatabaseReadProbe.Failure.CONFIG_REQUIRED_KEY,
                ModernDatabaseReadProbe.Stage.DATABASE_SETTINGS, "Password_LOGIN");
        fixture(sources().replace("source.2.id=7", "source.2.id=0"), database());
        diagnostic(() -> ModernDatabaseReadProbe.load(main), ModernDatabaseReadProbe.Failure.CONFIG_VALUE,
                ModernDatabaseReadProbe.Stage.SOURCE_SETTINGS, "source.2.id");
        fixture(sources() + SECRET + "=" + SECRET + "\n", database());
        diagnostic(() -> ModernDatabaseReadProbe.load(main), ModernDatabaseReadProbe.Failure.CONFIG_UNKNOWN_KEY,
                ModernDatabaseReadProbe.Stage.SOURCE_SETTINGS, null);
        fixture(sources() + SECRET + "=first\n" + SECRET + "=second\n", database());
        diagnostic(() -> ModernDatabaseReadProbe.load(main), ModernDatabaseReadProbe.Failure.CONFIG_DUPLICATE_KEY,
                ModernDatabaseReadProbe.Stage.SOURCE_FILE, "UNRECOGNIZED_KEY");
        fixture(sources() + "source.1.\\u0069d=99\n", database());
        diagnostic(() -> ModernDatabaseReadProbe.load(main), ModernDatabaseReadProbe.Failure.CONFIG_DUPLICATE_KEY,
                ModernDatabaseReadProbe.Stage.SOURCE_FILE, "source.1.id");
        Files.write(temporary.resolve("sources.properties"), new byte[] {(byte) 0xc3, 0x28});
        diagnostic(() -> ModernDatabaseReadProbe.load(main), ModernDatabaseReadProbe.Failure.CONFIG_UTF8,
                ModernDatabaseReadProbe.Stage.SOURCE_FILE, "source.servers.file");
        Files.write(temporary.resolve("sources.properties"), new byte[ModernDatabaseReadProbe.MAX_FILE_BYTES + 1]);
        diagnostic(() -> ModernDatabaseReadProbe.load(main), ModernDatabaseReadProbe.Failure.CONFIG_TOO_LARGE,
                ModernDatabaseReadProbe.Stage.SOURCE_FILE, "source.servers.file");
        fixture(sources() + "source.1.id=\\u12zz\n", database());
        diagnostic(() -> ModernDatabaseReadProbe.load(main), ModernDatabaseReadProbe.Failure.CONFIG_SYNTAX,
                ModernDatabaseReadProbe.Stage.SOURCE_FILE, "source.servers.file");
        fixture(sources(), database());
        Files.move(temporary.resolve("sources.properties"), temporary.resolve("saved-source.properties"));
        diagnostic(() -> ModernDatabaseReadProbe.load(main), ModernDatabaseReadProbe.Failure.CONFIG_IO,
                ModernDatabaseReadProbe.Stage.SOURCE_FILE, "source.servers.file");
        Files.writeString(main, "database.mode=lito_mysql\nsource.servers.file=../" + SECRET + "\n"
                + "database.connection.properties=database.properties\n");
        diagnostic(() -> ModernDatabaseReadProbe.load(main), ModernDatabaseReadProbe.Failure.CONFIG_PATH,
                ModernDatabaseReadProbe.Stage.SOURCE_FILE, "source.servers.file");
    }

    @Test void utf8BomAndPropertiesEquivalentSeparatorsRemainSupported() throws Exception {
        Path main = fixture("\ufeff" + sources().replace("=", ": "), "\ufeff" + database());
        try (var settings = ModernDatabaseReadProbe.load(main)) {
            assertEquals(List.of(7, 42), settings.sources.stream().map(ModernDatabaseReadProbe.Source::id).toList());
            assertEquals("太陽神", settings.sources.get(0).name());
        }
    }

    @Test void duplicateDecodedKeysMalformedUtf8OversizeAndParentPathsAreRejected() throws Exception {
        Path main = fixture(sources() + "source.1.\\u0069d=99\n", database());
        rejected(() -> ModernDatabaseReadProbe.load(main));
        fixture(sources(), database());
        Files.write(temporary.resolve("sources.properties"), new byte[] {(byte) 0xc3, 0x28});
        rejected(() -> ModernDatabaseReadProbe.load(main));
        Files.write(temporary.resolve("sources.properties"), new byte[ModernDatabaseReadProbe.MAX_FILE_BYTES + 1]);
        rejected(() -> ModernDatabaseReadProbe.load(main));
        fixture(sources(), database());
        Files.writeString(main, "database.mode=lito_mysql\nsource.servers.file=../sources.properties\n"
                + "database.connection.properties=database.properties\n");
        rejected(() -> ModernDatabaseReadProbe.load(main));
    }

    @Test void stdoutIsAllowlistedAndNeverIncludesCharacterNameOrCredentials() {
        var inspection = new ModernDatabaseReadProbe.Inspection();
        inspection.schema = ModernDatabaseReadProbe.Check.OK;
        inspection.charset = ModernDatabaseReadProbe.Check.OK;
        String output = ModernDatabaseReadProbe.render(new ModernDatabaseReadProbe.Source(7, "Sun\"\\\n", "sun_db"), inspection,
                new ModernDatabaseReadProbe.Outcome(true, List.of(new ModernApplicationProtocol.CharacterRow(3333, SECRET, 3, 1, 4_000_000_000L)), ModernDatabaseReadProbe.Status.OK));
        assertFalse(output.contains(SECRET));
        assertFalse(output.contains("jdbc:"));
        assertFalse(output.contains("password"));
        assertFalse(output.contains("\n"));
        assertTrue(output.contains("\"legacy_options_not_inherited\":true"));
        assertTrue(output.contains("\"level\":4000000000"));
        assertTrue(output.contains("\"source_id\":7"));
        var row = new ModernApplicationProtocol.CharacterRow(1, SECRET, 1, 0, 1);
        assertThrows(IllegalArgumentException.class, () -> new ModernDatabaseReadProbe.Outcome(true,
                java.util.Collections.nCopies(65, row), ModernDatabaseReadProbe.Status.OK));
    }

    @Test void metadataSuccessRetiresTemporaryLeaseAndTransfersSameOwnedConnection() throws Exception {
        Jdbc fixture = new Jdbc(false, false);
        var inspection = new ModernDatabaseReadProbe.Inspection();
        scoped(() -> {
            Connection result = ModernDatabaseReadProbe.inspectOwned(fixture.connection, ModernBackendScope.current(), inspection);
            assertSame(fixture.connection, result);
            assertFalse(fixture.closed);
            assertTrue(fixture.readOnly);
            assertEquals(ModernDatabaseReadProbe.Check.OK, inspection.schema);
            assertEquals(ModernDatabaseReadProbe.Check.OK, inspection.charset);
            assertFalse(inspection.declaredDriverVersionRange, "synthetic 5.5 is outside declared Connector/J support");
            try (var next = ModernBackendScope.current().register(result)) {
                assertEquals(3, fixture.sql.size());
            }
            result.close();
            return null;
        });
        assertTrue(fixture.closed);
        assertEquals(3, fixture.closedStatements);
        assertEquals(3, fixture.closedResults);
        assertEquals(0, fixture.aborts);
    }

    @Test void metadataAndCharsetFailuresRetireLeaseCloseOwnedConnectionAndRedactErrors() throws Exception {
        Jdbc sqlFailure = new Jdbc(false, false);
        sqlFailure.metadataFailure = true;
        for (Jdbc fixture : List.of(new Jdbc(true, false), new Jdbc(false, true), sqlFailure)) {
            var inspection = new ModernDatabaseReadProbe.Inspection();
            scoped(() -> {
                SQLException error = assertThrows(SQLException.class,
                        () -> ModernDatabaseReadProbe.inspectOwned(fixture.connection, ModernBackendScope.current(), inspection));
                assertFalse(error.toString().contains(SECRET));
                assertNull(error.getCause());
                assertEquals(0, error.getSuppressed().length);
                assertTrue(fixture.closed);
                ModernBackendScope.current().cancel(ModernBackendExecutor.Failure.CANCELLED);
                assertEquals(0, fixture.aborts, "cancel cannot reach retired temporary lease");
                return null;
            });
            assertEquals(fixture.sql.size(), fixture.closedStatements);
            assertEquals(fixture.sql.size(), fixture.closedResults);
        }
    }

    private static void rejected(Callable<?> action) {
        var failure = assertThrows(ModernDatabaseReadProbe.ProbeException.class, action::call);
        assertFalse(failure.toString().contains(SECRET));
        assertFalse(failure.toString().contains("jdbc:"));
        assertNull(failure.getCause());
        assertEquals(0, failure.getSuppressed().length);
    }
    private static void diagnostic(Callable<?> action, ModernDatabaseReadProbe.Failure reason,
            ModernDatabaseReadProbe.Stage stage, String key) {
        var failure = assertThrows(ModernDatabaseReadProbe.ProbeException.class, action::call);
        assertEquals(reason, failure.failure);
        assertEquals(stage, failure.stage);
        assertEquals(key, failure.key);
        String output = ModernDatabaseReadProbe.renderFailure(failure);
        assertFalse(output.contains(SECRET));
        assertFalse(output.contains("jdbc:"));
        assertFalse(output.contains("\n"));
        assertNull(failure.getCause());
        assertEquals(0, failure.getSuppressed().length);
    }
    private static <T> T scoped(Callable<T> action) throws Exception {
        var scope = new ModernBackendScope(System.nanoTime() + TimeUnit.SECONDS.toNanos(10), new ModernBackendScope.AbortScheduler() {
            public boolean execute(Runnable action) { action.run(); return true; }
            public void remove(Runnable action) {}
        });
        ModernBackendScope.bind(scope);
        try { return action.call(); }
        finally { scope.finish(); ModernBackendScope.unbind(); }
    }

    private static final class Jdbc {
        final Connection connection;
        final boolean invalidSchema, invalidCharset;
        final List<String> sql = new ArrayList<>();
        boolean closed, readOnly, metadataFailure;
        int closedStatements, closedResults, aborts;
        Jdbc(boolean invalidSchema, boolean invalidCharset) {
            this.invalidSchema = invalidSchema; this.invalidCharset = invalidCharset;
            connection = proxy(Connection.class, (name, args) -> switch (name) {
                case "setReadOnly" -> { readOnly = (boolean) args[0]; yield null; }
                case "setNetworkTimeout" -> { assertTrue((int) args[1] > 0 && (int) args[1] <= 3000); yield null; }
                case "getMetaData" -> {
                    if (metadataFailure) throw new SQLException(SECRET, new SQLException(SECRET));
                    yield proxy(DatabaseMetaData.class, (method, ignored) -> switch (method) {
                        case "getDatabaseMajorVersion", "getDatabaseMinorVersion" -> 5;
                        default -> throw new AssertionError("unexpected metadata call");
                    });
                }
                case "prepareStatement" -> statement((String) args[0]);
                case "close" -> { closed = true; yield null; }
                case "abort" -> { aborts++; yield null; }
                default -> throw new AssertionError("unexpected connection call");
            });
        }
        private PreparedStatement statement(String query) {
            assertTrue(List.of(ModernDatabaseReadProbe.ACCOUNT_SCHEMA_SQL, ModernDatabaseReadProbe.CHARACTER_SCHEMA_SQL,
                    ModernDatabaseReadProbe.CHARSET_SQL).contains(query), "only allowlisted SELECT SQL");
            sql.add(query);
            return proxy(PreparedStatement.class, (method, args) -> switch (method) {
                case "setMaxRows" -> { assertEquals(2, args[0]); yield null; }
                case "setQueryTimeout" -> { assertTrue((int) args[0] >= 1 && (int) args[0] <= 10); yield null; }
                case "setString" -> { assertEquals(ModernDatabaseReadProbe.ROUNDTRIP, args[1]); yield null; }
                case "executeQuery" -> result(query);
                case "close" -> { closedStatements++; yield null; }
                default -> throw new AssertionError("unexpected statement call");
            });
        }
        private ResultSet result(String query) {
            boolean charset = ModernDatabaseReadProbe.CHARSET_SQL.equals(query);
            int[] types = ModernDatabaseReadProbe.ACCOUNT_SCHEMA_SQL.equals(query)
                    ? new int[] {Types.VARCHAR, Types.VARCHAR, Types.TINYINT}
                    : new int[] {Types.INTEGER, Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.BIGINT, Types.VARCHAR, Types.TINYINT};
            AtomicInteger row = new AtomicInteger();
            return proxy(ResultSet.class, (method, args) -> switch (method) {
                case "next" -> charset && row.incrementAndGet() == 1;
                case "getString" -> invalidCharset ? SECRET : ModernDatabaseReadProbe.ROUNDTRIP;
                case "getMetaData" -> proxy(ResultSetMetaData.class, (name, values) -> switch (name) {
                    case "getColumnCount" -> types.length;
                    case "getColumnType" -> invalidSchema ? Types.BLOB : types[(int) values[0] - 1];
                    default -> throw new AssertionError("unexpected result metadata call");
                });
                case "close" -> { closedResults++; yield null; }
                default -> throw new AssertionError("unexpected result call");
            });
        }
    }
    @FunctionalInterface private interface Invocation { Object invoke(String name, Object[] arguments) throws Throwable; }
    private static <T> T proxy(Class<T> type, Invocation call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (instance, method, arguments) -> call.invoke(method.getName(), arguments)));
    }
}
