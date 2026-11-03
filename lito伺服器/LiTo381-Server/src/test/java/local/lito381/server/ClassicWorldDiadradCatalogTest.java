package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ClassicWorldDiadradCatalogTest {
    @Test
    void fourMapsAndThirtySevenSafeGroundRoutesAppendAfterImmutable345Ids() {
        assertEquals(List.of(320, 330, 400, 401), ClassicWorldDiadradCatalog.MAP_IDS);
        for (int mapId : ClassicWorldDiadradCatalog.MAP_IDS) {
            ClassicWorldDiadradCatalog.MapFlags flags =
                    ClassicWorldDiadradCatalog.flags(mapId);
            ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(mapId);
            assertEquals(ClassicWorldDiadradCatalog.MAP_SOURCE_SHA256.get(mapId),
                    map.sourceSha256());
            assertEquals(List.of(flags.startX(), flags.endX(), flags.startY(), flags.endY()),
                    List.of(map.startX(), map.endX(), map.startY(), map.endY()));
            assertEquals(flags.escapable(), map.allowsEscape());
            assertEquals(flags.teleportable(), map.allowsRandomTeleport());
            assertEquals(flags.recallPets(), map.allowsPetRecall());
        }
        assertEquals(List.of(448, 320), dimensions(320));
        assertEquals(List.of(192, 256), dimensions(330));
        assertEquals(List.of(640, 512), dimensions(400));
        assertEquals(List.of(192, 128), dimensions(401));

        List<ClassicWorldDungeonCatalog.Route> routes =
                ClassicWorldDungeonCatalog.all().stream()
                        .filter(ClassicWorldDungeonCatalog::isDiadradServiceCoreRoute)
                        .toList();
        assertEquals(37, routes.size());
        assertEquals(ClassicWorldDungeonCatalog.TOTAL_ROUTE_COUNT,
                ClassicWorldDungeonCatalog.all().size());
        assertEquals(486, ClassicWorldDungeonCatalog.PRE_378_ROUTE_COUNT);
        assertEquals(414, routes.get(0).routeId());
        assertEquals(450, routes.get(routes.size() - 1).routeId());
        assertTrue(routes.stream().allMatch(
                ClassicWorldDungeonCatalog::destinationLandingSafe));
        assertTrue(routes.stream().noneMatch(route ->
                List.of(410, 420, 450).contains(route.destinationMapId())));
        assertNull(ClassicWorldDungeonCatalog.find(400, 32957, 32824));
    }

    @Test
    void selectedResourcesRetainEightNpcsAndAppend1375Actors() {
        int population = 0;
        for (int mapId : ClassicWorldDiadradCatalog.MAP_IDS) {
            ClassicWorldDiadradCatalog.SourceContract npc =
                    ClassicWorldDiadradCatalog.NPC_CONTRACTS.get(mapId);
            ClassicWorldDiadradCatalog.SourceContract monster =
                    ClassicWorldDiadradCatalog.MONSTER_CONTRACTS.get(mapId);
            ClassicWorldDiadradCatalog.SourceContract drop =
                    ClassicWorldDiadradCatalog.DROP_CONTRACTS.get(mapId);
            assertEquals(npc.rows(), ClassicWorldNpcCatalog.all(mapId).size());
            assertEquals(monster.rows(), ClassicWorldMonsterCatalog.definitions(mapId).size());
            assertEquals(drop.rows(), ClassicWorldDropCatalog.dropCount(mapId));
            assertEquals(drop.population(), ClassicWorldDropCatalog.items(mapId).size());
            population += monster.population();
        }
        assertEquals(8, ClassicWorldDiadradCatalog.NPC_CONTRACTS.values().stream()
                .mapToInt(ClassicWorldDiadradCatalog.SourceContract::rows).sum());
        assertEquals(ClassicWorldMonsterCatalog.DIADRAD_SERVICE_CORE_POPULATION,
                population);
        assertNull(ClassicWorldNpcCatalog.npc(320,
                ClassicWorldNpcCatalog.objectId(1078)));
        assertNull(ClassicWorldNpcCatalog.npc(320,
                ClassicWorldNpcCatalog.objectId(1080)));

        ClassicWorldMonsterPopulation actors = new ClassicWorldMonsterPopulation();
        long objectId = ClassicWorldMonsterPopulation.OBJECT_BASE
                | (ClassicWorldMonsterCatalog.PRE_378_TOTAL_POPULATION
                    - ClassicWorldMonsterCatalog.PIRATE_DUNGEON_POPULATION
                    - ClassicWorldMonsterCatalog.ELEMENTAL_GRAVE_SLICE_POPULATION
                    - ClassicWorldMonsterCatalog.DEMON_TEMPLE_POPULATION
                    - ClassicWorldMonsterCatalog.DIADRAD_SERVICE_CORE_POPULATION + 1);
        int observed = 0;
        for (int mapId : ClassicWorldDiadradCatalog.MAP_IDS) {
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
        assertEquals(1_375, observed);
    }

    @Test
    void textReachableCraftingWarehouseStoryAndPaidEntryAreExact() {
        assertCrafter(320, 132, 2);
        assertCrafter(320, 750, 2);
        assertCrafter(320, 940, 11);
        assertCrafter(400, 938, 3);
        assertCrafter(400, 939, 4);
        assertCrafter(400, 1014, 1);

        assertNull(ClassicWorldCraftCatalog.find(
                320, 71125, "request claw of beast king"));
        assertNull(ClassicWorldCraftCatalog.find(
                400, 71113, "request potion of emotion:bravery"));
        assertNull(ClassicWorldCraftCatalog.find(
                400, 71114, "request potion of healing"));
        assertNotNull(ClassicWorldCraftCatalog.find(
                400, 71113, "request recluse teleport scroll"));
        assertNotNull(ClassicWorldCraftCatalog.find(
                400, 71114, "request resist teleport scroll"));
        assertEquals("adelio3", ClassicWorldDiadradCatalog.linkedHtml(
                npc(320, 940), "adelio3"));
        assertNull(ClassicWorldDiadradCatalog.linkedHtml(
                npc(320, 940), "adelio6"));
        assertEquals("clalchemist3", ClassicWorldDiadradCatalog.linkedHtml(
                npc(400, 938), "clalchemist3"));
        assertNull(ClassicWorldDiadradCatalog.linkedHtml(
                npc(400, 938), "clalchemist4"));

        ClassicWorldNpcCatalog.StaticNpc warehouse = npc(400, 293);
        assertTrue(ClassicWorldDiadradCatalog.isWarehouse(warehouse));
        assertEquals(warehouse, ClassicWorldNpcCatalog.fixedWarehouse(
                400, warehouse.objectId()));
        assertEquals("zidar", warehouse.initialHtml(0));
        assertEquals("zadarl", warehouse.initialHtml(-1001));

        ClassicWorldNpcCatalog.StaticNpc story = npc(400, 1182);
        assertTrue(ClassicWorldDiadradCatalog.isStoryNpc(story));
        assertTrue(ClassicWorldStoryQuestCatalog.hasNpc(80012));
        assertEquals(7, ClassicWorldStoryQuestCatalog.all().stream()
                .filter(event -> event.npcId() == 80012).count());

        ClassicWorldTeleportCatalog.Route entry =
                ClassicWorldTeleportCatalog.diadradEntry();
        ClassicWorldNpcCatalog.StaticNpc source = npc(4, 1286);
        assertEquals(source.objectId(), entry.sourceObjectId());
        assertEquals("wa_earth1", entry.initialHtml());
        assertEquals(1197, entry.routeId());
        assertEquals(320, entry.destinationMapId());
        assertEquals(7_480, entry.priceAmount());
        assertEquals(InventoryCatalog.ADENA, entry.priceItemId());
        assertEquals(entry, ClassicWorldTeleportCatalog.route(
                4, source.objectId(), "teleport silveria-diad"));
    }

    @Test
    void escapeRowsUseTown14AndExactGreatCavernDestinations() {
        for (int mapId : List.of(320, 330)) {
            ClassicWorldGetbackCatalog.Destination destination =
                    ClassicWorldGetbackCatalog.resolve(mapId, 32780, 32800, 0, 0);
            assertEquals(new ClassicWorldGetbackCatalog.Destination(
                    310, 32818, 32805, 14), destination);
        }
        assertEquals(new ClassicWorldGetbackCatalog.Destination(
                        400, 32563, 32680, 0),
                ClassicWorldGetbackCatalog.resolve(400, 32520, 32820, 0, 0));
        assertEquals(new ClassicWorldGetbackCatalog.Destination(
                        400, 32600, 32922, 0),
                ClassicWorldGetbackCatalog.resolve(400, 32920, 32930, 0, 0));
        assertEquals(new ClassicWorldGetbackCatalog.Destination(
                        400, 32563, 32680, 0),
                ClassicWorldGetbackCatalog.resolve(401, 32800, 32800, 0, 0));
    }

    private static void assertCrafter(int mapId, int spawnId, int recipeCount) {
        ClassicWorldNpcCatalog.StaticNpc actor = npc(mapId, spawnId);
        assertTrue(ClassicWorldDiadradCatalog.isCrafter(actor));
        List<ClassicWorldCraftCatalog.Recipe> recipes =
                ClassicWorldCraftCatalog.recipes(mapId, actor.npcId());
        assertEquals(recipeCount, recipes.size());
        assertTrue(recipes.stream().allMatch(recipe ->
                ClassicWorldCraftCatalog.matchesSource(actor, recipe)));
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
}
