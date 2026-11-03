// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.List;
import local.lito381.server.RepositoryPendingReward;

record RepositoryPendingRewardSnapshot(boolean authoritative, List<RepositoryPendingReward> rewards) {
    RepositoryPendingRewardSnapshot {
        List<Object> list = rewards = rewards == null ? List.of() : List.copyOf(rewards);
        if (!authoritative && !rewards.isEmpty()) {
            throw new IllegalArgumentException("unavailable pending rewards contain rows");
        }
    }

    static RepositoryPendingRewardSnapshot unavailable() {
        return new RepositoryPendingRewardSnapshot(false, List.of());
    }
}
