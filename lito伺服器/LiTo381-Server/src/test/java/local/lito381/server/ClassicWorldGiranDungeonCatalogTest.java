package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class ClassicWorldGiranDungeonCatalogTest {
    private static final int[][] ROUTES = {
            {233,4,33311,33061,53,32810,32731,6},
            {234,53,32732,32787,54,32810,32794,6},
            {235,53,32811,32730,4,33312,33061,2},
            {236,53,32811,32731,4,33312,33061,2},
            {237,54,32728,32811,55,32735,32728,4},
            {238,54,32735,32726,54,32750,32774,0},
            {239,54,32750,32775,54,32736,32726,2},
            {240,54,32811,32793,53,32732,32786,0},
            {241,54,32811,32794,53,32732,32786,0},
            {242,55,32735,32727,54,32728,32810,0},
            {243,55,32736,32727,54,32728,32810,0},
            {244,55,32736,32855,56,32769,32730,4},
            {245,56,32769,32729,55,32736,32854,0},
            {246,56,32770,32729,55,32736,32854,0}
    };

    @Test
    void fourMapsPinExactCollisionNpcMonsterAndDropResources() throws Exception {
        int[][] bounds = {
                {32704,32831,32704,32831,128,128},
                {32704,32831,32704,32831,128,128},
                {32704,32767,32704,32895,64,192},
                {32704,32831,32704,32831,128,128}
        };
        int[] monsterRows = {4, 3, 3, 4};
        int[] populations = {65, 60, 60, 80};
        int[] dropRows = {1, 0, 0, 0};
        int[] itemRows = {1, 0, 0, 0};
        String[] monsterResources = {
                "625AADDE9CBF035AB164C022DD3F58EDC9FEB6A883B97AEECD4B214B7F871A6C",
                "AC5EB44B268B8BB6DBCFAF38E4DB08D106ADF6D088ADD465183E9C4275B1285E",
                "ED9515304CA0FC7771D412667358B8BF2EE682C35CF5034D2BFE304B5D09A9CE",
                "DB7D840568D66DF75935D24530B403C0005ED042E4F3CB6D54FB5478DDFF5BC0"
        };
        String[] dropResources = {
                "5F0B24904E3079293B891E51F61187F6F0ABAD17C7A55A929423D1C19F5F3794",
                "D692779E8D5E143D537E617891016575101EEC24CCA280FC9BDE401D987E3554",
                "1A896B6DF4F26B7615139019DE292F7E6D875E81CF29CDD0BF1D60CAD706858D",
                "30176949B45433F84C1F2202768ED75EA860CC393291EECC2937AD3B73167965"
        };
        for (int index = 0; index < 4; index++) {
            int mapId = index + 53;
            ClassicWorldMapCatalog.ClassicMap map =
                    ClassicWorldMapCatalog.forMap(mapId);
            assertEquals(bounds[index][0], map.startX());
            assertEquals(bounds[index][1], map.endX());
            assertEquals(bounds[index][2], map.startY());
            assertEquals(bounds[index][3], map.endY());
            assertEquals(bounds[index][4], map.width());
            assertEquals(bounds[index][5], map.height());
            assertEquals(ClassicWorldMapCatalog.GIRAN_DUNGEON_MAP_SOURCE_SHA256
                    .get(mapId), map.sourceSha256());
            assertTrue(map.allowsEscape());
            assertTrue(map.allowsPetRecall());
            assertTrue(map.allowsRandomTeleport());
            assertResource("/maps/" + mapId + ".l38.gz",
                    ClassicWorldMapCatalog.GIRAN_DUNGEON_MAP_RESOURCE_SHA256
                            .get(mapId));
            assertResource("/world/map" + mapId + "-static-npcs.lwn.gz",
                    ClassicWorldNpcCatalog.GIRAN_DUNGEON_RESOURCE_SHA256.get(mapId));
            assertResource("/world/map" + mapId + "-monsters.lwm.gz",
                    monsterResources[index]);
            assertResource("/world/map" + mapId + "-drops.lwd.gz",
                    dropResources[index]);

            assertTrue(ClassicWorldNpcCatalog.all(mapId).isEmpty());
            List<ClassicWorldMonsterCatalog.SpawnDefinition> definitions =
                    ClassicWorldMonsterCatalog.definitions(mapId);
            assertEquals(monsterRows[index], definitions.size());
            assertEquals(populations[index], definitions.stream().mapToInt(
                    ClassicWorldMonsterCatalog.SpawnDefinition::count).sum());
            assertEquals(dropRows[index], ClassicWorldDropCatalog.dropCount(mapId));
            assertEquals(itemRows[index], ClassicWorldDropCatalog.items(mapId).size());
        }
        assertEquals(265, ClassicWorldMapCatalog.GIRAN_DUNGEON_MAP_IDS.stream()
                .flatMap(mapId -> ClassicWorldMonsterCatalog.definitions(mapId).stream())
                .mapToInt(ClassicWorldMonsterCatalog.SpawnDefinition::count).sum());
    }

    @Test
    void allFourteenRoutesCloseTheMap4ToMap56Component() {
        assertEquals("49CDA1675A50DB92877951C72937188A2CEFD50655A4F4C089165F2544858BB9",
                ClassicWorldDungeonCatalog.GIRAN_DUNGEON_SOURCE_SHA256);
        assertEquals(ClassicWorldDungeonCatalog.GIRAN_DUNGEON_SOURCE_SHA256,
                ClassicWorldDungeonCatalog.GIRAN_DUNGEON_SECONDARY_SOURCE_SHA256);
        assertEquals(java.util.stream.IntStream.rangeClosed(233, 246)
                        .boxed().toList(),
                ClassicWorldDungeonCatalog.all().stream()
                        .filter(route -> route.routeId() >= 233
                                && route.routeId() <= 246)
                        .map(ClassicWorldDungeonCatalog.Route::routeId).toList());
        for (int[] expected : ROUTES) {
            ClassicWorldDungeonCatalog.Route route = ClassicWorldDungeonCatalog.find(
                    expected[1], expected[2], expected[3]);
            assertNotNull(route, "route " + expected[0]);
            assertEquals(expected[0], route.routeId());
            assertEquals(expected[4], route.destinationMapId());
            assertEquals(expected[5], route.destinationX());
            assertEquals(expected[6], route.destinationY());
            assertEquals(expected[7], route.destinationHeading());
            assertEquals(0, route.requiredItemCatalogId());
            assertFalse(route.scheduledPirateShip());
            // The native client may request the portal tile from an adjacent
            // standing cell; route 233's map4 source tile is intentionally solid.
            assertTrue(ClassicWorldMapCatalog.forMap(route.sourceMapId())
                    .contains(route.sourceX(), route.sourceY()),
                    "source route " + route.routeId());
            assertTrue(ClassicWorldMapCatalog.forMap(route.destinationMapId())
                    .standingPassable(route.destinationX(), route.destinationY()),
                    "destination route " + route.routeId());
        }
    }

    @Test
    void flagsAndGetbackRowsRemainHashLocked() {
        assertEquals("CC51597B17D7603875D282BD74F0ABB775A51531A739FA4F8EB1D9C51E596ECD",
                ClassicWorldMapCatalog.GIRAN_DUNGEON_FLAGS_SOURCE_SHA256);
        assertEquals("D4FCB89DE78D4473DCA5B1F6C54B6F4B61C9C92D4B6B45756DF29A8DEFE47E39",
                ClassicWorldMapCatalog.GIRAN_DUNGEON_FLAGS_SECONDARY_SOURCE_SHA256);
        assertEquals("6CDB4C1182BE8234177A69D0FA82628CC5E80E68272768957E3C9F315C23C52E",
                ClassicWorldGetbackCatalog.GIRAN_DUNGEON_SOURCE_SHA256);
        Set<List<Integer>> townSeven = Set.of(
                List.of(33435,32803), List.of(33439,32817),
                List.of(33440,32809), List.of(33419,32810),
                List.of(33426,32823), List.of(33418,32818),
                List.of(33432,32824));
        for (int mapId : ClassicWorldMapCatalog.GIRAN_DUNGEON_MAP_IDS) {
            for (int selector = 0; selector < townSeven.size(); selector++) {
                ClassicWorldGetbackCatalog.Destination destination =
                        ClassicWorldGetbackCatalog.resolve(
                                mapId, 32768, 32768, 0, selector);
                assertEquals(4, destination.mapId());
                assertEquals(7, destination.townId());
                assertTrue(townSeven.contains(
                        List.of(destination.x(), destination.y())));
            }
        }
    }

    @Test
    void populationAppendsAll265ActorsAfterEveryPointSevenThreeFiveObject() {
        ClassicWorldMonsterPopulation population = new ClassicWorldMonsterPopulation();
        assertEquals(ClassicWorldMonsterCatalog.TOTAL_POPULATION, population.size());
        assertEquals(ClassicWorldMonsterCatalog.PRE_335_TOTAL_POPULATION + 375,
                ClassicWorldMonsterCatalog.PRE_336_TOTAL_POPULATION);
        assertEquals(ClassicWorldMonsterCatalog.PRE_336_TOTAL_POPULATION + 265
                        + ClassicWorldMonsterCatalog.WINDAWOOD_CASTLE_POPULATION
                        + ClassicWorldMonsterCatalog.KENT_COMPONENT_POPULATION
                        + ClassicWorldMonsterCatalog.IVORY_TOWER_POPULATION
                        + ClassicWorldMonsterCatalog.ADEN_TOI_POPULATION
                        + ClassicWorldMonsterCatalog.HEINE_EVA_POPULATION
                        + ClassicWorldMonsterCatalog.SILENT_CAVERN_POPULATION
                        + ClassicWorldMonsterCatalog.RASTABAD_OUM_POPULATION
                        + ClassicWorldMonsterCatalog.DIADRAD_SERVICE_CORE_POPULATION
                        + ClassicWorldMonsterCatalog.DEMON_TEMPLE_POPULATION
                        + ClassicWorldMonsterCatalog.ELEMENTAL_GRAVE_SLICE_POPULATION
                        + ClassicWorldMonsterCatalog.PIRATE_DUNGEON_POPULATION
                        + ClassicWorldMonsterCatalog.PIRATE_BACK_HALF_POPULATION
                        + ClassicWorldMonsterCatalog.DESIRE_CAVE_POPULATION
                        + ClassicWorldMonsterCatalog.MAP666_POPULATION,
                population.size());
        long objectId = ClassicWorldMonsterPopulation.OBJECT_BASE
                | (ClassicWorldMonsterCatalog.PRE_336_TOTAL_POPULATION + 1);
        int observed = 0;
        for (int mapId : ClassicWorldMapCatalog.GIRAN_DUNGEON_MAP_IDS) {
            for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                    : ClassicWorldMonsterCatalog.definitions(mapId)) {
                for (int slot = 1; slot <= definition.count(); slot++) {
                    ClassicWorldMonsterPopulation.Snapshot actor =
                            population.actor(mapId, objectId++);
                    assertNotNull(actor, "map=" + mapId + " spawn="
                            + definition.spawnId() + " slot=" + slot);
                    assertEquals(definition.spawnId(), actor.spawnId());
                    assertEquals(slot, actor.spawnNumber());
                    assertTrue(ClassicWorldMapCatalog.forMap(mapId)
                            .standingPassable(actor.x(), actor.y()));
                    observed++;
                }
            }
        }
        assertEquals(265, observed);
    }

    private void assertResource(String path, String expectedSha256) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream(path)) {
            assertNotNull(in, path);
            digest.update(in.readAllBytes());
        }
        assertEquals(expectedSha256,
                HexFormat.of().withUpperCase().formatHex(digest.digest()), path);
    }
}
