package local.lito381.server;

import java.util.Map;

/**
 * Server-selected bootstrap policy for the two native rental-town scenes.
 *
 * <p>The map and collision resources are exact selected-client facts.  The
 * original upstream server's first local-player coordinate has not survived,
 * so these anchors are deliberately labelled resource-derived policy rather
 * than historical packet evidence.</p>
 */
final class NativeLitoTownScenePlan {
    static final String SPAWN_AUTHORITY =
            "server-selected-native-Layer3-largest-component-center";

    record Scene(int mapId, int spawnX, int spawnY, int heading,
                 boolean historicalSpawnExact, String spawnAuthority,
                 RentalLoadoutPlan.StatePolicy loadoutPolicy) {
        Scene {
            NativeLitoTownCatalog.Town town = NativeLitoTownCatalog.forMap(mapId);
            if (town == null || heading < 0 || heading > 7
                    || historicalSpawnExact || !SPAWN_AUTHORITY.equals(spawnAuthority)
                    || loadoutPolicy == null
                    || !town.containsResourceCoordinate(spawnX, spawnY)
                    || !NativeLitoTownPassabilityCatalog.get()
                        .isStandingPassable(mapId, spawnX, spawnY)) {
                throw new IllegalArgumentException("invalid rental-town scene plan");
            }
        }
    }

    private static final Map<Integer, Scene> SCENES = Map.of(
            500, scene(500, 32770, 32818),
            560, scene(560, 32328, 32768));

    private NativeLitoTownScenePlan() {}

    static Scene require(int mapId) {
        Scene scene = SCENES.get(mapId);
        if (scene == null) {
            throw new IllegalArgumentException(
                    "unsupported native rental-town scene: " + mapId);
        }
        return scene;
    }

    private static Scene scene(int mapId, int x, int y) {
        return new Scene(mapId, x, y, 4, false, SPAWN_AUTHORITY,
                RentalLoadoutPlan.StatePolicy.IDENTIFIED_TABLE_SLOTS);
    }
}
