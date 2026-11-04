// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Objects;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeNpcConstructionPorts;

final class NativeDeNpcInventoryScope {
    private static final ThreadLocal<NativeDeNpcConstructionPorts.Services> current = new ThreadLocal();

    NativeDeNpcInventoryScope() {
    }

    static void run(NativeDeNpcConstructionPorts.Services services, Runnable runnable) {
        NativeDeNpcConstructionPorts.Services services2 = current.get();
        current.set(Objects.requireNonNull(services));
        try {
            runnable.run();
        }
        finally {
            if (services2 == null) {
                current.remove();
            } else {
                current.set(services2);
            }
        }
    }

    static boolean present() {
        return current.get() != null;
    }

    static final class World {
        World() {
        }

        static World get() {
            return new World();
        }

        void removeObject(Object object) {
            NativeDeNpcConstructionPorts.Services services = current.get();
            if (services == null) {
                throw new IllegalStateException("inventory source World scope absent");
            }
            services.removeInventoryObject((NativeDeDeletionPorts.ObjectRef)object);
        }
    }
}
