package local.lito381.server;

import java.util.List;
import java.util.Map;

/** Exact retained C_NPCAction constants for the Desire Cave NPC family. */
final class ClassicWorldDesireCaveActionCatalog {
    static final int QUEST_DESIRE = 36;
    static final int QUEST_END = 255;
    static final int DESIRE_SPY_GFX = 6034;
    static final int DESIRE_SPY_MORPH_SECONDS = 1_800;
    static final int DESIRE_SPY_MORPH_ADENA = 100;

    static final int DE_LV30 = 4003;
    static final int STATUS_CURSE_BARLOG = 4005;
    static final int STATUS_CURSE_YAHEE = 4006;
    static final int CKEW_LV50 = 4007;
    static final int YAHEE_CURSE_SECONDS = 1_020;
    static final int YAHEE_CURSE_EFFECT_GFX = 7247;

    record ItemAmount(long itemId, int count) {
        ItemAmount {
            if (itemId <= 0 || count <= 0) {
                throw new IllegalArgumentException("invalid Desire Cave item amount");
            }
        }
    }

    record Door(
            int npcId, long protectionId, long sealId, long rulerId,
            int destinationMapId, int destinationX, int destinationY,
            int destinationHeading) {}

    record Craft(
            String action, int minimumKarmaLevel, List<ItemAmount> materials,
            long resultItemId, String failureHtml) {
        Craft {
            materials = List.copyOf(materials);
            if (action == null || action.length() != 1 || minimumKarmaLevel < 1
                    || minimumKarmaLevel > 8 || materials.isEmpty()
                    || resultItemId <= 0 || failureHtml == null
                    || failureHtml.isBlank()) {
                throw new IllegalArgumentException("invalid Desire Cave craft");
            }
        }
    }

    static final Map<Integer, Door> DOORS = Map.of(
            80059, new Door(80059, 40909, 40913, 40917,
                    607, 32773, 32835, 5),
            80060, new Door(80060, 40912, 40916, 40920,
                    606, 32757, 32842, 5),
            80061, new Door(80061, 40910, 40914, 40918,
                    604, 32830, 32822, 5),
            80062, new Door(80062, 40911, 40915, 40919,
                    605, 32835, 32822, 5));

    static final Map<String, Craft> BARLOG_CRAFTS = Map.ofEntries(
            craft("a", 1, 20083, "lsmithaa", item(20158, 1),
                    item(40669, 50), item(40678, 100)),
            craft("b", 2, 20131, "lsmithbb", item(20144, 1),
                    item(40672, 50), item(40678, 100)),
            craft("c", 3, 20069, "lsmithcc", item(20075, 1),
                    item(40671, 50), item(40678, 100)),
            craft("d", 4, 20179, "lsmithdd", item(20183, 1),
                    item(40674, 20), item(40678, 100)),
            craft("e", 5, 20209, "lsmithee", item(20190, 1),
                    item(40674, 40), item(40678, 100)),
            craft("f", 6, 20290, "lsmithff", item(20078, 1),
                    item(40674, 5), item(40678, 100)),
            craft("g", 7, 20261, "lsmithgg", item(20078, 1),
                    item(40670, 1), item(40678, 100)),
            craft("h", 8, 20031, "lsmithhh", item(40719, 1),
                    item(40673, 1), item(40678, 100)));

    private ClassicWorldDesireCaveActionCatalog() {}

    static Door door(int npcId) {
        return DOORS.get(npcId);
    }

    static Craft craft(String action) {
        return action == null ? null : BARLOG_CRAFTS.get(action.toLowerCase());
    }

    static long earringForAction(String action) {
        if (action == null || action.length() != 1
                || action.charAt(0) < '1' || action.charAt(0) > '8') return 0;
        return 21019L + (action.charAt(0) - '0');
    }

    static int earringLevel(long itemId) {
        return itemId >= 21020 && itemId <= 21027 ? (int) itemId - 21019 : 0;
    }

    static String smithSelectionHtml(String action) {
        return switch (action == null ? "" : action) {
            case "0" -> "lsmitha";
            case "1" -> "lsmithb";
            case "2" -> "lsmithc";
            case "3" -> "lsmithd";
            case "4" -> "lsmithe";
            case "5" -> "lsmithf";
            case "7" -> "lsmithg";
            case "8" -> "lsmithh";
            default -> null;
        };
    }

    static int soulCrystalCount(String action) {
        return switch (action == null ? "" : action) {
            case "1" -> 1;
            case "2" -> 10;
            case "3" -> 100;
            default -> 0;
        };
    }

    static int soulCrystalKarmaCost(String action) {
        return switch (soulCrystalCount(action)) {
            case 1 -> 500;
            case 10 -> 5_000;
            case 100 -> 50_000;
            default -> 0;
        };
    }

    private static ItemAmount item(long itemId, int count) {
        return new ItemAmount(itemId, count);
    }

    private static Map.Entry<String, Craft> craft(
            String action, int minimumKarmaLevel, long resultItemId,
            String failureHtml, ItemAmount... materials) {
        return Map.entry(action, new Craft(action, minimumKarmaLevel,
                List.of(materials), resultItemId, failureHtml));
    }
}
