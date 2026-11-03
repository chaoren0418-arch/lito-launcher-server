package local.lito381.server;

import java.util.List;

/**
 * Server-authoritative envelopes for the four C_GOTO_MAP producers present
 * in the exact LiTo 3.81C binary.
 */
final class ShipRouteCatalog {
    record Route(int sourceMap, int destinationMap,
                 int minX, int maxX, int minY, int maxY,
                 long ticketCatalogId,
                 ClassicWorldShipSchedule.Window schedule) {
        Route {
            if (sourceMap < 0 || sourceMap > 0xFFFF
                    || destinationMap < 0 || destinationMap > 0xFFFF
                    || minX < 0 || maxX > 0xFFFF || minX > maxX
                    || minY < 0 || maxY > 0xFFFF || minY > maxY
                    || ticketCatalogId <= 0 || ticketCatalogId > 0xFFFF_FFFFL
                    || schedule == null) {
                throw new IllegalArgumentException("invalid native ship route");
            }
        }

        boolean accepts(int requestedMap, int x, int y) {
            return destinationMap == requestedMap
                    && x >= minX && x <= maxX
                    && y >= minY && y <= maxY;
        }

        boolean openAt(int secondsOfDay) {
            return schedule.openAt(secondsOfDay);
        }
    }

    /**
     * The two {@code entership} branches in sub_407C70. The source bounds and
     * animation state are client-side prerequisites; the fixed destination is
     * cross-checked against the matching 3.80/3.81 dungeon rows.
     */
    record Boarding(int selector, int sourceMap,
                    int minX, int maxX, int minY, int maxY,
                    int clientAnimationState, long ticketCatalogId,
                    int destinationMap, int destinationX, int destinationY,
                    int destinationHeading,
                    ClassicWorldShipSchedule.Window schedule) {
        Boarding {
            if ((selector != 0 && selector != 1)
                    || sourceMap < 0 || sourceMap > 0xFFFF
                    || minX < 0 || maxX > 0xFFFF || minX > maxX
                    || minY < 0 || maxY > 0xFFFF || minY > maxY
                    || clientAnimationState < 0
                    || ticketCatalogId <= 0 || ticketCatalogId > 0xFFFF_FFFFL
                    || destinationMap < 0 || destinationMap > 0xFFFF
                    || destinationX < 0 || destinationX > 0xFFFF
                    || destinationY < 0 || destinationY > 0xFFFF
                    || destinationHeading < 0 || destinationHeading > 7
                    || schedule == null) {
                throw new IllegalArgumentException("invalid native ship boarding route");
            }
        }

        boolean accepts(int authoritativeMap, int x, int y) {
            return sourceMap == authoritativeMap
                    && x >= minX && x <= maxX && y >= minY && y <= maxY;
        }


        boolean openAt(int secondsOfDay) {
            return schedule.openAt(secondsOfDay);
        }
    }

    private static final List<Route> ROUTES = List.of(
            new Route(5, 4, 32564, 32566, 32723, 32730,
                    InventoryCatalog.SHIP_TICKET_TO_GLUDIO,
                    ClassicWorldShipSchedule.Window.TALKING_TO_GLUDIO_DEPARTURE),
            new Route(6, 0, 32630, 32633, 32972, 32982,
                    InventoryCatalog.SHIP_TICKET_TO_TALKING_ISLAND,
                    ClassicWorldShipSchedule.Window.GLUDIO_TO_TALKING_DEPARTURE),
            new Route(83, 70, 32936, 32937, 33052, 33058,
                    InventoryCatalog.SHIP_TICKET_FORGOTTEN_ISLAND,
                    ClassicWorldShipSchedule.Window.GLUDIO_TO_TALKING_DEPARTURE),
            new Route(84, 4, 33426, 33427, 33489, 33500,
                    InventoryCatalog.SHIP_TICKET_HEINE,
                    ClassicWorldShipSchedule.Window.TALKING_TO_GLUDIO_DEPARTURE));

    private static final List<Boarding> BOARDINGS = List.of(
            new Boarding(0, 0, 32622, 32638, 32960, 32979, 5,
                    InventoryCatalog.SHIP_TICKET_TO_GLUDIO,
                    5, 32733, 32797, 4,
                    ClassicWorldShipSchedule.Window.TALKING_TO_GLUDIO_BOARDING),
            new Boarding(1, 4, 32550, 32564, 32720, 32729, 1,
                    InventoryCatalog.SHIP_TICKET_TO_TALKING_ISLAND,
                    6, 32735, 32795, 4,
                    ClassicWorldShipSchedule.Window.GLUDIO_TO_TALKING_BOARDING));

    private ShipRouteCatalog() {}

    static List<Route> all() {
        return ROUTES;
    }

    static List<Boarding> boardings() {
        return BOARDINGS;
    }

    static Route resolve(int sourceMap, int destinationMap, int x, int y) {
        return ROUTES.stream()
                .filter(route -> route.sourceMap() == sourceMap
                        && route.accepts(destinationMap, x, y))
                .findFirst().orElse(null);
    }

    static boolean supportsExitSource(int sourceMap) {
        return ROUTES.stream().anyMatch(route -> route.sourceMap() == sourceMap);
    }

    static Boarding boarding(int selector) {
        return BOARDINGS.stream()
                .filter(route -> route.selector() == selector)
                .findFirst().orElse(null);
    }
}
