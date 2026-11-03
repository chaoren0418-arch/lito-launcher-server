package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ClassicWorldWaterDragonMonsterCatalogTest {
    @Test
    void initialDefinitionsAndBossTemplatesPreserveRetainedRows() {
        List<ClassicWorldMonsterCatalog.SpawnDefinition> definitions =
                ClassicWorldWaterDragonMonsterCatalog.definitions();
        assertEquals(5, definitions.size());
        assertEquals(83, definitions.stream().mapToInt(
                ClassicWorldMonsterCatalog.SpawnDefinition::count).sum());
        assertEquals(List.of(50207, 50208, 50209, 50210, 50211),
                definitions.stream().map(
                        ClassicWorldMonsterCatalog.SpawnDefinition::spawnId).toList());
        assertEquals(List.of(70974, 70975, 70976, 70977, 70978),
                definitions.stream().map(definition ->
                        definition.template().npcId()).toList());

        ClassicWorldMonsterCatalog.MonsterTemplate phase1 =
                ClassicWorldWaterDragonMonsterCatalog.templateForNpc(71026);
        ClassicWorldMonsterCatalog.MonsterTemplate phase2 =
                ClassicWorldWaterDragonMonsterCatalog.templateForNpc(71027);
        ClassicWorldMonsterCatalog.MonsterTemplate phase3 =
                ClassicWorldWaterDragonMonsterCatalog.templateForNpc(71028);
        assertEquals(List.of(7864, 7869, 7870),
                List.of(phase1.gfxId(), phase2.gfxId(), phase3.gfxId()));
        assertEquals(List.of(85, 95, 105),
                List.of(phase1.level(), phase2.level(), phase3.level()));
        for (ClassicWorldMonsterCatalog.MonsterTemplate boss
                : List.of(phase1, phase2, phase3)) {
            assertEquals(40_000, boss.hp());
            assertEquals(5_000, boss.mp());
            assertTrue(boss.boss());
        }
        assertNull(ClassicWorldWaterDragonMonsterCatalog.templateForNpc(71029));
    }

    @Test
    void oneShowPopulationPlacesEightyThreeOneShotActorsAndDynamicBoss() {
        ClassicWorldMonsterPopulation population =
                ClassicWorldMonsterPopulation.questInstance(
                        117, 0x6700_0000L, 0x6700_1000L,
                        ClassicWorldWaterDragonMonsterCatalog.definitions());
        assertEquals(83, population.size());
        assertTrue(population.hasPresentNpc(1011,
                java.util.Set.of(70974, 70975, 70976, 70977, 70978)));
        ClassicWorldMonsterPopulation.Snapshot boss =
                population.spawnQuestMonsterAt(1011, 71026,
                        32958, 32835, 3,
                        ClassicWorldWaterDragonMonsterCatalog.templateForNpc(71026));
        assertNotNull(boss);
        assertEquals(71026, boss.npcId());
        assertEquals(40_000, boss.maximumHp());
        assertEquals(84, population.size());
        assertFalse(population.hasPresentNpc(1011, java.util.Set.of(71027)));
    }

    @Test
    void arbitraryPassObjectShowIdsCannotLoseFixedGuardiansToAreaPlacement() {
        List<Integer> showIds = new java.util.ArrayList<>();
        showIds.add(0x1234_5680);
        for (int index = 0; index < 128; index++) {
            showIds.add(0x7000_0000 + index);
        }
        for (int showId : showIds) {
            ClassicWorldMonsterPopulation population =
                    ClassicWorldMonsterPopulation.questInstance(
                            117L ^ showId, 0x6700_0000L, 0x6700_1000L,
                            ClassicWorldWaterDragonMonsterCatalog.definitions());
            assertEquals(ClassicWorldWaterDragonMonsterCatalog.INITIAL_POPULATION,
                    population.size(), "show=" + showId);
            for (int npcId : List.of(70976, 70977, 70978)) {
                assertTrue(population.hasPresentNpc(
                        ClassicWorldWaterDragonCatalog.MAP_ID,
                        java.util.Set.of(npcId)),
                        "fixed guardian " + npcId + " show=" + showId);
            }
        }
    }
}
