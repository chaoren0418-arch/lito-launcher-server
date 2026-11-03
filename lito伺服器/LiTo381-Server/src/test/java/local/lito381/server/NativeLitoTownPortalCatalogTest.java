package local.lito381.server;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NativeLitoTownPortalCatalogTest {
    @Test
    void exposesAllExactRentalVillageLayer7Routes() {
        assertEquals(56, NativeLitoTownPortalCatalog.routeCount());
        assertEquals(24, NativeLitoTownPortalCatalog.routesForMap(500).size());
        assertEquals(32, NativeLitoTownPortalCatalog.routesForMap(560).size());

        Map<Integer, Map<String, Integer>> slots = new HashMap<>();
        for (int mapId : List.of(500, 560)) {
            NativeLitoTownCatalog.Town town = NativeLitoTownCatalog.forMap(mapId);
            Map<String, Integer> counts = new HashMap<>();
            for (NativeLitoTownPortalCatalog.Route route
                    : NativeLitoTownPortalCatalog.routesForMap(mapId)) {
                assertEquals(mapId, route.sourceMapId());
                assertEquals(mapId, route.targetMapId());
                assertEquals(0x64, route.rawTag());
                assertTrue(town.containsResourceCoordinate(
                        route.source().x(), route.source().y()));
                assertTrue(town.containsResourceCoordinate(
                        route.target().x(), route.target().y()));
                counts.merge(route.slotName(), 1, Integer::sum);

                NativeLitoTownPortalCatalog.Route reverse =
                        NativeLitoTownPortalCatalog.find(mapId,
                                route.target().x(), route.target().y());
                assertNotNull(reverse);
                assertEquals(route.source(), reverse.target());
                assertEquals(route.slotName(), reverse.slotName());
            }
            slots.put(mapId, counts);
        }

        assertEquals(12, slots.get(500).size());
        assertEquals(16, slots.get(560).size());
        assertTrue(slots.values().stream().flatMap(map -> map.values().stream())
                .allMatch(count -> count == 2));
    }

    @Test
    void keepsTownRoutesSeparateFromBattleArenaRoutes() {
        NativeLitoTownPortalCatalog.Route lodun =
                NativeLitoTownPortalCatalog.find(500, 32788, 32719);
        assertNotNull(lodun);
        assertEquals("1", lodun.slotName());
        assertEquals(new NativeLitoTownPortalCatalog.Point(32771, 32824), lodun.target());
        assertNull(ArenaPortalCatalog.find(500, 32788, 32719));

        NativeLitoTownPortalCatalog.Route lenoa =
                NativeLitoTownPortalCatalog.find(560, 32251, 32799);
        assertNotNull(lenoa);
        assertEquals("p", lenoa.slotName());
        assertEquals(new NativeLitoTownPortalCatalog.Point(32306, 32796), lenoa.target());

        assertNull(NativeLitoTownPortalCatalog.find(500, 32788, 32720));
        assertNull(NativeLitoTownPortalCatalog.find(525, 32722, 32924));
    }
}
