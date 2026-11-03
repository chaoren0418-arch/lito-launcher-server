package local.lito381.server;

import java.util.List;

/** Exact selected 380_db contracts for Windawood Castle maps23/24/29. */
final class ClassicWorldWindawoodCastleCatalog {
    static final int CASTLE_ID = 3;
    static final int INTERIOR_MAP_ID = 29;
    static final List<Integer> COMPONENT_MAP_IDS = List.of(23, 24, 29);

    static final int TELEPORTER_SPAWN_ID = 93;
    static final int TELEPORTER_NPC_ID = 50052;
    static final int WAREHOUSE_SPAWN_ID = 282;
    static final int WAREHOUSE_NPC_ID = 60022;
    static final int WEAPON_ENCHANTER_SPAWN_ID = 765;
    static final int WEAPON_ENCHANTER_NPC_ID = 70777;
    static final int ARMOR_ENCHANTER_SPAWN_ID = 767;
    static final int ARMOR_ENCHANTER_NPC_ID = 70780;
    static final String WEAPON_ENCHANT_ACTION = "encw";
    static final String ARMOR_ENCHANT_ACTION = "enca";
    static final int STEWARD_SPAWN_ID = 770;
    static final int STEWARD_NPC_ID = 70784;
    static final int STEWARD_X = 32737;
    static final int STEWARD_Y = 32784;
    static final String CASTLE_GATE_ACTION = "castlegate";
    static final String TEXT_GATE_PAGE = "othmond5";
    static final List<String> TEXT_ONLY_GATE_ACTIONS = List.of(
            "healegate_winda ogl",
            "healegate_winda ogf",
            "healigate_windawood inner gate",
            "autorepairon",
            "autorepairoff");

    static final String NPC_SOURCE_SHA256 =
            "2EE0A6684889C238B3FC9AD7EBBF961119ED6DE8EB7450050E02726346CD9E51";
    static final String NPC_TELEPORT_SOURCE_SHA256 =
            "45F0F234AF7F11F400ED4F42B089C99170198D98E38DF26A954D9AB401C5841F";
    static final String DOOR_SOURCE_SHA256 =
            "3F02423AA89057BF71916DF30BF86A1350C83A521155CEB78CCEDD7390347475";
    static final String SELECTED_SKILL_SOURCE_SHA256 =
            "05B09D30D06D722E9387C70B75CC714386242BB03F79E2B0AD5F0279BFC7F484";
    static final String SECONDARY_SKILL_SOURCE_SHA256 =
            "4E6135F21D04587C1771312C102AAE7EC17402E87F71E2A719C5E0A1D7516EDD";
    static final String ACCOUNT_CASTLE_SOURCE_SHA256 =
            "7FFBED0E8A9856065F1A6BF5ED79EA3CC93BCD122EF88B529BA95A558902AA9D";

    static final int ENCHANT_WEAPON_SKILL_ID =
            ClassicWorldGiranCastleCatalog.ENCHANT_WEAPON_SKILL_ID;
    static final int ENCHANT_WEAPON_DURATION_SECONDS =
            ClassicWorldGiranCastleCatalog.ENCHANT_WEAPON_DURATION_SECONDS;
    static final int ENCHANT_WEAPON_ACTION_ID =
            ClassicWorldGiranCastleCatalog.ENCHANT_WEAPON_ACTION_ID;
    static final int ENCHANT_WEAPON_EFFECT_GFX =
            ClassicWorldGiranCastleCatalog.ENCHANT_WEAPON_EFFECT_GFX;
    static final int ENCHANT_WEAPON_DAMAGE =
            ClassicWorldGiranCastleCatalog.ENCHANT_WEAPON_DAMAGE;
    static final int ENCHANT_ARMOR_SKILL_ID =
            ClassicWorldGiranCastleCatalog.ENCHANT_ARMOR_SKILL_ID;
    static final int ENCHANT_ARMOR_DURATION_SECONDS =
            ClassicWorldGiranCastleCatalog.ENCHANT_ARMOR_DURATION_SECONDS;
    static final int ENCHANT_ARMOR_ACTION_ID =
            ClassicWorldGiranCastleCatalog.ENCHANT_ARMOR_ACTION_ID;
    static final int ENCHANT_ARMOR_EFFECT_GFX =
            ClassicWorldGiranCastleCatalog.ENCHANT_ARMOR_EFFECT_GFX;
    static final int ENCHANT_ARMOR_CLASS =
            ClassicWorldGiranCastleCatalog.ENCHANT_ARMOR_CLASS;

    record Door(int doorId, int gfxId, int x, int y, int mapId, int direction,
                int edgeLow, int edgeHigh, int maximumHp, int keeperNpcId) {
        Door {
            if (doorId <= 0 || gfxId <= 0 || x <= 0 || y <= 0 || mapId < 0
                    || direction < 0 || direction > 1 || edgeLow <= 0
                    || edgeHigh < edgeLow || maximumHp < 0 || keeperNpcId <= 0) {
                throw new IllegalArgumentException("invalid retained Windawood door");
            }
        }

        long objectId() {
            return 0x6C00_0000L | doorId;
        }
    }

    private static final List<Door> DOORS = List.of(
            new Door(2020, 339, 32678, 33392, 4, 0,
                    32677, 32678, 0, 70778),
            new Door(2021, 2736, 32590, 33409, 4, 1,
                    33407, 33410, 10_000, 70987),
            new Door(2022, 3234, 32625, 33436, 4, 0,
                    32623, 32626, 10_000, 70687));

    private ClassicWorldWindawoodCastleCatalog() {}

    static List<Door> doors() {
        return DOORS;
    }

    /**
     * Actions present in selected Text.pak but unreachable through the retained
     * 3.80 castle steward flow.  The authoritative castlegate branch repairs
     * all castle-area doors directly and closes the dialogue, so these must not
     * be promoted to independently callable server mutations.
     */
    static boolean isTextOnlyGateAction(String action) {
        return action != null && TEXT_ONLY_GATE_ACTIONS.stream()
                .anyMatch(candidate -> candidate.equalsIgnoreCase(action));
    }

    static boolean isSteward(ClassicWorldNpcCatalog.StaticNpc npc) {
        return exact(npc, STEWARD_SPAWN_ID, STEWARD_NPC_ID, STEWARD_X, STEWARD_Y);
    }

    static boolean isWeaponEnchanter(ClassicWorldNpcCatalog.StaticNpc npc) {
        return exact(npc, WEAPON_ENCHANTER_SPAWN_ID, WEAPON_ENCHANTER_NPC_ID,
                32747, 32786);
    }

    static boolean isArmorEnchanter(ClassicWorldNpcCatalog.StaticNpc npc) {
        return exact(npc, ARMOR_ENCHANTER_SPAWN_ID, ARMOR_ENCHANTER_NPC_ID,
                32746, 32786);
    }

    private static boolean exact(ClassicWorldNpcCatalog.StaticNpc npc,
                                 int spawnId, int npcId, int x, int y) {
        return npc != null && npc.mapId() == INTERIOR_MAP_ID
                && npc.spawnId() == spawnId && npc.npcId() == npcId
                && npc.x() == x && npc.y() == y;
    }
}
