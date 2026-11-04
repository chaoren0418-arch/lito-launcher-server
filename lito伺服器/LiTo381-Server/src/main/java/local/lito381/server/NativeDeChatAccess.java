// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.NativeDeCharacterFields;
import local.lito381.server.NativeDeChatBroadcast;
import local.lito381.server.NativeDeChatScope;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeNpcConstructionState;
import local.lito381.server.NativeDeNpcTemplate;
import local.lito381.server.NativeDeVisibilityPacket;
import local.lito381.server.NativeDeVisibilityRuntime;
import local.lito381.server.NativeDeWorldAccess;
import local.lito381.server.NativeDeWorldConnection;
import local.lito381.server.NativeDeWorldContext;

final class NativeDeChatAccess {
    static NativeDeNpcConstructionState npc(NativeDeDeletionPorts.ObjectRef objectRef) {
        return (NativeDeNpcConstructionState)objectRef;
    }

    static int L1NpcInstance$getId(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeChatAccess.npc(objectRef).getId();
    }

    static String L1NpcInstance$getNameId(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeChatAccess.npc(objectRef).getNameId();
    }

    static boolean L1NpcInstance$isDead(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeChatAccess.npc(objectRef).isDead();
    }

    static int L1NpcInstance$getHiddenStatus(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeChatAccess.npc(objectRef).getHiddenStatus();
    }

    static NativeDeNpcTemplate L1NpcInstance$getNpcTemplate(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeChatAccess.npc(objectRef).getNpcTemplate();
    }

    static void L1NpcInstance$broadcastPacketX8(NativeDeDeletionPorts.ObjectRef objectRef, NativeDeVisibilityPacket nativeDeVisibilityPacket) {
        NativeDeChatBroadcast.toVisiblePlayers(objectRef, 8, nativeDeVisibilityPacket);
    }

    static void L1NpcInstance$wideBroadcastPacket(NativeDeDeletionPorts.ObjectRef objectRef, NativeDeVisibilityPacket nativeDeVisibilityPacket) {
        NativeDeChatBroadcast.toVisiblePlayers(objectRef, 50, nativeDeVisibilityPacket);
    }

    static int L1PcInstance$getId(NativeDeDeletionPorts.ObjectRef objectRef) {
        return ((NativeDeCharacterFields)objectRef).getId();
    }

    static String L1PcInstance$getName(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeWorldAccess.L1PcInstance$getName(objectRef);
    }

    static NativeDeWorldConnection L1PcInstance$getNetConnection(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeVisibilityRuntime.connection((NativeDeDeletionPorts.Viewer)objectRef);
    }

    static int L1PcInstance$getOnlineStatus(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeVisibilityRuntime.online((NativeDeDeletionPorts.Viewer)objectRef);
    }

    static void L1PcInstance$sendPackets(NativeDeDeletionPorts.ObjectRef objectRef, NativeDeVisibilityPacket nativeDeVisibilityPacket) {
        NativeDeDeletionPorts.Viewer viewer = (NativeDeDeletionPorts.Viewer)objectRef;
        if (NativeDeWorldContext.entry((NativeDeDeletionPorts.ObjectRef)viewer).context != NativeDeChatScope.owner().context) {
            throw new IllegalArgumentException("foreign NPC chat viewer");
        }
        viewer.sendPackets(nativeDeVisibilityPacket);
    }

    private NativeDeChatAccess() {
    }
}
