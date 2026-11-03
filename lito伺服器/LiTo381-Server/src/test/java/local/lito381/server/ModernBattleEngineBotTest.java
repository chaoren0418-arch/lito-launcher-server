package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/** Actual room capabilities and calculator inputs; no fabricated BOT human row or SQL. */
class ModernBattleEngineBotTest {
    static final long STEP = TimeUnit.MILLISECONDS.toNanos(200);
    static final long ATTACK = TimeUnit.MILLISECONDS.toNanos(700);
    static final long CAST = TimeUnit.MILLISECONDS.toNanos(1200);
    static final ModernWorldMap.SectorKey SECTOR = new ModernWorldMap.SectorKey(0x8000, 0x8000);
    static final ModernBattleEngine.Limits LIMITS = new ModernBattleEngine.Limits(2000, 0, 60000);
    static ModernWorldMap.Cell cell(int x, int y) {
        return new ModernWorldMap.Cell(SECTOR.origin().x() + x, SECTOR.origin().y() + y);
    }
    static List<ModernSkills.Skill> learned() {
        return List.of(new ModernSkills.Skill(4, "$1439", 3, 10, ModernSkills.Support.LIGHT_ARROW),
                new ModernSkills.Skill(5, "$1440", 10, 10, ModernSkills.Support.TELEPORT));
    }

    @Test void serverLoadedBotDoesNotReplaceTheOneHumanResourceAcknowledgement() throws Exception {
        try (var f = new Fixture(1, 1)) {
            var loading = f.engine.tick();
            assertEquals(ModernBattleEngine.Phase.LOADING, loading.phase());
            assertEquals(2, loading.actors().size()); assertEquals(1, f.engine.ownerCount());
            assertFalse(loading.actors().get(0).loaded()); assertTrue(loading.actors().get(1).loaded());
            assertEquals(ModernParticipantId.human(7, 1), loading.actors().get(0).member().identity());
            assertEquals(ModernParticipantId.bot(7, 1), loading.actors().get(1).member().identity());
            assertEquals(40, loading.vitals().get(1).hp()); assertEquals(0, loading.vitals().get(1).mp());
            assertEquals(0, loading.casts().get(1).requestSequence()); assertEquals(0, loading.damages().get(1).castCount());
            assertEquals(ModernBattleEngine.Phase.ACTIVE, f.ack(0).phase());
        }
    }

    @Test void missingSecondHumanAckTimesOutEvenThoughEveryBotIsLoaded() throws Exception {
        try (var f = new Fixture(2, 1)) {
            assertEquals(2, f.engine.ownerCount());
            assertEquals(ModernBattleEngine.Phase.LOADING, f.ack(0).phase());
            f.clock.addAndGet(TimeUnit.MILLISECONDS.toNanos(2000));
            var failed = f.engine.tick();
            assertEquals(ModernBattleEngine.Phase.ABORTED, failed.phase());
            assertEquals(ModernBattleEngine.AbortReason.LOAD_TIMEOUT, failed.abortReason());
            assertNull(failed.result()); assertFalse(failed.actors().get(1).loaded());
            assertTrue(failed.actors().get(2).loaded());
        }
    }

    @Test void sameNumericHumanAndBotIdentityCanFightAndPublishOneCompleteTypedResult() throws Exception {
        try (var f = new Fixture(1, 1)) {
            f.ack(0); ModernBattleEngine.AttackResult hit = null;
            for (int index = 1; index <= 3; index++) {
                hit = f.attack(index); assertEquals(ModernBattleEngine.AttackStatus.ATTACKED, hit.status());
                f.clock.addAndGet(ATTACK);
            }
            var done = hit.snapshot();
            assertEquals(ModernBattleEngine.Phase.FINISHED, done.phase()); assertTrue(done.result().hasBots());
            assertEquals(100, done.vitals().get(0).hp()); assertEquals(0, done.vitals().get(1).hp());
            assertEquals(done.revision(), done.vitals().get(1).deathRevision());
            assertEquals(done.revision(), done.result().engineRevision());
            assertEquals(List.of(ModernParticipantId.human(7, 1), ModernParticipantId.bot(7, 1)),
                    done.result().participants().stream().map(ModernMatchSettlement.Participant::identity).toList());
            assertEquals(ModernMatchSettlement.Verdict.WIN, done.result().participants().get(0).verdict());
            assertEquals(ModernMatchSettlement.Verdict.LOSS, done.result().participants().get(1).verdict());
            assertThrows(IllegalStateException.class, () -> done.result().participants().get(1).key());
            assertEquals(0, done.vitals().get(1).attackSequence());
            assertEquals(done, f.attack(4).snapshot()); assertEquals(done, f.engine.tick());
            assertEquals(done, f.engine.cast(f.room.players.get(0).owner(), f.arrowRequest(1)).snapshot());
        }
    }

    @Test void lightArrowConsumesOnlyHumanMpAndUsesRealMrFortyUntilBotDeath() throws Exception {
        try (var f = new Fixture(1, 1)) {
            f.ack(0);
            for (int index = 1; index <= 4; index++) {
                var cast = f.engine.cast(f.room.players.get(0).owner(), f.arrowRequest(index));
                assertEquals(ModernSpellProtocol.Reason.NONE, cast.reason());
                var state = cast.snapshot();
                assertEquals(30 - index * 3, state.vitals().get(0).mp());
                assertEquals(Math.max(0, 40 - index * 13), state.vitals().get(1).hp());
                assertEquals(index, state.damages().get(0).castCount());
                assertEquals(state.revision(), state.damages().get(0).castRevision());
                assertEquals(0, state.damages().get(1).castCount()); assertEquals(0, state.casts().get(1).requestSequence());
                assertEquals(0, state.vitals().get(1).mp());
                f.clock.addAndGet(CAST);
            }
            assertEquals(ModernBattleEngine.Phase.FINISHED, f.engine.tick().phase());
            assertTrue(f.engine.tick().result().hasBots());
        }
    }

    @Test void unknownOwnerCannotBorrowBotsNumericIdentityOrIssueItsCommands() throws Exception {
        try (var f = new Fixture(1, 1)) {
            f.ack(0); var before = f.engine.tick(); var stranger = f.room.presence.connect();
            try {
                var scope = f.room.players.get(0).scope();
                assertThrows(SecurityException.class, () -> f.engine.ackLoaded(stranger, scope,
                        f.ticket.matchId(), f.ticket.generation(), f.room.map.digest()));
                assertThrows(SecurityException.class, () -> f.engine.attack(stranger, scope,
                        f.ticket.matchId(), f.ticket.generation(), 1, 1));
                assertThrows(SecurityException.class, () -> f.engine.cast(stranger, f.arrowRequest(1)));
                assertEquals(before, f.engine.tick()); assertEquals(1, f.engine.ownerCount());
            } finally { f.room.presence.disconnect(stranger); }
        }
    }

    @Test void legacyConstructorAndMissingExtraOrWrongTemplateBotsCannotClaimTheTicket() throws Exception {
        try (var room = new RoomFixture(1, 1)) {
            var ticket = room.capture(); var prepared = room.prepared(ticket); var bots = room.bots(ticket);
            var clock = new AtomicLong(1000000000L);
            assertThrows(IllegalArgumentException.class, () -> new ModernBattleEngine(ticket, room.binding, prepared,
                    LIMITS, clock::get, () -> 1000, bound -> 0, true));
            assertThrows(IllegalArgumentException.class, () -> new ModernBattleEngine(ticket, room.binding, List.of(), bots,
                    LIMITS, clock::get, () -> 1000, bound -> 0, true));
            var bot = bots.get(0);
            for (var malformed : List.of(List.of(bot, bot), List.of(new ModernBattleEngine.BotPrepared(
                    ModernParticipantId.bot(42, bot.identity().id()), bot.template(), bot.spawn())),
                    List.of(new ModernBattleEngine.BotPrepared(bot.identity(), ModernBotTemplate.roomTestDummy(6, 1), bot.spawn()))))
                assertThrows(IllegalArgumentException.class, () -> new ModernBattleEngine(ticket, room.binding, prepared, malformed,
                        LIMITS, clock::get, () -> 1000, bound -> 0, true));
            try (var accepted = new ModernBattleEngine(ticket, room.binding, prepared, bots,
                    LIMITS, clock::get, () -> 1000, bound -> 0, true)) {
                assertEquals(1, accepted.ownerCount()); assertEquals(2, accepted.tick().actors().size());
            } finally { room.rooms.releasePreparation(ticket); }
        }
    }

    @Test void aLateResolverCannotCommitHpMpOrVictoryAgainstABotAfterTimeout() throws Exception {
        try (var room = new RoomFixture(1, 1)) {
            var ticket = room.capture(); var clock = new AtomicLong(1000000000L);
            try (var engine = new ModernBattleEngine(ticket, room.binding, room.prepared(ticket), room.bots(ticket),
                    LIMITS, clock::get, () -> 1000, bound -> { clock.addAndGet(TimeUnit.SECONDS.toNanos(60)); return bound - 1; }, true)) {
                var p = room.players.get(0); var before = engine.ackLoaded(p.owner(), p.scope(), ticket.matchId(), ticket.generation(), room.map.digest());
                var after = engine.attack(p.owner(), p.scope(), ticket.matchId(), ticket.generation(), 1, 2).snapshot();
                assertEquals(ModernBattleEngine.AbortReason.ACTIVE_TIMEOUT, after.abortReason()); assertNull(after.result());
                assertEquals(before.vitals(), after.vitals()); assertEquals(before.casts(), after.casts());
                assertEquals(before.damages(), after.damages()); assertEquals(before.actors(), after.actors());
            } finally { room.rooms.releasePreparation(ticket); }
        }
    }

    @Test void botPacingCannotDependOnArbitraryHumanInputOrder() throws Exception {
        try (var room = new RoomFixture(2, 1)) {
            var ticket = room.capture(); var prepared = room.prepared(ticket); var second = prepared.get(1);
            var changed = new ArrayList<>(prepared);
            changed.set(1, new ModernBattleEngine.PreparedMember(second.owner(), second.scope(), second.character(),
                    second.spawn(), STEP + 1, second.fighter(), ATTACK, second.skills(), CAST, true));
            try {
                assertThrows(IllegalArgumentException.class, () -> new ModernBattleEngine(ticket, room.binding, changed,
                        room.bots(ticket), LIMITS, () -> 1000000000L, () -> 1000, bound -> 0, true));
                try (var valid = new ModernBattleEngine(ticket, room.binding, prepared, room.bots(ticket),
                        LIMITS, () -> 1000000000L, () -> 1000, bound -> 0, true)) {
                    assertEquals(STEP, valid.tick().actors().get(2).stepIntervalNanos());
                }
            } finally { room.rooms.releasePreparation(ticket); }
        }
    }

    private static final class Fixture implements AutoCloseable {
        final RoomFixture room; final ModernRooms.FrozenPreparation ticket; final ModernBattleEngine engine;
        final AtomicLong clock = new AtomicLong(1000000000L);
        Fixture(int humans, int bots) throws Exception {
            room = new RoomFixture(humans, bots); ticket = room.capture();
            engine = new ModernBattleEngine(ticket, room.binding, room.prepared(ticket), room.bots(ticket),
                    LIMITS, clock::get, () -> 1000, bound -> bound - 1, true);
        }
        ModernBattleEngine.Snapshot ack(int index) {
            var p = room.players.get(index);
            return engine.ackLoaded(p.owner(), p.scope(), ticket.matchId(), ticket.generation(), room.map.digest());
        }
        ModernBattleEngine.AttackResult attack(long sequence) {
            var p = room.players.get(0);
            return engine.attack(p.owner(), p.scope(), ticket.matchId(), ticket.generation(), sequence, 2);
        }
        ModernSpellProtocol.Request arrowRequest(long sequence) {
            return new ModernSpellProtocol.Request(room.players.get(0).scope(), ticket.matchId(), ticket.generation(), sequence, 4, 2);
        }
        @Override public void close() { engine.close(); room.rooms.releasePreparation(ticket); room.close(); }
    }

    record Player(TournamentPresence.Owner owner, ModernRooms.Scope scope, ModernApplicationProtocol.CharacterRow row) {}
    /** Shared test fixture creates only genuine human Owners; BOT members come through the real GM room command. */
    static final class RoomFixture implements AutoCloseable {
        final TournamentPresence presence = new TournamentPresence();
        final ModernWorldMap map; final ModernWorldBinding binding; final ModernRooms rooms;
        final List<Player> players = new ArrayList<>();
        RoomFixture(int humans, int bots) throws Exception {
            map = ModernWorldMap.fromSectorBytes(514, Map.of(SECTOR, ModernWorldMapTest.openSectorBytes()), () -> false);
            binding = new ModernWorldBinding(1, 514, map);
            rooms = new ModernRooms(Set.of(binding.key()), Set.of(binding.key()));
            for (int index = 0; index < humans; index++) {
                int source = index == 0 ? 7 : 42 + index - 1;
                var owner = presence.connect(); var row = new ModernApplicationProtocol.CharacterRow(1, "human-" + index, 1, 0, 80);
                var selection = new TournamentPresence.Selection(new TournamentPresence.CharacterKey(source, 1), "server-" + source,
                        row.name(), row.level(), row.characterClass(), row.sex());
                assertEquals(TournamentPresence.SelectStatus.ACCEPTED, presence.select(owner, selection));
                rooms.enterLobby(owner, selection, 1, 0, true);
                var player = new Player(owner, new ModernRooms.Scope(source, 1, 1, 0), row); players.add(player);
                var summary = index == 0 ? null : view().summary();
                var command = index == 0 ? new ModernRooms.Command(player.scope(), ModernRooms.Action.CREATE, 0, 0, 0,
                        ModernRoomsTest.draft(100, 1, 514, 0), "") : new ModernRooms.Command(player.scope(), ModernRooms.Action.JOIN,
                        summary.id(), summary.revision(), 0, null, "");
                ModernRoomsTest.accepted(rooms.execute(owner, command));
            }
            var first = players.get(0); var summary = view().summary();
            var allowed = new ModernGmCommands.Snapshot(first.scope().sourceId(), "fixture-owner", first.row(), 200, false, false,
                    new ModernGmCommands.Definition("de", 200, "L1SpawnDe", false));
            var added = rooms.spawnBots(first.owner(), first.scope(), summary.id(), summary.revision(), bots, allowed);
            ModernRoomsTest.accepted(added.result()); assertEquals(bots, added.added());
            for (var player : players) {
                summary = view().summary(); ModernRoomsTest.accepted(rooms.execute(player.owner(), new ModernRooms.Command(
                        player.scope(), ModernRooms.Action.READY, summary.id(), summary.revision(), 1, null, "")));
            }
        }
        ModernRooms.RoomSnapshot view() {
            var p = players.get(0);
            return rooms.execute(p.owner(), new ModernRooms.Command(p.scope(), ModernRooms.Action.READ, 0, 0, 0, null, "")).view().joined();
        }
        ModernRooms.FrozenPreparation capture() {
            var p = players.get(0); var s = view().summary();
            var result = rooms.prepareWorld(p.owner(), p.scope(), s.id(), s.revision());
            ModernRoomsTest.accepted(result.result()); return result.ticket();
        }
        List<ModernBattleEngine.PreparedMember> prepared(ModernRooms.FrozenPreparation ticket) {
            var result = new ArrayList<ModernBattleEngine.PreparedMember>();
            for (var participant : ticket.participants()) {
                var member = participant.member(); var player = players.stream().filter(p -> p.owner() == participant.owner()).findFirst().orElseThrow();
                var fighter = ModernMagicCombatTest.createFighter(member.key().sourceId(), player.row(), 100, 30, 12, 0);
                result.add(new ModernBattleEngine.PreparedMember(player.owner(), player.scope(), player.row(),
                        cell(10 + ticket.snapshot().members().indexOf(member), 10), STEP, fighter, ATTACK,
                        new ModernSkills.Snapshot(member.key().sourceId(), player.row(), learned()), CAST, true));
            }
            return List.copyOf(result);
        }
        List<ModernBattleEngine.BotPrepared> bots(ModernRooms.FrozenPreparation ticket) {
            return ticket.bots().stream().map(member -> new ModernBattleEngine.BotPrepared(member.identity(),
                    ModernBotTemplate.roomTestDummy(member.characterClass(), member.sex()),
                    cell(10 + ticket.snapshot().members().indexOf(member), 10))).toList();
        }
        @Override public void close() {
            for (var player : players) { rooms.disconnect(player.owner()); presence.disconnect(player.owner()); }
            assertEquals(0, rooms.memberCount()); assertEquals(0, rooms.ownerCount());
        }
    }
}
