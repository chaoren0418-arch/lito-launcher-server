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

    static void showChanged(NativeDeObjectFields nativeDeObjectFields, int n) {
        NativeDeObjectFields nativeDeObjectFields2;
        NativeDeWorldContext.Entry entry;
        if (nativeDeObjectFields instanceof NativeDeObjectFields && (entry = (NativeDeWorldContext.Entry)CombatAccess.field(nativeDeObjectFields2 = nativeDeObjectFields, "sourceDeWorldEntry")) != null) {
            entry.showId = n;
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
