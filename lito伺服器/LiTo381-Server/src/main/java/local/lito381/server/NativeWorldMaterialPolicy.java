// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatDatabase;
import local.lito381.server.NativeCastMaterialPolicy;
import local.lito381.server.NativeDeItemApplication;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativeReuseActor;
import local.lito381.server.NativeReuseGate;
import local.lito381.server.NativeSelfCastAction;

final class NativeWorldMaterialPolicy {
    private static final ThreadLocal<Prepared> current = new ThreadLocal();

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void run(ClientSession clientSession, boolean bl, int n, NativeSelfCastAction.Task task) throws IOException {
        if (!bl || CombatDatabase.value(0, n, "NativeWorldMaterialPolicy", "itemConsumeId") == 0) {
            task.run();
            return;
        }
        if (!clientSession.nativePlayerActionLock().isHeldByCurrentThread()) {
            throw new IllegalStateException("world material policy requires owned player lock");
        }
        NativeReuseActor nativeReuseActor = NativeReuseGate.actor(clientSession);
        NativePlayerCommit.Binding binding = NativePlayerCommit.Binding.capture(clientSession);
        NativeDeItemApplication.Owner owner = nativeReuseActor == null ? null : nativeReuseActor.owner;
        boolean bl2 = false;
        IOException iOException = null;
        try {
            bl2 = NativeCastMaterialPolicy.nativeOriginalGm(clientSession);
        }
        catch (IOException iOException2) {
            iOException = iOException2;
        }
        Prepared prepared = current.get();
        current.set(new Prepared(clientSession, binding, owner, bl2, iOException));
        try {
            task.run();
        }
        finally {
            if (prepared == null) {
                current.remove();
            } else {
                current.set(prepared);
            }
        }
    }

    static boolean gm(ClientSession clientSession) throws IOException {
        Prepared prepared = current.get();
        if (prepared == null) {
            current.remove();
            return NativeCastMaterialPolicy.nativeOriginalGm(clientSession);
        }
        if (prepared.pc != clientSession) {
            return NativeCastMaterialPolicy.nativeOriginalGm(clientSession);
        }
        if (!clientSession.nativePlayerActionLock().isHeldByCurrentThread()) {
            throw new IllegalStateException("prepared policy lost player lock");
        }
        NativeReuseActor nativeReuseActor = NativeReuseGate.actor(clientSession);
        if (!prepared.binding.matches(clientSession) || nativeReuseActor == null || nativeReuseActor.owner != prepared.owner) {
            throw new IOException("prepared material character policy role changed");
        }
        if (prepared.failure != null) {
            throw prepared.failure;
        }
        return prepared.gm;
    }

    private NativeWorldMaterialPolicy() {
    }

    private record Prepared(ClientSession pc, NativePlayerCommit.Binding binding, Object owner, boolean gm, IOException failure) {
    }
}
