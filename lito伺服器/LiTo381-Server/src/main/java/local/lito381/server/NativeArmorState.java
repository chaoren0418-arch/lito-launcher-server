// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.ClientSession;
import local.lito381.server.NativeLightAction;

final class NativeArmorState {
    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean activate(ClientSession clientSession, long l, int n) {
        NativeLightAction.enterState(clientSession);
        try {
            boolean bl = clientSession.nativeArmorStateActivate(l, n);
            return bl;
        }
        finally {
            NativeLightAction.exitState(clientSession);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean active(ClientSession clientSession, long l) {
        NativeLightAction.enterState(clientSession);
        try {
            boolean bl = clientSession.nativeArmorStateActive(l);
            return bl;
        }
        finally {
            NativeLightAction.exitState(clientSession);
        }
    }

    static boolean cancel(ClientSession clientSession) {
        NativeLightAction.enterState(clientSession);
        try {
            boolean bl = clientSession.nativeArmorStateCancel();
            return bl;
        }
        finally {
            NativeLightAction.exitState(clientSession);
        }
    }

    private NativeArmorState() {
    }
}
