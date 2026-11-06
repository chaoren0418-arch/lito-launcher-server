// DECOMPILED from candidate-50635f7e.jar (CFR 0.152), TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.NativeAreaMaterialAction;
import local.lito381.server.NativeDeItemPublication;
import local.lito381.server.NativePlayerActionScope;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativePlayerLifetime;
import local.lito381.server.NativeReuseShutdown;
import local.lito381.server.NativeRolePublication;
import local.lito381.server.NativeSelfCastAction;
import local.lito381.server.NativeServerLifetime;
import local.lito381.server.NativeSourceMaterialPairIngress;
import local.lito381.server.NativeSourceTargetNegativeLife;
import local.lito381.server.NativeSourceTargetNegativeReserve;
import local.lito381.server.Protocol;

final class NativePlayerVitalCompletions {
    static final String FIELD = "sourceVitalCompletions";
    private static final ConcurrentHashMap<NativePlayerLifetime, Long> pending = new ConcurrentHashMap();

    private static State state(ClientSession clientSession) {
        return (State)CombatAccess.field(clientSession, FIELD);
    }

    static long pending(NativePlayerLifetime nativePlayerLifetime) {
        return pending.getOrDefault(nativePlayerLifetime, 0L);
    }

    static Receipt reserve(ClientSession clientSession) {
        NativePlayerLifetime nativePlayerLifetime = NativePlayerLifetime.of(clientSession);
        NativeServerLifetime nativeServerLifetime = nativePlayerLifetime.application;
        if (nativeServerLifetime == null) {
            return nativePlayerLifetime.processes() ? new Receipt(clientSession, nativePlayerLifetime, null) : null;
        }
        Object object = nativeServerLifetime.fence;
        synchronized (object) {
            boolean bl;
            ArrayDeque<Object> arrayDeque = NativeServerLifetime.packets.get();
            bl = arrayDeque.contains(nativeServerLifetime) || NativePlayerLifetime.onWorker(nativeServerLifetime.hub);
            if (arrayDeque.isEmpty()) {
                NativeServerLifetime.packets.remove();
            }
            if (nativeServerLifetime.closing && !bl) {
                return null;
            }
            ++nativeServerLifetime.handlers;
            try {
                return new Receipt(clientSession, nativePlayerLifetime, nativeServerLifetime);
            }
            catch (Throwable throwable) {
                --nativeServerLifetime.handlers;
                nativeServerLifetime.fence.notifyAll();
                throw throwable;
            }
        }
    }

    static void enqueueLocked(Receipt receipt, Runnable runnable) {
        if (!receipt.pc.nativePlayerActionLock().isHeldByCurrentThread() || receipt.transferred || receipt.released.get()) {
            throw new IllegalStateException("vital completion requires actual locked mutation");
        }
        Runnable runnable2 = NativeDeItemPublication.completion(NativeRolePublication.completion(receipt.pc, runnable));
        NativePlayerVitalCompletions.state((ClientSession)receipt.pc).queue.add(NativePlayerActionScope.completion(receipt.pc, () -> receipt.run(runnable2)));
        receipt.transferred = true;
    }

    static void debit(Receipt receipt, int n, int n2, int n3) {
        NativePlayerVitalCompletions.enqueueLocked(receipt, () -> NativePlayerCommit.safeAuxSend(receipt.pc, Protocol.manaPoint(n, n2), "S_MANA_POINT equipment-debit=" + n3));
    }

    static void recovery(Receipt receipt, NativePlayerCommit.Binding binding, int n, int n2, int n3, int n4, boolean bl, boolean bl2, String string) {
        NativePlayerVitalCompletions.enqueueLocked(receipt, () -> {
            ClientSession clientSession = receipt.pc;
            if (bl) {
                NativePlayerCommit.safeAuxSend(clientSession, new Protocol.Writer().c(13).h(n).h(n2).bytes(), "S_HIT_POINT " + string + "=" + n);
                try {
                    if (binding.matches(clientSession)) {
                        CombatAccess.call(clientSession, "broadcastOwnHitRatio", "(Ljava/lang/String;)V", string);
                    }
                }
                catch (RuntimeException runtimeException) {
                    NativePlayerCommit.secondaryFailure("equipment recovery ratio", runtimeException);
                }
            }
            if (bl2) {
                NativePlayerCommit.safeAuxSend(clientSession, Protocol.manaPoint(n3, n4), "S_MANA_POINT " + string + "=" + n3);
            }
        });
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void drain(ClientSession clientSession) {
        if (NativePlayerActionScope.deferDrain(clientSession)) {
            return;
        }
        ReentrantLock reentrantLock = clientSession.nativePlayerActionLock();
        State state = NativePlayerVitalCompletions.state(clientSession);
        do {
            if (reentrantLock.isHeldByCurrentThread() || !state.draining.compareAndSet(false, true)) {
                return;
            }
            NativePlayerActionScope.Root root = null;
            try {
                do {
                    Runnable runnable;
                    ArrayDeque<Runnable> arrayDeque = new ArrayDeque<Runnable>();
                    reentrantLock.lock();
                    try {
                        while ((runnable = state.queue.peek()) != null) {
                            NativePlayerActionScope.Root root2 = NativePlayerActionScope.blocked(runnable);
                            if (root2 != null) {
                                root = root2;
                                break;
                            }
                            arrayDeque.addLast((Runnable)state.queue.remove());
                        }
                    }
                    finally {
                        reentrantLock.unlock();
                    }
                    if (arrayDeque.isEmpty()) {
                        break;
                    }
                    while ((runnable = (Runnable)arrayDeque.pollFirst()) != null) {
                        try {
                            runnable.run();
                        }
                        catch (Throwable throwable) {
                            NativePlayerVitalCompletions.logCompletionFailure(clientSession, throwable);
                        }
                    }
                } while (root == null);
            }
            finally {
                state.draining.set(false);
            }
            if (root == null || NativePlayerActionScope.resumeAfterDrain(clientSession, root)) continue;
            return;
        } while (!state.queue.isEmpty());
    }

    static void logCompletionFailure(ClientSession clientSession, Throwable throwable) {
        System.getLogger(NativePlayerVitalCompletions.class.getName()).log(System.Logger.Level.ERROR, "Vital completion failed: objectId=" + clientSession.playerObjectId() + " type=" + clientSession.getClass().getName(), throwable);
    }

    private NativePlayerVitalCompletions() {
    }

    static Receipt nativeSourceTargetNegativeReserve(ClientSession clientSession, NativeAreaMaterialAction.Context context, NativeSourceTargetNegativeLife.Proof proof) {
        if (!NativeSourceTargetNegativeReserve.initial(clientSession, context, proof)) {
            return null;
        }
        NativePlayerLifetime nativePlayerLifetime = NativeSourceTargetNegativeReserve.lifetime(clientSession, context, proof);
        NativeServerLifetime nativeServerLifetime = nativePlayerLifetime.application;
        if (nativeServerLifetime == null) {
            return NativeSourceTargetNegativeReserve.processes(nativePlayerLifetime.processes(), clientSession, context, proof, nativeServerLifetime) ? new Receipt(clientSession, nativePlayerLifetime, null) : null;
        }
        Object object = nativeServerLifetime.fence;
        synchronized (object) {
            block12: {
                block11: {
                    boolean bl;
                    if (!NativeSourceTargetNegativeReserve.current(clientSession, context, proof, nativeServerLifetime)) break block11;
                    ArrayDeque<Object> arrayDeque = NativeServerLifetime.packets.get();
                    bl = arrayDeque.contains(nativeServerLifetime) || NativePlayerLifetime.onWorker(nativeServerLifetime.hub);
                    if (arrayDeque.isEmpty()) {
                        NativeServerLifetime.packets.remove();
                    }
                    if (!nativeServerLifetime.closing || bl) break block12;
                }
                return null;
            }
            ++nativeServerLifetime.handlers;
            try {
                return new Receipt(clientSession, nativePlayerLifetime, nativeServerLifetime);
            }
            catch (Throwable throwable) {
                --nativeServerLifetime.handlers;
                nativeServerLifetime.fence.notifyAll();
                throw throwable;
            }
        }
    }

    static Receipt nativeSourceMaterialPairReserve(ClientSession clientSession, NativeSelfCastAction.Task task) {
        if (!NativeSourceMaterialPairIngress.initial(clientSession, task)) {
            return null;
        }
        NativePlayerLifetime nativePlayerLifetime = NativeSourceMaterialPairIngress.lifetime(clientSession, task);
        NativeServerLifetime nativeServerLifetime = nativePlayerLifetime.application;
        if (nativeServerLifetime == null) {
            return NativeSourceMaterialPairIngress.processes(nativePlayerLifetime.processes(), clientSession, task, nativeServerLifetime) ? new Receipt(clientSession, nativePlayerLifetime, null) : null;
        }
        Object object = nativeServerLifetime.fence;
        synchronized (object) {
            block12: {
                block11: {
                    boolean bl;
                    if (!NativeSourceMaterialPairIngress.application(clientSession, task, nativeServerLifetime)) break block11;
                    ArrayDeque<Object> arrayDeque = NativeServerLifetime.packets.get();
                    bl = arrayDeque.contains(nativeServerLifetime) || NativePlayerLifetime.onWorker(nativeServerLifetime.hub);
                    if (arrayDeque.isEmpty()) {
                        NativeServerLifetime.packets.remove();
                    }
                    if (!nativeServerLifetime.closing || bl) break block12;
                }
                return null;
            }
            ++nativeServerLifetime.handlers;
            try {
                return new Receipt(clientSession, nativePlayerLifetime, nativeServerLifetime);
            }
            catch (Throwable throwable) {
                --nativeServerLifetime.handlers;
                nativeServerLifetime.fence.notifyAll();
                throw throwable;
            }
        }
    }

    static final class State {
        final ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue();
        final AtomicBoolean draining = new AtomicBoolean();

        State() {
        }
    }

    static final class Receipt
    implements AutoCloseable {
        final ClientSession pc;
        final NativePlayerLifetime life;
        final NativeServerLifetime app;
        final NativePlayerCommit.Frame frame;
        final AtomicBoolean released = new AtomicBoolean();
        boolean transferred;

        Receipt(ClientSession clientSession, NativePlayerLifetime nativePlayerLifetime2, NativeServerLifetime nativeServerLifetime) {
            this.pc = clientSession;
            this.life = nativePlayerLifetime2;
            this.app = nativeServerLifetime;
            this.frame = NativePlayerCommit.frame();
            pending.compute(nativePlayerLifetime2, (nativePlayerLifetime, l) -> l == null ? 1L : l + 1L);
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        private void release() {
            if (!this.released.compareAndSet(false, true)) {
                throw new IllegalStateException("vital completion receipt released twice");
            }
            pending.compute(this.life, (nativePlayerLifetime, l) -> {
                if (l == null || l <= 0L) {
                    throw new IllegalStateException("vital completion receipt underflow");
                }
                return l == 1L ? null : Long.valueOf(l - 1L);
            });
            if (this.app != null) {
                Object object = this.app.fence;
                synchronized (object) {
                    if (--this.app.handlers < 0) {
                        throw new IllegalStateException("vital application receipt underflow");
                    }
                    this.app.fence.notifyAll();
                }
            }
            NativeReuseShutdown.receiptReleased(this.life);
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        void run(Runnable runnable) {
            ArrayDeque<Object> arrayDeque;
            ArrayDeque<Object> arrayDeque2 = arrayDeque = this.app == null ? null : NativeServerLifetime.packets.get();
            if (arrayDeque != null) {
                arrayDeque.push(this.app);
            }
            try {
                this.life.executeCommitted(() -> NativePlayerCommit.completionFrame(this.frame, runnable));
            }
            finally {
                try {
                    if (arrayDeque != null) {
                        if (arrayDeque.pop() != this.app) {
                            throw new IllegalStateException("vital completion packet context differs");
                        }
                        if (arrayDeque.isEmpty()) {
                            NativeServerLifetime.packets.remove();
                        }
                    }
                }
                finally {
                    this.release();
                }
            }
        }

        @Override
        public void close() {
            if (!this.transferred) {
                this.release();
            }
        }
    }
}
