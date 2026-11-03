package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

final class ClassicWorldCkewMonsterStatusSkillTest {
    private static final int MAP_ID = ClassicWorldCkewCatalog.MAP_ID;
    private static final long TARGET_ID = 0x7100_0001L;

    @Test
    void retainedWatcherRowSkillAndProbabilityColumnsAreExact() {
        assertEquals(1,
                ClassicWorldMonsterStatusSkillCatalog.definitions().size());
        ClassicWorldMonsterStatusSkillCatalog.Definition skill =
                ClassicWorldMonsterStatusSkillCatalog.forNpc(87005).get(0);
        assertEquals(0, skill.actNo());
        assertEquals(20, skill.triggerChance());
        assertEquals(-2, skill.triggerRange());
        assertEquals(1, skill.leverage());
        assertEquals(33, skill.skillId());
        assertEquals(150, skill.mpCost());
        assertEquals(5_000, skill.reuseDelayMillis());
        assertEquals(2, skill.maximumRange());
        assertEquals(19, skill.actionId());
        assertEquals(746, skill.effectGfx());
        assertEquals(1_120, skill.actionDelayMillis());
        assertEquals(5_000, skill.warningDelayMillis());
        assertEquals(10_000, skill.paralysisDurationMillis());
        assertEquals(280, skill.failureMessage());
        assertEquals(55, skill.probability(30, 12, 29, 0, 0));
        assertEquals(45, skill.probability(30, 12, 30, 0, 0));
        assertEquals(35, skill.probability(30, 12, 31, 0, 0));
        assertEquals(40, skill.probability(30, 12, 29, 100, 10));

        int population = ClassicWorldCkewMonsterCatalog.definitions().stream()
                .filter(spawn -> spawn.template().npcId() == 87005)
                .mapToInt(ClassicWorldMonsterCatalog.SpawnDefinition::count)
                .sum();
        assertEquals(
                ClassicWorldMonsterStatusSkillCatalog.CKEW_STATUS_POPULATION,
                population);
    }

    @Test
    void strictOuterChanceMpReuseAndSupportClockAreRetained() {
        long now = 50_000_000_000L;
        ClassicWorldMonsterPopulation population = population();
        ClassicWorldMonsterPopulation.Snapshot watcher = watcher(population);
        ClassicWorldMonsterPopulation.GridPoint target =
                targetAtDistance(watcher, 1);
        assertNotNull(target);
        assertTrue(population.provoke(MAP_ID, watcher.objectId(), TARGET_ID));

        assertNull(population.ckewSkillAttack(
                MAP_ID, watcher.objectId(), TARGET_ID,
                target.x(), target.y(), now, Set.of(target), 1, 20, 1));
        ClassicWorldMonsterPopulation.CkewSkillAttack cast =
                population.ckewSkillAttack(
                        MAP_ID, watcher.objectId(), TARGET_ID,
                        target.x(), target.y(), now, Set.of(target), 1, 19, 1);
        assertNotNull(cast);
        assertNull(cast.physical());
        assertNull(cast.magic());
        assertNotNull(cast.status());
        assertEquals(33, cast.status().skill().skillId());
        assertTrue(cast.status().triggerChanceApplied());
        assertEquals(19, cast.status().triggerRoll());
        assertEquals(350, cast.status().attacker().currentMp());
        assertFalse(population.actionReady(
                MAP_ID, watcher.objectId(), now + 1_119_999_999L));
        assertTrue(population.actionReady(
                MAP_ID, watcher.objectId(), now + 1_120_000_000L));
        assertNull(population.ckewSkillAttack(
                MAP_ID, watcher.objectId(), TARGET_ID,
                target.x(), target.y(), now + 1_120_000_000L,
                Set.of(target), 1, 1, 1));
        assertNotNull(population.ckewSkillAttack(
                MAP_ID, watcher.objectId(), TARGET_ID,
                target.x(), target.y(), now + 5_000_000_000L,
                Set.of(target), 1, 1, 1));
    }

    @Test
    void pursuitIgnoresOuterTriRndForWatcherStatusRow() {
        long now = 60_000_000_000L;
        ClassicWorldMonsterPopulation population = population();
        ClassicWorldMonsterPopulation.Snapshot watcher = watcher(population);
        ClassicWorldMonsterPopulation.GridPoint target =
                targetAtDistance(watcher, 2);
        assertNotNull(target);
        assertTrue(population.provoke(MAP_ID, watcher.objectId(), TARGET_ID));

        ClassicWorldMonsterPopulation.CkewSkillAttack cast =
                population.ckewSkillAttack(
                        MAP_ID, watcher.objectId(), TARGET_ID,
                        target.x(), target.y(), now, Set.of(target), 2, 100, 1);
        assertNotNull(cast);
        assertNotNull(cast.status());
        assertFalse(cast.status().triggerChanceApplied());
        assertEquals(0, cast.status().triggerRoll());
    }

    @Test
    void entangleDoublesCkewStatusActionClock() {
        long now = 70_000_000_000L;
        ClassicWorldMonsterPopulation population = population();
        ClassicWorldMonsterPopulation.Snapshot watcher = watcher(population);
        ClassicWorldMonsterPopulation.GridPoint target =
                targetAtDistance(watcher, 1);
        assertNotNull(target);
        assertTrue(population.provoke(MAP_ID, watcher.objectId(), TARGET_ID));
        assertTrue(population.slow(MAP_ID, watcher.objectId(), now, 64));

        ClassicWorldMonsterPopulation.CkewSkillAttack cast =
                population.ckewSkillAttack(
                        MAP_ID, watcher.objectId(), TARGET_ID,
                        target.x(), target.y(), now, Set.of(target), 1, 19, 1);
        assertNotNull(cast);
        assertNotNull(cast.status());
        long delayNanos = cast.status().skill().actionDelayMillis()
                * 2L * 1_000_000L;
        assertFalse(population.actionReady(
                MAP_ID, watcher.objectId(), now + delayNanos - 1));
        assertTrue(population.actionReady(
                MAP_ID, watcher.objectId(), now + delayNanos));
    }

    @Test
    void ordinarySilenceConsumesOneSelectedCkewStatusAttempt() {
        long now = 80_000_000_000L;
        ClassicWorldMonsterPopulation population = population();
        ClassicWorldMonsterPopulation.Snapshot watcher = watcher(population);
        ClassicWorldMonsterPopulation.GridPoint target =
                targetAtDistance(watcher, 1);
        assertNotNull(target);
        assertTrue(population.provoke(MAP_ID, watcher.objectId(), TARGET_ID));
        assertTrue(population.silence(
                MAP_ID, watcher.objectId(), now, 16));

        assertNull(population.ckewSkillAttack(
                MAP_ID, watcher.objectId(), TARGET_ID,
                target.x(), target.y(), now + 1, Set.of(target), 1, 19, 1));
        assertFalse(population.silenceActive(
                MAP_ID, watcher.objectId(), now + 2));
        ClassicWorldMonsterPopulation.CkewSkillAttack cast =
                population.ckewSkillAttack(
                        MAP_ID, watcher.objectId(), TARGET_ID,
                        target.x(), target.y(), now + 2,
                        Set.of(target), 1, 19, 1);
        assertNotNull(cast);
        assertNotNull(cast.status());
    }

    private static ClassicWorldMonsterPopulation population() {
        return ClassicWorldMonsterPopulation.questInstance(
                358L, 0x6700_0000L, 0x6700_1000L,
                ClassicWorldCkewMonsterCatalog.definitions());
    }

    private static ClassicWorldMonsterPopulation.Snapshot watcher(
            ClassicWorldMonsterPopulation population) {
        ClassicWorldMonsterPopulation.Snapshot result = population.nearest(
                MAP_ID, 32890, 32820, 87005);
        assertNotNull(result);
        return result;
    }

    private static ClassicWorldMonsterPopulation.GridPoint targetAtDistance(
            ClassicWorldMonsterPopulation.Snapshot actor, int distance) {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(actor.mapId());
        int[] dx = {0, 1, 1, 1, 0, -1, -1, -1};
        int[] dy = {-1, -1, 0, 1, 1, 1, 0, -1};
        for (int heading = 0; heading < dx.length; heading++) {
            int x = actor.x() + dx[heading] * distance;
            int y = actor.y() + dy[heading] * distance;
            if (map.attackPosition(actor.x(), actor.y(), x, y, distance)) {
                return new ClassicWorldMonsterPopulation.GridPoint(x, y);
            }
        }
        return null;
    }
}
