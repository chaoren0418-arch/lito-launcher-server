package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

final class ArenaPassabilityCatalogTest {
    @Test
    void map564UsesOriginalClientRuntimeValidatedOpenSpawns() {
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(564);
        ArenaPassabilityCatalog catalog = ArenaPassabilityCatalog.get();

        assertEquals(new ArenaMapCatalog.Point(32729, 32797), arena.spawn(0));
        assertEquals(new ArenaMapCatalog.Point(32741, 32801), arena.spawn(1));
        assertEquals(0xFF, openMask(catalog, 564,
                arena.spawn(0).x(), arena.spawn(0).y()));
        assertEquals(0xFF, openMask(catalog, 564,
                arena.spawn(1).x(), arena.spawn(1).y()));
    }

    @Test
    void loadsAllNativeModeMapsAndPinsEvidenceCounts() {
        ArenaPassabilityCatalog catalog = ArenaPassabilityCatalog.get();
        assertEquals(45, catalog.mapCount());
        assertEquals(91_245, catalog.blockedEdgeCount(532));
        assertEquals(100_380, catalog.blockedEdgeCount(540));
        assertEquals(20_180, catalog.blockedEdgeCount(573));
        assertEquals(20_175, catalog.blockedEdgeCount(574));
    }

    @Test
    void everyPublishedSafeAnchorIsNativeStandingPassable() {
        for (int mapId : new int[] {532, 540, 564, 573, 574}) {
            ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(mapId);
            for (int index = 0; index < 8; index++) {
                ArenaMapCatalog.Point spawn = arena.spawn(index);
                assertTrue(arena.nativeStandingFloor(spawn.x(), spawn.y()),
                        () -> "non-floor spawn " + mapId + ":" + spawn);
                assertTrue(hasNativeExit(arena, spawn),
                        () -> "sealed spawn " + mapId + ":" + spawn);
            }
            ArenaMapCatalog.Point feature = arena.featurePoint();
            assertTrue(arena.nativeStandingFloor(feature.x(), feature.y()),
                    () -> "non-floor feature " + mapId + ":" + feature);
        }
    }

    private static boolean hasNativeExit(ArenaMapCatalog.Arena arena,
                                         ArenaMapCatalog.Point spawn) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if ((dx != 0 || dy != 0) && arena.nativeCanStep(
                        spawn.x(), spawn.y(), spawn.x() + dx, spawn.y() + dy)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    void pinsNativeEightDirectionMasksFromRealMap573Layer3() {
        ArenaPassabilityCatalog catalog = ArenaPassabilityCatalog.get();
        assertEquals(0x00, openMask(catalog, 573, 32704, 32768));
        assertEquals(0x07, openMask(catalog, 573, 32720, 32870));
        assertEquals(0x10, openMask(catalog, 573, 32751, 32817));
        assertEquals(0x7F, openMask(catalog, 573, 32723, 32863));
        assertEquals(0xFE, openMask(catalog, 573, 32726, 32788));
        assertEquals(0xFF, openMask(catalog, 573, 32721, 32864));
    }

    @Test
    void compatibilityServicesNeverSealAnyNativePlayerSpawn() {
        for (int mapId : ArenaMapCatalog.mapIds()) {
            ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(mapId);
            Set<ArenaMapCatalog.Point> services = new HashSet<>();
            for (int slot = 0; slot < 8; slot++) {
                ArenaMapCatalog.Point point = ArenaMapCatalog.serviceSpawn(arena, slot);
                assertNotNull(point, () -> "missing service floor " + mapId);
                assertTrue(services.add(point),
                        () -> "stacked service floor " + mapId + ":" + point);
                assertTrue(arena.nativeStandingFloor(point.x(), point.y()),
                        () -> "blocked service floor " + mapId + ":" + point);
                assertFalse(arena.spawns().contains(point),
                        () -> "service overlaps player spawn " + mapId + ":" + point);
            }
            ArenaMapCatalog.Point first = arena.spawn(0);
            assertTrue(services.stream().allMatch(point -> Math.max(
                            Math.abs(point.x() - first.x()),
                            Math.abs(point.y() - first.y())) >= 2),
                    () -> "service actor still encloses first spawn on map " + mapId);
            assertTrue(hasNativeExit(arena, first),
                    () -> "first player spawn has no native exit on map " + mapId);
        }
    }

    private static int openMask(ArenaPassabilityCatalog catalog,
                                int mapId, int x, int y) {
        int result = 0;
        for (int heading = 0; heading < 8; heading++) {
            if (!catalog.directionBlocked(mapId, x, y, heading)) {
                result |= 1 << heading;
            }
        }
        return result;
    }
}
