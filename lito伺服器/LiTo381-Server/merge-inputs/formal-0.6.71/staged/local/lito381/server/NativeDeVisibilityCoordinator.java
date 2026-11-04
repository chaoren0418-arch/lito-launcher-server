// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.Collection;
import local.lito381.server.NativeDeCharacterFields;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeDeletionState;
import local.lito381.server.NativeDeVisibilityAccess;
import local.lito381.server.NativeDeVisibilityCheck;
import local.lito381.server.NativeDeVisibilityPorts;
import local.lito381.server.NativeDeVisibilityRuntime;
import local.lito381.server.NativeDeVisibilityScope;
import local.lito381.server.NativeDeVisibilityWorld;
import local.lito381.server.NativeDeWorldKinds;
import local.lito381.server.NativeDeWorldLog;

public final class NativeDeVisibilityCoordinator {
    private static final NativeDeWorldLog _log = NativeDeVisibilityScope.Logs.getLogger(NativeDeVisibilityCoordinator.class);
    private static volatile long _tickCount;
    private static volatile long _pcUpdateCount;

    private NativeDeVisibilityCoordinator() {
    }

    public static void tick() {
        NativeDeVisibilityScope._tickCount$set(NativeDeVisibilityScope._tickCount() + 1L);
        try {
            Collection<NativeDeDeletionPorts.Viewer> collection = NativeDeVisibilityWorld.get().getAllPlayers();
            if (collection == null || collection.isEmpty()) {
                return;
            }
            for (NativeDeDeletionPorts.Viewer viewer : collection) {
                if (viewer == null) continue;
                try {
                    if (!NativeDeVisibilityCheck.check(viewer)) continue;
                    NativeDeVisibilityCoordinator.updateForPc(viewer);
                    NativeDeVisibilityScope._pcUpdateCount$set(NativeDeVisibilityScope._pcUpdateCount() + 1L);
                }
                catch (Exception exception) {
                    NativeDeVisibilityScope.log().warn("VisibilityCoordinator \u8655\u7406\u73a9\u5bb6 {} \u7570\u5e38", (Object)NativeDeVisibilityAccess.L1PcInstance$getName(viewer), (Object)exception);
                }
            }
        }
        catch (Exception exception) {
            NativeDeVisibilityScope.log().error("VisibilityCoordinator tick \u7570\u5e38", exception);
            throw exception;
        }
    }

    static void visibilitySource$updateForPc(NativeDeDeletionPorts.Viewer viewer) {
        if (viewer == null) {
            return;
        }
        if (NativeDeVisibilityAccess.L1PcInstance$getOnlineStatus(viewer) != 1 && NativeDeVisibilityAccess.L1PcInstance$getOnlineStatus(viewer) != 2 && NativeDeVisibilityAccess.L1PcInstance$getOnlineStatus(viewer) != 3) {
            return;
        }
        NativeDeVisibilityAccess.L1PcInstance$removeOutOfRangeKnownObjects(viewer);
        NativeDeVisibilityWorld.get().forEachNearbyObject(viewer, NativeDeVisibilityScope.range(), null, objectRef -> NativeDeVisibilityCoordinator.onNearbyVisible(viewer, objectRef));
    }

    private static void onNearbyVisible(NativeDeDeletionPorts.Viewer viewer, NativeDeDeletionPorts.ObjectRef objectRef) {
        NativeDeCharacterFields nativeDeCharacterFields;
        if (objectRef == null || objectRef.equals(viewer)) {
            return;
        }
        if (objectRef instanceof NativeDeWorldKinds.Merchant) {
            NativeDeWorldKinds.Merchant merchant = (NativeDeWorldKinds.Merchant)objectRef;
            if (!NativeDeVisibilityAccess.L1PcInstance$knownsObject(viewer, objectRef)) {
                NativeDeVisibilityAccess.L1MerchantInstance$onPerceive(merchant, viewer);
            }
            return;
        }
        if (objectRef instanceof NativeDeWorldKinds.Dwarf) {
            NativeDeWorldKinds.Dwarf dwarf = (NativeDeWorldKinds.Dwarf)objectRef;
            if (!NativeDeVisibilityAccess.L1PcInstance$knownsObject(viewer, objectRef)) {
                NativeDeVisibilityAccess.L1DwarfInstance$onPerceive(dwarf, viewer);
            }
            return;
        }
        if (objectRef instanceof NativeDeVisibilityPorts.FieldObject) {
            NativeDeVisibilityPorts.FieldObject fieldObject = (NativeDeVisibilityPorts.FieldObject)objectRef;
            if (!NativeDeVisibilityAccess.L1PcInstance$knownsObject(viewer, objectRef)) {
                NativeDeVisibilityAccess.L1FieldObjectInstance$onPerceive(fieldObject, viewer);
            }
            return;
        }
        short s = NativeDeVisibilityAccess.L1PcInstance$getMapId(viewer);
        int n = NativeDeVisibilityAccess.L1PcInstance$get_showId(viewer);
        if (NativeDeVisibilityAccess.L1Object$getMapId(objectRef) != s || NativeDeVisibilityAccess.L1Object$get_showId(objectRef) != n) {
            return;
        }
        if (!NativeDeVisibilityAccess.L1PcInstance$knownsObject(viewer, objectRef)) {
            NativeDeVisibilityAccess.L1Object$onPerceive(objectRef, viewer);
        } else if (objectRef instanceof NativeDeDeletionState && NativeDeVisibilityAccess.L1NpcInstance$getHiddenStatus(nativeDeCharacterFields = (NativeDeDeletionState)objectRef) != 0) {
            NativeDeVisibilityAccess.L1NpcInstance$approachPlayer(nativeDeCharacterFields, viewer);
        }
        if (NativeDeVisibilityAccess.L1PcInstance$isHpBarTarget(viewer, objectRef) && NativeDeVisibilityAccess.isCharacter(objectRef)) {
            nativeDeCharacterFields = (NativeDeCharacterFields)objectRef;
            NativeDeVisibilityAccess.L1Character$broadcastPacketHP(nativeDeCharacterFields, viewer);
        }
        if (NativeDeVisibilityAccess.L1PcInstance$hasSkillEffect(viewer, 2001) && NativeDeVisibilityAccess.L1PcInstance$isGmHpBarTarget(viewer, objectRef) && NativeDeVisibilityAccess.isCharacter(objectRef)) {
            nativeDeCharacterFields = (NativeDeCharacterFields)objectRef;
            NativeDeVisibilityAccess.L1Character$broadcastPacketHP(nativeDeCharacterFields, viewer);
        }
    }

    public static long getTickCount() {
        return NativeDeVisibilityScope._tickCount();
    }

    public static long getPcUpdateCount() {
        return NativeDeVisibilityScope._pcUpdateCount();
    }

    public static String getMonitorLine() {
        return String.format("Visibility tick=%d pcUpdates=%d", NativeDeVisibilityScope._tickCount(), NativeDeVisibilityScope._pcUpdateCount());
    }

    public static void updateForPc(NativeDeDeletionPorts.Viewer viewer) {
        NativeDeVisibilityRuntime.updateSource(viewer);
    }
}

