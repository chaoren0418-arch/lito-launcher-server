package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Exact read-only inventory/template projection for both retained sources. */
final class LegacySourceInventoryIntegrationTest {
    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void projectsEverySelectedSourceRowWithoutChangingEitherDatabase() throws Exception {
        String configPath = System.getProperty(
                "lito.legacy.config", "src/test/resources/server-config/config/server.source-router-legacy-probe.properties");
        ServerConfig config = ServerConfig.load(Path.of(configPath));
        try (AccountRepository routed = AccountRepository.open(config)) {
            routed.healthCheck();
            assertSnapshot(routed.repositoryForClassicSource("完美天堂"), 10_163);
            assertSnapshot(routed.repositoryForClassicSource("完美天堂2"), 10_225);
            assertExactGmRingOverride(
                    routed.repositoryForClassicSource("完美天堂"));

            assertEveryRetainedCharacterFitsTheNativeSidecar(
                    routed.repositoryForClassicSource("完美天堂"), "完美天堂",
                    new long[] {10_163, 10_275, 10_337, 11_052, 11_554, 13_593});
            assertEveryRetainedCharacterFitsTheNativeSidecar(
                    routed.repositoryForClassicSource("完美天堂2"), "完美天堂2",
                    new long[] {10_225});
        }
    }

    private static void assertExactGmRingOverride(AccountRepository repository)
            throws Exception {
        List<SourceInventoryItem> before = repository.sourceInventory(10_163);
        SourceInventoryItem ring = before.stream()
                .filter(item -> item.sourceObjectId() == 10_221L)
                .findFirst().orElseThrow();
        assertEquals(88_888, ring.catalogId());
        assertTrue(ring.equipped());
        assertEquals(9, ring.sourceArmorStatus().armorType());

        SourceEquipmentEvaluation equipped =
                repository.equipmentEvaluation(10_163);
        assertEquals(999_999, equipped.vital().totalMaximumHp());
        assertEquals(999_999, equipped.vital().totalMaximumMp());
        assertEquals(99, equipped.vital().itemCharisma());
        assertEquals(1, equipped.magic().matchedArmorRows());

        SourceEquipmentEvaluation removed = repository.equipmentEvaluation(
                10_163, Map.of(10_221L, false));
        assertEquals(0, removed.vital().totalMaximumHp());
        assertEquals(0, removed.vital().totalMaximumMp());
        assertEquals(0, removed.vital().itemCharisma());
        assertEquals(0, removed.magic().matchedArmorRows());

        SourceEquipmentEvaluation restored = repository.equipmentEvaluation(
                10_163, Map.of(10_221L, true));
        assertEquals(equipped, restored);
        assertEquals(before, repository.sourceInventory(10_163));
    }

    private static void assertEveryRetainedCharacterFitsTheNativeSidecar(
            AccountRepository repository, String source, long[] characterIds)
            throws Exception {
        assertTrue(repository.description().contains("read-only=true"));
        GameStateStore state = new GameStateStore(Path.of("unused-source-inventory"), false);
        for (int index = 0; index < characterIds.length; index++) {
            String character = source + "\u001Fprobe-" + characterIds[index];
            GameStateStore.Economy baseline = state.economy(character);
            RuntimeInventory baselineRuntime = RuntimeInventory.fromDurableState(
                    baseline.inventory(), baseline.itemStates());
            List<SourceInventoryItem> rows = repository.sourceInventory(characterIds[index]);
            SourceEquipmentMagicStats equipment =
                    repository.equipmentMagicStats(characterIds[index]);
            GameStateStore.SourceInventoryReconciliation result =
                    state.reconcileSourceInventory(
                            character, rows, equipment);
            state.importSourcePets(character, repository.pets(characterIds[index]),
                    result.definitions());
            int importableRows = Math.toIntExact(rows.stream()
                    .filter(SourceInventoryItem::sidecarImportable).count());
            assertEquals(importableRows, result.importableRows());
            assertEquals(importableRows, result.activeSourceObjects());
            int armorRows = Math.toIntExact(rows.stream()
                    .filter(SourceInventoryItem::sourceArmorImportable).count());
            int equippedArmorRows = Math.toIntExact(rows.stream()
                    .filter(SourceInventoryItem::sourceArmorImportable)
                    .filter(SourceInventoryItem::equipped).count());
            assertEquals(armorRows, result.importedArmorRows());
            assertEquals(equippedArmorRows,
                    result.equippedSourceArmorObjects());
            int projectileRows = Math.toIntExact(rows.stream()
                    .filter(SourceInventoryItem::sourceProjectileImportable).count());
            assertEquals(projectileRows, result.importedProjectileRows());
            assertEquals(projectileRows, result.sourceProjectileObjects().size());
            assertEquals(equipment.physical().weapon().projectile().present() ? 1 : 0,
                    result.qualifiedSourceProjectileObjects());
            GameStateStore.Economy economy = state.economy(character);
            RuntimeInventory runtime = RuntimeInventory.fromDurableState(
                    economy.inventory(), economy.itemStates(),
                    result.definitions(), result.sourceWeapons());
            // Format 30 must reconstruct every imported template from LiTo's
            // own durable object state. Warehouse/trade recipients do not own
            // the original character's transient sourceDefinitions map.
            RuntimeInventory sidecarOnly = RuntimeInventory.fromDurableState(
                    economy.inventory(), economy.itemStates());
            Map<Long, InventoryCatalog.Definition> durableDefinitions = sidecarOnly.items()
                    .stream().collect(java.util.stream.Collectors.toMap(
                            RuntimeInventory.Item::objectId,
                            RuntimeInventory.Item::definition));
            assertTrue(runtime.items().stream().filter(RuntimeInventory.Item::sourceBound)
                    .allMatch(item -> item.definition().equals(
                            durableDefinitions.get(item.objectId()))));
            assertTrue(runtime.size() <= RuntimeInventory.MAX_SLOTS);
            assertTrue(runtime.items().stream().allMatch(item ->
                    item.wireItem().objectId() == item.objectId()));
            assertEquals(armorRows, runtime.items().stream()
                    .filter(RuntimeInventory.Item::sourceBound)
                    .filter(item -> item.definition().sourceArmor()).count());
            assertEquals(equippedArmorRows, runtime.items().stream()
                    .filter(RuntimeInventory.Item::sourceBound)
                    .filter(item -> item.definition().sourceArmor())
                    .filter(RuntimeInventory.Item::equipped).count());
            assertEquals(projectileRows, runtime.items().stream()
                    .filter(RuntimeInventory.Item::sourceBound)
                    .filter(item -> item.definition().projectile()).count());
            assertTrue(runtime.items().stream()
                    .filter(item -> item.definition().projectile())
                    .allMatch(item -> item.definition().useType() == 0xFE
                            || item.definition().useType() == 0xFD));
            int sourceSnapshotWeight = rows.stream()
                    .mapToInt(SourceInventoryItem::nativeWeight)
                    .reduce(0, BattleInventoryWeight::addWeight);
            int expectedWeight = BattleInventoryWeight.addWeight(
                    sourceSnapshotWeight, baselineRuntime.totalWeight());
            assertEquals(expectedWeight, BattleInventoryWeight.addWeight(
                    result.retainedSourceWeight(), runtime.totalWeight()));
            assertEquals(equipment.physical().weapon().present() ? 1 : 0,
                    result.qualifiedSourceWeaponObjects());
            if (equipment.physical().weapon().present()) {
                RuntimeInventory.Item weapon = runtime.equippedWeaponItem();
                assertTrue(weapon != null && weapon.sourceBound());
                assertEquals(equipment.physical().weapon(),
                        runtime.equippedPhysicalWeapon());
            }
            // Port 2000 owns mutable production state and may legitimately
            // change it between these SELECT-only calls. Each immutable
            // snapshot is validated independently; a difference is recorded
            // as an external race instead of being misreported as a LiTo
            // write. The repository itself is required above to open every
            // JDBC connection in read-only mode.
            List<SourceInventoryItem> afterRows =
                    repository.sourceInventory(characterIds[index]);
            SourceEquipmentMagicStats afterEquipment =
                    repository.equipmentMagicStats(characterIds[index]);
            validateSourceSnapshot(afterRows, afterEquipment);
            if (!rows.equals(afterRows) || !equipment.equals(afterEquipment)) {
                System.out.printf("[LiTo-test] external source mutation source=%s"
                                + " character=%d rows=%d->%d equipped-armor=%d->%d%n",
                        source, characterIds[index], rows.size(), afterRows.size(),
                        equipment.matchedArmorRows(), afterEquipment.matchedArmorRows());
            }
        }
    }

    private static void validateSourceSnapshot(
            List<SourceInventoryItem> rows,
            SourceEquipmentMagicStats equipment) {
        assertEquals(rows.size(), rows.stream()
                .map(SourceInventoryItem::sourceObjectId).distinct().count());
        assertTrue(rows.stream().allMatch(row -> row.definition().catalogId()
                == row.catalogId()));
        assertEquals(equipment.matchedArmorRows(), rows.stream()
                .filter(SourceInventoryItem::equipped)
                .filter(row -> row.wire().kind().equals("armor")).count());
    }

    private static void assertSnapshot(
            AccountRepository repository, long characterObjectId) throws Exception {
        List<SourceInventoryItem> items = repository.sourceInventory(characterObjectId);
        long quantity = items.stream().mapToLong(SourceInventoryItem::quantity).sum();
        long weapons = kind(items, "weapon");
        long armor = kind(items, "armor");
        long etc = kind(items, "etc");
        long petCollars = items.stream()
                .filter(SourceInventoryItem::linkedPetCollar).count();
        long importable = items.stream()
                .filter(SourceInventoryItem::sidecarImportable).count();
        assertEquals(repository.inventoryWeightStats(characterObjectId).rawWeight(),
                items.stream().mapToInt(SourceInventoryItem::nativeWeight)
                        .reduce(0, BattleInventoryWeight::addWeight));
        assertEquals(items.size(),
                items.stream().map(SourceInventoryItem::sourceObjectId).distinct().count());
        assertTrue(items.stream().allMatch(item -> item.definition().catalogId()
                == item.catalogId()));
        if (characterObjectId == 10_163) {
            assertTrue(items.stream().filter(item -> item.catalogId() == 40308)
                    .findFirst().orElseThrow().definition().tradable());
            assertTrue(!items.stream().filter(item -> item.sourceObjectId() == 10_165)
                    .findFirst().orElseThrow().definition().tradable());
        }
        assertEquals(items, repository.sourceInventory(characterObjectId));
        System.out.printf("[LiTo-test] %s source-inventory rows=%d quantity=%d"
                        + " weapon=%d armor=%d etc=%d pet-collars=%d importable=%d%n",
                repository.description(), items.size(), quantity,
                weapons, armor, etc, petCollars, importable);
    }

    private static long kind(List<SourceInventoryItem> items, String kind) {
        return items.stream().filter(item -> item.wire().kind().equals(kind)).count();
    }
}
