// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeKnownState;

final class NativeDePerceptionAccess {
    NativeDePerceptionAccess() {
    }

    static boolean Pc_tryAddKnownObject(NativeDeDeletionPorts.Viewer viewer, NativeDeDeletionPorts.ObjectRef objectRef) {
        if (!(viewer instanceof NativeDeKnownState)) {
            throw new IllegalArgumentException("perception requires a complete PC known-state endpoint");
        }
        NativeDeKnownState nativeDeKnownState = (NativeDeKnownState)viewer;
        return nativeDeKnownState.tryAddKnownObject(objectRef);
    }
}
