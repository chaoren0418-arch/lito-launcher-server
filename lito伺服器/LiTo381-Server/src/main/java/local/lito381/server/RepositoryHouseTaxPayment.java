// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.RepositoryHouseTaxPaymentStatus;
import local.lito381.server.RepositoryHouseTaxQuote;

record RepositoryHouseTaxPayment(RepositoryHouseTaxPaymentStatus status, RepositoryHouseTaxQuote quote) {
    RepositoryHouseTaxPayment {
        if (status == null) {
            throw new IllegalArgumentException("invalid repository house-tax payment");
        }
    }

    static RepositoryHouseTaxPayment unavailable() {
        return new RepositoryHouseTaxPayment(RepositoryHouseTaxPaymentStatus.UNAVAILABLE, null);
    }

    boolean paid() {
        return this.status == RepositoryHouseTaxPaymentStatus.SUCCESS;
    }
}
