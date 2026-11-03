package local.lito381.server;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resource-backed LiTo rental-hotel policy for maps 501..503.
 *
 * <p>The selected client proves the three S32 maps, their collision geometry,
 * the {@code inn/inn2/inn4/inn20} pages, a free lease, one-hour ordinary rooms,
 * a two-hour meeting hall and automatic return on expiry or town departure.
 * It does not contain the historical server's room-type threshold or landing
 * coordinates.  The retained 3.81 inn implementations independently agree on
 * item 40312 and the selected base item row supplies useType/icon/gfx 255/59/151;
 * the LT help pages explicitly require handing one key to each friend.  The
 * operational map mapping below is therefore
 * deliberately labelled as a resource-size policy: one key uses map 501, two
 * through four use map 502, and five through eight use the much larger map 503.
 * No ordinary Lineage hotel price or four-hour lease is imported.</p>
 */
final class NativeLitoRentalHotelCatalog {
    static final String RESOURCE = "/rental/hotel-passability-v1.bin";
    static final String RESOURCE_SHA256 =
            "2F23CF2D50ACF66B76299D081C001AECDBE178468CFAAAF8F8E60A38D02A5187";
    static final String RESOURCE_AUTHORITY =
            "selected-client-LTmap-501-503-Layer3-sub_48B640";
    static final String TYPE_POLICY_AUTHORITY =
            "selected-S32-standing-area-order-plus-explicit-operational-policy";
    static final boolean HISTORICAL_MAP_TYPE_ASSIGNMENT_EXACT = false;
    static final boolean HISTORICAL_KEY_COUNT_THRESHOLDS_EXACT = false;
    static final boolean HISTORICAL_KEY_ITEM_RESOURCE_AVAILABLE = true;
    static final boolean HISTORICAL_KEY_TRANSFER_AVAILABLE = true;
    static final long KEY_ITEM_ID = ClassicWorldHotelCatalog.KEY_ITEM_ID;
    static final String KEY_ITEM_AUTHORITY =
            "retained-3.81-C_Amount-item40312-plus-selected-base-item-row";
    static final String KEY_TRANSFER_AUTHORITY =
            "selected-LitoHelp15-and-Aria_Inn-multiple-keys-handed-to-friends";
    static final int MINIMUM_KEYS = 1;
    static final int MAXIMUM_KEYS = 8;
    static final int RETURN_MAP_ID = 500;
    static final int RETURN_X = 32779;
    static final int RETURN_Y = 32832;
    static final int RETURN_HEADING = 2;
    static final String INPUT_HTML = "inn2";
    static final String RENTED_HTML = "inn4";
    static final String RETURNED_HTML = "inn20";

    enum Kind { SMALL_ROOM, ROOM, MEETING_HALL }

    record Room(
            Kind kind, int mapId, int minimumKeys, int maximumKeys,
            long durationSeconds, int entryX, int entryY, int entryHeading,
            int standingCells, String resourceSha256) {
        Room {
            if (kind == null || mapId < 501 || mapId > 503
                    || minimumKeys < MINIMUM_KEYS || maximumKeys > MAXIMUM_KEYS
                    || minimumKeys > maximumKeys || durationSeconds <= 0
                    || entryX <= 0 || entryX > 0xFFFF
                    || entryY <= 0 || entryY > 0xFFFF
                    || entryHeading < 0 || entryHeading > 7
                    || standingCells <= 0 || resourceSha256 == null
                    || resourceSha256.length() != 64) {
                throw new IllegalArgumentException("invalid LiTo rental-hotel room");
            }
        }

        boolean accepts(int keyCount) {
            return keyCount >= minimumKeys && keyCount <= maximumKeys;
        }
    }

    private static final List<Room> ROOMS = List.of(
            new Room(Kind.SMALL_ROOM, 501, 1, 1, 3_600,
                    32802, 32796, 4, 22,
                    "BB07BB6B75EB14665D8D905A96118BD7779DCB9BFC7C21D8E11A7A80E7CFD918"),
            new Room(Kind.ROOM, 502, 2, 4, 3_600,
                    32800, 32793, 4, 23,
                    "FCD6855ECD1BF1E5D500537E9CF1D4FF67E6ADEDA270DFBC79BDAF77FC73035A"),
            new Room(Kind.MEETING_HALL, 503, 5, 8, 7_200,
                    32798, 32799, 4, 61,
                    "D5FB0E003A6812319D09BCA37ECA31754E24DB75994F18F5CA8C3A05BC9BAE7D"));

    private static final byte[] MAGIC = {'L', 'T', 'H', 'P', 1};
    private static final int BYTES_PER_SECTOR = 1024;

    private final Map<Integer, byte[]> maps;
    private final Map<Integer, Integer> blockedEdgeCounts;

    private NativeLitoRentalHotelCatalog(
            Map<Integer, byte[]> maps, Map<Integer, Integer> blockedEdgeCounts) {
        this.maps = Map.copyOf(maps);
        this.blockedEdgeCounts = Map.copyOf(blockedEdgeCounts);
    }

    static NativeLitoRentalHotelCatalog get() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        private static final NativeLitoRentalHotelCatalog INSTANCE = load();
    }

    static List<Room> rooms() {
        return ROOMS;
    }

    static Room forKeyCount(int keyCount) {
        for (Room room : ROOMS) {
            if (room.accepts(keyCount)) return room;
        }
        return null;
    }

    static Room forMap(int mapId) {
        for (Room room : ROOMS) {
            if (room.mapId() == mapId) return room;
        }
        return null;
    }

    static boolean supportsMap(int mapId) {
        return forMap(mapId) != null;
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

    int blockedEdgeCount(int mapId) {
        return blockedEdgeCounts.getOrDefault(mapId, 0);
    }

    private boolean directionBlocked(int mapId, int x, int y, int heading) {
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

    private boolean edgeBlocked(int mapId, int x, int y, int attribute) {
        byte[] bits = maps.get(mapId);
        if (bits == null || x < 32768 || x > 32831 || y < 32768 || y > 32831) {
            return true;
        }
        int localX = x - 32768;
        int localY = y - 32768;
        int bit = (localY * 64 + localX) * 2 + attribute;
        return (bits[bit >>> 3] & (1 << (bit & 7))) != 0;
    }

    private static NativeLitoRentalHotelCatalog load() {
        InputStream raw = NativeLitoRentalHotelCatalog.class.getResourceAsStream(RESOURCE);
        if (raw == null) {
            throw new IllegalStateException("Required rental-hotel resource is missing: "
                    + RESOURCE);
        }
        try (InputStream input = new BufferedInputStream(raw)) {
            for (byte expected : MAGIC) {
                if (input.read() != (expected & 0xFF)) {
                    throw new IllegalStateException("Invalid rental-hotel resource magic");
                }
            }
            int count = readU16(input);
            if (count != ROOMS.size()) {
                throw new IllegalStateException("Rental-hotel resource map count changed");
            }
            Map<Integer, byte[]> maps = new HashMap<>();
            Map<Integer, Integer> blocked = new HashMap<>();
            for (int index = 0; index < count; index++) {
                int mapId = readU16(input);
                int blockedEdges = readU16(input);
                byte[] bits = input.readNBytes(BYTES_PER_SECTOR);
                if (!supportsMap(mapId) || bits.length != BYTES_PER_SECTOR
                        || maps.put(mapId, bits) != null) {
                    throw new IllegalStateException(
                            "Invalid rental-hotel resource map record: " + mapId);
                }
                blocked.put(mapId, blockedEdges);
            }
            if (input.read() != -1) {
                throw new IllegalStateException("Trailing rental-hotel resource bytes");
            }
            NativeLitoRentalHotelCatalog catalog =
                    new NativeLitoRentalHotelCatalog(maps, blocked);
            for (Room room : ROOMS) {
                if (!catalog.isStandingPassable(
                        room.mapId(), room.entryX(), room.entryY())) {
                    throw new IllegalStateException(
                            "Rental-hotel entry is not passable: " + room.mapId());
                }
            }
            return catalog;
        } catch (IOException error) {
            throw new IllegalStateException(
                    "Cannot read rental-hotel passability resource", error);
        }
    }

    private static int readU16(InputStream input) throws IOException {
        int low = input.read();
        int high = input.read();
        if ((low | high) < 0) throw new IOException("truncated u16");
        return low | (high << 8);
    }
}
