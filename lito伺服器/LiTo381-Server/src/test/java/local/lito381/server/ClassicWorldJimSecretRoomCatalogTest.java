package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class ClassicWorldJimSecretRoomCatalogTest {
    @Test void exactMapResourcesNpcAndEmptyCombatSliceAreRegistered() throws Exception {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(237);
        assertEquals(32704, map.startX());
        assertEquals(32767, map.endX());
        assertEquals(32768, map.startY());
        assertEquals(32831, map.endY());
        assertEquals(64, map.width());
        assertEquals(64, map.height());
        assertEquals(ClassicWorldMapCatalog.MAP237_SOURCE_SHA256, map.sourceSha256());
        assertTrue(map.standingPassable(32736, 32807));
        assertTrue(map.standingPassable(32746, 32807));
        assertTrue(map.allowsEscape());
        assertTrue(map.allowsPetRecall());
        assertFalse(map.allowsRandomTeleport());

        assertResource("/maps/237.l38.gz", ClassicWorldMapCatalog.MAP237_RESOURCE_SHA256);
        assertResource("/world/map237-static-npcs.lwn.gz",
                ClassicWorldNpcCatalog.MAP237_RESOURCE_SHA256);
        assertResource("/world/map237-monsters.lwm.gz",
                ClassicWorldMonsterCatalog.MAP237_RESOURCE_SHA256);
        assertResource("/world/map237-drops.lwd.gz",
                ClassicWorldDropCatalog.MAP237_RESOURCE_SHA256);

        assertEquals(1, ClassicWorldNpcCatalog.all(237).size());
        ClassicWorldNpcCatalog.StaticNpc jim = ClassicWorldNpcCatalog.all(237).get(0);
        assertEquals(1144, jim.spawnId());
        assertEquals(0x6400_0478L, jim.objectId());
        assertEquals(70555, jim.npcId());
        assertEquals(32746, jim.x());
        assertEquals(32807, jim.y());
        assertEquals(2436, jim.gfxId());
        assertEquals("jim4", jim.normalAction());
        assertEquals(jim, ClassicWorldNpcCatalog.npc(237, jim.objectId()));

        assertTrue(ClassicWorldMonsterCatalog.definitions(237).isEmpty());
        assertEquals(0, ClassicWorldMonsterCatalog.definitions(237).stream()
                .mapToInt(ClassicWorldMonsterCatalog.SpawnDefinition::count).sum());
        assertEquals(0, ClassicWorldDropCatalog.dropCount(237));
        assertEquals(0, ClassicWorldDropCatalog.items(237).size());
    }

    @Test void exactMap13Map237PairGetbackAndSecretKeyRouteAreClosed() {
        ClassicWorldDungeonCatalog.Route enter =
                ClassicWorldDungeonCatalog.find(13, 32813, 32810);
        assertNotNull(enter);
        assertEquals(199, enter.routeId());
        assertEquals(237, enter.destinationMapId());
        assertEquals(32736, enter.destinationX());
        assertEquals(32807, enter.destinationY());
        assertEquals(5, enter.destinationHeading());
        ClassicWorldDungeonCatalog.Route leave =
                ClassicWorldDungeonCatalog.find(237, 32736, 32807);
        assertNotNull(leave);
        assertEquals(200, leave.routeId());
        assertEquals(13, leave.destinationMapId());
        assertEquals(32813, leave.destinationX());
        assertEquals(32810, leave.destinationY());
        assertEquals(2, leave.destinationHeading());

        ClassicWorldGetbackCatalog.Destination getback =
                ClassicWorldGetbackCatalog.resolve(237, 32746, 32807, 1, 0);
        assertEquals(new ClassicWorldGetbackCatalog.Destination(
                4, 32628, 32807, 3), getback);

        ClassicWorldCustomAreaTeleportCatalog.Route key =
                ClassicWorldCustomAreaTeleportCatalog.route(
                        InventoryCatalog.SECRET_ROOM_KEY);
        assertNotNull(key);
        assertTrue(key.matches(13, 32806, 32798));
        assertTrue(key.matches(13, 32814, 32807));
        assertFalse(key.matches(13, 32805, 32798));
        assertFalse(key.matches(237, 32810, 32803));
        assertEquals(13, key.destinationMapId());
        assertEquals(32815, key.destinationX());
        assertEquals(32810, key.destinationY());
        assertEquals(5, key.destinationHeading());
        assertTrue(ClassicWorldMapCatalog.forMap(13).standingPassable(
                key.destinationX(), key.destinationY()));
        assertTrue(ClassicWorldMapCatalog.forMap(13).standingPassable(
                enter.sourceX(), enter.sourceY()));
    }

    @Test void selectedJimStoryRequiresSkeletonAndExchangesExactQuestItems() {
        assertEquals(ClassicWorldStoryQuestCatalog.JIM_ROW_COUNT,
                ClassicWorldStoryQuestCatalog.all().stream()
                        .filter(row -> row.npcId()
                                == ClassicWorldStoryQuestCatalog.JIM_NPC_ID)
                        .count());

        Map<Integer, Integer> rescue = Map.of(10602, 1, 10701, 0);
        ClassicWorldStoryQuestCatalog.Dialogue outsideSkeleton =
                ClassicWorldStoryQuestCatalog.dialogue(70555, 1, 30, 61,
                        0, 2436, rescue, Map.of());
        assertNotNull(outsideSkeleton);
        assertEquals(1070106, outsideSkeleton.event().id());
        assertEquals("jim1", outsideSkeleton.html());
        assertEquals(rescue, outsideSkeleton.questSteps());

        ClassicWorldStoryQuestCatalog.Dialogue rescued =
                ClassicWorldStoryQuestCatalog.dialogue(70555, 1, 30, 2374,
                        0, 2436, rescue, Map.of());
        assertEquals(1070104, rescued.event().id());
        assertEquals("jim2", rescued.html());
        assertEquals(1, rescued.questSteps().get(10701));
        assertEquals(0, rescued.questSteps().get(10702));

        Map<Integer, Integer> revival = Map.of(10703, 1, 10704, 0);
        ClassicWorldStoryQuestCatalog.Dialogue waitingForPotion =
                ClassicWorldStoryQuestCatalog.dialogue(70555, 1, 30, 2374,
                        0, 2436, revival, Map.of());
        assertNotNull(waitingForPotion);
        assertEquals(1070402, waitingForPotion.event().id());
        assertEquals("jim2", waitingForPotion.html());
        ClassicWorldStoryQuestCatalog.ActionResolution missing =
                ClassicWorldStoryQuestCatalog.actionResolution(
                        70555, "request letter of gratitude", 1, 30, 2374,
                        0, 2436, revival, Map.of());
        assertNotNull(missing);
        assertFalse(missing.admitted());
        assertEquals(1070404, missing.event().id());
        assertEquals(InventoryCatalog.POTION_OF_REVIVAL,
                missing.missingItems().get(0).itemId());

        ClassicWorldStoryQuestCatalog.ActionResolution exchanged =
                ClassicWorldStoryQuestCatalog.actionResolution(
                        70555, "request letter of gratitude", 1, 30, 2374,
                        0, 2436, revival,
                        Map.of(InventoryCatalog.POTION_OF_REVIVAL, 1));
        assertTrue(exchanged.admitted());
        assertEquals(InventoryCatalog.POTION_OF_REVIVAL,
                exchanged.event().consumedItems().get(0).itemId());
        assertEquals(InventoryCatalog.LETTER_OF_GRATITUDE,
                exchanged.event().givenItems().get(0).itemId());
        Map<Integer, Integer> completed =
                ClassicWorldStoryQuestCatalog.applyQuestMutation(
                        exchanged.event(), revival);
        assertEquals(1, completed.get(10704));
        assertEquals(0, completed.get(10705));
        assertEquals("jimcg", ClassicWorldStoryQuestCatalog.responseHtml(
                exchanged.event(), completed));
        assertNull(ClassicWorldStoryQuestCatalog.actionResolution(
                70555, "request letter of gratitude", 1, 30, 2374,
                0, 2436, completed,
                Map.of(InventoryCatalog.POTION_OF_REVIVAL, 1)));
    }

    @Test void selectedQuestItemWireDefinitionsAreExact() {
        InventoryCatalog.Definition key = InventoryCatalog.require(
                InventoryCatalog.SECRET_ROOM_KEY);
        assertEquals(0, key.useType());
        assertEquals(59, key.icon());
        assertEquals(137, key.groundGfx());
        assertEquals("$2022", key.name());
        assertEquals(3_000, key.unitWeight());
        assertEquals(InventoryCatalog.Effect.CUSTOM_AREA_TELEPORT, key.effect());
        assertFalse(key.stackable());
        assertFalse(key.tradable());

        InventoryCatalog.Definition potion = InventoryCatalog.require(
                InventoryCatalog.POTION_OF_REVIVAL);
        assertEquals(0xFF, potion.useType());
        assertEquals(947, potion.icon());
        assertEquals(5925, potion.groundGfx());
        assertEquals("$2025", potion.name());
        InventoryCatalog.Definition letter = InventoryCatalog.require(
                InventoryCatalog.LETTER_OF_GRATITUDE);
        assertEquals(917, letter.icon());
        assertEquals(3963, letter.groundGfx());
        assertEquals("$2026", letter.name());
    }

    private static void assertResource(String name, String expected) throws Exception {
        try (InputStream in = ClassicWorldJimSecretRoomCatalogTest.class
                .getResourceAsStream(name)) {
            assertNotNull(in, name);
            String actual = HexFormat.of().withUpperCase().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(in.readAllBytes()));
            assertEquals(expected, actual, name);
        }
    }
}
