package local.lito381.server;

import java.util.List;

/** Exact retained timed departures for the two Pirate Island ship maps. */
final class ClassicWorldShipDepartureCatalog {
    static final String TIMER_SOURCE_SHA256 =
            "F65F65099D4565E1632DE9EC56F450039D2BBADAC08773DDE09B5242DB17CDD7";

    record Departure(int sourceMapId, long ticketCatalogId,
                     int destinationMapId, int destinationX, int destinationY,
                     int destinationHeading,
                     ClassicWorldShipSchedule.Window schedule) {
        Departure {
            if (sourceMapId < 0 || ticketCatalogId <= 0
                    || destinationMapId < 0
                    || destinationX <= 0 || destinationX > 0xFFFF
                    || destinationY <= 0 || destinationY > 0xFFFF
                    || destinationHeading < 0 || destinationHeading > 7
                    || schedule == null) {
                throw new IllegalArgumentException("invalid classic ship departure");
            }
        }

        boolean openAt(int secondsOfDay) {
            return schedule.openAt(secondsOfDay);
        }
    }

    private static final List<Departure> DEPARTURES = List.of(
            new Departure(
                    ClassicWorldMapCatalog.HIDDEN_DOCK_SHIP_MAP_ID,
                    InventoryCatalog.SHIP_TICKET_HIDDEN_DOCK,
                    ClassicWorldMapCatalog.HIDDEN_DOCK_MAP_ID,
                    32751, 32873, 0,
                    ClassicWorldShipSchedule.Window.HIDDEN_DOCK_DEPARTURE),
            new Departure(
                    ClassicWorldMapCatalog.PIRATE_ISLAND_SHIP_MAP_ID,
                    InventoryCatalog.SHIP_TICKET_PIRATE_ISLAND,
                    ClassicWorldElementalGraveCatalog.PIRATE_ISLAND_MAP_ID,
                    32297, 33086, 0,
                    ClassicWorldShipSchedule.Window.PIRATE_ISLAND_DEPARTURE));

    private ClassicWorldShipDepartureCatalog() {}

    static List<Departure> all() {
        return DEPARTURES;
    }

    static Departure find(int sourceMapId) {
        return DEPARTURES.stream()
                .filter(departure -> departure.sourceMapId() == sourceMapId)
                .findFirst().orElse(null);
    }
}
