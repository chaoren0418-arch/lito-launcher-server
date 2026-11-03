package local.lito381.server;

import static local.lito381.server.ModernWorldProtocol.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;

/** Real Rooms + preparation + engine + asynchronous writer; sources/writer are explicit fixtures, not SQL evidence. */
class ModernWorldSettlementTest {
    private static final class Fixture implements AutoCloseable {
        final ModernRoomsTest.Fixture rooms = new ModernRoomsTest.Fixture();
        final ModernRoomsTest.Player first = rooms.player(7, Long.MAX_VALUE, 30, 0);
        final ModernRoomsTest.Player second = rooms.player(42, Long.MAX_VALUE, 30, 0);
        final List<ModernRoomsTest.Player> players = List.of(first, second);
        final ModernWorldService service;
        Fixture(ModernMatchSettlement.Store store) throws Exception {
            var sector = new ModernWorldMap.SectorKey(0x8000, 0x8000);
            var map = ModernWorldMap.fromSectorBytes(514, Map.of(sector, ModernWorldMapTest.openSectorBytes()), () -> false);
            var binding = new ModernWorldBinding(1, 514, map);
            service = new ModernWorldService(rooms.rooms, Map.of(binding.key(), new ModernWorldService.Mapping(binding,
                    new ModernWorldMap.Cell(32778, 32778), 1_000_000, 1_000_000)),
                    new ModernWorldService.Limits(4, 2, 2, 2_000, 5, new ModernBattleEngine.Limits(2_000, 0, 10_000)),
                    bound -> 0, store);
            ModernRoomsTest.accepted(rooms.create(first, ModernRoomsTest.draft(100, 1, 514, 0)));
            ModernRoomsTest.accepted(rooms.join(second, rooms.view(first).joined().summary(), ""));
            for (var p : players) register(p, true);
            ready();
        }
        ModernApplicationProtocol.CharacterRow row(ModernRoomsTest.Player p) {
            var s = rooms.presence.selected(p.owner());
            return new ModernApplicationProtocol.CharacterRow(s.key().objectId(), s.characterName(), s.characterClass(), s.sex(), s.level());
        }
        void register(ModernRoomsTest.Player p, boolean v3) {
            service.register(p.owner(), p.scope(), deadline -> row(p), deadline -> {
                var source = ModernPhysicalCombatTest.fixtureSource(p.scope().sourceId(), row(p), 1, 10, 0);
                return ModernPhysicalCombat.qualify(source, ModernCombatProfileTest.binding(source.input().equipment()), Optional.empty(), System.currentTimeMillis());
            }, v3);
        }
        void ready() { for (var p : players) ModernRoomsTest.accepted(rooms.act(p, ModernRooms.Action.READY, 1)); }
        Reply prepare() {
            var room = rooms.view(first).joined().summary();
            return service.handle(first.owner(), new Request(first.scope(), Action.PREPARE, 0, room.id(), room.revision(), 0, 0, 0, 0, 0, ZERO_DIGEST));
        }
        Frame read(ModernRoomsTest.Player p) {
            return service.handle(p.owner(), new Request(p.scope(), Action.READ, 0, 0, 0, 0, 0, 0, 0, 0, ZERO_DIGEST)).frame();
        }
        Frame active() throws Exception {
            assertEquals(Status.OK, prepare().status());
            await(() -> read(first).phase() == Phase.LOADING);
            var frame = read(first);
            for (var p : players) assertEquals(Status.OK, service.handle(p.owner(), new Request(p.scope(), Action.LOADED,
                    0, 0, 0, frame.matchId(), frame.generation(), 0, 0, 0, frame.manifest().digest())).status());
            assertEquals(Phase.ACTIVE, read(first).phase());
            return read(first);
        }
        ModernCombatProtocol.Reply hit(Frame frame) {
            return service.combat(first.owner(), new ModernCombatProtocol.Request(first.scope(), ModernCombatProtocol.Action.ATTACK,
                    frame.matchId(), frame.generation(), 1, 2));
        }
        void leave(ModernRoomsTest.Player p, Frame frame) {
            var reply = service.handle(p.owner(), new Request(p.scope(), Action.LEAVE, 0, 0, 0,
                    frame.matchId(), frame.generation(), 0, 0, 0, ZERO_DIGEST));
            assertEquals(Status.OK, reply.status()); assertNull(reply.frame());
        }
        @Override public void close() throws Exception {
            service.close(); assertTrue(service.awaitTermination(4_000)); assertTrue(service.isQuiescent());
            for (var p : players) { rooms.rooms.disconnect(p.owner()); rooms.presence.disconnect(p.owner()); }
        }
    }
    private static void await(BooleanSupplier condition) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (!condition.getAsBoolean()) { assertTrue(System.nanoTime() < end, "bounded settlement condition"); Thread.sleep(5); }
    }
    private static void release(CountDownLatch latch) throws InterruptedException {
        if (!latch.await(3, TimeUnit.SECONDS)) throw new IllegalStateException("fixture barrier timed out");
    }

    private static final class JournalFixture implements ModernMatchSettlement.JournaledStore {
        final ModernMatchSettlement.Store record;
        final AtomicInteger reserved = new AtomicInteger(), released = new AtomicInteger();
        volatile boolean closed;
        JournalFixture(ModernMatchSettlement.Store record) { this.record = record; }
        @Override public ModernMatchSettlement.Reservation reserve(long match, long generation) {
            reserved.incrementAndGet();
            return new ModernMatchSettlement.Reservation() {
                boolean freed;
                @Override public void record(ModernMatchSettlement.Event event, long deadline) throws Exception {
                    assertEquals(match, event.matchId()); assertEquals(generation, event.generation());
                    record.persist(event, deadline);
                }
                @Override public ModernMatchSettlement.ReceiptState state() { return ModernMatchSettlement.ReceiptState.PENDING; }
                @Override public void close() { if (!freed) { freed = true; released.incrementAndGet(); } }
            };
        }
        @Override public boolean accepting() { return !closed; }
        @Override public void persist(ModernMatchSettlement.Event e, long d) { fail("journal path must not dispatch legacy writer"); }
        @Override public void close() { closed = true; }
        @Override public boolean awaitTermination(long ms) { return closed; }
        @Override public boolean isQuiescent() { return closed; }
    }
    @Test void journalMustConfirmBeforeExactFinalHitAckAndConcurrentWorldRead() throws Exception {
        var entered = new CountDownLatch(1); var finish = new CountDownLatch(1);
        var journal = new JournalFixture((event, deadline) -> { entered.countDown(); release(finish); });
        try (var f = new Fixture(journal)) {
            var frame = f.active(); assertEquals(1, journal.reserved.get());
            var hit = new AtomicReference<ModernCombatProtocol.Reply>(); var read = new AtomicReference<Frame>();
            var error = new AtomicReference<Throwable>();
            var attacker = new Thread(() -> { try { hit.set(f.hit(frame)); } catch (Throwable t) { error.set(t); } });
            var reader = new Thread(() -> { try { read.set(f.read(f.second)); } catch (Throwable t) { error.set(t); } });
            try {
                attacker.start(); assertTrue(entered.await(1, TimeUnit.SECONDS)); reader.start();
                Thread.sleep(30); assertNull(hit.get()); assertNull(read.get());
                assertFalse(f.rooms.rooms.isPreparing(f.first.owner()));
                finish.countDown(); attacker.join(2_000); reader.join(2_000);
                assertFalse(attacker.isAlive()); assertFalse(reader.isAlive()); assertNull(error.get());
                assertEquals(Phase.FINISHED, hit.get().frame().world().phase()); assertEquals(Phase.FINISHED, read.get().phase());
            } finally { finish.countDown(); attacker.join(3_000); reader.join(3_000); }
        }
        assertTrue(journal.closed); assertEquals(1, journal.released.get());
    }
    @Test void unconfirmedJournalFailsClosedWithoutLeakingFinishedAndStillRetires() throws Exception {
        var journal = new JournalFixture((event, deadline) -> { throw new java.io.IOException("fixture force failed"); });
        try (var f = new Fixture(journal)) {
            var frame = f.active(); assertThrows(IllegalStateException.class, () -> f.hit(frame));
            assertThrows(IllegalStateException.class, () -> f.read(f.second));
            f.service.tickNow(); // Failed publication is not an immortal cleanup worker.
            assertFalse(f.rooms.rooms.isPreparing(f.first.owner()));
        }
        assertTrue(journal.closed); assertEquals(1, journal.released.get());
    }
    @Test void abortedWorldReleasesUnusedDurableReservationWithoutRecordingFakeOutcome() throws Exception {
        var journal = new JournalFixture((event, deadline) -> fail("abort is not a win or loss"));
        try (var f = new Fixture(journal)) {
            var frame = f.active(); f.leave(f.first, frame);
            assertEquals(Phase.ABORTED, f.read(f.second).phase()); assertEquals(1, journal.released.get());
        }
    }

    @Test void finalHitAndWholeResultAreAtomicAndSavedOnlyAfterActualWriterReturns() throws Exception {
        var entered = new CountDownLatch(1); var finish = new CountDownLatch(1);
        var calls = new AtomicInteger(); var saved = new AtomicReference<ModernMatchSettlement.Event>();
        try (var f = new Fixture((event, deadline) -> {
            assertTrue(deadline - System.nanoTime() > 0); calls.incrementAndGet(); saved.set(event); entered.countDown(); release(finish);
        })) {
            try {
                var before = f.active(); var reply = f.hit(before); var world = reply.frame().world();
                assertEquals(ModernCombatProtocol.Status.OK, reply.status()); assertEquals(Phase.FINISHED, world.phase());
                assertEquals(before.revision() + 1, world.revision());
                assertEquals(world.revision(), reply.frame().vitals().get(1).deathRevision());
                assertEquals(0, reply.frame().vitals().get(1).hp());
                assertEquals(world.revision() - 1, world.completion().event().engineRevision());
                assertEquals(SettlementState.PENDING, world.completion().state());
                assertEquals(List.of(7, 42), world.completion().event().participants().stream().map(p -> p.key().sourceId()).toList());
                assertTrue(world.completion().event().participants().stream().allMatch(p -> p.key().objectId() == Long.MAX_VALUE));
                assertEquals(List.of(ModernMatchSettlement.Verdict.WIN, ModernMatchSettlement.Verdict.LOSS),
                        world.completion().event().participants().stream().map(ModernMatchSettlement.Participant::verdict).toList());
                assertTrue(entered.await(1, TimeUnit.SECONDS)); assertEquals(world.completion().event(), saved.get());
                for (int i = 0; i < 5; i++) { f.service.tickNow(); assertEquals(SettlementState.PENDING, f.read(f.second).completion().state()); }
                assertFalse(f.rooms.rooms.isPreparing(f.first.owner()));
                var room = f.rooms.view(f.first).joined();
                assertTrue(room.members().stream().noneMatch(ModernRooms.Member::ready)); assertEquals(2, room.members().size());
                var denied = f.hit(world); assertEquals(ModernCombatProtocol.Status.DENIED, denied.status());
                assertEquals(world.revision(), denied.frame().world().revision()); assertEquals(1, calls.get());
                finish.countDown(); await(() -> f.read(f.second).completion().state() == SettlementState.SAVED);
                var after = f.read(f.second); assertEquals(world.revision() + 1, after.revision());
                assertEquals(world.actors(), after.actors()); assertEquals(world.completion().event(), after.completion().event());
            } finally { finish.countDown(); }
        }
    }

    @Test void onePlayersReturnCannotEraseOtherResultAndBothCanReturnToNewMatch() throws Exception {
        var calls = new AtomicInteger();
        try (var f = new Fixture((event, deadline) -> calls.incrementAndGet())) {
            var result = f.hit(f.active()).frame().world();
            await(() -> f.read(f.first).completion().state() == SettlementState.SAVED);
            assertEquals(Status.BUSY, f.prepare().status());
            f.leave(f.first, result); assertNull(f.read(f.first));
            var secondResult = f.read(f.second); assertEquals(result.completion().event(), secondResult.completion().event());
            f.ready(); assertEquals(Status.BUSY, f.prepare().status());
            assertEquals(secondResult, f.read(f.second));
            f.leave(f.second, result);
            var next = f.active(); assertNotEquals(result.matchId(), next.matchId());
            var secondFinish = f.hit(next).frame().world();
            assertNotEquals(result.completion().event().resultId(), secondFinish.completion().event().resultId());
            await(() -> calls.get() == 2);
        }
    }

    @Test void committedResultSurvivesLaterDetachAndPersistenceIsNotCancelled() throws Exception {
        var entered = new CountDownLatch(1); var finish = new CountDownLatch(1);
        var calls = new AtomicInteger();
        try (var f = new Fixture((event, deadline) -> { calls.incrementAndGet(); entered.countDown(); release(finish); })) {
            try {
                var result = f.hit(f.active()).frame().world(); assertTrue(entered.await(1, TimeUnit.SECONDS));
                f.service.detach(f.first.owner());
                assertEquals(Phase.FINISHED, f.read(f.second).phase());
                assertEquals(result.completion().event(), f.read(f.second).completion().event());
                finish.countDown(); await(() -> f.read(f.second).completion().state() == SettlementState.SAVED);
                assertEquals(1, calls.get());
            } finally { finish.countDown(); }
        }
    }

    @Test void failedOrUnknownWriterIsUnconfirmedNotFakeSavedOrReversedWinner() throws Exception {
        try (var f = new Fixture((event, deadline) -> { throw new java.io.IOException("fixture commit acknowledgement lost"); })) {
            var result = f.hit(f.active()).frame().world();
            await(() -> f.read(f.first).completion().state() == SettlementState.FAILED);
            var failed = f.read(f.first); assertEquals(Phase.FINISHED, failed.phase());
            assertEquals(result.completion().event(), failed.completion().event());
            f.leave(f.first, failed); f.leave(f.second, failed);
            assertNull(f.read(f.first)); assertNull(f.read(f.second));
        }
    }

    @Test void activeAbortNeverAwardsWinAndPreV3ParticipantCannotEnterResultMatch() throws Exception {
        var calls = new AtomicInteger();
        try (var f = new Fixture((event, deadline) -> calls.incrementAndGet())) {
            f.register(f.second, false);
            assertEquals(Status.UNAVAILABLE, f.prepare().status()); assertEquals(0, calls.get());
            f.register(f.second, true); f.ready();
            var active = f.active(); f.leave(f.first, active);
            assertEquals(Phase.ABORTED, f.read(f.second).phase()); assertNull(f.read(f.second).completion());
            assertEquals(0, calls.get());
        }
    }

    @Test void unknownCommitRetriesTheExactEventAndDeadlineWithoutAwardingTwice() throws Exception {
        var calls = new AtomicInteger(); var eventSeen = new AtomicReference<ModernMatchSettlement.Event>();
        var deadlineSeen = new java.util.concurrent.atomic.AtomicLong();
        try (var f = new Fixture((event, deadline) -> {
            if (calls.incrementAndGet() == 1) {
                eventSeen.set(event); deadlineSeen.set(deadline);
                throw new ModernMatchSettlement.SettlementException(ModernMatchSettlement.Failure.COMMIT_UNKNOWN);
            }
            assertSame(eventSeen.get(), event); assertEquals(deadlineSeen.get(), deadline);
        })) {
            var result = f.hit(f.active()).frame().world();
            await(() -> f.read(f.first).completion().state() == SettlementState.SAVED);
            assertEquals(2, calls.get()); assertEquals(result.completion().event(), eventSeen.get());
            f.service.tickNow(); assertEquals(2, calls.get());
        }
    }

    @Test void permanentConflictIsNotRetriedAndTransientRetriesAreBounded() throws Exception {
        for (var failure : List.of(ModernMatchSettlement.Failure.CONFLICT, ModernMatchSettlement.Failure.UNAVAILABLE)) {
            var calls = new AtomicInteger();
            try (var f = new Fixture((event, deadline) -> {
                calls.incrementAndGet(); throw new ModernMatchSettlement.SettlementException(failure);
            })) {
                f.hit(f.active()); await(() -> f.read(f.first).completion().state() == SettlementState.FAILED);
                assertEquals(failure == ModernMatchSettlement.Failure.CONFLICT ? 1 : 3, calls.get());
            }
        }
    }

    @Test void closeWaitsForActualSettlementWorkerEvenAfterEveryFollowerReturns() throws Exception {
        var entered = new CountDownLatch(1); var finish = new CountDownLatch(1);
        try (var f = new Fixture((event, deadline) -> { entered.countDown(); release(finish); })) {
            try {
                var result = f.hit(f.active()).frame().world(); assertTrue(entered.await(1, TimeUnit.SECONDS));
                f.leave(f.first, result); f.leave(f.second, result);
                f.service.close(); assertFalse(f.service.awaitTermination(20)); assertFalse(f.service.isQuiescent());
            } finally { finish.countDown(); }
            assertTrue(f.service.awaitTermination(3_000)); assertTrue(f.service.isQuiescent());
        }
    }

    @Test void prepareCannotEraseUnpublishedFinalHitAfterRoomReleaseButBeforeTerminalPublication() throws Exception {
        var calls = new AtomicInteger();
        try (var f = new Fixture((event, deadline) -> calls.incrementAndGet())) {
            var active = f.active();
            var gateField = ModernWorldService.class.getDeclaredField("gate"); gateField.setAccessible(true);
            var registrationsField = ModernWorldService.class.getDeclaredField("registrations"); registrationsField.setAccessible(true);
            Object gate = gateField.get(f.service), entry, firstRegistration;
            java.lang.reflect.Field entryField, followersField;
            ModernBattleEngine engine;
            CountDownLatch released, published;
            synchronized (gate) {
                firstRegistration = ((Map<?, ?>) registrationsField.get(f.service)).get(f.first.owner());
                entryField = firstRegistration.getClass().getDeclaredField("entry"); entryField.setAccessible(true);
                entry = entryField.get(firstRegistration);
                var engineField = entry.getClass().getDeclaredField("engine"); engineField.setAccessible(true);
                engine = (ModernBattleEngine) engineField.get(entry);
                var releasedField = entry.getClass().getDeclaredField("releaseDone"); releasedField.setAccessible(true);
                released = (CountDownLatch) releasedField.get(entry);
                var publishedField = entry.getClass().getDeclaredField("terminationDone"); publishedField.setAccessible(true);
                published = (CountDownLatch) publishedField.get(entry);
                followersField = entry.getClass().getDeclaredField("followers"); followersField.setAccessible(true);
            }
            var workers = java.util.concurrent.Executors.newFixedThreadPool(2);
            try {
                java.util.concurrent.Future<?> left;
                ModernMatchSettlement.Event committed;
                // Reflection only locates the real captured engine/lifecycle latches.
                // This is the real authority attack with actual owner/scope; the
                // outer monitor delays Service's subsequent engine.tick(), not
                // the preparation Guard or an invented result/publication hook.
                synchronized (engine) {
                    var hit = engine.attack(f.first.owner(), f.first.scope(), active.matchId(), active.generation(), 1, 2);
                    assertEquals(ModernBattleEngine.Phase.FINISHED, hit.snapshot().phase());
                    committed = hit.snapshot().result(); assertNotNull(committed);
                    left = workers.submit(() -> f.leave(f.second, active));
                    assertTrue(released.await(2, TimeUnit.SECONDS), "room release must really finish while terminal publication is held");
                    assertFalse(f.rooms.rooms.isPreparing(f.first.owner()));
                    assertEquals(1, published.getCount()); assertFalse(left.isDone()); assertEquals(0, calls.get());
                    f.ready(); // The waiting room can legitimately become ready before world terminal publication.
                    var preparing = workers.submit(() -> { return f.prepare(); });
                    var rejected = preparing.get(2, TimeUnit.SECONDS);
                    assertEquals(Status.BUSY, rejected.status()); assertEquals(Reason.INVALID_STATE, rejected.reason());
                    synchronized (gate) {
                        assertSame(entry, entryField.get(firstRegistration));
                        assertEquals(2, ((java.util.Set<?>) followersField.get(entry)).size(), "neither unacknowledged follower may be forgotten");
                    }
                }
                left.get(2, TimeUnit.SECONDS);
                await(() -> f.read(f.first).completion().state() == SettlementState.SAVED);
                var retained = f.read(f.first);
                assertEquals(Phase.FINISHED, retained.phase()); assertEquals(committed, retained.completion().event());
                assertEquals(1, calls.get()); assertNull(f.read(f.second));
                assertEquals(Status.BUSY, f.prepare().status());
                f.leave(f.first, retained);
                assertNull(f.read(f.first)); assertNull(f.read(f.second));
                var next = f.active(); assertNotEquals(committed.matchId(), next.matchId());
            } finally {
                // Every assertion path exits the engine monitor before this
                // join, so a failed regression cannot leave a detached caller.
                workers.shutdownNow(); assertTrue(workers.awaitTermination(3, TimeUnit.SECONDS));
            }
        }
    }

    @Test void admittedFirstSettlementTaskRemainsOwnedBeforeItsWorkerActionStarts() throws Exception {
        var entered = new CountDownLatch(1); var runAction = new CountDownLatch(1);
        var calls = new AtomicInteger(); var barrierFailure = new AtomicReference<String>();
        try (var f = new Fixture((event, deadline) -> calls.incrementAndGet())) {
            var poolField = ModernWorldService.class.getDeclaredField("settlementWorkers"); poolField.setAccessible(true);
            var pool = (java.util.concurrent.ThreadPoolExecutor) poolField.get(f.service);
            var gateField = ModernWorldService.class.getDeclaredField("gate"); gateField.setAccessible(true);
            var ownedField = ModernWorldService.class.getDeclaredField("settlementTasks"); ownedField.setAccessible(true);
            Object gate = gateField.get(f.service);
            assertEquals(0, pool.getPoolSize()); // No result has ever been submitted.
            pool.setThreadFactory(action -> new Thread(() -> {
                entered.countDown();
                try {
                    if (!runAction.await(3, TimeUnit.SECONDS)) barrierFailure.set("first task barrier timed out");
                } catch (InterruptedException interrupted) {
                    barrierFailure.set("first task barrier interrupted"); Thread.currentThread().interrupt();
                } finally {
                    // Always invoke the executor's real Worker, even if a test
                    // assertion/cancellation fails; do not strand its firstTask.
                    action.run();
                }
            }, "test-owned-settlement-handoff"));
            try {
                var result = f.hit(f.active()).frame().world();
                assertTrue(entered.await(1, TimeUnit.SECONDS)); assertEquals(0, calls.get());
                f.leave(f.first, result); f.leave(f.second, result);
                await(() -> f.service.matchCount() == 0);
                synchronized (gate) { assertEquals(1, ownedField.getInt(f.service)); }
                assertFalse(f.service.isQuiescent());
                f.service.close(); assertFalse(f.service.awaitTermination(20)); assertFalse(f.service.isQuiescent());
                // This proves explicit task ownership before actual execution.
                // JDK's initial Worker lock is already counted as active; this
                // barrier is not claimed to reproduce getTask's tiny poll/lock gap.
            } finally { runAction.countDown(); }
            assertTrue(f.service.awaitTermination(3_000)); assertTrue(f.service.isQuiescent());
            synchronized (gate) { assertEquals(0, ownedField.getInt(f.service)); }
            assertEquals(1, calls.get()); assertNull(barrierFailure.get());
        }
    }
}
