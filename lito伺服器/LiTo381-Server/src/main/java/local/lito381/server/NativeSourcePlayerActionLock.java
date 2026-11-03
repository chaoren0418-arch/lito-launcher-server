// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import local.lito381.server.ClientSession;
import local.lito381.server.NativePlayerActionScope;

public final class NativeSourcePlayerActionLock {
    private static final Object IDENTITY_HASH_TIE_LOCK = new Object();

    private NativeSourcePlayerActionLock() {
    }

    private static void sourceRun(ClientSession clientSession, Runnable runnable) {
        Objects.requireNonNull(runnable, "action");
        ReentrantLock reentrantLock = NativeSourcePlayerActionLock.actionLock(clientSession);
        reentrantLock.lock();
        try {
            runnable.run();
        }
        finally {
            reentrantLock.unlock();
            if (!reentrantLock.isHeldByCurrentThread()) {
                NativePlayerActionScope.unlocked(clientSession);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static <T> T sourceCall(ClientSession clientSession, Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "action");
        ReentrantLock reentrantLock = NativeSourcePlayerActionLock.actionLock(clientSession);
        reentrantLock.lock();
        try {
            T t = supplier.get();
            return t;
        }
        finally {
            reentrantLock.unlock();
            if (!reentrantLock.isHeldByCurrentThread()) {
                NativePlayerActionScope.unlocked(clientSession);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void sourcePairOrdered(ClientSession clientSession, ClientSession clientSession2, Runnable runnable) {
        Objects.requireNonNull(runnable, "action");
        if (clientSession == null || clientSession2 == null) {
            throw new NullPointerException("\u73a9\u5bb6\u4e0d\u80fd\u70ba\u7a7a");
        }
        if (clientSession == clientSession2) {
            NativeSourcePlayerActionLock.run(clientSession, runnable);
            return;
        }
        int n = Integer.compare(NativeSourcePlayerActionLock.signedId(clientSession), NativeSourcePlayerActionLock.signedId(clientSession2));
        if (n == 0) {
            int n2 = Integer.compare(System.identityHashCode(clientSession), System.identityHashCode(clientSession2));
            if (n2 == 0) {
                Object object = IDENTITY_HASH_TIE_LOCK;
                synchronized (object) {
                    NativeSourcePlayerActionLock.runLockedPair(clientSession, clientSession2, runnable);
                }
                return;
            }
            ClientSession clientSession3 = n2 < 0 ? clientSession : clientSession2;
            ClientSession clientSession4 = clientSession3 == clientSession ? clientSession2 : clientSession;
            NativeSourcePlayerActionLock.runLockedPair(clientSession3, clientSession4, runnable);
            return;
        }
        ClientSession clientSession5 = n < 0 ? clientSession : clientSession2;
        ClientSession clientSession6 = clientSession5 == clientSession ? clientSession2 : clientSession;
        NativeSourcePlayerActionLock.runLockedPair(clientSession5, clientSession6, runnable);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void runLockedPair(ClientSession clientSession, ClientSession clientSession2, Runnable runnable) {
        ReentrantLock reentrantLock = NativeSourcePlayerActionLock.actionLock(clientSession);
        ReentrantLock reentrantLock2 = NativeSourcePlayerActionLock.actionLock(clientSession2);
        reentrantLock.lock();
        try {
            reentrantLock2.lock();
            try {
                runnable.run();
            }
            finally {
                reentrantLock2.unlock();
            }
        }
        finally {
            reentrantLock.unlock();
            if (!reentrantLock.isHeldByCurrentThread() && !reentrantLock2.isHeldByCurrentThread()) {
                NativePlayerActionScope.unlocked(clientSession);
                NativePlayerActionScope.unlocked(clientSession2);
            }
        }
    }

    private static ReentrantLock actionLock(ClientSession clientSession) {
        return Objects.requireNonNull(clientSession, "pc").nativePlayerActionLock();
    }

    public static void run(ClientSession clientSession, Runnable runnable) {
        Objects.requireNonNull(runnable, "action");
        Objects.requireNonNull(clientSession, "pc");
        try (NativePlayerActionScope.Ticket ticket = NativePlayerActionScope.enter(clientSession);){
            NativeSourcePlayerActionLock.sourceRun(clientSession, runnable);
        }
    }

    public static <T> T call(ClientSession clientSession, Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "action");
        Objects.requireNonNull(clientSession, "pc");
        try (NativePlayerActionScope.Ticket ticket = NativePlayerActionScope.enter(clientSession);){
            T t = NativeSourcePlayerActionLock.sourceCall(clientSession, supplier);
            return t;
        }
    }

    public static void runPairOrdered(ClientSession clientSession, ClientSession clientSession2, Runnable runnable) {
        Objects.requireNonNull(runnable, "action");
        if (clientSession == null || clientSession2 == null) {
            throw new NullPointerException("\u73a9\u5bb6\u4e0d\u80fd\u70ba\u7a7a");
        }
        try (NativePlayerActionScope.Ticket ticket = NativePlayerActionScope.enter(clientSession, clientSession2);){
            NativeSourcePlayerActionLock.sourcePairOrdered(clientSession, clientSession2, runnable);
        }
    }

    static int signedId(ClientSession clientSession) {
        return (int)clientSession.playerObjectId();
    }
}
