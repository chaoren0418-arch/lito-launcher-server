// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.RepositoryCastleSecuritySettlementStatus;
import local.lito381.server.RepositoryCastleSnapshot;

record RepositoryCastleSecuritySettlement(RepositoryCastleSecuritySettlementStatus status, RepositoryCastleSnapshot snapshot, long warStartMillis, long chargedAdena) {
    RepositoryCastleSecuritySettlement {
        if (status == null || snapshot == null || warStartMillis < 0L || chargedAdena < 0L || chargedAdena > Integer.MAX_VALUE || status == RepositoryCastleSecuritySettlementStatus.READY && (warStartMillis == 0L || chargedAdena == 0L)) {
            throw new IllegalArgumentException("castle security settlement result is incomplete");
        }
    }

    static RepositoryCastleSecuritySettlement unavailable() {
        return new RepositoryCastleSecuritySettlement(RepositoryCastleSecuritySettlementStatus.UNAVAILABLE, RepositoryCastleSnapshot.unavailable(), 0L, 0L);
    }

    static RepositoryCastleSecuritySettlement readOnly(RepositoryCastleSnapshot snapshot, long warStartMillis) {
        return new RepositoryCastleSecuritySettlement(RepositoryCastleSecuritySettlementStatus.READ_ONLY, snapshot == null ? RepositoryCastleSnapshot.unavailable() : snapshot, Math.max(0L, warStartMillis), 0L);
    }
}
