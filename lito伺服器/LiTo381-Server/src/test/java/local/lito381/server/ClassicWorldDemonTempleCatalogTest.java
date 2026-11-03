package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class ClassicWorldDemonTempleCatalogTest {
    @Test
    void map410AndTwoGroundRoutesAppendAfterImmutable346Ids() {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(410);
        assertEquals(ClassicWorldDemonTempleCatalog.MAP_SOURCE_SHA256,
                map.sourceSha256());
        assertEquals(List.of(32640, 32959, 32704, 33087), List.of(
                map.startX(), map.endX(), map.startY(), map.endY()));
        assertEquals(List.of(320, 384), List.of(map.width(), map.height()));
        assertTrue(map.allowsEscape());
        assertTrue(map.allowsPetRecall());
        assertFalse(map.allowsRandomTeleport());

        List<ClassicWorldDungeonCatalog.Route> routes =
                ClassicWorldDungeonCatalog.all().stream()
                        .filter(ClassicWorldDungeonCatalog::isDemonTempleRoute).toList();
        assertEquals(ClassicWorldDungeonCatalog.TOTAL_ROUTE_COUNT,
                ClassicWorldDungeonCatalog.all().size());
        assertEquals(486, ClassicWorldDungeonCatalog.PRE_378_ROUTE_COUNT);
        assertEquals(List.of(451, 452), routes.stream()
                .map(ClassicWorldDungeonCatalog.Route::routeId).toList());
        assertEquals(List.of(400, 32826, 33001, 410, 32929, 32995, 2),
                routeWire(routes.get(0)));
        assertEquals(List.of(410, 32930, 32995, 400, 32825, 33001, 2),
                routeWire(routes.get(1)));
        assertTrue(routes.stream().allMatch(
                ClassicWorldDungeonCatalog::destinationLandingSafe));
    }

    @Test
    void exactSelectedNpcsMonstersDropsAndAppendedActorsAreRegistered() {
        assertEquals(9, ClassicWorldNpcCatalog.all(410).size());
        assertTrue(ClassicWorldDemonTempleCatalog.isBobo(npc(605)));
        assertTrue(ClassicWorldDemonTempleCatalog.isBasch(npc(1141)));
        assertTrue(ClassicWorldDemonTempleCatalog.isRoi(npc(1297)));
        assertEquals("roi", ClassicWorldDemonTempleCatalog.dialogueHtml(
                npc(1297), 99));
        assertEquals(144, npc(1297).gfxId());
        assertTrue(ClassicWorldDemonTempleCatalog.isRoiTransformSource(
                npc(1297), ClassicWorldDemonTempleCatalog.ROI_CURSED_GFX));
        assertEquals("roi", ClassicWorldDemonTempleCatalog.dialogueHtml(
                npc(1297), 39, ClassicWorldDemonTempleCatalog.ROI_RESTORED_GFX));
        assertEquals("roi1", ClassicWorldDemonTempleCatalog.dialogueHtml(
                npc(1297), 40, ClassicWorldDemonTempleCatalog.ROI_RESTORED_GFX));
        assertTrue(ClassicWorldDemonTempleCatalog.isRestoredRoi(
                npc(1297), ClassicWorldDemonTempleCatalog.ROI_RESTORED_GFX));

        assertEquals(12, ClassicWorldMonsterCatalog.definitions(410).size());
        assertEquals(54, ClassicWorldMonsterCatalog.definitions(410).stream()
                .mapToInt(ClassicWorldMonsterCatalog.SpawnDefinition::count).sum());
        assertEquals(52, ClassicWorldDropCatalog.dropCount(410));
        assertEquals(37, ClassicWorldDropCatalog.items(410).size());

        ClassicWorldMonsterPopulation population = new ClassicWorldMonsterPopulation();
        assertEquals(ClassicWorldMonsterCatalog.TOTAL_POPULATION, population.size());
        long objectId = ClassicWorldMonsterPopulation.OBJECT_BASE
                | (ClassicWorldMonsterCatalog.PRE_378_TOTAL_POPULATION
                    - ClassicWorldMonsterCatalog.PIRATE_DUNGEON_POPULATION
                    - ClassicWorldMonsterCatalog.ELEMENTAL_GRAVE_SLICE_POPULATION
                    - ClassicWorldMonsterCatalog.DEMON_TEMPLE_POPULATION + 1);
        int observed = 0;
        for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                : ClassicWorldMonsterCatalog.definitions(410)) {
            for (int slot = 1; slot <= definition.count(); slot++) {
                ClassicWorldMonsterPopulation.Snapshot actor =
                        population.actor(410, objectId++);
                assertNotNull(actor);
                assertEquals(definition.spawnId(), actor.spawnId());
                assertEquals(slot, actor.spawnNumber());
                assertTrue(ClassicWorldMapCatalog.forMap(410)
                        .standingPassable(actor.x(), actor.y()));
                observed++;
            }
        }
        assertEquals(54, observed);
    }

    @Test
    void boboExposesOnlyThreeSelectedBuyRowsAndNoInventedCrafting() {
        ClassicWorldMerchantCatalog.Merchant merchant =
                ClassicWorldMerchantCatalog.find(npc(605));
        assertNotNull(merchant);
        assertEquals("bobo1", merchant.initialHtml());
        assertEquals("buy", merchant.buyAction());
        assertEquals(List.of(40010L, 40017L, 40018L), merchant.offers().stream()
                .map(offer -> offer.definition().catalogId()).toList());
        assertEquals(List.of(37L, 70L, 3795L), merchant.offers().stream()
                .map(ClassicWorldMerchantCatalog.Offer::unitPrice).toList());
        assertTrue(merchant.offers().stream().allMatch(offer -> offer.packCount() == 1));
        assertTrue(ClassicWorldMerchantCatalog.supportsEmptySell(merchant));
        assertTrue(ClassicWorldCraftCatalog.recipes(410, 80133).isEmpty());
    }

    @Test
    void storyRowsKeepNativeInstanceAndFollowerBoundariesClosed() {
        Set<Integer> npcIds = Set.of(80132, 80013, 80133, 80134, 80135, 80136);
        List<ClassicWorldStoryQuestCatalog.Event> events =
                ClassicWorldStoryQuestCatalog.all().stream()
                        .filter(event -> npcIds.contains(event.npcId())).toList();
        assertEquals(23, events.size());
        assertEquals(3, events.stream().filter(event -> event.npcId() == 80132).count());
        assertEquals(4, events.stream().filter(event -> event.npcId() == 80013).count());
        List<ClassicWorldStoryQuestCatalog.Event> entrances = events.stream()
                .filter(event -> Set.of(80133, 80134, 80135, 80136)
                        .contains(event.npcId())).toList();
        assertEquals(16, entrances.size());
        assertTrue(entrances.stream().filter(event -> event.action().equals("ent"))
                .allMatch(event -> event.parameters().getOrDefault(
                        "instance_id", "").startsWith("CKEW50_")));

        for (int spawnId : List.of(1279, 1280, 1281, 1282)) {
            ClassicWorldNpcCatalog.StaticNpc entrance = npc(spawnId);
            assertTrue(ClassicWorldDemonTempleCatalog.isStoryNpc(entrance));
            assertTrue(ClassicWorldDemonTempleCatalog.isDeferredInstanceAction(
                    entrance, "ent"));
        }
        assertTrue(ClassicWorldDemonTempleCatalog.isDeferredRoiFollowerAction(
                npc(1297), "start"));
        assertNull(ClassicWorldTeleportCatalog.source(410, npc(1181).objectId()));
    }

    @Test
    void absentGetbackUsesExistingAdenFallbackWithoutFabricatingASelectedRow() {
        assertEquals(new ClassicWorldGetbackCatalog.Destination(
                        4, 33089, 33397, 2),
                ClassicWorldGetbackCatalog.resolve(410, 32850, 32950, 0, 0));
    }

    @Test
    void transformedRoiPutObjectKeepsIdentityAndUsesRuntimeGfx() throws Exception {
        ClassicWorldNpcCatalog.StaticNpc roi = npc(1297);
        Protocol.Reader actor = new Protocol.Reader(Protocol.classicStaticNpc(
                roi, roi.heading(), ClassicWorldDemonTempleCatalog.ROI_RESTORED_GFX));
        assertEquals(Protocol.S_PUT_OBJECT, actor.c());
        assertEquals(roi.x(), actor.h());
        assertEquals(roi.y(), actor.h());
        assertEquals(roi.objectId(), actor.d());
        assertEquals(ClassicWorldDemonTempleCatalog.ROI_RESTORED_GFX, actor.h());
        assertEquals(0, actor.c());
        assertEquals(roi.heading(), actor.c());
    }

    private static ClassicWorldNpcCatalog.StaticNpc npc(int spawnId) {
        ClassicWorldNpcCatalog.StaticNpc npc = ClassicWorldNpcCatalog.npc(
                410, ClassicWorldNpcCatalog.objectId(spawnId));
        assertNotNull(npc);
        return npc;
    }

    private static List<Integer> routeWire(ClassicWorldDungeonCatalog.Route route) {
        return List.of(route.sourceMapId(), route.sourceX(), route.sourceY(),
                route.destinationMapId(), route.destinationX(), route.destinationY(),
                route.destinationHeading());
    }
}
