package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** NpcMove.setDirectionMove movement_distance applies to every monster step. */
final class ClassicWorldMonsterMovementDistanceTest {
    private static final int MAP_ID = 4;
    private static final long OBJECT_BASE = 0x6820_0000L;
    private static final long PLAYER = 0x4200_076AL;

    @Test
    void pursuitBroadcastStatePrecedesImmediateHomeSettlement() {
        int[] home = openEastCorridor();
        ClassicWorldMonsterPopulation population = population(home, 1200);
        long objectId = OBJECT_BASE + 1;
        assertTrue(population.provoke(MAP_ID, objectId, PLAYER));
        int targetX = home[0] + 10;

        ClassicWorldMonsterPopulation.Movement first = population.moveToward(
                MAP_ID, objectId, PLAYER, targetX, home[1], Set.of(), 1L);
        assertNotNull(first);
        assertFalse(first.teleportedHome());
        assertEquals(home[0] + 1, first.after().x());
        assertEquals(first.after(), first.settled());

        ClassicWorldMonsterPopulation.Movement second = population.moveToward(
                MAP_ID, objectId, PLAYER, targetX, home[1], Set.of(),
                10_000_000_000L);
        assertNotNull(second);
        assertEquals(home[0] + 2, second.after().x());
        assertTrue(second.teleportedHome());
        assertEquals(home[0], second.settled().x());
        assertEquals(home[1], second.settled().y());
        assertEquals(second.settled(), population.actor(MAP_ID, objectId));
    }

    @Test
    void escapeBroadcastStatePrecedesImmediateHomeSettlement() {
        int[] home = openEastCorridor();
        ClassicWorldMonsterPopulation population = population(home, 0);
        long objectId = OBJECT_BASE + 1;
        assertTrue(population.provoke(MAP_ID, objectId, PLAYER));
        int playerX = home[0] - 1;

        ClassicWorldMonsterPopulation.Movement first = population.moveAway(
                MAP_ID, objectId, PLAYER, playerX, home[1], Set.of(), 1L);
        assertNotNull(first);
        assertFalse(first.teleportedHome());
        assertEquals(home[0] + 1, first.after().x());

        ClassicWorldMonsterPopulation.Movement second = population.moveAway(
                MAP_ID, objectId, PLAYER, playerX, home[1], Set.of(),
                10_000_000_000L);
        assertNotNull(second);
        assertEquals(home[0] + 2, second.after().x());
        assertTrue(second.teleportedHome());
        assertEquals(home[0], second.settled().x());
        assertEquals(home[1], second.settled().y());
        assertEquals(PLAYER, population.targetObjectId(MAP_ID, objectId));
    }

    private static ClassicWorldMonsterPopulation population(
            int[] home, int attackSpeed) {
        ClassicWorldMonsterCatalog.MonsterTemplate source =
                ClassicWorldMonsterCatalog.definitions(MAP_ID).get(0).template();
        ClassicWorldMonsterCatalog.MonsterTemplate template =
                new ClassicWorldMonsterCatalog.MonsterTemplate(
                        source.npcId(), source.nameId(), "L1Monster",
                        source.gfxId(), source.level(), source.hp(), source.mp(),
                        source.armorClass(), source.strength(), source.constitution(),
                        source.dexterity(), source.wisdom(), source.intelligence(),
                        source.magicResistance(), source.experience(), source.lawful(),
                        source.size(), source.weakAttribute(), source.ranged(),
                        600, attackSpeed, source.bowActionId(), source.undead(),
                        source.poisonAttack(), source.paralysisAttack(), false,
                        source.damageReduction(), source.hard(), source.doppel(),
                        false, source.lightSize(), source.changeHead());
        ClassicWorldMonsterCatalog.SpawnDefinition definition =
                new ClassicWorldMonsterCatalog.SpawnDefinition(
                        MAP_ID, 990_770, "movement-distance-fixture", 1, 0,
                        home[0], home[1], 0, 0, 0, 0, 0, 0, 2,
                        0, 0, false, 1, false, 0, template);
        return ClassicWorldMonsterPopulation.retainedSourceSlice(
                770L, OBJECT_BASE, OBJECT_BASE + 0x1000L,
                List.of(definition));
    }

    private static int[] openEastCorridor() {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(MAP_ID);
        for (int x = map.startX() + 2; x <= map.endX() - 12; x++) {
            for (int y = map.startY() + 2; y <= map.endY() - 2; y++) {
                boolean open = map.standingPassable(x, y)
                        && !ClassicWorldNpcCatalog.occupies(MAP_ID, x, y);
                for (int step = 1; open && step <= 10; step++) {
                    open = map.canStep(x + step - 1, y, x + step, y)
                            && !ClassicWorldNpcCatalog.occupies(
                                    MAP_ID, x + step, y);
                }
                if (open) return new int[] {x, y};
            }
        }
        throw new AssertionError("missing open movement-distance corridor");
    }
}
