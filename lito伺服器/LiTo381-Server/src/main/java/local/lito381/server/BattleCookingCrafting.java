package local.lito381.server;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Same-family transactional cooking reference model.
 *
 * <p>This class deliberately has no production caller.  The selected
 * LiTo.bin only emits {@code C_USE_ITEM cds(objectId, empty)} for cooking
 * books and has no compatible recipe-selector receiver.  Keep the recovered
 * 24-recipe rules testable without presenting them as a native LiTo flow.</p>
 */
final class BattleCookingCrafting {
    enum Status {
        UNKNOWN_RECIPE, FIRE_REQUIRED, INVENTORY_FULL, COOLDOWN,
        INGREDIENTS_MISSING, NORMAL, SPECIAL, FAILURE
    }

    record Result(Status status, BattleCookingCraftingCatalog.Recipe recipe,
                  long outputCatalogId, int messageId, int effectGfx,
                  boolean cooldownStarted, Map<Long, Integer> catalogCounts) {
        Result {
            if (status == null || catalogCounts == null || outputCatalogId < 0
                    || outputCatalogId > 0xFFFF_FFFFL || messageId < 0
                    || effectGfx < 0) {
                throw new IllegalArgumentException("invalid cooking result");
            }
            catalogCounts = Map.copyOf(catalogCounts);
        }

        boolean inventoryChanged() {
            return status == Status.NORMAL || status == Status.SPECIAL
                    || status == Status.FAILURE;
        }
    }

    record InventoryResult(Result result, RuntimeInventory inventory) {
        InventoryResult {
            if (result == null || inventory == null) {
                throw new IllegalArgumentException("invalid runtime cooking result");
            }
            inventory = inventory.copy();
        }
    }

    private BattleCookingCrafting() {}

    static Result attempt(Map<Long, Integer> catalogCounts, int cookNo, int roll,
                          boolean nearFire, boolean inventoryAdmissionAllowed,
                          boolean cooldownActive) {
        Map<Long, Integer> before = validatedCounts(catalogCounts);
        BattleCookingCraftingCatalog.Recipe recipe =
                BattleCookingCraftingCatalog.find(cookNo);
        if (roll < 1 || roll > 100) {
            throw new IllegalArgumentException("cooking roll must be 1..100");
        }
        // Same-family Cooking_Book checks these gates before starting the
        // three-second COOKING_NOW lease or consuming any ingredient.
        if (!nearFire) {
            return result(Status.FIRE_REQUIRED, recipe, 0, 1160, 0, false, before);
        }
        if (!inventoryAdmissionAllowed) {
            return result(Status.INVENTORY_FULL, recipe, 0, 1103, 0, false, before);
        }
        if (cooldownActive) {
            return result(Status.COOLDOWN, recipe, 0, 0, 0, false, before);
        }
        // The retained handler starts COOKING_NOW before its switch. A forged
        // unknown cookNo therefore consumes the three-second lease but has no
        // inventory or packet side effect.
        if (recipe == null) {
            return result(Status.UNKNOWN_RECIPE, null, 0, 0, 0, true, before);
        }
        for (BattleCookingCraftingCatalog.Ingredient ingredient : recipe.ingredients()) {
            if (before.getOrDefault(ingredient.catalogId(), 0) < ingredient.count()) {
                return result(Status.INGREDIENTS_MISSING, recipe, 0, 1102, 0,
                        true, before);
            }
        }

        Map<Long, Integer> after = new LinkedHashMap<>(before);
        for (BattleCookingCraftingCatalog.Ingredient ingredient : recipe.ingredients()) {
            int remaining = after.get(ingredient.catalogId()) - ingredient.count();
            if (remaining == 0) after.remove(ingredient.catalogId());
            else after.put(ingredient.catalogId(), remaining);
        }

        BattleCookingCraftingCatalog.Outcome outcome = recipe.outcome(roll);
        if (outcome == BattleCookingCraftingCatalog.Outcome.FAILURE) {
            return result(Status.FAILURE, recipe, 0, 1101,
                    BattleCookingCraftingCatalog.FAILURE_EFFECT_GFX, true, after);
        }
        boolean special = outcome == BattleCookingCraftingCatalog.Outcome.SPECIAL;
        long output = special ? recipe.specialCatalogId() : recipe.normalCatalogId();
        after.merge(output, 1, Math::addExact);
        return result(special ? Status.SPECIAL : Status.NORMAL, recipe, output, 0,
                special ? BattleCookingCraftingCatalog.SPECIAL_EFFECT_GFX
                        : BattleCookingCraftingCatalog.NORMAL_EFFECT_GFX,
                true, after);
    }

    /**
     * Applies the same transition to native object-id inventory without
     * rebuilding unaffected objects. This is intentionally not a network
     * entry point: the selected LiTo client still has no proved cookNo sender.
     */
    static InventoryResult attempt(RuntimeInventory inventory, int cookNo, int roll,
                                   boolean nearFire, boolean inventoryAdmissionAllowed,
                                   boolean cooldownActive) {
        if (inventory == null) throw new IllegalArgumentException("cooking inventory is null");
        Result result = attempt(inventory.catalogCounts(), cookNo, roll,
                nearFire, inventoryAdmissionAllowed, cooldownActive);
        RuntimeInventory after = inventory.copy();
        if (result.inventoryChanged()) {
            for (BattleCookingCraftingCatalog.Ingredient ingredient
                    : result.recipe().ingredients()) {
                if (!after.removeCatalog(ingredient.catalogId(), ingredient.count())) {
                    throw new IllegalStateException("validated cooking ingredient vanished");
                }
            }
            if (result.outputCatalogId() != 0) {
                InventoryCatalog.Definition output =
                        InventoryCatalog.find(result.outputCatalogId());
                if (output == null || !after.add(output, 1)) {
                    throw new IllegalStateException("validated cooking output was not admitted");
                }
            }
            if (!after.catalogCounts().equals(result.catalogCounts())) {
                throw new IllegalStateException("runtime cooking transition diverged from counts");
            }
        }
        return new InventoryResult(result, after);
    }

    private static Map<Long, Integer> validatedCounts(Map<Long, Integer> counts) {
        Map<Long, Integer> result = new LinkedHashMap<>();
        if (counts == null) return result;
        for (Map.Entry<Long, Integer> entry : counts.entrySet()) {
            long catalogId = entry.getKey() == null ? 0 : entry.getKey();
            int count = entry.getValue() == null ? 0 : entry.getValue();
            if (catalogId <= 0 || catalogId > 0xFFFF_FFFFL || count <= 0) {
                throw new IllegalArgumentException("invalid cooking inventory count");
            }
            result.put(catalogId, count);
        }
        return result;
    }

    private static Result result(Status status,
                                 BattleCookingCraftingCatalog.Recipe recipe,
                                 long outputCatalogId, int messageId, int effectGfx,
                                 boolean cooldownStarted, Map<Long, Integer> counts) {
        return new Result(status, recipe, outputCatalogId, messageId, effectGfx,
                cooldownStarted, counts);
    }
}
