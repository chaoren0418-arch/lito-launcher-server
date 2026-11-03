package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BattlePetRaceRewardCatalogTest {
    @Test
    void selectedDatabaseWeightsCoverExactlyOneTenThousandRoll() {
        assertEquals(BattlePetRaceRewardCatalog.TOTAL_WEIGHT,
                BattlePetRaceRewardCatalog.heroPumpkinBagLoot().stream()
                        .mapToInt(BattlePetRaceRewardCatalog.Loot::weight).sum());
        assertEquals(6, BattlePetRaceRewardCatalog.heroPumpkinBagLoot().size());
        assertEquals(BattlePetRaceRewardCatalog.SKELETON_HOLY_GRAIL,
                BattlePetRaceRewardCatalog.rollHeroPumpkinBag(0).catalogId());
        assertEquals(InventoryCatalog.NATIVE_HEALING_POTION,
                BattlePetRaceRewardCatalog.rollHeroPumpkinBag(1_000).catalogId());
        assertEquals(30,
                BattlePetRaceRewardCatalog.rollHeroPumpkinBag(1_999).quantity());
        assertEquals(InventoryCatalog.POLYMORPH_SCROLL,
                BattlePetRaceRewardCatalog.rollHeroPumpkinBag(9_999).catalogId());
        assertThrows(IllegalArgumentException.class,
                () -> BattlePetRaceRewardCatalog.rollHeroPumpkinBag(-1));
        assertThrows(IllegalArgumentException.class,
                () -> BattlePetRaceRewardCatalog.rollHeroPumpkinBag(10_000));
    }

    @Test
    void onlyTheSourceProvenRaceBagCrossesRoundReset() {
        assertTrue(BattlePetRaceRewardCatalog.isDurablePrize(
                BattlePetRaceRewardCatalog.HERO_PUMPKIN_BAG));
        assertEquals("$5287", InventoryCatalog.require(
                BattlePetRaceRewardCatalog.HERO_PUMPKIN_BAG).name());
        assertEquals(943, InventoryCatalog.require(
                BattlePetRaceRewardCatalog.HERO_PUMPKIN_BAG).icon());
        assertEquals(2599, InventoryCatalog.require(
                BattlePetRaceRewardCatalog.SKELETON_HOLY_GRAIL).icon());
    }
}
