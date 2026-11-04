// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.concurrent.atomic.AtomicLong;
import local.lito381.server.BattleInvisibilityStatus;
import local.lito381.server.BattleTimedStatus;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatPoisonAtomic;
import local.lito381.server.CombatRules;
import local.lito381.server.NativeEffectCompletions;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativePlayerVitalCompletions;
import local.lito381.server.Protocol;

final class NativeAcceptedState {
    private static final String SLEEP = "sourceSleepLease";
    private static final String INVIS = "sourceInvisibilityLease";
    private static final ThreadLocal<Publication> publication = new ThreadLocal();
    private static final AtomicLong cancelledSleep = new AtomicLong();
    private static final AtomicLong cancelledInvis = new AtomicLong();
    private static final AtomicLong staleTails = new AtomicLong();
    private static final AtomicLong droppedPackets = new AtomicLong();

    NativeAcceptedState() {
    }

    static void sleepChanged(BattleTimedStatus battleTimedStatus) {
        if (CombatRules.loaded()) {
            CombatAccess.set(battleTimedStatus, SLEEP, new Object());
        }
    }

    static void invisibleChanged(BattleInvisibilityStatus battleInvisibilityStatus) {
        if (CombatRules.loaded()) {
            CombatAccess.set(battleInvisibilityStatus, INVIS, new Object());
        }
    }

    private static Object token(Object object, String string) {
        Object object2 = CombatAccess.field(object, string);
        if (object2 == null) {
            object2 = new Object();
            CombatAccess.set(object, string, object2);
        }
        return object2;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Incoming capture(ClientSession clientSession, boolean bl) {
        if (!bl) {
            return Incoming.empty();
        }
        BattleTimedStatus battleTimedStatus = CombatPoisonAtomic.status(clientSession);
        Object object = null;
        Object object2 = null;
        Object object3 = battleTimedStatus;
        synchronized (object3) {
            if (NativePlayerCommit.number(battleTimedStatus, "sleepExpiresAtNanos") != 0L) {
                object = NativeAcceptedState.token(battleTimedStatus, SLEEP);
            }
        }
        Object object4 = object3 = (BattleInvisibilityStatus)CombatAccess.field(clientSession, "invisibilityStatus");
        synchronized (object4) {
            if (((BattleInvisibilityStatus)object3).active()) {
                object2 = NativeAcceptedState.token(object3, INVIS);
            }
        }
        return new Incoming(true, object, object2);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void invisible(ClientSession clientSession, boolean bl) {
        BattleInvisibilityStatus battleInvisibilityStatus;
        if (!CombatRules.loaded()) {
            CombatAccess.set(clientSession, "invisible", bl);
            return;
        }
        BattleInvisibilityStatus battleInvisibilityStatus2 = battleInvisibilityStatus = (BattleInvisibilityStatus)CombatAccess.field(clientSession, "invisibilityStatus");
        synchronized (battleInvisibilityStatus2) {
            CombatAccess.set(clientSession, "invisible", battleInvisibilityStatus.active());
        }
    }

    static boolean isStale(Throwable throwable) {
        for (Throwable throwable2 = throwable; throwable2 != null; throwable2 = throwable2.getCause()) {
            if (!(throwable2 instanceof Stale)) continue;
            return true;
        }
        return false;
    }

    static void requireCurrent() {
        NativePlayerCommit.Frame frame = NativePlayerCommit.frame();
        if (frame != null && !NativePlayerCommit.validFrame(frame.pc)) {
            staleTails.incrementAndGet();
            throw new Stale();
        }
    }

    static void requireReceiver(ClientSession clientSession) {
        NativePlayerCommit.Frame frame = NativePlayerCommit.frame();
        if (frame != null && frame.pc == clientSession) {
            NativeAcceptedState.requireCurrent();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void sleep(ClientSession clientSession, String string) {
        NativePlayerCommit.Frame frame = NativePlayerCommit.frame();
        if (frame == null) {
            CombatAccess.call(clientSession, "cancelSleep", "(Ljava/lang/String;)V", string);
            return;
        }
        if (!frame.accepted) {
            return;
        }
        Incoming incoming = frame.event.incoming();
        if (!incoming.positive || incoming.sleep == null) {
            return;
        }
        Object object = null;
        BattleTimedStatus battleTimedStatus = CombatPoisonAtomic.status(clientSession);
        NativePlayerVitalCompletions.Receipt receipt = NativePlayerVitalCompletions.reserve(clientSession);
        if (receipt == null) {
            return;
        }
        try {
            frame.queue.vitalLock.lock();
            try {
                BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
                synchronized (battleTimedStatus2) {
                    if (NativePlayerCommit.validFrame(clientSession) && CombatAccess.field(battleTimedStatus, SLEEP) == incoming.sleep && battleTimedStatus.cancelSleep()) {
                        object = CombatAccess.field(battleTimedStatus, SLEEP);
                    }
                }
                if (object != null) {
                    cancelledSleep.incrementAndGet();
                    NativeEffectCompletions.state(receipt, frame, object, true, string);
                }
            }
            finally {
                frame.queue.vitalLock.unlock();
            }
        }
        finally {
            receipt.close();
            NativePlayerVitalCompletions.drain(clientSession);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void reveal(ClientSession clientSession, String string) {
        NativePlayerCommit.Frame frame = NativePlayerCommit.frame();
        if (frame == null) {
            CombatAccess.call(clientSession, "revealInvisibility", "(Ljava/lang/String;)V", string);
            return;
        }
        if (!frame.accepted) {
            return;
        }
        Incoming incoming = frame.event.incoming();
        if (!incoming.positive || incoming.invisible == null) {
            return;
        }
        Object object = null;
        BattleTimedStatus battleTimedStatus = CombatPoisonAtomic.status(clientSession);
        BattleInvisibilityStatus battleInvisibilityStatus = (BattleInvisibilityStatus)CombatAccess.field(clientSession, "invisibilityStatus");
        NativePlayerVitalCompletions.Receipt receipt = NativePlayerVitalCompletions.reserve(clientSession);
        if (receipt == null) {
            return;
        }
        try {
            frame.queue.vitalLock.lock();
            try {
                BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
                synchronized (battleTimedStatus2) {
                    BattleInvisibilityStatus battleInvisibilityStatus2 = battleInvisibilityStatus;
                    synchronized (battleInvisibilityStatus2) {
                        if (NativePlayerCommit.validFrame(clientSession) && battleInvisibilityStatus.cancelCaptured(incoming.invisible)) {
                            CombatAccess.set(clientSession, "invisible", battleInvisibilityStatus.active());
                            object = CombatAccess.field(battleInvisibilityStatus, INVIS);
                        }
                    }
                }
                if (object != null) {
                    cancelledInvis.incrementAndGet();
                    if (!battleInvisibilityStatus.active()) {
                        NativeEffectCompletions.state(receipt, frame, object, false, string);
                    }
                }
            }
            finally {
                frame.queue.vitalLock.unlock();
            }
        }
        finally {
            receipt.close();
            NativePlayerVitalCompletions.drain(clientSession);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void publish(NativePlayerCommit.Frame frame, Object object, boolean bl, Runnable runnable) {
        Publication publication = NativeAcceptedState.publication.get();
        NativeAcceptedState.publication.set(new Publication(frame, object, bl, publication));
        try {
            if (NativeAcceptedState.allowPublication(frame.pc)) {
                runnable.run();
            }
        }
        catch (RuntimeException runtimeException) {
            NativePlayerCommit.secondaryFailure(bl ? "accepted sleep release" : "accepted invisibility reveal", runtimeException);
        }
        finally {
            if (publication == null) {
                NativeAcceptedState.publication.remove();
            } else {
                NativeAcceptedState.publication.set(publication);
            }
        }
    }

    static boolean allowPublication(ClientSession clientSession) {
        Publication publication = NativeAcceptedState.publication.get();
        if (publication == null) {
            return true;
        }
        boolean bl = NativePlayerCommit.validFrame(publication.frame.pc);
        if (bl) {
            Object object = publication.sleep ? CombatPoisonAtomic.status(publication.frame.pc) : CombatAccess.field(publication.frame.pc, "invisibilityStatus");
            boolean bl2 = bl = CombatAccess.field(object, publication.sleep ? SLEEP : INVIS) == publication.released;
        }
        if (!bl) {
            droppedPackets.incrementAndGet();
        }
        return bl;
    }

    static boolean publishing() {
        return publication.get() != null;
    }

    static boolean idle() {
        return publication.get() == null;
    }

    static long cancelledSleep() {
        return cancelledSleep.get();
    }

    static long cancelledInvisible() {
        return cancelledInvis.get();
    }

    static long staleTails() {
        return staleTails.get();
    }

    static long droppedPackets() {
        return droppedPackets.get();
    }

    private static /* synthetic */ void lambda$reveal$1(ClientSession clientSession, String string) {
        CombatAccess.call(clientSession, "publishInvisibilityReveal", "(Ljava/lang/String;)V", string);
    }

    private static /* synthetic */ void lambda$sleep$0(ClientSession clientSession, String string) {
        NativePlayerCommit.safeAuxSend(clientSession, Protocol.paralysis(Protocol.ParalysisMode.SLEEP_RELEASE), "S_PARALYSE mode=11 reason=" + string);
    }

    static void completeState(NativePlayerCommit.Frame frame, Object object, boolean bl, Runnable runnable) {
        NativeAcceptedState.publish(frame, object, bl, runnable);
    }

    record Incoming(boolean positive, Object sleep, Object invisible) {
        static Incoming empty() {
            return new Incoming(false, null, null);
        }
    }

    static final class Stale
    extends RuntimeException {
        Stale() {
            super(null, null, false, false);
        }
    }

    private record Publication(NativePlayerCommit.Frame frame, Object released, boolean sleep, Publication previous) {
    }
}
