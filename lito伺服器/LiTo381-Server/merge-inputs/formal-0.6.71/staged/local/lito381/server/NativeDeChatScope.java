// DECOMPILED from base-0.6.71.jar, TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import local.lito381.server.NativeDeChatData;
import local.lito381.server.NativeDeChatJobs;
import local.lito381.server.NativeDeChatTable;
import local.lito381.server.NativeDeNpcConstructionState;
import local.lito381.server.NativeDeVisibilityScope;
import local.lito381.server.NativeDeWorldLog;

final class NativeDeChatScope {
    private static final ThreadLocal<Data> loading = new ThreadLocal();
    private static final ThreadLocal<NativeDeNpcConstructionState> actor = new ThreadLocal();
    private static final ThreadLocal<NativeDeChatJobs.Owner> runtime = new ThreadLocal();

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static <T> T loading(Data data, Supplier<T> supplier) {
        Data data2 = loading.get();
        loading.set(data);
        try {
            return supplier.get();
        }
        finally {
            if (data2 == null) {
                loading.remove();
            } else {
                loading.set(data2);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static <T> T call(NativeDeChatJobs.Owner owner, NativeDeNpcConstructionState nativeDeNpcConstructionState, Supplier<T> supplier) {
        NativeDeNpcConstructionState nativeDeNpcConstructionState2 = actor.get();
        NativeDeChatJobs.Owner owner2 = runtime.get();
        if (owner2 != null && owner2 != owner) {
            throw new IllegalArgumentException("foreign NPC chat World");
        }
        actor.set(nativeDeNpcConstructionState);
        runtime.set(owner);
        try {
            return NativeDeVisibilityScope.call(owner.visibility, supplier);
        }
        finally {
            if (nativeDeNpcConstructionState2 == null) {
                actor.remove();
            } else {
                actor.set(nativeDeNpcConstructionState2);
            }
            if (owner2 == null) {
                runtime.remove();
            } else {
                runtime.set(owner2);
            }
        }
    }

    static boolean present() {
        return runtime.get() != null;
    }

    static NativeDeNpcConstructionState actor() {
        return Objects.requireNonNull(actor.get(), "source NPC chat actor absent");
    }

    static NativeDeChatJobs.Owner owner() {
        return Objects.requireNonNull(runtime.get(), "source NPC chat owner absent");
    }

    static Data loadingData() {
        return Objects.requireNonNull(loading.get(), "NPC chat load ownership absent");
    }

    static Map<Integer, NativeDeChatData> view(Data data, Map<Integer, NativeDeChatData> map) {
        return data.frozen ? Collections.unmodifiableMap(map) : map;
    }

    static Map<Integer, NativeDeChatData> _npcChatAppearance(NativeDeChatTable nativeDeChatTable) {
        return NativeDeChatScope.view(nativeDeChatTable.nativeData, nativeDeChatTable.nativeData.appearance);
    }

    static Map<Integer, NativeDeChatData> _npcChatDead(NativeDeChatTable nativeDeChatTable) {
        return NativeDeChatScope.view(nativeDeChatTable.nativeData, nativeDeChatTable.nativeData.dead);
    }

    static Map<Integer, NativeDeChatData> _npcChatHide(NativeDeChatTable nativeDeChatTable) {
        return NativeDeChatScope.view(nativeDeChatTable.nativeData, nativeDeChatTable.nativeData.hide);
    }

    static Map<Integer, NativeDeChatData> _npcChatGameTime(NativeDeChatTable nativeDeChatTable) {
        return NativeDeChatScope.view(nativeDeChatTable.nativeData, nativeDeChatTable.nativeData.game);
    }

    static NativeDeWorldLog log() {
        Data data = loading.get();
        if (data != null) {
            return data.log;
        }
        NativeDeChatJobs.Owner owner = runtime.get();
        return owner == null ? (string, string2, objectArray) -> System.getLogger(NativeDeChatScope.class.getName()).log(System.Logger.Level.WARNING, string2 + Arrays.toString(objectArray)) : owner.context.log;
    }

    static AtomicLong counter(String string) {
        return NativeDeChatScope.owner().context.count("NPCChat." + string);
    }

    private NativeDeChatScope() {
    }

    static final class Data {
        final Map<Integer, NativeDeChatData> appearance = new HashMap<Integer, NativeDeChatData>();
        final Map<Integer, NativeDeChatData> dead = new HashMap<Integer, NativeDeChatData>();
        final Map<Integer, NativeDeChatData> hide = new HashMap<Integer, NativeDeChatData>();
        final Map<Integer, NativeDeChatData> game = new HashMap<Integer, NativeDeChatData>();
        volatile boolean frozen;
        final NativeDeWorldLog log;

        Data(NativeDeWorldLog nativeDeWorldLog) {
            this.log = nativeDeWorldLog;
        }
    }

    static final class Clock {
        Clock() {
        }

        static Clock get() {
            return new Clock();
        }

        void execute(Runnable runnable) {
            if (!NativeDeChatJobs.execute(NativeDeChatScope.owner(), NativeDeChatScope.actor(), runnable)) {
                throw new RejectedExecutionException("NPC chat worker unavailable");
            }
        }

        ScheduledFuture<?> aiSchedule(Runnable runnable, long l) {
            return NativeDeChatJobs.schedule(NativeDeChatScope.owner(), NativeDeChatScope.actor(), runnable, l, 0L);
        }

        ScheduledFuture<?> aiScheduleAtFixedRate(Runnable runnable, long l, long l2) {
            return NativeDeChatJobs.schedule(NativeDeChatScope.owner(), NativeDeChatScope.actor(), runnable, l, l2);
        }

        void cancel(ScheduledFuture<?> scheduledFuture, boolean bl) {
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
            }
        }
    }

    static final class Pool {
        Pool() {
        }

        static Pool get() {
            return new Pool();
        }

        boolean tryExecute(Runnable runnable) {
            return NativeDeChatJobs.execute(NativeDeChatScope.owner(), NativeDeChatScope.actor(), runnable);
        }

        boolean isShutdown() {
            return !NativeDeChatJobs.admits(NativeDeChatScope.owner(), NativeDeChatScope.actor());
        }

        static interface ShutdownDiscardable
        extends Runnable {
            public void discardOnShutdown();
        }
    }

    static final class Logs {
        Logs() {
        }

        static NativeDeWorldLog getLogger(Class<?> clazz) {
            return (string, string2, objectArray) -> NativeDeChatScope.log().emit(string, string2, objectArray);
        }
    }
}
