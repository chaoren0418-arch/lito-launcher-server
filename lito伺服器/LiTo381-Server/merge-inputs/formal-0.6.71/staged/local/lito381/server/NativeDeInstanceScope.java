// DECOMPILED from base-0.6.71.jar, TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Objects;
import java.util.function.Consumer;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeInitializerKernel;
import local.lito381.server.NativeDeInstanceState;
import local.lito381.server.NativeDeObjectFields;
import local.lito381.server.NativeDePerceptionPacket;
import local.lito381.server.NativeDePerceptionScope;
import local.lito381.server.NativeDeWorldContext;
import local.lito381.server.NativeDeWorldPlayerFilter;

final class NativeDeInstanceScope {
    private static final ThreadLocal<NativeDeInstanceState> current = new ThreadLocal();

    static NativeDeInstanceState actor() {
        return Objects.requireNonNull(current.get(), "De initializer subject absent");
    }

    static void run(NativeDeInstanceState nativeDeInstanceState, Runnable runnable) {
        NativeDeInstanceState nativeDeInstanceState2 = current.get();
        current.set(Objects.requireNonNull(nativeDeInstanceState));
        try {
            NativeDePerceptionScope.callNested(nativeDeInstanceState, () -> {
                runnable.run();
                return null;
            });
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

    static NativeDeInitializerKernel.Log log() {
        return NativeDeInstanceScope.actor().construction.initialization().log();
    }

    static void send(NativeDeDeletionPorts.Viewer viewer, NativeDePerceptionPacket nativeDePerceptionPacket) {
        viewer.sendPackets(nativeDePerceptionPacket);
    }

    private NativeDeInstanceScope() {
    }

    static final class World {
        World() {
        }

        static World get() {
            return new World();
        }

        void forEachNearbyPlayer(NativeDeObjectFields nativeDeObjectFields, int n, NativeDeWorldPlayerFilter nativeDeWorldPlayerFilter, Consumer<NativeDeDeletionPorts.Viewer> consumer) {
            NativeDeDeletionPorts.Services services;
            NativeDeInstanceState nativeDeInstanceState = NativeDeInstanceScope.actor();
            if (nativeDeObjectFields != nativeDeInstanceState || !((services = nativeDeInstanceState.deletionOwner.services) instanceof NativeDeWorldContext)) {
                throw new IllegalArgumentException("De broadcast requires the owning concrete World");
            }
            NativeDeWorldContext nativeDeWorldContext = (NativeDeWorldContext)services;
            nativeDeWorldContext.call("forEachNearbyPlayer", "(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;ILlocal/lito381/server/NativeDeWorldPlayerFilter;Ljava/util/function/Consumer;)V", new Object[]{nativeDeObjectFields, n, nativeDeWorldPlayerFilter, consumer});
        }
    }
}
