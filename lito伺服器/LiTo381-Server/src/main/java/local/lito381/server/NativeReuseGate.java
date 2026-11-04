// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.ClientSession;
import local.lito381.server.NativeCastTimeline;
import local.lito381.server.NativeDeItemApplication;
import local.lito381.server.NativeReuseActor;

final class NativeReuseGate {
    static NativeReuseActor actor(ClientSession clientSession) {
        if (!clientSession.nativePlayerActionLock().isHeldByCurrentThread()) {
            throw new IllegalStateException("reuse actor requires actual action lock");
        }
        NativeDeItemApplication.Owner owner = NativeDeItemApplication.publicationOwner(clientSession);
        if (owner == null) {
            return null;
        }
        NativeCastTimeline.State state = clientSession.nativeCastTimelineState();
        NativeReuseActor nativeReuseActor = state.reuseActor;
        if (nativeReuseActor == null || nativeReuseActor.owner != owner) {
            state.reuseActor = nativeReuseActor = new NativeReuseActor(clientSession, owner);
        }
        return nativeReuseActor;
    }

    private NativeReuseGate() {
    }
}
