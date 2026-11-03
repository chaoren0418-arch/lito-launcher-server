package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class ClassicWorldMonsterPathFailureTest {
    private static final int MAP_ID = 4;
    private static final long TARGET = 77;

    @Test
    void transientLiveCrowdKeepsTargetAndDoesNotAccumulateFailure() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot actor = boar(population);
        assertTrue(population.provoke(MAP_ID, actor.objectId(), TARGET));
        int targetX = actor.x() - 3;
        int targetY = actor.y() - 3;
        Set<ClassicWorldMonsterPopulation.GridPoint> occupied =
                new LinkedHashSet<>();
        for (int x = actor.x() - 1; x <= actor.x() + 1; x++) {
            for (int y = actor.y() - 1; y <= actor.y() + 1; y++) {
                if (x != actor.x() || y != actor.y()) {
                    occupied.add(new ClassicWorldMonsterPopulation.GridPoint(x, y));
                }
            }
        }
        occupied.add(new ClassicWorldMonsterPopulation.GridPoint(targetX, targetY));

        assertNull(population.moveToward(
                MAP_ID, actor.objectId(), TARGET, targetX, targetY,
                Set.copyOf(occupied), 1_000_000_000L));
        assertEquals(TARGET,
                population.targetObjectId(MAP_ID, actor.objectId()));
        assertEquals(0,
                population.pathFailureCount(MAP_ID, actor.objectId()));
    }

    @Test
    void ordinaryMonsterDropsTargetOnThirdProvenUnreachableAttempt() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot actor = boar(population);
        assertTrue(population.provoke(MAP_ID, actor.objectId(), TARGET));
        long now = 2_000_000_000L;

        for (int attempt = 1;
             attempt < ClassicWorldMonsterPopulation.PATH_FAIL_GIVE_UP;
             attempt++) {
            assertNull(population.moveToward(
                    MAP_ID, actor.objectId(), TARGET, 1, 1, Set.of(), now));
            assertEquals(attempt,
                    population.pathFailureCount(MAP_ID, actor.objectId()));
            assertEquals(TARGET,
                    population.targetObjectId(MAP_ID, actor.objectId()));
            now += ClassicWorldMonsterPopulation
                    .TRANSIENT_PATH_RETRY_MILLIS * 1_000_000L;
        }
        assertNull(population.moveToward(
                MAP_ID, actor.objectId(), TARGET, 1, 1, Set.of(), now));
        assertEquals(0,
                population.pathFailureCount(MAP_ID, actor.objectId()));
        assertEquals(0,
                population.targetObjectId(MAP_ID, actor.objectId()));
    }

    @Test
    void bossNeverDropsTargetForProvenUnreachablePath() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterCatalog.MonsterTemplate bossTemplate =
                ClassicWorldMapCatalog.supportedMapIds().stream()
                        .flatMap(mapId -> ClassicWorldMonsterCatalog
                                .definitions(mapId).stream())
                        .map(ClassicWorldMonsterCatalog.SpawnDefinition::template)
                        .filter(template -> template.boss()
                                && template.passispeed() > 0)
                        .findFirst().orElseThrow();
        int[] spawn = freeNearWorldTree(population);
        ClassicWorldMonsterPopulation.Snapshot boss =
                population.spawnQuestMonsterAt(
                        MAP_ID, 999_776, spawn[0], spawn[1], 0, bossTemplate);
        assertNotNull(boss);
        assertTrue(population.provoke(MAP_ID, boss.objectId(), TARGET));
        long now = 3_000_000_000L;

        for (int attempt = 0; attempt < 8; attempt++) {
            assertNull(population.moveToward(
                    MAP_ID, boss.objectId(), TARGET, 1, 1, Set.of(), now));
            assertEquals(0,
                    population.pathFailureCount(MAP_ID, boss.objectId()));
            assertEquals(TARGET,
                    population.targetObjectId(MAP_ID, boss.objectId()));
            now += ClassicWorldMonsterPopulation
                    .TRANSIENT_PATH_RETRY_MILLIS * 1_000_000L;
        }
    }

    private static ClassicWorldMonsterPopulation.Snapshot boar(
            ClassicWorldMonsterPopulation population) {
        ClassicWorldMonsterPopulation.Snapshot actor = population.nearest(
                MAP_ID, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45023);
        assertNotNull(actor);
        return actor;
    }

    private static int[] freeNearWorldTree(
            ClassicWorldMonsterPopulation population) {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(MAP_ID);
        for (int radius = 1; radius <= 12; radius++) {
            for (int x = ClassicWorldMapCatalog.WORLD_TREE_X - radius;
                 x <= ClassicWorldMapCatalog.WORLD_TREE_X + radius; x++) {
                for (int y = ClassicWorldMapCatalog.WORLD_TREE_Y - radius;
                     y <= ClassicWorldMapCatalog.WORLD_TREE_Y + radius; y++) {
                    if (map.standingPassable(x, y)
                            && !population.occupies(MAP_ID, x, y)
                            && !ClassicWorldNpcCatalog.occupies(MAP_ID, x, y)) {
                        return new int[] {x, y};
                    }
                }
            }
        }
        throw new AssertionError("no free WORLD test cell");
    }
}
