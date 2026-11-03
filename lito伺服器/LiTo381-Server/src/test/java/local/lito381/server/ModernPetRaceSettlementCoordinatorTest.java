package local.lito381.server;

import static local.lito381.server.ModernPetRaceSettlement.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernPetRaceSettlementCoordinatorTest {
    @TempDir Path directory;
    private static long deadline() { return System.nanoTime() + TimeUnit.SECONDS.toNanos(3); }
    private static Event event(long match) {
        return ModernPetRacePersistenceTestFixture.event(UUID.randomUUID(),match,123456789);
    }
    private static void await(BooleanSupplier check) throws Exception {
        long end = deadline();
        while (!check.getAsBoolean()) { assertTrue(end - System.nanoTime() > 0, "bounded coordinator wait"); Thread.sleep(5); }
    }
    private static void stop(ModernPetRaceSettlementCoordinator coordinator) throws Exception {
        coordinator.close(); assertTrue(coordinator.awaitTermination(3_000)); assertTrue(coordinator.isQuiescent());
    }
    @Test void reservesCapacityBeforeMatchAndUnusedSlotsAreReleased() throws Exception {
        var box = new ModernPetRaceOutbox(directory, new byte[32]);
        var coordinator = new ModernPetRaceSettlementCoordinator(box, (e, d) -> fail("no event recorded"), 2_000, null);
        try {
            var slots = new ArrayList<Reservation>();
            for (int i = 1; i <= ModernPetRaceOutbox.MAX_PENDING; i++) slots.add(coordinator.reserve(i, 11));
            assertFalse(coordinator.accepting());
            assertThrows(SettlementException.class, () -> coordinator.reserve(1_001, 11));
            slots.remove(0).close(); assertTrue(coordinator.accepting());
            slots.add(coordinator.reserve(1_001, 11)); assertFalse(coordinator.accepting());
            slots.forEach(Reservation::close); assertTrue(coordinator.accepting()); assertEquals(0, box.size());
        } finally { stop(coordinator); }
    }
    @Test void tokenIsBoundToCapturedMatchAndCannotBypassJournal() throws Exception {
        var coordinator = new ModernPetRaceSettlementCoordinator(new ModernPetRaceOutbox(directory, new byte[32]), (e, d) -> {}, 2_000, null);
        try {
            var slot = coordinator.reserve(7, 11);
            assertThrows(SettlementException.class, () -> coordinator.reserve(7, 11));
            assertEquals(Failure.CONFLICT, assertThrows(SettlementException.class, () -> slot.record(event(8), deadline())).failure());
            assertEquals(Failure.INVALID_STORAGE, assertThrows(SettlementException.class, () -> coordinator.persist(event(7), deadline())).failure());
            slot.close(); assertThrows(SettlementException.class, () -> slot.record(event(7), deadline()));
        } finally { stop(coordinator); }
    }
    @Test void savedReceiptFollowsActualSqlReturnAndRemainsAfterGarbageCollection() throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        var box = new ModernPetRaceOutbox(directory, new byte[32]);
        var coordinator = new ModernPetRaceSettlementCoordinator(box, (e, d) -> {
            assertEquals(List.of(e), box.pending(deadline())); entered.countDown();
            assertTrue(release.await(3, TimeUnit.SECONDS));
        }, 2_000, null);
        try {
            var event = event(7); var slot = coordinator.reserve(7, 11); slot.record(event, deadline());
            assertTrue(entered.await(1, TimeUnit.SECONDS)); assertEquals(ModernPetRaceProtocol.Settlement.PENDING, slot.state());
            slot.record(event, deadline()); // Idempotent token does not append or submit another result.
            slot.record(ModernPetRacePersistenceTestFixture.recorded(event,event.recordedAtMillis()+1), deadline());
            assertEquals(List.of(event),box.pending(deadline())); // First journal audit is immutable.
            assertThrows(SettlementException.class, () -> slot.record(event(7), deadline()));
            release.countDown(); await(() -> slot.state() == ModernPetRaceProtocol.Settlement.SAVED && box.size() == 0);
            assertEquals(ModernPetRaceProtocol.Settlement.SAVED, slot.state()); slot.close(); assertEquals(ModernPetRaceProtocol.Settlement.SAVED, slot.state());
        } finally { release.countDown(); stop(coordinator); }
    }
    @Test void uncertainCommitRetryUsesExactEventAndFreshBoundedAttemptDeadline() throws Exception {
        var calls = new AtomicInteger(); var seen = new AtomicReference<Event>(); var end = new java.util.concurrent.atomic.AtomicLong();
        var coordinator = new ModernPetRaceSettlementCoordinator(new ModernPetRaceOutbox(directory, new byte[32]), (e, d) -> {
            if (calls.incrementAndGet() == 1) { seen.set(e); end.set(d); throw new SettlementException(Failure.COMMIT_UNKNOWN); }
            assertEquals(seen.get(), e); assertTrue(d > end.get()); assertTrue(d - System.nanoTime() <= TimeUnit.SECONDS.toNanos(2));
        }, 2_000, null);
        try {
            var slot = coordinator.reserve(7, 11); slot.record(event(7), deadline());
            await(() -> slot.state() == ModernPetRaceProtocol.Settlement.SAVED); assertEquals(2, calls.get());
        } finally { stop(coordinator); }
    }
    @Test void transientFailureDoesNotBecomePermanentClientFailureAfterThreeAttempts() throws Exception {
        var calls = new AtomicInteger(); var allow = new java.util.concurrent.atomic.AtomicBoolean();
        var coordinator = new ModernPetRaceSettlementCoordinator(new ModernPetRaceOutbox(directory, new byte[32]), (e, d) -> {
            calls.incrementAndGet(); if (!allow.get()) throw new SettlementException(Failure.UNAVAILABLE);
        }, 2_000, null);
        try {
            var slot = coordinator.reserve(7, 11); slot.record(event(7), deadline());
            await(() -> calls.get() >= 4); assertEquals(ModernPetRaceProtocol.Settlement.PENDING, slot.state()); assertTrue(coordinator.accepting());
            allow.set(true); await(() -> slot.state() == ModernPetRaceProtocol.Settlement.SAVED);
        } finally { stop(coordinator); }
    }
    @Test void storageConflictHaltsAdmissionButPreservesExactPendingEvidence() throws Exception {
        var box = new ModernPetRaceOutbox(directory, new byte[32]);
        var coordinator = new ModernPetRaceSettlementCoordinator(box, (e, d) -> { throw new SettlementException(Failure.CONFLICT); }, 2_000, null);
        var event = event(7);
        try {
            var slot = coordinator.reserve(7, 11); slot.record(event, deadline());
            await(() -> slot.state() == ModernPetRaceProtocol.Settlement.FAILED); assertFalse(coordinator.accepting());
            assertEquals(List.of(event), box.pending(deadline()));
        } finally { stop(coordinator); }
        try (var reopened = new ModernPetRaceOutbox(directory, new byte[32])) { assertEquals(List.of(event), reopened.pending(deadline())); }
    }
    @Test void restartReplaysBeforeNewMatchesAndDeletesOnlyConfirmedRecord() throws Exception {
        var event = event(7);
        try (var first = new ModernPetRaceOutbox(directory, new byte[32])) { first.record(event, deadline()); }
        var seen = new AtomicReference<Event>(); var box = new ModernPetRaceOutbox(directory, new byte[32]);
        var coordinator = new ModernPetRaceSettlementCoordinator(box, (e, d) -> { assertEquals(event, e); seen.set(e); }, 2_000, null);
        try { await(() -> seen.get() != null && box.size() == 0); }
        finally { stop(coordinator); }
        try (var reopened = new ModernPetRaceOutbox(directory, new byte[32])) { assertEquals(List.of(), reopened.pending(deadline())); }
    }
    @Test void closeDoesNotPretendAnUnreturnedWriterHasRetired() throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        var coordinator = new ModernPetRaceSettlementCoordinator(new ModernPetRaceOutbox(directory, new byte[32]), (e, d) -> {
            entered.countDown(); assertTrue(release.await(3, TimeUnit.SECONDS));
        }, 2_000, null);
        try {
            var slot = coordinator.reserve(7, 11); slot.record(event(7), deadline());
            assertTrue(entered.await(1, TimeUnit.SECONDS)); coordinator.close();
            assertFalse(coordinator.accepting()); assertFalse(coordinator.awaitTermination(20)); assertFalse(coordinator.isQuiescent());
            release.countDown();
        } finally { release.countDown(); stop(coordinator); }
    }
    @Test void closeDuringActualForceKeepsFileOwnedUntilRecordReturnsAndLeavesReplay() throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1); var force = new AtomicInteger();
        var box = new ModernPetRaceOutbox(directory, new byte[32], new ModernPetRaceOutbox.FileOps() {
            @Override public void force(FileChannel file) throws java.io.IOException {
                if (force.incrementAndGet() == 2) {
                    entered.countDown();
                    try { if (!release.await(3, TimeUnit.SECONDS)) throw new java.io.IOException("bounded test force"); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new java.io.IOException("test interrupted"); }
                }
                file.force(true);
            }
        });
        var coordinator = new ModernPetRaceSettlementCoordinator(box, (e, d) -> fail("shutdown must not dispatch new SQL"), 2_000, null);
        var error = new AtomicReference<Throwable>(); var event = event(7); var slot = coordinator.reserve(7, 11);
        var writer = new Thread(() -> { try { slot.record(event, deadline()); } catch (Throwable t) { error.set(t); } });
        try {
            writer.start(); assertTrue(entered.await(1, TimeUnit.SECONDS)); slot.close(); coordinator.close();
            assertFalse(coordinator.awaitTermination(20)); assertFalse(box.isQuiescent());
            release.countDown(); writer.join(2_000); assertFalse(writer.isAlive()); assertNull(error.get());
        } finally { release.countDown(); writer.join(3_000); stop(coordinator); }
        try (var reopened = new ModernPetRaceOutbox(directory, new byte[32])) { assertEquals(List.of(event), reopened.pending(deadline())); }
    }
    @Test void SQLConfirmationSurvivesUncertainOutboxAcknowledgement() throws Exception {
        var moves = new AtomicInteger();
        var box = new ModernPetRaceOutbox(directory, new byte[32], new ModernPetRaceOutbox.FileOps() {
            @Override public void move(Path a, Path b) throws java.io.IOException {
                if (moves.incrementAndGet() == 3) throw new java.io.IOException("fixture unknown ack");
                ModernPetRaceOutbox.FileOps.super.move(a, b);
            }
        });
        var coordinator = new ModernPetRaceSettlementCoordinator(box, (e, d) -> {}, 2_000, null);
        var event = event(7);
        try {
            var slot = coordinator.reserve(7, 11); slot.record(event, deadline());
            await(() -> !box.isHealthy()); assertEquals(ModernPetRaceProtocol.Settlement.SAVED, slot.state()); assertFalse(coordinator.accepting());
        } finally { stop(coordinator); }
        try (var reopened = new ModernPetRaceOutbox(directory, new byte[32])) { assertEquals(List.of(event), reopened.pending(deadline())); }
    }
}
