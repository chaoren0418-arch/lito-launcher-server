package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * T124 (#355): write-ahead settlement journal behaviour around a process abort and restart.
 * No database: the sink stands in for {@link SettlementSql#apply}; the SQL side is covered by
 * {@code DisconnectSettlementRecoveryMySqlIntegrationTest}.
 */
final class SettlementJournalRecoveryTest {
    @TempDir Path temporary;

    /** Simulated process death: an Error, so no {@code catch (Exception)} in the code under test can absorb it. */
    static final class SimulatedCrash extends Error {
        SimulatedCrash(String where) { super("simulated crash at " + where); }
    }

    private static SettlementJournal.Entry entry(long character, String name, boolean won, boolean disconnected) {
        String battle = UUID.randomUUID().toString();
        return new SettlementJournal.Entry(SettlementJournal.Entry.key(battle, "380_ac_1", character),
                battle, "380_ac_1", "CI", "380_lito", character, name, won, disconnected);
    }

    @Test
    void failingFileStaysPendingWithoutBlockingTheOthers() throws Exception {
        Path dir = temporary.resolve("journal");
        SettlementJournal.Entry poison = entry(1, "Renamed", false, true);
        List<SettlementJournal.Entry> good = List.of(entry(2, "A", true, false), entry(3, "B", false, false),
                entry(4, "C", false, true), entry(5, "D", true, false));
        Set<String> applied = new HashSet<>();
        try (SettlementJournal journal = new SettlementJournal(dir)) {
            journal.save(List.of(poison));
            for (var e : good) journal.save(List.of(e));
            assertEquals(5, journal.pending());
            SQLException refused = assertThrows(SQLException.class, () -> journal.replay(entries -> {
                for (var e : entries) {
                    if (e.equals(poison)) throw new SQLException("Settlement character identity changed");
                    applied.add(e.id());
                }
            }));
            assertTrue(refused.getMessage().contains("identity changed"));
            // Before T124 the first failing file (random UUID order) stopped every later file.
            assertEquals(good.stream().map(SettlementJournal.Entry::id).collect(java.util.stream.Collectors.toSet()), applied);
            assertEquals(1, journal.pending(), "only the failing file remains pending");
            // Once the operator resolves the cause the same file is delivered again.
            List<SettlementJournal.Entry> retried = new ArrayList<>();
            journal.replay(retried::addAll);
            assertEquals(List.of(poison), retried);
            assertEquals(0, journal.pending());
        }
    }

    @Test
    void severalFailuresReportTheFirstAndSuppressTheRest() throws Exception {
        try (SettlementJournal journal = new SettlementJournal(temporary.resolve("journal"))) {
            journal.save(List.of(entry(1, "A", true, false)));
            journal.save(List.of(entry(2, "B", true, false)));
            journal.save(List.of(entry(3, "C", true, false)));
            SQLException failure = assertThrows(SQLException.class, () -> journal.replay(entries -> {
                throw new SQLException("database unavailable " + entries.get(0).character());
            }));
            assertEquals(2, failure.getSuppressed().length);
            assertEquals(3, journal.pending(), "an outage deletes nothing");
        }
    }

    @Test
    void crashAfterTheSinkBeforeDeletionRedeliversTheSameFrozenResultAfterRestart() throws Exception {
        Path dir = temporary.resolve("journal");
        SettlementJournal.Entry win = entry(7, "Winner", true, false);
        SettlementJournal.Entry dropped = entry(8, "Dropped", false, true);
        List<SettlementJournal.Entry> delivered = new ArrayList<>();
        Set<String> durable = new HashSet<>();
        SettlementJournal first = new SettlementJournal(dir);
        first.save(List.of(win, dropped));
        assertThrows(SimulatedCrash.class, () -> first.replay(entries -> {
            delivered.addAll(entries);
            for (var e : entries) durable.add(e.id()); // the SQL write completed ...
            throw new SimulatedCrash("before journal delete"); // ... but the process died before Files.delete
        }));
        first.close(); // the OS releases the owner lock of a dead process
        try (SettlementJournal restarted = new SettlementJournal(dir)) {
            assertEquals(1, restarted.pending());
            restarted.replay(entries -> {
                delivered.addAll(entries);
                for (var e : entries) durable.add(e.id()); // SQL side is idempotent per result id
            });
            assertEquals(0, restarted.pending());
        }
        // Same frozen verdict twice (won / disconnected flags unchanged); the result id dedupes it.
        assertEquals(List.of(win, dropped, win, dropped), delivered);
        assertEquals(Set.of(win.id(), dropped.id()), durable);
    }

    @Test
    void crashDuringSaveLeavesOnlyAnIgnoredTemporaryFile() throws Exception {
        Path dir = temporary.resolve("journal");
        try (SettlementJournal journal = new SettlementJournal(dir)) {
            journal.save(List.of(entry(9, "Kept", true, false)));
        }
        // A crash between the .tmp write and the atomic rename leaves a partial .tmp file.
        Files.writeString(dir.resolve(UUID.randomUUID() + ".tmp"), "format=1\ncount=1\n0.id=trunc",
                StandardCharsets.ISO_8859_1);
        try (SettlementJournal restarted = new SettlementJournal(dir)) {
            assertEquals(1, restarted.pending(), "the partial save is not a committed result");
            List<SettlementJournal.Entry> replayed = new ArrayList<>();
            restarted.replay(replayed::addAll);
            assertEquals(1, replayed.size());
            assertEquals("Kept", replayed.get(0).name());
        }
    }

    @Test
    void corruptedCommittedFileRefusesStartupInsteadOfDroppingResults() throws Exception {
        Path dir = temporary.resolve("journal");
        try (SettlementJournal journal = new SettlementJournal(dir)) {
            journal.save(List.of(entry(10, "Corrupt", true, false)));
        }
        Path pending;
        try (var files = Files.list(dir)) {
            pending = files.filter(p -> p.getFileName().toString().endsWith(".pending")).findFirst().orElseThrow();
        }
        String text = Files.readString(pending, StandardCharsets.ISO_8859_1);
        Files.writeString(pending, text.replace("0.won=true", "0.won=maybe"), StandardCharsets.ISO_8859_1);
        assertThrows(IOException.class, () -> new SettlementJournal(dir).close());
        // The file is kept for the operator; nothing was deleted.
        assertTrue(Files.exists(pending));
    }

    @Test
    void secondOwnerOfTheSameJournalIsRefusedUntilTheFirstIsGone() throws Exception {
        Path dir = temporary.resolve("journal");
        SettlementJournal owner = new SettlementJournal(dir);
        try {
            Exception refused = assertThrows(Exception.class, () -> new SettlementJournal(dir).close());
            assertTrue(refused instanceof IOException || refused instanceof java.nio.channels.OverlappingFileLockException,
                    refused.toString());
        } finally {
            owner.close();
        }
        SettlementJournal next = new SettlementJournal(dir);
        assertEquals(0, next.pending());
        next.close();
    }
}
