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

final class ClassicWorldKnightCaveCatalogTest {
    private static final int[] MAP_IDS = {25, 26, 27, 28};

    @Test
    void selectedMapsRetainCollisionFlagsAndClientScaleBounds() throws Exception {
        String[] resources = {
            "F9EEB131A7D8D1B29685DFAFAEEBAF7A36D81A8893A8E82AAC8D975A0EE25B83",
            "0218945CC1F1B8CA4C1085CD98EDAE37066880D34B0F86C34EFDA6FDE9A3C04A",
            "699DDAC328F157802968508363BD3444928410FAAD6302B333A5D4D6A639E0C7",
            "5ABB0CD8FEA0F3D57556B6CE2EA3455F5ED13E47ED3D706FA42CC65669636D4D"};
        for (int index = 0; index < MAP_IDS.length; index++) {
            int mapId = MAP_IDS[index];
            ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(mapId);
            assertEquals(32704, map.startX());
            assertEquals(32831, map.endX());
            assertEquals(32704, map.startY());
            assertEquals(32831, map.endY());
            assertEquals(128, map.width());
            assertEquals(128, map.height());
            assertEquals(ClassicWorldMapCatalog.KNIGHT_CAVE_MAP_SOURCE_SHA256.get(mapId),
                    map.sourceSha256());
            assertTrue(map.allowsEscape());
            assertTrue(map.allowsPetRecall());
            assertEquals(resources[index], resourceHash("/maps/" + mapId + ".l38.gz"));
        }
        assertEquals(
                "57CCC55086E8C34ADE2EBA7EF57D7FAE619753813A36E4928AFB49F3967F3703",
                ClassicWorldMapCatalog.KNIGHT_CAVE_FLAGS_SOURCE_SHA256);
        assertEquals(
                "3B6BFE370B611CF5713322FFF71E7D6EB1E13FAA1868E951384D42E3C4D10B35",
                ClassicWorldMapCatalog.KNIGHT_CAVE_FLAGS_SECONDARY_SOURCE_SHA256);
    }

    @Test
    void selectedGroundGraphRetainsAllTwelveRoutes() {
        assertEquals(
                "0C6FFFF79B9E8B87E8C621B52BB8445A049CBB969AADA0838DC0C07437F1A852",
                ClassicWorldDungeonCatalog.KNIGHT_CAVE_SOURCE_SHA256);
        assertEquals(
                "12BCE6890E8218B2A145A309CF1077992E8A1AE0607B802F6433078AF59C57B1",
                ClassicWorldDungeonCatalog.KNIGHT_CAVE_NORMALIZED_SOURCE_SHA256);
        List<ClassicWorldDungeonCatalog.Route> routes =
                ClassicWorldDungeonCatalog.all().stream()
                        .filter(route -> route.routeId() >= 131 && route.routeId() <= 142)
                        .toList();
        assertEquals(12, routes.size());
        assertEquals(List.of(131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142),
                routes.stream().map(ClassicWorldDungeonCatalog.Route::routeId).toList());
        assertEquals(25, ClassicWorldDungeonCatalog.find(4, 32951, 33502)
                .destinationMapId());
        assertEquals(26, ClassicWorldDungeonCatalog.find(4, 32910, 33503)
                .destinationMapId());
        assertEquals(27, ClassicWorldDungeonCatalog.find(4, 32880, 33511)
                .destinationMapId());
        assertEquals(28, ClassicWorldDungeonCatalog.find(27, 32723, 32742)
                .destinationMapId());
        assertEquals(27, ClassicWorldDungeonCatalog.find(28, 32799, 32797)
                .destinationMapId());
        for (ClassicWorldDungeonCatalog.Route route : routes) {
            assertEquals(0, route.requiredItemCatalogId());
            assertFalse(route.scheduledPirateShip());
            assertTrue(ClassicWorldMapCatalog.forMap(route.destinationMapId())
                    .standingPassable(route.destinationX(), route.destinationY()));
        }
    }

    @Test
    void selectedStaticNpcRetainsOnlyBlackKnightAdjutantAndNoInventedRecipe()
            throws Exception {
        for (int mapId : new int[] {25, 26, 27}) {
            assertTrue(ClassicWorldNpcCatalog.all(mapId).isEmpty());
        }
        ClassicWorldNpcCatalog.StaticNpc npc = ClassicWorldNpcCatalog.all(28).get(0);
        assertEquals(1011, npc.spawnId());
        assertEquals(81115, npc.npcId());
        assertEquals(32750, npc.x());
        assertEquals(32735, npc.y());
        assertEquals(4, npc.heading());
        assertEquals("$2987", npc.nameId());
        assertEquals("L1Merchant", npc.implementation());
        assertEquals(2337, npc.gfxId());
        assertEquals("adjutant1", npc.normalAction());
        assertTrue(npc.fixedMerchant());
        assertNull(ClassicWorldMerchantCatalog.find(npc));
        assertNull(ClassicWorldCraftCatalog.find(
                28, 81115, "request enchanted sword of chief"));
        assertEquals(ClassicWorldNpcCatalog.MAP25_RESOURCE_SHA256,
                resourceHash("/world/map25-static-npcs.lwn.gz"));
        assertEquals(ClassicWorldNpcCatalog.MAP26_RESOURCE_SHA256,
                resourceHash("/world/map26-static-npcs.lwn.gz"));
        assertEquals(ClassicWorldNpcCatalog.MAP27_RESOURCE_SHA256,
                resourceHash("/world/map27-static-npcs.lwn.gz"));
        assertEquals(ClassicWorldNpcCatalog.MAP28_RESOURCE_SHA256,
                resourceHash("/world/map28-static-npcs.lwn.gz"));
    }

    @Test
    void selectedMonsterRowsAndPopulationRemainHashLocked() {
        int[] rows = {10, 5, 4, 4};
        int[] population = {77, 65, 85, 75};
        String[] resources = {
            "EF70652B7451FB6B978FCE7C27B130473D4867D363FBC12B58D1899491EFF041",
            "3FABF7319699DB19E3BE0B7C4844DE349C063F32421DDF9F6F55BFD3FDC1112B",
            "F7EBE74B0F148B022634B759AECBAB24272A058B67CE0AAB027041DC772E203E",
            "2E5A259BA1AB854875172B1729825B7BBEC1D1B5FD62AEDB7A1CC84A7635B71E"};
        int observedPopulation = 0;
        for (int index = 0; index < MAP_IDS.length; index++) {
            List<ClassicWorldMonsterCatalog.SpawnDefinition> definitions =
                    ClassicWorldMonsterCatalog.definitions(MAP_IDS[index]);
            assertEquals(rows[index], definitions.size());
            int mapPopulation = definitions.stream()
                    .mapToInt(ClassicWorldMonsterCatalog.SpawnDefinition::count).sum();
            assertEquals(population[index], mapPopulation);
            observedPopulation += mapPopulation;
            assertEquals(resources[index], resourceHash(
                    "/world/map" + MAP_IDS[index] + "-monsters.lwm.gz"));
        }
        assertEquals(ClassicWorldMonsterCatalog.KNIGHT_CAVE_POPULATION,
                observedPopulation);
        assertEquals(ClassicWorldMonsterCatalog.TOTAL_POPULATION,
                new ClassicWorldMonsterPopulation().size());
    }

    @Test
    void selectedDropRowsItemsAndSilverKnightGetbackRemainExact() {
        int[] drops = {84, 46, 47, 47};
        int[] items = {58, 40, 32, 32};
        String[] resources = {
            "00973EF9FAC32E70AEE2B3C37267ABF21060571EBE007BBA62E891F6E10C22E2",
            "168D98F8D24A25CFA0535B2EB380A83431136E095967453AD16A85CD1B5A4606",
            "D0246B93BFC927C3E9AF763D0CAE3FEC13112FCBEE0D0512ADE616C23D60736D",
            "512D1988C0FCC5F36522B44AB0DE633A7F93386EC015BA5BBC999A4560E3F8CC"};
        for (int index = 0; index < MAP_IDS.length; index++) {
            int mapId = MAP_IDS[index];
            assertEquals(drops[index], ClassicWorldDropCatalog.dropCount(mapId));
            assertEquals(items[index], ClassicWorldDropCatalog.items(mapId).size());
            assertEquals(resources[index], resourceHash(
                    "/world/map" + mapId + "-drops.lwd.gz"));
            for (ClassicWorldDropCatalog.ItemWire item
                    : ClassicWorldDropCatalog.items(mapId)) {
                assertNotNull(item.inventoryDefinition());
                assertNotNull(InventoryCatalog.find(item.itemId()));
            }
            ClassicWorldGetbackCatalog.Destination destination =
                    ClassicWorldGetbackCatalog.resolve(mapId, 32750, 32750, 0, 0);
            assertEquals(4, destination.mapId());
            assertEquals(33071, destination.x());
            assertEquals(33402, destination.y());
            assertEquals(2, destination.townId());
        }
        assertEquals(
                "A4E547FC78894669BC7BEA823175E84232D5F195728B14759E4F22B70C46E4B8",
                ClassicWorldGetbackCatalog.KNIGHT_CAVE_SOURCE_SHA256);
    }

    private static String resourceHash(String resource) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = ClassicWorldKnightCaveCatalogTest.class
                    .getResourceAsStream(resource)) {
                assertNotNull(in);
                digest.update(in.readAllBytes());
            }
            return HexFormat.of().withUpperCase().formatHex(digest.digest());
        } catch (Exception error) {
            throw new AssertionError(error);
        }
    }
}
