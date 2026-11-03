package local.lito381.server;

import java.util.List;
import java.util.Set;

/**
 * Exact town rows retained by the selected LiTo 3.81C client.
 *
 * <p>The map ids and zone keys come from {@code LTmapstr-c.tbl} and
 * {@code LTzone-c.tbl}.  They describe loadable native scenes, but do not by
 * themselves prove that this client revision exposes a town transition in its
 * login/channel flow.  Native S32 Layer 7 now independently proves 24 internal
 * routes across twelve paired slots on map 500 and 32 routes across sixteen
 * paired slots on map 560; those exact routes live in
 * {@link NativeLitoTownPortalCatalog}.  They still do not identify the upstream
 * directory room, map assignment or initial player position.  The room map menu
 * can enumerate category 6 only after
 * the server has supplied category 6 in S_D5; selecting a row then emits C_D1
 * subtype 3 with map 500 or 560.  The exact S32 manifests and loaded-sector
 * bounds are retained here so future town movement cannot silently reuse a
 * same-numbered classic map.  They intentionally do not declare an original
 * spawn coordinate: S32 passability can prove safe floor, not the historical
 * server's initial position.  The client can load the chosen scene from
 * S_WORLD followed by S_D2.  The current server exposes that transition only
 * through the default-off, server-gated rental-town route; the exact original
 * assignment, spawn and grant policy remains unrecovered.  Keep these
 * non-arena scenes outside {@link ArenaMapCatalog}.</p>
 */
final class NativeLitoTownCatalog {
    record Town(int mapId, String mapTableName, String zoneKey, int zoneIndex,
                String resourceSha256, Set<Integer> loadedSectors,
                int minX, int maxX, int minY, int maxY) {
        Town {
            if (resourceSha256 == null || !resourceSha256.matches("[0-9A-F]{64}")) {
                throw new IllegalArgumentException("invalid town resource digest");
            }
            loadedSectors = Set.copyOf(loadedSectors);
            if (loadedSectors.isEmpty() || minX > maxX || minY > maxY) {
                throw new IllegalArgumentException("invalid town resource geometry");
            }
        }

        boolean containsResourceCoordinate(int x, int y) {
            if (x < minX || x > maxX || y < minY || y > maxY) return false;
            return loadedSectors.contains(sectorKeyForCoordinate(x, y));
        }
    }

    private static final List<Town> TOWNS = List.of(
            town(500, "town", "LODUN", 0,
                    "871DED11A6E61698E6C221C41F9E48DCF134AD0BC0DC09778A11263A09967F13",
                    "7ffe7fff 7ffe8000 7ffe8001 7ffe8002 7fff7ffe 7fff7fff "
                            + "7fff8000 7fff8001 7fff8002 80007ffe 80007fff 80008000 "
                            + "80008001 80008002 80017fff 80018000 80018001 80018002"),
            town(560, "town2", "LENOA", 1,
                    "DEEC05D279524BB9B565A4DD276403CD849726EFC5C7CCE13ADCA6B5AA7758B7",
                    "7ff67fff 7ff68000 7ff77fff 7ff78000 7ff87fff 7ff88000 "
                            + "7ff97ffe 7ff97fff 7ff98000 7ffa7fff 7ffa8000"));

    private NativeLitoTownCatalog() {}

    static List<Town> towns() {
        return TOWNS;
    }

    static Town forMap(int mapId) {
        return TOWNS.stream()
                .filter(town -> town.mapId() == mapId)
                .findFirst()
                .orElse(null);
    }

    private static Town town(int mapId, String mapTableName, String zoneKey,
                             int zoneIndex, String resourceSha256,
                             String sectorText) {
        Set<Integer> sectors = java.util.Arrays.stream(sectorText.split(" "))
                .map(token -> (int) Long.parseUnsignedLong(token, 16))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        int minX = sectors.stream().mapToInt(key -> sectorBegin(key >>> 16)).min()
                .orElseThrow();
        int maxX = sectors.stream().mapToInt(key -> sectorBegin(key >>> 16) + 63).max()
                .orElseThrow();
        int minY = sectors.stream().mapToInt(key -> sectorBegin(key & 0xFFFF)).min()
                .orElseThrow();
        int maxY = sectors.stream().mapToInt(key -> sectorBegin(key & 0xFFFF) + 63).max()
                .orElseThrow();
        return new Town(mapId, mapTableName, zoneKey, zoneIndex,
                resourceSha256, sectors, minX, maxX, minY, maxY);
    }

    private static int sectorKeyForCoordinate(int x, int y) {
        int blockX = 0x7FFF + Math.floorDiv(x - 32704, 64);
        int blockY = 0x7FFF + Math.floorDiv(y - 32704, 64);
        return (blockX << 16) | (blockY & 0xFFFF);
    }

    private static int sectorBegin(int block) {
        return (block - 0x7FFF) * 64 + 32704;
    }
}
