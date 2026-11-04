// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import local.lito381.server.NativeDeWorldScope;

final class NativeDeWorldMaps {
    final ConcurrentHashMap<Integer, Object> maps = new ConcurrentHashMap();

    NativeDeWorldMaps(Set<Integer> set) {
        for (int n : set) {
            this.maps.put(n, new Object());
        }
    }

    static NativeDeWorldMaps get() {
        return NativeDeWorldScope.owner().maps;
    }

    Map<Integer, Object> getMaps() {
        return this.maps;
    }

    boolean hasMap(int n) {
        return this.maps.containsKey(n);
    }
}
