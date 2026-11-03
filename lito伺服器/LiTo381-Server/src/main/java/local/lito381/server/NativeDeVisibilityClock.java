// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.concurrent.ScheduledFuture;
import local.lito381.server.NativeDeVisibilityJobs;
import local.lito381.server.NativeDeVisibilityScope;

final class NativeDeVisibilityClock {
    NativeDeVisibilityClock() {
    }

    static NativeDeVisibilityClock get() {
        return new NativeDeVisibilityClock();
    }

    ScheduledFuture<?> pcScheduleAtFixedRate(Runnable runnable, long l, long l2) {
        return NativeDeVisibilityJobs.schedule(NativeDeVisibilityScope.owner(), runnable, l, l2);
    }

    void cancel(ScheduledFuture<?> scheduledFuture, boolean bl) {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(bl);
        }
    }
}
