package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class ClassicWorldSilentCavernCatalogTest {
    @Test
    void twoMapsAndSixGroundRoutesAppendAfterImmutable343Ids() {
        assertEquals(List.of(304, 305), ClassicWorldSilentCavernCatalog.MAP_IDS);
        for (int mapId : ClassicWorldSilentCavernCatalog.MAP_IDS) {
            ClassicWorldSilentCavernCatalog.MapFlags flags =
                    ClassicWorldSilentCavernCatalog.flags(mapId);
            ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(mapId);
            assertEquals(ClassicWorldSilentCavernCatalog.MAP_SOURCE_SHA256.get(mapId),
                    map.sourceSha256());
            assertEquals(flags.startX(), map.startX());
            assertEquals(flags.endX(), map.endX());
            assertEquals(flags.startY(), map.startY());
            assertEquals(flags.endY(), map.endY());
            assertTrue(flags.escapable() && flags.usableItem() && flags.usableSkill());
            assertTrue(flags.teleportable() && flags.takePets() && flags.recallPets());
            assertTrue(map.allowsEscape());
            assertTrue(map.allowsPetRecall());
            assertTrue(map.allowsRandomTeleport());
        }
        assertEquals(List.of(384, 256), List.of(
                ClassicWorldMapCatalog.forMap(304).width(),
                ClassicWorldMapCatalog.forMap(304).height()));
        assertEquals(List.of(64, 64), List.of(
                ClassicWorldMapCatalog.forMap(305).width(),
                ClassicWorldMapCatalog.forMap(305).height()));

        assertEquals(ClassicWorldDungeonCatalog.TOTAL_ROUTE_COUNT,
                ClassicWorldDungeonCatalog.all().size());
        assertEquals(486, ClassicWorldDungeonCatalog.PRE_378_ROUTE_COUNT);
        List<ClassicWorldDungeonCatalog.Route> routes =
                ClassicWorldDungeonCatalog.all().stream()
                        .filter(ClassicWorldDungeonCatalog::isSilentCavernRoute).toList();
        assertEquals(List.of(400, 401, 402, 403, 404, 405),
                routes.stream().map(ClassicWorldDungeonCatalog.Route::routeId).toList());
        assertTrue(routes.stream().allMatch(
                ClassicWorldDungeonCatalog::destinationLandingSafe));
    }

    @Test
    void selectedRuntimeResourcesExcludeOnlyUnbackedCustomTeleporter() {
        assertEquals(7 + ClassicWorldScarecrowCatalog.MAP304_SPAWN_COUNT,
                ClassicWorldNpcCatalog.all(304).size());
        assertEquals(7, ClassicWorldNpcCatalog.all(305).size());
        assertNull(ClassicWorldNpcCatalog.npc(304, ClassicWorldNpcCatalog.objectId(
                ClassicWorldSilentCavernCatalog.EXCLUDED_CUSTOM_SPAWN_ID)));

        assertEquals(24, ClassicWorldMonsterCatalog.definitions(304).size());
        assertEquals(370, ClassicWorldMonsterCatalog.definitions(304).stream()
                .mapToInt(ClassicWorldMonsterCatalog.SpawnDefinition::count).sum());
        assertTrue(ClassicWorldMonsterCatalog.definitions(305).isEmpty());
        assertEquals(146, ClassicWorldDropCatalog.dropCount(304));
        assertEquals(0, ClassicWorldDropCatalog.dropCount(305));
        assertEquals(ClassicWorldMonsterCatalog.TOTAL_POPULATION,
                new ClassicWorldMonsterPopulation().size());
    }

    @Test
    void textBackedTeleporterShopHealerAndAssassinsMatchExactSpawns() {
        ClassicWorldNpcCatalog.StaticNpc teleporter = npc(304,
                ClassicWorldSilentCavernCatalog.TELEPORTER_SPAWN_ID);
        assertTrue(ClassicWorldSilentCavernCatalog.isTeleporter(teleporter));
        assertEquals(List.of(875, 876, 877, 878, 880, 881),
                ClassicWorldTeleportCatalog.silentCavern().stream()
                        .map(ClassicWorldTeleportCatalog.Route::routeId).toList());
        assertEquals("telescave2", ClassicWorldTeleportCatalog.followUpHtml(
                304, teleporter.objectId(), "teleportURL"));
        assertEquals(List.of("1500", "800", "600", "1800", "1800", "1000", "0"),
                ClassicWorldTeleportCatalog.followUpArguments(
                        304, teleporter.objectId(), "teleportURL"));
        assertNull(ClassicWorldTeleportCatalog.route(
                304, teleporter.objectId(), "teleport scave-silver"));
        assertTrue(ClassicWorldSilentCavernCatalog.isTextOnlyTeleportAction(
                teleporter, "teleport scave-silver"));

        ClassicWorldNpcCatalog.StaticNpc scwaty = npc(304,
                ClassicWorldSilentCavernCatalog.SCWATY_SPAWN_ID);
        ClassicWorldMerchantCatalog.Merchant merchant =
                ClassicWorldMerchantCatalog.find(scwaty);
        assertNotNull(merchant);
        assertEquals(26, merchant.offers().size());
        assertEquals(10, offer(merchant, 40743).packCount());
        assertEquals(5, offer(merchant, 40744).packCount());

        assertTrue(ClassicWorldSilentCavernCatalog.isHadesty(npc(304,
                ClassicWorldSilentCavernCatalog.HADESTY_SPAWN_ID)));
        long assassins = ClassicWorldNpcCatalog.all(305).stream()
                .filter(ClassicWorldSilentCavernCatalog::isDialogueNpc).count();
        assertEquals(6, assassins);
    }

    @Test
    void fiftySixStoryRowsPreserveRepeatExchangeCleanupAndSkillSemantics() {
        Set<Integer> nativeNpcIds = Set.of(
                ClassicWorldSilentCavernCatalog.PEARS_NPC_ID,
                ClassicWorldSilentCavernCatalog.KANGUARD_NPC_ID,
                ClassicWorldSilentCavernCatalog.RONDE_NPC_ID,
                ClassicWorldSilentCavernCatalog.KIMA_NPC_ID,
                ClassicWorldSilentCavernCatalog.BLUDIKA_NPC_ID);
        List<ClassicWorldStoryQuestCatalog.Event> events =
                ClassicWorldStoryQuestCatalog.all().stream()
                        .filter(event -> nativeNpcIds.contains(event.npcId())).toList();
        assertEquals(56, events.size());
        assertTrue(nativeNpcIds.stream().allMatch(ClassicWorldStoryQuestCatalog::hasNpc));

        ClassicWorldStoryQuestCatalog.Event pears = event(events, 1230103);
        assertEquals(List.of(new ClassicWorldStoryQuestCatalog.ItemAmount(40321, 1)),
                pears.consumedItems());
        assertEquals(List.of(new ClassicWorldStoryQuestCatalog.ItemAmount(40738, 1000)),
                pears.givenItems());
        assertTrue(pears.deleteQuestRowOnComplete());
        assertFalse(ClassicWorldStoryQuestCatalog.applyQuestMutation(
                pears, Map.of(pears.questId(), 1)).containsKey(pears.questId()));

        ClassicWorldStoryQuestCatalog.Event rondeSkill = event(events, 40000339);
        assertEquals(List.of(4005, 4006, 4007), rondeSkill.removedSkillEffects());
        assertEquals(Map.entry(4003, 1_500_000), rondeSkill.setSkillEffect());
        ClassicWorldStoryQuestCatalog.Event rondeCleanup = event(events, 40000340);
        assertEquals(7, rondeCleanup.cleanupItems().size());
        assertEquals(40557, rondeCleanup.cleanupItems().get(0).itemId());
    }

    @Test
    void absentGetbackRowsUseExistingMainlandFallbackWithoutFabrication() {
        for (int mapId : ClassicWorldSilentCavernCatalog.MAP_IDS) {
            ClassicWorldGetbackCatalog.Destination destination =
                    ClassicWorldGetbackCatalog.resolve(mapId, 32750, 32800, 0, 0);
            assertEquals(List.of(4, 33089, 33397, 2), List.of(
                    destination.mapId(), destination.x(), destination.y(),
                    destination.townId()));
        }
    }

    private static ClassicWorldNpcCatalog.StaticNpc npc(int mapId, int spawnId) {
        return ClassicWorldNpcCatalog.npc(mapId, ClassicWorldNpcCatalog.objectId(spawnId));
    }

    private static ClassicWorldMerchantCatalog.Offer offer(
            ClassicWorldMerchantCatalog.Merchant merchant, long itemId) {
        return merchant.offers().stream()
                .filter(candidate -> candidate.definition().catalogId() == itemId)
                .findFirst().orElseThrow();
    }

    private static ClassicWorldStoryQuestCatalog.Event event(
            List<ClassicWorldStoryQuestCatalog.Event> events, int id) {
        return events.stream().filter(candidate -> candidate.id() == id)
                .findFirst().orElseThrow();
    }
}
