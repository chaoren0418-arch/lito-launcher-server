package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/** Explicit shared Lineage candidates. These are not original LT-map equivalence claims. */
class ModernDeathmatchSharedMapTest {
    private static final int[] DX = {0,1,1,1,0,-1,-1,-1}, DY = {-1,-1,0,1,1,1,0,-1};
    static Set<ModernWorldMap.SectorKey> sectors(int map) {
        return switch (map) {
            case 551 -> Set.of(new ModernWorldMap.SectorKey(0x7ffe,0x8000), new ModernWorldMap.SectorKey(0x7ffe,0x8001),
                    new ModernWorldMap.SectorKey(0x7fff,0x8000), new ModernWorldMap.SectorKey(0x7fff,0x8001));
            case 552 -> Set.of(new ModernWorldMap.SectorKey(0x7fff,0x8000), new ModernWorldMap.SectorKey(0x7fff,0x8001));
            default -> throw new IllegalArgumentException("not an explicit shared candidate");
        };
    }
    static Path sharedRoot() {
        String root = System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(root != null && !root.isBlank(), "explicit shared root required");
        Path path = Path.of(root); assertTrue(path.isAbsolute()); return path;
    }
    static List<Set<ModernWorldMap.Cell>> components(ModernWorldMap map) {
        var remaining = new HashSet<>(map.teleportCells());
        var result = new ArrayList<Set<ModernWorldMap.Cell>>();
        var order = Comparator.comparingInt(ModernWorldMap.Cell::y).thenComparingInt(ModernWorldMap.Cell::x);
        while (!remaining.isEmpty()) {
            var start = remaining.stream().min(order).orElseThrow();
            var seen = new HashSet<ModernWorldMap.Cell>(); var queue = new ArrayDeque<ModernWorldMap.Cell>();
            seen.add(start); queue.add(start); remaining.remove(start);
            while (!queue.isEmpty()) {
                var from = queue.remove();
                for (int h=0;h<8;h++) {
                    var to = new ModernWorldMap.Cell(from.x()+DX[h],from.y()+DY[h]);
                    if (map.canStep(from,to) && map.canStep(to,from) && seen.add(to)) {
                        queue.add(to); remaining.remove(to);
                    }
                }
                assertTrue(seen.size() <= map.manifest().sectors().size()*4096);
            }
            result.add(Set.copyOf(seen));
        }
        result.sort(Comparator.<Set<ModernWorldMap.Cell>>comparingInt(Set::size).reversed());
        return List.copyOf(result);
    }
    static ModernWorldMap.Cell centre(Set<ModernWorldMap.Cell> cells) {
        long sx = cells.stream().mapToLong(ModernWorldMap.Cell::x).sum();
        long sy = cells.stream().mapToLong(ModernWorldMap.Cell::y).sum(), n = cells.size();
        return cells.stream().min(Comparator.<ModernWorldMap.Cell>comparingLong(c -> {
            long dx = c.x()*n-sx, dy = c.y()*n-sy; return dx*dx+dy*dy;
        }).thenComparingInt(ModernWorldMap.Cell::y).thenComparingInt(ModernWorldMap.Cell::x)).orElseThrow();
    }
    static String directionDigest(ModernWorldMap map) throws Exception {
        var hash = MessageDigest.getInstance("SHA-256");
        for (var sector : map.sectorBounds()) for (int y=0;y<64;y++) for (int x=0;x<64;x++)
            hash.update((byte)map.directionMask(new ModernWorldMap.Cell(sector.origin().x()+x,sector.origin().y()+y)));
        return HexFormat.of().formatHex(hash.digest());
    }
    @Test void fixedGeometryAndDirectionsDoNotClaimAllIslandsAreOneArena() throws Exception {
        for (int id : List.of(551,552)) {
            var map = ModernWorldMap.load(sharedRoot(), id, sectors(id), () -> false);
            var groups = components(map); var connected = groups.get(0); var anchor = centre(connected);
            var fields=ModernSharedDeathmatchPreset.fields(id);
            assertEquals(fields.get("manifestSha256"),map.digest());
            assertEquals(Integer.parseInt(fields.get("spawnX")),anchor.x());
            assertEquals(Integer.parseInt(fields.get("spawnY")),anchor.y());
            assertEquals(id==551?1057:341,map.teleportCells().size());
            assertEquals(id==551?List.of(183,179,175,175,175,170):List.of(171,170),groups.stream().map(Set::size).toList());
            assertEquals(id==551?"c48f16a15b2dd1f5839392d0bdfeec69d278614d145a66ea5f9c8956de5bc63d":"e91bc4e503588f601ed4e9c6f9287ecb263caa0fa3b60416a2a95fb27de86122",directionDigest(map));
            assertTrue(connected.size() >= 100); var spawns = map.serverChosenSpawns(anchor,100);
            map.validateSpawns(spawns); assertTrue(connected.containsAll(spawns));
        }
    }
    @Test void everyAdvertisedSizeUpToFiftyHasIndependentWalkingExitsButOneHundredHumansFails() throws Exception {
        for (int id : List.of(551,552)) {
            var map = ModernWorldMap.load(sharedRoot(), id, sectors(id), () -> false);
            var anchor = centre(components(map).get(0));
            for(int size=2;size<=50;size++) for(int humans:List.of(1,size/2,size)) {
                    var members = ModernWorldSpawnsTest.roster(humans,size-humans);
                    var plan = ModernWorldSpawns.allocate(map,members,List.of(anchor),Set.of(),()->false);
                    ModernWorldSpawnsTest.assertPlan(map,members,plan,Set.of());
            }
            assertEquals(ModernWorldMap.Failure.INVALID_SPAWNS,assertThrows(ModernWorldMap.MapException.class,
                    ()->ModernWorldSpawns.allocate(map,ModernWorldSpawnsTest.roster(100,0),List.of(anchor),Set.of(),()->false)).failure());
        }
    }
}
