package local.lito381.server;

import java.util.List;

/** Exact active rows from the selected client's Big5 LTzone-c.tbl. */
final class NativeLitoZoneCatalog {
    record Color(int index, int red, int green, int blue) {}

    record Zone(String key, int colorIndex, int mapId,
                int minX, int minY, int maxX, int maxY) {
        boolean coversWholeMap() {
            return minX == 0 && minY == 0 && maxX == 0 && maxY == 0;
        }
    }

    private static final List<Color> COLORS = List.of(
            new Color(0, 45, 95, 209),
            new Color(1, 10, 173, 90));

    private static final List<Zone> ZONES = List.of(
            new Zone("LODUN", 0, 500, 0, 0, 0, 0),
            new Zone("LENOA", 1, 560, 0, 0, 0, 0));

    private NativeLitoZoneCatalog() {}

    static List<Color> colors() {
        return ResourceDatabase.colors(COLORS);
    }

    static List<Zone> zones() {
        return ResourceDatabase.zones(ZONES);
    }

    static Zone forMap(int mapId) {
        return ResourceDatabase.zone(ZONES.stream()
                .filter(zone -> zone.mapId() == mapId)
                .findFirst()
                .orElse(null), mapId);
    }
}
