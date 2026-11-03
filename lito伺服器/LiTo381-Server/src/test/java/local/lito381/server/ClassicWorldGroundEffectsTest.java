package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class ClassicWorldGroundEffectsTest {
    private static final long SECOND = 1_000_000_000L;
    private static final ClassicWorldMapCatalog.ClassicMap MAP =
            ClassicWorldMapCatalog.forMap(4);

    @Test
    void fireWallBuildsEightNativeWorldCellsAndClassifiesPlayerAndMonsterPulses() {
        Corridor corridor = corridors(1).get(0);
        ClassicWorldGroundEffects effects = new ClassicWorldGroundEffects();
        BattleSpellCatalog.GroundSpell fireWall =
                BattleSpellCatalog.groundSpell(BattleSpellCatalog.FIRE_WALL);
        long start = SECOND;

        ClassicWorldGroundEffects.CastResult cast = effects.cast(
                fireWall, 101, corridor.sourceX(), corridor.y(),
                corridor.targetX(), corridor.y(), MAP, start);
        assertTrue(cast.accepted());
        assertEquals(8, cast.points().size());
        assertEquals(new ClassicWorldGroundEffects.Point(
                corridor.sourceX() + 1, corridor.y()), cast.points().get(0));
        assertEquals(new ClassicWorldGroundEffects.Point(
                corridor.targetX(), corridor.y()), cast.points().get(7));

        List<ClassicWorldGroundEffects.Actor> actors = List.of(
                new ClassicWorldGroundEffects.Actor(
                        101, corridor.sourceX(), corridor.y(), true,
                        ClassicWorldGroundEffects.ActorKind.PLAYER),
                new ClassicWorldGroundEffects.Actor(
                        201, corridor.sourceX() + 3, corridor.y(), true,
                        ClassicWorldGroundEffects.ActorKind.PLAYER),
                new ClassicWorldGroundEffects.Actor(
                        301, corridor.sourceX() + 6, corridor.y(), true,
                        ClassicWorldGroundEffects.ActorKind.MONSTER),
                new ClassicWorldGroundEffects.Actor(
                        401, corridor.sourceX() + 4, corridor.y(), false,
                        ClassicWorldGroundEffects.ActorKind.MONSTER));
        assertTrue(effects.advance(
                start + 1_649_999_999L, actors).isEmpty());
        List<ClassicWorldGroundEffects.Pulse> pulses = effects.advance(
                start + 1_650_000_000L, actors);
        assertEquals(6, pulses.size());
        assertEquals(3, pulses.stream().filter(pulse ->
                pulse.targetObjectId() == 201
                        && pulse.targetKind()
                            == ClassicWorldGroundEffects.ActorKind.PLAYER).count());
        assertEquals(3, pulses.stream().filter(pulse ->
                pulse.targetObjectId() == 301
                        && pulse.targetKind()
                            == ClassicWorldGroundEffects.ActorKind.MONSTER).count());
        assertTrue(pulses.stream().noneMatch(pulse ->
                pulse.targetObjectId() == 101
                        || pulse.targetObjectId() == 401));

        assertEquals(8, effects.activeCount(start + 9_999_999_999L));
        assertEquals(0, effects.activeCount(start + 10 * SECOND));
    }

    @Test
    void fireWallCapsOneCasterAtTwentyFourNonOverlappingSegments() {
        List<Corridor> corridors = corridors(4);
        ClassicWorldGroundEffects effects = new ClassicWorldGroundEffects();
        BattleSpellCatalog.GroundSpell fireWall =
                BattleSpellCatalog.groundSpell(BattleSpellCatalog.FIRE_WALL);
        long start = 2 * SECOND;

        for (int index = 0; index < 3; index++) {
            Corridor corridor = corridors.get(index);
            assertTrue(effects.cast(fireWall, 101,
                    corridor.sourceX(), corridor.y(),
                    corridor.targetX(), corridor.y(), MAP, start).accepted());
        }
        assertEquals(24, effects.activeCount(start + 1));
        Corridor rejected = corridors.get(3);
        assertFalse(effects.cast(fireWall, 101,
                rejected.sourceX(), rejected.y(),
                rejected.targetX(), rejected.y(), MAP, start).accepted());
        assertEquals(24, effects.activeCount(start + 1));
    }

    @Test
    void lifeStreamUsesRadiusThreeForExactlyThreeHundredTwentySecondsWithoutPulse() {
        Corridor corridor = corridors(1).get(0);
        ClassicWorldGroundEffects effects = new ClassicWorldGroundEffects();
        BattleSpellCatalog.GroundSpell lifeStream =
                BattleSpellCatalog.groundSpell(BattleSpellCatalog.LIFE_STREAM);
        long start = 3 * SECOND;
        int x = corridor.sourceX();
        int y = corridor.y();

        assertTrue(effects.cast(lifeStream, 101, x, y, x, y, MAP, start)
                .accepted());
        assertTrue(effects.contains(
                BattleSpellCatalog.LIFE_STREAM, x, y, start + 1));
        assertTrue(effects.contains(
                BattleSpellCatalog.LIFE_STREAM, x + 3, y + 3, start + 1));
        assertFalse(effects.contains(
                BattleSpellCatalog.LIFE_STREAM, x + 4, y, start + 1));
        assertTrue(effects.advance(start + 319 * SECOND,
                List.of(new ClassicWorldGroundEffects.Actor(
                        201, x, y, true,
                        ClassicWorldGroundEffects.ActorKind.PLAYER))).isEmpty());
        assertTrue(effects.contains(BattleSpellCatalog.LIFE_STREAM,
                x, y, start + 320 * SECOND - 1));
        assertFalse(effects.contains(BattleSpellCatalog.LIFE_STREAM,
                x, y, start + 320 * SECOND));
        assertEquals(0, effects.activeCount(start + 320 * SECOND));
    }

    private static List<Corridor> corridors(int requested) {
        List<Corridor> result = new ArrayList<>();
        Set<Long> leased = new HashSet<>();
        for (int y = 32300; y <= 32450 && result.size() < requested; y++) {
            for (int sourceX = 33250;
                    sourceX <= 33450 && result.size() < requested; sourceX++) {
                if (!MAP.contains(sourceX, y)) continue;
                boolean open = true;
                for (int offset = 1; offset <= 8; offset++) {
                    int x = sourceX + offset;
                    long cell = ((long) x << 32) ^ Integer.toUnsignedLong(y);
                    if (leased.contains(cell) || !MAP.arrowPassable(x, y)
                            || !MAP.attackPosition(
                                    x - 1, y, x, y, 1)) {
                        open = false;
                        break;
                    }
                }
                if (!open) continue;
                Corridor corridor = new Corridor(sourceX, sourceX + 8, y);
                result.add(corridor);
                for (int offset = 1; offset <= 8; offset++) {
                    leased.add(((long) (sourceX + offset) << 32)
                            ^ Integer.toUnsignedLong(y));
                }
                // Keep successive corridors on adjacent rows so all 24
                // leases remain inside the next cast's native screen.
                break;
            }
        }
        assertEquals(requested, result.size(),
                "selected map 4 must retain enough independent 8-cell corridors");
        return List.copyOf(result);
    }

    private record Corridor(int sourceX, int targetX, int y) {}
}
