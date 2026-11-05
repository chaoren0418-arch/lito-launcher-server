// DECOMPILED from base-0.6.71.jar, TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.function.Consumer;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeInstanceScope;
import local.lito381.server.NativeDeObjectFields;
import local.lito381.server.NativeDePerceptionPacket;
import local.lito381.server.NativeDeWorldPlayerFilter;

public final class NativeDeInstanceBroadcast {
    private NativeDeInstanceBroadcast() {
    }

    public static void toVisiblePlayers(NativeDeObjectFields nativeDeObjectFields, int n, NativeDePerceptionPacket nativeDePerceptionPacket) {
        if (nativeDeObjectFields == null || nativeDePerceptionPacket == null) {
            return;
        }
        try {
            NativeDeInstanceBroadcast.forEachVisiblePlayer(nativeDeObjectFields, n, NativeDeWorldPlayerFilter.BROADCAST, viewer -> NativeDeInstanceScope.send(viewer, nativeDePerceptionPacket));
        }
        catch (Exception exception) {
            NativeDeInstanceScope.log().error(exception.getLocalizedMessage(), exception);
        }
    }

    public static void forEachVisiblePlayer(NativeDeObjectFields nativeDeObjectFields, int n, NativeDeWorldPlayerFilter nativeDeWorldPlayerFilter, Consumer<NativeDeDeletionPorts.Viewer> consumer) {
        if (nativeDeObjectFields == null || consumer == null) {
            return;
        }
        NativeDeWorldPlayerFilter nativeDeWorldPlayerFilter2 = nativeDeWorldPlayerFilter != null ? nativeDeWorldPlayerFilter : NativeDeWorldPlayerFilter.BROADCAST;
        NativeDeInstanceScope.World.get().forEachNearbyPlayer(nativeDeObjectFields, n, nativeDeWorldPlayerFilter2, consumer);
    }
}
