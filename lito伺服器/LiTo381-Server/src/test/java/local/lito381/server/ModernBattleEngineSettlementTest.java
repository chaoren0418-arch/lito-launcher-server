package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/** Real opaque room capture and qualified fighters; no fabricated winner, DB, or network. */
class ModernBattleEngineSettlementTest {
    private static final ModernWorldMap.SectorKey SECTOR = new ModernWorldMap.SectorKey(0x8000, 0x8000);
    private static final long STEP = TimeUnit.MILLISECONDS.toNanos(200);
    private static final long ATTACK = TimeUnit.MILLISECONDS.toNanos(700);
    private static final ModernBattleEngine.Limits IMMEDIATE = new ModernBattleEngine.Limits(10_000, 0, 180_000);

    @Test void loadingAndCountdownCannotProduceAResultOrApplyAnAttack() throws Exception {
        var limits = new ModernBattleEngine.Limits(10_000, TimeUnit.SECONDS.toNanos(3), 180_000);
        try (var f = new Fixture(2, 1, 0, 1, false, false, limits, bound -> 0)) {
            f.create(true);
            var loading = f.attack(0, 1, 2);
            assertEquals(ModernBattleEngine.AttackStatus.NOT_ACTIVE, loading.status());
            assertEquals(ModernBattleEngine.Phase.LOADING, loading.snapshot().phase());
            assertNoDamageOrResult(loading.snapshot());
            f.ack(0); f.ack(1);
            var countdown = f.attack(0, 2, 2);
            assertEquals(ModernBattleEngine.Phase.COUNTDOWN, countdown.snapshot().phase());
            assertEquals(ModernBattleEngine.AttackStatus.NOT_ACTIVE, countdown.status());
            assertNoDamageOrResult(countdown.snapshot());
            f.nano.addAndGet(limits.countdownNanos());
            assertEquals(ModernBattleEngine.Phase.ACTIVE, f.engine.tick().phase());
            assertEquals(ModernBattleEngine.Phase.FINISHED, f.attack(0, 3, 2).snapshot().phase());
        }
    }

    @Test void finalHitAndEveryResultFieldCommitAtOneRevisionWithSourceSafeIdentity() throws Exception {
        try (var f = new Fixture()) {
            f.create(true); var before = f.active(); f.epoch.set(9_000);
            var reply = f.attack(0, 1, 2); var after = reply.snapshot(); var event = after.result();
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED, reply.status());
            assertEquals(ModernBattleEngine.Phase.FINISHED, after.phase());
            assertEquals(ModernBattleEngine.AbortReason.NONE, after.abortReason());
            assertEquals(before.revision() + 1, after.revision());
            assertNotNull(event); assertNotEquals(new UUID(0, 0), event.resultId());
            assertEquals(after.matchId(), event.matchId()); assertEquals(after.generation(), event.generation());
            assertEquals(after.revision(), event.engineRevision());
            assertEquals(1, event.mode()); assertEquals(514, event.mapId()); assertEquals(9_000, event.finishedAtMillis());
            assertEquals(after.revision(), after.vitals().get(0).attackRevision());
            assertEquals(after.revision(), after.vitals().get(1).hurtRevision());
            assertEquals(after.revision(), after.vitals().get(1).deathRevision());
            assertEquals(0, after.vitals().get(1).hp());
            assertEquals(before.actors(), after.actors(), "an attack never invents movement/heading acknowledgement");
            assertEquals(List.of(1L, 2L), event.participants().stream().map(ModernMatchSettlement.Participant::entityId).toList());
            assertEquals(List.of(7, 42), event.participants().stream().map(p -> p.key().sourceId()).toList());
            assertTrue(event.participants().stream().allMatch(p -> p.key().objectId() == 3333));
            assertEquals(List.of(ModernMatchSettlement.Verdict.WIN, ModernMatchSettlement.Verdict.LOSS), verdicts(event));
            assertThrows(UnsupportedOperationException.class, () -> event.participants().clear());
            assertNoDamageOrResult(before); // The earlier immutable frame was not changed in place.
        }
    }

    @Test void eliminationWaitsForLastOpponentAndAwardsDeadWinningTeamMembers() throws Exception {
        try (var f = new Fixture(4, 1, 0, 1, false, false, IMMEDIATE, bound -> 0)) {
            f.create(true); f.active();
            assertEquals(List.of(0, 1, 0, 1), f.view().actors().stream().map(a -> a.member().team()).toList());
            var deadAlly = f.attack(1, 1, 3).snapshot();
            assertEquals(0, deadAlly.vitals().get(2).hp()); assertNull(deadAlly.result());
            assertEquals(ModernBattleEngine.Phase.ACTIVE, deadAlly.phase());
            var firstOpponent = f.attack(0, 1, 2).snapshot();
            assertEquals(0, firstOpponent.vitals().get(1).hp()); assertNull(firstOpponent.result());
            assertEquals(ModernBattleEngine.Phase.ACTIVE, firstOpponent.phase());
            f.nano.addAndGet(ATTACK); f.epoch.set(5_000);
            var last = f.attack(0, 2, 4).snapshot();
            assertEquals(firstOpponent.revision() + 1, last.revision());
            assertEquals(ModernBattleEngine.Phase.FINISHED, last.phase());
            assertEquals(List.of(ModernMatchSettlement.Verdict.WIN, ModernMatchSettlement.Verdict.LOSS,
                    ModernMatchSettlement.Verdict.WIN, ModernMatchSettlement.Verdict.LOSS), verdicts(last.result()));
            assertEquals(0, last.vitals().get(2).hp(), "winning does not resurrect a dead ally");
            assertEquals(deadAlly.vitals().get(2).deathRevision(), last.vitals().get(2).deathRevision());
            assertEquals(5_000, last.result().finishedAtMillis());
        }
    }

    @Test void eitherTeamCanWinAndSameObjectIdsNeverMergeTheirVitalsOrVerdicts() throws Exception {
        try (var f = new Fixture()) {
            f.create(true); f.active(); var after = f.attack(1, 1, 1).snapshot();
            assertEquals(ModernBattleEngine.Phase.FINISHED, after.phase());
            assertEquals(List.of(ModernMatchSettlement.Verdict.LOSS, ModernMatchSettlement.Verdict.WIN), verdicts(after.result()));
            assertEquals(0, after.vitals().get(0).hp()); assertEquals(1, after.vitals().get(1).hp());
            assertNotEquals(after.result().participants().get(0).key(), after.result().participants().get(1).key());
        }
    }

    @Test void finishedEventSurvivesReplayLateClocksAbortRoomReleaseAndDisconnectUnchanged() throws Exception {
        try (var f = new Fixture()) {
            f.create(true); f.active(); var finished = f.attack(0, 1, 2).snapshot();
            for (long sequence : new long[]{1, 2, Long.MAX_VALUE}) {
                var denied = f.attack(0, sequence, 2);
                assertEquals(ModernBattleEngine.AttackStatus.NOT_ACTIVE, denied.status()); assertEquals(finished, denied.snapshot());
            }
            var move = f.move(0, 1, cell(2, 2), 0);
            assertEquals(ModernBattleEngine.MoveStatus.NOT_ACTIVE, move.status()); assertEquals(finished, move.snapshot());
            f.ack(0); f.ack(1); assertEquals(finished, f.engine.tick());
            f.nano.addAndGet(TimeUnit.DAYS.toNanos(1)); f.epoch.set(0);
            assertEquals(finished, f.view()); assertEquals(finished, f.engine.abort());
            assertTrue(f.rooms.rooms.releasePreparation(f.ticket));
            assertEquals(finished, f.view());
            f.engine.disconnect(f.players.get(1).owner());
            assertEquals(1, f.engine.ownerCount()); assertEquals(finished, f.engine.tick());
            assertSame(finished.result(), f.view().result(), "no replacement UUID/event can be generated by a replay");
        }
    }

    @Test void activeTimeoutIsAnAbortWithNoGuessedDrawOrWinnerEvenAtTheLastAttackBoundary() throws Exception {
        try (var f = new Fixture()) {
            f.create(true); var before = f.active();
            f.nano.addAndGet(TimeUnit.MILLISECONDS.toNanos(IMMEDIATE.activeTimeoutMillis()) - 1);
            assertEquals(ModernBattleEngine.Phase.ACTIVE, f.engine.tick().phase());
            f.nano.incrementAndGet(); var late = f.attack(0, 1, 2);
            assertEquals(ModernBattleEngine.AttackStatus.NOT_ACTIVE, late.status());
            assertAbortWithoutMutation(before, late.snapshot(), ModernBattleEngine.AbortReason.ACTIVE_TIMEOUT);
            assertEquals(late.snapshot(), f.engine.abort()); assertEquals(late.snapshot(), f.engine.tick());
        }
    }

    @Test void resolverReturningAtActiveDeadlineCannotCommitALethalHitOrItsSequence() throws Exception {
        var time = new AtomicLong(1_000_000_000L); var draws = new AtomicLong();
        long deadline = time.get() + TimeUnit.MILLISECONDS.toNanos(IMMEDIATE.activeTimeoutMillis());
        ModernPhysicalCombat.BoundedRandom delayed = bound -> { draws.incrementAndGet(); time.set(deadline); return 0; };
        try (var f = new Fixture(2, 1, 0, 1, false, false, IMMEDIATE, delayed, time, new AtomicLong(1_000))) {
            f.create(true); var before = f.active(); time.set(deadline - 1);
            var late = f.attack(0, 1, 2);
            assertTrue(draws.get() > 0, "the physical resolver, not an earlier state rejection, crosses the boundary");
            assertEquals(ModernBattleEngine.AttackStatus.NOT_ACTIVE, late.status());
            assertAbortWithoutMutation(before, late.snapshot(), ModernBattleEngine.AbortReason.ACTIVE_TIMEOUT);
        }
    }

    @Test void equipmentExpiryBeforeOrDuringFinalResolverCannotBecomeAWin() throws Exception {
        for (boolean duringResolver : List.of(false, true)) {
            var epoch = new AtomicLong(1_000); var draws = new AtomicLong();
            ModernPhysicalCombat.BoundedRandom random = bound -> {
                draws.incrementAndGet(); if (duringResolver) epoch.set(2_000); return 0;
            };
            try (var f = new Fixture(2, 1, 1, 1, false, true, IMMEDIATE, random, new AtomicLong(1_000_000_000L), epoch)) {
                f.create(true); var before = f.active();
                assertTrue(f.prepared.stream().allMatch(p -> p.fighter().profile().validUntilMillis() == 2_000));
                if (!duringResolver) epoch.set(2_000);
                var late = f.attack(0, 1, 2);
                assertEquals(duringResolver, draws.get() > 0);
                assertEquals(ModernBattleEngine.AttackStatus.NOT_ACTIVE, late.status());
                assertAbortWithoutMutation(before, late.snapshot(), ModernBattleEngine.AbortReason.SERVER_ABORT);
            }
        }
    }

    @Test void cancellationDisconnectServerAbortAndClockRegressionNeverAwardSurvivors() throws Exception {
        for (var reason : List.of(ModernBattleEngine.AbortReason.PREPARATION_CANCELLED,
                ModernBattleEngine.AbortReason.DISCONNECTED, ModernBattleEngine.AbortReason.SERVER_ABORT,
                ModernBattleEngine.AbortReason.CLOCK_REGRESSED)) {
            try (var f = new Fixture()) {
                f.create(true); var before = f.active();
                switch (reason) {
                    case PREPARATION_CANCELLED -> f.rooms.rooms.releasePreparation(f.ticket);
                    case DISCONNECTED -> f.engine.disconnect(f.players.get(1).owner());
                    case SERVER_ABORT -> f.engine.abort();
                    case CLOCK_REGRESSED -> f.nano.decrementAndGet();
                    default -> throw new AssertionError("unexpected test case");
                }
                var denied = f.attack(0, 1, 2);
                assertEquals(ModernBattleEngine.AttackStatus.NOT_ACTIVE, denied.status());
                assertAbortWithoutMutation(before, denied.snapshot(), reason);
            }
        }
        try (var f = new Fixture()) {
            f.create(true); var loading = f.view();
            f.nano.addAndGet(TimeUnit.MILLISECONDS.toNanos(IMMEDIATE.loadTimeoutMillis()));
            assertAbortWithoutMutation(loading, f.engine.tick(), ModernBattleEngine.AbortReason.LOAD_TIMEOUT);
        }
    }

    @Test void competingLastHitsPublishOneTerminalRevisionAndOneStableEvent() throws Exception {
        try (var f = new Fixture(3, 1, 0, 1, false, false, IMMEDIATE, bound -> 0)) {
            f.create(true); var before = f.active();
            var start = new CountDownLatch(1); var ready = new CountDownLatch(2);
            var workers = Executors.newFixedThreadPool(2);
            try {
                var first = workers.submit(() -> { ready.countDown(); assertTrue(start.await(3, TimeUnit.SECONDS)); return f.attack(0, 1, 2); });
                var second = workers.submit(() -> { ready.countDown(); assertTrue(start.await(3, TimeUnit.SECONDS)); return f.attack(2, 1, 2); });
                assertTrue(ready.await(3, TimeUnit.SECONDS)); start.countDown();
                var a = first.get(3, TimeUnit.SECONDS); var b = second.get(3, TimeUnit.SECONDS);
                assertEquals(1, List.of(a, b).stream().filter(r -> r.status() == ModernBattleEngine.AttackStatus.ATTACKED).count());
                assertEquals(1, List.of(a, b).stream().filter(r -> r.status() == ModernBattleEngine.AttackStatus.NOT_ACTIVE).count());
                assertEquals(a.snapshot(), b.snapshot()); assertEquals(before.revision() + 1, a.snapshot().revision());
                assertSame(a.snapshot().result(), b.snapshot().result());
                assertEquals(1, a.snapshot().vitals().stream().filter(v -> v.attackSequence() == 1).count());
                assertEquals(ModernBattleEngine.Phase.FINISHED, a.snapshot().phase());
            } finally {
                start.countDown(); workers.shutdownNow();
                assertTrue(workers.awaitTermination(3, TimeUnit.SECONDS), "both owned callers must retire");
            }
        }
    }

    @Test void settlementOptInRejectsMovementOnlyOrOtherModesWithoutConsumingTheCapture() throws Exception {
        for (boolean movementOnly : List.of(false, true)) {
            int mode = movementOnly ? 1 : 5;
            try (var f = new Fixture(2, mode, 0, 1, movementOnly, false, IMMEDIATE, bound -> 0)) {
                assertThrows(IllegalArgumentException.class, () -> f.create(true));
                assertTrue(f.ticket.isLive());
                f.createLegacy(); var before = f.active();
                var reply = f.attack(0, 1, 2);
                assertNull(reply.snapshot().result()); assertEquals(ModernBattleEngine.Phase.ACTIVE, reply.snapshot().phase());
                if (movementOnly) {
                    assertTrue(before.vitals().isEmpty()); assertEquals(before, reply.snapshot());
                } else {
                    assertEquals(ModernBattleEngine.AttackStatus.ATTACKED, reply.status()); assertEquals(0, reply.snapshot().vitals().get(1).hp());
                }
            }
        }
    }

    @Test void oldCombatConstructorStillDoesNotInventSettlementAfterElimination() throws Exception {
        try (var f = new Fixture()) {
            f.createLegacy(); f.active(); var kill = f.attack(0, 1, 2).snapshot();
            assertEquals(ModernBattleEngine.Phase.ACTIVE, kill.phase()); assertNull(kill.result());
            assertEquals(0, kill.vitals().get(1).hp());
            assertEquals(ModernBattleEngine.AttackStatus.TARGET_DEAD, f.attack(0, 2, 2).status());
            assertNull(f.engine.tick().result());
        }
    }

    @Test void sameTeamOnlyRoomCannotMintAReadyPreparationForSettlement() {
        var rooms = new ModernRoomsTest.Fixture(); var one = rooms.player(7, 3333, 30, 0); var two = rooms.player(42, 3333, 30, 0);
        try {
            ModernRoomsTest.accepted(rooms.create(one, ModernRoomsTest.draft(100, 1, 514, 0)));
            ModernRoomsTest.accepted(rooms.join(two, rooms.view(one).joined().summary(), ""));
            ModernRoomsTest.accepted(rooms.act(two, ModernRooms.Action.TEAM, 0));
            ModernRoomsTest.accepted(rooms.act(one, ModernRooms.Action.READY, 1));
            ModernRoomsTest.accepted(rooms.act(two, ModernRooms.Action.READY, 1));
            var summary = rooms.view(one).joined().summary(); assertFalse(summary.readySatisfied());
            var capture = rooms.rooms.prepareWorld(one.owner(), one.scope(), summary.id(), summary.revision());
            assertNotEquals(ModernRooms.Status.OK, capture.result().status()); assertNull(capture.ticket());
        } finally {
            for (var player : List.of(one, two)) { rooms.rooms.disconnect(player.owner()); rooms.presence.disconnect(player.owner()); }
        }
    }

    @Test void unsupportedAndInvalidNumericAttacksCannotCreateOrAnimateAResult() throws Exception {
        for (boolean unsupported : List.of(false, true)) {
            ModernPhysicalCombat.BoundedRandom random = unsupported ? bound -> 0 : bound -> bound;
            try (var f = new Fixture(2, 1, unsupported ? 4 : 0, 1, false, false, IMMEDIATE, random)) {
                f.create(true); f.active(); var reply = f.attack(0, 1, 2);
                assertEquals(unsupported ? ModernBattleEngine.AttackStatus.UNSUPPORTED : ModernBattleEngine.AttackStatus.NUMERIC, reply.status());
                assertEquals(ModernBattleEngine.Phase.ACTIVE, reply.snapshot().phase()); assertNoDamageOrResult(reply.snapshot());
                assertEquals(1, reply.snapshot().vitals().get(0).attackSequence());
            }
        }
    }

    @Test void snapshotRejectsMismatchedPhaseIdentityRosterAndNonEliminationResults() throws Exception {
        try (var f = new Fixture()) {
            f.create(true); var active = f.active(); var finished = f.attack(0, 1, 2).snapshot(); var good = finished.result();
            assertThrows(IllegalArgumentException.class, () -> snapshot(finished, ModernBattleEngine.Phase.FINISHED, finished.manifest(), finished.vitals(), null));
            assertThrows(IllegalArgumentException.class, () -> snapshot(finished, ModernBattleEngine.Phase.ACTIVE, finished.manifest(), finished.vitals(), good));
            for (int changed : new int[]{0, 1, 2, 3}) {
                var bad = new ModernMatchSettlement.Event(good.resultId(), good.matchId() + (changed == 0 ? 1 : 0),
                        good.generation() + (changed == 1 ? 1 : 0), good.engineRevision() + (changed == 2 ? 1 : 0),
                        good.mode(), changed == 3 ? 504 : good.mapId(), good.finishedAtMillis(), good.participants());
                assertThrows(IllegalArgumentException.class, () -> snapshot(finished, ModernBattleEngine.Phase.FINISHED, finished.manifest(), finished.vitals(), bad));
            }
            var wrongKey = new ArrayList<>(good.participants()); var first = wrongKey.get(0);
            wrongKey.set(0, new ModernMatchSettlement.Participant(first.entityId(),
                    new TournamentPresence.CharacterKey(first.key().sourceId(), first.key().objectId() + 1), first.team(), first.verdict()));
            var badKey = withParticipants(good, wrongKey);
            assertThrows(IllegalArgumentException.class, () -> snapshot(finished, ModernBattleEngine.Phase.FINISHED, finished.manifest(), finished.vitals(), badKey));
            var wrongTeams = good.participants().stream().map(p -> new ModernMatchSettlement.Participant(p.entityId(), p.key(), 1 - p.team(), p.verdict())).toList();
            var badTeams = withParticipants(good, wrongTeams);
            assertThrows(IllegalArgumentException.class, () -> snapshot(finished, ModernBattleEngine.Phase.FINISHED, finished.manifest(), finished.vitals(), badTeams));
            var differentMap = ModernWorldMap.fromSectorBytes(510, Map.of(SECTOR, ModernWorldMapTest.openSectorBytes()), () -> false);
            // A binding may explicitly map a logical room map to a different shared physical map.
            // Result identity follows room configuration; this value cannot re-prove the binding.
            assertDoesNotThrow(() -> snapshot(finished, ModernBattleEngine.Phase.FINISHED, differentMap.manifest(), finished.vitals(), good));
            assertThrows(IllegalArgumentException.class, () -> snapshot(finished, ModernBattleEngine.Phase.FINISHED, finished.manifest(), List.of(), good));
            var aliveLoser = new ArrayList<>(finished.vitals()); aliveLoser.set(1, active.vitals().get(1));
            assertThrows(IllegalArgumentException.class, () -> snapshot(finished, ModernBattleEngine.Phase.FINISHED, finished.manifest(), aliveLoser, good));
            var deadWinner = new ArrayList<>(finished.vitals()); var winner = deadWinner.get(0);
            deadWinner.set(0, new ModernCombatProtocol.Vital(winner.entityId(), 0, winner.maxHp(), winner.mp(), winner.maxMp(), winner.ac(), winner.weaponType(),
                    winner.attackSequence(), winner.attackRevision(), finished.revision(), finished.revision(), winner.lastTargetEntityId(), winner.attackIntervalNanos()));
            assertThrows(IllegalArgumentException.class, () -> snapshot(finished, ModernBattleEngine.Phase.FINISHED, finished.manifest(), deadWinner, good));
        }
    }

    private static List<ModernMatchSettlement.Verdict> verdicts(ModernMatchSettlement.Event event) {
        assertNotNull(event); return event.participants().stream().map(ModernMatchSettlement.Participant::verdict).toList();
    }
    private static ModernMatchSettlement.Event withParticipants(ModernMatchSettlement.Event event, List<ModernMatchSettlement.Participant> participants) {
        return new ModernMatchSettlement.Event(event.resultId(), event.matchId(), event.generation(), event.engineRevision(), event.mode(), event.mapId(), event.finishedAtMillis(), participants);
    }
    private static ModernBattleEngine.Snapshot snapshot(ModernBattleEngine.Snapshot source, ModernBattleEngine.Phase phase,
            ModernWorldMap.Manifest manifest, List<ModernCombatProtocol.Vital> vitals, ModernMatchSettlement.Event result) {
        return new ModernBattleEngine.Snapshot(source.matchId(), source.generation(), source.revision(), phase, ModernBattleEngine.AbortReason.NONE,
                source.room(), manifest, source.actors(), vitals, result);
    }
    private static void assertNoDamageOrResult(ModernBattleEngine.Snapshot frame) {
        assertNull(frame.result());
        assertTrue(frame.vitals().stream().allMatch(v -> v.hp() == v.maxHp() && v.attackRevision() == 0 && v.hurtRevision() == 0 && v.deathRevision() == 0));
    }
    private static void assertAbortWithoutMutation(ModernBattleEngine.Snapshot before, ModernBattleEngine.Snapshot after, ModernBattleEngine.AbortReason reason) {
        assertEquals(ModernBattleEngine.Phase.ABORTED, after.phase()); assertEquals(reason, after.abortReason());
        assertEquals(before.revision() + 1, after.revision()); assertNull(after.result());
        assertEquals(before.actors(), after.actors()); assertEquals(before.vitals(), after.vitals());
    }
    private static ModernWorldMap.Cell cell(int x, int y) { return new ModernWorldMap.Cell(SECTOR.origin().x() + x, SECTOR.origin().y() + y); }
    private static ModernPhysicalCombat.Source expiresAtTwoThousand(ModernPhysicalCombat.Source source) {
        var q = source.input().equipment(); var facts = new TreeMap<Long, ModernItemLifetime.Fact>();
        for (var item : q.source().inventory()) facts.put(item.objectId(), new ModernItemLifetime.Fact(item.objectId(), item.template().catalogId(),
                item.template().kind(), item.equipped(), OptionalLong.of(0), false, OptionalLong.empty(), Optional.of("1970-01-01 00:00:01.000000"), 0, false));
        var observed = new ModernItemLifetime.Observation(q.source().sourceId(), q.source().character().objectId(), "1970-01-01 00:00:00.000000", 1_000, 0, facts);
        var qualified = new ModernEquipmentFacts.Qualified(q.source(), q.items(), q.catalog(), observed);
        return new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(qualified, source.input().weaponsQualified(), source.input().weapons()),
                source.growth(), source.templates(), source.procCatalogIds());
    }

    private static final class Fixture implements AutoCloseable {
        final ModernRoomsTest.Fixture rooms = new ModernRoomsTest.Fixture();
        final List<ModernRoomsTest.Player> players = new ArrayList<>();
        final ModernWorldMap map; final ModernWorldBinding binding; final ModernRooms.FrozenPreparation ticket;
        final List<ModernBattleEngine.PreparedMember> prepared; final ModernBattleEngine.Limits limits;
        final AtomicLong nano, epoch; final ModernPhysicalCombat.BoundedRandom random;
        ModernBattleEngine engine;

        Fixture() throws Exception { this(2, 1, 0, 1, false, false, IMMEDIATE, bound -> 0); }
        Fixture(int count, int mode, long type, long hp, boolean movementOnly, boolean expiring, ModernBattleEngine.Limits limits,
                ModernPhysicalCombat.BoundedRandom random) throws Exception {
            this(count, mode, type, hp, movementOnly, expiring, limits, random, new AtomicLong(1_000_000_000L), new AtomicLong(1_000));
        }
        Fixture(int count, int mode, long type, long hp, boolean movementOnly, boolean expiring, ModernBattleEngine.Limits limits,
                ModernPhysicalCombat.BoundedRandom random, AtomicLong nano, AtomicLong epoch) throws Exception {
            this.limits = limits; this.random = random; this.nano = nano; this.epoch = epoch;
            int mapId = mode == 1 ? 514 : 510;
            map = ModernWorldMap.fromSectorBytes(mapId, Map.of(SECTOR, ModernWorldMapTest.openSectorBytes()), () -> false);
            binding = new ModernWorldBinding(mode, mapId, map);
            int[] sources = {7, 42, 99, 101};
            for (int index = 0; index < count; index++) players.add(rooms.player(sources[index], 3333, 30, 0));
            ModernRoomsTest.accepted(rooms.create(players.get(0), ModernRoomsTest.draft(100, mode, mapId, 0)));
            for (int index = 1; index < count; index++) ModernRoomsTest.accepted(rooms.join(players.get(index), rooms.view(players.get(0)).joined().summary(), ""));
            for (var player : players) ModernRoomsTest.accepted(rooms.act(player, ModernRooms.Action.READY, 1));
            var summary = rooms.view(players.get(0)).joined().summary();
            var captured = rooms.rooms.prepareWorld(players.get(0).owner(), players.get(0).scope(), summary.id(), summary.revision());
            ModernRoomsTest.accepted(captured.result()); ticket = captured.ticket();
            var values = new ArrayList<ModernBattleEngine.PreparedMember>();
            for (int index = 0; index < ticket.participants().size(); index++) {
                var participant = ticket.participants().get(index); var member = participant.member();
                var row = new ModernApplicationProtocol.CharacterRow(member.key().objectId(), member.name(), member.characterClass(), member.sex(), member.level());
                var position = cell(2 + index % 2, 2 + index / 2);
                if (movementOnly) values.add(new ModernBattleEngine.PreparedMember(participant.owner(), participant.scope(), row, position, STEP));
                else {
                    var source = ModernPhysicalCombatTest.fixtureSource(member.key().sourceId(), row, hp, 10, type);
                    if (expiring) source = expiresAtTwoThousand(source);
                    var fighter = ModernPhysicalCombat.qualify(source, ModernCombatProfileTest.binding(source.input().equipment()), Optional.empty(), 1_000);
                    values.add(new ModernBattleEngine.PreparedMember(participant.owner(), participant.scope(), row, position, STEP, fighter, ATTACK));
                }
            }
            prepared = List.copyOf(values);
        }
        void create(boolean settlement) { engine = new ModernBattleEngine(ticket, binding, prepared, limits, nano::get, epoch::get, random, settlement); }
        void createLegacy() { engine = new ModernBattleEngine(ticket, binding, prepared, limits, nano::get, epoch::get, random); }
        ModernBattleEngine.Snapshot view() { var p = players.get(0); return engine.snapshot(p.owner(), p.scope(), ticket.matchId(), ticket.generation()); }
        void ack(int index) { var p = players.get(index); engine.ackLoaded(p.owner(), p.scope(), ticket.matchId(), ticket.generation(), map.digest()); }
        ModernBattleEngine.Snapshot active() { for (int index = 0; index < players.size(); index++) ack(index); nano.addAndGet(limits.countdownNanos()); var state = engine.tick(); assertEquals(ModernBattleEngine.Phase.ACTIVE, state.phase()); return state; }
        ModernBattleEngine.AttackResult attack(int index, long sequence, long target) { var p = players.get(index); return engine.attack(p.owner(), p.scope(), ticket.matchId(), ticket.generation(), sequence, target); }
        ModernBattleEngine.MoveResult move(int index, long sequence, ModernWorldMap.Cell from, int heading) { var p = players.get(index); return engine.move(p.owner(), p.scope(), ticket.matchId(), ticket.generation(), sequence, from.x(), from.y(), heading); }
        @Override public void close() {
            if (engine != null) engine.close(); rooms.rooms.releasePreparation(ticket);
            for (var p : players) { rooms.rooms.disconnect(p.owner()); rooms.presence.disconnect(p.owner()); }
        }
    }
}
