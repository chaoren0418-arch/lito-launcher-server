package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ClassicWorldTeleportCatalogTest {
    @Test
    void selectedPhantasmIslandRouteMatchesNpcAndImportedLanding() {
        ClassicWorldTeleportCatalog.Route route =
                ClassicWorldTeleportCatalog.phantasmIsland();
        assertEquals(4, route.sourceMapId());
        assertEquals(83, route.sourceSpawnId());
        assertEquals(50045, route.sourceNpcId());
        assertEquals(0x6400_0053L, route.sourceObjectId());
        assertEquals(33978, route.sourceX());
        assertEquals(32923, route.sourceY());
        assertEquals("telephantasm", route.initialHtml());
        assertEquals(429, route.routeId());
        assertEquals("teleport phantasm-island", route.action());
        assertEquals(303, route.destinationMapId());
        assertEquals(32635, route.destinationX());
        assertEquals(32817, route.destinationY());
        assertEquals(5, route.heading());
        assertEquals(1_000, route.priceAmount());
        assertEquals(InventoryCatalog.ADENA, route.priceItemId());
        assertTrue(ClassicWorldMapCatalog.forMap(route.destinationMapId())
                .standingPassable(route.destinationX(), route.destinationY()));

        ClassicWorldNpcCatalog.StaticNpc npc = ClassicWorldNpcCatalog.npc(
                route.sourceMapId(), route.sourceObjectId());
        assertNotNull(npc);
        assertEquals(route.sourceSpawnId(), npc.spawnId());
        assertEquals(route.sourceNpcId(), npc.npcId());
        assertEquals("L1Teleporter", npc.implementation());
        assertEquals(route.initialHtml(), npc.initialHtml(0));
        assertEquals(route, ClassicWorldTeleportCatalog.route(
                4, route.sourceObjectId(), route.action()));
        assertNull(ClassicWorldTeleportCatalog.route(
                4, route.sourceObjectId(), "teleport phantasm-island "));
    }

    @Test
    void selectedGiranTradeZoneRouteKeepsBothEnabledFreeSourceSpawns() {
        java.util.List<ClassicWorldTeleportCatalog.Route> routes =
                ClassicWorldTeleportCatalog.giranTradeZone();
        assertEquals(2, routes.size());
        assertEquals(java.util.List.of(75, 76), routes.stream()
                .map(ClassicWorldTeleportCatalog.Route::sourceSpawnId).toList());
        assertEquals(java.util.List.of(0x6400_004BL, 0x6400_004CL), routes.stream()
                .map(ClassicWorldTeleportCatalog.Route::sourceObjectId).toList());
        for (ClassicWorldTeleportCatalog.Route route : routes) {
            assertEquals(4, route.sourceMapId());
            assertEquals(50034, route.sourceNpcId());
            assertEquals("grtztele", route.initialHtml());
            assertEquals(181, route.routeId());
            assertEquals("teleport giran-giran-trade-zone", route.action());
            assertEquals(350, route.destinationMapId());
            assertEquals(32702, route.destinationX());
            assertEquals(32842, route.destinationY());
            assertEquals(5, route.heading());
            assertEquals(0, route.priceAmount());
            assertEquals(InventoryCatalog.ADENA, route.priceItemId());
            assertTrue(ClassicWorldMapCatalog.forMap(route.destinationMapId())
                    .standingPassable(route.destinationX(), route.destinationY()));

            ClassicWorldNpcCatalog.StaticNpc npc = ClassicWorldNpcCatalog.npc(
                    route.sourceMapId(), route.sourceObjectId());
            assertNotNull(npc);
            assertEquals(route.sourceNpcId(), npc.npcId());
            assertEquals(route.initialHtml(), npc.initialHtml(0));
            assertEquals(route, ClassicWorldTeleportCatalog.source(
                    route.sourceMapId(), route.sourceObjectId()));
            assertEquals(route, ClassicWorldTeleportCatalog.route(
                    route.sourceMapId(), route.sourceObjectId(), route.action()));
        }
        assertNull(ClassicWorldTeleportCatalog.route(
                4, 0x6400_004BL, "teleport giran-giran-trade-zone "));
        assertNull(ClassicWorldTeleportCatalog.source(4, 0x6400_003DL),
                "selected spawn61 count=0 route175 must stay disabled");
    }

    @Test
    void selectedOrcTeleporterKeepsAllThreeNativeActionsAndMap21Route() {
        java.util.List<ClassicWorldTeleportCatalog.Route> routes =
                ClassicWorldTeleportCatalog.orcRoutes();
        assertEquals(3, routes.size());
        assertEquals(java.util.List.of(769, 770, 771), routes.stream()
                .map(ClassicWorldTeleportCatalog.Route::routeId).toList());
        assertEquals(java.util.List.of(
                        "teleport orc-village", "teleport orc-elvendun",
                        "teleport orc-dragonvalley"),
                routes.stream().map(ClassicWorldTeleportCatalog.Route::action).toList());
        assertEquals(java.util.List.of(4, 21, 4), routes.stream()
                .map(ClassicWorldTeleportCatalog.Route::destinationMapId).toList());
        for (ClassicWorldTeleportCatalog.Route route : routes) {
            assertEquals(4, route.sourceMapId());
            assertEquals(106, route.sourceSpawnId());
            assertEquals(50063, route.sourceNpcId());
            assertEquals(0x6400_006AL, route.sourceObjectId());
            assertEquals(32801, route.sourceX());
            assertEquals(32268, route.sourceY());
            assertEquals("telecorc1", route.initialHtml());
            assertEquals(0, route.priceAmount());
            assertEquals(InventoryCatalog.ADENA, route.priceItemId());
            assertTrue(ClassicWorldMapCatalog.forMap(route.destinationMapId())
                    .standingPassable(route.destinationX(), route.destinationY()));
            assertEquals(route, ClassicWorldTeleportCatalog.route(
                    4, route.sourceObjectId(), route.action()));
        }
        ClassicWorldTeleportCatalog.Route elven = routes.get(1);
        assertEquals(32760, elven.destinationX());
        assertEquals(32761, elven.destinationY());
        assertEquals(5, elven.heading());
        ClassicWorldNpcCatalog.StaticNpc npc = ClassicWorldNpcCatalog.npc(
                4, elven.sourceObjectId());
        assertNotNull(npc);
        assertEquals("L1Teleporter", npc.implementation());
        assertEquals("telecorc1", npc.initialHtml(0));
        assertNull(ClassicWorldTeleportCatalog.route(
                4, elven.sourceObjectId(), "teleport orc-elvendun "));
    }

    @Test
    void selectedFairyPrincessKeepsBothFreeNativeDungeonActions() {
        java.util.List<ClassicWorldTeleportCatalog.Route> routes =
                ClassicWorldTeleportCatalog.fairyDungeonRoutes();
        assertEquals(2, routes.size());
        assertEquals(java.util.List.of(1142, 1143), routes.stream()
                .map(ClassicWorldTeleportCatalog.Route::routeId).toList());
        assertEquals(java.util.List.of(
                        "teleport darkmar-dungen", "teleport dark-elf-dungen"),
                routes.stream().map(
                        ClassicWorldTeleportCatalog.Route::action).toList());
        assertEquals(java.util.List.of(213, 209), routes.stream()
                .map(ClassicWorldTeleportCatalog.Route::destinationMapId).toList());
        for (ClassicWorldTeleportCatalog.Route route : routes) {
            assertEquals(4, route.sourceMapId());
            assertEquals(1154, route.sourceSpawnId());
            assertEquals(70853, route.sourceNpcId());
            assertEquals(0x6400_0482L, route.sourceObjectId());
            assertEquals(32968, route.sourceX());
            assertEquals(32443, route.sourceY());
            assertEquals("fairyp3", route.initialHtml());
            assertEquals(32727, route.destinationX());
            assertEquals(32799, route.destinationY());
            assertEquals(5, route.heading());
            assertEquals(0, route.priceAmount());
            assertEquals(InventoryCatalog.ADENA, route.priceItemId());
            assertTrue(ClassicWorldMapCatalog.forMap(route.destinationMapId())
                    .standingPassable(route.destinationX(), route.destinationY()));
            assertEquals(route, ClassicWorldTeleportCatalog.route(
                    4, route.sourceObjectId(), route.action()));
        }
        ClassicWorldNpcCatalog.StaticNpc npc = ClassicWorldNpcCatalog.npc(
                4, routes.get(0).sourceObjectId());
        assertNotNull(npc);
        assertEquals(70853, npc.npcId());
        assertEquals("L1Merchant", npc.implementation());
        assertEquals("fairyp3", npc.initialHtml(0));
        assertEquals(routes.get(0), ClassicWorldTeleportCatalog.source(
                4, routes.get(0).sourceObjectId()));
        assertNull(ClassicWorldTeleportCatalog.route(
                4, routes.get(0).sourceObjectId(), "teleport darkmar-dungen "));
    }

    @Test
    void selectedDuvallRouteKeepsExactPaidHiddenDockAction() {
        ClassicWorldTeleportCatalog.Route route =
                ClassicWorldTeleportCatalog.hiddenDock();
        assertEquals(4, route.sourceMapId());
        assertEquals(107, route.sourceSpawnId());
        assertEquals(50064, route.sourceNpcId());
        assertEquals(0x6400_006BL, route.sourceObjectId());
        assertEquals(32721, route.sourceX());
        assertEquals(32443, route.sourceY());
        assertEquals("duvall1", route.initialHtml());
        assertEquals(772, route.routeId());
        assertEquals("teleport orctown-hiddendock", route.action());
        assertEquals(445, route.destinationMapId());
        assertEquals(32738, route.destinationX());
        assertEquals(32857, route.destinationY());
        assertEquals(5, route.heading());
        assertEquals(100, route.priceAmount());
        assertEquals(InventoryCatalog.ADENA, route.priceItemId());
        assertTrue(ClassicWorldMapCatalog.forMap(route.destinationMapId())
                .standingPassable(route.destinationX(), route.destinationY()));
        ClassicWorldNpcCatalog.StaticNpc source = ClassicWorldNpcCatalog.npc(
                4, route.sourceObjectId());
        assertNotNull(source);
        assertEquals("L1Teleporter", source.implementation());
        assertEquals("duvall1", source.initialHtml(0));
        assertEquals(route, ClassicWorldTeleportCatalog.source(
                4, route.sourceObjectId()));
        assertEquals(route, ClassicWorldTeleportCatalog.route(
                4, route.sourceObjectId(), route.action()));
        assertNull(ClassicWorldTeleportCatalog.route(
                4, route.sourceObjectId(), route.action() + " "));
    }

    @Test
    void selectedSepiaRouteKeepsBothOverlappingMap32Sources() {
        java.util.List<ClassicWorldTeleportCatalog.Route> routes =
                ClassicWorldTeleportCatalog.sepiaDungeon();
        assertEquals(2, routes.size());
        assertEquals(java.util.List.of(65, 1156), routes.stream()
                .map(ClassicWorldTeleportCatalog.Route::sourceSpawnId).toList());
        assertEquals(java.util.List.of(0x6400_0041L, 0x6400_0484L), routes.stream()
                .map(ClassicWorldTeleportCatalog.Route::sourceObjectId).toList());
        for (ClassicWorldTeleportCatalog.Route route : routes) {
            assertEquals(32, route.sourceMapId());
            assertEquals(50031, route.sourceNpcId());
            assertEquals(32713, route.sourceX());
            assertEquals(32842, route.sourceY());
            assertEquals("sepia2", route.initialHtml());
            assertEquals(176, route.routeId());
            assertEquals("teleport sepia-dungen", route.action());
            assertEquals(302, route.destinationMapId());
            assertEquals(32740, route.destinationX());
            assertEquals(32860, route.destinationY());
            assertEquals(5, route.heading());
            assertEquals(0, route.priceAmount());
            assertEquals(InventoryCatalog.ADENA, route.priceItemId());
            assertTrue(ClassicWorldMapCatalog.forMap(302)
                    .standingPassable(route.destinationX(), route.destinationY()));
            ClassicWorldNpcCatalog.StaticNpc npc = ClassicWorldNpcCatalog.npc(
                    32, route.sourceObjectId());
            assertNotNull(npc);
            assertEquals(route.sourceSpawnId(), npc.spawnId());
            assertEquals(route.sourceNpcId(), npc.npcId());
            assertEquals("L1Merchant", npc.implementation());
            assertEquals(route.initialHtml(), npc.initialHtml(0));
            assertEquals(route, ClassicWorldTeleportCatalog.route(
                    32, route.sourceObjectId(), route.action()));
        }
        assertNull(ClassicWorldTeleportCatalog.route(
                32, routes.get(0).sourceObjectId(), "teleport sepia-dungen "));
    }
}
