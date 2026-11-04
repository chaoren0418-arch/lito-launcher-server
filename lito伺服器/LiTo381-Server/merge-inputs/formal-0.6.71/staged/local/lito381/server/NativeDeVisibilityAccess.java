// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import local.lito381.server.NativeDeDeletionPorts.ObjectRef;
import local.lito381.server.NativeDeDeletionPorts.Packet;
import local.lito381.server.NativeDeDeletionPorts.Viewer;
import local.lito381.server.NativeDeDeletionVisibility.Snapshot;
import local.lito381.server.NativeDeVisibilityPorts.Follower;

import java.util.List;
import local.lito381.server.CombatAccess;
import local.lito381.server.NativeDeCharacterFields;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeDeletionState;
import local.lito381.server.NativeDeDeletionVisibility;
import local.lito381.server.NativeDeNpcConstructionState;
import local.lito381.server.NativeDeNpcVitalState;
import local.lito381.server.NativeDeObjectFields;
import local.lito381.server.NativeDeResurrectionEffects;
import local.lito381.server.NativeDeVisibilityPc;
import local.lito381.server.NativeDeVisibilityPorts;
import local.lito381.server.NativeDeVisibilityRuntime;
import local.lito381.server.NativeDeVisibilityScope;
import local.lito381.server.NativeDeWorldAccess;
import local.lito381.server.NativeDeWorldConnection;
import local.lito381.server.NativeDeWorldKinds;
import local.lito381.server.NativeDeWorldLocation;

final class NativeDeVisibilityAccess {
    NativeDeVisibilityAccess() {
    }

    static boolean isCharacter(NativeDeDeletionPorts.ObjectRef objectRef) {
        return objectRef instanceof NativeDeCharacterFields && !(objectRef instanceof NativeDeWorldKinds.Item) && !(objectRef instanceof NativeDeWorldKinds.Trap);
    }

    static void L1Character$broadcastPacketHP(NativeDeDeletionPorts.ObjectRef objectRef, NativeDeDeletionPorts.Viewer viewer) {
        ((NativeDeCharacterFields)objectRef).broadcastPacketHP(viewer);
    }

    static int L1Character$getCurrentHp(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeVisibilityRuntime.hp(objectRef, false);
    }

    static int L1Character$getId(NativeDeDeletionPorts.ObjectRef objectRef) {
        return ((NativeDeObjectFields)objectRef).getId();
    }

    static int L1Character$getMaxHp(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeVisibilityRuntime.hp(objectRef, true);
    }

    static void L1DwarfInstance$onPerceive(NativeDeDeletionPorts.ObjectRef objectRef, NativeDeDeletionPorts.Viewer viewer) {
        NativeDeVisibilityScope.owner().services.perceive(objectRef, viewer);
    }

    static void L1FieldObjectInstance$onPerceive(NativeDeDeletionPorts.ObjectRef objectRef, NativeDeDeletionPorts.Viewer viewer) {
        NativeDeVisibilityScope.owner().services.perceive(objectRef, viewer);
    }

    static NativeDeCharacterFields L1FollowerInstance$getMaster(NativeDeDeletionPorts.ObjectRef objectRef) {
        return ((NativeDeVisibilityPorts.Follower)objectRef).getMaster();
    }

    static void L1MerchantInstance$onPerceive(NativeDeDeletionPorts.ObjectRef objectRef, NativeDeDeletionPorts.Viewer viewer) {
        NativeDeVisibilityScope.owner().services.perceive(objectRef, viewer);
    }

    static void L1NpcInstance$approachPlayer(NativeDeDeletionPorts.ObjectRef objectRef, NativeDeDeletionPorts.Viewer viewer) {
        NativeDeVisibilityScope.owner().services.approach((NativeDeDeletionState)objectRef, viewer);
    }

    static boolean L1NpcInstance$deletionStarted(NativeDeDeletionPorts.ObjectRef objectRef) {
        return ((NativeDeDeletionState)objectRef).deletionStarted();
    }

    static NativeDeDeletionVisibility.Snapshot L1NpcInstance$deletionVisibilitySnapshot(NativeDeDeletionPorts.ObjectRef objectRef) {
        return ((NativeDeDeletionState)objectRef).deletionVisibilitySnapshot();
    }

    static boolean L1NpcInstance$destroyed(NativeDeDeletionPorts.ObjectRef objectRef) {
        return ((NativeDeDeletionState)objectRef).destroyed();
    }

    static long L1NpcInstance$getDeletionStallMillis() {
        return NativeDeVisibilityScope.owner().context.deletion.stallMillis;
    }

    static int L1NpcInstance$getHiddenStatus(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeVisibilityScope.owner().services.hidden((NativeDeDeletionState)objectRef);
    }

    static int L1NpcInstance$getId(NativeDeDeletionPorts.ObjectRef objectRef) {
        return ((NativeDeObjectFields)objectRef).getId();
    }

    static short L1NpcInstance$getMapId(NativeDeDeletionPorts.ObjectRef objectRef) {
        return ((NativeDeObjectFields)objectRef).getMapId();
    }

    static int L1NpcInstance$getNpcId(ObjectRef var0) {
      return var0 instanceof NativeDeNpcConstructionState var1
         ? var1.getNpcId()
         : ((NativeDeResurrectionEffects)CombatAccess.field(var0, "effects")).npcId((NativeDeNpcVitalState)var0);
   }

    static boolean L1NpcInstance$isViewerRegistered(NativeDeDeletionPorts.ObjectRef objectRef, NativeDeDeletionPorts.Viewer viewer) {
        return ((NativeDeDeletionState)objectRef).isViewerRegistered(viewer);
    }

    static NativeDeWorldLocation L1Object$getLocation(NativeDeDeletionPorts.ObjectRef objectRef) {
        return ((NativeDeObjectFields)objectRef).getLocation();
    }

    static short L1Object$getMapId(NativeDeDeletionPorts.ObjectRef objectRef) {
        return ((NativeDeObjectFields)objectRef).getMapId();
    }

    static int L1Object$get_showId(NativeDeDeletionPorts.ObjectRef objectRef) {
        return ((NativeDeObjectFields)objectRef).get_showId();
    }

    static void L1Object$onPerceive(NativeDeDeletionPorts.ObjectRef objectRef, NativeDeDeletionPorts.Viewer viewer) {
        NativeDeVisibilityScope.owner().services.perceive(objectRef, viewer);
    }

    static int L1PcInstance$getId(NativeDeDeletionPorts.ObjectRef objectRef) {
        return ((NativeDeObjectFields)objectRef).getId();
    }

    static List L1PcInstance$getKnownObjects(NativeDeDeletionPorts.ObjectRef objectRef) {
        return ((NativeDeCharacterFields)objectRef).getKnownObjects();
    }

    static NativeDeWorldLocation L1PcInstance$getLocation(NativeDeDeletionPorts.ObjectRef objectRef) {
        return ((NativeDeObjectFields)objectRef).getLocation();
    }

    static short L1PcInstance$getMapId(NativeDeDeletionPorts.ObjectRef objectRef) {
        return ((NativeDeObjectFields)objectRef).getMapId();
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

    static int L1PcInstance$get_showId(NativeDeDeletionPorts.ObjectRef objectRef) {
        return ((NativeDeObjectFields)objectRef).get_showId();
    }

    static boolean L1PcInstance$hasSkillEffect(NativeDeDeletionPorts.ObjectRef objectRef, int n) {
        return ((NativeDeCharacterFields)objectRef).hasSkillEffect(n);
    }

    static boolean L1PcInstance$isGmHpBarTarget(NativeDeDeletionPorts.ObjectRef objectRef, NativeDeDeletionPorts.ObjectRef objectRef2) {
        return NativeDeVisibilityPc.isGmHpBarTarget((NativeDeDeletionPorts.Viewer)objectRef, objectRef2);
    }

    static boolean L1PcInstance$isHpBarTarget(NativeDeDeletionPorts.ObjectRef objectRef, NativeDeDeletionPorts.ObjectRef objectRef2) {
        return NativeDeVisibilityPc.isHpBarTarget((NativeDeDeletionPorts.Viewer)objectRef, objectRef2);
    }

    static boolean L1PcInstance$isTeleport(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeVisibilityScope.owner().services.teleport((NativeDeDeletionPorts.Viewer)objectRef);
    }

    static boolean L1PcInstance$knownsObject(NativeDeDeletionPorts.ObjectRef objectRef, NativeDeDeletionPorts.ObjectRef objectRef2) {
        return ((NativeDeCharacterFields)objectRef).knownsObject(objectRef2);
    }

    static void L1PcInstance$removeKnownObject(NativeDeDeletionPorts.ObjectRef objectRef, NativeDeDeletionPorts.ObjectRef objectRef2) {
        ((NativeDeCharacterFields)objectRef).removeKnownObject(objectRef2);
    }

    static boolean L1PcInstance$removeKnownObjectIfPresent(NativeDeDeletionPorts.ObjectRef objectRef, NativeDeDeletionPorts.ObjectRef objectRef2) {
        return ((NativeDeCharacterFields)objectRef).removeKnownObjectIfPresent(objectRef2);
    }

    static void L1PcInstance$removeOutOfRangeKnownObjects(NativeDeDeletionPorts.ObjectRef objectRef) {
        NativeDeVisibilityPc.removeOutOfRangeKnownObjects((NativeDeDeletionPorts.Viewer)objectRef);
    }

    static void L1PcInstance$sendPackets(NativeDeDeletionPorts.ObjectRef objectRef, NativeDeDeletionPorts.Packet packet) {
        NativeDeVisibilityRuntime.send((NativeDeDeletionPorts.Viewer)objectRef, packet);
    }
}

