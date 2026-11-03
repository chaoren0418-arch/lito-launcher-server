// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.GameStateStore;
import local.lito381.server.RepositoryHouseSettlementStatus;

record RepositoryHouseSettlement(RepositoryHouseSettlementStatus status, GameStateStore.HouseAuction auction, long oldOwnerObjectId, long payout, long bidderObjectId, String bidder) {
    RepositoryHouseSettlement {
        if (status == null || oldOwnerObjectId < 0L || payout < 0L || bidderObjectId < 0L) {
            throw new IllegalArgumentException("invalid repository house settlement");
        }
        bidder = bidder == null ? "" : bidder;
    }

    static RepositoryHouseSettlement unavailable() {
        return new RepositoryHouseSettlement(RepositoryHouseSettlementStatus.UNAVAILABLE, null, 0L, 0L, 0L, "");
    }

    boolean settled() {
        return this.status == RepositoryHouseSettlementStatus.TRANSFERRED || this.status == RepositoryHouseSettlementStatus.CANCELLED || this.status == RepositoryHouseSettlementStatus.EXTENDED;
    }
}
