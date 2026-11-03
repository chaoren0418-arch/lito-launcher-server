// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Objects;
import local.lito381.server.NativeDeDeletionOwner;
import local.lito381.server.NativeDeInitializerKernel;
import local.lito381.server.NativeDeInstanceState;
import local.lito381.server.NativeDeNpcConstructionPorts;
import local.lito381.server.NativeDeNpcTemplatePorts;
import local.lito381.server.NativeDePerceptionEmblemData;
import local.lito381.server.NativeDePerceptionPorts;
import local.lito381.server.NativeDeResurrectionEffects;

final class NativeDeInstancePorts {
    private NativeDeInstancePorts() {
    }

    record Construction(int id, NativeDeResurrectionEffects effects, NativeDeDeletionOwner owner, NativeDePerceptionPorts.Services perception, NativeDeNpcTemplatePorts.Services template, NativeDeNpcConstructionPorts.Services npc, NativeDeInitializerKernel.Services initialization, Scene scene) {
        Construction {
            Objects.requireNonNull(nativeDeResurrectionEffects);
            Objects.requireNonNull(nativeDeDeletionOwner);
            Objects.requireNonNull(services);
            Objects.requireNonNull(services2);
            Objects.requireNonNull(services3);
            Objects.requireNonNull(services4);
            Objects.requireNonNull(scene);
        }
    }

    static interface Scene {
        public NativeDePerceptionEmblemData emblem(NativeDeInitializerKernel.Emblem var1);

        public void publish(NativeDeInstanceState var1, NativeDeInitializerKernel.Packet var2);
    }

    static interface Fishing
    extends NativeDePerceptionPorts.Fishing {
        public void start();

        public void stop();
    }

    static interface Shop
    extends NativeDePerceptionPorts.Shop {
        public void clearLists();
    }

    static interface Combat {
        public void checkAutoState();
    }
}
