// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import local.lito381.server.CombatAccess;
import local.lito381.server.NativeDeCharacterFields;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeInstanceState;
import local.lito381.server.NativeDeLiveAiJobs;
import local.lito381.server.NativeDeObjectFields;
import local.lito381.server.NativeDePerceptionScope;
import local.lito381.server.NativeDeSprites;
import local.lito381.server.NativeDeWorldContext;
import local.lito381.server.NativeDeWorldLog;
import local.lito381.server.NativeDeWorldMap;

final class NativeDeLiveAiScope {
    static final byte[] HEADING_TABLE_X = new byte[]{0, 1, 1, 1, 0, -1, -1, -1};
    static final byte[] HEADING_TABLE_Y = new byte[]{-1, -1, 0, 1, 1, 1, 0, -1};
    private static final ThreadLocal<NativeDeInstanceState> current = new ThreadLocal();

    static NativeDeInstanceState actor() {
        return Objects.requireNonNull(current.get(), "De AI subject absent");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Object call(NativeDeInstanceState nativeDeInstanceState, Supplier<Object> supplier) {
        NativeDeInstanceState nativeDeInstanceState2 = current.get();
        current.set(Objects.requireNonNull(nativeDeInstanceState));
        try {
            return NativeDePerceptionScope.callNested(nativeDeInstanceState, supplier);
        }
        finally {
            if (nativeDeInstanceState2 == null) {
                current.remove();
            } else {
                current.set(nativeDeInstanceState2);
            }
        }
    }

    static boolean present() {
        return current.get() != null;
    }

    static NativeDeWorldContext context() {
        return (NativeDeWorldContext)NativeDeLiveAiScope.actor().deletionOwner.services;
    }

    static NativeDeLiveAiJobs.Binding binding() {
        return NativeDeLiveAiJobs.binding(NativeDeLiveAiScope.actor());
    }

    static AtomicLong counter(String string) {
        return NativeDeLiveAiScope.context().count("DeLiveAI." + string);
    }

    static boolean wakeupEnable() {
        return NativeDeLiveAiScope.binding().config.wakeupEnable();
    }

    static int wakeupInterval() {
        return NativeDeLiveAiScope.binding().config.wakeupInterval();
    }

    static int wakeupDuration() {
        return NativeDeLiveAiScope.binding().config.wakeupDuration();
    }

    static NativeDeWorldLog log() {
        NativeDeLiveAiJobs.Binding binding = NativeDeLiveAiJobs.find(NativeDeLiveAiScope.actor());
        return binding == null ? NativeDeLiveAiScope.context().log : binding.environment.log();
    }

    static boolean isPassable(NativeDeWorldMap nativeDeWorldMap, int n, int n2, NativeDeCharacterFields nativeDeCharacterFields) {
        return NativeDeLiveAiScope.binding().environment.passable(nativeDeWorldMap, n, n2, nativeDeCharacterFields);
    }

    static void send(NativeDeDeletionPorts.Viewer viewer, NativeDeDeletionPorts.Packet packet) {
        viewer.sendPackets(packet);
    }

    static void removeKnown(NativeDeDeletionPorts.Viewer viewer, NativeDeObjectFields nativeDeObjectFields) {
        CombatAccess.call(viewer, "removeKnownObject", "(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V", nativeDeObjectFields);
    }

    static void update(NativeDeDeletionPorts.Viewer viewer) {
        NativeDeLiveAiScope.binding().environment.update(viewer);
    }

    private NativeDeLiveAiScope() {
    }

    static final class World {
        World() {
        }

        static World get() {
            return new World();
        }

        ArrayList<NativeDeDeletionPorts.Viewer> getVisiblePlayer(NativeDeObjectFields nativeDeObjectFields) {
            return (ArrayList)NativeDeLiveAiScope.context().call("getVisiblePlayer", "(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Ljava/util/ArrayList;", nativeDeObjectFields);
        }

        ArrayList<NativeDeDeletionPorts.Viewer> getRecognizePlayer(NativeDeObjectFields nativeDeObjectFields) {
            return (ArrayList)NativeDeLiveAiScope.context().call("getRecognizePlayer", "(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Ljava/util/ArrayList;", nativeDeObjectFields);
        }

        boolean relocateVisibleObject(NativeDeObjectFields nativeDeObjectFields, int n, int n2, int n3) {
            return (Boolean)NativeDeLiveAiScope.context().call("relocateVisibleObject", "(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;III)Z", nativeDeObjectFields, n, n2, n3);
        }
    }

    static final class Sprites {
        Sprites() {
        }

        static Sprites get() {
            return new Sprites();
        }

        int getAttackSpeed(int n, int n2) {
            return NativeDeSprites.catalog().attack(n, n2);
        }

        int getMoveSpeed(int n, int n2) {
            return NativeDeSprites.catalog().move(n, n2);
        }
    }

    static final class Manager {
        Manager() {
        }

        static Manager get() {
            return new Manager();
        }

        void add(NativeDeInstanceState nativeDeInstanceState) {
            NativeDeLiveAiJobs.background(nativeDeInstanceState, true);
        }

        void remove(NativeDeInstanceState nativeDeInstanceState) {
            NativeDeLiveAiJobs.background(nativeDeInstanceState, false);
        }
    }

    static final class Clock {
        Clock() {
        }

        static Clock get() {
            return new Clock();
        }

        ScheduledFuture<?> aiSchedule(Runnable runnable, long l, long l2) {
            return NativeDeLiveAiJobs.schedule(NativeDeLiveAiScope.actor(), runnable, l, l2);
        }
    }

    static final class Pool {
        Pool() {
        }

        static Pool get() {
            return new Pool();
        }

        boolean isShutdown() {
            return !NativeDeLiveAiJobs.admits(NativeDeLiveAiScope.actor());
        }

        boolean tryExecute(Runnable runnable) {
            return NativeDeLiveAiJobs.execute(NativeDeLiveAiScope.actor(), runnable);
        }

        static interface ShutdownDiscardable
        extends Runnable {
            public void discardOnShutdown();
        }
    }
}
