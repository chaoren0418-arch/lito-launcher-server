package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

final class BattleCookingCraftingTest {
    @Test void pinsCookingBookAndIngredientNativeInventoryRows() {
        int[] bookIcons = {2498, 2499, 2500};
        int[] bookGround = {5994, 5995, 5996};
        int[] bookDescriptions = {2552, 2553, 2554};
        for (int level = 0; level < 3; level++) {
            InventoryCatalog.Definition book = InventoryCatalog.require(41255 + level);
            assertEquals(0xF8, book.useType());
            assertEquals(bookIcons[level], book.icon());
            assertEquals(bookGround[level], book.groundGfx());
            assertEquals(bookDescriptions[level], book.itemDescriptionId());
            assertEquals(1_000, book.unitWeight());
            assertEquals(InventoryCatalog.Effect.COOKING_BOOK, book.effect());
            assertFalse(book.stackable());
        }

        for (int cookNo = 0; cookNo < 24; cookNo++) {
            for (BattleCookingCraftingCatalog.Ingredient ingredient
                    : BattleCookingCraftingCatalog.require(cookNo).ingredients()) {
                assertEquals(ingredient.catalogId(),
                        InventoryCatalog.require(ingredient.catalogId()).catalogId());
            }
        }
        assertNativeIngredient(40057, 38, 1205, 3176, 9, 10_000);
        assertNativeIngredient(41265, 0, 2488, 5985, 2564, 5_860);
        assertNativeIngredient(49046, 38, 2727, 36, 2778, 10_000);
        assertNativeIngredient(49048, 0, 2492, 5988, 2778, 10_000);
        assertNativeIngredient(49243, 0, 513, 1490, 3618, 1_000);
        assertNativeIngredient(49260, 0, 2941, 6550, 3632, 10_000);
        assertNativeIngredient(49267, 0, 2932, 6542, 3634, 10_000);
    }

    @Test void pinsAllTwentyFourRecipeProductsBooksAndIngredientMatrix() {
        long[][] ingredients = {
                {40057}, {41275}, {41263, 41265}, {41274, 41267},
                {40062, 40069, 40064}, {40056, 40060, 40061}, {41276}, {40499, 40060},
                {49040, 49048}, {49041, 49048}, {49042, 41265, 49048},
                {49043, 49048}, {49044, 49048}, {49045, 49048},
                {49046, 49048}, {49047, 40499, 49048},
                {49048, 49243, 49260}, {49048, 49243, 49261},
                {49048, 49243, 49262}, {49048, 49243, 49263},
                {49048, 49243, 49264}, {49048, 49243, 49265},
                {49048, 49243, 49266}, {49048, 49243, 49267}
        };
        long[] normal = {41277, 41278, 41279, 41280, 41281, 41282, 41283, 41284,
                49049, 49050, 49051, 49052, 49053, 49054, 49055, 49056,
                49244, 49245, 49246, 49247, 49248, 49249, 49250, 49251};
        long[] special = {41285, 41286, 41287, 41288, 41289, 41290, 41291, 41292,
                49057, 49058, 49059, 49060, 49061, 49062, 49063, 49064,
                49252, 49253, 49254, 49255, 49256, 49257, 49258, 49259};

        assertEquals(24, BattleCookingCraftingCatalog.size());
        for (int cookNo = 0; cookNo < 24; cookNo++) {
            BattleCookingCraftingCatalog.Recipe recipe =
                    BattleCookingCraftingCatalog.require(cookNo);
            assertEquals(cookNo / 8 + 1, recipe.level());
            assertEquals(41255 + cookNo / 8, recipe.cookbookCatalogId());
            assertEquals(cookNo & 7, recipe.selection());
            assertEquals(normal[cookNo], recipe.normalCatalogId());
            assertEquals(special[cookNo], recipe.specialCatalogId());
            assertEquals(java.util.Arrays.stream(ingredients[cookNo]).boxed().toList(),
                    recipe.ingredients().stream()
                            .map(BattleCookingCraftingCatalog.Ingredient::catalogId).toList());
            assertTrue(recipe.ingredients().stream().allMatch(row -> row.count() == 1));
        }
        assertEquals(3, BattleCookingCraftingCatalog.COOLDOWN_SECONDS);
        assertEquals(5943, BattleCookingCraftingCatalog.FIRE_EFFECT_GFX);
    }

    @Test void pinsTheLevelTwoSpiderRecipeProbabilityException() {
        BattleCookingCraftingCatalog.Recipe ordinary =
                BattleCookingCraftingCatalog.require(13);
        assertEquals(BattleCookingCraftingCatalog.Outcome.NORMAL, ordinary.outcome(90));
        assertEquals(BattleCookingCraftingCatalog.Outcome.SPECIAL, ordinary.outcome(95));
        assertEquals(BattleCookingCraftingCatalog.Outcome.FAILURE, ordinary.outcome(96));

        BattleCookingCraftingCatalog.Recipe spider =
                BattleCookingCraftingCatalog.require(14);
        assertEquals(30, spider.normalMaximumRoll());
        assertEquals(65, spider.specialMaximumRoll());
        assertEquals(BattleCookingCraftingCatalog.Outcome.NORMAL, spider.outcome(30));
        assertEquals(BattleCookingCraftingCatalog.Outcome.SPECIAL, spider.outcome(31));
        assertEquals(BattleCookingCraftingCatalog.Outcome.SPECIAL, spider.outcome(65));
        assertEquals(BattleCookingCraftingCatalog.Outcome.FAILURE, spider.outcome(66));
        assertThrows(IllegalArgumentException.class, () -> spider.outcome(0));
        assertThrows(IllegalArgumentException.class, () -> spider.outcome(101));
    }

    @Test void resolvesNormalSpecialAndFailureAsAtomicInventoryTransitions() {
        Map<Long, Integer> source = counts(49042, 2, 41265, 1, 49048, 3);
        BattleCookingCrafting.Result normal = BattleCookingCrafting.attempt(
                source, 10, 90, true, true, false);
        assertEquals(BattleCookingCrafting.Status.NORMAL, normal.status());
        assertEquals(49051, normal.outputCatalogId());
        assertEquals(6392, normal.effectGfx());
        assertEquals(1, normal.catalogCounts().get(49042L));
        assertNull(normal.catalogCounts().get(41265L));
        assertEquals(2, normal.catalogCounts().get(49048L));
        assertEquals(1, normal.catalogCounts().get(49051L));
        assertTrue(normal.cooldownStarted());
        assertEquals(counts(49042, 2, 41265, 1, 49048, 3), source);

        BattleCookingCrafting.Result special = BattleCookingCrafting.attempt(
                counts(49042, 1, 41265, 1, 49048, 1),
                10, 91, true, true, false);
        assertEquals(BattleCookingCrafting.Status.SPECIAL, special.status());
        assertEquals(49059, special.outputCatalogId());
        assertEquals(6390, special.effectGfx());
        assertEquals(Map.of(49059L, 1), special.catalogCounts());

        BattleCookingCrafting.Result failure = BattleCookingCrafting.attempt(
                counts(49042, 1, 41265, 1, 49048, 1),
                10, 96, true, true, false);
        assertEquals(BattleCookingCrafting.Status.FAILURE, failure.status());
        assertEquals(0, failure.outputCatalogId());
        assertEquals(1101, failure.messageId());
        assertEquals(6394, failure.effectGfx());
        assertTrue(failure.catalogCounts().isEmpty());
        assertTrue(failure.inventoryChanged());
    }

    @Test void preservesSourceGateOrderAndCooldownSemantics() {
        Map<Long, Integer> insufficient = counts(40057, 1);
        BattleCookingCrafting.Result fire = BattleCookingCrafting.attempt(
                insufficient, 2, 1, false, false, true);
        assertEquals(BattleCookingCrafting.Status.FIRE_REQUIRED, fire.status());
        assertEquals(1160, fire.messageId());
        assertFalse(fire.cooldownStarted());

        BattleCookingCrafting.Result full = BattleCookingCrafting.attempt(
                insufficient, 2, 1, true, false, true);
        assertEquals(BattleCookingCrafting.Status.INVENTORY_FULL, full.status());
        assertEquals(1103, full.messageId());
        assertFalse(full.cooldownStarted());

        BattleCookingCrafting.Result cooldown = BattleCookingCrafting.attempt(
                insufficient, 2, 1, true, true, true);
        assertEquals(BattleCookingCrafting.Status.COOLDOWN, cooldown.status());
        assertFalse(cooldown.cooldownStarted());

        BattleCookingCrafting.Result materials = BattleCookingCrafting.attempt(
                insufficient, 2, 1, true, true, false);
        assertEquals(BattleCookingCrafting.Status.INGREDIENTS_MISSING, materials.status());
        assertEquals(1102, materials.messageId());
        assertTrue(materials.cooldownStarted());
        assertFalse(materials.inventoryChanged());
        assertEquals(insufficient, materials.catalogCounts());

        BattleCookingCrafting.Result unknown = BattleCookingCrafting.attempt(
                insufficient, 24, 1, true, true, false);
        assertEquals(BattleCookingCrafting.Status.UNKNOWN_RECIPE, unknown.status());
        assertTrue(unknown.cooldownStarted());

        BattleCookingCrafting.Result unknownWithoutFire = BattleCookingCrafting.attempt(
                insufficient, 24, 1, false, true, false);
        assertEquals(BattleCookingCrafting.Status.FIRE_REQUIRED,
                unknownWithoutFire.status());
        assertFalse(unknownWithoutFire.cooldownStarted());
    }

    @Test void appliesRecipeToRuntimeInventoryWithoutRebuildingUnaffectedObjects() {
        AtomicLong ids = new AtomicLong(0x7100_0000L);
        RuntimeInventory before = RuntimeInventory.fromCatalogCounts(
                counts(49042, 2, 41265, 1, 49048, 3), ids::getAndIncrement);
        long meatObject = objectId(before, 49042);
        long sauceObject = objectId(before, 49048);
        long corianderObject = objectId(before, 41265);

        BattleCookingCrafting.InventoryResult transition =
                BattleCookingCrafting.attempt(before, 10, 91, true, true, false);
        RuntimeInventory after = transition.inventory();

        assertEquals(BattleCookingCrafting.Status.SPECIAL,
                transition.result().status());
        assertEquals(2, before.item(meatObject).quantity());
        assertEquals(3, before.item(sauceObject).quantity());
        assertEquals(1, before.item(corianderObject).quantity());
        assertEquals(1, after.item(meatObject).quantity());
        assertEquals(2, after.item(sauceObject).quantity());
        assertNull(after.item(corianderObject));
        assertEquals(meatObject, objectId(after, 49042));
        assertEquals(sauceObject, objectId(after, 49048));
        assertEquals(1, after.catalogCounts().get(49059L));
        assertEquals(transition.result().catalogCounts(), after.catalogCounts());
    }

    private static Map<Long, Integer> counts(long... pairs) {
        if ((pairs.length & 1) != 0) throw new IllegalArgumentException("pairs required");
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            result.put(pairs[index], Math.toIntExact(pairs[index + 1]));
        }
        return result;
    }

    private static void assertNativeIngredient(long catalogId, int useType,
                                               int icon, int groundGfx,
                                               int itemDescriptionId, int unitWeight) {
        InventoryCatalog.Definition item = InventoryCatalog.require(catalogId);
        assertEquals(useType, item.useType());
        assertEquals(icon, item.icon());
        assertEquals(groundGfx, item.groundGfx());
        assertEquals(itemDescriptionId, item.itemDescriptionId());
        assertEquals(unitWeight, item.unitWeight());
        assertTrue(item.stackable());
        assertEquals(InventoryCatalog.Effect.NONE, item.effect());
    }

    private static long objectId(RuntimeInventory inventory, long catalogId) {
        return inventory.items().stream()
                .filter(item -> item.definition().catalogId() == catalogId)
                .findFirst().orElseThrow().objectId();
    }
}
