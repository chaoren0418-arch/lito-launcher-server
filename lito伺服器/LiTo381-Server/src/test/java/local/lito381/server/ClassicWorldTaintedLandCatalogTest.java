package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Complete selected-source contract for map244 Tainted Land. */
final class ClassicWorldTaintedLandCatalogTest {
    @Test
    void selectedMapKeepsExactBoundsFlagsCollisionAndResource() throws Exception {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(244);
        assertEquals(32704, map.startX());
        assertEquals(33023, map.endX());
        assertEquals(32768, map.startY());
        assertEquals(33087, map.endY());
        assertEquals(320, map.width());
        assertEquals(320, map.height());
        assertEquals(ClassicWorldMapCatalog.MAP244_SOURCE_SHA256,
                map.sourceSha256());
        assertEquals("8AA4FB6AFEAFD7426F81F11819CC82C395A5C0CA9D913CF357FAF352227496F6",
                ClassicWorldMapCatalog.MAP244_FLAGS_SOURCE_SHA256);
        assertTrue(map.allowsEscape());
        assertTrue(map.allowsPetRecall());
        assertTrue(map.standingPassable(32982, 32808));
        assertEquals("25473DFD9A1D05C74A5062E31E21A79A08F3F6714239E566C76BD612838EB75A",
                resourceHash("/maps/244.l38.gz"));
    }

    @Test
    void selectedGroundRoutesKeepAllFourInternalTransitions() {
        assertEquals("0EFFFB4161CAE4D770D04B26A0F5F30594419EB547A6C52B6D908528A4C19EED",
                ClassicWorldDungeonCatalog.TAINTED_LAND_SOURCE_SHA256);
        List<ClassicWorldDungeonCatalog.Route> routes =
                ClassicWorldDungeonCatalog.all().stream()
                        .filter(route -> route.routeId() >= 127
                                && route.routeId() <= 130)
                        .toList();
        assertEquals(4, routes.size());
        assertEquals(List.of(32752, 32752, 32880, 32880), routes.stream()
                .map(ClassicWorldDungeonCatalog.Route::sourceX).toList());
        assertEquals(List.of(32812, 32813, 32907, 32908), routes.stream()
                .map(ClassicWorldDungeonCatalog.Route::sourceY).toList());
        assertEquals(List.of(32842, 32842, 32982, 32982), routes.stream()
                .map(ClassicWorldDungeonCatalog.Route::destinationX).toList());
        assertEquals(List.of(32917, 32917, 32808, 32808), routes.stream()
                .map(ClassicWorldDungeonCatalog.Route::destinationY).toList());
        for (ClassicWorldDungeonCatalog.Route route : routes) {
            assertEquals(244, route.sourceMapId());
            assertEquals(244, route.destinationMapId());
            assertEquals(0, route.destinationHeading());
            assertEquals(0, route.requiredItemCatalogId());
            assertFalse(route.scheduledPirateShip());
            assertTrue(ClassicWorldMapCatalog.forMap(244).contains(
                    route.sourceX(), route.sourceY()));
            assertTrue(ClassicWorldMapCatalog.forMap(244).standingPassable(
                    route.destinationX(), route.destinationY()));
        }
    }

    @Test
    void selectedNpcRoutesPreserveTwoStageEntryAndDirectExit() {
        assertEquals("F51A82C26062D6842C18BAC1D424E9346258F12235CF1E3A4CC89D6CA512CCCC",
                ClassicWorldTeleportCatalog.TAINTED_LAND_ROUTES_SOURCE_SHA256);
        List<ClassicWorldTeleportCatalog.Route> routes =
                ClassicWorldTeleportCatalog.taintedLand();
        assertEquals(2, routes.size());

        ClassicWorldTeleportCatalog.Route entry = routes.get(0);
        assertRoute(entry, 4, 935, 71039, 32777, 32298,
                "orcfbuwoo1", 1156, "teleport orcfcd",
                244, 32982, 32808, 5, 10_000);
        assertEquals("orcfbuwoo2", ClassicWorldTeleportCatalog.followUpHtml(
                4, entry.sourceObjectId(), "teleportURL"));
        assertEquals("orcfbuwoo2", ClassicWorldTeleportCatalog.followUpHtml(
                4, entry.sourceObjectId(), "teleporturl"));
        assertNull(ClassicWorldTeleportCatalog.followUpHtml(
                4, entry.sourceObjectId(), "teleport orcfcd"));

        ClassicWorldTeleportCatalog.Route exit = routes.get(1);
        assertRoute(exit, 244, 936, 71045, 32992, 32814,
                "orcfbawoo1", 1157, "1", 4, 32750, 32435, 5, 1);
        for (ClassicWorldTeleportCatalog.Route route : routes) {
            ClassicWorldNpcCatalog.StaticNpc npc = ClassicWorldNpcCatalog.npc(
                    route.sourceMapId(), route.sourceObjectId());
            assertNotNull(npc);
            assertEquals(route.sourceNpcId(), npc.npcId());
            assertEquals(route.initialHtml(), npc.initialHtml(0));
            assertTrue(npc.fixedMerchant());
            assertEquals(route, ClassicWorldTeleportCatalog.source(
                    route.sourceMapId(), route.sourceObjectId()));
            assertEquals(route, ClassicWorldTeleportCatalog.route(
                    route.sourceMapId(), route.sourceObjectId(), route.action()));
        }
    }

    @Test
    void selectedStaticNpcMonsterAndDropResourcesRemainHashLocked()
            throws Exception {
        assertEquals(1, ClassicWorldNpcCatalog.all(244).size());
        ClassicWorldNpcCatalog.StaticNpc exit = ClassicWorldNpcCatalog.all(244).get(0);
        assertEquals(936, exit.spawnId());
        assertEquals(71045, exit.npcId());
        assertEquals("$4664", exit.nameId());
        assertEquals(5815, exit.gfxId());
        assertEquals("L1Merchant", exit.implementation());
        assertEquals("orcfbawoo1", exit.normalAction());
        assertEquals(ClassicWorldNpcCatalog.MAP244_RESOURCE_SHA256,
                resourceHash("/world/map244-static-npcs.lwn.gz"));

        List<ClassicWorldMonsterCatalog.SpawnDefinition> monsters =
                ClassicWorldMonsterCatalog.definitions(244);
        assertEquals(22, monsters.size());
        assertEquals(400, monsters.stream()
                .mapToInt(ClassicWorldMonsterCatalog.SpawnDefinition::count).sum());
        assertEquals(1939, monsters.get(0).spawnId());
        assertEquals(45770, monsters.get(0).template().npcId());
        assertEquals(1960, monsters.get(21).spawnId());
        assertEquals(45793, monsters.get(21).template().npcId());
        assertEquals(ClassicWorldMonsterCatalog.MAP244_RESOURCE_SHA256,
                resourceHash("/world/map244-monsters.lwm.gz"));

        assertEquals(64, ClassicWorldDropCatalog.dropCount(244));
        assertEquals(33, ClassicWorldDropCatalog.items(244).size());
        assertEquals(ClassicWorldDropCatalog.MAP244_RESOURCE_SHA256,
                resourceHash("/world/map244-drops.lwd.gz"));
        assertEquals(List.of(128, 129, 131, 41084), ClassicWorldDropCatalog
                .rules(244, 45770).stream()
                .map(ClassicWorldDropCatalog.DropRule::itemId).toList());
        for (ClassicWorldDropCatalog.ItemWire item
                : ClassicWorldDropCatalog.items(244)) {
            assertNotNull(item.inventoryDefinition());
            assertNotNull(InventoryCatalog.find(item.itemId()));
        }
    }

    @Test
    void selectedProofWeaponKeepsTheIdenticalRetainedItem68Row() {
        assertEquals("C8A12A282EF5430040C5D9CD9AB67255B0C8F9AA142D59242B90F23D162070BE",
                InventoryCatalog.ANCIENT_SWORD_SOURCE_SHA256);
        InventoryCatalog.Definition definition = InventoryCatalog.require(
                InventoryCatalog.ANCIENT_SWORD);
        assertEquals(1, definition.useType());
        assertEquals(902, definition.icon());
        assertEquals(26, definition.groundGfx());
        assertEquals(0, definition.blessing());
        assertEquals("$1950", definition.name());
        assertEquals(672, definition.itemDescriptionId());
        assertEquals(30_000, definition.unitWeight());
        assertEquals(BattleActionCodes.SWORD_WALK, definition.weaponWalkAction());

        InventoryCatalog.NativeWeaponStatus status =
                InventoryCatalog.nativeWeaponStatus(InventoryCatalog.ANCIENT_SWORD);
        assertNotNull(status);
        assertEquals(35, status.damageSmall());
        assertEquals(20, status.damageLarge());
        assertEquals(11, status.material());
        assertEquals(30_000, status.unitWeight());
        assertEquals(0x3B, status.classMask());
        assertEquals(-1, status.safeEnchant());
    }

    @Test
    void selectedGetbackUsesEachOfTheThreeDirectOrcTownPoints() {
        assertEquals("B112C7B1A028AA18237E9BF9AD385503772965157013D974AC17CA441E51E559",
                ClassicWorldMapCatalog.MAP244_GETBACK_SOURCE_SHA256);
        assertEquals(new ClassicWorldGetbackCatalog.Destination(4, 32750, 32435, 0),
                ClassicWorldGetbackCatalog.resolve(244, 32982, 32808, 1, 0));
        assertEquals(new ClassicWorldGetbackCatalog.Destination(4, 32745, 32447, 0),
                ClassicWorldGetbackCatalog.resolve(244, 32982, 32808, 1, 1));
        assertEquals(new ClassicWorldGetbackCatalog.Destination(4, 32738, 32452, 0),
                ClassicWorldGetbackCatalog.resolve(244, 32982, 32808, 1, 2));
    }

    private static void assertRoute(
            ClassicWorldTeleportCatalog.Route route,
            int sourceMap, int spawn, int npc, int sourceX, int sourceY,
            String html, int routeId, String action,
            int destinationMap, int destinationX, int destinationY,
            int heading, int price) {
        assertEquals(sourceMap, route.sourceMapId());
        assertEquals(spawn, route.sourceSpawnId());
        assertEquals(npc, route.sourceNpcId());
        assertEquals(sourceX, route.sourceX());
        assertEquals(sourceY, route.sourceY());
        assertEquals(html, route.initialHtml());
        assertEquals(routeId, route.routeId());
        assertEquals(action, route.action());
        assertEquals(destinationMap, route.destinationMapId());
        assertEquals(destinationX, route.destinationX());
        assertEquals(destinationY, route.destinationY());
        assertEquals(heading, route.heading());
        assertEquals(price, route.priceAmount());
        assertEquals(InventoryCatalog.ADENA, route.priceItemId());
        assertEquals(0, route.minimumLevel());
        assertEquals(200, route.maximumLevel());
        assertTrue(ClassicWorldMapCatalog.forMap(destinationMap)
                .standingPassable(destinationX, destinationY));
    }

    private static String resourceHash(String name) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClassResource(name)) {
            assertNotNull(in);
            digest.update(in.readAllBytes());
        }
        return HexFormat.of().withUpperCase().formatHex(digest.digest());
    }

    private static InputStream getClassResource(String name) {
        return ClassicWorldTaintedLandCatalogTest.class.getResourceAsStream(name);
    }
}
