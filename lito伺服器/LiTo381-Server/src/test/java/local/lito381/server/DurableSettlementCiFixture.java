package local.lito381.server;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.Assumptions;

/**
 * T124 (#355): shared setup for the durable-settlement tests that need the disposable CI MySQL
 * fixture (T119, #334). Registers one {@link SharedDatabase.Route} per CI source
 * (380_ac MyISAM, 380_ac_1 MyISAM, 380_ac_2 InnoDB character_lito) and restores the previous
 * {@link SharedDatabase#ROUTES} on close.
 *
 * <p>Safety gate: identical to {@code SettlementRankingMySqlIntegrationTest}; a checkout whose
 * {@code config/server.properties} is not the CI account never reaches a write statement.</p>
 */
final class DurableSettlementCiFixture implements AutoCloseable {
    static final String CONTROLS = "380_lito";
    static final List<String> SOURCES = List.of("380_ac", "380_ac_1", "380_ac_2");
    static final long KNIGHT = 90000001L; // db/ci/fake_source_data.sql
    static final long WIZARD = 90000002L;
    static final int WIN_REWARD = 250;
    static final int LOSS_REWARD = 50;

    static String name(long character) {
        if (character == KNIGHT) return "CiKnight";
        if (character == WIZARD) return "CiWizard";
        throw new IllegalArgumentException("not a CI fixture character: " + character);
    }

    static String source(String database) { return "CI-" + database; }

    /** Loads config/server.properties only when it is the CI fixture; otherwise the caller is skipped. */
    static Properties requireCiConfig() throws Exception {
        LocalResources.assumeServerProperties();
        Properties config = new Properties();
        try (Reader in = Files.newBufferedReader(Path.of("config", "server.properties"), StandardCharsets.UTF_8)) {
            config.load(in);
        }
        String ciUser = System.getenv("LITO_CI_DB_USER");
        Assumptions.assumeTrue(ciUser != null && !ciUser.isBlank()
                        && ciUser.equals(config.getProperty("database.user")),
                "skipped: config/server.properties is not the CI MySQL fixture");
        CrashDriver.install();
        return config;
    }

    private final Properties config;
    private final Map<String, LegacyMySqlRepository> repositories = new LinkedHashMap<>();
    private final Map<LegacyMySqlRepository, SharedDatabase.Route> saved;

    DurableSettlementCiFixture(Properties config, Path directory) throws Exception {
        this.config = config;
        synchronized (SharedDatabase.ROUTES) {
            saved = new LinkedHashMap<>(SharedDatabase.ROUTES);
            SharedDatabase.ROUTES.clear();
        }
        Files.createDirectories(directory);
        Path credentials = directory.resolve("ci-legacy-credentials.properties");
        Properties legacy = new Properties();
        legacy.setProperty("URL1_LOGIN", config.getProperty("database.jdbc.base.url"));
        legacy.setProperty("URL3_LOGIN", config.getProperty("database.jdbc.options", ""));
        legacy.setProperty("Login_LOGIN", config.getProperty("database.user"));
        legacy.setProperty("Password_LOGIN", config.getProperty("database.password", ""));
        // LegacyMySqlRepository evaluates required("Login") eagerly as the Login_LOGIN fallback.
        legacy.setProperty("Login", config.getProperty("database.user"));
        legacy.setProperty("Password", config.getProperty("database.password", ""));
        try (Writer out = Files.newBufferedWriter(credentials, StandardCharsets.UTF_8)) {
            legacy.store(out, "T124 CI fixture credentials (runner temp only)");
        }
        try {
            for (String database : SOURCES) {
                // Lazy pool (minimumIdle 0): no connection is opened by the constructor.
                repositories.put(database, new LegacyMySqlRepository(credentials, source(database),
                        database, database, 100, true, "legacy_mysql"));
            }
        } catch (Exception e) {
            close();
            throw e;
        }
        restart();
    }

    /** Fresh routes with empty runtime caches, i.e. what a restarted process registers. */
    void restart() {
        synchronized (SharedDatabase.ROUTES) {
            SharedDatabase.ROUTES.clear();
            for (var e : repositories.entrySet()) SharedDatabase.ROUTES.put(e.getValue(), newRoute(e.getKey()));
        }
    }

    LegacyMySqlRepository repository(String database) { return repositories.get(database); }

    SharedDatabase.Route route(String database) throws SQLException { return SettlementSql.route(database); }

    private SharedDatabase.Route newRoute(String database) {
        Properties p = new Properties();
        p.setProperty("URL1_LOGIN", CrashDriver.wrap(config.getProperty("database.jdbc.base.url")));
        p.setProperty("URL3_LOGIN", config.getProperty("database.jdbc.options", ""));
        p.setProperty("Login", config.getProperty("database.user"));
        p.setProperty("Password", config.getProperty("database.password", ""));
        return new SharedDatabase.Route(source(database), database, CONTROLS, p, WIN_REWARD, LOSS_REWARD);
    }

    /** Puts a fixture row into a known state; the CI account may INSERT/UPDATE character_lito. */
    void seed(String database, long character, SettlementSql.Counters c, long version) throws SQLException {
        try (Connection connection = route(database).connection(); PreparedStatement p = connection.prepareStatement(
                "INSERT INTO character_lito(char_obj_id,char_name," + SettlementSql.COUNTER_COLUMNS
                        + ",version,last_result_id) VALUES(?,?,?,?,?,?,?,?,?,?,?,'') ON DUPLICATE KEY UPDATE"
                        + " char_name=VALUES(char_name),wins=VALUES(wins),losses=VALUES(losses),"
                        + "ranking_points=VALUES(ranking_points),disconnects=VALUES(disconnects),"
                        + "monthly_period=VALUES(monthly_period),monthly_ranking_points=VALUES(monthly_ranking_points),"
                        + "monthly_wins=VALUES(monthly_wins),monthly_losses=VALUES(monthly_losses),"
                        + "version=VALUES(version),last_result_id=''")) {
            p.setLong(1, character);
            p.setString(2, name(character));
            int i = c.bind(p, 3);
            p.setLong(i, version);
            p.executeUpdate();
        }
    }

    SettlementSql.Counters counters(String database, long character) throws Exception {
        return SettlementSql.Counters.of(SettlementSql.load(route(database), character, name(character)).state());
    }

    long version(String database, long character) throws Exception {
        return SettlementSql.load(route(database), character, name(character)).snapshotVersion();
    }

    String lastResult(String database, long character) throws SQLException {
        try (Connection c = route(database).connection();
             PreparedStatement p = c.prepareStatement("SELECT last_result_id FROM character_lito WHERE char_obj_id=?")) {
            p.setLong(1, character);
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? r.getString(1) : null;
            }
        }
    }

    /** {count, applied} of the receipts with these result ids. */
    long[] receipts(Collection<String> resultIds) throws SQLException {
        if (resultIds.isEmpty()) return new long[] {0, 0};
        String marks = String.join(",", java.util.Collections.nCopies(resultIds.size(), "?"));
        try (Connection c = route(SOURCES.get(0)).connection(); PreparedStatement p = c.prepareStatement(
                "SELECT COUNT(*),COALESCE(SUM(applied),0) FROM `" + CONTROLS + "`.`lito_battle_settlement` WHERE result_id IN ("
                        + marks + ")")) {
            int i = 1;
            for (String id : resultIds) p.setString(i++, id);
            try (ResultSet r = p.executeQuery()) {
                r.next();
                return new long[] {r.getLong(1), r.getLong(2)};
            }
        }
    }

    /** Test cleanup only: marks receipts as handled so later drainAll calls are not blocked by them. */
    void acknowledge(Collection<String> resultIds) throws SQLException {
        try (Connection c = route(SOURCES.get(0)).connection(); PreparedStatement p = c.prepareStatement(
                "UPDATE `" + CONTROLS + "`.`lito_battle_settlement` SET applied=1 WHERE result_id=?")) {
            for (String id : resultIds) {
                p.setString(1, id);
                p.executeUpdate();
            }
        }
    }

    @Override
    public void close() throws Exception {
        CrashDriver.reset();
        synchronized (SharedDatabase.ROUTES) {
            SharedDatabase.ROUTES.clear();
            SharedDatabase.ROUTES.putAll(saved);
        }
        Exception failure = null;
        for (LegacyMySqlRepository repository : repositories.values()) {
            try {
                repository.close();
            } catch (Exception e) {
                failure = e;
            }
        }
        if (failure != null) throw failure;
    }

    /** Simulated process death: an Error, so no {@code catch (Exception)} in the code under test absorbs it. */
    static final class SimulatedCrash extends Error {
        SimulatedCrash(String where) { super("simulated crash at " + where); }
    }

    /**
     * Wraps the real MySQL driver. Once armed it "kills the process" at one point of the settlement
     * write sequence: after that every statement fails, and only {@code close()} reaches the real
     * connection, so MySQL rolls back the open transaction and releases GET_LOCK exactly as for a
     * dropped client.
     */
    static final class CrashDriver implements Driver {
        static final String PREFIX = "jdbc:lito-crash:";
        enum Point { CONNECT, RECEIPT_INSERT, RECEIPT_COMMIT, COUNTER_BEFORE, COUNTER_AFTER, ACK_BEFORE, ACK_AFTER }
        private static final CrashDriver INSTANCE = new CrashDriver();
        private static boolean installed;
        private static volatile Point armed;
        private static volatile boolean dead;
        private static final AtomicInteger skip = new AtomicInteger();

        static synchronized void install() throws SQLException {
            if (!installed) {
                DriverManager.registerDriver(INSTANCE);
                installed = true;
            }
        }

        static String wrap(String url) {
            if (!url.startsWith("jdbc:")) throw new IllegalArgumentException("not a JDBC URL");
            return PREFIX + url.substring("jdbc:".length());
        }

        /** Crash at the (skipMatches+1)-th occurrence of {@code point}. */
        static void arm(Point point, int skipMatches) {
            skip.set(skipMatches);
            dead = false;
            armed = point;
        }

        static void reset() {
            armed = null;
            dead = false;
        }

        static boolean crashed() { return dead; }

        private static void hit(Point point, String where) {
            if (dead) throw new SimulatedCrash("(already dead) " + where);
            if (armed == point && skip.getAndDecrement() <= 0) {
                armed = null;
                dead = true;
                throw new SimulatedCrash(point + ": " + where);
            }
        }

        private static Object invoke(Object target, Method m, Object[] args) throws Throwable {
            try {
                return m.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }

        private static Point[] classify(String sql) {
            if (sql.startsWith("UPDATE character_lito") || sql.startsWith("INSERT INTO character_lito"))
                return new Point[] {Point.COUNTER_BEFORE, Point.COUNTER_AFTER};
            if (sql.contains("SET applied=1")) return new Point[] {Point.ACK_BEFORE, Point.ACK_AFTER};
            if (sql.startsWith("INSERT INTO") && sql.contains("lito_battle_settlement"))
                return new Point[] {Point.RECEIPT_INSERT, null};
            return new Point[] {null, null};
        }

        @Override public boolean acceptsURL(String url) { return url != null && url.startsWith(PREFIX); }

        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            if (!acceptsURL(url)) return null;
            hit(Point.CONNECT, url);
            Connection real = DriverManager.getConnection("jdbc:" + url.substring(PREFIX.length()), info);
            ClassLoader loader = CrashDriver.class.getClassLoader();
            return (Connection) Proxy.newProxyInstance(loader, new Class<?>[] {Connection.class}, (proxy, m, a) -> {
                String name = m.getName();
                if (name.equals("close") || name.equals("isClosed")) return invoke(real, m, a);
                if (dead) throw new SimulatedCrash("(already dead) Connection." + name);
                if (name.equals("commit")) hit(Point.RECEIPT_COMMIT, "commit");
                Object result = invoke(real, m, a);
                if (name.equals("prepareStatement") && result instanceof PreparedStatement statement) {
                    String sql = (String) a[0];
                    Point[] points = classify(sql);
                    return Proxy.newProxyInstance(loader, new Class<?>[] {PreparedStatement.class}, (p2, m2, a2) -> {
                        String n2 = m2.getName();
                        if (n2.equals("close") || n2.equals("isClosed")) return invoke(statement, m2, a2);
                        if (dead) throw new SimulatedCrash("(already dead) " + sql);
                        if (!n2.equals("executeUpdate")) return invoke(statement, m2, a2);
                        if (points[0] != null) hit(points[0], sql);
                        Object updated = invoke(statement, m2, a2);
                        if (points[1] != null) hit(points[1], sql);
                        return updated;
                    });
                }
                return result;
            });
        }

        @Override public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) { return new DriverPropertyInfo[0]; }
        @Override public int getMajorVersion() { return 1; }
        @Override public int getMinorVersion() { return 0; }
        @Override public boolean jdbcCompliant() { return false; }
        @Override public Logger getParentLogger() { return Logger.getGlobal(); }
    }
}
