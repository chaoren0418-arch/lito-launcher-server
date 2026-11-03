package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GameStatePrivateInventoryTest {
    @TempDir Path temporary;

    @Test void sqlPrivateSnapshotReplacesOnlyNonSourceObjects() throws Exception {
        GameStateStore state = new GameStateStore(
                temporary.resolve("private-inventory.properties"), false);
        long sourceObject = 0x6100_0001L;
        long staleSidecarObject = 0x6100_0002L;
        RuntimeInventory source = new RuntimeInventory();
        assertEquals(sourceObject, source.addDistinctState(
                InventoryCatalog.require(InventoryCatalog.SHORT_SWORD),
                new RuntimeInventory.PersistentItem(
                        sourceObject, 1, 3, 2, true,
                        RuntimeInventory.BLESSING_NORMAL, true, false, true,
                        null, 0, 0, false, false)));
        assertEquals(staleSidecarObject, source.addDistinctState(
                InventoryCatalog.require(InventoryCatalog.DEATH_SAFE_ALL),
                new RuntimeInventory.PersistentItem(
                        staleSidecarObject, 1, 0, 0, true,
                        RuntimeInventory.BLESSING_NORMAL, false, true, false,
                        null, 0, 0, false, false)));
        state.saveEconomyState("PrivateSql", 123,
                source.catalogCounts(), source.catalogItemStates());

        long sqlObject = 0x6100_0003L;
        LitoOwnedInventoryItem sqlItem = new LitoOwnedInventoryItem(
                sqlObject, (int) InventoryCatalog.DEATH_SAFE_ALL,
                InventoryCatalog.require(InventoryCatalog.DEATH_SAFE_ALL).name(),
                2, false, true, 0, true, 0, 0,
                RuntimeInventory.BLESSING_NORMAL, 1_900_000_000_000L, true);
        GameStateStore.Economy reconciled = state.reconcileSqlPrivateInventory(
                "PrivateSql", List.of(sqlItem));
        RuntimeInventory restored = RuntimeInventory.fromDurableState(
                reconciled.inventory(), reconciled.itemStates());

        assertTrue(restored.items().stream()
                .anyMatch(item -> item.objectId() == sourceObject && item.sourceBound()));
        assertFalse(restored.items().stream()
                .anyMatch(item -> item.objectId() == staleSidecarObject));
        RuntimeInventory.Item privateItem = restored.item(sqlObject);
        assertEquals(2, privateItem.quantity());
        assertTrue(privateItem.tournamentReward());
        assertTrue(privateItem.sealed());
        assertEquals(1_900_000_000_000L, privateItem.expiresAtEpochMillis());
        assertEquals(123, reconciled.points());
    }

    @Test void emptySqlSnapshotRemovesStalePrivateObjects() throws Exception {
        GameStateStore state = new GameStateStore(
                temporary.resolve("private-empty.properties"), false);
        state.saveEconomyState("PrivateEmpty", 77,
                Map.of(InventoryCatalog.DEATH_SAFE_ITEM, 1), Map.of());
        GameStateStore.Economy reconciled = state.reconcileSqlPrivateInventory(
                "PrivateEmpty", List.of());
        assertTrue(reconciled.inventory().isEmpty());
        assertTrue(reconciled.itemStates().isEmpty());
        assertEquals(77, reconciled.points());
    }
}
