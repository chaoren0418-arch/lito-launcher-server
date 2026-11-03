package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Retained checkTargetItem/searchTargetItem/onTargetItem state machine. */
final class ClassicWorldMonsterPickupTest {
    private static final int MAP_ID = 4;
    private static final long OBJECT_BASE = 0x6840_0000L;

    @Test
    void adjacentWholeStackMovesAtomicallyIntoNpcInventoryAndSleepsOneSecond() {
        int[] home = openCenter();
        ClassicWorldMonsterPopulation population = population(home, 45009);
        long monsterId = OBJECT_BASE + 1;
        ClassicWorldGroundItems ground = new ClassicWorldGroundItems();
        ClassicWorldGroundItems.Item adena = ground.put(
                MAP_ID, ClassicWorldDropCatalog.item(
                        MAP_ID, (int) InventoryCatalog.ADENA),
                37, home[0] + 1, home[1], 45009);
        assertNotNull(adena);

        ClassicWorldMonsterPopulation.ItemAction action = population.itemAction(
                MAP_ID, monsterId,
                ground.inScreen(MAP_ID, home[0], home[1]),
                Set.of(), 1L, 0);
        assertNotNull(action);
        assertEquals(ClassicWorldMonsterPopulation.ItemActionKind.PICKUP_READY,
                action.kind());

        ClassicWorldMonsterPopulation.ItemPickup[] pickup =
                new ClassicWorldMonsterPopulation.ItemPickup[1];
        ClassicWorldGroundItems.Take take = ground.takeWholeIf(
                MAP_ID, -1, adena.objectId(), item -> {
                    pickup[0] = population.acceptGroundItem(
                            MAP_ID, monsterId, item, 1L);
                    return pickup[0] != null;
                });
        assertNotNull(take);
        assertNotNull(pickup[0]);
        assertNull(ground.item(MAP_ID, adena.objectId()));
        List<RuntimeInventory.Item> carried = population.carriedItems(
                MAP_ID, monsterId);
        assertEquals(1, carried.size());
        assertEquals(37, carried.get(0).quantity());
        assertFalse(population.actionReady(
                MAP_ID, monsterId, 1L + 999_999_999L));
        assertTrue(population.actionReady(
                MAP_ID, monsterId, 1L + 1_000_000_000L));

        ClassicWorldMonsterPopulation.Hit death = population.damage(
                MAP_ID, monsterId, 0x4200_0771L, Integer.MAX_VALUE);
        assertTrue(death.diedNow());
        List<RuntimeInventory.Item> deathItems =
                population.takeMonsterCarriedItems(MAP_ID, monsterId);
        assertEquals(1, deathItems.size());
        assertEquals(37, deathItems.get(0).quantity());
        assertTrue(population.takeMonsterCarriedItems(
                MAP_ID, monsterId).isEmpty());
    }

    @Test
    void distantTargetUsesNpcMoveOrderAndCombatClearsItemQueue() {
        int[] home = openCenter();
        ClassicWorldMonsterPopulation population = population(home, 45009);
        long monsterId = OBJECT_BASE + 1;
        ClassicWorldGroundItems ground = new ClassicWorldGroundItems();
        ClassicWorldGroundItems.Item item = ground.put(
                MAP_ID, ClassicWorldDropCatalog.item(
                        MAP_ID, (int) InventoryCatalog.ADENA),
                1, home[0] + 3, home[1], 45009);

        ClassicWorldMonsterPopulation.ItemAction move = population.itemAction(
                MAP_ID, monsterId,
                ground.inScreen(MAP_ID, home[0], home[1]),
                Set.of(), 1L, 0);
        assertEquals(ClassicWorldMonsterPopulation.ItemActionKind.MOVED,
                move.kind());
        assertEquals(home[0] + 1, move.movement().after().x());
        assertEquals(home[1], move.movement().after().y());
        assertEquals(2, move.movement().after().heading());

        assertTrue(population.provoke(
                MAP_ID, monsterId, 0x4200_0771L));
        population.abandonItemTargetsForCombat(MAP_ID, monsterId);
        population.clearTarget(MAP_ID, monsterId, 0x4200_0771L);
        assertNull(population.itemAction(
                MAP_ID, monsterId, List.of(), Set.of(),
                10_000_000_000L, 0));
        assertNotNull(ground.item(MAP_ID, item.objectId()));
    }

    @Test
    void nonPickupTemplateLeavesGroundForOrdinaryIdleBranch() {
        int[] home = openCenter();
        ClassicWorldMonsterPopulation population = population(home, 45010);
        ClassicWorldGroundItems ground = new ClassicWorldGroundItems();
        ClassicWorldGroundItems.Item item = ground.put(
                MAP_ID, ClassicWorldDropCatalog.item(
                        MAP_ID, (int) InventoryCatalog.ADENA),
                1, home[0] + 1, home[1], 45010);
        assertNull(population.itemAction(
                MAP_ID, OBJECT_BASE + 1,
                ground.inScreen(MAP_ID, home[0], home[1]),
                Set.of(), 1L, 0));
        assertNotNull(ground.item(MAP_ID, item.objectId()));
    }

    @Test
    void autonomousPickupDoesNotReapplyPlayerTradabilityGate() {
        int[] home = openCenter();
        ClassicWorldMonsterPopulation population = population(home, 45009);
        long monsterId = OBJECT_BASE + 1;
        InventoryCatalog.Definition untradable = new InventoryCatalog.Definition(
                65_785, 0, 321, 322, 1, true,
                "NPC Ground Item", "native autonomous pickup", 0, 0,
                true, false, BattleActionCodes.WALK, 0,
                InventoryCatalog.Effect.NONE, 0, 100, 0, null, null);
        ClassicWorldGroundItems ground = new ClassicWorldGroundItems();
        ClassicWorldGroundItems.Item item = ground.put(
                MAP_ID, -1, untradable, 1,
                home[0] + 1, home[1], 45009);

        ClassicWorldMonsterPopulation.ItemAction action = population.itemAction(
                MAP_ID, monsterId,
                ground.inScreen(MAP_ID, home[0], home[1]),
                Set.of(), 1L, 0);
        assertNotNull(action);
        ClassicWorldMonsterPopulation.ItemPickup[] pickup =
                new ClassicWorldMonsterPopulation.ItemPickup[1];
        ClassicWorldGroundItems.Take take = ground.takeWholeIf(
                MAP_ID, -1, item.objectId(), candidate -> {
                    pickup[0] = population.acceptGroundItem(
                            MAP_ID, monsterId, candidate, 1L);
                    return pickup[0] != null;
                });

        assertNotNull(take);
        assertNotNull(pickup[0]);
        assertEquals(65_785, population.carriedItems(MAP_ID, monsterId)
                .get(0).definition().catalogId());
    }

    private static ClassicWorldMonsterPopulation population(
            int[] home, int npcId) {
        ClassicWorldMonsterCatalog.MonsterTemplate template =
                ClassicWorldMonsterCatalog.definitions(MAP_ID).stream()
                        .map(ClassicWorldMonsterCatalog.SpawnDefinition::template)
                        .filter(candidate -> candidate.npcId() == npcId)
                        .findFirst().orElseThrow();
        ClassicWorldMonsterCatalog.SpawnDefinition definition =
                new ClassicWorldMonsterCatalog.SpawnDefinition(
                        MAP_ID, 993_771, "native-item-target-fixture", 1, 0,
                        home[0], home[1], 0, 0, 0, 0, 0, 0, 2,
                        0, 0, false, 0, false, 0, template);
        return ClassicWorldMonsterPopulation.retainedSourceSlice(
                771L, OBJECT_BASE, OBJECT_BASE + 0x1000L,
                List.of(definition));
    }

    private static int[] openCenter() {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(MAP_ID);
        for (int x = map.startX() + 4; x <= map.endX() - 4; x++) {
            for (int y = map.startY() + 2; y <= map.endY() - 2; y++) {
                if (ClassicWorldNpcCatalog.occupies(MAP_ID, x, y)) continue;
                boolean open = true;
                for (int step = 1; step <= 3; step++) {
                    open &= map.canStep(x + step - 1, y, x + step, y)
                            && !ClassicWorldNpcCatalog.occupies(
                                    MAP_ID, x + step, y);
                }
                if (open) return new int[]{x, y};
            }
        }
        throw new AssertionError("missing open monster-pickup corridor");
    }
}
