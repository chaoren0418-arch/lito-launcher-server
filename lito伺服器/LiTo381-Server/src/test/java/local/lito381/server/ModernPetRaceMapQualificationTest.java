package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/** Shared-resource qualification only, not a playable or originally recovered race ruleset. */
class ModernPetRaceMapQualificationTest {
    static final int PHYSICAL = 5143;
    static final ModernWorldMap.Cell ANCHOR = new ModernWorldMap.Cell(32768, 32849);
    static final List<ModernWorldMap.SectorManifest> PINS = List.of(
            pin(0x7fff, 0x8000, 127474, "72f52a1f3dbc6db65650c4c470f254a6a1d94b4283dc928b78edb4921aacdb15"),
            pin(0x7fff, 0x8001, 141309, "2712d11716d060cf220d3f33ab2c26a190bb4c333ab78e185eb24baf996effea"),
            pin(0x8000, 0x8000, 111184, "c8f0ef53c400f2558e7239c21d092be10a42613fd2b74ef0fe9270d05a3e5cb8"),
            pin(0x8000, 0x8001, 99133, "054ca18e1cb46f4084135fc5409bb692bb8e99b998c50579148f6b2059bcae0d"));

    private static ModernWorldMap.SectorManifest pin(int x, int y, int length, String sha) {
        return new ModernWorldMap.SectorManifest(new ModernWorldMap.SectorKey(x, y), length, sha);
    }
    static Path root() {
        String value = System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(value != null && !value.isBlank(), "explicit shared resource root required");
        return Path.of(value);
    }
    static Set<ModernWorldMap.SectorKey> sectors() {
        return PINS.stream().map(ModernWorldMap.SectorManifest::key)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
    static ModernWorldMap shared() throws Exception {
        var map = ModernWorldMap.load(root(), PHYSICAL, sectors(), () -> false);
        assertEquals(PINS, map.manifest().sectors());
        return map;
    }

    @Test void fullShared5143HasPinnedGeometryAndAnExplicitDifferentLogicalCandidate() throws Exception {
        var map = shared();
        assertEquals(PHYSICAL, map.mapId());
        // Actual shared four-sector Rust decoder independently produced these
        // values in the first central probe; this is not native screenshot equivalence.
        assertEquals("f6c171c57eeae9e9223af51b12e89a1f0640086150abb41252e5e742c6feffde", map.digest());
        assertTrue(map.standing(ANCHOR), "retained Lineage spawn is only a candidate until actual terrain accepts it");
        var expected = new ModernWorldMapQualification.Expected(
                new ModernWorldBinding.Key(0, 565), map.manifest(), ANCHOR, 10);
        var binding = ModernWorldMapQualification.qualify(map, expected);
        assertEquals(565, binding.key().logicalMapId());
        assertEquals(PHYSICAL, binding.map().mapId());
        var directions = MessageDigest.getInstance("SHA-256");
        int standing = 0;
        for (var bound : map.sectorBounds()) {
            for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) {
                var cell = new ModernWorldMap.Cell(bound.origin().x() + x, bound.origin().y() + y);
                directions.update((byte) map.directionMask(cell));
                if (map.standing(cell)) standing++;
            }
        }
        String directionHash = HexFormat.of().formatHex(directions.digest());
        assertEquals("a95dddc3ea19bd1c979ce219839d990824b0ab6945256f9d01ade70e89cc7a58", directionHash);
        assertEquals(1471, standing);
        assertEquals(255, map.directionMask(ANCHOR));
        System.out.println("shared5143-java qualificationCandidate=true runtimeRace=false canonical="
                + map.digest() + " directionSHA=" + directionHash
                + " standing=" + standing + " anchorMask=" + map.directionMask(ANCHOR));
        var forged = new ModernWorldMap.Manifest(565, PINS, map.digest());
        var wrong = new ModernWorldMapQualification.Expected(new ModernWorldBinding.Key(0, 565), forged, ANCHOR, 10);
        assertEquals(ModernWorldMap.Failure.CHANGED_FILE,
                assertThrows(ModernWorldMap.MapException.class, () -> ModernWorldMapQualification.qualify(map, wrong)).failure());
    }

    @Test void tenRealHumanBirthsEachHaveAnIndependentOccupiedGridExit() throws Exception {
        var map = shared();
        var roster = new ArrayList<ModernRooms.Member>();
        for (int i = 0; i < 10; i++) {
            roster.add(new ModernRooms.Member(ModernParticipantId.human(i % 2 == 0 ? 7 : 42, 3333 + i / 2),
                    "Runner" + i, i % 2 == 0 ? "Sun" : "Moon", 80, 1, 0, 0, true));
        }
        var plan = ModernWorldSpawns.allocate(map, roster, List.of(ANCHOR), Set.of(), () -> false);
        assertEquals(10, plan.positions().size());
        assertEquals(10, plan.positions().stream().distinct().count());
        map.validateSpawns(plan.positions());
        int maximumSteps = 0;
        for (int i = 0; i < 10; i++) {
            var occupied = new HashSet<>(plan.positions());
            var from = plan.positions().get(i);
            occupied.remove(from);
            var path = route(map, from, plan.escapeCell()::equals, occupied);
            assertTrue(path.size() > 1);
            maximumSteps = Math.max(maximumSteps, path.size() - 1);
            for (int j = 1; j < path.size(); j++) assertTrue(independentStep(map, path.get(j - 1), path.get(j), occupied));
        }
        System.out.println("shared5143-java births=" + plan.positions() + " escape=" + plan.escapeCell()
                + " repaired=" + plan.repaired() + " maximumOccupiedExitSteps=" + maximumSteps);
    }

    @Test void retainedLineageCheckpointCandidateFormsAnActualDirectedWalkCycle() throws Exception {
        var map = shared();
        // Retained L1PolyRace.checkLapFinish uses physical5143. These are only
        // candidate bands checked against shared geometry, not LT565 coordinates
        // or a claim that original LiTo server gates were recovered.
        List<Predicate<ModernWorldMap.Cell>> gates = List.of(
                c -> c.x() == 32762 && c.y() >= 32845,
                c -> c.x() == 32754 && c.y() >= 32845,
                c -> c.x() == 32748 && c.y() >= 32845,
                c -> c.x() <= 32743 && c.y() == 32844,
                c -> c.x() <= 32742 && c.y() == 32840,
                c -> c.x() <= 32742 && c.y() == 32835,
                c -> c.x() <= 32742 && c.y() == 32830,
                c -> c.x() <= 32742 && c.y() == 32826,
                c -> c.x() <= 32742 && c.y() == 32822,
                c -> c.x() == 32749 && c.y() <= 32818,
                c -> c.x() == 32755 && c.y() <= 32818,
                c -> c.x() == 32760 && c.y() <= 32818,
                c -> c.x() == 32765 && c.y() <= 32818,
                c -> c.x() == 32770 && c.y() <= 32818,
                c -> c.x() == 32775 && c.y() <= 32818,
                c -> c.x() == 32780 && c.y() <= 32818,
                c -> c.x() == 32785 && c.y() <= 32818,
                c -> c.x() == 32789 && c.y() <= 32818,
                c -> c.x() >= 32792 && c.y() == 32821,
                c -> c.x() >= 32793 && c.y() == 32826,
                c -> c.x() >= 32793 && c.y() == 32831,
                c -> c.x() >= 32793 && c.y() == 32836,
                c -> c.x() >= 32793 && c.y() == 32842,
                c -> c.x() == 32790 && c.y() >= 32845,
                c -> c.x() == 32785 && c.y() >= 32845,
                c -> c.x() == 32780 && c.y() >= 32845,
                c -> c.x() == 32775 && c.y() >= 32845,
                c -> c.x() == 32770 && c.y() >= 32845,
                c -> c.x() == 32764 && c.y() >= 32845);
        var current = ANCHOR;
        var entries = new ArrayList<ModernWorldMap.Cell>();
        int steps = 0;
        for (var gate : gates) {
            assertFalse(gate.test(current), "each next checkpoint must actually be entered");
            var path = route(map, current, gate, Set.of());
            assertTrue(path.size() > 1);
            current = path.get(path.size() - 1);
            assertTrue(gate.test(current));
            assertFalse(gate.test(path.get(path.size() - 2)));
            entries.add(current);
            steps += path.size() - 1;
        }
        var closing = route(map, current, ANCHOR::equals, Set.of());
        assertTrue(closing.size() > 1);
        steps += closing.size() - 1;
        assertTrue(steps > gates.size());
        assertEquals(29, entries.size());
        System.out.println("shared5143-java candidateCheckpointEntries=" + entries + " closedCycleSteps=" + steps);
    }

    @Test void missingLogicalRaceFoldersAreRejectedWithoutReadingLtOrAnotherPhysicalMap() {
        var root = root();
        for (int logical : List.of(565, 566, 569, 570)) {
            assertTrue(Files.notExists(root.resolve("map").resolve(Integer.toString(logical)), LinkOption.NOFOLLOW_LINKS));
            // Missing ancestor directories fail path qualification first. Only
            // an absent leaf under an existing map is classified MISSING_SECTOR.
            assertEquals(ModernWorldMap.Failure.INVALID_PATH,
                    assertThrows(ModernWorldMap.MapException.class,
                            () -> ModernWorldMap.load(root, logical, sectors(), () -> false)).failure());
        }
    }

    private static List<ModernWorldMap.Cell> route(ModernWorldMap map, ModernWorldMap.Cell start,
            Predicate<ModernWorldMap.Cell> target, Set<ModernWorldMap.Cell> occupied) {
        assertFalse(occupied.contains(start));
        var pending = new ArrayDeque<ModernWorldMap.Cell>();
        var previous = new HashMap<ModernWorldMap.Cell, ModernWorldMap.Cell>();
        pending.add(start); previous.put(start, start);
        while (!pending.isEmpty()) {
            var from = pending.remove();
            if (target.test(from)) {
                var path = new ArrayList<ModernWorldMap.Cell>();
                for (var cell = from;; cell = previous.get(cell)) {
                    path.add(cell); if (cell.equals(start)) break;
                }
                java.util.Collections.reverse(path);
                return List.copyOf(path);
            }
            assertTrue(previous.size() <= 4 * 4096, "bounded explicit four-sector traversal");
            for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                var to = new ModernWorldMap.Cell(from.x() + dx, from.y() + dy);
                if (!previous.containsKey(to) && independentStep(map, from, to, occupied)) {
                    previous.put(to, from); pending.add(to);
                }
            }
        }
        fail("shared candidate has no approved terrain route from " + start);
        return List.of();
    }
    private static boolean independentStep(ModernWorldMap map, ModernWorldMap.Cell from,
            ModernWorldMap.Cell to, Set<ModernWorldMap.Cell> occupied) {
        if (occupied.contains(from) || occupied.contains(to) || !map.canStep(from, to)) return false;
        if (from.x() == to.x() || from.y() == to.y()) return true;
        var a = new ModernWorldMap.Cell(from.x(), to.y());
        var b = new ModernWorldMap.Cell(to.x(), from.y());
        return !occupied.contains(a) && map.canStep(from, a) && map.canStep(a, to)
                || !occupied.contains(b) && map.canStep(from, b) && map.canStep(b, to);
    }
}
