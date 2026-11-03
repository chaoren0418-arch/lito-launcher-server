package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/** Pure real room preparation + qualified engine; no SQL, sockets, assets or client RNG. */
class ModernBattleEngineSpellTest {
    private static final ModernWorldMap.SectorKey SECTOR = new ModernWorldMap.SectorKey(0x8000, 0x8000);
    private static final long STEP = TimeUnit.MILLISECONDS.toNanos(200);
    private static final long ATTACK = TimeUnit.MILLISECONDS.toNanos(700);
    private static final long CAST = TimeUnit.MILLISECONDS.toNanos(1_200);
    private static final ModernBattleEngine.Limits IMMEDIATE = new ModernBattleEngine.Limits(10_000, 0, 60_000);
    private static final int[] DX = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final int[] DY = {-1, -1, 0, 1, 1, 1, 0, -1};

    private static ModernWorldMap.Cell cell(int x, int y) {
        return new ModernWorldMap.Cell(SECTOR.origin().x() + x, SECTOR.origin().y() + y);
    }
    private static ModernWorldMap map(boolean exactlyTwoCells) throws Exception {
        byte[] bytes = ModernWorldMapTest.openSectorBytes();
        if (exactlyTwoCells) {
            for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++)
                ModernWorldMapTest.setAttributes(bytes, x, y, (short) 1, (short) 1);
            // The one east edge creates exactly the two connected, occupied standing cells.
            ModernWorldMapTest.setAttributes(bytes, 2, 2, (short) 1, (short) 0);
        }
        return ModernWorldMap.fromSectorBytes(514, Map.of(SECTOR, bytes), () -> false);
    }
    private static List<ModernSkills.Skill> learned() {
        return List.of(new ModernSkills.Skill(5, "$1440", 10, 10, ModernSkills.Support.TELEPORT),
                new ModernSkills.Skill(77, "$ultimate", 70, 5_000, ModernSkills.Support.UNIMPLEMENTED));
    }

    @Test void initialQualifiedSkillsAndI64MpBelongToExactSourceActor() throws Exception {
        try (var f = new Fixture(map(false), Long.MAX_VALUE, 100, learned(), List.of(), IMMEDIATE, bound -> 0, false)) {
            var snapshot = f.view();
            assertEquals(ModernBattleEngine.Phase.LOADING, snapshot.phase());
            assertEquals(List.of(7, 42), snapshot.actors().stream().map(a -> a.member().key().sourceId()).toList());
            assertEquals(List.of(3333L, 3333L), snapshot.actors().stream().map(a -> a.member().key().objectId()).toList());
            assertEquals(List.of(ModernSpellProtocol.CastState.initial(1, 1_200), ModernSpellProtocol.CastState.initial(2, 1_700)), snapshot.casts());
            assertTrue(snapshot.vitals().stream().allMatch(v -> v.mp() == Long.MAX_VALUE && v.maxMp() == Long.MAX_VALUE));
            assertThrows(UnsupportedOperationException.class, () -> snapshot.casts().clear());
            f.active(); var cast = f.cast(0, 1, 5);
            assertEquals(ModernSpellProtocol.Reason.NONE, cast.reason());
            assertEquals(Long.MAX_VALUE - 10, cast.snapshot().vitals().get(0).mp());
            assertEquals(Long.MAX_VALUE, cast.snapshot().vitals().get(1).mp());
            assertEquals(List.of(1_200, 1_700), cast.snapshot().casts().stream().map(ModernSpellProtocol.CastState::castIntervalMillis).toList());
        }
    }

    @Test void loadingAndCountdownDenialsConsumeOnlyRequestSequenceBeforeRealBarrier() throws Exception {
        var limits = new ModernBattleEngine.Limits(10_000, TimeUnit.SECONDS.toNanos(3), 60_000);
        try (var f = new Fixture(map(false), 30, 100, learned(), learned(), limits, bound -> 0, false)) {
            var loading = f.cast(0, 1, 5);
            assertEquals(ModernSpellProtocol.Reason.NOT_ACTIVE, loading.reason()); assertUncast(loading.snapshot(), 1, 30);
            f.ack(0); f.ack(1);
            var countdown = f.cast(0, 2, 5);
            assertEquals(ModernSpellProtocol.Reason.NOT_ACTIVE, countdown.reason()); assertUncast(countdown.snapshot(), 2, 30);
            f.clock.addAndGet(limits.countdownNanos());
            assertEquals(ModernBattleEngine.Phase.ACTIVE, f.engine.tick().phase());
            assertEquals(ModernSpellProtocol.Reason.NONE, f.cast(0, 3, 5).reason());
        }
    }

    @Test void serverTeleportPublishesMpPositionAndEventTogetherAndNextMoveUsesActualDestination() throws Exception {
        try (var f = new Fixture()) {
            var before = f.active(); var prior = before.actors().get(0);
            var result = f.cast(0, 1, 5); var after = result.snapshot(); var actor = after.actors().get(0); var cast = after.casts().get(0);
            assertEquals(ModernSpellProtocol.Reason.NONE, result.reason());
            assertEquals(before.revision() + 1, after.revision());
            assertEquals(after.revision(), cast.castRevision()); assertEquals(1, cast.requestSequence());
            assertEquals(1, cast.castCount()); assertEquals(10, cast.mpSpent()); assertEquals(5, cast.skillId());
            assertEquals(1_200, cast.castIntervalMillis());
            assertEquals(prior.position(), new ModernWorldMap.Cell(cast.fromX(), cast.fromY()));
            assertEquals(actor.position(), new ModernWorldMap.Cell(cast.toX(), cast.toY()));
            assertNotEquals(prior.position(), actor.position()); assertNotEquals(after.actors().get(1).position(), actor.position());
            assertTrue(f.map.standing(actor.position())); assertEquals(before.manifest(), after.manifest());
            assertEquals(prior.lastMoveSequence(), actor.lastMoveSequence()); assertEquals(prior.heading(), actor.heading());
            assertEquals(20, after.vitals().get(0).mp()); assertEquals(30, before.vitals().get(0).mp());
            assertEquals(before.actors().get(1), after.actors().get(1)); assertEquals(before.vitals().get(1), after.vitals().get(1));
            assertEquals(0, after.vitals().get(0).attackRevision()); assertEquals(0, after.vitals().get(0).hurtRevision());
            f.clock.addAndGet(CAST);
            int heading = f.availableHeading(actor.position());
            var oldCoordinates = f.move(0, 1, prior.position(), heading);
            assertEquals(ModernBattleEngine.MoveStatus.STALE_POSITION, oldCoordinates.status());
            assertEquals(actor.position(), oldCoordinates.snapshot().actors().get(0).position());
            var moved = f.move(0, 2, actor.position(), heading);
            assertEquals(ModernBattleEngine.MoveStatus.MOVED, moved.status());
            assertEquals(new ModernWorldMap.Cell(actor.position().x() + DX[heading], actor.position().y() + DY[heading]),
                    moved.snapshot().actors().get(0).position());
            assertEquals(after.casts(), moved.snapshot().casts()); assertEquals(20, moved.snapshot().vitals().get(0).mp());
        }
    }

    @Test void unlearnedAndUnsupportedAreSourceSpecificAndNeverInventAVisualEvent() throws Exception {
        try (var f = new Fixture(map(false), 30, 100, learned(), List.of(), IMMEDIATE, bound -> 0, false)) {
            f.active();
            var unknown = f.cast(0, 1, 6);
            assertEquals(ModernSpellProtocol.Reason.UNLEARNED, unknown.reason()); assertUncast(unknown.snapshot(), 1, 30);
            var unsupported = f.cast(0, 2, 77);
            assertEquals(ModernSpellProtocol.Reason.UNSUPPORTED, unsupported.reason()); assertUncast(unsupported.snapshot(), 2, 30);
            var otherSource = f.cast(1, 1, 5);
            assertEquals(ModernSpellProtocol.Reason.UNLEARNED, otherSource.reason());
            assertEquals(0, otherSource.snapshot().casts().get(1).castCount());
            assertEquals(1, otherSource.snapshot().casts().get(1).requestSequence());
            assertEquals(30, otherSource.snapshot().vitals().get(1).mp());
            assertEquals(ModernSpellProtocol.Reason.NONE, f.cast(0, 3, 5).reason()); // Denials do not start casting cooldown.
        }
        var invalidTemplate = List.of(new ModernSkills.Skill(5, "$1440", 11, 10, ModernSkills.Support.UNSUPPORTED_TEMPLATE));
        try (var f = new Fixture(map(false), 30, 100, invalidTemplate, learned(), IMMEDIATE, bound -> 0, false)) {
            f.active(); var result = f.cast(0, 1, 5);
            assertEquals(ModernSpellProtocol.Reason.UNSUPPORTED, result.reason()); assertUncast(result.snapshot(), 1, 30);
        }
    }

    @Test void exactMpExhaustionAndPolicyCooldownNeverRestartOrUnderflowTheEvent() throws Exception {
        try (var f = new Fixture(map(false), 10, 100, learned(), learned(), IMMEDIATE, bound -> 0, false)) {
            f.active(); var first = f.cast(0, 1, 5).snapshot(); var firstCast = first.casts().get(0);
            assertEquals(0, first.vitals().get(0).mp());
            f.clock.addAndGet(TimeUnit.MILLISECONDS.toNanos(10)); // Template reuse alone cannot bypass the qualified policy.
            var immediate = f.cast(0, 2, 5); assertEquals(ModernSpellProtocol.Reason.TOO_FAST, immediate.reason());
            assertSameCastEvent(firstCast, immediate.snapshot().casts().get(0));
            f.clock.addAndGet(CAST - TimeUnit.MILLISECONDS.toNanos(10) - 1);
            assertEquals(ModernSpellProtocol.Reason.TOO_FAST, f.cast(0, 3, 5).reason());
            f.clock.incrementAndGet(); var empty = f.cast(0, 4, 5);
            assertEquals(ModernSpellProtocol.Reason.NO_MP, empty.reason()); assertEquals(0, empty.snapshot().vitals().get(0).mp());
            assertSameCastEvent(firstCast, empty.snapshot().casts().get(0));
            assertEquals(4, empty.snapshot().casts().get(0).requestSequence());
            assertEquals(ModernSpellProtocol.Reason.NO_MP, f.cast(0, 5, 5).reason());
        }
    }

    @Test void moveAttackAndCastShareCooldownFencesInBothDirections() throws Exception {
        for (boolean attackFirst : List.of(false, true)) {
            try (var f = new Fixture()) {
                f.active();
                if (attackFirst) assertEquals(ModernBattleEngine.AttackStatus.ATTACKED, f.attack(0, 1, 2).status());
                else assertEquals(ModernBattleEngine.MoveStatus.MOVED, f.move(0, 1, cell(2, 2), 0).status());
                var before = f.view(); var rejected = f.cast(0, 1, 5);
                assertEquals(ModernSpellProtocol.Reason.TOO_FAST, rejected.reason());
                assertEquals(before.actors(), rejected.snapshot().actors()); assertEquals(before.vitals(), rejected.snapshot().vitals());
                f.clock.addAndGet(attackFirst ? ATTACK : STEP);
                var success = f.cast(0, 2, 5); assertEquals(ModernSpellProtocol.Reason.NONE, success.reason());
                var position = success.snapshot().actors().get(0).position(); int heading = f.availableHeading(position);
                long moveSequence = attackFirst ? 1 : 2, attackSequence = attackFirst ? 2 : 1;
                assertEquals(ModernBattleEngine.MoveStatus.TOO_FAST, f.move(0, moveSequence, position, heading).status());
                assertEquals(ModernBattleEngine.AttackStatus.TOO_FAST, f.attack(0, attackSequence, 2).status());
                f.clock.addAndGet(CAST);
                assertEquals(ModernBattleEngine.MoveStatus.MOVED, f.move(0, moveSequence + 1, position, heading).status());
            }
        }
    }

    @Test void scopeOwnerMatchGenerationAndSequenceAreValidatedBeforeAnyMutation() throws Exception {
        try (var f = new Fixture()) {
            var before = f.active(); var p = f.players.get(0); var s = p.scope();
            for (var wrong : List.of(new ModernRooms.Scope(42, s.objectId(), s.epoch(), s.channelId()),
                    new ModernRooms.Scope(s.sourceId(), s.objectId() + 1, s.epoch(), s.channelId()),
                    new ModernRooms.Scope(s.sourceId(), s.objectId(), s.epoch() + 1, s.channelId()),
                    new ModernRooms.Scope(s.sourceId(), s.objectId(), s.epoch(), 1))) {
                var request = new ModernSpellProtocol.Request(wrong, f.ticket.matchId(), f.ticket.generation(), 1, 5);
                assertThrows(SecurityException.class, () -> f.engine.cast(p.owner(), request));
            }
            assertThrows(SecurityException.class, () -> f.engine.cast(f.players.get(1).owner(), f.request(0, 1, 5)));
            assertThrows(SecurityException.class, () -> f.engine.cast(p.owner(), new ModernSpellProtocol.Request(s, f.ticket.matchId() + 1, f.ticket.generation(), 1, 5)));
            assertThrows(SecurityException.class, () -> f.engine.cast(p.owner(), new ModernSpellProtocol.Request(s, f.ticket.matchId(), f.ticket.generation() + 1, 1, 5)));
            assertThrows(SecurityException.class, () -> f.cast(0, 2, 5)); assertEquals(before, f.view());
            f.cast(0, 1, 5); var after = f.view();
            assertThrows(SecurityException.class, () -> f.cast(0, 1, 5)); assertEquals(after, f.view());
        }
    }

    @Test void invalidRandomSelectionConsumesOnlyRequestAndCannotMutateCoordinatesMpOrCooldown() throws Exception {
        for (boolean negative : List.of(false, true)) {
            try (var f = new Fixture(map(false), 30, 100, learned(), learned(), IMMEDIATE, bound -> negative ? -1 : bound, false)) {
                var before = f.active(); var result = f.cast(0, 1, 5);
                assertEquals(ModernSpellProtocol.Reason.NUMERIC, result.reason()); assertUncast(result.snapshot(), 1, 30);
                assertEquals(before.actors(), result.snapshot().actors()); assertEquals(before.vitals(), result.snapshot().vitals());
                assertEquals(ModernSpellProtocol.Reason.NUMERIC, f.cast(0, 2, 5).reason());
            }
        }
    }

    @Test void slowRandomReturningAfterDeadlineOrClockRegressionCannotPublishPartialTeleport() throws Exception {
        for (boolean regress : List.of(false, true)) for (int randomReply : List.of(0, 1, 2)) {
            var clock = new AtomicLong(1_000_000_000L); var calls = new AtomicLong();
            ModernPhysicalCombat.BoundedRandom delayed = bound -> {
                calls.incrementAndGet(); clock.addAndGet(regress ? -1 : TimeUnit.MILLISECONDS.toNanos(IMMEDIATE.activeTimeoutMillis()));
                return randomReply == 0 ? 0 : randomReply == 1 ? -1 : bound;
            };
            try (var f = new Fixture(map(false), 30, 100, learned(), learned(), IMMEDIATE, delayed, false, clock)) {
                var before = f.active(); var result = f.cast(0, 1, 5);
                assertEquals(1, calls.get()); assertEquals(ModernSpellProtocol.Reason.NOT_ACTIVE, result.reason());
                var after = result.snapshot(); assertEquals(ModernBattleEngine.Phase.ABORTED, after.phase());
                assertEquals(regress ? ModernBattleEngine.AbortReason.CLOCK_REGRESSED : ModernBattleEngine.AbortReason.ACTIVE_TIMEOUT, after.abortReason());
                assertEquals(before.revision() + 1, after.revision()); assertEquals(before.actors(), after.actors());
                assertEquals(before.vitals(), after.vitals()); assertEquals(before.casts(), after.casts());
                assertEquals(after, f.cast(0, 1, 5).snapshot());
            }
        }
    }

    @Test void fullyOccupiedStandingMapCannotAdmitImmobileHumansAndNeverDrawsRandom() throws Exception {
        var calls = new AtomicLong(); var tiny = map(true);
        assertEquals(List.of(cell(2, 2), cell(3, 2)), tiny.teleportCells());
        assertThrows(IllegalArgumentException.class, () -> new Fixture(tiny, 30, 100, learned(), learned(),
                IMMEDIATE, bound -> { calls.incrementAndGet(); return 0; }, false));
        assertEquals(0, calls.get(), "admission must not rely on a future teleport or attack");
    }

    @Test void cancelledPreparationAndDisconnectedPeerFenceSubsequentCastWithoutSequenceConsumption() throws Exception {
        for (boolean disconnect : List.of(false, true)) {
            try (var f = new Fixture()) {
                var before = f.active();
                if (disconnect) f.engine.disconnect(f.players.get(1).owner());
                else f.rooms.rooms.releasePreparation(f.ticket);
                var result = f.cast(0, 1, 5); var after = result.snapshot();
                assertEquals(ModernSpellProtocol.Reason.NOT_ACTIVE, result.reason()); assertEquals(ModernBattleEngine.Phase.ABORTED, after.phase());
                assertEquals(disconnect ? ModernBattleEngine.AbortReason.DISCONNECTED : ModernBattleEngine.AbortReason.PREPARATION_CANCELLED, after.abortReason());
                assertEquals(before.actors(), after.actors()); assertEquals(before.vitals(), after.vitals()); assertEquals(before.casts(), after.casts());
            }
        }
    }

    @Test void deathAndFinishedWorldCannotSpendMpOrStartCastAndFinishedHistoryIsFrozen() throws Exception {
        for (boolean settle : List.of(false, true)) {
            try (var f = new Fixture(map(false), 30, 1, learned(), learned(), IMMEDIATE, bound -> 0, settle)) {
                f.active(); var killed = f.attack(0, 1, 2).snapshot(); assertEquals(0, killed.vitals().get(1).hp());
                var denied = f.cast(1, 1, 5);
                assertEquals(settle ? ModernSpellProtocol.Reason.NOT_ACTIVE : ModernSpellProtocol.Reason.DEAD, denied.reason());
                assertEquals(killed.actors(), denied.snapshot().actors()); assertEquals(killed.vitals(), denied.snapshot().vitals());
                assertEquals(0, denied.snapshot().casts().get(1).castCount()); assertEquals(30, denied.snapshot().vitals().get(1).mp());
                if (settle) {
                    assertEquals(ModernBattleEngine.Phase.FINISHED, killed.phase()); assertNotNull(killed.result());
                    assertEquals(killed, denied.snapshot()); assertEquals(killed, f.cast(0, 1, 5).snapshot());
                    assertEquals(List.of(1_200, 1_700), killed.casts().stream().map(ModernSpellProtocol.CastState::castIntervalMillis).toList());
                    f.clock.addAndGet(TimeUnit.SECONDS.toNanos(61)); assertEquals(killed, f.engine.tick());
                } else assertEquals(1, denied.snapshot().casts().get(1).requestSequence());
            }
        }
    }

    @Test void preparedSkillsRequireExactSourceAndCharacterAndExplicitBoundedCastPolicy() throws Exception {
        try (var f = new Fixture()) {
            var p = f.prepared.get(0); var row = p.character();
            var wrongSource = new ModernSkills.Snapshot(42, row, learned());
            assertThrows(IllegalArgumentException.class, () -> prepared(p, wrongSource, CAST));
            var changed = new ModernApplicationProtocol.CharacterRow(row.objectId(), row.name(), row.characterClass(), row.sex(), row.level() + 1);
            assertThrows(IllegalArgumentException.class, () -> prepared(p, new ModernSkills.Snapshot(7, changed, learned()), CAST));
            assertThrows(IllegalArgumentException.class, () -> prepared(p, p.skills(), TimeUnit.MILLISECONDS.toNanos(10) - 1));
            assertThrows(IllegalArgumentException.class, () -> prepared(p, p.skills(), TimeUnit.SECONDS.toNanos(10) + 1));
            assertThrows(IllegalArgumentException.class, () -> prepared(p, p.skills(), CAST + 1)); // No silent nanos-to-ms truncation.
            assertThrows(IllegalArgumentException.class, () -> prepared(p, p.skills(), CAST + 999_999));
            assertDoesNotThrow(() -> prepared(p, p.skills(), TimeUnit.MILLISECONDS.toNanos(10)));
            assertDoesNotThrow(() -> prepared(p, p.skills(), TimeUnit.SECONDS.toNanos(10)));
            assertThrows(IllegalArgumentException.class, () -> prepared(p, null, CAST));
            assertThrows(IllegalArgumentException.class, () -> new ModernBattleEngine.PreparedMember(p.owner(), p.scope(), row, p.spawn(), STEP, null, 0, p.skills(), CAST));
        }
    }

    private static ModernBattleEngine.PreparedMember prepared(ModernBattleEngine.PreparedMember p, ModernSkills.Snapshot skills, long interval) {
        return new ModernBattleEngine.PreparedMember(p.owner(), p.scope(), p.character(), p.spawn(), p.stepIntervalNanos(), p.fighter(), p.attackIntervalNanos(), skills, interval);
    }
    private static void assertUncast(ModernBattleEngine.Snapshot snapshot, long sequence, long mp) {
        var cast = snapshot.casts().get(0);
        assertEquals(sequence, cast.requestSequence()); assertEquals(0, cast.castCount()); assertEquals(0, cast.castRevision());
        assertEquals(0, cast.mpSpent()); assertEquals(0, cast.skillId()); assertEquals(0, cast.fromX()); assertEquals(0, cast.fromY());
        assertEquals(0, cast.toX()); assertEquals(0, cast.toY()); assertEquals(mp, snapshot.vitals().get(0).mp());
        assertEquals(1_200, cast.castIntervalMillis());
    }
    private static void assertSameCastEvent(ModernSpellProtocol.CastState before, ModernSpellProtocol.CastState after) {
        assertEquals(before.entityId(), after.entityId()); assertEquals(before.castCount(), after.castCount()); assertEquals(before.castRevision(), after.castRevision());
        assertEquals(before.mpSpent(), after.mpSpent()); assertEquals(before.skillId(), after.skillId());
        assertEquals(before.fromX(), after.fromX()); assertEquals(before.fromY(), after.fromY()); assertEquals(before.toX(), after.toX()); assertEquals(before.toY(), after.toY());
        assertEquals(before.castIntervalMillis(), after.castIntervalMillis());
    }

    private static final class Fixture implements AutoCloseable {
        final ModernRoomsTest.Fixture rooms = new ModernRoomsTest.Fixture();
        final List<ModernRoomsTest.Player> players = new ArrayList<>();
        final ModernRooms.FrozenPreparation ticket;
        final ModernWorldMap map;
        final AtomicLong clock;
        final ModernBattleEngine.Limits limits;
        final List<ModernBattleEngine.PreparedMember> prepared;
        final ModernBattleEngine engine;
        Fixture() throws Exception { this(map(false), 30, 100, learned(), learned(), IMMEDIATE, bound -> 0, false); }
        Fixture(ModernWorldMap map, long mp, long hp, List<ModernSkills.Skill> first, List<ModernSkills.Skill> second,
                ModernBattleEngine.Limits limits, ModernPhysicalCombat.BoundedRandom random, boolean settlement) throws Exception {
            this(map, mp, hp, first, second, limits, random, settlement, new AtomicLong(1_000_000_000L));
        }
        Fixture(ModernWorldMap map, long mp, long hp, List<ModernSkills.Skill> first, List<ModernSkills.Skill> second,
                ModernBattleEngine.Limits limits, ModernPhysicalCombat.BoundedRandom random, boolean settlement, AtomicLong clock) throws Exception {
            this.map = map; this.clock = clock; this.limits = limits;
            players.add(rooms.player(7, 3333, 30, 0)); players.add(rooms.player(42, 3333, 30, 0));
            ModernRoomsTest.accepted(rooms.create(players.get(0), ModernRoomsTest.draft(2, 1, 514, 0)));
            ModernRoomsTest.accepted(rooms.join(players.get(1), rooms.view(players.get(0)).joined().summary(), ""));
            for (var player : players) ModernRoomsTest.accepted(rooms.act(player, ModernRooms.Action.READY, 1));
            var summary = rooms.view(players.get(0)).joined().summary();
            var preparation = rooms.rooms.prepareWorld(players.get(0).owner(), players.get(0).scope(), summary.id(), summary.revision());
            ModernRoomsTest.accepted(preparation.result()); ticket = preparation.ticket();
            var input = new ArrayList<ModernBattleEngine.PreparedMember>();
            for (int i = 0; i < ticket.participants().size(); i++) {
                var participant = ticket.participants().get(i); var member = participant.member();
                var row = new ModernApplicationProtocol.CharacterRow(member.key().objectId(), member.name(), member.characterClass(), member.sex(), member.level());
                var source = ModernPhysicalCombatTest.fixtureSource(member.key().sourceId(), row, hp, 10, 0);
                var equipment = ModernCombatProfileTest.withVitals(source.input().equipment(), hp, mp, 999, 999, 10);
                source = new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(equipment, true, source.input().weapons()),
                        source.growth(), source.templates(), source.procCatalogIds());
                var fighter = ModernPhysicalCombat.qualify(source, ModernCombatProfileTest.binding(equipment), Optional.empty(), 0);
                var skills = new ModernSkills.Snapshot(member.key().sourceId(), row, i == 0 ? first : second);
                input.add(new ModernBattleEngine.PreparedMember(participant.owner(), participant.scope(), row, cell(2 + i, 2), STEP, fighter, ATTACK, skills,
                        CAST + TimeUnit.MILLISECONDS.toNanos(i * 500L)));
            }
            prepared = List.copyOf(input);
            try { engine = new ModernBattleEngine(ticket, new ModernWorldBinding(1, 514, map), prepared, limits, clock::get, () -> 1_000L, random, settlement); }
            catch (RuntimeException failure) {
                rooms.rooms.releasePreparation(ticket);
                for (var p : players) { rooms.rooms.disconnect(p.owner()); rooms.presence.disconnect(p.owner()); }
                throw failure;
            }
        }
        ModernBattleEngine.Snapshot view() {
            var p = players.get(0); return engine.snapshot(p.owner(), p.scope(), ticket.matchId(), ticket.generation());
        }
        void ack(int index) { var p = players.get(index); engine.ackLoaded(p.owner(), p.scope(), ticket.matchId(), ticket.generation(), map.digest()); }
        ModernBattleEngine.Snapshot active() {
            ack(0); ack(1); clock.addAndGet(limits.countdownNanos());
            var snapshot = engine.tick(); assertEquals(ModernBattleEngine.Phase.ACTIVE, snapshot.phase()); return snapshot;
        }
        ModernSpellProtocol.Request request(int index, long sequence, int skill) {
            return new ModernSpellProtocol.Request(players.get(index).scope(), ticket.matchId(), ticket.generation(), sequence, skill);
        }
        ModernBattleEngine.CastResult cast(int index, long sequence, int skill) { return engine.cast(players.get(index).owner(), request(index, sequence, skill)); }
        ModernBattleEngine.AttackResult attack(int index, long sequence, long target) {
            var p = players.get(index); return engine.attack(p.owner(), p.scope(), ticket.matchId(), ticket.generation(), sequence, target);
        }
        ModernBattleEngine.MoveResult move(int index, long sequence, ModernWorldMap.Cell from, int heading) {
            var p = players.get(index); return engine.move(p.owner(), p.scope(), ticket.matchId(), ticket.generation(), sequence, from.x(), from.y(), heading);
        }
        int availableHeading(ModernWorldMap.Cell from) {
            var positions = view().actors().stream().map(ModernBattleEngine.Actor::position).toList();
            for (int heading = 0; heading < 8; heading++) {
                var to = new ModernWorldMap.Cell(from.x() + DX[heading], from.y() + DY[heading]);
                if (map.canStep(from, to) && !positions.contains(to)) return heading;
            }
            throw new AssertionError("synthetic open map must have an unoccupied step");
        }
        @Override public void close() {
            engine.close(); rooms.rooms.releasePreparation(ticket);
            for (var p : players) { rooms.rooms.disconnect(p.owner()); rooms.presence.disconnect(p.owner()); }
        }
    }
}
