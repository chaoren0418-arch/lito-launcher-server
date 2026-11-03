package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

final class ClassicWorldPirateBackHalfCatalogTest {
    @Test
    void allFourteenMapsAndPackedComponentsMatchSelectedAuthority()
            throws Exception {
        assertEquals(List.of(480, 481, 482, 483, 484,
                        550, 551, 552, 553, 554, 555, 556, 557, 558),
                ClassicWorldPirateBackHalfCatalog.MAP_IDS);
        for (int mapId : ClassicWorldPirateBackHalfCatalog.MAP_IDS) {
            ClassicWorldPirateBackHalfCatalog.MapFlags flags =
                    ClassicWorldPirateBackHalfCatalog.flags(mapId);
            ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(mapId);
            assertEquals(List.of(flags.startX(), flags.endX(), flags.startY(), flags.endY()),
                    List.of(map.startX(), map.endX(), map.startY(), map.endY()));
            assertEquals(ClassicWorldPirateBackHalfCatalog.MAP_SOURCE_SHA256.get(mapId),
                    map.sourceSha256());
            assertEquals(flags.teleportable(), map.allowsRandomTeleport());
            assertEquals(flags.recallPets(), map.allowsPetRecall());
            assertResource("/maps/" + mapId + ".l38.gz",
                    ClassicWorldPirateBackHalfCatalog.MAP_RESOURCE_SHA256.get(mapId));

            ClassicWorldPirateBackHalfCatalog.ComponentContract npc =
                    ClassicWorldPirateBackHalfCatalog.NPC_CONTRACTS.get(mapId);
            ClassicWorldPirateBackHalfCatalog.ComponentContract monster =
                    ClassicWorldPirateBackHalfCatalog.MONSTER_CONTRACTS.get(mapId);
            ClassicWorldPirateBackHalfCatalog.ComponentContract drop =
                    ClassicWorldPirateBackHalfCatalog.DROP_CONTRACTS.get(mapId);
            int reconstructedFrancu = mapId == 482 ? 1 : 0;
            assertEquals(npc.rows() + reconstructedFrancu,
                    ClassicWorldNpcCatalog.all(mapId).size());
            assertEquals(monster.rows(),
                    ClassicWorldMonsterCatalog.definitions(mapId).size());
            assertEquals(monster.secondaryCount(), population(mapId));
            assertEquals(drop.rows(), ClassicWorldDropCatalog.dropCount(mapId));
            assertEquals(drop.secondaryCount(), ClassicWorldDropCatalog.items(mapId).size());
            assertResource("/world/map" + mapId + "-static-npcs.lwn.gz",
                    npc.resourceSha256());
            assertResource("/world/map" + mapId + "-monsters.lwm.gz",
                    monster.resourceSha256());
            assertResource("/world/map" + mapId + "-drops.lwd.gz",
                    drop.resourceSha256());
        }
    }

    @Test
    void oneHundredTwentyTwoGroundRoutesAppendAfterFrozen377Prefix() {
        List<ClassicWorldDungeonCatalog.Route> routes =
                ClassicWorldDungeonCatalog.all().subList(
                        ClassicWorldDungeonCatalog.PRE_378_ROUTE_COUNT,
                        ClassicWorldDungeonCatalog.PRE_378_ROUTE_COUNT + 122);
        assertEquals(122, routes.size());
        assertEquals(IntStream.rangeClosed(487, 608).boxed().toList(),
                routes.stream().map(ClassicWorldDungeonCatalog.Route::routeId).toList());
        assertTrue(routes.stream().allMatch(
                ClassicWorldDungeonCatalog::isPirateBackHalfRoute));
        assertTrue(routes.stream().allMatch(
                ClassicWorldDungeonCatalog::destinationLandingSafe));
        assertEquals(List.of(570, 572, 580), routes.stream()
                .filter(ClassicWorldDungeonCatalog::isNativePortalOnlyLanding)
                .map(ClassicWorldDungeonCatalog.Route::routeId).toList());
        for (ClassicWorldDungeonCatalog.Route route : routes) {
            assertTrue(ClassicWorldMapCatalog.supports(route.sourceMapId()));
            assertTrue(ClassicWorldMapCatalog.forMap(route.sourceMapId())
                    .contains(route.sourceX(), route.sourceY()));
            assertTrue(ClassicWorldMapCatalog.forMap(route.destinationMapId())
                    .contains(route.destinationX(), route.destinationY()));
        }
    }

    @Test
    void getbackAndNpcTeleportEdgesCloseEveryImportedBoundary() {
        assertEquals(new ClassicWorldGetbackCatalog.Destination(440, 32431, 33058, 16),
                ClassicWorldGetbackCatalog.resolve(480, 32700, 32900, 0, 0));
        assertEquals(new ClassicWorldGetbackCatalog.Destination(440, 32407, 33054, 16),
                ClassicWorldGetbackCatalog.resolve(481, 32700, 32900, 0, 1));
        for (int mapId : List.of(482, 483, 484)) {
            assertEquals(new ClassicWorldGetbackCatalog.Destination(
                            440, 32451, 33058, 0),
                    ClassicWorldGetbackCatalog.resolve(mapId, 32700, 32800, 0, 9));
        }
        for (int mapId : ClassicWorldPirateBackHalfCatalog.SHIP_GRAVE_MAP_IDS) {
            assertEquals(new ClassicWorldGetbackCatalog.Destination(
                            550, 32844, 32694, 0),
                    ClassicWorldGetbackCatalog.resolve(mapId, 32700, 32800, 0, 9));
        }

        ClassicWorldTeleportCatalog.Route investigator =
                ClassicWorldTeleportCatalog.pirateBackHalfInvestigatorRoute();
        assertEquals(List.of(1134, 480, 50081, "B", 550, 32844, 32694, 5),
                List.of(investigator.routeId(), investigator.sourceMapId(),
                        investigator.sourceNpcId(), investigator.action(),
                        investigator.destinationMapId(), investigator.destinationX(),
                        investigator.destinationY(), investigator.heading()));
    }

    @Test
    void populationAppendsSevenHundredSeventyTwoStableActors() {
        ClassicWorldMonsterPopulation actors = new ClassicWorldMonsterPopulation();
        assertEquals(ClassicWorldMonsterCatalog.PRE_378_TOTAL_POPULATION + 772
                        + ClassicWorldMonsterCatalog.DESIRE_CAVE_POPULATION
                        + ClassicWorldMonsterCatalog.MAP666_POPULATION,
                actors.size());
        long objectId = ClassicWorldMonsterPopulation.OBJECT_BASE
                | (ClassicWorldMonsterCatalog.PRE_378_TOTAL_POPULATION + 1);
        int observed = 0;
        for (int mapId : ClassicWorldPirateBackHalfCatalog.MAP_IDS) {
            for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                    : ClassicWorldMonsterCatalog.definitions(mapId)) {
                for (int slot = 1; slot <= definition.count(); slot++) {
                    ClassicWorldMonsterPopulation.Snapshot actor =
                            actors.actor(mapId, objectId++);
                    assertNotNull(actor, "map=" + mapId + " spawn="
                            + definition.spawnId() + " slot=" + slot);
                    assertEquals(definition.spawnId(), actor.spawnId());
                    assertEquals(slot, actor.spawnNumber());
                    observed++;
                }
            }
        }
        assertEquals(772, observed);
        assertEquals((ClassicWorldMonsterPopulation.OBJECT_BASE
                | (ClassicWorldMonsterCatalog.TOTAL_POPULATION
                    - ClassicWorldMonsterCatalog.DESIRE_CAVE_POPULATION
                    - ClassicWorldMonsterCatalog.MAP666_POPULATION)) + 1,
                objectId);
    }

    @Test
    void shipGraveDialogueMerchantCraftAndWeakSoulAreRegistered() {
        List<ClassicWorldNpcCatalog.StaticNpc> surface = ClassicWorldNpcCatalog.all(550);
        assertEquals(List.of(80075, 80076, 80077),
                surface.stream().map(ClassicWorldNpcCatalog.StaticNpc::npcId).toList());
        ClassicWorldNpcCatalog.StaticNpc merchant = surface.get(0);
        assertTrue(ClassicWorldPirateShipGraveCatalog.isMerchant(merchant));
        assertEquals(18, ClassicWorldPirateShipGraveCatalog.offers().size());
        assertEquals(18, ClassicWorldMerchantCatalog.find(merchant).offers().size());

        assertEquals("voyager1", ClassicWorldPirateShipGraveCatalog
                .voyagerDialogue(Map.of()));
        assertEquals("voyager8", ClassicWorldPirateShipGraveCatalog
                .voyagerDialogue(Map.of(41058L, 1)));
        assertEquals("voyager7", ClassicWorldPirateShipGraveCatalog
                .voyagerDialogue(Map.of(49082L, 1)));
        assertEquals("voyager9", ClassicWorldPirateShipGraveCatalog
                .voyagerDialogue(Map.of(49083L, 1, 41038L, 1)));
        assertEquals(List.of(49082, 49083), List.of(
                ClassicWorldPirateShipGraveCatalog.voyagerReward(0),
                ClassicWorldPirateShipGraveCatalog.voyagerReward(1)));
        assertEquals(List.of("voyager6a", "voyager6b"), List.of(
                ClassicWorldPirateShipGraveCatalog.voyagerRewardHtml(49082),
                ClassicWorldPirateShipGraveCatalog.voyagerRewardHtml(49083)));
        assertEquals(1, ClassicWorldCraftCatalog.recipes(550, 80076).size());
        assertEquals(1, ClassicWorldCraftCatalog.recipes(550, 80077).size());
        for (int itemId : List.of(49082, 49083)) {
            assertNotNull(InventoryCatalog.find(itemId));
        }

        List<ClassicWorldPirateShipGraveCatalog.AldranRoute> routes = List.of(
                new ClassicWorldPirateShipGraveCatalog.AldranRoute(
                        "a", "水面中央地區", 550, 32674, 32871, 0),
                new ClassicWorldPirateShipGraveCatalog.AldranRoute(
                        "b", "水面東邊地區", 550, 32778, 33009, 0),
                new ClassicWorldPirateShipGraveCatalog.AldranRoute(
                        "c", "水面西邊地區", 550, 32471, 32766, 0),
                new ClassicWorldPirateShipGraveCatalog.AldranRoute(
                        "d", "水面南邊地區", 550, 32511, 32998, 0),
                new ClassicWorldPirateShipGraveCatalog.AldranRoute(
                        "e", "深海中央地區", 558, 32998, 33028, 0));
        for (ClassicWorldPirateShipGraveCatalog.AldranRoute route : routes) {
            assertEquals(route, ClassicWorldPirateShipGraveCatalog
                    .aldranRoute(route.action()));
            assertTrue(ClassicWorldMapCatalog.forMap(route.mapId())
                    .standingPassable(route.x(), route.y()));
        }
        assertEquals(null, ClassicWorldPirateShipGraveCatalog.aldranRoute("A"));
        assertNotNull(InventoryCatalog.find(
                ClassicWorldPirateShipGraveCatalog.CREW_CORPSE));
        assertNotNull(InventoryCatalog.find(
                ClassicWorldPirateShipGraveCatalog.WEAK_SOUL));
    }

    private static int population(int mapId) {
        return ClassicWorldMonsterCatalog.definitions(mapId).stream()
                .mapToInt(ClassicWorldMonsterCatalog.SpawnDefinition::count).sum();
    }

    private void assertResource(String path, String expected)
            throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = getClass().getResourceAsStream(path)) {
            assertNotNull(input, path);
            digest.update(input.readAllBytes());
        }
        String actual = HexFormat.of().withUpperCase().formatHex(digest.digest());
        assertEquals(expected, actual, path);
    }
}
