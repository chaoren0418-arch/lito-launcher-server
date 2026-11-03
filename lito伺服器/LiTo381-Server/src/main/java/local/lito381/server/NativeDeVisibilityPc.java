// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.ArrayList;
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
            ArrayList arrayList = new ArrayList(NativeDeVisibilityAccess.L1PcInstance$getKnownObjects(viewer));
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
                        switch (repairKind) {
                            case NORMAL_RACE: {
                                NativeDeVisibilityScope.NPC_VISIBILITY_RACE_REPAIR_COUNT().incrementAndGet();
                                break;
                            }
                            case STALL: {
                                long l = NativeDeVisibilityScope.NPC_VISIBILITY_STALL_REPAIR_COUNT().incrementAndGet();
                                NativeDeVisibilityScope.log().warn("[NpcLifecycle] stalled deletion visibility repaired: playerId={} player={} objectId={} npcId={} phase={} deleteAgeMs={} phaseAgeMs={} worldRegistered={} destroyed={} viewerTracked={} playerMap={} npcMap={} count={}", new Object[]{NativeDeVisibilityAccess.L1PcInstance$getId(viewer), NativeDeVisibilityAccess.L1PcInstance$getName(viewer), NativeDeVisibilityAccess.L1NpcInstance$getId(nativeDeDeletionState), NativeDeVisibilityAccess.L1NpcInstance$getNpcId(nativeDeDeletionState), snapshot.phase(), snapshot.deleteAgeMillis(), snapshot.phaseAgeMillis(), bl, NativeDeVisibilityAccess.L1NpcInstance$destroyed(nativeDeDeletionState), bl2, (int)NativeDeVisibilityAccess.L1PcInstance$getMapId(viewer), (int)NativeDeVisibilityAccess.L1NpcInstance$getMapId(nativeDeDeletionState), l});
                                break;
                            }
                            case LEAK: {
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
                    if (NativeDeVisibilityAccess.L1PcInstance$getLocation(viewer).isInScreen(NativeDeVisibilityAccess.L1Object$getLocation(objectRef))) continue;
                    NativeDeVisibilityAccess.L1PcInstance$removeKnownObject(viewer, objectRef);
                    NativeDeVisibilityAccess.L1PcInstance$sendPackets(viewer, new NativeDeDeletionPorts.RemoveObject(objectRef));
                    continue;
                }
                if (NativeDeVisibilityAccess.L1PcInstance$getLocation(viewer).getTileLineDistance(NativeDeVisibilityAccess.L1Object$getLocation(objectRef)) <= NativeDeVisibilityScope.range()) continue;
                NativeDeVisibilityAccess.L1PcInstance$removeKnownObject(viewer, objectRef);
                NativeDeVisibilityAccess.L1PcInstance$sendPackets(viewer, new NativeDeDeletionPorts.RemoveObject(objectRef));
            }
        }
        catch (Exception exception) {
            NativeDeVisibilityScope.log().error("\u8996\u91ce\u79fb\u9664\u8655\u7406\u7570\u5e38: " + NativeDeVisibilityAccess.L1PcInstance$getName(viewer), exception);
        }
    }

    public static void updateObject(NativeDeDeletionPorts.Viewer viewer) {
        NativeDeVisibilityCoordinator.updateForPc(viewer);
    }

    public static boolean isHpBarTarget(NativeDeDeletionPorts.Viewer viewer, NativeDeDeletionPorts.ObjectRef objectRef) {
        switch (NativeDeVisibilityAccess.L1PcInstance$getMapId(viewer)) {
            case 400: {
                NativeDeVisibilityPorts.Follower follower;
                if (!(objectRef instanceof NativeDeVisibilityPorts.Follower) || !NativeDeVisibilityAccess.L1FollowerInstance$getMaster(follower = (NativeDeVisibilityPorts.Follower)objectRef).equals(viewer)) break;
                return true;
            }
        }
        return false;
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
        return objectRef instanceof NativeDeVisibilityPorts.Follower;
    }
}
