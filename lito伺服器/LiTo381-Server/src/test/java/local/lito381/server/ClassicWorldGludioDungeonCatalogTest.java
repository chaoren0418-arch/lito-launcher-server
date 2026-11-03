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
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

final class ClassicWorldGludioDungeonCatalogTest {
    private static final int[][] ROUTES = {
            {201,4,32728,32929,7,32812,32724,4},
            {202,7,32740,32807,8,32750,32798,4},
            {203,7,32812,32723,4,32728,32929,5},
            {204,8,32726,32744,8,32773,32807,2},
            {205,8,32736,32772,8,32737,32812,2},
            {206,8,32738,32813,8,32737,32772,2},
            {207,8,32741,32745,8,32797,32803,2},
            {208,8,32750,32797,7,32740,32806,0},
            {209,8,32767,32734,8,32811,32761,2},
            {210,8,32773,32808,8,32727,32744,2},
            {211,8,32794,32728,9,32801,32754,4},
            {212,8,32797,32745,8,32742,32745,2},
            {213,8,32811,32762,8,32767,32733,2},
            {214,9,32771,32765,10,32763,32773,4},
            {215,9,32802,32754,8,32795,32728,2},
            {216,10,32726,32737,11,32728,32723,4},
            {217,10,32764,32772,9,32771,32764,0},
            {218,11,32729,32722,10,32727,32737,2},
            {219,11,32735,32739,11,32768,32805,2},
            {220,11,32745,32805,11,32757,32804,2},
            {221,11,32757,32805,11,32745,32804,2},
            {222,11,32763,32738,11,32773,32765,2},
            {223,11,32768,32806,11,32736,32739,2},
            {224,11,32772,32765,11,32764,32738,2},
            {225,11,32797,32810,12,32804,32725,4},
            {226,12,32728,32768,12,32764,32754,2},
            {227,12,32732,32802,12,32799,32795,7},
            {228,12,32764,32755,12,32729,32768,2},
            {229,12,32770,32811,13,32727,32725,4},
            {230,12,32799,32795,12,32732,32802,5},
            {231,12,32805,32724,11,32797,32809,2},
            {232,13,32727,32724,12,32770,32810,2}
    };

    @Test
    void sixMapsPinExactCollisionNpcMonsterAndDropResources() throws Exception {
        int[] monsterRows = {6, 7, 5, 7, 7, 7};
        int[] populations = {53, 58, 50, 73, 80, 61};
        int[] dropRows = {53, 61, 42, 66, 61, 90};
        int[] itemRows = {36, 45, 36, 53, 40, 66};
        for (int index = 0; index < 6; index++) {
            int mapId = index + 7;
            ClassicWorldMapCatalog.ClassicMap map =
                    ClassicWorldMapCatalog.forMap(mapId);
            assertEquals(32704, map.startX());
            assertEquals(32831, map.endX());
            assertEquals(32704, map.startY());
            assertEquals(32831, map.endY());
            assertEquals(128, map.width());
            assertEquals(128, map.height());
            assertEquals(ClassicWorldMapCatalog.GLUDIO_DUNGEON_MAP_SOURCE_SHA256
                    .get(mapId), map.sourceSha256());
            assertTrue(map.allowsEscape());
            assertTrue(map.allowsPetRecall());
            assertTrue(map.allowsRandomTeleport());
            assertResource("/maps/" + mapId + ".l38.gz",
                    ClassicWorldMapCatalog.GLUDIO_DUNGEON_MAP_RESOURCE_SHA256
                            .get(mapId));
            assertResource("/world/map" + mapId + "-static-npcs.lwn.gz",
                    ClassicWorldNpcCatalog.GLUDIO_DUNGEON_RESOURCE_SHA256.get(mapId));

            List<ClassicWorldMonsterCatalog.SpawnDefinition> definitions =
                    ClassicWorldMonsterCatalog.definitions(mapId);
            assertEquals(monsterRows[index], definitions.size());
            assertEquals(populations[index], definitions.stream()
                    .mapToInt(ClassicWorldMonsterCatalog.SpawnDefinition::count).sum());
            assertEquals(dropRows[index], ClassicWorldDropCatalog.dropCount(mapId));
            assertEquals(itemRows[index], ClassicWorldDropCatalog.items(mapId).size());
        }
        assertEquals(375, java.util.stream.IntStream.rangeClosed(7, 12)
                .map(mapId -> ClassicWorldMonsterCatalog.definitions(mapId).stream()
                        .mapToInt(ClassicWorldMonsterCatalog.SpawnDefinition::count).sum())
                .sum());
    }

    @Test
    void allThirtyTwoRoutesCloseTheMap4ToMap13Component() {
        assertEquals(ClassicWorldDungeonCatalog.GLUDIO_DUNGEON_SOURCE_SHA256,
                ClassicWorldDungeonCatalog.GLUDIO_DUNGEON_SECONDARY_SOURCE_SHA256);
        List<Integer> ids = ClassicWorldDungeonCatalog.all().stream()
                .filter(route -> route.routeId() >= 201 && route.routeId() <= 232).map(
                        ClassicWorldDungeonCatalog.Route::routeId).toList();
        assertEquals(java.util.stream.IntStream.rangeClosed(201, 232)
                .boxed().toList(), ids);
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
            assertTrue(ClassicWorldMapCatalog.forMap(route.sourceMapId())
                    .standingPassable(route.sourceX(), route.sourceY()));
            assertTrue(ClassicWorldMapCatalog.forMap(route.destinationMapId())
                    .standingPassable(route.destinationX(), route.destinationY()));
        }
    }

    @Test
    void allSixFlagsAndGetbackRowsRemainHashLocked() {
        assertEquals("652D9C1C7BEE7FFA9D0DBA8B90AAECBB637AE489E1B35A98024D52C556128BD8",
                ClassicWorldMapCatalog.GLUDIO_DUNGEON_FLAGS_SOURCE_SHA256);
        assertEquals("E6748C9F230A5D4D5D5D4F117EC626C69D2E3381D52FA6E5FA689450622B4E62",
                ClassicWorldMapCatalog.GLUDIO_DUNGEON_FLAGS_SECONDARY_SOURCE_SHA256);
        assertEquals(ClassicWorldMapCatalog.GLUDIO_DUNGEON_GETBACK_SOURCE_SHA256,
                ClassicWorldGetbackCatalog.GLUDIO_DUNGEON_SOURCE_SHA256);
        for (int mapId : ClassicWorldMapCatalog.GLUDIO_DUNGEON_MAP_IDS) {
            ClassicWorldGetbackCatalog.Destination destination =
                    ClassicWorldGetbackCatalog.resolve(mapId, 32768, 32768, 0, mapId);
            assertEquals(new ClassicWorldGetbackCatalog.Destination(
                    4, 32624, 32813, 3), destination);
        }
    }

    @Test
    void map12PinsFlameCrafterAndBloodSoulMerchant() throws Exception {
        List<ClassicWorldNpcCatalog.StaticNpc> npcs = ClassicWorldNpcCatalog.all(12);
        assertEquals(2, npcs.size());
        ClassicWorldNpcCatalog.StaticNpc flame =
                ClassicWorldNpcCatalog.npc(12, ClassicWorldCraftCatalog.FLAME_OBJECT_ID);
        assertNotNull(flame);
        assertEquals(1010, flame.spawnId());
        assertEquals(81114, flame.npcId());
        assertEquals(List.of(32732, 32796, 4),
                List.of(flame.x(), flame.y(), flame.heading()));
        assertEquals(3406, flame.gfxId());
        assertEquals("fsflame1", flame.normalAction());

        List<ClassicWorldCraftCatalog.Recipe> recipes =
                ClassicWorldCraftCatalog.recipes(12, 81114);
        assertEquals(3, recipes.size());
        assertEquals(List.of(
                        "request enchanted staff of demon",
                        "request enchanted staff of baphomet",
                        "request enchanted staff of beleth"),
                recipes.stream().map(ClassicWorldCraftCatalog.Recipe::action).toList());
        assertTrue(recipes.stream().flatMap(recipe -> recipe.materials().stream())
                .allMatch(item -> item.enchant() == -1));
        assertTrue(recipes.stream().flatMap(recipe -> recipe.results().stream())
                .allMatch(item -> item.enchant() == 0));
        assertTrue(recipes.stream().allMatch(recipe ->
                ClassicWorldCraftCatalog.matchesSource(flame, recipe)));
        assertResource("/world/map12-flame-crafting.lwc.gz",
                ClassicWorldCraftCatalog.FLAME_RESOURCE_SHA256);

        ClassicWorldNpcCatalog.StaticNpc soulNpc = ClassicWorldNpcCatalog.fixedMerchant(
                12, ClassicWorldNpcCatalog.objectId(1012));
        ClassicWorldMerchantCatalog.Merchant soul =
                ClassicWorldMerchantCatalog.find(soulNpc);
        assertNotNull(soul);
        assertEquals(81118, soul.npcId());
        assertEquals("bsoul1", soul.initialHtml());
        assertEquals("buy", soul.buyAction());
        assertEquals(1, soul.offers().size());
        ClassicWorldMerchantCatalog.Offer offer = soul.offers().get(0);
        assertEquals(0, offer.orderId());
        assertEquals(100_000, offer.unitPrice());
        assertEquals(49013, offer.definition().catalogId());
        assertEquals("$2988", offer.definition().name());
        assertEquals(989, offer.definition().icon());
        assertEquals(3963, offer.definition().groundGfx());
        assertEquals(1_000, offer.definition().unitWeight());
        assertFalse(offer.definition().stackable());
        assertTrue(offer.definition().tradable());
        assertEquals(offer.definition(), InventoryCatalog.require(49013));
    }

    @Test
    void flameResultPreservesExactSourceZeroEnchantState() {
        ClassicWorldCraftCatalog.Recipe demon = ClassicWorldCraftCatalog.find(
                12, 81114, "request enchanted staff of demon");
        assertNotNull(demon);
        assertEquals(List.of(108, 49014), demon.materials().stream()
                .map(ClassicWorldCraftCatalog.MakingItem::itemId).toList());
        assertEquals(List.of(119), demon.results().stream()
                .map(ClassicWorldCraftCatalog.MakingItem::itemId).toList());
        RuntimeInventory materials = RuntimeInventory.fromCatalogCounts(
                Map.of(108L, 1, 49014L, 1),
                new AtomicLong(0x7200_0000L)::getAndIncrement);
        assertEquals(1, ClassicWorldCraftCatalog.maximumAmount(materials, demon));

        InventoryCatalog.Definition staff = InventoryCatalog.require(119);
        RuntimeInventory result = new RuntimeInventory();
        assertTrue(result.add(staff, 1,
                new AtomicLong(0x7300_0000L)::getAndIncrement));
        long objectId = 0x7300_0000L;
        assertEquals(0, result.item(objectId).enchantLevel());
        assertEquals(119, result.item(objectId).definition().catalogId());
    }

    @Test
    void populationAppendsAll375ActorsAfterEveryPointSevenThreeFourObject() {
        ClassicWorldMonsterPopulation population = new ClassicWorldMonsterPopulation();
        assertEquals(ClassicWorldMonsterCatalog.TOTAL_POPULATION, population.size());
        assertEquals(ClassicWorldMonsterCatalog.PRE_335_TOTAL_POPULATION + 375,
                ClassicWorldMonsterCatalog.PRE_336_TOTAL_POPULATION);
        long objectId = ClassicWorldMonsterPopulation.OBJECT_BASE
                | (ClassicWorldMonsterCatalog.PRE_335_TOTAL_POPULATION + 1);
        int observed = 0;
        for (int mapId : ClassicWorldMapCatalog.GLUDIO_DUNGEON_MAP_IDS) {
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
        assertEquals(375, observed);
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
