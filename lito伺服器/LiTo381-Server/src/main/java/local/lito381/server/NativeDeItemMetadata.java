// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.NativeDeDarkItem;
import local.lito381.server.NativeDeWorldLog;

final class NativeDeItemMetadata {
    private final int objectId;
    private final NativeDeDarkItem darkItem;

    NativeDeItemMetadata(int n, NativeDeDarkItem nativeDeDarkItem) {
        if (nativeDeDarkItem != null && nativeDeDarkItem.getItemObjId() != n) {
            throw new IllegalArgumentException("dark item identity mismatch");
        }
        this.objectId = n;
        this.darkItem = nativeDeDarkItem;
    }

    static NativeDeItemMetadata empty(int n) {
        return new NativeDeItemMetadata(n, null);
    }

    static NativeDeItemMetadata require(int n, NativeDeItemMetadata nativeDeItemMetadata) {
        if (nativeDeItemMetadata == null || nativeDeItemMetadata.objectId != n) {
            throw new IllegalArgumentException("item metadata identity mismatch");
        }
        return nativeDeItemMetadata;
    }

    NativeDeDarkItem getDarkItem(int n) {
        if (n != this.objectId) {
            throw new IllegalArgumentException("item metadata lookup identity mismatch");
        }
        return this.darkItem;
    }

    static final class Logs {
        Logs() {
        }

        static NativeDeWorldLog getLogger(Class<?> clazz) {
            System.Logger logger = System.getLogger(clazz.getName());
            return (string, string2, objectArray) -> logger.log(System.Logger.Level.WARNING, string2.replace("{}", "%s").formatted(objectArray));
        }
    }
}
