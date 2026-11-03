// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

record RepositoryTournamentShopOffer(long itemId, int sortOrder, long buyPrice, long sellPrice) {
    RepositoryTournamentShopOffer {
        if (itemId <= 0L || sortOrder < 0 || buyPrice <= 0L || sellPrice <= 0L) {
            throw new IllegalArgumentException("invalid Tournament shop database row");
        }
    }
}
