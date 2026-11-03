package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GameStatePetTest {
    @TempDir Path temporary;

    @Test void sourcePetImportPinsCollarAndPetIdentityAcrossRestart() throws Exception {
        Path file = temporary.resolve("state.properties");
        String character = GameStateStore.sourceCharacter("Legacy One", "TestMage");
        SourcePet source = new SourcePet(10231, 10233,
                (int) InventoryCatalog.PET_COLLAR, 45034,
                "牧羊犬", 12, 78, 17, 21_375, 0);
        GameStateStore first = new GameStateStore(file, true);

        GameStateStore.PetState imported = first.importSourcePets(
                character, List.of(source)).get(0);
        assertTrue(!imported.litoOwned());
        long collarObjectId = imported.collarObjectId();
        long petObjectId = imported.objectId();
        RuntimeInventory firstInventory = RuntimeInventory.fromDurableState(
                first.economy(character).inventory(),
                first.economy(character).itemStates());
        assertNotNull(firstInventory.item(collarObjectId));
        assertEquals(InventoryCatalog.PET_COLLAR,
                firstInventory.item(collarObjectId).definition().catalogId());

        GameStateStore reloaded = new GameStateStore(file, true);
        RuntimeInventory restoredInventory = RuntimeInventory.fromDurableState(
                reloaded.economy(character).inventory(),
                reloaded.economy(character).itemStates());
        assertNotNull(restoredInventory.item(collarObjectId));
        assertEquals(petObjectId, reloaded.petForCollar(collarObjectId).objectId());
        assertTrue(reloaded.importSourcePets(character, List.of(source)).isEmpty());

        GameStateStore.PetState progressed = reloaded.petForCollar(collarObjectId)
                .withProgress("牧羊犬", 13, 84, 19, 24_000);
        reloaded.savePet(progressed);
        assertEquals(progressed, new GameStateStore(file, true)
                .petForObject(petObjectId));
    }

    @Test void deletedImportedPetKeepsItsSourceTombstone() throws Exception {
        Path file = temporary.resolve("deleted.properties");
        String character = GameStateStore.sourceCharacter("Legacy One", "TestMage");
        SourcePet source = new SourcePet(10231, 10233,
                (int) InventoryCatalog.PET_COLLAR, 45034,
                "牧羊犬", 12, 78, 17, 21_375, 0);
        GameStateStore state = new GameStateStore(file, true);
        long collar = state.importSourcePets(character, List.of(source))
                .get(0).collarObjectId();

        assertTrue(state.deletePet(collar));
        assertEquals(java.util.Set.of(10231L),
                state.deletedSourcePetCollarIds(character));
        GameStateStore reloaded = new GameStateStore(file, true);
        assertTrue(reloaded.importSourcePets(character, List.of(source)).isEmpty());
    }

    @Test void collarAndPetRowAreDeletedInOneDurableSnapshot() throws Exception {
        Path file = temporary.resolve("destroyed-collar.properties");
        String character = GameStateStore.sourceCharacter("Legacy One", "TestMage");
        SourcePet source = new SourcePet(10231, 10233,
                (int) InventoryCatalog.PET_COLLAR, 45034,
                "牧羊犬", 12, 78, 17, 21_375, 0);
        GameStateStore state = new GameStateStore(file, true);
        long collar = state.importSourcePets(character, List.of(source))
                .get(0).collarObjectId();
        GameStateStore.Economy economy = state.economy(character);
        RuntimeInventory inventory = RuntimeInventory.fromDurableState(
                economy.inventory(), economy.itemStates());
        assertNotNull(inventory.removeEntire(collar));

        assertTrue(state.saveEconomyStateDeletingPet(
                character, economy.points(), inventory.catalogCounts(),
                inventory.catalogItemStates(), collar));
        GameStateStore reloaded = new GameStateStore(file, true);
        assertNull(reloaded.petForCollar(collar));
        RuntimeInventory restored = RuntimeInventory.fromDurableState(
                reloaded.economy(character).inventory(),
                reloaded.economy(character).itemStates());
        assertNull(restored.item(collar));
        assertTrue(reloaded.importSourcePets(character, List.of(source)).isEmpty());
    }

    @Test void evolutionReplacesBothCollarAndPetIdentityInOneSnapshot() throws Exception {
        Path file = temporary.resolve("evolved.properties");
        String character = GameStateStore.sourceCharacter("Legacy One", "TestMage");
        SourcePet source = new SourcePet(10231, 10233,
                (int) InventoryCatalog.PET_COLLAR, 45034,
                "牧羊犬", 30, 120, 24, 60_000, 0);
        GameStateStore state = new GameStateStore(file, true);
        GameStateStore.PetState original = state.importSourcePets(
                character, List.of(source)).get(0);
        GameStateStore.Economy economy = state.economy(character);
        RuntimeInventory inventory = RuntimeInventory.fromDurableState(
                economy.inventory(), economy.itemStates());
        assertNotNull(inventory.removeEntire(original.collarObjectId()));
        assertTrue(inventory.add(InventoryCatalog.require(
                InventoryCatalog.HIGH_PET_COLLAR), 1));
        RuntimeInventory.Item newCollar = inventory.items().stream()
                .filter(item -> item.definition().catalogId()
                        == InventoryCatalog.HIGH_PET_COLLAR)
                .findFirst().orElseThrow();
        GameStateStore.PetState evolved = new GameStateStore.PetState(
                original.sourceKey(), newCollar.objectId(), original.objectId(),
                (int) InventoryCatalog.HIGH_PET_COLLAR, 45687,
                original.name(), 1, 60, 12, 0, original.lawful(), true);

        state.saveEconomyStateEvolvingPet(
                character, economy.points(), inventory.catalogCounts(),
                inventory.catalogItemStates(), original.collarObjectId(), evolved);
        GameStateStore reloaded = new GameStateStore(file, true);
        assertNull(reloaded.petForCollar(original.collarObjectId()));
        assertEquals(evolved, reloaded.petForCollar(newCollar.objectId()));
        assertEquals(evolved, reloaded.petForObject(original.objectId()));
        assertTrue(reloaded.petForObject(original.objectId()).litoOwned());
        assertTrue(java.nio.file.Files.readString(file).contains("format.version=43"));
        RuntimeInventory restored = RuntimeInventory.fromDurableState(
                reloaded.economy(character).inventory(),
                reloaded.economy(character).itemStates());
        assertNull(restored.item(original.collarObjectId()));
        assertEquals(InventoryCatalog.HIGH_PET_COLLAR,
                restored.item(newCollar.objectId()).definition().catalogId());

        SourceInventoryItem sqlCollar = new SourceInventoryItem(
                newCollar.objectId(), (int) InventoryCatalog.HIGH_PET_COLLAR,
                1, false, 0, 0, true, 1, true,
                petCollarWire((int) InventoryCatalog.HIGH_PET_COLLAR),
                null, 0, false, null, true, false);
        GameStateStore.SourceInventoryReconciliation sqlInventory =
                reloaded.reconcileSourceInventory(character, List.of(sqlCollar));
        SourcePet sqlPet = new SourcePet(newCollar.objectId(), evolved.objectId(),
                (int) InventoryCatalog.HIGH_PET_COLLAR, evolved.npcId(),
                evolved.name(), evolved.level(), evolved.maxHp(), evolved.maxMp(),
                evolved.experience(), evolved.lawful(), true);
        GameStateStore.PetState rebound = reloaded.importSourcePets(
                character, List.of(sqlPet), sqlInventory.definitions()).get(0);
        assertEquals(newCollar.objectId(), rebound.sourceCollarObjectId());
        assertTrue(reloaded.deletedSourcePetCollarIds(character).isEmpty());
    }

    @Test void litoOwnedSqlPetAdoptsExactCollarAndPetObjectIds() throws Exception {
        Path file = temporary.resolve("owned-sql-pet.properties");
        String character = GameStateStore.sourceCharacter("Legacy One", "TestMage");
        long collarId = 0x7000_0020L;
        long petId = 0x9000_0020L;
        ClassicWorldDropCatalog.ItemWire wire = petCollarWire(
                (int) InventoryCatalog.PET_COLLAR);
        SourceInventoryItem collar = new SourceInventoryItem(
                collarId, (int) InventoryCatalog.PET_COLLAR, 1, false,
                0, 0, true, 1, true, wire, null, 0, false, null,
                true, false);
        SourcePet sourcePet = new SourcePet(
                collarId, petId, (int) InventoryCatalog.PET_COLLAR, 45034,
                "SQL來源犬", 12, 78, 17, 21_375, 0, true);
        GameStateStore state = new GameStateStore(file, true);
        GameStateStore.SourceInventoryReconciliation inventory =
                state.reconcileSourceInventory(character, List.of(collar));
        GameStateStore.PetState imported = state.importSourcePets(
                character, List.of(sourcePet), inventory.definitions()).get(0);

        assertEquals(collarId, imported.collarObjectId());
        assertEquals(petId, imported.objectId());
        assertTrue(imported.litoOwned());
        RuntimeInventory runtime = RuntimeInventory.fromDurableState(
                state.economy(character).inventory(),
                state.economy(character).itemStates(), inventory.definitions());
        assertTrue(runtime.item(collarId).sourceBound());
        assertTrue(runtime.item(collarId).litoOwned());
        assertEquals(List.of(imported.persistence()), state.petsForInventory(runtime));

        GameStateStore restarted = new GameStateStore(file, true);
        assertEquals(petId, restarted.petForCollar(collarId).objectId());
        assertTrue(restarted.petForCollar(collarId).litoOwned());
    }

    @Test void sourcePetRekeysToExactCollarRetrievedFromSqlWarehouse() throws Exception {
        Path file = temporary.resolve("warehouse-source-pet.properties");
        String character = GameStateStore.sourceCharacter("Legacy One", "TestMage");
        long sourceCollarId = 10231L;
        SourcePet source = new SourcePet(sourceCollarId, 10233,
                (int) InventoryCatalog.PET_COLLAR, 45034,
                "倉庫牧羊犬", 12, 78, 17, 21_375, 0);
        GameStateStore state = new GameStateStore(file, true);
        GameStateStore.PetState first = state.importSourcePets(
                character, List.of(source)).get(0);
        assertTrue(first.collarObjectId() != sourceCollarId);

        GameStateStore.Economy economy = state.economy(character);
        RuntimeInventory inventory = RuntimeInventory.fromDurableState(
                economy.inventory(), economy.itemStates());
        assertNotNull(inventory.removeEntire(first.collarObjectId()));
        long adoptedId = inventory.addDistinctState(
                InventoryCatalog.require(InventoryCatalog.PET_COLLAR),
                new RuntimeInventory.PersistentItem(
                        sourceCollarId, 1, 0, 0, true, 1, true));
        assertEquals(sourceCollarId, adoptedId);
        state.saveEconomyState(character, economy.points(),
                inventory.catalogCounts(), inventory.catalogItemStates());

        GameStateStore.PetState rekeyed = state.importSourcePets(
                character, List.of(source)).get(0);
        assertEquals(sourceCollarId, rekeyed.collarObjectId());
        assertEquals(first.objectId(), rekeyed.objectId());
        assertNull(state.petForCollar(first.collarObjectId()));
        assertEquals(rekeyed, state.petForCollar(sourceCollarId));
        assertEquals(1, state.petsForInventory(inventory).size());

        GameStateStore restarted = new GameStateStore(file, true);
        assertNull(restarted.petForCollar(first.collarObjectId()));
        assertEquals(first.objectId(), restarted.petForCollar(sourceCollarId).objectId());
    }

    private static ClassicWorldDropCatalog.ItemWire petCollarWire(int catalogId) {
        return new ClassicWorldDropCatalog.ItemWire(
                catalogId, "etc", "$pet-collar", "other",
                487, 1317, 1, false, true, 5_000, 0,
                "", 0, 0, "leather", -1, 0);
    }
}
