// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

final class NativeDeWorldMap {
    private static final NativeDeWorldMap NULL = new NativeDeWorldMap(0, true);
    private final int id;
    private final boolean nullMap;

    NativeDeWorldMap(int n) {
        this(n, false);
    }

    private NativeDeWorldMap(int n, boolean bl) {
        this.id = n;
        this.nullMap = bl;
    }

    int getId() {
        return this.id;
    }

    boolean isNull() {
        return this.nullMap;
    }

    static NativeDeWorldMap newNull() {
        return NULL;
    }
}
