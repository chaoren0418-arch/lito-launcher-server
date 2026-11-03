package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ModernNpcRosterTest {
    static final ModernNpcRoster.Scope SCOPE = new ModernNpcRoster.Scope(11, 22, 33);
    static final ModernWorldMap.SectorKey KEY = ModernWorldMapTest.KEY;
    static ModernWorldMap.Cell cell(int x, int y) { return new ModernWorldMap.Cell(KEY.origin().x() + x, KEY.origin().y() + y); }
    static ModernWorldMap map() throws Exception { return ModernWorldMap.fromSectorBytes(554, Map.of(KEY, ModernWorldMapTest.openSectorBytes()), () -> false); }
    static ModernNpcRoster.Template template() { return new ModernNpcRoster.Template(7, "catalog", 45262, 1649, 280, 10, 100, 750); }
    static ModernNpcRoster.Spawn spawn(int x, int y) { return new ModernNpcRoster.Spawn(template(), 0, 1, cell(x, y), 2); }
    static ModernNpcRoster roster() throws Exception { return roster(map(), 128); }
    static ModernNpcRoster roster(ModernWorldMap map, int capacity) {
        return new ModernNpcRoster(SCOPE, map, List.of(cell(2, 2), cell(3, 2)), capacity, 3000);
    }
    static ModernNpcRoster.Actor one(ModernNpcRoster roster) { return roster.spawn(SCOPE, List.of(spawn(10, 10)), Set.of()).get(0); }
    static ModernNpcRoster.Target target(int source, int lane, int x, int y) {
        return new ModernNpcRoster.Target(ModernParticipantId.human(source, 3333), lane, cell(x, y), true);
    }
    static ModernNpcRoster.Plan plan(ModernNpcRoster roster, ModernNpcRoster.Actor actor, int heading) {
        return new ModernNpcRoster.Plan(actor.id(), actor.life(), roster.snapshot().revision(), ModernNpcRoster.PlanKind.MOVE, heading,
                ModernParticipantId.human(7, 3333), 1);
    }
    static void rejectedUnchanged(ModernNpcRoster roster, org.junit.jupiter.api.function.Executable action) {
        var before = roster.snapshot(); assertThrows(IllegalArgumentException.class, action); assertEquals(before, roster.snapshot());
    }

    @Test void identitiesHaveFullScopeAndNeverBecomeRoomOrDatabaseCharacters() throws Exception {
        try (var roster = roster()) {
            var other = new ModernNpcRoster.Template(42, "catalog", 45262, 1649, 280, 10, 100, 750);
            var rows = roster.spawn(SCOPE, List.of(spawn(10, 10), new ModernNpcRoster.Spawn(other, 1, 1, cell(12, 10), 6)), Set.of());
            assertEquals(1, rows.get(0).id().serial()); assertEquals(2, rows.get(1).id().serial());
            assertNotEquals(rows.get(0).template(), rows.get(1).template());
            assertNotEquals(ModernParticipantId.human(7, 1), rows.get(0).id());
            assertNotEquals(ModernParticipantId.bot(7, 1), rows.get(0).id());
            for (var scope : List.of(new ModernNpcRoster.Scope(12, 22, 33), new ModernNpcRoster.Scope(11, 23, 33), new ModernNpcRoster.Scope(11, 22, 34))) {
                assertNotEquals(rows.get(0).id(), new ModernNpcRoster.Id(scope, 1));
                rejectedUnchanged(roster, () -> roster.damage(scope, rows.get(0).id(), 1, 1));
                rejectedUnchanged(roster, () -> roster.damage(SCOPE, new ModernNpcRoster.Id(scope, 1), 1, 1));
            }
        }
    }

    @Test void independentAttackClockBlocksEarlyActionsAndNeverCatchesUpAsABurst() throws Exception {
        try (var roster = roster()) {
            var npc = one(roster); var target = target(7, 0, 11, 10); var players = Set.of(target.cell());
            var early = roster.pursue(SCOPE, npc.id(), 1, List.of(target), players, 4096, () -> false);
            assertEquals(ModernNpcRoster.PlanKind.IN_RANGE, early.kind()); rejectedUnchanged(roster, () -> roster.attack(SCOPE, early, target, players));
            roster.advance(SCOPE, 100); var first = roster.pursue(SCOPE, npc.id(), 1, List.of(target), players, 4096, () -> false);
            var attacked = roster.attack(SCOPE, first, target, players);
            assertEquals(ModernNpcRoster.Action.ATTACK, attacked.action()); assertEquals(2, attacked.heading());
            assertEquals(850, attacked.nextAttackMillis()); assertEquals(100, attacked.nextMoveMillis()); assertEquals(280, attacked.hp());
            rejectedUnchanged(roster, () -> roster.attack(SCOPE, first, target, players));
            roster.advance(SCOPE, 849); var waiting = roster.pursue(SCOPE, npc.id(), 1, List.of(target), players, 4096, () -> false);
            assertEquals(ModernNpcRoster.PlanKind.IN_RANGE, waiting.kind()); rejectedUnchanged(roster, () -> roster.attack(SCOPE, waiting, target, players));
            var move = plan(roster, attacked, 6); rejectedUnchanged(roster, () -> roster.move(SCOPE, move, players));
            var remote = target(7, 0, 20, 20);
            assertEquals(ModernNpcRoster.PlanKind.WAIT, roster.pursue(SCOPE, npc.id(), 1, List.of(remote), Set.of(remote.cell()), 4096, () -> false).kind());
            roster.advance(SCOPE, 850); var next = roster.pursue(SCOPE, npc.id(), 1, List.of(target), players, 4096, () -> false);
            assertEquals(1600, roster.attack(SCOPE, next, target, players).nextAttackMillis());
            roster.advance(SCOPE, 5000); var delayed = roster.pursue(SCOPE, npc.id(), 1, List.of(target), players, 4096, () -> false);
            assertEquals(5750, roster.attack(SCOPE, delayed, target, players).nextAttackMillis());
            var noBurst = roster.pursue(SCOPE, npc.id(), 1, List.of(target), players, 4096, () -> false);
            rejectedUnchanged(roster, () -> roster.attack(SCOPE, noBurst, target, players));
        }
    }

    @Test void attackRevalidatesFullTargetLaneLifeDistanceAndCurrentDiagonalOccupancy() throws Exception {
        try (var roster = roster()) {
            var npc = one(roster); var target = target(7, 0, 11, 11); var players = Set.of(target.cell()); roster.advance(SCOPE, 100);
            var proposal = roster.pursue(SCOPE, npc.id(), 1, List.of(target), players, 4096, () -> false);
            for (var invalid : List.of(target(42, 0, 11, 11), target(7, 1, 11, 11), target(7, 0, 12, 11),
                    new ModernNpcRoster.Target(target.participant(), 0, target.cell(), false),
                    new ModernNpcRoster.Target(ModernParticipantId.bot(7, 3333), 0, target.cell(), true))) {
                rejectedUnchanged(roster, () -> roster.attack(SCOPE, proposal, invalid, Set.of(invalid.cell())));
            }
            rejectedUnchanged(roster, () -> roster.attack(SCOPE, proposal, target, Set.of()));
            rejectedUnchanged(roster, () -> roster.attack(SCOPE, proposal, target, Set.of(target.cell(), cell(11, 10), cell(10, 11))));
            var attacked = roster.attack(SCOPE, proposal, target, Set.of(target.cell(), cell(11, 10)));
            assertEquals(3, attacked.heading()); assertEquals(850, attacked.nextAttackMillis());
        }
    }

    @Test void deathClearsBothClocksAndRevivalCannotReuseTheOldAttackOrSkipRecovery() throws Exception {
        try (var roster = roster()) {
            var npc = one(roster); var target = target(7, 0, 11, 10); var players = Set.of(target.cell()); roster.advance(SCOPE, 100);
            var proposal = roster.pursue(SCOPE, npc.id(), 1, List.of(target), players, 4096, () -> false);
            roster.attack(SCOPE, proposal, target, players);
            var dead = roster.damage(SCOPE, npc.id(), 1, Long.MAX_VALUE); assertEquals(0, dead.nextMoveMillis()); assertEquals(0, dead.nextAttackMillis());
            var revived = roster.resurrect(SCOPE, npc.id(), 1, 280, 10, players);
            assertEquals(2, revived.life()); assertEquals(200, revived.nextMoveMillis()); assertEquals(850, revived.nextAttackMillis());
            rejectedUnchanged(roster, () -> roster.attack(SCOPE, proposal, target, players));
            roster.advance(SCOPE, 200); var early = roster.pursue(SCOPE, npc.id(), 2, List.of(target), players, 4096, () -> false);
            rejectedUnchanged(roster, () -> roster.attack(SCOPE, early, target, players));
            roster.advance(SCOPE, 850); var ready = roster.pursue(SCOPE, npc.id(), 2, List.of(target), players, 4096, () -> false);
            assertEquals(1600, roster.attack(SCOPE, ready, target, players).nextAttackMillis());
        }
    }

    @Test void invalidTemplateScopeCapacityAndUnqualifiedGraphicsAreRejected() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> new ModernNpcRoster.Scope(0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new ModernNpcRoster.Scope(1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new ModernNpcRoster.Scope(1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new ModernNpcRoster.Id(SCOPE, 0));
        assertThrows(IllegalArgumentException.class, () -> new ModernNpcRoster.Template(0, "catalog", 45262, 1649, 1, 0, 100, 750));
        assertThrows(IllegalArgumentException.class, () -> new ModernNpcRoster.Template(7, "catalog", 0, 1649, 1, 0, 100, 750));
        assertThrows(IllegalArgumentException.class, () -> new ModernNpcRoster.Template(7, "catalog", 45262, 10001, 1, 0, 100, 750));
        assertThrows(IllegalArgumentException.class, () -> new ModernNpcRoster.Template(7, "catalog", 45262, 1649, 0, 0, 100, 750));
        assertThrows(IllegalArgumentException.class, () -> new ModernNpcRoster.Template(7, "catalog", 45262, 1649, 1, -1, 100, 750));
        assertThrows(IllegalArgumentException.class, () -> new ModernNpcRoster.Template(7, "catalog", 45262, 1649, 1, 0, 0, 750));
        assertThrows(IllegalArgumentException.class, () -> new ModernNpcRoster.Template(7, "catalog", 45262, 1649, 1, 0, 60001, 750));
        assertThrows(IllegalArgumentException.class, () -> new ModernNpcRoster.Template(7, "invalid.schema", 45262, 1649, 1, 0, 100, 750));
        assertThrows(IllegalArgumentException.class, () -> new ModernNpcRoster.Template(7, "catalog", 45262, 1649, 1, 0, 100, 0));
        assertThrows(IllegalArgumentException.class, () -> new ModernNpcRoster.Template(7, "catalog", 45262, 1649, 1, 0, 100, 60001));
        var map = map();
        assertThrows(IllegalArgumentException.class, () -> roster(map, 0));
        assertThrows(IllegalArgumentException.class, () -> roster(map, 129));
        assertThrows(IllegalArgumentException.class, () -> new ModernNpcRoster(SCOPE, map, List.of(cell(1, 1), cell(1, 1)), 2, 3000));
        assertThrows(IllegalArgumentException.class, () -> new ModernNpcRoster(SCOPE, map, List.of(cell(1, 1), cell(99, 1)), 2, 3000));
        assertThrows(IllegalArgumentException.class, () -> new ModernNpcRoster(SCOPE, map, List.of(cell(1, 1), cell(2, 1)), 2, 0));
    }

    @Test void batchSpawnFailureIsAtomicAndDoesNotConsumeSerialOrRevision() throws Exception {
        try (var roster = roster(map(), 2)) {
            rejectedUnchanged(roster, () -> roster.spawn(SCOPE, List.of(), Set.of()));
            rejectedUnchanged(roster, () -> roster.spawn(SCOPE, List.of(spawn(10, 10), spawn(10, 10)), Set.of()));
            rejectedUnchanged(roster, () -> roster.spawn(SCOPE, List.of(spawn(10, 10), spawn(99, 10)), Set.of()));
            rejectedUnchanged(roster, () -> roster.spawn(SCOPE, List.of(spawn(10, 10), spawn(11, 10)), Set.of(cell(11, 10))));
            rejectedUnchanged(roster, () -> roster.spawn(SCOPE, List.of(spawn(10, 10), spawn(11, 10), spawn(12, 10)), Set.of()));
            var first = one(roster); assertEquals(1, first.id().serial()); assertEquals(2, roster.snapshot().revision());
            rejectedUnchanged(roster, () -> roster.spawn(SCOPE, List.of(spawn(10, 10)), Set.of()));
            var second = roster.spawn(SCOPE, List.of(spawn(11, 10)), Set.of()).get(0);
            assertEquals(2, second.id().serial()); assertEquals(3, roster.snapshot().revision());
        }
    }

    @Test void all128NpcSpawnsUseTheSameLaneAnchorWithoutRelaxing100PlayerBound() throws Exception {
        try (var roster = roster()) {
            var batch = new ArrayList<ModernNpcRoster.Spawn>();
            for (int i = 0; i < 128; i++) batch.add(spawn(5 + i % 16, 5 + i / 16));
            assertEquals(128, roster.spawn(SCOPE, batch, Set.of()).size());
            assertEquals(128, roster.snapshot().actors().stream().map(ModernNpcRoster.Actor::cell).distinct().count());
            rejectedUnchanged(roster, () -> roster.spawn(SCOPE, List.of(spawn(40, 40)), Set.of()));
            assertEquals(100, ModernWorldMap.MAX_SPAWNS);
        }
    }

    @Test void everyLaneIsBoundToItsExplicitReachableAnchorIncludingSingleNpcBatches() throws Exception {
        var bytes = ModernWorldMapTest.openSectorBytes();
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) ModernWorldMapTest.setAttributes(bytes, x, y, (short) 1, (short) 1);
        ModernWorldMapTest.setAttributes(bytes, 10, 10, (short) 1, (short) 0);
        ModernWorldMapTest.setAttributes(bytes, 20, 20, (short) 1, (short) 0);
        var map = ModernWorldMap.fromSectorBytes(554, Map.of(KEY, bytes), () -> false);
        try (var roster = new ModernNpcRoster(SCOPE, map, List.of(cell(10, 10), cell(20, 20)), 4, 3000)) {
            rejectedUnchanged(roster, () -> roster.spawn(SCOPE, List.of(spawn(20, 20)), Set.of()));
            rejectedUnchanged(roster, () -> roster.spawn(SCOPE, List.of(spawn(10, 10), spawn(20, 20)), Set.of()));
            var rows = roster.spawn(SCOPE, List.of(spawn(11, 10), new ModernNpcRoster.Spawn(template(), 1, 1, cell(21, 20), 0)), Set.of());
            assertEquals(2, rows.size());
        }
    }

    @Test void fullWidthHpMpNeverNarrowOverflowOrWrap() throws Exception {
        try (var roster = roster()) {
            var high = new ModernNpcRoster.Template(7, "catalog", 45262, 1649, Long.MAX_VALUE, Long.MAX_VALUE, 100, 750);
            var npc = roster.spawn(SCOPE, List.of(new ModernNpcRoster.Spawn(high, 0, 1, cell(10, 10), 0)), Set.of()).get(0);
            assertEquals(1, roster.damage(SCOPE, npc.id(), 1, Long.MAX_VALUE - 1).hp());
            assertEquals(Long.MAX_VALUE, roster.heal(SCOPE, npc.id(), 1, Long.MAX_VALUE).hp());
            assertEquals(0, roster.drainMana(SCOPE, npc.id(), 1, Long.MAX_VALUE).mp());
            var dead = roster.damage(SCOPE, npc.id(), 1, Long.MAX_VALUE);
            assertEquals(0, dead.hp()); assertFalse(dead.alive()); assertEquals(ModernNpcRoster.Action.DEATH, dead.action());
            rejectedUnchanged(roster, () -> roster.heal(SCOPE, npc.id(), 1, Long.MAX_VALUE));
            var revived = roster.resurrect(SCOPE, npc.id(), 1, Long.MAX_VALUE, Long.MAX_VALUE, Set.of());
            assertEquals(2, revived.life()); assertEquals(Long.MAX_VALUE, revived.hp()); assertEquals(Long.MAX_VALUE, revived.mp());
            rejectedUnchanged(roster, () -> roster.damage(SCOPE, npc.id(), 1, 1));
            rejectedUnchanged(roster, () -> roster.drainMana(SCOPE, npc.id(), 1, 1));
            assertEquals(Long.MAX_VALUE - 1, roster.damage(SCOPE, npc.id(), 2, 1).hp());
        }
    }

    @Test void zeroAndCappedVitalsAreNoOpsAndInvalidAmountsLeaveSnapshotUnchanged() throws Exception {
        try (var roster = roster()) {
            var npc = one(roster); var before = roster.snapshot();
            roster.damage(SCOPE, npc.id(), 1, 0); roster.heal(SCOPE, npc.id(), 1, Long.MAX_VALUE); roster.drainMana(SCOPE, npc.id(), 1, 0);
            assertEquals(before, roster.snapshot());
            rejectedUnchanged(roster, () -> roster.damage(SCOPE, npc.id(), 1, -1));
            rejectedUnchanged(roster, () -> roster.heal(SCOPE, npc.id(), 1, -1));
            rejectedUnchanged(roster, () -> roster.drainMana(SCOPE, npc.id(), 1, -1));
            roster.drainMana(SCOPE, npc.id(), 1, Long.MAX_VALUE); before = roster.snapshot();
            roster.drainMana(SCOPE, npc.id(), 1, 1); assertEquals(before, roster.snapshot());
        }
    }

    @Test void corpsesConsumeResidencyUntilExactExpiryAndSerialsAreNeverReused() throws Exception {
        try (var roster = roster(map(), 1)) {
            var npc = one(roster); roster.advance(SCOPE, 500);
            var dead = roster.damage(SCOPE, npc.id(), 1, 280); assertEquals(3500, dead.corpseUntilMillis());
            rejectedUnchanged(roster, () -> roster.spawn(SCOPE, List.of(spawn(11, 10)), Set.of()));
            roster.advance(SCOPE, 3499); assertEquals(1, roster.snapshot().actors().size());
            long rev = roster.snapshot().revision(); roster.advance(SCOPE, 3500);
            assertTrue(roster.snapshot().actors().isEmpty()); assertEquals(rev + 1, roster.snapshot().revision());
            rejectedUnchanged(roster, () -> roster.resurrect(SCOPE, npc.id(), 1, 280, 10, Set.of()));
            var second = one(roster); assertEquals(2, second.id().serial());
            rejectedUnchanged(roster, () -> roster.damage(SCOPE, npc.id(), 1, 1));
        }
    }

    @Test void resurrectionRetainsOldStageAndCannotOverlapLivingNpcOrPlayer() throws Exception {
        try (var roster = roster()) {
            var npc = one(roster); roster.damage(SCOPE, npc.id(), 1, 280);
            rejectedUnchanged(roster, () -> roster.resurrect(SCOPE, npc.id(), 1, 100, 10, Set.of(cell(10, 10))));
            var nextStage = new ModernNpcRoster.Spawn(template(), 0, 2, cell(10, 10), 0);
            var current = roster.spawn(SCOPE, List.of(nextStage), Set.of()).get(0);
            assertEquals(2, roster.snapshot().actors().size(), "old corpse is not silently removed at a stage boundary");
            rejectedUnchanged(roster, () -> roster.resurrect(SCOPE, npc.id(), 1, 100, 10, Set.of()));
            roster.damage(SCOPE, current.id(), 1, 280);
            var revived = roster.resurrect(SCOPE, npc.id(), 1, 100, 10, Set.of());
            assertEquals(1, revived.stage()); assertEquals(2, revived.life());
            rejectedUnchanged(roster, () -> roster.resurrect(SCOPE, current.id(), 1, 100, 10, Set.of()));
        }
    }

    @Test void resurrectionAtLastLiveMillisecondCancelsOldCorpseDeadlineButNotLifeFence() throws Exception {
        try (var roster = roster()) {
            var npc = one(roster); roster.damage(SCOPE, npc.id(), 1, 280); roster.advance(SCOPE, 2999);
            var revived = roster.resurrect(SCOPE, npc.id(), 1, 1, 0, Set.of());
            assertEquals(0, revived.corpseUntilMillis()); roster.advance(SCOPE, 3000);
            assertEquals(1, roster.snapshot().actors().size()); assertTrue(roster.snapshot().actors().get(0).alive());
            assertEquals(6000, roster.damage(SCOPE, npc.id(), 2, 1).corpseUntilMillis());
        }
    }

    @Test void ownerClockIsMonotonicBoundedAndOnlyExplicitAdvanceRetiresCorpses() throws Exception {
        try (var roster = roster()) {
            var npc = one(roster); roster.advance(SCOPE, 100); var before = roster.snapshot();
            roster.advance(SCOPE, 100); assertEquals(before, roster.snapshot());
            rejectedUnchanged(roster, () -> roster.advance(SCOPE, 99));
            rejectedUnchanged(roster, () -> roster.advance(SCOPE, ModernNpcRoster.MAX_ELAPSED_MILLIS + 1));
            roster.advance(SCOPE, ModernNpcRoster.MAX_ELAPSED_MILLIS);
            var dead = roster.damage(SCOPE, npc.id(), 1, 280);
            assertEquals(ModernNpcRoster.MAX_ELAPSED_MILLIS + 3000, dead.corpseUntilMillis());
        }
    }

    @Test void movementRequiresCurrentRevisionLifeIntervalAndOccupancyWithoutCatchupBurst() throws Exception {
        try (var roster = roster()) {
            var npc = one(roster);
            rejectedUnchanged(roster, () -> roster.move(SCOPE, plan(roster, npc, 2), Set.of()));
            roster.advance(SCOPE, 100); var old = plan(roster, npc, 2);
            var moved = roster.move(SCOPE, old, Set.of()); assertEquals(cell(11, 10), moved.cell()); assertEquals(200, moved.nextMoveMillis());
            rejectedUnchanged(roster, () -> roster.move(SCOPE, old, Set.of()));
            rejectedUnchanged(roster, () -> roster.move(SCOPE, plan(roster, moved, 2), Set.of()));
            roster.advance(SCOPE, 5000); var delayed = roster.move(SCOPE, plan(roster, moved, 2), Set.of());
            assertEquals(cell(12, 10), delayed.cell()); assertEquals(5100, delayed.nextMoveMillis());
            rejectedUnchanged(roster, () -> roster.move(SCOPE, plan(roster, delayed, 2), Set.of()));
        }
    }

    @Test void hpOrOwnerTickInvalidatesQueuedAiMoveAndCurrentOccupancyIsRechecked() throws Exception {
        try (var roster = roster()) {
            var npc = one(roster); roster.advance(SCOPE, 100); var proposal = plan(roster, npc, 2);
            roster.damage(SCOPE, npc.id(), 1, 1);
            rejectedUnchanged(roster, () -> roster.move(SCOPE, proposal, Set.of()));
            var current = plan(roster, npc, 2);
            rejectedUnchanged(roster, () -> roster.move(SCOPE, current, Set.of(cell(11, 10))));
            roster.advance(SCOPE, 101);
            rejectedUnchanged(roster, () -> roster.move(SCOPE, current, Set.of()));
        }
    }

    @Test void diagonalMovementNeedsOneCompleteStaticAndDynamicallyFreeCornerRoute() throws Exception {
        try (var roster = roster()) {
            var npc = one(roster); roster.advance(SCOPE, 100);
            rejectedUnchanged(roster, () -> roster.move(SCOPE, plan(roster, npc, 1), Set.of(cell(10, 9), cell(11, 10))));
            assertEquals(cell(11, 9), roster.move(SCOPE, plan(roster, npc, 1), Set.of(cell(10, 9))).cell());
        }
        var bytes = ModernWorldMapTest.openSectorBytes();
        // Shut the east-corner -> NE edge. Blocking the north corner must not
        // borrow that closed static route just because the other corner is free.
        ModernWorldMapTest.setAttributes(bytes, 11, 10, (short) 1, (short) 0);
        var map = ModernWorldMap.fromSectorBytes(554, Map.of(KEY, bytes), () -> false);
        try (var roster = roster(map, 8)) {
            var npc = one(roster); roster.advance(SCOPE, 100);
            rejectedUnchanged(roster, () -> roster.move(SCOPE, plan(roster, npc, 1), Set.of(cell(10, 9))));
        }
    }

    @Test void inRangeProposalUsesSourceQualifiedTargetsWithoutCommittingDamageOrAttacks() throws Exception {
        try (var roster = roster()) {
            var npc = one(roster); roster.advance(SCOPE, 100);
            var sun = target(7, 0, 10, 9); var moon = target(42, 0, 11, 10);
            var before = roster.snapshot();
            var p = roster.pursue(SCOPE, npc.id(), 1, List.of(moon, sun), Set.of(sun.cell(), moon.cell()), 100, () -> false);
            assertEquals(ModernNpcRoster.PlanKind.IN_RANGE, p.kind()); assertEquals(sun.participant(), p.target());
            assertEquals(-1, p.heading()); assertEquals(before, roster.snapshot());
            var q = roster.pursue(SCOPE, npc.id(), 1, List.of(moon), Set.of(moon.cell()), 100, () -> false);
            assertEquals(moon.participant(), q.target()); assertNotEquals(p.target(), q.target());
            rejectedUnchanged(roster, () -> roster.move(SCOPE, p, Set.of(sun.cell(), moon.cell())));
        }
    }

    @Test void inRangeProposalIsNotDelayedByTheIndependentMovementClock() throws Exception {
        try (var roster = roster()) {
            var npc = one(roster); var target = target(7, 0, 11, 10); var before = roster.snapshot();
            var p = roster.pursue(SCOPE, npc.id(), 1, List.of(target), Set.of(target.cell()), 1, () -> false);
            assertEquals(ModernNpcRoster.PlanKind.IN_RANGE, p.kind()); assertEquals(before, roster.snapshot());
            assertEquals(0, roster.snapshot().elapsedMillis()); assertEquals(100, npc.nextMoveMillis());
        }
    }

    @Test void maximumParticipantTargetSetAndNpcResidencyRemainBoundedAndDoNotPolluteEachOther() throws Exception {
        try (var roster = roster()) {
            var batch = new ArrayList<ModernNpcRoster.Spawn>();
            batch.add(spawn(10, 10));
            for (int i = 0; i < 127; i++) batch.add(spawn(30 + i % 16, 30 + i / 16));
            var npcs = roster.spawn(SCOPE, batch, Set.of());
            var targets = new ArrayList<ModernNpcRoster.Target>(); var occupied = new HashSet<ModernWorldMap.Cell>();
            for (int i = 0; i < 100; i++) {
                var target = new ModernNpcRoster.Target(ModernParticipantId.human(i % 2 == 0 ? 7 : 42, 3333 + i / 2),
                        0, cell(45 + i % 10, 5 + i / 10), true);
                targets.add(target); occupied.add(target.cell());
            }
            roster.advance(SCOPE, 100); var before = roster.snapshot(); var npc = npcs.get(0);
            var result = roster.pursue(SCOPE, npc.id(), 1, targets, occupied, 4096, () -> false);
            assertEquals(ModernNpcRoster.PlanKind.MOVE, result.kind()); assertTrue(result.visited() <= 4096);
            assertEquals(before, roster.snapshot()); assertEquals(128, before.actors().size()); assertEquals(100, targets.size());
        }
    }

    @Test void pursuitSkipsDeadAndOtherLaneTargetsAndWaitsForItsOwnMovementClock() throws Exception {
        try (var roster = roster()) {
            var npc = one(roster); var live = target(7, 0, 20, 10);
            var p = roster.pursue(SCOPE, npc.id(), 1, List.of(live), Set.of(live.cell()), 100, () -> false);
            assertEquals(ModernNpcRoster.PlanKind.WAIT, p.kind());
            roster.advance(SCOPE, 100);
            var dead = new ModernNpcRoster.Target(ModernParticipantId.human(42, 3333), 0, cell(11, 10), false);
            var other = target(9, 1, 10, 9);
            p = roster.pursue(SCOPE, npc.id(), 1, List.of(dead, other), Set.of(other.cell()), 100, () -> false);
            assertEquals(ModernNpcRoster.PlanKind.NONE, p.kind());
            p = roster.pursue(SCOPE, npc.id(), 1, List.of(dead, other, live), Set.of(other.cell(), live.cell()), 4096, () -> false);
            assertEquals(ModernNpcRoster.PlanKind.MOVE, p.kind()); assertEquals(live.participant(), p.target());
        }
    }

    @Test void searchBudgetCancellationAndImpossiblePathsNeverGuessAnUnsafeStep() throws Exception {
        try (var roster = roster()) {
            var npc = one(roster); roster.advance(SCOPE, 100); var target = target(7, 0, 50, 50);
            var before = roster.snapshot();
            var limited = roster.pursue(SCOPE, npc.id(), 1, List.of(target), Set.of(target.cell()), 1, () -> false);
            assertEquals(ModernNpcRoster.PlanKind.BUDGET, limited.kind()); assertEquals(1, limited.visited()); assertEquals(-1, limited.heading());
            var calls = new AtomicInteger();
            var stopped = roster.pursue(SCOPE, npc.id(), 1, List.of(target), Set.of(target.cell()), 4096, () -> calls.incrementAndGet() >= 5);
            assertEquals(ModernNpcRoster.PlanKind.CANCELLED, stopped.kind()); assertTrue(stopped.visited() <= 4096);
            var walls = new HashSet<ModernWorldMap.Cell>(); walls.add(target.cell());
            for (int y = 9; y <= 11; y++) for (int x = 9; x <= 11; x++) if (x != 10 || y != 10) walls.add(cell(x, y));
            var none = roster.pursue(SCOPE, npc.id(), 1, List.of(target), walls, 4096, () -> false);
            assertEquals(ModernNpcRoster.PlanKind.NONE, none.kind()); assertEquals(1, none.visited());
            assertEquals(before, roster.snapshot());
        }
    }

    @Test void pursuitRejectsDuplicateOrUnboundTargetsAndInvalidPlayerSnapshots() throws Exception {
        try (var roster = roster()) {
            var npc = one(roster); roster.advance(SCOPE, 100); var target = target(7, 0, 20, 10);
            rejectedUnchanged(roster, () -> roster.pursue(SCOPE, npc.id(), 1, List.of(target), Set.of(), 100, () -> false));
            rejectedUnchanged(roster, () -> roster.pursue(SCOPE, npc.id(), 1, List.of(target, target), Set.of(target.cell()), 100, () -> false));
            var sameCell = target(42, 0, 20, 10);
            rejectedUnchanged(roster, () -> roster.pursue(SCOPE, npc.id(), 1, List.of(target, sameCell), Set.of(target.cell()), 100, () -> false));
            rejectedUnchanged(roster, () -> roster.pursue(SCOPE, npc.id(), 1, List.of(target), Set.of(target.cell()), 0, () -> false));
            rejectedUnchanged(roster, () -> roster.pursue(SCOPE, npc.id(), 1, List.of(target), Set.of(target.cell()), 4097, () -> false));
            rejectedUnchanged(roster, () -> roster.pursue(SCOPE, npc.id(), 1, List.of(target), Set.of(target.cell(), npc.cell()), 100, () -> false));
            var cells = new HashSet<ModernWorldMap.Cell>();
            for (int i = 0; i < 101; i++) cells.add(cell(i % 50, i / 50));
            rejectedUnchanged(roster, () -> roster.spawn(SCOPE, List.of(spawn(40, 40)), cells));
        }
    }

    @Test void pursuitWalksAroundOccupiedWallsRatherThanTeleportingAcrossThem() throws Exception {
        try (var roster = roster()) {
            var npc = one(roster); var target = target(7, 0, 14, 10);
            var occupied = Set.of(target.cell(), cell(11, 9), cell(11, 10), cell(11, 11));
            int moves = 0;
            for (int tick = 1; tick <= 30; tick++) {
                roster.advance(SCOPE, tick * 100);
                var p = roster.pursue(SCOPE, npc.id(), npc.life(), List.of(target), occupied, 4096, () -> false);
                if (p.kind() == ModernNpcRoster.PlanKind.IN_RANGE) break;
                assertEquals(ModernNpcRoster.PlanKind.MOVE, p.kind());
                var prior = npc; npc = roster.move(SCOPE, p, occupied); moves++;
                assertTrue(map().canStep(prior.cell(), npc.cell())); assertFalse(occupied.contains(npc.cell()));
            }
            assertTrue(moves >= 4); assertTrue(moves < 30);
            assertTrue(map().canStep(npc.cell(), target.cell())); assertEquals(280, npc.hp());
        }
    }

    @Test void frozenSnapshotsAndSpawnInputsCannotMutateTheOwnerAndCloseIsFinal() throws Exception {
        var roster = roster(); var batch = new ArrayList<>(List.of(spawn(10, 10)));
        var rows = roster.spawn(SCOPE, batch, Set.of()); batch.clear(); var before = roster.snapshot();
        assertThrows(UnsupportedOperationException.class, () -> rows.clear());
        assertThrows(UnsupportedOperationException.class, () -> before.actors().clear());
        roster.damage(SCOPE, rows.get(0).id(), 1, 1); assertEquals(280, before.actors().get(0).hp());
        roster.close(); var terminal = roster.snapshot(); assertTrue(terminal.closed()); assertTrue(terminal.actors().isEmpty());
        roster.close(); assertEquals(terminal, roster.snapshot());
        rejectedUnchanged(roster, () -> roster.spawn(SCOPE, List.of(spawn(10, 10)), Set.of()));
        rejectedUnchanged(roster, () -> roster.advance(SCOPE, 100));
        rejectedUnchanged(roster, () -> roster.damage(SCOPE, rows.get(0).id(), 1, 1));
    }

    @Test void malformedSnapshotsCannotRebindScopeRevisionOrRetainExpiredCorpses() throws Exception {
        try (var roster = roster()) {
            var actor = one(roster); var s = roster.snapshot();
            assertThrows(IllegalArgumentException.class, () -> new ModernNpcRoster.Snapshot(new ModernNpcRoster.Scope(12, 22, 33),
                    s.manifest(), s.revision(), 0, 1, false, s.actors()));
            assertThrows(IllegalArgumentException.class, () -> new ModernNpcRoster.Snapshot(SCOPE, s.manifest(), 1, 0, 1, false, s.actors()));
            assertThrows(IllegalArgumentException.class, () -> new ModernNpcRoster.Snapshot(SCOPE, s.manifest(), s.revision(), 0, 0, false, s.actors()));
            assertThrows(IllegalArgumentException.class, () -> new ModernNpcRoster.Snapshot(SCOPE, s.manifest(), s.revision(), 0, 1, true, s.actors()));
            assertThrows(IllegalArgumentException.class, () -> new ModernNpcRoster.Snapshot(SCOPE, s.manifest(), s.revision(), 0, 1, false, List.of(actor, actor)));
            var corpse = roster.damage(SCOPE, actor.id(), 1, 280); var d = roster.snapshot();
            assertThrows(IllegalArgumentException.class, () -> new ModernNpcRoster.Snapshot(SCOPE, d.manifest(), d.revision(),
                    corpse.corpseUntilMillis(), 1, false, d.actors()));
            assertEquals(d, roster.snapshot());
        }
    }

    @Test void malformedPlansCannotPretendCancellationOrInRangeIsAnApplicableMove() throws Exception {
        try (var roster = roster()) {
            var actor = one(roster); var target = ModernParticipantId.human(7, 3333);
            assertThrows(IllegalArgumentException.class, () -> new ModernNpcRoster.Plan(actor.id(), 0, 1, ModernNpcRoster.PlanKind.MOVE, 2, target, 1));
            assertThrows(IllegalArgumentException.class, () -> new ModernNpcRoster.Plan(actor.id(), 1, 0, ModernNpcRoster.PlanKind.MOVE, 2, target, 1));
            assertThrows(IllegalArgumentException.class, () -> new ModernNpcRoster.Plan(actor.id(), 1, 1, ModernNpcRoster.PlanKind.MOVE, -1, target, 1));
            assertThrows(IllegalArgumentException.class, () -> new ModernNpcRoster.Plan(actor.id(), 1, 1, ModernNpcRoster.PlanKind.MOVE, 2, null, 1));
            assertThrows(IllegalArgumentException.class, () -> new ModernNpcRoster.Plan(actor.id(), 1, 1, ModernNpcRoster.PlanKind.IN_RANGE, 2, target, 1));
            assertThrows(IllegalArgumentException.class, () -> new ModernNpcRoster.Plan(actor.id(), 1, 1, ModernNpcRoster.PlanKind.CANCELLED, -1, target, 1));
            assertThrows(IllegalArgumentException.class, () -> new ModernNpcRoster.Plan(actor.id(), 1, 1, ModernNpcRoster.PlanKind.BUDGET, -1, null, 4097));
        }
    }
}
