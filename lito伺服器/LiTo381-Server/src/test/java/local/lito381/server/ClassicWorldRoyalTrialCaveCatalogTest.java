package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

final class ClassicWorldRoyalTrialCaveCatalogTest {
    @Test
    void selectedMapOneAndTwoCollisionStaticMonsterAndDropResourcesAreLocked()
            throws Exception {
        ClassicWorldMapCatalog.ClassicMap map1 = ClassicWorldMapCatalog.forMap(1);
        ClassicWorldMapCatalog.ClassicMap map2 = ClassicWorldMapCatalog.forMap(2);
        ClassicWorldMapCatalog.ClassicMap map14 = ClassicWorldMapCatalog.forMap(14);
        assertEquals(List.of(32640, 32767, 32768, 32895),
                List.of(map1.startX(), map1.endX(), map1.startY(), map1.endY()));
        assertEquals(List.of(32640, 32831, 32768, 32895),
                List.of(map2.startX(), map2.endX(), map2.startY(), map2.endY()));
        assertEquals(ClassicWorldMapCatalog.MAP1_SOURCE_SHA256, map1.sourceSha256());
        assertEquals(ClassicWorldMapCatalog.MAP2_SOURCE_SHA256, map2.sourceSha256());
        assertEquals(List.of(32512, 33023, 32768, 32831),
                List.of(map14.startX(), map14.endX(), map14.startY(), map14.endY()));
        assertEquals(ClassicWorldMapCatalog.MAP14_SOURCE_SHA256, map14.sourceSha256());
        assertTrue(map1.allowsEscape());
        assertTrue(map2.allowsEscape());
        assertTrue(map1.allowsPetRecall());
        assertTrue(map2.allowsPetRecall());
        assertTrue(map14.allowsEscape());
        assertTrue(map14.allowsPetRecall());

        assertEquals(1, ClassicWorldNpcCatalog.all(1).size());
        assertEquals(29, ClassicWorldNpcCatalog.all(2).size());
        assertTrue(ClassicWorldNpcCatalog.all(2).stream().allMatch(
                npc -> npc.npcId() == 70558 && npc.implementation().equals("L1Bow")
                        && npc.gfxId() == 0 && npc.changeHead()));
        assertEquals(4, ClassicWorldMonsterCatalog.definitions(1).size());
        assertEquals(91, ClassicWorldMonsterCatalog.definitions(1).stream()
                .mapToInt(ClassicWorldMonsterCatalog.SpawnDefinition::count).sum());
        assertEquals(10, ClassicWorldMonsterCatalog.definitions(2).size());
        assertEquals(150, ClassicWorldMonsterCatalog.definitions(2).stream()
                .mapToInt(ClassicWorldMonsterCatalog.SpawnDefinition::count).sum());
        assertEquals(29, ClassicWorldDropCatalog.dropCount(1));
        assertEquals(26, ClassicWorldDropCatalog.items(1).size());
        assertEquals(89, ClassicWorldDropCatalog.dropCount(2));
        assertEquals(53, ClassicWorldDropCatalog.items(2).size());
        assertEquals(0, ClassicWorldNpcCatalog.all(14).size());
        assertEquals(3, ClassicWorldMonsterCatalog.definitions(14).size());
        assertEquals(55, ClassicWorldMonsterCatalog.definitions(14).stream()
                .mapToInt(ClassicWorldMonsterCatalog.SpawnDefinition::count).sum());
        assertEquals(15, ClassicWorldDropCatalog.dropCount(14));
        assertEquals(14, ClassicWorldDropCatalog.items(14).size());

        assertResource("/maps/1.l38.gz", ClassicWorldMapCatalog.MAP1_RESOURCE_SHA256);
        assertResource("/maps/2.l38.gz", ClassicWorldMapCatalog.MAP2_RESOURCE_SHA256);
        assertResource("/maps/14.l38.gz", ClassicWorldMapCatalog.MAP14_RESOURCE_SHA256);
        assertResource("/world/map1-static-npcs.lwn.gz",
                ClassicWorldNpcCatalog.MAP1_RESOURCE_SHA256);
        assertResource("/world/map2-static-npcs.lwn.gz",
                ClassicWorldNpcCatalog.MAP2_RESOURCE_SHA256);
        assertResource("/world/map1-monsters.lwm.gz",
                ClassicWorldMonsterCatalog.MAP1_RESOURCE_SHA256);
        assertResource("/world/map2-monsters.lwm.gz",
                ClassicWorldMonsterCatalog.MAP2_RESOURCE_SHA256);
        assertResource("/world/map1-drops.lwd.gz",
                ClassicWorldDropCatalog.MAP1_RESOURCE_SHA256);
        assertResource("/world/map2-drops.lwd.gz",
                ClassicWorldDropCatalog.MAP2_RESOURCE_SHA256);
        assertResource("/world/map14-static-npcs.lwn.gz",
                ClassicWorldNpcCatalog.MAP14_RESOURCE_SHA256);
        assertResource("/world/map14-monsters.lwm.gz",
                ClassicWorldMonsterCatalog.MAP14_RESOURCE_SHA256);
        assertResource("/world/map14-drops.lwd.gz",
                ClassicWorldDropCatalog.MAP14_RESOURCE_SHA256);
    }

    @Test
    void allEighteenSelectedGroundPortalsReachTheUnderseaTunnelBoundary() {
        List<ClassicWorldDungeonCatalog.Route> routes =
                ClassicWorldDungeonCatalog.all().stream()
                        .filter(route -> route.routeId() >= 173
                                && route.routeId() <= 190).toList();
        assertEquals(18, routes.size());
        assertEquals(List.of(173, 174, 175, 176, 177, 178, 179, 180,
                        181, 182, 183, 184, 185, 186, 187, 188, 189, 190),
                routes.stream().map(ClassicWorldDungeonCatalog.Route::routeId).toList());
        for (ClassicWorldDungeonCatalog.Route route : routes) {
            ClassicWorldMapCatalog.ClassicMap source =
                    ClassicWorldMapCatalog.forMap(route.sourceMapId());
            ClassicWorldMapCatalog.ClassicMap destination =
                    ClassicWorldMapCatalog.forMap(route.destinationMapId());
            assertTrue(source.contains(route.sourceX(), route.sourceY()));
            assertTrue(destination.contains(
                    route.destinationX(), route.destinationY()));
            assertTrue(source.standingPassable(route.sourceX(), route.sourceY()),
                    "source route " + route.routeId());
            assertTrue(destination.standingPassable(
                    route.destinationX(), route.destinationY()),
                    "destination route " + route.routeId());
            assertEquals(0, route.requiredItemCatalogId());
            assertFalse(route.scheduledPirateShip());
        }
        assertEquals(1, ClassicWorldDungeonCatalog.find(
                0, 32477, 32851).destinationMapId());
        assertEquals(2, ClassicWorldDungeonCatalog.find(
                1, 32737, 32877).destinationMapId());
        assertEquals(1, ClassicWorldDungeonCatalog.find(
                2, 32665, 32788).destinationMapId());
        assertEquals(14, ClassicWorldDungeonCatalog.find(
                2, 32804, 32873).destinationMapId());
        assertEquals(2, ClassicWorldDungeonCatalog.find(
                14, 32533, 32800).destinationMapId());
    }

    @Test
    void selectedGetbackRowsReturnBothCaveMapsToTalkingIslandTownOne() {
        for (int mapId : List.of(1, 2)) {
            ClassicWorldGetbackCatalog.Destination destination =
                    ClassicWorldGetbackCatalog.resolve(mapId, 32700, 32870, 0, 332);
            assertEquals(0, destination.mapId());
            assertEquals(32574, destination.x());
            assertEquals(32941, destination.y());
            assertEquals(1, destination.townId());
        }
        ClassicWorldGetbackCatalog.Destination tunnel =
                ClassicWorldGetbackCatalog.resolve(14, 32533, 32800, 0, 332);
        assertEquals(List.of(0, 32574, 32941, 0), List.of(
                tunnel.mapId(), tunnel.x(), tunnel.y(), tunnel.townId()));
    }

    @Test
    void publicSpawnRestoresTenHardStoneGolemsAfterEverySelectedActor() {
        List<ClassicWorldMonsterCatalog.SpawnDefinition> rows =
                ClassicWorldRoyalTrialCatalog.definitions(2);
        assertEquals(1, rows.size());
        ClassicWorldMonsterCatalog.SpawnDefinition row = rows.get(0);
        assertEquals(200009, row.spawnId());
        assertEquals(10, row.count());
        assertEquals(45126, row.template().npcId());
        assertEquals("$56", row.template().nameId());
        assertEquals(49, row.template().gfxId());
        assertEquals(13, row.template().level());
        assertEquals(150, row.template().hp());
        assertEquals(-1, row.template().armorClass());
        assertEquals(1280, row.template().passispeed());
        assertEquals(1840, row.template().atkspeed());
        assertTrue(row.template().hard());

        ClassicWorldMonsterPopulation population = new ClassicWorldMonsterPopulation();
        assertEquals(ClassicWorldMonsterCatalog.TOTAL_POPULATION, population.size());
        long firstObjectId = ClassicWorldMonsterPopulation.OBJECT_BASE
                | (ClassicWorldMonsterCatalog.PRE_333_TOTAL_POPULATION
                    - ClassicWorldRoyalTrialCatalog.STONE_GOLEM_POPULATION + 1);
        for (int slot = 0;
             slot < ClassicWorldRoyalTrialCatalog.STONE_GOLEM_POPULATION; slot++) {
            ClassicWorldMonsterPopulation.Snapshot actor =
                    population.actor(2, firstObjectId + slot);
            assertNotNull(actor);
            assertEquals(45126, actor.npcId());
            assertEquals(200009, actor.spawnId());
            assertTrue(ClassicWorldMapCatalog.forMap(2)
                    .standingPassable(actor.x(), actor.y()));
        }
        long firstBlackKnight = ClassicWorldMonsterPopulation.OBJECT_BASE
                | (ClassicWorldMonsterCatalog.PRE_332_SELECTED_TOTAL_POPULATION + 1);
        assertEquals(81066, population.actor(4, firstBlackKnight).npcId());
    }

    @Test
    void stoneGolemKeepsEighteenOrdinaryRowsAndSeparateFortyPercentLifeScroll() {
        List<ClassicWorldDropCatalog.DropRule> rules =
                ClassicWorldRoyalTrialCatalog.ordinaryRules(2, 45126);
        assertEquals(18, rules.size());
        ClassicWorldDropCatalog.DropRule adena = rules.stream()
                .filter(rule -> rule.itemId() == InventoryCatalog.ADENA)
                .findFirst().orElseThrow();
        assertEquals(37, adena.minimum());
        assertEquals(72, adena.maximum());
        assertEquals(ClassicWorldDropCatalog.CHANCE_SCALE, adena.chance());
        assertTrue(rules.stream().noneMatch(rule -> rule.itemId() == 40564));

        ClassicWorldDropCatalog.RolledItem rolledAdena =
                ClassicWorldRoyalTrialCatalog.rollOrdinaryDrops(
                        2, 45126, 1.0, 2.0, 1.0, new Random(332))
                        .stream().filter(item ->
                                item.item().itemId() == InventoryCatalog.ADENA)
                        .findFirst().orElseThrow();
        assertTrue(rolledAdena.quantity() >= 74);
        assertTrue(rolledAdena.quantity() <= 144);

        ClassicWorldRoyalTrialCatalog.QuestDropDecision life =
                ClassicWorldRoyalTrialCatalog.decideQuestDrop(
                        45126, 0, java.util.Map.of(10101, 1),
                        java.util.Map.of(), 39);
        assertTrue(life.awarded());
        assertEquals(40564, life.itemId());
        assertFalse(ClassicWorldRoyalTrialCatalog.decideQuestDrop(
                45126, 0, java.util.Map.of(10101, 1),
                java.util.Map.of(), 40).awarded());
        assertNotNull(InventoryCatalog.find(40564));
    }

    private void assertResource(String name, String expected) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = getClass().getResourceAsStream(name)) {
            assertNotNull(input, name);
            digest.update(input.readAllBytes());
        }
        assertEquals(expected,
                HexFormat.of().withUpperCase().formatHex(digest.digest()), name);
    }
}
