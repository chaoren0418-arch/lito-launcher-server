// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Objects;
import java.util.function.IntFunction;
import local.lito381.server.NativeDeInitializerKernel;
import local.lito381.server.NativeDeInstanceBroadcast;
import local.lito381.server.NativeDeInstancePorts;
import local.lito381.server.NativeDeInstanceScope;
import local.lito381.server.NativeDeInstanceState;
import local.lito381.server.NativeDePerceptionCharVisualUpdate;
import local.lito381.server.NativeDePerceptionEmblemData;
import local.lito381.server.NativeDeWorldContext;

final class NativeDeInstanceScene
implements NativeDeInstancePorts.Scene {
    final NativeDeWorldContext context;
    final IntFunction<NativeDePerceptionEmblemData> emblems;

    NativeDeInstanceScene(NativeDeWorldContext nativeDeWorldContext, IntFunction<NativeDePerceptionEmblemData> intFunction) {
        this.context = Objects.requireNonNull(nativeDeWorldContext);
        this.emblems = Objects.requireNonNull(intFunction);
    }

    static NativeDeInitializerKernel.Packet visual(int n, int n2) {
        NativeDeInstanceState nativeDeInstanceState = NativeDeInstanceScope.actor();
        if (nativeDeInstanceState.getId() != n) {
            throw new IllegalArgumentException("initializer visual source identity mismatch");
        }
        return new Visual(new NativeDePerceptionCharVisualUpdate(n, n2));
    }

    @Override
    public NativeDePerceptionEmblemData emblem(NativeDeInitializerKernel.Emblem emblem) {
        return emblem == null ? null : this.emblems.apply(emblem.id());
    }

    @Override
    public void publish(NativeDeInstanceState nativeDeInstanceState, NativeDeInitializerKernel.Packet packet) {
        Visual visual;
        block3: {
            block2: {
                if (nativeDeInstanceState.deletionOwner.services != this.context || NativeDeInstanceScope.actor() != nativeDeInstanceState || !(packet instanceof Visual)) break block2;
                visual = (Visual)packet;
                if (visual.packet().subject == nativeDeInstanceState) break block3;
            }
            throw new IllegalArgumentException("foreign initializer visual binding");
        }
        NativeDeInstanceBroadcast.toVisiblePlayers(nativeDeInstanceState, -1, visual.packet());
    }

    record Visual(NativeDePerceptionCharVisualUpdate packet) implements NativeDeInitializerKernel.Packet
    {
    }
}
