package local.lito381.server;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** Selected map4 getback rows plus retained L1TownLocation destinations. */
final class ClassicWorldGetbackCatalog {
    static final String GETBACK_TABLE_SOURCE_SHA256 =
            "186F017F608949988F8951C62A362E89E95ECDC72A8199895CDE0285FA181938";
    static final String TOWN_LOCATION_SOURCE_SHA256 =
            "DAEF00602E793678BFD1C9BC0E210D2E9856F2A7C45FC38D9D322A5A22A5EDE3";
    static final String MAP303_ABSENCE_SOURCE_SHA256 =
            "89E9FB154D6653B25664DF98A22A6C5917C13006C5FFAEEE7DCC754C05A41E65";
    static final String DIADRAD_SOURCE_SHA256 =
            ClassicWorldDiadradCatalog.GETBACK_SOURCE_SHA256;
    static final String MAP21_SOURCE_SHA256 =
            "741FF6D02AAAC1D5E8A311071E6133AEF6FACF87A34C3ACDE9E09142128001FE";
    static final String MAP17_SOURCE_SHA256 =
            ClassicWorldMapCatalog.MAP17_GETBACK_SOURCE_SHA256;
    static final String MAP22_SOURCE_SHA256 =
            ClassicWorldMapCatalog.MAP22_GETBACK_SOURCE_SHA256;
    static final String KNIGHT_CAVE_SOURCE_SHA256 =
            "A4E547FC78894669BC7BEA823175E84232D5F195728B14759E4F22B70C46E4B8";
    static final String ANT_CAVE_SOURCE_SHA256 =
            ClassicWorldMapCatalog.ANT_CAVE_GETBACK_SOURCE_SHA256;
    static final String MAP16_SOURCE_SHA256 =
            "FD74F2E14075C56686548E64593B77B2096ED50073E4637A90B9C641367D3AA6";
    static final String MAP67_SOURCE_SHA256 =
            "A1F96453400295F6C5B4EBF1C59C1C2F99D039F87ABA0A3F7BCC1F7B17493E93";
    static final String SLEEPING_DRAGON_SOURCE_SHA256 =
            "D7055E3189CA0740132CAEC9877C3F80DEF08D3AAC1B1093358B21DCB04B82FE";
    static final String MAP30_SOURCE_SHA256 =
            "506BA4A75FC53F989FA9E2D132FD5A6AD6BA15EBC927DB1EADF7EC696D4BD341";
    static final String MAP31_SOURCE_SHA256 =
            "210D9A00C765269EA79414A34961021A71985770DE2609755A1EED940A00C764";
    static final String MAP32_SOURCE_SHA256 =
            "205460B3D345E19BD070C6FCD1CE2B4FCC29109918E8CD9AFF36A4709B8AE31D";
    static final String MAP33_SOURCE_SHA256 =
            "7A3D41081387CD25AC4DD52280F3D76614ADD7483A97C4D2FF50FF13AD52E4BB";
    static final String DRAGON_VALLEY_DUNGEON_SOURCE_SHA256 =
            "1984FF57E5B9F62273D9A1B408006BC22767781B0D43A8A9CE42D016669A1E28";
    static final String DRAGON_VALLEY_DUNGEON_FLOORS_1_TO_4_SOURCE_SHA256 =
            "1E79CE03A3E808C3D541AD1BBFAA282171A24A21B30E9666C4EC964D7EBEE5EE";
    static final String MAP35_SOURCE_SHA256 =
            "FACF803491AFAF1A470ECEB4A19D9B18FE1B9497DCBBBD6EDFF59F18BC73110F";
    static final String MAP36_SOURCE_SHA256 =
            "6FD2A19BA04DE2E6A4411203853809E85BF8341EDE2F3BCA5B23C7AD9D813E7D";
    static final String MAP37_SOURCE_SHA256 =
            "A22D34BEDBD265BC9A1027F0D383DB4723F087F4E336DD3BC16391175F69327C";
    static final String DRAGON_VALLEY_DEEP_SOURCE_SHA256 =
            "0907AB320E588CE76BBE61A4D9E78F3CE0E2C624B37355CBCF038613EC00ED63";
    static final String MAP350_SOURCE_SHA256 =
            "76ABEEEB87228ED09A37E98CDB4177C4437AA90626EEB1D4BFC892992708E01D";
    static final String FAIRY_DUNGEONS_ABSENCE_SOURCE_SHA256 =
            "71071DF1C2501FC6C04A10F97F22B540CAB0DFA935C5F568ECF0220311738CA4";
    static final String MAP302_ABSENCE_SOURCE_SHA256 =
            "9C085F8BC0A0069739A6D4D3B787B3761DB85959056B8C5A0EBE75D80826A5FB";
    static final String MAP445_ABSENCE_SOURCE_SHA256 =
            "14EEDA7E06DAAEE06F7758DF68BCA61541765A0B11C8541A85CF2FEF3FB773CF";
    static final String FORGOTTEN_ISLAND_ROUTE_SOURCE_SHA256 =
            ClassicWorldMapCatalog.FORGOTTEN_ISLAND_GETBACK_SOURCE_SHA256;
    static final String MAP3_SOURCE_SHA256 =
            "795AD4F3CADBB6184317E01C95D4A1DF450DC7970AA05C0FCACA0C859C1BF5E6";
    static final String MAP237_SOURCE_SHA256 =
            ClassicWorldMapCatalog.MAP237_GETBACK_SOURCE_SHA256;
    static final String GLUDIO_DUNGEON_SOURCE_SHA256 =
            ClassicWorldMapCatalog.GLUDIO_DUNGEON_GETBACK_SOURCE_SHA256;
    static final String GIRAN_DUNGEON_SOURCE_SHA256 =
            ClassicWorldMapCatalog.GIRAN_DUNGEON_GETBACK_SOURCE_SHA256;
    static final String MAP52_SOURCE_SHA256 =
            ClassicWorldMapCatalog.MAP52_GETBACK_SOURCE_SHA256;
    static final String WINDAWOOD_CASTLE_SOURCE_SHA256 =
            ClassicWorldMapCatalog.WINDAWOOD_CASTLE_GETBACK_SOURCE_SHA256;
    static final String KENT_COMPONENT_SOURCE_SHA256 =
            ClassicWorldMapCatalog.KENT_COMPONENT_GETBACK_SOURCE_SHA256;
    static final String IVORY_TOWER_SOURCE_SHA256 =
            ClassicWorldIvoryTowerCatalog.GETBACK_SOURCE_SHA256;
    static final String MAP66_SOURCE_SHA256 =
            ClassicWorldDwarfCastleCatalog.MAP_GETBACK_SOURCE_SHA256;
    static final String ADEN_TOI_SOURCE_SHA256 =
            ClassicWorldAdenToiCatalog.GETBACK_SOURCE_SHA256;
    static final String HEINE_EVA_SOURCE_SHA256 =
            ClassicWorldHeineCatalog.GETBACK_SOURCE_SHA256;
    static final String MAP310_SOURCE_SHA256 =
            ClassicWorldRastabadOumCatalog.GETBACK_SOURCE_SHA256;
    static final String LASTABAD_GATE_SOURCE_SHA256 =
            ClassicWorldLastabadGateCatalog.GETBACK_SOURCE_SHA256;
    static final String WATER_DRAGON_SOURCE_SHA256 =
            "3BF9B04B6E1D788E361534A79F3AA49CEEEF84FAE5CE04471EDF1EC9764CA423";
    static final String ELEMENTAL_GRAVE_SOURCE_SHA256 =
            ClassicWorldElementalGraveCatalog.GETBACK_SOURCE_SHA256;
    static final String HOTEL_SOURCE_SHA256 =
            ClassicWorldHotelCatalog.GETBACK_SOURCE_SHA256;

    record Destination(int mapId, int x, int y, int townId) {
        Destination {
            if (mapId < 0 || x <= 0 || y <= 0 || townId < 0) {
                throw new IllegalArgumentException("invalid classic getback destination");
            }
        }
    }

    private record Area(
            int x1, int y1, int x2, int y2,
            int getbackX, int getbackY, int getbackMap,
            int townId, int elfTownId, int darkElfTownId) {
        boolean contains(int x, int y) {
            return x >= x1 && x <= x2 && y >= y1 && y <= y2;
        }
    }

    /* Exact selected 380_db.getback map4 rows in retained area_x1 DESC order. */
    private static final List<Area> MAP4 = List.of(
            new Area(33920,32128,34303,32738,34053,32284,4,10,0,0),
            new Area(33920,32739,34303,33532,33962,33254,4,12,0,0),
            new Area(33856,32192,33920,32738,34053,32284,4,10,0,0),
            new Area(33792,33023,34303,33532,33962,33254,4,12,0,0),
            new Area(33536,32574,33919,33022,33429,32814,4,7,0,0),
            new Area(33472,32192,33855,32574,33722,32496,4,9,0,0),
            new Area(33280,32511,33535,33022,33429,32814,4,7,0,0),
            new Area(33280,33023,33791,33532,33604,33237,4,8,0,0),
            new Area(33216,32192,33471,32510,33429,32814,4,7,11,0),
            new Area(32960,32192,33215,32510,33051,32343,4,4,11,0),
            new Area(32960,32511,33280,33022,33049,32780,4,6,0,0),
            new Area(32960,33023,33279,33532,33082,33389,4,2,0,0),
            new Area(32515,32192,32959,32536,32745,32445,4,4,0,0),
            new Area(32515,32537,32959,33022,32628,32807,4,3,0,0),
            new Area(32515,33023,32959,33532,32612,33188,4,5,0,0));

    /* Exact selected getback map350 row: a map-wide rule returning to Giran town 7. */
    private static final Area MAP350 =
            new Area(0,0,0,0,0,0,0,7,0,0);
    /* Exact selected getback map21 row: class-specific Orc/Elf return towns. */
    private static final Area MAP21 =
            new Area(0,0,0,0,32745,32445,4,4,11,0);
    /* Exact selected getback map17 row: class-specific Orc/Elf return towns. */
    private static final Area MAP17 =
            new Area(0,0,0,0,33051,32343,4,4,11,0);
    /* Exact selected getback map22 row: direct Gerard trial return, no town roll. */
    private static final Area MAP22 =
            new Area(0,0,0,0,33089,33397,4,0,0,0);
    /* Exact selected map25-28 rows return to Silver Knight town 2. */
    private static final Area KNIGHT_CAVE =
            new Area(0,0,0,0,33089,33397,4,2,0,0);
    /* Exact selected map43-51 rows share the Talking Island town2 return. */
    private static final Area ANT_CAVE =
            new Area(0,0,0,0,32612,33188,4,2,0,0);
    /* Exact selected getback map16 row: direct Hartin return, no town randomization. */
    private static final Area MAP16 =
            new Area(0,0,0,0,33051,32343,4,0,0,0);
    /* Exact selected getback map67 row: map-wide Valakas habitat return to Aden town 9. */
    private static final Area MAP67 =
            new Area(0,0,0,0,33719,32491,4,9,0,0);
    /* Exact selected map19/map20 rows share the same Orc/Elf return towns. */
    private static final Area SLEEPING_DRAGON =
            new Area(0,0,0,0,32745,32445,4,4,11,0);
    /* Exact selected map30-37 Dragon Valley rows return to Giran town 7. */
    private static final Area DRAGON_VALLEY_DUNGEON =
            new Area(0,0,0,0,33429,32814,4,7,0,0);
    /* Exact selected map23/map24/map29 rows return to Windawood town 5. */
    private static final Area WINDAWOOD_CASTLE =
            new Area(0,0,0,0,32612,33188,4,5,0,0);
    /* Exact selected map15/map240-243 rows return to Kent town 6. */
    private static final Area KENT_COMPONENT =
            new Area(0,0,0,0,33049,32780,4,6,0,0);
    /* Exact selected maps600/601/778/779 rows return to Windawood town 5. */
    private static final Area DESIRE_CAVE =
            new Area(0,0,0,0,32608,33178,4,5,0,0);
    /* Exact selected maps75-82 rows return directly to Oren/town 10. */
    private static final Area IVORY_TOWER =
            new Area(0,0,0,0,34053,32284,4,10,0,0);
    /* Exact selected map66 row returns to Aden/town 9. */
    private static final Area DWARF_CASTLE_CAVE =
            new Area(0,0,0,0,33722,32496,4,9,0,0);
    /* Exact selected maps101-110 rows return to Aden/town 12. */
    private static final Area TOWER_OF_INSOLENCE =
            new Area(0,0,0,0,33966,33253,4,12,0,0);
    private static final Area HEINE_DUNGEON =
            new Area(0,0,0,0,33614,33243,4,8,0,0);
    private static final Area HEINE_EVA_DEEP =
            new Area(0,0,0,0,33604,33237,4,8,0,0);
    /* Exact selected map310 row delegates to native Oum Dungeon town id14. */
    private static final Area OUM_DUNGEON =
            new Area(0,0,0,0,0,0,0,14,0,0);
    private static final int[][] OUM_DUNGEON_RETURNS = points(
            32818,32805,32800,32798,32815,32819,32823,32811,32817,32828);
    /* Exact selected 380_db.getback map400 rows in retained area_x1 ASC order. */
    private static final List<Area> DIADRAD_MAP400 = List.of(
            new Area(32512,32810,32581,32865,32563,32680,400,0,0,0),
            new Area(32512,32865,32553,32880,32563,32680,400,0,0,0),
            new Area(32512,32880,32541,32995,32563,32680,400,0,0,0),
            new Area(32530,32958,32730,33003,32600,32922,400,0,0,0),
            new Area(32538,32595,32695,32810,32563,32680,400,0,0,0),
            new Area(32555,32868,32659,32958,32600,32922,400,0,0,0),
            new Area(32568,32810,32575,32815,32563,32680,400,0,0,0),
            new Area(32586,32813,32659,32868,32600,32922,400,0,0,0),
            new Area(32658,32810,32665,32816,32563,32680,400,0,0,0),
            new Area(32659,32817,32665,32958,32600,32922,400,0,0,0),
            new Area(32665,32808,32730,32958,32600,32922,400,0,0,0),
            new Area(32695,32629,32710,32647,32563,32680,400,0,0,0),
            new Area(32695,32720,32737,32790,32563,32680,400,0,0,0),
            new Area(32695,32790,32710,32810,32563,32680,400,0,0,0),
            new Area(32699,32647,32740,32720,32600,32922,400,0,0,0),
            new Area(32730,32796,32746,32829,32600,32922,400,0,0,0),
            new Area(32730,32833,32860,32980,32563,32680,400,0,0,0),
            new Area(32736,32595,32890,32647,32600,32922,400,0,0,0),
            new Area(32737,32730,32890,32765,32563,32680,400,0,0,0),
            new Area(32737,32765,32755,32790,32563,32680,400,0,0,0),
            new Area(32740,32647,32890,32720,32600,32922,400,0,0,0),
            new Area(32740,32720,32885,32730,32600,32922,400,0,0,0),
            new Area(32752,32730,32865,32852,32563,32680,400,0,0,0),
            new Area(32788,32828,32800,32833,32563,32680,400,0,0,0),
            new Area(32810,32850,32915,32870,32600,32922,400,0,0,0),
            new Area(32863,32830,32915,32940,32563,32680,400,0,0,0),
            new Area(32890,32690,32911,32830,32563,32680,400,0,0,0),
            new Area(32900,32650,32911,32690,32563,32680,400,0,0,0),
            new Area(32911,32618,33004,32860,32563,32680,400,0,0,0),
            new Area(32915,32920,33000,33000,32600,32922,400,0,0,0));
    private static final Area DIADRAD_MAP401 =
            new Area(0,0,0,0,32563,32680,400,0,0,0);
    /* Exact selected map420/map430 rows land on Pirate Island. */
    private static final Area ABYSS_LAKE =
            new Area(0,0,0,0,32422,33074,440,17,0,0);
    private static final Area ELEMENTAL_GRAVE =
            new Area(0,0,0,0,32422,33074,440,0,0,0);
    /* Exact thirteen selected hotel getback rows. The five absent hotel-map
       rows deliberately retain GetbackTable's Silver Knight fallback. */
    private static final Map<Integer, Area> HOTELS = Map.ofEntries(
            Map.entry(16384, new Area(0,0,0,0,32574,32941,0,1,0,0)),
            Map.entry(16896, new Area(0,0,0,0,32574,32941,0,1,0,0)),
            Map.entry(17408, new Area(0,0,0,0,32628,32807,4,3,0,0)),
            Map.entry(17920, new Area(0,0,0,0,32628,32807,4,3,0,0)),
            Map.entry(18432, new Area(0,0,0,0,33429,32814,4,7,0,0)),
            Map.entry(18944, new Area(0,0,0,0,33429,32814,4,7,0,0)),
            Map.entry(19456, new Area(0,0,0,0,34053,32284,4,10,0,0)),
            Map.entry(19968, new Area(0,0,0,0,34053,32284,4,10,0,0)),
            Map.entry(20480, new Area(0,0,0,0,32612,33188,4,5,0,0)),
            Map.entry(20992, new Area(0,0,0,0,32612,33188,4,5,0,0)),
            Map.entry(22016, new Area(0,0,0,0,33082,33389,4,2,0,0)),
            Map.entry(22528, new Area(0,0,0,0,33604,33237,4,8,0,0)),
            Map.entry(23040, new Area(0,0,0,0,33604,33237,4,8,0,0)));
    /* Exact selected Pirate Island row is an in-map getback. */
    private static final Area PIRATE_ISLAND =
            new Area(0,0,0,0,32422,33074,440,0,0,0);
    /* Exact selected Pirate Island dungeon 1F-4F rows share this landing. */
    private static final Area PIRATE_DUNGEON =
            new Area(0,0,0,0,32422,33074,440,0,0,0);
    /* Maps480-481 delegate to retained Pirate-Island town id16. */
    private static final Area PIRATE_BACK_HALF =
            new Area(0,0,0,0,0,0,0,16,0,0);
    /* Maps482-484 are direct Kamyla-trial exits. */
    private static final Area KAMYLA_TRIAL =
            new Area(0,0,0,0,32451,33058,440,0,0,0);
    /* Maps550-558 share the Ship Grave surface return circle. */
    private static final Area SHIP_GRAVE =
            new Area(0,0,0,0,32844,32694,550,0,0,0);
    private static final int[][] PIRATE_ISLAND_RETURNS = points(
            32431,33058,32407,33054);
    /* Exact selected map70 row returns directly inside Forgotten Island. */
    private static final Area FORGOTTEN_ISLAND =
            new Area(0,0,0,0,32828,32848,70,0,0,0);
    /* Exact selected map83 row delegates to Aden-town id 8 randomization. */
    private static final Area MAINLAND_TO_FORGOTTEN_ISLAND_SHIP =
            new Area(0,0,0,0,33604,33237,4,8,0,0);
    /* Exact selected map84 row returns directly to the Aden dock. */
    private static final Area FORGOTTEN_ISLAND_TO_MAINLAND_SHIP =
            new Area(0,0,0,0,33604,33237,4,0,0,0);
    /* Exact selected map244 row has three direct Orc-town return points. */
    private static final int[][] TAINTED_LAND_RETURNS = points(
            32750,32435,32745,32447,32738,32452);
    /* Exact selected map450 row contains three in-map gate return points. */
    private static final int[][] LASTABAD_GATE_RETURNS = points(
            32656,32853,32656,32852,32656,32854);

    private static final Map<Integer, int[][]> TOWNS = Map.ofEntries(
            Map.entry(2, points(33071,33402,33091,33396,33085,33402,
                    33097,33366,33110,33365,33072,33392)),
            Map.entry(3, points(32601,32757,32625,32809,32611,32726,
                    32612,32781,32605,32761,32614,32739,32612,32775)),
            Map.entry(4, points(32750,32435,32745,32447,32738,32452,
                    32741,32436,32749,32446)),
            Map.entry(5, points(32608,33178,32626,33185,32630,33179,
                    32625,33207,32638,33203,32621,33179)),
            Map.entry(6, points(33048,32750,33059,32768,33047,32761,
                    33059,32759,33051,32775,33048,32778,33064,32773,33057,32748)),
            Map.entry(7, points(33435,32803,33439,32817,33440,32809,
                    33419,32810,33426,32823,33418,32818,33432,32824)),
            Map.entry(8, points(33593,33242,33593,33248,33604,33236,
                    33599,33236,33610,33247,33610,33241,33599,33252,33605,33252)),
            Map.entry(9, points(33702,32492,33747,32508,33696,32498,
                    33723,32512,33710,32521,33724,32488,33693,32513)),
            Map.entry(10, points(34086,32280,34037,32230,34022,32254,
                    34021,32269,34044,32290,34049,32316,34081,32249,
                    34074,32313,34064,32230)),
            Map.entry(11, points(33065,32358,33052,32313,33030,32342,
                    33068,32320,33071,32314,33030,32370,33076,32324,33068,32336)),
            Map.entry(12, points(33915,33114,34061,33115,34090,33168,
                    34011,33136,34093,33117,33959,33156,33992,33120,34047,33156)));

    private ClassicWorldGetbackCatalog() {}

    static Destination resolve(int mapId, int x, int y, int characterType) {
        return resolve(mapId, x, y, characterType,
                ThreadLocalRandom.current().nextInt());
    }

    static Destination resolve(
            int mapId, int x, int y, int characterType, int selector) {
        if (mapId == ClassicWorldMapCatalog.TALKING_ISLAND_MAP_ID) {
            return new Destination(0, 32574, 32941, 1);
        }
        if (mapId == ClassicWorldMapCatalog.ROYAL_TRIAL_CAVE_FIRST_MAP_ID
                || mapId == ClassicWorldMapCatalog.ROYAL_TRIAL_CAVE_SECOND_MAP_ID) {
            return new Destination(0, 32574, 32941, 1);
        }
        if (mapId == ClassicWorldMapCatalog.UNDERSEA_TUNNEL_MAP_ID) {
            return new Destination(0, 32574, 32941, 0);
        }
        if (mapId == ClassicWorldMapCatalog.UNDERSEA_TUNNEL_SEVENTH_FLOOR_MAP_ID) {
            return new Destination(4, 32624, 32813, 3);
        }
        if (mapId == ClassicWorldMapCatalog.JIM_SECRET_ROOM_MAP_ID) {
            return new Destination(4, 32628, 32807, 3);
        }
        if (ClassicWorldMapCatalog.GLUDIO_DUNGEON_MAP_IDS.contains(mapId)) {
            return new Destination(4, 32624, 32813, 3);
        }
        if (mapId == ClassicWorldMapCatalog.GUNTER_HOUSE_MAP_ID) {
            return new Destination(0, 32574, 32941, 1);
        }
        if (mapId == ClassicWorldMapCatalog.TALKING_ISLAND_TO_GLUDIO_SHIP_MAP_ID) {
            return new Destination(4, 32624, 32813, 1);
        }
        if (mapId == ClassicWorldMapCatalog.GLUDIO_TO_TALKING_ISLAND_SHIP_MAP_ID) {
            return new Destination(0, 32574, 32941, 3);
        }
        if (mapId == ClassicWorldMapCatalog.TAINTED_LAND_MAP_ID) {
            int[] point = TAINTED_LAND_RETURNS[
                    Math.floorMod(selector, TAINTED_LAND_RETURNS.length)];
            return new Destination(4, point[0], point[1], 0);
        }
        if (mapId == ClassicWorldMapCatalog.LASTABAD_GATE_MAP_ID) {
            int[] point = LASTABAD_GATE_RETURNS[
                    Math.floorMod(selector, LASTABAD_GATE_RETURNS.length)];
            return new Destination(mapId, point[0], point[1], 0);
        }
        if (mapId == ClassicWorldMapCatalog.WATER_DRAGON_LAIR_MAP_ID) {
            return new Destination(4, 33429, 32800, 0);
        }
        Area area = mapId == ClassicWorldMapCatalog.WORLD_TREE_MAP_ID
                ? MAP4.stream().filter(row -> row.contains(x, y))
                    .findFirst().orElse(null)
                : HOTELS.containsKey(mapId)
                    ? HOTELS.get(mapId)
                : mapId == ClassicWorldMapCatalog.NERUPA_CAVE_MAP_ID
                    ? MAP17
                : mapId == ClassicWorldMapCatalog.GERARD_TRIAL_DUNGEON_MAP_ID
                    ? MAP22
                : ClassicWorldMapCatalog.KNIGHT_CAVE_MAP_IDS.contains(mapId)
                    ? KNIGHT_CAVE
                : mapId >= 43 && mapId <= 51
                    ? ANT_CAVE
                : mapId == ClassicWorldMapCatalog.HARTIN_LABORATORY_MAP_ID
                    ? MAP16
                : mapId == ClassicWorldMapCatalog.VALAKAS_HABITAT_MAP_ID
                    ? MAP67
                : mapId == ClassicWorldMapCatalog.SLEEPING_DRAGON_FIRST_FLOOR_MAP_ID
                        || mapId == ClassicWorldMapCatalog.SLEEPING_DRAGON_SECOND_FLOOR_MAP_ID
                    ? SLEEPING_DRAGON
                : mapId == ClassicWorldMapCatalog.ORC_ELVEN_DUNGEON_MAP_ID
                    ? MAP21
                : mapId == ClassicWorldMapCatalog.FORGOTTEN_ISLAND_MAP_ID
                    ? FORGOTTEN_ISLAND
                : mapId == ClassicWorldMapCatalog.MAINLAND_TO_FORGOTTEN_ISLAND_SHIP_MAP_ID
                    ? MAINLAND_TO_FORGOTTEN_ISLAND_SHIP
                : mapId == ClassicWorldMapCatalog.FORGOTTEN_ISLAND_TO_MAINLAND_SHIP_MAP_ID
                    ? FORGOTTEN_ISLAND_TO_MAINLAND_SHIP
                : ClassicWorldMapCatalog.GIRAN_DUNGEON_MAP_IDS.contains(mapId)
                    ? DRAGON_VALLEY_DUNGEON
                : mapId == ClassicWorldMapCatalog.GIRAN_CASTLE_MAP_ID
                    ? DRAGON_VALLEY_DUNGEON
                : ClassicWorldMapCatalog.WINDAWOOD_CASTLE_MAP_IDS.contains(mapId)
                    ? WINDAWOOD_CASTLE
                : ClassicWorldMapCatalog.KENT_COMPONENT_MAP_IDS.contains(mapId)
                    ? KENT_COMPONENT
                : ClassicWorldDesireCaveCatalog.GETBACK_MAP_IDS.contains(mapId)
                    ? DESIRE_CAVE
                : ClassicWorldMapCatalog.IVORY_TOWER_MAP_IDS.contains(mapId)
                    ? IVORY_TOWER
                : mapId == ClassicWorldMapCatalog.DWARF_CASTLE_CAVE_MAP_ID
                    ? DWARF_CASTLE_CAVE
                : ClassicWorldAdenToiCatalog.TOI_MAP_IDS.contains(mapId)
                    ? TOWER_OF_INSOLENCE
                : mapId >= 59 && mapId <= 61
                    ? HEINE_DUNGEON
                : mapId >= 62 && mapId <= 65
                    ? HEINE_EVA_DEEP
                : mapId == ClassicWorldRastabadOumCatalog.OUM_DUNGEON_MAP_ID
                    ? OUM_DUNGEON
                : mapId == 320 || mapId == 330
                    ? OUM_DUNGEON
                : mapId == 400
                    ? DIADRAD_MAP400.stream().filter(row -> row.contains(x, y))
                        .findFirst().orElse(null)
                : mapId == 401
                    ? DIADRAD_MAP401
                : mapId == ClassicWorldElementalGraveCatalog.ABYSS_LAKE_MAP_ID
                    ? ABYSS_LAKE
                : mapId == ClassicWorldElementalGraveCatalog.ELEMENTAL_GRAVE_MAP_ID
                    ? ELEMENTAL_GRAVE
                : mapId == ClassicWorldElementalGraveCatalog.PIRATE_ISLAND_MAP_ID
                    ? PIRATE_ISLAND
                : ClassicWorldMapCatalog.PIRATE_DUNGEON_MAP_IDS.contains(mapId)
                    ? PIRATE_DUNGEON
                : mapId == 480 || mapId == 481
                    ? PIRATE_BACK_HALF
                : mapId >= 482 && mapId <= 484
                    ? KAMYLA_TRIAL
                : ClassicWorldPirateBackHalfCatalog.SHIP_GRAVE_MAP_IDS.contains(mapId)
                    ? SHIP_GRAVE
                : mapId == ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_FIRST_FLOOR_MAP_ID
                        || mapId == ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_SECOND_FLOOR_MAP_ID
                        || mapId == ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_THIRD_FLOOR_MAP_ID
                        || mapId == ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_FOURTH_FLOOR_MAP_ID
                        || mapId == ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_FIFTH_FLOOR_MAP_ID
                        || mapId == ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_SIXTH_FLOOR_MAP_ID
                        || mapId == ClassicWorldMapCatalog.ANTHARAS_LAIR_MAP_ID
                    ? DRAGON_VALLEY_DUNGEON
                    : mapId == ClassicWorldMapCatalog.GIRAN_TRADE_ZONE_MAP_ID
                        ? MAP350 : null;
        if (area == null) return new Destination(4, 33089, 33397, 2);
        int townId = characterType == 2 && area.elfTownId() > 0
                ? area.elfTownId()
                : characterType == 4 && area.darkElfTownId() > 0
                    ? area.darkElfTownId() : area.townId();
        if (townId == 14) {
            int[] point = OUM_DUNGEON_RETURNS[
                    Math.floorMod(selector, OUM_DUNGEON_RETURNS.length)];
            return new Destination(310, point[0], point[1], townId);
        }
        if (townId == 16) {
            int[] point = PIRATE_ISLAND_RETURNS[
                    Math.floorMod(selector, PIRATE_ISLAND_RETURNS.length)];
            return new Destination(440, point[0], point[1], townId);
        }
        int[][] town = TOWNS.get(townId);
        if (town == null) {
            return new Destination(area.getbackMap(), area.getbackX(),
                    area.getbackY(), 0);
        }
        int[] point = town[Math.floorMod(selector, town.length)];
        return new Destination(4, point[0], point[1], townId);
    }

    static int map4RowCount() {
        return MAP4.size();
    }

    private static int[][] points(int... coordinates) {
        if ((coordinates.length & 1) != 0) {
            throw new IllegalArgumentException("unpaired town coordinates");
        }
        int[][] result = new int[coordinates.length / 2][2];
        for (int index = 0; index < result.length; index++) {
            result[index][0] = coordinates[index * 2];
            result[index][1] = coordinates[index * 2 + 1];
        }
        return result;
    }
}
