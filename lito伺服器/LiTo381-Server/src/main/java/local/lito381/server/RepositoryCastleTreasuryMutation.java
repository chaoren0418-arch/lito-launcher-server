// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.RepositoryCastleSnapshot;
import local.lito381.server.RepositoryCastleTreasuryStatus;

record RepositoryCastleTreasuryMutation(RepositoryCastleTreasuryStatus status, RepositoryCastleSnapshot snapshot) {
    RepositoryCastleTreasuryMutation {
        if (status == null || snapshot == null) {
            throw new IllegalArgumentException("castle treasury mutation result is incomplete");
        }
    }

    static RepositoryCastleTreasuryMutation unavailable() {
        return new RepositoryCastleTreasuryMutation(RepositoryCastleTreasuryStatus.UNAVAILABLE, RepositoryCastleSnapshot.unavailable());
    }

    static RepositoryCastleTreasuryMutation readOnly(RepositoryCastleSnapshot snapshot) {
        return new RepositoryCastleTreasuryMutation(RepositoryCastleTreasuryStatus.READ_ONLY, snapshot == null ? RepositoryCastleSnapshot.unavailable() : snapshot);
    }
}
