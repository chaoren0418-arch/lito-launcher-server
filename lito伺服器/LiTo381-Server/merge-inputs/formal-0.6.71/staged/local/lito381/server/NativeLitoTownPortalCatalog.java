package local.lito381.server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Exact rental-village ground portals reconstructed from native S32 Layer 7. */
final class NativeLitoTownPortalCatalog {
    static final int RENTAL_PORTAL_TAG = 0x64;

    record Point(int x, int y) {}

    record Route(int sourceMapId, Point source, int targetMapId, Point target,
                 String slotName, int rawTag) {
        Route {
            if (source == null || target == null || slotName == null || slotName.isEmpty()) {
                throw new IllegalArgumentException("town portal fields are required");
            }
            if (rawTag != RENTAL_PORTAL_TAG) {
                throw new IllegalArgumentException("unsupported rental portal tag: " + rawTag);
            }
        }
    }

    private record RouteData(int mapId, String rows) {}

    private static final List<RouteData> RESOURCE_DATA = List.of(
            routes(500, "32762,32824,500,32788,32726,4,100 32760,32826,500,32788,32728,5,100 32758,32828,500,32788,32730,6,100 32758,32837,500,32788,32737,7,100 32760,32839,500,32788,32739,8,100 32762,32841,500,32788,32741,9,100 32788,32719,500,32771,32824,1,100 32788,32721,500,32773,32826,2,100 32788,32723,500,32775,32828,3,100 32788,32726,500,32762,32824,4,100 32788,32728,500,32760,32826,5,100 32788,32730,500,32758,32828,6,100 32788,32737,500,32758,32837,7,100 32788,32739,500,32760,32839,8,100 32788,32741,500,32762,32841,9,100 32788,32744,500,32771,32841,10,100 32788,32746,500,32773,32839,11,100 32788,32748,500,32775,32837,12,100 32771,32824,500,32788,32719,1,100 32773,32826,500,32788,32721,2,100 32775,32828,500,32788,32723,3,100 32771,32841,500,32788,32744,10,100 32773,32839,500,32788,32746,11,100 32775,32837,500,32788,32748,12,100"),
            routes(560, "32251,32734,560,32287,32748,a,100 32251,32736,560,32291,32752,b,100 32251,32738,560,32287,32756,c,100 32251,32740,560,32283,32752,d,100 32251,32751,560,32354,32747,e,100 32251,32754,560,32358,32751,f,100 32251,32757,560,32354,32755,g,100 32251,32760,560,32350,32751,h,100 32251,32771,560,32339,32801,i,100 32251,32774,560,32335,32805,j,100 32251,32777,560,32331,32801,k,100 32251,32780,560,32335,32796,l,100 32251,32795,560,32306,32804,n,100 32251,32797,560,32302,32800,o,100 32251,32799,560,32306,32796,p,100 32251,32783,560,32309,32802,m,100 32287,32748,560,32251,32734,a,100 32291,32752,560,32251,32736,b,100 32287,32756,560,32251,32738,c,100 32283,32752,560,32251,32740,d,100 32306,32804,560,32251,32795,n,100 32302,32800,560,32251,32797,o,100 32306,32796,560,32251,32799,p,100 32309,32802,560,32251,32783,m,100 32354,32747,560,32251,32751,e,100 32358,32751,560,32251,32754,f,100 32354,32755,560,32251,32757,g,100 32350,32751,560,32251,32760,h,100 32339,32801,560,32251,32771,i,100 32335,32805,560,32251,32774,j,100 32331,32801,560,32251,32777,k,100 32335,32796,560,32251,32780,l,100")
    );

    private static final Map<Integer, Map<Point, Route>> ROUTES = build();

    private NativeLitoTownPortalCatalog() {}

    static Route find(int mapId, int x, int y) {
        Map<Point, Route> routes = ROUTES.get(mapId);
        return ResourceDatabase.portal(routes == null ? null : routes.get(new Point(x, y)), mapId, x, y);
    }

    static List<Route> routesForMap(int mapId) {
        Map<Point, Route> routes = ROUTES.get(mapId);
        return ResourceDatabase.portals(routes == null ? List.of() : List.copyOf(routes.values()), mapId);
    }

    static int routeCount() {
        return ResourceDatabase.portalCount(ROUTES.values().stream().mapToInt(Map::size).sum());
    }

    private static RouteData routes(int mapId, String rows) {
        return new RouteData(mapId, rows);
    }

    private static Map<Integer, Map<Point, Route>> build() {
        Map<Integer, Map<Point, Route>> result = new HashMap<>();
        for (RouteData data : RESOURCE_DATA) {
            NativeLitoTownCatalog.Town sourceTown = NativeLitoTownCatalog.forMap(data.mapId());
            if (sourceTown == null) {
                throw new IllegalArgumentException("unknown native town: " + data.mapId());
            }
            Map<Point, Route> bySource = new HashMap<>();
            for (String token : data.rows().split(" ")) {
                String[] values = token.split(",", -1);
                if (values.length != 7) {
                    throw new IllegalArgumentException("invalid town portal token: " + token);
                }
                int sourceX = Integer.parseInt(values[0]);
                int sourceY = Integer.parseInt(values[1]);
                int targetMap = Integer.parseInt(values[2]);
                int targetX = Integer.parseInt(values[3]);
                int targetY = Integer.parseInt(values[4]);
                String slotName = values[5];
                int rawTag = Integer.parseInt(values[6]);
                NativeLitoTownCatalog.Town targetTown = NativeLitoTownCatalog.forMap(targetMap);
                if (!sourceTown.containsResourceCoordinate(sourceX, sourceY)
                        || targetTown == null
                        || !targetTown.containsResourceCoordinate(targetX, targetY)) {
                    throw new IllegalArgumentException("town portal outside native sectors: "
                            + data.mapId() + " " + token);
                }
                Point source = new Point(sourceX, sourceY);
                Route route = new Route(data.mapId(), source, targetMap,
                        new Point(targetX, targetY), slotName, rawTag);
                if (bySource.putIfAbsent(source, route) != null) {
                    throw new IllegalArgumentException("duplicate town portal source: "
                            + data.mapId() + " " + source);
                }
            }
            if (result.putIfAbsent(data.mapId(), Map.copyOf(bySource)) != null) {
                throw new IllegalArgumentException("duplicate town portal map: " + data.mapId());
            }
        }
        return Map.copyOf(result);
    }
}
