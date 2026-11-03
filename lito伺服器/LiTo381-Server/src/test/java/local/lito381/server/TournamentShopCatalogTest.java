package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

final class TournamentShopCatalogTest {
    @Test
    void preservesTheSameFamilySixteenRowOrderAndPrices() {
        List<TournamentShopCatalog.Offer> offers = TournamentShopCatalog.offers(true);

        assertEquals(16, offers.size());
        assertEquals(List.of(
                40010L, 40011L, 40013L, 40007L,
                40744L, 40068L, 40017L, 40012L,
                40318L, 40746L, 40014L, 40016L,
                40088L, 40317L, 40015L, 40319L),
                offers.stream().map(TournamentShopCatalog.Offer::catalogId).toList());
        assertEquals(List.of(
                40L, 200L, 200L, 5_000L,
                20L, 1_000L, 70L, 600L,
                900L, 30L, 800L, 600L,
                1_300L, 150L, 700L, 700L),
                offers.stream().map(TournamentShopCatalog.Offer::buyPrice).toList());
        assertEquals(offers.stream().map(TournamentShopCatalog.Offer::buyPrice).toList(),
                offers.stream().map(TournamentShopCatalog.Offer::sellPrice).toList());
        assertEquals(20_000, TournamentShopCatalog.SAME_FAMILY_ALLOWANCE);
        assertNull(TournamentShopCatalog.find(true, InventoryCatalog.TRAINING_SWORD));
    }

    @Test
    void exactSelectedDatabaseMetadataBacksEveryNewRow() {
        assertEquals(5, InventoryCatalog.require(InventoryCatalog.LIGHTNING_WAND).useType());
        assertEquals(14, InventoryCatalog.require(InventoryCatalog.WHETSTONE).useType());
        assertEquals(0xFE, InventoryCatalog.require(InventoryCatalog.SILVER_ARROW).useType());
        assertEquals(0xFE, InventoryCatalog.require(InventoryCatalog.MITHRIL_ARROW).useType());
        assertEquals(7, InventoryCatalog.nativeProjectileStatus(
                InventoryCatalog.SILVER_ARROW).damageSmall());
        assertEquals(10, InventoryCatalog.nativeProjectileStatus(
                InventoryCatalog.MITHRIL_ARROW).damageSmall());
        assertArrayEquals(new int[] {10, 20}, InventoryCatalog.healingRange(
                InventoryCatalog.NATIVE_HEALING_POTION));
        assertArrayEquals(new int[] {30, 50}, InventoryCatalog.healingRange(
                InventoryCatalog.EXTRA_HEALING_POTION));
        assertArrayEquals(new int[] {60, 80}, InventoryCatalog.healingRange(
                InventoryCatalog.GREATER_HEALING_POTION));
        assertEquals(189, InventoryCatalog.healingEffectGfx(
                InventoryCatalog.NATIVE_HEALING_POTION));
        assertEquals(194, InventoryCatalog.healingEffectGfx(
                InventoryCatalog.EXTRA_HEALING_POTION));
        assertEquals(197, InventoryCatalog.healingEffectGfx(
                InventoryCatalog.GREATER_HEALING_POTION));
    }

    @Test
    void databaseRowsControlOrderAndPricesAndRejectAmbiguity() {
        List<TournamentShopCatalog.Offer> offers = TournamentShopCatalog.fromDatabase(List.of(
                new RepositoryTournamentShopOffer(40014, 3, 123, 45),
                new RepositoryTournamentShopOffer(40010, 9, 456, 67)));
        assertEquals(List.of(40014L, 40010L),
                offers.stream().map(TournamentShopCatalog.Offer::catalogId).toList());
        assertEquals(List.of(123L, 456L),
                offers.stream().map(TournamentShopCatalog.Offer::buyPrice).toList());
        assertEquals(45, TournamentShopCatalog.find(offers, 40014).sellPrice());
        assertNull(TournamentShopCatalog.find(offers, 99999));
        assertThrows(IllegalArgumentException.class, () ->
                TournamentShopCatalog.fromDatabase(List.of(
                        new RepositoryTournamentShopOffer(40010, 0, 1, 1),
                        new RepositoryTournamentShopOffer(40010, 1, 1, 1))));
        assertThrows(IllegalArgumentException.class, () ->
                TournamentShopCatalog.fromDatabase(List.of(
                        new RepositoryTournamentShopOffer(40010, 0, 1, 1),
                        new RepositoryTournamentShopOffer(40011, 0, 1, 1))));
    }
}
