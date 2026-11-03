package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ClassicMerchantCatalogTest {
    @Test
    void exposesExactPandoraSubsetInNativeOrder() {
        List<ClassicMerchantCatalog.Offer> offers = ClassicMerchantCatalog.offers();

        assertEquals(List.of(0L, 1L, 2L, 3L),
                offers.stream().map(ClassicMerchantCatalog.Offer::orderId).toList());
        assertEquals(List.of(24L, 40010L, 40013L, 40017L),
                offers.stream().map(offer -> offer.definition().catalogId()).toList());
        assertEquals(List.of(110L, 37L, 200L, 70L),
                offers.stream().map(ClassicMerchantCatalog.Offer::unitPrice).toList());
        assertEquals("$100", ClassicMerchantCatalog.find(0).definition().name());
        assertEquals("$26", ClassicMerchantCatalog.find(1).definition().name());
        assertNull(ClassicMerchantCatalog.find(4));
    }

    @Test
    void usesServerOwnedSellPricesAndRejectsNull() {
        assertEquals(55, ClassicMerchantCatalog.sellPrice(
                InventoryCatalog.require(InventoryCatalog.SHORT_SWORD)));
        assertEquals(18, ClassicMerchantCatalog.sellPrice(
                InventoryCatalog.require(InventoryCatalog.NATIVE_HEALING_POTION)));
        assertEquals(0, ClassicMerchantCatalog.sellPrice(null));
    }
}
