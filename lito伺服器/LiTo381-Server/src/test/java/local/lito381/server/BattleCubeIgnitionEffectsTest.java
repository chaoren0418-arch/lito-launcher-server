package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattleCubeIgnitionEffectsTest {
    private static long ms(long value) {
        return TimeUnit.MILLISECONDS.toNanos(value);
    }

    @Test void placementUsesCasterTileAndRejectsSameCubeWithinThreeTiles() {
        BattleCubeIgnitionEffects effects = new BattleCubeIgnitionEffects();
        BattleCubeIgnitionEffects.Placement first = effects.place(10, 100, 200, 0);
        assertTrue(first.accepted());
        assertEquals(100, first.cube().x());
        assertEquals(200, first.cube().y());
        assertFalse(effects.place(11, 103, 203, 1).accepted());
        assertTrue(effects.place(11, 104, 204, 1).accepted());
        assertEquals(2, effects.active(1).size());
    }

    @Test void allyGetsThirtyFireForEightSecondsAndReappliesWhileCubeLives() {
        BattleCubeIgnitionEffects effects = new BattleCubeIgnitionEffects();
        effects.place(10, 100, 200, 0);
        BattleCubeIgnitionEffects.Actor ally = new BattleCubeIgnitionEffects.Actor(
                20, 101, 201, true, BattleCubeIgnitionEffects.ActorKind.PLAYER);

        List<BattleCubeIgnitionEffects.Event> start = effects.advance(
                0, List.of(ally), (caster, target) ->
                        BattleCubeIgnitionEffects.Relation.ALLY);
        assertEquals(List.of(BattleCubeIgnitionEffects.EventKind.ALLY_APPLIED),
                start.stream().map(BattleCubeIgnitionEffects.Event::kind).toList());
        assertEquals(30, effects.fireResistance(20, ms(7_999)));

        List<BattleCubeIgnitionEffects.Event> expiry = effects.advance(
                ms(8_000), List.of(ally), (caster, target) ->
                        BattleCubeIgnitionEffects.Relation.ALLY);
        assertEquals(List.of(
                        BattleCubeIgnitionEffects.EventKind.ALLY_EXPIRED,
                        BattleCubeIgnitionEffects.EventKind.ALLY_APPLIED),
                expiry.stream().map(BattleCubeIgnitionEffects.Event::kind).toList());
        assertEquals(30, effects.fireResistance(20, ms(8_000)));
    }

    @Test void enemyDamageFiresOnFourthAndEighthScheduledCallbacks() {
        BattleCubeIgnitionEffects effects = new BattleCubeIgnitionEffects();
        effects.place(10, 100, 200, 0);
        BattleCubeIgnitionEffects.Actor enemy = new BattleCubeIgnitionEffects.Actor(
                30, 101, 201, true, BattleCubeIgnitionEffects.ActorKind.MONSTER);
        effects.advance(0, List.of(enemy), (caster, target) ->
                BattleCubeIgnitionEffects.Relation.ENEMY);

        assertTrue(effects.advance(ms(3_899), List.of(enemy),
                (caster, target) -> BattleCubeIgnitionEffects.Relation.ENEMY)
                .stream().noneMatch(event ->
                        event.kind() == BattleCubeIgnitionEffects.EventKind.ENEMY_DAMAGE));
        assertEquals(1, effects.advance(ms(3_900), List.of(enemy),
                (caster, target) -> BattleCubeIgnitionEffects.Relation.ENEMY)
                .stream().filter(event ->
                        event.kind() == BattleCubeIgnitionEffects.EventKind.ENEMY_DAMAGE).count());
        assertEquals(1, effects.advance(ms(7_900), List.of(enemy),
                (caster, target) -> BattleCubeIgnitionEffects.Relation.ENEMY)
                .stream().filter(event ->
                        event.kind() == BattleCubeIgnitionEffects.EventKind.ENEMY_DAMAGE).count());
        List<BattleCubeIgnitionEffects.Event> expiry = effects.advance(
                ms(8_000), List.of(), (caster, target) ->
                        BattleCubeIgnitionEffects.Relation.ENEMY);
        assertEquals(List.of(BattleCubeIgnitionEffects.EventKind.ENEMY_EXPIRED),
                expiry.stream().map(BattleCubeIgnitionEffects.Event::kind).toList());
    }

    @Test void statusContinuesAfterActorLeavesAndCubeExpiresAtTwentySeconds() {
        BattleCubeIgnitionEffects effects = new BattleCubeIgnitionEffects();
        effects.place(10, 100, 200, 0);
        BattleCubeIgnitionEffects.Actor enemy = new BattleCubeIgnitionEffects.Actor(
                30, 101, 201, true, BattleCubeIgnitionEffects.ActorKind.PLAYER);
        effects.advance(0, List.of(enemy), (caster, target) ->
                BattleCubeIgnitionEffects.Relation.ENEMY);

        assertEquals(1, effects.advance(ms(3_900), List.of(),
                (caster, target) -> BattleCubeIgnitionEffects.Relation.ENEMY)
                .stream().filter(event ->
                        event.kind() == BattleCubeIgnitionEffects.EventKind.ENEMY_DAMAGE).count());
        List<BattleCubeIgnitionEffects.Event> cubeExpiry = effects.advance(
                TimeUnit.SECONDS.toNanos(20), List.of(),
                (caster, target) -> BattleCubeIgnitionEffects.Relation.ENEMY);
        assertTrue(cubeExpiry.stream().anyMatch(event ->
                event.kind() == BattleCubeIgnitionEffects.EventKind.CUBE_EXPIRED));
        assertTrue(effects.active(TimeUnit.SECONDS.toNanos(20)).isEmpty());
    }

    @Test void retainedAllyAndEnemyStatusIdsCanCoexistOnOneTarget() {
        BattleCubeIgnitionEffects effects = new BattleCubeIgnitionEffects();
        assertTrue(effects.place(10, 100, 200, 0).accepted());
        assertTrue(effects.place(11, 104, 200, 0).accepted());
        BattleCubeIgnitionEffects.Actor target = new BattleCubeIgnitionEffects.Actor(
                20, 102, 200, true, BattleCubeIgnitionEffects.ActorKind.PLAYER);

        List<BattleCubeIgnitionEffects.Event> applied = effects.advance(
                0, List.of(target), (caster, ignored) -> caster == 10
                        ? BattleCubeIgnitionEffects.Relation.ALLY
                        : BattleCubeIgnitionEffects.Relation.ENEMY);

        assertEquals(List.of(
                        BattleCubeIgnitionEffects.EventKind.ALLY_APPLIED,
                        BattleCubeIgnitionEffects.EventKind.ENEMY_APPLIED),
                applied.stream().map(BattleCubeIgnitionEffects.Event::kind).toList());
        assertEquals(2, effects.statusCount(0));
        assertEquals(30, effects.fireResistance(20, 0));
        assertEquals(1, effects.advance(ms(3_900), List.of(),
                (caster, ignored) -> BattleCubeIgnitionEffects.Relation.IGNORE)
                .stream().filter(event -> event.kind()
                        == BattleCubeIgnitionEffects.EventKind.ENEMY_DAMAGE).count());
    }
}
