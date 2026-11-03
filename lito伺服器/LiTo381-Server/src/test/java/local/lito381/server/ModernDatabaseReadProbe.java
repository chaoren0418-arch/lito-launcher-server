package local.lito381.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 * Opt-in, TEST CLASSPATH ONLY. Never packaged as a launcher/server feature.
 * Usage: ModernDatabaseReadProbe <absolute native server.properties path>
 * Test game credentials ONLY: LITO_MODERN_PROBE_ACCOUNT / LITO_MODERN_PROBE_PASSWORD.
 * Never calls native open/healthCheck/migration/main and never issues DDL/DML.
 */
public final class ModernDatabaseReadProbe {
    static final int MAX_FILE_BYTES = 32 * 1024;
    static final int SOURCE_DEADLINE_MILLIS = 5000;
    static final String ACCOUNT_SCHEMA_SQL = "SELECT login,password,banned FROM accounts LIMIT 0";
    static final String CHARACTER_SCHEMA_SQL = "SELECT objid,char_name,Type,Sex,level,account_name,Banned FROM characters LIMIT 0";
    static final String CHARSET_SQL = "SELECT ? AS lito_probe_text";
    static final String ROUNDTRIP = "LiTo-繁體中文-\ud83d\ude03";
    private static final Set<String> DATABASE_KEYS = Set.of("Driver_LOGIN", "URL1_LOGIN", "URL2_LOGIN", "URL3_LOGIN",
            "Login_LOGIN", "Password_LOGIN", "URL1", "URL2", "URL3", "Login", "Password");
    private ModernDatabaseReadProbe() {}

    enum Failure { ARGUMENTS, CONFIGURATION, ENVIRONMENT, BACKEND, SHUTDOWN,
        CONFIG_PATH, CONFIG_IO, CONFIG_TOO_LARGE, CONFIG_UTF8, CONFIG_SYNTAX,
        CONFIG_DUPLICATE_KEY, CONFIG_REQUIRED_KEY, CONFIG_VALUE, CONFIG_UNKNOWN_KEY,
        CONFIG_ENDPOINT_FORMAT, CONFIG_ENDPOINT_HOST, CONFIG_ENDPOINT_PORT, CONFIG_MODERN_MODEL }
    enum Stage { UNSPECIFIED, INPUT, SERVER_FILE, SERVER_SETTINGS, SOURCE_FILE, SOURCE_SETTINGS,
        DATABASE_FILE, DATABASE_SETTINGS, ENDPOINT, MODERN_MODEL, BACKEND }
    static final class ProbeException extends Exception {
        final Failure failure;
        final Stage stage;
        final String key;
        ProbeException(Failure failure) { this(failure, Stage.UNSPECIFIED, null); }
        ProbeException(Failure failure, Stage stage, String key) {
            super("modern database read probe: " + failure);
            this.failure = failure; this.stage = stage; this.key = diagnosticKey(key);
        }
        ProbeException contextual(Stage context, String fallbackKey) {
            return new ProbeException(failure, stage == Stage.UNSPECIFIED ? context : stage,
                    key == null ? fallbackKey : key);
        }
    }
    record Source(int id, String name, String database) {}
    record Endpoint(String host, int port) {}
    enum Check { NOT_CHECKED, OK, FAILED }
    enum Status { OK, INVALID_CREDENTIALS, BACKEND_UNAVAILABLE, INVALID_STATE,
        DEADLINE, CANCELLED, CLOSED, OVERLOADED, BUSY, BACKEND, WORKER_NOT_RETIRED }
    static final class Inspection {
        volatile Check schema = Check.NOT_CHECKED;
        volatile Check charset = Check.NOT_CHECKED;
        volatile boolean declaredDriverVersionRange;
        volatile boolean authenticated;
    }
    static final class Settings implements AutoCloseable {
        final Path directory;
        final Endpoint endpoint;
        final List<Source> sources;
        private final char[] username;
        private final char[] password;
        private boolean closed;
        Settings(Path directory, Endpoint endpoint, List<Source> sources, String username, String password) {
            this.directory = directory; this.endpoint = endpoint; this.sources = List.copyOf(sources);
            this.username = username.toCharArray(); this.password = password.toCharArray();
        }
        String username() { ensureOpen(); return new String(username); }
        String password() { ensureOpen(); return new String(password); }
        private void ensureOpen() { if (closed) throw new IllegalStateException("probe settings closed"); }
        @Override public void close() { Arrays.fill(username, '\0'); Arrays.fill(password, '\0'); closed = true; }
        @Override public String toString() { return "ModernProbeSettings[redacted]"; }
    }
    record Outcome(boolean authenticated, List<ModernApplicationProtocol.CharacterRow> rows, Status status) {
        Outcome {
            if (rows == null || rows.size() > ModernApplicationProtocol.MAX_ROWS || status == null
                    || (!authenticated && !rows.isEmpty())) throw new IllegalArgumentException("invalid probe outcome");
            rows = List.copyOf(rows);
        }
    }

    public static void main(String[] args) {
        PrintWriter output = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
        int exit = 2;
        Stage stage = Stage.INPUT;
        try {
            if (args.length != 1) throw new ProbeException(Failure.ARGUMENTS, Stage.INPUT, null);
            String account = System.getenv("LITO_MODERN_PROBE_ACCOUNT");
            String password = System.getenv("LITO_MODERN_PROBE_PASSWORD");
            if (!validText(account, 64, false)) throw new ProbeException(Failure.ENVIRONMENT, Stage.INPUT, "LITO_MODERN_PROBE_ACCOUNT");
            if (!validText(password, 256, true)) throw new ProbeException(Failure.ENVIRONMENT, Stage.INPUT, "LITO_MODERN_PROBE_PASSWORD");
            Path serverFile;
            try { serverFile = Path.of(args[0]); }
            catch (java.nio.file.InvalidPathException failure) { throw new ProbeException(Failure.CONFIG_PATH, Stage.INPUT, null); }
            stage = Stage.SERVER_FILE;
            try (Settings settings = load(serverFile); ModernServerConfig modern = modernConfig(settings)) {
                stage = Stage.BACKEND;
                exit = execute(settings, modern, account, password, output);
            }
        } catch (ProbeException failure) {
            output.println(renderFailure(failure));
        } catch (Exception failure) {
            // Never print cause, suppressed exceptions, native Properties, URL or env values.
            output.println(renderFailure(new ProbeException(stage == Stage.BACKEND ? Failure.BACKEND : Failure.CONFIGURATION, stage, null)));
        }
        output.flush();
        if (exit != 0) System.exit(exit);
    }

    private static int execute(Settings settings, ModernServerConfig modern, String account, String password, PrintWriter output)
            throws Exception {
        ModernBackendExecutor executor = new ModernBackendExecutor(1, 0);
        boolean allPassed = true;
        try {
            for (int index = 0; index < settings.sources.size(); index++) {
                Source source = settings.sources.get(index);
                Inspection inspection = new Inspection();
                ModernJdbcBackend.Connector connector = (url, properties) -> {
                    Connection connection = new com.mysql.cj.jdbc.Driver().connect(url, properties);
                    if (connection == null) throw unavailable();
                    return inspectOwned(connection, ModernBackendScope.current(), inspection);
                };
                var backend = new ModernJdbcBackend(modern.sources().get(index).database(),
                        modern.dbConnectMillis(), modern.dbNetworkMillis(), connector);
                var verifier = ModernAccountAuthentication.repositories(Map.of(source.id,
                        ModernAccountAuthentication.SourceBinding.direct(source.name, backend::authenticate)), ignored -> false);
                var characters = ModernJdbcBackend.characters(Map.of(source.id, backend));
                Outcome outcome;
                try (var gate = executor.openSession()) {
                    char[] ownedPassword = password.toCharArray();
                    try {
                        outcome = gate.call(SOURCE_DEADLINE_MILLIS, new ModernBackendExecutor.Work<Outcome>() {
                            @Override public Outcome run(ModernBackendScope scope) throws Exception {
                                try {
                                    var principal = ModernAccountAuthentication.verify(verifier, account, ownedPassword);
                                    inspection.authenticated = true;
                                    return new Outcome(true, characters.characters(principal, source.id), Status.OK);
                                } catch (ModernAccountAuthentication.AuthenticationException failure) {
                                    scope.checkActive();
                                    return new Outcome(false, List.of(), Status.valueOf(failure.failure().name()));
                                }
                            }
                            @Override public void close() { Arrays.fill(ownedPassword, '\0'); }
                        });
                    } catch (ModernBackendExecutor.BackendException failure) {
                        outcome = new Outcome(false, List.of(), Status.valueOf(failure.failure().name()));
                    }
                }
                // Cancellation may return before the driver actually leaves. Do not
                // start another source or pretend its capacity was already returned.
                if (!awaitQuiescent(executor, 3000)) {
                    output.println(render(source, inspection, new Outcome(false, List.of(), Status.WORKER_NOT_RETIRED)));
                    executor.close();
                    if (!executor.awaitTermination(3000)) throw new ProbeException(Failure.SHUTDOWN);
                    return 1;
                }
                outcome = new Outcome(inspection.authenticated, outcome.rows, outcome.status);
                output.println(render(source, inspection, outcome));
                allPassed &= outcome.status == Status.OK && outcome.authenticated
                        && inspection.schema == Check.OK && inspection.charset == Check.OK;
            }
            return allPassed ? 0 : 1;
        } finally {
            executor.close();
            if (!executor.awaitTermination(3000)) throw new ProbeException(Failure.SHUTDOWN);
        }
    }

    private static boolean awaitQuiescent(ModernBackendExecutor executor, long millis) throws InterruptedException {
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(millis);
        while (!executor.isQuiescent()) {
            if (System.nanoTime() - deadline >= 0) return false;
            Thread.sleep(5);
        }
        return true;
    }

    /**
     * Takes ownership immediately. On success retires this temporary JDBC lease
     * before returning the SAME connection to the DAO; its raw SocketLease remains
     * armed in DAO Attempt throughout. On failure retires, closes, then throws only
     * a redacted error. This is not a second pool or a separate unbounded connection.
     */
    static Connection inspectOwned(Connection connection, ModernBackendScope scope, Inspection inspection) throws SQLException {
        boolean transferred = false;
        try {
            try (var lease = scope.register(connection)) {
                scope.checkActive();
                connection.setNetworkTimeout(Runnable::run, Math.min(3000, scope.remainingMillis()));
                connection.setReadOnly(true);
                if (inspection.schema == Check.NOT_CHECKED) {
                    inspection.schema = Check.FAILED;
                    var metadata = connection.getMetaData();
                    int major = metadata.getDatabaseMajorVersion();
                    int minor = metadata.getDatabaseMinorVersion();
                    inspection.declaredDriverVersionRange = (major == 5 && minor == 7) || (major == 8 && minor == 0);
                    schema(connection, scope, ACCOUNT_SCHEMA_SQL, new boolean[] {false, false, true});
                    schema(connection, scope, CHARACTER_SCHEMA_SQL, new boolean[] {true, false, true, true, true, false, true});
                    inspection.schema = Check.OK;
                    inspection.charset = Check.FAILED;
                    try (PreparedStatement statement = prepare(connection, scope, CHARSET_SQL)) {
                        statement.setString(1, ROUNDTRIP);
                        try (ResultSet result = statement.executeQuery()) {
                            if (!result.next() || !ROUNDTRIP.equals(result.getString(1)) || result.next()) throw unavailable();
                        }
                    }
                    inspection.charset = Check.OK;
                } else if (inspection.schema != Check.OK || inspection.charset != Check.OK) throw unavailable();
                scope.checkActive();
            }
            scope.checkActive();
            transferred = true;
            return connection;
        } catch (Exception failure) {
            throw unavailable();
        } finally {
            if (!transferred) try { connection.close(); } catch (SQLException | RuntimeException ignored) {}
        }
    }

    private static void schema(Connection connection, ModernBackendScope scope, String sql, boolean[] numeric) throws Exception {
        try (PreparedStatement statement = prepare(connection, scope, sql); ResultSet result = statement.executeQuery()) {
            var metadata = result.getMetaData();
            if (metadata.getColumnCount() != numeric.length || result.next()) throw unavailable();
            for (int index = 0; index < numeric.length; index++) {
                int type = metadata.getColumnType(index + 1);
                boolean supported = numeric[index]
                        ? Set.of(Types.BIGINT, Types.INTEGER, Types.SMALLINT, Types.TINYINT, Types.BIT, Types.BOOLEAN).contains(type)
                        : Set.of(Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR, Types.NCHAR, Types.NVARCHAR, Types.LONGNVARCHAR).contains(type);
                if (!supported) throw unavailable();
            }
            scope.checkActive();
        }
    }
    private static PreparedStatement prepare(Connection connection, ModernBackendScope scope, String sql) throws Exception {
        scope.checkActive();
        PreparedStatement statement = connection.prepareStatement(sql);
        try {
            statement.setMaxRows(2);
            statement.setQueryTimeout(Math.max(1, (scope.remainingMillis() + 999) / 1000));
            return statement;
        } catch (Exception failure) {
            try { statement.close(); } catch (SQLException ignored) {}
            throw failure;
        }
    }

    static Settings load(Path serverFile) throws ProbeException {
        Properties main = new Properties(), sources = new Properties(), database = new Properties();
        Stage stage = Stage.SERVER_FILE;
        String key = null;
        try {
            Path checkedMain = checkedFile(serverFile);
            Path directory = checkedMain.getParent();
            main = readProperties(checkedMain);
            stage = Stage.SERVER_SETTINGS;
            key = "database.mode";
            if (!"lito_mysql".equals(required(main, key))) throw invalid(Failure.CONFIG_VALUE);
            key = "source.servers.file";
            String sourcePath = required(main, key);
            stage = Stage.SOURCE_FILE;
            Path sourceFile = childFile(directory, sourcePath);
            sources = readProperties(sourceFile);
            stage = Stage.SERVER_SETTINGS;
            key = "database.connection.properties";
            String databasePath = required(main, key);
            stage = Stage.DATABASE_FILE;
            Path databaseFile = childFile(directory, databasePath);
            database = readProperties(databaseFile);
            stage = Stage.SOURCE_SETTINGS;
            key = "source.count";
            int count = number(required(sources, key), 1, 16);
            Set<String> allowed = new HashSet<>(Set.of("source.count"));
            Set<Integer> ids = new HashSet<>();
            Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            List<Source> explicit = new ArrayList<>();
            for (int index = 1; index <= count; index++) {
                String prefix = "source." + index + ".";
                for (String suffix : List.of("id", "name", "database", "weight.rate", "resource.database")) allowed.add(prefix + suffix);
                key = prefix + "id";
                int id = number(required(sources, key), 1, 255);
                if (!ids.add(id)) throw invalid(Failure.CONFIG_VALUE);
                key = prefix + "name";
                String name = required(sources, key);
                if (!validText(name, 256, false) || name.length() > 64 || !name.equals(name.strip()) || !names.add(name)) {
                    throw invalid(Failure.CONFIG_VALUE);
                }
                key = prefix + "database";
                String schema = required(sources, key);
                if (!schema.matches("[A-Za-z0-9_]{1,64}")) throw invalid(Failure.CONFIG_VALUE);
                explicit.add(new Source(id, name, schema));
            }
            key = null;
            if (!allowed.containsAll(sources.stringPropertyNames())) throw invalid(Failure.CONFIG_UNKNOWN_KEY);
            stage = Stage.DATABASE_SETTINGS;
            if (!DATABASE_KEYS.containsAll(database.stringPropertyNames())) throw invalid(Failure.CONFIG_UNKNOWN_KEY);
            key = "URL1_LOGIN";
            Endpoint endpoint = endpoint(required(database, key));
            key = "Login_LOGIN";
            String username = required(database, key);
            if (!validText(username, 256, false) || !username.equals(username.strip())) throw invalid(Failure.CONFIG_VALUE);
            key = "Password_LOGIN";
            String password = required(database, key);
            if (!validText(password, 4096, true)) throw invalid(Failure.CONFIG_VALUE);
            explicit.sort(java.util.Comparator.comparingInt(Source::id));
            // URL2_LOGIN never chooses a source. URL3_LOGIN and all legacy driver
            // options are deliberately NOT inherited, not parsed into new options.
            return new Settings(directory, endpoint, explicit, username, password);
        } catch (ProbeException failure) {
            throw failure.contextual(stage, key);
        } catch (IOException failure) {
            throw new ProbeException(Failure.CONFIG_IO, stage, key);
        } catch (Exception failure) {
            throw new ProbeException(Failure.CONFIGURATION, stage, key);
        } finally { main.clear(); sources.clear(); database.clear(); }
    }

    static ModernServerConfig modernConfig(Settings settings) throws ProbeException {
        return modernConfig(settings, null);
    }

    static ModernServerConfig modernConfig(Settings settings, String catalogDatabase) throws ProbeException {
        if (catalogDatabase != null && !catalogDatabase.matches("[A-Za-z0-9_]{1,64}")) {
            throw new ProbeException(Failure.CONFIG_MODERN_MODEL, Stage.MODERN_MODEL, null);
        }
        StringBuilder text = new StringBuilder("schema=1\nbind.host=127.0.0.1\nbind.port=0\n")
                // Required by the strict shared config model; never created/opened,
                // and no listener/TLS identity is constructed by this test probe.
                .append("tls.pkcs12=").append(property(settings.directory.resolve("unused-modern-probe-listener.p12").toString())).append('\n')
                .append("tls.password.env=PROBE_UNUSED_LISTENER\nlimits.dbConnectMillis=1000\nlimits.dbNetworkMillis=3000\n")
                .append("limits.backendDeadlineMillis=5000\nsources=")
                .append(settings.sources.stream().map(source -> Integer.toString(source.id)).collect(java.util.stream.Collectors.joining(","))).append('\n');
        for (Source source : settings.sources) {
            String prefix = "source." + source.id + ".";
            text.append(prefix).append("name=").append(property(source.name)).append('\n')
                    .append(prefix).append("mysql.host=").append(settings.endpoint.host).append('\n')
                    .append(prefix).append("mysql.port=").append(settings.endpoint.port).append('\n')
                    .append(prefix).append("mysql.database=").append(source.database).append('\n')
                    .append(prefix).append("mysql.username.env=PROBE_DB_USER\n")
                    .append(prefix).append("mysql.password.env=PROBE_DB_PASSWORD\n")
                    .append(prefix).append("mysql.passwordScheme=plain\n")
                    .append(prefix).append("mysql.tls=disabled\n");
            if (catalogDatabase != null) text.append(prefix).append("catalog.database=").append(catalogDatabase).append('\n');
        }
        try {
            return ModernServerConfig.parse(text.toString().getBytes(StandardCharsets.UTF_8), name -> switch (name) {
                case "PROBE_UNUSED_LISTENER" -> "unused-test-probe-listener";
                case "PROBE_DB_USER" -> settings.username();
                case "PROBE_DB_PASSWORD" -> settings.password();
                default -> null;
            });
        } catch (Exception failure) {
            throw new ProbeException(Failure.CONFIG_MODERN_MODEL, Stage.MODERN_MODEL, null);
        }
    }

    static Endpoint endpoint(String value) throws ProbeException {
        Failure reason = Failure.CONFIG_ENDPOINT_FORMAT;
        try {
            if (value == null || !value.startsWith("jdbc:mysql://") || value.length() > 256) throw invalid(reason);
            URI uri = URI.create(value.substring(5));
            if (uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null || !"/".equals(uri.getRawPath())) throw invalid(reason);
            String authority = uri.getRawAuthority();
            reason = Failure.CONFIG_ENDPOINT_HOST;
            if (authority == null || !authority.matches("(?:(?i:localhost)|[0-9.]+|\\[[0-9A-Fa-f:]+\\])(?::[0-9]+)?")) throw invalid(reason);
            int portSeparator = authority.startsWith("[") ? authority.indexOf(']') + 1 : authority.indexOf(':');
            if (portSeparator == authority.length()) portSeparator = -1;
            reason = Failure.CONFIG_ENDPOINT_PORT;
            int port = portSeparator < 0 ? 3306 : Integer.parseInt(authority.substring(portSeparator + 1));
            if (port < 1 || port > 65535) throw invalid(reason);
            reason = Failure.CONFIG_ENDPOINT_HOST;
            String host = uri.getHost();
            if (host == null) throw invalid(reason);
            // Exact legacy alias only. Never consult DNS, the hosts file or the
            // JVM resolver for this name; the new DAO always receives a numeric
            // IPv4 loopback address, with no inherited legacy URL options.
            if (host.equalsIgnoreCase("localhost")) return new Endpoint("127.0.0.1", port);
            if (host.startsWith("[")) host = host.substring(1, host.length() - 1);
            InetAddress numeric;
            if (host.contains(":")) numeric = InetAddress.getByName(host); // Colon-only numeric grammar above: no DNS.
            else {
                String[] octets = host.split("\\.", -1);
                if (octets.length != 4) throw invalid(reason);
                byte[] bytes = new byte[4];
                for (int index = 0; index < 4; index++) bytes[index] = (byte) number(octets[index], 0, 255);
                numeric = InetAddress.getByAddress(bytes);
            }
            if (!numeric.isLoopbackAddress()) throw invalid(reason);
            return new Endpoint(numeric.getHostAddress(), port);
        } catch (Exception failure) { throw new ProbeException(reason, Stage.ENDPOINT, "URL1_LOGIN"); }
    }

    private static Path childFile(Path directory, String configured) throws Exception {
        Path child;
        try { child = Path.of(configured); }
        catch (java.nio.file.InvalidPathException failure) { throw invalid(Failure.CONFIG_PATH); }
        for (Path part : child) if (part.toString().equals("..") || part.toString().equals(".")) throw invalid(Failure.CONFIG_PATH);
        Path absolute = child.isAbsolute() ? child : directory.resolve(child);
        if (!absolute.normalize().startsWith(directory)) throw invalid(Failure.CONFIG_PATH);
        return checkedFile(absolute);
    }
    private static Path checkedFile(Path input) throws Exception {
        String spelling = input.toString();
        if (!input.isAbsolute() || spelling.startsWith("\\\\") || spelling.startsWith("//")) throw invalid(Failure.CONFIG_PATH);
        for (Path part : input) {
            String name = part.toString();
            if (name.equals(".") || name.equals("..") || name.contains(":") || name.chars().anyMatch(Character::isISOControl)) throw invalid(Failure.CONFIG_PATH);
        }
        Path path = input.normalize();
        for (Path current = path; current != null; current = current.getParent()) {
            var attributes = Files.readAttributes(current, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (attributes.isSymbolicLink() || attributes.isOther()
                    || (current.equals(path) ? !attributes.isRegularFile() : !attributes.isDirectory())) throw invalid(Failure.CONFIG_PATH);
        }
        return path;
    }
    static Properties readProperties(Path path) throws Exception {
        Properties properties = new Properties() {
            @Override public synchronized Object put(Object key, Object value) {
                if (!(key instanceof String text) || text.length() > 128
                        || !(value instanceof String content) || content.length() > MAX_FILE_BYTES) {
                    throw new IllegalArgumentException("invalid probe properties");
                }
                if (containsKey(key)) throw new DuplicateKey(text);
                return super.put(key, value);
            }
        };
        ByteBuffer bytes = ByteBuffer.allocate(MAX_FILE_BYTES + 1);
        try (var channel = Files.newByteChannel(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            while (bytes.hasRemaining() && channel.read(bytes) != -1) {}
            if (bytes.position() > MAX_FILE_BYTES) throw invalid(Failure.CONFIG_TOO_LARGE);
            bytes.flip();
            var decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
            String contents = decoder.decode(bytes).toString();
            if (contents.startsWith("\ufeff")) contents = contents.substring(1);
            properties.load(new StringReader(contents));
            return properties;
        } catch (ProbeException failure) {
            properties.clear();
            throw failure;
        } catch (java.nio.charset.CharacterCodingException failure) {
            properties.clear();
            throw invalid(Failure.CONFIG_UTF8);
        } catch (DuplicateKey failure) {
            properties.clear();
            throw new ProbeException(Failure.CONFIG_DUPLICATE_KEY, Stage.UNSPECIFIED, failure.key);
        } catch (IllegalArgumentException failure) {
            properties.clear();
            throw invalid(Failure.CONFIG_SYNTAX);
        } catch (IOException failure) {
            properties.clear();
            throw invalid(Failure.CONFIG_IO);
        } catch (Exception failure) {
            properties.clear();
            throw invalid(Failure.CONFIGURATION);
        } finally { Arrays.fill(bytes.array(), (byte) 0); }
    }

    private static final class DuplicateKey extends IllegalArgumentException {
        final String key;
        DuplicateKey(String key) { super("duplicate probe property"); this.key = diagnosticKey(key); }
    }

    static String renderFailure(ProbeException failure) {
        return "{\"probe\":\"failed\",\"reason\":" + json(failure.failure.name())
                + ",\"stage\":" + json(failure.stage.name())
                + ",\"key\":" + (failure.key == null ? "null" : json(failure.key)) + "}";
    }

    /** Only known schema keys, never arbitrary input text, may appear in diagnostics. */
    private static String diagnosticKey(String key) {
        if (key == null) return null;
        if (DATABASE_KEYS.contains(key) || Set.of("database.mode", "source.servers.file",
                "database.connection.properties", "source.count", "LITO_MODERN_PROBE_ACCOUNT",
                "LITO_MODERN_PROBE_PASSWORD").contains(key)
                || key.matches("source\\.(?:[1-9]|1[0-6])\\.(?:id|name|database|weight\\.rate|resource\\.database)")) return key;
        return "UNRECOGNIZED_KEY";
    }

    static String render(Source source, Inspection inspection, Outcome outcome) {
        StringBuilder output = new StringBuilder("{\"source_id\":").append(source.id)
                .append(",\"source_name\":").append(json(source.name)).append(",\"schema\":").append(json(source.database))
                .append(",\"legacy_options_not_inherited\":true,\"schema_check\":").append(json(inspection.schema.name()))
                .append(",\"charset_check\":").append(json(inspection.charset.name()))
                .append(",\"declared_connector_server_range\":").append(inspection.declaredDriverVersionRange)
                .append(",\"login_ok\":").append(outcome.authenticated).append(",\"status\":").append(json(outcome.status.name()))
                .append(",\"character_count\":").append(outcome.rows.size()).append(",\"characters\":[");
        boolean first = true;
        for (var row : outcome.rows) {
            if (!first) output.append(',');
            first = false;
            output.append("{\"objid\":").append(row.objectId()).append(",\"class\":").append(row.characterClass())
                    .append(",\"sex\":").append(row.sex()).append(",\"level\":").append(row.level()).append('}');
        }
        return output.append("]}").toString();
    }
    private static String json(String text) {
        StringBuilder result = new StringBuilder("\"");
        for (char value : text.toCharArray()) {
            if (value == '\\' || value == '"') result.append('\\').append(value);
            else if (value < 32) result.append(String.format("\\u%04x", (int) value));
            else result.append(value);
        }
        return result.append('"').toString();
    }
    private static String property(String text) { return text.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r"); }
    private static String required(Properties properties, String key) throws ProbeException {
        String value = properties.getProperty(key);
        if (value == null || value.isEmpty()) throw new ProbeException(Failure.CONFIG_REQUIRED_KEY, Stage.UNSPECIFIED, key);
        return value;
    }
    private static int number(String text, int minimum, int maximum) throws ProbeException {
        if (!text.matches("0|[1-9][0-9]{0,9}")) throw invalid(Failure.CONFIG_VALUE);
        try { int value = Integer.parseInt(text); if (value < minimum || value > maximum) throw invalid(Failure.CONFIG_VALUE); return value; }
        catch (NumberFormatException failure) { throw invalid(Failure.CONFIG_VALUE); }
    }
    private static boolean validText(String value, int bytes, boolean secret) {
        return value != null && !value.isEmpty() && value.length() <= bytes && (secret || !value.isBlank())
                && value.chars().noneMatch(Character::isISOControl) && StandardCharsets.UTF_8.newEncoder().canEncode(value)
                && value.getBytes(StandardCharsets.UTF_8).length <= bytes;
    }
    private static ProbeException invalid(Failure failure) { return new ProbeException(failure); }
    private static SQLException unavailable() { return new SQLException("modern read probe metadata unavailable"); }
}
