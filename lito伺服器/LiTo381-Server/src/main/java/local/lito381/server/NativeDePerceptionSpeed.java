// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.NativeDePerceptionState;

public final class NativeDePerceptionSpeed {
    private NativeDePerceptionSpeed() {
    }

    public static int calculate(NativeDePerceptionState actor, int baseInterval, ActionType actionType) {
        if (actor == null) {
            return Math.max(1, baseInterval);
        }
        return NativeDePerceptionSpeed.calculate(baseInterval, actionType, NativeDePerceptionSpeed.stateOf(actor));
    }

    public static SpeedState stateOf(NativeDePerceptionState actor) {
        int moveSpeed = actor.getMoveSpeed();
        return new SpeedState(moveSpeed == 1 || actor.hasSkillEffect(1001) || actor.hasSkillEffect(43) || actor.hasSkillEffect(54), moveSpeed == 2, actor.hasSkillEffect(52) || actor.hasSkillEffect(101) || actor.hasSkillEffect(150) || actor.hasSkillEffect(1017), actor.hasSkillEffect(186), actor.getBraveSpeed() == 1 || actor.hasSkillEffect(1000), actor.hasSkillEffect(998), actor.hasSkillEffect(1016), actor.hasSkillEffect(167));
    }

    public static int calculate(int baseInterval, ActionType actionType, SpeedState state) {
        ActionType type;
        int interval = Math.max(1, baseInterval);
        ActionType actionType2 = type = actionType != null ? actionType : ActionType.SPELL;
        if (state.slowed()) {
            interval = NativeDePerceptionSpeed.multiply(interval, 2.0);
        } else if (state.haste()) {
            interval = NativeDePerceptionSpeed.multiply(interval, 0.755);
        }
        if (type == ActionType.MOVE && state.fastMovable()) {
            interval = NativeDePerceptionSpeed.multiply(interval, 0.665);
        }
        if (type == ActionType.ATTACK && state.fastAttackable()) {
            interval = NativeDePerceptionSpeed.multiply(interval, 0.775);
        }
        if (state.brave()) {
            interval = NativeDePerceptionSpeed.multiply(interval, 0.755);
        }
        if (state.braveX()) {
            interval = NativeDePerceptionSpeed.multiply(interval, 0.755);
        }
        if (state.elfBrave()) {
            interval = NativeDePerceptionSpeed.multiply(interval, 0.855);
            if (type == ActionType.ATTACK) {
                interval = NativeDePerceptionSpeed.multiply(interval, 0.9);
            }
        }
        if (state.windShackle() && (type == ActionType.ATTACK || type == ActionType.SPELL)) {
            interval = NativeDePerceptionSpeed.multiply(interval, 1.25);
        }
        return Math.max(1, interval);
    }

    private static int multiply(int interval, double multiplier) {
        return (int)((double)interval * multiplier);
    }

    public record SpeedState(boolean haste, boolean slowed, boolean fastMovable, boolean fastAttackable, boolean brave, boolean braveX, boolean elfBrave, boolean windShackle) {
    }

    public static enum ActionType {
        MOVE,
        ATTACK,
        SPELL;

    }
}
