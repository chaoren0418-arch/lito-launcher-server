package local.lito381.server;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** Exact rental-town directional collision edges decoded from native S32 Layer3. */
final class NativeLitoTownPassabilityCatalog {
    static final String RESOURCE = "/rental/town-passability-v1.bin";
    static final String RESOURCE_SHA256 =
            "DE313C6ACC593267B5E1F9CE37CDDEC05E05BBA80AA1315736DE7EADD1B42FC4";
    static final String SOURCE = "selected-client-LTmap-Layer3-sub_48B640";
    private static final byte[] MAGIC = {'L', 'T', 'R', 'P', 1};
    private static final int BYTES_PER_SECTOR = 1024;

    private final Map<Integer, Map<Integer, byte[]>> maps;
    private final Map<Integer, Integer> blockedEdgeCounts;

    private NativeLitoTownPassabilityCatalog(
            Map<Integer, Map<Integer, byte[]>> maps,
            Map<Integer, Integer> blockedEdgeCounts) {
        this.maps = Map.copyOf(maps);
        this.blockedEdgeCounts = Map.copyOf(blockedEdgeCounts);
    }

    static NativeLitoTownPassabilityCatalog get() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        private static final NativeLitoTownPassabilityCatalog INSTANCE = load();
    }

    boolean isStandingPassable(int mapId, int x, int y) {
        return !directionBlocked(mapId, x, y, 0)
                || !directionBlocked(mapId, x, y, 2)
                || !directionBlocked(mapId, x, y, 4)
                || !directionBlocked(mapId, x, y, 6);
    }

    boolean canStep(int mapId, int fromX, int fromY, int toX, int toY) {
        int dx = toX - fromX;
        int dy = toY - fromY;
        if (dx == 0 && dy == 0) {
            return isStandingPassable(mapId, fromX, fromY);
        }
        if (Math.abs(dx) > 1 || Math.abs(dy) > 1
                || !isStandingPassable(mapId, toX, toY)) {
            return false;
        }
        int heading = switch ((dx + 1) + (dy + 1) * 3) {
            case 1 -> 0;
            case 2 -> 1;
            case 5 -> 2;
            case 8 -> 3;
            case 7 -> 4;
            case 6 -> 5;
            case 3 -> 6;
            case 0 -> 7;
            default -> throw new IllegalArgumentException("invalid adjacent step");
        };
        return !directionBlocked(mapId, fromX, fromY, heading);
    }

    boolean directionBlocked(int mapId, int x, int y, int heading) {
        return switch (heading) {
            case 0 -> edgeBlocked(mapId, x, y, 0);
            case 1 -> (edgeBlocked(mapId, x, y, 0)
                        || edgeBlocked(mapId, x, y - 1, 1))
                    && (edgeBlocked(mapId, x, y, 1)
                        || edgeBlocked(mapId, x + 1, y, 0));
            case 2 -> edgeBlocked(mapId, x, y, 1);
            case 3 -> (edgeBlocked(mapId, x, y, 1)
                        || edgeBlocked(mapId, x + 1, y + 1, 0))
                    && (edgeBlocked(mapId, x, y + 1, 0)
                        || edgeBlocked(mapId, x, y + 1, 1));
            case 4 -> edgeBlocked(mapId, x, y + 1, 0);
            case 5 -> (edgeBlocked(mapId, x, y + 1, 0)
                        || edgeBlocked(mapId, x - 1, y + 1, 1))
                    && (edgeBlocked(mapId, x - 1, y, 1)
                        || edgeBlocked(mapId, x - 1, y + 1, 0));
            case 6 -> edgeBlocked(mapId, x - 1, y, 1);
            case 7 -> (edgeBlocked(mapId, x - 1, y, 1)
                        || edgeBlocked(mapId, x - 1, y, 0))
                    && (edgeBlocked(mapId, x - 1, y - 1, 1)
                        || edgeBlocked(mapId, x, y, 0));
            default -> throw new IllegalArgumentException("invalid heading: " + heading);
        };
    }

    boolean edgeBlocked(int mapId, int x, int y, int attribute) {
        Map<Integer, byte[]> sectors = maps.get(mapId);
        if (sectors == null) return true;
        int blockX = 0x7FFF + Math.floorDiv(x - 32704, 64);
        int blockY = 0x7FFF + Math.floorDiv(y - 32704, 64);
        int sectorKey = (blockX << 16) | (blockY & 0xFFFF);
        byte[] bits = sectors.get(sectorKey);
        if (bits == null) return true;
        int localX = Math.floorMod(x - 32704, 64);
        int localY = Math.floorMod(y - 32704, 64);
        int bit = (localY * 64 + localX) * 2 + attribute;
        return (bits[bit >>> 3] & (1 << (bit & 7))) != 0;
    }

    int mapCount() {
        return maps.size();
    }

    int blockedEdgeCount(int mapId) {
        return blockedEdgeCounts.getOrDefault(mapId, 0);
    }

    private static NativeLitoTownPassabilityCatalog load() {
        InputStream raw = NativeLitoTownPassabilityCatalog.class
                .getResourceAsStream(RESOURCE);
        if (raw == null) {
            throw new IllegalStateException(
                    "Required rental-town passability resource is missing: " + RESOURCE);
        }
        try (InputStream input = new BufferedInputStream(raw)) {
            for (byte expected : MAGIC) {
                if (input.read() != (expected & 0xFF)) {
                    throw new IllegalStateException(
                            "Invalid rental-town passability resource magic");
                }
            }
            int mapCount = readU16(input);
            if (mapCount != 2) {
                throw new IllegalStateException(
                        "Rental-town passability resource must contain exactly two maps");
            }
            Map<Integer, Map<Integer, byte[]>> maps = new HashMap<>();
            Map<Integer, Integer> counts = new HashMap<>();
            for (int mapIndex = 0; mapIndex < mapCount; mapIndex++) {
                int mapId = readU16(input);
                int sectorCount = readU16(input);
                NativeLitoTownCatalog.Town town = NativeLitoTownCatalog.forMap(mapId);
                if (town == null || sectorCount != town.loadedSectors().size()
                        || maps.containsKey(mapId)) {
                    throw new IllegalStateException(
                            "Invalid rental-town passability map record: " + mapId);
                }
                Map<Integer, byte[]> sectors = new HashMap<>();
                int blockedEdges = 0;
                for (int sectorIndex = 0; sectorIndex < sectorCount; sectorIndex++) {
                    int key = readI32(input);
                    byte[] bits = input.readNBytes(BYTES_PER_SECTOR);
                    if (bits.length != BYTES_PER_SECTOR
                            || !town.loadedSectors().contains(key)
                            || sectors.put(key, bits) != null) {
                        throw new IllegalStateException(
                                "Invalid rental-town passability sector: map=" + mapId
                                        + " key=" + Integer.toUnsignedString(key, 16));
                    }
                    for (byte value : bits) {
                        blockedEdges += Integer.bitCount(value & 0xFF);
                    }
                }
                maps.put(mapId, Map.copyOf(sectors));
                counts.put(mapId, blockedEdges);
            }
            if (input.read() != -1) {
                throw new IllegalStateException(
                        "Trailing rental-town passability resource bytes");
            }
            return new NativeLitoTownPassabilityCatalog(maps, counts);
        } catch (IOException error) {
            throw new IllegalStateException(
                    "Cannot read rental-town passability resource: " + RESOURCE, error);
        }
    }

    private static int readU16(InputStream input) throws IOException {
        int low = input.read();
        int high = input.read();
        if ((low | high) < 0) throw new IOException("truncated u16");
        return low | (high << 8);
    }

    private static int readI32(InputStream input) throws IOException {
        int b0 = input.read();
        int b1 = input.read();
        int b2 = input.read();
        int b3 = input.read();
        if ((b0 | b1 | b2 | b3) < 0) throw new IOException("truncated i32");
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }
}
