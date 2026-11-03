package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ClassicWorldRastabadOumCatalogTest {
    @Test
    void fourMapsAndEightSafeRoutesAppendAfterImmutable344Ids() {
        assertEquals(List.of(307, 308, 309, 310),
                ClassicWorldRastabadOumCatalog.MAP_IDS);
        for (int mapId : ClassicWorldRastabadOumCatalog.MAP_IDS) {
            ClassicWorldRastabadOumCatalog.MapFlags flags =
                    ClassicWorldRastabadOumCatalog.flags(mapId);
            ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(mapId);
            assertEquals(ClassicWorldRastabadOumCatalog.MAP_SOURCE_SHA256.get(mapId),
                    map.sourceSha256());
            assertEquals(List.of(flags.startX(), flags.endX(), flags.startY(), flags.endY()),
                    List.of(map.startX(), map.endX(), map.startY(), map.endY()));
            assertEquals(flags.escapable(), map.allowsEscape());
            assertEquals(flags.teleportable(), map.allowsRandomTeleport());
            assertEquals(flags.recallPets(), map.allowsPetRecall());
        }
        assertEquals(List.of(256, 192), dimensions(307));
        assertEquals(List.of(320, 192), dimensions(308));
        assertEquals(List.of(384, 192), dimensions(309));
        assertEquals(List.of(256, 256), dimensions(310));

        assertEquals(ClassicWorldDungeonCatalog.TOTAL_ROUTE_COUNT,
                ClassicWorldDungeonCatalog.all().size());
        assertEquals(486, ClassicWorldDungeonCatalog.PRE_378_ROUTE_COUNT);
        List<ClassicWorldDungeonCatalog.Route> routes =
                ClassicWorldDungeonCatalog.all().stream()
                        .filter(ClassicWorldDungeonCatalog::isRastabadOumRoute).toList();
        assertEquals(List.of(406, 407, 408, 409, 410, 411, 412, 413),
                routes.stream().map(ClassicWorldDungeonCatalog.Route::routeId).toList());
        assertTrue(routes.stream().allMatch(
                ClassicWorldDungeonCatalog::destinationLandingSafe));

        // The .346 service core now owns the two previously staged boundaries.
        assertEquals(414, ClassicWorldDungeonCatalog.find(
                309, 33060, 32882).routeId());
        assertEquals(415, ClassicWorldDungeonCatalog.find(
                310, 32785, 32939).routeId());
        assertTrue(ClassicWorldMapCatalog.supports(320));
        assertTrue(ClassicWorldMapCatalog.supports(401));
    }

    @Test
    void selectedRuntimeResourcesRetainExactRowsAndAppend721Actors() {
        int population = 0;
        for (int mapId : ClassicWorldRastabadOumCatalog.MAP_IDS) {
            ClassicWorldRastabadOumCatalog.SourceContract npc =
                    ClassicWorldRastabadOumCatalog.NPC_CONTRACTS.get(mapId);
            ClassicWorldRastabadOumCatalog.SourceContract monster =
                    ClassicWorldRastabadOumCatalog.MONSTER_CONTRACTS.get(mapId);
            ClassicWorldRastabadOumCatalog.SourceContract drop =
                    ClassicWorldRastabadOumCatalog.DROP_CONTRACTS.get(mapId);
            assertEquals(npc.rows(), ClassicWorldNpcCatalog.all(mapId).size());
            assertEquals(monster.rows(), ClassicWorldMonsterCatalog.definitions(mapId).size());
            assertEquals(drop.rows(), ClassicWorldDropCatalog.dropCount(mapId));
            population += monster.population();
        }
        assertEquals(721, population);
        assertEquals(ClassicWorldMonsterCatalog.RASTABAD_OUM_POPULATION, population);
        assertNull(ClassicWorldNpcCatalog.npc(310, ClassicWorldNpcCatalog.objectId(
                ClassicWorldRastabadOumCatalog.EXCLUDED_CUSTOM_SPAWN_ID)));

        ClassicWorldMonsterPopulation actors = new ClassicWorldMonsterPopulation();
        assertEquals(ClassicWorldMonsterCatalog.TOTAL_POPULATION, actors.size());
        long objectId = ClassicWorldMonsterPopulation.OBJECT_BASE
                | (ClassicWorldMonsterCatalog.PRE_378_TOTAL_POPULATION
                    - ClassicWorldMonsterCatalog.PIRATE_DUNGEON_POPULATION
                    - ClassicWorldMonsterCatalog.ELEMENTAL_GRAVE_SLICE_POPULATION
                    - ClassicWorldMonsterCatalog.DEMON_TEMPLE_POPULATION
                    - ClassicWorldMonsterCatalog.DIADRAD_SERVICE_CORE_POPULATION
                    - ClassicWorldMonsterCatalog.RASTABAD_OUM_POPULATION + 1);
        int observed = 0;
        for (int mapId : ClassicWorldRastabadOumCatalog.MAP_IDS) {
            for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                    : ClassicWorldMonsterCatalog.definitions(mapId)) {
                for (int slot = 1; slot <= definition.count(); slot++) {
                    ClassicWorldMonsterPopulation.Snapshot actor =
                            actors.actor(mapId, objectId++);
                    assertNotNull(actor);
                    assertEquals(definition.spawnId(), actor.spawnId());
                    assertEquals(slot, actor.spawnNumber());
                    assertTrue(ClassicWorldMapCatalog.forMap(mapId)
                            .standingPassable(actor.x(), actor.y()));
                    observed++;
                }
            }
        }
        assertEquals(721, observed);
    }

    @Test
    void oversubscribedNativeTwoByTwoSpawnKeepsSharedCellOccupied() {
        ClassicWorldMonsterPopulation actors = new ClassicWorldMonsterPopulation();
        long first = ClassicWorldMonsterPopulation.OBJECT_BASE
                | (ClassicWorldMonsterCatalog.PRE_378_TOTAL_POPULATION
                    - ClassicWorldMonsterCatalog.PIRATE_DUNGEON_POPULATION
                    - ClassicWorldMonsterCatalog.ELEMENTAL_GRAVE_SLICE_POPULATION
                    - ClassicWorldMonsterCatalog.DIADRAD_SERVICE_CORE_POPULATION
                    - ClassicWorldMonsterCatalog.RASTABAD_OUM_POPULATION + 1);
        long last = ClassicWorldMonsterPopulation.OBJECT_BASE
                | (ClassicWorldMonsterCatalog.PRE_378_TOTAL_POPULATION
                    - ClassicWorldMonsterCatalog.PIRATE_DUNGEON_POPULATION
                    - ClassicWorldMonsterCatalog.ELEMENTAL_GRAVE_SLICE_POPULATION
                    - ClassicWorldMonsterCatalog.DIADRAD_SERVICE_CORE_POPULATION);
        List<ClassicWorldMonsterPopulation.Snapshot> group =
                java.util.stream.LongStream.rangeClosed(first, last)
                        .mapToObj(objectId -> actors.actor(309, objectId))
                        .filter(java.util.Objects::nonNull)
                        .filter(actor -> actor.spawnId() == 2121)
                        .toList();
        assertEquals(5, group.size());
        ClassicWorldMonsterPopulation.Snapshot shared = null;
        for (int left = 0; left < group.size() && shared == null; left++) {
            for (int right = left + 1; right < group.size(); right++) {
                if (group.get(left).x() == group.get(right).x()
                        && group.get(left).y() == group.get(right).y()) {
                    shared = group.get(left);
                    break;
                }
            }
        }
        assertNotNull(shared);
        assertTrue(actors.occupies(309, shared.x(), shared.y()));
        assertTrue(actors.damage(309, shared.objectId(), shared.maximumHp()).diedNow());
        assertTrue(actors.occupies(309, shared.x(), shared.y()));
    }

    @Test
    void textBackedTeleporterWarehouseAndMerchantMatchExactSpawns() {
        ClassicWorldNpcCatalog.StaticNpc teleporter = npc(
                ClassicWorldRastabadOumCatalog.TELEPORTER_SPAWN_ID);
        assertTrue(ClassicWorldRastabadOumCatalog.isTeleporter(teleporter));
        assertEquals(List.of(940, 941, 942, 943, 944, 946, 947, 948, 949),
                ClassicWorldTeleportCatalog.rastabadOum().stream()
                        .map(ClassicWorldTeleportCatalog.Route::routeId).toList());
        assertEquals("teleoum2", ClassicWorldTeleportCatalog.followUpHtml(
                310, teleporter.objectId(), "teleportURL"));
        assertEquals(List.of("2200", "1800", "1000", "1600", "2200", "1200",
                        "1300", "2000", "2000"),
                ClassicWorldTeleportCatalog.followUpArguments(
                        310, teleporter.objectId(), "teleportURL"));
        assertEquals(304, ClassicWorldTeleportCatalog.route(
                310, teleporter.objectId(), "teleport oum-scave").destinationMapId());

        ClassicWorldNpcCatalog.StaticNpc warehouse = npc(
                ClassicWorldRastabadOumCatalog.WAREHOUSE_SPAWN_ID);
        assertTrue(ClassicWorldRastabadOumCatalog.isWarehouse(warehouse));
        assertEquals(warehouse, ClassicWorldNpcCatalog.fixedWarehouse(
                310, warehouse.objectId()));
        assertEquals("gawl", warehouse.initialHtml(0));
        assertEquals("gawll", warehouse.initialHtml(-1001));

        ClassicWorldNpcCatalog.StaticNpc karupu = npc(
                ClassicWorldRastabadOumCatalog.KARUPU_SPAWN_ID);
        assertTrue(ClassicWorldRastabadOumCatalog.isKarupu(karupu));
        ClassicWorldMerchantCatalog.Merchant merchant =
                ClassicWorldMerchantCatalog.find(karupu);
        assertNotNull(merchant);
        assertEquals(24, merchant.offers().size());
        assertTrue(merchant.offers().stream().allMatch(offer -> offer.packCount() > 0));
        assertTrue(ClassicWorldMerchantCatalog.supportsEmptySell(merchant));
    }

    @Test
    void map310EscapeUsesExactRetainedTown14Points() {
        List<List<Integer>> expected = List.of(
                List.of(32818, 32805), List.of(32800, 32798),
                List.of(32815, 32819), List.of(32823, 32811),
                List.of(32817, 32828));
        for (int selector = 0; selector < expected.size(); selector++) {
            ClassicWorldGetbackCatalog.Destination destination =
                    ClassicWorldGetbackCatalog.resolve(310, 32780, 32800, 0, selector);
            assertEquals(310, destination.mapId());
            assertEquals(expected.get(selector), List.of(destination.x(), destination.y()));
            assertEquals(14, destination.townId());
        }
        for (int mapId : List.of(307, 308, 309)) {
            assertEquals(new ClassicWorldGetbackCatalog.Destination(4, 33089, 33397, 2),
                    ClassicWorldGetbackCatalog.resolve(mapId, 32780, 32800, 0, 0));
        }
    }

    private static ClassicWorldNpcCatalog.StaticNpc npc(int spawnId) {
        return ClassicWorldNpcCatalog.npc(310, ClassicWorldNpcCatalog.objectId(spawnId));
    }

    private static List<Integer> dimensions(int mapId) {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(mapId);
        return List.of(map.width(), map.height());
    }
}
