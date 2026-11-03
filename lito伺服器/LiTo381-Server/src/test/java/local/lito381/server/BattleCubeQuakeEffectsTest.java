package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattleCubeQuakeEffectsTest {
    private static long ms(long value) {
        return TimeUnit.MILLISECONDS.toNanos(value);
    }

    private static BattleCubeQuakeEffects.BindProtection unprotected() {
        return (target, kind, now) -> false;
    }

    @Test void placementUsesCasterTileAndRejectsSameCubeWithinThreeTiles() {
        BattleCubeQuakeEffects effects = new BattleCubeQuakeEffects();
        BattleCubeQuakeEffects.Placement first = effects.place(10, 100, 200, 0);
        assertTrue(first.accepted());
        assertEquals(100, first.cube().x());
        assertEquals(200, first.cube().y());
        assertFalse(effects.place(11, 103, 203, 1).accepted());
        assertTrue(effects.place(11, 104, 204, 1).accepted());
        assertEquals(2, effects.active(1).size());
    }

    @Test void allyGetsThirtyEarthForEightSecondsAndReappliesWhileCubeLives() {
        BattleCubeQuakeEffects effects = new BattleCubeQuakeEffects();
        effects.place(10, 100, 200, 0);
        BattleCubeQuakeEffects.Actor ally = new BattleCubeQuakeEffects.Actor(
                20, 101, 201, true, BattleCubeQuakeEffects.ActorKind.PLAYER);

        List<BattleCubeQuakeEffects.Event> start = effects.advance(
                0, List.of(ally), (caster, target) ->
                        BattleCubeQuakeEffects.Relation.ALLY, unprotected());
        assertEquals(List.of(BattleCubeQuakeEffects.EventKind.ALLY_APPLIED),
                start.stream().map(BattleCubeQuakeEffects.Event::kind).toList());
        assertEquals(30, effects.earthResistance(20, ms(7_999)));

        List<BattleCubeQuakeEffects.Event> expiry = effects.advance(
                ms(8_000), List.of(ally), (caster, target) ->
                        BattleCubeQuakeEffects.Relation.ALLY, unprotected());
        assertEquals(List.of(
                        BattleCubeQuakeEffects.EventKind.ALLY_EXPIRED,
                        BattleCubeQuakeEffects.EventKind.ALLY_APPLIED),
                expiry.stream().map(BattleCubeQuakeEffects.Event::kind).toList());
        assertEquals(30, effects.earthResistance(20, ms(8_000)));
    }

    @Test void enemyBindStartsAtFourthCallbackAndExpiresAfterOneSecond() {
        BattleCubeQuakeEffects effects = new BattleCubeQuakeEffects();
        effects.place(10, 100, 200, 0);
        BattleCubeQuakeEffects.Actor enemy = new BattleCubeQuakeEffects.Actor(
                30, 101, 201, true, BattleCubeQuakeEffects.ActorKind.PLAYER);
        effects.advance(0, List.of(enemy), (caster, target) ->
                BattleCubeQuakeEffects.Relation.ENEMY, unprotected());

        assertTrue(effects.advance(ms(3_899), List.of(enemy),
                (caster, target) -> BattleCubeQuakeEffects.Relation.ENEMY,
                unprotected()).stream().noneMatch(event -> event.kind()
                        == BattleCubeQuakeEffects.EventKind.ENEMY_BIND_STARTED));
        assertEquals(List.of(BattleCubeQuakeEffects.EventKind.ENEMY_BIND_STARTED),
                effects.advance(ms(3_900), List.of(enemy),
                        (caster, target) -> BattleCubeQuakeEffects.Relation.ENEMY,
                        unprotected()).stream().map(
                                BattleCubeQuakeEffects.Event::kind).toList());
        assertTrue(effects.movementStopped(30, ms(4_899)));
        assertEquals(List.of(BattleCubeQuakeEffects.EventKind.ENEMY_BIND_EXPIRED),
                effects.advance(ms(4_900), List.of(),
                        (caster, target) -> BattleCubeQuakeEffects.Relation.ENEMY,
                        unprotected()).stream().map(
                                BattleCubeQuakeEffects.Event::kind).toList());
        assertFalse(effects.movementStopped(30, ms(4_900)));
    }

    @Test void protectedEnemySkipsBindAtCallbackWithoutCreatingMovementLock() {
        BattleCubeQuakeEffects effects = new BattleCubeQuakeEffects();
        effects.place(10, 100, 200, 0);
        BattleCubeQuakeEffects.Actor enemy = new BattleCubeQuakeEffects.Actor(
                30, 101, 201, true, BattleCubeQuakeEffects.ActorKind.MONSTER);
        effects.advance(0, List.of(enemy), (caster, target) ->
                BattleCubeQuakeEffects.Relation.ENEMY, unprotected());

        List<BattleCubeQuakeEffects.Event> events = effects.advance(
                ms(3_900), List.of(),
                (caster, target) -> BattleCubeQuakeEffects.Relation.ENEMY,
                (target, kind, now) -> true);
        assertTrue(events.isEmpty());
        assertFalse(effects.movementStopped(30, ms(3_900)));
    }

    @Test void finalBindOutlivesEightSecondEnemyLease() {
        BattleCubeQuakeEffects effects = new BattleCubeQuakeEffects();
        effects.place(10, 100, 200, 0);
        BattleCubeQuakeEffects.Actor enemy = new BattleCubeQuakeEffects.Actor(
                30, 101, 201, true, BattleCubeQuakeEffects.ActorKind.MONSTER);
        effects.advance(0, List.of(enemy), (caster, target) ->
                BattleCubeQuakeEffects.Relation.ENEMY, unprotected());
        effects.advance(ms(3_900), List.of(),
                (caster, target) -> BattleCubeQuakeEffects.Relation.ENEMY,
                unprotected());
        effects.advance(ms(4_900), List.of(),
                (caster, target) -> BattleCubeQuakeEffects.Relation.ENEMY,
                unprotected());
        effects.advance(ms(7_900), List.of(),
                (caster, target) -> BattleCubeQuakeEffects.Relation.ENEMY,
                unprotected());

        List<BattleCubeQuakeEffects.Event> leaseExpiry = effects.advance(
                ms(8_000), List.of(),
                (caster, target) -> BattleCubeQuakeEffects.Relation.ENEMY,
                unprotected());
        assertEquals(List.of(BattleCubeQuakeEffects.EventKind.ENEMY_EXPIRED),
                leaseExpiry.stream().map(BattleCubeQuakeEffects.Event::kind).toList());
        assertTrue(effects.movementStopped(30, ms(8_899)));
        assertEquals(1, effects.bindCount(ms(8_899)));
        assertEquals(List.of(BattleCubeQuakeEffects.EventKind.ENEMY_BIND_EXPIRED),
                effects.advance(ms(8_900), List.of(),
                        (caster, target) -> BattleCubeQuakeEffects.Relation.ENEMY,
                        unprotected()).stream().map(
                                BattleCubeQuakeEffects.Event::kind).toList());
        assertEquals(0, effects.bindCount(ms(8_900)));
    }

    @Test void cubeExpiresAtTwentySecondsButAppliedStatusUsesItsOwnClock() {
        BattleCubeQuakeEffects effects = new BattleCubeQuakeEffects();
        effects.place(10, 100, 200, 0);
        BattleCubeQuakeEffects.Actor ally = new BattleCubeQuakeEffects.Actor(
                20, 101, 201, true, BattleCubeQuakeEffects.ActorKind.PLAYER);
        effects.advance(TimeUnit.SECONDS.toNanos(19), List.of(ally),
                (caster, target) -> BattleCubeQuakeEffects.Relation.ALLY,
                unprotected());

        List<BattleCubeQuakeEffects.Event> cubeExpiry = effects.advance(
                TimeUnit.SECONDS.toNanos(20), List.of(),
                (caster, target) -> BattleCubeQuakeEffects.Relation.ENEMY,
                unprotected());
        assertTrue(cubeExpiry.stream().anyMatch(event -> event.kind()
                == BattleCubeQuakeEffects.EventKind.CUBE_EXPIRED));
        assertEquals(30, effects.earthResistance(
                20, TimeUnit.SECONDS.toNanos(20)));
    }
}
