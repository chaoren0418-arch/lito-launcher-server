// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeKnownState;

final class NativeDePerceptionAccess {
    NativeDePerceptionAccess() {
    }

    static boolean Pc_tryAddKnownObject(NativeDeDeletionPorts.Viewer viewer, NativeDeDeletionPorts.ObjectRef objectRef) {
        NativeDeKnownState nativeDeKnownState;
        if (viewer instanceof NativeDeKnownState) {
            nativeDeKnownState = (NativeDeKnownState)viewer;
        } else {
            throw new IllegalArgumentException("perception requires a complete PC known-state endpoint");
        }
        return nativeDeKnownState.tryAddKnownObject(objectRef);
    }
}
