// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeVisibilityAccess;
import local.lito381.server.NativeDeVisibilityScope;
import local.lito381.server.NativeDeWorldLog;

public class NativeDeVisibilityCheck {
    private static final NativeDeWorldLog _log = NativeDeVisibilityScope.Logs.getLogger(NativeDeVisibilityCheck.class);

    public static boolean check(NativeDeDeletionPorts.Viewer viewer) {
        try {
            if (viewer == null) {
                return false;
            }
            if (NativeDeVisibilityAccess.L1PcInstance$getOnlineStatus(viewer) == 0) {
                return false;
            }
            if (NativeDeVisibilityAccess.L1PcInstance$getNetConnection(viewer) == null) {
                return false;
            }
            if (NativeDeVisibilityAccess.L1PcInstance$isTeleport(viewer)) {
                return false;
            }
        }
        catch (Exception exception) {
            NativeDeVisibilityScope.log().error(exception.getLocalizedMessage(), exception);
            return false;
        }
        return true;
    }
}
