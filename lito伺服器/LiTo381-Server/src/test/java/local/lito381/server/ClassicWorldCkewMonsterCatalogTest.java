package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class ClassicWorldCkewMonsterCatalogTest {
    @Test
    void retainedAndCorroboratingRowsCreateTheExactOneShotPopulation() {
        List<ClassicWorldMonsterCatalog.SpawnDefinition> definitions =
                ClassicWorldCkewMonsterCatalog.definitions();
        assertEquals(ClassicWorldCkewMonsterCatalog.DEFINITION_COUNT,
                definitions.size());
        assertEquals(ClassicWorldCkewMonsterCatalog.POPULATION,
                definitions.stream()
                .mapToInt(ClassicWorldMonsterCatalog.SpawnDefinition::count).sum());
        assertEquals(List.of(50143, 50144, 50145, 50150, 50151, 50152,
                        50153, 50154, 50155, 50156, 50157, 50158,
                        400343, 400347,
                        400351, 400352, 400353, 400354, 400355, 400356, 400357,
                        400358, 400359, 400360, 400361, 400362, 400363, 400364,
                        400415, 400420, 400425, 400430, 400433),
                definitions.stream().map(
                        ClassicWorldMonsterCatalog.SpawnDefinition::spawnId).toList());
        assertEquals(Set.of(87000, 87001, 87002, 87003, 87004, 87005, 87006,
                        87007, 87008, 87009, 87010, 87011, 87012, 87013, 87014),
                definitions.stream().map(definition ->
                        definition.template().npcId()).collect(
                                java.util.stream.Collectors.toSet()));
        assertTrue(definitions.subList(0, 12).stream().allMatch(definition ->
                definition.mapId() == 2000 && definition.sourceAreaSpawn()
                        && definition.minRespawnSeconds() == 30
                        && definition.maxRespawnSeconds() == 30));

        ClassicWorldMonsterCatalog.SpawnDefinition altar = definitions.get(12);
        assertEquals(400343, altar.spawnId());
        assertEquals(87000, altar.template().npcId());
        assertEquals(32816, altar.locX());
        assertEquals(32773, altar.locY());
        assertFalse(altar.sourceAreaSpawn());
        assertFalse(altar.template().aggressive());
        assertEquals(15, altar.minRespawnSeconds());
        assertEquals(30, altar.maxRespawnSeconds());
        assertEquals(1, altar.movementDistance());

        assertSpawn(definitions, 400347, 87001, 10,
                32750, 32873, 32793, 32914);
        assertSpawn(definitions, 400351, 87002, 1,
                32807, 32881, 32826, 32909);
        assertSpawn(definitions, 400357, 87002, 1,
                32755, 32840, 32784, 32858);
        assertSpawn(definitions, 400358, 87003, 8,
                32807, 32881, 32826, 32909);
        assertSpawn(definitions, 400364, 87003, 8,
                32755, 32840, 32784, 32858);
        assertSpawn(definitions, 400415, 87009, 1,
                32863, 32890, 32901, 32929);
        assertSpawn(definitions, 400420, 87010, 1,
                32863, 32890, 32901, 32929);
        assertSpawn(definitions, 400425, 87007, 1,
                32863, 32890, 32901, 32929);
        assertSpawn(definitions, 400430, 87008, 1,
                32863, 32890, 32901, 32929);
        assertSpawn(definitions, 400433, 87011, 6,
                32727, 32740, 32775, 32804);

        assertEquals(2,
                ClassicWorldCkewMonsterCatalog.physicalSkills(87004).size());
        assertEquals(1,
                ClassicWorldCkewMonsterCatalog.physicalSkills(87006).size());
        assertEquals(1,
                ClassicWorldCkewMonsterCatalog.physicalSkills(87014).size());
        for (int npcId = 87007; npcId <= 87010; npcId++) {
            ClassicWorldMonsterPhysicalSkillCatalog.Definition skill =
                    ClassicWorldCkewMonsterCatalog.physicalSkills(npcId).get(0);
            assertEquals(2, skill.actNo());
            assertEquals(50, skill.triggerChance());
            assertEquals(-3, skill.triggerRange());
            assertEquals(3, skill.range());
            assertEquals(20, skill.leverage());
            assertEquals(19, skill.actionId());
        }
        for (int npcId = 87015; npcId <= 87018; npcId++) {
            ClassicWorldMonsterPhysicalSkillCatalog.Definition skill =
                    ClassicWorldCkewMonsterCatalog.physicalSkills(npcId).get(0);
            assertEquals(2, skill.actNo());
            assertEquals(100, skill.triggerChance());
            assertEquals(-2, skill.triggerRange());
            assertEquals(2, skill.range());
            assertEquals(50, skill.leverage());
            assertEquals(0, skill.actionId());
        }
        assertTrue(ClassicWorldCkewMonsterCatalog.physicalSkills(87005).isEmpty());
        assertEquals(2495,
                ClassicWorldCkewMonsterCatalog.templateForNpc(87015).gfxId());
        assertEquals(3000,
                ClassicWorldCkewMonsterCatalog.templateForNpc(87018).hp());
        assertNull(ClassicWorldCkewMonsterCatalog.templateForNpc(99999));

        ClassicWorldMonsterPopulation population =
                ClassicWorldMonsterPopulation.questInstance(122L,
                        0x6600_0000L, 0x6600_1000L, definitions);
        assertEquals(ClassicWorldCkewMonsterCatalog.POPULATION, population.size());
        for (ClassicWorldMonsterPopulation.Snapshot actor
                : population.inScreen(2000, 32800, 32800)) {
            assertTrue(ClassicWorldMapCatalog.forMap(2000)
                    .standingPassable(actor.x(), actor.y()));
        }
        ClassicWorldMonsterPopulation.Snapshot first =
                population.actor(2000, 0x6600_0001L);
        assertNotNull(first);
        ClassicWorldMonsterPopulation.Hit death = population.damage(
                2000, first.objectId(), 7L, first.maximumHp());
        assertTrue(death.diedNow());
        assertNotNull(population.removeCorpse(2000, first.objectId()));
        assertEquals(-1, population.nextRespawnDelaySeconds(
                2000, first.objectId()));
        assertNull(population.actor(2000, first.objectId()));
    }

    @Test
    void independentShowsUseDisjointObjectRangesAndState() {
        ClassicWorldMonsterPopulation first =
                ClassicWorldMonsterPopulation.questInstance(100L,
                        0x6600_0000L, 0x6600_1000L,
                        ClassicWorldCkewMonsterCatalog.definitions());
        ClassicWorldMonsterPopulation second =
                ClassicWorldMonsterPopulation.questInstance(101L,
                        0x6600_2000L, 0x6600_3000L,
                        ClassicWorldCkewMonsterCatalog.definitions());
        ClassicWorldMonsterPopulation.Snapshot firstActor =
                first.actor(2000, 0x6600_0001L);
        ClassicWorldMonsterPopulation.Snapshot secondActor =
                second.actor(2000, 0x6600_2001L);
        assertNotNull(firstActor);
        assertNotNull(secondActor);
        assertNotEquals(firstActor.objectId(), secondActor.objectId());
        assertNull(first.actor(2000, secondActor.objectId()));
        assertNull(second.actor(2000, firstActor.objectId()));

        assertTrue(first.provoke(2000, firstActor.objectId(), 77L));
        assertFalse(second.targetObjectId(
                2000, secondActor.objectId()) == 77L);
    }

    private static void assertSpawn(
            List<ClassicWorldMonsterCatalog.SpawnDefinition> definitions,
            int spawnId, int npcId, int count,
            int x1, int y1, int x2, int y2) {
        ClassicWorldMonsterCatalog.SpawnDefinition definition = definitions.stream()
                .filter(candidate -> candidate.spawnId() == spawnId)
                .findFirst().orElseThrow();
        assertEquals(npcId, definition.template().npcId());
        assertEquals(count, definition.count());
        assertEquals(x1, definition.locX1());
        assertEquals(y1, definition.locY1());
        assertEquals(x2, definition.locX2());
        assertEquals(y2, definition.locY2());
        assertTrue(definition.sourceAreaSpawn());
    }
}
