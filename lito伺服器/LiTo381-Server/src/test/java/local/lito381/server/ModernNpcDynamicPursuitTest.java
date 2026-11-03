package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernNpcRosterTest.*;

import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModernNpcDynamicPursuitTest {
    @Test void aTargetTakingLegalStepsEvery200MillisMustNotStarveThe64NodeNpcSearch() throws Exception {
        chase(true);
    }

    @Test void anUnrelatedMovingParticipantMustNotLeaveTheNpcAtItsSpawnForever() throws Exception {
        chase(false);
    }

    @Test void completeRoutesAgreeWithAnIndependentBreadthFirstOracleAtEveryServiceQuota() throws Exception {
        var map = map();
        for (int scenario = 0; scenario < 20; scenario++) {
            var targets = List.of(target(7, 0, 34, 22 + scenario % 10), target(42, 0, 9, 40));
            var occupied = new HashSet<ModernWorldMap.Cell>();
            targets.forEach(t -> occupied.add(t.cell()));
            if (scenario == 0) {
                for (int y = 1; y < 35; y++) occupied.add(cell(11, y)); // A real detour.
            } else if (scenario == 1) {
                for (int y = 9; y <= 11; y++) for (int x = 9; x <= 11; x++)
                    if (x != 10 || y != 10) occupied.add(cell(x, y)); // No exit, no guessed move.
            } else if (scenario == 2) {
                occupied.add(cell(11, 10)); occupied.add(cell(10, 11)); // Blocked diagonal corner.
            } else {
                var random = new java.util.Random(249L + scenario);
                while (occupied.size() < 62) occupied.add(cell(11 + random.nextInt(22), 4 + random.nextInt(25)));
            }
            int distance = breadthFirstDistance(map, cell(10, 10), targets, occupied);
            var slow = confirmedRoute(map, targets, occupied, 1, distance);
            assertEquals(slow, confirmedRoute(map, targets, occupied, 7, distance), "slice independence " + scenario);
            assertEquals(slow, confirmedRoute(map, targets, occupied, 64, distance), "deterministic route " + scenario);
        }
    }

    /** Independent FIFO search: no production pursuit, heuristic, cached route,
     * private node, or synthetic Plan is used to derive the optimal distance. */
    static int breadthFirstDistance(ModernWorldMap map, ModernWorldMap.Cell start,
            List<ModernNpcRoster.Target> targets, Set<ModernWorldMap.Cell> occupied) {
        var distances = new HashMap<ModernWorldMap.Cell, Integer>();
        var queue = new ArrayDeque<ModernWorldMap.Cell>(); distances.put(start, 0); queue.add(start);
        while (!queue.isEmpty()) {
            var here = queue.removeFirst(); int distance = distances.get(here);
            for (var target : targets) if (legal(map, here, target.cell(), occupied, true)) return distance;
            for (int y = -1; y <= 1; y++) for (int x = -1; x <= 1; x++) {
                if (x == 0 && y == 0) continue;
                var next = new ModernWorldMap.Cell(here.x() + x, here.y() + y);
                if (!distances.containsKey(next) && legal(map, here, next, occupied, false)) {
                    distances.put(next, distance + 1); queue.addLast(next);
                }
            }
        }
        return -1;
    }

    static boolean legal(ModernWorldMap map, ModernWorldMap.Cell from, ModernWorldMap.Cell to,
            Set<ModernWorldMap.Cell> occupied, boolean attack) {
        if (from.equals(to) || !attack && occupied.contains(to) || !map.canStep(from, to)) return false;
        if (from.x() == to.x() || from.y() == to.y()) return true;
        // A diagonal needs at least one unobstructed orthogonal route.
        for (var middle : List.of(new ModernWorldMap.Cell(from.x(), to.y()), new ModernWorldMap.Cell(to.x(), from.y())))
            if (!occupied.contains(middle) && map.canStep(from, middle) && map.canStep(middle, to)) return true;
        return false;
    }

    private static List<ModernWorldMap.Cell> confirmedRoute(ModernWorldMap map, List<ModernNpcRoster.Target> targets,
            Set<ModernWorldMap.Cell> occupied, int quota, int distance) throws Exception {
        try (var roster = roster(map, 8)) {
            var npc = one(roster); var route = new ArrayList<ModernWorldMap.Cell>();
            for (int tick = 1; tick <= 16_384; tick++) {
                roster.advance(SCOPE, tick * 100L); var before = roster.snapshot();
                var plan = roster.pursue(SCOPE, npc.id(), 1, targets, occupied, quota, () -> false);
                assertEquals(before, roster.snapshot()); assertTrue(plan.visited() <= quota);
                auditFrontier(roster); // Read-only, including every partial slice.
                if (plan.kind() == ModernNpcRoster.PlanKind.BUDGET) continue;
                if (plan.kind() == ModernNpcRoster.PlanKind.NONE) {
                    assertEquals(-1, distance); assertTrue(route.isEmpty()); return route;
                }
                if (plan.kind() == ModernNpcRoster.PlanKind.IN_RANGE) {
                    assertEquals(distance, route.size(), "a full legal shortest route, not only a preferred first step");
                    var end = npc.cell();
                    assertTrue(targets.stream().anyMatch(t -> t.participant().equals(plan.target()) && legal(map, end, t.cell(), occupied, true)));
                    return route;
                }
                assertEquals(ModernNpcRoster.PlanKind.MOVE, plan.kind());
                var previous = npc; npc = roster.move(SCOPE, plan, occupied);
                assertTrue(legal(map, previous.cell(), npc.cell(), occupied, false));
                route.add(npc.cell()); assertTrue(route.size() <= distance);
            }
            fail("bounded search failed to finish the oracle-qualified fixture"); return List.of();
        }
    }

    static void auditFrontier(ModernNpcRoster roster) throws Exception {
        var field = ModernNpcRoster.class.getDeclaredField("pursuits"); field.setAccessible(true);
        for (var work : ((Map<?, ?>) field.get(roster)).values()) {
            var seenField = work.getClass().getDeclaredField("seen"); seenField.setAccessible(true);
            var pendingField = work.getClass().getDeclaredField("pending"); pendingField.setAccessible(true);
            var seen = (Map<?, ?>) seenField.get(work); var pending = (Set<?>) pendingField.get(work);
            var expandedField = work.getClass().getDeclaredField("expanded"); expandedField.setAccessible(true);
            var expanded = (Set<?>) expandedField.get(work);
            assertTrue(expanded.size() <= ModernNpcRoster.MAX_PATH_CELLS);
            if (!seen.isEmpty()) assertTrue(seen.keySet().containsAll(expanded));
            assertTrue(seen.size() <= ModernNpcRoster.MAX_PATH_CELLS); assertTrue(pending.size() <= seen.size());
            var cells = new HashSet<>();
            for (var node : pending) {
                var cellMethod = node.getClass().getDeclaredMethod("cell"); cellMethod.setAccessible(true);
                var cell = cellMethod.invoke(node);
                assertTrue(cells.add(cell), "no stale duplicate frontier entry"); assertSame(node, seen.get(cell));
            }
        }
    }

    private static void chase(boolean movingTarget) throws Exception {
        var map = map();
        try (var roster = roster(map, 8)) {
            var npc = one(roster); int moves = 0, yielded = 0; boolean reached = false;
            ModernWorldMap.Cell oldTarget = null, oldOther = null;
            for (int tick = 1; tick <= 80; tick++) {
                var target = target(7, 0, 50, 50 + (movingTarget ? (tick / 2) % 2 : 0));
                if (target.cell().equals(npc.cell())) target = new ModernNpcRoster.Target(target.participant(), 0, oldTarget, true);
                var other = target(42, 1, 20, 2 + (movingTarget ? 0 : tick % 2));
                if (oldTarget != null && !oldTarget.equals(target.cell())) assertTrue(map.canStep(oldTarget, target.cell()));
                if (oldOther != null && !oldOther.equals(other.cell())) assertTrue(map.canStep(oldOther, other.cell()));
                oldTarget = target.cell(); oldOther = other.cell();
                var occupied = Set.of(target.cell(), other.cell());
                roster.advance(SCOPE, tick * 100L); var before = roster.snapshot();
                var plan = roster.pursue(SCOPE, npc.id(), 1, List.of(target, other), occupied, 64, () -> false);
                assertTrue(plan.visited() <= 64); assertEquals(before, roster.snapshot());
                if (plan.kind() == ModernNpcRoster.PlanKind.BUDGET) { yielded++; continue; }
                assertEquals(target.participant(), plan.target(), "never switch to another lane/source");
                if (plan.kind() == ModernNpcRoster.PlanKind.IN_RANGE) { reached = true; break; }
                assertEquals(ModernNpcRoster.PlanKind.MOVE, plan.kind());
                var previous = npc; npc = roster.move(SCOPE, plan, occupied); moves++;
                assertTrue(map.canStep(previous.cell(), npc.cell())); assertFalse(occupied.contains(npc.cell()));
                assertEquals(moves, npc.walk().sequence()); assertEquals(280, npc.hp());
            }
            assertTrue(reached, "dynamic geometry starved pursuit: moves=" + moves + ", budget yields=" + yielded);
            assertTrue(moves >= 39 && moves <= 41, "full legal approach, not a teleport or a fabricated near fixture");
            System.out.println("dynamic NPC pursuit movingTarget=" + movingTarget + " moves=" + moves + " budgetYields=" + yielded);
        }
    }
}
