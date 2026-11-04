// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.function.Consumer;
import local.lito381.server.NativeDeChatAccess;
import local.lito381.server.NativeDeChatScope;
import local.lito381.server.NativeDeChatWorld;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeVisibilityPacket;
import local.lito381.server.NativeDeWorldPlayerFilter;

public final class NativeDeChatBroadcast {
    public static void toVisiblePlayers(NativeDeDeletionPorts.ObjectRef objectRef, int n, NativeDeVisibilityPacket nativeDeVisibilityPacket) {
        if (objectRef == null || nativeDeVisibilityPacket == null) {
            return;
        }
        try {
            NativeDeChatBroadcast.forEachVisiblePlayer(objectRef, n, NativeDeWorldPlayerFilter.BROADCAST, viewer -> NativeDeChatAccess.L1PcInstance$sendPackets(viewer, nativeDeVisibilityPacket));
        }
        catch (Exception exception) {
            NativeDeChatScope.log().error(exception.getLocalizedMessage(), exception);
        }
    }

    public static void forEachVisiblePlayer(NativeDeDeletionPorts.ObjectRef objectRef, int n, NativeDeWorldPlayerFilter nativeDeWorldPlayerFilter, Consumer<NativeDeDeletionPorts.Viewer> consumer) {
        if (objectRef == null || consumer == null) {
            return;
        }
        NativeDeWorldPlayerFilter nativeDeWorldPlayerFilter2 = nativeDeWorldPlayerFilter != null ? nativeDeWorldPlayerFilter : NativeDeWorldPlayerFilter.BROADCAST;
        NativeDeChatWorld.get().forEachNearbyPlayer(objectRef, n, nativeDeWorldPlayerFilter2, consumer);
    }
}
