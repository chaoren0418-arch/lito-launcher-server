package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
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
    void aFileAlreadyConsumedByAnotherReplayerIsSkipped() throws Exception {
        Path dir = temporary.resolve("journal");
        try (SettlementJournal journal = new SettlementJournal(dir)) {
            journal.save(List.of(entry(11, "First", true, false)));
            journal.save(List.of(entry(12, "Second", false, true)));
            List<SettlementJournal.Entry> delivered = new ArrayList<>();
            journal.replay(entries -> {
                delivered.addAll(entries);
                // A concurrent replayer (the retry worker) applies and deletes every file meanwhile.
                try (var files = Files.list(dir)) {
                    for (Path p : files.filter(f -> f.toString().endsWith(".pending")).toList()) Files.delete(p);
                }
            });
            assertEquals(1, delivered.size(), "the vanished second file is neither re-read nor reported");
            assertEquals(0, journal.pending());
        }
    }

    @Test
    void crashDuringSaveLeavesOnlyAnIgnoredTemporaryFile() throws Exception {
        Path dir = temporary.resolve("journal");
        try (SettlementJournal journal = new SettlementJournal(dir)) {
            journal.save(List.of(entry(9, "Kept", true, false)));
        }
        // A crash between the .tmp write and the atomic rename leaves a partial .tmp file.
        Path partial = dir.resolve(UUID.randomUUID() + ".tmp");
        Files.writeString(partial, "format=1\ncount=1\n0.id=trunc", StandardCharsets.ISO_8859_1);
        Files.setLastModifiedTime(partial, FileTime.fromMillis(System.currentTimeMillis() - 60_000));
        try (SettlementJournal restarted = new SettlementJournal(dir)) {
            assertEquals(1, restarted.pending(), "the partial save is not a committed result");
            // T136: the dead owner's leftover is removed once this process owns the directory.
            assertFalse(Files.exists(partial));
            assertEquals(List.of(partial.getFileName()), restarted.removedTemporaryFiles().stream().map(Path::getFileName).toList());
            List<SettlementJournal.Entry> replayed = new ArrayList<>();
            restarted.replay(replayed::addAll);
            assertEquals(1, replayed.size());
            assertEquals("Kept", replayed.get(0).name());
        }
    }

    @Test
    void onlyStaleTemporaryFilesWithTheSaveNamePatternAreRemoved() throws Exception {
        Path dir = temporary.resolve("journal");
        Files.createDirectories(dir);
        long old = System.currentTimeMillis() - 60_000;
        Path stale = dir.resolve(UUID.randomUUID() + ".tmp");
        Path foreign = dir.resolve("operator-notes.tmp"); // not a save() name
        Path upper = dir.resolve(UUID.randomUUID().toString().toUpperCase(java.util.Locale.ROOT) + ".tmp");
        Path future = dir.resolve(UUID.randomUUID() + ".tmp"); // modified after the lock was taken
        Path folder = dir.resolve(UUID.randomUUID() + ".tmp"); // not a regular file
        for (Path p : List.of(stale, foreign, upper, future)) Files.writeString(p, "x");
        Files.createDirectory(folder);
        for (Path p : List.of(stale, foreign, upper, folder)) Files.setLastModifiedTime(p, FileTime.fromMillis(old));
        Files.setLastModifiedTime(future, FileTime.fromMillis(System.currentTimeMillis() + 3_600_000));
        try (SettlementJournal journal = new SettlementJournal(dir)) {
            assertFalse(Files.exists(stale));
            for (Path p : List.of(foreign, upper, future, folder)) assertTrue(Files.exists(p), p.getFileName().toString());
            assertEquals(1, journal.removedTemporaryFiles().size());
            assertEquals(4, journal.keptTemporaryFiles().size());
        }
    }

    @Test
    void secondOwnerNeverTouchesTheTemporaryFilesOfTheRunningOwner() throws Exception {
        Path dir = temporary.resolve("journal");
        try (SettlementJournal owner = new SettlementJournal(dir)) {
            Path inFlight = dir.resolve(UUID.randomUUID() + ".tmp");
            Files.writeString(inFlight, "x");
            Files.setLastModifiedTime(inFlight, FileTime.fromMillis(System.currentTimeMillis() - 60_000));
            assertThrows(Exception.class, () -> new SettlementJournal(dir).close());
            assertTrue(Files.exists(inFlight), "a refused second process deletes nothing");
        }
    }

    @Test
    void voidRequestListIsParsedStrictly() throws Exception {
        Path file = temporary.resolve("void-requests.txt");
        String a = "a".repeat(64), b = "B".repeat(64);
        Files.writeString(file, "\uFEFF# operator list\n\n" + a + "   角色已改名 ticket-17\n" + b + "\n" + a + " duplicate\n",
                StandardCharsets.UTF_8);
        var parsed = SettlementJournal.voidRequests(file);
        assertEquals(List.of(a, "b".repeat(64)), List.copyOf(parsed.keySet()));
        assertEquals("角色已改名 ticket-17", parsed.get(a));
        assertEquals("identity changed in source database", parsed.get("b".repeat(64)));
        Files.writeString(file, a + "\n" + "c".repeat(63) + "\n", StandardCharsets.UTF_8);
        IOException bad = assertThrows(IOException.class, () -> SettlementJournal.voidRequests(file));
        assertTrue(bad.getMessage().contains("line 2"), bad.getMessage());
        Files.writeString(file, a + " " + "x".repeat(201) + "\n", StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> SettlementJournal.voidRequests(file));
    }

    @Test
    void archiveCopiesOnlyTheFilesHoldingVoidedResultsAndKeepsThemPending() throws Exception {
        Path dir = temporary.resolve("journal");
        SettlementJournal.Entry renamed = entry(21, "Renamed", false, true);
        SettlementJournal.Entry partner = entry(22, "Partner", true, false);
        SettlementJournal.Entry other = entry(23, "Other", true, false);
        try (SettlementJournal journal = new SettlementJournal(dir)) {
            journal.save(List.of(renamed, partner));
            journal.save(List.of(other));
            List<Path> copies = journal.archive(Set.of(renamed.id()));
            assertEquals(1, copies.size());
            assertTrue(copies.get(0).startsWith(dir.resolve(SettlementJournal.VOIDED_DIRECTORY)));
            assertEquals(2, journal.pending(), "archiving copies; the replay still owns the originals");
            assertEquals(copies, journal.archive(Set.of(renamed.id())), "a rerun does not duplicate the copy");
        }
        // The audit sub-directory is not read as committed results.
        try (SettlementJournal reopened = new SettlementJournal(dir)) {
            assertEquals(2, reopened.pending());
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
