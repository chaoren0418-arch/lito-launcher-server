// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

record RepositoryWarehousePassword(boolean authoritative, int password) {
    RepositoryWarehousePassword {
        if (password < 0 || password > 999999) {
            throw new IllegalArgumentException("invalid repository warehouse password");
        }
    }

    static RepositoryWarehousePassword unavailable() {
        return new RepositoryWarehousePassword(false, 0);
    }
}
