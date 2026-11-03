package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ClassicWorldMainlandShipCatalogTest {
    @Test
    void selectedMapsZeroFiveAndSixAreHashLockedAndNativeLandingsArePassable()
            throws Exception {
        assertEquals(List.of(0, 5, 6), ClassicWorldMapCatalog.MAINLAND_SHIP_MAP_IDS);
        assertMap(0, 32256, 32767, 32768, 33279,
                ClassicWorldMapCatalog.MAP0_SOURCE_SHA256,
                ClassicWorldMapCatalog.MAP0_RESOURCE_SHA256);
        assertMap(5, 32704, 32767, 32768, 32831,
                ClassicWorldMapCatalog.MAP5_SOURCE_SHA256,
                ClassicWorldMapCatalog.MAP5_RESOURCE_SHA256);
        assertMap(6, 32704, 32831, 32768, 32831,
                ClassicWorldMapCatalog.MAP6_SOURCE_SHA256,
                ClassicWorldMapCatalog.MAP6_RESOURCE_SHA256);

        for (ShipRouteCatalog.Boarding boarding : ShipRouteCatalog.boardings()) {
            assertTrue(ClassicWorldMapCatalog.forMap(boarding.destinationMap())
                    .standingPassable(boarding.destinationX(), boarding.destinationY()));
        }
        for (ShipRouteCatalog.Route route : ShipRouteCatalog.all().stream()
                .filter(candidate -> candidate.sourceMap() == 5
                        || candidate.sourceMap() == 6).toList()) {
            int passable = 0;
            for (int x = route.minX(); x <= route.maxX(); x++) {
                for (int y = route.minY(); y <= route.maxY(); y++) {
                    if (ClassicWorldMapCatalog.forMap(route.destinationMap())
                            .standingPassable(x, y)) passable++;
                }
            }
            assertTrue(passable > 0);
        }
        // Exact LiTo random range includes this map0 dock-edge cell; the
        // session normalizes it to the nearest passable point in the same
        // authenticated C_GOTO_MAP envelope before S_PORTAL.
        assertFalse(ClassicWorldMapCatalog.forMap(0)
                .standingPassable(32633, 32972));
    }

    @Test
    void selectedMapZeroContentAndEmptyShipInteriorsRemainIndependent() {
        assertEquals(6, ClassicWorldNpcCatalog.all(0).size());
        assertEquals(0, ClassicWorldNpcCatalog.all(5).size());
        assertEquals(0, ClassicWorldNpcCatalog.all(6).size());
        assertEquals(53, ClassicWorldMonsterCatalog.definitions(0).size());
        assertEquals(492, ClassicWorldMonsterCatalog.definitions(0).stream()
                .mapToInt(ClassicWorldMonsterCatalog.SpawnDefinition::count).sum());
        assertTrue(ClassicWorldMonsterCatalog.definitions(5).isEmpty());
        assertTrue(ClassicWorldMonsterCatalog.definitions(6).isEmpty());
        assertEquals(103, ClassicWorldDropCatalog.dropCount(0));
        assertEquals(65, ClassicWorldDropCatalog.items(0).size());
        assertEquals(0, ClassicWorldDropCatalog.dropCount(5));
        assertEquals(0, ClassicWorldDropCatalog.dropCount(6));
    }

    @Test
    void selectedPortMastersSellOnlyTheirExactThreeHundredAdenaTicket() {
        ClassicWorldMerchantCatalog.Merchant talking = ClassicWorldMerchantCatalog.find(
                ClassicWorldNpcCatalog.fixedMerchant(
                        0, ClassicWorldNpcCatalog.objectId(1324)));
        assertNotNull(talking);
        assertEquals(70011, talking.npcId());
        assertEquals(32633, talking.x());
        assertEquals(32968, talking.y());
        assertEquals("shipEvI3", talking.initialHtml());
        assertEquals(300, talking.offers().get(0).unitPrice());
        assertSame(InventoryCatalog.require(InventoryCatalog.SHIP_TICKET_TO_GLUDIO),
                talking.offers().get(0).definition());
        ClassicWorldNpcCatalog.StaticNpc talkingNpc = ClassicWorldNpcCatalog.fixedMerchant(
                0, ClassicWorldNpcCatalog.objectId(1324));
        assertEquals("shipEvI6", ClassicWorldMerchantCatalog.dialogueHtml(
                talkingNpc, 0, 6 * 3_600 - 1));
        assertEquals("shipEvI3", ClassicWorldMerchantCatalog.dialogueHtml(
                talkingNpc, 0, 6 * 3_600));
        assertEquals("shipEvI3", ClassicWorldMerchantCatalog.dialogueHtml(
                talkingNpc, 0, 20 * 3_600));
        assertEquals("shipEvI6", ClassicWorldMerchantCatalog.dialogueHtml(
                talkingNpc, 0, 20 * 3_600 + 1));

        ClassicWorldMerchantCatalog.Merchant gludio = ClassicWorldMerchantCatalog.find(
                ClassicWorldNpcCatalog.npc(
                        4, ClassicWorldNpcCatalog.objectId(1325)));
        assertNotNull(gludio);
        assertEquals(70022, gludio.npcId());
        assertEquals(32560, gludio.x());
        assertEquals(32723, gludio.y());
        assertEquals("shipEvM1", gludio.initialHtml());
        assertEquals(300, gludio.offers().get(0).unitPrice());
        assertSame(InventoryCatalog.require(
                        InventoryCatalog.SHIP_TICKET_TO_TALKING_ISLAND),
                gludio.offers().get(0).definition());
    }

    @Test
    void nativeBoardingAndDepartureWindowsKeepExactFourHourBoundaries() {
        ShipRouteCatalog.Boarding talking = ShipRouteCatalog.boarding(0);
        assertFalse(talking.openAt(8 * 3_600 - 1));
        assertTrue(talking.openAt(8 * 3_600));
        assertTrue(talking.openAt(9 * 3_600 - 1));
        assertFalse(talking.openAt(9 * 3_600));
        assertTrue(talking.openAt(12 * 3_600));
        assertTrue(talking.openAt(20 * 3_600));
        assertFalse(talking.openAt(21 * 3_600));

        ShipRouteCatalog.Boarding gludio = ShipRouteCatalog.boarding(1);
        assertFalse(gludio.openAt(6 * 3_600 - 1));
        assertTrue(gludio.openAt(6 * 3_600));
        assertTrue(gludio.openAt(7 * 3_600 - 1));
        assertFalse(gludio.openAt(7 * 3_600));
        assertTrue(gludio.openAt(10 * 3_600));
        assertTrue(gludio.openAt(18 * 3_600));
        assertFalse(gludio.openAt(22 * 3_600));

        ShipRouteCatalog.Route map5 = ShipRouteCatalog.resolve(5, 4, 32564, 32723);
        assertFalse(map5.openAt(9 * 3_600 - 1));
        assertTrue(map5.openAt(9 * 3_600));
        assertTrue(map5.openAt(9 * 3_600 + 59));
        assertFalse(map5.openAt(9 * 3_600 + 60));
        assertTrue(map5.openAt(21 * 3_600));

        ShipRouteCatalog.Route map6 = ShipRouteCatalog.resolve(6, 0, 32630, 32972);
        assertFalse(map6.openAt(7 * 3_600 - 1));
        assertTrue(map6.openAt(7 * 3_600));
        assertTrue(map6.openAt(7 * 3_600 + 59));
        assertFalse(map6.openAt(7 * 3_600 + 60));
        assertTrue(map6.openAt(19 * 3_600));
        assertThrows(IllegalArgumentException.class, () -> talking.openAt(-1));
        assertThrows(IllegalArgumentException.class, () -> map6.openAt(86_400));
    }

    @Test
    void selectedDungeonRowsAndGetbackDestinationsCoverBothRoundTrips() {
        assertEquals(12, ClassicWorldDungeonCatalog.all().stream()
                .filter(route -> route.routeId() >= 143 && route.routeId() <= 154)
                .count());
        assertDungeon(0, 32630, 32983, 5, 32733, 32797, 40299);
        assertDungeon(5, 32734, 32796, 0, 32631, 32982, 40299);
        assertDungeon(4, 32544, 32728, 6, 32735, 32795, 40298);
        assertDungeon(6, 32736, 32794, 4, 32544, 32727, 40298);

        assertEquals(new ClassicWorldGetbackCatalog.Destination(0, 32574, 32941, 1),
                ClassicWorldGetbackCatalog.resolve(0, 32600, 32900, 0, 0));
        assertEquals(new ClassicWorldGetbackCatalog.Destination(4, 32624, 32813, 1),
                ClassicWorldGetbackCatalog.resolve(5, 32733, 32797, 0, 0));
        assertEquals(new ClassicWorldGetbackCatalog.Destination(0, 32574, 32941, 3),
                ClassicWorldGetbackCatalog.resolve(6, 32735, 32795, 0, 0));
    }

    private static void assertDungeon(int sourceMap, int sourceX, int sourceY,
                                      int destinationMap, int destinationX,
                                      int destinationY, long ticket) {
        ClassicWorldDungeonCatalog.Route route = ClassicWorldDungeonCatalog.find(
                sourceMap, sourceX, sourceY);
        assertNotNull(route);
        assertEquals(destinationMap, route.destinationMapId());
        assertEquals(destinationX, route.destinationX());
        assertEquals(destinationY, route.destinationY());
        assertEquals(ticket, route.requiredItemCatalogId());
        assertTrue(ClassicWorldMapCatalog.forMap(destinationMap)
                .standingPassable(destinationX, destinationY));
    }

    private static void assertMap(int mapId, int startX, int endX,
                                  int startY, int endY, String sourceHash,
                                  String resourceHash) throws Exception {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(mapId);
        assertEquals(startX, map.startX());
        assertEquals(endX, map.endX());
        assertEquals(startY, map.startY());
        assertEquals(endY, map.endY());
        assertEquals(sourceHash, map.sourceSha256());
        assertTrue(map.allowsEscape());
        assertTrue(map.allowsPetRecall());
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = ClassicWorldMainlandShipCatalogTest.class
                .getResourceAsStream("/maps/" + mapId + ".l38.gz")) {
            assertNotNull(input);
            digest.update(input.readAllBytes());
        }
        assertEquals(resourceHash,
                HexFormat.of().withUpperCase().formatHex(digest.digest()));
    }
}
