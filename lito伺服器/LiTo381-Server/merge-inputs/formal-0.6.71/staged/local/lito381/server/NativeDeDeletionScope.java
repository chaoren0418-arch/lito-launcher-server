// DECOMPILED from base-0.6.71.jar, TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import local.lito381.server.NativeDeDeletionOwner;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeDeletionState;

final class NativeDeDeletionScope {
    private static final ThreadLocal<NativeDeDeletionOwner> current = new ThreadLocal();

    static NativeDeDeletionOwner owner() {
        NativeDeDeletionOwner nativeDeDeletionOwner = current.get();
        if (nativeDeDeletionOwner == null) {
            throw new IllegalStateException("deletion scope absent");
        }
        return nativeDeDeletionOwner;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static <T> T call(NativeDeDeletionOwner nativeDeDeletionOwner, Supplier<T> supplier) {
        NativeDeDeletionOwner nativeDeDeletionOwner2 = current.get();
        if (nativeDeDeletionOwner2 != null && nativeDeDeletionOwner2 != nativeDeDeletionOwner) {
            throw new IllegalArgumentException("cross-application deletion");
        }
        current.set(nativeDeDeletionOwner);
        try {
            T t = supplier.get();
            return t;
        }
        finally {
            if (nativeDeDeletionOwner2 == null) {
                current.remove();
            } else {
                current.set(nativeDeDeletionOwner2);
            }
        }
    }

    static void run(NativeDeDeletionOwner nativeDeDeletionOwner, Runnable runnable) {
        NativeDeDeletionScope.call(nativeDeDeletionOwner, () -> {
            runnable.run();
            return null;
        });
    }

    static void require(NativeDeDeletionState nativeDeDeletionState) {
        if (NativeDeDeletionScope.owner() != nativeDeDeletionState.deletionOwner) {
            throw new IllegalArgumentException("deletion actor owner mismatch");
        }
    }

    static long nanoTime() {
        return NativeDeDeletionScope.owner().clock.getAsLong();
    }

    static NativeDeDeletionPorts.Log _log() {
        return NativeDeDeletionScope.owner().services.log();
    }

    static AtomicLong DELETE_DUPLICATE_SUPPRESSED_COUNT() {
        return NativeDeDeletionScope.owner().duplicate;
    }

    static AtomicLong DELETE_VERIFY_FAILURE_COUNT() {
        return NativeDeDeletionScope.owner().verifyFailure;
    }

    static AtomicLong LATE_VIEWER_REJECTED_COUNT() {
        return NativeDeDeletionScope.owner().lateViewer;
    }

    static AtomicLong DELETION_STALL_COUNT() {
        return NativeDeDeletionScope.owner().stall;
    }

    static AtomicLong DELETION_AUDIT_FAILURE_COUNT() {
        return NativeDeDeletionScope.owner().auditFailure;
    }

    static long DELETION_STALL_MILLIS() {
        return NativeDeDeletionScope.owner().stallMillis;
    }

    static ConcurrentHashMap<Integer, NativeDeDeletionState> DELETIONS_IN_FLIGHT() {
        return NativeDeDeletionScope.owner().inFlight;
    }

    static ReentrantLock _denameListLock() {
        return NativeDeDeletionScope.owner().services.namesLock();
    }

    static boolean BOSS_DEATH_AUDIT_ENABLE() {
        return NativeDeDeletionScope.owner().services.bossAudit();
    }

    static boolean present() {
        return current.get() != null;
    }

    private NativeDeDeletionScope() {
    }
}
