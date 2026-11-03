package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class ClassicWorldMonsterItemLifecycleTest {
    private static final int MAP_ID = 21;
    private static final long OBJECT_BASE = 0x6860_0000L;
    private static final long MONSTER_ID = OBJECT_BASE + 1;

    @Test
    void cGiveRunsRefineThenShuffleAndTracksInputForDigest() throws Exception {
        ClassicWorldMonsterPopulation population = population();
        ClassicWorldMonsterPopulation.Snapshot monster =
                population.actor(MAP_ID, MONSTER_ID);
        assertNotNull(monster);

        give(population, monster, 40508, 300);
        give(population, monster, 40521, 6);
        ClassicWorldMonsterPopulation.MonsterItemReceipt receipt =
                give(population, monster, 40045, 6);

        assertEquals(List.of(new ClassicWorldMonsterPopulation.Refinement(20, 2)),
                receipt.refinements());
        List<RuntimeInventory.Item> carried =
                population.carriedItems(MAP_ID, MONSTER_ID);
        assertEquals(2, carried.stream()
                .filter(item -> item.definition().catalogId() == 20)
                .mapToLong(RuntimeInventory.Item::quantity).sum());
        assertEquals(0, carried.stream()
                .filter(item -> item.definition().catalogId() == 40508
                        || item.definition().catalogId() == 40521
                        || item.definition().catalogId() == 40045)
                .mapToLong(RuntimeInventory.Item::quantity).sum());
        assertEquals(30, population.digestRemainingSeconds(
                MAP_ID, MONSTER_ID, receipt.destinationObjectId()));
    }

    @Test
    void digestClockSkipsDeadActorAndDeletesWholeTrackedStack() throws Exception {
        ClassicWorldMonsterPopulation population = population();
        ClassicWorldMonsterPopulation.Snapshot monster = population.actor(MAP_ID, MONSTER_ID);
        ClassicWorldMonsterPopulation.MonsterItemReceipt receipt =
                give(population, monster, 40508, 7);
        assertTrue(population.advanceDigestItems(25).isEmpty());
        assertEquals(5, population.digestRemainingSeconds(
                MAP_ID, MONSTER_ID, receipt.destinationObjectId()));

        ClassicWorldMonsterPopulation.Hit death =
                population.damage(MAP_ID, MONSTER_ID, 91, Integer.MAX_VALUE);
        assertTrue(death.diedNow());
        assertTrue(population.advanceDigestItems(5).isEmpty());
        assertEquals(5, population.digestRemainingSeconds(
                MAP_ID, MONSTER_ID, receipt.destinationObjectId()));
    }

    @Test
    void liveDigestExpiryRemovesTheEntireCurrentObjectCount() throws Exception {
        ClassicWorldMonsterPopulation population = population();
        ClassicWorldMonsterPopulation.Snapshot monster = population.actor(MAP_ID, MONSTER_ID);
        ClassicWorldMonsterPopulation.MonsterItemReceipt first =
                give(population, monster, 40508, 3);
        ClassicWorldMonsterPopulation.MonsterItemReceipt second =
                give(population, monster, 40508, 4);
        assertEquals(first.destinationObjectId(), second.destinationObjectId());

        List<ClassicWorldMonsterPopulation.DigestRemoval> expired =
                population.advanceDigestItems(30);
        assertEquals(1, expired.size());
        assertEquals(7, expired.get(0).item().quantity());
        assertTrue(population.carriedItems(MAP_ID, MONSTER_ID).isEmpty());
        assertEquals(0, population.digestRemainingSeconds(
                MAP_ID, MONSTER_ID, first.destinationObjectId()));
    }

    @Test
    void staleCoordinateAndDeadActorAreNotReceivable() throws Exception {
        ClassicWorldMonsterPopulation population = population();
        ClassicWorldMonsterPopulation.Snapshot monster = population.actor(MAP_ID, MONSTER_ID);
        RuntimeInventory inventory = itemInventory(40508, 1);
        RuntimeInventory.Item item = inventory.items().get(0);
        assertEquals(ClassicWorldMonsterPopulation.GiveAdmission.STALE_POSITION,
                population.canReceiveGivenItem(MAP_ID, MONSTER_ID,
                        monster.x() + 1, monster.y(), item, 1));
        population.damage(MAP_ID, MONSTER_ID, 91, Integer.MAX_VALUE);
        assertEquals(ClassicWorldMonsterPopulation.GiveAdmission.UNKNOWN_TARGET,
                population.canReceiveGivenItem(MAP_ID, MONSTER_ID,
                        monster.x(), monster.y(), item, 1));
        assertNull(population.receiveGivenItemPersisted(MAP_ID, MONSTER_ID,
                monster.x(), monster.y(), item, 1, () -> {}));
    }

    @Test
    void autonomousGroundPickupRunsTheSameRefineShuffleDigestOnGetItemOrder()
            throws Exception {
        ClassicWorldMonsterPopulation population = population();
        ClassicWorldMonsterPopulation.Snapshot monster =
                population.actor(MAP_ID, MONSTER_ID);
        give(population, monster, 40508, 300);
        give(population, monster, 40521, 6);

        ClassicWorldGroundItems ground = new ClassicWorldGroundItems();
        ClassicWorldGroundItems.Item ingredient = ground.put(
                MAP_ID, -1, InventoryCatalog.require(40045), 6,
                monster.x() + 1, monster.y(), 45032);
        ClassicWorldMonsterPopulation.ItemAction action = population.itemAction(
                MAP_ID, MONSTER_ID,
                ground.inScreen(MAP_ID, monster.x(), monster.y()),
                Set.of(), 1L, 0);
        assertNotNull(action);
        assertEquals(ClassicWorldMonsterPopulation.ItemActionKind.PICKUP_READY,
                action.kind());

        ClassicWorldMonsterPopulation.ItemPickup[] receipt =
                new ClassicWorldMonsterPopulation.ItemPickup[1];
        ClassicWorldGroundItems.Take take = ground.takeWholeIf(
                MAP_ID, -1, ingredient.objectId(), item -> {
                    receipt[0] = population.acceptGroundItem(
                            MAP_ID, MONSTER_ID, item, 1L);
                    return receipt[0] != null;
                });

        assertNotNull(take);
        assertNotNull(receipt[0]);
        assertEquals(List.of(new ClassicWorldMonsterPopulation.Refinement(20, 2)),
                receipt[0].refinements());
        assertEquals(30, receipt[0].digestSeconds());
        assertEquals(ingredient.objectId(), receipt[0].itemObjectId());
        assertEquals(ingredient.objectId(), receipt[0].destinationObjectId());
        List<RuntimeInventory.Item> carried = population.carriedItems(
                MAP_ID, MONSTER_ID);
        assertEquals(2, carried.stream()
                .filter(item -> item.definition().catalogId() == 20)
                .mapToLong(RuntimeInventory.Item::quantity).sum());
        assertEquals(0, carried.stream()
                .filter(item -> item.definition().catalogId() == 40508
                        || item.definition().catalogId() == 40521
                        || item.definition().catalogId() == 40045)
                .mapToLong(RuntimeInventory.Item::quantity).sum());
    }

    private static ClassicWorldMonsterPopulation.MonsterItemReceipt give(
            ClassicWorldMonsterPopulation population,
            ClassicWorldMonsterPopulation.Snapshot monster,
            long catalogId, int quantity) throws Exception {
        RuntimeInventory source = itemInventory(catalogId, quantity);
        RuntimeInventory.Item item = source.items().get(0);
        ClassicWorldMonsterPopulation.MonsterItemReceipt receipt =
                population.receiveGivenItemPersisted(MAP_ID, MONSTER_ID,
                        monster.x(), monster.y(), item, quantity, () -> {});
        assertNotNull(receipt);
        return receipt;
    }

    private static RuntimeInventory itemInventory(long catalogId, int quantity) {
        InventoryCatalog.Definition definition = InventoryCatalog.find(catalogId);
        assertNotNull(definition, "missing retained item " + catalogId);
        RuntimeInventory inventory = new RuntimeInventory();
        assertTrue(inventory.add(definition, quantity));
        return inventory;
    }

    private static ClassicWorldMonsterPopulation population() {
        ClassicWorldMonsterCatalog.SpawnDefinition source =
                ClassicWorldMonsterCatalog.definitions(MAP_ID).stream()
                        .filter(row -> row.template().npcId() == 45032)
                        .findFirst().orElseThrow();
        ClassicWorldMonsterCatalog.SpawnDefinition definition =
                new ClassicWorldMonsterCatalog.SpawnDefinition(
                        MAP_ID, 993_778, "native-give-refine-digest-fixture", 1,
                        0, source.locX(), source.locY(), source.randomX(),
                        source.randomY(), source.locX1(), source.locY1(),
                        source.locX2(), source.locY2(), source.heading(),
                        source.minRespawnSeconds(), source.maxRespawnSeconds(),
                        source.respawnScreen(), source.movementDistance(),
                        source.rest(), source.groupId(), source.template());
        return ClassicWorldMonsterPopulation.retainedSourceSlice(
                778L, OBJECT_BASE, OBJECT_BASE + 0x1000L, List.of(definition));
    }
}
