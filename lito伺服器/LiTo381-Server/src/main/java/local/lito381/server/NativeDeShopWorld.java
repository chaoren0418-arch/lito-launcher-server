// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.NativeDeShopTable;
import local.lito381.server.NativeDeTerrainWorld;
import local.lito381.server.NativeDeWorldContext;

final class NativeDeShopWorld {
    static boolean classicAllowed(NativeDeWorldContext nativeDeWorldContext, int n, int n2, int n3, int n4) {
        return NativeDeTerrainWorld.call(nativeDeWorldContext, () -> NativeDeTerrainWorld.classic(nativeDeWorldContext, n, n2).isUsableShop(n3, n4));
    }

    static int classicCurrency(NativeDeWorldContext nativeDeWorldContext, int n, int n2, int n3, int n4) {
        return NativeDeTerrainWorld.call(nativeDeWorldContext, () -> {
            NativeDeTerrainWorld.classic(nativeDeWorldContext, n, n2);
            return NativeDeShopTable.get().getCurrency(n2, n3, n4);
        });
    }

    private NativeDeShopWorld() {
    }
}
