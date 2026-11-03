package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Retained L1MonsterInstance onItemUse/useItem inventory behavior. */
final class ClassicWorldMonsterItemUseTest {
    private static final int MAP_ID = 4;
    private static final long OBJECT_BASE = 0x6842_0000L;
    private static final long MONSTER_ID = OBJECT_BASE + 1;
    private static final long PLAYER_ID = 0x4200_0772L;

    @Test
    void initialCombatTickUsesGreaterHasteThenGreaterHealingInSourceOrder() {
        Fixture fixture = fixture(772L);
        long now = 1L;
        now = carry(fixture, 40013, now);
        now = carry(fixture, 40018, now);
        now = carry(fixture, 40010, now);
        now = carry(fixture, 40012, now);
        ClassicWorldMonsterPopulation.Snapshot full = fixture.population.actor(
                MAP_ID, MONSTER_ID);
        int targetHp = Math.max(1, full.maximumHp() * 30 / 100);
        assertNotNull(fixture.population.damage(
                MAP_ID, MONSTER_ID, PLAYER_ID,
                full.currentHp() - targetHp));
        assertTrue(fixture.population.provoke(MAP_ID, MONSTER_ID, PLAYER_ID));

        List<ClassicWorldMonsterPopulation.ItemUse> uses =
                fixture.population.itemUse(MAP_ID, MONSTER_ID, now, 40, 50);
        assertEquals(2, uses.size());
        assertEquals(ClassicWorldMonsterPopulation.ItemUseKind.HASTE,
                uses.get(0).kind());
        assertEquals(40018, uses.get(0).catalogId());
        assertEquals(191, uses.get(0).effectGfx());
        assertEquals(1800, uses.get(0).durationSeconds());
        assertEquals(ClassicWorldMonsterPopulation.ItemUseKind.HEAL,
                uses.get(1).kind());
        assertEquals(40012, uses.get(1).catalogId());
        assertEquals(197, uses.get(1).effectGfx());
        assertTrue(uses.get(1).appliedHpDelta() > 0);
        assertTrue(fixture.population.hasteActive(
                MAP_ID, MONSTER_ID, now + 1));

        fixture.population.beginTargetAction(MAP_ID, MONSTER_ID);
        assertTrue(fixture.population.itemUse(
                MAP_ID, MONSTER_ID, now + 1, 0, 99).isEmpty());
        assertEquals(1, quantity(fixture.population, 40013));
        assertEquals(0, quantity(fixture.population, 40018));
        assertEquals(1, quantity(fixture.population, 40010));
        assertEquals(0, quantity(fixture.population, 40012));
    }

    @Test
    void healProbabilityIsInclusiveAndPolluteWaterHalvesOddBaseAmount() {
        Fixture accepted = fixture(773L);
        long now = carry(accepted, 40010, 1L);
        ClassicWorldMonsterPopulation.Snapshot full = accepted.population.actor(
                MAP_ID, MONSTER_ID);
        int targetHp = Math.max(1, full.maximumHp() * 30 / 100);
        accepted.population.damage(
                MAP_ID, MONSTER_ID, full.currentHp() - targetHp);
        assertTrue(accepted.population.polluteWater(
                MAP_ID, MONSTER_ID, now, 30));
        List<ClassicWorldMonsterPopulation.ItemUse> uses =
                accepted.population.itemUse(
                        MAP_ID, MONSTER_ID, now, 99, 50);
        assertEquals(1, uses.size());
        assertEquals(40010, uses.get(0).catalogId());
        assertEquals(189, uses.get(0).effectGfx());
        assertEquals(7, uses.get(0).appliedHpDelta());

        Fixture rejected = fixture(774L);
        long rejectedNow = carry(rejected, 40010, 1L);
        ClassicWorldMonsterPopulation.Snapshot rejectedFull =
                rejected.population.actor(MAP_ID, MONSTER_ID);
        rejected.population.damage(MAP_ID, MONSTER_ID,
                rejectedFull.currentHp()
                        - Math.max(1, rejectedFull.maximumHp() * 30 / 100));
        assertTrue(rejected.population.itemUse(
                MAP_ID, MONSTER_ID, rejectedNow, 99, 51).isEmpty());
        assertEquals(1, quantity(rejected.population, 40010));
    }

    @Test
    void decayPotionBlocksBothUseItemBranchesWithoutConsumingInventory() {
        Fixture fixture = fixture(775L);
        long now = carry(fixture, 40013, 1L);
        now = carry(fixture, 40010, now);
        ClassicWorldMonsterPopulation.Snapshot full = fixture.population.actor(
                MAP_ID, MONSTER_ID);
        fixture.population.damage(MAP_ID, MONSTER_ID, PLAYER_ID,
                full.currentHp()
                        - Math.max(1, full.maximumHp() * 30 / 100));
        assertTrue(fixture.population.decayPotion(
                MAP_ID, MONSTER_ID, now, 16));
        assertTrue(fixture.population.itemUse(
                MAP_ID, MONSTER_ID, now, 0, 0).isEmpty());
        assertFalse(fixture.population.hasteActive(
                MAP_ID, MONSTER_ID, now));
        assertEquals(1, quantity(fixture.population, 40013));
        assertEquals(1, quantity(fixture.population, 40010));
    }

    @Test
    void unobservedNoTargetEndsAiLifetimeAndAllowsNextInitialHasteAttempt() {
        Fixture fixture = fixture(776L);
        long now = carry(fixture, 40013, 1L);
        assertTrue(fixture.population.provoke(
                MAP_ID, MONSTER_ID, PLAYER_ID));
        assertTrue(fixture.population.itemUse(
                MAP_ID, MONSTER_ID, now, 99, 99).isEmpty());
        fixture.population.beginTargetAction(MAP_ID, MONSTER_ID);
        fixture.population.clearTarget(MAP_ID, MONSTER_ID, PLAYER_ID);
        assertTrue(fixture.population.resetUnobservedHome(
                MAP_ID, Set.of(), now + 1).isEmpty());

        assertTrue(fixture.population.provoke(
                MAP_ID, MONSTER_ID, PLAYER_ID));
        List<ClassicWorldMonsterPopulation.ItemUse> uses =
                fixture.population.itemUse(
                        MAP_ID, MONSTER_ID, now + 2, 0, 99);
        assertEquals(1, uses.size());
        assertEquals(ClassicWorldMonsterPopulation.ItemUseKind.HASTE,
                uses.get(0).kind());
        assertEquals(0, quantity(fixture.population, 40013));
    }

    private static long carry(Fixture fixture, long catalogId, long now) {
        ClassicWorldMonsterPopulation.Snapshot actor = fixture.population.actor(
                MAP_ID, MONSTER_ID);
        InventoryCatalog.Definition definition =
                ClassicWorldDropCatalog.inventoryDefinition((int) catalogId);
        if (definition == null) {
            definition = InventoryCatalog.builtIn(catalogId);
        }
        assertNotNull(definition, "missing source item " + catalogId);
        ClassicWorldGroundItems.Item ground = fixture.ground.put(
                MAP_ID, -1, definition, 1, fixture.itemX, fixture.itemY,
                actor.npcId());
        assertNotNull(ground);
        ClassicWorldMonsterPopulation.ItemAction action =
                fixture.population.itemAction(
                        MAP_ID, MONSTER_ID,
                        fixture.ground.inScreen(MAP_ID, actor.x(), actor.y()),
                        Set.of(), now, 0);
        assertEquals(ClassicWorldMonsterPopulation.ItemActionKind.PICKUP_READY,
                action.kind());
        ClassicWorldGroundItems.Take take = fixture.ground.takeWholeIf(
                MAP_ID, -1, ground.objectId(), item ->
                        fixture.population.acceptGroundItem(
                                MAP_ID, MONSTER_ID, item, now) != null);
        assertNotNull(take);
        return now + 1_000_000_000L;
    }

    private static long quantity(
            ClassicWorldMonsterPopulation population, long catalogId) {
        return population.carriedItems(MAP_ID, MONSTER_ID).stream()
                .filter(item -> item.definition().catalogId() == catalogId)
                .mapToLong(RuntimeInventory.Item::quantity).sum();
    }

    private static Fixture fixture(long seed) {
        int[] home = openCenter();
        ClassicWorldMonsterCatalog.MonsterTemplate template =
                ClassicWorldMonsterCatalog.definitions(MAP_ID).stream()
                        .map(ClassicWorldMonsterCatalog.SpawnDefinition::template)
                        .filter(ClassicWorldMonsterPickupCatalog::enabled)
                        .filter(candidate -> candidate.hp() >= 100)
                        .filter(candidate -> candidate.atkspeed() > 0)
                        .findFirst().orElseThrow();
        ClassicWorldMonsterCatalog.SpawnDefinition definition =
                new ClassicWorldMonsterCatalog.SpawnDefinition(
                        MAP_ID, 994_772, "native-monster-item-use-fixture",
                        1, 0, home[0], home[1], 0, 0, 0, 0, 0, 0, 2,
                        0, 0, false, 0, false, 0, template);
        ClassicWorldMonsterPopulation population =
                ClassicWorldMonsterPopulation.retainedSourceSlice(
                        seed, OBJECT_BASE, OBJECT_BASE + 0x1000L,
                        List.of(definition));
        return new Fixture(population, new ClassicWorldGroundItems(),
                home[0] + 1, home[1]);
    }

    private static int[] openCenter() {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(MAP_ID);
        for (int x = map.startX() + 2; x < map.endX() - 2; x++) {
            for (int y = map.startY() + 2; y < map.endY() - 2; y++) {
                if (!ClassicWorldNpcCatalog.occupies(MAP_ID, x, y)
                        && !ClassicWorldNpcCatalog.occupies(MAP_ID, x + 1, y)
                        && map.canStep(x, y, x + 1, y)) {
                    return new int[]{x, y};
                }
            }
        }
        throw new AssertionError("missing item-use fixture cell");
    }

    private record Fixture(
            ClassicWorldMonsterPopulation population,
            ClassicWorldGroundItems ground, int itemX, int itemY) {}
}
