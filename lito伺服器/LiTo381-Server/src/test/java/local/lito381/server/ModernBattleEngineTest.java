package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import org.junit.jupiter.api.Test;

class ModernBattleEngineTest {
    private static final ModernWorldMap.SectorKey SECTOR = new ModernWorldMap.SectorKey(0x8000, 0x8000);
    private static final long STEP = TimeUnit.MILLISECONDS.toNanos(200);
    private static final ModernBattleEngine.Limits LIMITS = new ModernBattleEngine.Limits(10_000, TimeUnit.SECONDS.toNanos(3), 60_000);
    private static final ModernBattleEngine.Limits IMMEDIATE = new ModernBattleEngine.Limits(10_000, 0, 60_000);
    private static ModernWorldMap.Cell cell(int x, int y) {
        return new ModernWorldMap.Cell(SECTOR.origin().x() + x, SECTOR.origin().y() + y);
    }
    private static ModernWorldMap flatMap(int physicalMap) throws Exception {
        return ModernWorldMap.fromSectorBytes(physicalMap, Map.of(SECTOR, ModernWorldMapTest.openSectorBytes()), () -> false);
    }
    private static final class Fixture implements AutoCloseable {
        final ModernRoomsTest.Fixture rooms = new ModernRoomsTest.Fixture();
        final ModernRoomsTest.Player first = rooms.player(7, Long.MAX_VALUE, Long.MAX_VALUE, 0);
        final ModernRoomsTest.Player second = rooms.player(42, Long.MAX_VALUE, Long.MAX_VALUE, 0);
        final ModernRooms.FrozenPreparation ticket;
        final AtomicLong clock;
        final ModernWorldMap map;
        final ModernWorldBinding binding;
        final List<ModernBattleEngine.PreparedMember> prepared;
        final ModernBattleEngine.Limits limits;
        ModernBattleEngine engine;
        Fixture() throws Exception { this(flatMap(514), LIMITS, 1_000_000_000L, List.of(cell(2, 2), cell(6, 2))); }
        Fixture(ModernWorldMap map, ModernBattleEngine.Limits limits, long now, List<ModernWorldMap.Cell> spawns) throws Exception {
            this.map = map; this.limits = limits; clock = new AtomicLong(now); binding = new ModernWorldBinding(1, 514, map);
            ModernRoomsTest.accepted(rooms.create(first, ModernRoomsTest.draft(100, 1, 514, 0)));
            ModernRoomsTest.accepted(rooms.join(second, rooms.view(first).joined().summary(), ""));
            rooms.act(first, ModernRooms.Action.READY, 1); rooms.act(second, ModernRooms.Action.READY, 1);
            var current = rooms.view(first).joined().summary();
            var preparedRoom = rooms.rooms.prepareWorld(first.owner(), first.scope(), current.id(), current.revision());
            ModernRoomsTest.accepted(preparedRoom.result()); ticket = preparedRoom.ticket();
            var values = new ArrayList<ModernBattleEngine.PreparedMember>();
            for (int index = 0; index < ticket.participants().size(); index++) {
                var participant = ticket.participants().get(index); var member = participant.member();
                values.add(new ModernBattleEngine.PreparedMember(participant.owner(), participant.scope(),
                        new ModernApplicationProtocol.CharacterRow(member.key().objectId(), member.name(), member.characterClass(),
                                member.sex(), member.level()), spawns.get(index), STEP));
            }
            prepared = List.copyOf(values);
            engine = new ModernBattleEngine(ticket, binding, prepared, limits, clock::get);
        }
        ModernBattleEngine.Snapshot view() { return engine.snapshot(first.owner(), first.scope(), ticket.matchId(), ticket.generation()); }
        ModernBattleEngine.Snapshot ack(ModernRoomsTest.Player player) {
            return engine.ackLoaded(player.owner(), player.scope(), ticket.matchId(), ticket.generation(), map.digest());
        }
        ModernBattleEngine.Snapshot active() {
            ack(first); ack(second); clock.addAndGet(limits.countdownNanos()); return engine.tick();
        }
        ModernBattleEngine.MoveResult move(ModernRoomsTest.Player player, long seq, ModernWorldMap.Cell from, int heading) {
            return engine.move(player.owner(), player.scope(), ticket.matchId(), ticket.generation(), seq, from.x(), from.y(), heading);
        }
        ModernRooms.FrozenPreparation recapture() {
            rooms.rooms.releasePreparation(ticket);
            rooms.act(first, ModernRooms.Action.READY, 1); rooms.act(second, ModernRooms.Action.READY, 1);
            var current = rooms.view(first).joined().summary();
            return rooms.rooms.prepareWorld(first.owner(), first.scope(), current.id(), current.revision()).ticket();
        }
        @Override public void close() { engine.close(); rooms.rooms.releasePreparation(ticket); rooms.rooms.disconnect(first.owner()); rooms.rooms.disconnect(second.owner()); }
    }

    @Test void capturedRosterGeneratesIndependentEntityHandlesAndPreservesFullSourceIdentity() throws Exception {
        try (var f = new Fixture()) {
            var snapshot = f.view(); assertEquals(ModernBattleEngine.Phase.LOADING, snapshot.phase());
            assertEquals(1, snapshot.revision()); assertEquals(List.of(1L, 2L), snapshot.actors().stream().map(ModernBattleEngine.Actor::entityId).toList());
            assertEquals(List.of(7, 42), snapshot.actors().stream().map(actor -> actor.member().key().sourceId()).toList());
            assertTrue(snapshot.actors().stream().allMatch(actor -> actor.member().key().objectId() == Long.MAX_VALUE && actor.member().level() == Long.MAX_VALUE));
            assertEquals(f.ticket.snapshot(), snapshot.room()); assertEquals(f.map.manifest(), snapshot.manifest());
            assertThrows(UnsupportedOperationException.class, () -> snapshot.actors().clear());
        }
    }

    @Test void wholeRosterMustLoadThenRealThreeSecondServerCountdownElapses() throws Exception {
        try (var f = new Fixture()) {
            var first = f.ack(f.first); assertEquals(ModernBattleEngine.Phase.LOADING, first.phase());
            assertTrue(first.actors().get(0).loaded()); assertFalse(first.actors().get(1).loaded());
            var second = f.ack(f.second); assertEquals(ModernBattleEngine.Phase.COUNTDOWN, second.phase());
            f.clock.addAndGet(TimeUnit.SECONDS.toNanos(3) - 1); assertEquals(ModernBattleEngine.Phase.COUNTDOWN, f.engine.tick().phase());
            f.clock.incrementAndGet(); var active = f.engine.tick(); assertEquals(ModernBattleEngine.Phase.ACTIVE, active.phase());
            assertEquals(second.revision() + 1, active.revision());
        }
    }

    @Test void duplicateLoadAckIsIdempotentAndCannotRestartCountdown() throws Exception {
        try (var f = new Fixture()) {
            f.ack(f.first); assertEquals(f.view(), f.ack(f.first));
            var countdown = f.ack(f.second); f.clock.addAndGet(TimeUnit.SECONDS.toNanos(2));
            assertEquals(countdown, f.ack(f.second)); f.clock.addAndGet(TimeUnit.SECONDS.toNanos(1));
            assertEquals(ModernBattleEngine.Phase.ACTIVE, f.engine.tick().phase());
        }
    }

    @Test void zeroCountdownIsAnExplicitLimitRatherThanClientTimeAuthority() throws Exception {
        try (var f = new Fixture(flatMap(514), IMMEDIATE, -100, List.of(cell(2, 2), cell(6, 2)))) {
            f.ack(f.first); assertEquals(ModernBattleEngine.Phase.ACTIVE, f.ack(f.second).phase());
            assertEquals(ModernBattleEngine.MoveStatus.MOVED, f.move(f.first, 1, cell(2, 2), 2).status());
        }
    }

    @Test void manifestOwnerScopeAndMatchGenerationAreStrictBeforeMutations() throws Exception {
        try (var f = new Fixture()) {
            var before = f.view();
            assertThrows(SecurityException.class, () -> f.engine.ackLoaded(f.first.owner(), f.first.scope(), f.ticket.matchId(), f.ticket.generation(), "0".repeat(64)));
            assertThrows(SecurityException.class, () -> f.engine.snapshot(f.second.owner(), f.first.scope(), f.ticket.matchId(), f.ticket.generation()));
            assertThrows(SecurityException.class, () -> f.engine.snapshot(f.first.owner(), f.first.scope(), f.ticket.matchId() + 1, f.ticket.generation()));
            assertThrows(SecurityException.class, () -> f.engine.snapshot(f.first.owner(), f.first.scope(), f.ticket.matchId(), f.ticket.generation() + 1));
            var stale = new ModernRooms.Scope(7, Long.MAX_VALUE, 2, 0);
            assertThrows(SecurityException.class, () -> f.engine.snapshot(f.first.owner(), stale, f.ticket.matchId(), f.ticket.generation()));
            assertEquals(before, f.view());
        }
    }

    @Test void movementBeforeActivationConsumesSequenceButNeverPosition() throws Exception {
        try (var f = new Fixture()) {
            var result = f.move(f.first, 1, cell(2, 2), 2); assertEquals(ModernBattleEngine.MoveStatus.NOT_ACTIVE, result.status());
            assertEquals(cell(2, 2), result.snapshot().actors().get(0).position()); assertEquals(1, result.snapshot().actors().get(0).lastMoveSequence());
            f.ack(f.first); f.ack(f.second); assertEquals(ModernBattleEngine.MoveStatus.NOT_ACTIVE, f.move(f.first, 2, cell(2, 2), 2).status());
            f.clock.addAndGet(LIMITS.countdownNanos()); f.engine.tick();
            assertEquals(ModernBattleEngine.MoveStatus.MOVED, f.move(f.first, 3, cell(2, 2), 2).status());
        }
    }

    @Test void sequenceRejectsReplaysGapsZeroAndUnknownHeadingWithoutPartialMutation() throws Exception {
        try (var f = new Fixture()) {
            f.active(); var before = f.view();
            assertThrows(SecurityException.class, () -> f.move(f.first, 2, cell(2, 2), 2));
            assertThrows(IllegalArgumentException.class, () -> f.move(f.first, 0, cell(2, 2), 2));
            assertThrows(IllegalArgumentException.class, () -> f.move(f.first, 1, cell(2, 2), 8));
            assertEquals(before, f.view()); var moved = f.move(f.first, 1, cell(2, 2), 2).snapshot();
            assertThrows(SecurityException.class, () -> f.move(f.first, 1, cell(2, 2), 2)); assertEquals(moved, f.view());
        }
    }

    @Test void paceGateHasExactBoundaryAndRejectedMovesDoNotResetAcceptedMovementClock() throws Exception {
        try (var f = new Fixture()) {
            f.active(); assertEquals(ModernBattleEngine.MoveStatus.MOVED, f.move(f.first, 1, cell(2, 2), 2).status());
            f.clock.addAndGet(STEP - 1);
            var denied = f.move(f.first, 2, cell(3, 2), 2); assertEquals(ModernBattleEngine.MoveStatus.TOO_FAST, denied.status());
            assertEquals(cell(3, 2), denied.snapshot().actors().get(0).position());
            f.clock.incrementAndGet(); assertEquals(ModernBattleEngine.MoveStatus.MOVED, f.move(f.first, 3, cell(3, 2), 2).status());
        }
    }

    @Test void staleSourceCoordinatesAreCorrectedByTheAuthoritativeSnapshot() throws Exception {
        try (var f = new Fixture()) {
            f.active(); var result = f.move(f.first, 1, cell(3, 2), 2);
            assertEquals(ModernBattleEngine.MoveStatus.STALE_POSITION, result.status());
            assertEquals(cell(2, 2), result.snapshot().actors().get(0).position()); assertEquals(1, result.snapshot().actors().get(0).lastMoveSequence());
        }
    }

    @Test void selectedSectorBoundaryAndDirectionalClosedEdgesAreEnforced() throws Exception {
        byte[] bytes = ModernWorldMapTest.openSectorBytes(); ModernWorldMapTest.setAttributes(bytes, 2, 2, (short) 1, (short) 0);
        var map = ModernWorldMap.fromSectorBytes(514, Map.of(SECTOR, bytes), () -> false);
        try (var f = new Fixture(map, IMMEDIATE, 0, List.of(cell(2, 2), cell(6, 2)))) {
            f.active(); assertEquals(ModernBattleEngine.MoveStatus.BLOCKED, f.move(f.first, 1, cell(2, 2), 0).status());
            assertEquals(ModernBattleEngine.MoveStatus.MOVED, f.move(f.first, 2, cell(2, 2), 2).status());
        }
        try (var f = new Fixture(flatMap(514), IMMEDIATE, 0, List.of(cell(0, 2), cell(6, 2)))) {
            f.active(); assertEquals(ModernBattleEngine.MoveStatus.BLOCKED, f.move(f.first, 1, cell(0, 2), 6).status());
        }
    }

    @Test void occupantsCannotOverlapButVacatedCellsBecomeAvailable() throws Exception {
        try (var f = new Fixture(flatMap(514), IMMEDIATE, 0, List.of(cell(2, 2), cell(3, 2)))) {
            f.active(); assertEquals(ModernBattleEngine.MoveStatus.OCCUPIED, f.move(f.first, 1, cell(2, 2), 2).status());
            assertEquals(ModernBattleEngine.MoveStatus.MOVED, f.move(f.second, 1, cell(3, 2), 2).status());
            assertEquals(ModernBattleEngine.MoveStatus.MOVED, f.move(f.first, 2, cell(2, 2), 2).status());
            assertEquals(2, f.view().actors().stream().map(ModernBattleEngine.Actor::position).distinct().count());
        }
    }

    @Test void loadTimeoutAndActiveLifecycleTimeoutAbortWithoutInventingBattleResults() throws Exception {
        try (var f = new Fixture()) {
            f.ack(f.first); f.clock.addAndGet(TimeUnit.MILLISECONDS.toNanos(LIMITS.loadTimeoutMillis()));
            var timed = f.ack(f.second); assertEquals(ModernBattleEngine.AbortReason.LOAD_TIMEOUT, timed.abortReason());
            assertFalse(timed.actors().get(1).loaded()); assertEquals(ModernBattleEngine.Phase.ABORTED, timed.phase());
        }
        try (var f = new Fixture()) {
            f.active(); f.clock.addAndGet(TimeUnit.MILLISECONDS.toNanos(LIMITS.activeTimeoutMillis()));
            assertEquals(ModernBattleEngine.AbortReason.ACTIVE_TIMEOUT, f.engine.tick().abortReason());
            assertEquals(ModernBattleEngine.MoveStatus.ABORTED, f.move(f.first, 1, cell(2, 2), 2).status());
        }
    }

    @Test void lateSchedulerTickCannotExtendCountdownOrActiveLifetime() throws Exception {
        try (var f = new Fixture()) {
            f.ack(f.first); f.ack(f.second);
            f.clock.addAndGet(LIMITS.countdownNanos() + TimeUnit.MILLISECONDS.toNanos(LIMITS.activeTimeoutMillis()));
            assertEquals(ModernBattleEngine.AbortReason.ACTIVE_TIMEOUT, f.engine.tick().abortReason());
        }
    }

    @Test void monotonicSignedOriginWrapWorksButClockRegressionAborts() throws Exception {
        try (var f = new Fixture(flatMap(514), LIMITS, Long.MAX_VALUE - TimeUnit.SECONDS.toNanos(2), List.of(cell(2, 2), cell(6, 2)))) {
            assertEquals(ModernBattleEngine.Phase.ACTIVE, f.active().phase());
            assertEquals(ModernBattleEngine.MoveStatus.MOVED, f.move(f.first, 1, cell(2, 2), 2).status());
        }
        try (var f = new Fixture()) {
            f.clock.decrementAndGet(); assertEquals(ModernBattleEngine.AbortReason.CLOCK_REGRESSED, f.engine.tick().abortReason());
        }
    }

    @Test void disconnectAbortsWholeMatchAndCannotBeUsedToControlAnotherActorsSeat() throws Exception {
        try (var f = new Fixture()) {
            f.active(); f.engine.disconnect(f.rooms.presence.connect()); assertEquals(ModernBattleEngine.Phase.ACTIVE, f.view().phase());
            f.engine.disconnect(f.second.owner()); assertEquals(1, f.engine.ownerCount());
            assertEquals(ModernBattleEngine.AbortReason.DISCONNECTED, f.view().abortReason());
            assertThrows(SecurityException.class, () -> f.engine.snapshot(f.second.owner(), f.second.scope(), f.ticket.matchId(), f.ticket.generation()));
            assertEquals(ModernBattleEngine.MoveStatus.ABORTED, f.move(f.first, 1, cell(2, 2), 2).status());
            f.engine.disconnect(f.second.owner()); assertEquals(1, f.engine.ownerCount());
        }
    }

    @Test void releasedRoomTicketPreventsLateLoadedAckAndMoveCommit() throws Exception {
        try (var f = new Fixture()) {
            f.active(); var before = f.view(); assertTrue(f.rooms.rooms.releasePreparation(f.ticket));
            var result = f.move(f.first, 1, cell(2, 2), 2); assertEquals(ModernBattleEngine.MoveStatus.ABORTED, result.status());
            assertEquals(ModernBattleEngine.AbortReason.PREPARATION_CANCELLED, result.snapshot().abortReason());
            assertEquals(before.actors(), result.snapshot().actors()); assertEquals(result.snapshot(), f.ack(f.second));
        }
    }

    @Test void cancellationWinningGuardRaceCannotPublishQueuedGameplay() throws Exception {
        try (var f = new Fixture()) {
            f.active(); var before = f.view(); var workers = Executors.newSingleThreadExecutor();
            try {
                java.util.concurrent.Future<ModernBattleEngine.MoveResult> pending;
                try (var guard = f.ticket.guard()) {
                    pending = workers.submit(() -> f.move(f.first, 1, cell(2, 2), 2));
                    assertTrue(f.rooms.rooms.releasePreparation(f.ticket));
                }
                var result = pending.get(2, TimeUnit.SECONDS); assertEquals(ModernBattleEngine.MoveStatus.ABORTED, result.status());
                assertEquals(before.actors(), result.snapshot().actors());
            } finally { workers.shutdownNow(); assertTrue(workers.awaitTermination(2, TimeUnit.SECONDS)); }
        }
    }

    @Test void cancelledPreparationCannotConstructANewEngineAndPreparedRowsMustMatchExactly() throws Exception {
        try (var f = new Fixture()) {
            var first = f.prepared.get(0); var second = f.prepared.get(1);
            var mismatch = new ModernBattleEngine.PreparedMember(first.owner(), first.scope(),
                    new ModernApplicationProtocol.CharacterRow(Long.MAX_VALUE, first.character().name(), 6, 1, 99), first.spawn(), STEP);
            assertThrows(IllegalArgumentException.class, () -> new ModernBattleEngine(f.ticket, f.binding, List.of(mismatch, second), LIMITS, f.clock::get));
            assertThrows(IllegalArgumentException.class, () -> new ModernBattleEngine(f.ticket, f.binding, List.of(first), LIMITS, f.clock::get));
            assertThrows(IllegalArgumentException.class, () -> new ModernBattleEngine(f.ticket, f.binding, List.of(first, first), LIMITS, f.clock::get));
            assertThrows(IllegalArgumentException.class, () -> new ModernBattleEngine(f.ticket, f.binding, Collections.nCopies(101, first), LIMITS, f.clock::get));
            var overlap = new ModernBattleEngine.PreparedMember(second.owner(), second.scope(), second.character(), first.spawn(), STEP);
            assertThrows(IllegalArgumentException.class, () -> new ModernBattleEngine(f.ticket, f.binding, List.of(first, overlap), LIMITS, f.clock::get));
            var outside = new ModernBattleEngine.PreparedMember(second.owner(), second.scope(), second.character(), new ModernWorldMap.Cell(0, 0), STEP);
            assertThrows(IllegalArgumentException.class, () -> new ModernBattleEngine(f.ticket, f.binding, List.of(first, outside), LIMITS, f.clock::get));
            f.rooms.rooms.releasePreparation(f.ticket);
            assertThrows(IllegalStateException.class, () -> new ModernBattleEngine(f.ticket, f.binding, f.prepared, LIMITS, f.clock::get));
        }
    }

    @Test void preparedInputOrderDoesNotChangeEntityAssignmentAndPhysicalMapRequiresExplicitBinding() throws Exception {
        try (var f = new Fixture(flatMap(4), LIMITS, 0, List.of(cell(2, 2), cell(6, 2)))) {
            assertEquals(514, f.view().room().summary().config().mapId()); assertEquals(4, f.view().manifest().mapId());
            var originalActors = f.view().actors();
            var reversed = new ArrayList<>(f.prepared); Collections.reverse(reversed);
            var replacement = f.recapture();
            try (var other = new ModernBattleEngine(replacement, f.binding, reversed, LIMITS, f.clock::get)) {
                assertEquals(originalActors, other.tick().actors()); assertNotEquals(f.ticket.matchId(), other.tick().matchId());
            } finally { f.rooms.rooms.releasePreparation(replacement); }
            var another = f.recapture();
            try {
                assertThrows(IllegalArgumentException.class, () -> new ModernBattleEngine(another,
                        new ModernWorldBinding(1, 564, f.map), f.prepared, LIMITS, f.clock::get));
            } finally {
                f.rooms.rooms.releasePreparation(another);
            }
        }
    }

    @Test void limitsAndPreparedMovementIntervalsCannotDefaultUnknownFactsToZero() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> new ModernBattleEngine.Limits(0, 0, 100));
        assertThrows(IllegalArgumentException.class, () -> new ModernBattleEngine.Limits(100, -1, 100));
        assertThrows(IllegalArgumentException.class, () -> new ModernBattleEngine.Limits(100, TimeUnit.SECONDS.toNanos(31), 100));
        assertEquals(300_001, new ModernBattleEngine.Limits(100, 0, 300_001).activeTimeoutMillis());
        assertThrows(IllegalArgumentException.class, () -> new ModernBattleEngine.Limits(100, 0, 1_800_001));
        try (var f = new Fixture()) {
            var first = f.prepared.get(0);
            assertThrows(IllegalArgumentException.class, () -> new ModernBattleEngine.PreparedMember(first.owner(), first.scope(), first.character(), first.spawn(), 0));
            assertThrows(IllegalArgumentException.class, () -> new ModernBattleEngine.PreparedMember(first.owner(), first.scope(), first.character(), first.spawn(), Long.MAX_VALUE));
        }
    }

    @Test void positiveSequenceAndRevisionNeverWrapAndCloseRetiresCapabilities() throws Exception {
        try (var f = new Fixture()) {
            f.active(); Field actorsField = ModernBattleEngine.class.getDeclaredField("actors"); actorsField.setAccessible(true);
            Object actor = ((List<?>) actorsField.get(f.engine)).get(0); Field seq = actor.getClass().getDeclaredField("sequence"); seq.setAccessible(true);
            seq.setLong(actor, Long.MAX_VALUE - 1);
            assertEquals(Long.MAX_VALUE, f.move(f.first, Long.MAX_VALUE, cell(2, 2), 2).snapshot().actors().get(0).lastMoveSequence());
            assertThrows(SecurityException.class, () -> f.move(f.first, 1, cell(3, 2), 2));
            Field revision = ModernBattleEngine.class.getDeclaredField("revision"); revision.setAccessible(true); revision.setLong(f.engine, Long.MAX_VALUE);
            assertThrows(IllegalStateException.class, () -> f.engine.abort()); assertTrue(f.engine.isClosed()); assertEquals(0, f.engine.ownerCount());
            f.engine.close(); assertThrows(IllegalStateException.class, f::view);
        }
    }

    @Test void allEightHeadingDeltasUseTheSameGridAsSharedDirectionalGeometry() throws Exception {
        try (var f = new Fixture(flatMap(514), IMMEDIATE, 0, List.of(cell(10, 10), cell(30, 30)))) {
            f.active(); int[] dx = {0, 1, 1, 1, 0, -1, -1, -1}, dy = {-1, -1, 0, 1, 1, 1, 0, -1};
            var position = cell(10, 10);
            for (int heading = 0; heading < 8; heading++) {
                f.clock.addAndGet(STEP); var result = f.move(f.first, heading + 1, position, heading);
                assertEquals(ModernBattleEngine.MoveStatus.MOVED, result.status());
                position = new ModernWorldMap.Cell(position.x() + dx[heading], position.y() + dy[heading]);
                assertEquals(position, result.snapshot().actors().get(0).position());
                assertEquals(heading, result.snapshot().actors().get(0).heading());
            }
        }
    }

    @Test void onePreparationCanOnlyOwnOneEngineEvenAfterCloseAndFailedValidationDoesNotClaimIt() throws Exception {
        try (var f = new Fixture()) {
            assertThrows(IllegalStateException.class, () -> new ModernBattleEngine(f.ticket, f.binding, f.prepared, LIMITS, f.clock::get));
            f.engine.close();
            assertThrows(IllegalStateException.class, () -> new ModernBattleEngine(f.ticket, f.binding, f.prepared, LIMITS, f.clock::get));
            var replacement = f.recapture();
            try {
                assertThrows(IllegalArgumentException.class, () -> new ModernBattleEngine(replacement, f.binding, List.of(f.prepared.get(0)), LIMITS, f.clock::get));
                assertThrows(IllegalStateException.class, () -> new ModernBattleEngine(replacement, f.binding, f.prepared, LIMITS,
                        () -> { throw new IllegalStateException("test clock failed before claim"); }));
                try (var valid = new ModernBattleEngine(replacement, f.binding, f.prepared, LIMITS, f.clock::get)) {
                    assertEquals(ModernBattleEngine.Phase.LOADING, valid.tick().phase());
                }
                assertThrows(IllegalStateException.class, () -> new ModernBattleEngine(replacement, f.binding, f.prepared, LIMITS, f.clock::get));
            } finally { f.rooms.rooms.releasePreparation(replacement); }
        }
    }

    @Test void spawnConnectivityValidationDoesNotWaitForCancellationGuardAndCannotClaimCancelledTicket() throws Exception {
        try (var f = new Fixture()) {
            f.engine.close(); var replacement = f.recapture();
            byte[] bytes = ModernWorldMapTest.openSectorBytes();
            // Both spawns stand, but an entire sector-height wall separates
            // them. Rejection therefore exercises the actual connectivity BFS.
            for (int y = 0; y < 64; y++) ModernWorldMapTest.setAttributes(bytes, 4, y, (short) 0, (short) 1);
            var disconnected = ModernWorldMap.fromSectorBytes(514, Map.of(SECTOR, bytes), () -> false);
            assertTrue(disconnected.standing(f.prepared.get(0).spawn()));
            assertTrue(disconnected.standing(f.prepared.get(1).spawn()));
            var worker = Executors.newSingleThreadExecutor();
            try {
                java.util.concurrent.Future<IllegalStateException> cancelledConstruction;
                try (var ignored = replacement.guard()) {
                    var rejected = worker.submit(() -> assertThrows(IllegalArgumentException.class,
                            () -> new ModernBattleEngine(replacement, new ModernWorldBinding(1, 514, disconnected),
                                    f.prepared, LIMITS, f.clock::get)));
                    assertNotNull(rejected.get(2, TimeUnit.SECONDS), "immutable geometry validation must not wait for the Guard");
                    cancelledConstruction = worker.submit(() -> assertThrows(IllegalStateException.class,
                            () -> new ModernBattleEngine(replacement, f.binding, f.prepared, LIMITS, f.clock::get)));
                    assertTrue(f.rooms.rooms.releasePreparation(replacement));
                }
                assertNotNull(cancelledConstruction.get(2, TimeUnit.SECONDS));
                assertFalse(replacement.isLive());
            } finally {
                worker.shutdownNow(); assertTrue(worker.awaitTermination(2, TimeUnit.SECONDS));
                f.rooms.rooms.releasePreparation(replacement);
            }
        }
    }

    @Test void admittedMovementFinishesBeforeCancellationAndNothingCommitsAfterCancellationReturns() throws Exception {
        try (var f = new Fixture()) {
            f.engine.close(); var replacement = f.recapture();
            var armed = new AtomicBoolean(); var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
            LongSupplier clock = () -> {
                if (armed.compareAndSet(true, false)) {
                    entered.countDown();
                    try { if (!release.await(2, TimeUnit.SECONDS)) throw new IllegalStateException("test barrier timed out"); }
                    catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException("test interrupted"); }
                }
                return f.clock.get();
            };
            var workers = Executors.newFixedThreadPool(2);
            try (var engine = new ModernBattleEngine(replacement, f.binding, f.prepared, IMMEDIATE, clock)) {
                engine.ackLoaded(f.first.owner(), f.first.scope(), replacement.matchId(), replacement.generation(), f.map.digest());
                engine.ackLoaded(f.second.owner(), f.second.scope(), replacement.matchId(), replacement.generation(), f.map.digest());
                armed.set(true);
                var move = workers.submit(() -> engine.move(f.first.owner(), f.first.scope(), replacement.matchId(), replacement.generation(), 1,
                        cell(2, 2).x(), cell(2, 2).y(), 2));
                assertTrue(entered.await(1, TimeUnit.SECONDS), "move holds the ticket Guard before cancellation begins");
                var cancelEntered = new CountDownLatch(1);
                var cancel = workers.submit(() -> { cancelEntered.countDown(); return f.rooms.rooms.releasePreparation(replacement); });
                assertTrue(cancelEntered.await(1, TimeUnit.SECONDS));
                assertThrows(TimeoutException.class, () -> cancel.get(50, TimeUnit.MILLISECONDS));
                release.countDown(); assertEquals(ModernBattleEngine.MoveStatus.MOVED, move.get(2, TimeUnit.SECONDS).status());
                assertTrue(cancel.get(2, TimeUnit.SECONDS));
                var before = engine.tick(); assertEquals(ModernBattleEngine.AbortReason.PREPARATION_CANCELLED, before.abortReason());
                var rejected = engine.move(f.first.owner(), f.first.scope(), replacement.matchId(), replacement.generation(), 2,
                        cell(3, 2).x(), cell(3, 2).y(), 2);
                assertEquals(ModernBattleEngine.MoveStatus.ABORTED, rejected.status()); assertEquals(before.actors(), rejected.snapshot().actors());
            } finally {
                release.countDown(); workers.shutdownNow(); assertTrue(workers.awaitTermination(2, TimeUnit.SECONDS));
                f.rooms.rooms.releasePreparation(replacement);
            }
        }
    }

    @Test void completeHundredPlayerPreparationIsBoundedAndEverySpawnAndEntityIsUnique() throws Exception {
        var rooms = new ModernRoomsTest.Fixture(); var players = new ArrayList<ModernRoomsTest.Player>();
        var owner = rooms.player(); players.add(owner); ModernRoomsTest.accepted(rooms.create(owner, ModernRoomsTest.draft(100, 1, 514, 0)));
        for (int index = 1; index < 100; index++) {
            var player = rooms.player(); players.add(player); ModernRoomsTest.accepted(rooms.join(player, rooms.view(owner).joined().summary(), ""));
        }
        for (var player : players) ModernRoomsTest.accepted(rooms.act(player, ModernRooms.Action.READY, 1));
        var summary = rooms.view(owner).joined().summary();
        var ticket = rooms.rooms.prepareWorld(owner.owner(), owner.scope(), summary.id(), summary.revision()).ticket();
        assertNotNull(ticket); var map = flatMap(514);
        var plan = ModernWorldSpawns.allocate(map,ticket.snapshot().members(),List.of(cell(10,10)),java.util.Set.of(),()->false);
        var spawns = plan.positions();
        assertEquals(plan.escapeCell(),ModernWorldSpawns.qualify(map,ticket.snapshot().members(),spawns,java.util.Set.of(),()->false));
        var prepared = new ArrayList<ModernBattleEngine.PreparedMember>();
        for (int index = 0; index < 100; index++) {
            var row = ticket.participants().get(index); var member = row.member();
            prepared.add(new ModernBattleEngine.PreparedMember(row.owner(), row.scope(), new ModernApplicationProtocol.CharacterRow(
                    member.key().objectId(), member.name(), member.characterClass(), member.sex(), member.level()), spawns.get(index), STEP));
        }
        try (var engine = new ModernBattleEngine(ticket, new ModernWorldBinding(1, 514, map), prepared, IMMEDIATE, () -> 0L)) {
            assertEquals(100, engine.ownerCount());
            for (var row : ticket.participants()) engine.ackLoaded(row.owner(), row.scope(), ticket.matchId(), ticket.generation(), map.digest());
            var snapshot = engine.tick(); assertEquals(ModernBattleEngine.Phase.ACTIVE, snapshot.phase());
            assertEquals(100, snapshot.actors().size()); assertEquals(100, snapshot.actors().stream().map(ModernBattleEngine.Actor::position).distinct().count());
            assertEquals(100, snapshot.actors().get(99).entityId());
        } finally {
            rooms.rooms.releasePreparation(ticket); for (var player : players) rooms.rooms.disconnect(player.owner());
        }
    }
}
