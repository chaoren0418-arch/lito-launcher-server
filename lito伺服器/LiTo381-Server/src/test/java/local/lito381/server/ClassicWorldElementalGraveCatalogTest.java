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

final class ClassicWorldElementalGraveCatalogTest {
    @Test
    void threeMapsAndPackedResourcesMatchSelectedSource() throws Exception {
        assertEquals(List.of(420, 430, 440),
                ClassicWorldElementalGraveCatalog.MAP_IDS);
        assertEquals(List.of(192, 320), dimensions(420));
        assertEquals(List.of(384, 384), dimensions(430));
        assertEquals(List.of(512, 512), dimensions(440));

        for (int mapId : ClassicWorldElementalGraveCatalog.MAP_IDS) {
            ClassicWorldElementalGraveCatalog.MapFlags flags =
                    ClassicWorldElementalGraveCatalog.flags(mapId);
            ClassicWorldMapCatalog.ClassicMap map =
                    ClassicWorldMapCatalog.forMap(mapId);
            assertEquals(List.of(flags.startX(), flags.endX(),
                            flags.startY(), flags.endY()),
                    List.of(map.startX(), map.endX(), map.startY(), map.endY()));
            assertEquals(ClassicWorldElementalGraveCatalog.MAP_SOURCE_SHA256.get(mapId),
                    map.sourceSha256());
            assertEquals(flags.escapable(), map.allowsEscape());
            assertEquals(flags.teleportable(), map.allowsRandomTeleport());
            assertEquals(flags.recallPets(), map.allowsPetRecall());
            assertTrue(flags.usableItem());
            assertTrue(flags.usableSkill());
            assertResource("/maps/" + mapId + ".l38.gz",
                    ClassicWorldElementalGraveCatalog.MAP_RESOURCE_SHA256.get(mapId));

            ClassicWorldElementalGraveCatalog.SourceContract npc =
                    ClassicWorldElementalGraveCatalog.NPC_CONTRACTS.get(mapId);
            ClassicWorldElementalGraveCatalog.SourceContract monster =
                    ClassicWorldElementalGraveCatalog.MONSTER_CONTRACTS.get(mapId);
            ClassicWorldElementalGraveCatalog.SourceContract drop =
                    ClassicWorldElementalGraveCatalog.DROP_CONTRACTS.get(mapId);
            int reconstructedNpcCount = mapId == 440 ? 1 : 0;
            assertEquals(npc.rows() + reconstructedNpcCount,
                    ClassicWorldNpcCatalog.all(mapId).size());
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

        assertTrue(ClassicWorldMapCatalog.forMap(420).allowsRandomTeleport());
        assertFalse(ClassicWorldMapCatalog.forMap(420).allowsPetRecall());
        assertFalse(ClassicWorldMapCatalog.forMap(430).allowsRandomTeleport());
        assertTrue(ClassicWorldMapCatalog.forMap(430).allowsPetRecall());
    }

    @Test
    void exactFourStaticNpcsAndThreeNamedTeleportsAreReachable() {
        ClassicWorldNpcCatalog.StaticNpc abyss = npc(420, 116);
        assertEquals(List.of(50077, 32804, 33047, 6, 4047), List.of(
                abyss.npcId(), abyss.x(), abyss.y(), abyss.heading(), abyss.gfxId()));
        assertEquals("L1Teleporter", abyss.implementation());
        assertEquals("telabyss1", abyss.normalAction());

        ClassicWorldNpcCatalog.StaticNpc grave = npc(430, 115);
        assertEquals(List.of(50076, 32922, 32798, 6, 4047), List.of(
                grave.npcId(), grave.x(), grave.y(), grave.heading(), grave.gfxId()));
        assertEquals("L1Teleporter", grave.implementation());
        assertEquals("telgrave1", grave.normalAction());

        ClassicWorldNpcCatalog.StaticNpc kuud = npc(440, 292);
        assertEquals(List.of(60034, 32425, 33068, 143), List.of(
                kuud.npcId(), kuud.x(), kuud.y(), kuud.gfxId()));
        assertEquals("L1Dwarf", kuud.implementation());
        assertEquals(List.of("kuud", "kuudl"),
                List.of(kuud.normalAction(), kuud.chaoticAction()));

        ClassicWorldNpcCatalog.StaticNpc inn = npc(440, 1336);
        assertEquals(List.of(70096, 32448, 33045, 2466), List.of(
                inn.npcId(), inn.x(), inn.y(), inn.gfxId()));
        assertEquals("L1Merchant", inn.implementation());
        assertEquals(List.of("inn", "Inn1"),
                List.of(inn.normalAction(), inn.chaoticAction()));

        List<ClassicWorldTeleportCatalog.Route> routes =
                ClassicWorldTeleportCatalog.elementalGraveRoutes();
        assertEquals(List.of(1057, 1055, 1056), routes.stream()
                .map(ClassicWorldTeleportCatalog.Route::routeId).toList());
        assertEquals(List.of(430, 420, 440), routes.stream()
                .map(ClassicWorldTeleportCatalog.Route::destinationMapId).toList());
        assertEquals(List.of("teleport abysslake-elegrave",
                        "teleport elegrave-abysslake",
                        "teleport elegrave-drakeIsland"),
                routes.stream().map(ClassicWorldTeleportCatalog.Route::action).toList());
        assertTrue(routes.stream().allMatch(route -> route.priceAmount() == 0
                && route.priceItemId() == InventoryCatalog.ADENA
                && route.minimumLevel() == 0 && route.maximumLevel() == 200));
        for (ClassicWorldTeleportCatalog.Route route : routes) {
            assertEquals(route, ClassicWorldTeleportCatalog.route(
                    route.sourceMapId(), route.sourceObjectId(), route.action()));
            ClassicWorldMapCatalog.ClassicMap destination =
                    ClassicWorldMapCatalog.forMap(route.destinationMapId());
            assertTrue(destination.standingPassable(
                    route.destinationX(), route.destinationY()));
        }
        assertNull(ClassicWorldTeleportCatalog.route(
                430, grave.objectId(), "teleport elegrave-hotel"));
    }

    @Test
    void exactGroundRoutesAndGetbackLandingsAreSafeWhileExternalBoundaryStaysClosed() {
        List<ClassicWorldDungeonCatalog.Route> routes =
                ClassicWorldDungeonCatalog.all().subList(465, 468);
        assertEquals(List.of(466, 467, 468), routes.stream()
                .map(ClassicWorldDungeonCatalog.Route::routeId).toList());
        assertEquals(List.of(
                        List.of(400, 32930, 32971, 420, 32777, 32854, 2),
                        List.of(420, 32777, 32853, 400, 32930, 32972, 2),
                        List.of(420, 32807, 33047, 430, 32934, 32786, 2)),
                routes.stream().map(ClassicWorldElementalGraveCatalogTest::routeWire)
                        .toList());
        assertTrue(routes.stream().allMatch(
                ClassicWorldDungeonCatalog::destinationLandingSafe));
        assertTrue(ClassicWorldDungeonCatalog.all().stream()
                .limit(ClassicWorldDungeonCatalog.PRE_378_ROUTE_COUNT)
                .filter(route -> route.sourceMapId() == 440)
                .allMatch(ClassicWorldDungeonCatalog::isPirateDungeonRoute));
        assertTrue(ClassicWorldDungeonCatalog.all().stream()
                .limit(ClassicWorldDungeonCatalog.PRE_378_ROUTE_COUNT)
                .noneMatch(route -> route.sourceMapId() == 440
                        && (route.destinationMapId() == 446
                            || route.destinationMapId() == 480
                            || route.destinationMapId() == 24576
                            || route.destinationMapId() == 25088)));

        for (int mapId : ClassicWorldElementalGraveCatalog.MAP_IDS) {
            ClassicWorldGetbackCatalog.Destination destination =
                    ClassicWorldGetbackCatalog.resolve(mapId, 32768, 32768, 0, 0);
            assertEquals(new ClassicWorldGetbackCatalog.Destination(
                    440, 32422, 33074, 0), destination);
            ClassicWorldMapCatalog.ClassicMap map =
                    ClassicWorldMapCatalog.forMap(destination.mapId());
            assertTrue(map.standingPassable(destination.x(), destination.y())
                    || map.hasWalkableExit(destination.x(), destination.y()));
        }
    }

    @Test
    void selectedMonsterRowsAppendSevenHundredOneStableActors() {
        ClassicWorldMonsterPopulation actors = new ClassicWorldMonsterPopulation();
        assertEquals(ClassicWorldMonsterCatalog.TOTAL_POPULATION, actors.size());
        long objectId = ClassicWorldMonsterPopulation.OBJECT_BASE
                | (ClassicWorldMonsterCatalog.PRE_378_TOTAL_POPULATION
                    - ClassicWorldMonsterCatalog.ELEMENTAL_GRAVE_SLICE_POPULATION
                    - ClassicWorldMonsterCatalog.PIRATE_DUNGEON_POPULATION + 1);
        int observed = 0;
        for (int mapId : ClassicWorldElementalGraveCatalog.MAP_IDS) {
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
        assertEquals(701, observed);
        assertEquals((ClassicWorldMonsterPopulation.OBJECT_BASE
                        | (ClassicWorldMonsterCatalog.PRE_378_TOTAL_POPULATION
                            - ClassicWorldMonsterCatalog.PIRATE_DUNGEON_POPULATION)) + 1,
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
