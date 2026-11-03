package local.lito381.server;

import java.util.List;

/** Source-pinned PET_RACE prize and Hero's Pumpkin Bag contents. */
final class BattlePetRaceRewardCatalog {
    static final long HERO_PUMPKIN_BAG = 41308;
    static final long SKELETON_HOLY_GRAIL = 41251;
    static final int TOTAL_WEIGHT = 10_000;

    record Loot(long catalogId, int quantity, int weight) {
        Loot {
            if (catalogId <= 0 || quantity <= 0 || weight <= 0) {
                throw new IllegalArgumentException("invalid PET_RACE prize loot");
            }
        }
    }

    /* Exact selected 380_db etcitem_box rows 501118..501123. */
    private static final List<Loot> HERO_PUMPKIN_BAG_LOOT = List.of(
            new Loot(SKELETON_HOLY_GRAIL, 1, 1_000),
            new Loot(InventoryCatalog.NATIVE_HEALING_POTION, 30, 1_000),
            new Loot(InventoryCatalog.NATIVE_HEALING_POTION, 15, 2_000),
            new Loot(InventoryCatalog.EXTRA_HEALING_POTION, 15, 2_000),
            new Loot(InventoryCatalog.GREATER_HEALING_POTION, 15, 2_000),
            new Loot(InventoryCatalog.POLYMORPH_SCROLL, 6, 2_000));

    static {
        int total = HERO_PUMPKIN_BAG_LOOT.stream().mapToInt(Loot::weight).sum();
        if (total != TOTAL_WEIGHT) {
            throw new IllegalStateException("PET_RACE prize weights must total 10000");
        }
    }

    private BattlePetRaceRewardCatalog() {}

    static List<Loot> heroPumpkinBagLoot() {
        return HERO_PUMPKIN_BAG_LOOT;
    }

    static Loot rollHeroPumpkinBag(int roll) {
        if (roll < 0 || roll >= TOTAL_WEIGHT) {
            throw new IllegalArgumentException("PET_RACE prize roll outside 0..9999");
        }
        int boundary = 0;
        for (Loot loot : HERO_PUMPKIN_BAG_LOOT) {
            boundary += loot.weight();
            if (roll < boundary) return loot;
        }
        throw new IllegalStateException("PET_RACE prize weights contain a gap");
    }

    /** Only server-awarded unopened bags cross the Tournament round reset. */
    static boolean isDurablePrize(long catalogId) {
        return catalogId == HERO_PUMPKIN_BAG;
    }
}
