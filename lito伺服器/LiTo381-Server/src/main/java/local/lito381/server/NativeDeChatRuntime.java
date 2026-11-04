// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.CombatAccess;
import local.lito381.server.NativeDeChatJobs;
import local.lito381.server.NativeDeChatScope;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeNpcConstructionState;
import local.lito381.server.NativeDeWorldContext;

final class NativeDeChatRuntime {
    static void start(NativeDeNpcConstructionState nativeDeNpcConstructionState, int n) {
        NativeDeChatJobs.Owner owner = NativeDeChatJobs.owner(NativeDeWorldContext.entry((NativeDeDeletionPorts.ObjectRef)nativeDeNpcConstructionState).context);
        NativeDeChatJobs.call(owner, nativeDeNpcConstructionState, () -> {
            CombatAccess.call(nativeDeNpcConstructionState, "sourceChat$startChat", "(I)V", n);
            return null;
        });
    }

    static void cancel(NativeDeNpcConstructionState nativeDeNpcConstructionState) {
        NativeDeWorldContext nativeDeWorldContext = NativeDeWorldContext.entry((NativeDeDeletionPorts.ObjectRef)nativeDeNpcConstructionState).context;
        NativeDeChatJobs.Owner owner = NativeDeChatJobs.find(nativeDeWorldContext);
        if (owner == null) {
            return;
        }
        NativeDeChatScope.call(owner, nativeDeNpcConstructionState, () -> {
            CombatAccess.call(nativeDeNpcConstructionState, "sourceChat$cancelNpcChatTimer", "()V", new Object[0]);
            return null;
        });
    }

    static void cancelObject(NativeDeDeletionPorts.ObjectRef objectRef) {
        if (objectRef instanceof NativeDeNpcConstructionState) {
            NativeDeNpcConstructionState nativeDeNpcConstructionState = (NativeDeNpcConstructionState)objectRef;
            NativeDeChatRuntime.cancel(nativeDeNpcConstructionState);
            NativeDeChatJobs.retire(nativeDeNpcConstructionState);
        }
    }

    private NativeDeChatRuntime() {
    }
}
