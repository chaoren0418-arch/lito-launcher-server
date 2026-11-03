package local.lito381.server;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ArenaPortalCatalogTest {
    @Test
    void exposesEveryResourceDerivedRouteInsideNativeSectors() {
        assertEquals(122, ArenaPortalCatalog.routeCount());
        int tagged = 0;
        int tag64 = 0;
        int tag65 = 0;
        int tag66 = 0;
        for (int mapId : List.of(525, 532, 535, 547, 548, 549, 550, 554,
                562, 563, 573, 574)) {
            ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(mapId);
            for (ArenaPortalCatalog.Route route : ArenaPortalCatalog.routesForMap(mapId)) {
                assertEquals(mapId, route.sourceMapId());
                assertTrue(arena.contains(route.source().x(), route.source().y()));
                ArenaMapCatalog.Arena target = ArenaMapCatalog.forMap(route.targetMapId());
                assertTrue(target.contains(route.target().x(), route.target().y()));
                if (route.tagged()) tagged++;
                if (route.rawTag() == 0x64) tag64++;
                if (route.rawTag() == 0x65) tag65++;
                if (route.rawTag() == 0x66) tag66++;
            }
        }
        assertEquals(80, tagged);
        assertEquals(4, tag64);
        assertEquals(38, tag65);
        assertEquals(38, tag66);
    }

    @Test
    void preservesExactPairedAndOneWayRoutes() {
        ArenaPortalCatalog.Route red = ArenaPortalCatalog.find(573, 32755, 32792);
        assertEquals(new ArenaMapCatalog.Point(32742, 32797), red.target());
        assertEquals(0x65, red.rawTag());
        ArenaPortalCatalog.Route redReturn = ArenaPortalCatalog.find(573, 32742, 32797);
        assertEquals(red.source(), redReturn.target());

        ArenaPortalCatalog.Route blue = ArenaPortalCatalog.find(574, 32755, 32792);
        assertEquals(0x66, blue.rawTag());
        ArenaPortalCatalog.Route death = ArenaPortalCatalog.find(525, 32722, 32924);
        assertEquals(new ArenaMapCatalog.Point(32803, 32806), death.target());
        assertFalse(death.tagged());

        ArenaPortalCatalog.Route quest = ArenaPortalCatalog.find(554, 32746, 32926);
        assertEquals(new ArenaMapCatalog.Point(32742, 32888), quest.target());
        assertNull(ArenaPortalCatalog.find(554, 32742, 32888));
    }

    @Test
    void opaqueSuffixesAreSiegeResourceMetadataNotQuestGates() {
        List<ArenaPortalCatalog.Route> questRoutes = BattleRules.Mode.QUEST_BATTLE
                .maps().stream()
                .flatMap(mapId -> ArenaPortalCatalog.routesForMap(mapId).stream())
                .toList();
        assertEquals(4, questRoutes.size());
        assertTrue(questRoutes.stream().allMatch(route -> route.sourceMapId() == 554));
        assertTrue(questRoutes.stream().noneMatch(ArenaPortalCatalog.Route::tagged));

        // Ordinary Siege exposes the odd representatives 547/563/573, while
        // their even companions and the event-only 549/550 pair are retained
        // S32 resources of the same exact category.
        List<Integer> siegeResourceMaps = List.of(
                547, 548, 549, 550, 562, 563, 573, 574);
        List<ArenaPortalCatalog.Route> siegeRoutes = siegeResourceMaps.stream()
                .flatMap(mapId -> ArenaPortalCatalog.routesForMap(mapId).stream())
                .toList();
        assertEquals(96, siegeRoutes.size());
        assertEquals(80, siegeRoutes.stream()
                .filter(ArenaPortalCatalog.Route::tagged).count());
        assertTrue(siegeRoutes.stream().filter(ArenaPortalCatalog.Route::tagged)
                .allMatch(route -> siegeResourceMaps.contains(route.sourceMapId())));
    }

    @Test
    void rejectsCoordinatesThatAreNotNativeLayer7Sources() {
        assertTrue(ArenaPortalCatalog.routesForMap(510).isEmpty());
        assertNull(ArenaPortalCatalog.find(510, 32799, 32799));
        assertNull(ArenaPortalCatalog.find(525, 32721, 32924));
        assertNull(ArenaPortalCatalog.find(999, 32755, 32792));
    }
}
