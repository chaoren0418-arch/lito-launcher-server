package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernNpcRosterTest.*;
import static local.lito381.server.ModernNpcDynamicPursuitTest.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModernNpcPursuitDependenciesTest {
    @Test void oneNodeSlicesSurviveAContinuouslyMovingDistantBystander() throws Exception {
        chase(1, false, false);
    }

    @Test void sixteenNodeSlicesSurviveAContinuouslyMovingTarget() throws Exception {
        chase(16, true, false);
    }

    @Test void aDetourKeepsItsSearchWorkWhenADistantBystanderMoves() throws Exception {
        chase(16, false, true);
    }

    private static void chase(int budget, boolean movingTarget, boolean detour) throws Exception {
        var map = map();
        try (var roster = roster(map, 8)) {
            var npc = one(roster); int moves = 0, yields = 0, initialDistance = -1; boolean reached = false;
            ModernWorldMap.Cell oldTarget = null, oldOther = null;
            for (int tick = 1; tick <= 160; tick++) {
                var target = target(7, 0, detour ? 20 : 50, detour ? 10 : 50 + (movingTarget ? (tick / 2) % 2 : 0));
                if (target.cell().equals(npc.cell())) target = new ModernNpcRoster.Target(target.participant(), 0, oldTarget, true);
                var other = target(42, 1, detour ? 50 : 2, detour ? 50 + tick % 2 : 2 + tick % 2);
                if (oldTarget != null && !oldTarget.equals(target.cell())) assertTrue(map.canStep(oldTarget, target.cell()));
                if (oldOther != null) assertTrue(map.canStep(oldOther, other.cell()));
                oldTarget = target.cell(); oldOther = other.cell();
                var occupied = new HashSet<>(Set.of(target.cell(), other.cell()));
                if (detour) for (int y = 1; y < 35; y++) occupied.add(cell(11, y));
                if (tick == 1) initialDistance = breadthFirstDistance(map, npc.cell(), List.of(target), occupied);
                roster.advance(SCOPE, tick * 100L); var before = roster.snapshot();
                var plan = roster.pursue(SCOPE, npc.id(), npc.life(), List.of(target, other), occupied, budget, () -> false);
                assertEquals(before, roster.snapshot()); assertTrue(plan.visited() <= budget);
                if (plan.kind() == ModernNpcRoster.PlanKind.BUDGET) { yields++; continue; }
                assertEquals(target.participant(), plan.target());
                if (plan.kind() == ModernNpcRoster.PlanKind.IN_RANGE) { reached = true; break; }
                assertEquals(ModernNpcRoster.PlanKind.MOVE, plan.kind());
                var previous = npc; npc = roster.move(SCOPE, plan, occupied); moves++;
                assertTrue(map.canStep(previous.cell(), npc.cell())); assertFalse(occupied.contains(npc.cell()));
                assertEquals(moves, npc.walk().sequence());
            }
            assertTrue(reached, "pursuit starved: budget=" + budget + " movingTarget=" + movingTarget + " detour=" + detour + " moves=" + moves + " yields=" + yields);
            if (movingTarget) assertTrue(moves >= initialDistance && moves <= initialDistance + 2);
            else assertEquals(initialDistance, moves, "independent exact shortest route for the static target");
            System.out.println("dependent NPC pursuit budget=" + budget + " movingTarget=" + movingTarget + " detour=" + detour + " moves=" + moves + " yields=" + yields);
        }
    }

    @Test void unrelatedOccupancyPreservesBothPendingWorkAndTheCompletedRoute() throws Exception {
        for (boolean completed : new boolean[]{false, true}) try (var roster = roster()) {
            var npc = one(roster); roster.advance(SCOPE, 100); var target = target(7, 0, 50, 50);
            var old = Set.of(target.cell(), cell(2, 2));
            if (completed) ModernNpcPursuitTest.finish(roster, npc, List.of(target), old, 64);
            else assertEquals(ModernNpcRoster.PlanKind.BUDGET, roster.pursue(SCOPE, npc.id(), 1, List.of(target), old, 1, () -> false).kind());
            var work = work(roster, npc); var now = Set.of(target.cell(), cell(2, 3)); var before = roster.snapshot();
            var plan = roster.pursue(SCOPE, npc.id(), 1, List.of(target), now, 1, () -> false);
            assertSame(work, work(roster, npc)); assertEquals(before, roster.snapshot());
            assertEquals(completed ? ModernNpcRoster.PlanKind.MOVE : ModernNpcRoster.PlanKind.BUDGET, plan.kind());
            assertEquals(completed ? 0 : 1, plan.visited());
            follow(roster, npc, List.of(target), now, 1);
        }
    }

    @Test void aNewBlockInExaminedGeometryDiscardsBothPendingAndCompletedProofs() throws Exception {
        for (boolean completed : new boolean[]{false, true}) try (var roster = roster()) {
            var npc = one(roster); roster.advance(SCOPE, 100); var target = target(7, 0, 50, 50);
            var old = Set.of(target.cell());
            if (completed) ModernNpcPursuitTest.finish(roster, npc, List.of(target), old, 64);
            else assertEquals(ModernNpcRoster.PlanKind.BUDGET, roster.pursue(SCOPE, npc.id(), 1, List.of(target), old, 8, () -> false).kind());
            var work = work(roster, npc); var now = Set.of(target.cell(), cell(13, 13));
            assertEquals(ModernNpcRoster.PlanKind.BUDGET, roster.pursue(SCOPE, npc.id(), 1, List.of(target), now, 1, () -> false).kind());
            assertNotSame(work, work(roster, npc)); follow(roster, npc, List.of(target), now, 7);
        }
    }

    @Test void removingAnOffRouteWallMustNotKeepTheLongerCompletedDetour() throws Exception {
        try (var roster = roster()) {
            var npc = one(roster); roster.advance(SCOPE, 100); var target = target(7, 0, 20, 10);
            var occupied = new HashSet<ModernWorldMap.Cell>(); occupied.add(target.cell());
            for (int y = 1; y < 35; y++) occupied.add(cell(11, y));
            var old = ModernNpcPursuitTest.finish(roster, npc, List.of(target), occupied, 64);
            assertNotEquals(2, old.heading()); var work = work(roster, npc);
            occupied.remove(cell(11, 10));
            var next = roster.pursue(SCOPE, npc.id(), 1, List.of(target), occupied, 64, () -> false);
            assertNotSame(work, work(roster, npc)); assertEquals(ModernNpcRoster.PlanKind.MOVE, next.kind());
            assertEquals(2, next.heading()); assertEquals(9, follow(roster, npc, List.of(target), occupied, 64));
        }
    }

    @Test void diagonalSideCellsAreDependenciesEvenWhenNeitherIsOnTheRoute() throws Exception {
        try (var roster = roster()) {
            var npc = one(roster); roster.advance(SCOPE, 100); var target = target(7, 0, 50, 50);
            var old = ModernNpcPursuitTest.finish(roster, npc, List.of(target), Set.of(target.cell()), 64);
            assertEquals(3, old.heading()); var work = work(roster, npc);
            var blocked = Set.of(target.cell(), cell(11, 10), cell(10, 11)); var before = roster.snapshot();
            assertThrows(IllegalArgumentException.class, () -> roster.move(SCOPE, old, blocked)); assertEquals(before, roster.snapshot());
            roster.pursue(SCOPE, npc.id(), 1, List.of(target), blocked, 1, () -> false);
            assertNotSame(work, work(roster, npc)); follow(roster, npc, List.of(target), blocked, 7);
        }
    }

    @Test void aNewGoalInAlreadySettledSpaceIsRequeuedUnderItsCurrentIdentity() throws Exception {
        try (var roster = roster()) {
            var npc = one(roster); roster.advance(SCOPE, 100);
            var far = target(7, 0, 50, 50); var near = target(42, 1, 20, 20); var occupied = Set.of(far.cell(), near.cell());
            assertEquals(ModernNpcRoster.PlanKind.BUDGET, roster.pursue(SCOPE, npc.id(), 1, List.of(far, near), occupied, 16, () -> false).kind());
            var work = work(roster, npc);
            // An authority-owned position exchange keeps occupancy unchanged,
            // but turns a previously settled neighbour into a different goal.
            var target = target(7, 0, 20, 20); var other = target(42, 1, 50, 50);
            var next = roster.pursue(SCOPE, npc.id(), 1, List.of(target, other), occupied, 1, () -> false);
            assertSame(work, work(roster, npc)); assertEquals(ModernNpcRoster.PlanKind.MOVE, next.kind());
            assertEquals(target.participant(), next.target()); assertEquals(1, next.visited());
            assertEquals(9, follow(roster, npc, List.of(target), occupied, 1));
        }
    }

    @Test void cancellationDuringRetargetingDropsAllPrivateWorkWithoutPublication() throws Exception {
        try (var roster = roster()) {
            var npc = one(roster); roster.advance(SCOPE, 100);
            var far = target(7, 0, 50, 50); var occupied = Set.of(far.cell(), cell(20, 20));
            roster.pursue(SCOPE, npc.id(), 1, List.of(far), occupied, 16, () -> false); var before = roster.snapshot();
            var calls = new java.util.concurrent.atomic.AtomicInteger(); var near = target(7, 0, 20, 20);
            var cancelled = roster.pursue(SCOPE, npc.id(), 1, List.of(near), occupied, 1, () -> calls.incrementAndGet() >= 5);
            assertEquals(ModernNpcRoster.PlanKind.CANCELLED, cancelled.kind()); assertNull(work(roster, npc));
            assertEquals(before, roster.snapshot());
            assertEquals(ModernNpcRoster.PlanKind.BUDGET, roster.pursue(SCOPE, npc.id(), 1, List.of(near), occupied, 1, () -> false).kind());
        }
    }

    @Test void twoNpcSearchesAndMovesMakeConcurrentProgressAtOneNodePerCall() throws Exception {
        try (var roster = roster()) {
            var rows = roster.spawn(SCOPE, List.of(spawn(10, 10), spawn(10, 30)), Set.of());
            var target = target(7, 0, 50, 50); var occupied = Set.of(target.cell());
            int[] moves = {0, 0}, firstMove = {0, 0}, arrival = {0, 0}; boolean[] reached = {false, false};
            for (int tick = 1; tick <= 160; tick++) {
                roster.advance(SCOPE, tick * 100L);
                for (int i = 0; i < rows.size(); i++) {
                    var id = rows.get(i).id(); var npc = roster.snapshot().actors().stream().filter(a -> a.id().equals(id)).findFirst().orElseThrow();
                    var before = roster.snapshot(); var plan = roster.pursue(SCOPE, id, 1, List.of(target), occupied, 1, () -> false);
                    assertEquals(before, roster.snapshot()); assertTrue(plan.visited() <= 1); auditFrontier(roster);
                    if (plan.kind() == ModernNpcRoster.PlanKind.BUDGET) continue;
                    if (plan.kind() == ModernNpcRoster.PlanKind.IN_RANGE) { reached[i] = true; if (arrival[i] == 0) arrival[i] = tick; continue; }
                    assertEquals(ModernNpcRoster.PlanKind.MOVE, plan.kind());
                    var next = roster.move(SCOPE, plan, occupied); moves[i]++;
                    if (firstMove[i] == 0) firstMove[i] = tick;
                    assertTrue(map().canStep(npc.cell(), next.cell())); assertEquals(moves[i], next.walk().sequence());
                    assertEquals(2, roster.snapshot().actors().stream().map(ModernNpcRoster.Actor::cell).distinct().count());
                }
                if (reached[0] && reached[1]) break;
            }
            System.out.println("concurrent NPC pursuit quota=1 moves=" + java.util.Arrays.toString(moves)
                    + " firstMoveTicks=" + java.util.Arrays.toString(firstMove) + " arrivalTicks=" + java.util.Arrays.toString(arrival));
            assertTrue(reached[0] && reached[1], java.util.Arrays.toString(moves));
            assertTrue(firstMove[0] < arrival[1] && firstMove[1] < arrival[0], "both must move before either completes its approach");
        }
    }

    @Test void anotherNpcMovingInsideExaminedSpaceStillInvalidatesTheSearch() throws Exception {
        try (var roster = roster()) {
            var rows = roster.spawn(SCOPE, List.of(spawn(10, 11), spawn(10, 10)), Set.of());
            var first = rows.get(0); var second = rows.get(1); var target = target(7, 0, 50, 50); var occupied = Set.of(target.cell());
            roster.advance(SCOPE, 100); roster.pursue(SCOPE, second.id(), 1, List.of(target), occupied, 1, () -> false);
            var work = work(roster, second); roster.move(SCOPE, plan(roster, first, 4), occupied);
            roster.pursue(SCOPE, second.id(), 1, List.of(target), occupied, 1, () -> false);
            assertNotSame(work, work(roster, second));
        }
    }

    @Test void mutableGeometryMatchesAFreshIndependentShortestPathOracle() throws Exception {
        var map = map();
        for (int seed = 0; seed < 20; seed++) for (boolean completed : new boolean[]{false, true}) try (var roster = roster(map, 8)) {
            var npc = one(roster); roster.advance(SCOPE, 100); var old = target(7, 0, 50, 50);
            var occupied = new HashSet<ModernWorldMap.Cell>(); occupied.add(old.cell());
            var random = new java.util.Random(250L + seed);
            while (occupied.size() < 61) occupied.add(cell(11 + random.nextInt(22), 4 + random.nextInt(25)));
            if (completed) ModernNpcPursuitTest.finish(roster, npc, List.of(old), occupied, 64);
            else roster.pursue(SCOPE, npc.id(), 1, List.of(old), occupied, 8, () -> false);
            var next = target(7, 0, 50, 51); occupied.remove(old.cell()); occupied.add(next.cell());
            occupied.remove(cell(11, 10)); occupied.add(cell(12 + seed % 10, 12 + seed % 8));
            follow(roster, npc, List.of(next), occupied, 7);
        }
    }

    private static int follow(ModernNpcRoster roster, ModernNpcRoster.Actor npc, List<ModernNpcRoster.Target> targets,
            Set<ModernWorldMap.Cell> occupied, int budget) throws Exception {
        var map = map(); int expected = breadthFirstDistance(map, npc.cell(), targets, occupied), moves = 0;
        for (int tick = 0; tick < 16_384; tick++) {
            var before = roster.snapshot();
            var plan = roster.pursue(SCOPE, npc.id(), npc.life(), targets, occupied, budget, () -> false);
            assertEquals(before, roster.snapshot()); assertTrue(plan.visited() <= budget); auditFrontier(roster);
            if (plan.kind() == ModernNpcRoster.PlanKind.BUDGET) { roster.advance(SCOPE, before.elapsedMillis() + 1); continue; }
            if (plan.kind() == ModernNpcRoster.PlanKind.NONE) { assertEquals(-1, expected); assertEquals(0, moves); return moves; }
            if (plan.kind() == ModernNpcRoster.PlanKind.IN_RANGE) { assertEquals(expected, moves); return moves; }
            assertEquals(ModernNpcRoster.PlanKind.MOVE, plan.kind());
            var previous = npc; npc = roster.move(SCOPE, plan, occupied); moves++;
            assertTrue(legal(map, previous.cell(), npc.cell(), occupied, false)); assertTrue(moves <= expected);
            roster.advance(SCOPE, npc.nextMoveMillis());
        }
        fail("live geometry never completed its independently qualified route"); return -1;
    }

    private static Object work(ModernNpcRoster roster, ModernNpcRoster.Actor npc) throws Exception {
        var field = ModernNpcRoster.class.getDeclaredField("pursuits"); field.setAccessible(true);
        return ((java.util.Map<?, ?>) field.get(roster)).get(npc.id());
    }
}
