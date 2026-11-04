// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeWorldAccess;

public enum NativeDeWorldPlayerFilter {
    NONE,
    BROADCAST,
    ACTIVE_ONLY;


    public boolean accepts(NativeDeDeletionPorts.Viewer pc) {
        if (pc == null) {
            return false;
        }
        return switch (this) {
            default -> throw new IncompatibleClassChangeError();
            case NONE -> true;
            case BROADCAST -> {
                if (NativeDeWorldAccess.L1PcInstance$getNetConnection(pc) != null || NativeDeWorldAccess.L1PcInstance$getOnlineStatus(pc) != 2 && NativeDeWorldAccess.L1PcInstance$getOnlineStatus(pc) != 3) {
                    yield true;
                }
                yield false;
            }
            case ACTIVE_ONLY -> NativeDeWorldAccess.L1PcInstance$getOnlineStatus(pc) != 2 && NativeDeWorldAccess.L1PcInstance$getOnlineStatus(pc) != 3;
        };
    }
}

