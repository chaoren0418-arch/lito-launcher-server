package local.lito381.server;

import java.util.List;

/** Exact selected 380_db contracts for Dwarf Castle/Cave map66. */
final class ClassicWorldDwarfCastleCatalog {
    static final int CASTLE_ID = 6;
    static final int MAP_ID = 66;

    static final int TELEPORTER_SPAWN_ID = 80;
    static final int TELEPORTER_NPC_ID = 50040;
    static final int WAREHOUSE_SPAWN_ID = 275;
    static final int WAREHOUSE_NPC_ID = 60014;
    static final int WEAPON_ENCHANTER_SPAWN_ID = 731;
    static final int WEAPON_ENCHANTER_NPC_ID = 70655;
    static final int ARMOR_ENCHANTER_SPAWN_ID = 733;
    static final int ARMOR_ENCHANTER_NPC_ID = 70657;
    static final String WEAPON_ENCHANT_ACTION = "encw";
    static final String ARMOR_ENCHANT_ACTION = "enca";
    static final int STEWARD_SPAWN_ID = 738;
    static final int STEWARD_NPC_ID = 70665;
    static final int STEWARD_X = 32865;
    static final int STEWARD_Y = 32808;
    static final int COMMANDER_SPAWN_ID = 739;
    static final int COMMANDER_NPC_ID = 70666;
    static final int COMMANDER_X = 32864;
    static final int COMMANDER_Y = 32804;
    static final int TOWER_SPAWN_ID = 1007;
    static final int TOWER_NPC_ID = 81111;
    static final int TOWER_X = 32828;
    static final int TOWER_Y = 32818;

    static final String CASTLE_GATE_ACTION = "castlegate";
    static final String TEXT_GATE_PAGE = "potempin5";
    static final List<String> TEXT_ONLY_GATE_ACTIONS = List.of(
            "healegate_iron door a",
            "healegate_iron door b",
            "autorepairon",
            "autorepairoff");
    static final String OPEN_INNER_GATE_ACTION = "openigate";
    static final String CLOSE_INNER_GATE_ACTION = "closeigate";
    static final String OUTER_MEMBER_HTML = "gateokeeper";
    static final String INNER_MEMBER_HTML = "gatekeeper";
    static final String OUTSIDER_HTML = "gatekeeperop";
    static final String COMMANDER_DEMAND_ACTION = "demand";
    static final String COMMANDER_EMPTY_HTML = "freckson2";

    /** Text.pak emits this action, but neither retained database contains it. */
    static final String TEXT_ONLY_GRAVE_ACTION = "teleport dwarfc-grave";
    /** The exact retained row remains callable only by its database action. */
    static final String DATABASE_CRYSTAL_ACTION = "teleport dwarfc-crystal";

    static final String MAP_SOURCE_SHA256 =
            "2F3B8ABB05AD0F9451B597E3CC197257CCE8C79D76F549C5EC9AB4B44144D7DF";
    static final String MAP_RESOURCE_SHA256 =
            "DA3E2E0F407151636E784507AFEB8CB0C5F7BC97211A335948DBCB140844825E";
    static final String MAP_FLAGS_SOURCE_SHA256 =
            "7B7A8483525BC58C749C18C5D69BAC783FC1FAAF9C39156D00A4CBA79808B805";
    static final String MAP_GETBACK_SOURCE_SHA256 =
            "15B638A4E5235376CD92040B00E32A2BE51D5BE6025E03B00CEAEBEA9568A138";
    static final String GROUND_ROUTE_SOURCE_SHA256 =
            "A99786D38C3BE9BB96C9AAD2C38977C4EDFBF25CA83E6DEBA9B9376F4C034EF8";
    static final String NPC_SOURCE_SHA256 =
            "7E36BDE3CA2D3574AAA6BC28CE11FB4316BF89E2A45DC85F812434FA05D1B379";
    static final String NPC_RESOURCE_SHA256 =
            "2F880E21B47BD9271AB4B59F62F3D8D6332F1CCDEF251C26E5FD3B16FED9B7EF";
    static final String NPC_TELEPORT_SOURCE_SHA256 =
            "6EDBC13F2AF01EB024E1FB649F2635E9829CB11EB0A66BFCC2A65193DEDCBDB3";
    static final String DOOR_SOURCE_SHA256 =
            "3A0E290F4D9CF1F4A465D724554200C8D35735373286F1AC3ABDE7FA7DD84948";
    static final String SHOP_SOURCE_SHA256 =
            "A7F51095A2B4BD99569CB93C1CD8E6CF02A9F3F4CC206D164ACEB353B4A3D423";
    static final String MONSTER_SOURCE_SHA256 =
            "219255B3830F63B32C0D35542B91DF47B5532113719CB79075F5CD233B6DE5E7";
    static final String MONSTER_RESOURCE_SHA256 =
            "A48DE200244873BECB6BFB456D76162803EB05B349ACA2FBD5296BDEE27CB9B7";
    static final String DROP_SOURCE_SHA256 =
            "3FE93EDCC0A0A171C6BB8ADF328460441832AF757DA26B9DAB1E8C8A2963C828";
    static final String DROP_RESOURCE_SHA256 =
            "99B8A7D6AFB90607A580537A73F934D1A047A817262AB87B1CB831F98D70E40F";
    static final String SELECTED_SKILL_SOURCE_SHA256 =
            "05B09D30D06D722E9387C70B75CC714386242BB03F79E2B0AD5F0279BFC7F484";
    static final String SECONDARY_SKILL_SOURCE_SHA256 =
            "4E6135F21D04587C1771312C102AAE7EC17402E87F71E2A719C5E0A1D7516EDD";
    static final String ACCOUNT_CASTLE_SOURCE_SHA256 =
            "EE11164B5B08425AF87BB1E0EB01139B0E30959A0CF536395E32CFE9D67C64A5";

    record Door(int doorId, int gfxId, int x, int y, int mapId, int direction,
                int edgeLow, int edgeHigh, int maximumHp, int keeperNpcId) {
        Door {
            if (doorId <= 0 || gfxId <= 0 || x <= 0 || y <= 0 || mapId != MAP_ID
                    || direction < 0 || direction > 1 || edgeLow <= 0
                    || edgeHigh < edgeLow || maximumHp < 0 || keeperNpcId <= 0) {
                throw new IllegalArgumentException("invalid retained Dwarf Castle door");
            }
        }

        long objectId() {
            return 0x6C00_0000L | doorId;
        }

        int entranceX() {
            return direction == 0 ? x : x - 1;
        }

        int entranceY() {
            return direction == 0 ? y + 1 : y;
        }

        boolean controls(int cellX, int cellY) {
            return direction == 0
                    ? cellY == entranceY()
                        && cellX >= edgeLow - 1 && cellX <= edgeHigh + 1
                    : cellX == entranceX()
                        && cellY >= edgeLow - 1 && cellY <= edgeHigh + 1;
        }
    }

    private static final List<Door> DOORS = List.of(
            new Door(2050, 443, 32845, 32812, MAP_ID, 1,
                    32812, 32813, 0, 70995),
            new Door(2051, 1826, 32780, 32859, MAP_ID, 1,
                    32857, 32860, 10_000, 70994),
            new Door(2052, 1827, 32811, 32887, MAP_ID, 0,
                    32810, 32813, 10_000, 70993));

    private ClassicWorldDwarfCastleCatalog() {}

    static List<Door> doors() {
        return DOORS;
    }

    static boolean isTextOnlyGateAction(String action) {
        return action != null && TEXT_ONLY_GATE_ACTIONS.stream()
                .anyMatch(candidate -> candidate.equalsIgnoreCase(action));
    }

    static boolean isTextOnlyTeleportAction(String action) {
        return TEXT_ONLY_GRAVE_ACTION.equals(action);
    }

    static boolean isSteward(ClassicWorldNpcCatalog.StaticNpc npc) {
        return exact(npc, STEWARD_SPAWN_ID, STEWARD_NPC_ID, STEWARD_X, STEWARD_Y);
    }

    static boolean isWeaponEnchanter(ClassicWorldNpcCatalog.StaticNpc npc) {
        return exact(npc, WEAPON_ENCHANTER_SPAWN_ID, WEAPON_ENCHANTER_NPC_ID,
                32861, 32800);
    }

    static boolean isArmorEnchanter(ClassicWorldNpcCatalog.StaticNpc npc) {
        return exact(npc, ARMOR_ENCHANTER_SPAWN_ID, ARMOR_ENCHANTER_NPC_ID,
                32860, 32800);
    }

    static boolean isCommander(ClassicWorldNpcCatalog.StaticNpc npc) {
        return exact(npc, COMMANDER_SPAWN_ID, COMMANDER_NPC_ID,
                COMMANDER_X, COMMANDER_Y);
    }

    static boolean isTower(ClassicWorldNpcCatalog.StaticNpc npc) {
        return exact(npc, TOWER_SPAWN_ID, TOWER_NPC_ID, TOWER_X, TOWER_Y);
    }

    static boolean isGuard(ClassicWorldNpcCatalog.StaticNpc npc) {
        return npc != null && npc.mapId() == MAP_ID
                && ((npc.spawnId() == 926 || npc.spawnId() == 927)
                    && npc.npcId() == 70993
                    || (npc.spawnId() == 928 || npc.spawnId() == 929)
                    && npc.npcId() == 70994
                    || (npc.spawnId() == 930 || npc.spawnId() == 931)
                    && npc.npcId() == 70995);
    }

    static boolean isOuterGuard(ClassicWorldNpcCatalog.StaticNpc npc) {
        return isGuard(npc) && (npc.npcId() == 70993 || npc.npcId() == 70994);
    }

    static boolean isInnerGuard(ClassicWorldNpcCatalog.StaticNpc npc) {
        return isGuard(npc) && npc.npcId() == 70995;
    }

    private static boolean exact(ClassicWorldNpcCatalog.StaticNpc npc,
                                 int spawnId, int npcId, int x, int y) {
        return npc != null && npc.mapId() == MAP_ID
                && npc.spawnId() == spawnId && npc.npcId() == npcId
                && npc.x() == x && npc.y() == y;
    }
}
