// DECOMPILED from base-0.6.71.jar, TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.NativeDeAiTimeline;

final class NativeDeAiWorkerProtocol {
    private NativeDeAiWorkerProtocol() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void dispatch(NativeDeAiTimeline.State state, Port port, long l, boolean bl) {
        synchronized (state.lock) {
            if (!NativeDeAiTimeline.owns(state, l) || state.chain != l || state.stop || !state.pending || state.pendingBackground != bl || state.executing || state.activeTimer != 0L) {
                return;
            }
        }
        if (port.tryExecute(l, bl)) {
            return;
        }
        long l2 = port.rejected();
        if (port.shutdown()) {
            NativeDeAiTimeline.stop(state, port, l);
            return;
        }
        int n = NativeDeAiTimeline.retry(state, l, bl);
        if (n < 0) {
            return;
        }
        port.retried();
        if (l2 == 1L || (l2 & l2 - 1L) == 0L) {
            port.notice(l2, n);
        }
        port.schedule(l, n, bl);
    }

    static void run(NativeDeAiTimeline.State state, Port port, long l, boolean bl) {
        if (port.shutdown()) {
            NativeDeAiTimeline.stop(state, port, l);
            return;
        }
        if (bl) {
            port.background(l);
        } else {
            port.active(l);
        }
    }

    static void discard(NativeDeAiTimeline.State state, Port port, long l) {
        NativeDeAiTimeline.stop(state, port, l);
    }

    static interface Port
    extends NativeDeAiTimeline.Port {
        public boolean tryExecute(long var1, boolean var3);

        public long rejected();

        public void retried();

        public void notice(long var1, int var3);

        public void active(long var1);

        public void background(long var1);
    }
}
