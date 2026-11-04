// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  local.lito381.server.ClientSession
 *  local.lito381.server.CombatAccess
 *  local.lito381.server.GameHub
 *  local.lito381.server.NativeDeItemApplication
 *  local.lito381.server.NativeDeItemApplication$Owner
 *  local.lito381.server.NativeSourceTargetIconBatch
 */
package local.lito381.server;

import java.util.concurrent.atomic.AtomicLong;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.GameHub;
import local.lito381.server.NativeDeItemApplication;
import local.lito381.server.NativeSourceTargetIconBatch;

final class NativeRolePublication {
    private static final ThreadLocal<Lease> current = new ThreadLocal();
    private static final AtomicLong dropped = new AtomicLong();

    static Runnable completion(ClientSession clientSession, Runnable runnable) {
        if (!clientSession.nativePlayerActionLock().isHeldByCurrentThread()) {
            throw new IllegalStateException("role publication capture requires actual mutation lock");
        }
        GameHub gameHub = (GameHub)CombatAccess.field((Object)clientSession, (String)"hub");
        NativeDeItemApplication nativeDeItemApplication = (NativeDeItemApplication)CombatAccess.field((Object)gameHub, (String)"sourceItemApplication");
        Lease lease = new Lease(clientSession, nativeDeItemApplication, NativeDeItemApplication.publicationOwner((ClientSession)clientSession));
        return () -> {
            Lease lease2 = current.get();
            current.set(lease);
            try {
                runnable.run();
            }
            finally {
                if (lease2 == null) {
                    current.remove();
                } else {
                    current.set(lease2);
                }
            }
        };
    }

    static boolean allowPublication(ClientSession clientSession) {
        boolean bl;
        Lease lease = current.get();
        if (lease == null) {
            return true;
        }
        ClientSession clientSession2 = lease.pc;
        NativeDeItemApplication.Owner owner = lease.owner;
        boolean bl2 = bl = owner != null && owner.active && owner.matches(clientSession2) && owner.application.get() == lease.application && CombatAccess.field((Object)clientSession2, (String)"sourceItemOwner") == owner && CombatAccess.field((Object)CombatAccess.field((Object)clientSession2, (String)"hub"), (String)"sourceItemApplication") == lease.application && (lease.application == null || !lease.application.closed);
        if (!bl) {
            dropped.incrementAndGet();
        }
        return bl;
    }

    static long droppedPackets() {
        return dropped.get();
    }

    private NativeRolePublication() {
    }

    static Runnable nativeSourceTargetIconCompletion(ClientSession clientSession, Runnable runnable) {
        if (!clientSession.nativePlayerActionLock().isHeldByCurrentThread()) {
            throw new IllegalStateException("role publication capture requires actual mutation lock");
        }
        GameHub gameHub = (GameHub)CombatAccess.field((Object)clientSession, (String)"hub");
        NativeDeItemApplication nativeDeItemApplication = (NativeDeItemApplication)CombatAccess.field((Object)gameHub, (String)"sourceItemApplication");
        Lease lease = new Lease(clientSession, nativeDeItemApplication, NativeSourceTargetIconBatch.existing((ClientSession)clientSession));
        return () -> {
            Lease lease2 = current.get();
            current.set(lease);
            try {
                runnable.run();
            }
            finally {
                if (lease2 == null) {
                    current.remove();
                } else {
                    current.set(lease2);
                }
            }
        };
    }

    private record Lease(ClientSession pc, NativeDeItemApplication application, NativeDeItemApplication.Owner owner) {
    }
}

