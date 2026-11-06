// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import local.lito381.server.CombatAccess;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeObjectFields;
import local.lito381.server.NativeDeWorldContext;
import local.lito381.server.NativeDeWorldLocation;

final class NativeDeObjectBindings {
    NativeDeObjectBindings() {
    }

    static NativeDeWorldLocation attachLocation(NativeDeDeletionPorts.ObjectRef objectRef, NativeDeWorldContext nativeDeWorldContext, int n, int n2, int n3, int n4) {
        NativeDeObjectFields nativeDeObjectFields = (NativeDeObjectFields)objectRef;
        NativeDeWorldLocation nativeDeWorldLocation = nativeDeObjectFields.getLocation();
        nativeDeObjectFields.setLocation(new NativeDeWorldLocation(n, n2, nativeDeWorldContext.map(n3)));
        nativeDeObjectFields.set_showId(n4);
        return nativeDeWorldLocation;
    }

    static void showChanged(NativeDeObjectFields actor, int showId) {
        if (actor instanceof NativeDeObjectFields) {
            NativeDeObjectFields object = (NativeDeObjectFields)(Object)actor;
            NativeDeWorldContext.Entry entry = (NativeDeWorldContext.Entry)CombatAccess.field(object, "sourceDeWorldEntry");
            if (entry != null) {
                entry.showId = showId;
            }
        }
    }

    static int x(NativeDeObjectFields nativeDeObjectFields) {
        return nativeDeObjectFields.getLocation().getX();
    }

    static int y(NativeDeObjectFields nativeDeObjectFields) {
        return nativeDeObjectFields.getLocation().getY();
    }

    static short mapId(NativeDeObjectFields nativeDeObjectFields) {
        return (short)nativeDeObjectFields.getLocation().getMapId();
    }
}
