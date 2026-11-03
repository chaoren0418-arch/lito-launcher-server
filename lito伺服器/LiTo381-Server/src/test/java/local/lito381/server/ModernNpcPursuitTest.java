package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernNpcRosterTest.*;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

/** Legal service slices (1..64), not the engine-only 4096-call budget. */
class ModernNpcPursuitTest {
    static ModernNpcRoster.Plan finish(ModernNpcRoster roster, ModernNpcRoster.Actor npc,
            List<ModernNpcRoster.Target> targets, Set<ModernWorldMap.Cell> occupied, int budget) {
        for (int i = 0; i <= ModernNpcRoster.MAX_PATH_CELLS / budget + 1; i++) {
            var before = roster.snapshot();
            var result = roster.pursue(SCOPE, npc.id(), npc.life(), targets, occupied, budget, () -> false);
            assertTrue(result.visited() <= budget, "per-call search work");
            assertEquals(before, roster.snapshot(), "planning must not publish actor changes");
            if (result.kind() != ModernNpcRoster.PlanKind.BUDGET) return result;
            roster.advance(SCOPE, before.elapsedMillis() + 1); // A new authority revision must not restart geometry.
        }
        fail("reachable pursuit keeps restarting at the service quota");
        return null;
    }

    @Test void distantStaticTargetCompletesAcrossLegalServiceSlicesAndKeepsShortestFirstStep() throws Exception {
        try (var roster = roster(); var reference = roster()) {
            var npc = one(roster); var other = one(reference); var target = target(7, 0, 50, 50);
            var occupied = Set.of(target.cell());
            roster.advance(SCOPE, 100); reference.advance(SCOPE, 100);
            var complete = reference.pursue(SCOPE, other.id(), 1, List.of(target), occupied, 4096, () -> false);
            assertEquals(ModernNpcRoster.PlanKind.MOVE, complete.kind());
            var first = roster.pursue(SCOPE, npc.id(), 1, List.of(target), occupied, 8, () -> false);
            assertEquals(ModernNpcRoster.PlanKind.BUDGET, first.kind());
            var result = finish(roster, npc, List.of(target), occupied, 8);
            assertEquals(complete.heading(), result.heading()); assertEquals(complete.target(), result.target());
        }
    }

    @Test void completedRouteIsConsumedOneConfirmedStepPerClockWithoutAnotherLongSearch() throws Exception {
        try (var roster = roster()) {
            var npc = one(roster); var target = target(7, 0, 50, 50); var occupied = Set.of(target.cell());
            roster.advance(SCOPE, 100);
            var plan = finish(roster, npc, List.of(target), occupied, 64);
            for (int moves = 0; moves < 39; moves++) {
                assertEquals(ModernNpcRoster.PlanKind.MOVE, plan.kind());
                var before = npc; npc = roster.move(SCOPE, plan, occupied);
                assertTrue(map().canStep(before.cell(), npc.cell())); assertFalse(occupied.contains(npc.cell()));
                assertEquals(moves + 1, npc.walk().sequence());
                var moving = npc;
                assertThrows(IllegalArgumentException.class, () -> roster.move(SCOPE, plan(roster, moving, 4), occupied));
                roster.advance(SCOPE, npc.nextMoveMillis());
                plan = roster.pursue(SCOPE, npc.id(), 1, List.of(target), occupied, 64, () -> false);
            }
            assertEquals(ModernNpcRoster.PlanKind.IN_RANGE, plan.kind());
            assertEquals(target.participant(), plan.target()); assertEquals(280, npc.hp());
        }
    }

    @Test void evenOneNodeQuotaEventuallyFindsAReachableRoute() throws Exception {
        try (var roster = roster()) {
            var npc = one(roster); var target = target(7, 0, 20, 10); roster.advance(SCOPE, 100);
            assertEquals(ModernNpcRoster.PlanKind.MOVE,
                    finish(roster, npc, List.of(target), Set.of(target.cell()), 1).kind());
        }
    }

    @Test void detourAroundOccupiedWallStillFindsARealShortestRoute() throws Exception {
        var target = target(7, 0, 20, 10); var occupied = new HashSet<ModernWorldMap.Cell>();
        occupied.add(target.cell());
        for (int y = 1; y < 35; y++) occupied.add(cell(11, y));
        try (var roster = roster(); var reference = roster()) {
            var npc = one(roster); var other = one(reference);
            roster.advance(SCOPE, 100); reference.advance(SCOPE, 100);
            var expected = reference.pursue(SCOPE, other.id(), 1, List.of(target), occupied, 4096, () -> false);
            var actual = finish(roster, npc, List.of(target), occupied, 64);
            assertEquals(ModernNpcRoster.PlanKind.MOVE, actual.kind()); assertEquals(expected.heading(), actual.heading());
            assertEquals(expected.target(), actual.target());
        }
    }

    @Test void changedTargetCellAndFullSourceIdentityDiscardBothPendingAndCompletedRoutes() throws Exception {
        for (boolean completed : new boolean[]{false, true}) try (var roster = roster(); var reference = roster()) {
            var npc = one(roster); var other = one(reference); roster.advance(SCOPE, 100); reference.advance(SCOPE, 100);
            var old = target(7, 0, 50, 50); var next = target(42, 0, 10, 30);
            if (completed) finish(roster, npc, List.of(old), Set.of(old.cell()), 64);
            else assertEquals(ModernNpcRoster.PlanKind.BUDGET, roster.pursue(SCOPE, npc.id(), 1, List.of(old), Set.of(old.cell()), 1, () -> false).kind());
            var expected = reference.pursue(SCOPE, other.id(), 1, List.of(next), Set.of(next.cell()), 4096, () -> false);
            var actual = finish(roster, npc, List.of(next), Set.of(next.cell()), 64);
            assertEquals(expected.heading(), actual.heading()); assertEquals(next.participant(), actual.target());
            // Same coordinates and objid, different selected database source.
            var third = target(7, 0, 10, 30);
            assertEquals(third.participant(), finish(roster, npc, List.of(third), Set.of(third.cell()), 64).target());
        }
    }

    @Test void changedOccupancyDiscardsPendingAndCompletedRoutesAndCommitRechecksNextCell() throws Exception {
        for (boolean completed : new boolean[]{false, true}) try (var roster = roster()) {
            var npc = one(roster); roster.advance(SCOPE, 100); var target = target(7, 0, 50, 50);
            var occupied = Set.of(target.cell());
            if (completed) finish(roster, npc, List.of(target), occupied, 64);
            else assertEquals(ModernNpcRoster.PlanKind.BUDGET, roster.pursue(SCOPE, npc.id(), 1, List.of(target), occupied, 1, () -> false).kind());
            var walls = new HashSet<>(occupied);
            for (int y = 9; y <= 11; y++) for (int x = 9; x <= 11; x++) if (x != 10 || y != 10) walls.add(cell(x, y));
            var none = roster.pursue(SCOPE, npc.id(), 1, List.of(target), walls, 64, () -> false);
            assertEquals(ModernNpcRoster.PlanKind.NONE, none.kind()); assertEquals(1, none.visited());
            var route = finish(roster, npc, List.of(target), occupied, 64);
            var before = roster.snapshot();
            assertThrows(IllegalArgumentException.class, () -> roster.move(SCOPE, route, walls));
            assertEquals(before, roster.snapshot());
            // Failed occupancy commit does not grant a movement or poison a valid retry.
            assertNotEquals(npc.cell(), roster.move(SCOPE, route, occupied).cell());
        }
    }

    @Test void cancellationDropsPrivateWorkAndNeverPublishesOrReusesAMove() throws Exception {
        try (var roster = roster()) {
            var npc = one(roster); roster.advance(SCOPE, 100); var target = target(7, 0, 50, 50);
            var occupied = Set.of(target.cell()); finish(roster, npc, List.of(target), occupied, 64);
            var before = roster.snapshot();
            assertEquals(ModernNpcRoster.PlanKind.CANCELLED, roster.pursue(SCOPE, npc.id(), 1, List.of(target), occupied, 64, () -> true).kind());
            assertEquals(before, roster.snapshot());
            assertEquals(ModernNpcRoster.PlanKind.BUDGET, roster.pursue(SCOPE, npc.id(), 1, List.of(target), occupied, 1, () -> false).kind());
            var calls = new java.util.concurrent.atomic.AtomicInteger();
            assertEquals(ModernNpcRoster.PlanKind.CANCELLED, roster.pursue(SCOPE, npc.id(), 1, List.of(target), occupied, 64,
                    () -> calls.incrementAndGet() >= 5).kind());
            assertTrue(work(roster).isEmpty()); assertEquals(before, roster.snapshot());
        }
    }

    @Test void deadOrOtherLaneTargetsCannotKeepACompletedRouteAlive() throws Exception {
        try (var roster = roster()) {
            var npc = one(roster); roster.advance(SCOPE, 100); var target = target(7, 0, 50, 50);
            var occupied = Set.of(target.cell()); finish(roster, npc, List.of(target), occupied, 64);
            var dead = new ModernNpcRoster.Target(target.participant(), 0, target.cell(), false);
            assertEquals(ModernNpcRoster.PlanKind.NONE, roster.pursue(SCOPE, npc.id(), 1, List.of(dead), Set.of(), 64, () -> false).kind());
            assertTrue(work(roster).isEmpty());
            var wrongLane = target(42, 1, 50, 50);
            assertEquals(ModernNpcRoster.PlanKind.NONE, roster.pursue(SCOPE, npc.id(), 1, List.of(wrongLane), occupied, 64, () -> false).kind());
        }
    }

    @Test void deathResurrectionOldPlansAndCloseCannotReuseNavigationWork() throws Exception {
        var roster = roster();
        try {
            var npc = one(roster); roster.advance(SCOPE, 100); var target = target(7, 0, 50, 50);
            var occupied = Set.of(target.cell()); var old = finish(roster, npc, List.of(target), occupied, 64);
            roster.damage(SCOPE, npc.id(), 1, Long.MAX_VALUE); assertTrue(work(roster).isEmpty());
            var revived = roster.resurrect(SCOPE, npc.id(), 1, 280, 10, occupied);
            roster.advance(SCOPE, revived.nextAttackMillis());
            assertThrows(IllegalArgumentException.class, () -> roster.move(SCOPE, old, occupied));
            assertThrows(IllegalArgumentException.class, () -> roster.pursue(SCOPE, npc.id(), 1, List.of(target), occupied, 64, () -> false));
            assertEquals(ModernNpcRoster.PlanKind.BUDGET, roster.pursue(SCOPE, npc.id(), 2, List.of(target), occupied, 1, () -> false).kind());
            var next = finish(roster, revived, List.of(target), occupied, 64);
            assertEquals(2, next.life()); assertEquals(1, work(roster).size());
            roster.close(); assertTrue(work(roster).isEmpty());
            assertThrows(IllegalArgumentException.class, () -> roster.move(SCOPE, next, occupied));
        } finally { roster.close(); }
    }

    @Test void cachesBelongToTheirExactNpcAndCurrentRevisionStillGuardsCommit() throws Exception {
        try (var roster = roster()) {
            var rows = roster.spawn(SCOPE, List.of(spawn(10, 10), spawn(10, 30)), Set.of());
            var first = rows.get(0); var second = rows.get(1); var target = target(7, 0, 50, 50); var occupied = Set.of(target.cell());
            roster.advance(SCOPE, 100);
            var plan = finish(roster, first, List.of(target), occupied, 64);
            assertEquals(ModernNpcRoster.PlanKind.BUDGET, roster.pursue(SCOPE, second.id(), 1, List.of(target), occupied, 1, () -> false).kind());
            var pendingSecond = work(roster).get(second.id());
            assertEquals(2, work(roster).size());
            roster.advance(SCOPE, roster.snapshot().elapsedMillis() + 1);
            assertThrows(IllegalArgumentException.class, () -> roster.move(SCOPE, plan, occupied));
            var fresh = roster.pursue(SCOPE, first.id(), 1, List.of(target), occupied, 64, () -> false);
            assertEquals(0, fresh.visited()); assertEquals(plan.heading(), fresh.heading());
            roster.move(SCOPE, fresh, occupied);
            // This other NPC moved far outside the second search's examined
            // edges. Keep its own work; live revision/commit guards still hold.
            var next = roster.pursue(SCOPE, second.id(), 1, List.of(target), occupied, 1, () -> false);
            assertEquals(ModernNpcRoster.PlanKind.BUDGET, next.kind()); assertEquals(1, next.visited());
            assertSame(pendingSecond, work(roster).get(second.id()));
        }
    }

    @Test void retainedSearchMemoryHasTheExistingHardCapAndNeverGuessesAcrossIt() throws Exception {
        var second = new ModernWorldMap.SectorKey(KEY.x() + 1, KEY.y());
        var map = ModernWorldMap.fromSectorBytes(554, java.util.Map.of(KEY, ModernWorldMapTest.openSectorBytes(),
                second, ModernWorldMapTest.openSectorBytes()), () -> false);
        try (var roster = roster(map, 128)) {
            var npc = one(roster); roster.advance(SCOPE, 100); var target = target(7, 0, 120, 50);
            // Reachable open space exceeds 4096 cells, but an occupied outer
            // ring separates the still-free attack goals from that space.
            var occupied = new HashSet<ModernWorldMap.Cell>(); occupied.add(target.cell());
            for (int x = 118; x <= 122; x++) for (int y = 48; y <= 52; y++)
                if (Math.max(Math.abs(x - 120), Math.abs(y - 50)) == 2) occupied.add(cell(x, y));
            var before = roster.snapshot();
            for (int i = 0; i < 70; i++) {
                var result = roster.pursue(SCOPE, npc.id(), 1, List.of(target), occupied, 64, () -> false);
                assertEquals(ModernNpcRoster.PlanKind.BUDGET, result.kind()); assertTrue(result.visited() <= 64);
            }
            var search = work(roster).get(npc.id());
            var seen = search.getClass().getDeclaredField("seen"); seen.setAccessible(true);
            assertEquals(ModernNpcRoster.MAX_PATH_CELLS, ((java.util.Map<?, ?>) seen.get(search)).size());
            var pending = search.getClass().getDeclaredField("pending"); pending.setAccessible(true);
            assertTrue(((java.util.Set<?>) pending.get(search)).isEmpty(), "do not accept a goal after frontier truncation");
            assertEquals(before, roster.snapshot());
        }
    }

    // Read-only ownership/memory audit; no test inserts or rewrites cached data.
    private static java.util.Map<?, ?> work(ModernNpcRoster roster) throws Exception {
        var field = ModernNpcRoster.class.getDeclaredField("pursuits"); field.setAccessible(true);
        return (java.util.Map<?, ?>) field.get(roster);
    }
}
