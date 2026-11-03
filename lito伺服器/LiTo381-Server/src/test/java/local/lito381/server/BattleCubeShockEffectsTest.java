package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattleCubeShockEffectsTest {
    private static final BattleCubeShockEffects.Alliance ENEMIES =
            (caster, target) -> BattleCubeShockEffects.Relation.ENEMY;

    @Test void placementRejectsSameCubeInsideRadiusAndAcceptsDistanceFour() {
        BattleCubeShockEffects effects = new BattleCubeShockEffects();
        BattleCubeShockEffects.Placement first = effects.place(10, 100, 200, 0);
        assertTrue(first.accepted());
        assertFalse(effects.place(11, 103, 203, 1).accepted());
        assertTrue(effects.place(11, 104, 204, 1).accepted());
        assertEquals(2, effects.active(1).size());
    }

    @Test void allyReceivesOneEightSecondWindLease() {
        BattleCubeShockEffects effects = new BattleCubeShockEffects();
        effects.place(10, 100, 200, 0);
        BattleCubeShockEffects.Actor ally = new BattleCubeShockEffects.Actor(
                20, 101, 201, true, BattleCubeShockEffects.ActorKind.PLAYER);
        List<BattleCubeShockEffects.Event> start = effects.advance(
                0, List.of(ally), (caster, target) ->
                        BattleCubeShockEffects.Relation.ALLY);
        assertEquals(List.of(BattleCubeShockEffects.EventKind.ALLY_APPLIED),
                kinds(start));
        assertEquals(30, effects.windResistance(20, 0));
        assertEquals(List.of(BattleCubeShockEffects.EventKind.ALLY_EXPIRED),
                kinds(effects.advance(seconds(8), List.of(
                        new BattleCubeShockEffects.Actor(20, 110, 210, true,
                                BattleCubeShockEffects.ActorKind.PLAYER)),
                        (caster, target) -> BattleCubeShockEffects.Relation.ALLY)));
        assertEquals(0, effects.windResistance(20, seconds(8)));
    }

    @Test void enemyClockStartsAtNineHundredAndRefreshesMinusTenForFourSeconds() {
        BattleCubeShockEffects effects = new BattleCubeShockEffects();
        effects.place(10, 100, 200, 0);
        BattleCubeShockEffects.Actor enemy = new BattleCubeShockEffects.Actor(
                30, 101, 201, true, BattleCubeShockEffects.ActorKind.MONSTER);
        assertEquals(List.of(BattleCubeShockEffects.EventKind.ENEMY_APPLIED),
                kinds(effects.advance(0, List.of(enemy), ENEMIES)));
        assertEquals(0, effects.magicResistanceModifier(30, millis(899)));
        assertEquals(List.of(BattleCubeShockEffects.EventKind.ENEMY_MR_STARTED),
                kinds(effects.advance(millis(900), List.of(enemy), ENEMIES)));
        assertEquals(-10, effects.magicResistanceModifier(30, millis(900)));
        assertEquals(List.of(), kinds(effects.advance(
                millis(7_900), List.of(enemy), ENEMIES)));
        assertEquals(-10, effects.magicResistanceModifier(30, millis(11_899)));

        BattleCubeShockEffects.Actor outside = new BattleCubeShockEffects.Actor(
                30, 110, 210, true, BattleCubeShockEffects.ActorKind.MONSTER);
        assertEquals(List.of(BattleCubeShockEffects.EventKind.ENEMY_EXPIRED),
                kinds(effects.advance(seconds(8), List.of(outside), ENEMIES)));
        assertEquals(List.of(BattleCubeShockEffects.EventKind.ENEMY_MR_EXPIRED),
                kinds(effects.advance(millis(11_900), List.of(outside), ENEMIES)));
        assertEquals(0, effects.magicResistanceModifier(30, millis(11_900)));
    }

    @Test void cubeExpiryDoesNotPrematurelyDeleteIndependentMrLease() {
        BattleCubeShockEffects effects = new BattleCubeShockEffects();
        BattleCubeShockEffects.Cube cube = effects.place(
                10, 100, 200, 0).cube();
        BattleCubeShockEffects.Actor enemy = new BattleCubeShockEffects.Actor(
                30, 101, 201, true, BattleCubeShockEffects.ActorKind.BOT);
        effects.advance(seconds(19), List.of(enemy), ENEMIES);
        effects.advance(millis(19_900), List.of(enemy), ENEMIES);
        assertEquals(-10, effects.magicResistanceModifier(30, seconds(20)));
        List<BattleCubeShockEffects.Event> expiry = effects.advance(
                seconds(20), List.of(new BattleCubeShockEffects.Actor(
                        30, 110, 210, true,
                        BattleCubeShockEffects.ActorKind.BOT)), ENEMIES);
        assertTrue(expiry.stream().anyMatch(event ->
                event.kind() == BattleCubeShockEffects.EventKind.CUBE_EXPIRED
                && event.cube().equals(cube)));
        assertEquals(-10, effects.magicResistanceModifier(30, seconds(20)));
    }

    private static List<BattleCubeShockEffects.EventKind> kinds(
            List<BattleCubeShockEffects.Event> events) {
        return events.stream().map(BattleCubeShockEffects.Event::kind).toList();
    }

    private static long millis(long value) {
        return TimeUnit.MILLISECONDS.toNanos(value);
    }

    private static long seconds(long value) {
        return TimeUnit.SECONDS.toNanos(value);
    }
}
