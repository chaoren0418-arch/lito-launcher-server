// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import local.lito381.server.NativeDeWorldContext;
import local.lito381.server.NativeDeWorldLog;

final class NativeDeWorldScope {
    private static final ThreadLocal<NativeDeWorldContext> current = new ThreadLocal();

    NativeDeWorldScope() {
    }

    static NativeDeWorldContext owner() {
        NativeDeWorldContext nativeDeWorldContext = current.get();
        if (nativeDeWorldContext == null) {
            throw new IllegalStateException("World scope absent");
        }
        return nativeDeWorldContext;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static <T> T call(NativeDeWorldContext nativeDeWorldContext, Supplier<T> supplier) {
        NativeDeWorldContext nativeDeWorldContext2 = current.get();
        if (nativeDeWorldContext2 != null && nativeDeWorldContext2 != nativeDeWorldContext) {
            throw new IllegalArgumentException("cross-application World scope");
        }
        current.set(nativeDeWorldContext);
        T t;
        try {
            t = supplier.get();
        }
        finally {
            if (nativeDeWorldContext2 == null) {
                current.remove();
            } else {
                current.set(nativeDeWorldContext2);
            }
        }
        return t;
    }

    static Object registry(String string) {
        return NativeDeWorldScope.owner().registries.get(string);
    }

    static NativeDeWorldLog _log() {
        return NativeDeWorldScope.owner().log;
    }

    static int PC_RECOGNIZE_RANGE() {
        return NativeDeWorldScope.owner().recognizeRange;
    }

    static AtomicLong SPATIAL_STALE_ENTRY_COUNT() {
        return NativeDeWorldScope.owner().count("SPATIAL_STALE_ENTRY_COUNT");
    }

    static AtomicLong SPATIAL_STALE_PLAYER_ENTRY_COUNT() {
        return NativeDeWorldScope.owner().count("SPATIAL_STALE_PLAYER_ENTRY_COUNT");
    }

    static AtomicLong SPATIAL_BINDING_REPAIR_COUNT() {
        return NativeDeWorldScope.owner().count("SPATIAL_BINDING_REPAIR_COUNT");
    }

    static AtomicLong SPATIAL_BINDING_CONFLICT_COUNT() {
        return NativeDeWorldScope.owner().count("SPATIAL_BINDING_CONFLICT_COUNT");
    }

    static AtomicLong SPATIAL_TRAVERSAL_STALE_READ_COUNT() {
        return NativeDeWorldScope.owner().count("SPATIAL_TRAVERSAL_STALE_READ_COUNT");
    }

    static AtomicLong SPATIAL_RECONCILE_FAILURE_COUNT() {
        return NativeDeWorldScope.owner().count("SPATIAL_RECONCILE_FAILURE_COUNT");
    }

    static AtomicLong SPATIAL_AUDIT_COUNT() {
        return NativeDeWorldScope.owner().count("SPATIAL_AUDIT_COUNT");
    }

    static AtomicLong SPATIAL_AUDIT_REPAIR_COUNT() {
        return NativeDeWorldScope.owner().count("SPATIAL_AUDIT_REPAIR_COUNT");
    }

    static AtomicLong VISIBLE_REGISTRATION_FAILURE_COUNT() {
        return NativeDeWorldScope.owner().count("VISIBLE_REGISTRATION_FAILURE_COUNT");
    }

    static AtomicLong VISIBLE_REGISTRATION_ROLLBACK_FAILURE_COUNT() {
        return NativeDeWorldScope.owner().count("VISIBLE_REGISTRATION_ROLLBACK_FAILURE_COUNT");
    }

    static boolean present() {
        return current.get() != null;
    }
}

