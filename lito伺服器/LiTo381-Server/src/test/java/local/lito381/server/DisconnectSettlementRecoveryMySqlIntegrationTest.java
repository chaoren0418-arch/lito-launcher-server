package local.lito381.server;

import static local.lito381.server.DurableSettlementCiFixture.CONTROLS;
import static local.lito381.server.DurableSettlementCiFixture.KNIGHT;
import static local.lito381.server.DurableSettlementCiFixture.LOSS_REWARD;
import static local.lito381.server.DurableSettlementCiFixture.SOURCES;
import static local.lito381.server.DurableSettlementCiFixture.WIN_REWARD;
import static local.lito381.server.DurableSettlementCiFixture.WIZARD;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import local.lito381.server.DurableSettlementCiFixture.CrashDriver;
import local.lito381.server.DurableSettlementCiFixture.SimulatedCrash;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * T124 (#355): disconnect settlement under process abort/restart and under heavy duplicate delivery,
 * against the disposable CI MySQL fixture. The battle-side freeze is the production
 * {@link DurableBattleSettlement#entry}; the write side is the production
 * {@link SettlementJournal} + {@link SettlementSql} (receipt → counter row with last_result_id → ack).
 */
final class DisconnectSettlementRecoveryMySqlIntegrationTest {
    private static Properties config;
    @TempDir Path temporary;
    private DurableSettlementCiFixture fixture;

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
        fixture.close();
    }

    private static int period() {
        return GameStateStore.rankingPeriodKeyAt(System.currentTimeMillis());
    }

    private static SettlementSql.Counters next(SettlementSql.Counters c, SettlementJournal.Entry e, int period) {
        return SettlementSql.next(c, e.won(), e.disconnected(), WIN_REWARD, LOSS_REWARD, period);
    }

    private static SettlementJournal.Entry frozen(String battle, long runtimeId, String database, long character,
            BattleRules.Resolution resolution, Set<Long> disconnected) {
        return DurableBattleSettlement.entry(battle, runtimeId, database, DurableSettlementCiFixture.source(database),
                CONTROLS, character, DurableSettlementCiFixture.name(character), resolution, disconnected);
    }

    /** What DurableBattleSettlement.open does on startup: replay every WAL file, then drain stray receipts. */
    private static void startupRecovery(Path journalDirectory) throws Exception {
        try (SettlementJournal journal = new SettlementJournal(journalDirectory)) {
            journal.replay(SettlementSql::apply);
            SettlementSql.drainAll();
            assertEquals(0, journal.pending(), "every WAL file is consumed after recovery");
        }
    }

    // ---------------------------------------------------------------- 2. restart recovery

    enum Crash {
        /** Battle ended but the process died before the WAL save: nothing durable exists. */
        BEFORE_JOURNAL,
        /** WAL saved; died before the first SQL connection. */
        JOURNAL_ONLY,
        /** Died at COMMIT of the receipt transaction (receipts rolled back). */
        RECEIPT_TXN_LOST,
        /** Receipt committed; died before the character_lito write. */
        RECEIPT_COMMITTED,
        /** character_lito UPDATE executed (wins/points/disconnects + last_result_id); died before the reply. */
        COUNTER_WRITTEN_REPLY_LOST,
        /** Counter row written; died before the receipt acknowledgement (applied=1). */
        ACK_LOST,
        /** All SQL done by the WAL replay; died before the WAL file was deleted. */
        ACKED_JOURNAL_NOT_DELETED,
        /** Two-player battle: winner fully written, died before the disconnected loser's counter write. */
        MID_BATCH,
        /** Receipts committed by the replay batch, then the WAL directory was lost with the process. */
        JOURNAL_LOST_RECEIPTS_SURVIVE,
        /** Died after a counter write, and died again at the next counter write during recovery. */
        CRASH_AGAIN_DURING_RECOVERY
    }

    static Stream<Arguments> crashes() {
        List<Arguments> out = new ArrayList<>();
        for (Crash crash : Crash.values()) {
            out.add(Arguments.of(crash, "380_ac_1")); // MyISAM character_lito
            out.add(Arguments.of(crash, "380_ac_2")); // InnoDB character_lito
        }
        return out.stream();
    }

    /** Each player's ClientSession.recordBattleResult → DurableBattleSettlement.record → SettlementSql.apply(one). */
    private static void liveRecordUntilCrash(List<SettlementJournal.Entry> entries) {
        assertThrows(SimulatedCrash.class, () -> {
            for (var e : entries) SettlementSql.apply(List.of(e));
        });
        assertTrue(CrashDriver.crashed());
    }

    @ParameterizedTest(name = "{0} on {1}")
    @MethodSource("crashes")
    void crashAndRestartSettleEveryFrozenResultExactlyOnce(Crash crash, String database) throws Exception {
        int now = period();
        SettlementSql.Counters knightBase = new SettlementSql.Counters(10, 5, 5000, 2, now, 1000, 3, 1);
        SettlementSql.Counters wizardBase = new SettlementSql.Counters(4, 8, 3000, 3, now, 800, 1, 2);
        fixture.seed(database, KNIGHT, knightBase, 30);
        fixture.seed(database, WIZARD, wizardBase, 40);

        // The Wizard's socket closed mid-battle; the Knight is the only one left and wins.
        String battle = UUID.randomUUID().toString();
        var resolution = new BattleRules.Resolution(true, false, Set.of(1L), Set.of(2L), "combatant-disconnected");
        var knight = frozen(battle, 1, database, KNIGHT, resolution, Set.of(2L));
        var wizard = frozen(battle, 2, database, WIZARD, resolution, Set.of(2L));
        assertTrue(knight.won() && !knight.disconnected());
        assertTrue(!wizard.won() && wizard.disconnected());
        List<SettlementJournal.Entry> entries = List.of(knight, wizard);

        Path wal = temporary.resolve("battle-settlements");
        SettlementJournal journal = new SettlementJournal(wal);
        try {
            switch (crash) {
                case BEFORE_JOURNAL -> { /* freeze never reached journal.save */ }
                case JOURNAL_ONLY -> { journal.save(entries); CrashDriver.arm(CrashDriver.Point.CONNECT, 0); liveRecordUntilCrash(entries); }
                case RECEIPT_TXN_LOST -> { journal.save(entries); CrashDriver.arm(CrashDriver.Point.RECEIPT_COMMIT, 0); liveRecordUntilCrash(entries); }
                case RECEIPT_COMMITTED -> { journal.save(entries); CrashDriver.arm(CrashDriver.Point.COUNTER_BEFORE, 0); liveRecordUntilCrash(entries); }
                case COUNTER_WRITTEN_REPLY_LOST, CRASH_AGAIN_DURING_RECOVERY -> {
                    journal.save(entries); CrashDriver.arm(CrashDriver.Point.COUNTER_AFTER, 0); liveRecordUntilCrash(entries);
                }
                case ACK_LOST -> { journal.save(entries); CrashDriver.arm(CrashDriver.Point.ACK_BEFORE, 0); liveRecordUntilCrash(entries); }
                case MID_BATCH -> { journal.save(entries); CrashDriver.arm(CrashDriver.Point.COUNTER_BEFORE, 1); liveRecordUntilCrash(entries); }
                case ACKED_JOURNAL_NOT_DELETED -> {
                    journal.save(entries);
                    CrashDriver.arm(CrashDriver.Point.ACK_AFTER, 1); // after the second (= last) acknowledgement
                    assertThrows(SimulatedCrash.class, () -> journal.replay(SettlementSql::apply));
                }
                case JOURNAL_LOST_RECEIPTS_SURVIVE -> {
                    journal.save(entries);
                    CrashDriver.arm(CrashDriver.Point.COUNTER_BEFORE, 0);
                    // Background worker path: receipts for the whole battle in one transaction, then drain.
                    assertThrows(SimulatedCrash.class, () -> SettlementSql.apply(entries));
                }
            }
        } finally {
            journal.close(); // the OS drops the owner lock of a dead process
        }
        if (crash == Crash.JOURNAL_LOST_RECEIPTS_SURVIVE) {
            try (var files = Files.list(wal)) {
                for (Path p : files.filter(f -> f.toString().endsWith(".pending")).toList()) Files.delete(p);
            }
        }

        // Restart: new routes (empty runtime caches), healthy database.
        CrashDriver.reset();
        fixture.restart();
        if (crash == Crash.CRASH_AGAIN_DURING_RECOVERY) {
            CrashDriver.arm(CrashDriver.Point.COUNTER_AFTER, 0);
            assertThrows(SimulatedCrash.class, () -> startupRecovery(wal));
            CrashDriver.reset();
            fixture.restart();
        }
        startupRecovery(wal);

        if (crash == Crash.BEFORE_JOURNAL) {
            // Current behaviour: a result that was never frozen to disk is gone; the match counts for nobody.
            assertEquals(knightBase, fixture.counters(database, KNIGHT));
            assertEquals(wizardBase, fixture.counters(database, WIZARD));
            assertEquals(30, fixture.version(database, KNIGHT));
            assertEquals(40, fixture.version(database, WIZARD));
            assertArrayEquals(new long[] {0, 0}, fixture.receipts(List.of(knight.id(), wizard.id())));
            return;
        }
        SettlementSql.Counters knightAfter = next(knightBase, knight, now);
        SettlementSql.Counters wizardAfter = next(wizardBase, wizard, now);
        assertEquals(new SettlementSql.Counters(11, 5, 5250, 2, now, 1250, 4, 1), knightAfter);
        assertEquals(new SettlementSql.Counters(4, 9, 2950, 4, now, 750, 1, 3), wizardAfter);
        assertSettledOnce(database, knight, knightAfter, 31);
        assertSettledOnce(database, wizard, wizardAfter, 41);

        // A further restart (e.g. operator restarts again) changes nothing.
        fixture.restart();
        startupRecovery(wal);
        for (var e : entries) SettlementSql.apply(List.of(e)); // late retry from a stale ticket
        assertSettledOnce(database, knight, knightAfter, 31);
        assertSettledOnce(database, wizard, wizardAfter, 41);
    }

    private void assertSettledOnce(String database, SettlementJournal.Entry e, SettlementSql.Counters expected,
            long version) throws Exception {
        assertEquals(expected, fixture.counters(database, e.character()), "counters " + e.name());
        assertEquals(version, fixture.version(database, e.character()), "exactly one version step " + e.name());
        assertEquals(e.id(), fixture.lastResult(database, e.character()));
        assertArrayEquals(new long[] {1, 1}, fixture.receipts(List.of(e.id())), "one acknowledged receipt " + e.name());
    }

    // ---------------------------------------------------------------- 1. stress (SQL side)

    record Identity(String database, long character) {}

    enum Phase { BEFORE_START, DURING_BATTLE, ALL_DISCONNECTED, NORMAL_END, AFTER_RESULT }

    /** One frozen group = one journal.save call (DurableBattleSettlement.freeze). */
    private record Frozen(List<SettlementJournal.Entry> entries) {}

    /**
     * Individual PvP, the TeamDisconnectSettlement rule: ≥2 survivors → match continues and the departed
     * are frozen as disconnect losses; 1 survivor → wins; every combatant gone → void.
     */
    private static List<Frozen> battle(Random random, Phase phase, List<Identity> players) {
        String battle = UUID.randomUUID().toString();
        List<Long> runtime = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) runtime.add((long) i + 1);
        List<Frozen> out = new ArrayList<>();
        switch (phase) {
            case BEFORE_START, ALL_DISCONNECTED -> {
                // BEFORE_START: registerBattleDisconnect refuses (battle not started) → no freeze at all.
                // ALL_DISCONNECTED: freeze runs with a void resolution → entry() is null for everyone.
                var invalid = new BattleRules.Resolution(true, false, Set.of(), Set.of(), "all-combatants-disconnected-invalid");
                for (int i = 0; i < players.size(); i++)
                    assertEquals(null, frozen(battle, runtime.get(i), players.get(i).database(), players.get(i).character(),
                            invalid, new LinkedHashSet<>(runtime)));
            }
            case DURING_BATTLE -> {
                int departing = 1 + random.nextInt(players.size() - 1);
                List<Long> shuffled = new ArrayList<>(runtime);
                Collections.shuffle(shuffled, random);
                Set<Long> departed = new LinkedHashSet<>(shuffled.subList(0, departing));
                List<Long> survivors = new ArrayList<>(shuffled.subList(departing, shuffled.size()));
                if (survivors.size() >= 2) {
                    var continuing = new BattleRules.Resolution(false, false, Set.of(), departed, "member-disconnected-battle-continues");
                    out.add(group(battle, players, runtime, continuing, departed));
                    long winner = survivors.get(random.nextInt(survivors.size()));
                    Set<Long> losers = new LinkedHashSet<>(survivors);
                    losers.remove(winner);
                    var end = new BattleRules.Resolution(true, false, Set.of(winner), losers, "normal-end");
                    out.add(group(battle, players, runtime, end, Set.of()));
                } else {
                    var end = new BattleRules.Resolution(true, false, Set.of(survivors.get(0)), departed, "combatant-disconnected");
                    out.add(group(battle, players, runtime, end, departed));
                }
            }
            case NORMAL_END, AFTER_RESULT -> {
                // AFTER_RESULT: the disconnect arrives after the freeze; the frozen terminal verdict is kept.
                long winner = runtime.get(random.nextInt(runtime.size()));
                Set<Long> losers = new LinkedHashSet<>(runtime);
                losers.remove(winner);
                out.add(group(battle, players, runtime, new BattleRules.Resolution(true, false, Set.of(winner), losers, "normal-end"), Set.of()));
            }
        }
        return out;
    }

    private static Frozen group(String battle, List<Identity> players, List<Long> runtime,
            BattleRules.Resolution resolution, Set<Long> disconnected) {
        List<SettlementJournal.Entry> entries = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            long id = runtime.get(i);
            if (!resolution.winners().contains(id) && !resolution.losers().contains(id)) continue;
            // DurableBattleSettlement.freeze: a combatant that already has a ticket entry is not frozen again.
            if (!resolution.ended() && !disconnected.contains(id)) continue;
            entries.add(frozen(battle, id, players.get(i).database(), players.get(i).character(), resolution, disconnected));
        }
        return new Frozen(List.copyOf(entries));
    }

    @Test
    void manyRoomsWithRepeatedDisconnectsAndDuplicateConcurrentDeliverySettleEachResultOnce() throws Exception {
        int now = period();
        List<Identity> identities = new ArrayList<>();
        for (String database : SOURCES) for (long character : List.of(KNIGHT, WIZARD)) identities.add(new Identity(database, character));
        // Far from the 0 floor so the expected totals do not depend on delivery order.
        SettlementSql.Counters base = new SettlementSql.Counters(100, 100, 1_000_000, 10, now, 500_000, 10, 10);
        Map<Identity, SettlementSql.Counters> expected = new LinkedHashMap<>();
        Map<Identity, Long> versions = new LinkedHashMap<>();
        for (Identity id : identities) {
            fixture.seed(id.database(), id.character(), base, 1000);
            expected.put(id, base);
            versions.put(id, 1000L);
        }

        Random random = new Random(355);
        List<List<Frozen>> rounds = new ArrayList<>();
        List<String> resultIds = new ArrayList<>();
        Map<Phase, Integer> perPhase = new LinkedHashMap<>();
        for (int b = 0; b < 60; b++) {
            List<Identity> shuffled = new ArrayList<>(identities);
            Collections.shuffle(shuffled, random);
            List<Identity> players = shuffled.subList(0, 2 + random.nextInt(3)); // 2..4 combatants, mixed sources
            Phase phase = Phase.values()[b % Phase.values().length];
            perPhase.merge(phase, 1, Integer::sum);
            List<Frozen> frozen = battle(random, phase, players);
            for (Frozen f : frozen) for (var e : f.entries()) {
                Identity id = new Identity(e.database(), e.character());
                expected.put(id, next(expected.get(id), e, now));
                versions.merge(id, 1L, Long::sum);
                resultIds.add(e.id());
            }
            rounds.add(frozen);
        }
        assertEquals(resultIds.size(), new LinkedHashSet<>(resultIds).size(), "result ids are unique per (battle, source, character)");

        Path wal = temporary.resolve("battle-settlements");
        ConcurrentLinkedQueue<Throwable> unexpected = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<String> transientFailures = new ConcurrentLinkedQueue<>();
        // Two process lifetimes: half of the rooms, a restart with fresh routes, the other half.
        for (List<List<Frozen>> half : List.of(rounds.subList(0, 30), rounds.subList(30, 60))) {
            try (SettlementJournal journal = new SettlementJournal(wal)) {
                List<Runnable> deliveries = new ArrayList<>();
                for (List<Frozen> room : half) for (Frozen f : room) {
                    if (f.entries().isEmpty()) continue;
                    journal.save(f.entries());
                    for (var e : f.entries()) {
                        deliveries.add(() -> deliver(List.of(e), transientFailures, unexpected)); // live record()
                        deliveries.add(() -> deliver(List.of(e), transientFailures, unexpected)); // retry after a lost reply
                    }
                    deliveries.add(() -> deliver(f.entries(), transientFailures, unexpected)); // worker replay of the WAL file
                }
                Collections.shuffle(deliveries, random);
                ExecutorService pool = Executors.newFixedThreadPool(8);
                try {
                    List<Future<?>> jobs = new ArrayList<>();
                    for (Runnable r : deliveries) jobs.add(pool.submit(r));
                    // The background worker replays the WAL while the live deliveries are still running.
                    jobs.add(pool.submit(() -> {
                        try {
                            journal.replay(SettlementSql::apply);
                        } catch (SQLException e) {
                            transientFailures.add(e.getMessage());
                        } catch (Throwable e) {
                            unexpected.add(e);
                        }
                    }));
                    for (Future<?> job : jobs) job.get(120, TimeUnit.SECONDS);
                } finally {
                    pool.shutdownNow();
                }
            }
            fixture.restart();
        }
        assertTrue(unexpected.isEmpty(), () -> "unexpected failures: " + unexpected);

        // Converge exactly like the startup recovery does (lock-busy/deadlock attempts are retried here).
        for (int attempt = 0; ; attempt++) {
            try {
                startupRecovery(wal);
                break;
            } catch (SQLException e) {
                if (attempt >= 3) throw e;
            }
        }
        for (Identity id : identities) {
            assertEquals(expected.get(id), fixture.counters(id.database(), id.character()), "counters " + id);
            assertEquals(versions.get(id), fixture.version(id.database(), id.character()), "one version step per result " + id);
        }
        assertArrayEquals(new long[] {resultIds.size(), resultIds.size()}, fixture.receipts(resultIds),
                "one acknowledged receipt per frozen result");
        // Another restart + recovery, then late duplicate deliveries: still nothing changes.
        fixture.restart();
        startupRecovery(wal);
        for (List<Frozen> room : rounds) for (Frozen f : room) if (!f.entries().isEmpty()) SettlementSql.apply(f.entries());
        for (Identity id : identities)
            assertEquals(expected.get(id), fixture.counters(id.database(), id.character()), "after replay " + id);
        System.out.println("[T124] stress: battles=" + rounds.size() + " phases=" + perPhase + " results=" + resultIds.size()
                + " transient-failures=" + transientFailures.size());
        assertFalse(resultIds.isEmpty());
    }

    private static void deliver(List<SettlementJournal.Entry> entries, ConcurrentLinkedQueue<String> transientFailures,
            ConcurrentLinkedQueue<Throwable> unexpected) {
        try {
            SettlementSql.apply(entries);
        } catch (SQLException e) {
            // GET_LOCK timeout / InnoDB deadlock: the production caller keeps the WAL file and retries.
            transientFailures.add(e.getMessage());
        } catch (Throwable e) {
            unexpected.add(e);
        }
    }

    // ---------------------------------------------------------------- defect fixed by T124

    @Test
    void renamedCharacterResultNoLongerBlocksTheOtherPendingResults() throws Exception {
        String database = "380_ac_1";
        int now = period();
        SettlementSql.Counters knightBase = new SettlementSql.Counters(1, 1, 100, 0, now, 100, 1, 1);
        SettlementSql.Counters wizardBase = new SettlementSql.Counters(2, 2, 200, 0, now, 200, 2, 2);
        fixture.seed(database, KNIGHT, knightBase, 5);
        fixture.seed(database, WIZARD, wizardBase, 6);
        // The Knight row was frozen under a name that no longer matches characters.char_name
        // (renamed/deleted in the source game database while the result was pending).
        String renamedBattle = UUID.randomUUID().toString(), mixedBattle = UUID.randomUUID().toString(),
                wizardBattle = UUID.randomUUID().toString();
        var renamed = new SettlementJournal.Entry(SettlementJournal.Entry.key(renamedBattle, database, KNIGHT), renamedBattle,
                database, DurableSettlementCiFixture.source(database), CONTROLS, KNIGHT, "CiKnightRenamed", false, true);
        var renamedMixed = new SettlementJournal.Entry(SettlementJournal.Entry.key(mixedBattle, database, KNIGHT), mixedBattle,
                database, DurableSettlementCiFixture.source(database), CONTROLS, KNIGHT, "CiKnightRenamed", false, false);
        var wizardMixed = new SettlementJournal.Entry(SettlementJournal.Entry.key(mixedBattle, database, WIZARD), mixedBattle,
                database, DurableSettlementCiFixture.source(database), CONTROLS, WIZARD, "CiWizard", true, false);
        var wizardAlone = new SettlementJournal.Entry(SettlementJournal.Entry.key(wizardBattle, database, WIZARD), wizardBattle,
                database, DurableSettlementCiFixture.source(database), CONTROLS, WIZARD, "CiWizard", true, false);
        Path wal = temporary.resolve("battle-settlements");
        try (SettlementJournal journal = new SettlementJournal(wal)) {
            journal.save(List.of(renamed));
            journal.save(List.of(renamedMixed, wizardMixed));
            journal.save(List.of(wizardAlone));
            SQLException refused = assertThrows(SQLException.class, () -> journal.replay(SettlementSql::apply));
            assertTrue(refused.getMessage().contains("identity changed"), refused.getMessage());
            // Both Wizard wins are written whatever the (random) file order is; before T124 a renamed
            // row sorted first stopped the replay and the Wizard's results waited behind it.
            assertEquals(next(next(wizardBase, wizardMixed, now), wizardAlone, now), fixture.counters(database, WIZARD));
            assertEquals(8, fixture.version(database, WIZARD));
            assertEquals(knightBase, fixture.counters(database, KNIGHT), "a mismatched identity is never written");
            assertEquals(2, journal.pending(), "files holding the refused row stay for the operator");
            assertThrows(SQLException.class, SettlementSql::drainAll);
            assertEquals(next(next(wizardBase, wizardMixed, now), wizardAlone, now), fixture.counters(database, WIZARD));
        } finally {
            // Test cleanup only: the refused receipts would otherwise block drainAll in later CI tests.
            fixture.acknowledge(List.of(renamed.id(), renamedMixed.id()));
        }
        SettlementSql.drainAll();
    }
}
