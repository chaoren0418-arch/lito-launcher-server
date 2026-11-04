// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

public final class NativeSourceActionIntervalCalculator {
    private NativeSourceActionIntervalCalculator() {
    }

    public static int calculate(int n, ActionType actionType, SpeedState speedState) {
        ActionType actionType2;
        int n2 = Math.max(1, n);
        ActionType actionType3 = actionType2 = actionType != null ? actionType : ActionType.SPELL;
        if (speedState.slowed()) {
            n2 = NativeSourceActionIntervalCalculator.multiply(n2, 2.0);
        } else if (speedState.haste()) {
            n2 = NativeSourceActionIntervalCalculator.multiply(n2, 0.755);
        }
        if (actionType2 == ActionType.MOVE && speedState.fastMovable()) {
            n2 = NativeSourceActionIntervalCalculator.multiply(n2, 0.665);
        }
        if (actionType2 == ActionType.ATTACK && speedState.fastAttackable()) {
            n2 = NativeSourceActionIntervalCalculator.multiply(n2, 0.775);
        }
        if (speedState.brave()) {
            n2 = NativeSourceActionIntervalCalculator.multiply(n2, 0.755);
        }
        if (speedState.braveX()) {
            n2 = NativeSourceActionIntervalCalculator.multiply(n2, 0.755);
        }
        if (speedState.elfBrave()) {
            n2 = NativeSourceActionIntervalCalculator.multiply(n2, 0.855);
            if (actionType2 == ActionType.ATTACK) {
                n2 = NativeSourceActionIntervalCalculator.multiply(n2, 0.9);
            }
        }
        if (speedState.windShackle() && (actionType2 == ActionType.ATTACK || actionType2 == ActionType.SPELL)) {
            n2 = NativeSourceActionIntervalCalculator.multiply(n2, 1.25);
        }
        return Math.max(1, n2);
    }

    private static int multiply(int n, double d) {
        return (int)((double)n * d);
    }

    public record SpeedState(boolean haste, boolean slowed, boolean fastMovable, boolean fastAttackable, boolean brave, boolean braveX, boolean elfBrave, boolean windShackle) {
    }

    public static enum ActionType {
        MOVE,
        ATTACK,
        SPELL;

    }
}
