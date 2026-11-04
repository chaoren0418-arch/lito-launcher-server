// DECOMPILED from base-0.6.71.jar, TODO review
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
            return NativeDeNpcKnowledge.admitted(nativeDeDeletionState, objectRef);
        }
        finally {
            reentrantLock.unlock();
        }
    }

    private static boolean admitted(NativeDeDeletionState actor, NativeDeDeletionPorts.ObjectRef object) {
        NativePlayerLifetime life = actor.deletionOwner.timer.life;
        if (!life.admits() || actor.destroyed() || actor.deletionStarted()) return false;
        NativeDeWorldContext.Entry entry = (NativeDeWorldContext.Entry)CombatAccess.field(actor, "sourceDeWorldEntry");
        if (entry != null && entry.context.closed) return false;
        if (object instanceof NativeDeDeletionState state && state.deletionOwner.timer.life != life) return false;
        if (object instanceof NativeDeClientViewer viewer && (viewer.life != life || viewer.retired)) return false;
        if (object instanceof NativeDeObjectFields) {
            NativeDeWorldContext.Entry other = (NativeDeWorldContext.Entry)CombatAccess.field(object, "sourceDeWorldEntry");
            if (other != null && (other.context.life != life || other.context.closed)) return false;
        }
        return actor.tryAddKnownObject(object);
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
