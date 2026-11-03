package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

final class RentalItemSetCatalogTest {
    @Test void retainsExactNativeClassSelectionMatrix() {
        assertEquals(13, RentalItemSetCatalog.all().size());
        assertEquals(3, RentalItemSetCatalog.choices(0));
        assertEquals(3, RentalItemSetCatalog.choices(1));
        assertEquals(2, RentalItemSetCatalog.choices(2));
        assertEquals(2, RentalItemSetCatalog.choices(3));
        assertEquals(3, RentalItemSetCatalog.choices(4));
        assertEquals(0, RentalItemSetCatalog.choices(5));
        assertThrows(IllegalArgumentException.class,
                () -> RentalItemSetCatalog.require(2, 2));
        assertEquals("鋼爪型", RentalItemSetCatalog.require(4, 1).label());
    }

    @Test void everyResolvedRentalIdentityNowHasAnInventoryDefinition() {
        Set<Long> identities = RentalItemSetCatalog.all().stream()
                .flatMap(set -> set.items().stream())
                .map(RentalItemSetCatalog.Item::catalogId)
                .collect(Collectors.toSet());
        assertEquals(59, identities.size());
        List<Long> missing = identities.stream().sorted()
                .filter(id -> InventoryCatalog.find(id) == null).toList();
        assertEquals(List.of(), missing, "a resolved rental identity has no wire definition");
        for (RentalItemSetCatalog.Set set : RentalItemSetCatalog.all()) {
            for (RentalItemSetCatalog.Item item : set.items()) {
                InventoryCatalog.Definition definition = item.definitionOrNull();
                assertNotNull(definition);
                assertEquals(item.catalogId(), definition.catalogId());
                assertTrue(definition.weapon() || definition.armor()
                                || definition.projectile(),
                        () -> "unexpected rental item family " + item.catalogId());
            }
        }
    }

    @Test void preservesNativeEnchantAndProjectileQuantities() {
        RentalItemSetCatalog.Set elfRanged = RentalItemSetCatalog.require(2, 0);
        RentalItemSetCatalog.Item arrows = elfRanged.items().stream()
                .filter(item -> item.catalogId() == 40746L).findFirst().orElseThrow();
        assertEquals(2000, arrows.quantity());
        assertEquals(0, arrows.enchantLevel());
        assertEquals(10, elfRanged.items().get(0).enchantLevel());

        RentalItemSetCatalog.Set darkElfRanged = RentalItemSetCatalog.require(4, 2);
        RentalItemSetCatalog.Item stings = darkElfRanged.items().stream()
                .filter(item -> item.catalogId() == 40740L).findFirst().orElseThrow();
        assertEquals(2000, stings.quantity());
    }
}
