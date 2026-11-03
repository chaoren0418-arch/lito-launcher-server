package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattleCubeHarmonyEffectsTest {
    private static final BattleCubeHarmonyEffects.Alliance ENEMIES =
            (caster, target) -> BattleCubeHarmonyEffects.Relation.ENEMY;

    @Test void placementPinsNativeObjectLifetimeAndSameCubeRadius() {
        BattleCubeHarmonyEffects effects = new BattleCubeHarmonyEffects();
        BattleCubeHarmonyEffects.Placement first = effects.place(
                10, 100, 200, 0);
        assertTrue(first.accepted());
        assertEquals(100, first.cube().x());
        assertEquals(200, first.cube().y());
        assertEquals(seconds(20), first.cube().expiresAtNanos());
        assertFalse(effects.place(11, 103, 203, 0).accepted());
        assertTrue(effects.place(11, 104, 200, 0).accepted());
    }

    @Test void statusUsesNative900ClockMpFourAndEightDamageFive() {
        BattleCubeHarmonyEffects effects = new BattleCubeHarmonyEffects();
        effects.place(10, 100, 200, 0);
        BattleCubeHarmonyEffects.Actor target = new BattleCubeHarmonyEffects.Actor(
                20, 101, 201, true,
                BattleCubeHarmonyEffects.ActorKind.PLAYER);

        assertEquals(List.of(BattleCubeHarmonyEffects.EventKind.STATUS_APPLIED),
                kinds(effects.advance(0, List.of(target), ENEMIES)));
        assertTrue(effects.hasStatus(20, 0));
        assertEquals(List.of(), kinds(effects.advance(
                millis(3_899), List.of(target), ENEMIES)));
        assertEquals(List.of(BattleCubeHarmonyEffects.EventKind.MP_RECOVERY),
                kinds(effects.advance(millis(3_900), List.of(target), ENEMIES)));
        assertEquals(List.of(BattleCubeHarmonyEffects.EventKind.HP_DAMAGE),
                kinds(effects.advance(millis(4_900), List.of(target), ENEMIES)));
        assertEquals(List.of(BattleCubeHarmonyEffects.EventKind.MP_RECOVERY),
                kinds(effects.advance(millis(7_900), List.of(target), ENEMIES)));
    }

    @Test void leavingDoesNotCancelLeaseAndExpiryCanReapplyInActiveCube() {
        BattleCubeHarmonyEffects effects = new BattleCubeHarmonyEffects();
        effects.place(10, 100, 200, 0);
        BattleCubeHarmonyEffects.Actor inside = new BattleCubeHarmonyEffects.Actor(
                20, 101, 201, true,
                BattleCubeHarmonyEffects.ActorKind.MONSTER);
        BattleCubeHarmonyEffects.Actor outside = new BattleCubeHarmonyEffects.Actor(
                20, 110, 210, true,
                BattleCubeHarmonyEffects.ActorKind.MONSTER);
        effects.advance(0, List.of(inside), ENEMIES);

        assertEquals(List.of(BattleCubeHarmonyEffects.EventKind.MP_RECOVERY),
                kinds(effects.advance(millis(3_900),
                        List.of(outside), ENEMIES)));
        assertEquals(List.of(BattleCubeHarmonyEffects.EventKind.HP_DAMAGE),
                kinds(effects.advance(millis(4_900),
                        List.of(outside), ENEMIES)));
        assertEquals(List.of(BattleCubeHarmonyEffects.EventKind.MP_RECOVERY),
                kinds(effects.advance(millis(7_900),
                        List.of(outside), ENEMIES)));
        assertEquals(List.of(BattleCubeHarmonyEffects.EventKind.STATUS_EXPIRED,
                        BattleCubeHarmonyEffects.EventKind.STATUS_APPLIED),
                kinds(effects.advance(seconds(8), List.of(inside), ENEMIES)));
        assertEquals(1, effects.statusCount(seconds(8)));
    }

    @Test void allyAndEnemyShareOneNonStackingStatusWhileIgnoreIsExcluded() {
        BattleCubeHarmonyEffects effects = new BattleCubeHarmonyEffects();
        effects.place(10, 100, 200, 0);
        effects.place(11, 104, 200, 0);
        BattleCubeHarmonyEffects.Actor shared = new BattleCubeHarmonyEffects.Actor(
                30, 102, 200, true, BattleCubeHarmonyEffects.ActorKind.BOT);
        BattleCubeHarmonyEffects.Actor ignored = new BattleCubeHarmonyEffects.Actor(
                40, 100, 201, true, BattleCubeHarmonyEffects.ActorKind.PLAYER);
        BattleCubeHarmonyEffects.Alliance alliance = (caster, target) ->
                target == 40 ? BattleCubeHarmonyEffects.Relation.IGNORE
                        : caster == 10 ? BattleCubeHarmonyEffects.Relation.ALLY
                        : BattleCubeHarmonyEffects.Relation.ENEMY;

        assertEquals(List.of(BattleCubeHarmonyEffects.EventKind.STATUS_APPLIED),
                kinds(effects.advance(0, List.of(shared, ignored), alliance)));
        assertEquals(1, effects.statusCount(0));
        assertTrue(effects.hasStatus(30, 0));
        assertFalse(effects.hasStatus(40, 0));
    }

    @Test void cubeExpiryLeavesIndependentStatusLeaseAlive() {
        BattleCubeHarmonyEffects effects = new BattleCubeHarmonyEffects();
        BattleCubeHarmonyEffects.Cube cube = effects.place(
                10, 100, 200, 0).cube();
        BattleCubeHarmonyEffects.Actor target = new BattleCubeHarmonyEffects.Actor(
                30, 101, 201, true,
                BattleCubeHarmonyEffects.ActorKind.MONSTER);
        effects.advance(seconds(19), List.of(target), ENEMIES);

        List<BattleCubeHarmonyEffects.Event> expiry = effects.advance(
                seconds(20), List.of(), ENEMIES);
        assertTrue(expiry.stream().anyMatch(event ->
                event.kind() == BattleCubeHarmonyEffects.EventKind.CUBE_EXPIRED
                        && event.cube().equals(cube)));
        assertTrue(effects.hasStatus(30, seconds(20)));
    }

    private static List<BattleCubeHarmonyEffects.EventKind> kinds(
            List<BattleCubeHarmonyEffects.Event> events) {
        return events.stream().map(BattleCubeHarmonyEffects.Event::kind).toList();
    }

    private static long millis(long value) {
        return TimeUnit.MILLISECONDS.toNanos(value);
    }

    private static long seconds(long value) {
        return TimeUnit.SECONDS.toNanos(value);
    }
}
