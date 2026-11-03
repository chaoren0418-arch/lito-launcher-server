package local.lito381.server;

import static local.lito381.server.ModernWorldProtocol.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;

/** Real service scheduler and Rooms lifecycle; only the persistence receipt is controlled. */
class ModernWorldServicePetRaceTest {
    static final ModernWorldMap.Cell ANCHOR = ModernPetRaceTest.cell(10, 10);
    static final ModernWorldMap.Cell LEFT = ModernPetRaceTest.cell(9, 10);
    static final ModernWorldMap.Cell RIGHT = ModernPetRaceTest.cell(11, 10);
    static final long STEP = 1_000_000;
    static final ModernSessionBoundary.Negotiated V16 = new ModernSessionBoundary.Negotiated(1, 16, 1, 196975);

    static final class Journal implements ModernPetRaceSettlement.JournaledStore {
        final List<Slot> slots = new ArrayList<>();
        volatile boolean closed;
        @Override public synchronized Slot reserve(long match, long generation) {
            assertFalse(closed);
            var slot = new Slot(match, generation);
            slots.add(slot);
            return slot;
        }
        synchronized Slot latest() { return slots.get(slots.size() - 1); }
        synchronized int count() { return slots.size(); }
        @Override public boolean accepting() { return !closed; }
        @Override public void persist(ModernPetRaceSettlement.Event event, long deadline) {
            throw new AssertionError("service must use its reserved durable slot");
        }
        @Override public void close() { closed = true; }
        @Override public boolean awaitTermination(long timeout) { return closed; }
        @Override public boolean isQuiescent() { return closed; }
    }
    static final class Slot implements ModernPetRaceSettlement.Reservation {
        final long match, generation;
        final AtomicInteger calls = new AtomicInteger();
        volatile ModernPetRaceSettlement.Event event;
        volatile ModernPetRaceProtocol.Settlement state = ModernPetRaceProtocol.Settlement.PENDING;
        volatile long recordedNanos;
        volatile boolean closed;
        Slot(long match, long generation) { this.match = match; this.generation = generation; }
        @Override public synchronized void record(ModernPetRaceSettlement.Event value, long deadline) {
            assertTrue(deadline - System.nanoTime() > 0);
            assertEquals(match, value.matchId()); assertEquals(generation, value.generation());
            assertNull(event); calls.incrementAndGet(); recordedNanos = System.nanoTime(); event = value;
        }
        @Override public ModernPetRaceProtocol.Settlement state() { return state; }
        @Override public void close() { closed = true; }
    }
    static final class Fixture implements AutoCloseable {
        final ModernRoomsTest.Fixture rooms = new ModernRoomsTest.Fixture();
        final ModernRoomsTest.Player sun = rooms.player(7, 3333, 80, 0);
        final ModernRoomsTest.Player moon = rooms.player(42, 3333, 80, 0);
        final Journal journal = new Journal();
        final ModernWorldService service;
        Fixture() throws Exception {
            var map = ModernPetRaceTest.map();
            var binding = new ModernWorldBinding(0, 565, map);
            var course = ModernPetRace.qualify(binding.key(), map, map.manifest(),
                    List.of(ModernPetRaceTest.gate(LEFT), ModernPetRaceTest.gate(RIGHT)), 2145, STEP,
                    List.of(ANCHOR, ModernPetRaceTest.cell(10, 9)), () -> false);
            var mapping = new ModernWorldService.Mapping(binding, ANCHOR, STEP).withRace(course);
            service = new ModernWorldService(rooms.rooms, Map.of(binding.key(), mapping),
                    new ModernWorldService.Limits(4, 1, 1, 3000, 5,
                            new ModernBattleEngine.Limits(3000, 0, 30_000)),
                    bound -> { throw new AssertionError("race cannot use combat RNG"); }, null, Map.of(), journal);
            ModernRoomsTest.accepted(rooms.create(sun, ModernRoomsTest.draft(2, 0, 565, 0)));
            ModernRoomsTest.accepted(rooms.join(moon, rooms.view(sun).joined().summary(), ""));
            register(sun, V16); register(moon, V16); ready();
        }
        void register(ModernRoomsTest.Player player, ModernSessionBoundary.Negotiated protocol) {
            service.register(player.owner(), player.scope(), deadline -> {
                var row = rooms.presence.selected(player.owner());
                return new ModernApplicationProtocol.CharacterRow(row.key().objectId(), row.characterName(),
                        row.characterClass(), row.sex(), row.level());
            }, null, false, null, false, null, false, false, false, false, false, false, protocol);
        }
        void ready() { for (var player : List.of(sun, moon)) ModernRoomsTest.accepted(rooms.act(player, ModernRooms.Action.READY, 1)); }
        Reply prepare() {
            var room = rooms.view(sun).joined().summary();
            return service.handle(sun.owner(), new Request(sun.scope(), Action.PREPARE, 0, room.id(), room.revision(), 0, 0, 0, 0, 0, ZERO_DIGEST));
        }
        Frame read(ModernRoomsTest.Player player) {
            return service.handle(player.owner(), new Request(player.scope(), Action.READ, 0, 0, 0, 0, 0, 0, 0, 0, ZERO_DIGEST)).frame();
        }
        Frame loading() throws Exception {
            assertEquals(Status.OK, prepare().status());
            await(() -> read(sun).phase() == Phase.LOADING, 3000);
            return read(sun);
        }
        Frame active() throws Exception {
            var frame = loading();
            for (var player : List.of(sun, moon)) {
                var reply = service.handle(player.owner(), new Request(player.scope(), Action.LOADED, 0, 0, 0,
                        frame.matchId(), frame.generation(), 0, 0, 0, frame.manifest().digest()));
                assertEquals(Status.OK, reply.status());
            }
            var active = read(sun); assertEquals(Phase.ACTIVE, active.phase());
            assertEquals(ANCHOR, active.actors().get(0).position());
            assertNotEquals(LEFT, active.actors().get(1).position());
            assertNotEquals(RIGHT, active.actors().get(1).position());
            return active;
        }
        Frame step(ModernWorldMap.Cell to) throws Exception {
            Thread.sleep(2); // Genuine elapsed cadence, no injected clock/progress.
            var before = read(sun); var actor = before.actors().get(0); var from = actor.position();
            assertEquals(from.y(), to.y()); assertEquals(1, Math.abs(to.x() - from.x()));
            var result = service.handle(sun.owner(), new Request(sun.scope(), Action.MOVE, to.x() < from.x() ? 6 : 2, 0, 0,
                    before.matchId(), before.generation(), actor.lastMoveSequence() + 1, from.x(), from.y(), ZERO_DIGEST));
            assertEquals(Status.OK, result.status()); assertEquals(to, result.frame().actors().get(0).position());
            return result.frame();
        }
        Frame finish() throws Exception {
            var frame = read(sun);
            for (int i = 0; frame.race().runners().get(0).placement() == 0; i++) {
                assertTrue(i < 16);
                if (!frame.actors().get(0).position().equals(ANCHOR)) frame = step(ANCHOR);
                frame = step(frame.race().runners().get(0).nextCheckpoint() == 0 ? LEFT : RIGHT);
            }
            assertEquals(4, frame.race().runners().get(0).completedLaps());
            assertEquals(Phase.ACTIVE, frame.phase()); return frame;
        }
        Reply leave(ModernRoomsTest.Player player, Frame frame) {
            return service.handle(player.owner(), new Request(player.scope(), Action.LEAVE, 0, 0, 0,
                    frame.matchId(), frame.generation(), 0, 0, 0, ZERO_DIGEST));
        }
        void disconnect(ModernRoomsTest.Player player) {
            service.detach(player.owner()); rooms.rooms.disconnect(player.owner()); rooms.presence.disconnect(player.owner());
        }
        @Override public void close() throws Exception {
            service.close(); assertTrue(service.awaitTermination(5000)); assertTrue(service.isQuiescent());
            assertEquals(0, service.matchCount()); assertEquals(0, service.registrationCount());
            for (var player : List.of(sun, moon)) { rooms.rooms.disconnect(player.owner()); rooms.presence.disconnect(player.owner()); }
            assertEquals(0, rooms.rooms.ownerCount()); assertEquals(0, rooms.rooms.roomCount()); assertEquals(0, rooms.presence.ownerCount());
        }
    }
    static void await(BooleanSupplier ready, int millis) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis);
        while (!ready.getAsBoolean()) { assertTrue(deadline - System.nanoTime() > 0, "bounded service condition did not complete"); Thread.sleep(5); }
    }

    @Test void pendingAndFailedCannotLeaveOrPrepareAgainButSavedCanReturnAndStartANewGeneration() throws Exception {
        for (var outcome : List.of(ModernPetRaceProtocol.Settlement.SAVED, ModernPetRaceProtocol.Settlement.FAILED)) {
            try (var f = new Fixture()) {
                f.active(); var first = f.finish(); var slot = f.journal.latest();
                long started = System.nanoTime();
                await(() -> f.read(f.sun).phase() == Phase.FINISHED, 15_000);
                assertTrue(System.nanoTime() - started >= TimeUnit.MILLISECONDS.toNanos(9800));
                var pending = f.read(f.sun); assertEquals(ModernPetRaceProtocol.Settlement.PENDING, pending.race().settlement());
                assertNotNull(slot.event); assertEquals(1, slot.calls.get()); assertNull(pending.completion());
                assertEquals(List.of(7, 42), slot.event.rows().stream().map(r -> r.source().sourceId()).toList());
                assertTrue(slot.event.rows().stream().allMatch(r -> r.source().objectId() == 3333));
                assertEquals(Status.BUSY, f.leave(f.sun, pending).status()); assertEquals(pending, f.read(f.sun));
                f.ready(); var blocked = f.prepare();
                assertEquals(Status.BUSY, blocked.status()); assertEquals(Reason.INVALID_STATE, blocked.reason());
                assertEquals(pending, blocked.frame()); assertEquals(1, f.journal.count());
                slot.state = outcome;
                await(() -> f.read(f.sun).race().settlement() == outcome, 3000);
                var terminal = f.read(f.sun);
                assertEquals(pending.revision() + 1, terminal.revision());
                assertEquals(first.race().finishWindowDeadlineElapsedNanos(), terminal.race().finishWindowDeadlineElapsedNanos());
                assertEquals(pending.race().resultId(), terminal.race().resultId()); assertEquals(pending.actors(), terminal.actors());
                assertEquals(pending.race().contentSha256(), terminal.race().contentSha256());
                if (outcome == ModernPetRaceProtocol.Settlement.FAILED) {
                    assertEquals(Status.BUSY, f.leave(f.sun, terminal).status()); assertEquals(terminal, f.read(f.sun));
                } else {
                    for (var player : List.of(f.sun, f.moon)) { assertEquals(Status.OK, f.leave(player, terminal).status()); assertNull(f.read(player)); }
                    assertTrue(slot.closed); assertEquals(0, f.service.matchCount());
                    f.ready(); var next = f.loading();
                    assertNotEquals(terminal.matchId(), next.matchId()); assertNotEquals(terminal.generation(), next.generation());
                    assertEquals(2, f.journal.count()); assertTrue(next.race().runners().stream().allMatch(r -> r.completedLaps() == 0));
                    assertTrue(next.actors().stream().allMatch(a -> !a.loaded() && a.lastMoveSequence() == 0));
                    assertThrows(SecurityException.class, () -> f.leave(f.sun, terminal));
                }
                assertEquals(1, slot.calls.get());
            }
        }
    }

    @Test void allOwnersDisconnectAfterFirstFinisherButRealWindowStillJournalsAndRetires() throws Exception {
        try (var f = new Fixture()) {
            f.active(); var first = f.finish(); long before = System.nanoTime(); var slot = f.journal.latest();
            f.disconnect(f.sun); f.disconnect(f.moon);
            assertEquals(0, f.service.registrationCount()); assertEquals(0, f.rooms.rooms.roomCount());
            assertEquals(1, f.service.matchCount()); assertNull(slot.event); assertFalse(slot.closed);
            await(() -> slot.event != null && f.service.matchCount() == 0, 15_000);
            assertTrue(slot.recordedNanos - before >= TimeUnit.MILLISECONDS.toNanos(9800));
            assertEquals(1, slot.calls.get()); assertTrue(slot.closed);
            assertEquals(first.race().finishWindowDeadlineElapsedNanos(), slot.event.finishWindowDeadlineElapsedNanos());
            assertEquals(1, slot.event.rows().get(0).runner().placement());
            assertEquals(ModernPetRace.Dnf.DISCONNECTED, slot.event.rows().get(1).runner().dnf());
        }
    }

    @Test void withdrawalThatCrossesRealFinishDeadlineRetainsReceiptAndDoesNotWithdrawRetiredOwnerTwice() throws Exception {
        try (var f = new Fixture()) {
            f.active();
            // Locate, but never replace or mutate, the real admitted authority.
            var gateField = ModernWorldService.class.getDeclaredField("gate"); gateField.setAccessible(true);
            var registrationsField = ModernWorldService.class.getDeclaredField("registrations"); registrationsField.setAccessible(true);
            ModernBattleEngine engine;
            synchronized (gateField.get(f.service)) {
                Object registration = ((Map<?, ?>) registrationsField.get(f.service)).get(f.sun.owner());
                var entryField = registration.getClass().getDeclaredField("entry"); entryField.setAccessible(true);
                Object entry = entryField.get(registration);
                var engineField = entry.getClass().getDeclaredField("engine"); engineField.setAccessible(true);
                engine = (ModernBattleEngine) engineField.get(entry);
            }
            var first = f.finish(); var slot = f.journal.latest();
            synchronized (engine) {
                // Hold only publication's engine monitor. Real elapsed time,
                // actual qualified MOVEs and the real withdrawal create the
                // final event, reproducing expiry between LEAVE's two reads.
                Thread.sleep(10_050);
                var withdrawn = engine.withdrawRacer(f.sun.owner(), ModernPetRace.Dnf.LEFT);
                assertEquals(ModernBattleEngine.Phase.FINISHED, withdrawn.phase());
                assertEquals(1, engine.ownerCount());
            }
            var pending = f.read(f.sun);
            assertEquals(Phase.FINISHED, pending.phase());
            assertEquals(ModernPetRaceProtocol.Settlement.PENDING, pending.race().settlement());
            assertEquals(first.race().finishWindowDeadlineElapsedNanos(), pending.race().finishWindowDeadlineElapsedNanos());
            assertEquals(Status.BUSY, f.leave(f.sun, pending).status()); assertEquals(pending, f.read(f.sun));
            assertNotNull(slot.event); assertEquals(1, slot.calls.get());
            slot.state = ModernPetRaceProtocol.Settlement.SAVED;
            await(() -> f.read(f.sun).race().settlement() == ModernPetRaceProtocol.Settlement.SAVED, 3000);
            var saved = f.read(f.sun);
            assertEquals(pending.revision() + 1, saved.revision()); assertEquals(pending.race().resultId(), saved.race().resultId());
            for (var player : List.of(f.sun, f.moon)) {
                assertEquals(Status.OK, f.leave(player, saved).status()); assertNull(f.read(player));
            }
            assertEquals(1, slot.calls.get()); assertEquals(0, f.service.matchCount()); assertTrue(slot.closed);
        }
    }

    @Test void preactiveDepartureOrAllDnfBeforeAnyWinnerNeverCreateAnAward() throws Exception {
        for (boolean active : List.of(false, true)) try (var f = new Fixture()) {
            if (active) f.active(); else f.loading();
            var slot = f.journal.latest(); f.disconnect(f.moon);
            var remaining = f.read(f.sun);
            assertEquals(active ? Phase.ACTIVE : Phase.ABORTED, remaining.phase());
            if (active) assertEquals(ModernPetRace.Dnf.DISCONNECTED, remaining.race().runners().get(1).dnf());
            f.disconnect(f.sun); await(() -> f.service.matchCount() == 0, 3000);
            assertEquals(0, slot.calls.get()); assertNull(slot.event); assertTrue(slot.closed);
        }
    }

    @Test void unsupportedPeerAndUnavailableJournalRejectBeforeReservationAndRecoverCleanly() throws Exception {
        try (var f = new Fixture()) {
            for (var protocol : List.of(new ModernSessionBoundary.Negotiated(1, 15, 1, 65903), new ModernSessionBoundary.Negotiated(1, 16, 1, 65903))) {
                f.register(f.moon, protocol); f.ready();
                assertEquals(Status.UNAVAILABLE, f.prepare().status()); assertEquals(0, f.journal.count());
                assertFalse(f.rooms.rooms.isPreparing(f.sun.owner())); assertEquals(0, f.service.matchCount());
            }
            f.register(f.moon, V16); f.ready(); f.journal.closed = true;
            assertEquals(Reason.CAPACITY, f.prepare().reason()); assertEquals(0, f.journal.count());
            f.journal.closed = false; f.ready(); assertEquals(Phase.LOADING, f.loading().phase()); assertEquals(1, f.journal.count());
        }
    }
}
