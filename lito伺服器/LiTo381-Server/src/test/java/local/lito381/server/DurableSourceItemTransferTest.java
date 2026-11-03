package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class DurableSourceItemTransferTest {
    private static final long SOURCE_CATALOG_ID = 65_001;

    @TempDir Path temporary;

    @Test
    void definitionCodecRoundTripsOrdinaryWeaponAndArmorTemplates() {
        InventoryCatalog.Definition ordinary = sourceEtc("來源藥草, / ~ :", 321);
        InventoryCatalog.Definition weapon = new InventoryCatalog.Definition(
                65_002, 1, 444, 555, 0, true, "來源之劍", "精確武器狀態",
                10, 5, false, true, BattleActionCodes.SWORD_WALK, 0,
                InventoryCatalog.Effect.NONE, 91, 2_300, -4,
                new InventoryCatalog.NativeWeaponStatus(9, 11, 14, 2_300, 0x7F, 6),
                null);
        InventoryCatalog.Definition armor = new InventoryCatalog.Definition(
                65_003, 2, 666, 777, 2, false, "來源盔甲", "精確護甲狀態",
                20, 8, false, true, BattleActionCodes.WALK, 0,
                InventoryCatalog.Effect.NONE, 92, 4_500, 5, null,
                new InventoryCatalog.NativeArmorStatus(
                        7, 15, 4_500, 0x7F, 4,
                        1, 2, 3, 4, 5, 6, 120, 80, 9, 3, true,
                        10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 2));
        InventoryCatalog.Definition cantDelete = new InventoryCatalog.Definition(
                65_004, 0xFF, 778, 779, 0, true,
                "不可刪除來源物", "死亡時也不可掉落",
                0, 0, false, true, BattleActionCodes.WALK, 0,
                InventoryCatalog.Effect.NONE, 93, 700, 0,
                null, null, true);

        for (InventoryCatalog.Definition definition : new InventoryCatalog.Definition[] {
                ordinary, weapon, armor, cantDelete}) {
            String encoded = DurableItemDefinitionCodec.encode(definition);
            assertFalse(encoded.isBlank());
            assertEquals(definition, DurableItemDefinitionCodec.decode(encoded));
        }
        assertThrows(IllegalArgumentException.class,
                () -> DurableItemDefinitionCodec.decode("not_a_complete_definition"));
    }

    @Test
    void sourceEtcCrossesWarehouseAndRestartWithExactNativeTemplate() throws Exception {
        InventoryCatalog.Definition definition = sourceEtc("來源倉庫藥草", 321);
        RuntimeInventory inventory = new RuntimeInventory();
        assertTrue(inventory.add(InventoryCatalog.require(InventoryCatalog.ADENA), 100));
        long sourceObjectId = inventory.addDistinctState(definition,
                new RuntimeInventory.PersistentItem(
                        0, 3, 0, 0, true, 1, true));
        assertTrue(sourceObjectId > 0);
        assertTrue(inventory.item(sourceObjectId).transferable());

        ClassicWarehouseTransaction.Outcome deposit = ClassicWarehouseTransaction.apply(
                2, Map.of(sourceObjectId, 2L), 1, Set.of(sourceObjectId),
                inventory, new RuntimeInventory());
        assertTrue(deposit.accepted());
        RuntimeInventory.Item stored = deposit.warehouse().items().get(0);
        assertEquals(definition, stored.definition());
        assertTrue(stored.sourceBound());

        Path file = temporary.resolve("source-template-warehouse.properties");
        GameStateStore state = new GameStateStore(file, true);
        GameStateStore.Warehouse empty = state.personalWarehouse(
                "TemplateAccount", "TestKnight");
        assertTrue(state.saveInventoryAndPersonalWarehouse(
                "TestKnight", "TemplateAccount",
                deposit.inventory().catalogCounts(), deposit.inventory().catalogItemStates(),
                empty, deposit.warehouse().catalogCounts(),
                deposit.warehouse().catalogItemStates()));

        GameStateStore reloaded = new GameStateStore(file, true);
        GameStateStore.Warehouse durableWarehouse = reloaded.personalWarehouse(
                "TemplateAccount", "TestKnight");
        RuntimeInventory restoredInventory = RuntimeInventory.fromDurableState(
                reloaded.economy("TestKnight").inventory(),
                reloaded.economy("TestKnight").itemStates());
        RuntimeInventory restoredWarehouse = RuntimeInventory.fromDurableState(
                durableWarehouse.inventory(), durableWarehouse.itemStates());
        RuntimeInventory.Item restored = restoredWarehouse.items().get(0);
        assertEquals(definition, restored.definition());
        assertEquals(stored.nativeViewName(), restored.nativeViewName());
        assertArrayEquals(stored.nativeStatusDetail(), restored.nativeStatusDetail());

        ClassicWarehouseTransaction.Outcome retrieve = ClassicWarehouseTransaction.apply(
                3, Map.of(restored.objectId(), 2L), 1, Set.of(restored.objectId()),
                restoredInventory, restoredWarehouse);
        assertTrue(retrieve.accepted());
        RuntimeInventory.Item combined = retrieve.inventory().items().stream()
                .filter(item -> item.definition().catalogId() == SOURCE_CATALOG_ID)
                .findFirst().orElseThrow();
        assertEquals(3, combined.quantity());
        assertEquals(definition, combined.definition());
        assertEquals(70, retrieve.inventory().items().stream()
                .filter(item -> item.definition().catalogId() == InventoryCatalog.ADENA)
                .findFirst().orElseThrow().quantity());
        assertTrue(Files.readString(file).contains("format.version=43"));
    }

    @Test
    void playerTransferPersistsBothTemplatesWithSameCatalogWithoutCollapsing()
            throws Exception {
        InventoryCatalog.Definition sourceDefinition = sourceEtc("來源交易藥草", 321);
        InventoryCatalog.Definition recipientDefinition = sourceEtc("另一來源同編號藥草", 654);
        RuntimeInventory source = new RuntimeInventory();
        long sourceObjectId = source.addDistinctState(sourceDefinition,
                new RuntimeInventory.PersistentItem(
                        0, 4, 0, 0, true, 1, true));
        RuntimeInventory recipient = new RuntimeInventory();
        assertTrue(recipient.add(recipientDefinition, 1));

        RuntimeInventory.Item offered = source.item(sourceObjectId);
        assertTrue(offered.transferable());
        assertTrue(recipient.add(offered, 2));
        assertTrue(source.remove(sourceObjectId, 2));
        assertEquals(2, recipient.size());
        assertNotEquals(recipient.items().get(0).definition(),
                recipient.items().get(1).definition());

        Path file = temporary.resolve("source-template-player-trade.properties");
        GameStateStore state = new GameStateStore(file, true);
        state.saveEconomyStates(
                "TestKnight", state.economy("TestKnight").points(),
                source.catalogCounts(), source.catalogItemStates(),
                "TestMage", state.economy("TestMage").points(),
                recipient.catalogCounts(), recipient.catalogItemStates());

        GameStateStore reloaded = new GameStateStore(file, true);
        GameStateStore.Economy durableRecipient = reloaded.economy("TestMage");
        RuntimeInventory restored = RuntimeInventory.fromDurableState(
                durableRecipient.inventory(), durableRecipient.itemStates());
        assertEquals(3, durableRecipient.inventory().get(SOURCE_CATALOG_ID));
        assertEquals(2, restored.size());
        assertEquals(Set.of(sourceDefinition, recipientDefinition),
                Set.copyOf(restored.items().stream()
                        .map(RuntimeInventory.Item::definition).toList()));
        RuntimeInventory.Item transferred = restored.items().stream()
                .filter(RuntimeInventory.Item::sourceBound).findFirst().orElseThrow();
        assertEquals(2, transferred.quantity());
        assertEquals(sourceDefinition, transferred.definition());
    }

    @Test
    void timedAndSealedObjectFieldsRoundTripThroughFormat38() throws Exception {
        InventoryCatalog.Definition sword = InventoryCatalog.require(
                InventoryCatalog.SHORT_SWORD);
        RuntimeInventory inventory = new RuntimeInventory();
        long expiresAt = 1_900_000_000_000L;
        long objectId = inventory.addDistinctState(sword,
                new RuntimeInventory.PersistentItem(
                        0, 1, 4, 1, true, RuntimeInventory.BLESSING_NORMAL,
                        false, false, false, null, 0, expiresAt, true));
        assertTrue(objectId > 0);

        Path file = temporary.resolve("timed-sealed-format38.properties");
        GameStateStore state = new GameStateStore(file, true);
        state.saveEconomyState("TestKnight", state.economy("TestKnight").points(),
                inventory.catalogCounts(), inventory.catalogItemStates());

        String durableText = Files.readString(file);
        assertTrue(durableText.contains("format.version=43"));
        assertTrue(durableText.contains("~1900000000000~1"));
        GameStateStore reloaded = new GameStateStore(file, true);
        GameStateStore.Economy economy = reloaded.economy("TestKnight");
        RuntimeInventory restored = RuntimeInventory.fromDurableState(
                economy.inventory(), economy.itemStates());
        RuntimeInventory.Item item = restored.item(objectId);
        assertEquals(expiresAt, item.expiresAtEpochMillis());
        assertTrue(item.timed());
        assertTrue(item.sealed());
    }

    private static InventoryCatalog.Definition sourceEtc(String name, int icon) {
        return new InventoryCatalog.Definition(
                SOURCE_CATALOG_ID, 0, icon, icon + 1, 1, true,
                name, "來源 detail：保留原生欄位", 17, 9,
                true, true, BattleActionCodes.WALK, 0,
                InventoryCatalog.Effect.NONE, 87, 1_250, 3, null, null);
    }
}
