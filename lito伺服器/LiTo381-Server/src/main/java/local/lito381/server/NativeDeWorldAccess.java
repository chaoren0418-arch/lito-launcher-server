// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.CombatAccess;
import local.lito381.server.NativeDeDeletionDeState;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeDeletionState;
import local.lito381.server.NativeDeObjectFields;
import local.lito381.server.NativeDeWorldConnection;
import local.lito381.server.NativeDeWorldContext;
import local.lito381.server.NativeDeWorldLocation;
import local.lito381.server.NativeDeWorldMap;

final class NativeDeWorldAccess {
    NativeDeWorldAccess() {
    }

    private static NativeDeWorldContext.Entry entry(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeWorldContext.entry(objectRef);
    }

    private static int id(NativeDeDeletionPorts.ObjectRef objectRef) {
        return ((NativeDeObjectFields)objectRef).getId();
    }

    static int L1BowInstance$getId(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeWorldAccess.id(objectRef);
    }

    static String L1DeInstance$getNameId(NativeDeDeletionPorts.ObjectRef objectRef) {
        return ((NativeDeDeletionDeState)objectRef).getNameId();
    }

    static int L1DollInstance$getId(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeWorldAccess.id(objectRef);
    }

    static int L1EffectInstance$getId(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeWorldAccess.id(objectRef);
    }

    static int L1ItemInstance$getId(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeWorldAccess.id(objectRef);
    }

    static int L1MonsterInstance$getId(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeWorldAccess.id(objectRef);
    }

    static void L1NpcInstance$abortFailedWorldRegistration(NativeDeDeletionPorts.ObjectRef objectRef) {
        NativeDeDeletionState nativeDeDeletionState = (NativeDeDeletionState)objectRef;
        CombatAccess.call(nativeDeDeletionState, "abortFailedWorldRegistration", "()V", new Object[0]);
    }

    static int L1NpcInstance$getId(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeWorldAccess.id(objectRef);
    }

    static int L1NpcInstance$get_showId(NativeDeDeletionPorts.ObjectRef objectRef) {
        return ((NativeDeObjectFields)objectRef).get_showId();
    }

    static int L1Object$getId(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeWorldAccess.id(objectRef);
    }

    static NativeDeWorldLocation L1Object$getLocation(NativeDeDeletionPorts.ObjectRef objectRef) {
        return ((NativeDeObjectFields)objectRef).getLocation();
    }

    static NativeDeWorldMap L1Object$getMap(NativeDeDeletionPorts.ObjectRef objectRef) {
        return ((NativeDeObjectFields)objectRef).getLocation().getMap();
    }

    static short L1Object$getMapId(NativeDeDeletionPorts.ObjectRef objectRef) {
        return (short)((NativeDeObjectFields)objectRef).getLocation().getMapId();
    }

    static int L1Object$getX(NativeDeDeletionPorts.ObjectRef objectRef) {
        return ((NativeDeObjectFields)objectRef).getLocation().getX();
    }

    static int L1Object$getY(NativeDeDeletionPorts.ObjectRef objectRef) {
        return ((NativeDeObjectFields)objectRef).getLocation().getY();
    }

    static int L1Object$get_showId(NativeDeDeletionPorts.ObjectRef objectRef) {
        return ((NativeDeObjectFields)objectRef).get_showId();
    }

    static void L1Object$setLocation(NativeDeDeletionPorts.ObjectRef objectRef, int n, int n2, int n3) {
        ((NativeDeObjectFields)objectRef).setLocation(n, n2, n3);
    }

    static int L1PcInstance$getId(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeWorldAccess.id(objectRef);
    }

    static NativeDeWorldLocation L1PcInstance$getLocation(NativeDeDeletionPorts.ObjectRef objectRef) {
        return ((NativeDeObjectFields)objectRef).getLocation();
    }

    static short L1PcInstance$getMapId(NativeDeDeletionPorts.ObjectRef objectRef) {
        return (short)((NativeDeObjectFields)objectRef).getLocation().getMapId();
    }

    static String L1PcInstance$getName(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeWorldAccess.entry((NativeDeDeletionPorts.ObjectRef)objectRef).name;
    }

    static NativeDeWorldConnection L1PcInstance$getNetConnection(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeWorldAccess.entry((NativeDeDeletionPorts.ObjectRef)objectRef).connection;
    }

    static int L1PcInstance$getOnlineStatus(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeWorldAccess.entry((NativeDeDeletionPorts.ObjectRef)objectRef).onlineStatus;
    }

    static boolean L1PcInstance$isCrown(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeWorldAccess.entry((NativeDeDeletionPorts.ObjectRef)objectRef).classType == 0;
    }

    static boolean L1PcInstance$isDarkelf(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeWorldAccess.entry((NativeDeDeletionPorts.ObjectRef)objectRef).classType == 4;
    }

    static boolean L1PcInstance$isDragonKnight(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeWorldAccess.entry((NativeDeDeletionPorts.ObjectRef)objectRef).classType == 5;
    }

    static boolean L1PcInstance$isElf(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeWorldAccess.entry((NativeDeDeletionPorts.ObjectRef)objectRef).classType == 2;
    }

    static boolean L1PcInstance$isIllusionist(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeWorldAccess.entry((NativeDeDeletionPorts.ObjectRef)objectRef).classType == 6;
    }

    static boolean L1PcInstance$isKnight(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeWorldAccess.entry((NativeDeDeletionPorts.ObjectRef)objectRef).classType == 1;
    }

    static boolean L1PcInstance$isWizard(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeWorldAccess.entry((NativeDeDeletionPorts.ObjectRef)objectRef).classType == 3;
    }

    static int L1PetInstance$getId(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeWorldAccess.id(objectRef);
    }

    static int L1SummonInstance$getId(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeWorldAccess.id(objectRef);
    }

    static int L1TrapInstance$getId(NativeDeDeletionPorts.ObjectRef objectRef) {
        return NativeDeWorldAccess.id(objectRef);
    }

    static short L1TrapInstance$getMapId(NativeDeDeletionPorts.ObjectRef objectRef) {
        return (short)((NativeDeObjectFields)objectRef).getLocation().getMapId();
    }

    static int L1TrapInstance$getX(NativeDeDeletionPorts.ObjectRef objectRef) {
        return ((NativeDeObjectFields)objectRef).getLocation().getX();
    }

    static int L1TrapInstance$getY(NativeDeDeletionPorts.ObjectRef objectRef) {
        return ((NativeDeObjectFields)objectRef).getLocation().getY();
    }
}
