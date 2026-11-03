package local.lito381.server;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hash-locked native 3.81 inn contract shared by the nine retained innkeepers.
 *
 * <p>The retained server uses one pool of sixteen lease rows per innkeeper.
 * At most twelve active rows may be ordinary rooms and at most four may be
 * meeting halls.  The row number is not a room/hall type: that flag belongs to
 * the active lease and is cleared when the lease is returned.</p>
 */
final class ClassicWorldHotelCatalog {
    static final long KEY_ITEM_ID = 40312;
    static final long ADENA_ITEM_ID = InventoryCatalog.ADENA;
    static final int PRICE_PER_KEY = 300;
    static final int ROOM_KEY_MINIMUM = 1;
    static final int ROOM_KEY_MAXIMUM = 8;
    static final int HALL_KEY_MINIMUM = 1;
    static final int HALL_KEY_MAXIMUM = 16;
    static final int LEASE_SLOT_COUNT = 16;
    static final int ROOM_CAPACITY = 12;
    static final int HALL_CAPACITY = 4;
    static final long LEASE_SECONDS = 4L * 60L * 60L;
    static final int ACTIVE_LEASE_BASE_REFUND = 60;
    static final int REFUND_PER_RETURNED_KEY = 20;
    static final int CROWN_CHARACTER_TYPE = 0;
    static final int ENTRY_HEADING = 6;
    static final String INITIAL_HTML = "inn";
    static final String CHAOTIC_HTML = "Inn1";
    static final String ROOM_INPUT_HTML = "inn2";
    static final String INSUFFICIENT_ADENA_HTML = "inn3";
    static final String RENTED_HTML = "inn4";
    static final String ALREADY_ROOM_HTML = "inn5";
    static final String ROOMS_FULL_HTML = "inn6";
    static final String HALL_CROWN_ONLY_HTML = "inn10";
    static final String HALL_INPUT_HTML = "inn12";
    static final String ALREADY_HALL_HTML = "inn15";
    static final String HALLS_FULL_HTML = "inn16";
    static final String RETURNED_HTML = "inn20";
    static final String ROOM_ACTION = "room";
    static final String HALL_ACTION = "hall";
    static final String RETURN_ACTION = "return";
    static final String ENTER_ACTION = "enter";

    static final String MAPIDS_SOURCE_SHA256 =
            "BDB3139EE535BF29609A80887424E61CFAF643B0D05896E948CBA9D5E0261F65";
    static final String GETBACK_SOURCE_SHA256 =
            "EB7B3F85AA25626D556179CC8853928FCA26EB5F48FEC3CC8D766B840BAA7C90";
    static final String DUNGEON_SOURCE_SHA256 =
            "C3AF4AC04D50E57B8508F5A7CBD5DE1F505B6EB7531421511B51F2B0B91402D7";
    static final String KEY_ITEM_SOURCE_SHA256 =
            "9F3EE9AE4AA5C5EBA365BF92D2FA6BE94BBC527D02B338CA8AB71870B322DA48";
    static final String NPC_SPAWN_SOURCE_SHA256 =
            "E4EA7005C65786636CAC6265F1070792EAF6D43D978000A0485DF734B391D173";
    static final String NPC_ACTION_SOURCE_SHA256 =
            "A330A274FD7F8BDEBB233388DD2F24E76B90FB3D061A1ADF6CF6984BCE200E18";
    static final String RETAINED_C_NPC_ACTION_SHA256 =
            "E938CBF921A1BE9A2D7E3EA00F363177EAB102DF5D88D288F6D9865BC56EF306";
    static final String RETAINED_C_AMOUNT_SHA256 =
            "1302360A8270995968E1B83C40E54031721E8C4334F987CD68BC5E094F0D96C0";
    static final String RETAINED_S_HOW_MANY_KEY_SHA256 =
            "5A8C29B8C0D39052A558A722D5B654B61BDC30B8AE5E3CC502EA5F854160B7BC";
    static final String SELECTED_TEXT_PAK_SHA256 =
            "CE8FA5ADE2E69525F781B8DFF5681229FD4CFE8AC27496574E8F281182227439";

    record Location(int mapId, int x, int y, int heading) {
        Location {
            if (mapId < 0 || x <= 0 || y <= 0 || heading < 0 || heading > 7) {
                throw new IllegalArgumentException("invalid inn location");
            }
        }
    }

    record Inn(int npcId, int spawnId, int exteriorMapId, int x, int y,
               Location room, Location hall) {
        Inn {
            if (npcId <= 0 || spawnId <= 0 || exteriorMapId < 0
                    || x <= 0 || y <= 0 || room == null || hall == null
                    || room.mapId() == hall.mapId()) {
                throw new IllegalArgumentException("invalid inn definition");
            }
        }

        long objectId() {
            return ClassicWorldNpcCatalog.objectId(spawnId);
        }

        Location destination(boolean meetingHall) {
            return meetingHall ? hall : room;
        }
    }

    private static final List<Inn> INNS = List.of(
            inn(70012, 1329, 0, 32596, 32929,
                    16384, 32745, 32803, 16896, 32743, 32808),
            inn(70019, 1330, 4, 32627, 32758,
                    17408, 32743, 32803, 17920, 32744, 32807),
            inn(70031, 1331, 4, 33431, 32785,
                    18432, 32744, 32803, 18944, 32744, 32807),
            inn(70065, 1332, 4, 34062, 32252,
                    19456, 32744, 32803, 19968, 32744, 32807),
            inn(70070, 1333, 4, 32618, 33166,
                    20480, 32744, 32803, 20992, 32744, 32807),
            inn(70075, 1334, 4, 33106, 33377,
                    21504, 32744, 32803, 22016, 32744, 32807),
            inn(70084, 1335, 4, 33599, 33273,
                    22528, 32744, 32803, 23040, 32744, 32807),
            inn(70054, 565, 4, 33988, 33311,
                    23552, 32744, 32803, 24064, 32744, 32807),
            inn(70096, 1336, 440, 32448, 33045,
                    24576, 32744, 32803, 25088, 32744, 32807));

    static final List<Integer> MAP_IDS = INNS.stream()
            .flatMap(inn -> java.util.stream.Stream.of(
                    inn.room().mapId(), inn.hall().mapId()))
            .toList();

    static final Map<Integer, String> MAP_SOURCE_SHA256 = Map.ofEntries(
            Map.entry(16384, "06A852E49F4FB36DFB9C0249A46438F930256275344A32CFD0553CC1BAFBBA4B"),
            Map.entry(16896, "3C3C4C302E9F746A260983E702F2A7DEDF02027B105CF62715DB161D2239A9CD"),
            Map.entry(17408, "F03593A94C30562B57F64F947E0D121B73E4D0A37C4A221095ED1995ADDB8023"),
            Map.entry(17920, "B9F03197B66315215EB4A58A327BE092B525F44AC12CF4CB590ECE57C5565CD3"),
            Map.entry(18432, "0198A262F26A7339AE7CD8FBC50769878517BAE81711009524A945F23565D80D"),
            Map.entry(18944, "5D27B065F959EC474F1AB07A2B3F8E19895B62B02661095355CFD615A81F2B84"),
            Map.entry(19456, "0198A262F26A7339AE7CD8FBC50769878517BAE81711009524A945F23565D80D"),
            Map.entry(19968, "839C8222913B0BFD6F519BBC465F0FCF7777D4DDE7B54189A343381422EDB32F"),
            Map.entry(20480, "0198A262F26A7339AE7CD8FBC50769878517BAE81711009524A945F23565D80D"),
            Map.entry(20992, "839C8222913B0BFD6F519BBC465F0FCF7777D4DDE7B54189A343381422EDB32F"),
            Map.entry(21504, "0198A262F26A7339AE7CD8FBC50769878517BAE81711009524A945F23565D80D"),
            Map.entry(22016, "839C8222913B0BFD6F519BBC465F0FCF7777D4DDE7B54189A343381422EDB32F"),
            Map.entry(22528, "0198A262F26A7339AE7CD8FBC50769878517BAE81711009524A945F23565D80D"),
            Map.entry(23040, "839C8222913B0BFD6F519BBC465F0FCF7777D4DDE7B54189A343381422EDB32F"),
            Map.entry(23552, "0198A262F26A7339AE7CD8FBC50769878517BAE81711009524A945F23565D80D"),
            Map.entry(24064, "839C8222913B0BFD6F519BBC465F0FCF7777D4DDE7B54189A343381422EDB32F"),
            Map.entry(24576, "0198A262F26A7339AE7CD8FBC50769878517BAE81711009524A945F23565D80D"),
            Map.entry(25088, "839C8222913B0BFD6F519BBC465F0FCF7777D4DDE7B54189A343381422EDB32F"));

    static final Map<Integer, String> MAP_RESOURCE_SHA256 = Map.ofEntries(
            Map.entry(16384, "697B0F1619164C9DC0AE1154F6160B0947DCE59165458A96D6A7307726C4FAA5"),
            Map.entry(16896, "75753ED428F5E2CBBCC505E2C3708A8776F3081CE3E0B3601BA569DC7C1807A9"),
            Map.entry(17408, "96246EC1179889784BE9B944E2815E8269C93EE446B4D2340B52579BFC798166"),
            Map.entry(17920, "E417CCF94062550B7C65066040E1CBF0666DD67EA29291295311BAFE1EDD1395"),
            Map.entry(18432, "F65C507108338EE298465CDFB86E01DEEC08CC3555FCF576BF718A9190698462"),
            Map.entry(18944, "F95FD229014D00E326C5B09E3CC56D8D29AC95209D17C2BE2BE323A8E1245105"),
            Map.entry(19456, "D9E175A4EDCC13D6BA515CF8553A77D5D4C6B1EB4F298BB0816815D5BBA2AB67"),
            Map.entry(19968, "3F8F6FA1278C3C4EB6F60D29E1F6976689BE99AF8C612E3EB475ADFBB882081B"),
            Map.entry(20480, "F383759046EF33B419D8FE1B3051F5613EFFE2FD1F138CEB77B143FEBB531392"),
            Map.entry(20992, "6E3A227A2D2062FB5AD9E3D52E9E2C4A9122C30132F648F14AC5A91F9BC0F732"),
            Map.entry(21504, "20754C7F568A4CDFBFBB27E77A914175EDCEDB870EEC46279DF8C7B1946EEB75"),
            Map.entry(22016, "1264E978896565B728D2239BE6AD8553695F02CFA3963DD8A78D2DA12A5273E1"),
            Map.entry(22528, "4C0ED22B83DA1B9089714DF130D5A8E10CF8DC59C9A526BEA589B71AC75E4A72"),
            Map.entry(23040, "E6F3CB0CE347B426E6E72F12510D2DFE31DFAA375D6C7D27FD24C09FAC4999DB"),
            Map.entry(23552, "DD04A65CABF6511DFF9D4740EE5A0B3C57FA829558234B5C37C9A0A4B3F20428"),
            Map.entry(24064, "94F7E582E4223DA4CEF6BCB4D37DEE25C3894AE24A35CA557F957418891D0620"),
            Map.entry(24576, "1EBC13B13D859BBC1A41EF2DDA13C1978099FDBADAFD53EFCE025083F33EF916"),
            Map.entry(25088, "7A9654BE9ABB3EE6D98DCDB1B66538ADDE7B766575BB2D67FAF8B5F641BFECC5"));

    private static final Map<Integer, Inn> BY_NPC;
    private static final Map<Integer, Inn> BY_MAP;

    static {
        Map<Integer, Inn> byNpc = new LinkedHashMap<>();
        Map<Integer, Inn> byMap = new LinkedHashMap<>();
        for (Inn inn : INNS) {
            if (byNpc.put(inn.npcId(), inn) != null
                    || byMap.put(inn.room().mapId(), inn) != null
                    || byMap.put(inn.hall().mapId(), inn) != null) {
                throw new ExceptionInInitializerError("duplicate native inn contract");
            }
        }
        BY_NPC = Map.copyOf(byNpc);
        BY_MAP = Map.copyOf(byMap);
    }

    private ClassicWorldHotelCatalog() {}

    static List<Inn> inns() {
        return INNS;
    }

    static Inn forNpc(int npcId) {
        return BY_NPC.get(npcId);
    }

    static Inn forMap(int mapId) {
        return BY_MAP.get(mapId);
    }

    static boolean isHotelMap(int mapId) {
        return BY_MAP.containsKey(mapId);
    }

    static boolean isInnkeeper(ClassicWorldNpcCatalog.StaticNpc npc) {
        Inn inn = npc == null ? null : forNpc(npc.npcId());
        return inn != null && npc.spawnId() == inn.spawnId()
                && npc.mapId() == inn.exteriorMapId()
                && npc.x() == inn.x() && npc.y() == inn.y();
    }

    static boolean meetingHallMap(int mapId) {
        Inn inn = forMap(mapId);
        return inn != null && inn.hall().mapId() == mapId;
    }

    static int maximumKeys(boolean meetingHall) {
        return meetingHall ? HALL_KEY_MAXIMUM : ROOM_KEY_MAXIMUM;
    }

    static String inputHtml(boolean meetingHall) {
        return meetingHall ? HALL_INPUT_HTML : ROOM_INPUT_HTML;
    }

    private static Inn inn(
            int npcId, int spawnId, int exteriorMapId, int x, int y,
            int roomMapId, int roomX, int roomY,
            int hallMapId, int hallX, int hallY) {
        return new Inn(npcId, spawnId, exteriorMapId, x, y,
                new Location(roomMapId, roomX, roomY, ENTRY_HEADING),
                new Location(hallMapId, hallX, hallY, ENTRY_HEADING));
    }
}
