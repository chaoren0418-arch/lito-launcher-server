package local.lito381.server;

import java.util.List;

/** Exact retained Dragon Gem rows and their fixed per-kill EXP awards. */
final class BattleExperienceGemCatalog {
    static final long DRAGON_DIAMOND = 49_549L;
    static final long DRAGON_SAPPHIRE = 49_550L;
    static final long DRAGON_RUBY = 49_551L;
    static final int SKILL_ID = 4_026;
    static final int MINIMUM_LEVEL = 49;
    static final int DURATION_SECONDS = 600;
    static final int LEGACY_ICON_GRAPHIC_ID = 21;

    record Gem(long itemId, String nameId, int inventoryGfx, int groundGfx,
               int bonusExperience, int durationSeconds) {
        Gem {
            if (itemId <= 0 || itemId > 0xFFFF_FFFFL || nameId == null
                    || nameId.isBlank() || inventoryGfx < 0 || inventoryGfx > 0xFFFF
                    || groundGfx < 0 || groundGfx > 0xFFFF
                    || bonusExperience <= 0 || durationSeconds <= 0
                    || durationSeconds > 0xFFFF) {
                throw new IllegalArgumentException("invalid retained Dragon Gem row");
            }
        }
    }

    private static final List<Gem> GEMS = List.of(
            new Gem(DRAGON_DIAMOND, "$7969", 3_721, 3_963,
                    3_000, DURATION_SECONDS),
            new Gem(DRAGON_SAPPHIRE, "$7970", 3_727, 3_963,
                    2_000, DURATION_SECONDS),
            new Gem(DRAGON_RUBY, "$7971", 3_725, 3_963,
                    1_000, DURATION_SECONDS));

    private BattleExperienceGemCatalog() {}

    static List<Gem> all() {
        return GEMS;
    }

    static Gem find(long itemId) {
        return GEMS.stream().filter(gem -> gem.itemId() == itemId)
                .findFirst().orElse(null);
    }

    static Gem require(long itemId) {
        Gem gem = find(itemId);
        if (gem == null) throw new IllegalArgumentException("unknown Dragon Gem " + itemId);
        return gem;
    }

    static Gem findByBonus(int bonusExperience) {
        return GEMS.stream().filter(gem -> gem.bonusExperience() == bonusExperience)
                .findFirst().orElse(null);
    }
}
