package local.lito381.server;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resource-identified core and service NPCs for native LiTo rental town map 500.
 *
 * <p>The LTdesc/LTicon indices, displayed names, local HTML entry points and
 * action strings are exact selected-client resources.  The original server's
 * actor packets did not survive.  In particular, the client-side LTdesc/LTicon
 * resource namespace beginning at 20000 is not the Sprite.idx actor namespace:
 * sending one of those values in S_PUT_OBJECT makes the selected client index
 * beyond its sprite table.  Actor GFX values below are therefore deliberately
 * role-matched compatibility choices from the retained 2010 server and the
 * selected 3.80 database, and every value is checked against the selected
 * spr_action table.  Coordinates are likewise deliberately
 * tagged as a mixed resource/retained-geometry placement policy: Pan is south
 * east of the exact Layer8 fountain effect as stated by LTCGuide, while Soul
 * and Ezee reuse same-geometry retained functional anchors.  The remaining
 * two core anchors and the eight service anchors are passable native-map policy
 * positions, not claimed upstream coordinates.  Service actions are exposed
 * only where their selected-client HTML is exact.  Rental prices now have a
 * hash-pinned retained-2010 compatibility source, but are not presented as
 * surviving official values; hotel room/key assignments remain unavailable.</p>
 */
final class NativeLitoRentalTownNpcCatalog {
    static final int MAP_ID = 500;
    static final String IDENTITY_AUTHORITY =
            "selected-client-LTdesc-LTicon-HTML-sub_442A30";
    static final String PLACEMENT_AUTHORITY =
            "native-Layer8-guide-plus-retained-map4302-functional-anchors";
    static final String ACTOR_GFX_AUTHORITY =
            "retained-2010-role-match-plus-selected-380-role-match";
    static final boolean HISTORICAL_ACTOR_COORDINATES_EXACT = false;
    static final boolean HISTORICAL_ACTOR_GFX_EXACT = false;
    static final int INTERACTION_RANGE = 5;

    // Retained/source-role matches, not claims about the unavailable LiTo actor packets.
    static final int GUIDE_GFX = 992;              // selected 3.80 arena manager
    static final int CLAN_MANAGER_GFX = 949;       // selected 3.80 clan executor
    static final int WEAPON_MERCHANT_GFX = 2505;   // retained 2010 tournament merchant 71286
    static final int ARMOR_MERCHANT_GFX = 1254;    // retained 2010 tournament merchant 71287
    static final int INNKEEPER_GFX = 695;          // selected 3.80 innkeeper 70012
    static final int INNKEEPER_ALT_GFX = 700;      // selected 3.80 innkeeper 70019
    static final int LEAD_TELEPORTER_GFX = 2141;   // selected 3.80 beginner teleporter
    static final int TELEPORTER_GFX = 940;          // selected 3.80 town teleporter
    static final int TELEPORTER_ALT_GFX = 460;      // selected 3.80 teleporter 50023
    static final int TELEPORTER_ALT_2_GFX = 2712;   // selected 3.80 teleporter 50031

    static final int AUTHORITY_EMBLEM_INDEX = 6;
    static final int CRAFTSMAN_EMBLEM_INDEX = 7;
    static final int DELIGHT_EMBLEM_INDEX = 8;
    static final int PEACE_EMBLEM_INDEX = 9;
    static final int MAYA_RESOURCE_INDEX = 2;
    static final int FIFO_RESOURCE_INDEX = 10;
    static final int KRI_RESOURCE_INDEX = 11;
    static final int LOVE_RESOURCE_INDEX = 12;
    static final int SHEBA_RESOURCE_INDEX = 91;
    static final int YUILIN_RESOURCE_INDEX = 92;
    static final int AIN_RESOURCE_INDEX = 93;
    static final int YUROYA_RESOURCE_INDEX = 94;
    static final int ASHA_RESOURCE_INDEX = 95;

    static final String REQUEST_LIGHT_GEM = "request LTC light jewel";
    static final String REQUEST_AUTHORITY = "request LTC emblem of authority";
    static final String REQUEST_CRAFTSMAN = "request LTC emblem of craftsman";
    static final String REQUEST_DELIGHT = "request LTC emblem of delight";
    static final String REQUEST_PEACE = "request LTC emblem of peace";
    static final String RENT_ACTION = "buy";
    static final String RETURN_ACTION = "sell";
    static final String NO_RENT_HTML = "NoRent";
    static final boolean HISTORICAL_RENTAL_PRICE_CATALOG_AVAILABLE = false;
    static final boolean RETAINED_RENTAL_PRICE_CATALOG_AVAILABLE = true;
    static final String HOTEL_ROOM_ACTION = "room";
    static final String HOTEL_RETURN_ACTION = "return";
    static final String HOTEL_ENTER_ACTION = "enter";
    static final boolean HISTORICAL_HOTEL_ROOM_MAP_ASSIGNMENT_AVAILABLE = false;
    static final String TELEPORT_DEFAULT_HTML = "TeleportMe";
    static final String TELEPORT_TRIAL_HTML = "TeleportMe_C";
    static final String TELEPORT_HONOR_HTML = "TeleportMe_L";
    static final String TELEPORT_ACTION_A = "LT_TELEPORT_A";
    static final String TELEPORT_ACTION_B = "LT_TELEPORT_B";
    static final String TELEPORT_ACTION_C = "LT_TELEPORT_C";
    static final String TELEPORT_ACTION_D = "LT_TELEPORT_D";
    static final boolean HISTORICAL_TELEPORT_HTML_ASSIGNMENT_EXACT = false;
    static final boolean HISTORICAL_TELEPORT_ENDPOINT_CATALOG_AVAILABLE = false;
    static final Map<String, Set<String>> TELEPORT_ACTIONS_BY_HTML = Map.of(
            TELEPORT_DEFAULT_HTML, Set.of(TELEPORT_ACTION_B, TELEPORT_ACTION_C),
            TELEPORT_TRIAL_HTML,
                Set.of(TELEPORT_ACTION_B, TELEPORT_ACTION_C, TELEPORT_ACTION_D),
            TELEPORT_HONOR_HTML, Set.of(TELEPORT_ACTION_A));

    enum Role { GUIDE, EMBLEM, RENTAL_MERCHANT, HOTEL_SERVICE, TELEPORT_SERVICE }

    record Npc(
            int resourceIndex,
            int gfxId,
            long objectId,
            int x,
            int y,
            int heading,
            String name,
            String startHtml,
            String endHtml,
            Role role,
            String action,
            int emblemResourceIndex,
            String emblemName) {
        Npc {
            if (!supportedNpcResource(resourceIndex)
                    || gfxId <= 0
                    || gfxId >= Protocol.LITO_RESOURCE_WIRE_BASE
                    || !BattleAnimationCatalog.get().supportsTimedAction(
                        gfxId, BattleActionCodes.WALK)
                    || objectId != NativeLitoRentalTownNpcCatalog.objectId(resourceIndex)
                    || x <= 0 || x > 0xFFFF || y <= 0 || y > 0xFFFF
                    || heading < 0 || heading > 7
                    || name == null || name.isBlank()
                    || startHtml == null || startHtml.isBlank()
                    || role == null
                    || !NativeLitoTownPassabilityCatalog.get()
                        .isStandingPassable(MAP_ID, x, y)) {
                throw new IllegalArgumentException(
                        "invalid native LiTo rental-town NPC resource " + resourceIndex);
            }
            switch (role) {
                case GUIDE -> {
                    if (endHtml == null || endHtml.isBlank()
                            || action == null || action.isBlank()
                            || emblemResourceIndex >= 0 || emblemName != null) {
                        throw new IllegalArgumentException("invalid rental-town guide");
                    }
                }
                case EMBLEM -> {
                    if (endHtml == null || endHtml.isBlank()
                            || action == null || action.isBlank()
                            || emblemResourceIndex < AUTHORITY_EMBLEM_INDEX
                            || emblemResourceIndex > PEACE_EMBLEM_INDEX
                            || emblemName == null || emblemName.isBlank()) {
                        throw new IllegalArgumentException("invalid rental-town emblem NPC");
                    }
                }
                case RENTAL_MERCHANT, HOTEL_SERVICE, TELEPORT_SERVICE -> {
                    if (endHtml != null || action != null
                            || emblemResourceIndex >= 0 || emblemName != null) {
                        throw new IllegalArgumentException("invalid rental-town service NPC");
                    }
                }
            }
        }

    }

    private static final List<Npc> NPCS = List.of(
            npc(0, GUIDE_GFX, 32772, 32836, 7, "指導員-潘塔休",
                    "Phan_Start", "Phan_End_k", Role.GUIDE,
                    REQUEST_LIGHT_GEM, -1, null),
            npc(1, CLAN_MANAGER_GFX, 32767, 32825, 4, "血盟管理員-蘇耳",
                    "Soul_Start", "Soul_End", Role.EMBLEM,
                    REQUEST_AUTHORITY, AUTHORITY_EMBLEM_INDEX, "權威的象徵"),
            npc(2, WEAPON_MERCHANT_GFX, 32762, 32828, 2, "武器商人-瑪雅",
                    "Maya_Start", "Maya_End", Role.EMBLEM,
                    REQUEST_CRAFTSMAN, CRAFTSMAN_EMBLEM_INDEX, "工匠的象徵"),
            npc(3, LEAD_TELEPORTER_GFX, 32775, 32824, 6, "首席傳送師-伊奇",
                    "Ezee_Start", "Ezee_End", Role.EMBLEM,
                    REQUEST_DELIGHT, DELIGHT_EMBLEM_INDEX, "歡喜的象徵"),
            npc(4, INNKEEPER_GFX, 32774, 32832, 0, "旅館主人-阿里亞",
                    "Aria_Start", "Aria_End", Role.EMBLEM,
                    REQUEST_PEACE, PEACE_EMBLEM_INDEX, "和平的象徵"),
            serviceNpc(10, ARMOR_MERCHANT_GFX, 32758, 32820, 4, "防具商人-菲佛",
                    "Paper_start", Role.RENTAL_MERCHANT),
            serviceNpc(11, ARMOR_MERCHANT_GFX, 32762, 32820, 4, "防具商人-克里",
                    "Click_Start", Role.RENTAL_MERCHANT),
            serviceNpc(12, ARMOR_MERCHANT_GFX, 32766, 32820, 4, "飾品商人-勒夫",
                    "Love_Start", Role.RENTAL_MERCHANT),
            serviceNpc(91, INNKEEPER_ALT_GFX, 32780, 32832, 6, "旅館服務生-席巴",
                    "inn", Role.HOTEL_SERVICE),
            serviceNpc(92, TELEPORTER_GFX, 32778, 32820, 0, "傳送師-尤依琳",
                    TELEPORT_DEFAULT_HTML, Role.TELEPORT_SERVICE),
            serviceNpc(93, LEAD_TELEPORTER_GFX, 32782, 32820, 2, "傳送師-艾恩",
                    TELEPORT_DEFAULT_HTML, Role.TELEPORT_SERVICE),
            serviceNpc(94, TELEPORTER_ALT_GFX, 32782, 32824, 4, "傳送師-尤蘿雅",
                    TELEPORT_DEFAULT_HTML, Role.TELEPORT_SERVICE),
            serviceNpc(95, TELEPORTER_ALT_2_GFX, 32782, 32828, 6, "傳送師-阿霞",
                    TELEPORT_DEFAULT_HTML, Role.TELEPORT_SERVICE));

    private NativeLitoRentalTownNpcCatalog() {}

    static List<Npc> forMap(int mapId) {
        return mapId == MAP_ID ? NPCS : List.of();
    }

    static Npc find(int mapId, long objectId) {
        if (mapId != MAP_ID) return null;
        for (Npc npc : NPCS) {
            if (npc.objectId() == objectId) return npc;
        }
        return null;
    }

    static Npc emblemNpcForAction(String action) {
        if (action == null) return null;
        for (Npc npc : NPCS) {
            if (npc.role() == Role.EMBLEM && npc.action().equals(action)) return npc;
        }
        return null;
    }

    static boolean isMaya(Npc npc) {
        return npc != null && npc.resourceIndex() == MAYA_RESOURCE_INDEX;
    }

    static boolean isRentalMerchant(Npc npc) {
        return npc != null && (npc.resourceIndex() == MAYA_RESOURCE_INDEX
                || npc.role() == Role.RENTAL_MERCHANT);
    }

    static boolean isHotelService(Npc npc) {
        return npc != null && npc.role() == Role.HOTEL_SERVICE;
    }

    static boolean isHotelAction(String action) {
        return HOTEL_ROOM_ACTION.equals(action)
                || HOTEL_RETURN_ACTION.equals(action)
                || HOTEL_ENTER_ACTION.equals(action);
    }

    static boolean isTeleportService(Npc npc) {
        return npc != null && npc.role() == Role.TELEPORT_SERVICE;
    }

    static boolean isTeleportActionForNpc(Npc npc, String action) {
        Set<String> actions = npc == null
                ? null : TELEPORT_ACTIONS_BY_HTML.get(npc.startHtml());
        return actions != null && actions.contains(action);
    }

    static long emblemObjectId(int resourceIndex) {
        if (resourceIndex < AUTHORITY_EMBLEM_INDEX
                || resourceIndex > PEACE_EMBLEM_INDEX) {
            throw new IllegalArgumentException("invalid rental-town emblem index");
        }
        return 0x6C00_0000L + resourceIndex;
    }

    private static long objectId(int resourceIndex) {
        return 0x6C10_0000L + resourceIndex;
    }

    private static boolean supportedNpcResource(int resourceIndex) {
        return resourceIndex >= 0 && resourceIndex <= 4
                || resourceIndex >= FIFO_RESOURCE_INDEX
                    && resourceIndex <= LOVE_RESOURCE_INDEX
                || resourceIndex >= SHEBA_RESOURCE_INDEX
                    && resourceIndex <= ASHA_RESOURCE_INDEX;
    }

    private static Npc npc(
            int resourceIndex, int gfxId, int x, int y, int heading, String name,
            String startHtml, String endHtml, Role role, String action,
            int emblemResourceIndex, String emblemName) {
        return new Npc(resourceIndex, gfxId, objectId(resourceIndex), x, y, heading,
                name, startHtml, endHtml, role, action,
                emblemResourceIndex, emblemName);
    }

    private static Npc serviceNpc(
            int resourceIndex, int gfxId, int x, int y, int heading, String name,
            String startHtml, Role role) {
        return new Npc(resourceIndex, gfxId, objectId(resourceIndex), x, y, heading,
                name, startHtml, null, role, null, -1, null);
    }
}
