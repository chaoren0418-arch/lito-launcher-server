package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HexFormat;
import org.junit.jupiter.api.Test;

final class ClassicWorldMerchantCatalogTest {
    @Test
    void duranPinsExactSelectedNpcHtmlAndSingleTicketOffer() {
        ClassicWorldNpcCatalog.StaticNpc npc = ClassicWorldNpcCatalog.fixedMerchant(
                445, ClassicWorldNpcCatalog.objectId(601));
        ClassicWorldMerchantCatalog.Merchant merchant =
                ClassicWorldMerchantCatalog.find(npc);
        assertNotNull(merchant);
        assertEquals(445, merchant.mapId());
        assertEquals(601, merchant.spawnId());
        assertEquals(70094, merchant.npcId());
        assertEquals(32753, merchant.x());
        assertEquals(32860, merchant.y());
        assertEquals(3, merchant.maximumDistance());
        assertEquals("duran", merchant.initialHtml());
        assertEquals("duran2", merchant.localShopHtml());
        assertEquals("buy", merchant.buyAction());
        assertEquals(1, merchant.offers().size());

        ClassicWorldMerchantCatalog.Offer offer = merchant.offers().get(0);
        assertEquals(0, offer.orderId());
        assertEquals(690, offer.unitPrice());
        assertSame(InventoryCatalog.require(
                InventoryCatalog.SHIP_TICKET_PIRATE_ISLAND), offer.definition());
        assertEquals("$3439", offer.definition().name());
        assertEquals(138, offer.definition().icon());
        assertEquals(151, offer.definition().groundGfx());
        assertEquals(1_000, offer.definition().unitWeight());
        assertTrue(offer.definition().tradable());
        assertEquals(InventoryCatalog.Effect.SHIP_TICKET,
                offer.definition().effect());

        assertEquals("00000000_8A00_B2020000_24333433390000".replace("_", ""),
                HexFormat.of().withUpperCase().formatHex(
                        new Protocol.Writer().d(offer.wireRow().orderId())
                                .h(offer.wireRow().icon())
                                .d(offer.wireRow().unitPrice())
                                .s(offer.wireRow().name())
                                .c(offer.wireRow().detail().length)
                                .raw(offer.wireRow().detail()).bytes()));
    }

    @Test
    void catalogRejectsUnrelatedFixedMerchant() {
        assertNull(ClassicWorldMerchantCatalog.find(
                ClassicWorldNpcCatalog.fixedMerchant(4, 0x6400_04FCL)));
    }
}
