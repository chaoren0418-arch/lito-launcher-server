package local.lito381.server;

import java.util.List;

/** Exact retained 380_db contracts required by the Giran Castle component. */
final class ClassicWorldGiranCastleCatalog {
    static final int CASTLE_ID = 4;
    static final int INTERIOR_MAP_ID = 52;

    static final int TELEPORTER_SPAWN_ID = 77;
    static final int TELEPORTER_NPC_ID = 50035;
    static final int WAREHOUSE_SPAWN_ID = 273;
    static final int WAREHOUSE_NPC_ID = 60012;
    static final int WEAPON_ENCHANTER_SPAWN_ID = 717;
    static final int WEAPON_ENCHANTER_NPC_ID = 70599;
    static final String WEAPON_ENCHANT_ACTION = "encw";
    static final int ARMOR_ENCHANTER_SPAWN_ID = 719;
    static final int ARMOR_ENCHANTER_NPC_ID = 70601;
    static final String ARMOR_ENCHANT_ACTION = "enca";
    static final int STEWARD_SPAWN_ID = 725;
    static final int STEWARD_NPC_ID = 70623;
    static final int STEWARD_X = 32732;
    static final int STEWARD_Y = 32789;

    static final String NPC_SOURCE_SHA256 =
            "5B400EC1C2454846FD29572C738AEEB4D7701DAFA15C26DBE07AED931ED61089";
    static final String NPC_TELEPORT_SOURCE_SHA256 =
            "4AEBCAA0133852D02E1901C54FC9F0DC52EBC42191A0F9B5A7B6CCEA3C20344B";
    static final String DOOR_SOURCE_SHA256 =
            "25FF3C137D0E4131B74E5A0843697AA7B31186EAF8F33D93C55D0A7D1D905872";
    static final String SELECTED_SKILL_SOURCE_SHA256 =
            "05B09D30D06D722E9387C70B75CC714386242BB03F79E2B0AD5F0279BFC7F484";
    static final String SECONDARY_SKILL_SOURCE_SHA256 =
            "4E6135F21D04587C1771312C102AAE7EC17402E87F71E2A719C5E0A1D7516EDD";

    static final int ENCHANT_WEAPON_SKILL_ID = 12;
    static final int ENCHANT_WEAPON_DURATION_SECONDS = 1_800;
    static final int ENCHANT_WEAPON_ACTION_ID = 19;
    static final int ENCHANT_WEAPON_EFFECT_GFX = 747;
    static final int ENCHANT_WEAPON_DAMAGE = 2;
    static final int ENCHANT_ARMOR_SKILL_ID = 21;
    static final int ENCHANT_ARMOR_DURATION_SECONDS = 1_800;
    static final int ENCHANT_ARMOR_ACTION_ID = 19;
    static final int ENCHANT_ARMOR_EFFECT_GFX = 748;
    static final int ENCHANT_ARMOR_CLASS = -3;

    record Door(int doorId, int gfxId, int x, int y, int mapId, int direction,
                int edgeLow, int edgeHigh, int maximumHp, int keeperNpcId) {
        Door {
            if (doorId <= 0 || gfxId <= 0 || x <= 0 || y <= 0 || mapId < 0
                    || direction < 0 || direction > 1 || edgeLow <= 0
                    || edgeHigh < edgeLow || maximumHp < 0 || keeperNpcId <= 0) {
                throw new IllegalArgumentException("invalid retained castle door");
            }
        }

        long objectId() {
            return 0x6C00_0000L | doorId;
        }
    }

    private static final List<Door> DOORS = List.of(
            new Door(2031, 2745, 33631, 32734, 4, 0,
                    33630, 33633, 10_000, 70800),
            new Door(2030, 1336, 33632, 32660, 4, 0,
                    33631, 33632, 0, 70817),
            new Door(2032, 2746, 33580, 32678, 4, 1,
                    32676, 32679, 10_000, 70988),
            new Door(2034, 1338, 33609, 32677, 4, 1,
                    32677, 32678, 3_000, 70989),
            new Door(2033, 2744, 33631, 32702, 4, 0,
                    33631, 33634, 10_000, 70990),
            new Door(2035, 1338, 33656, 32677, 4, 1,
                    32677, 32678, 3_000, 70991));

    private ClassicWorldGiranCastleCatalog() {}

    static List<Door> doors() {
        return DOORS;
    }

    static boolean isSteward(ClassicWorldNpcCatalog.StaticNpc npc) {
        return exact(npc, STEWARD_SPAWN_ID, STEWARD_NPC_ID, STEWARD_X, STEWARD_Y);
    }

    static boolean isWeaponEnchanter(ClassicWorldNpcCatalog.StaticNpc npc) {
        return exact(npc, WEAPON_ENCHANTER_SPAWN_ID, WEAPON_ENCHANTER_NPC_ID,
                32742, 32813);
    }

    static boolean isArmorEnchanter(ClassicWorldNpcCatalog.StaticNpc npc) {
        return exact(npc, ARMOR_ENCHANTER_SPAWN_ID, ARMOR_ENCHANTER_NPC_ID,
                32741, 32813);
    }

    private static boolean exact(ClassicWorldNpcCatalog.StaticNpc npc,
                                 int spawnId, int npcId, int x, int y) {
        return npc != null && npc.mapId() == INTERIOR_MAP_ID
                && npc.spawnId() == spawnId && npc.npcId() == npcId
                && npc.x() == x && npc.y() == y;
    }
}
