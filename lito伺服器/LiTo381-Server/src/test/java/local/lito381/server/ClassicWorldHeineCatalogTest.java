package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ClassicWorldHeineCatalogTest {
    @Test
    void tenMapsAndTwentyEightGroundRoutesAppendWithoutChangingOlderIds() {
        assertEquals(List.of(59, 60, 61, 62, 63, 64, 65, 252, 253, 254),
                ClassicWorldHeineCatalog.MAP_IDS);
        for (int mapId : ClassicWorldHeineCatalog.MAP_IDS) {
            ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(mapId);
            assertEquals(ClassicWorldHeineCatalog.MAP_SOURCE_SHA256.get(mapId),
                    map.sourceSha256());
            assertTrue(map.allowsEscape());
        }
        assertEquals(ClassicWorldDungeonCatalog.TOTAL_ROUTE_COUNT,
                ClassicWorldDungeonCatalog.all().size());
        assertEquals(486, ClassicWorldDungeonCatalog.PRE_378_ROUTE_COUNT);
        assertEquals(28, ClassicWorldDungeonCatalog.all().stream()
                .filter(ClassicWorldDungeonCatalog::isHeineEvaRoute).count());
        assertEquals(List.of(372, 399), List.of(
                ClassicWorldDungeonCatalog.all().get(371).routeId(),
                ClassicWorldDungeonCatalog.all().get(398).routeId()));
    }

    @Test
    void selectedWorldResourcesRetainExactPopulationAndActorContracts() {
        int population = 0;
        for (int mapId : ClassicWorldHeineCatalog.MAP_IDS) {
            ClassicWorldHeineCatalog.SourceContract npc =
                    ClassicWorldHeineCatalog.NPC_CONTRACTS.get(mapId);
            ClassicWorldHeineCatalog.SourceContract monster =
                    ClassicWorldHeineCatalog.MONSTER_CONTRACTS.get(mapId);
            ClassicWorldHeineCatalog.SourceContract drop =
                    ClassicWorldHeineCatalog.DROP_CONTRACTS.get(mapId);
            assertEquals(npc.rows(), ClassicWorldNpcCatalog.all(mapId).size());
            assertEquals(monster.rows(),
                    ClassicWorldMonsterCatalog.definitions(mapId).size());
            assertEquals(drop.rows(), ClassicWorldDropCatalog.dropCount(mapId));
            population += monster.population();
        }
        assertEquals(779, population);
        assertEquals(ClassicWorldMonsterCatalog.TOTAL_POPULATION,
                new ClassicWorldMonsterPopulation().size());
    }

    @Test
    void teleporterShopCraftAndStoryActorsMatchSelectedSpawns() {
        ClassicWorldNpcCatalog.StaticNpc teleporter = ClassicWorldNpcCatalog.npc(
                64, ClassicWorldNpcCatalog.objectId(
                        ClassicWorldHeineCatalog.TELEPORTER_SPAWN_ID));
        assertTrue(ClassicWorldHeineCatalog.isTeleporter(teleporter));
        assertEquals(List.of(853, 854, 855),
                ClassicWorldTeleportCatalog.heineCastle().stream()
                        .map(ClassicWorldTeleportCatalog.Route::routeId).toList());
        assertEquals("telecheine2", ClassicWorldTeleportCatalog.followUpHtml(
                64, teleporter.objectId(), "teleportURL"));
        assertEquals(List.of("0", "0", "0"),
                ClassicWorldTeleportCatalog.followUpArguments(
                        64, teleporter.objectId(), "teleportURL"));

        ClassicWorldNpcCatalog.StaticNpc crafter = ClassicWorldNpcCatalog.npc(
                62, ClassicWorldNpcCatalog.objectId(
                        ClassicWorldHeineCatalog.EVA_CRAFTER_SPAWN_ID));
        assertTrue(ClassicWorldHeineCatalog.isCrafter(crafter));
        assertEquals(4, ClassicWorldCraftCatalog.recipes(
                62, ClassicWorldHeineCatalog.EVA_CRAFTER_NPC_ID).size());
        assertTrue(ClassicWorldCraftCatalog.recipes(
                62, ClassicWorldHeineCatalog.EVA_CRAFTER_NPC_ID).stream()
                .allMatch(recipe -> ClassicWorldCraftCatalog.matchesSource(crafter, recipe)));

        ClassicWorldNpcCatalog.StaticNpc shop = ClassicWorldNpcCatalog.npc(
                63, ClassicWorldNpcCatalog.objectId(
                        ClassicWorldHeineCatalog.BUAKHEU_SPAWN_ID));
        ClassicWorldMerchantCatalog.Merchant merchant =
                ClassicWorldMerchantCatalog.find(shop);
        assertNotNull(merchant);
        assertEquals(15, merchant.offers().size());
        assertEquals(5, merchant.offers().stream()
                .filter(offer -> offer.definition().catalogId() == 40744)
                .findFirst().orElseThrow().packCount());

        ClassicWorldNpcCatalog.StaticNpc story = ClassicWorldNpcCatalog.npc(
                62, ClassicWorldNpcCatalog.objectId(
                        ClassicWorldHeineCatalog.ZYBRIL_SPAWN_ID));
        assertTrue(ClassicWorldHeineCatalog.isStoryNpc(story));
        assertTrue(ClassicWorldStoryQuestCatalog.hasNpc(
                ClassicWorldHeineCatalog.ZYBRIL_NPC_ID));
    }

    @Test
    void castleDoorsAndGetbackRulesAreServerOwnedAndSourceExact() {
        ClassicWorldHeineCastleDoorState state =
                new ClassicWorldHeineCastleDoorState();
        assertEquals(3, state.snapshots().size());
        assertTrue(state.snapshots().stream().noneMatch(
                ClassicWorldHeineCastleDoorState.Snapshot::open));
        ClassicWorldHeineCatalog.Door inner = ClassicWorldHeineCatalog.doors().get(0);
        assertTrue(state.blocks(inner.edgeLow(), inner.y() + 1));
        assertEquals(1, state.setOpenForKeeper(inner.keeperNpcId(), true).size());
        assertFalse(state.blocks(inner.edgeLow(), inner.y() + 1));
        assertEquals(1, state.repairAll());

        for (int mapId = 59; mapId <= 61; mapId++) {
            ClassicWorldGetbackCatalog.Destination destination =
                    ClassicWorldGetbackCatalog.resolve(mapId, 32700, 32800, 0, 0);
            assertEquals(List.of(4, 8),
                    List.of(destination.mapId(), destination.townId()));
        }
        for (int mapId = 62; mapId <= 65; mapId++) {
            ClassicWorldGetbackCatalog.Destination destination =
                    ClassicWorldGetbackCatalog.resolve(mapId, 32700, 32800, 0, 0);
            assertEquals(List.of(4, 8),
                    List.of(destination.mapId(), destination.townId()));
        }
    }
}
