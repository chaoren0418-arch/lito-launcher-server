// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.GameStateStore;
import local.lito381.server.RepositoryHouseBidStatus;

record RepositoryHouseBidMutation(RepositoryHouseBidStatus status, GameStateStore.HouseAuction auction, String previousBidder, long previousBidderObjectId, long previousPrice) {
    RepositoryHouseBidMutation {
        if (status == null || previousBidderObjectId < 0L || previousPrice < 0L) {
            throw new IllegalArgumentException("invalid repository house bid result");
        }
        previousBidder = previousBidder == null ? "" : previousBidder;
    }

    static RepositoryHouseBidMutation unavailable() {
        return new RepositoryHouseBidMutation(RepositoryHouseBidStatus.UNAVAILABLE, null, "", 0L, 0L);
    }

    boolean accepted() {
        return this.status == RepositoryHouseBidStatus.ACCEPTED;
    }
}
