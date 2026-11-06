// DECOMPILED from candidate-50635f7e.jar (CFR 0.152), TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import local.lito381.server.NativeAcceptedFreezeMutation;
import local.lito381.server.NativeAcceptedHpContext;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativePlayerVitalCompletions;

final class NativeAcceptedHpCompletion
implements Runnable {
    private final NativePlayerCommit.Frame frame;
    private final AtomicInteger handoff = new AtomicInteger();
    private final AtomicBoolean executed = new AtomicBoolean();

    private NativeAcceptedHpCompletion(NativePlayerCommit.Frame frame) {
        this.frame = frame;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static NativeAcceptedHpCompletion enqueue(NativePlayerCommit.Frame frame) {
        NativePlayerVitalCompletions.Receipt receipt = NativePlayerVitalCompletions.reserve(frame.pc);
        if (receipt == null) {
            throw new IllegalStateException("accepted native HP completion lost application admission");
        }
        try {
            NativeAcceptedHpCompletion nativeAcceptedHpCompletion = new NativeAcceptedHpCompletion(frame);
            NativePlayerVitalCompletions.enqueueLocked(receipt, nativeAcceptedHpCompletion);
            NativeAcceptedHpCompletion nativeAcceptedHpCompletion2 = nativeAcceptedHpCompletion;
            return nativeAcceptedHpCompletion2;
        }
        finally {
            receipt.close();
        }
    }

    boolean suspend() {
        if (this.frame.queue.vitalLock.isHeldByCurrentThread()) {
            throw new IllegalStateException("native HP lane handoff while vital lock held");
        }
        if (this.handoff.compareAndSet(0, 2)) {
            return true;
        }
        if (this.handoff.get() != 1) {
            throw new IllegalStateException("native HP lane handed off twice");
        }
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void run() {
        if (!this.executed.compareAndSet(false, true)) {
            throw new IllegalStateException("native accepted HP completion replay");
        }
        NativeAcceptedHpContext nativeAcceptedHpContext = null;
        try {
            nativeAcceptedHpContext = NativeAcceptedHpContext.detach();
            NativePlayerCommit.acceptedTail(this.frame);
            NativeAcceptedFreezeMutation.completed(this.frame);
        }
        catch (Throwable throwable) {
            NativePlayerCommit.acceptedTailFailure(this.frame, throwable);
        }
        finally {
            try {
                if (!this.handoff.compareAndSet(0, 1)) {
                    if (!this.handoff.compareAndSet(2, 3)) {
                        throw new IllegalStateException("native HP continuation claimed twice");
                    }
                    NativePlayerCommit.resumeAccepted(this.frame.pc, this.frame.queue);
                }
            }
            finally {
                if (nativeAcceptedHpContext != null) {
                    nativeAcceptedHpContext.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static NativeAcceptedHpCompletion enqueue(NativePlayerCommit.Frame frame, NativePlayerVitalCompletions.Receipt receipt) {
        if (receipt == null) {
            throw new IllegalStateException("accepted native HP completion lost application admission");
        }
        try {
            NativeAcceptedHpCompletion nativeAcceptedHpCompletion = new NativeAcceptedHpCompletion(frame);
            NativePlayerVitalCompletions.enqueueLocked(receipt, nativeAcceptedHpCompletion);
            NativeAcceptedHpCompletion nativeAcceptedHpCompletion2 = nativeAcceptedHpCompletion;
            return nativeAcceptedHpCompletion2;
        }
        finally {
            receipt.close();
        }
    }
}
