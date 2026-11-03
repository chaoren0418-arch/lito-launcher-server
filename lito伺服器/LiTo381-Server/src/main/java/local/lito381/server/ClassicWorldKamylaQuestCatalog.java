package local.lito381.server;

import java.util.List;
import java.util.Map;

/**
 * Exact retained Kamyla/Francu quest-32 dialogue and action contract.
 *
 * <p>The retained {@code L1MerchantInstance} owns the dialogue state while
 * {@code C_NPCAction.handlePirateIslandAction} owns actions a/c/e/i and the
 * three Kamyla teleports.  Francu's lower-case {@code b} return is instead
 * selected database row {@code npc_action_teleport.id=1160}.  Text.pak also
 * exposes five final reward actions g/h/i/j/k.  The retained dispatcher only
 * returned {@code kamyla25} for i and omitted the reward/state mutation, while
 * the other four actions were absent.  Those omissions are closed here from
 * the selected Text.pak action labels and the exact selected 380_db item rows;
 * the final transition is atomic and one-shot.</p>
 */
final class ClassicWorldKamylaQuestCatalog {
    static final int QUEST_ID = 32;
    static final int CADMUS_QUEST_ID = 31;
    static final int QUEST_END = 255;

    static final int DRAKE_KEY = 40621;
    static final int MAZE_MAP = 40644;
    static final int DIEGO_DIARY = 40630;
    static final int CRUMPLED_LOVE_LETTER = 40717;

    static final int SWORD_OF_HONOR = 33;
    static final int EDORYU_OF_ABYSS = 79;
    static final int CROSSBOW_OF_SILENCE = 178;
    static final int JET_BLACK_CRYSTAL_BALL = 118;
    static final int SHIELD_OF_FAITHFULNESS = 20234;

    static final String SELECTED_TEXT_IDX_SHA256 =
            "B23C985CDAD807808E996FED31DBCB174D920B17EEE65025535EE92BF76FEA82";
    static final String SELECTED_TEXT_PAK_SHA256 =
            "CE8FA5ADE2E69525F781B8DFF5681229FD4CFE8AC27496574E8F281182227439";

    static final int KAMYLA_MAP_ID = 481;
    static final int FRANCU_MAP_ID = 482;
    static final int FRANCU_RETURN_ROUTE_ID = 1160;
    static final String FRANCU_RETURN_SOURCE_SHA256 =
            "E7962A953E4212303EBEA814C984476A692D410E97048CD1720BEC3747101BF0";

    static final String RETAINED_ACTION_SOURCE_SHA256 =
            "3A73E38D0D55A80BB9BD87FA2999A0929E295BC63E30E6C0F6BE5AFC9AC9BA72";
    static final String RETAINED_DIALOGUE_SOURCE_SHA256 =
            "357612978AACEFB49412D046C78104E4CE166A63A68F5BCFBB495A0DF03E96AA";

    private static final Map<Integer, ClassicWorldDropCatalog.ItemWire> EXTRA_ITEMS = Map.of(
            MAZE_MAP, item(MAZE_MAP, "$4020", 1138, 3963, false, 1_000, 1870),
            CRUMPLED_LOVE_LETTER,
            item(CRUMPLED_LOVE_LETTER, "$3787", 1867, 4755,
                    false, 1_000, 1804));

    private static final Map<String, Integer> FINAL_REWARDS = Map.of(
            "g", SWORD_OF_HONOR,
            "h", EDORYU_OF_ABYSS,
            "i", CROSSBOW_OF_SILENCE,
            "j", JET_BLACK_CRYSTAL_BALL,
            "k", SHIELD_OF_FAITHFULNESS);

    private static final Map<Integer, InventoryCatalog.Definition> REWARD_ITEMS = Map.of(
            SWORD_OF_HONOR, weapon(SWORD_OF_HONOR, "$3790", 1871, 4759,
                    1807, 50_000, BattleActionCodes.SWORD_WALK,
                    9, 12, "iron", 0x22, 6, 0, 2),
            EDORYU_OF_ABYSS, weapon(EDORYU_OF_ABYSS, "$3791", 1875, 4763,
                    1808, 20_000, BattleActionCodes.EDORYU_WALK,
                    14, 10, "steel", 0x10, 6, 0, 0),
            CROSSBOW_OF_SILENCE, weapon(CROSSBOW_OF_SILENCE, "$3792", 1873, 4761,
                    1809, 50_000, BattleActionCodes.BOW_WALK,
                    3, 2, "wood", 0x44, 6, 1, 1),
            JET_BLACK_CRYSTAL_BALL, weapon(JET_BLACK_CRYSTAL_BALL, "$3793", 1878, 773,
                    1810, 15_000, BattleActionCodes.STAFF_WALK,
                    1, 1, "steel", 0x48, 6, 0, 0),
            SHIELD_OF_FAITHFULNESS, shieldOfFaithfulness());

    record ActionResult(String html, int nextStep, int consumedItemId,
                        int givenItemId, Protocol.PortalDestination destination,
                        int heading, boolean retainedSourceGap) {
        ActionResult {
            html = html == null ? "" : html;
            if (nextStep < 0 || consumedItemId < 0 || givenItemId < 0
                    || heading < 0 || heading > 7) {
                throw new IllegalArgumentException("invalid Kamyla quest action");
            }
        }

        boolean mutatesState() {
            return nextStep > 0 || consumedItemId > 0 || givenItemId > 0;
        }

        boolean teleports() { return destination != null; }
    }

    static boolean isDialogueNpc(ClassicWorldNpcCatalog.StaticNpc npc) {
        return npc != null && (npc.mapId() == KAMYLA_MAP_ID
                && npc.npcId() == ClassicWorldPirateBackHalfCatalog.KAMYLA_NPC_ID
                || npc.mapId() == FRANCU_MAP_ID
                && npc.npcId() == ClassicWorldPirateBackHalfCatalog.FRANCU_NPC_ID);
    }

    static String dialogue(int npcId, Map<Integer, Integer> questSteps,
                           Map<Long, Integer> inventory) {
        if (questSteps == null || inventory == null) {
            throw new IllegalArgumentException("Kamyla dialogue state is required");
        }
        int step = questSteps.getOrDefault(QUEST_ID, 0);
        if (npcId == ClassicWorldPirateBackHalfCatalog.KAMYLA_NPC_ID) {
            if (step == QUEST_END) return "kamyla26";
            if (step == 4 && has(inventory, CRUMPLED_LOVE_LETTER)) return "kamyla15";
            if (step == 4) return "kamyla14";
            if (step == 3 && has(inventory, DIEGO_DIARY)) return "kamyla12";
            if (step == 3) return "kamyla11";
            if (step == 2 && has(inventory, MAZE_MAP)) return "kamyla9";
            if (step == 1) return "kamyla8";
            if (questSteps.getOrDefault(CADMUS_QUEST_ID, 0) == QUEST_END
                    && has(inventory, DRAKE_KEY)) return "kamyla1";
            return "kamyla000";
        }
        if (npcId == ClassicWorldPirateBackHalfCatalog.FRANCU_NPC_ID) {
            return step == 2 ? "francu12" : "francu1";
        }
        return null;
    }

    static ActionResult action(int npcId, String action,
                               Map<Integer, Integer> questSteps,
                               Map<Long, Integer> inventory) {
        if (action == null || questSteps == null || inventory == null) return null;
        int step = questSteps.getOrDefault(QUEST_ID, 0);
        if (npcId == ClassicWorldPirateBackHalfCatalog.KAMYLA_NPC_ID) {
            return switch (action) {
                case "a" -> step == 0
                        && questSteps.getOrDefault(CADMUS_QUEST_ID, 0) == QUEST_END
                        && has(inventory, DRAKE_KEY)
                        ? new ActionResult("kamyla7", 1, 0, 0, null, 5, false)
                        : null;
                case "b" -> step == 1
                        ? portal(482, 32679, 32742, 5) : null;
                case "c" -> step == 2 && has(inventory, MAZE_MAP)
                        ? new ActionResult("kamyla10", 3, MAZE_MAP,
                                0, null, 5, false) : null;
                case "d" -> step == 3
                        ? portal(483, 32736, 32800, 5) : null;
                case "e" -> step == 3 && has(inventory, DIEGO_DIARY)
                        ? new ActionResult("kamyla13", 4, DIEGO_DIARY,
                                0, null, 5, false) : null;
                case "f" -> step == 4
                        ? portal(484, 32746, 32807, 5) : null;
                case "g", "h", "i", "j", "k" ->
                        step == 4 && has(inventory, CRUMPLED_LOVE_LETTER)
                                ? new ActionResult("kamyla25", QUEST_END,
                                        CRUMPLED_LOVE_LETTER,
                                        FINAL_REWARDS.get(action), null, 5, false)
                                : null;
                default -> null;
            };
        }
        if (npcId == ClassicWorldPirateBackHalfCatalog.FRANCU_NPC_ID) {
            return switch (action) {
                case "a" -> step == 1 ? new ActionResult(
                        "francu10", 2, 0, MAZE_MAP, null, 5, false) : null;
                case "b" -> step == 2 ? portal(481, 32735, 32845, 5) : null;
                default -> null;
            };
        }
        return null;
    }

    static InventoryCatalog.Definition inventoryDefinition(int itemId) {
        InventoryCatalog.Definition reward = REWARD_ITEMS.get(itemId);
        if (reward != null) return reward;
        ClassicWorldDropCatalog.ItemWire item = EXTRA_ITEMS.get(itemId);
        return item == null ? null : item.inventoryDefinition();
    }

    static Map<String, Integer> finalRewards() {
        return FINAL_REWARDS;
    }

    private static ActionResult portal(int mapId, int x, int y, int heading) {
        return new ActionResult("", 0, 0, 0,
                new Protocol.PortalDestination(mapId, x, y, true),
                heading, false);
    }

    private static boolean has(Map<Long, Integer> inventory, int itemId) {
        return inventory.getOrDefault((long) itemId, 0) > 0;
    }

    private static ClassicWorldDropCatalog.ItemWire item(
            int itemId, String nameId, int inventoryGfx, int groundGfx,
            boolean tradable, int unitWeight, int itemDescriptionId) {
        return new ClassicWorldDropCatalog.ItemWire(
                itemId, "etc", nameId, "normal", inventoryGfx, groundGfx,
                0, false, tradable, unitWeight, itemDescriptionId,
                "", 0, 0, "paper", 0, 0x7F);
    }

    private static InventoryCatalog.Definition weapon(
            int itemId, String nameId, int inventoryGfx, int groundGfx,
            int itemDescriptionId, int unitWeight, int walkAction,
            int damageSmall, int damageLarge, String material, int classMask,
            int safeEnchant, int hitModifier, int damageModifier) {
        return new InventoryCatalog.Definition(
                itemId, 1, inventoryGfx, groundGfx, 0, false,
                nameId, nameId, 0, 0, false, true, walkAction, 0,
                InventoryCatalog.Effect.NONE, itemDescriptionId, unitWeight, 0,
                new InventoryCatalog.NativeWeaponStatus(
                        damageSmall, damageLarge,
                        ClassicWorldDropCatalog.materialCode(material),
                        unitWeight, classMask, safeEnchant,
                        hitModifier, damageModifier));
    }

    private static InventoryCatalog.Definition shieldOfFaithfulness() {
        InventoryCatalog.NativeArmorStatus status =
                new InventoryCatalog.NativeArmorStatus(
                        3, ClassicWorldDropCatalog.materialCode("iron"),
                        50_000, 0x01, 6,
                        0, 0, 0, 0, 0, 2, 0, 0, 0, 0, false,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7);
        return new InventoryCatalog.Definition(
                SHIELD_OF_FAITHFULNESS, 25, 1874, 23, 0, false,
                "$3794", "$3794", 0, 0, false, true,
                BattleActionCodes.WALK, 0, InventoryCatalog.Effect.NONE,
                1811, 50_000, 0, null, status);
    }

    private ClassicWorldKamylaQuestCatalog() {}
}
