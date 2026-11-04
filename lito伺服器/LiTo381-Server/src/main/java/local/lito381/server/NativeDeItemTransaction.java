// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.InventoryCatalog;
import local.lito381.server.NativeCastMaterial;
import local.lito381.server.NativeDeItemApplication;
import local.lito381.server.NativeDeItemPublication;
import local.lito381.server.NativePcActionAuthority;
import local.lito381.server.RuntimeInventory;

final class NativeDeItemTransaction {
    static <T> T call(ClientSession clientSession, Operation<T> operation) throws IOException {
        return NativePcActionAuthority.item(clientSession, operation);
    }

    static <T> T nativeCall(ClientSession clientSession, Operation<T> operation) throws IOException {
        NativeCastMaterial.enterTransaction(clientSession, operation);
        try {
            T t = NativeDeItemTransaction.nativeMaterialOriginalCall(clientSession, operation);
            return t;
        }
        finally {
            NativeCastMaterial.exitTransaction(clientSession, operation);
        }
    }

    static Change grant(ClientSession clientSession, int n, int n2) throws IOException {
        Change change = NativeDeItemTransaction.call(clientSession, () -> clientSession.nativeDeGrantPeriodicItem(n, n2));
        NativeDeItemPublication.committed(clientSession, change);
        return change;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static RuntimeInventory wireCopy(RuntimeInventory runtimeInventory) {
        RuntimeInventory runtimeInventory2 = runtimeInventory;
        synchronized (runtimeInventory2) {
            RuntimeInventory runtimeInventory3 = new RuntimeInventory();
            LinkedHashMap<Long, RuntimeInventory.Item> linkedHashMap = new LinkedHashMap<Long, RuntimeInventory.Item>();
            for (RuntimeInventory.Item item : runtimeInventory.items()) {
                linkedHashMap.put(item.objectId(), item);
            }
            runtimeInventory3.nativeDeBind(Collections.unmodifiableMap(linkedHashMap));
            runtimeInventory3.nativeDePhysical().putAll(runtimeInventory.nativeDePhysical());
            return runtimeInventory3;
        }
    }

    private NativeDeItemTransaction() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static <T> T nativeMaterialOriginalCall(ClientSession clientSession, Operation<T> operation) throws IOException {
        Object object;
        Object object2 = object = CombatAccess.field(clientSession, "sourceItemAdmissionGate");
        synchronized (object2) {
            NativeDeItemPublication.requireCurrent(clientSession);
            NativeDeItemApplication.Owner owner = NativeDeItemApplication.transactionOwner(clientSession);
            Object object3 = owner == null ? object : owner;
            synchronized (object3) {
                RuntimeInventory runtimeInventory;
                RuntimeInventory runtimeInventory2 = runtimeInventory = clientSession.nativeDeInventory(0);
                synchronized (runtimeInventory2) {
                    return operation.call();
                }
            }
        }
    }

    @FunctionalInterface
    static interface Operation<T> {
        public T call() throws IOException;
    }

    record Change(RuntimeInventory previous, RuntimeInventory next, InventoryCatalog.Definition definition) {        Change {
            previous = NativeDeItemTransaction.wireCopy(previous);
            next = NativeDeItemTransaction.wireCopy(next);
        }
    }
}
