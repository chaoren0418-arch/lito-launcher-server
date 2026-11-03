package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ClassicWorldPirateDungeonCatalogTest {
    @Test
    void fourFloorsAndEveryPackedResourceMatchSelectedSource() throws Exception {
        assertEquals(List.of(441, 442, 443, 444),
                ClassicWorldPirateDungeonCatalog.MAP_IDS);
        assertEquals(List.of(256, 256), dimensions(441));
        assertEquals(List.of(192, 192), dimensions(442));
        assertEquals(List.of(256, 320), dimensions(443));
        assertEquals(List.of(64, 64), dimensions(444));

        for (int mapId : ClassicWorldPirateDungeonCatalog.MAP_IDS) {
            ClassicWorldPirateDungeonCatalog.MapFlags flags =
                    ClassicWorldPirateDungeonCatalog.flags(mapId);
            ClassicWorldMapCatalog.ClassicMap map =
                    ClassicWorldMapCatalog.forMap(mapId);
            assertEquals(List.of(flags.startX(), flags.endX(),
                            flags.startY(), flags.endY()),
                    List.of(map.startX(), map.endX(), map.startY(), map.endY()));
            assertEquals(ClassicWorldPirateDungeonCatalog.MAP_SOURCE_SHA256.get(mapId),
                    map.sourceSha256());
            assertEquals(flags.escapable(), map.allowsEscape());
            assertEquals(flags.teleportable(), map.allowsRandomTeleport());
            assertEquals(flags.recallPets(), map.allowsPetRecall());
            assertTrue(flags.usableItem());
            assertTrue(flags.usableSkill());
            assertResource("/maps/" + mapId + ".l38.gz",
                    ClassicWorldPirateDungeonCatalog.MAP_RESOURCE_SHA256.get(mapId));

            ClassicWorldPirateDungeonCatalog.SourceContract npc =
                    ClassicWorldPirateDungeonCatalog.NPC_CONTRACTS.get(mapId);
            ClassicWorldPirateDungeonCatalog.SourceContract monster =
                    ClassicWorldPirateDungeonCatalog.MONSTER_CONTRACTS.get(mapId);
            ClassicWorldPirateDungeonCatalog.SourceContract drop =
                    ClassicWorldPirateDungeonCatalog.DROP_CONTRACTS.get(mapId);
            assertEquals(npc.rows(), ClassicWorldNpcCatalog.all(mapId).size());
            assertEquals(monster.rows(),
                    ClassicWorldMonsterCatalog.definitions(mapId).size());
            assertEquals(monster.population(), population(mapId));
            assertEquals(drop.rows(), ClassicWorldDropCatalog.dropCount(mapId));
            assertEquals(drop.population(), ClassicWorldDropCatalog.items(mapId).size());
            assertResource("/world/map" + mapId + "-static-npcs.lwn.gz",
                    npc.resourceSha256());
            assertResource("/world/map" + mapId + "-monsters.lwm.gz",
                    monster.resourceSha256());
            assertResource("/world/map" + mapId + "-drops.lwd.gz",
                    drop.resourceSha256());
        }

        assertTrue(ClassicWorldMapCatalog.forMap(443).allowsRandomTeleport());
        assertFalse(ClassicWorldMapCatalog.forMap(444).allowsRandomTeleport());
    }

    @Test
    void fourthFloorServicesAndNamedElementalTombRouteAreExact() throws Exception {
        ClassicWorldNpcCatalog.StaticNpc crystal = npc(444, 117);
        assertEquals(List.of(50078, 32736, 32807, 6, 155), List.of(
                crystal.npcId(), crystal.x(), crystal.y(),
                crystal.heading(), crystal.gfxId()));
        assertEquals("L1Teleporter", crystal.implementation());
        assertEquals("picrystal", crystal.normalAction());

        ClassicWorldNpcCatalog.StaticNpc merchant = npc(444, 1015);
        assertEquals(List.of(81121, 32731, 32804, 4, 4359), List.of(
                merchant.npcId(), merchant.x(), merchant.y(),
                merchant.heading(), merchant.gfxId()));
        assertEquals("L1Merchant", merchant.implementation());
        assertEquals("pielemental", merchant.normalAction());

        List<ClassicWorldTeleportCatalog.Route> routes =
                ClassicWorldTeleportCatalog.pirateDungeonRoutes();
        assertEquals(1, routes.size());
        ClassicWorldTeleportCatalog.Route route = routes.get(0);
        assertEquals(List.of(444, 117, 50078, 32736, 32807, 1058,
                        32924, 32801, 430, 5, 0, 40308L, 0, 200),
                List.of(route.sourceMapId(), route.sourceSpawnId(), route.sourceNpcId(),
                        route.sourceX(), route.sourceY(), route.routeId(),
                        route.destinationX(), route.destinationY(),
                        route.destinationMapId(), route.heading(),
                        route.priceAmount(), route.priceItemId(),
                        route.minimumLevel(), route.maximumLevel()));
        assertEquals("teleport piratedungeon-elementaltomb", route.action());
        assertEquals("picrystal1", ClassicWorldTeleportCatalog.followUpHtml(
                444, crystal.objectId(), "teleportURL"));
        assertEquals(route, ClassicWorldTeleportCatalog.route(
                444, crystal.objectId(), route.action()));
        assertTrue(ClassicWorldMapCatalog.forMap(430).standingPassable(
                route.destinationX(), route.destinationY()));

        ClassicWorldCraftCatalog.Recipe recipe = ClassicWorldCraftCatalog.find(
                444, ClassicWorldPirateDungeonCatalog.CRAFTER_NPC_ID,
                ClassicWorldPirateDungeonCatalog.CRAFT_ACTION);
        assertNotNull(recipe);
        assertEquals(443, recipe.id());
        assertTrue(recipe.amountInputable());
        assertEquals(List.of(
                        new ClassicWorldCraftCatalog.MakingItem(40471, 20, -1),
                        new ClassicWorldCraftCatalog.MakingItem(40318, 3, -1)),
                recipe.materials());
        assertEquals(List.of(
                        new ClassicWorldCraftCatalog.MakingItem(40417, 1, 0)),
                recipe.results());
        assertEquals(1_000, recipe.successRate());
        assertEquals(0, recipe.failItemId());
        assertTrue(ClassicWorldCraftCatalog.matchesSource(merchant, recipe));
        assertResource("/world/map444-npc81121-crafting.lwc.gz",
                ClassicWorldPirateDungeonCatalog.CRAFT_RESOURCE_SHA256);
    }

    @Test
    void allEighteenGroundRowsAndFourGetbackRowsAreLive() {
        List<ClassicWorldDungeonCatalog.Route> routes =
                ClassicWorldDungeonCatalog.all().stream()
                    .filter(ClassicWorldDungeonCatalog::isPirateDungeonRoute).toList();
        assertEquals(18, routes.size());
        assertEquals(List.of(469, 470, 471, 472, 473, 474, 475, 476, 477,
                        478, 479, 480, 481, 482, 483, 484, 485, 486),
                routes.stream().map(ClassicWorldDungeonCatalog.Route::routeId).toList());
        assertEquals(List.of(
                        List.of(440, 32524, 33134, 441, 32679, 32862, 2),
                        List.of(440, 32524, 33135, 441, 32679, 32862, 2),
                        List.of(440, 32524, 33136, 441, 32679, 32862, 2),
                        List.of(440, 32536, 32888, 441, 32738, 32738, 2),
                        List.of(440, 32537, 32888, 441, 32738, 32738, 2),
                        List.of(440, 32538, 32888, 441, 32738, 32738, 2),
                        List.of(441, 32678, 32861, 440, 32522, 33135, 2),
                        List.of(441, 32679, 32861, 440, 32522, 33135, 2),
                        List.of(441, 32680, 32861, 440, 32522, 33135, 2),
                        List.of(441, 32737, 32737, 440, 32537, 32890, 2),
                        List.of(441, 32738, 32737, 440, 32537, 32890, 2),
                        List.of(441, 32739, 32737, 440, 32537, 32890, 2),
                        List.of(441, 32805, 32802, 442, 32862, 32730, 2),
                        List.of(442, 32860, 32799, 443, 32864, 32781, 2),
                        List.of(442, 32862, 32729, 441, 32806, 32802, 2),
                        List.of(443, 32752, 32745, 444, 32745, 32809, 2),
                        List.of(443, 32864, 32780, 442, 32860, 32798, 2),
                        List.of(444, 32745, 32809, 443, 32752, 32745, 2)),
                routes.stream().map(ClassicWorldPirateDungeonCatalogTest::routeWire)
                        .toList());
        assertTrue(routes.stream().allMatch(
                ClassicWorldDungeonCatalog::destinationLandingSafe));

        for (int mapId : ClassicWorldPirateDungeonCatalog.MAP_IDS) {
            assertEquals(new ClassicWorldGetbackCatalog.Destination(
                            440, 32422, 33074, 0),
                    ClassicWorldGetbackCatalog.resolve(
                            mapId, 32745, 32809, 0, mapId));
        }
    }

    @Test
    void selectedMonsterRowsAppendTwoHundredFiftyOneStableActors() {
        ClassicWorldMonsterPopulation actors = new ClassicWorldMonsterPopulation();
        assertEquals(ClassicWorldMonsterCatalog.TOTAL_POPULATION, actors.size());
        long objectId = ClassicWorldMonsterPopulation.OBJECT_BASE
                | (ClassicWorldMonsterCatalog.PRE_378_TOTAL_POPULATION
                    - ClassicWorldMonsterCatalog.PIRATE_DUNGEON_POPULATION + 1);
        int observed = 0;
        for (int mapId : ClassicWorldPirateDungeonCatalog.MAP_IDS) {
            for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                    : ClassicWorldMonsterCatalog.definitions(mapId)) {
                for (int slot = 1; slot <= definition.count(); slot++) {
                    ClassicWorldMonsterPopulation.Snapshot actor =
                            actors.actor(mapId, objectId++);
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
        assertEquals(251, observed);
        assertEquals((ClassicWorldMonsterPopulation.OBJECT_BASE
                        | ClassicWorldMonsterCatalog.PRE_378_TOTAL_POPULATION) + 1,
                objectId);
    }

    private static int population(int mapId) {
        return ClassicWorldMonsterCatalog.definitions(mapId).stream()
                .mapToInt(ClassicWorldMonsterCatalog.SpawnDefinition::count).sum();
    }

    private static ClassicWorldNpcCatalog.StaticNpc npc(int mapId, int spawnId) {
        ClassicWorldNpcCatalog.StaticNpc result = ClassicWorldNpcCatalog.npc(
                mapId, ClassicWorldNpcCatalog.objectId(spawnId));
        assertNotNull(result);
        return result;
    }

    private static List<Integer> dimensions(int mapId) {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(mapId);
        return List.of(map.width(), map.height());
    }

    private static List<Integer> routeWire(ClassicWorldDungeonCatalog.Route route) {
        return List.of(route.sourceMapId(), route.sourceX(), route.sourceY(),
                route.destinationMapId(), route.destinationX(), route.destinationY(),
                route.destinationHeading());
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
