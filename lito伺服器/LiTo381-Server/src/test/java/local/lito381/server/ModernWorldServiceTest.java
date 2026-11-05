package local.lito381.server;

import static local.lito381.server.ModernWorldProtocol.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;

class ModernWorldServiceTest {
    private static final String ZERO = "0".repeat(64);
    private static final ModernWorldMap.SectorKey SECTOR = new ModernWorldMap.SectorKey(0x8000, 0x8000);
    private static final ModernWorldMap.Cell ANCHOR = new ModernWorldMap.Cell(32778, 32778);
    private static final ModernBattleEngine.Limits ENGINE = new ModernBattleEngine.Limits(2_000, TimeUnit.MILLISECONDS.toNanos(40), 2_000);
    private static ModernWorldService.Limits limits(int matches, int workers, int queue, int timeout, ModernBattleEngine.Limits engine) {
        return new ModernWorldService.Limits(matches, workers, queue, timeout, 5, engine);
    }
    private static final class Fixture implements AutoCloseable {
        final ModernRoomsTest.Fixture rooms = new ModernRoomsTest.Fixture();
        final List<ModernRoomsTest.Player> players = new ArrayList<>();
        final ModernRoomsTest.Player first, second;
        final ModernWorldMap map;
        final ModernWorldService service;
        final AtomicInteger reads = new AtomicInteger();
        Fixture() throws Exception { this(limits(4, 2, 2, 2_000, ENGINE), 514); }
        Fixture(ModernWorldService.Limits limits, int logicalMap) throws Exception {
            map = ModernWorldMap.fromSectorBytes(4, Map.of(SECTOR, ModernWorldMapTest.openSectorBytes()), () -> false);
            var binding = new ModernWorldBinding(1, 514, map);
            service = new ModernWorldService(rooms.rooms, Map.of(binding.key(), new ModernWorldService.Mapping(binding, ANCHOR,
                    TimeUnit.MILLISECONDS.toNanos(100))), limits);
            first = player(7, Long.MAX_VALUE); second = player(42, Long.MAX_VALUE);
            ModernRoomsTest.accepted(rooms.create(first, ModernRoomsTest.draft(100, 1, logicalMap, 0)));
            ModernRoomsTest.accepted(rooms.join(second, rooms.view(first).joined().summary(), ""));
            register(first); register(second); ready(first); ready(second);
        }
        ModernRoomsTest.Player player(int source, long object) {
            var player = rooms.player(source, object, Long.MAX_VALUE, 0); players.add(player); return player;
        }
        ModernApplicationProtocol.CharacterRow row(ModernRoomsTest.Player player) {
            var selected = rooms.presence.selected(player.owner());
            return new ModernApplicationProtocol.CharacterRow(selected.key().objectId(), selected.characterName(),
                    selected.characterClass(), selected.sex(), selected.level());
        }
        void register(ModernRoomsTest.Player player) {
            service.register(player.owner(), player.scope(), deadline -> { reads.incrementAndGet(); return row(player); });
        }
        void ready(ModernRoomsTest.Player player) { ModernRoomsTest.accepted(rooms.act(player, ModernRooms.Action.READY, 1)); }
        Reply prepare() { return service.handle(first.owner(), prepareRequest(first, rooms.view(first).joined().summary())); }
        Frame read(ModernRoomsTest.Player player) { return service.handle(player.owner(), readRequest(player)).frame(); }
        Frame loading() throws Exception {
            var prepared = prepare(); assertEquals(Status.OK, prepared.status()); assertNotNull(prepared.frame());
            await(() -> read(first).phase() == Phase.LOADING); return read(first);
        }
        Frame active() throws Exception {
            Frame frame = loading();
            service.handle(first.owner(), loaded(first, frame)); service.handle(second.owner(), loaded(second, frame));
            await(() -> read(first).phase() == Phase.ACTIVE); return read(first);
        }
        @Override public void close() throws Exception {
            service.close(); assertTrue(service.awaitTermination(4_000));
            for (var player : players) { rooms.rooms.disconnect(player.owner()); rooms.presence.disconnect(player.owner()); }
        }
    }
    private static Request readRequest(ModernRoomsTest.Player player) {
        return new Request(player.scope(), Action.READ, 0, 0, 0, 0, 0, 0, 0, 0, ZERO);
    }
    private static Request prepareRequest(ModernRoomsTest.Player player, ModernRooms.Summary room) {
        return new Request(player.scope(), Action.PREPARE, 0, room.id(), room.revision(), 0, 0, 0, 0, 0, ZERO);
    }
    private static Request loaded(ModernRoomsTest.Player player, Frame frame) {
        return new Request(player.scope(), Action.LOADED, 0, 0, 0, frame.matchId(), frame.generation(), 0, 0, 0, frame.manifest().digest());
    }
    private static Request leave(ModernRoomsTest.Player player, Frame frame) {
        return new Request(player.scope(), Action.LEAVE, 0, 0, 0, frame.matchId(), frame.generation(), 0, 0, 0, ZERO);
    }
    private static Request move(ModernRoomsTest.Player player, Frame frame, long sequence, int heading, ModernWorldMap.Cell from) {
        return new Request(player.scope(), Action.MOVE, heading, 0, 0, frame.matchId(), frame.generation(), sequence, from.x(), from.y(), ZERO);
    }
    private static void await(BooleanSupplier condition) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (!condition.getAsBoolean()) { assertTrue(System.nanoTime() < end, "bounded condition did not complete"); Thread.sleep(5); }
    }
    /** LEAVE retires followers synchronously, but the preparation worker can
     * still own capacity until its real finally. Keep the exact zero assertion. */
    static void awaitNoMatches(ModernWorldService service) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (service.matchCount() != 0 && System.nanoTime() < deadline) {
            if (Thread.interrupted()) throw new InterruptedException("waiting for world retirement");
            java.util.concurrent.locks.LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
        }
        assertEquals(0, service.matchCount(), "preparation worker did not retire within 3 seconds");
    }
    private static void blocking(CountDownLatch release) throws Exception {
        if (!release.await(3, TimeUnit.SECONDS)) throw new IllegalStateException("test reader barrier timed out");
    }

    @Test void preparingReplyIsImmediateWhileRealRegisteredReaderIsStillBlocked() throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        try (var f = new Fixture()) {
            f.service.register(f.first.owner(), f.first.scope(), deadline -> { entered.countDown(); blocking(release); return f.row(f.first); });
            long started = System.nanoTime(); Reply reply = f.prepare();
            assertTrue(System.nanoTime() - started < TimeUnit.SECONDS.toNanos(1));
            assertEquals(Status.OK, reply.status()); assertEquals(Phase.PREPARING, reply.frame().phase());
            assertTrue(entered.await(1, TimeUnit.SECONDS)); assertEquals(1, f.service.active());
            assertTrue(f.rooms.rooms.isPreparing(f.first.owner()));
            assertEquals(Reason.NOT_ACTIVE, f.service.handle(f.first.owner(), loaded(f.first, reply.frame())).reason());
            release.countDown(); await(() -> f.read(f.first).phase() == Phase.LOADING);
        } finally { release.countDown(); }
    }

    @Test void exactReadersKeepSameI64ObjectInDifferentSourcesAndExplicitPhysicalBinding() throws Exception {
        try (var f = new Fixture()) {
            var order = new ArrayList<Integer>(); var deadlines = new ArrayList<Long>();
            for (var player : List.of(f.first, f.second)) f.service.register(player.owner(), player.scope(), deadline -> {
                synchronized (order) { order.add(player.scope().sourceId()); deadlines.add(deadline); } return f.row(player);
            });
            var frame = f.loading(); assertEquals(4, frame.manifest().mapId()); assertEquals(514, frame.room().summary().config().mapId());
            assertEquals(List.of(7, 42), order); assertEquals(2, deadlines.size()); assertEquals(deadlines.get(0), deadlines.get(1));
            assertEquals(List.of(1L, 2L), frame.actors().stream().map(ModernBattleEngine.Actor::entityId).toList());
            assertTrue(frame.actors().stream().allMatch(actor -> actor.member().key().objectId() == Long.MAX_VALUE && actor.member().level() == Long.MAX_VALUE));
            assertNotEquals(frame.actors().get(0).position(), frame.actors().get(1).position());
        }
    }

    @Test void onlyTrustedRegisteredOwnerAndAllFourScopeFieldsAreAccepted() throws Exception {
        try (var f = new Fixture()) {
            var scope = f.first.scope();
            for (var wrong : List.of(new ModernRooms.Scope(42, scope.objectId(), scope.epoch(), 0),
                    new ModernRooms.Scope(7, 1, scope.epoch(), 0), new ModernRooms.Scope(7, scope.objectId(), 2, 0),
                    new ModernRooms.Scope(7, scope.objectId(), scope.epoch(), 1))) {
                assertThrows(SecurityException.class, () -> f.service.handle(f.first.owner(), new Request(wrong, Action.READ, 0, 0, 0, 0, 0, 0, 0, 0, ZERO)));
            }
            var unknown = f.rooms.presence.connect();
            try { assertThrows(SecurityException.class, () -> f.service.handle(unknown, readRequest(f.first))); }
            finally { f.rooms.presence.disconnect(unknown); }
            assertEquals(0, f.reads.get()); assertNull(f.read(f.first));
        }
    }

    @Test void roomOwnerReadinessAndExpectedRevisionAreStillAuthoritative() throws Exception {
        try (var f = new Fixture()) {
            var room = f.rooms.view(f.first).joined().summary();
            assertEquals(Reason.NOT_OWNER, f.service.handle(f.second.owner(), prepareRequest(f.second, room)).reason());
            var old = prepareRequest(f.first, room);
            ModernRoomsTest.accepted(f.rooms.act(f.second, ModernRooms.Action.READY, 0));
            assertEquals(Reason.ROOM_REVISION, f.service.handle(f.first.owner(), old).reason());
            assertEquals(Reason.NOT_READY, f.prepare().reason()); assertEquals(0, f.reads.get());
            assertFalse(f.rooms.rooms.isPreparing(f.first.owner())); assertTrue(f.service.isQuiescent());
        }
    }

    @Test void missingExplicitBindingAndUnregisteredParticipantFailWithoutInventedWorldOrIdentity() throws Exception {
        try (var f = new Fixture(limits(4, 1, 1, 2_000, ENGINE), 564)) {
            assertEquals(Reason.NO_BINDING, f.prepare().reason()); assertFalse(f.rooms.rooms.isPreparing(f.first.owner()));
            assertEquals(0, f.rooms.view(f.first).joined().summary().ready()); assertEquals(0, f.reads.get());
        }
        try (var f = new Fixture()) {
            f.service.detach(f.second.owner());
            assertEquals(Status.UNAVAILABLE, f.prepare().status()); assertEquals(0, f.reads.get());
            assertFalse(f.rooms.rooms.isPreparing(f.first.owner()));
        }
    }

    @Test void changedFreshRowAbortsWholePreparationAndResetsReadinessWithoutPartialPublication() throws Exception {
        try (var f = new Fixture()) {
            f.service.register(f.second.owner(), f.second.scope(), deadline -> {
                var row = f.row(f.second); return new ModernApplicationProtocol.CharacterRow(row.objectId(), "changed-owner-name", row.characterClass(), row.sex(), row.level());
            });
            var initial = f.prepare().frame(); assertNotNull(initial);
            await(() -> f.read(f.first).phase() == Phase.ABORTED);
            assertEquals(AbortReason.PREPARATION_FAILED, f.read(f.first).abortReason());
            assertTrue(f.read(f.first).actors().stream().noneMatch(ModernBattleEngine.Actor::loaded));
            await(() -> !f.rooms.rooms.isPreparing(f.first.owner())); assertEquals(0, f.rooms.view(f.first).joined().summary().ready());
            assertNull(f.service.handle(f.first.owner(), leave(f.first, initial)).frame());
        }
    }

    @Test void manifestAndMatchGenerationAreStrictAndLoadBarrierPrecedesActualCountdown() throws Exception {
        try (var f = new Fixture()) {
            Frame frame = f.loading();
            var wrong = new Request(f.first.scope(), Action.LOADED, 0, 0, 0, frame.matchId(), frame.generation(), 0, 0, 0, "a".repeat(64));
            assertThrows(SecurityException.class, () -> f.service.handle(f.first.owner(), wrong));
            var mismatch = new Request(f.first.scope(), Action.READ, 0, 0, 0, frame.matchId(), frame.generation() + 1, 0, 0, 0, ZERO);
            assertThrows(SecurityException.class, () -> f.service.handle(f.first.owner(), mismatch));
            var first = f.service.handle(f.first.owner(), loaded(f.first, frame)); assertEquals(Phase.LOADING, first.frame().phase());
            assertEquals(first.frame(), f.service.handle(f.first.owner(), loaded(f.first, frame)).frame());
            var second = f.service.handle(f.second.owner(), loaded(f.second, frame)); assertEquals(Phase.COUNTDOWN, second.frame().phase());
            await(() -> f.read(f.first).phase() == Phase.ACTIVE);
        }
    }

    @Test void movementUsesEngineCollisionSequenceAndPaceNotClientCoordinates() throws Exception {
        try (var f = new Fixture()) {
            Frame frame = f.active(); var first = frame.actors().get(0).position();
            assertEquals(Reason.OCCUPIED, f.service.handle(f.first.owner(), move(f.first, frame, 1, 0, first)).reason());
            var moved = f.service.handle(f.first.owner(), move(f.first, frame, 2, 2, first));
            assertEquals(Status.OK, moved.status()); assertEquals(first.x() + 1, moved.frame().actors().get(0).position().x());
            var current = moved.frame().actors().get(0).position();
            assertEquals(Reason.TOO_FAST, f.service.handle(f.first.owner(), move(f.first, frame, 3, 2, current)).reason());
            assertThrows(SecurityException.class, () -> f.service.handle(f.first.owner(), move(f.first, frame, 3, 2, current)));
            assertEquals(Reason.STALE_POSITION, f.service.handle(f.first.owner(), move(f.first, frame, 4, 2, first)).reason());
        }
    }

    @Test void schedulerAbortsUnloadedMatchWithoutAnyClientPollAndReleasesRoomTicket() throws Exception {
        var shortEngine = new ModernBattleEngine.Limits(75, 0, 1_000);
        try (var f = new Fixture(limits(2, 1, 1, 2_000, shortEngine), 514)) {
            Frame initial = f.prepare().frame(); assertNotNull(initial);
            // Deliberately do NOT call handle(READ), tickNow or any Engine method.
            await(() -> !f.rooms.rooms.isPreparing(f.first.owner()));
            Frame terminal = f.read(f.first); assertEquals(Phase.ABORTED, terminal.phase());
            assertEquals(AbortReason.LOAD_TIMEOUT, terminal.abortReason());
            assertEquals(0, f.rooms.view(f.first).joined().summary().ready());
        }
    }

    @Test void schedulerExpiresActiveLifetimeWithoutFakeWinLossAndKeepsBoundedTerminalUntilLeave() throws Exception {
        var shortEngine = new ModernBattleEngine.Limits(2_000, 0, 75);
        try (var f = new Fixture(limits(2, 1, 1, 2_000, shortEngine), 514)) {
            Frame frame = f.loading(); f.service.handle(f.first.owner(), loaded(f.first, frame)); f.service.handle(f.second.owner(), loaded(f.second, frame));
            await(() -> !f.rooms.rooms.isPreparing(f.first.owner())); // No world polling advances this clock.
            assertEquals(AbortReason.ACTIVE_TIMEOUT, f.read(f.first).abortReason()); assertEquals(1, f.service.matchCount());
            f.service.handle(f.first.owner(), leave(f.first, frame)); assertNull(f.read(f.first)); assertNotNull(f.read(f.second));
            f.service.handle(f.second.owner(), leave(f.second, frame)); await(f.service::isQuiescent);
        }
    }

    @Test void anyMemberLeaveAbortsEveryoneThenNewReadyPreparationHasNewMatchGeneration() throws Exception {
        try (var f = new Fixture()) {
            Frame before = f.active(); assertNull(f.service.handle(f.second.owner(), leave(f.second, before)).frame());
            Frame abort = f.read(f.first); assertEquals(Phase.ABORTED, abort.phase()); assertEquals(AbortReason.SERVER_ABORT, abort.abortReason());
            assertEquals(0, f.rooms.view(f.first).joined().summary().ready());
            f.ready(f.first); f.ready(f.second); Frame next = f.loading();
            assertTrue(next.matchId() > before.matchId()); assertTrue(next.generation() > before.generation());
            assertThrows(SecurityException.class, () -> f.service.handle(f.first.owner(), loaded(f.first, before)));
            assertEquals(1, f.service.matchCount());
        }
    }

    @Test void cancelledReaderWhichIgnoresInterruptStillConsumesCapacityUntilRealFinally() throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1); var interruptions = new AtomicInteger();
        Fixture f = new Fixture(limits(1, 1, 0, 2_000, ENGINE), 514);
        try {
            f.service.register(f.first.owner(), f.first.scope(), deadline -> {
                entered.countDown();
                while (release.getCount() != 0) { try { release.await(20, TimeUnit.MILLISECONDS); } catch (InterruptedException ignored) { interruptions.incrementAndGet(); } }
                return f.row(f.first);
            });
            Frame initial = f.prepare().frame(); assertTrue(entered.await(1, TimeUnit.SECONDS));
            f.service.detach(f.first.owner());
            f.service.handle(f.second.owner(), leave(f.second, initial));
            assertEquals(1, f.service.active()); assertEquals(1, f.service.matchCount()); assertFalse(f.service.isQuiescent());
            f.register(f.first); f.ready(f.first); f.ready(f.second);
            assertEquals(Reason.CAPACITY, f.prepare().reason()); assertFalse(f.rooms.rooms.isPreparing(f.first.owner()));
            await(() -> interruptions.get() != 0);
            release.countDown(); await(f.service::isQuiescent); assertNull(f.read(f.first));
            assertEquals(Phase.LOADING, f.loading().phase());
        } finally { release.countDown(); f.close(); }
    }

    @Test void sequentialFreshReadsReceiveOneAbsoluteDeadlineAndCannotResetIt() throws Exception {
        var firstDeadline = new AtomicLong(); var secondDeadline = new AtomicLong(); var secondEntered = new CountDownLatch(1);
        try (var f = new Fixture(limits(2, 1, 1, 300, ENGINE), 514)) {
            f.service.register(f.first.owner(), f.first.scope(), deadline -> { firstDeadline.set(deadline); Thread.sleep(125); return f.row(f.first); });
            f.service.register(f.second.owner(), f.second.scope(), deadline -> { secondDeadline.set(deadline); secondEntered.countDown(); Thread.sleep(2_000); return f.row(f.second); });
            f.prepare(); assertTrue(secondEntered.await(1, TimeUnit.SECONDS)); assertEquals(firstDeadline.get(), secondDeadline.get());
            await(() -> !f.rooms.rooms.isPreparing(f.first.owner()));
            assertEquals(AbortReason.LOAD_TIMEOUT, f.read(f.first).abortReason());
            await(() -> f.service.active() == 0);
        }
    }

    @Test void preparationQueueIsBoundedBeforeRoomFreezeAndQueuedCancellationDoesNotRunReader() throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1); var queuedReads = new AtomicInteger();
        Fixture f = new Fixture(limits(3, 1, 1, 2_000, ENGINE), 514);
        try {
            f.service.register(f.first.owner(), f.first.scope(), deadline -> { entered.countDown(); blocking(release); return f.row(f.first); });
            f.prepare(); assertTrue(entered.await(1, TimeUnit.SECONDS));
            var third = f.player(7, 100); var fourth = f.player(7, 101);
            ModernRoomsTest.accepted(f.rooms.create(third, ModernRoomsTest.draft(100, 1, 514, 0)));
            ModernRoomsTest.accepted(f.rooms.join(fourth, f.rooms.view(third).joined().summary(), ""));
            for (var player : List.of(third, fourth)) { f.service.register(player.owner(), player.scope(), deadline -> { queuedReads.incrementAndGet(); return f.row(player); }); f.ready(player); }
            assertEquals(Status.OK, f.service.handle(third.owner(), prepareRequest(third, f.rooms.view(third).joined().summary())).status());
            assertEquals(1, f.service.queued());
            var fifth = f.player(7, 102); var sixth = f.player(7, 103);
            ModernRoomsTest.accepted(f.rooms.create(fifth, ModernRoomsTest.draft(100, 1, 514, 0)));
            ModernRoomsTest.accepted(f.rooms.join(sixth, f.rooms.view(fifth).joined().summary(), ""));
            for (var player : List.of(fifth, sixth)) { f.register(player); f.ready(player); }
            var full = f.service.handle(fifth.owner(), prepareRequest(fifth, f.rooms.view(fifth).joined().summary()));
            assertEquals(Reason.CAPACITY, full.reason()); assertFalse(f.rooms.rooms.isPreparing(fifth.owner()));
            f.service.detach(third.owner()); await(() -> f.service.queued() == 0); assertEquals(0, queuedReads.get());
            release.countDown();
        } finally { release.countDown(); f.close(); }
    }

    @Test void shutdownCancelsAdmissionButAwaitsIgnoringReaderAndCannotPublishLateEngine() throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        Fixture f = new Fixture();
        try {
            f.service.register(f.first.owner(), f.first.scope(), deadline -> {
                entered.countDown(); while (release.getCount() != 0) { try { release.await(20, TimeUnit.MILLISECONDS); } catch (InterruptedException ignored) {} } return f.row(f.first);
            });
            f.prepare(); assertTrue(entered.await(1, TimeUnit.SECONDS)); f.service.close();
            assertFalse(f.service.awaitTermination(25)); assertEquals(1, f.service.active());
            assertThrows(IllegalStateException.class, () -> f.register(f.first));
            assertThrows(IllegalStateException.class, f::prepare); assertFalse(f.rooms.rooms.isPreparing(f.first.owner()));
            release.countDown(); assertTrue(f.service.awaitTermination(3_000)); assertEquals(0, f.service.matchCount());
        } finally { release.countDown(); f.close(); }
    }

    @Test void staleRegistrationCannotReceiveLatePreparationAfterDetachAndReconnect() throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        Fixture f = new Fixture();
        try {
            f.service.register(f.first.owner(), f.first.scope(), deadline -> { entered.countDown(); blocking(release); return f.row(f.first); });
            var frame = f.prepare().frame(); assertTrue(entered.await(1, TimeUnit.SECONDS)); f.service.detach(f.first.owner());
            assertThrows(SecurityException.class, () -> f.read(f.first));
            f.register(f.first); assertNull(f.read(f.first)); release.countDown(); await(() -> f.service.active() == 0);
            assertNull(f.read(f.first)); assertEquals(Phase.ABORTED, f.read(f.second).phase());
            assertThrows(SecurityException.class, () -> f.service.handle(f.first.owner(), loaded(f.first, frame)));
        } finally { release.countDown(); f.close(); }
    }

    @Test void limitsAndMappingsCannotDefaultUnknownGeometryOrTiming() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> limits(0, 1, 0, 100, ENGINE));
        assertThrows(IllegalArgumentException.class, () -> limits(101, 1, 0, 100, ENGINE));
        assertThrows(IllegalArgumentException.class, () -> limits(1, 0, 0, 100, ENGINE));
        assertThrows(IllegalArgumentException.class, () -> limits(1, 1, -1, 100, ENGINE));
        try (var f = new Fixture()) {
            var binding = new ModernWorldBinding(1, 514, f.map);
            assertThrows(IllegalArgumentException.class, () -> new ModernWorldService.Mapping(binding, ANCHOR, 0));
            assertThrows(IllegalArgumentException.class, () -> new ModernWorldService.Mapping(binding, new ModernWorldMap.Cell(0, 0), 1_000_000));
            assertThrows(IllegalArgumentException.class, () -> new ModernWorldService(f.rooms.rooms,
                    Map.of(new ModernWorldBinding.Key(1, 564), new ModernWorldService.Mapping(binding, ANCHOR, 1_000_000)), limits(1, 1, 0, 100, ENGINE)));
        }
    }

    @Test void actualServiceRepliesRoundTripTheExactWorldWireForSuccessAndDenials() throws Exception {
        try (var f = new Fixture()) {
            var request = prepareRequest(f.first, f.rooms.view(f.first).joined().summary());
            var prepared = checked(f, f.first, request); assertEquals(Status.OK, prepared.status());
            await(() -> f.read(f.first).phase() == Phase.LOADING);
            var frame = checked(f, f.first, readRequest(f.first)).frame();
            assertEquals(Reason.NOT_ACTIVE, checked(f, f.first, move(f.first, frame, 1, 2, frame.actors().get(0).position())).reason());
            checked(f, f.first, loaded(f.first, frame)); checked(f, f.second, loaded(f.second, frame));
            await(() -> f.read(f.first).phase() == Phase.ACTIVE);
            var active = f.read(f.first);
            assertEquals(Status.OK, checked(f, f.first, move(f.first, active, 2, 2, active.actors().get(0).position())).status());
            checked(f, f.second, leave(f.second, active));
            assertEquals(Reason.NOT_ACTIVE, checked(f, f.first, loaded(f.first, active)).reason());
            checked(f, f.first, leave(f.first, active)); assertNull(checked(f, f.first, readRequest(f.first)).frame());
        }
    }

    @Test void concurrentLeaveCannotAcknowledgeRoomReleaseWhileItsGuardStillHasAnAdmittedOwner() throws Exception {
        try (var f = new Fixture()) {
            Frame frame = f.loading();
            var gateField = ModernWorldService.class.getDeclaredField("gate"); gateField.setAccessible(true);
            var registrationsField = ModernWorldService.class.getDeclaredField("registrations"); registrationsField.setAccessible(true);
            ModernRooms.FrozenPreparation ticket;
            Object capturedEntry;
            java.lang.reflect.Field terminalField;
            synchronized (gateField.get(f.service)) {
                Object registration = ((Map<?, ?>) registrationsField.get(f.service)).get(f.first.owner());
                var entryField = registration.getClass().getDeclaredField("entry"); entryField.setAccessible(true);
                capturedEntry = entryField.get(registration);
                var ticketField = capturedEntry.getClass().getDeclaredField("ticket"); ticketField.setAccessible(true);
                ticket = (ModernRooms.FrozenPreparation) ticketField.get(capturedEntry);
                terminalField = capturedEntry.getClass().getDeclaredField("terminal"); terminalField.setAccessible(true);
            }
            var workers = java.util.concurrent.Executors.newFixedThreadPool(2);
            try {
                java.util.concurrent.Future<?> detached;
                java.util.concurrent.Future<Reply> left;
                try (var ignored = ticket.guard()) {
                    detached = workers.submit(() -> f.service.detach(f.first.owner()));
                    // Observe admission fencing, NOT a fabricated early terminal
                    // frame. READ correctly waits for the admitted Engine Guard.
                    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
                    while (true) {
                        synchronized (gateField.get(f.service)) {
                            if (terminalField.getBoolean(capturedEntry)) break;
                        }
                        assertTrue(System.nanoTime() < deadline); Thread.sleep(5);
                    }
                    left = workers.submit(() -> f.service.handle(f.second.owner(), leave(f.second, frame)));
                    assertThrows(java.util.concurrent.TimeoutException.class, () -> detached.get(25, TimeUnit.MILLISECONDS));
                    assertThrows(java.util.concurrent.TimeoutException.class, () -> left.get(25, TimeUnit.MILLISECONDS));
                }
                detached.get(2, TimeUnit.SECONDS); assertNull(left.get(2, TimeUnit.SECONDS).frame());
                assertFalse(f.rooms.rooms.isPreparing(f.second.owner())); await(f.service::isQuiescent);
            } finally { workers.shutdownNow(); assertTrue(workers.awaitTermination(3, TimeUnit.SECONDS)); }
        }
    }

    private static Reply checked(Fixture fixture, ModernRoomsTest.Player player, Request request) throws Exception {
        Reply reply = fixture.service.handle(player.owner(), request);
        var message = new ModernApplicationProtocol.WorldResult(17, request, reply);
        byte[] frame = ModernApplicationProtocol.encode(message);
        try { assertEquals(ModernMatchDescriptor.forPeer(message,new ModernSessionBoundary.Negotiated(1,1,1,127)), ModernApplicationProtocol.decode(frame)); }
        finally { java.util.Arrays.fill(frame, (byte) 0); }
        return reply;
    }
}
