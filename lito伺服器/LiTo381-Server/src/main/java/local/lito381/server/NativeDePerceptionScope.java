// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDePerceptionControls;
import local.lito381.server.NativeDePerceptionState;

final class NativeDePerceptionScope {
    private static final ThreadLocal<NativeDePerceptionState> current = new ThreadLocal();

    NativeDePerceptionScope() {
    }

    static NativeDePerceptionState actor() {
        NativeDePerceptionState nativeDePerceptionState = current.get();
        if (nativeDePerceptionState == null) {
            throw new IllegalStateException("perception scope absent");
        }
        return nativeDePerceptionState;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static <T> T call(NativeDePerceptionState nativeDePerceptionState, Supplier<T> supplier) {
        NativeDePerceptionState nativeDePerceptionState2 = current.get();
        if (nativeDePerceptionState2 != null && nativeDePerceptionState2 != nativeDePerceptionState) {
            throw new IllegalArgumentException("foreign perception subject");
        }
        current.set(nativeDePerceptionState);
        try {
            T t = supplier.get();
            return t;
        }
        finally {
            if (nativeDePerceptionState2 == null) {
                current.remove();
            } else {
                current.set(nativeDePerceptionState2);
            }
        }
    }

    static void run(NativeDePerceptionState nativeDePerceptionState, Runnable runnable) {
        NativeDePerceptionScope.call(nativeDePerceptionState, () -> {
            runnable.run();
            return null;
        });
    }

    static NativeDeDeletionPorts.Log _log() {
        return NativeDePerceptionScope.actor().deletionOwner.services.log();
    }

    static String CLIENT_LANGUAGE_CODE() {
        return NativeDePerceptionControls.current().language();
    }

    static ArrayList<String> BAD_TEXT_LIST() {
        return NativeDePerceptionControls.current().badText();
    }

    static AtomicLong count(String string) {
        return NativeDePerceptionScope.actor().context().count("packet." + string);
    }

    static AtomicLong POST_FINALIZE_WRITE_COUNT() {
        return NativeDePerceptionScope.count("POST_FINALIZE_WRITE_COUNT");
    }

    static AtomicLong FINALIZE_FAILURE_COUNT() {
        return NativeDePerceptionScope.count("FINALIZE_FAILURE_COUNT");
    }

    static AtomicLong FINALIZE_INVARIANT_COUNT() {
        return NativeDePerceptionScope.count("FINALIZE_INVARIANT_COUNT");
    }

    static AtomicLong CONSTRUCTION_FAILURE_COUNT() {
        return NativeDePerceptionScope.count("CONSTRUCTION_FAILURE_COUNT");
    }

    static boolean present() {
        return current.get() != null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static <T> T callNested(NativeDePerceptionState nativeDePerceptionState, Supplier<T> supplier) {
        NativeDePerceptionState nativeDePerceptionState2 = current.get();
        current.set(nativeDePerceptionState);
        try {
            T t = supplier.get();
            return t;
        }
        finally {
            if (nativeDePerceptionState2 == null) {
                current.remove();
            } else {
                current.set(nativeDePerceptionState2);
            }
        }
    }
}
