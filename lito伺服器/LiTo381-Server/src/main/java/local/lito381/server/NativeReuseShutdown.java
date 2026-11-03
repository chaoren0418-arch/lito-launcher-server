// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import local.lito381.server.NativePlayerLifetime;
import local.lito381.server.NativePlayerVitalCompletions;

final class NativeReuseShutdown {
    private static final Set<NativePlayerLifetime> deferred = ConcurrentHashMap.newKeySet();

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean defer(NativePlayerLifetime nativePlayerLifetime) {
        Object object = nativePlayerLifetime.admission;
        synchronized (object) {
            if (NativePlayerVitalCompletions.pending(nativePlayerLifetime) == 0L) {
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
            if (NativePlayerVitalCompletions.pending(nativePlayerLifetime) == 0L && deferred.remove(nativePlayerLifetime)) {
                nativePlayerLifetime.retry.shutdown();
                nativePlayerLifetime.workers.shutdown();
            }
        }
    }

    private NativeReuseShutdown() {
    }
}
