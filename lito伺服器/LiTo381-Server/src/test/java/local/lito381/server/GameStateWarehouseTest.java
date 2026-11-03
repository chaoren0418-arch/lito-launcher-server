package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GameStateWarehouseTest {
    @TempDir Path temporary;

    @Test
    void persistsAccountWarehouseAndInventoryTogetherWithConflictGuard() throws Exception {
        Path file = temporary.resolve("warehouse-state.properties");
        GameStateStore state = new GameStateStore(file, true);
        Map<Long, Integer> inventory = new LinkedHashMap<>(
                state.economy("TestKnight").inventory());
        inventory.put(InventoryCatalog.NATIVE_HEALING_POTION, 2);
        assertTrue(state.saveInventoryAndPersonalWarehouse(
                "TestKnight", "SharedAccount", inventory, Map.of(),
                Map.of(InventoryCatalog.SHORT_SWORD, 1)));

        // A second character on the same account sees the native personal
        // warehouse, while a stale capability cannot overwrite it or mutate
        // that character's inventory.
        assertEquals(1, state.personalWarehouse("sharedaccount", "TestMage")
                .inventory().get(InventoryCatalog.SHORT_SWORD));
        Map<Long, Integer> staleInventory = new LinkedHashMap<>(
                state.economy("TestMage").inventory());
        staleInventory.put(InventoryCatalog.NATIVE_HEALING_POTION, 99);
        assertFalse(state.saveInventoryAndPersonalWarehouse(
                "TestMage", "SharedAccount", staleInventory, Map.of(), Map.of()));
        assertFalse(state.economy("TestMage").inventory()
                .containsKey(InventoryCatalog.NATIVE_HEALING_POTION));

        GameStateStore reloaded = new GameStateStore(file, true);
        assertEquals(2, reloaded.economy("TestKnight").inventory()
                .get(InventoryCatalog.NATIVE_HEALING_POTION));
        assertEquals(1, reloaded.personalWarehouse("SharedAccount", "TestKnight").inventory()
                .get(InventoryCatalog.SHORT_SWORD));
        String durable = Files.readString(file);
        assertTrue(durable.contains("format.version=43"));
        assertTrue(durable.contains("warehouse.account.count=1"));
    }

    @Test
    void migratesEveryVersionSixCharacterWarehouseForTheAccount() throws Exception {
        Path file = temporary.resolve("legacy-warehouse-state.properties");
        Files.writeString(file, """
                format.version=6
                profile.LegacyOne.name=LegacyOne
                profile.LegacyOne.inventory=40308:100
                profile.LegacyOne.warehouse=24:1
                profile.LegacyTwo.name=LegacyTwo
                profile.LegacyTwo.inventory=40308:100
                profile.LegacyTwo.warehouse=40010:2
                """);
        GameStateStore state = new GameStateStore(file, true);

        Map<Long, Integer> migrated = state.personalWarehouse(
                "LegacyAccount", List.of("LegacyOne", "LegacyTwo")).inventory();
        assertEquals(1, migrated.get(InventoryCatalog.SHORT_SWORD));
        assertEquals(2, migrated.get(InventoryCatalog.NATIVE_HEALING_POTION));
        assertTrue(state.saveInventoryAndPersonalWarehouse(
                "LegacyOne", "LegacyAccount",
                state.economy("LegacyOne").inventory(), migrated, migrated));

        GameStateStore reloaded = new GameStateStore(file, true);
        assertEquals(migrated, reloaded.personalWarehouse(
                "legacyaccount", List.of("LegacyTwo", "LegacyOne")).inventory());
    }

    @Test
    void persistsClanAndElvenWarehousesInIndependentSharedScopes() throws Exception {
        Path file = temporary.resolve("scoped-warehouse-state.properties");
        GameStateStore state = new GameStateStore(file, true);
        assertTrue(state.saveInventoryAndClanWarehouse(
                "TestKnight", "LegacyEmblem",
                state.economy("TestKnight").inventory(), Map.of(),
                Map.of(InventoryCatalog.SHORT_SWORD, 2)));
        assertTrue(state.saveInventoryAndElfWarehouse(
                "TestMage", "SharedElfAccount",
                state.economy("TestMage").inventory(), Map.of(),
                Map.of(InventoryCatalog.NATIVE_HEALING_POTION, 3)));

        GameStateStore reloaded = new GameStateStore(file, true);
        assertEquals(2, reloaded.clanWarehouse("legacyemblem").inventory()
                .get(InventoryCatalog.SHORT_SWORD));
        assertEquals(3, reloaded.elfWarehouse("sharedelfaccount").inventory()
                .get(InventoryCatalog.NATIVE_HEALING_POTION));
        String durable = Files.readString(file);
        assertTrue(durable.contains("warehouse.clan.count=1"));
        assertTrue(durable.contains("warehouse.elf.count=1"));
        assertTrue(durable.contains("format.version=43"));
    }

    @Test
    void allWarehouseScopesPersistCompleteNativeItemState() throws Exception {
        Path file = temporary.resolve("exact-warehouse-state.properties");
        GameStateStore state = new GameStateStore(file, true);
        RuntimeInventory.PersistentItem item = new RuntimeInventory.PersistentItem(
                1, 1, 0, false, RuntimeInventory.BLESSING_CURSED);
        Map<Long, Integer> storedCounts = Map.of(InventoryCatalog.SHORT_SWORD, 1);
        Map<Long, List<RuntimeInventory.PersistentItem>> storedStates =
                Map.of(InventoryCatalog.SHORT_SWORD, List.of(item));
        GameStateStore.Economy knight = state.economy("TestKnight");
        GameStateStore.Economy mage = state.economy("TestMage");

        assertTrue(state.saveInventoryAndPersonalWarehouse(
                "TestKnight", "ExactAccount", knight.inventory(), knight.itemStates(),
                state.personalWarehouse("ExactAccount", "TestKnight"),
                storedCounts, storedStates));
        assertTrue(state.saveInventoryAndClanWarehouse(
                "TestKnight", "ExactClan", knight.inventory(), knight.itemStates(),
                state.clanWarehouse("ExactClan"), storedCounts, storedStates));
        assertTrue(state.saveInventoryAndElfWarehouse(
                "TestMage", "ExactElf", mage.inventory(), mage.itemStates(),
                state.elfWarehouse("ExactElf"), storedCounts, storedStates));

        GameStateStore reloaded = new GameStateStore(file, true);
        assertEquals(storedStates,
                reloaded.personalWarehouse("ExactAccount", "TestKnight").itemStates());
        assertEquals(storedStates, reloaded.clanWarehouse("ExactClan").itemStates());
        assertEquals(storedStates, reloaded.elfWarehouse("ExactElf").itemStates());
    }

    @Test
    void allWarehouseScopesRestartWithExactDynamicSourceTemplate() throws Exception {
        Path file = temporary.resolve("source-template-scoped-warehouse.properties");
        GameStateStore state = new GameStateStore(file, true);
        InventoryCatalog.Definition definition = sourceEtc("Scoped Source Token", 321);
        RuntimeInventory stored = new RuntimeInventory();
        assertTrue(stored.addDistinctState(definition,
                new RuntimeInventory.PersistentItem(
                        0, 2, 0, 0, true,
                        RuntimeInventory.BLESSING_NORMAL, true)) > 0);
        GameStateStore.Economy knight = state.economy("TestKnight");
        GameStateStore.Economy mage = state.economy("TestMage");

        assertTrue(state.saveInventoryAndPersonalWarehouse(
                "TestKnight", "SourceAccount", knight.inventory(), knight.itemStates(),
                state.personalWarehouse("SourceAccount", "TestKnight"),
                stored.catalogCounts(), stored.catalogItemStates()));
        assertTrue(state.saveInventoryAndClanWarehouse(
                "TestKnight", "SourceClan", knight.inventory(), knight.itemStates(),
                state.clanWarehouse("SourceClan"),
                stored.catalogCounts(), stored.catalogItemStates()));
        assertTrue(state.saveInventoryAndElfWarehouse(
                "TestMage", "SourceElf", mage.inventory(), mage.itemStates(),
                state.elfWarehouse("SourceElf"),
                stored.catalogCounts(), stored.catalogItemStates()));

        GameStateStore reloaded = new GameStateStore(file, true);
        for (GameStateStore.Warehouse warehouse : List.of(
                reloaded.personalWarehouse("SourceAccount", "TestKnight"),
                reloaded.clanWarehouse("SourceClan"),
                reloaded.elfWarehouse("SourceElf"))) {
            RuntimeInventory restored = RuntimeInventory.fromDurableState(
                    warehouse.inventory(), warehouse.itemStates());
            RuntimeInventory.Item item = restored.items().get(0);
            assertEquals(definition, item.definition());
            assertEquals(2, item.quantity());
            assertTrue(item.sourceBound());
        }
    }

    @Test
    void warehousePasswordIsAccountScopedAtomicDurableAndDisableable() throws Exception {
        Path file = temporary.resolve("warehouse-password-state.properties");
        GameStateStore state = new GameStateStore(file, true);
        assertEquals(0, state.warehousePassword("SharedAccount"));
        assertEquals(GameStateStore.WarehousePasswordStatus.CHANGED,
                state.changeWarehousePassword("SharedAccount", 0, 654321));
        assertEquals(654321, state.warehousePassword("sharedaccount"));
        assertEquals(GameStateStore.WarehousePasswordStatus.WRONG_OLD,
                state.changeWarehousePassword("SHAREDACCOUNT", 123456, 456789));
        assertEquals(654321, state.warehousePassword("SharedAccount"));
        assertEquals(GameStateStore.WarehousePasswordStatus.UNCHANGED,
                state.changeWarehousePassword("SharedAccount", 654321, 654321));

        GameStateStore reloaded = new GameStateStore(file, true);
        assertEquals(654321, reloaded.warehousePassword("SHAREDACCOUNT"));
        assertEquals(GameStateStore.WarehousePasswordStatus.CHANGED,
                reloaded.changeWarehousePassword("sharedaccount", 654321, 0));
        assertEquals(0, reloaded.warehousePassword("SharedAccount"));

        GameStateStore disabled = new GameStateStore(file, true);
        assertEquals(0, disabled.warehousePassword("SharedAccount"));
        String durable = Files.readString(file);
        assertTrue(durable.contains("warehouse.password.count=0"));
        assertTrue(durable.contains("format.version=43"));
    }

    @Test
    void sourceQualifiedWarehouseOwnersAreIndependentAndClaimLegacyOnce() throws Exception {
        Path file = temporary.resolve("source-warehouse-state.properties");
        GameStateStore legacy = new GameStateStore(file, true);
        assertTrue(legacy.saveInventoryAndPersonalWarehouse(
                "LegacyHero", "SharedAccount", legacy.economy("LegacyHero").inventory(),
                Map.of(), Map.of(InventoryCatalog.SHORT_SWORD, 1)));
        assertTrue(legacy.saveInventoryAndClanWarehouse(
                "LegacyHero", "SameClan", legacy.economy("LegacyHero").inventory(),
                Map.of(), Map.of(InventoryCatalog.NATIVE_HEALING_POTION, 2)));
        assertTrue(legacy.saveInventoryAndElfWarehouse(
                "LegacyElf", "SharedAccount", legacy.economy("LegacyElf").inventory(),
                Map.of(), Map.of(InventoryCatalog.MAGIC_GEM, 3)));
        assertEquals(GameStateStore.WarehousePasswordStatus.CHANGED,
                legacy.changeWarehousePassword("SharedAccount", 0, 111111));

        String sunAccount = GameStateStore.sourceCharacter("Sun", "SharedAccount");
        String moonAccount = GameStateStore.sourceCharacter("Moon", "SharedAccount");
        String sunClan = GameStateStore.sourceCharacter("Sun", "SameClan");
        String moonClan = GameStateStore.sourceCharacter("Moon", "SameClan");
        String moonHero = GameStateStore.sourceCharacter("Moon", "SameHero");
        GameStateStore state = new GameStateStore(file, true);

        // The first concrete source claims each old unqualified owner exactly once.
        assertEquals(1, state.personalWarehouse(sunAccount, List.of()).inventory()
                .get(InventoryCatalog.SHORT_SWORD));
        assertEquals(2, state.clanWarehouse(sunClan).inventory()
                .get(InventoryCatalog.NATIVE_HEALING_POTION));
        assertEquals(3, state.elfWarehouse(sunAccount).inventory()
                .get(InventoryCatalog.MAGIC_GEM));
        assertEquals(111111, state.warehousePassword(sunAccount));

        assertTrue(state.personalWarehouse(moonAccount, List.of()).inventory().isEmpty());
        assertTrue(state.clanWarehouse(moonClan).inventory().isEmpty());
        assertTrue(state.elfWarehouse(moonAccount).inventory().isEmpty());
        assertEquals(0, state.warehousePassword(moonAccount));

        assertTrue(state.saveInventoryAndPersonalWarehouse(
                moonHero, moonAccount, state.economy(moonHero).inventory(), Map.of(),
                Map.of(InventoryCatalog.NATIVE_HEALING_POTION, 4)));
        assertTrue(state.saveInventoryAndClanWarehouse(
                moonHero, moonClan, state.economy(moonHero).inventory(), Map.of(),
                Map.of(InventoryCatalog.SHORT_SWORD, 5)));
        assertTrue(state.saveInventoryAndElfWarehouse(
                moonHero, moonAccount, state.economy(moonHero).inventory(), Map.of(),
                Map.of(InventoryCatalog.MAGIC_GEM, 6)));
        assertEquals(GameStateStore.WarehousePasswordStatus.CHANGED,
                state.changeWarehousePassword(moonAccount, 0, 222222));

        GameStateStore reloaded = new GameStateStore(file, true);
        assertEquals(1, reloaded.personalWarehouse(sunAccount, List.of()).inventory()
                .get(InventoryCatalog.SHORT_SWORD));
        assertEquals(4, reloaded.personalWarehouse(moonAccount, List.of()).inventory()
                .get(InventoryCatalog.NATIVE_HEALING_POTION));
        assertEquals(2, reloaded.clanWarehouse(sunClan).inventory()
                .get(InventoryCatalog.NATIVE_HEALING_POTION));
        assertEquals(5, reloaded.clanWarehouse(moonClan).inventory()
                .get(InventoryCatalog.SHORT_SWORD));
        assertEquals(3, reloaded.elfWarehouse(sunAccount).inventory()
                .get(InventoryCatalog.MAGIC_GEM));
        assertEquals(6, reloaded.elfWarehouse(moonAccount).inventory()
                .get(InventoryCatalog.MAGIC_GEM));
        assertEquals(111111, reloaded.warehousePassword(sunAccount));
        assertEquals(222222, reloaded.warehousePassword(moonAccount));
    }

    private static InventoryCatalog.Definition sourceEtc(String name, int icon) {
        return new InventoryCatalog.Definition(
                65_001, 0, icon, icon + 1, 1, true,
                name, "retained source detail", 17, 9,
                true, true, BattleActionCodes.WALK, 0,
                InventoryCatalog.Effect.NONE, 87, 1_250, 3, null, null);
    }
}
