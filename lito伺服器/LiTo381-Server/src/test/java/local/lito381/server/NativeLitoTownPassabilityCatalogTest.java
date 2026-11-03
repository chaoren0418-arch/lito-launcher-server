package local.lito381.server;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NativeLitoTownPassabilityCatalogTest {
    @Test void selectedClientResourceDigestAndMapCountsArePinned() throws Exception {
        NativeLitoTownPassabilityCatalog catalog =
                NativeLitoTownPassabilityCatalog.get();
        assertEquals(2, catalog.mapCount());
        assertEquals(124_950, catalog.blockedEdgeCount(500));
        assertEquals(63_573, catalog.blockedEdgeCount(560));
        try (InputStream input = getClass().getResourceAsStream(
                NativeLitoTownPassabilityCatalog.RESOURCE)) {
            assertTrue(input != null);
            assertEquals(NativeLitoTownPassabilityCatalog.RESOURCE_SHA256,
                    java.util.HexFormat.of().withUpperCase().formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(input.readAllBytes())));
        }
    }

    @Test void resourceDerivedSceneAnchorsArePassableButNotHistoricalClaims() {
        NativeLitoTownPassabilityCatalog catalog =
                NativeLitoTownPassabilityCatalog.get();
        for (int mapId : new int[] {500, 560}) {
            NativeLitoTownScenePlan.Scene scene =
                    NativeLitoTownScenePlan.require(mapId);
            assertEquals(mapId, scene.mapId());
            assertTrue(catalog.isStandingPassable(
                    mapId, scene.spawnX(), scene.spawnY()));
            assertFalse(scene.historicalSpawnExact());
            assertEquals(NativeLitoTownScenePlan.SPAWN_AUTHORITY,
                    scene.spawnAuthority());
            assertEquals(RentalLoadoutPlan.StatePolicy.IDENTIFIED_TABLE_SLOTS,
                    scene.loadoutPolicy());
        }
        assertThrows(IllegalArgumentException.class,
                () -> NativeLitoTownScenePlan.require(501));
    }

    @Test void everyNativeLayerSevenTownRouteHasPassableEndpoints() {
        NativeLitoTownPassabilityCatalog catalog =
                NativeLitoTownPassabilityCatalog.get();
        for (int mapId : new int[] {500, 560}) {
            for (NativeLitoTownPortalCatalog.Route route
                    : NativeLitoTownPortalCatalog.routesForMap(mapId)) {
                assertTrue(catalog.isStandingPassable(
                        route.sourceMapId(), route.source().x(), route.source().y()));
                assertTrue(catalog.isStandingPassable(
                        route.targetMapId(), route.target().x(), route.target().y()));
            }
        }
    }

    @Test void movementIsAdjacentAndFailClosedOutsideTownResources() {
        NativeLitoTownPassabilityCatalog catalog =
                NativeLitoTownPassabilityCatalog.get();
        NativeLitoTownScenePlan.Scene scene = NativeLitoTownScenePlan.require(500);
        assertFalse(catalog.canStep(500, scene.spawnX(), scene.spawnY(),
                scene.spawnX() + 2, scene.spawnY()));
        assertFalse(catalog.isStandingPassable(501, scene.spawnX(), scene.spawnY()));
        assertFalse(catalog.canStep(501, scene.spawnX(), scene.spawnY(),
                scene.spawnX() + 1, scene.spawnY()));
    }
}
