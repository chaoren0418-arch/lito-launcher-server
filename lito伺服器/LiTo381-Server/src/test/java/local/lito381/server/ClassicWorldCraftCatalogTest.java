package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class ClassicWorldCraftCatalogTest {
    @Test
    void selectedNerupaCatalogPinsAllRecipesAndItemWireRows() throws Exception {
        List<ClassicWorldCraftCatalog.Recipe> recipes =
                ClassicWorldCraftCatalog.recipes(17, 70838);
        assertEquals(32, recipes.size());
        assertTrue(recipes.stream().allMatch(recipe -> recipe.amountInputable()
                && recipe.successRate() == 1_000 && recipe.failItemId() == 0
                && recipe.minimumLevel() == 0));
        assertTrue(recipes.stream().flatMap(recipe -> recipe.materials().stream())
                .allMatch(item -> item.enchant() == -1));
        assertTrue(recipes.stream().flatMap(recipe -> recipe.results().stream())
                .allMatch(item -> item.enchant() == 0));
        assertNull(ClassicWorldCraftCatalog.find(4, 70838, "request bow"));
        assertNull(ClassicWorldCraftCatalog.find(17, 70838, "request absent"));

        long distinctItems = recipes.stream()
                .flatMap(recipe -> java.util.stream.Stream.concat(
                        recipe.materials().stream(), recipe.results().stream()))
                .map(ClassicWorldCraftCatalog.MakingItem::itemId).distinct().count();
        assertEquals(58, distinctItems);
        for (ClassicWorldCraftCatalog.Recipe recipe : recipes) {
            for (ClassicWorldCraftCatalog.MakingItem item
                    : java.util.stream.Stream.concat(
                            recipe.materials().stream(), recipe.results().stream()).toList()) {
                assertNotNull(ClassicWorldCraftCatalog.inventoryDefinition(item.itemId()));
                assertNotNull(InventoryCatalog.find(item.itemId()));
            }
        }

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream(
                "/world/map17-crafting.lwc.gz")) {
            assertNotNull(in);
            digest.update(in.readAllBytes());
        }
        assertEquals(ClassicWorldCraftCatalog.NERUPA_RESOURCE_SHA256,
                HexFormat.of().withUpperCase().formatHex(digest.digest()));
    }

    @Test
    void retainedBowRecipeComputesExactMaximumFromBothMaterials() {
        ClassicWorldCraftCatalog.Recipe bow =
                ClassicWorldCraftCatalog.find(17, 70838, "request bow");
        assertNotNull(bow);
        assertEquals(List.of(40507, 40502), bow.materials().stream()
                .map(ClassicWorldCraftCatalog.MakingItem::itemId).toList());
        assertEquals(List.of(1, 5), bow.materials().stream()
                .map(ClassicWorldCraftCatalog.MakingItem::count).toList());
        assertEquals(List.of(172), bow.results().stream()
                .map(ClassicWorldCraftCatalog.MakingItem::itemId).toList());

        RuntimeInventory inventory = RuntimeInventory.fromCatalogCounts(
                Map.of(40507L, 2, 40502L, 10), new java.util.concurrent.atomic.AtomicLong(
                        0x7100_0000L)::getAndIncrement);
        assertEquals(2, ClassicWorldCraftCatalog.maximumAmount(inventory, bow));
        assertFalse(InventoryCatalog.find(172).stackable());
        assertNotNull(InventoryCatalog.nativeWeaponStatus(172));
    }
}
