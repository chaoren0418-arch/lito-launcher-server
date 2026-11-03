package local.lito381.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Native LiTo 3.81C Tournament map geometry.
 *
 * <p>Map ids in the protected Tournament client reuse 500-series ids that
 * mean something else in the older Lineage world database.  The exact loaded
 * sector set and safe spawn candidates below are derived from
 * {@code LiTo/LTmap/<map-id>/*.s32}.  Each S32 filename supplies its 64x64
 * coordinate sector. Spawn anchors and movement use the Layer3 directional
 * edges exactly as consumed by LiTo.bin {@code sub_48B640}.  Where an original
 * LiTo.bin runtime capture proves a better operational spawn, that coordinate
 * takes precedence over the deterministic resource candidate.</p>
 */
final class ArenaMapCatalog {
    private static final int[][] SERVICE_DIRECTIONS = {
            {0, 1}, {-1, 0}, {0, -1}, {-1, 1},
            {1, -1}, {1, 0}, {1, 1}, {-1, -1}
    };

    record Point(int x, int y) {}

    record Arena(int mapId, int minX, int maxX, int minY, int maxY,
                 Set<Integer> loadedSectors, List<Point> spawns, Point featurePoint) {
        Arena {
            loadedSectors = Set.copyOf(loadedSectors);
            spawns = List.copyOf(spawns);
            if (minX > maxX || minY > maxY) {
                throw new IllegalArgumentException("invalid arena bounds: " + mapId);
            }
            if (spawns.isEmpty() || featurePoint == null) {
                throw new IllegalArgumentException("arena anchors are missing: " + mapId);
            }
        }

        Point spawn(int index) {
            return spawns.get(Math.floorMod(index, spawns.size()));
        }

        Point botSpawn() {
            return spawn(1);
        }

        boolean contains(int x, int y) {
            if (x < minX || x > maxX || y < minY || y > maxY) return false;
            // Empty is reserved for small synthetic unit-test rectangles.
            return loadedSectors.isEmpty()
                    || loadedSectors.contains(sectorKeyForCoordinate(x, y));
        }

        boolean nativeStandingFloor(int x, int y) {
            return contains(x, y) && (loadedSectors.isEmpty()
                    || ArenaPassabilityCatalog.get().isStandingPassable(mapId, x, y));
        }

        boolean nativeCanStep(int fromX, int fromY, int toX, int toY) {
            return contains(fromX, fromY) && contains(toX, toY)
                    && (loadedSectors.isEmpty()
                        || ArenaPassabilityCatalog.get().canStep(
                                mapId, fromX, fromY, toX, toY));
        }

        /**
         * The protected LiTo maps are battle arenas, not the same-numbered
         * classic-world mapids rows.  Their native flow already creates and
         * transfers controlled companions, so pet recall is enabled for every
         * arena admitted by this catalog.
         */
        boolean allowsPetRecall() {
            return true;
        }
    }

    private static final Map<Integer, Arena> ARENAS = Map.ofEntries(
            arena(504, "7fff8000 80007fff 80008000 80008001 80017fff 80018000 80018001",
                    "32780,32783 32819,32817 32784,32817 32815,32783 32798,32799 32817,32800 32801,32817 32782,32800", 32800, 32800),
            arena(505, "80008000",
                    "32799,32806 32803,32807 32811,32797 32794,32798 32803,32796 32807,32790 32799,32798 32799,32789", 32802, 32798),
            arena(506, "7fff7fff 80007fff 80008000 80008001 80018000 80018001",
                    "32708,32767 32708,32708 32708,32766 32766,32708 32737,32737 32708,32737 32737,32708 32737,32766", 32735, 32735),
            arena(508, "80008000",
                    "32790,32792 32804,32808 32790,32807 32804,32793 32796,32799 32797,32806 32803,32800 32797,32792", 32797, 32800),
            arena(509, "80008000",
                    "32789,32809 32789,32792 32789,32808 32804,32793 32797,32801 32789,32800 32796,32793 32796,32809", 32796, 32800),
            arena(510, "7fff7fff 7fff8000 7fff8001 80007fff 80008000 80008001 80017fff 80018000 80018001",
                    "32766,32819 32820,32766 32770,32781 32828,32816 32799,32796 32766,32810 32792,32765 32834,32789", 32800, 32798),
            arena(512, "7fff7fff 7fff8000 7fff8001 80007fff 80008000 80008001 80017fff 80018000 80018001",
                    "32767,32771 32835,32826 32818,32772 32785,32835 32763,32807 32809,32805 32838,32796 32793,32759", 32808, 32802),
            arena(513, "7fff7fff 7fff8000 7fff8001 80007ffe 80007fff 80008000 80008001 80017ffe 80017fff 80018000 80018001 80027ffe 80027fff 80028000",
                    "32798,32803 32919,32679 32825,32709 32888,32776 32924,32733 32867,32674 32797,32752 32847,32803", 32852, 32737),
            arena(514, "80008000",
                    "32798,32789 32802,32814 32797,32805 32807,32805 32798,32794 32807,32793 32802,32808 32797,32810", 32799, 32804),
            arena(516, "80008000",
                    "32793,32786 32813,32808 32793,32807 32813,32787 32802,32796 32803,32806 32812,32797 32803,32786", 32803, 32797),
            arena(517, "7ffe7fff 7ffe8000 7ffe8001 7fff7fff 7fff8000 7fff8001 80007fff 80008000 80008001",
                    "32706,32768 32694,32780 32726,32824 32729,32772 32716,32798 32749,32831 32742,32808 32709,32767", 32724, 32802),
            arena(518, "7ffe7ffd 7fff7ffd 7fff7ffe 7fff7fff 7fff8000 80007ffd 80007ffe 80007fff 80008000",
                    "32644,32639 32644,32580 32644,32638 32702,32580 32673,32609 32644,32609 32673,32580 32673,32638", 32671, 32607),
            arena(519, "7fff7ffd 7fff7ffe 7fff7fff 7fff8000 80007ffd 80007ffe 80007fff 80008000",
                    "32758,32792 32765,32654 32762,32725 32778,32690 32759,32761 32757,32677 32775,32744 32761,32702", 32766, 32721),
            arena(520, "7fff7ffd 7fff7ffe 7fff7fff 7fff8000 80007ffd 80007ffe 80007fff 80008000",
                    "32758,32792 32765,32654 32762,32725 32778,32690 32759,32761 32757,32677 32775,32744 32761,32702", 32766, 32721),
            arena(521, "80008000 80008001 80018000 80018001 80028000 80028001",
                    "32800,32821 32934,32840 32866,32838 32901,32820 32830,32842 32846,32820 32908,32843 32807,32842", 32869, 32834),
            arena(522, "80008000 80008001 80018000 80018001 80028000 80028001",
                    "32800,32821 32934,32840 32866,32838 32901,32820 32830,32842 32846,32820 32908,32843 32807,32842", 32869, 32834),
            arena(524, "7fff7fff 7fff8000 7fff8001 80007fff 80008000 80008001 80018000 80018001",
                    "32772,32781 32821,32828 32778,32821 32815,32787 32791,32791 32801,32815 32822,32807 32771,32795", 32791, 32808),
            arena(525, "7ffe8001 7ffe8002 7ffe8003 7fff8000 7fff8001 7fff8002 7fff8003 80007fff 80008000 80008001 80018000",
                    "32644,32960 32644,33019 32691,32960 32729,33019 32644,32970 32686,33004 32729,32977 32767,33002", 32703, 32991),
            arena(526, "7fff8000 80007fff 80008000 80018000",
                    "32788,32789 32824,32806 32810,32789 32799,32807 32824,32791 32811,32805 32788,32802 32799,32794", 32810, 32797),
            arena(527, "7fff7fff 7fff8000 7fff8001 80007fff 80008000 80008001 80017fff 80018000 80018001 80027fff 80028000 80028001 80037fff 80038000 80038001 80047fff 80048000 80048001",
                    "33034,32801 32775,32775 32904,32821 32968,32774 32825,32826 32862,32780 32988,32827 32775,32825", 32899, 32798),
            arena(528, "7fff7fff 7fff8000 7fff8001 80007fff 80008000",
                    "32823,32831 32708,32708 32827,32711 32770,32767 32710,32767 32768,32708 32827,32773 32775,32820", 32780, 32755),
            arena(529, "7fff8000 7fff8001 80008000 80008001",
                    "32747,32787 32747,32825 32782,32823 32782,32787 32763,32806 32781,32805 32765,32789 32764,32823", 32764, 32807),
            arena(530, "7ffe7fff 7ffe8000 7ffe8001 7fff7fff 7fff8000 7fff8001 80007fff 80008000 80008001",
                    "32709,32791 32761,32809 32742,32782 32726,32818 32740,32803 32709,32809 32761,32790 32725,32784", 32735, 32800),
            arena(531, "7ffe8000 7fff7fff 7fff8000 7fff8001 80008000",
                    "32726,32809 32721,32797 32745,32789 32737,32809 32731,32788 32750,32809 32758,32789 32726,32812", 32739, 32793),
            arena(532, "7ffe8000 7ffe8001 7fff7fff 7fff8000 7fff8001 7fff8002 80007fff 80008000 80008001 80008002 80018000 80018001 80018002",
                    "32719,32780 32818,32880 32798,32801 32740,32858 32752,32813 32785,32846 32830,32835 32766,32768", 32772, 32826),
            arena(534, "7ffe7fff 7ffe8000 7ffe8001 7ffe8002 7fff7fff 7fff8000 7fff8001 7fff8002 80007fff 80008000 80008001 80008002 80017fff 80018000 80018001 80018002",
                    "32706,32777 32829,32895 32827,32776 32710,32895 32767,32834 32708,32836 32769,32893 32826,32836", 32768, 32841),
            arena(535, "7ffd8000 7ffd8001 7ffd8002 7ffd8003 7ffd8004 7ffe8000 7ffe8001 7ffe8002 7ffe8003 7ffe8004 7fff8000 7fff8001 7fff8002 7fff8003 7fff8004 80008000 80008001 80008002 80008003 80008004 80018000 80018001 80018002 80018003 80018004",
                    "32641,32835 32641,33019 32645,32835 32826,33019 32733,32929 32737,32837 32828,32927 32641,32927", 32735, 32927),
            arena(540, "7ffe7fff 7ffe8000 7ffe8001 7fff7fff 7fff8000 7fff8001 80007ffe 80007fff 80008000 80008001 80017ffe 80017fff 80018000",
                    "32709,32829 32764,32772 32709,32773 32764,32828 32736,32801 32736,32772 32737,32829 32709,32802", 32737, 32800),
            arena(547, "7ffd7fff 7ffd8000 7ffd8001 7ffe7fff 7ffe8000 7ffe8001 7fff7fff 7fff8000 7fff8001 80007fff 80008000 80008001",
                    "32752,32773 32632,32848 32666,32761 32704,32826 32755,32824 32630,32797 32711,32777 32663,32821", 32685, 32801),
            arena(548, "7ffd8001 7ffe7fff 7ffe8000 7ffe8001 7ffe8002 7fff7fff 7fff8000 7fff8001 7fff8002 80007fff 80008000 80008001 80008002",
                    "32754,32776 32705,32916 32775,32863 32698,32836 32713,32788 32728,32874 32744,32828 32695,32884", 32734, 32848),
            arena(549, "7ffe8001 7ffe8002 7ffe8003 7ffe8004 7fff8001 7fff8002 7fff8003 7fff8004 80008001 80008002 80008003 80008004 80018001 80018002 80018003 80018004",
                    "32673,33068 32858,32877 32676,32895 32845,33050 32774,32958 32689,32982 32757,33044 32770,32871", 32760, 32969),
            arena(550, "7ffe8001 7ffe8002 7ffe8003 7ffe8004 7fff8001 7fff8002 7fff8003 7fff8004 80008001 80008002 80008003 80008004 80018001 80018002 80018003 80018004",
                    "32673,33068 32858,32877 32676,32895 32845,33050 32774,32958 32689,32982 32757,33044 32770,32871", 32760, 32969),
            arena(551, "7fff7fff 7fff8000 80008000",
                    "32736,32810 32752,32771 32726,32783 32763,32797 32745,32791 32737,32772 32751,32809 32726,32798", 32744, 32790),
            arena(552, "7fff7fff 7fff8000 80007fff 80008000",
                    "32788,32784 32740,32751 32751,32784 32785,32751 32761,32759 32775,32775 32741,32769 32795,32766", 32761, 32767),
            arena(553, "7fff7ffc 7fff7ffd 7fff7ffe 7fff7fff 7fff8000 80007ffc 80007ffd 80007ffe 80007fff 80008000 80017ffc 80017ffd 80017ffe 80017fff 80018000",
                    "32844,32788 32779,32576 32747,32707 32851,32676 32763,32788 32786,32647 32831,32607 32835,32732", 32792, 32684),
            arena(554, "7fff8000 7fff8001 7fff8002 80008000 80008001 80008002",
                    "32801,32885 32755,32785 32727,32872 32791,32830 32745,32830 32774,32865 32780,32802 32800,32856", 32762, 32838),
            arena(562, "7ffd8001 7ffd8002 7ffd8003 7ffe8000 7ffe8001 7ffe8002 7ffe8003 7fff8000 7fff8001 7fff8002 7fff8003 80008000 80008001 80008002 80008003 80018000 80018001 80018002 80018003",
                    "32643,32848 32845,32966 32706,32972 32767,32863 32641,32927 32774,32939 32835,32889 32714,32905", 32737, 32916),
            arena(563, "7ffd8001 7ffd8002 7ffd8003 7ffe8000 7ffe8001 7ffe8002 7ffe8003 7fff8000 7fff8001 7fff8002 7fff8003 80008000 80008001 80008002 80008003 80018000 80018001 80018002 80018003",
                    "32643,32848 32845,32966 32706,32972 32767,32863 32641,32927 32774,32939 32835,32889 32714,32905", 32737, 32916),
            arena(564, "7fff8000",
                    "32729,32797 32741,32801 32723,32785 32749,32785 32735,32799 32749,32799 32736,32812 32723,32798", 32736, 32800),
            arena(565, "7fff8000 7fff8001 80008000 80008001",
                    "32740,32852 32799,32814 32755,32811 32782,32852 32736,32827 32799,32837 32777,32818 32761,32845", 32768, 32818),
            arena(566, "7ffe8000 7ffe8001 7fff8000 7fff8001",
                    "32676,32872 32748,32786 32664,32792 32738,32851 32707,32814 32670,32832 32711,32872 32740,32818", 32707, 32826),
            arena(569, "7fff8000 7fff8001 80008000 80008001",
                    "32740,32852 32799,32814 32755,32811 32782,32852 32736,32827 32799,32837 32777,32818 32761,32845", 32768, 32818),
            arena(570, "7ffe8000 7ffe8001 7fff8000 7fff8001",
                    "32676,32872 32748,32786 32664,32792 32738,32851 32707,32814 32670,32832 32711,32872 32740,32818", 32707, 32826),
            arena(573, "7fff8000 7fff8001 80008000 80008001",
                    "32724,32784 32729,32879 32724,32791 32814,32865 32765,32833 32767,32784 32773,32879 32724,32837", 32767, 32831),
            arena(574, "7fff8000 7fff8001 80008000 80008001",
                    "32724,32784 32730,32879 32724,32791 32814,32865 32769,32828 32725,32835 32767,32784 32773,32878", 32767, 32831)
    );

    private ArenaMapCatalog() {}

    static Arena forMap(int mapId) {
        Arena exact = ARENAS.get(mapId);
        if (exact == null) {
            throw new IllegalArgumentException("unsupported native LiTo arena map: " + mapId);
        }
        return exact;
    }

    static Set<Integer> mapIds() {
        return ARENAS.keySet();
    }

    /**
     * Places optional compatibility-service actors near, but never directly
     * around, the first player spawn.  The old compatibility bootstrap used
     * the adjacent screen area for repair/shop/warehouse/castle actors.  Those
     * hit boxes intercepted ground clicks before LiTo.bin could produce
     * C_MOVE, even though server-side movement admission itself remained open.
     */
    static Point serviceSpawn(Arena arena, int slot) {
        if (slot < 0 || slot >= SERVICE_DIRECTIONS.length) {
            throw new IllegalArgumentException("invalid service slot: " + slot);
        }
        List<Point> selected = new ArrayList<>();
        Set<Point> reserved = new HashSet<>(arena.spawns());
        reserved.add(arena.featurePoint());
        Point origin = arena.spawn(0);
        for (int currentSlot = 0; currentSlot < SERVICE_DIRECTIONS.length;
             currentSlot++) {
            int[] direction = SERVICE_DIRECTIONS[currentSlot];
            Point candidate = null;
            for (int distance = 2; distance <= 12 && candidate == null; distance++) {
                Point point = new Point(origin.x() + direction[0] * distance,
                        origin.y() + direction[1] * distance);
                if (serviceFloor(arena, point, reserved, selected)) candidate = point;
            }
            // Irregular arenas can close one of the preferred rays. Search a
            // bounded ring instead of reflecting onto another slot and
            // accidentally stacking two service actors on the same tile.
            for (int radius = 2; radius <= 16 && candidate == null; radius++) {
                for (int dx = -radius; dx <= radius && candidate == null; dx++) {
                    for (int dy = -radius; dy <= radius; dy++) {
                        if (Math.max(Math.abs(dx), Math.abs(dy)) != radius) continue;
                        Point point = new Point(origin.x() + dx, origin.y() + dy);
                        if (serviceFloor(arena, point, reserved, selected)) {
                            candidate = point;
                            break;
                        }
                    }
                }
            }
            if (candidate == null) return null;
            selected.add(candidate);
        }
        return selected.get(slot);
    }

    private static boolean serviceFloor(Arena arena, Point point,
                                        Set<Point> reserved, List<Point> selected) {
        if (reserved.contains(point) || selected.contains(point)
                || !arena.nativeStandingFloor(point.x(), point.y())) return false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if ((dx != 0 || dy != 0) && arena.nativeCanStep(
                        point.x(), point.y(), point.x() + dx, point.y() + dy)) {
                    return true;
                }
            }
        }
        return false;
    }

    static Arena rectangularForTest(int mapId, int minX, int maxX, int minY, int maxY) {
        Point center = new Point(minX + (maxX - minX) / 2,
                minY + (maxY - minY) / 2);
        return new Arena(mapId, minX, maxX, minY, maxY, Set.of(),
                List.of(center), center);
    }

    private static Map.Entry<Integer, Arena> arena(int mapId, String sectorText,
                                                    String spawnText,
                                                    int featureX, int featureY) {
        Set<Integer> sectors = new HashSet<>();
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (String token : sectorText.split(" ")) {
            if (token.length() != 8) {
                throw new IllegalArgumentException("invalid sector token: " + token);
            }
            int key = (int) Long.parseUnsignedLong(token, 16);
            if (!sectors.add(key)) {
                throw new IllegalArgumentException("duplicate sector: " + token);
            }
            int blockX = (key >>> 16) & 0xFFFF;
            int blockY = key & 0xFFFF;
            int beginX = sectorBegin(blockX);
            int beginY = sectorBegin(blockY);
            minX = Math.min(minX, beginX);
            maxX = Math.max(maxX, beginX + 63);
            minY = Math.min(minY, beginY);
            maxY = Math.max(maxY, beginY + 63);
        }

        List<Point> spawns = new ArrayList<>();
        for (String token : spawnText.split(" ")) {
            String[] coordinates = token.split(",", -1);
            if (coordinates.length != 2) {
                throw new IllegalArgumentException("invalid spawn token: " + token);
            }
            spawns.add(new Point(Integer.parseInt(coordinates[0]),
                    Integer.parseInt(coordinates[1])));
        }
        if (spawns.size() != 8 || new HashSet<>(spawns).size() != 8) {
            throw new IllegalArgumentException("native arena needs eight distinct spawns: "
                    + mapId);
        }
        Point feature = new Point(featureX, featureY);
        Arena result = new Arena(mapId, minX, maxX, minY, maxY,
                sectors, spawns, feature);
        for (Point spawn : spawns) {
            if (!result.contains(spawn.x(), spawn.y())) {
                throw new IllegalArgumentException("spawn outside native sectors: "
                        + mapId + " " + spawn);
            }
            if (distanceSquared(spawn, feature) < 2) {
                throw new IllegalArgumentException("spawn overlaps feature anchor: "
                        + mapId + " " + spawn);
            }
        }
        if (!result.contains(feature.x(), feature.y())) {
            throw new IllegalArgumentException("feature outside native sectors: " + mapId);
        }
        return Map.entry(mapId, result);
    }

    private static int sectorKeyForCoordinate(int x, int y) {
        int blockX = 0x7FFF + Math.floorDiv(x - 32704, 64);
        int blockY = 0x7FFF + Math.floorDiv(y - 32704, 64);
        return (blockX << 16) | (blockY & 0xFFFF);
    }

    private static int sectorBegin(int block) {
        return (block - 0x7FFF) * 64 + 32704;
    }

    private static int distanceSquared(Point left, Point right) {
        int dx = left.x() - right.x();
        int dy = left.y() - right.y();
        return dx * dx + dy * dy;
    }
}
