package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ClassicWorldIvoryTowerCatalogTest {
    @Test
    void selectedMapsKeepExactBoundsFlagsClientAndResourceHashes() throws Exception {
        assertEquals(List.of(75, 76, 77, 78, 79, 80, 81, 82),
                ClassicWorldIvoryTowerCatalog.MAP_IDS);
        for (int mapId : ClassicWorldIvoryTowerCatalog.MAP_IDS) {
            ClassicWorldIvoryTowerCatalog.MapFlags flags =
                    ClassicWorldIvoryTowerCatalog.flags(mapId);
            ClassicWorldMapCatalog.ClassicMap map =
                    ClassicWorldMapCatalog.forMap(mapId);
            assertEquals(flags.startX(), map.startX());
            assertEquals(flags.endX(), map.endX());
            assertEquals(flags.startY(), map.startY());
            assertEquals(flags.endY(), map.endY());
            assertEquals(ClassicWorldIvoryTowerCatalog.MAP_SOURCE_SHA256.get(mapId),
                    map.sourceSha256());
            assertTrue(map.allowsEscape());
            assertTrue(map.allowsPetRecall());
            assertEquals(flags.teleportable(), map.allowsRandomTeleport());
            assertEquals(ClassicWorldIvoryTowerCatalog.MAP_RESOURCE_SHA256.get(mapId),
                    resourceHash("/maps/" + mapId + ".l38.gz"));
            assertEquals(64,
                    ClassicWorldIvoryTowerCatalog.CLIENT_MAP_SHA256.get(mapId).length());
        }
        assertEquals(192, ClassicWorldMapCatalog.forMap(75).height());
        assertFalse(ClassicWorldMapCatalog.forMap(80).allowsRandomTeleport());
        assertFalse(ClassicWorldMapCatalog.forMap(81).allowsRandomTeleport());
        assertFalse(ClassicWorldMapCatalog.forMap(82).allowsRandomTeleport());
    }

    @Test
    void selectedGroundRoutesAreExactAndAllLandingsCanBeExited() {
        List<ClassicWorldDungeonCatalog.Route> routes =
                ClassicWorldDungeonCatalog.all().stream()
                        .filter(ClassicWorldDungeonCatalog::isIvoryTowerRoute)
                        .toList();
        assertEquals(ClassicWorldIvoryTowerCatalog.ROUTE_COUNT, routes.size());
        assertEquals(List.of(273, 274, 275, 276), routes.subList(0, 4).stream()
                .map(ClassicWorldDungeonCatalog.Route::routeId).toList());
        assertEquals(314, routes.get(routes.size() - 1).routeId());
        assertEquals(75, ClassicWorldDungeonCatalog.find(4, 34041, 32154)
                .destinationMapId());
        assertEquals(4, ClassicWorldDungeonCatalog.find(75, 32763, 32903)
                .destinationMapId());
        assertEquals(82, ClassicWorldDungeonCatalog.find(81, 32733, 32793)
                .destinationMapId());
        for (ClassicWorldDungeonCatalog.Route route : routes) {
            assertTrue(ClassicWorldDungeonCatalog.destinationLandingSafe(route),
                    "route " + route.routeId());
        }
        assertEquals(ClassicWorldIvoryTowerCatalog.ROUTES_SOURCE_SHA256,
                ClassicWorldDungeonCatalog.IVORY_TOWER_SOURCE_SHA256);
    }

    @Test
    void selectedNpcMonsterAndDropResourcesKeepEveryPerMapContract()
            throws Exception {
        int npcRows = 0;
        int monsterRows = 0;
        int population = 0;
        int drops = 0;
        for (int mapId : ClassicWorldIvoryTowerCatalog.MAP_IDS) {
            ClassicWorldIvoryTowerCatalog.SourceContract npc =
                    ClassicWorldIvoryTowerCatalog.NPC_CONTRACTS.get(mapId);
            ClassicWorldIvoryTowerCatalog.SourceContract monster =
                    ClassicWorldIvoryTowerCatalog.MONSTER_CONTRACTS.get(mapId);
            ClassicWorldIvoryTowerCatalog.SourceContract drop =
                    ClassicWorldIvoryTowerCatalog.DROP_CONTRACTS.get(mapId);
            assertEquals(npc.rows(), ClassicWorldNpcCatalog.all(mapId).size());
            assertEquals(monster.rows(),
                    ClassicWorldMonsterCatalog.definitions(mapId).size());
            assertEquals(monster.population(), ClassicWorldMonsterCatalog
                    .definitions(mapId).stream().mapToInt(
                            ClassicWorldMonsterCatalog.SpawnDefinition::count).sum());
            assertEquals(drop.rows(), ClassicWorldDropCatalog.dropCount(mapId));
            assertEquals(drop.population(), ClassicWorldDropCatalog.items(mapId).size());
            assertEquals(npc.resourceSha256(), resourceHash(
                    "/world/map" + mapId + "-static-npcs.lwn.gz"));
            assertEquals(monster.resourceSha256(), resourceHash(
                    "/world/map" + mapId + "-monsters.lwm.gz"));
            assertEquals(drop.resourceSha256(), resourceHash(
                    "/world/map" + mapId + "-drops.lwd.gz"));
            npcRows += npc.rows();
            monsterRows += monster.rows();
            population += monster.population();
            drops += drop.rows();
        }
        assertEquals(7, npcRows);
        assertEquals(21, monsterRows);
        assertEquals(ClassicWorldIvoryTowerCatalog.MONSTER_POPULATION, population);
        assertEquals(ClassicWorldIvoryTowerCatalog.DROP_ROW_COUNT, drops);
    }

    @Test
    void selectedServicesKeepExactShopStoryAndExplicitSourceGaps()
            throws Exception {
        List<ClassicWorldIvoryTowerCatalog.ShopOffer> rinda =
                ClassicWorldIvoryTowerCatalog.shopOffers(
                        ClassicWorldIvoryTowerCatalog.RINDA_NPC_ID);
        List<ClassicWorldIvoryTowerCatalog.ShopOffer> pagoru =
                ClassicWorldIvoryTowerCatalog.shopOffers(
                        ClassicWorldIvoryTowerCatalog.PAGORU_NPC_ID);
        assertEquals(9, rinda.size());
        assertEquals(29, pagoru.size());
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 8, 6, 7),
                rinda.stream().map(
                        ClassicWorldIvoryTowerCatalog.ShopOffer::sourceOrderId).toList());
        assertEquals(java.util.stream.IntStream.range(0, 9).boxed().toList(),
                rinda.stream().map(
                        ClassicWorldIvoryTowerCatalog.ShopOffer::wireOrderId).toList());
        assertTrue(rinda.stream().allMatch(offer -> offer.packCount() == 1
                && offer.purchasingPrice() == -1));
        assertTrue(pagoru.stream().allMatch(offer -> offer.packCount() == 1
                && offer.purchasingPrice() == -1));
        assertTrue(ClassicWorldIvoryTowerCatalog.shopOffers(
                ClassicWorldIvoryTowerCatalog.VARYETH_NPC_ID).isEmpty());
        assertTrue(ClassicWorldIvoryTowerCatalog.shopOffers(
                ClassicWorldIvoryTowerCatalog.KREISTER_NPC_ID).isEmpty());
        assertEquals(ClassicWorldIvoryTowerCatalog.SHOP_RESOURCE_SHA256,
                resourceHash("/world/ivory-tower-shops.lws.gz"));

        long dicardingTalk = ClassicWorldStoryQuestCatalog.all().stream()
                .filter(event -> event.npcId()
                        == ClassicWorldIvoryTowerCatalog.DICARDING_NPC_ID)
                .filter(event -> event.eventType()
                        == ClassicWorldStoryQuestCatalog.EventType.NPC_TALK).count();
        long dicardingAction = ClassicWorldStoryQuestCatalog.all().stream()
                .filter(event -> event.npcId()
                        == ClassicWorldIvoryTowerCatalog.DICARDING_NPC_ID)
                .filter(event -> event.eventType()
                        == ClassicWorldStoryQuestCatalog.EventType.NPC_ACTION).count();
        long pietaTalk = ClassicWorldStoryQuestCatalog.all().stream()
                .filter(event -> event.npcId()
                        == ClassicWorldIvoryTowerCatalog.PIETA_NPC_ID)
                .filter(event -> event.eventType()
                        == ClassicWorldStoryQuestCatalog.EventType.NPC_TALK).count();
        long pietaAction = ClassicWorldStoryQuestCatalog.all().stream()
                .filter(event -> event.npcId()
                        == ClassicWorldIvoryTowerCatalog.PIETA_NPC_ID)
                .filter(event -> event.eventType()
                        == ClassicWorldStoryQuestCatalog.EventType.NPC_ACTION).count();
        assertEquals(45, dicardingTalk);
        assertEquals(23, dicardingAction);
        assertEquals(3, pietaTalk);
        assertEquals(2, pietaAction);
        assertEquals(ClassicWorldIvoryTowerCatalog.STORY_ROW_COUNT,
                dicardingTalk + dicardingAction + pietaTalk + pietaAction);
    }

    @Test
    void selectedStaticNpcBindingsMatchNativeHtmlAndShopCapabilities() {
        ClassicWorldNpcCatalog.StaticNpc pagoru = ClassicWorldNpcCatalog.npc(
                75, ClassicWorldNpcCatalog.objectId(1306));
        ClassicWorldNpcCatalog.StaticNpc rinda = ClassicWorldNpcCatalog.npc(
                75, ClassicWorldNpcCatalog.objectId(1323));
        ClassicWorldNpcCatalog.StaticNpc ellyonne = ClassicWorldNpcCatalog.npc(
                76, ClassicWorldNpcCatalog.objectId(760));
        ClassicWorldNpcCatalog.StaticNpc pieta = ClassicWorldNpcCatalog.npc(
                77, ClassicWorldNpcCatalog.objectId(1137));
        assertNotNull(ClassicWorldMerchantCatalog.find(pagoru));
        assertNotNull(ClassicWorldMerchantCatalog.find(rinda));
        assertEquals("pagoru", pagoru.normalAction());
        assertEquals("rinda", rinda.normalAction());
        assertTrue(ClassicWorldIvoryTowerCatalog.isEllyonne(ellyonne));
        assertTrue(ClassicWorldIvoryTowerCatalog.isStoryNpc(pieta));
    }

    private String resourceHash(String name) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream(name)) {
            assertNotNull(in, name);
            digest.update(in.readAllBytes());
        }
        return HexFormat.of().withUpperCase().formatHex(digest.digest());
    }
}
