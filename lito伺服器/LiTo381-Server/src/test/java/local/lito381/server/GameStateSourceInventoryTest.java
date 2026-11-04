package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GameStateSourceInventoryTest {
    private static final String CHARACTER = "RetainedSource\u001FInventoryHero";

    @TempDir Path temporary;

    @Test
    void questTombstoneConsumesSourceWeaponWithoutEditingOrReimportingItsRow()
            throws Exception {
        Path file = temporary.resolve("source-quest-tombstone.properties");
        SourceInventoryItem sourceWeapon = new SourceInventoryItem(
                801, 65, 1, true, 0, 0, true, 1, false,
                new ClassicWorldDropCatalog.ItemWire(
                        65, "weapon", "$source65", "weapon", 100, 101, 1,
                        false, true, 80_000, 0, "sword", 14, 6,
                        "silver", 6, 0x0F));
        SourceWeaponPhysicalStats physical = new SourceWeaponPhysicalStats(
                65, 1, 14, 6, 1, 0, 0, 0, 0).withNameId("$source65");
        SourceEquipmentMagicStats equipment = new SourceEquipmentMagicStats(
                new SourceEquipmentPhysicalStats(physical, false, false), 1, 0);

        GameStateStore state = new GameStateStore(file, true);
        GameStateStore.SourceInventoryReconciliation imported =
                state.reconcileSourceInventory(
                        CHARACTER, List.of(sourceWeapon), equipment);
        RuntimeInventory runtime = RuntimeInventory.fromDurableState(
                imported.economy().inventory(), imported.economy().itemStates(),
                imported.definitions(), imported.sourceWeapons());
        RuntimeInventory.Item item = runtime.equippedWeaponItem();
        RuntimeInventory removed = runtime.withoutObjects(Set.of(item.objectId()));
        state.saveEconomyStateAndStoryQuestsConsumingSourceObjects(
                CHARACTER, 0, removed.catalogCounts(), removed.catalogItemStates(),
                Map.of(ClassicWorldCkewCatalog.QUEST_ID,
                        ClassicWorldCkewCatalog.QUEST_ENDED),
                Set.of(item.objectId()));

        GameStateStore restarted = new GameStateStore(file, true);
        GameStateStore.SourceInventoryReconciliation restored =
                restarted.reconcileSourceInventory(
                        CHARACTER, List.of(sourceWeapon), equipment);
        assertEquals(0, restored.activeSourceObjects());
        assertEquals(0, restored.qualifiedSourceWeaponObjects());
        assertFalse(restored.economy().inventory().containsKey(65L));
        assertEquals(Set.of(801L), restored.consumedSourceEquipmentObjects());
        assertEquals(List.of(new GameStateStore.SourceInventoryBinding(
                        801, item.objectId(), 65, true)),
                restarted.sourceInventoryBindings(CHARACTER));
        assertEquals(ClassicWorldCkewCatalog.QUEST_ENDED,
                restarted.storyQuests(CHARACTER).step(
                        ClassicWorldCkewCatalog.QUEST_ID));
        String durable = java.nio.file.Files.readString(file);
        assertTrue(durable.contains("sourceInventory.0.consumed=1"));
        assertTrue(durable.contains("sourceInventory.0.sourceObjectId=801"));
    }

    @Test
    void preservesDebitsAcrossRestartAndResetsOnlyChangedSourceRows() throws Exception {
        Path file = temporary.resolve("source-inventory.properties");
        SourceInventoryItem first = item(101, 50_001, 5);
        SourceInventoryItem second = item(102, 50_001, 7);

        GameStateStore state = new GameStateStore(file, true);
        GameStateStore.SourceInventoryReconciliation imported =
                state.reconcileSourceInventory(CHARACTER, List.of(first, second));
        assertEquals(2, imported.importedRows());
        assertEquals(2, imported.activeSourceObjects());
        assertEquals(2, imported.observedSourceWeight());
        assertEquals(0, imported.retainedSourceWeight());
        assertEquals(12, imported.economy().inventory().get(50_001L));

        List<RuntimeInventory.PersistentItem> originalStates =
                imported.economy().itemStates().get(50_001L);
        assertEquals(2, originalStates.size());
        assertNotEquals(originalStates.get(0).objectId(), originalStates.get(1).objectId());
        assertTrue(originalStates.stream().allMatch(
                RuntimeInventory.PersistentItem::sourceBound));
        assertEquals(List.of(101L, 102L), state.sourceInventoryBindings(CHARACTER).stream()
                .map(GameStateStore.SourceInventoryBinding::sourceObjectId).toList());
        assertEquals(Set.copyOf(originalStates.stream()
                        .map(RuntimeInventory.PersistentItem::objectId).toList()),
                Set.copyOf(state.sourceInventoryBindings(CHARACTER).stream()
                        .map(GameStateStore.SourceInventoryBinding::localObjectId).toList()));

        RuntimeInventory spent = RuntimeInventory.fromDurableState(
                imported.economy().inventory(), imported.economy().itemStates(),
                imported.definitions());
        long debitedObject = originalStates.get(0).objectId();
        assertTrue(spent.item(debitedObject).transferable());
        assertTrue(spent.remove(debitedObject, 3));
        state.saveEconomyState(CHARACTER, imported.economy().points(),
                spent.catalogCounts(), spent.catalogItemStates());

        GameStateStore.SourceInventoryReconciliation unchanged =
                state.reconcileSourceInventory(CHARACTER, List.of(first, second));
        assertEquals(0, unchanged.importedRows());
        assertEquals(0, unchanged.resetRows());
        assertEquals(9, unchanged.economy().inventory().get(50_001L));
        assertEquals(2, unchanged.economy().itemStates().get(50_001L).size());

        GameStateStore restored = new GameStateStore(file, true);
        GameStateStore.SourceInventoryReconciliation afterRestart =
                restored.reconcileSourceInventory(CHARACTER, List.of(first, second));
        assertEquals(9, afterRestart.economy().inventory().get(50_001L));
        assertEquals(0, afterRestart.resetRows());
        assertTrue(afterRestart.economy().itemStates().get(50_001L).stream()
                .allMatch(RuntimeInventory.PersistentItem::sourceBound));

        SourceInventoryItem externallyChanged = item(101, 50_001, 9);
        GameStateStore.SourceInventoryReconciliation reset =
                restored.reconcileSourceInventory(
                        CHARACTER, List.of(externallyChanged, second));
        assertEquals(1, reset.resetRows());
        assertEquals(16, reset.economy().inventory().get(50_001L));

        GameStateStore.SourceInventoryReconciliation removed =
                restored.reconcileSourceInventory(CHARACTER, List.of(externallyChanged));
        assertEquals(1, removed.removedRows());
        assertEquals(9, removed.economy().inventory().get(50_001L));
        assertTrue(java.nio.file.Files.readString(file).contains("format.version=43"));
    }

    @Test
    void importsProjectilesWhileDeferringUnsafeEtcAndPetCollars() throws Exception {
        GameStateStore state = new GameStateStore(temporary.resolve("deferred.properties"), false);
        SourceInventoryItem equippedEtc = new SourceInventoryItem(
                201, 50_002, 1, true, 0, 0, true, 1, false,
                etcWire(50_002, true));
        SourceInventoryItem petCollar = new SourceInventoryItem(
                202, 50_003, 1, false, 0, 0, true, 1, true,
                etcWire(50_003, false));
        SourceInventoryItem weapon = new SourceInventoryItem(
                203, 24, 1, false, 0, 0, true, 1, false,
                new ClassicWorldDropCatalog.ItemWire(
                        24, "weapon", "$100", "weapon", 106, 26, 1,
                        false, true, 30_000, 0, "sword", 6, 8,
                        "iron", 6, 0x3F));
        SourceInventoryItem arrow = new SourceInventoryItem(
                204, 40_703, 20, false, 0, 0, true, 1, false,
                new ClassicWorldDropCatalog.ItemWire(
                        40_703, "etc", "$2376", "arrow", 407, 75, 1,
                        true, true, 10, 0, "", 0, 0,
                        "wood", -1, 0));

        GameStateStore.SourceInventoryReconciliation result =
                state.reconcileSourceInventory(
                        CHARACTER, List.of(equippedEtc, petCollar, weapon, arrow));
        assertEquals(2, result.importableRows());
        assertEquals(1, result.deferredEquipmentRows());
        assertEquals(1, result.deferredPetCollarRows());
        assertEquals(0, result.deferredWeaponArmorRows());
        assertEquals(0, result.deferredProjectileRows());
        assertEquals(1, result.importedWeaponRows());
        assertEquals(0, result.qualifiedSourceWeaponObjects());
        assertEquals(33, result.observedSourceWeight());
        assertEquals(1, result.retainedSourceWeight());
        assertEquals(2, result.activeSourceObjects());
        assertEquals(1, result.importedProjectileRows());
        assertEquals(0, result.qualifiedSourceProjectileObjects());
        RuntimeInventory runtime = RuntimeInventory.fromDurableState(
                result.economy().inventory(), result.economy().itemStates(),
                result.definitions(), result.sourceWeapons());
        RuntimeInventory.Item importedWeapon = runtime.items().stream()
                .filter(item -> item.definition().weapon()).findFirst().orElseThrow();
        assertTrue(importedWeapon.sourceBound());
        assertTrue(runtime.toggleWeapon(importedWeapon.objectId()).isEmpty());
        RuntimeInventory.Item importedArrow = runtime.items().stream()
                .filter(item -> item.definition().projectile()).findFirst().orElseThrow();
        assertEquals(0xFE, importedArrow.definition().useType());
        assertEquals(20, importedArrow.quantity());
        assertTrue(importedArrow.sourceBound());
        assertEquals(Map.of(204L, importedArrow.objectId()),
                result.sourceProjectileObjects());
        assertFalse(runtime.remove(importedArrow.objectId(), 1));
        assertNull(runtime.removeEntire(importedArrow.objectId()));
    }

    @Test
    void unequippedSourceWeaponCanBeSqlQualifiedWithExtendedEnchant()
            throws Exception {
        SourceInventoryItem sourceWeapon = new SourceInventoryItem(
                333_295, 10, 1, false, 999, 0, true, 1, false,
                new ClassicWorldDropCatalog.ItemWire(
                        10, "weapon", "$2920", "weapon", 64, 66, 1,
                        false, true, 30_000, 0, "dagger", 8, 8,
                        "iron", 6, 0x7F));
        GameStateStore state = new GameStateStore(
                temporary.resolve("extended-source-weapon.properties"), true);
        GameStateStore.SourceInventoryReconciliation imported =
                state.reconcileSourceInventory(
                        CHARACTER, List.of(sourceWeapon),
                        SourceEquipmentMagicStats.none());
        RuntimeInventory runtime = RuntimeInventory.fromDurableState(
                imported.economy().inventory(), imported.economy().itemStates(),
                imported.definitions(), imported.sourceWeapons());
        RuntimeInventory.Item item = runtime.items().stream()
                .filter(candidate -> candidate.definition().catalogId() == 10)
                .findFirst().orElseThrow();
        assertEquals((byte) 999, item.enchantLevel());
        assertFalse(runtime.hasQualifiedPhysicalWeapon(item.objectId()));
        assertTrue(runtime.toggleWeapon(item.objectId()).isEmpty());

        RuntimeInventory candidate = runtime.copy();
        assertEquals(1, candidate.toggleSourceWeaponForEvaluation(
                item.objectId()).size());
        SourceWeaponPhysicalStats exact = new SourceWeaponPhysicalStats(
                10, 2, 8, 8, 1, 0, 0, 999, 0)
                .withNameId("$2920");
        candidate.qualifySourceWeapon(item.objectId(), exact);

        assertTrue(candidate.hasQualifiedPhysicalWeapon(item.objectId()));
        assertEquals(999, candidate.equippedPhysicalWeapon().enchantLevel());
        assertEquals(BattleActionCodes.DAGGER_WALK,
                candidate.equippedWalkAction());
    }

    @Test
    void materializedProjectileDebitUpdatesVisibleStackAndLedgerAtomically()
            throws Exception {
        Path file = temporary.resolve("source-projectile-inventory.properties");
        SourceInventoryItem bowRow = new SourceInventoryItem(
                701, 172, 1, true, 0, 0, true, 1, false,
                new ClassicWorldDropCatalog.ItemWire(
                        172, "weapon", "$245", "weapon", 173, 65, 1,
                        false, true, 30_000, 0, "bow", 6, 6,
                        "wood", 6, 0x08));
        SourceInventoryItem arrowRow = new SourceInventoryItem(
                702, 40_744, 2, false, 0, 0, true, 0, false,
                new ClassicWorldDropCatalog.ItemWire(
                        40_744, "etc", "$93", "arrow", 96, 75, 0,
                        true, true, 47, 68, "", 0, 0,
                        "silver", -1, 0));
        SourceProjectilePhysicalStats projectile = new SourceProjectilePhysicalStats(
                702, 40_744, -2, 7, 6, 14, 0, -1, true, 2);
        SourceWeaponPhysicalStats bow = new SourceWeaponPhysicalStats(
                172, 4, 6, 6, 15, 0, 0, 0, 0).withProjectile(projectile);
        SourceEquipmentMagicStats equipment = new SourceEquipmentMagicStats(
                new SourceEquipmentPhysicalStats(bow, false, false), 1, 0);

        GameStateStore state = new GameStateStore(file, true);
        GameStateStore.SourceInventoryReconciliation imported =
                state.reconcileSourceInventory(
                        CHARACTER, List.of(bowRow, arrowRow), equipment);
        assertEquals(1, imported.importedProjectileRows());
        assertEquals(1, imported.qualifiedSourceProjectileObjects());
        long runtimeObjectId = imported.sourceProjectileObjects().get(702L);
        RuntimeInventory runtime = RuntimeInventory.fromDurableState(
                imported.economy().inventory(), imported.economy().itemStates(),
                imported.definitions(), imported.sourceWeapons());
        assertEquals(2, runtime.item(runtimeObjectId).quantity());

        RuntimeInventory afterOne = runtime.copy();
        assertTrue(afterOne.consumeSourceProjectile(runtimeObjectId) != null);
        GameStateStore.SourceProjectileReservation first = state.reserveSourceProjectile(
                CHARACTER, projectile, runtimeObjectId,
                afterOne.catalogCounts(), afterOne.catalogItemStates());
        assertTrue(first.accepted());
        assertEquals(1, first.balance().remainingCount());
        assertEquals(1, state.economy(CHARACTER).inventory().get(40_744L));

        GameStateStore restarted = new GameStateStore(file, true);
        GameStateStore.SourceInventoryReconciliation restored =
                restarted.reconcileSourceInventory(
                        CHARACTER, List.of(bowRow, arrowRow), equipment);
        assertEquals(runtimeObjectId, restored.sourceProjectileObjects().get(702L));
        RuntimeInventory afterRestart = RuntimeInventory.fromDurableState(
                restored.economy().inventory(), restored.economy().itemStates(),
                restored.definitions(), restored.sourceWeapons());
        assertEquals(1, afterRestart.item(runtimeObjectId).quantity());

        RuntimeInventory exhausted = afterRestart.copy();
        assertTrue(exhausted.consumeSourceProjectile(runtimeObjectId) != null);
        GameStateStore.SourceProjectileReservation last =
                restarted.reserveSourceProjectile(
                        CHARACTER, projectile, runtimeObjectId,
                        exhausted.catalogCounts(), exhausted.catalogItemStates());
        assertTrue(last.accepted());
        assertEquals(0, last.balance().remainingCount());
        assertNull(restarted.economy(CHARACTER).inventory().get(40_744L));

        GameStateStore finalRestart = new GameStateStore(file, true);
        GameStateStore.SourceInventoryReconciliation empty =
                finalRestart.reconcileSourceInventory(
                        CHARACTER, List.of(bowRow, arrowRow), equipment);
        assertTrue(empty.sourceProjectileObjects().isEmpty());
        assertEquals(0, finalRestart.sourceProjectileBalance(
                CHARACTER, projectile).remainingCount());
    }

    @Test
    void bindsTheExactEquippedSourceWeaponAndPersistsUnequipAcrossRestart()
            throws Exception {
        Path file = temporary.resolve("source-weapon.properties");
        SourceInventoryItem sourceWeapon = new SourceInventoryItem(
                301, 24, 1, true, 4, 2, true, 1, false,
                new ClassicWorldDropCatalog.ItemWire(
                        24, "weapon", "$100", "weapon", 106, 26, 1,
                        false, true, 30_000, 0, "sword", 6, 8,
                        "iron", 6, 0x3F));
        SourceWeaponPhysicalStats physicalWeapon = new SourceWeaponPhysicalStats(
                24, 1, 6, 8, 1, 2, 3, 4, 2)
                .withAbility(5, 7, 0, 0)
                .withNameId("$100");
        SourceEquipmentMagicStats equipment = new SourceEquipmentMagicStats(
                new SourceEquipmentPhysicalStats(physicalWeapon, false, false), 1, 0);

        GameStateStore state = new GameStateStore(file, true);
        GameStateStore.SourceInventoryReconciliation imported =
                state.reconcileSourceInventory(
                        CHARACTER, List.of(sourceWeapon), equipment);
        assertEquals(1, imported.importedWeaponRows());
        assertEquals(1, imported.qualifiedSourceWeaponObjects());
        assertEquals(0, imported.retainedSourceWeight());
        SourcePet sourcePet = new SourcePet(
                901, 902, (int) InventoryCatalog.PET_COLLAR, 45034,
                "來源犬", 5, 30, 8, 1_000, 0);
        assertEquals(1, state.importSourcePets(
                CHARACTER, List.of(sourcePet), imported.definitions()).size());
        GameStateStore.Economy economy = state.economy(CHARACTER);
        RuntimeInventory runtime = RuntimeInventory.fromDurableState(
                economy.inventory(), economy.itemStates(),
                imported.definitions(), imported.sourceWeapons());
        RuntimeInventory.Item item = runtime.equippedWeaponItem();
        assertTrue(item != null && item.sourceBound() && item.equipped());
        assertTrue(runtime.hasQualifiedPhysicalWeapon(item.objectId()));
        assertEquals(physicalWeapon, runtime.equippedPhysicalWeapon());
        assertTrue(!runtime.remove(item.objectId(), 1));
        assertTrue(!runtime.toggleWeapon(item.objectId()).isEmpty());
        assertTrue(!runtime.hasEquippedWeapon());
        state.saveEconomyState(CHARACTER, economy.points(),
                runtime.catalogCounts(), runtime.catalogItemStates());

        GameStateStore restarted = new GameStateStore(file, true);
        GameStateStore.SourceInventoryReconciliation restored =
                restarted.reconcileSourceInventory(
                        CHARACTER, List.of(sourceWeapon), equipment);
        assertTrue(restarted.importSourcePets(
                CHARACTER, List.of(sourcePet), restored.definitions()).isEmpty());
        RuntimeInventory afterRestart = RuntimeInventory.fromDurableState(
                restored.economy().inventory(), restored.economy().itemStates(),
                restored.definitions(), restored.sourceWeapons());
        RuntimeInventory.Item restoredItem = afterRestart.items().stream()
                .filter(candidate -> candidate.definition().weapon())
                .findFirst().orElseThrow();
        assertEquals(item.objectId(), restoredItem.objectId());
        assertTrue(!restoredItem.equipped());
        assertTrue(afterRestart.hasQualifiedPhysicalWeapon(restoredItem.objectId()));
        assertTrue(!afterRestart.toggleWeapon(restoredItem.objectId()).isEmpty());
        assertEquals(physicalWeapon, afterRestart.equippedPhysicalWeapon());
        assertTrue(java.nio.file.Files.readString(file).contains("format.version=43"));

        SourceInventoryItem changedSource = new SourceInventoryItem(
                301, 24, 1, true, 5, 0, true, 1, false,
                sourceWeapon.wire());
        SourceWeaponPhysicalStats changedPhysical = physicalWeapon
                .withEnchantLevel(5).withDurability(0);
        SourceEquipmentMagicStats changedEquipment = new SourceEquipmentMagicStats(
                new SourceEquipmentPhysicalStats(changedPhysical, false, false), 1, 0);
        GameStateStore.SourceInventoryReconciliation changed =
                restarted.reconcileSourceInventory(
                        CHARACTER, List.of(changedSource), changedEquipment);
        assertEquals(1, changed.resetRows());
        RuntimeInventory reset = RuntimeInventory.fromDurableState(
                changed.economy().inventory(), changed.economy().itemStates(),
                changed.definitions(), changed.sourceWeapons());
        assertNotEquals(restoredItem.objectId(), reset.equippedWeaponItem().objectId());
        assertEquals(changedPhysical, reset.equippedPhysicalWeapon());

        GameStateStore.SourceInventoryReconciliation removed =
                restarted.reconcileSourceInventory(
                        CHARACTER, List.of(), SourceEquipmentMagicStats.none());
        assertEquals(1, removed.removedRows());
        assertEquals(0, removed.qualifiedSourceWeaponObjects());
        assertTrue(!removed.economy().inventory().containsKey(24L));
    }

    @Test
    void rejectsAnEquippedWeaponThatDoesNotMatchTheCombatSnapshot() throws Exception {
        SourceInventoryItem sourceWeapon = new SourceInventoryItem(
                401, 24, 1, true, 0, 0, true, 1, false,
                new ClassicWorldDropCatalog.ItemWire(
                        24, "weapon", "$100", "weapon", 106, 26, 1,
                        false, true, 30_000, 0, "sword", 6, 8,
                        "iron", 6, 0x3F));
        SourceWeaponPhysicalStats wrong = new SourceWeaponPhysicalStats(
                30, 1, 8, 12, 1, 0, 0, 0, 0);
        SourceEquipmentMagicStats equipment = new SourceEquipmentMagicStats(
                new SourceEquipmentPhysicalStats(wrong, false, false), 1, 0);
        GameStateStore state = new GameStateStore(
                temporary.resolve("source-weapon-mismatch.properties"), false);

        assertThrows(java.io.IOException.class, () -> state.reconcileSourceInventory(
                CHARACTER, List.of(sourceWeapon), equipment));
    }

    @Test
    void mirrorsSourceArmorNativeStatusAndRetainsLiToEquippedState()
            throws Exception {
        Path file = temporary.resolve("source-armor.properties");
        SourceInventoryItem sourceArmor = sourceArmor(501, true);
        SourceEquipmentMagicStats equipment = new SourceEquipmentMagicStats(
                SourceEquipmentPhysicalStats.none(), 1, 1);

        GameStateStore state = new GameStateStore(file, true);
        GameStateStore.SourceInventoryReconciliation imported =
                state.reconcileSourceInventory(
                        CHARACTER, List.of(sourceArmor), equipment);
        assertEquals(1, imported.importableRows());
        assertEquals(1, imported.importedArmorRows());
        assertEquals(1, imported.equippedSourceArmorObjects());

        RuntimeInventory runtime = RuntimeInventory.fromDurableState(
                imported.economy().inventory(), imported.economy().itemStates(),
                imported.definitions(), imported.sourceWeapons());
        RuntimeInventory.Item armor = runtime.items().stream()
                .filter(item -> item.definition().sourceArmor())
                .findFirst().orElseThrow();
        assertTrue(armor.sourceBound());
        assertTrue(armor.equipped());
        assertEquals(23, armor.definition().useType());
        assertEquals("+0 GM戒指 ($117)", armor.nativeViewName());
        // LiTo sub_4A2090 only accepts tags 1..33.  Wide HP/MP are saturated
        // into the native fields instead of emitting the later-client 0x27
        // custom-string tag which made this entire description disappear.
        assertEquals("13001401000000077F0D630EFF7F207F",
                java.util.HexFormat.of().withUpperCase()
                        .formatHex(armor.nativeStatusDetail()));
        assertFalse(armor.transferable());
        assertFalse(runtime.remove(armor.objectId(), 1));
        assertFalse(runtime.removeCatalog(armor.definition().catalogId(), 1));
        assertNull(runtime.removeEntire(armor.objectId()));
        assertTrue(runtime.toggleRing(armor.objectId()).isEmpty());
        assertEquals(Map.of(armor.objectId(), 501L),
                imported.sourceEquipmentObjects());
        assertEquals(1, runtime.toggleSourceArmor(armor.objectId()).size());
        assertFalse(runtime.item(armor.objectId()).equipped());
        state.saveEconomyState(CHARACTER, imported.economy().points(),
                runtime.catalogCounts(), runtime.catalogItemStates());

        GameStateStore restarted = new GameStateStore(file, true);
        GameStateStore.SourceInventoryReconciliation restored =
                restarted.reconcileSourceInventory(
                        CHARACTER, List.of(sourceArmor), equipment);
        RuntimeInventory afterRestart = RuntimeInventory.fromDurableState(
                restored.economy().inventory(), restored.economy().itemStates(),
                restored.definitions(), restored.sourceWeapons());
        RuntimeInventory.Item stable = afterRestart.items().stream()
                .filter(item -> item.definition().sourceArmor())
                .findFirst().orElseThrow();
        assertEquals(armor.objectId(), stable.objectId());
        assertFalse(stable.equipped());
        assertEquals(Map.of(stable.objectId(), 501L),
                restored.sourceEquipmentObjects());
        assertEquals(0, restored.importedRows());
    }

    @Test
    void retainedArmorStatusSeparatesBodyArmorFromAccessories() {
        InventoryCatalog.NativeArmorStatus status = new InventoryCatalog.NativeArmorStatus(
                5, 11, 30_000, 0x7F, 6,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1);
        String body = java.util.HexFormat.of().withUpperCase()
                .formatHex(status.detail(22, 1, 4, 2));
        String accessory = java.util.HexFormat.of().withUpperCase()
                .formatHex(status.detail(23, 1, 4, 2));

        assertEquals("13050B1E00000002040302077F", body);
        assertEquals("13050B1E000000077F", accessory);
    }

    @Test
    void rejectsArmorWhoseEquippedBitDiffersFromEquipmentSnapshot() throws Exception {
        GameStateStore state = new GameStateStore(
                temporary.resolve("source-armor-mismatch.properties"), false);
        SourceEquipmentMagicStats noEquippedArmor = new SourceEquipmentMagicStats(
                SourceEquipmentPhysicalStats.none(), 0, 0);

        assertThrows(java.io.IOException.class, () -> state.reconcileSourceInventory(
                CHARACTER, List.of(sourceArmor(601, true)), noEquippedArmor));
    }

    @Test
    void sourceDeadlineAndSealImportThenExpiryTombstonePreventsReimport()
            throws Exception {
        Path file = temporary.resolve("source-timed-sealed.properties");
        long deadline = 1_900_000_000_000L;
        ClassicWorldDropCatalog.ItemWire wire = etcWire(55_501, true);
        SourceInventoryItem source = new SourceInventoryItem(
                901, 55_501, 3, false, 0, 0, true, 1,
                false, wire, null, deadline, true);
        assertEquals(deadline, source.expiresAtEpochMillis());
        assertTrue(source.sealed());

        GameStateStore state = new GameStateStore(file, true);
        GameStateStore.SourceInventoryReconciliation imported =
                state.reconcileSourceInventory(CHARACTER, List.of(source));
        RuntimeInventory.PersistentItem durableItem = imported.economy().itemStates()
                .get(55_501L).get(0);
        assertEquals(deadline, durableItem.expiresAtEpochMillis());
        assertTrue(durableItem.sealed());
        RuntimeInventory runtime = RuntimeInventory.fromDurableState(
                imported.economy().inventory(), imported.economy().itemStates(),
                imported.definitions(), imported.sourceWeapons());
        RuntimeInventory.Item item = runtime.items().stream()
                .filter(candidate -> candidate.definition().catalogId() == 55_501)
                .findFirst().orElseThrow();
        assertEquals(deadline, item.expiresAtEpochMillis());
        assertTrue(item.sealed());

        RuntimeInventory expired = runtime.copy();
        assertEquals(List.of(item.objectId()), expired.expireTimedItems(deadline + 1)
                .stream().map(RuntimeInventory.Item::objectId).toList());
        state.saveEconomyStateExpiringTimedItems(
                CHARACTER, imported.economy().points(), expired.catalogCounts(),
                expired.catalogItemStates(), Set.of(item.objectId()), deadline + 1);

        GameStateStore restarted = new GameStateStore(file, true);
        GameStateStore.SourceInventoryReconciliation restored =
                restarted.reconcileSourceInventory(CHARACTER, List.of(source));
        assertEquals(0, restored.activeSourceObjects());
        assertFalse(restored.economy().inventory().containsKey(55_501L));
        String durable = java.nio.file.Files.readString(file);
        assertTrue(durable.contains("sourceInventory.0.consumed=1"));
    }

    @Test
    void offlineTimerSkipsLiveInventoryOwnerAndExpiresOtherProfiles()
            throws Exception {
        Path file = temporary.resolve("offline-timed-items.properties");
        long deadline = 1_900_000_000_000L;
        String offline = "ServerA\u001FOfflineHero";
        String online = "ServerA\u001FOnlineHero";
        SourceInventoryItem offlineItem = new SourceInventoryItem(
                910, 55_510, 1, false, 0, 0, true, 1, false,
                etcWire(55_510, true), null, deadline, false);
        SourceInventoryItem onlineItem = new SourceInventoryItem(
                911, 55_511, 1, false, 0, 0, true, 1, false,
                etcWire(55_511, true), null, deadline, false);
        GameStateStore state = new GameStateStore(file, true);
        state.reconcileSourceInventory(offline, List.of(offlineItem));
        state.reconcileSourceInventory(online, List.of(onlineItem));

        assertEquals(1, state.expireOfflineTimedItems(
                deadline + 1, Set.of(online)));

        assertFalse(state.economy(offline).inventory().containsKey(55_510L));
        assertTrue(state.economy(online).inventory().containsKey(55_511L));
        GameStateStore restarted = new GameStateStore(file, true);
        assertFalse(restarted.reconcileSourceInventory(offline, List.of(offlineItem))
                .economy().inventory().containsKey(55_510L));
    }

    @Test
    void sourceMagicDollTypeSurvivesDurableObjectReconciliation() throws Exception {
        GameStateStore state = new GameStateStore(
                temporary.resolve("source-magic-doll.properties"), false);
        SourceMagicDollType type = new SourceMagicDollType(
                55_000, 7, 2184, List.of(102, 50),
                List.of(40_308), List.of(1_000), 1_800, 6_100,
                "魔法娃娃：肥肥");
        ClassicWorldDropCatalog.ItemWire wire = new ClassicWorldDropCatalog.ItemWire(
                55_000, "etc", "$source55000", "other",
                2589, 3963, 1, false, true, 10, 0,
                "", 0, 0, "wood", -1, 0);
        SourceInventoryItem source = new SourceInventoryItem(
                920, 55_000, 1, false, 0, 0, true, 1,
                false, wire, null, 0, false, type);

        GameStateStore.SourceInventoryReconciliation result =
                state.reconcileSourceInventory(CHARACTER, List.of(source));

        assertEquals(1, result.sourceMagicDolls().size());
        Map.Entry<Long, SourceMagicDollType> mapped =
                result.sourceMagicDolls().entrySet().iterator().next();
        assertEquals(type, mapped.getValue());
        RuntimeInventory runtime = RuntimeInventory.fromDurableState(
                result.economy().inventory(), result.economy().itemStates(),
                result.definitions(), result.sourceWeapons());
        assertEquals(InventoryCatalog.Effect.MAGIC_DOLL,
                runtime.item(mapped.getKey()).definition().effect());
    }

    @Test
    void adoptsLiToOwnedSqlRowWithoutDuplicatingOrFreezingItsRuntimeObject()
            throws Exception {
        Path file = temporary.resolve("lito-owned-source-inventory.properties");
        long objectId = 0x7000_0010L;
        int catalogId = 50_020;
        InventoryCatalog.Definition definition = etcWire(catalogId, true)
                .inventoryDefinition();
        RuntimeInventory beforeSql = new RuntimeInventory();
        assertEquals(objectId, beforeSql.addDistinctState(
                definition, new RuntimeInventory.PersistentItem(
                        objectId, 2, 0, 0, true, 1,
                        false, true, false, null, 0, 0, false)));
        GameStateStore state = new GameStateStore(file, true);
        state.saveEconomyState(CHARACTER, 0, beforeSql.catalogCounts(),
                beforeSql.catalogItemStates());

        SourceInventoryItem persisted = new SourceInventoryItem(
                objectId, catalogId, 3, false, 0, 0, true, 1,
                false, etcWire(catalogId, true), null, 0, false, null,
                true, true);
        GameStateStore.SourceInventoryReconciliation adopted =
                state.reconcileSourceInventory(CHARACTER, List.of(persisted));

        assertEquals(3, adopted.economy().inventory().get((long) catalogId));
        assertEquals(1, adopted.economy().itemStates().get((long) catalogId).size());
        RuntimeInventory runtime = RuntimeInventory.fromDurableState(
                adopted.economy().inventory(), adopted.economy().itemStates(),
                adopted.definitions(), adopted.sourceWeapons());
        RuntimeInventory.Item item = runtime.item(objectId);
        assertTrue(item.sourceBound());
        assertTrue(item.litoOwned());
        assertTrue(item.tournamentReward());
        assertTrue(item.transferable());
        assertTrue(runtime.remove(objectId, 1));
        assertEquals(List.of(new GameStateStore.SourceInventoryBinding(
                        objectId, objectId, catalogId, false, true)),
                state.sourceInventoryBindings(CHARACTER));

        GameStateStore restarted = new GameStateStore(file, true);
        GameStateStore.SourceInventoryReconciliation restored =
                restarted.reconcileSourceInventory(CHARACTER, List.of(persisted));
        RuntimeInventory restoredRuntime = RuntimeInventory.fromDurableState(
                restored.economy().inventory(), restored.economy().itemStates(),
                restored.definitions(), restored.sourceWeapons());
        assertTrue(restoredRuntime.item(objectId).litoOwned());
        assertTrue(java.nio.file.Files.readString(file).contains("format.version=43"));
        assertTrue(java.nio.file.Files.readString(file).contains(
                "sourceInventory.0.litoOwned=1"));
    }

    private static SourceInventoryItem sourceArmor(
            long sourceObjectId, boolean equipped) {
        InventoryCatalog.NativeArmorStatus status = new InventoryCatalog.NativeArmorStatus(
                0, 20, 1_000, 0x7F, -1,
                0, 0, 0, 0, 0, 99, 999_999, 999_999, 0, 0, false,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9);
        ClassicWorldDropCatalog.ItemWire wire = new ClassicWorldDropCatalog.ItemWire(
                88_888, "armor", "GM戒指", "ring", 100, 101, 0,
                false, false, 1_000, 0, "", 0, 0,
                "gemstone", -1, 0x7F);
        return new SourceInventoryItem(
                sourceObjectId, 88_888, 1, equipped, 0, 0, true, 0,
                false, wire, status);
    }

    private static SourceInventoryItem item(long sourceObjectId, int catalogId, int count) {
        return new SourceInventoryItem(sourceObjectId, catalogId, count,
                false, 0, 0, true, 1, false, etcWire(catalogId, true));
    }

    private static ClassicWorldDropCatalog.ItemWire etcWire(
            int catalogId, boolean stackable) {
        return new ClassicWorldDropCatalog.ItemWire(
                catalogId, "etc", "$source" + catalogId, "normal",
                100, 101, 1, stackable, true, 10, 0,
                "", 0, 0, "paper", -1, 0);
    }
}
