package local.lito381.server;

import java.util.List;

/** Exact selected {@code 380_db.道具_自訂區域傳送} rows in imported WORLD slices. */
final class ClassicWorldCustomAreaTeleportCatalog {
    static final int ROW_COUNT = 1;
    static final String SOURCE_SHA256 =
            "3F269D72D7976393961CC2CE4DB2D183ACF1FE884882F7E65F177C09C858A91F";

    record Route(
            int rowId, long itemId, String itemName,
            int sourceMapId, int x1, int y1, int x2, int y2,
            int destinationX, int destinationY, int destinationRandomRange,
            int destinationMapId, int destinationHeading, String note) {
        Route {
            if (rowId <= 0 || itemId <= 0 || itemId > 0xFFFF_FFFFL
                    || itemName == null || itemName.isBlank()
                    || sourceMapId < -1 || x1 < 0 || y1 < 0 || x2 < x1 || y2 < y1
                    || destinationX <= 0 || destinationX > 0xFFFF
                    || destinationY <= 0 || destinationY > 0xFFFF
                    || destinationRandomRange < 0 || destinationMapId < 0
                    || destinationHeading < 0 || destinationHeading > 7
                    || note == null) {
                throw new IllegalArgumentException("invalid custom-area teleport route");
            }
        }

        boolean matches(int mapId, int x, int y) {
            return (sourceMapId < 0 || sourceMapId == mapId)
                    && x >= x1 && x <= x2 && y >= y1 && y <= y2;
        }
    }

    private static final List<Route> ROUTES = List.of(new Route(
            1, InventoryCatalog.SECRET_ROOM_KEY, "密室鑰匙",
            13, 32806, 32798, 32814, 32807,
            32815, 32810, 0, 13, 5,
            "騎士30原密室入口；已與任務狀態解耦"));

    private ClassicWorldCustomAreaTeleportCatalog() {}

    static List<Route> all() {
        return ROUTES;
    }

    static Route route(long itemId) {
        return ROUTES.stream().filter(row -> row.itemId() == itemId)
                .findFirst().orElse(null);
    }

    static Route resolve(long itemId, int mapId, int x, int y) {
        Route route = route(itemId);
        return route != null && route.matches(mapId, x, y) ? route : null;
    }
}
