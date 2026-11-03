// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.GameStateStore;

record RepositoryWarehousePasswordChange(boolean authoritative, GameStateStore.WarehousePasswordStatus status) {
    RepositoryWarehousePasswordChange {
        if (status == null) {
            throw new IllegalArgumentException("password status is required");
        }
    }

    static RepositoryWarehousePasswordChange unavailable() {
        return new RepositoryWarehousePasswordChange(false, GameStateStore.WarehousePasswordStatus.UNCHANGED);
    }
}
