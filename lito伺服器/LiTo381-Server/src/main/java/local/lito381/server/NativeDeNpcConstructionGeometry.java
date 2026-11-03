// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeWorldContext;
import local.lito381.server.NativeDeWorldLocation;

final class NativeDeNpcConstructionGeometry {
    NativeDeNpcConstructionGeometry() {
    }

    static NativeDeWorldLocation location(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeWorldContext.entry((NativeDeDeletionPorts.ObjectRef)objectRef).location;
    }

    static short mapId(NativeDeDeletionPorts.ObjectRef objectRef) {
        return (short)NativeDeNpcConstructionGeometry.location(objectRef).getMapId();
    }

    static int showId(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeWorldContext.entry((NativeDeDeletionPorts.ObjectRef)objectRef).showId;
    }
}
