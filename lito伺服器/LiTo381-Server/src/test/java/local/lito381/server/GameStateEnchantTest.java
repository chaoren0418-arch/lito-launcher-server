package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GameStateEnchantTest {
    @TempDir Path temporary;

    @Test void persistsWeaponEnchantmentsWithoutWritingTheLegacyDatabase() throws Exception {
        Path file = temporary.resolve("enchant-state.properties");
        GameStateStore state = new GameStateStore(file, true);
        Map<Long, Integer> inventory = Map.of(
                InventoryCatalog.SHORT_SWORD, 1,
                InventoryCatalog.MAGIC_GEM, 10);
        state.saveEconomy("TestMage", 5_000, inventory,
                Map.of(InventoryCatalog.SHORT_SWORD, List.of(1)));

        GameStateStore reloaded = new GameStateStore(file, true);
        GameStateStore.Economy economy = reloaded.economy("TestMage");
        assertEquals(inventory, economy.inventory());
        assertEquals(Map.of(InventoryCatalog.SHORT_SWORD, List.of(1)),
                economy.inventoryEnchantments());
        String durable = Files.readString(file);
        assertTrue(durable.contains("format.version=43"));
        assertTrue(durable.contains("inventory.enchantments=24\\:1"));

        reloaded.saveEconomy("TestMage", 5_000, inventory);
        assertEquals(Map.of(InventoryCatalog.SHORT_SWORD, List.of(1)),
                reloaded.economy("TestMage").inventoryEnchantments());
        reloaded.saveEconomy("TestMage", 5_000,
                Map.of(InventoryCatalog.MAGIC_GEM, 10));
        assertTrue(reloaded.economy("TestMage").inventoryEnchantments().isEmpty());
        assertFalse(Files.readString(file).contains("inventory.enchantments=24\\:1"));
    }

    @Test void formatTwentyFourRoundTripsEveryMutableNativeItemField() throws Exception {
        Path file = temporary.resolve("complete-item-state.properties");
        GameStateStore state = new GameStateStore(file, true);
        Map<Long, Integer> inventory = Map.of(InventoryCatalog.SHORT_SWORD, 1);
        RuntimeInventory.PersistentItem expected = new RuntimeInventory.PersistentItem(
                1, 1, 3, false, RuntimeInventory.BLESSING_CURSED);

        state.saveEconomyState("ExactItem", 99, inventory,
                Map.of(InventoryCatalog.SHORT_SWORD, List.of(expected)));

        GameStateStore.Economy durable = new GameStateStore(file, true)
                .economy("ExactItem");
        assertEquals(Map.of(InventoryCatalog.SHORT_SWORD, List.of(expected)),
                durable.itemStates());
        RuntimeInventory restored = RuntimeInventory.fromDurableState(
                durable.inventory(), durable.itemStates());
        RuntimeInventory.Item sword = restored.items().get(0);
        assertEquals(1, sword.enchantLevel());
        assertEquals(3, sword.durability());
        assertFalse(sword.identified());
        assertEquals(RuntimeInventory.BLESSING_CURSED, sword.blessing());
        assertTrue(Files.readString(file).contains(
                "inventory.items=24\\:1~1~3~0~2~0"));
    }

    @Test void formatThirtyFourRoundTripsTournamentRewardProvenance() throws Exception {
        Path file = temporary.resolve("tournament-reward-state.properties");
        GameStateStore state = new GameStateStore(file, true);
        RuntimeInventory inventory = new RuntimeInventory();
        InventoryCatalog.Definition potion = InventoryCatalog.require(
                InventoryCatalog.NATIVE_HEALING_POTION);
        assertTrue(inventory.add(potion, 5));
        assertTrue(inventory.addTournamentReward(potion, 30));

        state.saveEconomyState("RaceWinner", 20_000,
                inventory.catalogCounts(), inventory.catalogItemStates());
        GameStateStore.Economy durable = new GameStateStore(file, true)
                .economy("RaceWinner");
        RuntimeInventory restored = RuntimeInventory.fromDurableState(
                durable.inventory(), durable.itemStates());

        assertEquals(List.of(false, true), restored.items().stream()
                .map(RuntimeInventory.Item::tournamentReward).toList());
        assertTrue(Files.readString(file).contains("format.version=43"));
        assertTrue(Files.readString(file).contains("~1"));
    }
}
