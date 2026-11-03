package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ClassicWorldLastabadGateCatalogTest {
    @Test
    void selectedEmptyGateAppendsAfterImmutable360MapsAndKeepsFloorOneClosed() {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(450);
        assertEquals(ClassicWorldLastabadGateCatalog.MAP_SOURCE_SHA256,
                map.sourceSha256());
        assertEquals(List.of(32576, 32831, 32768, 32959), List.of(
                map.startX(), map.endX(), map.startY(), map.endY()));
        assertEquals(List.of(256, 192), List.of(map.width(), map.height()));
        assertTrue(map.allowsEscape());
        assertTrue(map.allowsPetRecall());
        assertFalse(map.allowsRandomTeleport());

        List<ClassicWorldDungeonCatalog.Route> routes =
                ClassicWorldDungeonCatalog.all().stream()
                        .filter(ClassicWorldDungeonCatalog::isLastabadGateRoute).toList();
        assertEquals(ClassicWorldDungeonCatalog.TOTAL_ROUTE_COUNT,
                ClassicWorldDungeonCatalog.all().size());
        assertEquals(486, ClassicWorldDungeonCatalog.PRE_378_ROUTE_COUNT);
        assertEquals(List.of(453, 454, 455, 456, 457, 458), routes.stream()
                .map(ClassicWorldDungeonCatalog.Route::routeId).toList());
        assertEquals(List.of(400, 33005, 32762, 450, 32656, 32852, 2),
                wire(routes.get(0)));
        assertEquals(List.of(400, 33005, 32764, 450, 32656, 32854, 2),
                wire(routes.get(2)));
        assertEquals(List.of(450, 32655, 32852, 400, 33004, 32762, 6),
                wire(routes.get(3)));
        assertEquals(List.of(450, 32655, 32854, 400, 33004, 32764, 6),
                wire(routes.get(5)));
        assertTrue(routes.stream().allMatch(
                ClassicWorldDungeonCatalog::destinationLandingSafe));

        // These retained DB rows are stale rather than merely waiting for map451:
        // selected server collision is raw zero, while the independently parsed
        // selected-client Layer3 authority blocks all headings and places the
        // nearest standing cells 58-60 tiles away.
        assertEquals("B5B6E360E5DF950440A085F31740109C6E7B12B7A13FA67AF568F3C597AD9173",
                ClassicWorldLastabadGateCatalog.MAP451_COLLISION_AUTHORITY_SHA256);
        for (int y = 32848; y <= 32850; y++) {
            assertEquals(0, map.rawCell(32770, y));
            assertFalse(map.standingPassable(32770, y));
            assertNull(ClassicWorldDungeonCatalog.find(450, 32770, y));
            for (int x = 32767; x <= 32773; x++) {
                for (int nearY = y - 3; nearY <= y + 3; nearY++) {
                    assertFalse(map.standingPassable(x, nearY));
                }
            }
        }
    }

    @Test
    void selectedAuthorityIsExplicitlyEmptyWhileGetbackStaysInsideTheGate() {
        assertTrue(ClassicWorldNpcCatalog.all(450).isEmpty());
        assertTrue(ClassicWorldMonsterCatalog.definitions(450).isEmpty());
        assertEquals(0, ClassicWorldDropCatalog.dropCount(450));
        assertTrue(ClassicWorldDropCatalog.items(450).isEmpty());

        assertEquals(new ClassicWorldGetbackCatalog.Destination(450, 32656, 32853, 0),
                ClassicWorldGetbackCatalog.resolve(450, 32700, 32850, 0, 0));
        assertEquals(new ClassicWorldGetbackCatalog.Destination(450, 32656, 32852, 0),
                ClassicWorldGetbackCatalog.resolve(450, 32700, 32850, 0, 1));
        assertEquals(new ClassicWorldGetbackCatalog.Destination(450, 32656, 32854, 0),
                ClassicWorldGetbackCatalog.resolve(450, 32700, 32850, 0, 2));
    }

    private static List<Integer> wire(ClassicWorldDungeonCatalog.Route route) {
        return List.of(route.sourceMapId(), route.sourceX(), route.sourceY(),
                route.destinationMapId(), route.destinationX(), route.destinationY(),
                route.destinationHeading());
    }
}
