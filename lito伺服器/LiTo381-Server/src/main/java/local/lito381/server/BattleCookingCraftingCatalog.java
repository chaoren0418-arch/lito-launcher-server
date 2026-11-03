package local.lito381.server;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Exact same-family level-one through level-three cooking-book recipes. */
final class BattleCookingCraftingCatalog {
    static final int COOLDOWN_SECONDS = 3;
    static final int FIRE_EFFECT_GFX = 5943;
    static final int NORMAL_EFFECT_GFX = 6392;
    static final int SPECIAL_EFFECT_GFX = 6390;
    static final int FAILURE_EFFECT_GFX = 6394;

    static final long LEVEL_ONE_BOOK = 41255;
    static final long LEVEL_TWO_BOOK = 41256;
    static final long LEVEL_THREE_BOOK = 41257;

    record Ingredient(long catalogId, int count) {
        Ingredient {
            if (catalogId <= 0 || catalogId > 0xFFFF_FFFFL || count <= 0) {
                throw new IllegalArgumentException("invalid cooking ingredient");
            }
        }
    }

    enum Outcome { NORMAL, SPECIAL, FAILURE }

    record Recipe(int cookNo, int level, long cookbookCatalogId,
                  List<Ingredient> ingredients, long normalCatalogId,
                  long specialCatalogId, int normalMaximumRoll,
                  int specialMaximumRoll) {
        Recipe {
            if (cookNo < 0 || cookNo >= 24 || level < 1 || level > 3
                    || cookbookCatalogId != LEVEL_ONE_BOOK + level - 1
                    || cookNo / 8 + 1 != level || ingredients == null
                    || ingredients.isEmpty() || normalCatalogId <= 0
                    || specialCatalogId <= 0 || normalMaximumRoll <= 0
                    || normalMaximumRoll >= specialMaximumRoll
                    || specialMaximumRoll > 100) {
                throw new IllegalArgumentException("invalid classic cooking-book recipe");
            }
            ingredients = List.copyOf(ingredients);
            BattleCookingCatalog.Recipe normal = BattleCookingCatalog.require(normalCatalogId);
            BattleCookingCatalog.Recipe special = BattleCookingCatalog.require(specialCatalogId);
            if (normal.special() || !special.special()) {
                throw new IllegalArgumentException("cooking products do not form a normal/special pair");
            }
        }

        int selection() {
            return cookNo & 7;
        }

        Outcome outcome(int roll) {
            if (roll < 1 || roll > 100) {
                throw new IllegalArgumentException("cooking roll must be 1..100");
            }
            if (roll <= normalMaximumRoll) return Outcome.NORMAL;
            if (roll <= specialMaximumRoll) return Outcome.SPECIAL;
            return Outcome.FAILURE;
        }
    }

    private static final Map<Integer, Recipe> RECIPES;

    static {
        Map<Integer, Recipe> recipes = new LinkedHashMap<>();
        // Level one: the retained source consumes these materials one at a
        // time and uses the same global cookNo values returned by its window.
        register(recipes, recipe(0, ingredients(40057), 41277, 41285));
        register(recipes, recipe(1, ingredients(41275), 41278, 41286));
        register(recipes, recipe(2, ingredients(41263, 41265), 41279, 41287));
        register(recipes, recipe(3, ingredients(41274, 41267), 41280, 41288));
        register(recipes, recipe(4, ingredients(40062, 40069, 40064), 41281, 41289));
        register(recipes, recipe(5, ingredients(40056, 40060, 40061), 41282, 41290));
        register(recipes, recipe(6, ingredients(41276), 41283, 41291));
        register(recipes, recipe(7, ingredients(40499, 40060), 41284, 41292));

        // Level two. Recipe 14 is an exact source exception: 30% normal,
        // 35% special and 35% failure instead of the usual 90/5/5 split.
        register(recipes, recipe(8, ingredients(49040, 49048), 49049, 49057));
        register(recipes, recipe(9, ingredients(49041, 49048), 49050, 49058));
        register(recipes, recipe(10, ingredients(49042, 41265, 49048), 49051, 49059));
        register(recipes, recipe(11, ingredients(49043, 49048), 49052, 49060));
        register(recipes, recipe(12, ingredients(49044, 49048), 49053, 49061));
        register(recipes, recipe(13, ingredients(49045, 49048), 49054, 49062));
        register(recipes, new Recipe(14, 2, LEVEL_TWO_BOOK,
                ingredients(49046, 49048), 49055, 49063, 30, 65));
        register(recipes, recipe(15, ingredients(49047, 40499, 49048), 49056, 49064));

        // Level three uses one sauce, one coriander and one meat/egg item.
        register(recipes, recipe(16, ingredients(49048, 49243, 49260), 49244, 49252));
        register(recipes, recipe(17, ingredients(49048, 49243, 49261), 49245, 49253));
        register(recipes, recipe(18, ingredients(49048, 49243, 49262), 49246, 49254));
        register(recipes, recipe(19, ingredients(49048, 49243, 49263), 49247, 49255));
        register(recipes, recipe(20, ingredients(49048, 49243, 49264), 49248, 49256));
        register(recipes, recipe(21, ingredients(49048, 49243, 49265), 49249, 49257));
        register(recipes, recipe(22, ingredients(49048, 49243, 49266), 49250, 49258));
        register(recipes, recipe(23, ingredients(49048, 49243, 49267), 49251, 49259));
        RECIPES = Map.copyOf(recipes);
    }

    private BattleCookingCraftingCatalog() {}

    static Recipe find(int cookNo) {
        return RECIPES.get(cookNo);
    }

    static Recipe require(int cookNo) {
        Recipe recipe = find(cookNo);
        if (recipe == null) throw new IllegalArgumentException("unknown cooking recipe " + cookNo);
        return recipe;
    }

    static int size() {
        return RECIPES.size();
    }

    private static Recipe recipe(int cookNo, List<Ingredient> ingredients,
                                 long normalCatalogId, long specialCatalogId) {
        int level = cookNo / 8 + 1;
        return new Recipe(cookNo, level, LEVEL_ONE_BOOK + level - 1,
                ingredients, normalCatalogId, specialCatalogId, 90, 95);
    }

    private static List<Ingredient> ingredients(long... catalogIds) {
        return java.util.Arrays.stream(catalogIds)
                .mapToObj(catalogId -> new Ingredient(catalogId, 1)).toList();
    }

    private static void register(Map<Integer, Recipe> recipes, Recipe recipe) {
        if (recipes.putIfAbsent(recipe.cookNo(), recipe) != null) {
            throw new IllegalStateException("duplicate cooking recipe " + recipe.cookNo());
        }
    }
}
