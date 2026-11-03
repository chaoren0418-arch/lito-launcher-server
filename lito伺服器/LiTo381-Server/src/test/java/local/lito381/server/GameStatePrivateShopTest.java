package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GameStatePrivateShopTest {
    @TempDir Path temporary;

    @Test void persistsBothPrivateShopEconomiesInOneSave() throws Exception {
        Path file = temporary.resolve("private-shop-state.properties");
        GameStateStore state = new GameStateStore(file, true);
        state.saveEconomy("Seller", 5_000,
                Map.of(InventoryCatalog.ADENA, 100_000, 1002L, 3));
        state.saveEconomy("Buyer", 4_000,
                Map.of(InventoryCatalog.ADENA, 100_000, 1002L, 2));

        state.saveEconomies("Seller", 5_000,
                Map.of(InventoryCatalog.ADENA, 100_100, 1002L, 2),
                "Buyer", 4_000,
                Map.of(InventoryCatalog.ADENA, 99_900, 1002L, 3));

        GameStateStore reloaded = new GameStateStore(file, true);
        assertEquals(Map.of(InventoryCatalog.ADENA, 100_100, 1002L, 2),
                reloaded.economy("Seller").inventory());
        assertEquals(Map.of(InventoryCatalog.ADENA, 99_900, 1002L, 3),
                reloaded.economy("Buyer").inventory());
    }

    @Test void rejectsAliasingTheSameDurableProfile() throws Exception {
        GameStateStore state = new GameStateStore(
                temporary.resolve("unused.properties"), false);
        assertThrows(IllegalArgumentException.class, () -> state.saveEconomies(
                "Same", 1, Map.of(1L, 1), "same", 2, Map.of(2L, 1)));
    }

    @Test void atomicallyMovesCompleteItemStateBetweenPrivateShopProfiles()
            throws Exception {
        Path file = temporary.resolve("private-shop-exact-state.properties");
        GameStateStore state = new GameStateStore(file, true);
        RuntimeInventory.PersistentItem sword = new RuntimeInventory.PersistentItem(
                1, 1, 2, false, RuntimeInventory.BLESSING_CURSED);
        state.saveEconomyState("Seller", 5_000,
                Map.of(InventoryCatalog.SHORT_SWORD, 1),
                Map.of(InventoryCatalog.SHORT_SWORD, List.of(sword)));
        state.saveEconomyState("Buyer", 4_000, Map.of(), Map.of());

        state.saveEconomyStates("Seller", 5_100, Map.of(), Map.of(),
                "Buyer", 3_900, Map.of(InventoryCatalog.SHORT_SWORD, 1),
                Map.of(InventoryCatalog.SHORT_SWORD, List.of(sword)));

        GameStateStore reloaded = new GameStateStore(file, true);
        assertEquals(Map.of(), reloaded.economy("Seller").itemStates());
        assertEquals(Map.of(InventoryCatalog.SHORT_SWORD, List.of(sword)),
                reloaded.economy("Buyer").itemStates());
    }

    @Test
    void dynamicSourceTemplateSurvivesPrivateShopAtomicRestart() throws Exception {
        Path file = temporary.resolve("private-shop-source-template.properties");
        GameStateStore state = new GameStateStore(file, true);
        InventoryCatalog.Definition definition = new InventoryCatalog.Definition(
                65_001, 0, 321, 322, 1, true,
                "Private Shop Source Token", "retained source detail", 17, 9,
                true, true, BattleActionCodes.WALK, 0,
                InventoryCatalog.Effect.NONE, 87, 1_250, 3, null, null);
        RuntimeInventory seller = new RuntimeInventory();
        long objectId = seller.addDistinctState(definition,
                new RuntimeInventory.PersistentItem(
                        0, 2, 0, 0, true,
                        RuntimeInventory.BLESSING_NORMAL, true));
        RuntimeInventory buyer = new RuntimeInventory();
        RuntimeInventory.Item offered = seller.item(objectId);
        assertTrue(buyer.add(offered, 1));
        assertTrue(seller.remove(objectId, 1));

        state.saveEconomyStates(
                "Seller", 5_100, seller.catalogCounts(), seller.catalogItemStates(),
                "Buyer", 3_900, buyer.catalogCounts(), buyer.catalogItemStates());

        GameStateStore reloaded = new GameStateStore(file, true);
        for (String character : List.of("Seller", "Buyer")) {
            GameStateStore.Economy economy = reloaded.economy(character);
            RuntimeInventory restored = RuntimeInventory.fromDurableState(
                    economy.inventory(), economy.itemStates());
            RuntimeInventory.Item item = restored.items().get(0);
            assertEquals(definition, item.definition());
            assertEquals(1, item.quantity());
            assertTrue(item.sourceBound());
        }
    }
}
