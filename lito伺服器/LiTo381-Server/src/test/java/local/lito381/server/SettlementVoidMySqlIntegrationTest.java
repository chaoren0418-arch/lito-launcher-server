package local.lito381.server;

import static local.lito381.server.DurableSettlementCiFixture.CONTROLS;
import static local.lito381.server.DurableSettlementCiFixture.KNIGHT;
import static local.lito381.server.DurableSettlementCiFixture.LOSS_REWARD;
import static local.lito381.server.DurableSettlementCiFixture.WIN_REWARD;
import static local.lito381.server.DurableSettlementCiFixture.WIZARD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import local.lito381.server.DurableSettlementCiFixture.CrashDriver;
import local.lito381.server.DurableSettlementCiFixture.SimulatedCrash;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * T136 (#387): voiding a pending settlement result whose source character was renamed or deleted, through the
 * startup path ({@link DurableBattleSettlement#recover}) against the disposable CI MySQL fixture. Same safety gate
 * as {@code DisconnectSettlementRecoveryMySqlIntegrationTest}: skipped unless config/server.properties is the CI account.
 */
final class SettlementVoidMySqlIntegrationTest {
    private static Properties config;
    @TempDir Path temporary;
    private DurableSettlementCiFixture fixture;
    /** Receipts this test created; any still pending at the end are acknowledged so later drainAll calls are not blocked. */
    private final List<String> created = new ArrayList<>();

    @BeforeAll
    static void requireCiFixture() throws Exception {
        config = DurableSettlementCiFixture.requireCiConfig();
    }

    @BeforeEach
    void open() throws Exception {
        fixture = new DurableSettlementCiFixture(config, temporary.resolve("fixture"));
    }

    @AfterEach
    void close() throws Exception {
        if (fixture == null) return;
        try {
            CrashDriver.reset();
            fixture.restart();
            List<String> pending = new ArrayList<>();
            for (String id : created) if (receipt(id).applied() == SettlementSql.PENDING) pending.add(id);
            fixture.acknowledge(pending);
        } finally {
            fixture.close();
        }
    }

    private static int period() {
        return GameStateStore.rankingPeriodKeyAt(System.currentTimeMillis());
    }

    private static SettlementSql.Counters next(SettlementSql.Counters c, SettlementJournal.Entry e, int period) {
        return SettlementSql.next(c, e.won(), e.disconnected(), WIN_REWARD, LOSS_REWARD, period);
    }

    private SettlementJournal.Entry entry(String battle, String database, long character, String frozenName,
            boolean won, boolean disconnected) {
        var e = new SettlementJournal.Entry(SettlementJournal.Entry.key(battle, database, character), battle, database,
                DurableSettlementCiFixture.source(database), CONTROLS, character, frozenName, won, disconnected);
        created.add(e.id());
        return e;
    }

    record Receipt(int applied, Timestamp voidedAt, String reason) {}

    private Receipt receipt(String id) throws SQLException {
        try (Connection c = fixture.route("380_ac_1").connection(); PreparedStatement p = c.prepareStatement(
                "SELECT applied,voided_at,void_reason FROM `" + CONTROLS + "`.`lito_battle_settlement` WHERE result_id=?")) {
            p.setString(1, id);
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? new Receipt(r.getInt(1), r.getTimestamp(2), r.getString(3)) : new Receipt(-1, null, null);
            }
        }
    }

    private static void request(Path wal, String text) throws Exception {
        Files.createDirectories(wal);
        Files.writeString(wal.resolve(SettlementJournal.VOID_REQUESTS), text, StandardCharsets.UTF_8);
    }

    private static void recoverAndClose(Path wal) throws Exception {
        try (SettlementJournal journal = DurableBattleSettlement.recover(wal)) {
            assertEquals(0, journal.pending(), "every WAL file is consumed after recovery");
        }
    }

    private static String audit(Path wal) throws Exception {
        Path log = wal.resolve(SettlementJournal.VOID_AUDIT);
        return Files.exists(log) ? Files.readString(log, StandardCharsets.UTF_8) : "";
    }

    @ParameterizedTest(name = "character_lito in {0}")
    @ValueSource(strings = {"380_ac_1", "380_ac_2"}) // MyISAM, InnoDB
    void voidedRenamedResultsLetStartupSucceedWithoutTouchingOtherResults(String database) throws Exception {
        int now = period();
        SettlementSql.Counters knightBase = new SettlementSql.Counters(1, 1, 100, 0, now, 100, 1, 1);
        SettlementSql.Counters wizardBase = new SettlementSql.Counters(2, 2, 200, 0, now, 200, 2, 2);
        fixture.seed(database, KNIGHT, knightBase, 5);
        fixture.seed(database, WIZARD, wizardBase, 6);
        String r = UUID.randomUUID().toString(), m = UUID.randomUUID().toString(), l = UUID.randomUUID().toString(),
                w = UUID.randomUUID().toString();
        // Frozen while the Knight was still "CiKnightRenamed"; the source now says "CiKnight".
        var renamed = entry(r, database, KNIGHT, "CiKnightRenamed", false, true);
        var renamedMixed = entry(m, database, KNIGHT, "CiKnightRenamed", false, false);
        var wizardMixed = entry(m, database, WIZARD, "CiWizard", true, false);
        // A later, valid Knight result: drain walks the Knight's receipts in sequence order and stops at a refused one.
        var knightLater = entry(l, database, KNIGHT, "CiKnight", true, false);
        var wizardAlone = entry(w, database, WIZARD, "CiWizard", true, false);
        Path wal = temporary.resolve("battle-settlements");
        try (SettlementJournal journal = new SettlementJournal(wal)) {
            journal.save(List.of(renamed));
            journal.save(List.of(renamedMixed, wizardMixed));
            journal.save(List.of(knightLater));
            journal.save(List.of(wizardAlone));
        }

        // 1. Before any void: startup is refused and the log names the result id to void.
        SQLException refused = assertThrows(SQLException.class, () -> DurableBattleSettlement.recover(wal));
        String all = refused.getMessage() + List.of(refused.getSuppressed()).stream().map(Throwable::getMessage).toList();
        assertTrue(all.contains("identity changed"), all);
        assertTrue(all.contains("result=" + renamed.id()) || all.contains("result=" + renamedMixed.id()), all);

        // 2. The operator lists both refused results and restarts.
        fixture.restart();
        request(wal, "# 2026-11-14 角色已改名，營運確認作廢\n" + renamed.id() + " renamed in source, ticket-1\n"
                + renamedMixed.id() + "\n");
        recoverAndClose(wal);

        SettlementSql.Counters knightAfter = next(knightBase, knightLater, now);
        SettlementSql.Counters wizardAfter = next(next(wizardBase, wizardMixed, now), wizardAlone, now);
        assertEquals(knightAfter, fixture.counters(database, KNIGHT), "only the valid Knight result is counted");
        assertEquals(6, fixture.version(database, KNIGHT));
        assertEquals(knightLater.id(), fixture.lastResult(database, KNIGHT));
        assertEquals(wizardAfter, fixture.counters(database, WIZARD), "the partner result in the same file is counted");
        assertEquals(8, fixture.version(database, WIZARD));
        Receipt voided = receipt(renamed.id());
        assertEquals(SettlementSql.VOIDED, voided.applied());
        assertNotNull(voided.voidedAt());
        assertEquals("renamed in source, ticket-1", voided.reason());
        assertEquals(SettlementSql.VOIDED, receipt(renamedMixed.id()).applied());
        assertEquals("identity changed in source database", receipt(renamedMixed.id()).reason());
        for (var e : List.of(wizardMixed, knightLater, wizardAlone)) {
            Receipt ok = receipt(e.id());
            assertEquals(SettlementSql.APPLIED, ok.applied(), e.name());
            assertNull(ok.voidedAt());
        }
        // Audit trail: copies of both WAL files holding a voided row, the audit log, the consumed request list.
        Path voidedDir = wal.resolve(SettlementJournal.VOIDED_DIRECTORY);
        try (var files = Files.list(voidedDir)) {
            List<String> names = files.map(p -> p.getFileName().toString()).toList();
            assertEquals(2, names.stream().filter(n -> n.endsWith(".pending")).count(), names.toString());
            assertEquals(1, names.stream().filter(n -> n.startsWith("void-requests-")).count(), names.toString());
        }
        assertFalse(Files.exists(wal.resolve(SettlementJournal.VOID_REQUESTS)));
        String log = audit(wal);
        assertTrue(log.contains("\tvoided\t" + renamed.id() + "\t" + database + "/" + KNIGHT + "\tfrozen=CiKnightRenamed\tnow=CiKnight"), log);
        assertTrue(log.contains("\tvoided\t" + renamedMixed.id()), log);

        // 3. Voiding again is idempotent: same receipts, same counters, same voided_at.
        fixture.restart();
        request(wal, renamed.id() + " second attempt\n" + renamedMixed.id() + "\n");
        recoverAndClose(wal);
        assertEquals(voided, receipt(renamed.id()), "an already voided receipt keeps its first audit values");
        assertTrue(audit(wal).contains("\talready-voided\t" + renamed.id()), audit(wal));
        // A stale ticket or a leftover WAL copy delivering the voided rows again changes nothing.
        SettlementSql.apply(List.of(renamed));
        SettlementSql.apply(List.of(renamedMixed, wizardMixed));
        SettlementSql.drainAll();
        assertEquals(knightAfter, fixture.counters(database, KNIGHT));
        assertEquals(6, fixture.version(database, KNIGHT));
        assertEquals(wizardAfter, fixture.counters(database, WIZARD));
        assertEquals(8, fixture.version(database, WIZARD));
    }

    @Test
    void unsafeVoidRequestsRefuseStartupAndChangeNothing() throws Exception {
        String database = "380_ac_1";
        int now = period();
        SettlementSql.Counters knightBase = new SettlementSql.Counters(3, 3, 300, 1, now, 300, 3, 3);
        fixture.seed(database, KNIGHT, knightBase, 9);
        Path wal = temporary.resolve("battle-settlements");

        // A valid result whose receipt is committed but not yet counted (process died before character_lito).
        var valid = entry(UUID.randomUUID().toString(), database, KNIGHT, "CiKnight", true, false);
        try (SettlementJournal journal = new SettlementJournal(wal)) {
            journal.save(List.of(valid));
            CrashDriver.arm(CrashDriver.Point.COUNTER_BEFORE, 0);
            assertThrows(SimulatedCrash.class, () -> SettlementSql.apply(List.of(valid)));
        }
        CrashDriver.reset();
        fixture.restart();
        assertEquals(SettlementSql.PENDING, receipt(valid.id()).applied());

        // (a) identity unchanged: the result would settle normally, so it may not be voided.
        request(wal, valid.id() + "\n");
        SQLException unchanged = assertThrows(SQLException.class, () -> DurableBattleSettlement.recover(wal));
        assertTrue(unchanged.getMessage().contains("unchanged"), unchanged.getMessage());
        assertEquals(new Receipt(SettlementSql.PENDING, null, null), receipt(valid.id()));
        assertTrue(Files.exists(wal.resolve(SettlementJournal.VOID_REQUESTS)), "a refused list stays for the operator");
        assertFalse(Files.exists(wal.resolve(SettlementJournal.VOIDED_DIRECTORY)), "a refused request leaves no audit copy");
        assertEquals(knightBase, fixture.counters(database, KNIGHT));
        Files.delete(wal.resolve(SettlementJournal.VOID_REQUESTS));
        fixture.restart();
        recoverAndClose(wal);
        SettlementSql.Counters knightAfter = next(knightBase, valid, now);
        assertEquals(knightAfter, fixture.counters(database, KNIGHT));

        // (b) already counted (applied=1).
        request(wal, valid.id() + "\n");
        SQLException counted = assertThrows(SQLException.class, () -> DurableBattleSettlement.recover(wal));
        assertTrue(counted.getMessage().contains("already counted"), counted.getMessage());
        assertEquals(SettlementSql.APPLIED, receipt(valid.id()).applied());

        // (c) unknown result id.
        String unknown = HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)));
        request(wal, unknown + "\n");
        SQLException missing = assertThrows(SQLException.class, () -> DurableBattleSettlement.recover(wal));
        assertTrue(missing.getMessage().contains("unknown result"), missing.getMessage());

        // (d) renamed, but character_lito already carries this result (written, acknowledgement lost): not voidable.
        var renamed = entry(UUID.randomUUID().toString(), database, KNIGHT, "CiKnightRenamed", false, false);
        Files.delete(wal.resolve(SettlementJournal.VOID_REQUESTS));
        try (SettlementJournal journal = new SettlementJournal(wal)) {
            journal.save(List.of(renamed));
        }
        assertThrows(SQLException.class, () -> DurableBattleSettlement.recover(wal));
        setLastResult(database, KNIGHT, renamed.id());
        request(wal, renamed.id() + "\n");
        SQLException inRow = assertThrows(SQLException.class, () -> DurableBattleSettlement.recover(wal));
        assertTrue(inRow.getMessage().contains("already in character_lito"), inRow.getMessage());
        assertEquals(SettlementSql.PENDING, receipt(renamed.id()).applied());
        // Restore the marker; now the same request is safe and startup succeeds.
        setLastResult(database, KNIGHT, valid.id());
        fixture.restart();
        recoverAndClose(wal);
        assertEquals(SettlementSql.VOIDED, receipt(renamed.id()).applied());
        assertEquals(knightAfter, fixture.counters(database, KNIGHT), "a voided result is never counted");
        assertEquals(10, fixture.version(database, KNIGHT));
        assertEquals("", audit(wal).lines().filter(line -> line.contains(valid.id()) || line.contains(unknown))
                .reduce("", String::concat), "refused requests are not written as voided");
    }

    @Test
    void malformedRequestListRefusesStartupBeforeAnyDatabaseChange() throws Exception {
        Path wal = temporary.resolve("battle-settlements");
        request(wal, "not-a-result-id\n");
        Exception refused = assertThrows(Exception.class, () -> DurableBattleSettlement.recover(wal));
        assertTrue(refused.getMessage().contains("line 1"), refused.getMessage());
        assertTrue(Files.exists(wal.resolve(SettlementJournal.VOID_REQUESTS)));
    }

    /** Test-only stand-in for "counter row written, acknowledgement lost"; the CI account may UPDATE character_lito. */
    private void setLastResult(String database, long character, String resultId) throws SQLException {
        try (Connection c = fixture.route(database).connection();
             PreparedStatement p = c.prepareStatement("UPDATE character_lito SET last_result_id=? WHERE char_obj_id=?")) {
            p.setString(1, resultId);
            p.setLong(2, character);
            assertEquals(1, p.executeUpdate());
        }
    }
}
