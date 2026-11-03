package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class NativeEtcItemTypeCatalogTest {
    @Test
    void retainedPotionAndFoodRowsStayHashLocked() {
        NativeEtcItemTypeCatalog.ResourceContract contract =
                NativeEtcItemTypeCatalog.resourceContract();
        assertEquals(126, contract.potionCount());
        assertEquals(98, contract.foodCount());
        assertEquals(NativeEtcItemTypeCatalog.POTION,
                NativeEtcItemTypeCatalog.typeOf(40010));
        assertEquals(NativeEtcItemTypeCatalog.FOOD,
                NativeEtcItemTypeCatalog.typeOf(40056));
        assertEquals(-1, NativeEtcItemTypeCatalog.typeOf(
                InventoryCatalog.ADENA));
        assertTrue(NativeEtcItemTypeCatalog.airbornePickupEligible(
                InventoryCatalog.builtIn(
                        InventoryCatalog.NATIVE_HEALING_POTION)));
        assertFalse(NativeEtcItemTypeCatalog.airbornePickupEligible(
                InventoryCatalog.builtIn(InventoryCatalog.ADENA)));
    }
}
