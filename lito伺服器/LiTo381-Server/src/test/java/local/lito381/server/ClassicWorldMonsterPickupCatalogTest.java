package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

final class ClassicWorldMonsterPickupCatalogTest {
    @Test
    void selectedNpcProjectionIsExactAndFeedsLoadedPopulation() {
        assertEquals(68, ClassicWorldMonsterPickupCatalog.size());
        assertTrue(ClassicWorldMonsterPickupCatalog.enabled(45009));
        assertTrue(ClassicWorldMonsterPickupCatalog.enabled(45032));
        assertTrue(ClassicWorldMonsterPickupCatalog.enabled(70984));
        assertFalse(ClassicWorldMonsterPickupCatalog.enabled(45010));

        Set<Integer> used = allDefinitions()
                .map(definition -> definition.template().npcId())
                .filter(ClassicWorldMonsterPickupCatalog::enabled)
                .collect(Collectors.toSet());
        assertEquals(22, used.size());
        int spawnRows = (int) allDefinitions()
                .filter(definition -> ClassicWorldMonsterPickupCatalog.enabled(
                        definition.template()))
                .count();
        int population = allDefinitions()
                .filter(definition -> ClassicWorldMonsterPickupCatalog.enabled(
                        definition.template()))
                .mapToInt(ClassicWorldMonsterCatalog.SpawnDefinition::count)
                .sum();
        assertEquals(298, spawnRows);
        assertEquals(1_962, population);
    }

    private static Stream<ClassicWorldMonsterCatalog.SpawnDefinition>
            allDefinitions() {
        return ClassicWorldMapCatalog.supportedMapIds().stream()
                .flatMap(mapId -> ClassicWorldMonsterCatalog
                        .definitions(mapId).stream());
    }
}
