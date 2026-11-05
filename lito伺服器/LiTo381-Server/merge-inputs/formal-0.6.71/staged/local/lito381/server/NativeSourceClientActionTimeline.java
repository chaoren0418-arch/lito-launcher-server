// DECOMPILED from base-0.6.71.jar, TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;
import local.lito381.server.NativeSourcePacketExecutionContext;

public final class NativeSourceClientActionTimeline {
    private static final AtomicLong SKILL_INTERRUPTED_ATTACK_COUNT = new AtomicLong();
    private static final AtomicLong SKILL_INTERRUPTED_MOVE_COUNT = new AtomicLong();
    private static final AtomicLong SKILL_CONFLICT_REJECT_COUNT = new AtomicLong();
    private static final AtomicLong STALE_SKILL_REJECT_COUNT = new AtomicLong();
    private final Object _mutationLock = new Object();
    private final LongSupplier _nanoClock;
    private final AtomicReference<Deadline> _nextIngressActionAtNanos = new AtomicReference();
    private final AtomicReference<Deadline> _nextIngressMoveAtNanos = new AtomicReference();
    private final AtomicReference<Deadline> _nextVisibleMoveAtNanos = new AtomicReference();
    private final AtomicReference<Deadline> _nextVisibleActionAtNanos = new AtomicReference();

    public NativeSourceClientActionTimeline() {
        this(System::nanoTime);
    }

    public NativeSourceClientActionTimeline(LongSupplier longSupplier) {
        this._nanoClock = Objects.requireNonNull(longSupplier, "nanoClock");
    }

    public boolean isActionBlocked() {
        return this.getActionDelayMs() > 0L;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public long getActionDelayMs() {
        synchronized (this._mutationLock) {
            Deadline deadline = this.latestVisibleWindow(this._nanoClock.getAsLong());
            if (deadline == null) {
                return 0L;
            }
            long l = deadline.nanos() - this._nanoClock.getAsLong();
            if (l <= 0L) {
                return 0L;
            }
            return TimeUnit.NANOSECONDS.toMillis(l - 1L) + 1L;
        }
    }

    public Reservation tryReserveCombatAction(long l) {
        return this.tryReserve(this._nextIngressActionAtNanos, l, ActionKind.ATTACK, false);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public CombatDeferral getAuthoritativeAttackDeferral(long l) {
        if (l < 0L || !NativeSourcePacketExecutionContext.hasArrivalNanos()) {
            return null;
        }
        long l2 = TimeUnit.MILLISECONDS.toNanos(l);
        long l3 = NativeSourcePacketExecutionContext.getArrivalNanos();
        synchronized (this._mutationLock) {
            long l5 = this._nanoClock.getAsLong();
            Deadline deadline = this.latestVisibleWindow(l5);
            Deadline deadline2 = this._nextIngressActionAtNanos.get();
            long l6 = 0L;
            long l7 = 0L;
            boolean bl = false;
            if (NativeSourceClientActionTimeline.isActiveAt(deadline, l5)) {
                long l4 = deadline.nanos() - l5;
                bl = true;
                l6 = l4;
                l7 = l4;
            }
            if (NativeSourceClientActionTimeline.isActiveAt(deadline2, l3)) {
                long l4 = deadline2.nanos() - l3;
                if (deadline2.kind() != ActionKind.SKILL && (l2 <= 0L || l4 > l2)) {
                    return null;
                }
                bl = true;
                l6 = Math.max(l6, l4);
                l7 = Math.max(l7, Math.max(0L, deadline2.nanos() - l5));
            }
            if (!bl) {
                return null;
            }
            return new CombatDeferral(Math.max(1L, NativeSourceClientActionTimeline.ceilMillis(l7)), NativeSourceClientActionTimeline.ceilMillis(l6));
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
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

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
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

    private static String activeKindName(Deadline deadline, long l) {
        return NativeSourceClientActionTimeline.isActiveAt(deadline, l) ? deadline.kind().name() : "NONE";
    }

    private static long remainingMillis(Deadline deadline, long l) {
        return NativeSourceClientActionTimeline.isActiveAt(deadline, l) ? NativeSourceClientActionTimeline.ceilMillis(deadline.nanos() - l) : 0L;
    }

    public Reservation tryReserveSkillAction(long l) {
        return this.tryReserve(this._nextIngressActionAtNanos, l, ActionKind.SKILL, true);
    }

    public boolean canReserveSkillAction() {
        return this.canReserve(this._nextIngressActionAtNanos, ActionKind.SKILL, true);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public Reservation tryReserveMovement(long l) {
        boolean bl = NativeSourcePacketExecutionContext.hasArrivalNanos();
        long l2 = NativeSourcePacketExecutionContext.getArrivalNanos();
        long l3 = TimeUnit.MILLISECONDS.toNanos(NativeSourceClientActionTimeline.resolvedDuration(l));
        synchronized (this._mutationLock) {
            long l4 = this._nanoClock.getAsLong();
            long l5 = bl ? l2 : l4;
            Deadline deadline = this._nextIngressMoveAtNanos.get();
            Deadline deadline2 = this._nextVisibleMoveAtNanos.get();
            if (bl && deadline != null && NativeSourceClientActionTimeline.isAfter(deadline.admittedAtNanos(), l5) || NativeSourceClientActionTimeline.exceedsMovementCredit(deadline, l5, l3) || NativeSourceClientActionTimeline.exceedsMovementCredit(deadline2, l4, l3)) {
                return null;
            }
            Deadline deadline3 = NativeSourceClientActionTimeline.movementDeadline(deadline, l5, l3);
            Deadline deadline4 = NativeSourceClientActionTimeline.movementDeadline(deadline2, l4, l3);
            this._nextIngressMoveAtNanos.set(deadline3);
            this._nextVisibleMoveAtNanos.set(deadline4);
            return new Reservation(this._mutationLock, this._nextIngressMoveAtNanos, deadline, deadline3, this._nextVisibleMoveAtNanos, deadline2, deadline4, this._nanoClock, l3);
        }
    }

    private static boolean exceedsMovementCredit(Deadline deadline, long l, long l2) {
        return deadline != null && deadline.kind() == ActionKind.MOVE && deadline.nanos() - l > l2;
    }

    private static Deadline movementDeadline(Deadline deadline, long l, long l2) {
        long l3 = deadline != null && deadline.kind() == ActionKind.MOVE && NativeSourceClientActionTimeline.isAfter(deadline.nanos(), l) ? deadline.nanos() : l;
        return new Deadline(NativeSourceClientActionTimeline.addDuration(l3, l2), ActionKind.MOVE, l);
    }

    private Reservation tryReserve(AtomicReference<Deadline> ingress, long durationMillis, ActionKind kind, boolean allowInterrupt) {
        long arrival = NativeSourcePacketExecutionContext.getArrivalNanos();
        boolean hasArrival = NativeSourcePacketExecutionContext.hasArrivalNanos();
        long duration = TimeUnit.MILLISECONDS.toNanos(resolvedDuration(durationMillis));
        synchronized (this._mutationLock) {
            long now = this._nanoClock.getAsLong();
            long actionTime = hasArrival ? arrival : now;
            AtomicReference<Deadline> visible = kind == ActionKind.MOVE ? this._nextVisibleMoveAtNanos : this._nextVisibleActionAtNanos;
            Deadline other = kind == ActionKind.MOVE ? this._nextVisibleActionAtNanos.get() : this._nextVisibleMoveAtNanos.get();
            if (isActiveAt(other, now) && kind == ActionKind.ATTACK) return null;
            if (isStaleSkillPacket(hasArrival, kind, actionTime, now, duration)) {
                STALE_SKILL_REJECT_COUNT.incrementAndGet();
                return null;
            }
            Deadline initialVisible = visible.get();
            if (conflictsWithSkillAtIngress(hasArrival, kind, actionTime, initialVisible)) {
                recordSkillConflict(kind, initialVisible);
                return null;
            }
            Deadline next = new Deadline(addDuration(actionTime, duration), kind);
            while (true) {
                Deadline previousVisible = visible.get();
                boolean visibleActive = isActiveAt(previousVisible, now);
                if (visibleActive && !canInterrupt(kind, previousVisible, allowInterrupt)) {
                    recordSkillConflict(kind, previousVisible);
                    return null;
                }
                Deadline previous = ingress.get();
                boolean ingressActive = isActiveAt(previous, actionTime);
                if (ingressActive && !canInterrupt(kind, previous, allowInterrupt)) {
                    recordSkillConflict(kind, previous);
                    return null;
                }
                if (ingress.compareAndSet(previous, next)) {
                    VisibleWindow window = this.replaceVisibleWindow(visible, previousVisible, duration, now, kind);
                    if (window == null) {
                        ingress.compareAndSet(next, previous);
                        return null;
                    }
                    if (kind == ActionKind.SKILL) {
                        if (visibleActive) recordSkillInterruption(previousVisible.kind());
                        else if (isActiveAt(other, now)) recordSkillInterruption(other.kind());
                    }
                    return new Reservation(this._mutationLock, ingress, previous, next, visible, window.previous(), window.reservedUntil(), this._nanoClock, duration);
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
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

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
   public void clear() {
      synchronized (this._mutationLock) {
         this._nextIngressActionAtNanos.set(null);
         this._nextIngressMoveAtNanos.set(null);
         this._nextVisibleActionAtNanos.set(null);
         this._nextVisibleMoveAtNanos.set(null);
      }
   }

    private static long resolvedDuration(long l) {
        return Math.max(0L, l);
    }

    private static long resolveActionTimeNanos(long l) {
        long l2 = NativeSourcePacketExecutionContext.getArrivalNanos();
        return NativeSourcePacketExecutionContext.hasArrivalNanos() ? l2 : l;
    }

    private Deadline latestVisibleWindow(long l) {
        Deadline deadline = this._nextVisibleActionAtNanos.get();
        Deadline deadline2 = this._nextVisibleMoveAtNanos.get();
        return NativeSourceClientActionTimeline.isActiveAt(deadline2, l) && (!NativeSourceClientActionTimeline.isActiveAt(deadline, l) || NativeSourceClientActionTimeline.isAfter(deadline2.nanos(), deadline.nanos())) ? deadline2 : deadline;
    }

    private VisibleWindow replaceVisibleWindow(AtomicReference<Deadline> atomicReference, Deadline deadline, long l, long l2, ActionKind actionKind) {
        Deadline deadline2 = new Deadline(NativeSourceClientActionTimeline.addDuration(l2, l), actionKind);
        return atomicReference.compareAndSet(deadline, deadline2) ? new VisibleWindow(deadline, deadline2) : null;
    }

    private VisibleWindow reserveVisibleWindow(long duration, long now, ActionKind kind) {
        while (true) {
            Deadline previous = this._nextVisibleActionAtNanos.get();
            long remaining = previous == null ? 0L : Math.max(0L, previous.nanos() - now);
            long total = remaining + duration;
            Deadline reserved = new Deadline(addDuration(now, total), kind);
            if (this._nextVisibleActionAtNanos.compareAndSet(previous, reserved)) {
                return new VisibleWindow(previous, reserved);
            }
        }
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

    private static boolean isAfter(long l, long l2) {
        return l - l2 > 0L;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
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

    private static boolean isActiveAt(Deadline deadline, long l) {
        return deadline != null && NativeSourceClientActionTimeline.isAfter(deadline.nanos(), l);
    }

    private static long ceilMillis(long l) {
        if (l <= 0L) {
            return 0L;
        }
        return TimeUnit.NANOSECONDS.toMillis(l - 1L) + 1L;
    }

    private static boolean canInterrupt(ActionKind actionKind, Deadline deadline, boolean bl) {
        return bl && actionKind == ActionKind.SKILL && deadline != null && (deadline.kind() == ActionKind.ATTACK || deadline.kind() == ActionKind.MOVE);
    }

    private static boolean isStaleSkillPacket(boolean bl, ActionKind actionKind, long l, long l2, long l3) {
        return bl && actionKind == ActionKind.SKILL && l3 > 0L && l2 - l >= l3;
    }

    private static boolean conflictsWithSkillAtIngress(boolean bl, ActionKind actionKind, long l, Deadline deadline) {
        return bl && actionKind == ActionKind.SKILL && deadline != null && deadline.kind() == ActionKind.SKILL && NativeSourceClientActionTimeline.isActiveAt(deadline, l);
    }

    private static void recordSkillInterruption(ActionKind actionKind) {
        if (actionKind == ActionKind.ATTACK) {
            SKILL_INTERRUPTED_ATTACK_COUNT.incrementAndGet();
        } else if (actionKind == ActionKind.MOVE) {
            SKILL_INTERRUPTED_MOVE_COUNT.incrementAndGet();
        }
    }

    private static void recordSkillConflict(ActionKind actionKind, Deadline deadline) {
        if (actionKind == ActionKind.SKILL && deadline != null && deadline.kind() == ActionKind.SKILL) {
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

    private static long addDuration(long l, long l2) {
        return l + l2;
    }

    private record Deadline(long nanos, ActionKind kind, long admittedAtNanos) {
        private Deadline(long l, ActionKind actionKind) {
            this(l, actionKind, l);
        }
    }

    private static enum ActionKind {
        MOVE,
        ATTACK,
        SKILL;

    }

    public static final class Reservation {
        private final Object _mutationLock;
        private final AtomicReference<Deadline> _ingressTimeline;
        private final Deadline _previousIngress;
        private final Deadline _reservedIngressUntil;
        private final AtomicReference<Deadline> _visibleTimeline;
        private final Deadline _previousVisible;
        private Deadline _reservedVisibleUntil;
        private final LongSupplier _nanoClock;
        private final long _durationNanos;
        private final AtomicBoolean _cancelOpen = new AtomicBoolean(true);

        private Reservation(Object object, AtomicReference<Deadline> atomicReference, Deadline deadline, Deadline deadline2, AtomicReference<Deadline> atomicReference2, Deadline deadline3, Deadline deadline4, LongSupplier longSupplier, long l) {
            this._mutationLock = object;
            this._ingressTimeline = atomicReference;
            this._previousIngress = deadline;
            this._reservedIngressUntil = deadline2;
            this._visibleTimeline = atomicReference2;
            this._previousVisible = deadline3;
            this._reservedVisibleUntil = deadline4;
            this._nanoClock = longSupplier;
            this._durationNanos = l;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        public boolean commitAtRuntime() {
            synchronized (this._mutationLock) {
                if (!this._cancelOpen.get()) {
                    return false;
                }
                if (this._ingressTimeline.get() != this._reservedIngressUntil || this._visibleTimeline.get() != this._reservedVisibleUntil) {
                    this._cancelOpen.set(false);
                    return false;
                }
                long l = NativeSourceClientActionTimeline.addDuration(this._nanoClock.getAsLong(), this._durationNanos);
                long l2 = this._reservedVisibleUntil.kind() == ActionKind.MOVE && NativeSourceClientActionTimeline.isAfter(this._reservedVisibleUntil.nanos(), l) ? this._reservedVisibleUntil.nanos() : l;
                Deadline deadline = new Deadline(l2, this._reservedVisibleUntil.kind(), this._reservedVisibleUntil.admittedAtNanos());
                this._visibleTimeline.set(deadline);
                this._reservedVisibleUntil = deadline;
                this._cancelOpen.set(false);
                return true;
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        public boolean cancel() {
            synchronized (this._mutationLock) {
                if (!this._cancelOpen.compareAndSet(true, false)) {
                    return false;
                }
                if (this._ingressTimeline.get() != this._reservedIngressUntil || this._visibleTimeline.get() != this._reservedVisibleUntil) {
                    return false;
                }
                this._ingressTimeline.set(this._previousIngress);
                this._visibleTimeline.set(this._previousVisible);
                return true;
            }
        }
    }

    public record CombatDeferral(long delayMs, long earlyMs) {
    }

    public record ActionBlockSnapshot(String ingressKind, long ingressRemainingMs, String visibleKind, long visibleRemainingMs, boolean packetArrivalAvailable) {
    }

    private record VisibleWindow(Deadline previous, Deadline reservedUntil) {
    }
}
