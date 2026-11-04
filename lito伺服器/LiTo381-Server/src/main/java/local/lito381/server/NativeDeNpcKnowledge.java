// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatNpcDamageQueue;
import local.lito381.server.NativeDeClientViewer;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeDeletionState;
import local.lito381.server.NativeDeObjectFields;
import local.lito381.server.NativeDeWorldContext;
import local.lito381.server.NativePlayerLifetime;

final class NativeDeNpcKnowledge {
    NativeDeNpcKnowledge() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean tryAdd(NativeDeDeletionState nativeDeDeletionState, NativeDeDeletionPorts.ObjectRef objectRef) {
        ReentrantLock reentrantLock = (ReentrantLock)CombatAccess.field(nativeDeDeletionState, "_knownObjectLock");
        reentrantLock.lock();
        try {
            boolean bl = NativeDeNpcKnowledge.admitted(nativeDeDeletionState, objectRef);
            return bl;
        }
        finally {
            reentrantLock.unlock();
        }
    }

    private static boolean admitted(NativeDeDeletionState nativeDeDeletionState, NativeDeDeletionPorts.ObjectRef objectRef) {
        Object object;
        NativePlayerLifetime nativePlayerLifetime = nativeDeDeletionState.deletionOwner.timer.life;
        if (!nativePlayerLifetime.admits() || nativeDeDeletionState.destroyed() || nativeDeDeletionState.deletionStarted()) {
            return false;
        }
        NativeDeWorldContext.Entry entry = (NativeDeWorldContext.Entry)CombatAccess.field(nativeDeDeletionState, "sourceDeWorldEntry");
        if (entry != null && entry.context.closed) {
            return false;
        }
        if (objectRef instanceof NativeDeDeletionState) {
            object = (NativeDeDeletionState)objectRef;
            if (((NativeDeDeletionState)object).deletionOwner.timer.life != nativePlayerLifetime) {
                return false;
            }
        }
        if (objectRef instanceof NativeDeClientViewer) {
            object = (NativeDeClientViewer)objectRef;
            if (((NativeDeClientViewer)object).life != nativePlayerLifetime || ((NativeDeClientViewer)object).retired) {
                return false;
            }
        }
        if (objectRef instanceof NativeDeObjectFields && (object = (NativeDeWorldContext.Entry)CombatAccess.field(objectRef, "sourceDeWorldEntry")) != null && (((NativeDeWorldContext.Entry)object).context.life != nativePlayerLifetime || ((NativeDeWorldContext.Entry)object).context.closed)) {
            return false;
        }
        return nativeDeDeletionState.tryAddKnownObject(objectRef);
    }

    static void collect(Object object, NativeDeWorldContext nativeDeWorldContext, Set<Object> set, Set<NativeDeDeletionState> set2) {
        if (object == null || !set.add(object)) {
            return;
        }
        if (object instanceof NativeDeDeletionState) {
            NativeDeDeletionState nativeDeDeletionState = (NativeDeDeletionState)object;
            if (nativeDeDeletionState.deletionOwner == nativeDeWorldContext.deletion) {
                set2.add(nativeDeDeletionState);
            }
        } else if (object instanceof Map) {
            Map map = (Map)object;
            for (Object v : map.values()) {
                NativeDeNpcKnowledge.collect(v, nativeDeWorldContext, set, set2);
            }
        } else if (object.getClass().getEnclosingClass() == nativeDeWorldContext.core.getClass() && object.getClass().getSimpleName().equals("VisibleBinding")) {
            NativeDeNpcKnowledge.collect(CombatAccess.call(object, "object", "()Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;", new Object[0]), nativeDeWorldContext, set, set2);
        }
    }

    static void retireWorld(NativeDeWorldContext nativeDeWorldContext) {
        Set<Object> set = Collections.newSetFromMap(new IdentityHashMap());
        Set<NativeDeDeletionState> set2 = Collections.newSetFromMap(new IdentityHashMap());
        for (String object : List.of("_allObjects", "_visibleObjects", "_visibleBindings", "_spatialIndex")) {
            NativeDeNpcKnowledge.collect(CombatAccess.field(nativeDeWorldContext.core, object), nativeDeWorldContext, set, set2);
        }
        for (NativeDeDeletionState nativeDeDeletionState : set2) {
            try {
                nativeDeDeletionState.removeAllKnownObjects();
            }
            catch (Throwable throwable) {
                CombatNpcDamageQueue.failed(throwable);
            }
        }
    }
}
