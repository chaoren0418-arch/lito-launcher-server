// DECOMPILED from candidate-50635f7e.jar (CFR 0.152), TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import local.lito381.server.NativePlayerLifetime;
import local.lito381.server.NativeSummonRetirement;

final class NativeReuseShutdown {
    private static final Set<NativePlayerLifetime> deferred = ConcurrentHashMap.newKeySet();

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean defer(NativePlayerLifetime nativePlayerLifetime) {
        Object object = nativePlayerLifetime.admission;
        synchronized (object) {
            if (NativeSummonRetirement.pending(nativePlayerLifetime) == 0L) {
                return false;
            }
            deferred.add(nativePlayerLifetime);
            return true;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void receiptReleased(NativePlayerLifetime nativePlayerLifetime) {
        Object object = nativePlayerLifetime.admission;
        synchronized (object) {
            if (NativeSummonRetirement.pending(nativePlayerLifetime) == 0L && deferred.remove(nativePlayerLifetime)) {
                nativePlayerLifetime.retry.shutdown();
                nativePlayerLifetime.workers.shutdown();
            }
        }
    }

    private NativeReuseShutdown() {
    }
}
