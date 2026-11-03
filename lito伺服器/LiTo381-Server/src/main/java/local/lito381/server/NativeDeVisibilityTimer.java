// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;
import local.lito381.server.NativeDeVisibilityClock;
import local.lito381.server.NativeDeVisibilityCoordinator;
import local.lito381.server.NativeDeVisibilityScope;
import local.lito381.server.NativeDeWorldLog;

public class NativeDeVisibilityTimer
implements Runnable {
    private static final NativeDeWorldLog _log = NativeDeVisibilityScope.Logs.getLogger(NativeDeVisibilityTimer.class);
    private ScheduledFuture<?> _timer;
    private static final AtomicLong SCHEDULE_REJECTED_COUNT = new AtomicLong();
    private static final AtomicLong SCHEDULE_ERROR_COUNT = new AtomicLong();
    private static final AtomicLong RESTART_COUNT = new AtomicLong();

    public static String getMonitorLine() {
        return "PcUpdateObjectTimer scheduleRejected=" + NativeDeVisibilityScope.SCHEDULE_REJECTED_COUNT().get() + " scheduleError=" + NativeDeVisibilityScope.SCHEDULE_ERROR_COUNT().get() + " restart=" + NativeDeVisibilityScope.RESTART_COUNT().get();
    }

    public void start() {
        int n = 300;
        this._timer = this.scheduleTimer(300);
    }

    private ScheduledFuture<?> scheduleTimer(int n) {
        try {
            ScheduledFuture<?> scheduledFuture = NativeDeVisibilityClock.get().pcScheduleAtFixedRate(this, n, n);
            if (scheduledFuture != null) {
                return scheduledFuture;
            }
            NativeDeVisibilityScope.SCHEDULE_ERROR_COUNT().incrementAndGet();
            NativeDeVisibilityScope.log().warn("[PcUpdateObjectTimer] scheduleAtFixedRate returned null: interval={}", (Object)n);
        }
        catch (RejectedExecutionException rejectedExecutionException) {
            NativeDeVisibilityScope.SCHEDULE_REJECTED_COUNT().incrementAndGet();
            NativeDeVisibilityScope.log().warn("[PcUpdateObjectTimer] scheduleAtFixedRate rejected: interval={}", (Object)n, (Object)rejectedExecutionException);
        }
        catch (RuntimeException runtimeException) {
            NativeDeVisibilityScope.SCHEDULE_ERROR_COUNT().incrementAndGet();
            NativeDeVisibilityScope.log().warn("[PcUpdateObjectTimer] scheduleAtFixedRate failed: interval={}", (Object)n, (Object)runtimeException);
        }
        return null;
    }

    private void cancelTimer() {
        try {
            if (this._timer != null) {
                NativeDeVisibilityClock.get().cancel(this._timer, false);
            }
        }
        catch (RuntimeException runtimeException) {
            NativeDeVisibilityScope.log().warn("[PcUpdateObjectTimer] timer cancel failed", runtimeException);
        }
        finally {
            this._timer = null;
        }
    }

    private void restartTimer() {
        ScheduledFuture<?> scheduledFuture = this._timer;
        if (scheduledFuture == null || !scheduledFuture.isCancelled() && !scheduledFuture.isDone()) {
            return;
        }
        this.cancelTimer();
        NativeDeVisibilityScope.RESTART_COUNT().incrementAndGet();
        NativeDeVisibilityTimer nativeDeVisibilityTimer = new NativeDeVisibilityTimer();
        nativeDeVisibilityTimer.start();
    }

    @Override
    public void run() {
        try {
            NativeDeVisibilityCoordinator.tick();
        }
        catch (Exception exception) {
            NativeDeVisibilityScope.log().error("PC \u53ef\u898b\u7269\u66f4\u65b0\u8655\u7406\u6642\u9593\u8ef8\u7570\u5e38", exception);
            this.restartTimer();
        }
    }
}
