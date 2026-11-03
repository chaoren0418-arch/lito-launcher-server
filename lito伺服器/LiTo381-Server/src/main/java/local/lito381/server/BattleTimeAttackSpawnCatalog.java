package local.lito381.server;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Cooperative Time Attack deployment around the map-540 start objective. */
final class BattleTimeAttackSpawnCatalog {
    static final int MAP_ID = 540;
    static final String DEPLOYMENT_SOURCE =
            "GameMeca-LWC-2005-center-circle-start+"
                    + "LTmap-540-Layer3-nearest-passable-provisional-exact-server-coordinates-unrecovered";
    static final String CENTRAL_ACTION_BOUNDARY_SOURCE =
            "GameMeca-LWC-2005-player-rule+dragon-rumor-not-proven-server-predicate";
    static final String ENCOUNTER_PLACEMENT_SOURCE =
            "LTmap-540-S32-cardinal-safe-anchor-provisional-outside-central-deployment";
    private static final int[] ENCOUNTER_ANCHORS = {5, 3, 6, 7};

    private static final ArenaMapCatalog.Point FEATURE =
            new ArenaMapCatalog.Point(32737, 32800);
    private static final List<ArenaMapCatalog.Point> SPAWNS = points(
            "32737,32799 32736,32800 32738,32800 32737,32801 "
                    + "32736,32799 32738,32799 32736,32801 32738,32801 "
                    + "32737,32798 32735,32800 32739,32800 32737,32802 "
                    + "32736,32798 32738,32798 32735,32799 32739,32799 "
                    + "32735,32801 32739,32801 32736,32802 32738,32802 "
                    + "32735,32798 32739,32798 32735,32802 32739,32802 "
                    + "32737,32797 32734,32800 32740,32800 32737,32803 "
                    + "32736,32797 32738,32797 32734,32799 32740,32799 "
                    + "32734,32801 32740,32801 32736,32803 32738,32803 "
                    + "32735,32797 32739,32797 32734,32798 32740,32798 "
                    + "32734,32802 32740,32802 32735,32803 32739,32803 "
                    + "32737,32796 32733,32800 32741,32800 32737,32804 "
                    + "32736,32796 32738,32796 32733,32799 32741,32799 "
                    + "32733,32801 32741,32801 32736,32804 32738,32804 "
                    + "32734,32797 32740,32797 32734,32803 32740,32803");

    static {
        if (SPAWNS.size() != BattleTimeAttackEncounter.MAXIMUM_PLAYERS
                || new HashSet<>(SPAWNS).size() != SPAWNS.size()
                || SPAWNS.contains(FEATURE)) {
            throw new ExceptionInInitializerError("invalid Time Attack deployment");
        }
        for (ArenaMapCatalog.Point point : SPAWNS) {
            int dx = point.x() - FEATURE.x();
            int dy = point.y() - FEATURE.y();
            if (dx * dx + dy * dy > 18) {
                throw new ExceptionInInitializerError(
                        "Time Attack deployment escaped central formation");
            }
        }
    }

    private BattleTimeAttackSpawnCatalog() {}

    static boolean supports(int mapId) {
        return mapId == MAP_ID;
    }

    static ArenaMapCatalog.Point spawn(int ordinal) {
        return SPAWNS.get(Math.floorMod(ordinal, SPAWNS.size()));
    }

    static List<ArenaMapCatalog.Point> spawns() {
        return SPAWNS;
    }

    static ArenaMapCatalog.Point featurePoint() {
        return FEATURE;
    }

    static ArenaMapCatalog.Point encounterSpawn(ArenaMapCatalog.Arena arena,
                                                  int ordinal) {
        if (arena == null || arena.mapId() != MAP_ID) {
            throw new IllegalArgumentException("Time Attack map 540 is required");
        }
        return arena.spawn(ENCOUNTER_ANCHORS[
                Math.floorMod(ordinal, ENCOUNTER_ANCHORS.length)]);
    }

    private static List<ArenaMapCatalog.Point> points(String value) {
        return Set.of(value.trim().split("\\s+")).stream()
                .map(token -> token.split(","))
                .map(parts -> new ArenaMapCatalog.Point(
                        Integer.parseInt(parts[0]), Integer.parseInt(parts[1])))
                .sorted((left, right) -> {
                    int leftDx = left.x() - FEATURE.x();
                    int leftDy = left.y() - FEATURE.y();
                    int rightDx = right.x() - FEATURE.x();
                    int rightDy = right.y() - FEATURE.y();
                    int distance = Integer.compare(
                            leftDx * leftDx + leftDy * leftDy,
                            rightDx * rightDx + rightDy * rightDy);
                    if (distance != 0) return distance;
                    int y = Integer.compare(left.y(), right.y());
                    return y != 0 ? y : Integer.compare(left.x(), right.x());
                })
                .toList();
    }
}
