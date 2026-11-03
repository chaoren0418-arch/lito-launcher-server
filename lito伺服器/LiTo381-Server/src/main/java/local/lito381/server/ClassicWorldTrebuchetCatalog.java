package local.lito381.server;

import java.util.Locale;
import java.util.function.IntUnaryOperator;

/** Exact retained {@code C_NPCAction} trebuchet actions and impact rectangles. */
final class ClassicWorldTrebuchetCatalog {
    static final String SOURCE_SHA256 =
            "3A73E38D0D55A80BB9BD87FA2999A0929E295BC63E30E6C0F6BE5AFC9AC9BA72";
    static final String TEXT_IDX_SHA256 =
            "B23C985CDAD807808E996FED31DBCB174D920B17EEE65025535EE92BF76FEA82";
    static final String TEXT_PAK_SHA256 =
            "CE8FA5ADE2E69525F781B8DFF5681229FD4CFE8AC27496574E8F281182227439";

    static final int NORMAL_SHELL_ITEM_ID = 46_281;
    static final int SILENCE_SHELL_ITEM_ID = 46_282;
    static final int DAMAGE = 300;
    static final int IMPACT_RADIUS = 4;
    static final int COOLDOWN_SECONDS = 10;
    static final int ATTACK_EFFECT_GFX = 12_205;
    static final int DEFENCE_EFFECT_GFX = 12_193;
    static final int SILENCE_EFFECT_GFX = 2_177;
    /** Exact selected 380_db.sql item name; simplified text is retained evidence. */
    static final String SOURCE_NORMAL_SHELL_NAME = "炮弹";
    /** CP950-lossless display form used by the selected traditional client. */
    static final String CLIENT_NORMAL_SHELL_NAME = "砲彈";
    /** Literal retained source text; it is not losslessly encodable in CP950. */
    static final String SOURCE_NO_AMMO_MESSAGE = "炮弹不足投石机无法使用.";
    /** Same message localized to the selected LiTo client's CP950 receiver. */
    static final String CLIENT_NO_AMMO_MESSAGE = "砲彈不足投石機無法使用.";

    record Shot(String action, int npcId, int x, int y, int effectGfx,
                boolean damaging, int consumedItemId) {
        Shot {
            if (action == null || action.isEmpty()
                    || npcId != ClassicWorldCastleWarField.ATTACK_TREBUCHET_NPC_ID
                        && npcId != ClassicWorldCastleWarField.DEFENCE_TREBUCHET_NPC_ID
                    || x <= 0 || y <= 0 || effectGfx <= 0
                    || consumedItemId != NORMAL_SHELL_ITEM_ID
                        && consumedItemId != SILENCE_SHELL_ITEM_ID) {
                throw new IllegalArgumentException("invalid native trebuchet shot");
            }
        }

        boolean impacts(int targetX, int targetY) {
            return targetX >= x - IMPACT_RADIUS && targetX <= x + IMPACT_RADIUS
                    && targetY >= y - IMPACT_RADIUS
                    && targetY <= y + IMPACT_RADIUS;
        }
    }

    private ClassicWorldTrebuchetCatalog() {}

    static String html(int npcId) {
        return switch (npcId) {
            case ClassicWorldCastleWarField.ATTACK_TREBUCHET_NPC_ID -> "corca";
            case ClassicWorldCastleWarField.DEFENCE_TREBUCHET_NPC_ID -> "corcd";
            default -> "";
        };
    }

    static Shot resolve(int npcId, String action, IntUnaryOperator randomBounded) {
        if (action == null || randomBounded == null) return null;
        String normalized = action.trim().toLowerCase(Locale.ROOT);
        return switch (npcId) {
            case ClassicWorldCastleWarField.ATTACK_TREBUCHET_NPC_ID ->
                switch (normalized) {
                    case "3" -> shot(normalized, npcId, randomBounded,
                            33_629, 6, 32_730, 4, true, NORMAL_SHELL_ITEM_ID);
                    case "4" -> shot(normalized, npcId, randomBounded,
                            33_629, 6, 32_675, 6, true, NORMAL_SHELL_ITEM_ID);
                    case "1-14" -> shot(normalized, npcId, randomBounded,
                            33_629, 6, 32_730, 4, false, SILENCE_SHELL_ITEM_ID);
                    case "1-15" -> shot(normalized, npcId, randomBounded,
                            33_629, 6, 32_675, 6, false, SILENCE_SHELL_ITEM_ID);
                    default -> null;
                };
            case ClassicWorldCastleWarField.DEFENCE_TREBUCHET_NPC_ID ->
                normalized.equals("9") ? shot(normalized, npcId, randomBounded,
                        33_629, 6, 32_735, 4, true, NORMAL_SHELL_ITEM_ID) : null;
            default -> null;
        };
    }

    private static Shot shot(
            String action, int npcId, IntUnaryOperator randomBounded,
            int baseX, int xBound, int baseY, int yBound,
            boolean damaging, int consumedItemId) {
        int xRoll = randomBounded.applyAsInt(xBound);
        int yRoll = randomBounded.applyAsInt(yBound);
        if (xRoll < 0 || xRoll >= xBound || yRoll < 0 || yRoll >= yBound) {
            throw new IllegalArgumentException("trebuchet random roll outside native bound");
        }
        return new Shot(action, npcId, baseX + xRoll, baseY + yRoll,
                npcId == ClassicWorldCastleWarField.ATTACK_TREBUCHET_NPC_ID
                    ? ATTACK_EFFECT_GFX : DEFENCE_EFFECT_GFX,
                damaging, consumedItemId);
    }
}
