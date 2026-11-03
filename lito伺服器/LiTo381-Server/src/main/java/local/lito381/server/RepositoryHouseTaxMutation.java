// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.GameStateStore;
import local.lito381.server.RepositoryHouseTaxState;
import local.lito381.server.RepositoryHouseTaxStatus;

record RepositoryHouseTaxMutation(RepositoryHouseTaxStatus status, RepositoryHouseTaxState house, GameStateStore.HouseAuction auction, long previousClanId) {
    RepositoryHouseTaxMutation {
        if (status == null || previousClanId < 0L) {
            throw new IllegalArgumentException("invalid repository house tax mutation");
        }
    }

    static RepositoryHouseTaxMutation unavailable() {
        return new RepositoryHouseTaxMutation(RepositoryHouseTaxStatus.UNAVAILABLE, null, null, 0L);
    }

    boolean auctioned() {
        return this.status == RepositoryHouseTaxStatus.AUCTIONED;
    }
}
