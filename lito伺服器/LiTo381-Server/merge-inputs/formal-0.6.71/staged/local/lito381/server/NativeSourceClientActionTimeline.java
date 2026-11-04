// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

public final class NativeSourceClientActionTimeline {
   private static final AtomicLong SKILL_INTERRUPTED_ATTACK_COUNT = new AtomicLong();
   private static final AtomicLong SKILL_INTERRUPTED_MOVE_COUNT = new AtomicLong();
   private static final AtomicLong SKILL_CONFLICT_REJECT_COUNT = new AtomicLong();
   private static final AtomicLong STALE_SKILL_REJECT_COUNT = new AtomicLong();
   private final Object _mutationLock = new Object();
   private final LongSupplier _nanoClock;
   private final AtomicReference<NativeSourceClientActionTimeline.Deadline> _nextIngressActionAtNanos = new AtomicReference<>();
   private final AtomicReference<NativeSourceClientActionTimeline.Deadline> _nextIngressMoveAtNanos = new AtomicReference<>();
   private final AtomicReference<NativeSourceClientActionTimeline.Deadline> _nextVisibleMoveAtNanos = new AtomicReference<>();
   private final AtomicReference<NativeSourceClientActionTimeline.Deadline> _nextVisibleActionAtNanos = new AtomicReference<>();

   public NativeSourceClientActionTimeline() {
      this(System::nanoTime);
   }

   public NativeSourceClientActionTimeline(LongSupplier var1) {
      this._nanoClock = Objects.requireNonNull(var1, "nanoClock");
   }

   public boolean isActionBlocked() {
      return this.getActionDelayMs() > 0L;
   }

   public long getActionDelayMs() {
      synchronized (this._mutationLock) {
         NativeSourceClientActionTimeline.Deadline var2 = this.latestVisibleWindow(this._nanoClock.getAsLong());
         if (var2 == null) {
            return 0L;
         } else {
            long var3 = var2.nanos() - this._nanoClock.getAsLong();
            return var3 <= 0L ? 0L : TimeUnit.NANOSECONDS.toMillis(var3 - 1L) + 1L;
         }
      }
   }

   public NativeSourceClientActionTimeline.Reservation tryReserveCombatAction(long var1) {
      return this.tryReserve(this._nextIngressActionAtNanos, var1, NativeSourceClientActionTimeline.ActionKind.ATTACK, false);
   }

   public NativeSourceClientActionTimeline.CombatDeferral getAuthoritativeAttackDeferral(long var1) {
      if (var1 >= 0L && NativeSourcePacketExecutionContext.hasArrivalNanos()) {
         long var3 = TimeUnit.MILLISECONDS.toNanos(var1);
         long var5 = NativeSourcePacketExecutionContext.getArrivalNanos();
         synchronized (this._mutationLock) {
            long var8 = this._nanoClock.getAsLong();
            NativeSourceClientActionTimeline.Deadline var10 = this.latestVisibleWindow(var8);
            NativeSourceClientActionTimeline.Deadline var11 = this._nextIngressActionAtNanos.get();
            long var12 = 0L;
            long var14 = 0L;
            boolean var16 = false;
            if (isActiveAt(var10, var8)) {
               long var17 = var10.nanos() - var8;
               var16 = true;
               var12 = var17;
               var14 = var17;
            }

            if (isActiveAt(var11, var5)) {
               long var21 = var11.nanos() - var5;
               if (var11.kind() != NativeSourceClientActionTimeline.ActionKind.SKILL && (var3 <= 0L || var21 > var3)) {
                  return null;
               }

               var16 = true;
               var12 = Math.max(var12, var21);
               var14 = Math.max(var14, Math.max(0L, var11.nanos() - var8));
            }

            return !var16 ? null : new NativeSourceClientActionTimeline.CombatDeferral(Math.max(1L, ceilMillis(var14)), ceilMillis(var12));
         }
      } else {
         return null;
      }
   }

   public NativeSourceClientActionTimeline.ActionBlockSnapshot getActionBlockSnapshot() {
      boolean var1 = NativeSourcePacketExecutionContext.hasArrivalNanos();
      long var2 = NativeSourcePacketExecutionContext.getArrivalNanos();
      synchronized (this._mutationLock) {
         long var5 = this._nanoClock.getAsLong();
         NativeSourceClientActionTimeline.Deadline var7 = this._nextIngressActionAtNanos.get();
         NativeSourceClientActionTimeline.Deadline var8 = this.latestVisibleWindow(var5);
         long var9 = var1 ? var2 : var5;
         return new NativeSourceClientActionTimeline.ActionBlockSnapshot(
            activeKindName(var7, var9), remainingMillis(var7, var9), activeKindName(var8, var5), remainingMillis(var8, var5), var1
         );
      }
   }

   public NativeSourceClientActionTimeline.ActionBlockSnapshot getMovementBlockSnapshot() {
      boolean var1 = NativeSourcePacketExecutionContext.hasArrivalNanos();
      long var2 = NativeSourcePacketExecutionContext.getArrivalNanos();
      synchronized (this._mutationLock) {
         long var5 = this._nanoClock.getAsLong();
         NativeSourceClientActionTimeline.Deadline var7 = this._nextIngressMoveAtNanos.get();
         NativeSourceClientActionTimeline.Deadline var8 = this._nextVisibleMoveAtNanos.get();
         long var9 = var1 ? var2 : var5;
         return new NativeSourceClientActionTimeline.ActionBlockSnapshot(
            activeKindName(var7, var9), remainingMillis(var7, var9), activeKindName(var8, var5), remainingMillis(var8, var5), var1
         );
      }
   }

   private static String activeKindName(NativeSourceClientActionTimeline.Deadline var0, long var1) {
      return isActiveAt(var0, var1) ? var0.kind().name() : "NONE";
   }

   private static long remainingMillis(NativeSourceClientActionTimeline.Deadline var0, long var1) {
      return isActiveAt(var0, var1) ? ceilMillis(var0.nanos() - var1) : 0L;
   }

   public NativeSourceClientActionTimeline.Reservation tryReserveSkillAction(long var1) {
      return this.tryReserve(this._nextIngressActionAtNanos, var1, NativeSourceClientActionTimeline.ActionKind.SKILL, true);
   }

   public boolean canReserveSkillAction() {
      return this.canReserve(this._nextIngressActionAtNanos, NativeSourceClientActionTimeline.ActionKind.SKILL, true);
   }

   public NativeSourceClientActionTimeline.Reservation tryReserveMovement(long var1) {
      boolean var3 = NativeSourcePacketExecutionContext.hasArrivalNanos();
      long var4 = NativeSourcePacketExecutionContext.getArrivalNanos();
      long var6 = TimeUnit.MILLISECONDS.toNanos(resolvedDuration(var1));
      synchronized (this._mutationLock) {
         long var9 = this._nanoClock.getAsLong();
         long var11 = var3 ? var4 : var9;
         NativeSourceClientActionTimeline.Deadline var13 = this._nextIngressMoveAtNanos.get();
         NativeSourceClientActionTimeline.Deadline var14 = this._nextVisibleMoveAtNanos.get();
         if ((!var3 || var13 == null || !isAfter(var13.admittedAtNanos(), var11))
            && !exceedsMovementCredit(var13, var11, var6)
            && !exceedsMovementCredit(var14, var9, var6)) {
            NativeSourceClientActionTimeline.Deadline var15 = movementDeadline(var13, var11, var6);
            NativeSourceClientActionTimeline.Deadline var16 = movementDeadline(var14, var9, var6);
            this._nextIngressMoveAtNanos.set(var15);
            this._nextVisibleMoveAtNanos.set(var16);
            return new NativeSourceClientActionTimeline.Reservation(
               this._mutationLock, this._nextIngressMoveAtNanos, var13, var15, this._nextVisibleMoveAtNanos, var14, var16, this._nanoClock, var6
            );
         } else {
            return null;
         }
      }
   }

   private static boolean exceedsMovementCredit(NativeSourceClientActionTimeline.Deadline var0, long var1, long var3) {
      return var0 != null && var0.kind() == NativeSourceClientActionTimeline.ActionKind.MOVE && var0.nanos() - var1 > var3;
   }

   private static NativeSourceClientActionTimeline.Deadline movementDeadline(NativeSourceClientActionTimeline.Deadline var0, long var1, long var3) {
      long var5 = var0 != null && var0.kind() == NativeSourceClientActionTimeline.ActionKind.MOVE && isAfter(var0.nanos(), var1) ? var0.nanos() : var1;
      return new NativeSourceClientActionTimeline.Deadline(addDuration(var5, var3), NativeSourceClientActionTimeline.ActionKind.MOVE, var1);
   }

   private NativeSourceClientActionTimeline.Reservation tryReserve(
      AtomicReference<NativeSourceClientActionTimeline.Deadline> var1, long var2, NativeSourceClientActionTimeline.ActionKind var4, boolean var5
   ) {
      long var6 = NativeSourcePacketExecutionContext.getArrivalNanos();
      boolean var8 = NativeSourcePacketExecutionContext.hasArrivalNanos();
      long var9 = TimeUnit.MILLISECONDS.toNanos(resolvedDuration(var2));
      synchronized (this._mutationLock) {
         long var12 = this._nanoClock.getAsLong();
         long var14 = var8 ? var6 : var12;
         AtomicReference var16 = var4 == NativeSourceClientActionTimeline.ActionKind.MOVE ? this._nextVisibleMoveAtNanos : this._nextVisibleActionAtNanos;
         NativeSourceClientActionTimeline.Deadline var17 = var4 == NativeSourceClientActionTimeline.ActionKind.MOVE
            ? this._nextVisibleActionAtNanos.get()
            : this._nextVisibleMoveAtNanos.get();
         if (isActiveAt(var17, var12) && var4 == NativeSourceClientActionTimeline.ActionKind.ATTACK) {
            return null;
         } else if (isStaleSkillPacket(var8, var4, var14, var12, var9)) {
            STALE_SKILL_REJECT_COUNT.incrementAndGet();
            return null;
         } else {
            NativeSourceClientActionTimeline.Deadline var18 = (NativeSourceClientActionTimeline.Deadline)var16.get();
            if (conflictsWithSkillAtIngress(var8, var4, var14, var18)) {
               recordSkillConflict(var4, var18);
               return null;
            } else {
               NativeSourceClientActionTimeline.Deadline var19 = new NativeSourceClientActionTimeline.Deadline(addDuration(var14, var9), var4);

               NativeSourceClientActionTimeline.Deadline var20;
               boolean var21;
               NativeSourceClientActionTimeline.Deadline var22;
               do {
                  var20 = (NativeSourceClientActionTimeline.Deadline)var16.get();
                  var21 = isActiveAt(var20, var12);
                  if (var21 && !canInterrupt(var4, var20, var5)) {
                     recordSkillConflict(var4, var20);
                     return null;
                  }

                  var22 = (NativeSourceClientActionTimeline.Deadline)var1.get();
                  boolean var23 = isActiveAt(var22, var14);
                  if (var23 && !canInterrupt(var4, var22, var5)) {
                     recordSkillConflict(var4, var22);
                     return null;
                  }
               } while (!var1.compareAndSet(var22, var19));

               NativeSourceClientActionTimeline.VisibleWindow var24 = this.replaceVisibleWindow(var16, var20, var9, var12, var4);
               if (var24 == null) {
                  var1.compareAndSet(var19, var22);
                  return null;
               } else {
                  if (var4 == NativeSourceClientActionTimeline.ActionKind.SKILL) {
                     if (var21) {
                        recordSkillInterruption(var20.kind());
                     } else if (isActiveAt(var17, var12)) {
                        recordSkillInterruption(var17.kind());
                     }
                  }

                  return new NativeSourceClientActionTimeline.Reservation(
                     this._mutationLock, var1, var22, var19, var16, var24.previous(), var24.reservedUntil(), this._nanoClock, var9
                  );
               }
            }
         }
      }
   }

   public void reserveAction(long var1) {
      long var3 = TimeUnit.MILLISECONDS.toNanos(resolvedDuration(var1));
      synchronized (this._mutationLock) {
         long var6 = this._nanoClock.getAsLong();
         NativeSourceClientActionTimeline.Deadline var8 = new NativeSourceClientActionTimeline.Deadline(
            addDuration(resolveActionTimeNanos(var6), var3), NativeSourceClientActionTimeline.ActionKind.ATTACK
         );
         maxSet(this._nextIngressActionAtNanos, var8);
         this.reserveVisibleWindow(var3, var6, NativeSourceClientActionTimeline.ActionKind.ATTACK);
      }
   }

   public void clear() {
      synchronized (this._mutationLock) {
         this._nextIngressActionAtNanos.set(null);
         this._nextIngressMoveAtNanos.set(null);
         this._nextVisibleActionAtNanos.set(null);
         this._nextVisibleMoveAtNanos.set(null);
      }
   }

   private static long resolvedDuration(long var0) {
      return Math.max(0L, var0);
   }

   private static long resolveActionTimeNanos(long var0) {
      long var2 = NativeSourcePacketExecutionContext.getArrivalNanos();
      return NativeSourcePacketExecutionContext.hasArrivalNanos() ? var2 : var0;
   }

   private NativeSourceClientActionTimeline.Deadline latestVisibleWindow(long var1) {
      NativeSourceClientActionTimeline.Deadline var3 = this._nextVisibleActionAtNanos.get();
      NativeSourceClientActionTimeline.Deadline var4 = this._nextVisibleMoveAtNanos.get();
      return !isActiveAt(var4, var1) || isActiveAt(var3, var1) && !isAfter(var4.nanos(), var3.nanos()) ? var3 : var4;
   }

   private NativeSourceClientActionTimeline.VisibleWindow replaceVisibleWindow(
      AtomicReference<NativeSourceClientActionTimeline.Deadline> var1,
      NativeSourceClientActionTimeline.Deadline var2,
      long var3,
      long var5,
      NativeSourceClientActionTimeline.ActionKind var7
   ) {
      NativeSourceClientActionTimeline.Deadline var8 = new NativeSourceClientActionTimeline.Deadline(addDuration(var5, var3), var7);
      return var1.compareAndSet(var2, var8) ? new NativeSourceClientActionTimeline.VisibleWindow(var2, var8) : null;
   }

   private NativeSourceClientActionTimeline.VisibleWindow reserveVisibleWindow(long var1, long var3, NativeSourceClientActionTimeline.ActionKind var5) {
      NativeSourceClientActionTimeline.Deadline var6;
      NativeSourceClientActionTimeline.Deadline var11;
      do {
         var6 = this._nextVisibleActionAtNanos.get();
         long var7 = var6 == null ? 0L : Math.max(0L, var6.nanos() - var3);
         long var9 = var7 + var1;
         var11 = new NativeSourceClientActionTimeline.Deadline(addDuration(var3, var9), var5);
      } while (!this._nextVisibleActionAtNanos.compareAndSet(var6, var11));

      return new NativeSourceClientActionTimeline.VisibleWindow(var6, var11);
   }

   private static void maxSet(AtomicReference<NativeSourceClientActionTimeline.Deadline> var0, NativeSourceClientActionTimeline.Deadline var1) {
      boolean var2;
      do {
         NativeSourceClientActionTimeline.Deadline var3 = (NativeSourceClientActionTimeline.Deadline)var0.get();
         var2 = var3 != null && !isAfter(var1.nanos(), var3.nanos());
         if (!var2) {
            var2 = var0.compareAndSet(var3, var1);
         }
      } while (!var2);
   }

   private static boolean isAfter(long var0, long var2) {
      return var0 - var2 > 0L;
   }

   private boolean canReserve(AtomicReference<NativeSourceClientActionTimeline.Deadline> var1, NativeSourceClientActionTimeline.ActionKind var2, boolean var3) {
      long var4 = NativeSourcePacketExecutionContext.getArrivalNanos();
      boolean var6 = NativeSourcePacketExecutionContext.hasArrivalNanos();
      synchronized (this._mutationLock) {
         long var8 = this._nanoClock.getAsLong();
         long var10 = var6 ? var4 : var8;
         NativeSourceClientActionTimeline.Deadline var12 = this._nextVisibleActionAtNanos.get();
         if (conflictsWithSkillAtIngress(var6, var2, var10, var12)) {
            recordSkillConflict(var2, var12);
            return false;
         } else if (isActiveAt(var12, var8) && !canInterrupt(var2, var12, var3)) {
            recordSkillConflict(var2, var12);
            return false;
         } else {
            NativeSourceClientActionTimeline.Deadline var13 = (NativeSourceClientActionTimeline.Deadline)var1.get();
            if (isActiveAt(var13, var10) && !canInterrupt(var2, var13, var3)) {
               recordSkillConflict(var2, var13);
               return false;
            } else {
               return true;
            }
         }
      }
   }

   private static boolean isActiveAt(NativeSourceClientActionTimeline.Deadline var0, long var1) {
      return var0 != null && isAfter(var0.nanos(), var1);
   }

   private static long ceilMillis(long var0) {
      if (var0 <= 0L) {
         return 0L;
      }
      return TimeUnit.NANOSECONDS.toMillis(var0 - 1L) + 1L;
   }

   private static boolean canInterrupt(NativeSourceClientActionTimeline.ActionKind var0, NativeSourceClientActionTimeline.Deadline var1, boolean var2) {
      return var2
         && var0 == NativeSourceClientActionTimeline.ActionKind.SKILL
         && var1 != null
         && (var1.kind() == NativeSourceClientActionTimeline.ActionKind.ATTACK || var1.kind() == NativeSourceClientActionTimeline.ActionKind.MOVE);
   }

   private static boolean isStaleSkillPacket(boolean var0, NativeSourceClientActionTimeline.ActionKind var1, long var2, long var4, long var6) {
      return var0 && var1 == NativeSourceClientActionTimeline.ActionKind.SKILL && var6 > 0L && var4 - var2 >= var6;
   }

   private static boolean conflictsWithSkillAtIngress(
      boolean var0, NativeSourceClientActionTimeline.ActionKind var1, long var2, NativeSourceClientActionTimeline.Deadline var4
   ) {
      return var0
         && var1 == NativeSourceClientActionTimeline.ActionKind.SKILL
         && var4 != null
         && var4.kind() == NativeSourceClientActionTimeline.ActionKind.SKILL
         && isActiveAt(var4, var2);
   }

   private static void recordSkillInterruption(NativeSourceClientActionTimeline.ActionKind var0) {
      if (var0 == NativeSourceClientActionTimeline.ActionKind.ATTACK) {
         SKILL_INTERRUPTED_ATTACK_COUNT.incrementAndGet();
      } else if (var0 == NativeSourceClientActionTimeline.ActionKind.MOVE) {
         SKILL_INTERRUPTED_MOVE_COUNT.incrementAndGet();
      }
   }

   private static void recordSkillConflict(NativeSourceClientActionTimeline.ActionKind var0, NativeSourceClientActionTimeline.Deadline var1) {
      if (var0 == NativeSourceClientActionTimeline.ActionKind.SKILL && var1 != null && var1.kind() == NativeSourceClientActionTimeline.ActionKind.SKILL) {
         SKILL_CONFLICT_REJECT_COUNT.incrementAndGet();
      }
   }

   public static long getSkillInterruptedAttackCount() {
      return SKILL_INTERRUPTED_ATTACK_COUNT.get();
   }

   public static long getSkillInterruptedMoveCount() {
      return SKILL_INTERRUPTED_MOVE_COUNT.get();
   }

   public static long getSkillConflictRejectCount() {
      return SKILL_CONFLICT_REJECT_COUNT.get();
   }

   public static long getStaleSkillRejectCount() {
      return STALE_SKILL_REJECT_COUNT.get();
   }

   private static long addDuration(long var0, long var2) {
      return var0 + var2;
   }

   public record ActionBlockSnapshot(String ingressKind, long ingressRemainingMs, String visibleKind, long visibleRemainingMs, boolean packetArrivalAvailable) {
   }

   private static enum ActionKind {
      MOVE,
      ATTACK,
      SKILL;
   }

   public record CombatDeferral(long delayMs, long earlyMs) {
   }

   private record Deadline(long nanos, NativeSourceClientActionTimeline.ActionKind kind, long admittedAtNanos) {
      private Deadline(long var1, NativeSourceClientActionTimeline.ActionKind var3) {
         this(var1, var3, var1);
      }
   }

   public static final class Reservation {
      private final Object _mutationLock;
      private final AtomicReference<NativeSourceClientActionTimeline.Deadline> _ingressTimeline;
      private final NativeSourceClientActionTimeline.Deadline _previousIngress;
      private final NativeSourceClientActionTimeline.Deadline _reservedIngressUntil;
      private final AtomicReference<NativeSourceClientActionTimeline.Deadline> _visibleTimeline;
      private final NativeSourceClientActionTimeline.Deadline _previousVisible;
      private NativeSourceClientActionTimeline.Deadline _reservedVisibleUntil;
      private final LongSupplier _nanoClock;
      private final long _durationNanos;
      private final AtomicBoolean _cancelOpen = new AtomicBoolean(true);

      private Reservation(
         Object var1,
         AtomicReference<NativeSourceClientActionTimeline.Deadline> var2,
         NativeSourceClientActionTimeline.Deadline var3,
         NativeSourceClientActionTimeline.Deadline var4,
         AtomicReference<NativeSourceClientActionTimeline.Deadline> var5,
         NativeSourceClientActionTimeline.Deadline var6,
         NativeSourceClientActionTimeline.Deadline var7,
         LongSupplier var8,
         long var9
      ) {
         this._mutationLock = var1;
         this._ingressTimeline = var2;
         this._previousIngress = var3;
         this._reservedIngressUntil = var4;
         this._visibleTimeline = var5;
         this._previousVisible = var6;
         this._reservedVisibleUntil = var7;
         this._nanoClock = var8;
         this._durationNanos = var9;
      }

      public boolean commitAtRuntime() {
         synchronized (this._mutationLock) {
            if (!this._cancelOpen.get()) {
               return false;
            } else if (this._ingressTimeline.get() == this._reservedIngressUntil && this._visibleTimeline.get() == this._reservedVisibleUntil) {
               long var2 = NativeSourceClientActionTimeline.addDuration(this._nanoClock.getAsLong(), this._durationNanos);
               long var4 = this._reservedVisibleUntil.kind() == NativeSourceClientActionTimeline.ActionKind.MOVE
                     && NativeSourceClientActionTimeline.isAfter(this._reservedVisibleUntil.nanos(), var2)
                  ? this._reservedVisibleUntil.nanos()
                  : var2;
               NativeSourceClientActionTimeline.Deadline var6 = new NativeSourceClientActionTimeline.Deadline(
                  var4, this._reservedVisibleUntil.kind(), this._reservedVisibleUntil.admittedAtNanos()
               );
               this._visibleTimeline.set(var6);
               this._reservedVisibleUntil = var6;
               this._cancelOpen.set(false);
               return true;
            } else {
               this._cancelOpen.set(false);
               return false;
            }
         }
      }

      public boolean cancel() {
         synchronized (this._mutationLock) {
            if (!this._cancelOpen.compareAndSet(true, false)) {
               return false;
            } else if (this._ingressTimeline.get() == this._reservedIngressUntil && this._visibleTimeline.get() == this._reservedVisibleUntil) {
               this._ingressTimeline.set(this._previousIngress);
               this._visibleTimeline.set(this._previousVisible);
               return true;
            } else {
               return false;
            }
         }
      }
   }

   private record VisibleWindow(NativeSourceClientActionTimeline.Deadline previous, NativeSourceClientActionTimeline.Deadline reservedUntil) {
   }
}
