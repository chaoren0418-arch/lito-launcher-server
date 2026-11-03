// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.RepositoryHouseTaxQuoteStatus;
import local.lito381.server.RepositoryHouseTaxState;

record RepositoryHouseTaxQuote(RepositoryHouseTaxQuoteStatus status, RepositoryHouseTaxState house, int keeperNpcId, long ownerClanId, String ownerClanName, String ownerLeaderName) {
    RepositoryHouseTaxQuote {
        if (status == null || keeperNpcId < 0 || ownerClanId < 0L) {
            throw new IllegalArgumentException("invalid repository house-tax quote");
        }
        ownerClanName = ownerClanName == null ? "" : ownerClanName;
        String string = ownerLeaderName = ownerLeaderName == null ? "" : ownerLeaderName;
        if (!(status != RepositoryHouseTaxQuoteStatus.UNAVAILABLE || house == null && keeperNpcId == 0 && ownerClanId == 0L && ownerClanName.isEmpty() && ownerLeaderName.isEmpty())) {
            throw new IllegalArgumentException("unavailable house-tax quote has data");
        }
        if (status != RepositoryHouseTaxQuoteStatus.UNAVAILABLE && status != RepositoryHouseTaxQuoteStatus.NOT_FOUND && (house == null || keeperNpcId <= 0)) {
            throw new IllegalArgumentException("house-tax quote is missing its house");
        }
        if ((status == RepositoryHouseTaxQuoteStatus.MEMBER || status == RepositoryHouseTaxQuoteStatus.OCCUPIED) && (ownerClanId <= 0L || ownerClanName.isBlank() || ownerLeaderName.isBlank())) {
            throw new IllegalArgumentException("occupied house-tax quote has no owner");
        }
    }

    static RepositoryHouseTaxQuote unavailable() {
        return new RepositoryHouseTaxQuote(RepositoryHouseTaxQuoteStatus.UNAVAILABLE, null, 0, 0L, "", "");
    }

    boolean member() {
        return this.status == RepositoryHouseTaxQuoteStatus.MEMBER;
    }
}
