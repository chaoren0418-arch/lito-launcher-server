// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeVisibilityScope;
import local.lito381.server.NativeDeWorldContext;

final class NativeDeVisibilityWorld {
    NativeDeVisibilityWorld() {
    }

    static NativeDeVisibilityWorld get() {
        return new NativeDeVisibilityWorld();
    }

    NativeDeWorldContext ctx() {
        return NativeDeVisibilityScope.owner().context;
    }

    Collection<NativeDeDeletionPorts.Viewer> getAllPlayers() {
        return (Collection)this.ctx().call("getAllPlayers", "()Ljava/util/Collection;", new Object[0]);
    }

    void forEachNearbyObject(NativeDeDeletionPorts.ObjectRef objectRef, int n, Predicate<NativeDeDeletionPorts.ObjectRef> predicate, Consumer<NativeDeDeletionPorts.ObjectRef> consumer) {
        this.ctx().require(objectRef);
        this.ctx().call("forEachNearbyObject", "(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;ILjava/util/function/Predicate;Ljava/util/function/Consumer;)V", objectRef, n, predicate, consumer);
    }

    boolean isObjectRegistered(NativeDeDeletionPorts.ObjectRef objectRef) {
        return this.ctx().world.isObjectRegistered(objectRef);
    }
}
