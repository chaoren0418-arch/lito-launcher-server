package local.lito381.server;

import java.util.LinkedHashMap;
import java.util.Map;

/** Same-family Food3 status resources used by the native LiTo item-use path. */
final class BattleCookingCatalog {
    static final int DURATION_SECONDS = 900;

    static final long FLOATING_EYE_STEAK = 41277;
    static final long ROAST_BEAR_MEAT = 41278;
    static final long NUT_PANCAKE = 41279;
    static final long ANT_LEG_CHEESE = 41280;
    static final long FRUIT_SALAD = 41281;
    static final long SWEET_SOUR_MEAT = 41282;
    static final long BOAR_SKEWER = 41283;
    static final long MUSHROOM_SOUP = 41284;
    static final long SPECIAL_FLOATING_EYE_STEAK = 41285;
    static final long SPECIAL_ROAST_BEAR_MEAT = 41286;
    static final long SPECIAL_NUT_PANCAKE = 41287;
    static final long SPECIAL_ANT_LEG_CHEESE = 41288;
    static final long SPECIAL_FRUIT_SALAD = 41289;
    static final long SPECIAL_SWEET_SOUR_MEAT = 41290;
    static final long SPECIAL_BOAR_SKEWER = 41291;
    static final long SPECIAL_MUSHROOM_SOUP = 41292;

    static final long CAVIAR = 49049;
    static final long ALLIGATOR_STEAK = 49050;
    static final long TURTLE_DRAGON_COOKIE = 49051;
    static final long KIWI_PARROT_ROAST = 49052;
    static final long SCORPION_SKEWER = 49053;
    static final long ELECTON_STEW = 49054;
    static final long SPIDER_LEG_SKEWER = 49055;
    static final long CRAB_SOUP = 49056;
    static final long SPECIAL_CAVIAR = 49057;
    static final long SPECIAL_ALLIGATOR_STEAK = 49058;
    static final long SPECIAL_TURTLE_DRAGON_COOKIE = 49059;
    static final long SPECIAL_KIWI_PARROT_ROAST = 49060;
    static final long SPECIAL_SCORPION_SKEWER = 49061;
    static final long SPECIAL_ELECTON_STEW = 49062;
    static final long SPECIAL_SPIDER_LEG_SKEWER = 49063;
    static final long SPECIAL_CRAB_SOUP = 49064;

    static final long QUESTANUX_CLAW = 49244;
    static final long GRIFFON_ROAST = 49245;
    static final long ARIAN_TAIL_STEAK = 49246;
    static final long GIANT_TURTLE_ROAST = 49247;
    static final long BABY_DRAGON_WING = 49248;
    static final long GRILLED_WYVERN = 49249;
    static final long DEEP_SEA_FISH_STEW = 49250;
    static final long EVIL_LIZARD_EGG_SOUP = 49251;
    static final long SPECIAL_QUESTANUX_CLAW = 49252;
    static final long SPECIAL_GRIFFON_ROAST = 49253;
    static final long SPECIAL_ARIAN_TAIL_STEAK = 49254;
    static final long SPECIAL_GIANT_TURTLE_ROAST = 49255;
    static final long SPECIAL_BABY_DRAGON_WING = 49256;
    static final long SPECIAL_GRILLED_WYVERN = 49257;
    static final long SPECIAL_DEEP_SEA_FISH_STEW = 49258;
    static final long SPECIAL_EVIL_LIZARD_EGG_SOUP = 49259;

    static final long STRENGTH_STEAK = 49825;
    static final long DEXTERITY_SALMON = 49826;
    static final long CHARCOAL_TURKEY = 49827;
    static final long HEALTH_CHICKEN_SOUP = 49828;

    static final int COOKING_1_2_N = 3002;
    static final int COOKING_1_2_S = 3010;
    static final int COOKING_2_4_N = 3020;
    static final int COOKING_2_4_S = 3028;
    static final int COOKING_3_5_N = 3037;
    static final int COOKING_3_5_S = 3045;

    enum Slot { MAIN_COURSE, DESSERT }

    record Recipe(long catalogId, int statusId, int cookingType, Slot slot,
                  int durationSeconds, int maxHp, int maxMp, int armorClass,
                  int allElementResistance, int magicResistance,
                  int hpRegen, int mpRegen, int spellPower,
                  int meleeHit, int meleeDamage,
                  int rangedHit, int rangedDamage,
                  int damageReduction, int experiencePercent, boolean special) {
        Recipe {
            if (catalogId <= 0 || catalogId > 0xFFFF_FFFFL
                    || statusId < 3000 || statusId > 3051
                    || cookingType < 0 || cookingType > 0xFF || slot == null
                    || durationSeconds <= 0 || durationSeconds > 0xFFFF
                    || maxHp < 0 || maxMp < 0
                    || armorClass > 0 || armorClass < -127
                    || allElementResistance < 0 || magicResistance < 0
                    || hpRegen < 0 || mpRegen < 0 || spellPower < 0
                    || meleeHit < 0 || meleeDamage < 0
                    || rangedHit < 0 || rangedDamage < 0
                    || damageReduction < 0
                    || experiencePercent < 0 || experiencePercent > 100) {
                throw new IllegalArgumentException("invalid classic cooking recipe");
            }
            boolean dessertType = cookingType < 24
                    ? (cookingType & 7) == 7 : cookingType == 160;
            if (dessertType != (slot == Slot.DESSERT)) {
                throw new IllegalArgumentException("cooking slot does not match native type");
            }
            if (special != (damageReduction == 5)) {
                throw new IllegalArgumentException("invalid special-cooking reduction");
            }
        }

        boolean dessert() {
            return slot == Slot.DESSERT;
        }
    }

    private static final Map<Long, Recipe> RECIPES;

    static {
        Map<Long, Recipe> recipes = new LinkedHashMap<>();

        // Exact L1Cooking level-one pairs. Normal statuses are 3000..3007;
        // special statuses are 3008..3015 and add the source -5 reduction.
        registerLevelOnePair(recipes, FLOATING_EYE_STEAK,
                SPECIAL_FLOATING_EYE_STEAK, 0,
                0, 0, 0, 10, 0, 0, 0, 0,
                0, 0, 0, 0, 0);
        registerLevelOnePair(recipes, ROAST_BEAR_MEAT,
                SPECIAL_ROAST_BEAR_MEAT, 1,
                30, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0);
        registerLevelOnePair(recipes, NUT_PANCAKE,
                SPECIAL_NUT_PANCAKE, 2,
                0, 0, 0, 0, 0, 0, 3, 0,
                0, 0, 0, 0, 0);
        registerLevelOnePair(recipes, ANT_LEG_CHEESE,
                SPECIAL_ANT_LEG_CHEESE, 3,
                0, 0, -1, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0);
        registerLevelOnePair(recipes, FRUIT_SALAD,
                SPECIAL_FRUIT_SALAD, 4,
                0, 20, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0);
        registerLevelOnePair(recipes, SWEET_SOUR_MEAT,
                SPECIAL_SWEET_SOUR_MEAT, 5,
                0, 0, 0, 0, 0, 3, 0, 0,
                0, 0, 0, 0, 0);
        registerLevelOnePair(recipes, BOAR_SKEWER,
                SPECIAL_BOAR_SKEWER, 6,
                0, 0, 0, 0, 5, 0, 0, 0,
                0, 0, 0, 0, 0);
        registerLevelOnePair(recipes, MUSHROOM_SOUP,
                SPECIAL_MUSHROOM_SOUP, 7,
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 1);

        // Exact level-two L1Cooking/L1AttackList/PcRegenExecutor/CalcExp
        // matrix. Normal statuses are 3016..3023 and special statuses are
        // 3024..3031; every special main course or dessert adds -5 damage.
        registerPair(recipes, CAVIAR, SPECIAL_CAVIAR, 3016, 3024, 8,
                0, 0, 0, 0, 0, 0, 0, 0,
                1, 1, 0, 0, 0);
        registerPair(recipes, ALLIGATOR_STEAK, SPECIAL_ALLIGATOR_STEAK,
                3017, 3025, 9, 30, 30, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0);
        registerPair(recipes, TURTLE_DRAGON_COOKIE,
                SPECIAL_TURTLE_DRAGON_COOKIE, 3018, 3026, 10,
                0, 0, -2, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0);
        registerPair(recipes, KIWI_PARROT_ROAST, SPECIAL_KIWI_PARROT_ROAST,
                3019, 3027, 11, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 1, 1, 0);
        registerPair(recipes, SCORPION_SKEWER, SPECIAL_SCORPION_SKEWER,
                3020, 3028, 12, 0, 0, 0, 0, 0, 2, 2, 0,
                0, 0, 0, 0, 0);
        registerPair(recipes, ELECTON_STEW, SPECIAL_ELECTON_STEW,
                3021, 3029, 13, 0, 0, 0, 0, 10, 0, 0, 0,
                0, 0, 0, 0, 0);
        registerPair(recipes, SPIDER_LEG_SKEWER, SPECIAL_SPIDER_LEG_SKEWER,
                3022, 3030, 14, 0, 0, 0, 0, 0, 0, 0, 1,
                0, 0, 0, 0, 0);
        registerPair(recipes, CRAB_SOUP, SPECIAL_CRAB_SOUP,
                3023, 3031, 15, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 2);

        // Exact level-three matrix. L1AttackList's two repeated level-two
        // SKD1 keys are a source typo: its matching SKU1 rows and the classic
        // item effect establish type 18 as melee hit/damage +2.
        registerPair(recipes, QUESTANUX_CLAW, SPECIAL_QUESTANUX_CLAW,
                3032, 3040, 16, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 1, 1, 0);
        registerPair(recipes, GRIFFON_ROAST, SPECIAL_GRIFFON_ROAST,
                3033, 3041, 17, 50, 50, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0);
        registerPair(recipes, ARIAN_TAIL_STEAK, SPECIAL_ARIAN_TAIL_STEAK,
                3034, 3042, 18, 0, 0, 0, 0, 0, 0, 0, 0,
                2, 2, 0, 0, 0);
        registerPair(recipes, GIANT_TURTLE_ROAST, SPECIAL_GIANT_TURTLE_ROAST,
                3035, 3043, 19, 0, 0, -3, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0);
        registerPair(recipes, BABY_DRAGON_WING, SPECIAL_BABY_DRAGON_WING,
                3036, 3044, 20, 0, 0, 0, 10, 15, 0, 0, 0,
                0, 0, 0, 0, 0);
        registerPair(recipes, GRILLED_WYVERN, SPECIAL_GRILLED_WYVERN,
                3037, 3045, 21, 0, 0, 0, 0, 0, 0, 2, 2,
                0, 0, 0, 0, 0);
        registerPair(recipes, DEEP_SEA_FISH_STEW,
                SPECIAL_DEEP_SEA_FISH_STEW, 3038, 3046, 22,
                30, 0, 0, 0, 0, 2, 0, 0,
                0, 0, 0, 0, 0);
        registerPair(recipes, EVIL_LIZARD_EGG_SOUP,
                SPECIAL_EVIL_LIZARD_EGG_SOUP, 3039, 3047, 23,
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 3);

        // Food3 level four is one 1,800-second main-course trio plus one
        // dessert. All four reduce incoming damage by 2 and add EXP; the
        // first two split melee/ranged hit and damage exactly as SKD/SKU do.
        register(recipes, new Recipe(STRENGTH_STEAK, 3048, 157,
                Slot.MAIN_COURSE, 1_800,
                0, 0, 0, 10, 10, 2, 2, 0,
                1, 2, 0, 0, 2, 2, false));
        register(recipes, new Recipe(DEXTERITY_SALMON, 3049, 158,
                Slot.MAIN_COURSE, 1_800,
                0, 0, 0, 10, 10, 2, 2, 0,
                0, 0, 1, 2, 2, 2, false));
        register(recipes, new Recipe(CHARCOAL_TURKEY, 3050, 159,
                Slot.MAIN_COURSE, 1_800,
                0, 0, 0, 10, 10, 2, 3, 2,
                0, 0, 0, 0, 2, 2, false));
        register(recipes, new Recipe(HEALTH_CHICKEN_SOUP, 3051, 160,
                Slot.DESSERT, 1_800,
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 2, 4, false));
        RECIPES = Map.copyOf(recipes);
    }

    private BattleCookingCatalog() {}

    static Recipe find(long catalogId) {
        return RECIPES.get(catalogId);
    }

    static Recipe require(long catalogId) {
        Recipe recipe = find(catalogId);
        if (recipe == null) throw new IllegalArgumentException("unknown cooking item");
        return recipe;
    }

    static int size() {
        return RECIPES.size();
    }

    static int damageAfterReduction(int damage, int reduction) {
        if (damage < 0 || reduction < 0) {
            throw new IllegalArgumentException("invalid cooking damage reduction");
        }
        return Math.max(0, damage - reduction);
    }

    /** Same-family {@code INT + itemSP}: cooking SP joins the qualified term. */
    static int magicIntelligenceAfterCooking(int qualifiedTerm, int cookingSpellPower) {
        if (qualifiedTerm < 0 || cookingSpellPower < 0) {
            throw new IllegalArgumentException("invalid cooking spell-power term");
        }
        return Math.addExact(qualifiedTerm, cookingSpellPower);
    }

    static long experienceAfterBonus(long baseExperience, int bonusPercent) {
        if (baseExperience < 0 || bonusPercent < 0 || bonusPercent > 100) {
            throw new IllegalArgumentException("invalid cooking experience bonus");
        }
        long bonus = Math.multiplyExact(baseExperience, bonusPercent) / 100;
        return Math.addExact(baseExperience, bonus);
    }

    private static void registerLevelOnePair(
            Map<Long, Recipe> recipes, long normalId, long specialId, int cookingType,
            int maxHp, int maxMp, int armorClass, int allElementResistance,
            int magicResistance, int hpRegen, int mpRegen, int spellPower,
            int meleeHit, int meleeDamage, int rangedHit, int rangedDamage,
            int experiencePercent) {
        registerPair(recipes, normalId, specialId,
                3000 + cookingType, 3008 + cookingType, cookingType,
                maxHp, maxMp, armorClass, allElementResistance, magicResistance,
                hpRegen, mpRegen, spellPower,
                meleeHit, meleeDamage, rangedHit, rangedDamage,
                experiencePercent);
    }

    private static void registerPair(
            Map<Long, Recipe> recipes, long normalId, long specialId,
            int normalStatusId, int specialStatusId, int cookingType,
            int maxHp, int maxMp, int armorClass, int allElementResistance,
            int magicResistance, int hpRegen, int mpRegen, int spellPower,
            int meleeHit, int meleeDamage, int rangedHit, int rangedDamage,
            int experiencePercent) {
        Slot slot = (cookingType & 7) == 7 ? Slot.DESSERT : Slot.MAIN_COURSE;
        register(recipes, new Recipe(normalId, normalStatusId,
                cookingType, slot, DURATION_SECONDS,
                maxHp, maxMp, armorClass, allElementResistance, magicResistance,
                hpRegen, mpRegen, spellPower,
                meleeHit, meleeDamage, rangedHit, rangedDamage,
                0, experiencePercent, false));
        register(recipes, new Recipe(specialId, specialStatusId,
                cookingType, slot, DURATION_SECONDS,
                maxHp, maxMp, armorClass, allElementResistance, magicResistance,
                hpRegen, mpRegen, spellPower,
                meleeHit, meleeDamage, rangedHit, rangedDamage,
                5, experiencePercent, true));
    }

    private static void register(Map<Long, Recipe> recipes, Recipe recipe) {
        if (recipes.put(recipe.catalogId(), recipe) != null) {
            throw new IllegalStateException("duplicate cooking item " + recipe.catalogId());
        }
    }
}
