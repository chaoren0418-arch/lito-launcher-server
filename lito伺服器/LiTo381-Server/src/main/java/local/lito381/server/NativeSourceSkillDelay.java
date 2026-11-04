// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import local.lito381.server.NativeReuseActor;
import local.lito381.server.NativeReuseLog;

public class NativeSourceSkillDelay {
    private static final NativeReuseLog.Logger _log = NativeReuseLog.getLogger(NativeSourceSkillDelay.class);
    private static final AtomicLong USE_COUNT = new AtomicLong();
    private static final AtomicLong SCHEDULED_COUNT = new AtomicLong();
    private static final AtomicLong ACTIVE_COUNT = new AtomicLong();
    private static final AtomicLong COMPLETED_COUNT = new AtomicLong();
    private static final AtomicLong TIMER_RUN_COUNT = new AtomicLong();
    private static final AtomicLong DELAY_SET_COUNT = new AtomicLong();
    private static final AtomicLong DELAY_CLEAR_COUNT = new AtomicLong();
    private static final AtomicLong SKIP_COUNT = new AtomicLong();
    private static final AtomicLong REJECTED_COUNT = new AtomicLong();
    private static final AtomicLong ERROR_COUNT = new AtomicLong();

    private NativeSourceSkillDelay() {
    }

    private static ScheduledFuture<?> scheduleDelayTimer(Lease lease, int n) {
        if (lease == null || !lease.isOwned()) {
            SKIP_COUNT.incrementAndGet();
            return null;
        }
        try {
            SCHEDULED_COUNT.incrementAndGet();
            ScheduledFuture<?> scheduledFuture = lease._cha.pcSchedule(() -> {
                ACTIVE_COUNT.incrementAndGet();
                try {
                    lease.expire();
                    TIMER_RUN_COUNT.incrementAndGet();
                }
                catch (RuntimeException runtimeException) {
                    ERROR_COUNT.incrementAndGet();
                    _log.error("L1SkillDelay timer failed: chaId={}", lease.characterId(), runtimeException);
                    throw runtimeException;
                }
                finally {
                    COMPLETED_COUNT.incrementAndGet();
                    ACTIVE_COUNT.decrementAndGet();
                }
            }, n);
            if (scheduledFuture == null) {
                REJECTED_COUNT.incrementAndGet();
                ERROR_COUNT.incrementAndGet();
                _log.warn("L1SkillDelay timer schedule returned null: chaId={} time={}", lease.characterId(), n);
                return null;
            }
            return scheduledFuture;
        }
        catch (RejectedExecutionException rejectedExecutionException) {
            REJECTED_COUNT.incrementAndGet();
            ERROR_COUNT.incrementAndGet();
            _log.warn("L1SkillDelay timer rejected: chaId={} time={}", lease.characterId(), n, rejectedExecutionException);
            return null;
        }
        catch (RuntimeException runtimeException) {
            REJECTED_COUNT.incrementAndGet();
            ERROR_COUNT.incrementAndGet();
            _log.warn("L1SkillDelay timer submit failed: chaId={} time={}", lease.characterId(), n, runtimeException);
            return null;
        }
    }

    public static void onSkillUse(NativeReuseActor nativeReuseActor, int n) {
        NativeSourceSkillDelay.tryOnSkillUse(nativeReuseActor, n);
    }

    public static boolean tryOnSkillUse(NativeReuseActor nativeReuseActor, int n) {
        Lease lease = NativeSourceSkillDelay.tryReserveSkillUse(nativeReuseActor, n);
        if (lease == null) {
            return false;
        }
        lease.commit();
        return true;
    }

    public static Lease tryReserveSkillUse(NativeReuseActor nativeReuseActor, int n) {
        USE_COUNT.incrementAndGet();
        if (nativeReuseActor == null) {
            SKIP_COUNT.incrementAndGet();
            return null;
        }
        if (n <= 0) {
            return Lease.noDelay();
        }
        Lease lease = new Lease(nativeReuseActor, new Object(), true, n);
        try {
            if (!nativeReuseActor.trySetSkillDelay(lease._token)) {
                SKIP_COUNT.incrementAndGet();
                return null;
            }
            DELAY_SET_COUNT.incrementAndGet();
            ScheduledFuture<?> scheduledFuture = NativeSourceSkillDelay.scheduleDelayTimer(lease, n);
            if (scheduledFuture == null) {
                lease.cancel();
                return null;
            }
            lease.installFuture(scheduledFuture);
            return lease;
        }
        catch (Exception exception) {
            ERROR_COUNT.incrementAndGet();
            _log.error(exception.getLocalizedMessage(), exception);
            lease.cancel();
            return null;
        }
    }

    public static String getMonitorLine() {
        return String.format("L1SkillDelay use=%d scheduled=%d active=%d done=%d timerRun=%d delaySet=%d delayClear=%d skip=%d rejected=%d error=%d", USE_COUNT.get(), SCHEDULED_COUNT.get(), ACTIVE_COUNT.get(), COMPLETED_COUNT.get(), TIMER_RUN_COUNT.get(), DELAY_SET_COUNT.get(), DELAY_CLEAR_COUNT.get(), SKIP_COUNT.get(), REJECTED_COUNT.get(), ERROR_COUNT.get());
    }

    public static final class Lease {
        private final NativeReuseActor _cha;
        private final Object _token;
        private final boolean _owned;
        private final int _delayMillis;
        private final AtomicBoolean _cancelOpen = new AtomicBoolean(true);
        private final AtomicBoolean _committed = new AtomicBoolean(false);
        private final AtomicBoolean _terminal = new AtomicBoolean(false);
        private volatile long _deadlineNanos;
        private volatile ScheduledFuture<?> _future;

        private Lease(NativeReuseActor nativeReuseActor, Object object, boolean bl, int n) {
            this._cha = nativeReuseActor;
            this._token = object;
            this._owned = bl;
            this._delayMillis = Math.max(0, n);
            this._deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(this._delayMillis);
        }

        private static Lease noDelay() {
            return new Lease(null, null, false, 0);
        }

        private boolean isOwned() {
            return this._owned;
        }

        private int characterId() {
            return this._cha != null ? this._cha.getId() : 0;
        }

        private void installFuture(ScheduledFuture<?> scheduledFuture) {
            this._future = scheduledFuture;
            if (this._terminal.get()) {
                scheduledFuture.cancel(false);
            }
        }

        public void commit() {
            if (this._owned && this._cancelOpen.compareAndSet(true, false)) {
                this._deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(this._delayMillis);
                this._committed.set(true);
            }
        }

        public boolean cancel() {
            if (!this._owned) {
                return true;
            }
            if (!this._cancelOpen.compareAndSet(true, false)) {
                return false;
            }
            this._terminal.set(true);
            ScheduledFuture<?> scheduledFuture = this._future;
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
            }
            if (this._cha.clearSkillDelay(this._token)) {
                DELAY_CLEAR_COUNT.incrementAndGet();
            }
            return true;
        }

        private void expire() {
            if (!this._owned || this._terminal.get()) {
                return;
            }
            if (!this._committed.get()) {
                this.reschedule(Math.max(1L, (long)this._delayMillis));
                return;
            }
            long l = this._deadlineNanos - System.nanoTime();
            if (l > 0L) {
                long l2 = TimeUnit.NANOSECONDS.toMillis(l - 1L) + 1L;
                this.reschedule(l2);
                return;
            }
            if (this._terminal.compareAndSet(false, true) && this._cha.clearSkillDelay(this._token)) {
                DELAY_CLEAR_COUNT.incrementAndGet();
            }
        }

        private void reschedule(long l) {
            int n = (int)Math.min(Integer.MAX_VALUE, Math.max(1L, l));
            ScheduledFuture<?> scheduledFuture = NativeSourceSkillDelay.scheduleDelayTimer(this, n);
            if (scheduledFuture == null) {
                _log.error("L1SkillDelay rebase timer unavailable; keeping gate closed: chaId={} delay={}", this.characterId(), n);
                return;
            }
            this.installFuture(scheduledFuture);
        }
    }
}
