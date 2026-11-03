package local.lito381.server;

import java.util.List;

/** Hash-locked selected-source native NPC teleport routes. */
final class ClassicWorldTeleportCatalog {
    static final int TELEPORT_EFFECT_GFX = 169;
    static final int PHANTASM_SOURCE_SPAWN_ID = 83;
    static final int PHANTASM_SOURCE_NPC_ID = 50045;
    static final long PHANTASM_SOURCE_OBJECT_ID =
            ClassicWorldNpcCatalog.objectId(PHANTASM_SOURCE_SPAWN_ID);
    static final int PHANTASM_ROUTE_ID = 429;
    static final String PHANTASM_INITIAL_HTML = "telephantasm";
    static final String PHANTASM_ACTION = "teleport phantasm-island";
    static final String PHANTASM_SOURCE_SHA256 =
            "7274C3BE3EF6F88820EE5DECB98D3078064678C2544FBECD2A9BB62C025893F7";
    static final int GIRAN_TRADE_SOURCE_NPC_ID = 50034;
    static final int GIRAN_TRADE_ROUTE_ID = 181;
    static final String GIRAN_TRADE_INITIAL_HTML = "grtztele";
    static final String GIRAN_TRADE_ACTION = "teleport giran-giran-trade-zone";
    static final String GIRAN_TRADE_SOURCE_SHA256 =
            "839A114E835C81906C644D0C208FDD0ABC7C2BD2F78FD8D79F179283AE539C76";
    static final int ORC_SOURCE_SPAWN_ID = 106;
    static final int ORC_SOURCE_NPC_ID = 50063;
    static final String ORC_INITIAL_HTML = "telecorc1";
    static final String ORC_ROUTES_SOURCE_SHA256 =
            "2A3080B77B1E425C3241E70C314173F9DA5202A2EF49EDF348974446E71C4240";
    static final int FAIRY_SOURCE_SPAWN_ID = 1154;
    static final int FAIRY_SOURCE_NPC_ID = 70853;
    static final String FAIRY_INITIAL_HTML = "fairyp3";
    static final String FAIRY_ROUTES_SOURCE_SHA256 =
            "8BD971D2C7485EB9355B943C5A273EAE4ED8B11CD9FFC17B2A8067DEB14903B5";
    static final int HIDDEN_DOCK_SOURCE_SPAWN_ID = 107;
    static final int HIDDEN_DOCK_SOURCE_NPC_ID = 50064;
    static final int HIDDEN_DOCK_ROUTE_ID = 772;
    static final String HIDDEN_DOCK_INITIAL_HTML = "duvall1";
    static final String HIDDEN_DOCK_ACTION = "teleport orctown-hiddendock";
    static final String HIDDEN_DOCK_SOURCE_SHA256 =
            "FE093FFDD2E45FCDBD6026E850C10F300C27EF5388DD694D9379055B3E77A55F";
    static final int SEPIA_SOURCE_NPC_ID = 50031;
    static final int SEPIA_ROUTE_ID = 176;
    static final String SEPIA_INITIAL_HTML = "sepia2";
    static final String SEPIA_ACTION = "teleport sepia-dungen";
    static final String SEPIA_SOURCE_SHA256 =
            "494184DA11B99B0E0D64912D515C37C0C2BEA7AF9043829A1D76D2AC79502E58";
    static final int TAINTED_ENTRY_SPAWN_ID = 935;
    static final int TAINTED_ENTRY_NPC_ID = 71039;
    static final int TAINTED_EXIT_SPAWN_ID = 936;
    static final int TAINTED_EXIT_NPC_ID = 71045;
    static final String TAINTED_ENTRY_INITIAL_HTML = "orcfbuwoo1";
    static final String TAINTED_ENTRY_CONFIRM_HTML = "orcfbuwoo2";
    static final String TAINTED_ENTRY_CONFIRM_ACTION = "teleportURL";
    static final String TAINTED_EXIT_INITIAL_HTML = "orcfbawoo1";
    static final String TAINTED_LAND_ROUTES_SOURCE_SHA256 =
            "F51A82C26062D6842C18BAC1D424E9346258F12235CF1E3A4CC89D6CA512CCCC";
    static final int GIRAN_CASTLE_SOURCE_SPAWN_ID = 77;
    static final int GIRAN_CASTLE_SOURCE_NPC_ID = 50035;
    static final String GIRAN_CASTLE_INITIAL_HTML = "telecgiran1";
    static final String GIRAN_CASTLE_CONFIRM_HTML = "telecgiran2";
    static final String GIRAN_CASTLE_CONFIRM_ACTION = "teleportURL";
    static final String GIRAN_CASTLE_ROUTES_SOURCE_SHA256 =
            "4AEBCAA0133852D02E1901C54FC9F0DC52EBC42191A0F9B5A7B6CCEA3C20344B";
    static final int WINDAWOOD_CASTLE_SOURCE_SPAWN_ID = 93;
    static final int WINDAWOOD_CASTLE_SOURCE_NPC_ID = 50052;
    static final String WINDAWOOD_CASTLE_INITIAL_HTML = "telecwoods1";
    static final String WINDAWOOD_CASTLE_CONFIRM_HTML = "telecwoods2";
    static final String WINDAWOOD_CASTLE_CONFIRM_ACTION = "teleportURL";
    static final String WINDAWOOD_CASTLE_ROUTES_SOURCE_SHA256 =
            "45F0F234AF7F11F400ED4F42B089C99170198D98E38DF26A954D9AB401C5841F";
    static final int KENT_CASTLE_SOURCE_SPAWN_ID = 46;
    static final int KENT_CASTLE_SOURCE_NPC_ID = 50021;
    static final String KENT_CASTLE_INITIAL_HTML = "teleckent1";
    static final String KENT_CASTLE_CONFIRM_HTML = "teleckent2";
    static final String KENT_CASTLE_CONFIRM_ACTION = "teleportURL";
    static final int KENT_OBER_SOURCE_SPAWN_ID = 48;
    static final int KENT_OBER_SOURCE_NPC_ID = 50023;
    static final String KENT_OBER_INITIAL_HTML = "kentober1";
    static final String KENT_OBER_CONFIRM_HTML = "kentober2";
    static final String KENT_OBER_CONFIRM_ACTION = "teleportURL";
    static final String KENT_COMPONENT_ROUTES_SOURCE_SHA256 =
            "1997A7DB20C9ADAC78BEB42493205964D106F7ECF25787895E5F985A856E1121";
    static final int DWARF_CASTLE_SOURCE_SPAWN_ID =
            ClassicWorldDwarfCastleCatalog.TELEPORTER_SPAWN_ID;
    static final int DWARF_CASTLE_SOURCE_NPC_ID =
            ClassicWorldDwarfCastleCatalog.TELEPORTER_NPC_ID;
    static final String DWARF_CASTLE_INITIAL_HTML = "telecdwarf1";
    static final String DWARF_CASTLE_CONFIRM_HTML = "telecdwarf2";
    static final String DWARF_CASTLE_CONFIRM_ACTION = "teleportURL";
    static final String DWARF_CASTLE_ROUTES_SOURCE_SHA256 =
            ClassicWorldDwarfCastleCatalog.NPC_TELEPORT_SOURCE_SHA256;
    static final int ADEN_CASTLE_SOURCE_SPAWN_ID =
            ClassicWorldAdenToiCatalog.TELEPORTER_SPAWN_ID;
    static final int ADEN_CASTLE_SOURCE_NPC_ID =
            ClassicWorldAdenToiCatalog.TELEPORTER_NPC_ID;
    static final String ADEN_CASTLE_INITIAL_HTML = "telecaden1";
    static final String ADEN_CASTLE_CONFIRM_HTML = "telecaden2";
    static final String ADEN_CASTLE_CONFIRM_ACTION = "teleportURL";
    static final String ADEN_CASTLE_ROUTES_SOURCE_SHA256 =
            ClassicWorldAdenToiCatalog.NPC_TELEPORT_SOURCE_SHA256;
    static final int HEINE_CASTLE_SOURCE_SPAWN_ID =
            ClassicWorldHeineCatalog.TELEPORTER_SPAWN_ID;
    static final int HEINE_CASTLE_SOURCE_NPC_ID =
            ClassicWorldHeineCatalog.TELEPORTER_NPC_ID;
    static final String HEINE_CASTLE_INITIAL_HTML =
            ClassicWorldHeineCatalog.TELEPORT_INITIAL_HTML;
    static final String HEINE_CASTLE_CONFIRM_HTML =
            ClassicWorldHeineCatalog.TELEPORT_CONFIRM_HTML;
    static final String HEINE_CASTLE_CONFIRM_ACTION =
            ClassicWorldHeineCatalog.TELEPORT_CONFIRM_ACTION;
    static final String HEINE_CASTLE_ROUTES_SOURCE_SHA256 =
            ClassicWorldHeineCatalog.NPC_TELEPORT_SOURCE_SHA256;
    static final String SILENT_CAVERN_ROUTES_SOURCE_SHA256 =
            ClassicWorldSilentCavernCatalog.ROUTES_SOURCE_SHA256;
    static final String RASTABAD_OUM_ROUTES_SOURCE_SHA256 =
            ClassicWorldRastabadOumCatalog.NAMED_TELEPORT_SOURCE_SHA256;
    static final String DIADRAD_ROUTE_SOURCE_SHA256 =
            "B314D8D0D3D14B35C079F5C919AB48418D36AE7EE9425CFB9E9A8B2D937AB283";
    static final String ELEMENTAL_GRAVE_ROUTE_SOURCE_SHA256 =
            ClassicWorldElementalGraveCatalog.ENABLED_NPC_TELEPORT_SOURCE_SHA256;
    static final String DESIRE_CAVE_ROUTE_SOURCE_SHA256 =
            ClassicWorldDesireCaveCatalog.NPC_TELEPORT_SOURCE_SHA256;

    record Route(
            int sourceMapId, int sourceSpawnId, int sourceNpcId,
            int sourceX, int sourceY, String initialHtml,
            int routeId, String action,
            int destinationX, int destinationY, int destinationMapId,
            int heading, int priceAmount, long priceItemId,
            int minimumLevel, int maximumLevel) {
        Route {
            if (sourceMapId < 0 || sourceSpawnId <= 0 || sourceNpcId <= 0
                    || sourceX <= 0 || sourceY <= 0
                    || initialHtml == null || initialHtml.isEmpty()
                    || routeId <= 0 || action == null || action.isEmpty()
                    || destinationX <= 0 || destinationY <= 0
                    || destinationMapId < 0 || heading < 0 || heading > 7
                    || priceAmount < 0 || priceItemId <= 0
                    || minimumLevel < 0 || maximumLevel < minimumLevel) {
                throw new IllegalArgumentException("invalid classic WORLD teleport route");
            }
        }

        long sourceObjectId() {
            return ClassicWorldNpcCatalog.objectId(sourceSpawnId);
        }
    }

    private static final Route PHANTASM_ISLAND = new Route(
            ClassicWorldMapCatalog.WORLD_TREE_MAP_ID,
            PHANTASM_SOURCE_SPAWN_ID, PHANTASM_SOURCE_NPC_ID,
            33978, 32923, PHANTASM_INITIAL_HTML,
            PHANTASM_ROUTE_ID, PHANTASM_ACTION,
            ClassicWorldMapCatalog.PHANTASM_ISLAND_X,
            ClassicWorldMapCatalog.PHANTASM_ISLAND_Y,
            ClassicWorldMapCatalog.PHANTASM_ISLAND_MAP_ID,
            ClassicWorldMapCatalog.PHANTASM_ISLAND_HEADING,
            1_000, InventoryCatalog.ADENA, 0, 200);
    private static final List<Route> GIRAN_TRADE_ZONE = List.of(
            new Route(
                    ClassicWorldMapCatalog.WORLD_TREE_MAP_ID,
                    75, GIRAN_TRADE_SOURCE_NPC_ID,
                    33438, 32809, GIRAN_TRADE_INITIAL_HTML,
                    GIRAN_TRADE_ROUTE_ID, GIRAN_TRADE_ACTION,
                    ClassicWorldMapCatalog.GIRAN_TRADE_ZONE_X,
                    ClassicWorldMapCatalog.GIRAN_TRADE_ZONE_Y,
                    ClassicWorldMapCatalog.GIRAN_TRADE_ZONE_MAP_ID,
                    ClassicWorldMapCatalog.GIRAN_TRADE_ZONE_HEADING,
                    0, InventoryCatalog.ADENA, 0, 200),
            new Route(
                    ClassicWorldMapCatalog.WORLD_TREE_MAP_ID,
                    76, GIRAN_TRADE_SOURCE_NPC_ID,
                    33510, 32766, GIRAN_TRADE_INITIAL_HTML,
                    GIRAN_TRADE_ROUTE_ID, GIRAN_TRADE_ACTION,
                    ClassicWorldMapCatalog.GIRAN_TRADE_ZONE_X,
                    ClassicWorldMapCatalog.GIRAN_TRADE_ZONE_Y,
                    ClassicWorldMapCatalog.GIRAN_TRADE_ZONE_MAP_ID,
                    ClassicWorldMapCatalog.GIRAN_TRADE_ZONE_HEADING,
                    0, InventoryCatalog.ADENA, 0, 200));
    private static final List<Route> ORC_ROUTES = List.of(
            new Route(
                    ClassicWorldMapCatalog.WORLD_TREE_MAP_ID,
                    ORC_SOURCE_SPAWN_ID, ORC_SOURCE_NPC_ID,
                    32801, 32268, ORC_INITIAL_HTML,
                    769, "teleport orc-village",
                    32715, 32448, ClassicWorldMapCatalog.WORLD_TREE_MAP_ID,
                    5, 0, InventoryCatalog.ADENA, 0, 200),
            new Route(
                    ClassicWorldMapCatalog.WORLD_TREE_MAP_ID,
                    ORC_SOURCE_SPAWN_ID, ORC_SOURCE_NPC_ID,
                    32801, 32268, ORC_INITIAL_HTML,
                    770, "teleport orc-elvendun",
                    ClassicWorldMapCatalog.ORC_ELVEN_DUNGEON_X,
                    ClassicWorldMapCatalog.ORC_ELVEN_DUNGEON_Y,
                    ClassicWorldMapCatalog.ORC_ELVEN_DUNGEON_MAP_ID,
                    ClassicWorldMapCatalog.ORC_ELVEN_DUNGEON_HEADING,
                    0, InventoryCatalog.ADENA, 0, 200),
            new Route(
                    ClassicWorldMapCatalog.WORLD_TREE_MAP_ID,
                    ORC_SOURCE_SPAWN_ID, ORC_SOURCE_NPC_ID,
                    32801, 32268, ORC_INITIAL_HTML,
                    771, "teleport orc-dragonvalley",
                    33432, 32546, ClassicWorldMapCatalog.WORLD_TREE_MAP_ID,
                    5, 0, InventoryCatalog.ADENA, 0, 200));
    private static final List<Route> FAIRY_DUNGEON_ROUTES = List.of(
            new Route(
                    ClassicWorldMapCatalog.WORLD_TREE_MAP_ID,
                    FAIRY_SOURCE_SPAWN_ID, FAIRY_SOURCE_NPC_ID,
                    32968, 32443, FAIRY_INITIAL_HTML,
                    1142, "teleport darkmar-dungen",
                    ClassicWorldMapCatalog.FAIRY_DUNGEON_X,
                    ClassicWorldMapCatalog.FAIRY_DUNGEON_Y,
                    ClassicWorldMapCatalog.DARKMAR_DUNGEON_MAP_ID,
                    ClassicWorldMapCatalog.FAIRY_DUNGEON_HEADING,
                    0, InventoryCatalog.ADENA, 0, 200),
            new Route(
                    ClassicWorldMapCatalog.WORLD_TREE_MAP_ID,
                    FAIRY_SOURCE_SPAWN_ID, FAIRY_SOURCE_NPC_ID,
                    32968, 32443, FAIRY_INITIAL_HTML,
                    1143, "teleport dark-elf-dungen",
                    ClassicWorldMapCatalog.FAIRY_DUNGEON_X,
                    ClassicWorldMapCatalog.FAIRY_DUNGEON_Y,
                    ClassicWorldMapCatalog.DARK_ELF_DUNGEON_MAP_ID,
                    ClassicWorldMapCatalog.FAIRY_DUNGEON_HEADING,
                    0, InventoryCatalog.ADENA, 0, 200));
    private static final Route HIDDEN_DOCK = new Route(
            ClassicWorldMapCatalog.WORLD_TREE_MAP_ID,
            HIDDEN_DOCK_SOURCE_SPAWN_ID, HIDDEN_DOCK_SOURCE_NPC_ID,
            32721, 32443, HIDDEN_DOCK_INITIAL_HTML,
            HIDDEN_DOCK_ROUTE_ID, HIDDEN_DOCK_ACTION,
            ClassicWorldMapCatalog.HIDDEN_DOCK_X,
            ClassicWorldMapCatalog.HIDDEN_DOCK_Y,
            ClassicWorldMapCatalog.HIDDEN_DOCK_MAP_ID,
            ClassicWorldMapCatalog.HIDDEN_DOCK_HEADING,
            100, InventoryCatalog.ADENA, 0, 200);
    private static final List<Route> SEPIA_DUNGEON = List.of(
            new Route(
                    ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_THIRD_FLOOR_MAP_ID,
                    65, SEPIA_SOURCE_NPC_ID,
                    32713, 32842, SEPIA_INITIAL_HTML,
                    SEPIA_ROUTE_ID, SEPIA_ACTION,
                    ClassicWorldMapCatalog.SEPIA_DUNGEON_X,
                    ClassicWorldMapCatalog.SEPIA_DUNGEON_Y,
                    ClassicWorldMapCatalog.SEPIA_DUNGEON_MAP_ID,
                    ClassicWorldMapCatalog.SEPIA_DUNGEON_HEADING,
                    0, InventoryCatalog.ADENA, 0, 200),
            new Route(
                    ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_THIRD_FLOOR_MAP_ID,
                    1156, SEPIA_SOURCE_NPC_ID,
                    32713, 32842, SEPIA_INITIAL_HTML,
                    SEPIA_ROUTE_ID, SEPIA_ACTION,
                    ClassicWorldMapCatalog.SEPIA_DUNGEON_X,
                    ClassicWorldMapCatalog.SEPIA_DUNGEON_Y,
                    ClassicWorldMapCatalog.SEPIA_DUNGEON_MAP_ID,
                    ClassicWorldMapCatalog.SEPIA_DUNGEON_HEADING,
                    0, InventoryCatalog.ADENA, 0, 200));
    private static final List<Route> TAINTED_LAND = List.of(
            new Route(
                    ClassicWorldMapCatalog.WORLD_TREE_MAP_ID,
                    TAINTED_ENTRY_SPAWN_ID, TAINTED_ENTRY_NPC_ID,
                    32777, 32298, TAINTED_ENTRY_INITIAL_HTML,
                    1156, "teleport orcfcd",
                    ClassicWorldMapCatalog.TAINTED_LAND_X,
                    ClassicWorldMapCatalog.TAINTED_LAND_Y,
                    ClassicWorldMapCatalog.TAINTED_LAND_MAP_ID,
                    ClassicWorldMapCatalog.TAINTED_LAND_HEADING,
                    10_000, InventoryCatalog.ADENA, 0, 200),
            new Route(
                    ClassicWorldMapCatalog.TAINTED_LAND_MAP_ID,
                    TAINTED_EXIT_SPAWN_ID, TAINTED_EXIT_NPC_ID,
                    32992, 32814, TAINTED_EXIT_INITIAL_HTML,
                    1157, "1",
                    32750, 32435, ClassicWorldMapCatalog.WORLD_TREE_MAP_ID,
                    5, 1, InventoryCatalog.ADENA, 0, 200));
    private static final List<Route> GIRAN_CASTLE = List.of(
            new Route(
                    ClassicWorldMapCatalog.GIRAN_CASTLE_MAP_ID,
                    GIRAN_CASTLE_SOURCE_SPAWN_ID, GIRAN_CASTLE_SOURCE_NPC_ID,
                    32733, 32789, GIRAN_CASTLE_INITIAL_HTML,
                    182, "teleport giranc-village",
                    33438, 32796, ClassicWorldMapCatalog.WORLD_TREE_MAP_ID,
                    5, 0, InventoryCatalog.ADENA, 0, 200),
            new Route(
                    ClassicWorldMapCatalog.GIRAN_CASTLE_MAP_ID,
                    GIRAN_CASTLE_SOURCE_SPAWN_ID, GIRAN_CASTLE_SOURCE_NPC_ID,
                    32733, 32789, GIRAN_CASTLE_INITIAL_HTML,
                    183, "teleport giranc-girandun",
                    32731, 32728, 55,
                    5, 0, InventoryCatalog.ADENA, 0, 200),
            new Route(
                    ClassicWorldMapCatalog.GIRAN_CASTLE_MAP_ID,
                    GIRAN_CASTLE_SOURCE_SPAWN_ID, GIRAN_CASTLE_SOURCE_NPC_ID,
                    32733, 32789, GIRAN_CASTLE_INITIAL_HTML,
                    184, "teleport giranc-dragonvalley",
                    33432, 32546, ClassicWorldMapCatalog.WORLD_TREE_MAP_ID,
                    5, 0, InventoryCatalog.ADENA, 0, 200),
            new Route(
                    ClassicWorldMapCatalog.GIRAN_CASTLE_MAP_ID,
                    GIRAN_CASTLE_SOURCE_SPAWN_ID, GIRAN_CASTLE_SOURCE_NPC_ID,
                    32733, 32789, GIRAN_CASTLE_INITIAL_HTML,
                    185, "teleport giranc-littlebb",
                    33517, 32851, ClassicWorldMapCatalog.WORLD_TREE_MAP_ID,
                    5, 0, InventoryCatalog.ADENA, 0, 200));
    private static final List<Route> WINDAWOOD_CASTLE = List.of(
            new Route(
                    ClassicWorldMapCatalog.WINDAWOOD_CASTLE_MAP_ID,
                    WINDAWOOD_CASTLE_SOURCE_SPAWN_ID,
                    WINDAWOOD_CASTLE_SOURCE_NPC_ID,
                    32733, 32784, WINDAWOOD_CASTLE_INITIAL_HTML,
                    598, "teleport woodsc-village",
                    32640, 33203, ClassicWorldMapCatalog.WORLD_TREE_MAP_ID,
                    5, 0, InventoryCatalog.ADENA, 0, 200),
            new Route(
                    ClassicWorldMapCatalog.WINDAWOOD_CASTLE_MAP_ID,
                    WINDAWOOD_CASTLE_SOURCE_SPAWN_ID,
                    WINDAWOOD_CASTLE_SOURCE_NPC_ID,
                    32733, 32784, WINDAWOOD_CASTLE_INITIAL_HTML,
                    599, "teleport woodsc-windun",
                    32805, 32811, 27,
                    5, 0, InventoryCatalog.ADENA, 0, 200),
            new Route(
                    ClassicWorldMapCatalog.WINDAWOOD_CASTLE_MAP_ID,
                    WINDAWOOD_CASTLE_SOURCE_SPAWN_ID,
                    WINDAWOOD_CASTLE_SOURCE_NPC_ID,
                    32733, 32784, WINDAWOOD_CASTLE_INITIAL_HTML,
                    600, "teleport woodsc-oasis",
                    32863, 33252, ClassicWorldMapCatalog.WORLD_TREE_MAP_ID,
                    5, 0, InventoryCatalog.ADENA, 0, 200),
            new Route(
                    ClassicWorldMapCatalog.WINDAWOOD_CASTLE_MAP_ID,
                    WINDAWOOD_CASTLE_SOURCE_SPAWN_ID,
                    WINDAWOOD_CASTLE_SOURCE_NPC_ID,
                    32733, 32784, WINDAWOOD_CASTLE_INITIAL_HTML,
                    601, "teleport woodsc-sknight",
                    33080, 33386, ClassicWorldMapCatalog.WORLD_TREE_MAP_ID,
                    5, 0, InventoryCatalog.ADENA, 0, 200));
    private static final List<Route> KENT_COMPONENT = List.of(
            new Route(
                    ClassicWorldMapCatalog.KENT_CASTLE_MAP_ID,
                    KENT_CASTLE_SOURCE_SPAWN_ID, KENT_CASTLE_SOURCE_NPC_ID,
                    32734, 32784, KENT_CASTLE_INITIAL_HTML,
                    85, "teleport kentc-village",
                    33050, 32782, ClassicWorldMapCatalog.WORLD_TREE_MAP_ID,
                    5, 0, InventoryCatalog.ADENA, 0, 200),
            new Route(
                    ClassicWorldMapCatalog.KENT_CASTLE_MAP_ID,
                    KENT_CASTLE_SOURCE_SPAWN_ID, KENT_CASTLE_SOURCE_NPC_ID,
                    32734, 32784, KENT_CASTLE_INITIAL_HTML,
                    86, "teleport kentc-gludun",
                    32764, 32774, 10,
                    5, 0, InventoryCatalog.ADENA, 0, 200),
            new Route(
                    ClassicWorldMapCatalog.KENT_CASTLE_MAP_ID,
                    KENT_CASTLE_SOURCE_SPAWN_ID, KENT_CASTLE_SOURCE_NPC_ID,
                    32734, 32784, KENT_CASTLE_INITIAL_HTML,
                    87, "teleport kentc-girdun",
                    32809, 32793, 54,
                    5, 0, InventoryCatalog.ADENA, 0, 200),
            new Route(
                    ClassicWorldMapCatalog.KENT_CASTLE_MAP_ID,
                    KENT_OBER_SOURCE_SPAWN_ID, KENT_OBER_SOURCE_NPC_ID,
                    32751, 32796, KENT_OBER_INITIAL_HTML,
                    88, "teleport kentcd",
                    32721, 33087, 240,
                    5, 10_000, InventoryCatalog.ADENA, 0, 200));
    private static final List<Route> DWARF_CASTLE = List.of(
            new Route(
                    ClassicWorldDwarfCastleCatalog.MAP_ID,
                    DWARF_CASTLE_SOURCE_SPAWN_ID, DWARF_CASTLE_SOURCE_NPC_ID,
                    32865, 32803, DWARF_CASTLE_INITIAL_HTML,
                    346, "teleport dwarfc-village",
                    33709, 32500, ClassicWorldMapCatalog.WORLD_TREE_MAP_ID,
                    5, 0, InventoryCatalog.ADENA, 0, 200),
            new Route(
                    ClassicWorldDwarfCastleCatalog.MAP_ID,
                    DWARF_CASTLE_SOURCE_SPAWN_ID, DWARF_CASTLE_SOURCE_NPC_ID,
                    32865, 32803, DWARF_CASTLE_INITIAL_HTML,
                    347, "teleport dwarfc-ivorytower",
                    32773, 32806, 79,
                    5, 0, InventoryCatalog.ADENA, 0, 200),
            new Route(
                    ClassicWorldDwarfCastleCatalog.MAP_ID,
                    DWARF_CASTLE_SOURCE_SPAWN_ID, DWARF_CASTLE_SOURCE_NPC_ID,
                    32865, 32803, DWARF_CASTLE_INITIAL_HTML,
                    348, ClassicWorldDwarfCastleCatalog.DATABASE_CRYSTAL_ACTION,
                    34174, 32184, ClassicWorldMapCatalog.WORLD_TREE_MAP_ID,
                    5, 0, InventoryCatalog.ADENA, 0, 200));
    private static final List<Route> ADEN_CASTLE = List.of(
            new Route(
                    ClassicWorldAdenToiCatalog.ADEN_INTERIOR_MAP_ID,
                    ADEN_CASTLE_SOURCE_SPAWN_ID, ADEN_CASTLE_SOURCE_NPC_ID,
                    32891, 32524, ADEN_CASTLE_INITIAL_HTML,
                    510, "teleport adenc-village",
                    33965, 33253, ClassicWorldMapCatalog.WORLD_TREE_MAP_ID,
                    5, 0, InventoryCatalog.ADENA, 0, 200),
            new Route(
                    ClassicWorldAdenToiCatalog.ADEN_INTERIOR_MAP_ID,
                    ADEN_CASTLE_SOURCE_SPAWN_ID, ADEN_CASTLE_SOURCE_NPC_ID,
                    32891, 32524, ADEN_CASTLE_INITIAL_HTML,
                    511, "teleport adenc-omantower",
                    32795, 32798, 101,
                    5, 0, InventoryCatalog.ADENA, 0, 200),
            new Route(
                    ClassicWorldAdenToiCatalog.ADEN_INTERIOR_MAP_ID,
                    ADEN_CASTLE_SOURCE_SPAWN_ID, ADEN_CASTLE_SOURCE_NPC_ID,
                    32891, 32524, ADEN_CASTLE_INITIAL_HTML,
                    512, "teleport adenc-gypsy",
                    33979, 32926, ClassicWorldMapCatalog.WORLD_TREE_MAP_ID,
                    5, 0, InventoryCatalog.ADENA, 0, 200));
    private static final List<Route> HEINE_CASTLE = List.of(
            new Route(64, HEINE_CASTLE_SOURCE_SPAWN_ID,
                    HEINE_CASTLE_SOURCE_NPC_ID, 32579, 32811,
                    HEINE_CASTLE_INITIAL_HTML, 853,
                    "teleport heinec-village", 33612, 33257, 4,
                    5, 0, InventoryCatalog.ADENA, 0, 200),
            new Route(64, HEINE_CASTLE_SOURCE_SPAWN_ID,
                    HEINE_CASTLE_SOURCE_NPC_ID, 32579, 32811,
                    HEINE_CASTLE_INITIAL_HTML, 854,
                    "teleport heinec-evadun", 32694, 32828, 61,
                    5, 0, InventoryCatalog.ADENA, 0, 200),
            new Route(64, HEINE_CASTLE_SOURCE_SPAWN_ID,
                    HEINE_CASTLE_SOURCE_NPC_ID, 32579, 32811,
                    HEINE_CASTLE_INITIAL_HTML, 855,
                    "teleport heinec-forgotten", 33427, 33481, 4,
                    5, 0, InventoryCatalog.ADENA, 0, 200));
    private static final List<Route> SILENT_CAVERN = List.of(
            new Route(304, ClassicWorldSilentCavernCatalog.TELEPORTER_SPAWN_ID,
                    ClassicWorldSilentCavernCatalog.TELEPORTER_NPC_ID,
                    32894, 32886, ClassicWorldSilentCavernCatalog.TELEPORT_INITIAL_HTML,
                    875, "teleport scave-gludin", 32608, 32734, 4,
                    5, 1500, InventoryCatalog.ADENA, 0, 200),
            new Route(304, ClassicWorldSilentCavernCatalog.TELEPORTER_SPAWN_ID,
                    ClassicWorldSilentCavernCatalog.TELEPORTER_NPC_ID,
                    32894, 32886, ClassicWorldSilentCavernCatalog.TELEPORT_INITIAL_HTML,
                    876, "teleport scave-kent", 33050, 32782, 4,
                    5, 800, InventoryCatalog.ADENA, 0, 200),
            new Route(304, ClassicWorldSilentCavernCatalog.TELEPORTER_SPAWN_ID,
                    ClassicWorldSilentCavernCatalog.TELEPORTER_NPC_ID,
                    32894, 32886, ClassicWorldSilentCavernCatalog.TELEPORT_INITIAL_HTML,
                    877, "teleport scave-giran", 33438, 32796, 4,
                    5, 600, InventoryCatalog.ADENA, 0, 200),
            new Route(304, ClassicWorldSilentCavernCatalog.TELEPORTER_SPAWN_ID,
                    ClassicWorldSilentCavernCatalog.TELEPORTER_NPC_ID,
                    32894, 32886, ClassicWorldSilentCavernCatalog.TELEPORT_INITIAL_HTML,
                    878, "teleport scave-aden", 33965, 33253, 4,
                    5, 1800, InventoryCatalog.ADENA, 0, 200),
            new Route(304, ClassicWorldSilentCavernCatalog.TELEPORTER_SPAWN_ID,
                    ClassicWorldSilentCavernCatalog.TELEPORTER_NPC_ID,
                    32894, 32886, ClassicWorldSilentCavernCatalog.TELEPORT_INITIAL_HTML,
                    880, "teleport scave-heine", 33612, 33257, 4,
                    5, 1800, InventoryCatalog.ADENA, 0, 200),
            new Route(304, ClassicWorldSilentCavernCatalog.TELEPORTER_SPAWN_ID,
                    ClassicWorldSilentCavernCatalog.TELEPORTER_NPC_ID,
                    32894, 32886, ClassicWorldSilentCavernCatalog.TELEPORT_INITIAL_HTML,
                    881, "teleport scave-oren", 34062, 32278, 4,
                    5, 1000, InventoryCatalog.ADENA, 0, 200));
    private static final List<Route> RASTABAD_OUM = List.of(
            new Route(310, ClassicWorldRastabadOumCatalog.TELEPORTER_SPAWN_ID,
                    ClassicWorldRastabadOumCatalog.TELEPORTER_NPC_ID,
                    32805, 32794, ClassicWorldRastabadOumCatalog.TELEPORT_INITIAL_HTML,
                    940, "teleport oum-gludin", 32608, 32734, 4,
                    5, 2200, InventoryCatalog.ADENA, 0, 200),
            new Route(310, ClassicWorldRastabadOumCatalog.TELEPORTER_SPAWN_ID,
                    ClassicWorldRastabadOumCatalog.TELEPORTER_NPC_ID,
                    32805, 32794, ClassicWorldRastabadOumCatalog.TELEPORT_INITIAL_HTML,
                    941, "teleport oum-kent", 33050, 32782, 4,
                    5, 1800, InventoryCatalog.ADENA, 0, 200),
            new Route(310, ClassicWorldRastabadOumCatalog.TELEPORTER_SPAWN_ID,
                    ClassicWorldRastabadOumCatalog.TELEPORTER_NPC_ID,
                    32805, 32794, ClassicWorldRastabadOumCatalog.TELEPORT_INITIAL_HTML,
                    942, "teleport oum-giran", 33438, 32796, 4,
                    5, 1000, InventoryCatalog.ADENA, 0, 200),
            new Route(310, ClassicWorldRastabadOumCatalog.TELEPORTER_SPAWN_ID,
                    ClassicWorldRastabadOumCatalog.TELEPORTER_NPC_ID,
                    32805, 32794, ClassicWorldRastabadOumCatalog.TELEPORT_INITIAL_HTML,
                    943, "teleport oum-aden", 33965, 33253, 4,
                    5, 1600, InventoryCatalog.ADENA, 0, 200),
            new Route(310, ClassicWorldRastabadOumCatalog.TELEPORTER_SPAWN_ID,
                    ClassicWorldRastabadOumCatalog.TELEPORTER_NPC_ID,
                    32805, 32794, ClassicWorldRastabadOumCatalog.TELEPORT_INITIAL_HTML,
                    944, "teleport oum-heine", 33612, 33257, 4,
                    5, 2200, InventoryCatalog.ADENA, 0, 200),
            new Route(310, ClassicWorldRastabadOumCatalog.TELEPORTER_SPAWN_ID,
                    ClassicWorldRastabadOumCatalog.TELEPORTER_NPC_ID,
                    32805, 32794, ClassicWorldRastabadOumCatalog.TELEPORT_INITIAL_HTML,
                    946, "teleport oum-oren", 34062, 32278, 4,
                    5, 1200, InventoryCatalog.ADENA, 0, 200),
            new Route(310, ClassicWorldRastabadOumCatalog.TELEPORTER_SPAWN_ID,
                    ClassicWorldRastabadOumCatalog.TELEPORTER_NPC_ID,
                    32805, 32794, ClassicWorldRastabadOumCatalog.TELEPORT_INITIAL_HTML,
                    947, "teleport oum-scave", 32922, 32802, 304,
                    5, 1300, InventoryCatalog.ADENA, 0, 200),
            new Route(310, ClassicWorldRastabadOumCatalog.TELEPORTER_SPAWN_ID,
                    ClassicWorldRastabadOumCatalog.TELEPORTER_NPC_ID,
                    32805, 32794, ClassicWorldRastabadOumCatalog.TELEPORT_INITIAL_HTML,
                    948, "teleport oum-lowful", 33118, 32933, 4,
                    5, 2000, InventoryCatalog.ADENA, 0, 200),
            new Route(310, ClassicWorldRastabadOumCatalog.TELEPORTER_SPAWN_ID,
                    ClassicWorldRastabadOumCatalog.TELEPORTER_NPC_ID,
                    32805, 32794, ClassicWorldRastabadOumCatalog.TELEPORT_INITIAL_HTML,
                    949, "teleport oum-chaos", 32885, 32652, 4,
                    5, 2000, InventoryCatalog.ADENA, 0, 200));
    private static final Route DIADRAD_ENTRY = new Route(
            4, 1286, 80146, 33695, 32422, "wa_earth1",
            1197, "teleport silveria-diad", 32828, 32900, 320,
            5, 7480, InventoryCatalog.ADENA, 0, 200);
    private static final List<Route> ELEMENTAL_GRAVE = List.of(
            new Route(420, 116, 50077, 32804, 33047, "telabyss1",
                    1057, "teleport abysslake-elegrave",
                    32924, 32801, 430, 5, 0, InventoryCatalog.ADENA, 0, 200),
            new Route(430, 115, 50076, 32922, 32798, "telgrave1",
                    1055, "teleport elegrave-abysslake",
                    32800, 33051, 420, 5, 0, InventoryCatalog.ADENA, 0, 200),
            new Route(430, 115, 50076, 32922, 32798, "telgrave1",
                    1056, "teleport elegrave-drakeIsland",
                    32669, 32982, 440, 5, 0, InventoryCatalog.ADENA, 0, 200));
    private static final List<Route> PIRATE_DUNGEON = List.of(
            new Route(444, 117, 50078, 32736, 32807, "picrystal",
                    1058, "teleport piratedungeon-elementaltomb",
                    32924, 32801, 430, 5, 0, InventoryCatalog.ADENA, 0, 200));
    private static final Route PIRATE_BACK_HALF_INVESTIGATOR = new Route(
            480, 118, ClassicWorldPirateBackHalfCatalog.INVESTIGATOR_NPC_ID,
            32852, 32798, "subinvest1", 1134, "B",
            32844, 32694, 550, 5, 0, InventoryCatalog.ADENA, 0, 200);
    private static final Route PIRATE_BACK_HALF_FRANCU_RETURN = new Route(
            482, ClassicWorldPirateBackHalfCatalog.FRANCU_RECONSTRUCTED_SPAWN_ID,
            ClassicWorldPirateBackHalfCatalog.FRANCU_NPC_ID,
            32679, 32860, "francu1",
            ClassicWorldKamylaQuestCatalog.FRANCU_RETURN_ROUTE_ID, "b",
            32735, 32845, 481, 5, 0, InventoryCatalog.ADENA, 0, 200);
    private static final List<Route> DESIRE_CAVE = List.of(
            new Route(4, 954, 80058, 32760, 33459, "cpass01",
                    1164, "a", 32757, 32794, 600, 5,
                    0, InventoryCatalog.ADENA, 0, 200),
            new Route(4, 954, 80058, 32760, 33459, "cpass01",
                    1165, "b", 32834, 32790, 778, 5,
                    0, InventoryCatalog.ADENA, 0, 200),
            new Route(601, 961, 80065, 32852, 32838, "uturn0",
                    1166, "4", 32747, 32813, 410, 5,
                    0, InventoryCatalog.ADENA, 0, 200));
    private static final List<Route> ROUTES = List.of(
            PHANTASM_ISLAND, GIRAN_TRADE_ZONE.get(0), GIRAN_TRADE_ZONE.get(1),
            ORC_ROUTES.get(0), ORC_ROUTES.get(1), ORC_ROUTES.get(2),
            FAIRY_DUNGEON_ROUTES.get(0), FAIRY_DUNGEON_ROUTES.get(1),
            HIDDEN_DOCK, SEPIA_DUNGEON.get(0), SEPIA_DUNGEON.get(1),
            TAINTED_LAND.get(0), TAINTED_LAND.get(1),
            GIRAN_CASTLE.get(0), GIRAN_CASTLE.get(1),
            GIRAN_CASTLE.get(2), GIRAN_CASTLE.get(3),
            WINDAWOOD_CASTLE.get(0), WINDAWOOD_CASTLE.get(1),
            WINDAWOOD_CASTLE.get(2), WINDAWOOD_CASTLE.get(3),
            KENT_COMPONENT.get(0), KENT_COMPONENT.get(1),
            KENT_COMPONENT.get(2), KENT_COMPONENT.get(3),
            DWARF_CASTLE.get(0), DWARF_CASTLE.get(1), DWARF_CASTLE.get(2),
            ADEN_CASTLE.get(0), ADEN_CASTLE.get(1), ADEN_CASTLE.get(2),
            HEINE_CASTLE.get(0), HEINE_CASTLE.get(1), HEINE_CASTLE.get(2),
            SILENT_CAVERN.get(0), SILENT_CAVERN.get(1), SILENT_CAVERN.get(2),
            SILENT_CAVERN.get(3), SILENT_CAVERN.get(4), SILENT_CAVERN.get(5),
            RASTABAD_OUM.get(0), RASTABAD_OUM.get(1), RASTABAD_OUM.get(2),
            RASTABAD_OUM.get(3), RASTABAD_OUM.get(4), RASTABAD_OUM.get(5),
            RASTABAD_OUM.get(6), RASTABAD_OUM.get(7), RASTABAD_OUM.get(8),
            DIADRAD_ENTRY,
            ELEMENTAL_GRAVE.get(0), ELEMENTAL_GRAVE.get(1),
            ELEMENTAL_GRAVE.get(2), PIRATE_DUNGEON.get(0),
            PIRATE_BACK_HALF_INVESTIGATOR, PIRATE_BACK_HALF_FRANCU_RETURN,
            DESIRE_CAVE.get(0), DESIRE_CAVE.get(1), DESIRE_CAVE.get(2));

    private ClassicWorldTeleportCatalog() {}

    static Route route(int mapId, long sourceObjectId, String action) {
        return ROUTES.stream()
                .filter(candidate -> candidate.sourceMapId() == mapId)
                .filter(candidate -> candidate.sourceObjectId() == sourceObjectId)
                .filter(candidate -> candidate.action().equals(action))
                .findFirst().orElse(null);
    }

    static Route source(int mapId, long sourceObjectId) {
        return ROUTES.stream()
                .filter(candidate -> candidate.sourceMapId() == mapId)
                .filter(candidate -> candidate.sourceObjectId() == sourceObjectId)
                .findFirst().orElse(null);
    }

    static String followUpHtml(int mapId, long sourceObjectId, String action) {
        if (mapId == 444
                && sourceObjectId == ClassicWorldNpcCatalog.objectId(
                        ClassicWorldPirateDungeonCatalog.CRYSTAL_SPAWN_ID)
                && ClassicWorldPirateDungeonCatalog.CRYSTAL_CONFIRM_ACTION
                        .equalsIgnoreCase(action)) {
            return ClassicWorldPirateDungeonCatalog.CRYSTAL_CONFIRM_HTML;
        }
        Route entry = TAINTED_LAND.get(0);
        if (mapId == entry.sourceMapId()
                && sourceObjectId == entry.sourceObjectId()
                && TAINTED_ENTRY_CONFIRM_ACTION.equalsIgnoreCase(action)) {
            return TAINTED_ENTRY_CONFIRM_HTML;
        }
        Route castle = GIRAN_CASTLE.get(0);
        if (mapId == castle.sourceMapId()
                && sourceObjectId == castle.sourceObjectId()
                && GIRAN_CASTLE_CONFIRM_ACTION.equalsIgnoreCase(action)) {
            return GIRAN_CASTLE_CONFIRM_HTML;
        }
        Route windawood = WINDAWOOD_CASTLE.get(0);
        if (mapId == windawood.sourceMapId()
                && sourceObjectId == windawood.sourceObjectId()
                && WINDAWOOD_CASTLE_CONFIRM_ACTION.equalsIgnoreCase(action)) {
            return WINDAWOOD_CASTLE_CONFIRM_HTML;
        }
        Route kent = KENT_COMPONENT.get(0);
        if (mapId == kent.sourceMapId()
                && sourceObjectId == kent.sourceObjectId()
                && KENT_CASTLE_CONFIRM_ACTION.equalsIgnoreCase(action)) {
            return KENT_CASTLE_CONFIRM_HTML;
        }
        Route ober = KENT_COMPONENT.get(3);
        if (mapId == ober.sourceMapId()
                && sourceObjectId == ober.sourceObjectId()
                && KENT_OBER_CONFIRM_ACTION.equalsIgnoreCase(action)
                ) {
            return KENT_OBER_CONFIRM_HTML;
        }
        Route dwarf = DWARF_CASTLE.get(0);
        if (mapId == dwarf.sourceMapId()
                && sourceObjectId == dwarf.sourceObjectId()
                && DWARF_CASTLE_CONFIRM_ACTION.equalsIgnoreCase(action)) {
            return DWARF_CASTLE_CONFIRM_HTML;
        }
        Route aden = ADEN_CASTLE.get(0);
        if (mapId == aden.sourceMapId()
                && sourceObjectId == aden.sourceObjectId()
                && ADEN_CASTLE_CONFIRM_ACTION.equalsIgnoreCase(action)) {
            return ADEN_CASTLE_CONFIRM_HTML;
        }
        Route heine = HEINE_CASTLE.get(0);
        if (mapId == heine.sourceMapId()
                && sourceObjectId == heine.sourceObjectId()
                && HEINE_CASTLE_CONFIRM_ACTION.equalsIgnoreCase(action)
                ) return HEINE_CASTLE_CONFIRM_HTML;
        Route silent = SILENT_CAVERN.get(0);
        if (mapId == silent.sourceMapId()
                && sourceObjectId == silent.sourceObjectId()
                && ClassicWorldSilentCavernCatalog.TELEPORT_CONFIRM_ACTION
                        .equalsIgnoreCase(action)) {
            return ClassicWorldSilentCavernCatalog.TELEPORT_CONFIRM_HTML;
        }
        Route oum = RASTABAD_OUM.get(0);
        return mapId == oum.sourceMapId()
                && sourceObjectId == oum.sourceObjectId()
                && ClassicWorldRastabadOumCatalog.TELEPORT_CONFIRM_ACTION
                        .equalsIgnoreCase(action)
                ? ClassicWorldRastabadOumCatalog.TELEPORT_CONFIRM_HTML : null;
    }

    static List<String> followUpArguments(
            int mapId, long sourceObjectId, String action) {
        String html = followUpHtml(mapId, sourceObjectId, action);
        if (TAINTED_ENTRY_CONFIRM_HTML.equals(html)) return List.of("10000");
        if (GIRAN_CASTLE_CONFIRM_HTML.equals(html)) {
            return List.of("0", "0", "0", "0");
        }
        if (WINDAWOOD_CASTLE_CONFIRM_HTML.equals(html)) {
            return List.of("0", "0", "0", "0");
        }
        if (KENT_CASTLE_CONFIRM_HTML.equals(html)) {
            return List.of("0", "0", "0");
        }
        if (KENT_OBER_CONFIRM_HTML.equals(html)) return List.of("10000");
        if (DWARF_CASTLE_CONFIRM_HTML.equals(html)) {
            return List.of("0", "0", "0");
        }
        if (ADEN_CASTLE_CONFIRM_HTML.equals(html)) {
            return List.of("0", "0", "0");
        }
        if (HEINE_CASTLE_CONFIRM_HTML.equals(html)) {
            return List.of("0", "0", "0");
        }
        if (ClassicWorldSilentCavernCatalog.TELEPORT_CONFIRM_HTML.equals(html)) {
            // Text.pak retains a seventh Silver Knight button, but neither retained
            // database has a matching scave-silver server route.  Keep its variable
            // slot deterministic without inventing a destination.
            return List.of("1500", "800", "600", "1800", "1800", "1000", "0");
        }
        if (ClassicWorldRastabadOumCatalog.TELEPORT_CONFIRM_HTML.equals(html)) {
            return List.of("2200", "1800", "1000", "1600", "2200", "1200",
                    "1300", "2000", "2000");
        }
        return List.of();
    }

    static Route phantasmIsland() {
        return PHANTASM_ISLAND;
    }

    static List<Route> giranTradeZone() {
        return GIRAN_TRADE_ZONE;
    }

    static List<Route> orcRoutes() {
        return ORC_ROUTES;
    }

    static List<Route> fairyDungeonRoutes() {
        return FAIRY_DUNGEON_ROUTES;
    }

    static Route hiddenDock() {
        return HIDDEN_DOCK;
    }

    static List<Route> sepiaDungeon() {
        return SEPIA_DUNGEON;
    }

    static List<Route> taintedLand() {
        return TAINTED_LAND;
    }

    static List<Route> giranCastle() {
        return GIRAN_CASTLE;
    }

    static List<Route> windawoodCastle() {
        return WINDAWOOD_CASTLE;
    }

    static List<Route> kentComponent() {
        return KENT_COMPONENT;
    }

    static List<Route> dwarfCastle() {
        return DWARF_CASTLE;
    }

    static List<Route> adenCastle() {
        return ADEN_CASTLE;
    }

    static List<Route> heineCastle() {
        return HEINE_CASTLE;
    }

    static List<Route> silentCavern() {
        return SILENT_CAVERN;
    }

    static List<Route> rastabadOum() {
        return RASTABAD_OUM;
    }

    static Route diadradEntry() {
        return DIADRAD_ENTRY;
    }

    static List<Route> elementalGraveRoutes() {
        return ELEMENTAL_GRAVE;
    }

    static List<Route> pirateDungeonRoutes() {
        return PIRATE_DUNGEON;
    }

    static Route pirateBackHalfInvestigatorRoute() {
        return PIRATE_BACK_HALF_INVESTIGATOR;
    }

    static Route pirateBackHalfFrancuReturnRoute() {
        return PIRATE_BACK_HALF_FRANCU_RETURN;
    }

    static List<Route> desireCaveRoutes() {
        return DESIRE_CAVE;
    }
}
