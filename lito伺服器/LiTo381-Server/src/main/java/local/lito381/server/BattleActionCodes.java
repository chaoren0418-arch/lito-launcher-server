// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Set;

final class BattleActionCodes {
    static final int WALK = 0;
    static final int ATTACK = 1;
    static final int DAMAGE = 2;
    static final int IDLE = 3;
    static final int APPEAR = 4;
    static final int SWORD_WALK = 4;
    static final int SWORD_ATTACK = 5;
    static final int AXE_WALK = 11;
    static final int HIDE = 11;
    static final int AXE_ATTACK = 12;
    static final int DIE = 8;
    static final int PICKUP = 15;
    static final int WAND = 17;
    static final int SPELL_CAST = 19;
    static final int BOW_WALK = 20;
    static final int BOW_ATTACK = 21;
    static final int SPEAR_WALK = 24;
    static final int SPEAR_ATTACK = 25;
    static final int STAFF_WALK = 40;
    static final int STAFF_ATTACK = 41;
    static final int MOVE_UP = 44;
    static final int MOVE_DOWN = 45;
    static final int DAGGER_WALK = 46;
    static final int DAGGER_ATTACK = 47;
    static final int TWO_HAND_SWORD_WALK = 50;
    static final int TWO_HAND_SWORD_ATTACK = 51;
    static final int EDORYU_WALK = 54;
    static final int EDORYU_ATTACK = 55;
    static final int CLAW_WALK = 58;
    static final int CLAW_ATTACK = 59;
    static final int THROWING_KNIFE_WALK = 62;
    static final int THROWING_KNIFE_ATTACK = 63;
    static final int CHAINSWORD_WALK = 83;
    static final int CHAINSWORD_ATTACK = 84;
    static final int DOOR_CRACK_1 = 32;
    static final int DOOR_CRACK_2 = 33;
    static final int DOOR_CRACK_3 = 34;
    static final int DOOR_CRACK_4 = 35;
    static final int DOOR_CRACK_5 = 36;
    static final int DOOR_DIE = 37;
    private static final Set<Integer> WALK_ACTIONS = Set.of(0, 4, 11, 20, 24, 40, 46, 50, 54, 58, 62, 83);

    private BattleActionCodes() {
    }

    static boolean isWalkAction(int actionId) {
        return WALK_ACTIONS.contains(actionId);
    }

    static int attackForWalk(int walkAction) {
        return switch (walkAction) {
            case 0 -> 1;
            case 4 -> 5;
            case 11 -> 12;
            case 20 -> 21;
            case 24 -> 25;
            case 40 -> 41;
            case 46 -> 47;
            case 50 -> 51;
            case 54 -> 55;
            case 58 -> 59;
            case 62 -> 63;
            case 83 -> 84;
            default -> throw new IllegalArgumentException("No attack action is mapped for walk action " + walkAction);
        };
    }

    static boolean isUntimedCoreAction(int actionId) {
        return actionId == 2 || actionId == 3 || actionId == 8;
    }
}
