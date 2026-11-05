// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import local.lito381.server.NativeDeDeletionPorts.Monster;
import local.lito381.server.NativeDeDeletionPorts.ObjectRef;
import local.lito381.server.NativeDeDeletionPorts.RemoveObject;
import local.lito381.server.NativeDeDeletionPorts.Summon;
import local.lito381.server.NativeDeDeletionPorts.Viewer;
import local.lito381.server.NativeDeDeletionVisibility.RepairKind;
import local.lito381.server.NativeDeDeletionVisibility.Snapshot;
import local.lito381.server.NativeDeVisibilityPorts.Follower;

import java.util.ArrayList;
import java.util.List;
import local.lito381.server.NativeDeDeletionDeState;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeDeletionState;
import local.lito381.server.NativeDeDeletionVisibility;
import local.lito381.server.NativeDeVisibilityAccess;
import local.lito381.server.NativeDeVisibilityCoordinator;
import local.lito381.server.NativeDeVisibilityPorts;
import local.lito381.server.NativeDeVisibilityScope;
import local.lito381.server.NativeDeVisibilityWorld;
import local.lito381.server.NativeDeWorldKinds;

public class NativeDeVisibilityPc {
    public static void removeOutOfRangeKnownObjects(NativeDeDeletionPorts.Viewer viewer) {
        try {
            List<NativeDeDeletionPorts.ObjectRef> arrayList = new ArrayList<NativeDeDeletionPorts.ObjectRef>(NativeDeVisibilityAccess.L1PcInstance$getKnownObjects(viewer));
            short s = NativeDeVisibilityAccess.L1PcInstance$getMapId(viewer);
            int n = NativeDeVisibilityAccess.L1PcInstance$get_showId(viewer);
            for (NativeDeDeletionPorts.ObjectRef objectRef : arrayList) {
                if (objectRef == null) continue;
                if (objectRef instanceof NativeDeDeletionState) {
                    NativeDeDeletionState nativeDeDeletionState = (NativeDeDeletionState)objectRef;
                    NativeDeDeletionVisibility.Snapshot snapshot = NativeDeVisibilityAccess.L1NpcInstance$deletionVisibilitySnapshot(nativeDeDeletionState);
                    boolean bl = NativeDeVisibilityWorld.get().isObjectRegistered(nativeDeDeletionState);
                    if (NativeDeVisibilityAccess.L1NpcInstance$destroyed(nativeDeDeletionState) || !bl) {
                        boolean bl2 = NativeDeVisibilityAccess.L1NpcInstance$isViewerRegistered(nativeDeDeletionState, viewer);
                        if (!NativeDeVisibilityAccess.L1PcInstance$removeKnownObjectIfPresent(viewer, objectRef)) continue;
                        NativeDeDeletionVisibility.RepairKind repairKind = NativeDeDeletionVisibility.classifyRepair(snapshot, NativeDeVisibilityAccess.L1NpcInstance$deletionStarted(nativeDeDeletionState), NativeDeVisibilityAccess.L1NpcInstance$getDeletionStallMillis());
                        NativeDeVisibilityAccess.L1PcInstance$sendPackets(viewer, new NativeDeDeletionPorts.RemoveObject(objectRef));
                        switch (NativeDeVisibilitySwitch.$SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$RepairKind[repairKind.ordinal()]) {
                            case 1: {
                                NativeDeVisibilityScope.NPC_VISIBILITY_RACE_REPAIR_COUNT().incrementAndGet();
                                break;
                            }
                            case 2: {
                                long l = NativeDeVisibilityScope.NPC_VISIBILITY_STALL_REPAIR_COUNT().incrementAndGet();
                                NativeDeVisibilityScope.log().warn("[NpcLifecycle] stalled deletion visibility repaired: playerId={} player={} objectId={} npcId={} phase={} deleteAgeMs={} phaseAgeMs={} worldRegistered={} destroyed={} viewerTracked={} playerMap={} npcMap={} count={}", new Object[]{NativeDeVisibilityAccess.L1PcInstance$getId(viewer), NativeDeVisibilityAccess.L1PcInstance$getName(viewer), NativeDeVisibilityAccess.L1NpcInstance$getId(nativeDeDeletionState), NativeDeVisibilityAccess.L1NpcInstance$getNpcId(nativeDeDeletionState), snapshot.phase(), snapshot.deleteAgeMillis(), snapshot.phaseAgeMillis(), bl, NativeDeVisibilityAccess.L1NpcInstance$destroyed(nativeDeDeletionState), bl2, (int)NativeDeVisibilityAccess.L1PcInstance$getMapId(viewer), (int)NativeDeVisibilityAccess.L1NpcInstance$getMapId(nativeDeDeletionState), l});
                                break;
                            }
                            case 3: {
                                long l = NativeDeVisibilityScope.NPC_VISIBILITY_LEAK_REPAIR_COUNT().incrementAndGet();
                                NativeDeVisibilityScope.log().warn("[NpcLifecycle] stale NPC leak repaired: playerId={} player={} objectId={} npcId={} phase={} deleteAgeMs={} phaseAgeMs={} worldRegistered={} destroyed={} viewerTracked={} playerMap={} npcMap={} count={}", new Object[]{NativeDeVisibilityAccess.L1PcInstance$getId(viewer), NativeDeVisibilityAccess.L1PcInstance$getName(viewer), NativeDeVisibilityAccess.L1NpcInstance$getId(nativeDeDeletionState), NativeDeVisibilityAccess.L1NpcInstance$getNpcId(nativeDeDeletionState), snapshot.phase(), snapshot.deleteAgeMillis(), snapshot.phaseAgeMillis(), bl, NativeDeVisibilityAccess.L1NpcInstance$destroyed(nativeDeDeletionState), bl2, (int)NativeDeVisibilityAccess.L1PcInstance$getMapId(viewer), (int)NativeDeVisibilityAccess.L1NpcInstance$getMapId(nativeDeDeletionState), l});
                                break;
                            }
                        }
                        continue;
                    }
                }
                if (NativeDeVisibilityAccess.L1Object$getMapId(objectRef) != s || NativeDeVisibilityAccess.L1Object$get_showId(objectRef) != n) {
                    NativeDeVisibilityAccess.L1PcInstance$removeKnownObject(viewer, objectRef);
                    NativeDeVisibilityAccess.L1PcInstance$sendPackets(viewer, new NativeDeDeletionPorts.RemoveObject(objectRef));
                    continue;
                }
                if (NativeDeVisibilityScope.range() == -1) {
                    if (!NativeDeVisibilityAccess.L1PcInstance$getLocation(viewer).isInScreen(NativeDeVisibilityAccess.L1Object$getLocation(objectRef))) {
                        NativeDeVisibilityAccess.L1PcInstance$removeKnownObject(viewer, objectRef);
                        NativeDeVisibilityAccess.L1PcInstance$sendPackets(viewer, new NativeDeDeletionPorts.RemoveObject(objectRef));
                    }
                } else if (NativeDeVisibilityAccess.L1PcInstance$getLocation(viewer).getTileLineDistance(NativeDeVisibilityAccess.L1Object$getLocation(objectRef)) > NativeDeVisibilityScope.range()) {
                    NativeDeVisibilityAccess.L1PcInstance$removeKnownObject(viewer, objectRef);
                    NativeDeVisibilityAccess.L1PcInstance$sendPackets(viewer, new NativeDeDeletionPorts.RemoveObject(objectRef));
                }
            }
        }
        catch (Exception exception) {
            NativeDeVisibilityScope.log().error("\u8996\u91ce\u79fb\u9664\u8655\u7406\u7570\u5e38: " + NativeDeVisibilityAccess.L1PcInstance$getName(viewer), exception);
        }
    }

    public static void updateObject(NativeDeDeletionPorts.Viewer viewer) {
        NativeDeVisibilityCoordinator.updateForPc(viewer);
    }

    public static boolean isHpBarTarget(Viewer var0, ObjectRef var1) {
      switch (NativeDeVisibilityAccess.L1PcInstance$getMapId(var0)) {
         case 400:
            if (var1 instanceof Follower var2 && NativeDeVisibilityAccess.L1FollowerInstance$getMaster(var2).equals(var0)) {
               return true;
            }
         default:
            return false;
      }
   }

    public static boolean isGmHpBarTarget(NativeDeDeletionPorts.Viewer viewer, NativeDeDeletionPorts.ObjectRef objectRef) {
        if (objectRef instanceof NativeDeDeletionPorts.Monster) {
            return true;
        }
        if (objectRef instanceof NativeDeDeletionPorts.Viewer) {
            return true;
        }
        if (objectRef instanceof NativeDeDeletionPorts.Summon) {
            return true;
        }
        if (objectRef instanceof NativeDeWorldKinds.Pet) {
            return true;
        }
        if (objectRef instanceof NativeDeDeletionDeState) {
            return true;
        }
        if (objectRef instanceof NativeDeVisibilityPorts.Follower) {
            return true;
        }
        return false;
    }
}
