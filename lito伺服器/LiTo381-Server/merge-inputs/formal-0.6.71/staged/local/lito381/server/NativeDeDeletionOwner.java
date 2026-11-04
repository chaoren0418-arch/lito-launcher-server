// DECOMPILED from base-0.6.71.jar, TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import local.lito381.server.NativeDeCorpseState;
import local.lito381.server.NativeDeCorpseTimers;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeDeletionScope;
import local.lito381.server.NativeDeDeletionState;
import local.lito381.server.NativePlayerLifetime;

final class NativeDeDeletionOwner
implements NativeDeCorpseTimers.Deletion {
    final NativeDeDeletionPorts.Services services;
    final LongSupplier clock;
    final NativeDeCorpseTimers.Owner timer;
    final AtomicLong duplicate = new AtomicLong();
    final AtomicLong verifyFailure = new AtomicLong();
    final AtomicLong lateViewer = new AtomicLong();
    final AtomicLong stall = new AtomicLong();
    final AtomicLong auditFailure = new AtomicLong();
    final long stallMillis = Math.max(1000L, Math.min(60000L, Long.getLong("lineage.npcDeletion.stallMillis", 5000L)));
    final ConcurrentHashMap<Integer, NativeDeDeletionState> inFlight = new ConcurrentHashMap();

    NativeDeDeletionOwner(NativePlayerLifetime nativePlayerLifetime, NativeDeDeletionPorts.Services services, LongSupplier longSupplier) {
        this.services = Objects.requireNonNull(services);
        this.clock = Objects.requireNonNull(longSupplier);
        this.timer = NativeDeCorpseTimers.of(nativePlayerLifetime, this, longSupplier);
    }

    @Override
    public void auditDeletionStalls() {
        NativeDeDeletionScope.run(this, NativeDeDeletionState::sourceAuditDeletionStalls);
    }

    @Override
    public void delete(NativeDeCorpseState nativeDeCorpseState) {
        if (nativeDeCorpseState instanceof NativeDeDeletionState) {
            NativeDeDeletionState nativeDeDeletionState = (NativeDeDeletionState)nativeDeCorpseState;
            if (nativeDeDeletionState.deletionOwner == this) {
                nativeDeDeletionState.deleteMe();
                return;
            }
        }
        throw new IllegalArgumentException("scene deletion owner mismatch");
    }

    @Override
    public short mapId(NativeDeCorpseState nativeDeCorpseState) {
        if (nativeDeCorpseState instanceof NativeDeDeletionState) {
            NativeDeDeletionState nativeDeDeletionState = (NativeDeDeletionState)nativeDeCorpseState;
            if (nativeDeDeletionState.deletionOwner == this) {
                return this.services.mapId(nativeDeDeletionState);
            }
        }
        throw new IllegalArgumentException("scene map owner mismatch");
    }
}
