// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Objects;
import java.util.function.Consumer;
import local.lito381.server.ArenaMapCatalog;
import local.lito381.server.NativeDeCharacterFields;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeInstanceState;
import local.lito381.server.NativeDeLiveAiPorts;
import local.lito381.server.NativeDeTerrainWorld;
import local.lito381.server.NativeDeVisibilityRuntime;
import local.lito381.server.NativeDeWorldContext;
import local.lito381.server.NativeDeWorldLog;
import local.lito381.server.NativeDeWorldMap;

final class NativeDeTerrainEnvironment
implements NativeDeLiveAiPorts.Environment {
    final NativeDeInstanceState actor;
    final NativeDeWorldContext context;
    final NativeDeTerrainWorld.Domain domain;
    final Consumer<NativeDeDeletionPorts.Viewer> refresh;

    NativeDeTerrainEnvironment(NativeDeInstanceState nativeDeInstanceState, NativeDeTerrainWorld.Domain domain, Consumer<NativeDeDeletionPorts.Viewer> consumer) {
        this.actor = Objects.requireNonNull(nativeDeInstanceState);
        this.context = (NativeDeWorldContext)nativeDeInstanceState.deletionOwner.services;
        this.domain = Objects.requireNonNull(domain);
        this.refresh = Objects.requireNonNull(consumer);
        NativeDeWorldContext.Entry entry = this.context.require(nativeDeInstanceState);
        NativeDeTerrainWorld.call(this.context, () -> {
            if (domain == NativeDeTerrainWorld.Domain.CLASSIC) {
                NativeDeTerrainWorld.classic(this.context, entry.showId, nativeDeInstanceState.getMapId());
            } else {
                ArenaMapCatalog.forMap(nativeDeInstanceState.getMapId());
            }
            return null;
        });
    }

    @Override
    public boolean passable(NativeDeWorldMap nativeDeWorldMap, int n, int n2, NativeDeCharacterFields nativeDeCharacterFields) {
        return NativeDeTerrainWorld.passable(this.context, this.domain, this.context.require((NativeDeDeletionPorts.ObjectRef)this.actor).showId, nativeDeWorldMap, n, n2, nativeDeCharacterFields);
    }

    @Override
    public void update(NativeDeDeletionPorts.Viewer viewer) {
        if (this.context.require((NativeDeDeletionPorts.ObjectRef)viewer).showId != this.context.require((NativeDeDeletionPorts.ObjectRef)this.actor).showId) {
            throw new IllegalArgumentException("foreign source terrain refresh");
        }
        this.refresh.accept(viewer);
    }

    @Override
    public NativeDeWorldLog log() {
        return this.context.log;
    }

    NativeDeTerrainEnvironment(NativeDeInstanceState nativeDeInstanceState, NativeDeTerrainWorld.Domain domain) {
        this(nativeDeInstanceState, domain, NativeDeVisibilityRuntime::update);
    }
}
