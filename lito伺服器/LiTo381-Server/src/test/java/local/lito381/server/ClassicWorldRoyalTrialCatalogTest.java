package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class ClassicWorldRoyalTrialCatalogTest {
    @Test
    void restoresEveryClassicBlackKnightSearchPartyRowWithoutChangingSelectedRows() {
        List<ClassicWorldMonsterCatalog.SpawnDefinition> rows =
                ClassicWorldRoyalTrialCatalog.definitions(4);
        assertEquals(ClassicWorldRoyalTrialCatalog.BLACK_KNIGHT_SPAWN_ROWS,
                rows.size());
        assertEquals(ClassicWorldRoyalTrialCatalog.BLACK_KNIGHT_POPULATION,
                rows.stream().mapToInt(
                        ClassicWorldMonsterCatalog.SpawnDefinition::count).sum());
        assertEquals(List.of(
                8335, 8336, 8337, 8338, 8339, 8340, 8341,
                26553, 26554, 26555, 26556, 26557, 26558),
                rows.stream().map(
                        ClassicWorldMonsterCatalog.SpawnDefinition::spawnId).toList());
        assertEquals(729, ClassicWorldMonsterCatalog.definitions(4).size());
        assertTrue(ClassicWorldRoyalTrialCatalog.definitions(0).isEmpty());

        ClassicWorldMonsterCatalog.SpawnDefinition first = rows.get(0);
        assertEquals(81066, first.template().npcId());
        assertEquals("$1929", first.template().nameId());
        assertEquals(4003, first.template().gfxId());
        assertEquals(16, first.template().level());
        assertEquals(100, first.template().hp());
        assertEquals(-10, first.template().armorClass());
        assertEquals(32798, first.locX());
        assertEquals(32944, first.locY());
        assertEquals(3, first.randomX());
        assertEquals(3, first.randomY());
        assertEquals(60, first.minRespawnSeconds());
        assertEquals(120, first.maxRespawnSeconds());
    }

    @Test
    void runtimeAppendsTwentySixPassableActorsAfterTheSelectedPopulation() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        assertEquals(ClassicWorldMonsterCatalog.TOTAL_POPULATION,
                population.size());
        Set<Long> actors = new HashSet<>();
        for (int index = 1;
             index <= ClassicWorldRoyalTrialCatalog.BLACK_KNIGHT_POPULATION;
             index++) {
            long objectId = ClassicWorldMonsterPopulation.OBJECT_BASE
                    | ClassicWorldMonsterCatalog.PRE_332_SELECTED_TOTAL_POPULATION + index;
            ClassicWorldMonsterPopulation.Snapshot actor =
                    population.actor(4, objectId);
            assertNotNull(actor);
            assertEquals(81066, actor.npcId());
            assertTrue(ClassicWorldMapCatalog.forMap(4)
                    .standingPassable(actor.x(), actor.y()));
            actors.add(actor.objectId());
        }
        assertEquals(ClassicWorldRoyalTrialCatalog.BLACK_KNIGHT_POPULATION,
                actors.size());
        ClassicWorldMonsterPopulation.Snapshot nearest =
                population.nearest(4, 32798, 32944, 81066);
        assertNotNull(nearest);
        assertEquals(8335, nearest.spawnId());
    }

    @Test
    void retainedFortyPercentRuleRequiresActiveCrownTrialAndNoDuplicateItem() {
        Map<Integer, Integer> active = Map.of(10101, 0);
        ClassicWorldRoyalTrialCatalog.QuestDropDecision search =
                ClassicWorldRoyalTrialCatalog.decideQuestDrop(
                        81066, 0, active, Map.of(), 39);
        assertNotNull(search);
        assertEquals(40565, search.itemId());
        assertTrue(search.awarded());

        assertFalse(ClassicWorldRoyalTrialCatalog.decideQuestDrop(
                81066, 0, active, Map.of(), 40).awarded());
        assertFalse(ClassicWorldRoyalTrialCatalog.decideQuestDrop(
                81066, 1, active, Map.of(), 0).awarded());
        assertFalse(ClassicWorldRoyalTrialCatalog.decideQuestDrop(
                81066, 0, Map.of(), Map.of(), 0).awarded());
        assertFalse(ClassicWorldRoyalTrialCatalog.decideQuestDrop(
                81066, 0, active, Map.of(40565L, 1), 0).awarded());
        assertFalse(ClassicWorldRoyalTrialCatalog.decideQuestDrop(
                81066, 0, Map.of(10101, 1, 10103, 1), Map.of(), 0).awarded());

        ClassicWorldRoyalTrialCatalog.QuestDropDecision life =
                ClassicWorldRoyalTrialCatalog.decideQuestDrop(
                        45126, 0, Map.of(10101, 1, 10102, 0), Map.of(), 0);
        assertEquals(40564, life.itemId());
        assertTrue(life.awarded());
        assertNull(ClassicWorldRoyalTrialCatalog.decideQuestDrop(
                45009, 0, active, Map.of(), 0));
    }

    @Test
    void restoredActorUsesExactSelectedOrdinaryDropRowsAndItemMetadata() {
        List<ClassicWorldDropCatalog.DropRule> rules =
                ClassicWorldRoyalTrialCatalog.ordinaryRules(4, 81066);
        assertEquals(17, rules.size());
        assertEquals(50_000, rules.stream().filter(rule -> rule.itemId() == 40015)
                .findFirst().orElseThrow().chance());
        assertEquals(10_000, rules.stream().filter(rule -> rule.itemId() == 20085)
                .findFirst().orElseThrow().chance());
        assertEquals(0, rules.stream().filter(rule -> rule.itemId() == 20154)
                .findFirst().orElseThrow().chance());
        assertTrue(ClassicWorldRoyalTrialCatalog.ordinaryRules(4, 45126).isEmpty());

        List<ClassicWorldDropCatalog.RolledItem> rolled =
                ClassicWorldRoyalTrialCatalog.rollOrdinaryDrops(
                        4, 81066, 1_000.0, 1.0, 1.0, new Random(331));
        assertEquals(Set.of(
                52, 20063, 20085, 40014, 40015,
                40044, 40045, 40046, 40047),
                rolled.stream().map(item -> item.item().itemId())
                        .collect(java.util.stream.Collectors.toSet()));
        ClassicWorldDropCatalog.RolledItem shirt = rolled.stream()
                .filter(item -> item.item().itemId() == 20085)
                .findFirst().orElseThrow();
        assertEquals("$168", shirt.item().nameId());
        assertEquals(110, shirt.item().inventoryGfx());
        assertEquals(24, shirt.item().groundGfx());
        assertEquals(104, shirt.item().itemDescriptionId());
        assertEquals(127, shirt.item().classMask());
    }

    @Test
    void loopbackValidationCanPinTheSuccessfulZeroBasedRoll() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.world-zero-gunter-selftest.properties"));
        assertEquals(1, config.classicWorldQuestDropFixtureRoll());
    }
}
