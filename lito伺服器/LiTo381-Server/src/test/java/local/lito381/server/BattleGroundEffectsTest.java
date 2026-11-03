package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class BattleGroundEffectsTest {

    @Test
    void npcPatternProducesEightFixedIntervalPulsesWithoutCatalogLookup() {
        BattleGroundEffects effects = new BattleGroundEffects();
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(540);
        ArenaMapCatalog.Point point = new ArenaMapCatalog.Point(32737, 32829);
        assertTrue(effects.castPattern(30004, 9001, List.of(point), 0,
                10_000, 1_200, arena, 0).accepted());
        BattleGroundEffects.Actor target = new BattleGroundEffects.Actor(
                8001, point.x(), point.y(), true, false);
        int pulses = 0;
        for (int index = 1; index <= 8; index++) {
            List<BattleGroundEffects.Pulse> due = effects.advance(
                    index * 1_200_000_000L, List.of(target));
            assertEquals(1, due.size());
            assertEquals(30004, due.get(0).skillId());
            assertEquals(9001, due.get(0).casterObjectId());
            pulses += due.size();
        }
        assertEquals(8, pulses);
        assertTrue(effects.advance(10_000_000_000L, List.of(target)).isEmpty());
    }
    private static final ArenaMapCatalog.Arena ARENA =
            ArenaMapCatalog.rectangularForTest(509, 90, 120, 90, 120);

    @Test void fireWallBuildsEightServerLeasesAndPulsesEvery1650Millis() {
        BattleGroundEffects effects = new BattleGroundEffects();
        BattleSpellCatalog.GroundSpell fireWall =
                BattleSpellCatalog.groundSpell(BattleSpellCatalog.FIRE_WALL);
        long start = 1_000_000_000L;

        BattleGroundEffects.CastResult cast = effects.cast(
                fireWall, 1, 100, 100, 108, 100, ARENA, start);
        assertTrue(cast.accepted());
        assertEquals(8, cast.points().size());
        assertEquals(new ArenaMapCatalog.Point(101, 100), cast.points().get(0));
        assertEquals(new ArenaMapCatalog.Point(108, 100), cast.points().get(7));
        assertEquals(8, effects.activeCount());

        List<BattleGroundEffects.Actor> actors = List.of(
                new BattleGroundEffects.Actor(1, 100, 100, true, false),
                new BattleGroundEffects.Actor(3, 103, 100, true, false),
                new BattleGroundEffects.Actor(5, 103, 100, false, false));
        assertTrue(effects.advance(start + 1_649_999_999L, actors).isEmpty());
        List<BattleGroundEffects.Pulse> pulses =
                effects.advance(start + 1_650_000_000L, actors);
        // Radius one intersects the segments at x=102,103,104. The caster
        // and dead actors are never emitted as damage candidates.
        assertEquals(3, pulses.size());
        assertTrue(pulses.stream().allMatch(pulse -> pulse.targetObjectId() == 3));

        effects.advance(start + 10_000_000_000L, actors);
        assertEquals(0, effects.activeCount());
    }

    @Test void duplicateFireWallCellsAreRejectedAndLifeStreamHasNoGuessedPulse() {
        BattleGroundEffects effects = new BattleGroundEffects();
        long start = 2_000_000_000L;
        BattleSpellCatalog.GroundSpell fireWall =
                BattleSpellCatalog.groundSpell(BattleSpellCatalog.FIRE_WALL);
        assertTrue(effects.cast(fireWall, 1, 100, 100, 108, 100, ARENA, start).accepted());
        assertFalse(effects.cast(fireWall, 2, 100, 100, 108, 100, ARENA, start).accepted());

        BattleSpellCatalog.GroundSpell lifeStream =
                BattleSpellCatalog.groundSpell(BattleSpellCatalog.LIFE_STREAM);
        assertTrue(effects.cast(lifeStream, 1, 100, 100, 104, 104, ARENA, start).accepted());
        assertTrue(effects.contains(BattleSpellCatalog.LIFE_STREAM,
                104, 104, start + 1));
        assertTrue(effects.contains(BattleSpellCatalog.LIFE_STREAM,
                101, 101, start + 1));
        assertFalse(effects.contains(BattleSpellCatalog.LIFE_STREAM,
                100, 100, start + 1));
        assertTrue(effects.advance(start + 319_000_000_000L,
                List.of(new BattleGroundEffects.Actor(3, 104, 104, true, false)))
                .stream().noneMatch(pulse -> pulse.skillId() == BattleSpellCatalog.LIFE_STREAM));
        effects.advance(start + 320_000_000_000L, List.of());
        assertFalse(effects.contains(BattleSpellCatalog.LIFE_STREAM,
                104, 104, start + 320_000_000_000L));
        assertEquals(0, effects.activeCount());
        effects.clear();
        assertEquals(0, effects.activeCount());
    }

    @Test void fireWallStopsAtNativeLayer3ClosedEdge() {
        ArenaMapCatalog.Arena nativeArena = ArenaMapCatalog.forMap(573);
        BattleGroundEffects effects = new BattleGroundEffects();
        BattleSpellCatalog.GroundSpell fireWall =
                BattleSpellCatalog.groundSpell(BattleSpellCatalog.FIRE_WALL);

        // Both cells on this real map-573 south-east edge are valid standing
        // floor, but native Layer3 closes the boundary between them.
        assertTrue(nativeArena.nativeStandingFloor(32758, 32794));
        assertTrue(nativeArena.nativeStandingFloor(32759, 32795));
        assertFalse(nativeArena.nativeCanStep(32758, 32794, 32759, 32795));

        BattleGroundEffects.CastResult cast = effects.cast(
                fireWall, 1, 32758, 32794, 32759, 32795,
                nativeArena, 3_000_000_000L);
        assertFalse(cast.accepted());
        assertEquals(0, effects.activeCount());
    }

    @Test void shockStunEffectRefreshReplacesOneTargetAndExpiresAuthoritatively() {
        BattleShockStunEffects effects = new BattleShockStunEffects();
        long start = 4_000_000_000L;
        BattleShockStunEffects.Placement first = effects.place(
                16, 32749, 32813, start, 2);
        assertEquals(16, first.placed().targetObjectId());
        assertEquals(4183, BattleShockStunEffects.GFX);
        assertEquals(81162, BattleShockStunEffects.NPC_ID);
        assertEquals(List.of(first.placed()), effects.active(start + 1));

        BattleShockStunEffects.Placement refresh = effects.place(
                16, 32749, 32813, start + 1_000_000_000L, 4);
        assertEquals(first.placed(), refresh.previous());
        assertFalse(first.placed().objectId() == refresh.placed().objectId());
        assertEquals(List.of(refresh.placed()), effects.active(start + 2_000_000_000L));
        assertTrue(effects.expire(start + 4_999_999_999L).isEmpty());
        assertEquals(List.of(refresh.placed()),
                effects.expire(start + 5_000_000_000L));
        assertTrue(effects.active(start + 5_000_000_000L).isEmpty());
    }

    @Test void boneBreakEffectIsFixedTwoSecondsAndNeverRefreshes() {
        BattleBoneBreakEffects effects = new BattleBoneBreakEffects();
        long start = 6_000_000_000L;
        BattleBoneBreakEffects.Effect first = effects.placeIfAbsent(
                32, 32749, 32813, start, 2);
        assertEquals(32, first.targetObjectId());
        assertEquals(6_552, BattleBoneBreakEffects.GFX);
        assertEquals(86_123, BattleBoneBreakEffects.NPC_ID);
        assertEquals(start + 2_000_000_000L, first.expiresAtNanos());
        assertEquals(List.of(first), effects.active(start + 1));
        assertEquals(null, effects.placeIfAbsent(
                32, 32750, 32814, start + 1_000_000_000L, 2));
        assertEquals(List.of(first), effects.active(start + 1_999_999_999L));
        assertEquals(List.of(first), effects.expire(start + 2_000_000_000L));
        assertTrue(effects.active(start + 2_000_000_000L).isEmpty());

        BattleBoneBreakEffects.Effect second = effects.placeIfAbsent(
                32, 32750, 32814, start + 3_000_000_000L, 2);
        assertFalse(first.objectId() == second.objectId());
        assertEquals(second, effects.remove(32));
        assertTrue(effects.active(start + 3_000_000_001L).isEmpty());
    }
}
