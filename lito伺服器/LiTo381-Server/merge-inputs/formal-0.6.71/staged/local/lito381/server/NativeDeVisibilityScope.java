// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import local.lito381.server.NativeDePerceptionControls;
import local.lito381.server.NativeDeVisibilityPacket;
import local.lito381.server.NativeDeVisibilityRuntime;
import local.lito381.server.NativeDeWorldLog;

final class NativeDeVisibilityScope {
    private static final ThreadLocal<NativeDeVisibilityRuntime.Owner> current = new ThreadLocal();

    static NativeDeVisibilityRuntime.Owner owner() {
        return Objects.requireNonNull(current.get(), "source visibility scope absent");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static <T> T call(NativeDeVisibilityRuntime.Owner owner, Supplier<T> supplier) {
        NativeDeVisibilityRuntime.Owner owner2 = current.get();
        if (owner2 != null && owner2 != owner) {
            throw new IllegalArgumentException("foreign visibility World");
        }
        current.set(owner);
        T t;
        try {
            t = supplier.get();
        }
        finally {
            if (owner2 == null) {
                current.remove();
            } else {
                current.set(owner2);
            }
        }
        return t;
    }

    static boolean present() {
        return current.get() != null;
    }

    static NativeDeWorldLog log() {
        NativeDeVisibilityRuntime.Owner owner = current.get();
        return owner == null ? (string, string2, objectArray) -> System.getLogger(NativeDeVisibilityScope.class.getName()).log(System.Logger.Level.WARNING, string2 + Arrays.toString(objectArray)) : owner.context.log;
    }

    static int range() {
        return NativeDeVisibilityScope.owner().context.recognizeRange;
    }

    static AtomicLong count(String string) {
        return NativeDeVisibilityScope.owner().context.count("visibility." + string);
    }

    static AtomicLong NPC_VISIBILITY_LEAK_REPAIR_COUNT() {
        return NativeDeVisibilityScope.count("NPC_VISIBILITY_LEAK_REPAIR_COUNT");
    }

    static AtomicLong NPC_VISIBILITY_RACE_REPAIR_COUNT() {
        return NativeDeVisibilityScope.count("NPC_VISIBILITY_RACE_REPAIR_COUNT");
    }

    static AtomicLong NPC_VISIBILITY_STALL_REPAIR_COUNT() {
        return NativeDeVisibilityScope.count("NPC_VISIBILITY_STALL_REPAIR_COUNT");
    }

    static AtomicLong RESTART_COUNT() {
        return NativeDeVisibilityScope.count("RESTART_COUNT");
    }

    static AtomicLong SCHEDULE_ERROR_COUNT() {
        return NativeDeVisibilityScope.count("SCHEDULE_ERROR_COUNT");
    }

    static AtomicLong SCHEDULE_REJECTED_COUNT() {
        return NativeDeVisibilityScope.count("SCHEDULE_REJECTED_COUNT");
    }

    static long _tickCount() {
        return NativeDeVisibilityScope.count("_tickCount").get();
    }

    static void _tickCount$set(long l) {
        NativeDeVisibilityScope.count("_tickCount").set(l);
    }

    static long _pcUpdateCount() {
        return NativeDeVisibilityScope.count("_pcUpdateCount").get();
    }

    static void _pcUpdateCount$set(long l) {
        NativeDeVisibilityScope.count("_pcUpdateCount").set(l);
    }

    static AtomicLong POST_FINALIZE_WRITE_COUNT() {
        return NativeDeVisibilityScope.count("POST_FINALIZE_WRITE_COUNT");
    }

    static AtomicLong FINALIZE_FAILURE_COUNT() {
        return NativeDeVisibilityScope.count("FINALIZE_FAILURE_COUNT");
    }

    static AtomicLong FINALIZE_INVARIANT_COUNT() {
        return NativeDeVisibilityScope.count("FINALIZE_INVARIANT_COUNT");
    }

    static AtomicLong CONSTRUCTION_FAILURE_COUNT() {
        return NativeDeVisibilityScope.count("CONSTRUCTION_FAILURE_COUNT");
    }

    static String CLIENT_LANGUAGE_CODE() {
        return NativeDePerceptionControls.current().language();
    }

    static ArrayList<String> BAD_TEXT_LIST() {
        return NativeDePerceptionControls.current().badText();
    }

    static int HP_OPCODE() {
        return 104;
    }

    static void debug(NativeDeVisibilityPacket nativeDeVisibilityPacket) {
        System.getLogger(NativeDeVisibilityScope.class.getName()).log(System.Logger.Level.DEBUG, nativeDeVisibilityPacket.getType());
    }

    private NativeDeVisibilityScope() {
    }

    static final class Logs {
        Logs() {
        }

        static NativeDeWorldLog getLogger(Class<?> clazz) {
            return (string, string2, objectArray) -> NativeDeVisibilityScope.log().emit(string, string2, objectArray);
        }
    }
}

