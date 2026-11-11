package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * T103b (#335): durable ranking counters against the disposable CI MySQL fixture (T119, #334).
 *
 * <p>Runs only when {@code config/server.properties} points at the CI fixture account
 * ({@code LITO_CI_DB_USER} set by the workflow and equal to {@code database.user}); a local
 * checkout with a real deployment config never reaches the write statements below.
 * 380_ac_1 keeps the 1-伺服器 MyISAM character_lito, 380_ac_2 is InnoDB
 * ({@code db/ci/build_ci_database.py}).</p>
 */
final class SettlementRankingMySqlIntegrationTest {
    private static final long CHARACTER = 90000002L; // CiWizard, db/ci/fake_source_data.sql
    private static final String NAME = "CiWizard";
    private static final String CONTROLS = "380_lito";
    private static Properties config;

    @BeforeAll
    static void requireCiFixture() throws Exception {
        LocalResources.assumeServerProperties();
        config = new Properties();
        try (Reader in = Files.newBufferedReader(Path.of("config", "server.properties"),
                StandardCharsets.UTF_8)) {
            config.load(in);
        }
        String ciUser = System.getenv("LITO_CI_DB_USER");
        Assumptions.assumeTrue(ciUser != null && !ciUser.isBlank()
                        && ciUser.equals(config.getProperty("database.user")),
                "skipped: config/server.properties is not the CI MySQL fixture");
    }

    private static SharedDatabase.Route route(String database) {
        Properties p = new Properties();
        p.setProperty("URL1_LOGIN", config.getProperty("database.jdbc.base.url"));
        p.setProperty("URL3_LOGIN", config.getProperty("database.jdbc.options", ""));
        p.setProperty("Login", config.getProperty("database.user"));
        p.setProperty("Password", config.getProperty("database.password", ""));
        return new SharedDatabase.Route("CI", database, CONTROLS, p, 250, 50);
    }

    private static SettlementJournal.Entry entry(String database, boolean won, boolean disconnected) {
        String battle = UUID.randomUUID().toString();
        return new SettlementJournal.Entry(SettlementJournal.Entry.key(battle, database, CHARACTER),
                battle, database, "CI", CONTROLS, CHARACTER, NAME, won, disconnected);
    }

    /** Puts the fixture row into a known state (the CI account may INSERT/UPDATE character_lito). */
    private static void seed(SharedDatabase.Route route, SettlementSql.Counters c, long version)
            throws SQLException {
        try (Connection connection = route.connection(); PreparedStatement p = connection.prepareStatement(
                "INSERT INTO character_lito(char_obj_id,char_name," + SettlementSql.COUNTER_COLUMNS
                        + ",version,last_result_id) VALUES(?,?,?,?,?,?,?,?,?,?,?,'') ON DUPLICATE KEY UPDATE"
                        + " char_name=VALUES(char_name),wins=VALUES(wins),losses=VALUES(losses),"
                        + "ranking_points=VALUES(ranking_points),disconnects=VALUES(disconnects),"
                        + "monthly_period=VALUES(monthly_period),monthly_ranking_points=VALUES(monthly_ranking_points),"
                        + "monthly_wins=VALUES(monthly_wins),monthly_losses=VALUES(monthly_losses),"
                        + "version=VALUES(version),last_result_id=''")) {
            p.setLong(1, CHARACTER);
            p.setString(2, NAME);
            int i = c.bind(p, 3);
            p.setLong(i, version);
            p.executeUpdate();
        }
    }

    private static SettlementSql.Counters counters(SharedDatabase.Route route) throws Exception {
        return SettlementSql.Counters.of(SettlementSql.load(route, CHARACTER, NAME).state());
    }

    private static long version(SharedDatabase.Route route) throws Exception {
        return SettlementSql.load(route, CHARACTER, NAME).snapshotVersion();
    }

    private static void settle(SharedDatabase.Route route, SettlementJournal.Entry e) throws Exception {
        SettlementSql.receipts(route, List.of(e));
        SettlementSql.drain(route, CHARACTER);
    }

    private static void markUnapplied(SharedDatabase.Route route, SettlementJournal.Entry e) throws SQLException {
        try (Connection c = route.connection(); PreparedStatement p = c.prepareStatement(
                "UPDATE `" + CONTROLS + "`.`lito_battle_settlement` SET applied=0 WHERE result_id=?")) {
            p.setString(1, e.id());
            assertEquals(1, p.executeUpdate());
        }
    }

    private static String engine(SharedDatabase.Route route) throws SQLException {
        try (Connection c = route.connection(); PreparedStatement p = c.prepareStatement(
                "SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=? AND TABLE_NAME='character_lito'")) {
            p.setString(1, route.database);
            try (ResultSet r = p.executeQuery()) {
                assertTrue(r.next());
                return r.getString(1);
            }
        }
    }

    @ParameterizedTest(name = "{0} ({1})")
    @CsvSource({"380_ac_1, MyISAM", "380_ac_2, InnoDB"})
    void replayCrossMonthAndDisconnectOnBothEngines(String database, String expectedEngine) throws Exception {
        SharedDatabase.Route route = route(database);
        assertEquals(expectedEngine.toLowerCase(), engine(route).toLowerCase());
        SettlementSql.check(route); // ranking columns exist after db/lito_character_ranking.sql
        int now = GameStateStore.rankingPeriodKeyAt(System.currentTimeMillis());

        // An old month: the next result restarts the monthly accumulator.
        seed(route, new SettlementSql.Counters(3, 2, 500, 1, 200001, 999, 3, 2), 7);
        SettlementJournal.Entry win = entry(database, true, false);
        settle(route, win);
        SettlementSql.Counters afterWin = new SettlementSql.Counters(4, 2, 750, 1, now, 250, 1, 0);
        assertEquals(afterWin, counters(route));
        assertEquals(8, version(route));

        // Replaying the same frozen result (journal replay / retry) never adds twice.
        settle(route, win);
        assertEquals(afterWin, counters(route));
        // Counter row written but receipt acknowledgement lost: last_result_id stops the second add.
        markUnapplied(route, win);
        SettlementSql.drain(route, CHARACTER);
        assertEquals(afterWin, counters(route));
        assertEquals(8, version(route));

        // Disconnect: a loss, one more disconnect, minus the loss reward.
        SettlementJournal.Entry dropped = entry(database, false, true);
        settle(route, dropped);
        assertEquals(new SettlementSql.Counters(4, 3, 700, 2, now, 200, 1, 1), counters(route));
        markUnapplied(route, dropped);
        SettlementSql.drain(route, CHARACTER);
        assertEquals(new SettlementSql.Counters(4, 3, 700, 2, now, 200, 1, 1), counters(route));

        // Floor at 0 for both lifetime and monthly points.
        seed(route, new SettlementSql.Counters(4, 3, 10, 2, now, 30, 1, 1), 20);
        settle(route, entry(database, false, true));
        assertEquals(new SettlementSql.Counters(4, 4, 0, 3, now, 0, 1, 2), counters(route));
        assertEquals(21, version(route));

        // Restart: a new Route (empty runtime cache) loads the persisted points instead of 0.
        var reloaded = SettlementSql.load(route(database), CHARACTER, NAME).state();
        assertEquals(0, reloaded.rankingPoints());
        assertEquals(3, reloaded.disconnects());
        assertEquals(now, reloaded.monthlyPeriod());
        assertEquals(2, reloaded.monthlyLosses());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"380_ac_1", "380_ac_2"})
    void directSettlementPathWritesTheSameColumns(String database) throws Exception {
        SharedDatabase.Route route = route(database);
        int now = GameStateStore.rankingPeriodKeyAt(System.currentTimeMillis());
        seed(route, new SettlementSql.Counters(1, 1, 300, 0, now, 300, 1, 1), 3);
        RepositoryTournamentProfile expected = SettlementSql.load(route, CHARACTER, NAME);
        // What GameStateStore.recordResult produces for a disconnect on top of the baseline.
        var replacement = DurableBattleSettlement.durable(expected.state(), route.profile(CHARACTER, NAME,
                new SettlementSql.Counters(1, 2, 250, 1, now, 250, 1, 2), 3).state(), "CI");
        RepositoryTournamentMutation result = SharedDatabase.persist(route, CHARACTER, NAME,
                expected, replacement);
        assertEquals(RepositoryTournamentMutationStatus.SUCCESS, result.status());
        assertEquals(new SettlementSql.Counters(1, 2, 250, 1, now, 250, 1, 2), counters(route));
        assertEquals(4, version(route));
        // A replacement that adds a disconnect without a loss is refused.
        RepositoryTournamentProfile current = SettlementSql.load(route, CHARACTER, NAME);
        var bogus = DurableBattleSettlement.durable(current.state(), route.profile(CHARACTER, NAME,
                new SettlementSql.Counters(2, 2, 500, 2, now, 500, 2, 2), 4).state(), "CI");
        assertThrows(SQLException.class,
                () -> SharedDatabase.persist(route, CHARACTER, NAME, current, bogus));
        assertEquals(new SettlementSql.Counters(1, 2, 250, 1, now, 250, 1, 2), counters(route));
    }
}
