// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.NativeDeCharacterFields;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeObjectFields;

final class NativeDeInventoryIdentity {
    static void requireWorldIdentity(NativeDeDeletionPorts.ObjectRef objectRef) {
        if (objectRef instanceof NativeDeObjectFields) {
            NativeDeObjectFields nativeDeObjectFields = (NativeDeObjectFields)objectRef;
            if (!(objectRef instanceof NativeDeCharacterFields) && nativeDeObjectFields.getId() == 0) {
                throw new IllegalArgumentException("non-character source object has no World identity");
            }
        }
    }

    private NativeDeInventoryIdentity() {
    }
}
