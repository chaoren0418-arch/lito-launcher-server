package local.lito381.server;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NativeLitoRentalMerchantCatalogTest {
    @Test void retainedRowsCoverEverySelectedClientRentalEquipmentResource() {
        List<NativeLitoRentalMerchantCatalog.Item> items =
                NativeLitoRentalMerchantCatalog.all();
        assertEquals(78, items.size());
        assertEquals(78, items.stream().map(item -> item.resourceIndex())
                .collect(Collectors.toSet()).size());
        assertEquals(78, items.stream().map(item -> item.sourceItemId())
                .collect(Collectors.toSet()).size());
        for (int resourceIndex = 13; resourceIndex <= 90; resourceIndex++) {
            int expected = resourceIndex;
            NativeLitoRentalMerchantCatalog.Item item = items.stream()
                    .filter(row -> row.resourceIndex() == expected)
                    .findFirst().orElseThrow();
            assertEquals(20_000 + resourceIndex, item.definition().icon());
            assertEquals(3_600, item.maximumSeconds());
            assertTrue(item.definition().identified());
            assertFalse(item.definition().tradable());
            assertTrue(item.definition().cantDelete());
            assertTrue(item.buyListDetail().length > 0);
        }
        assertFalse(NativeLitoRentalMerchantCatalog.HISTORICAL_OFFICIAL_EXACT);
        assertTrue(NativeLitoRentalMerchantCatalog
                .RETAINED_SERVER_COMPATIBILITY_AVAILABLE);
    }

    @Test void fourSelectedClientMerchantsExposeOneDisjointCompleteCatalog() {
        List<NativeLitoRentalMerchantCatalog.Item> weapons =
                NativeLitoRentalMerchantCatalog.offeredBy(2);
        List<NativeLitoRentalMerchantCatalog.Item> primaryArmor =
                NativeLitoRentalMerchantCatalog.offeredBy(10);
        List<NativeLitoRentalMerchantCatalog.Item> secondaryArmor =
                NativeLitoRentalMerchantCatalog.offeredBy(11);
        List<NativeLitoRentalMerchantCatalog.Item> accessories =
                NativeLitoRentalMerchantCatalog.offeredBy(12);
        assertEquals(25, weapons.size());
        assertEquals(24, primaryArmor.size());
        assertEquals(12, secondaryArmor.size());
        assertEquals(17, accessories.size());
        Set<Integer> joined = java.util.stream.Stream.of(
                        weapons, primaryArmor, secondaryArmor, accessories)
                .flatMap(List::stream)
                .map(NativeLitoRentalMerchantCatalog.Item::resourceIndex)
                .collect(Collectors.toSet());
        assertEquals(78, joined.size());
        assertEquals("黑魔石匕首", weapons.get(0).definition().name());
        assertEquals(10, weapons.get(0).lightGemPrice());
        assertEquals("權能的皇冠", primaryArmor.get(0).definition().name());
        assertEquals(5, primaryArmor.get(0).lightGemPrice());
        assertEquals("太陽盾牌", secondaryArmor.get(0).definition().name());
        assertEquals(10, secondaryArmor.get(0).lightGemPrice());
        assertEquals("權力魔法項鍊", accessories.get(0).definition().name());
        assertEquals(4, accessories.get(0).lightGemPrice());
        assertNotNull(NativeLitoRentalMerchantCatalog.findBySourceItemId(29052));
        assertTrue(NativeLitoRentalMerchantCatalog.offeredBy(91).isEmpty());
    }
}
