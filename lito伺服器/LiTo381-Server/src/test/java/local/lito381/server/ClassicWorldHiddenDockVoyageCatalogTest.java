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

final class ClassicWorldHiddenDockVoyageCatalogTest {
    @Test
    void map446IsHashLockedEmptyAndUsesItsExactMapFlags() throws Exception {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(446);
        assertEquals(32704, map.startX());
        assertEquals(32831, map.endX());
        assertEquals(32768, map.startY());
        assertEquals(32831, map.endY());
        assertEquals(ClassicWorldMapCatalog.MAP446_SOURCE_SHA256,
                map.sourceSha256());
        assertTrue(map.allowsEscape());
        assertTrue(map.allowsPetRecall());
        assertFalse(map.allowsRandomTeleport());
        assertTrue(ClassicWorldNpcCatalog.all(446).isEmpty());
        assertTrue(ClassicWorldMonsterCatalog.definitions(446).isEmpty());
        assertEquals(0, ClassicWorldDropCatalog.dropCount(446));

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = getClass().getResourceAsStream("/maps/446.l38.gz")) {
            assertNotNull(input);
            digest.update(input.readAllBytes());
        }
        assertEquals(ClassicWorldMapCatalog.MAP446_RESOURCE_SHA256,
                HexFormat.of().withUpperCase().formatHex(digest.digest()));
    }

    @Test
    void reconstructedRoxanneSellsTheExactHiddenDockTicket() {
        ClassicWorldNpcCatalog.StaticNpc npc = ClassicWorldNpcCatalog.npc(
                440, ClassicWorldNpcCatalog.objectId(71054));
        assertNotNull(npc);
        assertEquals(71054, npc.npcId());
        assertEquals(List.of(32292, 33067, 4, 2466),
                List.of(npc.x(), npc.y(), npc.heading(), npc.gfxId()));
        assertEquals("$3438", npc.nameId());
        assertEquals("L1Merchant", npc.implementation());
        assertEquals("roxxan", npc.normalAction());

        ClassicWorldMerchantCatalog.Merchant merchant =
                ClassicWorldMerchantCatalog.find(npc);
        assertNotNull(merchant);
        assertEquals("roxxan1", merchant.localShopHtml());
        assertEquals("buy", merchant.buyAction());
        assertEquals(1, merchant.offers().size());
        ClassicWorldMerchantCatalog.Offer offer = merchant.offers().get(0);
        assertEquals(0, offer.orderId());
        assertEquals(690, offer.unitPrice());
        assertSame(InventoryCatalog.require(
                InventoryCatalog.SHIP_TICKET_HIDDEN_DOCK), offer.definition());
        assertEquals("$3440", offer.definition().name());
        assertEquals(138, offer.definition().icon());
        assertEquals(151, offer.definition().groundGfx());
        assertEquals(1_000, offer.definition().unitWeight());
        assertFalse(offer.definition().stackable());
        assertTrue(offer.definition().tradable());
    }

    @Test
    void retainedBoardingRowsAndTimerCompleteBothDirections() {
        assertEquals(613, ClassicWorldDungeonCatalog.PRE_381_ROUTE_COUNT);
        assertEquals(645, ClassicWorldDungeonCatalog.TOTAL_ROUTE_COUNT);
        for (int x = 32296; x <= 32298; x++) {
            assertRoute(440, x, 33087, 446, 32735, 32795, 4);
        }
        assertRoute(446, 32735, 32794, 440, 32297, 33086, 0);
        assertRoute(446, 32736, 32794, 440, 32297, 33086, 0);

        ClassicWorldShipSchedule.Window boarding =
                ClassicWorldShipSchedule.Window.HIDDEN_DOCK_BOARDING;
        assertTrue(boarding.openAt(0));
        assertTrue(boarding.openAt(3_599));
        assertFalse(boarding.openAt(3_600));
        assertTrue(boarding.openAt(10_800));

        ClassicWorldShipDepartureCatalog.Departure outbound =
                ClassicWorldShipDepartureCatalog.find(446);
        assertNotNull(outbound);
        assertEquals(InventoryCatalog.SHIP_TICKET_HIDDEN_DOCK,
                outbound.ticketCatalogId());
        assertEquals(List.of(445, 32751, 32873), List.of(
                outbound.destinationMapId(), outbound.destinationX(),
                outbound.destinationY()));
        assertFalse(outbound.openAt(5_399));
        assertTrue(outbound.openAt(5_400));
        assertTrue(outbound.openAt(7_199));
        assertFalse(outbound.openAt(7_200));

        ClassicWorldShipDepartureCatalog.Departure inbound =
                ClassicWorldShipDepartureCatalog.find(447);
        assertNotNull(inbound);
        assertEquals(InventoryCatalog.SHIP_TICKET_PIRATE_ISLAND,
                inbound.ticketCatalogId());
        assertEquals(List.of(440, 32297, 33086), List.of(
                inbound.destinationMapId(), inbound.destinationX(),
                inbound.destinationY()));
        assertFalse(inbound.openAt(10_799));
        assertTrue(inbound.openAt(10_800));
        assertTrue(inbound.openAt(12_599));
        assertFalse(inbound.openAt(12_600));
    }

    private static void assertRoute(
            int sourceMap, int sourceX, int sourceY,
            int destinationMap, int destinationX, int destinationY,
            int destinationHeading) {
        ClassicWorldDungeonCatalog.Route route = ClassicWorldDungeonCatalog.find(
                sourceMap, sourceX, sourceY);
        assertNotNull(route);
        assertTrue(ClassicWorldDungeonCatalog.isHiddenDockShipRoute(route));
        assertEquals(destinationMap, route.destinationMapId());
        assertEquals(destinationX, route.destinationX());
        assertEquals(destinationY, route.destinationY());
        assertEquals(destinationHeading, route.destinationHeading());
        assertEquals(InventoryCatalog.SHIP_TICKET_HIDDEN_DOCK,
                route.requiredItemCatalogId());
        assertSame(ClassicWorldShipSchedule.Window.HIDDEN_DOCK_BOARDING,
                route.schedule());
        assertTrue(ClassicWorldDungeonCatalog.destinationLandingSafe(route));
    }
}
