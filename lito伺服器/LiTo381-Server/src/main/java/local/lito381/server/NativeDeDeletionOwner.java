// DECOMPILED from base-0.6.71.jar, TODO review
// Decompiled with Vineflower 1.11.2; reviewed against formal bytecode.
package local.lito381.server;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

final class NativeDeDeletionOwner implements NativeDeCorpseTimers.Deletion {
   final NativeDeDeletionPorts.Services services;
   final LongSupplier clock;
   final NativeDeCorpseTimers.Owner timer;
   final AtomicLong duplicate = new AtomicLong();
   final AtomicLong verifyFailure = new AtomicLong();
   final AtomicLong lateViewer = new AtomicLong();
   final AtomicLong stall = new AtomicLong();
   final AtomicLong auditFailure = new AtomicLong();
   final long stallMillis = Math.max(1000L, Math.min(60000L, Long.getLong("lineage.npcDeletion.stallMillis", 5000L)));
   final ConcurrentHashMap<Integer, NativeDeDeletionState> inFlight = new ConcurrentHashMap<>();

   NativeDeDeletionOwner(NativePlayerLifetime var1, NativeDeDeletionPorts.Services var2, LongSupplier var3) {
      this.services = Objects.requireNonNull(var2);
      this.clock = Objects.requireNonNull(var3);
      this.timer = NativeDeCorpseTimers.of(var1, this, var3);
   }

   @Override
   public void auditDeletionStalls() {
      NativeDeDeletionScope.run(this, NativeDeDeletionState::sourceAuditDeletionStalls);
   }

   @Override
   public void delete(NativeDeCorpseState var1) {
      if (!(var1 instanceof NativeDeDeletionState var2) || var2.deletionOwner != this) {
         throw new IllegalArgumentException("scene deletion owner mismatch");
      }
      var2.deleteMe();
   }

   @Override
   public short mapId(NativeDeCorpseState var1) {
      if (!(var1 instanceof NativeDeDeletionState var2) || var2.deletionOwner != this) {
         throw new IllegalArgumentException("scene map owner mismatch");
      }
      return this.services.mapId(var2);
   }
}
