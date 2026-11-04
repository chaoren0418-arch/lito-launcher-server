// DECOMPILED from base-0.6.71.jar, TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.concurrent.RejectedExecutionException;
import local.lito381.server.NativeDeAiTimeline;

final class NativeDeAiTimerProtocol {
    private NativeDeAiTimerProtocol() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Object schedule(NativeDeAiTimeline.State state, Port port, long l, int n, boolean bl) {
        long l2;
        if (port.shutdown()) {
            NativeDeAiTimeline.stop(state, port, l);
            return null;
        }
        Object object;
        synchronized (state.lock) {
            if (!NativeDeAiTimeline.owns(state, l) || state.chain != l || state.stop || !state.pending || state.pendingBackground != bl) {
                return null;
            }
            if (state.activeTimer != 0L) {
                return state.future;
            }
            state.activeTimer = l2 = NativeDeAiTimeline.nextTimer(state);
        }
        try {
            object = port.install(l, l2, bl, Math.max(0, n));
            if (object != null) {
                boolean bl2 = false;
                synchronized (state.lock) {
                    if (NativeDeAiTimeline.owns(state, l) && state.chain == l && !state.stop && state.activeTimer == l2) {
                        state.future = object;
                        bl2 = true;
                    }
                }
                if (!bl2) {
                    port.cancel(object);
                    return null;
                }
                return object;
            }
            port.failure(Failure.NULL_FUTURE);
        }
        catch (RejectedExecutionException rejectedExecutionException) {
            port.failure(Failure.REJECTED);
        }
        catch (RuntimeException runtimeException) {
            port.failure(Failure.ERROR);
        }
        NativeDeAiTimeline.failTimer(state, port, l, l2);
        return null;
    }

    static interface Port
    extends NativeDeAiTimeline.Port {
        public Object install(long var1, long var3, boolean var5, int var6);

        public void failure(Failure var1);
    }

    static enum Failure {
        NULL_FUTURE,
        REJECTED,
        ERROR;

    }
}
