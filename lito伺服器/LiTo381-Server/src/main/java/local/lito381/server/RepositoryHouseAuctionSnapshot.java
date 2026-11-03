// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.List;
import local.lito381.server.GameStateStore;

record RepositoryHouseAuctionSnapshot(boolean authoritative, List<GameStateStore.HouseAuction> auctions) {
    RepositoryHouseAuctionSnapshot {
        List<Object> list = auctions = auctions == null ? List.of() : List.copyOf(auctions);
        if (!authoritative && !auctions.isEmpty()) {
            throw new IllegalArgumentException("unavailable house auction has rows");
        }
    }

    static RepositoryHouseAuctionSnapshot unavailable() {
        return new RepositoryHouseAuctionSnapshot(false, List.of());
    }

    GameStateStore.HouseAuction byId(long houseId) {
        return this.auctions.stream().filter(row -> row.houseId() == houseId).findFirst().orElse(null);
    }
}
