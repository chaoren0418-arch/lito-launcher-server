package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** T103b (#335, #310 D3): durable ranking counters — pure rules, no database. */
final class SettlementRankingTest {
    private static final int WIN = 250;
    private static final int LOSS = 50;
    private static final int NOV = 202611;

    @Test
    void winAndLossAddConfiguredPointsAndMonthlyCounters() {
        var c = SettlementSql.Counters.ZERO;
        c = SettlementSql.next(c, true, false, WIN, LOSS, NOV);
        assertEquals(new SettlementSql.Counters(1, 0, 250, 0, NOV, 250, 1, 0), c);
        c = SettlementSql.next(c, false, false, WIN, LOSS, NOV);
        assertEquals(new SettlementSql.Counters(1, 1, 300, 0, NOV, 300, 1, 1), c);
    }

    @Test
    void disconnectSubtractsLossRewardAndNeverGoesBelowZero() {
        var c = new SettlementSql.Counters(4, 2, 120, 1, NOV, 80, 1, 1);
        c = SettlementSql.next(c, false, true, WIN, LOSS, NOV);
        assertEquals(new SettlementSql.Counters(4, 3, 70, 2, NOV, 30, 1, 2), c);
        c = SettlementSql.next(c, false, true, WIN, LOSS, NOV);
        assertEquals(new SettlementSql.Counters(4, 4, 20, 3, NOV, 0, 1, 3), c);
        c = SettlementSql.next(c, false, true, WIN, LOSS, NOV);
        assertEquals(new SettlementSql.Counters(4, 5, 0, 4, NOV, 0, 1, 4), c);
        assertThrows(IllegalArgumentException.class,
                () -> SettlementSql.next(SettlementSql.Counters.ZERO, true, true, WIN, LOSS, NOV));
    }

    @Test
    void newMonthRestartsMonthlyAccumulatorButKeepsLifetimeTotals() {
        var october = new SettlementSql.Counters(10, 5, 2_000, 2, 202610, 900, 4, 3);
        var c = SettlementSql.next(october, true, false, WIN, LOSS, NOV);
        assertEquals(new SettlementSql.Counters(11, 5, 2_250, 2, NOV, 250, 1, 0), c);
        // A row that never had a month (fresh upgrade: monthly_period=0) also starts clean.
        var upgraded = new SettlementSql.Counters(3, 3, 0, 0, 0, 0, 0, 0);
        assertEquals(new SettlementSql.Counters(3, 4, 0, 1, NOV, 0, 0, 1),
                SettlementSql.next(upgraded, false, true, WIN, LOSS, NOV));
    }

    @Test
    void rankingMonthTurnsAtFiveAmTaipei() {
        ZoneId taipei = ZoneId.of("Asia/Taipei");
        long before = ZonedDateTime.of(2026, 11, 1, 4, 59, 59, 0, taipei).toInstant().toEpochMilli();
        long at = ZonedDateTime.of(2026, 11, 1, 5, 0, 0, 0, taipei).toInstant().toEpochMilli();
        long newYear = ZonedDateTime.of(2027, 1, 1, 4, 0, 0, 0, taipei).toInstant().toEpochMilli();
        assertEquals(202610, GameStateStore.rankingPeriodKeyAt(before));
        assertEquals(202611, GameStateStore.rankingPeriodKeyAt(at));
        assertEquals(202612, GameStateStore.rankingPeriodKeyAt(newYear));
    }

    @Test
    void durableRuleMatchesInMemoryRecordResult(@TempDir Path temporary) throws Exception {
        // Same rule on both sides: the value the client sees right after a battle equals
        // the value a restarted server reloads from character_lito.
        GameStateStore store = new GameStateStore(temporary.resolve("parity.properties"), false);
        String character = "Parity";
        int period = GameStateStore.rankingPeriodKeyAt(System.currentTimeMillis());
        var durable = SettlementSql.Counters.ZERO;
        boolean[][] results = {{true, false}, {false, false}, {false, true}, {false, true},
                {false, true}, {false, true}, {false, true}, {false, true}, {true, false}};
        for (boolean[] r : results) {
            store.recordResult(character, r[0], r[1], r[0] ? WIN : LOSS, 0);
            durable = SettlementSql.next(durable, r[0], r[1], WIN, LOSS, period);
            assertEquals(durable, SettlementSql.Counters.of(store.tournamentProfileState(character)));
        }
    }

    @Test
    void routeLoadsDurableCountersAndClampsUnrepresentableRows() {
        var route = new SharedDatabase.Route("Sun", "380_ac", "380_lito", credentials(), WIN, LOSS);
        var loaded = route.profile(7, "Knight",
                new SettlementSql.Counters(5, 3, 1_250, 2, NOV, 300, 2, 1), 9).state();
        assertEquals(new SettlementSql.Counters(5, 3, 1_250, 2, NOV, 300, 2, 1),
                SettlementSql.Counters.of(loaded));
        assertEquals("Sun", loaded.classicServerName());
        // Edited outside LiTo: more disconnects than losses, invalid month.
        var clamped = route.profile(8, "Elf",
                new SettlementSql.Counters(1, 1, 10, 5, 202613, 99, 9, 9), 2).state();
        assertEquals(new SettlementSql.Counters(1, 1, 10, 1, 0, 0, 0, 0),
                SettlementSql.Counters.of(clamped));
        assertEquals(250, route.winReward);
        assertEquals(50, route.lossReward);
        assertThrows(IllegalArgumentException.class,
                () -> new SharedDatabase.Route("Sun", "380_ac", "380_lito", credentials(), -1, 0));
    }

    @Test
    void durableOverlayCopiesEveryCounterAndKeepsExperience() throws Exception {
        var route = new SharedDatabase.Route("Sun", "380_ac", "380_lito", credentials(), WIN, LOSS);
        var memory = route.profile(1, "A", SettlementSql.Counters.ZERO, 0).state();
        memory = DurableBattleSettlement.with(memory, Map.of("experience", 4_321L, "level", 5));
        var db = route.profile(1, "A", new SettlementSql.Counters(2, 1, 550, 1, NOV, 550, 2, 1), 3).state();
        var merged = DurableBattleSettlement.durable(memory, db, "Sun");
        assertEquals(SettlementSql.Counters.of(db), SettlementSql.Counters.of(merged));
        assertEquals(4_321L, merged.experience());
        assertEquals(5, merged.level());
        assertTrue(DurableBattleSettlement.sameDurable(merged, db));
    }

    private static Properties credentials() {
        Properties p = new Properties();
        p.setProperty("URL1_LOGIN", "jdbc:mysql://127.0.0.1:1/");
        p.setProperty("Login", "unused");
        return p;
    }
}
