package local.lito381.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * Deterministic initial placement, not movement, teleport or respawn authority.
 * Every human can walk to one common free cell outside the Chebyshev-one halo
 * of the complete initial roster, while all other actors and objectives stay
 * occupied. A corpse therefore provides no fictitious escape route.
 *
 * <p>Safe legacy placements are retained byte-for-byte. Repair removes only
 * non-articulation vertices from one cardinal component, permanently reserving
 * the hub halo and a free cardinal neighbour of every human. Honor repair uses
 * frozen distances from the configured camps: own distance <= other distance
 * (ties belong to either camp). It never imports native/LT/SQL coordinates.</p>
 *
 * <p>At most four hub attempts share a 64-million-work-unit search budget.
 * One unit covers one bounded vertex, adjacency, candidate or roster check;
 * bulk array work is charged by length. Cancellation is checked every 256
 * traversal units and at bounded bulk-array boundaries, including final qualification. Exhaustion rejects
 * admission: this bounded search does not claim to solve every feasible graph.
 * Callers supply the existing absolute preparation deadline/cancellation; no
 * IO, random source, service gate or preparation Guard belongs in this code.</p>
 */
final class ModernWorldSpawns {
    static final long MAX_WORK = 64_000_000;
    static final int MAX_HUB_ATTEMPTS = 4;
    // Time Attack may reserve 32 distinct bounded waves and its activation
    // tower. Map, roster, hub-attempt and total traversal work stay bounded.
    static final int MAX_RESERVED_CELLS = 32 * 128 + 1;
    private static final int[] DX = {0, 1, 0, -1}, DY = {-1, 0, 1, 0};
    private ModernWorldSpawns() {}

    record Plan(List<ModernWorldMap.Cell> positions, ModernWorldMap.Cell escapeCell, boolean repaired) {
        Plan { positions = List.copyOf(positions); Objects.requireNonNull(escapeCell); }
    }

    /** Production entry: even legacy nearest-cell enumeration shares the work/deadline budget. */
    static Plan allocate(ModernWorldMap map, List<ModernRooms.Member> roster,
            List<ModernWorldMap.Cell> camps, Set<ModernWorldMap.Cell> objectives,
            BooleanSupplier cancelled) throws ModernWorldMap.MapException {
        return allocate(map,roster,camps,objectives,cancelled,false);
    }
    /** Quest validates a human in the WHOLE room before splitting isolated forces. */
    static Plan allocateQuestForce(ModernWorldMap map,List<ModernRooms.Member> roster,
            List<ModernWorldMap.Cell> camps,Set<ModernWorldMap.Cell> objectives,BooleanSupplier cancelled)
            throws ModernWorldMap.MapException {
        return allocate(map,roster,camps,objectives,cancelled,true);
    }
    private static Plan allocate(ModernWorldMap map,List<ModernRooms.Member> roster,
            List<ModernWorldMap.Cell> camps,Set<ModernWorldMap.Cell> objectives,BooleanSupplier cancelled,boolean forceSubset)
            throws ModernWorldMap.MapException {
        var budget = new Budget(cancelled, MAX_WORK);
        if (roster == null || roster.isEmpty() || roster.size() > 100) throw invalid();
        camps = List.copyOf(camps);
        if (camps.size() != 1 && camps.size() != 2) throw invalid();
        var graph = new Graph(map, objectives, budget);
        var excluded = new HashSet<>(objectives);
        var preferred = new ModernWorldMap.Cell[roster.size()];
        for (int camp = 0; camp < camps.size(); camp++) {
            int count = 0;
            for (var member : roster) { budget.work(); if (camps.size() == 1 || member.team() == camp) count++; }
            if (count == 0) throw invalid();
            var positions = graph.legacy(map, camps.get(camp), count, excluded, budget);
            excluded.addAll(positions); int next = 0;
            for (int i = 0; i < roster.size(); i++) {
                budget.work(); if (camps.size() == 1 || roster.get(i).team() == camp) preferred[i] = positions.get(next++);
            }
        }
        var old = List.of(preferred);
        return allocate(graph, members(roster, old, budget,forceSubset), old, camps, budget);
    }

    static Plan allocate(ModernWorldMap map, List<ModernRooms.Member> roster,
            List<ModernWorldMap.Cell> preferred, List<ModernWorldMap.Cell> camps,
            Set<ModernWorldMap.Cell> objectives, BooleanSupplier cancelled) throws ModernWorldMap.MapException {
        return allocate(map, roster, preferred, camps, objectives, cancelled, MAX_WORK);
    }

    /** Explicit work limit is a deterministic test seam, never a larger production allowance. */
    static Plan allocate(ModernWorldMap map, List<ModernRooms.Member> roster,
            List<ModernWorldMap.Cell> preferred, List<ModernWorldMap.Cell> camps,
            Set<ModernWorldMap.Cell> objectives, BooleanSupplier cancelled, long maximumWork)
            throws ModernWorldMap.MapException {
        var budget = new Budget(cancelled, maximumWork);
        var members = members(roster, preferred, budget);
        var graph = new Graph(map, objectives, budget);
        return allocate(graph, members, preferred, camps, budget);
    }

    private static Plan allocate(Graph graph, List<ModernRooms.Member> members,
            List<ModernWorldMap.Cell> preferred, List<ModernWorldMap.Cell> camps, Budget budget)
            throws ModernWorldMap.MapException {
        int[] old = graph.positions(preferred, budget);
        int safe = graph.qualify(members, old, budget);
        if (safe >= 0) return new Plan(preferred, graph.cells.get(safe), false);
        camps = List.copyOf(camps);
        if (camps.size() != 1 && camps.size() != 2) throw invalid();
        int[][] distances = new int[camps.size()][];
        for (int i = 0; i < camps.size(); i++) {
            budget.work();
            Integer start = graph.index.get(camps.get(i));
            if (start == null || !graph.available[start]) throw invalid();
            distances[i] = graph.distances(start, graph.available, budget);
        }
        if (camps.size() == 2 && distances[0][graph.index.get(camps.get(1))] < 0) throw invalid();
        boolean[] component = new boolean[graph.size()];
        boolean[] oldHalo = graph.halo(old, budget);
        budget.bulk(component.length);
        for (int i = 0; i < component.length; i++) {
            budget.work(); component[i] = distances[0][i] >= 0;
        }
        // Prefer nearby space already outside the original birth zone. Then
        // consider other hubs; the final layout, not the old halo, is authoritative.
        var tried = new HashSet<Integer>();
        for (int attempt = 0; attempt < MAX_HUB_ATTEMPTS; attempt++) {
            int hub = -1;
            for (int i = 0; i < component.length; i++) {
                budget.work();
                if (!component[i] || tried.contains(i)) continue;
                if (hub < 0 || hubBefore(graph, i, hub, oldHalo, distances)) hub = i;
            }
            if (hub < 0) break;
            tried.add(hub);
            int[] placed = graph.repair(members, old, component, distances, hub, budget);
            if (placed == null) continue;
            int exit = graph.qualify(members, placed, budget);
            if (exit >= 0) {
                var result = new ArrayList<ModernWorldMap.Cell>(placed.length);
                for (int value : placed) { budget.work(); result.add(graph.cells.get(value)); }
                budget.check();
                return new Plan(result, graph.cells.get(exit), true);
            }
        }
        throw invalid();
    }

    static ModernWorldMap.Cell qualify(ModernWorldMap map, List<ModernRooms.Member> roster,
            List<ModernWorldMap.Cell> positions, Set<ModernWorldMap.Cell> objectives,
            BooleanSupplier cancelled) throws ModernWorldMap.MapException {
        return qualify(map,roster,positions,objectives,cancelled,false);
    }
    static ModernWorldMap.Cell qualifyQuestForce(ModernWorldMap map,List<ModernRooms.Member> roster,
            List<ModernWorldMap.Cell> positions,Set<ModernWorldMap.Cell> objectives,BooleanSupplier cancelled)
            throws ModernWorldMap.MapException {
        return qualify(map,roster,positions,objectives,cancelled,true);
    }
    private static ModernWorldMap.Cell qualify(ModernWorldMap map,List<ModernRooms.Member> roster,
            List<ModernWorldMap.Cell> positions,Set<ModernWorldMap.Cell> objectives,BooleanSupplier cancelled,boolean forceSubset)
            throws ModernWorldMap.MapException {
        var budget = new Budget(cancelled, MAX_WORK);
        var members = members(roster, positions, budget,forceSubset);
        var graph = new Graph(map, objectives, budget);
        int result = graph.qualify(members, graph.positions(positions, budget), budget);
        if (result < 0) throw invalid();
        budget.check();
        return graph.cells.get(result);
    }

    /** Exact conservative client edge, including occupied diagonal intermediates. */
    static boolean stepOpen(ModernWorldMap map, ModernWorldMap.Cell from, ModernWorldMap.Cell to,
            Set<ModernWorldMap.Cell> occupied) {
        if (occupied.contains(from) || occupied.contains(to) || !map.canStep(from, to)) return false;
        if (from.x() == to.x() || from.y() == to.y()) return true;
        var viaX = new ModernWorldMap.Cell(to.x(), from.y());
        var viaY = new ModernWorldMap.Cell(from.x(), to.y());
        return !occupied.contains(viaX) && map.canStep(from, viaX) && map.canStep(viaX, to)
                || !occupied.contains(viaY) && map.canStep(from, viaY) && map.canStep(viaY, to);
    }

    private static List<ModernRooms.Member> members(List<ModernRooms.Member> roster,
            List<ModernWorldMap.Cell> positions, Budget budget) throws ModernWorldMap.MapException {
        return members(roster,positions,budget,false);
    }
    private static List<ModernRooms.Member> members(List<ModernRooms.Member> roster,
            List<ModernWorldMap.Cell> positions,Budget budget,boolean forceSubset)throws ModernWorldMap.MapException {
        if (roster == null || positions == null || roster.isEmpty() || roster.size() > 100
                || roster.size() != positions.size()) throw invalid();
        var copy = List.copyOf(roster); var identities = new HashSet<ModernParticipantId>(); boolean human = false;
        for (var member : copy) {
            budget.work();
            if (!identities.add(member.identity()) || member.team() < 0 || member.team() > 1) throw invalid();
            human |= member.identity().isHuman();
        }
        if (!human&&!forceSubset) throw invalid();
        return copy;
    }

    private static boolean hubBefore(Graph graph, int a, int b, boolean[] halo, int[][] distances) {
        if (halo[a] != halo[b]) return !halo[a];
        int da = distances[0][a], db = distances[0][b];
        if (distances.length == 2) { da = Math.max(da, distances[1][a]); db = Math.max(db, distances[1][b]); }
        return da != db ? da < db : graph.before(a, b);
    }

    private static final class Graph {
        final List<ModernWorldMap.Cell> cells = new ArrayList<>();
        final HashMap<ModernWorldMap.Cell, Integer> index = new HashMap<>();
        final int[][] edges;
        final boolean[] available;

        Graph(ModernWorldMap map, Set<ModernWorldMap.Cell> objectives, Budget budget) throws ModernWorldMap.MapException {
            Objects.requireNonNull(map);
            if (objectives.size() > MAX_RESERVED_CELLS) throw invalid();
            objectives = Set.copyOf(objectives);
            for (var sector : map.sectorBounds()) {
                for (int y = 0; y < sector.height(); y++) for (int x = 0; x < sector.width(); x++) {
                    budget.work();
                    var cell = new ModernWorldMap.Cell(Math.addExact(sector.origin().x(), x), Math.addExact(sector.origin().y(), y));
                    if (map.standing(cell)) { index.put(cell, cells.size()); cells.add(cell); }
                }
            }
            if (cells.isEmpty() || cells.size() > ModernWorldMap.MAX_SECTORS * 4096) throw invalid();
            available = new boolean[size()]; edges = new int[size()][4]; budget.bulk((long)size() * 5);
            Arrays.fill(available, true);
            for (var objective : objectives) {
                budget.work(); Integer i = index.get(objective); if (i == null) throw invalid(); available[i] = false;
            }
            for (int i = 0; i < size(); i++) {
                var cell = cells.get(i);
                for (int d = 0; d < 4; d++) {
                    budget.work();
                    var next = new ModernWorldMap.Cell(Math.addExact(cell.x(), DX[d]), Math.addExact(cell.y(), DY[d]));
                    Integer j = index.get(next);
                    edges[i][d] = j != null && map.canStep(cell, next) ? j : -1;
                }
            }
        }
        int size() { return cells.size(); }
        List<ModernWorldMap.Cell> legacy(ModernWorldMap map, ModernWorldMap.Cell anchor, int count,
                Set<ModernWorldMap.Cell> excluded, Budget budget) throws ModernWorldMap.MapException {
            Integer first = index.get(anchor); if (first == null) throw invalid();
            boolean[] seen = new boolean[size()]; int[] queue = new int[size()]; budget.bulk((long)size() * 2);
            var result = new ArrayList<ModernWorldMap.Cell>(count); int read = 0, write = 0;
            seen[first] = true; queue[write++] = first;
            int[] dx = {0,1,1,1,0,-1,-1,-1}, dy = {-1,-1,0,1,1,1,0,-1};
            while (read < write && result.size() < count) {
                budget.work(); var cell = cells.get(queue[read++]);
                if (!excluded.contains(cell)) result.add(cell);
                for (int d = 0; d < 8; d++) {
                    budget.work(); var next = new ModernWorldMap.Cell(Math.addExact(cell.x(),dx[d]),Math.addExact(cell.y(),dy[d]));
                    Integer value = index.get(next);
                    if (value != null && !seen[value] && map.canStep(cell,next)) { seen[value] = true; queue[write++] = value; }
                }
            }
            if (result.size() != count) throw invalid();
            return result;
        }
        boolean before(int a, int b) {
            var x = cells.get(a); var y = cells.get(b);
            return x.x() != y.x() ? x.x() < y.x() : x.y() < y.y();
        }
        int[] positions(List<ModernWorldMap.Cell> positions, Budget budget) throws ModernWorldMap.MapException {
            int[] result = new int[positions.size()]; var distinct = new HashSet<Integer>();
            for (int i = 0; i < result.length; i++) {
                budget.work(); Integer value = index.get(positions.get(i));
                if (value == null || !available[value] || !distinct.add(value)) throw invalid();
                result[i] = value;
            }
            return result;
        }
        int[] distances(int start, boolean[] free, Budget budget) throws ModernWorldMap.MapException {
            int[] distance = new int[size()], queue = new int[size()]; budget.bulk((long)size() * 2);
            Arrays.fill(distance, -1); if (!free[start]) return distance;
            int read = 0, write = 0; distance[start] = 0; queue[write++] = start;
            while (read < write) {
                int v = queue[read++]; budget.work();
                for (int w : edges[v]) {
                    budget.work();
                    if (w >= 0 && free[w] && distance[w] < 0) { distance[w] = distance[v] + 1; queue[write++] = w; }
                }
            }
            return distance;
        }
        boolean[] halo(int[] positions, Budget budget) throws ModernWorldMap.MapException {
            boolean[] halo = new boolean[size()]; budget.bulk(size());
            for (int value : positions) {
                var p = cells.get(value);
                for (int y = -1; y <= 1; y++) for (int x = -1; x <= 1; x++) {
                    budget.work(); Integer i = index.get(new ModernWorldMap.Cell(Math.addExact(p.x(), x), Math.addExact(p.y(), y)));
                    if (i != null) halo[i] = true;
                }
            }
            return halo;
        }
        /**
         * Cardinal connectivity is exactly client step_open reachability: every
         * permitted diagonal has an unoccupied intermediate and both cardinal
         * edges. Conversely every free cardinal edge is permitted by the client.
         * Thus no destination-only diagonal can manufacture an escape here.
         */
        int qualify(List<ModernRooms.Member> roster, int[] positions, Budget budget) throws ModernWorldMap.MapException {
            int[] reach = distances(positions[0], available, budget);
            for (int p : positions) { budget.work(); if (reach[p] < 0) return -1; }
            boolean[] free = available.clone(); budget.bulk(size());
            for (int p : positions) { budget.work(); free[p] = false; }
            boolean[] halo = halo(positions, budget);
            int[] label = new int[size()], queue = new int[size()], exits = new int[size()];
            budget.bulk((long)size() * 3); Arrays.fill(label, -1); Arrays.fill(exits, -1);
            int count = 0;
            for (int start = 0; start < size(); start++) {
                budget.work(); if (!free[start] || label[start] >= 0) continue;
                int read = 0, write = 0; queue[write++] = start; label[start] = count;
                while (read < write) {
                    budget.work(); int v = queue[read++];
                    if (!halo[v] && (exits[count] < 0 || before(v, exits[count]))) exits[count] = v;
                    for (int w : edges[v]) {
                        budget.work();
                        if (w >= 0 && free[w] && label[w] < 0) { label[w] = count; queue[write++] = w; }
                    }
                }
                count++;
            }
            boolean[] eligible = new boolean[count]; budget.bulk(count); Arrays.fill(eligible, true);
            for (int i = 0; i < positions.length; i++) {
                budget.work(); if (!roster.get(i).identity().isHuman()) continue;
                int[] touching = new int[4]; Arrays.fill(touching, -1); int n = 0;
                for (int neighbour : edges[positions[i]]) {
                    budget.work(); if (neighbour >= 0 && free[neighbour]) touching[n++] = label[neighbour];
                }
                for (int c = 0; c < count; c++) {
                    budget.work(); boolean found = false;
                    for (int j = 0; j < n; j++) { budget.work(); found |= touching[j] == c; }
                    eligible[c] &= found;
                }
            }
            int exit = -1;
            for (int c = 0; c < count; c++) {
                budget.work(); if (eligible[c] && exits[c] >= 0 && (exit < 0 || before(exits[c], exit))) exit = exits[c];
            }
            budget.check(); return exit;
        }
        int[] repair(List<ModernRooms.Member> roster, int[] old, boolean[] component, int[][] distances,
                int hub, Budget budget) throws ModernWorldMap.MapException {
            boolean[] free = component.clone(), reserved = halo(new int[]{hub}, budget);
            budget.bulk(size()); int[] result = new int[roster.size()];
            for (int i = 0; i < result.length; i++) {
                boolean[] cuts = cuts(hub, free, budget);
                int team = distances.length == 1 ? 0 : roster.get(i).team();
                int choice = -1;
                for (int v = 0; v < size(); v++) {
                    budget.work();
                    if (!free[v] || reserved[v] || cuts[v] || distances[team][v] < 0
                            || distances.length == 2 && distances[team][v] > distances[1-team][v]) continue;
                    if (choice < 0 || v == old[i] || choice != old[i] && (distances[team][v] < distances[team][choice]
                            || distances[team][v] == distances[team][choice] && before(v, choice))) choice = v;
                }
                if (choice < 0) return null;
                // A non-root, non-cut vertex has a surviving DFS parent. Prefer
                // an already reserved neighbour to avoid consuming extra space.
                if (roster.get(i).identity().isHuman()) {
                    int neighbour = -1;
                    for (int v : edges[choice]) {
                        budget.work(); if (v < 0 || !free[v]) continue;
                        if (neighbour < 0 || reserved[v] && !reserved[neighbour]
                                || reserved[v] == reserved[neighbour] && before(v, neighbour)) neighbour = v;
                    }
                    if (neighbour < 0) return null;
                    reserved[neighbour] = true;
                }
                result[i] = choice; free[choice] = false;
            }
            return result;
        }
        /** Iterative Tarjan; even a 65,536-cell corridor never consumes the Java call stack. */
        boolean[] cuts(int root, boolean[] free, Budget budget) throws ModernWorldMap.MapException {
            int n = size(); budget.bulk((long)n * 7);
            int[] seen = new int[n], low = new int[n], parent = new int[n], children = new int[n];
            int[] stack = new int[n], edge = new int[n]; boolean[] cut = new boolean[n];
            Arrays.fill(parent, -1); int size = 1, time = 1; stack[0] = root; seen[root] = low[root] = time;
            while (size > 0) {
                budget.work(); int v = stack[size - 1];
                if (edge[v] < 4) {
                    int w = edges[v][edge[v]++]; budget.work();
                    if (w < 0 || !free[w]) continue;
                    if (seen[w] == 0) {
                        parent[w] = v; children[v]++; seen[w] = low[w] = ++time; stack[size++] = w;
                    } else if (w != parent[v]) low[v] = Math.min(low[v], seen[w]);
                } else {
                    size--; int p = parent[v];
                    if (p < 0) cut[v] = children[v] > 1;
                    else {
                        low[p] = Math.min(low[p], low[v]);
                        if (parent[p] >= 0 && low[v] >= seen[p]) cut[p] = true;
                    }
                }
            }
            return cut;
        }
    }

    private static final class Budget {
        final BooleanSupplier cancelled; long remaining; int untilCheck = 256;
        Budget(BooleanSupplier cancelled, long limit) throws ModernWorldMap.MapException {
            this.cancelled = Objects.requireNonNull(cancelled);
            if (limit < 1 || limit > MAX_WORK) throw new IllegalArgumentException("invalid spawn work bound");
            remaining = limit; check();
        }
        void work() throws ModernWorldMap.MapException {
            if (--remaining < 0) throw new ModernWorldMap.MapException(ModernWorldMap.Failure.TOO_LARGE);
            if (--untilCheck == 0) { check(); untilCheck = 256; }
        }
        void bulk(long n) throws ModernWorldMap.MapException {
            check(); remaining -= n;
            if (remaining < 0) throw new ModernWorldMap.MapException(ModernWorldMap.Failure.TOO_LARGE);
        }
        void check() throws ModernWorldMap.MapException {
            if (Thread.currentThread().isInterrupted() || cancelled.getAsBoolean())
                throw new ModernWorldMap.MapException(ModernWorldMap.Failure.CANCELLED);
        }
    }
    private static ModernWorldMap.MapException invalid() {
        return new ModernWorldMap.MapException(ModernWorldMap.Failure.INVALID_SPAWNS);
    }
}
