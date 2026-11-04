// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;
import local.lito381.server.BattleTimedStatus;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatPoisonAtomic;
import local.lito381.server.NativeDeItemTransaction;
import local.lito381.server.NativePlayerActionScope;

final class NativePcActionAuthority {
    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    static <T> T item(ClientSession clientSession, NativeDeItemTransaction.Operation<T> operation) throws IOException {
        try (NativePlayerActionScope.Ticket ticket = NativePlayerActionScope.enter(clientSession);){
            BattleTimedStatus battleTimedStatus = CombatPoisonAtomic.status(clientSession);
            ReentrantLock reentrantLock = clientSession.nativePlayerActionLock();
            reentrantLock.lock();
            try {
                BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
                synchronized (battleTimedStatus2) {
                    T t = NativeDeItemTransaction.nativeCall(clientSession, operation);
                    return t;
                }
            }
            finally {
                reentrantLock.unlock();
                NativePlayerActionScope.unlocked(clientSession);
            }
        }
    }

    private NativePcActionAuthority() {
    }
}
