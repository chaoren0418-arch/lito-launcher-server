// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.RepositoryPendingReward;
import local.lito381.server.RepositoryPendingRewardClaimStatus;

record RepositoryPendingRewardClaim(RepositoryPendingRewardClaimStatus status, RepositoryPendingReward reward, boolean personalWarehouse) {
    RepositoryPendingRewardClaim {
        if (status == null) {
            throw new IllegalArgumentException("reward claim status is required");
        }
    }

    static RepositoryPendingRewardClaim unavailable(RepositoryPendingReward reward) {
        return new RepositoryPendingRewardClaim(RepositoryPendingRewardClaimStatus.READ_ONLY, reward, false);
    }
}
