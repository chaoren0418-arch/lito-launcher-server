package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class NativeWorldEarthBindLifecycleTest {
    @Test
    void controlledWorldActorStopsUntilCancellationOrExactExpiry() {
        BattleCompanion companion = new BattleCompanion(0x6000_1570L);
        companion.resetForBattle(100, 120);
        assertTrue(companion.selectTarget(0x6500_0001L));
        long now = 1_000_000_000L;

        assertTrue(companion.activateEarthBind(now, 2));
        assertTrue(companion.earthBindActive(now + 1));
        assertFalse(companion.ready(now + 1));
        BattleCompanion.MagicCancellation cancelled =
                companion.cancelMagicEffects();
        assertTrue(cancelled.earthBind());
        assertFalse(companion.earthBindActive(now + 2));
        assertTrue(companion.ready(now + 2));

        assertTrue(companion.activateEarthBind(now + 3, 2));
        assertFalse(companion.expireEarthBind(now + 2_000_000_002L));
        assertTrue(companion.expireEarthBind(now + 2_000_000_003L));
        assertTrue(companion.ready(now + 2_000_000_003L));
    }

    @Test
    void ordinaryWorldMonsterRejectsDamageAndRestoresAfterLeaseEnds() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot monster = population.nearest(
                4, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45009);
        long now = System.nanoTime();
        int hp = monster.currentHp();

        assertTrue(population.earthBind(4, monster.objectId(), now, 2));
        assertNull(population.damage(4, monster.objectId(), 99));
        assertEquals(hp, population.actor(4, monster.objectId()).currentHp());
        ClassicWorldMonsterPopulation.MagicCancellation cancelled =
                population.cancelMagicEffects(4, monster.objectId());
        assertTrue(cancelled.earthBind());
        assertNotNull(population.damage(4, monster.objectId(), 1));

        assertTrue(population.earthBind(4, monster.objectId(), now + 3, 2));
        assertTrue(population.expireEarthBinds(
                4, now + 2_000_000_003L).size() == 1);
        assertFalse(population.earthBindActive(
                4, monster.objectId(), now + 2_000_000_003L));
        assertNotNull(population.damage(4, monster.objectId(), 1));
    }
}
