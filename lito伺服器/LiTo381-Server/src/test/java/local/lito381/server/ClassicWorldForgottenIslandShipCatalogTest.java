package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ClassicWorldForgottenIslandShipCatalogTest {
    @Test
    void selectedMapsSeventyEightyThreeAndEightyFourAreHashLocked()
            throws Exception {
        assertEquals(List.of(70, 83, 84),
                ClassicWorldMapCatalog.FORGOTTEN_ISLAND_ROUTE_MAP_IDS);
        assertMap(70, 32576, 33023, 32640, 33087,
                ClassicWorldMapCatalog.MAP70_SOURCE_SHA256,
                ClassicWorldMapCatalog.MAP70_RESOURCE_SHA256);
        assertMap(83, 32704, 32831, 32768, 32831,
                ClassicWorldMapCatalog.MAP83_SOURCE_SHA256,
                ClassicWorldMapCatalog.MAP83_RESOURCE_SHA256);
        assertMap(84, 32704, 32767, 32768, 32831,
                ClassicWorldMapCatalog.MAP84_SOURCE_SHA256,
                ClassicWorldMapCatalog.MAP84_RESOURCE_SHA256);
        assertEquals("10048B2E32C5897D075F362EEE02B357C118B2C03AB5E981B424D37A91CF2DD2",
                ClassicWorldMapCatalog.FORGOTTEN_ISLAND_FLAGS_SOURCE_SHA256);
    }

    @Test
    void forgottenIslandContentAndEmptyShipInteriorsRemainIndependent() {
        assertEquals(5, ClassicWorldNpcCatalog.all(70).size());
        assertTrue(ClassicWorldNpcCatalog.all(83).isEmpty());
        assertTrue(ClassicWorldNpcCatalog.all(84).isEmpty());
        assertEquals(66, ClassicWorldMonsterCatalog.definitions(70).size());
        assertEquals(506, ClassicWorldMonsterCatalog.definitions(70).stream()
                .mapToInt(ClassicWorldMonsterCatalog.SpawnDefinition::count).sum());
        assertTrue(ClassicWorldMonsterCatalog.definitions(83).isEmpty());
        assertTrue(ClassicWorldMonsterCatalog.definitions(84).isEmpty());
        assertEquals(475, ClassicWorldDropCatalog.dropCount(70));
        assertEquals(45, ClassicWorldDropCatalog.items(70).size());
        assertEquals(0, ClassicWorldDropCatalog.dropCount(83));
        assertEquals(0, ClassicWorldDropCatalog.dropCount(84));
    }

    @Test
    void exactTicketMerchantsSellTheTwoFifteenThousandAdenaTickets() {
        assertEquals("72F87522054BB2E181E2CD309F27C9BF3764B534B207A9F832452B41DABD4BFE",
                ClassicWorldMerchantCatalog.FORGOTTEN_ISLAND_TICKET_MERCHANT_SOURCE_SHA256);
        ClassicWorldMerchantCatalog.Merchant isvall = ClassicWorldMerchantCatalog.find(
                ClassicWorldNpcCatalog.npc(4,
                        ClassicWorldNpcCatalog.objectId(
                                ClassicWorldMerchantCatalog.ISVALL_SPAWN_ID)));
        assertNotNull(isvall);
        assertEquals("isvall4", isvall.initialHtml());
        assertEquals("isvall1", isvall.localShopHtml());
        assertEquals(15_000, isvall.offers().get(0).unitPrice());
        assertSame(InventoryCatalog.require(
                        InventoryCatalog.SHIP_TICKET_FORGOTTEN_ISLAND),
                isvall.offers().get(0).definition());

        ClassicWorldMerchantCatalog.Merchant oblivion = ClassicWorldMerchantCatalog.find(
                ClassicWorldNpcCatalog.npc(70,
                        ClassicWorldNpcCatalog.objectId(
                                ClassicWorldMerchantCatalog.OBLIVION_SPAWN_ID)));
        assertNotNull(oblivion);
        assertEquals("oblivion1", oblivion.initialHtml());
        assertEquals("oblivion2", oblivion.localShopHtml());
        assertEquals(15_000, oblivion.offers().get(0).unitPrice());
        assertSame(InventoryCatalog.require(InventoryCatalog.SHIP_TICKET_HEINE),
                oblivion.offers().get(0).definition());
    }

    @Test
    void nativeC59DeparturesUseTheExactOneMinuteFourHourWindows() {
        ShipRouteCatalog.Route map83 = ShipRouteCatalog.resolve(
                83, 70, 32936, 33052);
        assertNotNull(map83);
        assertEquals(InventoryCatalog.SHIP_TICKET_FORGOTTEN_ISLAND,
                map83.ticketCatalogId());
        assertFalse(map83.openAt(7 * 3_600 - 1));
        assertTrue(map83.openAt(7 * 3_600));
        assertTrue(map83.openAt(7 * 3_600 + 59));
        assertFalse(map83.openAt(7 * 3_600 + 60));
        assertTrue(map83.openAt(19 * 3_600));

        ShipRouteCatalog.Route map84 = ShipRouteCatalog.resolve(
                84, 4, 33426, 33489);
        assertNotNull(map84);
        assertEquals(InventoryCatalog.SHIP_TICKET_HEINE,
                map84.ticketCatalogId());
        assertFalse(map84.openAt(9 * 3_600 - 1));
        assertTrue(map84.openAt(9 * 3_600));
        assertTrue(map84.openAt(9 * 3_600 + 59));
        assertFalse(map84.openAt(9 * 3_600 + 60));
        assertTrue(map84.openAt(21 * 3_600));
    }

    @Test
    void selectedGroundRoutesAndGetbacksCoverTheFullRoundTrip() {
        assertEquals(16, ClassicWorldDungeonCatalog.all().stream()
                .filter(route -> route.routeId() >= 155 && route.routeId() <= 170)
                .count());
        assertDungeon(4, 33423, 33502, 83, 32735, 32795,
                InventoryCatalog.SHIP_TICKET_FORGOTTEN_ISLAND);
        // SQL route 158 says x=33735, outside map83; the adjacent four-row
        // native source envelope proves this normalized x=32735 destination.
        assertDungeon(4, 33426, 33502, 83, 32735, 32795,
                InventoryCatalog.SHIP_TICKET_FORGOTTEN_ISLAND);
        assertDungeon(70, 32935, 33058, 84, 32734, 32797,
                InventoryCatalog.SHIP_TICKET_HEINE);
        assertDungeon(83, 32735, 32794, 4, 33425, 33501,
                InventoryCatalog.SHIP_TICKET_FORGOTTEN_ISLAND);
        assertDungeon(84, 32734, 32796, 70, 32936, 33057,
                InventoryCatalog.SHIP_TICKET_HEINE);
        assertDungeon(70, 32945, 33012, 70, 32826, 32850, 0);

        assertEquals("2B420AC3956D183D069A7549841578724661630363C3944A3114C4B8D3862C2C",
                ClassicWorldGetbackCatalog.FORGOTTEN_ISLAND_ROUTE_SOURCE_SHA256);
        assertEquals(new ClassicWorldGetbackCatalog.Destination(
                        70, 32828, 32848, 0),
                ClassicWorldGetbackCatalog.resolve(70, 32936, 33057, 1, 0));
        assertEquals(new ClassicWorldGetbackCatalog.Destination(
                        4, 33593, 33242, 8),
                ClassicWorldGetbackCatalog.resolve(83, 32735, 32795, 1, 0));
        assertEquals(new ClassicWorldGetbackCatalog.Destination(
                        4, 33604, 33237, 0),
                ClassicWorldGetbackCatalog.resolve(84, 32734, 32797, 1, 0));
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
        try (InputStream input = ClassicWorldForgottenIslandShipCatalogTest.class
                .getResourceAsStream("/maps/" + mapId + ".l38.gz")) {
            assertNotNull(input);
            digest.update(input.readAllBytes());
        }
        assertEquals(resourceHash,
                HexFormat.of().withUpperCase().formatHex(digest.digest()));
    }
}
