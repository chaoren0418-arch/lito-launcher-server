package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

final class ClassicWorldMonsterPhysicalSkillCatalogTest {
    @Test
    void map4KeepsEverySelectedSourceType1RowAndPopulation() {
        List<ClassicWorldMonsterPhysicalSkillCatalog.Definition> definitions =
                ClassicWorldMonsterPhysicalSkillCatalog.definitions();
        assertEquals(ClassicWorldMonsterPhysicalSkillCatalog.MAP4_PHYSICAL_ROWS,
                definitions.size());
        Set<Integer> npcIds = definitions.stream()
                .map(ClassicWorldMonsterPhysicalSkillCatalog.Definition::npcId)
                .collect(Collectors.toSet());
        assertEquals(ClassicWorldMonsterPhysicalSkillCatalog.MAP4_PHYSICAL_NPC_TYPES,
                npcIds.size());
        int population = ClassicWorldMonsterCatalog.definitions(4).stream()
                .filter(spawn -> npcIds.contains(spawn.template().npcId()))
                .mapToInt(ClassicWorldMonsterCatalog.SpawnDefinition::count)
                .sum();
        assertEquals(ClassicWorldMonsterPhysicalSkillCatalog.MAP4_PHYSICAL_POPULATION,
                population);

        assertEquals(1, ClassicWorldMonsterPhysicalSkillCatalog.forNpc(45278).size());
        assertEquals(1, ClassicWorldMonsterPhysicalSkillCatalog.forNpc(45284).size());
        assertEquals(1, ClassicWorldMonsterPhysicalSkillCatalog.forNpc(45291).size());
        assertEquals(2, ClassicWorldMonsterPhysicalSkillCatalog.forNpc(45341).size());
        assertTrue(ClassicWorldMonsterPhysicalSkillCatalog.forNpc(45009).isEmpty());
    }

    @Test
    void exactRowsKeepSignedDistanceStrictChanceCountAreaLeverageAndEffects() {
        ClassicWorldMonsterPhysicalSkillCatalog.Definition ogre =
                ClassicWorldMonsterPhysicalSkillCatalog.forNpc(45278).get(0);
        assertEquals(30, ogre.triggerChance());
        assertEquals(-2, ogre.triggerRange());
        assertTrue(ogre.triggerDistance(2));
        assertFalse(ogre.triggerDistance(3));
        assertTrue(ogre.triggerRoll(29));
        assertFalse(ogre.triggerRoll(30));
        assertEquals(2, ogre.range());
        assertEquals(1, ogre.areaWidth());
        assertEquals(2, ogre.areaHeight());
        assertEquals(20, ogre.effectiveLeverage());
        assertEquals(30, ogre.actionId());

        List<ClassicWorldMonsterPhysicalSkillCatalog.Definition> fire =
                ClassicWorldMonsterPhysicalSkillCatalog.forNpc(45341);
        assertEquals(2, fire.get(0).triggerRange());
        assertEquals(2, fire.get(0).triggerCount());
        assertEquals(954, fire.get(0).gfxId());
        assertEquals(3, fire.get(0).actionId());
        assertEquals(10, fire.get(0).effectiveLeverage());
        assertEquals(-1, fire.get(1).triggerRange());
        assertEquals(30, fire.get(1).actionId());
    }

    @Test
    void retainedRotatedForwardBoxUsesHeadingWidthAndHeight() {
        ClassicWorldMonsterPhysicalSkillCatalog.Definition ogre =
                ClassicWorldMonsterPhysicalSkillCatalog.forNpc(45278).get(0);
        assertTrue(ogre.containsBoxTarget(100, 100, 2, 101, 100));
        assertTrue(ogre.containsBoxTarget(100, 100, 2, 102, 99));
        assertTrue(ogre.containsBoxTarget(100, 100, 2, 102, 101));
        assertFalse(ogre.containsBoxTarget(100, 100, 2, 102, 102));
        assertFalse(ogre.containsBoxTarget(100, 100, 2, 99, 100));
        assertTrue(ogre.containsBoxTarget(100, 100, 0, 100, 98));
        assertFalse(ogre.containsBoxTarget(100, 100, 0, 100, 102));
    }
}
