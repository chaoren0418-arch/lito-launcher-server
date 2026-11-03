package local.lito381.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Native Tournament ground-portal routes reconstructed from S32 Layer 7.
 *
 * <p>{@code rawTag} is the parser's optional opaque 0x64..0x66 suffix.  The
 * selected client stores it but its {@code C_ENTER_PORTAL} producer sends only
 * source X/Y and has no tag-table reference.  In the selected resources every
 * Quest route is untagged; tagged rows belong to the Siege resource family.
 * It must therefore remain provenance only, never a Force, room or gate
 * authorization input.</p>
 */
final class ArenaPortalCatalog {
    static final int NO_TAG = -1;

    record Route(int sourceMapId, ArenaMapCatalog.Point source,
                 int targetMapId, ArenaMapCatalog.Point target, int rawTag) {
        Route {
            if (source == null || target == null) {
                throw new IllegalArgumentException("ground-portal coordinates are required");
            }
            if (rawTag != NO_TAG && (rawTag < 0x64 || rawTag > 0x66)) {
                throw new IllegalArgumentException("unsupported native portal tag: " + rawTag);
            }
        }

        /** Storage-format fact only; callers must not interpret it as access. */
        boolean tagged() {
            return rawTag != NO_TAG;
        }
    }

    private record RouteData(int mapId, String rows) {}

    private static final List<RouteData> RESOURCE_DATA = List.of(
            routes(525, "32722,32924,525,32803,32806,-1 32723,32924,525,32804,32805,-1 32723,32902,525,32794,32795,-1 32722,32902,525,32793,32796,-1 32734,32985,525,32812,32807,-1 32748,32999,525,32798,32793,-1 32803,32806,525,32722,32924,-1 32804,32805,525,32723,32924,-1 32794,32795,525,32723,32902,-1 32793,32796,525,32722,32902,-1"),
            routes(532, "32758,32831,532,32770,32783,-1 32767,32841,532,32813,32829,-1 32770,32783,532,32758,32831,-1 32813,32829,532,32767,32841,-1"),
            routes(535, "32654,32853,535,32719,32974,-1 32686,32881,535,32687,33000,-1 32687,33000,535,32686,32881,-1 32719,32881,535,32817,33000,-1 32753,32974,535,32784,32854,-1 32719,32974,535,32654,32853,-1 32784,32854,535,32753,32974,-1 32817,33000,535,32719,32881,-1"),
            routes(547, "32741,32779,547,32726,32786,-1 32726,32786,547,32741,32779,-1 32741,32815,547,32726,32806,-1 32726,32806,547,32741,32815,-1 32722,32777,547,32723,32774,-1 32723,32774,547,32722,32777,-1 32722,32812,547,32723,32809,-1 32723,32809,547,32722,32812,-1"),
            routes(548, "32719,32795,548,32730,32809,-1 32730,32809,548,32719,32795,-1 32755,32795,548,32748,32810,-1 32748,32810,548,32755,32795,-1 32721,32815,548,32725,32814,-1 32725,32814,548,32721,32815,-1 32757,32815,548,32761,32814,-1 32761,32814,548,32757,32815,-1"),
            routes(549, "32693,33003,549,32696,32985,102 32696,32985,549,32693,33003,102 32753,32918,549,32771,32907,101 32746,32905,549,32748,32901,101 32748,32901,549,32746,32905,101 32746,32922,549,32748,32918,101 32748,32918,549,32746,32922,101 32753,32958,549,32778,32940,101 32746,32959,549,32748,32956,101 32748,32956,549,32746,32959,101 32746,32976,549,32748,32973,101 32748,32973,549,32746,32976,101 32753,32981,549,32757,32979,101 32757,32979,549,32753,32981,101 32731,33005,549,32748,33008,102 32748,33008,549,32731,33005,102 32719,32993,549,32720,32979,102 32720,32979,549,32719,32993,102 32745,33034,549,32725,33038,102 32725,33038,549,32745,33034,102 32771,32907,549,32753,32918,101 32778,32940,549,32753,32958,101 32788,32950,549,32769,32974,101 32819,32957,549,32809,32974,101 32769,32974,549,32788,32950,101 32771,32981,549,32775,32979,101 32775,32979,549,32771,32981,101 32809,32974,549,32819,32957,101 32815,32981,549,32819,32979,101 32819,32979,549,32815,32981,101 32827,32981,549,32831,32979,101 32831,32979,549,32827,32981,101"),
            routes(550, "32693,33003,550,32696,32985,101 32696,32985,550,32693,33003,101 32746,32905,550,32748,32901,102 32748,32901,550,32746,32905,102 32753,32918,550,32771,32907,102 32746,32922,550,32748,32918,102 32748,32918,550,32746,32922,102 32753,32958,550,32778,32940,102 32746,32959,550,32748,32956,102 32748,32956,550,32746,32959,102 32746,32976,550,32748,32973,102 32748,32973,550,32746,32976,102 32753,32981,550,32757,32979,102 32757,32979,550,32753,32981,102 32719,32993,550,32720,32979,101 32720,32979,550,32719,32993,101 32731,33005,550,32748,33008,101 32748,33008,550,32731,33005,101 32725,33038,550,32745,33034,101 32745,33034,550,32725,33038,101 32771,32907,550,32753,32918,102 32778,32940,550,32753,32958,102 32788,32950,550,32769,32974,102 32819,32957,550,32809,32974,102 32771,32981,550,32775,32979,102 32775,32979,550,32771,32981,102 32769,32974,550,32788,32950,102 32809,32974,550,32819,32957,102 32815,32981,550,32819,32979,102 32819,32979,550,32815,32981,102 32827,32981,550,32831,32979,102 32831,32979,550,32827,32981,102"),
            routes(554, "32746,32926,554,32742,32888,-1 32747,32926,554,32743,32888,-1 32788,32926,554,32793,32888,-1 32789,32926,554,32794,32888,-1"),
            routes(562, "32714,32898,562,32705,32907,102 32705,32907,562,32714,32898,102 32729,32943,562,32789,32969,100 32813,32874,562,32799,32910,101 32799,32910,562,32813,32874,101 32789,32969,562,32729,32943,100"),
            routes(563, "32714,32898,563,32705,32907,101 32705,32907,563,32714,32898,101 32729,32943,563,32789,32969,100 32813,32874,563,32799,32910,102 32799,32910,563,32813,32874,102 32789,32969,563,32729,32943,100"),
            routes(573, "32755,32792,573,32742,32797,101 32742,32797,573,32755,32792,101"),
            routes(574, "32755,32792,574,32742,32797,102 32742,32797,574,32755,32792,102")
    );

    private static final Map<Integer, Map<ArenaMapCatalog.Point, Route>> ROUTES = build();

    private ArenaPortalCatalog() {}

    static Route find(int mapId, int x, int y) {
        Map<ArenaMapCatalog.Point, Route> routes = ROUTES.get(mapId);
        return routes == null ? null : routes.get(new ArenaMapCatalog.Point(x, y));
    }

    static List<Route> routesForMap(int mapId) {
        Map<ArenaMapCatalog.Point, Route> routes = ROUTES.get(mapId);
        return routes == null ? List.of() : List.copyOf(routes.values());
    }

    static int routeCount() {
        return ROUTES.values().stream().mapToInt(Map::size).sum();
    }

    private static RouteData routes(int mapId, String rows) {
        return new RouteData(mapId, rows);
    }

    private static Map<Integer, Map<ArenaMapCatalog.Point, Route>> build() {
        Map<Integer, Map<ArenaMapCatalog.Point, Route>> result = new HashMap<>();
        for (RouteData data : RESOURCE_DATA) {
            ArenaMapCatalog.Arena sourceArena = ArenaMapCatalog.forMap(data.mapId());
            Map<ArenaMapCatalog.Point, Route> bySource = new HashMap<>();
            for (String token : data.rows().split(" ")) {
                String[] values = token.split(",", -1);
                if (values.length != 6) {
                    throw new IllegalArgumentException("invalid ground-portal token: " + token);
                }
                int sourceX = Integer.parseInt(values[0]);
                int sourceY = Integer.parseInt(values[1]);
                int targetMap = Integer.parseInt(values[2]);
                int targetX = Integer.parseInt(values[3]);
                int targetY = Integer.parseInt(values[4]);
                int rawTag = Integer.parseInt(values[5]);
                ArenaMapCatalog.Point source = new ArenaMapCatalog.Point(sourceX, sourceY);
                ArenaMapCatalog.Point target = new ArenaMapCatalog.Point(targetX, targetY);
                ArenaMapCatalog.Arena targetArena = ArenaMapCatalog.forMap(targetMap);
                if (!sourceArena.contains(sourceX, sourceY)
                        || !targetArena.contains(targetX, targetY)) {
                    throw new IllegalArgumentException("ground portal outside native sectors: "
                            + data.mapId() + " " + token);
                }
                Route route = new Route(data.mapId(), source, targetMap, target, rawTag);
                if (bySource.putIfAbsent(source, route) != null) {
                    throw new IllegalArgumentException("duplicate ground-portal source: "
                            + data.mapId() + " " + source);
                }
            }
            if (result.putIfAbsent(data.mapId(), Map.copyOf(bySource)) != null) {
                throw new IllegalArgumentException("duplicate ground-portal map: " + data.mapId());
            }
        }
        return Map.copyOf(result);
    }
}
