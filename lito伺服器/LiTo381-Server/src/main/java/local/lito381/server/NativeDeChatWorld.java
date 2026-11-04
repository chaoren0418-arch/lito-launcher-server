// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Collection;
import java.util.function.Consumer;
import local.lito381.server.NativeDeChatAccess;
import local.lito381.server.NativeDeChatScope;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeVisibilityPacket;
import local.lito381.server.NativeDeWorldContext;
import local.lito381.server.NativeDeWorldPlayerFilter;

final class NativeDeChatWorld {
    NativeDeChatWorld() {
    }

    static NativeDeChatWorld get() {
        return new NativeDeChatWorld();
    }

    NativeDeWorldContext ctx() {
        return NativeDeChatScope.owner().context;
    }

    Collection<NativeDeDeletionPorts.Viewer> getAllPlayers() {
        return (Collection)this.ctx().call("getAllPlayers", "()Ljava/util/Collection;", new Object[0]);
    }

    void forEachNearbyPlayer(NativeDeDeletionPorts.ObjectRef objectRef, int n, NativeDeWorldPlayerFilter nativeDeWorldPlayerFilter, Consumer<NativeDeDeletionPorts.Viewer> consumer) {
        this.ctx().require(objectRef);
        this.ctx().call("forEachNearbyPlayer", "(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;ILlocal/lito381/server/NativeDeWorldPlayerFilter;Ljava/util/function/Consumer;)V", new Object[]{objectRef, n, nativeDeWorldPlayerFilter, consumer});
    }

    public void broadcastPacketToAll(NativeDeVisibilityPacket nativeDeVisibilityPacket) {
        for (NativeDeDeletionPorts.Viewer viewer : this.getAllPlayers()) {
            if (NativeDeChatAccess.L1PcInstance$getNetConnection(viewer) != null) {
                NativeDeChatAccess.L1PcInstance$sendPackets(viewer, nativeDeVisibilityPacket);
                continue;
            }
            if (NativeDeChatAccess.L1PcInstance$getOnlineStatus(viewer) == 2 || NativeDeChatAccess.L1PcInstance$getOnlineStatus(viewer) == 3) continue;
            NativeDeChatAccess.L1PcInstance$sendPackets(viewer, nativeDeVisibilityPacket);
        }
    }
}
