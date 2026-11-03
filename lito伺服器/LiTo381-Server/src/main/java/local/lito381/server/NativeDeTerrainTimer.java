// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

public class NativeDeTerrainTimer {
    private long _begin = System.currentTimeMillis();

    public void reset() {
        this._begin = System.currentTimeMillis();
    }

    public long get() {
        return System.currentTimeMillis() - this._begin;
    }
}
