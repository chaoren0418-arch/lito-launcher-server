// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
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
        Object object = this._mutationLock;
        synchronized (object) {
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
        Object object = this._mutationLock;
        synchronized (object) {
            long l4;
            long l5 = this._nanoClock.getAsLong();
            Deadline deadline = this.latestVisibleWindow(l5);
            Deadline deadline2 = this._nextIngressActionAtNanos.get();
            long l6 = 0L;
            long l7 = 0L;
            boolean bl = false;
            if (NativeSourceClientActionTimeline.isActiveAt(deadline, l5)) {
                l4 = deadline.nanos() - l5;
                bl = true;
                l6 = l4;
                l7 = l4;
            }
            if (NativeSourceClientActionTimeline.isActiveAt(deadline2, l3)) {
                l4 = deadline2.nanos() - l3;
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
    public ActionBlockSnapshot getActionBlockSnapshot() {
        boolean bl = NativeSourcePacketExecutionContext.hasArrivalNanos();
        long l = NativeSourcePacketExecutionContext.getArrivalNanos();
        Object object = this._mutationLock;
        synchronized (object) {
            long l2 = this._nanoClock.getAsLong();
            Deadline deadline = this._nextIngressActionAtNanos.get();
            Deadline deadline2 = this.latestVisibleWindow(l2);
            long l3 = bl ? l : l2;
            return new ActionBlockSnapshot(NativeSourceClientActionTimeline.activeKindName(deadline, l3), NativeSourceClientActionTimeline.remainingMillis(deadline, l3), NativeSourceClientActionTimeline.activeKindName(deadline2, l2), NativeSourceClientActionTimeline.remainingMillis(deadline2, l2), bl);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public ActionBlockSnapshot getMovementBlockSnapshot() {
        boolean bl = NativeSourcePacketExecutionContext.hasArrivalNanos();
        long l = NativeSourcePacketExecutionContext.getArrivalNanos();
        Object object = this._mutationLock;
        synchronized (object) {
            long l2 = this._nanoClock.getAsLong();
            Deadline deadline = this._nextIngressMoveAtNanos.get();
            Deadline deadline2 = this._nextVisibleMoveAtNanos.get();
            long l3 = bl ? l : l2;
            return new ActionBlockSnapshot(NativeSourceClientActionTimeline.activeKindName(deadline, l3), NativeSourceClientActionTimeline.remainingMillis(deadline, l3), NativeSourceClientActionTimeline.activeKindName(deadline2, l2), NativeSourceClientActionTimeline.remainingMillis(deadline2, l2), bl);
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
        Object object = this._mutationLock;
        synchronized (object) {
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

    private Reservation tryReserve(AtomicReference<Deadline> atomicReference, long l, ActionKind actionKind, boolean bl) {
        long l2 = NativeSourcePacketExecutionContext.getArrivalNanos();
        boolean bl2 = NativeSourcePacketExecutionContext.hasArrivalNanos();
        long l3 = TimeUnit.MILLISECONDS.toNanos(NativeSourceClientActionTimeline.resolvedDuration(l));
        Object object = this._mutationLock;
        synchronized (object) {
            Deadline deadline;
            boolean bl3;
            Deadline deadline2;
            Deadline deadline3;
            long l4 = this._nanoClock.getAsLong();
            long l5 = bl2 ? l2 : l4;
            AtomicReference<Deadline> atomicReference2 = actionKind == ActionKind.MOVE ? this._nextVisibleMoveAtNanos : this._nextVisibleActionAtNanos;
            Deadline deadline4 = deadline3 = actionKind == ActionKind.MOVE ? this._nextVisibleActionAtNanos.get() : this._nextVisibleMoveAtNanos.get();
            if (NativeSourceClientActionTimeline.isActiveAt(deadline3, l4) && actionKind == ActionKind.ATTACK) {
                return null;
            }
            if (NativeSourceClientActionTimeline.isStaleSkillPacket(bl2, actionKind, l5, l4, l3)) {
                STALE_SKILL_REJECT_COUNT.incrementAndGet();
                return null;
            }
            Deadline deadline5 = atomicReference2.get();
            if (NativeSourceClientActionTimeline.conflictsWithSkillAtIngress(bl2, actionKind, l5, deadline5)) {
                NativeSourceClientActionTimeline.recordSkillConflict(actionKind, deadline5);
                return null;
            }
            Deadline deadline6 = new Deadline(NativeSourceClientActionTimeline.addDuration(l5, l3), actionKind);
            do {
                if ((bl3 = NativeSourceClientActionTimeline.isActiveAt(deadline = atomicReference2.get(), l4)) && !NativeSourceClientActionTimeline.canInterrupt(actionKind, deadline, bl)) {
                    NativeSourceClientActionTimeline.recordSkillConflict(actionKind, deadline);
                    return null;
                }
                deadline2 = atomicReference.get();
                boolean bl4 = NativeSourceClientActionTimeline.isActiveAt(deadline2, l5);
                if (!bl4 || NativeSourceClientActionTimeline.canInterrupt(actionKind, deadline2, bl)) continue;
                NativeSourceClientActionTimeline.recordSkillConflict(actionKind, deadline2);
                return null;
            } while (!atomicReference.compareAndSet(deadline2, deadline6));
            VisibleWindow visibleWindow = this.replaceVisibleWindow(atomicReference2, deadline, l3, l4, actionKind);
            if (visibleWindow == null) {
                atomicReference.compareAndSet(deadline6, deadline2);
                return null;
            }
            if (actionKind == ActionKind.SKILL) {
                if (bl3) {
                    NativeSourceClientActionTimeline.recordSkillInterruption(deadline.kind());
                } else if (NativeSourceClientActionTimeline.isActiveAt(deadline3, l4)) {
                    NativeSourceClientActionTimeline.recordSkillInterruption(deadline3.kind());
                }
            }
            return new Reservation(this._mutationLock, atomicReference, deadline2, deadline6, atomicReference2, visibleWindow.previous(), visibleWindow.reservedUntil(), this._nanoClock, l3);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void reserveAction(long l) {
        long l2 = TimeUnit.MILLISECONDS.toNanos(NativeSourceClientActionTimeline.resolvedDuration(l));
        Object object = this._mutationLock;
        synchronized (object) {
            long l3 = this._nanoClock.getAsLong();
            Deadline deadline = new Deadline(NativeSourceClientActionTimeline.addDuration(NativeSourceClientActionTimeline.resolveActionTimeNanos(l3), l2), ActionKind.ATTACK);
            NativeSourceClientActionTimeline.maxSet(this._nextIngressActionAtNanos, deadline);
            this.reserveVisibleWindow(l2, l3, ActionKind.ATTACK);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void clear() {
        Object object = this._mutationLock;
        synchronized (object) {
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

    private VisibleWindow reserveVisibleWindow(long l, long l2, ActionKind actionKind) {
        Deadline deadline;
        Deadline deadline2;
        do {
            deadline2 = this._nextVisibleActionAtNanos.get();
            long l3 = deadline2 == null ? 0L : Math.max(0L, deadline2.nanos() - l2);
            long l4 = l3 + l;
            deadline = new Deadline(NativeSourceClientActionTimeline.addDuration(l2, l4), actionKind);
        } while (!this._nextVisibleActionAtNanos.compareAndSet(deadline2, deadline));
        return new VisibleWindow(deadline2, deadline);
    }

    private static void maxSet(AtomicReference<Deadline> atomicReference, Deadline deadline) {
        boolean bl;
        do {
            Deadline deadline2;
            boolean bl2 = bl = (deadline2 = atomicReference.get()) != null && !NativeSourceClientActionTimeline.isAfter(deadline.nanos(), deadline2.nanos());
            if (bl) continue;
            bl = atomicReference.compareAndSet(deadline2, deadline);
        } while (!bl);
    }

    private static boolean isAfter(long l, long l2) {
        return l - l2 > 0L;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean canReserve(AtomicReference<Deadline> atomicReference, ActionKind actionKind, boolean bl) {
        long l = NativeSourcePacketExecutionContext.getArrivalNanos();
        boolean bl2 = NativeSourcePacketExecutionContext.hasArrivalNanos();
        Object object = this._mutationLock;
        synchronized (object) {
            long l2 = this._nanoClock.getAsLong();
            long l3 = bl2 ? l : l2;
            Deadline deadline = this._nextVisibleActionAtNanos.get();
            if (NativeSourceClientActionTimeline.conflictsWithSkillAtIngress(bl2, actionKind, l3, deadline)) {
                NativeSourceClientActionTimeline.recordSkillConflict(actionKind, deadline);
                return false;
            }
            if (NativeSourceClientActionTimeline.isActiveAt(deadline, l2) && !NativeSourceClientActionTimeline.canInterrupt(actionKind, deadline, bl)) {
                NativeSourceClientActionTimeline.recordSkillConflict(actionKind, deadline);
                return false;
            }
            Deadline deadline2 = atomicReference.get();
            if (NativeSourceClientActionTimeline.isActiveAt(deadline2, l3) && !NativeSourceClientActionTimeline.canInterrupt(actionKind, deadline2, bl)) {
                NativeSourceClientActionTimeline.recordSkillConflict(actionKind, deadline2);
                return false;
            }
            return true;
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
            Object object = this._mutationLock;
            synchronized (object) {
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
            Object object = this._mutationLock;
            synchronized (object) {
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
