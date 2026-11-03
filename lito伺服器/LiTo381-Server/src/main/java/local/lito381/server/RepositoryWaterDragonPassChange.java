// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.GameStateStore;

record RepositoryWaterDragonPassChange(GameStateStore.WaterDragonPassState pass) {
    RepositoryWaterDragonPassChange {
        if (pass == null) {
            throw new IllegalArgumentException("Water Dragon pass SQL change is required");
        }
    }
}
