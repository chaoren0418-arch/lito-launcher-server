package local.lito381.server;

import java.util.List;

/** Exact selected 380_db contracts for Kent Castle maps15/240-243. */
final class ClassicWorldKentCastleCatalog {
    static final int CASTLE_ID = 1;
    static final int INTERIOR_MAP_ID = 15;
    static final List<Integer> COMPONENT_MAP_IDS = List.of(15, 240, 241, 242, 243);

    static final int TELEPORTER_SPAWN_ID = 46;
    static final int TELEPORTER_NPC_ID = 50021;
    static final int OBER_TELEPORTER_SPAWN_ID = 48;
    static final int OBER_TELEPORTER_NPC_ID = 50023;
    static final int NIKI_SPAWN_ID = 126;
    static final int NIKI_NPC_ID = 50118;
    static final int NIKI_X = 32749;
    static final int NIKI_Y = 32795;
    static final String NIKI_SELL_ACTION = "sell";
    static final int WAREHOUSE_SPAWN_ID = 263;
    static final int WAREHOUSE_NPC_ID = 60002;
    static final int WEAPON_ENCHANTER_SPAWN_ID = 673;
    static final int WEAPON_ENCHANTER_NPC_ID = 70547;
    static final int ARMOR_ENCHANTER_SPAWN_ID = 675;
    static final int ARMOR_ENCHANTER_NPC_ID = 70550;
    static final String WEAPON_ENCHANT_ACTION = "encw";
    static final String ARMOR_ENCHANT_ACTION = "enca";
    static final int STEWARD_SPAWN_ID = 676;
    static final int STEWARD_NPC_ID = 70553;
    static final int STEWARD_X = 32738;
    static final int STEWARD_Y = 32784;
    static final String CASTLE_GATE_ACTION = "castlegate";
    static final String TEXT_GATE_PAGE = "ishmael5";
    static final List<String> TEXT_ONLY_GATE_ACTIONS = List.of(
            "healegate_kent ogl",
            "healegate_kent ogf",
            "healigate_kent inner gate",
            "autorepairon",
            "autorepairoff");

    static final String NPC_SOURCE_SHA256 =
            "B5B4681508A75BB00FB700EE93A2FCA50CB5D23C2A3205198A6D392356599EDE";
    static final String NPC_TELEPORT_SOURCE_SHA256 =
            "1997A7DB20C9ADAC78BEB42493205964D106F7ECF25787895E5F985A856E1121";
    static final String DOOR_SOURCE_SHA256 =
            "57FCF3FC75332DCBE0DBE2BEE4AFC9D31936FCDF0930B49BB7D6A6BD312446B3";
    static final String SELECTED_SKILL_SOURCE_SHA256 =
            "05B09D30D06D722E9387C70B75CC714386242BB03F79E2B0AD5F0279BFC7F484";
    static final String SECONDARY_SKILL_SOURCE_SHA256 =
            "4E6135F21D04587C1771312C102AAE7EC17402E87F71E2A719C5E0A1D7516EDD";
    static final String NIKI_SHOP_SOURCE_SHA256 =
            "A7F51095A2B4BD99569CB93C1CD8E6CF02A9F3F4CC206D164ACEB353B4A3D423";
    static final String ACCOUNT_CASTLE_SOURCE_SHA256 =
            "1AEF7E95C0DD4280BE550635EFEDC7044FF690CDE7209E85F695FFDAE4D2B2E3";

    record Door(int doorId, int gfxId, int x, int y, int mapId, int direction,
                int edgeLow, int edgeHigh, int maximumHp, int keeperNpcId) {
        Door {
            if (doorId <= 0 || gfxId <= 0 || x <= 0 || y <= 0 || mapId < 0
                    || direction < 0 || direction > 1 || edgeLow <= 0
                    || edgeHigh < edgeLow || maximumHp < 0 || keeperNpcId <= 0) {
                throw new IllegalArgumentException("invalid retained Kent door");
            }
        }

        long objectId() {
            return 0x6C00_0000L | doorId;
        }
    }

    private static final List<Door> DOORS = List.of(
            new Door(2000, 339, 33171, 32759, 4, 0,
                    33170, 33171, 0, 70656),
            new Door(2001, 11987, 33111, 32770, 4, 1,
                    32767, 32773, 20_000, 70549),
            new Door(2002, 11989, 33112, 32763, 4, 1,
                    33112, 33112, 25_000, 70549),
            new Door(2003, 11991, 33112, 32777, 4, 1,
                    33112, 33112, 25_000, 70549));

    private ClassicWorldKentCastleCatalog() {}

    static List<Door> doors() {
        return DOORS;
    }

    static boolean isTextOnlyGateAction(String action) {
        return action != null && TEXT_ONLY_GATE_ACTIONS.stream()
                .anyMatch(candidate -> candidate.equalsIgnoreCase(action));
    }

    static boolean isSteward(ClassicWorldNpcCatalog.StaticNpc npc) {
        return exact(npc, STEWARD_SPAWN_ID, STEWARD_NPC_ID, STEWARD_X, STEWARD_Y);
    }

    static boolean isWeaponEnchanter(ClassicWorldNpcCatalog.StaticNpc npc) {
        return exact(npc, WEAPON_ENCHANTER_SPAWN_ID, WEAPON_ENCHANTER_NPC_ID,
                32747, 32785);
    }

    static boolean isArmorEnchanter(ClassicWorldNpcCatalog.StaticNpc npc) {
        return exact(npc, ARMOR_ENCHANTER_SPAWN_ID, ARMOR_ENCHANTER_NPC_ID,
                32747, 32784);
    }

    static boolean isNiki(ClassicWorldNpcCatalog.StaticNpc npc) {
        return exact(npc, NIKI_SPAWN_ID, NIKI_NPC_ID, NIKI_X, NIKI_Y);
    }

    private static boolean exact(ClassicWorldNpcCatalog.StaticNpc npc,
                                 int spawnId, int npcId, int x, int y) {
        return npc != null && npc.mapId() == INTERIOR_MAP_ID
                && npc.spawnId() == spawnId && npc.npcId() == npcId
                && npc.x() == x && npc.y() == y;
    }
}
