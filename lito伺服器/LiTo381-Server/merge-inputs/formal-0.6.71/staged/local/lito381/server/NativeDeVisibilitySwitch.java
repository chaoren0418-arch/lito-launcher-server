// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

class NativeDeVisibilitySwitch {
    static final int[] $SwitchMap$com$lineage$server$model$Instance$DeathLifecycleGate$Phase;
    static final int[] $SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$RepairKind;

    static {
        $SwitchMap$com$lineage$server$model$Instance$DeathLifecycleGate$Phase = new int[NativeDeathLifecycleGate.Phase.values().length];
        try {
            $SwitchMap$com$lineage$server$model$Instance$DeathLifecycleGate$Phase[NativeDeathLifecycleGate.Phase.ACTIVE.ordinal()] = 1;
        }
        catch (NoSuchFieldError noSuchFieldError) {
        }
        try {
            $SwitchMap$com$lineage$server$model$Instance$DeathLifecycleGate$Phase[NativeDeathLifecycleGate.Phase.RESURRECTING.ordinal()] = 2;
        }
        catch (NoSuchFieldError noSuchFieldError) {
        }
        try {
            $SwitchMap$com$lineage$server$model$Instance$DeathLifecycleGate$Phase[NativeDeathLifecycleGate.Phase.DEAD_PENDING.ordinal()] = 3;
        }
        catch (NoSuchFieldError noSuchFieldError) {
        }
        try {
            $SwitchMap$com$lineage$server$model$Instance$DeathLifecycleGate$Phase[NativeDeathLifecycleGate.Phase.DEAD_SETTLED.ordinal()] = 4;
        }
        catch (NoSuchFieldError noSuchFieldError) {
        }
        try {
            $SwitchMap$com$lineage$server$model$Instance$DeathLifecycleGate$Phase[NativeDeathLifecycleGate.Phase.DEATH_DROPPED.ordinal()] = 5;
        }
        catch (NoSuchFieldError noSuchFieldError) {
        }
        try {
            $SwitchMap$com$lineage$server$model$Instance$DeathLifecycleGate$Phase[NativeDeathLifecycleGate.Phase.FINALIZING.ordinal()] = 6;
        }
        catch (NoSuchFieldError noSuchFieldError) {
        }
        try {
            $SwitchMap$com$lineage$server$model$Instance$DeathLifecycleGate$Phase[NativeDeathLifecycleGate.Phase.TERMINATED.ordinal()] = 7;
        }
        catch (NoSuchFieldError noSuchFieldError) {
        }
        $SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$RepairKind = new int[NativeDeDeletionVisibility.RepairKind.values().length];
        try {
            $SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$RepairKind[NativeDeDeletionVisibility.RepairKind.NORMAL_RACE.ordinal()] = 1;
        }
        catch (NoSuchFieldError noSuchFieldError) {
        }
        try {
            $SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$RepairKind[NativeDeDeletionVisibility.RepairKind.STALL.ordinal()] = 2;
        }
        catch (NoSuchFieldError noSuchFieldError) {
        }
        try {
            $SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$RepairKind[NativeDeDeletionVisibility.RepairKind.LEAK.ordinal()] = 3;
        }
        catch (NoSuchFieldError noSuchFieldError) {
        }
    }
}
