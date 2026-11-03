package local.lito381.server;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Native eight-direction collision edges derived from LiTo LTmap Layer3.
 *
 * <p>Each logical tile contributes Attribute1 and Attribute2 bit zero.  A set
 * bit closes that boundary.  {@link #canStep(int, int, int, int, int)} mirrors
 * the cardinal and diagonal tests in LiTo.bin {@code sub_48B640}.</p>
 */
final class ArenaPassabilityCatalog {
    static final String RESOURCE = "/battle/arena-passable-v1.bin";
    static final String SOURCE = "LTmap-Layer3-native-sub_48B640-directional";
    private static final byte[] MAGIC = {'L', 'T', 'A', 'P', 2};
    private static final int BYTES_PER_SECTOR = 1024;

    private final Map<Integer, Map<Integer, byte[]>> maps;
    private final Map<Integer, Integer> blockedEdgeCounts;

    private ArenaPassabilityCatalog(Map<Integer, Map<Integer, byte[]>> maps,
                                    Map<Integer, Integer> blockedEdgeCounts) {
        this.maps = Map.copyOf(maps);
        this.blockedEdgeCounts = Map.copyOf(blockedEdgeCounts);
    }

    static ArenaPassabilityCatalog get() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        private static final ArenaPassabilityCatalog INSTANCE = load();
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
        if (dx == 0 && dy == 0) return isStandingPassable(mapId, fromX, fromY);
        if (Math.abs(dx) > 1 || Math.abs(dy) > 1
                || !isStandingPassable(mapId, toX, toY)) return false;
        int heading = switch ((dx + 1) + (dy + 1) * 3) {
            case 1 -> 0; // north
            case 2 -> 1; // north-east
            case 5 -> 2; // east
            case 8 -> 3; // south-east
            case 7 -> 4; // south
            case 6 -> 5; // south-west
            case 3 -> 6; // west
            case 0 -> 7; // north-west
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

    private static ArenaPassabilityCatalog load() {
        InputStream raw = ArenaPassabilityCatalog.class.getResourceAsStream(RESOURCE);
        if (raw == null) {
            throw new IllegalStateException("Required arena passability resource is missing: "
                    + RESOURCE);
        }
        try (InputStream input = new BufferedInputStream(raw)) {
            for (byte expected : MAGIC) {
                int actual = input.read();
                if (actual != (expected & 0xFF)) {
                    throw new IllegalStateException("Invalid arena passability resource magic");
                }
            }
            int mapCount = readU16(input);
            if (mapCount <= 0) {
                throw new IllegalStateException("Arena passability resource is empty");
            }
            Map<Integer, Map<Integer, byte[]>> maps = new HashMap<>();
            Map<Integer, Integer> counts = new HashMap<>();
            for (int mapIndex = 0; mapIndex < mapCount; mapIndex++) {
                int mapId = readU16(input);
                int sectorCount = readU16(input);
                if (sectorCount <= 0 || maps.containsKey(mapId)) {
                    throw new IllegalStateException(
                            "Invalid arena passability map record: " + mapId);
                }
                Map<Integer, byte[]> sectors = new HashMap<>();
                int blockedEdges = 0;
                for (int sectorIndex = 0; sectorIndex < sectorCount; sectorIndex++) {
                    int key = readI32(input);
                    byte[] bits = input.readNBytes(BYTES_PER_SECTOR);
                    if (bits.length != BYTES_PER_SECTOR || sectors.put(key, bits) != null) {
                        throw new IllegalStateException(
                                "Invalid arena passability sector: map=" + mapId
                                        + " key=" + Integer.toUnsignedString(key, 16));
                    }
                    for (byte value : bits) blockedEdges += Integer.bitCount(value & 0xFF);
                }
                maps.put(mapId, Map.copyOf(sectors));
                counts.put(mapId, blockedEdges);
            }
            if (input.read() != -1) {
                throw new IllegalStateException("Trailing arena passability resource bytes");
            }
            return new ArenaPassabilityCatalog(maps, counts);
        } catch (IOException error) {
            throw new IllegalStateException(
                    "Cannot read arena passability resource: " + RESOURCE, error);
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
