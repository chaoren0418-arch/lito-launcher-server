package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ClassicWorldUnderseaTunnelCatalogTest {
    @Test
    void selectedMap13CollisionStaticMonsterAndDropResourcesAreHashLocked()
            throws Exception {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(13);
        assertEquals(List.of(32704, 32831, 32704, 32831),
                List.of(map.startX(), map.endX(), map.startY(), map.endY()));
        assertEquals(128, map.width());
        assertEquals(128, map.height());
        assertEquals(ClassicWorldMapCatalog.MAP13_SOURCE_SHA256,
                map.sourceSha256());
        assertTrue(map.allowsEscape());
        assertTrue(map.allowsPetRecall());

        List<ClassicWorldNpcCatalog.StaticNpc> npcs = ClassicWorldNpcCatalog.all(13);
        assertEquals(1, npcs.size());
        ClassicWorldNpcCatalog.StaticNpc orim = npcs.get(0);
        assertEquals(1309, orim.spawnId());
        assertEquals(70017, orim.npcId());
        assertEquals(32814, orim.x());
        assertEquals(32803, orim.y());
        assertEquals(6, orim.heading());
        assertEquals("$406", orim.nameId());
        assertEquals("L1Merchant", orim.implementation());
        assertEquals(266, orim.gfxId());
        assertEquals("orim1", orim.normalAction());
        assertTrue(orim.fixedMerchant());

        assertEquals(4, ClassicWorldMonsterCatalog.definitions(13).size());
        assertEquals(45, ClassicWorldMonsterCatalog.definitions(13).stream()
                .mapToInt(ClassicWorldMonsterCatalog.SpawnDefinition::count).sum());
        assertEquals(List.of(45060, 45161, 45241, 45298),
                ClassicWorldMonsterCatalog.definitions(13).stream()
                        .map(row -> row.template().npcId()).toList());
        assertEquals(43, ClassicWorldDropCatalog.dropCount(13));
        assertEquals(38, ClassicWorldDropCatalog.items(13).size());

        assertResource("/maps/13.l38.gz", ClassicWorldMapCatalog.MAP13_RESOURCE_SHA256);
        assertResource("/world/map13-static-npcs.lwn.gz",
                ClassicWorldNpcCatalog.MAP13_RESOURCE_SHA256);
        assertResource("/world/map13-monsters.lwm.gz",
                ClassicWorldMonsterCatalog.MAP13_RESOURCE_SHA256);
        assertResource("/world/map13-drops.lwd.gz",
                ClassicWorldDropCatalog.MAP13_RESOURCE_SHA256);
    }

    @Test
    void sixInternalRoutesAndMap14RoundTripMatchTheSelectedRows() {
        List<ClassicWorldDungeonCatalog.Route> routes =
                ClassicWorldDungeonCatalog.all().stream()
                        .filter(route -> route.routeId() >= 191
                                && route.routeId() <= 198).toList();
        assertEquals(List.of(191, 192, 193, 194, 195, 196, 197, 198),
                routes.stream().map(ClassicWorldDungeonCatalog.Route::routeId)
                        .toList());
        for (ClassicWorldDungeonCatalog.Route route : routes) {
            ClassicWorldMapCatalog.ClassicMap source =
                    ClassicWorldMapCatalog.forMap(route.sourceMapId());
            ClassicWorldMapCatalog.ClassicMap destination =
                    ClassicWorldMapCatalog.forMap(route.destinationMapId());
            assertTrue(source.standingPassable(route.sourceX(), route.sourceY()),
                    "source route " + route.routeId());
            assertTrue(destination.standingPassable(
                    route.destinationX(), route.destinationY()),
                    "destination route " + route.routeId());
            assertEquals(0, route.requiredItemCatalogId());
            assertFalse(route.scheduledPirateShip());
        }
        assertRoute(13, 32764, 32766, 13, 32780, 32741, 2);
        assertRoute(13, 32780, 32742, 13, 32765, 32766, 2);
        assertRoute(13, 32786, 32795, 13, 32811, 32729, 2);
        assertRoute(13, 32811, 32730, 13, 32787, 32795, 2);
        assertRoute(13, 32801, 32803, 13, 32808, 32802, 2);
        assertRoute(13, 32808, 32802, 13, 32801, 32803, 6);
        assertRoute(13, 32806, 32724, 14, 33000, 32808, 7);
        assertRoute(14, 33000, 32808, 13, 32807, 32724, 2);

        // The map12 boundary is closed by the independently hash-locked
        // Gludio Dungeon component; Map237 remains its own leaf pair.
        assertRoute(13, 32727, 32724, 12, 32770, 32810, 2);
    }

    @Test
    void selectedGetbackAndPopulationAppendPreserveEveryPointSevenThreeThreeId() {
        ClassicWorldGetbackCatalog.Destination getback =
                ClassicWorldGetbackCatalog.resolve(13, 32807, 32724, 0, 333);
        assertEquals(List.of(4, 32624, 32813, 3), List.of(
                getback.mapId(), getback.x(), getback.y(), getback.townId()));

        ClassicWorldMonsterPopulation population = new ClassicWorldMonsterPopulation();
        assertEquals(ClassicWorldMonsterCatalog.TOTAL_POPULATION, population.size());
        long first = ClassicWorldMonsterPopulation.OBJECT_BASE
                | (ClassicWorldMonsterCatalog.PRE_333_TOTAL_POPULATION + 1);
        int observed = 0;
        for (long objectId = first;
             objectId < first + ClassicWorldMonsterCatalog.MAP13_POPULATION;
             objectId++) {
            ClassicWorldMonsterPopulation.Snapshot actor =
                    population.actor(13, objectId);
            assertNotNull(actor, "map13 object " + objectId);
            assertTrue(ClassicWorldMapCatalog.forMap(13)
                    .standingPassable(actor.x(), actor.y()));
            observed++;
        }
        assertEquals(45, observed);

        long firstGolem = ClassicWorldMonsterPopulation.OBJECT_BASE
                | (ClassicWorldMonsterCatalog.PRE_333_TOTAL_POPULATION
                    - ClassicWorldRoyalTrialCatalog.STONE_GOLEM_POPULATION + 1);
        assertEquals(45126, population.actor(2, firstGolem).npcId());
    }

    private void assertRoute(int sourceMap, int sourceX, int sourceY,
                             int destinationMap, int destinationX,
                             int destinationY, int heading) {
        ClassicWorldDungeonCatalog.Route route =
                ClassicWorldDungeonCatalog.find(sourceMap, sourceX, sourceY);
        assertNotNull(route);
        assertEquals(destinationMap, route.destinationMapId());
        assertEquals(destinationX, route.destinationX());
        assertEquals(destinationY, route.destinationY());
        assertEquals(heading, route.destinationHeading());
    }

    private void assertResource(String name, String expected) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = getClass().getResourceAsStream(name)) {
            assertNotNull(input, name);
            digest.update(input.readAllBytes());
        }
        assertEquals(expected,
                HexFormat.of().withUpperCase().formatHex(digest.digest()), name);
    }
}
