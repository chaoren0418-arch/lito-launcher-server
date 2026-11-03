package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

final class ClassicWorldDesireCaveCatalogTest {
    @Test void componentLocksAllSelectedMapsAndResources() {
        assertEquals(10, ClassicWorldDesireCaveCatalog.MAP_IDS.size());
        assertEquals(ClassicWorldDesireCaveCatalog.MAP_IDS,
                ClassicWorldDesireCaveCatalog.MAP_SOURCE_SHA256.keySet().stream()
                        .sorted().toList());
        assertEquals(583, ClassicWorldMonsterCatalog.DESIRE_CAVE_POPULATION);
        assertFalse(ClassicWorldDesireCaveCatalog.flags(604).takePets());
        assertFalse(ClassicWorldDesireCaveCatalog.flags(604).recallPets());
        assertTrue(ClassicWorldDesireCaveCatalog.flags(605).recallPets());
        assertEquals(ClassicWorldDesireCaveItemCatalog.ITEM_COUNT,
                ClassicWorldDesireCaveItemCatalog.items().size());
        for (long itemId : new long[] {20031, 20083, 20290, 40678, 40909,
                40921, 41130, 41138, 21020, 21027}) {
            assertNotNull(InventoryCatalog.find(itemId), "item " + itemId);
        }
    }

    @Test void retainedKarmaSignRuleUsesOpposingFiveTimesMultiplier() {
        assertEquals(-6, ClassicWorldDesireCaveCatalog.monsterKarmaDelta(
                45739, 0));
        assertEquals(-6, ClassicWorldDesireCaveCatalog.monsterKarmaDelta(
                45739, -1));
        assertEquals(-30, ClassicWorldDesireCaveCatalog.monsterKarmaDelta(
                45739, 1));
        assertEquals(0, ClassicWorldDesireCaveCatalog.monsterKarmaDelta(
                46070, 8));
    }

    @Test void dynamicPagesFollowLevelKarmaQuestAndInventory() {
        GameStateStore.FactionKarmaBalance neutral =
                new GameStateStore.FactionKarmaBalance(0);
        assertEquals("cpass03", ClassicWorldDesireCaveCatalog.dialogueHtml(
                80058, 44, 61, neutral, Map.of(), Map.of()));
        assertEquals("cpass02", ClassicWorldDesireCaveCatalog.dialogueHtml(
                80058, 45, 61, neutral, Map.of(), Map.of()));
        assertEquals("cpass01", ClassicWorldDesireCaveCatalog.dialogueHtml(
                80058, 52, 61, neutral, Map.of(), Map.of()));
        assertEquals("wpass06", ClassicWorldDesireCaveCatalog.dialogueHtml(
                80059, 52, 61, neutral, Map.of(),
                Map.of(40909L, 1, 40913L, 10)));
        assertEquals("minicod03", ClassicWorldDesireCaveCatalog.dialogueHtml(
                80067, 52, 6034, neutral, Map.of(36, 1), Map.of()));
        assertEquals("minicod05", ClassicWorldDesireCaveCatalog.dialogueHtml(
                80067, 52, 61, neutral, Map.of(36, 1), Map.of()));
        assertEquals("infamous01", ClassicWorldDesireCaveCatalog.dialogueHtml(
                80074, 52, 61, new GameStateStore.FactionKarmaBalance(10_000_000),
                Map.of(), Map.of()));
    }

    @Test void retainedActionsKeepExactDoorsCraftsAndRewards() {
        ClassicWorldDesireCaveActionCatalog.Door earth =
                ClassicWorldDesireCaveActionCatalog.door(80059);
        assertEquals(40909, earth.protectionId());
        assertEquals(40913, earth.sealId());
        assertEquals(40917, earth.rulerId());
        assertEquals(607, earth.destinationMapId());
        assertEquals(10, ClassicWorldDesireCaveCatalog.necessarySealCount(Map.of()));
        assertEquals(500, ClassicWorldDesireCaveCatalog.necessarySealCount(Map.of(
                40917L, 1, 40920L, 1, 40918L, 1)));

        ClassicWorldDesireCaveActionCatalog.Craft helm =
                ClassicWorldDesireCaveActionCatalog.craft("h");
        assertEquals(8, helm.minimumKarmaLevel());
        assertEquals(20031, helm.resultItemId());
        assertEquals("lsmithhh", helm.failureHtml());
        assertEquals(3, helm.materials().size());
        assertEquals(21027,
                ClassicWorldDesireCaveActionCatalog.earringForAction("8"));
        assertEquals(50_000,
                ClassicWorldDesireCaveActionCatalog.soulCrystalKarmaCost("3"));
    }

    @Test void desireActorsAppendAfterEverySealedPointSevenThreeEightId() {
        ClassicWorldMonsterPopulation population = new ClassicWorldMonsterPopulation();
        int previousPopulation = ClassicWorldMonsterCatalog.TOTAL_POPULATION
                - ClassicWorldMonsterCatalog.DESIRE_CAVE_POPULATION
                - ClassicWorldMonsterCatalog.MAP666_POPULATION;
        long objectId = ClassicWorldMonsterPopulation.OBJECT_BASE
                | (previousPopulation + 1L);
        int observed = 0;
        for (int mapId : ClassicWorldDesireCaveCatalog.MAP_IDS) {
            for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                    : ClassicWorldMonsterCatalog.definitions(mapId)) {
                for (int slot = 1; slot <= definition.count(); slot++) {
                    ClassicWorldMonsterPopulation.Snapshot actor =
                            population.actor(mapId, objectId++);
                    assertNotNull(actor, "map=" + mapId + " spawn="
                            + definition.spawnId() + " slot=" + slot);
                    assertEquals(definition.spawnId(), actor.spawnId());
                    assertEquals(slot, actor.spawnNumber());
                    observed++;
                }
            }
        }
        assertEquals(583, observed);
        assertEquals(ClassicWorldMonsterCatalog.TOTAL_POPULATION,
                population.size());
    }
}
